package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.SMisc
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.TopBar
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.theme.DesktopDark
import com.cl0de.app.ui.theme.DarkColors
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.InterFamily
import com.cl0de.app.ui.theme.Oc
import com.cl0de.app.ui.theme.OcColors
import com.cl0de.app.ui.theme.TerminalDark
import com.cl0de.app.ui.theme.Look
import com.cl0de.app.ui.theme.Looks
import com.cl0de.app.ui.theme.SerifDisplay
import com.cl0de.app.ui.theme.SerifFamily
import com.cl0de.app.ui.theme.UnboundedFamily

@Composable
fun EditionScreen(nav: Nav) {
    val c = Oc.c
    Column(Modifier.fillMaxSize().screenBackdrop("EditionScreen1")) {
        TopBar(SMisc.interfaceLabel, left = { IconBtn(Ic.Back, { nav.pop() }) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // two columns: several looks side by side at once
            Looks.all.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { l ->
                        val on = Graph.prefs.edition == l.id
                        Column(
                            Modifier.weight(1f).clip(Oc.round(20.dp)).background(c.surface)
                                .border(if (on) 2.dp else 1.dp, if (on) c.link else c.border, Oc.round(20.dp))
                                .clickable { Graph.prefs.edition = l.id }.padding(10.dp),
                        ) {
                            LookPreview(l)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Txt(l.title, Modifier.weight(1f), size = 15.sp, weight = FontWeight.Medium, maxLines = 1)
                                Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (on) c.link else c.border, CircleShape).padding(4.dp).then(if (on) Modifier.background(c.link, CircleShape) else Modifier))
                            }
                            Txt(l.sub, Modifier.padding(top = 3.dp), size = 12.sp, color = c.textDim, lineHeight = 16.sp, maxLines = 3)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Txt(SMisc.editionFooter, Modifier.padding(horizontal = 6.dp, vertical = 6.dp), size = 13.sp, color = c.textDim, lineHeight = 19.sp)
            // intro animation and icon for the chosen look
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var intro by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Graph.prefs.introEnabled) }
            val match = com.cl0de.app.ui.theme.AppIcons.all.firstOrNull { it.id == "look_" + Graph.prefs.edition }
            com.cl0de.app.ui.components.SettingsGroup(Modifier.padding(top = 4.dp)) {
                com.cl0de.app.ui.components.SettingsRow(SMisc.launchAnimation, subtitle = SMisc.launchAnimationSub, icon = Ic.Play, trailing = {
                    com.cl0de.app.ui.components.OcSwitch(intro) { intro = it; Graph.prefs.introEnabled = it }
                })
                com.cl0de.app.ui.components.SettingsRow(SMisc.playItNow, icon = Ic.Sparkle, onClick = { com.cl0de.app.ui.components.Intro.replay.intValue++ })
                if (match != null) com.cl0de.app.ui.components.SettingsRow(
                    if (Graph.prefs.appIcon == match.id) SMisc.iconMatches else SMisc.useLookIcon(match.label),
                    subtitle = SMisc.anyIconSettings, icon = Ic.Image,
                    onClick = { Graph.prefs.appIcon = match.id; com.cl0de.app.ui.theme.AppIcons.apply(ctx, match.id) },
                )
            }
        }
    }
}

/** A small screen mockup in the look's own palette, fonts and style, independent of the current theme. */
@Composable
internal fun LookPreview(l: Look) {
    val p: OcColors = l.dark
    val font: FontFamily = when (l.id) { "terminal" -> FontFamily.Monospace; "notebook" -> SerifFamily; "messenger" -> FontFamily.Default; "claude" -> InterFamily; else -> InterFamily }
    val head: FontFamily = when (l.id) { "claude", "notebook" -> SerifDisplay; "hairline" -> UnboundedFamily; else -> font }
    val r: (Dp) -> Shape = { d -> when (l.id) { "terminal" -> RoundedCornerShape(0.dp); "desktop" -> RoundedCornerShape(6.dp); else -> RoundedCornerShape((d * l.radius).let { x -> l.radiusMax?.let { x.coerceAtMost(it.dp) } ?: x }) } }
    val send = l.sendColor?.invoke(p) ?: p.text
    Box(Modifier.fillMaxWidth().height(132.dp).clip(r(14.dp)).background(p.bg).padding(9.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Txt(if (l.id == "terminal") "opencode" else "OpenCode", size = 14.sp, color = p.text, family = head, weight = if (l.id == "hairline" || l.id == "terminal") FontWeight.Bold else null)
                Spacer(Modifier.height(6.dp))
                val msg = SMisc.makeItBlue
                when (l.user) {
                    "plain" -> Txt(SMisc.youMessage(msg), size = 10.sp, color = p.textDim, family = font)
                    "line" -> Box(Modifier.align(Alignment.End).border(1.dp, p.border, r(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Txt(msg, size = 10.sp, color = p.text, family = font) }
                    "card" -> Box(Modifier.align(Alignment.End).clip(r(10.dp)).background(p.surface).border(1.dp, p.border, r(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Txt(msg, size = 10.sp, color = p.text, family = font) }
                    else -> if (l.id == "terminal") Row(Modifier.fillMaxWidth().height(22.dp).background(p.surface)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(p.link)); Txt(msg, Modifier.padding(start = 6.dp).align(Alignment.CenterVertically), size = 10.sp, color = p.text, family = font)
                    } else Box(Modifier.align(Alignment.End).clip(r(12.dp)).background(p.userBubble).padding(horizontal = 8.dp, vertical = 4.dp)) { Txt(msg, size = 10.sp, color = p.text, family = font) }
                }
                Spacer(Modifier.height(5.dp))
                val ans = SMisc.doneButtonBlue
                when (l.reply) {
                    "bubble" -> Box(Modifier.clip(r(12.dp)).background(p.surface).padding(horizontal = 8.dp, vertical = 4.dp)) { Txt(ans, size = 10.sp, color = p.text, family = font) }
                    "card" -> Box(Modifier.fillMaxWidth().clip(r(10.dp)).background(p.surface).border(1.dp, p.border, r(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Txt(ans, size = 10.sp, color = p.text, family = font) }
                    else -> Txt(ans, size = 10.sp, color = p.text, family = if (l.id == "claude" || l.id == "notebook") SerifFamily else font)
                }
            }
            Row(
                Modifier.fillMaxWidth().height(28.dp).clip(if (l.compact || l.id == "claude") RoundedCornerShape(50) else r(12.dp)).background(p.surface).border(1.dp, p.border, if (l.compact || l.id == "claude") RoundedCornerShape(50) else r(12.dp)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(if (l.id == "terminal") "Build · pickle" else SMisc.askAnything, size = 10.sp, color = p.textDim, family = font, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(18.dp).clip(if (l.pill != null || l.frame != "claude") r(5.dp) else CircleShape).background(send))
            }
        }
    }
}
