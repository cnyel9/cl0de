package com.cl0de.app.data

import com.cl0de.app.Graph
import com.cl0de.app.api.OcJson
import com.cl0de.app.runtime.DeepState
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Deep provider: chat models of the DeepChat service, registered in the
 * OpenCode config as the OpenAI-compatible provider "deep" pointing at the
 * local bridge (a Node.js process in the same proot, supervised by
 * RuntimeController, wrapping the bundled DeepChat module untouched).
 *
 * Registration follows the FreeModels template (a static catalog written
 * through the server), not the offline one (a set derived from disk). Deep is
 * chat only: its API has no tool calling, so every model is registered
 * tool_call=false and the provider carries its own light agent (see
 * [agentJson]); the send gate enforces that agent from the active model.
 */
object DeepModels {
    const val ID = "deep"
    /** Name of the light agent registered next to the provider; Deep sessions run as it. */
    const val AGENT = "deep"
    const val BASE_URL = "http://127.0.0.1:8181/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    data class DeepModel(val id: String, val providerId: String, val displayName: String, val context: Int, val image: Boolean)

    /**
     * The chat catalog, filtered from the service's own getConfig() snapshot
     * (8 Nov 2026): capabilities.text_chat == true AND enabled == true.
     * deepseek-reasoner and sonar-deep-research are text models the catalog
     * disables, so they are NOT here (the gist README advertises the former
     * anyway). creditCost from the catalog is deliberately not carried: the
     * spike measured actual deductions differing from it.
     */
    val catalog: List<DeepModel> = listOf(
        DeepModel("gpt-4o-mini", "chatgpt", "GPT-4o Mini", 128000, true),
        DeepModel("gpt-4.1", "chatgpt", "GPT-4.1", 1000000, true),
        DeepModel("claude-haiku-4-5-20251001", "claude", "Claude Haiku 4.5", 200000, true),
        DeepModel("claude-opus-4-7", "claude", "Claude Opus 4.7", 1000000, true),
        DeepModel("gemini-2.5-flash-lite", "gemini", "Gemini 2.5 Flash Lite", 1000000, false),
        DeepModel("gemini-2.5-pro", "gemini", "Gemini 2.5 Pro", 1000000, true),
        DeepModel("deepseek-v4-flash", "deepseek", "DeepSeek V4 Flash", 128000, false),
        DeepModel("deepseek-chat", "deepseek", "DeepSeek V3", 128000, false),
        DeepModel("kimi-k3", "kimi", "Kimi K3", 1048576, true),
        DeepModel("kimi-k2.6", "kimi", "Kimi K2.6", 256000, false),
        DeepModel("grok-4-1-fast", "grok", "Grok 4.1 Fast", 2000000, false),
        DeepModel("grok-4", "grok", "Grok 4", 131072, false),
        DeepModel("ministral-8b-latest", "mistral", "Mistral Small", 128000, false),
        DeepModel("mistral-large-latest", "mistral", "Mistral Large 3", 128000, false),
        DeepModel("sonar-pro", "perplexity", "Sonar Pro", 200000, false),
        DeepModel("sonar", "perplexity", "Sonar", 200000, false),
    )

    fun byId(id: String): DeepModel? = catalog.firstOrNull { it.id == id }

    // ---------- provider registration (FreeModels pattern) ----------

    private val configFile get() = Graph.paths.host("/root/.config/opencode/opencode.jsonc")

    fun isEnabled(): Boolean = runCatching {
        JSONObject(configFile.readText()).optJSONObject("provider")?.has(ID) == true
    }.getOrDefault(false)

    private fun disabledList(): List<String> = runCatching {
        val arr = JSONObject(configFile.readText()).optJSONArray("disabled_providers") ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    /** The "(Chat)" suffix is the picker's label for this provider's nature, exactly like "(Offline)". */
    private fun modelJson(m: DeepModel) = JSONObject()
        .put("name", "${m.displayName} (Chat)")
        .put("tool_call", false).put("reasoning", false).put("attachment", m.image)
        .put("limit", JSONObject().put("context", m.context).put("output", 4096))

    /**
     * Deep's own light agent, registered and disabled together with the
     * provider. It must not borrow offline's agent: that one's registration
     * lives and dies with OfflineModels.apply() and can sit disabled for a
     * user who never downloaded an offline model.
     */
    private fun agentJson() = JSONObject()
        .put("mode", "primary")
        .put("description", SDeep.agentDescription)
        .put("prompt", SDeep.agentPrompt)
        .put("tools", JSONObject().put("*", false))

    /** Applied through the server: it writes the global config and picks it up at once (FreeModels.apply's shape). */
    private suspend fun apply(block: Boolean) {
        // "deep" is filtered out of the disabled list on the same patch: an
        // imported config can carry it there, which would hide the provider
        // silently. Only an explicit disconnect (disable() below, which sets
        // the prefs flag the sync checks) keeps Deep off.
        val disabled = disabledList().filter { it != ID }.toMutableList()
        val patch = JSONObject()
        if (block) {
            val modelsJson = JSONObject()
            catalog.forEach { m -> modelsJson.put(m.id, modelJson(m)) }
            patch.put(
                "provider",
                JSONObject().put(
                    ID,
                    JSONObject()
                        .put("name", "Deep")
                        .put("npm", "@ai-sdk/openai-compatible")
                        .put("options", JSONObject().put("baseURL", BASE_URL).put("apiKey", "local"))
                        .put("models", modelsJson),
                ),
            )
            patch.put("agent", JSONObject().put(AGENT, agentJson()))
        } else {
            disabled.add(ID)
            patch.put("agent", JSONObject().put(AGENT, JSONObject().put("disable", true)))
        }
        patch.put("disabled_providers", JSONArray(disabled))
        Graph.api.patchGlobalConfig(OcJson.parseToJsonElement(patch.toString()) as JsonObject)
    }

    /** Set only after a registration that refresh proved visible; a swallowed failure retries on the next server Running. */
    @Volatile private var synced = false

    /**
     * Register the provider (every install, new and old) and, only for a
     * latched fresh install, seed the default model. Called whenever the local
     * server reaches Running, so a registration that lost a race with a still
     * starting server is retried instead of lost. Never touches a remote
     * device's server: the bridge only exists locally.
     */
    suspend fun syncProvider() {
        if (synced) return
        if (Graph.devices.isRemote) return
        if (Graph.prefs.deepDisabled) return
        runCatching { apply(true) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        // refresh() swallows its own failures, and every older caller discards
        // the patch result: only the default model actually sitting in the
        // catalog proves the registration landed. Anything else leaves
        // `synced` false and the seed pending.
        if (Graph.catalog.model(ID, DEFAULT_MODEL) == null) return
        val prefs = Graph.prefs
        if (prefs.deepSeedPending && !prefs.deepSeedDone) {
            // finish() order: the provider is visible first, the prefs are
            // written last, so Catalog.ensureSelection can no longer replace
            // the default with a higher-ranked fallback
            prefs.providerId = ID
            prefs.modelId = DEFAULT_MODEL
            prefs.variant = ""
            prefs.deepSeedDone = true
            prefs.deepSeedPending = false
        }
        Vault.save()
        synced = true
    }

    /** Providers screen disconnect: drop the provider via the disabled list (deep merge never removes keys), with the full reload pattern. */
    suspend fun disable() {
        Graph.prefs.deepDisabled = true
        runCatching { apply(false) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        Vault.save()
        synced = false
    }

    // ---------- use / chat gate ----------

    /** Deep's light agent takes the session over (the generalized light-agent set lives in OfflineModels). */
    fun activateAgent() = OfflineModels.activateLightAgent(AGENT)

    /**
     * The Deep branch of the send gate (OfflineModels.ensureForSend routes
     * here with the send's snapshot; a remote device never reaches this, it
     * takes the cloud branch): the light agent is enforced before anything,
     * then the bridge must answer healthily before anything is sent. Health
     * is checked on every send, never cached as "was ready once": the process
     * can have died since the last send, and a restarted bridge means a new
     * DeepChat session anyway. Starting is single-flight in RuntimeController
     * (its job guard), so concurrent sends (the user's and a scheduled one)
     * all wait on the same supervisor state here instead of spawning rivals.
     */
    suspend fun ensureForSend(modelId: String) {
        activateAgent()
        Graph.runtime.stopLlama()
        byId(modelId) ?: throw Exception(SDeep.gateModelGone)
        if (Graph.runtime.deepHealthy(2)) return
        Graph.runtime.startDeep()
        val deadline = System.currentTimeMillis() + 300_000
        while (System.currentTimeMillis() < deadline) {
            when (Graph.runtime.deepState.value) {
                is DeepState.Running -> return
                is DeepState.Failed -> throw Exception(SDeep.gateServerFailed)
                else -> delay(500)
            }
        }
        throw Exception(SDeep.gateServerSlow)
    }
}
