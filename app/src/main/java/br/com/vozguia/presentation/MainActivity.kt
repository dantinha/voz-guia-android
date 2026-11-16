package br.com.vozguia.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.vozguia.VozGuiaApplication
import br.com.vozguia.core.caregiver.AndroidMotorolaPermissionProvider
import br.com.vozguia.core.caregiver.CaregiverDashboardViewModel
import br.com.vozguia.core.voice.tts.AndroidTextToSpeechSpeaker
import br.com.vozguia.core.voice.wakeword.VoiceRuntimeBridge
import br.com.vozguia.presentation.caregiver.CaregiverDashboardScreen
import br.com.vozguia.service.voice.VoiceWakeWordService

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CaregiverDashboardViewModel
    private lateinit var previewTtsSpeaker: AndroidTextToSpeechSpeaker

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshState()
        syncWakeServiceState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val app = application as VozGuiaApplication
        val permissionProvider = AndroidMotorolaPermissionProvider(this)
        previewTtsSpeaker = AndroidTextToSpeechSpeaker(this)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CaregiverDashboardViewModel(
                    contactRepository = app.contactRepository,
                    reminderRepository = app.reminderRepository,
                    favoriteRepository = app.favoriteRepository,
                    queryRepository = app.queryRepository,
                    preferenceRepository = app.preferenceRepository,
                    permissionProvider = permissionProvider,
                    contactHabitRepository = app.contactHabitRepository
                ) as T
            }
        }

        viewModel = ViewModelProvider(this, factory)[CaregiverDashboardViewModel::class.java]
        syncWakeServiceState()

        setContent {
            MaterialTheme {
                Scaffold(
                    containerColor = Color(0xFF121212),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CaregiverDashboardScreen(
                        viewModel = viewModel,
                        onPreviewTts = { speed, text ->
                            previewTtsSpeaker.speak(
                                text = text,
                                speechRate = speed,
                                pitch = 1.0f,
                                onDone = null
                            )
                        },
                        onRequestRuntimePermissions = {
                            requestCoreRuntimePermissions()
                        },
                        onSyncWakeService = { enabled ->
                            if (enabled) {
                                syncWakeServiceState(forceStart = true)
                            } else {
                                try {
                                    VoiceWakeWordService.stop(this@MainActivity)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::previewTtsSpeaker.isInitialized) {
            previewTtsSpeaker.shutdown()
        }
    }

    private fun requestCoreRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        runtimePermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun syncWakeServiceState(forceStart: Boolean = false) {
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val wakeEnabled = forceStart || VoiceRuntimeBridge.config.value.wakeWordEnabled
        if (hasMic && wakeEnabled) {
            try {
                VoiceWakeWordService.start(this)
            } catch (_: Exception) {}
        }
    }
}
