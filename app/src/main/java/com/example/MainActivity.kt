package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AndroidHardwareAudioBridge
import com.example.data.DashboardPreferencesRepository
import com.example.data.GoogleMapsNavigationService
import com.example.data.VoiceGuidanceSpeaker
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.YezdiDashTheme
import com.example.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(
            repository = DashboardPreferencesRepository(applicationContext),
            audioBridge = AndroidHardwareAudioBridge(applicationContext),
            voiceSpeaker = VoiceGuidanceSpeaker(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.launchMapsEvent.collect { destination ->
                    GoogleMapsNavigationService.launchGoogleMapsTurnByTurn(this@MainActivity, destination)
                }
            }

            LaunchedEffect(uiState.screenBrightness) {
                val lp = window.attributes
                lp.screenBrightness = uiState.screenBrightness.coerceIn(0.15f, 1.0f)
                window.attributes = lp
            }

            LaunchedEffect(uiState.isNightMode) {
                if (uiState.isNightMode) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        ),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    )
                }
            }

            YezdiDashTheme(isNightMode = uiState.isNightMode) {
                DashboardScreen(
                    state = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * Intercepts physical/Bluetooth motorcycle handlebar navigation controller keys
     * (D-Pad 5-way joystick, bar buttons, media & headset keys) for 100% hands-free operation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.onSwitchgearUp()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.onSwitchgearDown()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.onSwitchgearLeft()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.onSwitchgearRight()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (event.isLongPress) {
                        viewModel.onSwitchgearSetLongPress()
                    } else if (event.repeatCount == 0) {
                        viewModel.onSwitchgearSet()
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    viewModel.onMusicPlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    viewModel.onMusicNext()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    viewModel.onMusicPrev()
                    return true
                }
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_CALL -> {
                    viewModel.onAttendCall()
                    return true
                }
                KeyEvent.KEYCODE_M -> {
                    viewModel.onCycleRideMode()
                    return true
                }
                KeyEvent.KEYCODE_H -> {
                    viewModel.onToggleHazard()
                    return true
                }
                KeyEvent.KEYCODE_N -> {
                    viewModel.onOpenDestinationPicker()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
