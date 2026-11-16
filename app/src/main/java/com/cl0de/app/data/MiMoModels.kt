package com.cl0de.app.data

import com.cl0de.app.Graph
import com.cl0de.app.api.OcJson
import com.cl0de.app.runtime.MiMoState
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject

/**
 * The MiMo provider: chat models of Xiaomi MiMo Studio, registered in the
 * OpenCode config as the OpenAI-compatible provider "mimo" pointing at the
 * local bridge (a Node.js process in the same proot, supervised by
 * RuntimeController, wrapping the bundled MiMo module untouched).
 *
 * Registration follows the Deep template (a static catalog written through
 * the server). MiMo is chat only: the module has no tool calling and no
 * image path at all (its multiMedias is always empty), so every model is
 * registered tool_call=false with text-only modalities and attachment=false,
 * and the provider carries its own light agent (see [agentJson]); the send
 * gate enforces that agent from the active model.
 *
 * Two deliberate departures from the Deep template:
 * - No default seeding. A fresh install's default stays the Deep model;
 *   nothing here touches the seed latch, so MiMo is simply available.
 * - Disable is reversible. Deep's disabled flag is a one-way latch (nothing
 *   in the app ever clears it); MiMo has [enable] next to [disable], wired
 *   to the Providers screen, so a disconnect can be undone.
 *
 * The session cookie is NOT part of registration: it ships inside the APK
 * assets (injected at CI build time from a repo secret) and the bridge
 * stages it next to the module, whose own loader picks it up per call.
 */
object MiMoModels {
    const val ID = "mimo"
    /** Name of the light agent registered next to the provider; MiMo sessions run as it. */
    const val AGENT = "mimo"
    const val BASE_URL = "http://127.0.0.1:8182/v1"

    data class MiMoModel(val id: String, val displayName: String, val context: Int, val suffix: String = " (Chat)")

    /**
     * The catalog is the module's own AVAILABLE_MODELS, verified live in
     * the spike (16 Nov 2026): all four answered, with usage and reasoning.
     *
     * ASSUMPTION, unverified: the context figures. The spike only measured
     * the server-side prompt (~3,815 tokens, mostly cached); the service
     * publishes no context window the spike could read. 128000 for all
     * four is a conservative placeholder for the picker's display and
     * OpenCode's compaction bookkeeping, not a measured limit.
     */
    val catalog: List<MiMoModel> = listOf(
        MiMoModel("mimo-v2.6-pro", "MiMo V2.6 Pro", 128000),
        MiMoModel("mimo-v2.6-flash", "MiMo V2.6 Flash", 128000),
        MiMoModel("mimo-v2.5-pro", "MiMo V2.5 Pro", 128000),
        MiMoModel("mimo-v2.5", "MiMo V2.5", 128000),
    )

    fun byId(id: String): MiMoModel? = catalog.firstOrNull { it.id == id }

    // ---------- provider registration (Deep template) ----------

    private val configFile get() = Graph.paths.host("/root/.config/opencode/opencode.jsonc")

    fun isEnabled(): Boolean = runCatching {
        JSONObject(configFile.readText()).optJSONObject("provider")?.has(ID) == true
    }.getOrDefault(false)

    private fun disabledList(): List<String> = runCatching {
        val arr = JSONObject(configFile.readText()).optJSONArray("disabled_providers") ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    /**
     * The "(Chat)" suffix is the picker's label for this provider's nature, exactly like "(Offline)".
     *
     * reasoning is true, departing from Deep's false: MiMo provably returns
     * a separate reasoning stream (the spike measured reasoning tokens on
     * every call, thinking flag or not) and the bridge maps it to
     * reasoning_content, so declaring the capability is what lets the
     * client show it. attachment stays false and modalities text-only:
     * unlike Deep there is no bridge-side relay to rescue an image for a
     * text model, the module simply has no image input.
     */
    private fun modelJson(m: MiMoModel) = JSONObject()
        .put("name", "${m.displayName}${m.suffix}")
        .put("tool_call", false).put("reasoning", true).put("attachment", false)
        .put(
            "modalities",
            JSONObject()
                .put("input", JSONArray().put("text"))
                .put("output", JSONArray().put("text")),
        )
        .put("limit", JSONObject().put("context", m.context).put("output", 4096))

    /**
     * MiMo's own light agent, registered and disabled together with the
     * provider. It must not borrow Deep's or offline's agent: their
     * registrations live and die with their own providers.
     */
    private fun agentJson() = JSONObject()
        .put("mode", "primary")
        .put("description", SMiMo.agentDescription)
        .put("prompt", SMiMo.agentPrompt)
        .put("tools", JSONObject().put("*", false))

    /** Applied through the server: it writes the global config and picks it up at once (DeepModels.apply's shape). */
    private suspend fun apply(block: Boolean) {
        // "mimo" is filtered out of the disabled list on the same patch: an
        // imported config can carry it there, which would hide the provider
        // silently. Only an explicit disconnect (disable() below, which sets
        // the prefs flag the sync checks) keeps MiMo off.
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
                        .put("name", "MiMo")
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
     * Register the provider (every install, new and old). Called whenever
     * the local server reaches Running, so a registration that lost a race
     * with a still starting server is retried instead of lost. Never
     * touches a remote device's server: the bridge only exists locally.
     * Unlike Deep there is NO default seeding here: the fresh-install
     * default stays Deep's model.
     */
    suspend fun syncProvider() {
        if (synced) return
        if (Graph.devices.isRemote) return
        if (Graph.prefs.mimoDisabled) return
        runCatching { apply(true) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        // refresh() swallows its own failures, and every older caller discards
        // the patch result: only a MiMo model actually sitting in the catalog
        // proves the registration landed. Anything else leaves `synced` false.
        if (Graph.catalog.model(ID, catalog.first().id) == null) return
        Vault.save()
        synced = true
    }

    /** Providers screen disconnect: drop the provider via the disabled list (deep merge never removes keys), with the full reload pattern. */
    suspend fun disable() {
        Graph.prefs.mimoDisabled = true
        runCatching { apply(false) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        Vault.save()
        synced = false
    }

    /**
     * The way back from [disable]: clear the flag first, then register
     * again through the same patch. Deep has no equivalent (its flag is a
     * one-way latch); MiMo's disconnect is meant to be undoable from the
     * Providers screen's add list.
     */
    suspend fun enable() {
        Graph.prefs.mimoDisabled = false
        runCatching { apply(true) }
        runCatching { Graph.api.disposeAll() }
        runCatching { Graph.catalog.refresh() }
        Vault.save()
        synced = Graph.catalog.model(ID, catalog.first().id) != null
    }

    // ---------- use / chat gate ----------

    /** MiMo's light agent takes the session over (the generalized light-agent set lives in OfflineModels). */
    fun activateAgent() = OfflineModels.activateLightAgent(AGENT)

    /**
     * The MiMo branch of the send gate (OfflineModels.ensureForSend routes
     * here with the send's snapshot; a remote device never reaches this, it
     * takes the cloud branch): the light agent is enforced before anything,
     * then the bridge must answer healthily before anything is sent. Health
     * is checked on every send, never cached as "was ready once": the
     * process can have died since the last send, and a restarted bridge
     * means the server-side conversations it held ids for are still valid
     * but its map is empty (the bridge's defined map-miss path covers it).
     * Starting is single-flight in RuntimeController (its job guard), so
     * concurrent sends all wait on the same supervisor state here instead
     * of spawning rivals.
     */
    suspend fun ensureForSend(modelId: String) {
        activateAgent()
        Graph.runtime.stopLlama()
        byId(modelId) ?: throw Exception(SMiMo.gateModelGone)
        if (Graph.runtime.mimoHealthy(2)) return
        Graph.runtime.startMiMo()
        val deadline = System.currentTimeMillis() + 300_000
        while (System.currentTimeMillis() < deadline) {
            when (Graph.runtime.mimoState.value) {
                is MiMoState.Running -> return
                is MiMoState.Failed -> throw Exception(SMiMo.gateServerFailed)
                else -> delay(500)
            }
        }
        throw Exception(SMiMo.gateServerSlow)
    }
}
