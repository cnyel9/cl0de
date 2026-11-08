package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.BuildConfig
import com.cl0de.app.data.S
import com.cl0de.app.data.SSet
import com.cl0de.app.Graph
import com.cl0de.app.api.OcJson
import com.cl0de.app.api.str
import com.cl0de.app.runtime.RuntimeState
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.components.Chip
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcDialog
import com.cl0de.app.ui.components.OcSheet
import com.cl0de.app.ui.components.OcSwitch
import com.cl0de.app.ui.components.OcTextField
import com.cl0de.app.ui.components.Pill
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.Segmented
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.SmallButton
import com.cl0de.app.ui.components.Spinner
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File

// ---------- Connectors (MCP) ----------

@Composable
fun ConnectorsScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<JsonObject?>(null) }
    var add by remember { mutableStateOf(false) }
    suspend fun load() { status = runCatching { Graph.api.mcpStatus() }.getOrDefault(JsonObject(emptyMap())) }
    LaunchedEffect(Unit) { load() }
    Box(Modifier.fillMaxSize().screenBackdrop("MoreScreens1")) {
        Column(Modifier.fillMaxSize()) {
            TopBar(SSet.connectors, left = { IconBtn(Ic.Back, { nav.pop() }) })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 120.dp)) {
                val s = status
                if (s == null) Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner(28.dp) }
                else if (s.isEmpty()) Txt(SSet.noMcpServers, Modifier.padding(24.dp), size = 16.sp, color = c.textDim, lineHeight = 23.sp)
                else SettingsGroup {
                    s.forEach { (name, v) ->
                        val st = (v as? JsonObject)?.str("status") ?: SSet.unknown
                        val on = st == "connected"
                        SettingsRow(name, subtitle = (v as? JsonObject)?.str("error") ?: st.replace('_', ' '), icon = Ic.Puzzle, trailing = {
                            OcSwitch(on) { want ->
                                scope.launch { runCatching { if (want) Graph.api.mcpConnect(name) else Graph.api.mcpDisconnect(name) }; load() }
                            }
                        })
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) { Pill(SSet.addConnector, { add = true }, icon = Ic.Plus) }
    }
    if (add) AddConnectorDialog({ add = false; scope.launch { load() } })
}

@Composable
private fun AddConnectorDialog(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("remote") }
    var target by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    OcDialog(SSet.addConnector, onDone, S.add, {
        val cfg = if (kind == "remote") buildJsonObject { put("type", "remote"); put("url", target.trim()); put("enabled", true) }
        else buildJsonObject {
            put("type", "local"); put("enabled", true)
            putJsonArray("command") { target.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { add(JsonPrimitive(it)) } }
        }
        scope.launch {
            runCatching { Graph.api.patchConfig(buildJsonObject { put("mcp", buildJsonObject { put(name.trim(), cfg) }) }); Graph.api.mcpAdd(name.trim(), cfg) }
                .onSuccess { onDone() }.onFailure { error = it.message }
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OcTextField(name, { name = it }, placeholder = SSet.nameEgGithub)
            Segmented(listOf("remote" to SSet.remoteUrl, "local" to SSet.localCommand), kind, { kind = it })
            OcTextField(target, { target = it }, placeholder = if (kind == "remote") "https://…/mcp" else "npx -y @modelcontextprotocol/server-everything", mono = true)
            error?.let { Txt(it, size = 14.sp, color = Oc.c.danger) }
        }
    }
}

// ---------- Local server ----------

@Composable
fun ServerScreen(nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rt by Graph.runtime.state.collectAsState()
    val log by Graph.runtime.log.collectAsState()
    val prefs = Graph.prefs
    var showLog by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf<List<String>?>(null) }
    var reset by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().screenBackdrop("MoreScreens2")) {
        TopBar(SSet.localServer, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Oc.round(24.dp)).background(c.surface).padding(20.dp)) {
                val (label, color) = when (val s = rt) {
                    is RuntimeState.Running -> SSet.runningVersion(s.version) to c.ok
                    is RuntimeState.Starting -> SSet.starting to c.warn
                    is RuntimeState.Crashed -> s.message to c.danger
                    else -> SSet.stopped to c.textDim
                }
                Txt(label, size = 19.sp, color = color)
                Txt("Ubuntu 24.04 (proot) · 127.0.0.1:${prefs.serverPort}", size = 15.sp, color = c.textDim, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (rt is RuntimeState.Running || rt is RuntimeState.Starting) SmallButton(S.stop, { Graph.runtime.stopServer() }, danger = true)
                    else SmallButton(SSet.start, { Graph.runtime.startServer() }, filled = true)
                    SmallButton(SSet.restart, { Graph.runtime.stopServer(); scope.launch { kotlinx.coroutines.delay(1200); Graph.runtime.startServer() } })
                    SmallButton("Log", { showLog = true })
                }
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.startWithApp, icon = Ic.Zap, trailing = { OcSwitch(prefs.autoStartServer) { prefs.autoStartServer = it } })
                SettingsRow(SSet.keepCpuAwake, icon = Ic.Server, trailing = { OcSwitch(prefs.keepAwake) { prefs.keepAwake = it } })
                SettingsRow(SSet.compatibilityMode, subtitle = SSet.compatibilitySub, icon = Ic.Shield, trailing = { OcSwitch(prefs.noSeccomp) { prefs.noSeccomp = it } })
                // Android 12+ kills "extra" child processes of apps (phantom process killer), and proot has many
                SettingsRow(SSet.serverKeepsStopping, subtitle = SSet.serverKeepsStoppingSub, icon = Ic.Info, onClick = {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { Graph.chats.toasts.tryEmit(SSet.devOptionsToast) }
                })
            }
            Spacer(Modifier.height(16.dp))
            ServerRecoveryCard()
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.updateOpenCode, subtitle = updating ?: SSet.installedVersion(prefs.opencodeVersion.ifBlank { "?" }), icon = Ic.Download, onClick = {
                    if (updating == null) { updating = SSet.checking; scope.launch {
                        updating = runCatching { SSet.updatedTo(Graph.runtime.updateOpencode()) }.getOrElse { it.message }
                    } }
                })
                SettingsRow(SSet.installDevTools, subtitle = "git, ripgrep, curl, python3, build-essential, nodejs", icon = Ic.Terminal, onClick = {
                    val lines = mutableStateListOf<String>(); tools = lines
                    scope.launch {
                        val code = Graph.runtime.installDevTools { l -> scope.launch(Dispatchers.Main) { lines.add(l) } }
                        lines.add(if (code == 0) SSet.doneCheck else SSet.failedExit(code))
                    }
                })
                SettingsRow(SSet.dontKillServer, subtitle = SSet.exemptBattery, icon = Ic.Zap, onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                })
                SettingsRow(SSet.resetEnvironment, subtitle = SSet.deletesUbuntu, icon = Ic.Trash, titleColor = c.danger, onClick = { reset = true })
            }
            Txt(
                SSet.adbHintPrefix +
                    "adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "adb shell settings put global settings_enable_monitor_phantom_procs false",
                size = 13.sp, color = c.textDim, modifier = Modifier.padding(20.dp), lineHeight = 19.sp, family = Oc.mono,
            )
        }
    }

    if (showLog) LogSheet(log) { showLog = false }
    tools?.let { lines -> LogSheet(lines, SSet.installingTools) { tools = null } }
    if (reset) OcDialog(SSet.resetEnvironmentQuestion, { reset = false }, SSet.reset, {
        reset = false; Graph.runtime.resetRuntime()
    }, danger = true) { Txt(SSet.resetBody, size = 16.sp, color = Oc.c.textDim) }
}

/**
 * Recovery flow for a server that crashed or will not start, right under the
 * server settings group: the field fix as five numbered steps (stop the
 * server, App info, clear cache, force stop, start again), plus a shortcut
 * button that opens this app's App info page directly.
 */
@Composable
private fun ServerRecoveryCard() {
    val c = Oc.c
    val ctx = LocalContext.current
    val steps = listOf(
        SSet.recoveryStepStopServer,
        SSet.recoveryStepAppInfo,
        SSet.recoveryStepClearCache,
        SSet.recoveryStepForceStop,
        SSet.recoveryStepRestartServer,
    )
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Oc.round(24.dp)).background(c.surface).padding(20.dp)) {
        Txt(SSet.serverRecoveryTitle, size = 17.sp, weight = androidx.compose.ui.text.font.FontWeight.Medium)
        Txt(SSet.serverRecoveryIntro, size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        steps.forEachIndexed { i, label ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(c.surfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) { Txt((i + 1).toString(), size = 12.sp, weight = androidx.compose.ui.text.font.FontWeight.Medium) }
                    if (i < steps.lastIndex) Box(Modifier.width(2.dp).height(16.dp).background(c.border))
                }
                Spacer(Modifier.width(12.dp))
                Txt(label, size = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        SmallButton(SSet.openAppInfo, {
            val appInfo = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${ctx.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(appInfo) }
                .onFailure { Graph.chats.toasts.tryEmit(SSet.appInfoUnavailable) }
        }, filled = true)
    }
}

@Composable
private fun LogSheet(lines: List<String>, title: String = SSet.serverLog, onDismiss: () -> Unit) {
    val c = Oc.c
    val clip = LocalClipboardManager.current
    OcSheet(onDismiss, title, skipHalf = false) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallButton(S.copy, { clip.setText(AnnotatedString(lines.joinToString("\n"))) })
        }
        Column(Modifier.fillMaxWidth().height(480.dp).verticalScroll(rememberScrollState(), reverseScrolling = true).padding(16.dp)) {
            Txt(lines.joinToString("\n").ifBlank { SSet.emptyLog }, size = 11.sp, family = Oc.mono, color = c.textDim, lineHeight = 15.sp)
        }
    }
}

// ---------- Config editor ----------

@Composable
fun ConfigEditorScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var importMsg by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val importCtx = LocalContext.current
    val pretty = remember { Json { prettyPrint = true } }
    suspend fun load() {
        text = runCatching { pretty.encodeToString(JsonObject.serializer(), Graph.api.config()) }.getOrElse { "{}" }; loaded = true
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true; importMsg = null
            runCatching { com.cl0de.app.data.ConfigImport.import(importCtx, uri) }
                .onSuccess { r ->
                    importMsg = SSet.importedCounts(r.providers, r.models) + " " + SSet.listReloaded
                    load()
                }
                .onFailure { importMsg = it.message ?: SSet.importFailed }
            importing = false
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().screenBackdrop("MoreScreens3")) {
        TopBar("Config", left = { IconBtn(Ic.Back, { nav.pop() }) }, right = { IconBtn(Ic.Refresh, { scope.launch { load() } }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Txt(SSet.configDescription, size = 14.sp, color = c.textDim, modifier = Modifier.padding(bottom = 12.dp), lineHeight = 20.sp)
            if (!loaded) Spinner(26.dp) else OcTextField(text, { text = it; msg = null }, singleLine = false, minLines = 16, mono = true)
            Spacer(Modifier.height(14.dp))
            Pill(S.save, {
                val o = runCatching { OcJson.parseToJsonElement(text) as JsonObject }.getOrNull()
                if (o == null) msg = SSet.notValidJson
                else scope.launch {
                    msg = runCatching {
                        Graph.api.patchGlobalConfig(o)
                        // Same reload the FreeModels flow applies after it changes the
                        // global config, so new providers/models show up at once.
                        runCatching { Graph.api.disposeAll() }
                        runCatching { Graph.catalog.refresh() }
                        SSet.savedModelsReloaded
                    }.getOrElse { it.message }
                }
            }, modifier = Modifier.fillMaxWidth())
            msg?.let { Txt(it, size = 15.sp, color = if (it == SSet.saved || it == SSet.savedModelsReloaded) c.ok else c.danger, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(26.dp))
            Txt(SSet.importFromOldApp, size = 16.sp, weight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            Txt(
                SSet.importFromOldAppDesc,
                size = 14.sp, color = c.textDim, modifier = Modifier.padding(bottom = 12.dp), lineHeight = 20.sp,
            )
            Pill(if (importing) SSet.importing else SSet.importConfigOldApp, { if (!importing) importer.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth())
            importMsg?.let { Txt(it, size = 15.sp, color = c.textDim, modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}

// ---------- Profile ----------

@Composable
fun ProfileScreen(nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    val prefs = Graph.prefs
    var name by remember { mutableStateOf(prefs.userName) }
    var rules by remember { mutableStateOf(com.cl0de.app.data.Memory.userRules()) }
    var saved by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runCatching {
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val side = minOf(bmp.width, bmp.height)
            val sq = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val out = android.graphics.Bitmap.createScaledBitmap(sq, 512.coerceAtMost(side), 512.coerceAtMost(side), true)
            val f = File(ctx.filesDir, "avatar-${System.currentTimeMillis()}.jpg")
            f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            prefs.avatarPath = f.path
        }
    }
    Column(Modifier.fillMaxSize().screenBackdrop("MoreScreens4")) {
        TopBar(SSet.profile, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(112.dp) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            Spacer(Modifier.height(14.dp))
            SmallButton(SSet.editPhoto, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt(SSet.whatCallYou, size = 17.sp, modifier = Modifier.padding(start = 4.dp))
                OcTextField(name, { name = it })
                Pill(SSet.updateProfile, { prefs.userName = name.trim() }, enabled = name.trim() != prefs.userName, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Txt(SSet.personalPreferencesQuestion, size = 19.sp, modifier = Modifier.padding(start = 4.dp))
                Txt(SSet.savedAsAgentsMd, size = 14.sp, color = c.textDim, modifier = Modifier.padding(start = 4.dp))
                OcTextField(rules, { rules = it; saved = false }, singleLine = false, minLines = 6, placeholder = SSet.profileRulesPlaceholder)
                Pill(if (saved) SSet.saved else SSet.savePreferences, {
                    runCatching { com.cl0de.app.data.Memory.saveUserRules(rules) }.onSuccess { saved = true }
                }, enabled = !saved, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------- About ----------

@Composable
fun AboutScreen(nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    fun open(u: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    Column(Modifier.fillMaxSize().screenBackdrop("MoreScreens5")) {
        TopBar(SSet.about, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                SettingsRow("OpenCode mobile", subtitle = "v${BuildConfig.VERSION_NAME}", icon = Ic.Info)
                if (com.cl0de.app.ui.theme.Oc.look.id == "cloud") SettingsRow("Cl0de", subtitle = Graph.prefs.petCount.let { "${SSet.petted} $it ${if (it == "1") SSet.timeWord else SSet.timesWord}" }, icon = Ic.Cloud, chevron = true, onClick = { nav.push(com.cl0de.app.ui.Route.MascotSettings) })
                SettingsRow(SSet.sourceOnGitHub, subtitle = "cnyel9/cl0de", icon = Ic.GitHub, onClick = { open("https://github.com/cnyel9/cl0de") })
                SettingsRow("OpenCode (in container)", subtitle = Graph.prefs.opencodeVersion.ifBlank { SSet.notInstalled }, icon = Ic.Terminal)
                SettingsRow(SSet.openCodeDocumentation, icon = Ic.External, onClick = { open("https://opencode.ai/docs") })
                SettingsRow(SSet.openCodeGoConsole, icon = Ic.External, onClick = { open("https://opencode.ai/console") })
            }
            // Updates from GitHub releases
            val upd by com.cl0de.app.data.Updater.state.collectAsState()
            SettingsGroup {
                SettingsRow(
                    SSet.checkForUpdates, icon = Ic.Download, chevron = true,
                    subtitle = when (val u = upd) {
                        is com.cl0de.app.data.Updater.State.Checking -> SSet.checkingGitHub
                        is com.cl0de.app.data.Updater.State.UpToDate -> SSet.youreOnNewest
                        is com.cl0de.app.data.Updater.State.Available -> SSet.versionIsOut(u.r.name)
                        is com.cl0de.app.data.Updater.State.Downloading -> SSet.downloadingPlain((u.progress * 100).toInt())
                        is com.cl0de.app.data.Updater.State.Installing -> SSet.installing
                        is com.cl0de.app.data.Updater.State.Failed -> u.message
                        else -> SSet.fromReleases(com.cl0de.app.data.Updater.REPO)
                    },
                    onClick = {
                        if (upd !is com.cl0de.app.data.Updater.State.Available) com.cl0de.app.data.Updater.check()
                        com.cl0de.app.ui.components.UpdateUi.open.value = true
                    },
                )
                SettingsRow(SSet.checkAutomatically, subtitle = SSet.looksForNewVersion, icon = Ic.Refresh,
                    trailing = { OcSwitch(Graph.prefs.updateAuto) { Graph.prefs.updateAuto = it } })
            }
            Txt(
                SSet.unofficialClientNote,
                size = 13.sp, color = c.textDim, modifier = Modifier.padding(20.dp), lineHeight = 19.sp,
            )
        }
    }
}
