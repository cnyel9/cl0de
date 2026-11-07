package com.cl0de.app.data

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cl0de.app.Graph
import com.cl0de.app.api.OcJson
import com.cl0de.app.runtime.LlamaState
import com.cl0de.app.runtime.ResumableDownloader
import com.cl0de.app.runtime.RuntimeController
import com.cl0de.app.runtime.TarExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline models: GGUF files served by a llama.cpp `llama-server` inside the same proot
 * and registered in the OpenCode config as the OpenAI-compatible provider "offline"
 * (the FreeModels pattern). The engine binary is a runtime download, never bundled;
 * model downloads follow the house rules: total size up front, MB + percent progress,
 * byte-level resume with auto-retry, and a clean delete when a download fails for good.
 */
object OfflineModels {
    const val ID = "offline"
    const val BASE_URL = "http://127.0.0.1:8081/v1"

    const val ENGINE_URL = "https://github.com/ggml-org/llama.cpp/releases/download/b12179/llama-b12179-bin-ubuntu-arm64.tar.gz"
    const val ENGINE_SIZE = 18_731_427L
    const val ENGINE_SHA = "900970cce91b11cd13abe9582cf0e7e589e75db07e15b7833afef62bb2430148"

    enum class EngineState { Missing, Downloading, Extracting, Ready, Failed }
    data class DlState(val fraction: Float, val doneBytes: Long, val totalBytes: Long, val failed: Boolean = false)

    var engineState by mutableStateOf(EngineState.Missing)
    var engineProgress by mutableFloatStateOf(0f)
    var engineDetail by mutableStateOf("")
    val dlStates = mutableStateMapOf<String, DlState>()
    /** modelId -> "running" | "failed" while a speed test is going or just failed. */
    val benchStates = mutableStateMapOf<String, String>()
    /** Bumped whenever the on-disk model set changes, so the screen re-reads it. */
    var catalogTick by mutableStateOf(0)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dlJobs = HashMap<String, Job>()
    private var engineJob: Job? = null
    @Volatile private var syncedSig: String? = null

    // ---------- paths ----------

    fun modelsDir(): File = Graph.paths.host("/root/models")
    fun engineDir(): File = Graph.paths.host(RuntimeController.LLAMA_DIR)
    fun engineReady(): Boolean = File(engineDir(), "llama-server").isFile
    private fun guestPath(f: OfflineFile) = "/root/models/${f.filename}"
    private fun hostFile(f: OfflineFile) = File(modelsDir(), f.filename)

    fun totalRamBytes(ctx: Context): Long {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem
    }

    // ---------- downloaded set ----------

    fun isDownloaded(m: OfflineModel): Boolean = m.files.all { f ->
        val hf = hostFile(f)
        hf.isFile && hf.length() == f.sizeBytes
    }

    fun downloadedModels(): List<OfflineModel> = OfflineCatalog.all.filter { isDownloaded(it) }

    /** Bytes already on disk in `.part` files: the point a resume continues from. */
    fun partialBytes(m: OfflineModel): Long = m.files.sumOf { f ->
        val p = File(modelsDir(), f.filename + ".part")
        if (p.isFile) p.length() else 0L
    }

    // ---------- engine ----------

    fun setupEngine() {
        if (engineReady()) { engineState = EngineState.Ready; return }
        if (engineJob?.isActive == true) return
        engineJob = scope.launch {
            engineState = EngineState.Downloading
            engineProgress = 0f
            engineDetail = ""
            val ok = runCatching {
                val tgz = File(Graph.paths.downloads, "llama-b12179-ubuntu-arm64.tar.gz")
                ResumableDownloader.fetch(ENGINE_URL, tgz, ENGINE_SIZE, ENGINE_SHA) { d, t ->
                    engineProgress = if (t > 0) (d.toFloat() / t * 0.8f).coerceIn(0f, 0.8f) else 0f
                    engineDetail = mb(d, t)
                }
                engineState = EngineState.Extracting
                TarExtractor.extract(tgz, Graph.paths.host("/opt")) { d, t ->
                    if (t > 0) engineProgress = 0.8f + 0.2f * (d.toFloat() / t).coerceIn(0f, 1f)
                }
                check(engineReady()) { "llama-server missing after extraction" }
                tgz.delete()
            }.onFailure { runCatching { engineDir().deleteRecursively() } }.isSuccess
            engineState = if (ok) EngineState.Ready else EngineState.Failed
            if (ok) catalogTick++
        }
    }

    private suspend fun engineReadyOrSetup(): Boolean {
        if (engineReady()) return true
        setupEngine()
        engineJob?.join()
        return engineReady()
    }

    // ---------- model downloads ----------

    fun downloadModel(m: OfflineModel) {
        if (dlJobs[m.id]?.isActive == true) return
        if (isDownloaded(m)) return
        dlJobs[m.id] = scope.launch {
            dlStates[m.id] = DlState(0f, partialBytes(m), m.totalBytes)
            val ok = runCatching {
                check(engineReadyOrSetup()) { "engine setup failed" }
                modelsDir().mkdirs()
                var base = 0L
                for (f in m.files) {
                    val b = base
                    ResumableDownloader.fetch(f.url, hostFile(f), f.sizeBytes, f.sha256) { done, _ ->
                        dlStates[m.id] = DlState(
                            ((b + done).toFloat() / m.totalBytes).coerceIn(0f, 1f),
                            b + done, m.totalBytes,
                        )
                    }
                    base += f.sizeBytes
                }
                check(isDownloaded(m)) { "model incomplete after download" }
            }.isSuccess
            if (ok) {
                dlStates.remove(m.id)
                catalogTick++
                syncProvider()
            } else {
                // total failure deletes EVERY file of the model, not just the partial
                // one: a companion file that already finished (the main file of a
                // vision model, gigabytes) must not survive either, so the next try
                // starts from a clean zero
                m.files.forEach { f ->
                    hostFile(f).delete()
                    File(modelsDir(), f.filename + ".part").delete()
                }
                dlStates[m.id] = DlState(0f, 0L, m.totalBytes, failed = true)
                catalogTick++
            }
        }
    }

    fun cancelDownload(m: OfflineModel) {
        dlJobs[m.id]?.cancel()
        dlJobs.remove(m.id)
        dlStates.remove(m.id)
        catalogTick++ // the .part stays, so the button becomes a resume
    }

    fun deleteModel(m: OfflineModel) {
        scope.launch {
            dlJobs[m.id]?.cancel()
            dlJobs.remove(m.id)
            dlStates.remove(m.id)
            val st = Graph.runtime.llamaState.value
            if ((st is LlamaState.Running && st.modelId == m.id) || (st is LlamaState.Starting && st.modelId == m.id)) {
                Graph.runtime.stopLlama()
            }
            m.files.forEach { f ->
                hostFile(f).delete()
                File(modelsDir(), f.filename + ".part").delete()
            }
            catalogTick++
            syncProvider()
        }
    }

    // ---------- provider registration (FreeModels pattern) ----------

    private val configFile get() = Graph.paths.host("/root/.config/opencode/opencode.jsonc")

    private fun disabledList(): List<String> = runCatching {
        val arr = JSONObject(configFile.readText()).optJSONArray("disabled_providers") ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun modelJson(name: String) = JSONObject()
        .put("name", name).put("tool_call", false).put("reasoning", false).put("attachment", false)
        .put("limit", JSONObject().put("context", 4096).put("output", 1024))

    private suspend fun apply(models: List<OfflineModel>?) {
        val disabled = disabledList().filter { it != ID }.toMutableList()
        if (models == null) disabled.add(ID)
        val patch = JSONObject().put("disabled_providers", JSONArray(disabled))
        if (models != null) {
            val modelsJson = JSONObject()
            models.forEach { m -> modelsJson.put(m.id, modelJson("${m.displayName} (Offline)")) }
            patch.put(
                "provider",
                JSONObject().put(
                    ID,
                    JSONObject()
                        .put("name", "Offline")
                        .put("npm", "@ai-sdk/openai-compatible")
                        .put("options", JSONObject().put("baseURL", BASE_URL).put("apiKey", "local"))
                        .put("models", modelsJson),
                ),
            )
        }
        Graph.api.patchGlobalConfig(OcJson.parseToJsonElement(patch.toString()) as JsonObject)
    }

    /** Re-register the provider with exactly the set of models on disk (release-1 reload pattern). */
    suspend fun syncProvider() {
        val models = downloadedModels()
        val sig = models.joinToString(",") { it.id }
        if (sig == syncedSig) return
        runCatching { apply(if (models.isEmpty()) null else models) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        Vault.save()
        syncedSig = sig
    }

    /** Providers screen disconnect: drop the provider from the config, keep the files. */
    suspend fun disable() {
        Graph.runtime.stopLlama()
        runCatching { apply(null) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        Vault.save()
        syncedSig = null
    }

    // ---------- use / chat gate ----------

    fun useModel(m: OfflineModel) {
        val prefs = Graph.prefs
        prefs.providerId = ID
        prefs.modelId = m.id
        prefs.variant = ""
        val key = "$ID/${m.id}"
        prefs.recentModels = (listOf(key) + prefs.recentModels.split(',').filter { it.isNotBlank() && it != key }).take(6).joinToString(",")
        Graph.runtime.startLlama(m.id, guestPath(m.file), m.mmproj?.let { guestPath(it) })
    }

    /**
     * The gate at the top of ChatHub.send: with a cloud model selected the local server
     * has no business running; with an offline model selected the chat must not go out
     * before the server answers healthily with exactly that model loaded.
     */
    suspend fun ensureForSend() {
        val prefs = Graph.prefs
        if (prefs.providerId != ID) {
            Graph.runtime.stopLlama()
            return
        }
        val m = OfflineCatalog.byId(prefs.modelId) ?: throw Exception(SOffline.gateNotDownloaded)
        if (!isDownloaded(m)) throw Exception(SOffline.gateNotDownloaded)
        if (!engineReady()) throw Exception(SOffline.gateEngineMissing)
        val st = Graph.runtime.llamaState.value
        if (st is LlamaState.Running && st.modelId == m.id) return
        Graph.runtime.startLlama(m.id, guestPath(m.file), m.mmproj?.let { guestPath(it) })
        val deadline = System.currentTimeMillis() + 240_000
        while (System.currentTimeMillis() < deadline) {
            val s = Graph.runtime.llamaState.value
            if (s is LlamaState.Running && s.modelId == m.id) return
            if (s is LlamaState.Failed) throw Exception(SOffline.gateServerFailed)
            delay(500)
        }
        throw Exception(SOffline.gateServerSlow)
    }

    // ---------- speed test (llama-bench) ----------

    fun benchResult(m: OfflineModel): Pair<Double, Double>? = runCatching {
        val o = JSONObject(Graph.prefs.offlineBench)
        if (!o.has(m.id)) null else {
            val a = o.getJSONArray(m.id)
            a.getDouble(0) to a.getDouble(1)
        }
    }.getOrNull()

    fun bench(m: OfflineModel) {
        if (benchStates[m.id] == "running") return
        if (!engineReady() || !isDownloaded(m)) return
        benchStates[m.id] = "running"
        scope.launch {
            // the bench loads its own copy of the model: the server must not hold RAM at the same time
            Graph.runtime.stopLlama()
            val remembered = Graph.prefs.llamaBackend
            val v2 = "${RuntimeController.LLAMA_DIR}/libggml-cpu-armv8.2_2.so"
            val v1 = "${RuntimeController.LLAMA_DIR}/libggml-cpu-armv8.2_1.so"
            val candidates: List<Pair<String, Map<String, String>>> = when {
                remembered == "auto" -> listOf("auto" to emptyMap())
                remembered.isNotBlank() -> listOf(remembered to mapOf("GGML_BACKEND_PATH" to remembered))
                else -> listOf(
                    "auto" to emptyMap(),
                    v2 to mapOf("GGML_BACKEND_PATH" to v2),
                    v1 to mapOf("GGML_BACKEND_PATH" to v1),
                )
            }
            var pp: Double? = null
            var tg: Double? = null
            for ((label, env) in candidates) {
                val out = StringBuilder()
                val code = runCatching {
                    Graph.runtime.runGuest(
                        listOf(
                            "${RuntimeController.LLAMA_DIR}/llama-bench",
                            "-m", guestPath(m.file),
                            "-p", "512", "-n", "128",
                            "-t", Graph.runtime.llamaThreads().toString(),
                            "-r", "1",
                        ),
                        timeoutSec = 1200,
                        extraEnv = env,
                    ) { out.appendLine(it) }
                }.getOrDefault(-1)
                val text = out.toString()
                pp = parseBench(text, "pp512")
                tg = parseBench(text, "tg128")
                if (code == 0 && pp != null && tg != null) {
                    Graph.prefs.llamaBackend = label // the server reuses the way that worked
                    break
                }
                pp = null; tg = null
                if (!text.contains("backend", ignoreCase = true)) break // not a backend problem: don't burn time on retries
            }
            if (pp != null && tg != null) {
                val o = runCatching { JSONObject(Graph.prefs.offlineBench) }.getOrDefault(JSONObject())
                o.put(m.id, JSONArray().put(pp).put(tg))
                Graph.prefs.offlineBench = o.toString()
                benchStates.remove(m.id)
            } else {
                benchStates[m.id] = "failed"
            }
            catalogTick++
        }
    }

    private fun parseBench(text: String, test: String): Double? {
        val line = text.lineSequence().firstOrNull { it.contains("| $test |") || it.contains(test) && it.contains("±") } ?: return null
        return Regex("""([0-9]+(?:\.[0-9]+)?)\s*±""").findAll(line).lastOrNull()?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun mb(d: Long, t: Long) = if (t > 0) "%.0f / %.0f MB".format(d / 1e6, t / 1e6) else "%.0f MB".format(d / 1e6)
}
