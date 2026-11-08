package com.cl0de.app.data

import com.cl0de.app.Graph
import com.cl0de.app.api.AgentInfo
import com.cl0de.app.api.EventHub
import com.cl0de.app.api.ModelInfo
import com.cl0de.app.api.OpenCodeApi
import com.cl0de.app.api.ProviderInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Connected providers, their models and agents: what model selection needs. */
class Catalog(private val api: OpenCodeApi, private val prefs: Prefs, private val events: EventHub) {
    val providers = MutableStateFlow<List<ProviderInfo>>(emptyList())
    val agents = MutableStateFlow<List<AgentInfo>>(emptyList())
    /** Commands and skills for the "/" menu in the input. */
    val commands = MutableStateFlow<List<com.cl0de.app.api.NamedItem>>(emptyList())
    val loaded = MutableStateFlow(false)
    private var defaults: Map<String, String> = emptyMap()

    fun start() {
        Graph.scope.launch { events.generation.collect { if (it > 0) refresh() } }
    }

    suspend fun refresh() {
        runCatching {
            val r = api.connectedProviders()
            providers.value = r.providers.sortedWith(compareBy({ rank(it.id) }, { it.name }))
            defaults = r.default
            loaded.value = true
        }
        runCatching { agents.value = api.agents().filter { !it.hidden && it.mode != "subagent" } }
        runCatching { commands.value = api.commands().sortedWith(compareBy({ if (it.source == "skill") 0 else 1 }, { it.name })) }
        ensureSelection()
    }

    private fun rank(id: String) = when (id) { "deep" -> 0; "opencode-go" -> 1; "opencode" -> 2; else -> 5 }

    fun model(providerId: String, modelId: String): ModelInfo? =
        providers.value.firstOrNull { it.id == providerId }?.models?.get(modelId)

    fun provider(id: String): ProviderInfo? = providers.value.firstOrNull { it.id == id }

    /** If the selected model is gone (first launch or the provider was disconnected), take the best available one. */
    private fun ensureSelection() {
        // a latched fresh install is waiting for its Deep seed: an auto-picked
        // fallback here would read as "the user already has a model" and close
        // the seed's door before it ever runs (the seed overwrites prefs itself)
        if (prefs.deepSeedPending) return
        val list = providers.value
        if (list.isEmpty()) return
        if (prefs.hasModel && model(prefs.providerId, prefs.modelId) != null) return
        for (p in list) {
            val m = defaults[p.id]?.let { p.models[it] } ?: p.models.values.firstOrNull() ?: continue
            prefs.providerId = p.id; prefs.modelId = m.id; prefs.variant = ""
            // a forced switch away from a local provider releases the offline model's RAM right away
            OfflineModels.onModelPicked(p.id)
            return
        }
    }

    /** After a provider is connected, select its model: prefer fast ("flash") ones and the server's recommended ones. */
    fun selectBestFor(providerId: String) {
        val p = provider(providerId) ?: return
        val m = defaults[providerId]?.let { p.models[it] }
            ?: p.models.values.filter { it.id.contains("flash", true) && !it.id.contains("lite", true) }.maxByOrNull { it.id }
            ?: p.models.values.firstOrNull() ?: return
        prefs.providerId = p.id; prefs.modelId = m.id; prefs.variant = ""
        // the shared post-pick decision (a local pick enters its light agent, a cloud pick releases the local server)
        OfflineModels.onModelPicked(p.id)
    }

    fun selectedLabel(): String {
        val m = model(prefs.providerId, prefs.modelId)
        return m?.name?.takeIf { it.isNotBlank() } ?: prefs.modelId.ifBlank { "Select model" }
    }
}
