package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cl0de.app.Graph
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.Route
import com.cl0de.app.ui.components.Ico
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.Heading
import com.cl0de.app.ui.components.PixelWordmark
import com.cl0de.app.ui.components.Spinner
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.components.tap
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/** Saves the OpenCode account key on the server: it works for both Zen and Go. */
suspend fun connectAccount(key: String, method: String) {
    Graph.api.setApiKey("opencode", key)
    runCatching { Graph.api.setApiKey("opencode-go", key) }
    runCatching { Graph.api.disposeAll() }
    Graph.prefs.signedIn = method
    Graph.prefs.loginSeen = true
    runCatching { Graph.catalog.refresh() }
}

/** Sign-in screen: free models through a GitHub account (or Groq/OpenRouter), no subscription. */
@Composable
fun LoginScreen(nav: Nav) {
    val c = Oc.c
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    fun leave() { Graph.prefs.loginSeen = true; if (!nav.pop()) nav.root(Route.Home) }

    // Like in Claude: one main path and a couple of sign-in options, no technical details.
    Column(Modifier.fillMaxSize().screenBackdrop("LoginScreen1").statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) { IconBtn(Ic.Close, { leave() }) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            PixelWordmark(Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(32.dp))
            Rise(shown, 1) { Heading("Code and chat, right on your phone", size = 28.sp, align = TextAlign.Center) }
            Spacer(Modifier.height(12.dp))
            Rise(shown, 2) {
                Txt("Free models work without an account. Sign in to add more models — your keys and memory stay on this phone.", size = 16.sp, color = c.textDim, align = TextAlign.Center, lineHeight = 23.sp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Rise(shown, 3) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(Oc.pill).background(c.text).tap(onClick = { leave() }),
                    contentAlignment = Alignment.Center,
                ) { Txt("Start without an account", size = 17.sp, color = c.bg, weight = FontWeight.Medium) }
            }
            freeProviders.filter { it.id == "github-copilot" }.forEachIndexed { i, p ->
                Rise(shown, 4 + i) {
                    ProviderButton("Continue with ${p.title}", p.note, when (p.id) { "github-copilot" -> Brand.GitHub; else -> Brand.Key }) { nav.push(Route.Connect(p.id)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            Box(Modifier.clip(CircleShape).clickable { nav.push(Route.ConsoleLogin("")) }.padding(horizontal = 14.dp, vertical = 12.dp)) { Txt("OpenCode account", size = 15.sp, color = c.textDim) }
            Box(Modifier.clip(CircleShape).clickable { nav.push(Route.Providers) }.padding(horizontal = 14.dp, vertical = 12.dp)) { Txt("Other providers", size = 15.sp, color = c.textDim) }
        }
    }
}

@Composable
private fun Rise(shown: Boolean, step: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        shown,
        enter = fadeIn(tween(220, delayMillis = 50 * step)) + slideInVertically(tween(260, delayMillis = 50 * step)) { it / 5 },
    ) { content() }
}

private enum class Brand { GitHub, Email, Key }

@Composable
private fun ProviderButton(text: String, note: String, brand: Brand, onClick: () -> Unit) {
    val c = Oc.c
    Row(
        Modifier.fillMaxWidth().clip(Oc.pill).border(1.dp, c.border, Oc.pill).background(c.surface)
            .tap(onClick = onClick).height(64.dp).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            when (brand) {
                Brand.GitHub -> Ico(Ic.GitHub, size = 28.dp)
                Brand.Email -> Ico(Ic.Mail, size = 26.dp)
                Brand.Key -> Ico(Ic.Key, size = 26.dp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Txt(text, size = 18.sp)
            Txt(note, size = 13.sp, color = c.textDim)
        }
    }
}
