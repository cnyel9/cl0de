package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SSet
import com.cl0de.app.data.UsageWindow
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcDialog
import com.cl0de.app.ui.components.OcTextField
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.SmallButton
import com.cl0de.app.ui.components.Spinner
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.components.UsageBar
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.launch

private fun resetText(w: UsageWindow): String {
    val at = w.resetAt ?: return SSet.noUsageInWindow
    val ms = at - System.currentTimeMillis()
    if (ms <= 0) return SSet.resettingNow
    val min = ms / 60_000
    return when {
        min < 60 -> SSet.freesUpMinutes(min)
        min < 60 * 48 -> SSet.freesUpHours(min / 60, min % 60)
        else -> SSet.freesUpDays(min / 1440, (min % 1440) / 60)
    }
}

private fun money(x: Double) = if (x >= 100) "$%.0f".format(x) else "$%.2f".format(x)

@Composable
private fun UsageCard(title: String, w: UsageWindow) {
    val c = Oc.c
    val pct = w.percent
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Oc.round(24.dp)).background(c.surface).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(title, size = 21.sp, modifier = Modifier.weight(1f))
            Txt(if (pct > 0 && pct < 1) SSet.lessThanOnePctUsed else SSet.pctUsed(pct), size = 17.sp, color = c.textDim)
        }
        Spacer(Modifier.height(14.dp))
        UsageBar(w.fraction.toFloat(), warn = pct >= 85)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Txt(resetText(w), size = 16.sp, color = c.textDim, modifier = Modifier.weight(1f))
            Txt("${money(w.used)} / ${money(w.limit)}", size = 16.sp, color = c.textDim)
        }
    }
}

@Composable
fun UsageScreen(nav: Nav) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    val snap by Graph.usage.snapshot.collectAsState()
    val refreshing by Graph.usage.refreshing.collectAsState()
    var info by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { Graph.usage.recompute(); runCatching { Graph.usage.backfill() } }

    Column(Modifier.fillMaxSize().screenBackdrop("UsageScreen1")) {
        TopBar(SSet.usage, left = { IconBtn(Ic.Back, { nav.pop() }) }, right = { IconBtn(Ic.Info, { info = true }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UsageCard(SSet.currentSession5Hours, snap.fiveHour)
            SectionLabel(SSet.weeklyLimits, Modifier.padding(bottom = 0.dp))
            UsageCard(SSet.last7Days, snap.week)
            SectionLabel(SSet.monthlyLimits, Modifier.padding(bottom = 0.dp))
            UsageCard(SSet.last30Days, snap.month)

            SectionLabel(SSet.modelsLast30Days, Modifier.padding(bottom = 0.dp))
            if (snap.byModel.isEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Oc.round(24.dp)).background(c.surface).padding(20.dp)) {
                    Txt(SSet.noPaidUsageYet, size = 20.sp)
                    Txt(SSet.onceAgentAnswers, size = 16.sp, color = c.textDim, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                SettingsGroup {
                    snap.byModel.take(12).forEach { m ->
                        SettingsRow(
                            title = m.model.ifBlank { SSet.unknown }, subtitle = SSet.modelUsageSubtitle(m.provider, m.messages, "%.1f".format(m.tokens / 1000.0)),
                            trailing = { Txt(money(m.cost), size = 17.sp) },
                        )
                    }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallButton(SSet.editLimits, { edit = true })
                SmallButton(if (refreshing) SSet.refreshing else SSet.refresh, { scope.launch { Graph.usage.backfill() } })
                if (refreshing) Spinner(18.dp, stroke = 2.dp)
            }
            Txt(
                SSet.trackedFromText(snap.tracked, money(snap.totalMonth)),
                size = 14.sp, color = c.textDim, modifier = Modifier.padding(horizontal = 20.dp), lineHeight = 20.sp,
            )
        }
    }

    if (info) OcDialog(SSet.howCounted, { info = false }) {
        Txt(
            SSet.howCountedBody,
            size = 15.sp, color = Oc.c.textDim, lineHeight = 22.sp,
        )
    }
    if (edit) EditLimitsDialog { edit = false }
}

@Composable
private fun EditLimitsDialog(onDone: () -> Unit) {
    val p = Graph.prefs
    var a by remember { mutableStateOf(p.limit5h.toString().removeSuffix(".0")) }
    var w by remember { mutableStateOf(p.limitWeek.toString().removeSuffix(".0")) }
    var m by remember { mutableStateOf(p.limitMonth.toString().removeSuffix(".0")) }
    var prov by remember { mutableStateOf(p.limitProviders) }
    OcDialog(SSet.limitsUsd, onDone, S.save, {
        a.toDoubleOrNull()?.let { p.limit5h = it }; w.toDoubleOrNull()?.let { p.limitWeek = it }; m.toDoubleOrNull()?.let { p.limitMonth = it }
        p.limitProviders = prov; Graph.usage.recompute(); onDone()
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(SSet.fiveHours, size = 14.sp, color = Oc.c.textDim); OcTextField(a, { a = it }, keyboardType = KeyboardType.Decimal)
            Txt(SSet.sevenDays, size = 14.sp, color = Oc.c.textDim); OcTextField(w, { w = it }, keyboardType = KeyboardType.Decimal)
            Txt(SSet.thirtyDays, size = 14.sp, color = Oc.c.textDim); OcTextField(m, { m = it }, keyboardType = KeyboardType.Decimal)
            Txt(SSet.providersCounted, size = 14.sp, color = Oc.c.textDim); OcTextField(prov, { prov = it })
        }
    }
}
