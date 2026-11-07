package com.cl0de.app.runtime

import com.cl0de.app.data.SOffline
import com.cl0de.app.data.Vault
import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cl0de.app.api.OpenCodeApi
import com.cl0de.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Owns the local server: installs the Ubuntu rootfs and OpenCode, runs
 * `opencode serve` inside proot and keeps the process alive.
 */
class RuntimeController(
    private val app: Application,
    val paths: RuntimePaths,
    private val prefs: Prefs,
    private val api: OpenCodeApi,
) {
    companion object {
        const val ROOTFS_URL = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz"
        const val ROOTFS_SHA = "a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2"
        // Pin a tested version: the client is written against its API.
        const val OC_VERSION = "1.19.4"
        const val OC_SHA = "8568230ccd847dc93e95b9f6894379077d3e014a16b48040743573e77c23e85f"
        const val OC_BIN = "/usr/local/bin/opencode"
        fun ocUrl(v: String) = "https://github.com/anomalyco/opencode/releases/download/v$v/opencode-linux-arm64.tar.gz"
        // Slow-device supervision: a fresh serve on a heavy phone can take minutes to
        // load its providers, so the startup grace is long and the watchdog only ever
        // judges a server that already answered a health check once.
        const val WATCHDOG_INTERVAL_MS = 20_000L
        const val PORT_FREE_WAIT_MS = 90_000L
        // The offline-model server: its own port and its own place inside the rootfs.
        const val LLAMA_PORT = 8081
        const val LLAMA_DIR = "/opt/llama-b12179"
    }

    val state = MutableStateFlow<RuntimeState>(
        if (prefs.runtimeReady && paths.opencodeBin.exists()) RuntimeState.Stopped else RuntimeState.NotInstalled
    )
    val log = MutableStateFlow<List<String>>(emptyList())

    private val proot = ProotCommand(paths, prefs)
    @Volatile private var process: Process? = null
    @Volatile private var stopRequested = false
    @Volatile private var installing = false

    fun logLine(s: String) = log.update { (it + s).takeLast(600) }

    // ---------- install ----------

    suspend fun install() = withContext(Dispatchers.IO) {
        if (installing) return@withContext
        installing = true
        try {
            step("Preparing", null)
            paths.libDir.mkdirs()
            File(paths.nativeDir, "libtalloc.so").copyTo(File(paths.libDir, "libtalloc.so.2"), overwrite = true)

            if (!paths.rootfsReady) {
                val tgz = File(paths.downloads, "ubuntu-base.tar.gz")
                step("Downloading Ubuntu", 0f)
                Downloader.fetch(ROOTFS_URL, tgz, ROOTFS_SHA) { d, t -> step("Downloading Ubuntu", frac(d, t), mb(d, t)) }
                step("Unpacking Ubuntu", 0f)
                paths.rootfs.deleteRecursively()
                val r = TarExtractor.extract(tgz, paths.rootfs) { d, t -> step("Unpacking Ubuntu", frac(d, t)) }
                logLine("rootfs: ${r.files} files, skipped ${r.skipped}")
                configureRootfs()
                tgz.delete()
            }

            Vault.restore()
            installOpencode(OC_VERSION, OC_URL_OR(OC_VERSION), if (OC_VERSION == "1.19.4") OC_SHA else null)
            smokeTest()

            prefs.runtimeReady = true
            state.value = RuntimeState.Stopped
        } catch (e: Throwable) {
            logLine("Install failed: ${e.message}")
            state.value = RuntimeState.InstallFailed(e.message ?: e.javaClass.simpleName)
        } finally {
            installing = false
        }
    }

    private fun OC_URL_OR(v: String) = ocUrl(v)

    /** Update OpenCode to the latest 1.x release (the client is tested on 1.19.4). */
    suspend fun updateOpencode(): String = withContext(Dispatchers.IO) {
        val wasRunning = state.value is RuntimeState.Running
        val latest = Regex("\"version\":\\s*\"([0-9.]+)\"").find(Downloader.getText("https://registry.npmjs.org/opencode-ai/latest"))?.groupValues?.get(1)
            ?: error("Could not determine the latest version")
        if (!latest.startsWith("1.")) error("Latest version $latest is not on the 1.x line; this client was only verified against 1.x")
        if (latest == prefs.opencodeVersion) return@withContext latest
        requestStop(); delay(1500)
        installOpencode(latest, ocUrl(latest), null)
        smokeTest()
        if (wasRunning) startServer()
        latest
    }

    private fun installOpencode(version: String, url: String, sha: String?) {
        val tgz = File(paths.downloads, "opencode-$version.tar.gz")
        step("Downloading OpenCode $version", 0f)
        Downloader.fetch(url, tgz, sha) { d, t -> step("Downloading OpenCode $version", frac(d, t), mb(d, t)) }
        step("Installing OpenCode", 0f)
        val tmp = File(paths.downloads, "oc-extract")
        tmp.deleteRecursively()
        TarExtractor.extract(tgz, tmp) { d, t -> step("Installing OpenCode", frac(d, t)) }
        val bin = tmp.walkTopDown().firstOrNull { it.isFile && it.name == "opencode" } ?: error("No opencode binary in the archive")
        paths.opencodeBin.parentFile?.mkdirs()
        paths.opencodeBin.delete()
        bin.copyTo(paths.opencodeBin, overwrite = true)
        paths.opencodeBin.setExecutable(true, false)
        tmp.deleteRecursively()
        tgz.delete()
        prefs.opencodeVersion = version
    }

    private suspend fun smokeTest() {
        step("Checking startup", null)
        suspend fun once(): Pair<Int, String> {
            val out = StringBuilder()
            val code = runGuest(listOf(OC_BIN, "--version"), timeoutSec = 120) { out.appendLine(it) }
            return code to out.toString()
        }
        var (code, out) = once()
        if (code != 0 || !Regex("\\d+\\.\\d+\\.\\d+").containsMatchIn(out)) {
            logLine("First launch failed (code $code), retrying with PROOT_NO_SECCOMP=1")
            prefs.noSeccomp = true
            val second = once()
            code = second.first; out = second.second
            if (code != 0 || !Regex("\\d+\\.\\d+\\.\\d+").containsMatchIn(out)) {
                prefs.noSeccomp = false
                error("opencode does not start in proot (code $code): ${out.trim().takeLast(400)}")
            }
        }
        logLine("opencode: ${out.trim()}")
    }

    private fun configureRootfs() {
        fun w(guest: String, text: String) {
            val f = paths.host(guest)
            f.parentFile?.mkdirs()
            f.delete()
            f.writeText(text)
        }
        w("/etc/resolv.conf", "nameserver 1.1.1.1\nnameserver 8.8.8.8\n")
        w("/etc/hosts", "127.0.0.1 localhost\n::1 localhost ip6-localhost\n")
        w("/etc/hostname", "opencode-mobile\n")
        // apt inside proot can't drop privileges to the _apt user.
        w("/etc/apt/apt.conf.d/99proot", "APT::Sandbox::User \"root\";\nAcquire::Retries \"3\";\n")
        val pem = app.assets.open("cacert.pem").use { it.readBytes() }
        for (p in listOf("/etc/ssl/certs/ca-certificates.crt", "/etc/ssl/cert.pem")) {
            val f = paths.host(p); f.parentFile?.mkdirs(); f.delete(); f.writeBytes(pem)
        }
        paths.host("/root/work").mkdirs()
        paths.host("/root/.config/opencode").mkdirs()
        paths.host("/tmp").mkdirs()
        File(paths.rootfs, ".oc-configured").writeText("1")
    }

    private fun step(name: String, p: Float?, detail: String = "") {
        state.value = RuntimeState.Installing(name, p, detail)
    }
    private fun frac(d: Long, t: Long) = if (t > 0) (d.toFloat() / t).coerceIn(0f, 1f) else null
    private fun mb(d: Long, t: Long) = if (t > 0) "%.0f / %.0f MB".format(d / 1e6, t / 1e6) else "%.0f MB".format(d / 1e6)

    // ---------- run ----------

    suspend fun supervise() = withContext(Dispatchers.IO) {
        stopRequested = false
        var failures = 0
        var windowStart = System.currentTimeMillis()

        // Never race the runtime install/configuration (rootfs unpacking, apt, the
        // OpenCode install): serve only starts once that phase is over and the rootfs
        // carries the .oc-configured flag. Starting serve in the middle of it was one
        // half of the crash loop seen after an update.
        if (installing || !paths.rootfsReady) {
            logLine("Server start postponed: the runtime setup is still running")
            while (isActive && !stopRequested && (installing || !paths.rootfsReady)) {
                val s = state.value
                if (s is RuntimeState.InstallFailed || (s is RuntimeState.NotInstalled && !installing)) {
                    logLine("Server not started: the runtime setup did not complete")
                    return@withContext
                }
                delay(1000)
            }
            if (stopRequested || !isActive) { state.value = RuntimeState.Stopped; return@withContext }
            logLine("Runtime setup finished: starting the server")
        }

        while (isActive && !stopRequested) {
            // Never spawn on top of a predecessor that still holds the port: the new
            // serve then dies at once with ServeError (exit code 1) and the supervisor
            // restarts it into the same wall, over and over.
            val port = prefs.serverPort.toIntOrNull() ?: 4096
            if (!awaitPortFree(port)) {
                if (stopRequested) break
                failures++
                state.value = RuntimeState.Crashed("Port $port is still in use. Restarting…")
                delay((1500L * failures).coerceAtMost(30_000))
                continue
            }
            state.value = RuntimeState.Starting
            val cmd = proot.build(
                listOf(OC_BIN, "serve", "--hostname", "127.0.0.1", "--port", prefs.serverPort),
                cwd = "/root/work",
                extraEnv = mapOf(
                    "OPENCODE_SERVER_PASSWORD" to prefs.serverPassword,
                    "OPENCODE_DISABLE_AUTOUPDATE" to "true",
                ),
            )
            val pb = ProcessBuilder(cmd).redirectErrorStream(true)
            pb.environment().putAll(proot.env())
            val spawnedAt = System.currentTimeMillis()
            val p = try { pb.start() } catch (e: Exception) {
                logLine("Could not start proot: ${e.message}")
                failures++
                state.value = RuntimeState.Crashed("Could not start proot: ${e.message}")
                delay((1500L * failures).coerceAtMost(30_000))
                continue
            }
            process = p
            logLine("Server process started on port $port, waiting for it to answer…")
            val reader = launch { runCatching { p.inputStream.bufferedReader().forEachLine { logLine(it) } } }
            var version: String? = null
            var dog: Job? = null
            try {
                // Startup grace: poll quietly (the old 250 ms loop flooded the log with
                // one line per failed knock and pushed the real messages out). A failure
                // here never counts and never restarts anything: a server that is still
                // loading its providers is not a hung server.
                var firstErr = true
                var lastNote = 0L
                while (isActive && p.isAlive && version == null) {
                    version = try { api.healthVersionOrThrow() } catch (e: Exception) {
                        if (firstErr) { firstErr = false; logLine("Server is loading (health not answering yet: ${e.javaClass.simpleName})") }
                        null
                    }
                    if (version == null) {
                        val elapsed = System.currentTimeMillis() - spawnedAt
                        if (elapsed - lastNote >= 30_000) { lastNote = elapsed; logLine("Still starting… ${elapsed / 1000}s elapsed") }
                        delay(1000)
                    }
                }
                if (version != null) {
                    state.value = RuntimeState.Running(version)
                    prefs.opencodeVersion = version
                    failures = 0
                    windowStart = System.currentTimeMillis()
                    logLine("Server is up: OpenCode $version on port $port (took ${(System.currentTimeMillis() - spawnedAt) / 1000}s to answer)")
                    // Watchdog: only judges a server that already answered once, with a
                    // patient per-check timeout, so a busy-but-alive server on a slow
                    // phone is not mistaken for a hung one.
                    dog = launch {
                        var bad = 0
                        while (isActive && p.isAlive) {
                            delay(WATCHDOG_INTERVAL_MS)
                            val err = try { if (api.healthVersionPatient() != null) null else "health answered but not healthy" }
                                catch (e: Exception) { "${e.javaClass.simpleName}: ${e.message}" }
                            if (err == null) {
                                bad = 0
                            } else {
                                bad++
                                logLine("Watchdog: health check failed ($bad/3): $err")
                                if (bad >= 3) { logLine("Watchdog: server stopped answering — restarting it"); p.destroy(); break }
                            }
                        }
                    }
                }
                runInterruptible { p.waitFor() }
            } finally {
                dog?.cancel()
                p.destroy()
                if (p.isAlive) { runCatching { p.waitFor(3, TimeUnit.SECONDS) }; if (p.isAlive) p.destroyForcibly() }
                process = null
                reader.cancel()
            }
            if (stopRequested) break
            val code = runCatching { p.exitValue() }.getOrDefault(-1)
            val now = System.currentTimeMillis()
            if (now - windowStart > 120_000) { failures = 0; windowStart = now }
            failures++
            val restartIn = (1500L * failures).coerceAtMost(30_000)
            logLine(
                "Server exited (code $code) after ${(now - spawnedAt) / 1000}s" +
                    (if (version == null) ", before it ever answered a health check" else "") +
                    " — restarting in ${restartIn / 1000}s"
            )
            state.value = RuntimeState.Crashed("Server stopped (code $code). Restarting…")
            // don't give up: restart with a growing pause (up to 30 s) until the user stops the server
            delay(restartIn)
        }
        state.value = RuntimeState.Stopped
    }

    /** True once nothing listens on [port] anymore; waits a bounded while for a dying predecessor to release it. */
    private suspend fun awaitPortFree(port: Int): Boolean {
        fun free(): Boolean = try {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("127.0.0.1", port)); close() }
            true
        } catch (e: Exception) { false }
        if (free()) return true
        logLine("Port $port is still held by the previous server — waiting for it to free up")
        val deadline = System.currentTimeMillis() + PORT_FREE_WAIT_MS
        while (!stopRequested && System.currentTimeMillis() < deadline) {
            delay(1000)
            if (free()) { logLine("Port $port is free again"); return true }
        }
        if (!stopRequested) logLine("Port $port is still busy after ${PORT_FREE_WAIT_MS / 1000}s — not starting on top of it")
        return !stopRequested && free()
    }

    /** The user stopped the server: then we don't start it when they come back to the app. */
    @Volatile var userStopped = false

    /** The server process is still alive (after stopServer it doesn't exit right away). */
    val serverAlive get() = process != null

    fun requestStop() {
        stopRequested = true
        process?.destroy()
    }

    /** Start the server through a foreground service, otherwise Android kills the process in the background. */
    fun startServer() {
        val s = state.value
        if (s is RuntimeState.NotInstalled || s is RuntimeState.Installing || s is RuntimeState.InstallFailed) return
        userStopped = false
        ContextCompat.startForegroundService(app, Intent(app, ServerService::class.java).setAction(ServerService.ACTION_START))
    }

    fun stopServer() {
        userStopped = true
        requestStop()
        app.stopService(Intent(app, ServerService::class.java))
        if (state.value !is RuntimeState.NotInstalled) state.value = RuntimeState.Stopped
    }

    /** Run a command inside Ubuntu (installing git and so on). */
    suspend fun runGuest(
        argv: List<String>,
        cwd: String = "/root",
        timeoutSec: Long = 900,
        extraEnv: Map<String, String> = emptyMap(),
        onLine: (String) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val pb = ProcessBuilder(proot.build(argv, cwd, extraEnv)).redirectErrorStream(true)
        pb.environment().putAll(proot.env())
        val p = pb.start()
        val rd = launch { runCatching { p.inputStream.bufferedReader().forEachLine { onLine(it); logLine(it) } } }
        val done = try { runInterruptible { p.waitFor(timeoutSec, TimeUnit.SECONDS) } } finally { if (p.isAlive) p.destroyForcibly() }
        if (!done) { rd.cancel(); return@withContext -1 }
        rd.join()
        p.exitValue()
    }

    /** git, ripgrep and the like: without them the agent in Ubuntu is nearly useless. */
    suspend fun installDevTools(onLine: (String) -> Unit): Int {
        val script = "apt-get update && apt-get install -y --no-install-recommends git ripgrep curl ca-certificates openssh-client unzip less nano python3 build-essential"
        return runGuest(listOf("/bin/bash", "-lc", script), onLine = onLine)
    }

    // ---------- llama-server (offline models) ----------
    // A sibling supervision next to supervise(): its own process, port, stop flag and
    // state, so nothing here can disturb the OpenCode server. The same patient rules
    // apply: no spawn on a busy port, a quiet startup grace with no deadline, and a
    // watchdog that only judges a server that already answered once.

    val llamaState = MutableStateFlow<LlamaState>(LlamaState.Stopped)

    @Volatile private var llamaProcess: Process? = null
    @Volatile private var llamaStopRequested = false
    @Volatile private var llamaWantedModel: String? = null
    private var llamaJob: Job? = null
    private val llamaScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val llamaHttp = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build()

    /** 80% of the cores, at least 4, at most 8 (the value the Tahap 0 device test used). */
    fun llamaThreads(): Int = (Runtime.getRuntime().availableProcessors() * 4 / 5).coerceIn(4, 8)

    private fun llamaLog(s: String) = logLine("[llama] $s")

    /** llama-server answers /health with 503 while the model loads and 200 once it serves. */
    private fun llamaHealthy(callTimeoutSec: Long): Boolean = try {
        val c = llamaHttp.newBuilder().callTimeout(callTimeoutSec, TimeUnit.SECONDS).build()
        c.newCall(Request.Builder().url("http://127.0.0.1:$LLAMA_PORT/health").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }

    fun startLlama(modelId: String, guestModel: String, guestMmproj: String?) {
        val cur = llamaState.value
        if (llamaWantedModel == modelId && (cur is LlamaState.Starting || cur is LlamaState.Running)) return
        llamaJob?.cancel(); llamaJob = null
        llamaProcess?.destroy()
        llamaStopRequested = false
        llamaWantedModel = modelId
        llamaState.value = LlamaState.Starting(modelId)
        llamaJob = llamaScope.launch { superviseLlama(modelId, guestModel, guestMmproj) }
    }

    fun stopLlama() {
        llamaStopRequested = true
        llamaWantedModel = null
        llamaJob?.cancel(); llamaJob = null
        llamaProcess?.destroy()
        if (llamaState.value !is LlamaState.Stopped) llamaState.value = LlamaState.Stopped
    }

    private suspend fun superviseLlama(modelId: String, guestModel: String, guestMmproj: String?) = withContext(Dispatchers.IO) {
        // the same gate as the OpenCode supervisor: never race the runtime setup
        while (isActive && !llamaStopRequested && (installing || !paths.rootfsReady)) {
            if (state.value is RuntimeState.InstallFailed || (state.value is RuntimeState.NotInstalled && !installing)) {
                llamaState.value = LlamaState.Failed(SOffline.failRuntimeNotReady)
                return@withContext
            }
            delay(1000)
        }
        if (llamaStopRequested || !isActive) return@withContext

        // How ggml finds its CPU backend: automatic discovery first, then the explicit
        // armv8.2 variants. The way that already worked on this device (remembered) is
        // tried first, but the chain is always complete: a remembered way that starts
        // failing falls through to the others, it never narrows the chain to itself.
        // The 8.6/9.2 variants are never tried: phones in this class don't have those
        // instruction sets.
        val v2 = "$LLAMA_DIR/libggml-cpu-armv8.2_2.so"
        val v1 = "$LLAMA_DIR/libggml-cpu-armv8.2_1.so"
        val fullChain: List<Pair<String, Map<String, String>>> = listOf(
            "auto" to emptyMap(),
            v2 to mapOf("GGML_BACKEND_PATH" to v2),
            v1 to mapOf("GGML_BACKEND_PATH" to v1),
        )
        val remembered = prefs.llamaBackend
        val candidates: List<Pair<String, Map<String, String>>> =
            if (remembered.isBlank() || remembered == "auto") fullChain
            else fullChain.filter { it.first == remembered } + fullChain.filter { it.first != remembered }
        var failures = 0
        // one display name per backend way, shared by every line that mentions it
        fun backendName(label: String): String = if (label == "auto") "automatic discovery" else label
        // ONE atomic line per death, written only once the decision is known (the
        // OpenCode supervisor's shape): exit code, lifetime, whether it ever answered
        // a health check, the model and backend that died, and what happens next
        fun deathLine(code: Int, aliveSec: Long, wasHealthy: Boolean, backend: String, nextAction: String): String =
            "server exited (code $code) after ${aliveSec}s" +
                (if (wasHealthy) ", after it had answered a health check" else ", before it ever answered a health check") +
                ", model: $modelId, backend: $backend — $nextAction"
        while (isActive && !llamaStopRequested) {
            if (!awaitLlamaPortFree()) {
                if (llamaStopRequested) break
                failures++
                llamaLog("port $LLAMA_PORT stayed busy, retrying shortly")
                delay((1500L * failures).coerceAtMost(30_000))
                continue
            }
            var served = false
            // the last death of this round: recorded where the process dies, logged
            // once, where the decision is made (next backend / giving up / restart)
            var lastExitCode: Int? = null
            var lastAliveSec = 0L
            var lastHealthy = false
            var lastBackend = ""
            chain@ for ((index, cand) in candidates.withIndex()) {
                val (label, env) = cand
                if (llamaStopRequested || !isActive) break@chain
                llamaState.value = LlamaState.Starting(modelId)
                val argv = buildList {
                    add("$LLAMA_DIR/llama-server")
                    add("--host"); add("127.0.0.1"); add("--port"); add(LLAMA_PORT.toString())
                    add("-m"); add(guestModel)
                    add("-c"); add("4096")
                    add("-t"); add(llamaThreads().toString())
                    if (guestMmproj != null) { add("--mmproj"); add(guestMmproj) }
                }
                val pb = ProcessBuilder(proot.build(argv, cwd = "/root", extraEnv = env)).redirectErrorStream(true)
                pb.environment().putAll(proot.env())
                val spawnedAt = System.currentTimeMillis()
                val p = try { pb.start() } catch (e: Exception) {
                    llamaLog("could not start proot: ${e.message}")
                    break@chain
                }
                llamaProcess = p
                llamaLog("server process started for $modelId (backend: ${backendName(label)}), waiting for it to answer…")
                val recent = java.util.Collections.synchronizedList(mutableListOf<String>())
                val reader = launch { runCatching { p.inputStream.bufferedReader().forEachLine { l -> recent.add(l); if (recent.size > 200) recent.removeAt(0); llamaLog(l) } } }
                var healthy = false
                var dog: Job? = null
                try {
                    while (isActive && !llamaStopRequested && p.isAlive && !healthy) {
                        healthy = llamaHealthy(2)
                        if (!healthy) delay(1000)
                    }
                    if (healthy) {
                        served = true
                        prefs.llamaBackend = label
                        llamaState.value = LlamaState.Running(modelId)
                        failures = 0
                        llamaLog("server is up: $modelId on port $LLAMA_PORT (took ${(System.currentTimeMillis() - spawnedAt) / 1000}s, backend: ${backendName(label)})")
                        dog = launch {
                            var bad = 0
                            while (isActive && !llamaStopRequested && p.isAlive) {
                                delay(WATCHDOG_INTERVAL_MS)
                                if (llamaHealthy(15)) bad = 0 else {
                                    bad++
                                    llamaLog("watchdog: health check failed ($bad/3)")
                                    if (bad >= 3) { llamaLog("watchdog: server stopped answering, restarting it"); p.destroy(); break }
                                }
                            }
                        }
                        runInterruptible { p.waitFor() }
                    }
                } finally {
                    dog?.cancel()
                    p.destroy()
                    if (p.isAlive) { runCatching { p.waitFor(3, TimeUnit.SECONDS) }; if (p.isAlive) p.destroyForcibly() }
                    if (llamaProcess == p) llamaProcess = null
                    // drain the reader to EOF before the buffer is inspected below: a
                    // process that dies fast can otherwise lose its last lines, and with
                    // them the "no backends are loaded" that moves the chain along
                    withTimeoutOrNull(2_000) { reader.join() }
                    reader.cancel()
                }
                if (llamaStopRequested || !isActive) break@chain
                // record the death now; its one atomic line is written where the
                // decision is made below (or by the outer restart), never in fragments
                lastExitCode = runCatching { p.exitValue() }.getOrDefault(-1)
                lastAliveSec = (System.currentTimeMillis() - spawnedAt) / 1000
                lastHealthy = healthy
                lastBackend = backendName(label)
                if (served) break@chain // it ran and ended: the outer loop restarts it whole and logs the death there
                val noBackend = synchronized(recent) { recent.any { it.contains("no backends are loaded") } }
                if (noBackend && index < candidates.lastIndex) {
                    llamaLog(deathLine(lastExitCode!!, lastAliveSec, lastHealthy, lastBackend, "trying next backend: ${backendName(candidates[index + 1].first)}"))
                    lastExitCode = null
                    continue@chain
                }
                if (noBackend) {
                    llamaLog(deathLine(lastExitCode!!, lastAliveSec, lastHealthy, lastBackend, "giving up"))
                    lastExitCode = null
                    llamaState.value = LlamaState.Failed(SOffline.failNoBackend)
                }
                break@chain
            }
            if (llamaStopRequested || !isActive) break
            if (llamaState.value is LlamaState.Failed) break
            failures++
            val restartIn = (1500L * failures).coerceAtMost(30_000)
            val death = lastExitCode
            if (death != null) {
                // a death that reached the outer loop (it served, or died for a
                // non-backend reason) gets its atomic line here, with the restart
                llamaLog(deathLine(death, lastAliveSec, lastHealthy, lastBackend, "restarting in ${restartIn / 1000}s (failure $failures)"))
                lastExitCode = null
            } else {
                llamaLog("restarting in ${restartIn / 1000}s")
            }
            llamaState.value = LlamaState.Starting(modelId)
            delay(restartIn)
        }
        if (llamaState.value !is LlamaState.Failed) llamaState.value = LlamaState.Stopped
    }

    /** The llama twin of awaitPortFree, watching the llama stop flag instead. */
    private suspend fun awaitLlamaPortFree(): Boolean {
        fun free(): Boolean = try {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("127.0.0.1", LLAMA_PORT)); close() }
            true
        } catch (e: Exception) { false }
        if (free()) return true
        llamaLog("port $LLAMA_PORT is still held by the previous server, waiting for it to free up")
        val deadline = System.currentTimeMillis() + PORT_FREE_WAIT_MS
        while (!llamaStopRequested && System.currentTimeMillis() < deadline) {
            delay(1000)
            if (free()) { llamaLog("port $LLAMA_PORT is free again"); return true }
        }
        return !llamaStopRequested && free()
    }

    fun resetRuntime() {
        Vault.save()
        stopServer()
        paths.rootfs.deleteRecursively()
        paths.downloads.deleteRecursively()
        prefs.runtimeReady = false
        state.value = RuntimeState.NotInstalled
    }
}
