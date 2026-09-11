package com.melo.music.ui.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.melo.music.auth.YouTubeAccountManager

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleLoginBottomSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLoading by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141916),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(Color(0xFF141916))
        ) {
            // Верхняя плашка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Вход в Google / YouTube Music",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Для доступа к трекам 18+ и вашей медиатеке",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            // User Agent Chrome для нормального Google Auth
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false

                                    val currentUrl = url ?: return
                                    val cookies = cookieManager.getCookie(currentUrl) ?: ""

                                    // Проверяем наличие ключевых авторизационных cookie YouTube
                                    val hasSapisid = cookies.contains("SAPISID=") || cookies.contains("__Secure-3PAPISID=")
                                    val hasSid = cookies.contains("SID=") || cookies.contains("LOGIN_INFO=")

                                    if (hasSapisid && hasSid) {
                                        // Забираем все cookies для music.youtube.com
                                        val fullCookies = cookieManager.getCookie("https://music.youtube.com") ?: cookies
                                        YouTubeAccountManager.saveSession(fullCookies)
                                        onSuccess()
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }
                            }

                            // Стартуем вход на YouTube Music
                            loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F")
                        }
                    }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
