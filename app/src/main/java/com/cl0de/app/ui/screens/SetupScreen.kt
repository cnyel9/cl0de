package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.Graph
import com.cl0de.app.data.SMisc
import com.cl0de.app.runtime.RuntimeState
import com.cl0de.app.ui.components.Heading
import com.cl0de.app.ui.components.Logo
import com.cl0de.app.ui.components.Pill
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.components.UsageBar
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.launch

/** First launch: install Ubuntu and OpenCode inside the phone. */
@Composable
fun SetupScreen(rt: RuntimeState) {
    val c = Oc.c
    val scope = rememberCoroutineScope()
    val log by Graph.runtime.log.collectAsState()
    Column(
        Modifier.fillMaxSize().screenBackdrop("SetupScreen1").statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Logo(80.dp)
        Spacer(Modifier.height(28.dp))
        Heading(SMisc.openCodeOnPhone, size = 30.sp, align = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Txt(
            SMisc.setupBody,
            size = 16.sp, color = c.textDim, align = TextAlign.Center, lineHeight = 23.sp,
        )
        Spacer(Modifier.height(32.dp))
        when (rt) {
            is RuntimeState.Installing -> {
                Txt(rt.step, size = 18.sp)
                Spacer(Modifier.height(14.dp))
                UsageBar(rt.progress ?: 0.04f)
                if (rt.detail.isNotBlank()) Txt(rt.detail, size = 14.sp, color = c.textDim, modifier = Modifier.padding(top = 10.dp))
            }
            is RuntimeState.InstallFailed -> {
                Txt(rt.message, size = 15.sp, color = c.danger, align = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Pill(SMisc.tryAgain, { scope.launch { Graph.runtime.install() } })
            }
            else -> Pill(SMisc.setUp, { scope.launch { Graph.runtime.install() } })
        }
        if (log.isNotEmpty() && rt !is RuntimeState.NotInstalled) {
            Spacer(Modifier.height(24.dp))
            Txt(
                log.takeLast(6).joinToString("\n"), size = 11.sp, color = c.textDim, family = Oc.mono, maxLines = 8,
                modifier = Modifier.fillMaxWidth().clip(Oc.round(14.dp)).background(c.codeBg).padding(12.dp),
            )
        }
    }
}
