package app.vidown.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.vidown.data.python.CookieHelper
import app.vidown.ui.theme.Theme

class LoginActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val siteName = intent.getStringExtra("site_name") ?: "YouTube"
        val loginUrl = when (siteName.lowercase()) {
            "youtube" -> "https://accounts.google.com/ServiceLogin?service=youtube"
            "instagram" -> "https://www.instagram.com/accounts/login/"
            "twitter", "x" -> "https://twitter.com/i/flow/login"
            "tiktok" -> "https://www.tiktok.com/login"
            else -> "https://www.google.com"
        }

        setContent {
            Theme {
                var webViewInstance by remember { mutableStateOf<WebView?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Log in to $siteName", style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    @Suppress("DEPRECATION")
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    val cookieManager = CookieManager.getInstance()
                                    val currentUrl = webViewInstance?.url ?: loginUrl
                                    val cookies = cookieManager.getCookie(currentUrl)
                                    if (cookies != null) {
                                        CookieHelper.saveNetscapeCookies(this@LoginActivity, siteName, cookies)
                                    }
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }) {
                                    Icon(imageVector = Icons.Default.Done, contentDescription = "Save Cookies")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewInstance = this

                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                    webChromeClient = android.webkit.WebChromeClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    @Suppress("DEPRECATION")
                                    settings.databaseEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true

                                    val siteLower = siteName.lowercase()
                                    settings.userAgentString = when (siteLower) {
                                        "youtube" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:119.0) Gecko/20100101 Firefox/119.0"
                                        "tiktok" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1"
                                        else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                            val urlStr = request?.url?.toString() ?: return false
                                            if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                                                return false
                                            }
                                            return try {
                                                val intent = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                                intent.component = null
                                                intent.selector = null
                                                context.startActivity(intent)
                                                true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                true
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            if (url != null) {
                                                val cookies = CookieManager.getInstance().getCookie(url)
                                                if (cookies != null && (cookies.contains("SID") || cookies.contains("sessionid") || cookies.contains("auth_token") || cookies.contains("sid"))) {
                                                    CookieHelper.saveNetscapeCookies(this@LoginActivity, siteName, cookies)
                                                }
                                            }
                                        }
                                    }
                                    loadUrl(loginUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
