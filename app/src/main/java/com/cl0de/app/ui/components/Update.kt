package com.cl0de.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cl0de.app.BuildConfig
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SSet
import com.cl0de.app.data.Updater
import com.cl0de.app.data.Updater.State
import com.cl0de.app.ui.theme.Oc

/** Open the update window by hand (from "About"). */
object UpdateUi { val open = mutableStateOf(false) }

/**
 * The update announcer: checks GitHub by itself on launch and on every resume. A fresh version
 * pops up as a dialog with an Install button; the download progress and the system installer
 * run through the sheet below, which is also the manual window opened from "About".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateHost() {
    val st by Updater.state.collectAsState()
    var dismissed by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { Updater.autoCheck() }
    val r = when (val s = st) {
        is State.Available -> s.r
        is State.Downloading -> s.r
        is State.Installing -> s.r
        is State.Failed -> s.r
        else -> null
    }
    val ctx = LocalContext.current
    var allowed by remember { mutableStateOf(Updater.canInstall(ctx)) }
    val owner = LocalLifecycleOwner.current
    // every return to the app re-checks for updates (the check itself is unthrottled now) and refreshes the install permission
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { allowed = Updater.canInstall(ctx); Updater.autoCheck() } }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    // A fresh version announces itself as a dialog, on every route (this host lives in the root).
    // "Later" skips the tag for good, exactly like the sheet's skip below; download and install
    // progress stay with the sheet, so this dialog only ever shows the Available state and the
    // two never appear together for the same release.
    val showDialog = st is State.Available && r != null && r.tag != Graph.prefs.updateSkipped && r.tag != dismissed && !UpdateUi.open.value
    if (showDialog && r != null) {
        val dc = Oc.c
        OcDialog(
            title = SSet.updateAvailable,
            onDismiss = { Graph.prefs.updateSkipped = r.tag; dismissed = r.tag },
            confirmText = if (allowed) SSet.install else SSet.allowInstalls,
            onConfirm = { if (allowed) Updater.install(ctx, r) else Updater.openInstallPermission(ctx) },
            dismissText = SSet.later,
        ) {
            Column {
                Txt("v${Updater.current}  →  ${r.name.removePrefix("v").let { "v$it" }}", size = 15.sp, color = dc.textDim)
                if (!allowed) {
                    Spacer(Modifier.height(10.dp))
                    Txt(SSet.androidAsksInstall, size = 14.sp, color = dc.textDim, lineHeight = 20.sp)
                }
                if (r.notes.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).background(dc.surfaceHigh).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Txt(r.notes.replace("**", "").replace(Regex("(?m)^- "), "•  ").trim(), size = 14.sp, lineHeight = 20.sp)
                    }
                }
                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(10.dp))
                    Txt(SSet.testBuildNote, size = 13.sp, color = dc.textDim)
                }
            }
        }
        return
    }
    // the sheet pops up by itself only for an in-flight or failed install (Available belongs to the dialog),
    // and only for a version that wasn't skipped or dismissed
    val auto = r != null && (st is State.Downloading || st is State.Installing || st is State.Failed) && r.tag != Graph.prefs.updateSkipped && r.tag != dismissed
    if (!UpdateUi.open.value && !auto) return
    if (UpdateUi.open.value && st is State.Idle) LaunchedEffect(Unit) { Updater.check() }

    val c = Oc.c
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun close() { UpdateUi.open.value = false; if (r != null) dismissed = r.tag }

    ModalBottomSheet(onDismissRequest = ::close, sheetState = sheet, containerColor = c.surface, dragHandle = null, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
            Txt(
                when (st) {
                    is State.Checking, is State.Idle -> SSet.checkingForUpdates
                    is State.UpToDate -> SSet.upToDate
                    is State.Failed -> SSet.updateDidntGoThrough
                    else -> SSet.updateAvailable
                },
                size = 22.sp, weight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Txt(
                if (r != null) "v${Updater.current}  →  ${r.name.removePrefix("v").let { "v$it" }}" else SSet.newestVersion(Updater.current),
                size = 15.sp, color = c.textDim,
            )
            if (r != null && r.notes.isNotBlank() && st !is State.Failed) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceHigh).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Txt(r.notes.replace("**", "").replace(Regex("(?m)^- "), "•  ").trim(), size = 14.sp, lineHeight = 20.sp)
                }
            }
            (st as? State.Failed)?.let { Spacer(Modifier.height(12.dp)); Txt(it.message, size = 14.sp, color = c.danger, lineHeight = 20.sp) }
            if (BuildConfig.DEBUG && r != null) {
                Spacer(Modifier.height(10.dp))
                Txt(SSet.testBuildNote, size = 13.sp, color = c.textDim)
            }
            Spacer(Modifier.height(20.dp))
            when (val s = st) {
                is State.Downloading -> {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.surfaceHigh)) {
                        Box(Modifier.fillMaxWidth(s.progress.coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape).background(c.accent))
                    }
                    Spacer(Modifier.height(10.dp))
                    Txt(SSet.downloadingPct((s.progress * 100).toInt()) + if (s.r.size > 0) "  ·  ${s.r.size / 1_048_576} MB" else "", size = 14.sp, color = c.textDim)
                }
                is State.Installing -> Txt(SSet.openingInstaller, size = 14.sp, color = c.textDim)
                is State.Available, is State.Failed -> if (r == null) Pill(SSet.tryAgain, { Updater.check() }, Modifier.fillMaxWidth())
                    else if (!allowed) {
                        Txt(SSet.androidAsksInstall, size = 14.sp, color = c.textDim, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        Pill(SSet.allowInstalls, { Updater.openInstallPermission(ctx) }, Modifier.fillMaxWidth())
                    } else Pill(if (s is State.Failed) SSet.tryAgain else SSet.install, { Updater.install(ctx, r) }, Modifier.fillMaxWidth())
                else -> Pill(S.close, ::close, Modifier.fillMaxWidth(), filled = false)
            }
            if (r != null && (st is State.Available || st is State.Failed)) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt(SSet.skipThisVersion, Modifier.clip(CircleShape).tap(scale = 1f) { Graph.prefs.updateSkipped = r.tag; close() }.padding(vertical = 12.dp), size = 15.sp, color = c.textDim)
                    Txt(SSet.later, Modifier.clip(CircleShape).tap(scale = 1f, onClick = ::close).padding(vertical = 12.dp), size = 15.sp, color = c.text)
                }
            }
        }
    }
}
