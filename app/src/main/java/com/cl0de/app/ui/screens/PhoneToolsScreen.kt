package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.api.obj
import com.cl0de.app.api.str
import com.cl0de.app.data.PhoneBridge
import com.cl0de.app.data.S
import com.cl0de.app.data.SMisc
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcSwitch
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.launch

/** What the agent can do on the phone: the "phone" MCP server the app runs itself. */
@Composable
fun PhoneToolsScreen(nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    val prefs = Graph.prefs
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var rev by remember { mutableIntStateOf(0) }
    LaunchedEffect(prefs.phoneTools, rev) {
        status = runCatching { Graph.api.mcpStatus().obj(PhoneBridge.NAME)?.str("status") }.getOrNull()
    }
    val located = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { rev++ }
    val off = prefs.phoneToolsOff.split(',').filter { it.isNotBlank() }.toSet()

    Column(Modifier.fillMaxSize().screenBackdrop("PhoneToolsScreen1")) {
        TopBar(SMisc.phoneTools, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
            Txt(
                SMisc.phoneToolsIntro,
                Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp), size = 15.sp, color = c.textDim, lineHeight = 21.sp,
            )
            SettingsGroup {
                SettingsRow(
                    SMisc.letOpenCodeUsePhone, icon = Ic.Phone,
                    subtitle = when {
                        Graph.devices.isRemote -> SMisc.onlyForThisPhoneServer
                        !prefs.phoneTools -> S.off
                        status == "connected" -> SMisc.connectedTools(PhoneBridge.enabledTools().size)
                        status == null -> SMisc.startsWithServer
                        else -> "Status: $status"
                    },
                    trailing = { OcSwitch(prefs.phoneTools) { prefs.phoneTools = it; scope.launch { PhoneBridge.register(); rev++ } } },
                )
                if (!located) SettingsRow(SMisc.allowLocation, subtitle = SMisc.locationToolNeeded, icon = Ic.Pin, chevron = true, onClick = {
                    askLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                })
            }
            if (prefs.phoneTools) {
                SectionLabel(SMisc.toolsLabel)
                SettingsGroup {
                    PhoneBridge.all.forEach { t ->
                        SettingsRow(SMisc.toolTitle(t.name, t.title), subtitle = SMisc.toolDesc(t.name, t.desc).substringBefore(". ").removeSuffix("."), trailing = {
                            OcSwitch(t.name !in off) { on -> prefs.phoneToolsOff = (if (on) off - t.name else off + t.name).joinToString(",") }
                        })
                    }
                }
                Txt(
                    SMisc.phoneToolsTry,
                    Modifier.padding(20.dp), size = 13.sp, color = c.textDim, lineHeight = 19.sp,
                )
            }
        }
    }
}
