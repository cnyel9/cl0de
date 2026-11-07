package com.cl0de.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.AppLang
import com.cl0de.app.data.OfflineCatalog
import com.cl0de.app.data.OfflineModel
import com.cl0de.app.data.OfflineModels
import com.cl0de.app.data.S
import com.cl0de.app.data.SOffline
import com.cl0de.app.runtime.LlamaState
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.components.Card
import com.cl0de.app.ui.components.Chip
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcDialog
import com.cl0de.app.ui.components.Pill
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.SmallButton
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.components.UsageBar
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import com.cl0de.app.ui.theme.screenBackdrop
import kotlinx.coroutines.launch

private fun fmtGb(v: Double): String {
    val raw = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
    return if (AppLang.indonesian()) raw.replace('.', ',') else raw
}

private fun mb(bytes: Long) = "%.0f MB".format(bytes / 1e6)

/**
 * Offline Models: the llama.cpp engine setup, the RAM-class catalog with its
 * downloads, and the local server status. Downloaded models also appear in the
 * normal model picker (registered as the "offline" provider).
 */
@Composable
fun OfflineModelsScreen(nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val llama by Graph.runtime.llamaState.collectAsState()
    val tick = OfflineModels.catalogTick // re-read the on-disk set whenever it changes
    val ramBytes = remember { OfflineModels.totalRamBytes(ctx) }
    val catalog = remember(ramBytes) { OfflineCatalog.forRam(ramBytes) }
    var deleteTarget by remember { mutableStateOf<OfflineModel?>(null) }

    LaunchedEffect(Unit) { scope.launch { OfflineModels.syncProvider() } }

    Column(Modifier.fillMaxSize().screenBackdrop("OfflineModels1")) {
        TopBar(SOffline.title, left = { IconBtn(Ic.Back, { nav.pop() }) }, serif = true)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp),
        ) {
            // --- local server + device ---
            SettingsGroup {
                val statusText = when (val s = llama) {
                    is LlamaState.Running -> SOffline.serverRunning(nameOf(s.modelId))
                    is LlamaState.Starting -> SOffline.serverStarting(nameOf(s.modelId))
                    is LlamaState.Failed -> SOffline.serverFailed(s.message)
                    LlamaState.Stopped -> SOffline.serverStopped
                }
                SettingsRow(SOffline.serverTitle, subtitle = statusText, icon = Ic.Server)
                SettingsRow(
                    SOffline.deviceRam,
                    subtitle = SOffline.ramInfo(fmtGb(ramBytes / 1_073_741_824.0), OfflineCatalog.isHighClass(ramBytes)),
                    icon = Ic.Cpu,
                )
                if (llama is LlamaState.Running || llama is LlamaState.Starting) {
                    SettingsRow(SOffline.stopServer, icon = Ic.Stop, titleColor = c.danger, onClick = { Graph.runtime.stopLlama() })
                }
            }
            Spacer(Modifier.height(16.dp))

            // --- engine ---
            val engineReady = OfflineModels.engineReady()
            val eng = OfflineModels.engineState
            if (!engineReady || eng == OfflineModels.EngineState.Failed) {
                SectionLabel(SOffline.engineTitle)
                Card(Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        when {
                            eng == OfflineModels.EngineState.Downloading || eng == OfflineModels.EngineState.Extracting -> {
                                Txt(if (eng == OfflineModels.EngineState.Downloading) SOffline.engineDownloading else SOffline.engineExtracting, size = 18.sp)
                                Spacer(Modifier.height(14.dp))
                                UsageBar(OfflineModels.engineProgress)
                                if (OfflineModels.engineDetail.isNotBlank()) {
                                    Txt(OfflineModels.engineDetail, size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 10.dp))
                                }
                            }
                            eng == OfflineModels.EngineState.Failed -> {
                                Txt(SOffline.engineFailed, size = 16.sp, color = c.danger)
                                Spacer(Modifier.height(12.dp))
                                Pill(S.retry, { OfflineModels.setupEngine() })
                            }
                            else -> {
                                Txt(SOffline.engineMissing, size = 16.sp, color = c.textDim, lineHeight = 23.sp)
                                Spacer(Modifier.height(12.dp))
                                Pill(SOffline.setUpEngine, { OfflineModels.setupEngine() })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // --- catalog of this device's RAM class ---
            SectionLabel(SOffline.catalogTitle)
            catalog.forEach { m ->
                ModelCard(m, ramBytes, llama, tick, onDelete = { deleteTarget = it })
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    deleteTarget?.let { m ->
        OcDialog(
            SOffline.deleteTitle, { deleteTarget = null }, S.delete,
            { deleteTarget = null; OfflineModels.deleteModel(m) },
            dismissText = S.cancel, danger = true,
        ) { Txt(SOffline.deleteBody(m.displayName), size = 16.sp, color = c.textDim) }
    }
}

private fun nameOf(modelId: String): String = OfflineCatalog.byId(modelId)?.displayName ?: modelId

@Composable
private fun ModelCard(m: OfflineModel, ramBytes: Long, llama: LlamaState, tick: Int, onDelete: (OfflineModel) -> Unit) {
    val c = Oc.c
    val downloaded = remember(m.id, tick) { OfflineModels.isDownloaded(m) }
    val dl = OfflineModels.dlStates[m.id]
    val ramOk = OfflineCatalog.ramOk(ramBytes, m)
    val isActive = (llama as? LlamaState.Running)?.modelId == m.id
    val isStarting = (llama as? LlamaState.Starting)?.modelId == m.id
    val bench = OfflineModels.benchStates[m.id]
    val benchRes = remember(m.id, tick) { OfflineModels.benchResult(m) }
    val partial = remember(m.id, tick, dl) { if (downloaded) 0L else OfflineModels.partialBytes(m) }

    Card(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(m.displayName, size = 18.sp, weight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                when {
                    downloaded -> Chip(if (isActive) SOffline.activeLabel else SOffline.ready)
                    dl != null -> Unit // downloading shows progress below; a failed state shows its own retry button below
                    partial > 0L -> Unit // the resume button below is this entry's single download trigger
                    !ramOk -> Unit
                    else -> IconBtn(Ic.Download, { OfflineModels.downloadModel(m) }, size = 40.dp, iconSize = 22.dp)
                }
            }
            val desc = SOffline.desc(m.id)
            if (desc.isNotBlank()) {
                Txt(desc, size = 15.sp, color = c.textDim, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(mb(m.totalBytes))
                Chip(SOffline.minRam(fmtGb(m.minRamGb)))
            }
            if (benchRes != null) {
                // the bench result gets its own full-width block: inside the row above,
                // its long text overflowed the card and the start of it was clipped away
                Chip(
                    SOffline.benchResult("%.1f".format(benchRes.first), "%.1f".format(benchRes.second)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            when {
                dl != null && !dl.failed -> {
                    Spacer(Modifier.height(12.dp))
                    Txt(SOffline.downloadingLabel, size = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    UsageBar(dl.fraction)
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(
                            "${mb(dl.doneBytes)} / ${mb(dl.totalBytes)} · ${(dl.fraction * 100).toInt()}%",
                            size = 14.sp, color = c.textDim, modifier = Modifier.weight(1f),
                        )
                        SmallButton(S.cancel, { OfflineModels.cancelDownload(m) })
                    }
                }
                dl != null && dl.failed -> {
                    Spacer(Modifier.height(12.dp))
                    Txt(SOffline.downloadFailed, size = 15.sp, color = c.danger)
                    Spacer(Modifier.height(8.dp))
                    SmallButton(S.retry, { OfflineModels.downloadModel(m) })
                }
                downloaded -> {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallButton(
                            if (isActive) SOffline.activeLabel else SOffline.use,
                            { OfflineModels.useModel(m) },
                            filled = !isActive,
                        )
                        when (bench) {
                            // the running-status text is the only flexible element: it
                            // shrinks and wraps into whatever space the buttons leave,
                            // so no button is ever squeezed below its natural width
                            "running" -> Txt(SOffline.benchRunning, size = 14.sp, color = c.textDim, modifier = Modifier.weight(1f))
                            else -> {
                                SmallButton(SOffline.benchButton, { OfflineModels.bench(m) })
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        // delete is an icon-only button by request: no text label
                        IconBtn(
                            Ic.Trash, { onDelete(m) },
                            modifier = Modifier.semantics { contentDescription = S.delete },
                            tint = c.danger, size = 40.dp, iconSize = 20.dp,
                        )
                    }
                    if (bench == "failed") Txt(SOffline.benchFailed, size = 14.sp, color = c.danger, modifier = Modifier.padding(top = 6.dp))
                    if (isStarting) Txt(SOffline.serverStarting(m.displayName), size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 6.dp))
                }
                !ramOk -> {
                    Spacer(Modifier.height(10.dp))
                    Txt(SOffline.needsRam(fmtGb(m.minRamGb)), size = 14.sp, color = c.textDim)
                }
                partial > 0L -> {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallButton(SOffline.resumeDownload, { OfflineModels.downloadModel(m) }, filled = true)
                        Spacer(Modifier.width(10.dp))
                        Txt(mb(partial), size = 14.sp, color = c.textDim)
                    }
                }
            }
        }
    }
}
