package com.ryzumi.miraiai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.datastore.ThemeSettings
import com.ryzumi.miraiai.domain.util.ChatNotificationHelper
import com.ryzumi.miraiai.ui.navigation.MiraiNavGraph
import com.ryzumi.miraiai.ui.theme.MiraiAITheme

class MainActivity : ComponentActivity() {

    private var targetSessionId by mutableStateOf<String?>(null)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        targetSessionId = intent?.getStringExtra(ChatNotificationHelper.EXTRA_SESSION_ID)

        val settingsRepo = SettingsRepository(this)

        setContent {
            val themeSettings by settingsRepo.themeSettingsFlow.collectAsState(initial = ThemeSettings())

            MiraiAITheme(
                themeMode = themeSettings.themeMode,
                isMonetEnabled = themeSettings.isMonetEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MiraiNavGraph(initialSessionId = targetSessionId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newSessionId = intent.getStringExtra(ChatNotificationHelper.EXTRA_SESSION_ID)
        if (!newSessionId.isNullOrBlank()) {
            targetSessionId = newSessionId
        }
    }
}
