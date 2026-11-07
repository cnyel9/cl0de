package com.cl0de.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SMisc
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.Route
import com.cl0de.app.ui.components.Holiday
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcSwitch
import com.cl0de.app.ui.components.PickRow
import com.cl0de.app.ui.components.PickerFrame
import com.cl0de.app.ui.components.PixelCloud
import com.cl0de.app.ui.components.SectionLabel
import com.cl0de.app.ui.components.SettingsGroup
import com.cl0de.app.ui.components.SettingsRow
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import com.cl0de.app.ui.theme.screenBackdrop

/**
 * The "Cl0de" tab in settings (only in the Cl0de look): a live preview of the mascot, what it
 * can do and reacts to, its outfit (by date, none, or any by hand), stats and the room.
 */
@Composable
fun MascotSettings(nav: Nav) {
    val c = Oc.c
    val p = Graph.prefs
    // switches change settings at once; the counter redraws the screen and the preview
    var rev by remember { mutableIntStateOf(0) }
    var outfitSheet by remember { mutableStateOf(false) }
    fun toggle(set: (Boolean) -> Unit): (Boolean) -> Unit = { set(it); rev++ }

    Column(Modifier.fillMaxSize().screenBackdrop("MascotSettings")) {
        TopBar("Cl0de", left = { IconBtn(Ic.Back, { nav.pop() }) })
        key(rev) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp)) {
                Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                    PixelCloud(Modifier.size(width = 150.dp, height = 102.dp))
                }
                Txt(SMisc.mascotTryHere, size = 13.sp, color = c.textDim, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))

                SettingsGroup {
                    SettingsRow(SMisc.showOnHomeScreen, icon = Ic.Cloud, trailing = { OcSwitch(p.mascotOn, toggle { p.mascotOn = it }) })
                    SettingsRow(SMisc.room, subtitle = SMisc.roomSub, icon = Ic.Play, chevron = true, onClick = { nav.push(Route.Mascot) })
                }

                SectionLabel(SMisc.inChats)
                SettingsGroup {
                    SettingsRow(SMisc.assistantIsCl0de, subtitle = SMisc.assistantIsCl0deSubLong, icon = Ic.Chat,
                        trailing = { OcSwitch(p.mascotPersona, toggle { p.mascotPersona = it }) })
                }

                SectionLabel(SMisc.reactsTo)
                SettingsGroup {
                    SettingsRow(SMisc.vibration, subtitle = SMisc.vibrationSubMascot, icon = Ic.Vibrate, trailing = { OcSwitch(p.mascotHaptics, toggle { p.mascotHaptics = it }) })
                    SettingsRow(SMisc.shakeTilt, subtitle = SMisc.shakeTiltSub, icon = Ic.Phone, trailing = { OcSwitch(p.mascotSensors, toggle { p.mascotSensors = it }) })
                    SettingsRow(SMisc.chargingLowBattery, icon = Ic.Zap, trailing = { OcSwitch(p.mascotBattery, toggle { p.mascotBattery = it }) })
                    SettingsRow(SMisc.dozesOff, icon = Ic.Moon, trailing = { OcSwitch(p.mascotSleepy, toggle { p.mascotSleepy = it }) })
                    SettingsRow(SMisc.morningStretch, subtitle = SMisc.morningStretchSub, icon = Ic.Sun, trailing = { OcSwitch(p.mascotMorning, toggle { p.mascotMorning = it }) })
                }

                SectionLabel(SMisc.outfits)
                SettingsGroup {
                    SettingsRow(SMisc.outfit, subtitle = outfitLabel(p.mascotOutfit), icon = Ic.Sparkle, chevron = true, onClick = { outfitSheet = true })
                    SettingsRow(SMisc.snowLeaves, subtitle = SMisc.snowLeavesSub, icon = Ic.Drop, trailing = { OcSwitch(p.mascotSeasonal, toggle { p.mascotSeasonal = it }) })
                }

                SectionLabel(SMisc.together)
                SettingsGroup {
                    val pets = p.petCount
                    SettingsRow(SMisc.pettedLabel, subtitle = SMisc.timesCount(pets), icon = Ic.Sparkle)
                    SettingsRow(SMisc.bestRally, subtitle = p.pongBest, icon = Ic.ListCheck)
                    SettingsRow(SMisc.reset, icon = Ic.Refresh, titleColor = c.danger, onClick = { p.petCount = "0"; p.pongBest = "0"; rev++ })
                }
            }
        }
    }

    if (outfitSheet) PickerFrame(SMisc.outfit, { outfitSheet = false }) {
        fun pick(v: String) { p.mascotOutfit = v; outfitSheet = false; rev++ }
        PickRow(SMisc.byDate, { pick("auto") }, selected = p.mascotOutfit == "auto")
        PickRow(S.none, { pick("off") }, selected = p.mascotOutfit == "off")
        Holiday.wardrobe.forEach { (label, h) -> PickRow(label, { pick(h.name) }, selected = p.mascotOutfit == h.name) }
    }
}

private fun outfitLabel(v: String): String = when (v) {
    "auto" -> SMisc.byDate + (Holiday.now()?.let { h -> Holiday.wardrobe.firstOrNull { it.second == h }?.first?.let { SMisc.outfitToday(it) } } ?: "")
    "off" -> S.none
    else -> Holiday.wardrobe.firstOrNull { it.second.name == v }?.first ?: v
}
