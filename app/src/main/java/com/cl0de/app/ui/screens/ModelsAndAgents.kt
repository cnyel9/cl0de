package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SMisc
import com.cl0de.app.api.NamedItem
import com.cl0de.app.api.str
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.Route
import com.cl0de.app.ui.components.ChoiceRow
import com.cl0de.app.ui.components.Chip
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.ModelSheet
import com.cl0de.app.ui.components.OcSheet
import com.cl0de.app.ui.components.Segmented
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun ModelsScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    val providers by Graph.catalog.providers.collectAsState()
    var cfg by remember { mutableStateOf(JsonObject(emptyMap())) }
    var pickMain by remember { mutableStateOf(false) }
    var pickSmall by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cfg = runCatching { Graph.api.config() }.getOrDefault(cfg) }
    Column(Modifier.fillMaxSize().screenBackdrop("ModelsAndAgents1")) {
        TopBar(S.models, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                SettingsRow(SMisc.modelForNewMessages, subtitle = Graph.catalog.selectedLabel(), icon = Ic.Cpu, chevron = true, onClick = { pickMain = true })
                SettingsRow(SMisc.smallModel, subtitle = cfg.str("small_model") ?: SMisc.autoTitlesSummaries, icon = Ic.Sparkle, chevron = true, onClick = { pickSmall = true })
            }
            SectionLabel(SMisc.available)
            SettingsGroup {
                providers.forEach { p -> SettingsRow(p.name.ifBlank { p.id }, subtitle = SMisc.modelsCount(p.models.size), icon = Ic.Server) }
            }
        }
    }
    if (pickMain) ModelSheet(onDismiss = { pickMain = false }, onOfflineModels = { pickMain = false; nav.push(Route.OfflineModels) })
    if (pickSmall) OcSheet({ pickSmall = false }, SMisc.smallModel, skipHalf = false) {
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            item { ChoiceRow(SMisc.autoLabel, cfg.str("small_model") == null, { pickSmall = false }, subtitle = SMisc.letOpenCodeChoose) }
            providers.forEach { p ->
                items(p.models.values.toList(), key = { p.id + it.id }) { m ->
                    val ref = p.id + "/" + m.id
                    ChoiceRow(m.name.ifBlank { m.id }, cfg.str("small_model") == ref, {
                        pickSmall = false
                        scope.launch { cfg = runCatching { Graph.api.patchConfig(buildJsonObject { put("small_model", ref) }) }.getOrDefault(cfg) }
                    }, subtitle = p.name)
                }
            }
        }
    }
}

@Composable
fun PermissionsScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    var cfg by remember { mutableStateOf(JsonObject(emptyMap())) }
    LaunchedEffect(Unit) { cfg = runCatching { Graph.api.config() }.getOrDefault(cfg) }
    val perm = cfg["permission"]
    fun valueOf(k: String): String = when (perm) {
        is JsonPrimitive -> perm.content
        is JsonObject -> (perm[k] as? JsonPrimitive)?.content ?: "default"
        else -> "default"
    }
    fun set(k: String, v: String) = scope.launch {
        cfg = runCatching { Graph.api.patchConfig(buildJsonObject { put("permission", buildJsonObject { put(k, v) }) }) }.getOrDefault(cfg)
    }
    val opts = listOf("allow" to SMisc.allow, "ask" to SMisc.ask, "deny" to SMisc.deny)
    Column(Modifier.fillMaxSize().screenBackdrop("ModelsAndAgents2")) {
        TopBar(SMisc.permissions, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SectionLabel(SMisc.newSessions)
            Column(Modifier.padding(horizontal = 16.dp)) {
                Segmented(listOf("ask" to SMisc.ask, "edits" to SMisc.autoEdit, "auto" to SMisc.autoLabel), Graph.prefs.permissionMode, { Graph.prefs.permissionMode = it })
                Txt(
                    SMisc.permissionsDesc,
                    size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 10.dp), lineHeight = 20.sp,
                )
            }
            SectionLabel(SMisc.globalRules)
            listOf("edit" to SMisc.fileEdits, "bash" to SMisc.shellCommands, "webfetch" to SMisc.webFetch, "external_directory" to SMisc.outsideProject).forEach { (k, label) ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Txt(label, size = 17.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                    Segmented(opts, valueOf(k), { set(k, it) })
                }
            }
        }
    }
}

@Composable
fun AgentsScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    val agents by Graph.catalog.agents.collectAsState()
    LaunchedEffect(Unit) { Graph.catalog.refresh() }
    Column(Modifier.fillMaxSize().screenBackdrop("ModelsAndAgents3")) {
        TopBar(SMisc.agents, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                agents.forEach { a ->
                    SettingsRow(
                        a.name.replaceFirstChar { it.uppercase() }, subtitle = a.description, icon = Ic.Bot,
                        trailing = { if (Graph.prefs.agent == a.name) Chip(SMisc.active, color = c.link) },
                        onClick = {
                            Graph.prefs.agent = a.name
                            scope.launch { runCatching { Graph.api.patchConfig(buildJsonObject { put("default_agent", a.name) }) } }
                        },
                    )
                }
            }
            Txt(SMisc.customAgentsNote, size = 14.sp, color = c.textDim, modifier = Modifier.padding(20.dp), lineHeight = 20.sp)
        }
    }
}
