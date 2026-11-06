package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SSet
import com.cl0de.app.runtime.RuntimeState
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.Route
import com.cl0de.app.ui.components.ChoiceRow
import com.cl0de.app.ui.components.Chip
import com.cl0de.app.ui.components.Ico
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.ModelSheet
import com.cl0de.app.ui.components.OcSwitch
import com.cl0de.app.ui.components.OcTextField
import com.cl0de.app.ui.components.OcDialog
import com.cl0de.app.ui.components.Pill
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.AppIcons
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc

@Composable
fun ChoiceDialog(title: String, options: List<Triple<String, String, ImageVector?>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Oc.c
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(Oc.round(32.dp)).background(c.bg).border(1.dp, c.border, Oc.round(32.dp)).heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Txt(title, Modifier.padding(start = 24.dp, top = 16.dp, bottom = 10.dp), size = 22.sp)
            options.forEach { (k, l, ic) -> ChoiceRow(l, k == selected, { onSelect(k); onDismiss() }, icon = ic) }
        }
    }
}

@Composable
fun SettingsScreen(nav: Nav, onMenu: () -> Unit) {
    val c = Oc.c
    val prefs = Graph.prefs
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val rt by Graph.runtime.state.collectAsState()
    val providers by Graph.catalog.providers.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }
    var modelSheet by remember { mutableStateOf(false) }
    var iconSheet by remember { mutableStateOf(false) }
    val hasGo = providers.any { it.id == "opencode-go" }
    val modelLabel = Graph.catalog.selectedLabel()
    val serverText = when (rt) {
        is RuntimeState.Running -> SSet.runningVersion((rt as RuntimeState.Running).version)
        is RuntimeState.Starting -> SSet.starting
        is RuntimeState.Crashed -> SSet.crashed
        else -> SSet.stopped
    }

    Column(Modifier.fillMaxSize().screenBackdrop("SettingsScreen1")) {
        TopBar(S.settings, left = { IconBtn(Ic.Menu, onMenu) }, right = { IconBtn(Ic.Info, { nav.push(Route.About) }) }, serif = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            // Account card (in Messenger, a profile like in Telegram)
            if (Oc.look.id == "messenger") Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(96.dp) { nav.push(Route.Profile) }
                Spacer(Modifier.height(12.dp))
                Txt(prefs.userName.ifBlank { "OpenCode" }, size = 22.sp, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Txt(if (hasGo) "OpenCode Go" else SSet.freeModels, size = 14.sp, color = c.link, modifier = Modifier.padding(top = 2.dp))
            } else Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Oc.round(22.dp)).border(1.dp, c.border, Oc.round(22.dp))
                    .clickable { nav.push(if (prefs.signedIn.isBlank()) Route.Login else Route.Providers) }.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(prefs.userName.ifBlank { "OpenCode" }, size = 19.sp)
                    Txt(
                        when (prefs.signedIn) { "github" -> SSet.signedInWithGitHub; "email" -> SSet.signedInWithEmail; "key" -> SSet.usingApiKey; else -> SSet.notSignedIn },
                        size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(Modifier.clip(Oc.pill).background(c.text).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Txt(if (hasGo) "Go" else S.free, size = 14.sp, color = c.bg)
                }
                Spacer(Modifier.width(10.dp)); Ico(Ic.ChevronRight, tint = c.textDim, size = 22.dp)
            }
            if (prefs.signedIn.isBlank()) {
                Column(Modifier.padding(16.dp).fillMaxWidth().clip(Oc.round(22.dp)).border(1.dp, c.border, Oc.round(22.dp)).padding(24.dp)) {
                    Txt(SSet.signInForMore, size = 21.sp)
                    Txt(SSet.freeModelsShareLimit, size = 17.sp, color = c.textDim, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    Pill(SSet.signIn, { nav.push(Route.Login) })
                }
            } else if (!hasGo) {
                Column(Modifier.padding(16.dp).fillMaxWidth().clip(Oc.round(22.dp)).border(1.dp, c.border, Oc.round(22.dp)).padding(24.dp)) {
                    Txt(SSet.wantMoreModels, size = 21.sp)
                    Txt(SSet.connectStrongerModels, size = 17.sp, color = c.textDim, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    Pill(SSet.connect, { nav.push(Route.Providers) })
                }
            } else Spacer(Modifier.height(16.dp))

            // Like in Claude: four short groups, the rest on nested screens.
            SettingsGroup {
                SettingsRow(SSet.profile, icon = Ic.User, onClick = { nav.push(Route.Profile) })
                SettingsRow(SSet.billing, icon = Ic.Dollar, onClick = { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://opencode.ai/console")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) })
                SettingsRow(SSet.usage, icon = Ic.Drop, onClick = { nav.push(Route.Usage) })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.capabilities, subtitle = listOfNotNull(SSet.memory.takeIf { prefs.memoryEnabled }, modelLabel.takeIf { it.isNotBlank() }).joinToString(" · "), icon = Ic.Sliders, onClick = { nav.push(Route.Capabilities) })
                SettingsRow("OpenCode", subtitle = serverText, icon = Ic.Terminal, onClick = { nav.push(Route.OcSettings) })
                SettingsRow(SSet.connectors, subtitle = SSet.mcpServers, icon = Ic.Puzzle, onClick = { nav.push(Route.Connectors) })
                SettingsRow("GitHub", subtitle = if (com.cl0de.app.data.GitHub.signedIn) "@" + prefs.githubLogin else SSet.letOpenCodeWorkRepos, icon = Ic.GitHub, onClick = { nav.push(Route.GitHub) })
                SettingsRow(SSet.phoneTools, subtitle = if (prefs.phoneTools) SSet.phoneToolsOn else S.off, icon = Ic.Phone, onClick = { nav.push(Route.PhoneTools) })
                SettingsRow(SSet.permissions, subtitle = mapOf("ask" to SSet.permissionAsk, "edits" to SSet.permissionAutoEdit, "auto" to SSet.permissionAuto)[prefs.permissionMode], icon = Ic.Shield, onClick = { nav.push(Route.PermissionsCfg) })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.interfaceTitle, subtitle = com.cl0de.app.ui.theme.Looks.of(prefs.edition).title, icon = Ic.Laptop, onClick = { nav.push(Route.Edition) })
                // the mascot tab: only in the Cl0de look
                if (Oc.look.id == "cloud") SettingsRow("Cl0de", subtitle = SSet.mascotOutfitsGames, icon = Ic.Cloud, onClick = { nav.push(Route.MascotSettings) })
                SettingsRow(SSet.colorMode, subtitle = prefs.colorMode.replaceFirstChar { it.uppercase() }, icon = Ic.Moon, onClick = { dialog = "color" })
                SettingsRow(SSet.fontStyle, subtitle = if (prefs.fontStyle == "default") SSet.defaultLabel else SSet.systemLabel, icon = Ic.FontSize, onClick = { nav.push(Route.Appearance) })
                SettingsRow(SSet.voice, subtitle = com.cl0de.app.data.Speech.label(), icon = Ic.Waveform, onClick = { dialog = "voice" })
                SettingsRow("Bahasa / Language", subtitle = com.cl0de.app.data.AppLang.label(), icon = Ic.Globe, onClick = { dialog = "applang" })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.hapticFeedback, icon = Ic.Vibrate, trailing = { OcSwitch(prefs.haptics) { prefs.haptics = it } })
                SettingsRow(SSet.progressOnLockScreen, subtitle = SSet.progressOnLockScreenSub, icon = Ic.ListCheck, trailing = { OcSwitch(prefs.liveNotify) { prefs.liveNotify = it } })
                SettingsRow(SSet.costBeforeSending, subtitle = SSet.costBeforeSendingSub, icon = Ic.Dollar, trailing = { OcSwitch(prefs.costHint) { prefs.costHint = it } })
                SettingsRow(SSet.notifications, icon = Ic.Bell, onClick = {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                })
                SettingsRow(SSet.appIcon, subtitle = AppIcons.all.firstOrNull { it.id == prefs.appIcon }?.label, icon = Ic.Image, onClick = { iconSheet = true })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.reportBug, subtitle = SSet.reportBugSub, icon = Ic.Warning, onClick = { dialog = "bug" })
            }
            if (prefs.signedIn.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                SettingsGroup {
                    SettingsRow(SSet.logOut, icon = Ic.External, titleColor = c.danger, onClick = {
                        scope.launch {
                            for (pid in listOf("opencode", "opencode-go", "github-copilot", "google", "groq", "openrouter")) runCatching { Graph.api.removeAuth(pid) }
                            runCatching { Graph.api.disposeAll() }
                            com.cl0de.app.data.Vault.forgetAccount()
                            prefs.signedIn = ""; runCatching { Graph.catalog.refresh() }
                        }
                    })
                }
            }
        }
    }

    if (dialog == "bug") BugDialog { dialog = null }
    when (dialog) {
        "voice" -> ChoiceDialog(SSet.speechLanguage, com.cl0de.app.data.Speech.languages.map { Triple(it.first, it.second, null) }, prefs.speechLang, { prefs.speechLang = it }) { dialog = null }
        "applang" -> ChoiceDialog("Bahasa / Language", com.cl0de.app.data.AppLang.options.map { Triple(it.first, it.second, null) }, prefs.appLang, { prefs.appLang = it; com.cl0de.app.data.AppLang.applyRule() }) { dialog = null }
        "color" -> ChoiceDialog(SSet.colorMode, listOf(Triple("system", SSet.systemLabel, Ic.Phone), Triple("light", SSet.lightLabel, Ic.Sun), Triple("dark", SSet.darkLabel, Ic.Moon)), prefs.colorMode, { prefs.colorMode = it }) { dialog = null }
        "font" -> ChoiceDialog(SSet.fontStyle, listOf(Triple("default", SSet.defaultSerifHeadings, null), Triple("system", SSet.systemLabel, Ic.Phone)), prefs.fontStyle, { prefs.fontStyle = it }) { dialog = null }
        "size" -> ChoiceDialog(SSet.textSize, listOf(Triple("small", SSet.smallLabel, null), Triple("medium", SSet.mediumLabel, null), Triple("large", SSet.largeLabel, null)), prefs.textSize, { prefs.textSize = it }) { dialog = null }
        "scale" -> ChoiceDialog(SSet.displaySize, listOf(Triple("90", SSet.compactLabel, null), Triple("100", SSet.defaultLabel, null), Triple("110", SSet.largeLabel, null), Triple("120", SSet.extraLargeLabel, null)), prefs.uiScale, { prefs.uiScale = it }) { dialog = null }
        "code" -> ChoiceDialog(SSet.codeFont, listOf(Triple("mono", SSet.monospaceLabel, null), Triple("system", SSet.systemLabel, null)), prefs.codeFont, { prefs.codeFont = it }) { dialog = null }
    }
    if (modelSheet) ModelSheet { modelSheet = false }
    if (iconSheet) IconPickerSheet { iconSheet = false }
}

/** "Capabilities": model, memory, agents, skills, what Claude keeps in the section of the same name. */
@Composable
fun CapabilitiesScreen(nav: Nav) {
    val c = Oc.c
    val prefs = Graph.prefs
    var modelSheet by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().screenBackdrop("SettingsScreen2")) {
        TopBar(SSet.capabilities, left = { IconBtn(Ic.Back, { nav.pop() }) }, serif = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                SettingsRow(S.model, subtitle = Graph.catalog.selectedLabel(), icon = Ic.Cpu, onClick = { modelSheet = true })
                SettingsRow(SSet.modelsAndSmallModel, icon = Ic.Sparkle, onClick = { nav.push(Route.Models) })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.memory, subtitle = if (prefs.memoryEnabled) SSet.shelvesCount(com.cl0de.app.data.Memory.shelves().size) else S.off, icon = Ic.Scroll, onClick = { nav.push(Route.Memory) })
                SettingsRow(SSet.agents, subtitle = prefs.agent.replaceFirstChar { it.uppercase() }, icon = Ic.Bot, onClick = { nav.push(Route.Agents) })
                SettingsRow(SSet.team, subtitle = when (prefs.teamMode) { "manual" -> SSet.yourTeamHelpers(com.cl0de.app.data.Team.members.size); "auto" -> SSet.automaticHelpers(com.cl0de.app.data.Team.max); else -> S.off }, icon = Ic.Users, onClick = { nav.push(Route.Team) })
                SettingsRow("Skills", subtitle = SSet.skillsTypeInChat(com.cl0de.app.data.Skills.local().size), icon = Ic.Scroll, onClick = { nav.push(Route.Skills) })
            }
        }
    }
    if (modelSheet) ModelSheet { modelSheet = false }
}

/** "OpenCode": the server on the phone, providers, config, like the Claude Code section in Claude. */
@Composable
fun OcSettingsScreen(nav: Nav) {
    val c = Oc.c
    val rt by Graph.runtime.state.collectAsState()
    Column(Modifier.fillMaxSize().screenBackdrop("SettingsScreen3")) {
        TopBar("OpenCode", left = { IconBtn(Ic.Back, { nav.pop() }) }, serif = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                SettingsRow(S.providers, subtitle = SSet.signInsAndApiKeys, icon = Ic.Key, onClick = { nav.push(Route.Providers) })
                SettingsRow(SSet.localServer, subtitle = when (rt) {
                    is RuntimeState.Running -> SSet.runningVersion((rt as RuntimeState.Running).version)
                    is RuntimeState.Starting -> SSet.starting; is RuntimeState.Crashed -> SSet.restarting; else -> SSet.stopped
                }, icon = Ic.Server, onClick = { nav.push(Route.Server) })
                SettingsRow("Config (opencode.json)", icon = Ic.Code, onClick = { nav.push(Route.ConfigEditor) })
                SettingsRow(SSet.backupAndMove, subtitle = SSet.everythingInOneFile, icon = Ic.Archive, onClick = { nav.push(Route.Backup) })
            }
        }
    }
}

/** "Font style": font, text and interface size, code, one screen instead of five rows. */
@Composable
fun AppearanceScreen(nav: Nav) {
    val c = Oc.c
    val prefs = Graph.prefs
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().screenBackdrop("SettingsScreen4")) {
        TopBar(SSet.fontStyle, left = { IconBtn(Ic.Back, { nav.pop() }) }, serif = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            SettingsGroup {
                SettingsRow("Font", subtitle = if (prefs.fontStyle == "default") SSet.defaultSerifReplies else SSet.systemLabel, icon = Ic.FontSize, onClick = { dialog = "font" })
                SettingsRow(SSet.textSize, subtitle = prefs.textSize.replaceFirstChar { it.uppercase() }, icon = Ic.File, onClick = { dialog = "size" })
                SettingsRow(SSet.displaySize, subtitle = when (prefs.uiScale) { "90" -> SSet.compactLabel; "110" -> SSet.largeLabel; "120" -> SSet.extraLargeLabel; else -> SSet.defaultLabel }, icon = Ic.Expand, onClick = { dialog = "scale" })
            }
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                SettingsRow(SSet.codeFont, subtitle = if (prefs.codeFont == "mono") SSet.monospaceLabel else SSet.systemLabel, icon = Ic.Terminal, onClick = { dialog = "code" })
                SettingsRow(SSet.wrapLongLines, icon = Ic.Diff, trailing = { OcSwitch(prefs.wrapLines) { prefs.wrapLines = it } })
            }
        }
    }
    when (dialog) {
        "font" -> ChoiceDialog("Font", listOf(Triple("default", SSet.defaultSerifReplies, null), Triple("system", SSet.systemLabel, Ic.Phone)), prefs.fontStyle, { prefs.fontStyle = it }) { dialog = null }
        "size" -> ChoiceDialog(SSet.textSize, listOf(Triple("small", SSet.smallLabel, null), Triple("medium", SSet.mediumLabel, null), Triple("large", SSet.largeLabel, null)), prefs.textSize, { prefs.textSize = it }) { dialog = null }
        "scale" -> ChoiceDialog(SSet.displaySize, listOf(Triple("90", SSet.compactLabel, null), Triple("100", SSet.defaultLabel, null), Triple("110", SSet.largeLabel, null), Triple("120", SSet.extraLargeLabel, null)), prefs.uiScale, { prefs.uiScale = it }) { dialog = null }
        "code" -> ChoiceDialog(SSet.codeFont, listOf(Triple("mono", SSet.monospaceLabel, null), Triple("system", SSet.systemLabel, null)), prefs.codeFont, { prefs.codeFont = it }) { dialog = null }
    }
}

/** What happened: a couple of lines and, optionally, the tail of the server log; then GitHub opens. */
@Composable
private fun BugDialog(onDismiss: () -> Unit) {
    val c = Oc.c
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var what by remember { mutableStateOf("") }
    var withLog by remember { mutableStateOf(true) }
    OcDialog(SSet.reportBug, onDismiss, confirmText = SSet.openGitHub, onConfirm = {
        com.cl0de.app.data.BugReport.open(ctx, "Bug: " + what.trim().lineSequence().firstOrNull().orEmpty().take(70), what, withLog)
        onDismiss()
    }) {
        Column {
            Txt(SSet.bugQuestion, size = 15.sp, color = c.textDim)
            Spacer(Modifier.height(10.dp))
            OcTextField(what, { what = it }, Modifier.fillMaxWidth(), placeholder = SSet.bugPlaceholder, singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(SSet.attachServerLog, size = 15.sp)
                    Txt(SSet.lastLinesKeysRemoved, size = 13.sp, color = c.textDim)
                }
                OcSwitch(withLog) { withLog = it }
            }
            Spacer(Modifier.height(8.dp))
            Txt(SSet.bugGitHubNote, size = 13.sp, color = c.textDim, lineHeight = 18.sp)
        }
    }
}
