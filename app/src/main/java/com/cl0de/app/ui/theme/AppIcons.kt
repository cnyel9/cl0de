package com.cl0de.app.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.cl0de.app.R

class AppIcon(val id: String, val label: String, val component: String, val bg: Color, val fgRes: Int)

/** The app's icon set: each one is a separate activity-alias in the manifest. */
object AppIcons {
    val all = listOf(
    // for the interface looks
    AppIcon("look_cloud", "Cl0de", "com.cl0de.app.IconLookCloud", Color(0xFF232428), R.drawable.ic_fg_look_cloud),
    AppIcon("cloud_light", "Cl0de · light", "com.cl0de.app.IconCloudLight", Color(0xFFF4F5F7), R.drawable.ic_fg_cloud_light),
    AppIcon("look_claude", "Claude", "com.cl0de.app.IconLookClaude", Color(0xFFD97757), R.drawable.ic_fg_look_claude),
    AppIcon("look_terminal", "OpenCode · Terminal", "com.cl0de.app.IconLookTerminal", Color(0xFF0A0A0A), R.drawable.ic_fg_look_terminal),
    AppIcon("look_desktop", "OpenCode · Desktop", "com.cl0de.app.IconLookDesktop", Color(0xFF1E1E1E), R.drawable.ic_fg_look_desktop),
    AppIcon("look_hairline", "Hairline", "com.cl0de.app.IconLookHairline", Color(0xFF000000), R.drawable.ic_fg_look_hairline),
    AppIcon("look_chatgpt", "ChatGPT", "com.cl0de.app.IconLookChatgpt", Color(0xFF000000), R.drawable.ic_fg_look_chatgpt),
    AppIcon("look_kimi", "Kimi", "com.cl0de.app.IconLookKimi", Color(0xFF111111), R.drawable.ic_fg_look_kimi),
    AppIcon("look_notebook", "Notebook", "com.cl0de.app.IconLookNotebook", Color(0xFFF7F3EA), R.drawable.ic_fg_look_notebook),
    // colored
    AppIcon("terracotta", "Terracotta", "com.cl0de.app.IconTerracotta", Color(0xFFDE7356), R.drawable.ic_fg_terracotta),
    AppIcon("classic", "Classic", "com.cl0de.app.IconClassic", Color(0xFF0E0F13), R.drawable.ic_fg_classic),
    AppIcon("blue", "Blue", "com.cl0de.app.IconBlue", Color(0xFF4A7DFB), R.drawable.ic_fg_blue),
    AppIcon("cream", "Cream", "com.cl0de.app.IconCream", Color(0xFFF3ECE0), R.drawable.ic_fg_cream),
    AppIcon("cream_blue", "Cream & blue", "com.cl0de.app.IconCreamBlue", Color(0xFFF3ECE0), R.drawable.ic_fg_cream_blue),
    AppIcon("graphite", "Graphite", "com.cl0de.app.IconGraphite", Color(0xFF1C1D22), R.drawable.ic_fg_graphite),
    AppIcon("green", "Green", "com.cl0de.app.IconGreen", Color(0xFF3F8F6B), R.drawable.ic_fg_green),
    AppIcon("mustard", "Mustard", "com.cl0de.app.IconMustard", Color(0xFFE5A93D), R.drawable.ic_fg_mustard),
    AppIcon("pink", "Pink", "com.cl0de.app.IconPink", Color(0xFFE46A8F), R.drawable.ic_fg_pink),
    AppIcon("teal", "Teal", "com.cl0de.app.IconTeal", Color(0xFF2B8A96), R.drawable.ic_fg_teal),
    AppIcon("violet", "Violet", "com.cl0de.app.IconViolet", Color(0xFF7B61D9), R.drawable.ic_fg_violet),
    )

    const val DEFAULT = "look_cloud"

    /** Enables the chosen alias and disables the rest; the app doesn't restart. */
    fun apply(ctx: Context, id: String) {
        val pm = ctx.packageManager
        // Enable the new one first, then disable the old ones: otherwise for a moment there's no entry point at all.
        all.sortedByDescending { it.id == id }.forEach { icon ->
            val state = if (icon.id == id) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(ComponentName(ctx, icon.component), state, PackageManager.DONT_KILL_APP)
        }
    }
}
