package com.cl0de.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cl0de.app.ui.OcRoot
import com.cl0de.app.ui.theme.OcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // no saved state: the app was started fresh, not brought back from the background
        if (savedInstanceState == null) com.cl0de.app.ui.NavMemory.startFresh()
        setContent { OcTheme { OcRoot() } }
        handle(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: android.content.Intent?) {
        // Cl0de://connect?… — a QR code from a computer, opened by the camera or as a link
        if (i?.action == android.content.Intent.ACTION_VIEW) i.dataString?.let { com.cl0de.app.data.ConnectLink.offer(it) }
        i?.getStringExtra("session")?.let { Graph.chats.openRequest.value = it }
        if (i?.action == android.content.Intent.ACTION_SEND || i?.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            com.cl0de.app.data.ShareInbox.take(this, i)
            Graph.chats.openRequest.value = "route:home"
        }
        // debug build only: adb am start --es dev_edition kimi --es dev_route settings|home|drawer
        if (BuildConfig.DEBUG) {
            i?.getStringExtra("dev_edition")?.let { Graph.prefs.edition = it }
            i?.getStringExtra("dev_route")?.let { Graph.chats.openRequest.value = "route:$it" }
            i?.getStringExtra("dev_holiday")?.let { com.cl0de.app.ui.components.MascotSignals.forced = com.cl0de.app.ui.components.Holiday.parse(it) }
        }
    }

    override fun onStart() { super.onStart(); Graph.chats.foreground = true; com.cl0de.app.data.LiveNotify.onForeground() }
    override fun onStop() {
        super.onStop(); Graph.chats.foreground = false; com.cl0de.app.data.LiveNotify.onBackground()
        // the offline-model server releases its model when the app leaves the foreground
        Graph.runtime.stopLlama()
    }

    override fun onResume() {
        super.onResume()
        // Android may have killed the server in the background: start it again unless it was stopped by hand.
        val s = Graph.runtime.state.value
        if (Graph.prefs.autoStartServer && !Graph.runtime.userStopped && (s is com.cl0de.app.runtime.RuntimeState.Crashed || s is com.cl0de.app.runtime.RuntimeState.Stopped)) Graph.runtime.startServer()
    }
}
