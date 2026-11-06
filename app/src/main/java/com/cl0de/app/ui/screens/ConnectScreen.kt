package com.cl0de.app.ui.screens

import com.cl0de.app.ui.theme.screenBackdrop
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cl0de.app.Graph
import com.cl0de.app.data.S
import com.cl0de.app.data.SAcct
import com.cl0de.app.api.str
import com.cl0de.app.ui.Nav
import com.cl0de.app.ui.Route
import com.cl0de.app.ui.components.Ico
import com.cl0de.app.ui.components.IconBtn
import com.cl0de.app.ui.components.OcTextField
import com.cl0de.app.ui.components.Pill
import com.cl0de.app.ui.components.Spinner
import com.cl0de.app.ui.components.Txt
import com.cl0de.app.ui.components.tap
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Providers with a free tier: sign in with your account on their site, then the key reaches the app by itself. */
class FreeProvider(val id: String, val title: String, val note: String, val url: String, val keyRegex: Regex?, val where: String)

val freeProviders get() = listOf(
    FreeProvider("github-copilot", "GitHub", SAcct.copilotFreePlan, "", null, "GitHub"),
    FreeProvider("groq", "Groq", SAcct.groqFreeNote, "https://console.groq.com/keys", Regex("gsk_[A-Za-z0-9]{30,}"), "Groq Console"),
    FreeProvider("openrouter", "OpenRouter", SAcct.openRouterFreeNote, "https://openrouter.ai/settings/keys", Regex("sk-or-[A-Za-z0-9_\\-]{20,}"), "OpenRouter"),
)

/**
 * Connecting a provider without manual copying: GitHub signs in with a device code (the server waits for confirmation itself),
 * the others sign in on the site, tap "Copy" next to the key and return to the app: the key is picked up from the clipboard.
 */
@Composable
fun ConnectScreen(providerId: String, nav: Nav) {
    val c = Oc.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val fp = freeProviders.firstOrNull { it.id == providerId } ?: freeProviders.first()
    var state by remember { mutableStateOf("wait") }            // wait | saving | done | error
    var error by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf<String?>(null) }       // GitHub device code
    var manual by remember { mutableStateOf("") }
    var handled by remember { mutableStateOf(false) }
    val device = fp.keyRegex == null

    fun open(url: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun done(method: String) {
        Graph.prefs.signedIn = method
        Graph.prefs.loginSeen = true
        state = "done"
        scope.launch {
            runCatching { Graph.api.disposeAll() }
            runCatching { Graph.catalog.refresh() }
            Graph.catalog.selectBestFor(providerId)
            delay(1400); nav.root(Route.Home)
        }
    }

    fun acceptKey(key: String) {
        if (handled) return
        handled = true; state = "saving"
        scope.launch {
            try { Graph.api.setApiKey(providerId, key.trim()); done("key") }
            catch (e: Exception) { error = e.message ?: SAcct.couldNotSaveKey; state = "error"; handled = false }
        }
    }

    if (device) {
        LaunchedEffect(Unit) {
            try {
                val r = Graph.api.oauthAuthorize(providerId, 0, mapOf("deploymentType" to "github.com"))
                val instr = r.str("instructions").orEmpty()
                code = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}").find(instr)?.value ?: instr
                val url = r.str("url") ?: "https://github.com/login/device"
                open(url)
                Graph.api.oauthCallback(providerId, 0, null)           // waiting for confirmation in the browser
                runCatching { com.cl0de.app.data.FreeModels.enableCopilot(select = false) }
                done("github")
            } catch (e: Exception) { error = e.message ?: SAcct.githubDidNotConfirm; state = "error" }
        }
    } else {
        LaunchedEffect(Unit) { open(fp.url) }
        val owner = LocalLifecycleOwner.current
        DisposableEffect(owner) {
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    fp.keyRegex?.find(text)?.let { acceptKey(it.value) }
                }
            }
            owner.lifecycle.addObserver(obs)
            onDispose { owner.lifecycle.removeObserver(obs) }
        }
    }

    Box(Modifier.fillMaxSize().screenBackdrop("ConnectScreen1")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Ic.Close, { nav.pop() })
                Txt(fp.title, Modifier.weight(1f), size = 20.sp, weight = FontWeight.Medium, align = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (device) {
                    Txt(SAcct.enterCodeOnGitHub, size = 24.sp, weight = FontWeight.Medium, align = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    if (code == null) Spinner(34.dp) else {
                        val clip = LocalClipboardManager.current
                        Box(Modifier.clip(Oc.round(24.dp)).background(c.surface).border(1.dp, c.border, Oc.round(24.dp)).tap { clip.setText(AnnotatedString(code!!)); Graph.chats.toasts.tryEmit(SAcct.codeCopied) }.padding(horizontal = 32.dp, vertical = 22.dp)) {
                            Txt(code!!, size = 36.sp, family = Oc.mono, weight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(18.dp))
                        Txt(SAcct.githubOpenedHint, size = 16.sp, color = c.textDim, align = TextAlign.Center, lineHeight = 23.sp)
                        Spacer(Modifier.height(20.dp)); Spinner(28.dp)
                        Spacer(Modifier.height(20.dp))
                        Txt(SAcct.freeAccessCopilot, size = 14.sp, color = c.textDim, align = TextAlign.Center)
                    }
                } else {
                    Spinner(34.dp)
                    Spacer(Modifier.height(26.dp))
                    Txt(SAcct.signInWithTitle(fp.title), size = 24.sp, weight = FontWeight.Medium, align = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Txt(
                        SAcct.signInInstructions(fp.where),
                        size = 16.sp, color = c.textDim, lineHeight = 25.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.clip(CircleShape).border(1.dp, c.border, CircleShape).tap { open(fp.url) }.padding(horizontal = 22.dp, vertical = 14.dp)) { Txt(SAcct.openAgain(fp.where), size = 16.sp) }
                    Spacer(Modifier.height(28.dp))
                    Txt(SAcct.orPasteKeyHere, size = 14.sp, color = c.textDim)
                    Spacer(Modifier.height(8.dp))
                    OcTextField(manual, { manual = it }, placeholder = SAcct.apiKey, password = true, mono = true)
                    Spacer(Modifier.height(10.dp))
                    Pill(SAcct.connect, { acceptKey(manual) }, enabled = manual.length > 12, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        AnimatedVisibility(state != "wait", enter = fadeIn(tween(250))) {
            Box(Modifier.fillMaxSize().screenBackdrop("ConnectScreen2"), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state) {
                        "saving" -> { Spinner(40.dp); Spacer(Modifier.height(20.dp)); Txt(SAcct.connecting, size = 18.sp, color = c.textDim) }
                        "done" -> {
                            Box(Modifier.size(72.dp).clip(CircleShape).background(c.ok), contentAlignment = Alignment.Center) { Ico(Ic.Check, tint = c.bg, size = 40.dp) }
                            Spacer(Modifier.height(20.dp)); Txt(SAcct.providerConnected(fp.title), size = 22.sp, weight = FontWeight.Medium)
                            Txt(Graph.catalog.selectedLabel(), Modifier.padding(top = 6.dp), size = 15.sp, color = c.textDim)
                        }
                        else -> {
                            Txt(error ?: SAcct.somethingWentWrong, size = 17.sp, color = c.danger, align = TextAlign.Center, modifier = Modifier.padding(32.dp))
                            Box(Modifier.clip(CircleShape).border(1.dp, c.border, CircleShape).tap { nav.pop() }.padding(horizontal = 22.dp, vertical = 14.dp)) { Txt(S.back, size = 16.sp) }
                        }
                    }
                }
            }
        }
    }
}
