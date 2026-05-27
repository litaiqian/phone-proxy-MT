package top.ipla.phone_proxy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import top.ipla.phone_proxy.data.ApiClient
import top.ipla.phone_proxy.data.LoginRequest
import top.ipla.phone_proxy.service.ProxyForegroundService
import top.ipla.phone_proxy.ui.navigation.AppNavGraph
import top.ipla.phone_proxy.ui.theme.PhoneProxyTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动即隧道（无需登录，WebSocket 注册只发 device_id + name）
        startProxyService()

        // 尝试自动登录（验证/恢复 token）
        checkAutoLogin()

        setContent {
            PhoneProxyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(navController)
                }
            }
        }
    }

    private fun checkAutoLogin() {
        CoroutineScope(Dispatchers.IO).launch {
            var token: String? = null
            PhoneProxyApp.instance.prefs.token.collect { token = it }
            if (!token.isNullOrEmpty()) {
                ApiClient.token = token!!
                try {
                    // 验证 token 是否有效（用 bind_status）
                    val r = ApiClient.service.getBindStatus()
                    if (r.ok == false) {
                        // Token 已失效
                        PhoneProxyApp.instance.prefs.clearAuth()
                        ApiClient.token = ""
                    }
                } catch (_: Exception) {
                    // 网络不通，保留 token 继续尝试
                }
            }
        }
    }

    fun startProxyService() {
        val intent = Intent(this, ProxyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
