package com.cl0de.app.ui.components

import com.cl0de.app.ui.theme.Oc
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.cl0de.app.Graph
import com.cl0de.app.data.SChat
import com.cl0de.app.ui.theme.Ic
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** What to give Coil: guest Linux files live inside the rootfs on the host. */
fun imageModel(url: String): Any =
    if (url.startsWith("file://")) Graph.paths.host(Uri.parse(url).path ?: "") else url

fun imageBytes(url: String): ByteArray? = runCatching {
    when {
        url.startsWith("data:") -> Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT)
        url.startsWith("file://") -> Graph.paths.host(Uri.parse(url).path ?: "").readBytes()
        else -> null
    }
}.getOrNull()

private val imageHttp = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

/**
 * Bytes plus mime for any image URL the viewer can show, remote ones included
 * (generated pictures live on a third-party host). Blocking: call off the
 * main thread. The mime comes from the response when there is one, so a JPEG
 * is never saved labelled as PNG.
 */
private fun fetchImage(url: String): Pair<ByteArray, String>? {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        return imageBytes(url)?.let { it to mimeOf(url) }
    }
    return runCatching {
        imageHttp.newCall(Request.Builder().url(url).build()).execute().use { r ->
            val body = r.body
            if (!r.isSuccessful || body == null) null
            else {
                val bytes = body.bytes()
                val ct = body.contentType()
                bytes to (ct?.let { "${it.type}/${it.subtype}" } ?: mimeOf(url))
            }
        }
    }.getOrNull()
}

@Composable
fun ImageThumb(url: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageModel(url), contentDescription = null, contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(Oc.round(14.dp)).background(Color(0x22888888)).clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewer(urls: List<String>, start: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (urls.size - 1).coerceAtLeast(0))) { urls.size }
    var chrome by remember { mutableStateOf(true) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val tState = rememberTransformableState { zoom, pan, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { chrome = !chrome },
                                onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f },
                            )
                        }
                        .transformable(tState, canPan = { scale > 1f })
                        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(imageModel(urls[page]), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            if (chrome) {
                Row(Modifier.fillMaxSize().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.Top) {
                    IconBtn(Ic.Close, onDismiss, tint = Color.White, bg = Color(0x66000000))
                    Box(Modifier.weight(1f))
                    IconBtn(Ic.External, { shareImage(ctx, urls[pager.currentPage]) }, tint = Color.White, bg = Color(0x66000000))
                    Box(Modifier.padding(start = 8.dp))
                    if (Build.VERSION.SDK_INT >= 29) IconBtn(Ic.Download, { saveImage(ctx, urls[pager.currentPage]) }, tint = Color.White, bg = Color(0x66000000))
                }
                if (urls.size > 1) {
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)) {
                        Txt("${pager.currentPage + 1} / ${urls.size}", color = Color.White, size = 14.sp)
                    }
                }
            }
        }
    }
}

private fun mimeOf(url: String) = if (url.startsWith("data:")) url.substringAfter("data:").substringBefore(';') else "image/png"

/**
 * Saves the shown image into Pictures/OpenCode through MediaStore (the
 * viewer only offers this on API 29+, where that needs no permission).
 * Remote bytes are fetched on IO; the user always gets a toast, success
 * or failure, instead of the old silent no-op for http URLs.
 */
private fun saveImage(ctx: Context, url: String) {
    Graph.scope.launch {
        val ok = withContext(Dispatchers.IO) {
            val got = fetchImage(url)
            if (got == null) false
            else {
                val (bytes, mime) = got
                val ext = mime.substringAfter('/').substringBefore(';').replace("jpeg", "jpg")
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "opencode-${System.currentTimeMillis()}.$ext")
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OpenCode")
                }
                runCatching {
                    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    val out = uri?.let { ctx.contentResolver.openOutputStream(it) }
                    if (out == null) false else { out.use { it.write(bytes) }; true }
                }.getOrDefault(false)
            }
        }
        Toast.makeText(ctx, if (ok) SChat.savedToPictures else SChat.imageSaveFailed, Toast.LENGTH_SHORT).show()
    }
}

private fun shareImage(ctx: Context, url: String) {
    Graph.scope.launch {
        val got = withContext(Dispatchers.IO) { fetchImage(url) }
        if (got == null) {
            Toast.makeText(ctx, SChat.imageSaveFailed, Toast.LENGTH_SHORT).show()
            return@launch
        }
        val (bytes, mime) = got
        val ext = mime.substringAfter('/').substringBefore(';').replace("jpeg", "jpg")
        val f = withContext(Dispatchers.IO) {
            runCatching {
                File(ctx.cacheDir, "camera").apply { mkdirs() }.let { File(it, "share-${System.currentTimeMillis()}.$ext") }
                    .also { it.writeBytes(bytes) }
            }.getOrNull()
        }
        if (f == null) {
            Toast.makeText(ctx, SChat.imageSaveFailed, Toast.LENGTH_SHORT).show()
            return@launch
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                null,
            )
        )
    }
}
