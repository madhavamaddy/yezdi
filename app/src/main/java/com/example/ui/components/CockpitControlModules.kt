package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CallStatus
import com.example.model.DashboardUiState
import com.example.model.SwitchgearFocus
import com.example.ui.theme.HazardRed
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.YezdiCrimson
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CallHudBanner(
    state: DashboardUiState,
    onAttendCall: () -> Unit,
    onDeclineOrEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.callState.status != CallStatus.IDLE,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        val isIncoming = state.callState.status == CallStatus.INCOMING
        val accent = if (isIncoming) NeutralEmerald else RainCyan
        val mins = state.callState.durationSeconds / 60
        val secs = state.callState.durationSeconds % 60

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.5.dp, accent, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("call_hud_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Bluetooth call",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isIncoming) {
                            "INCOMING CALL • PRESS SET TO ATTEND"
                        } else {
                            String.format(Locale.US, "CALL CONNECTED • %02d:%02d", mins, secs)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                    Text(
                        text = state.callState.callerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalCockpitPalette.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.callState.callerNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalCockpitPalette.current.textSecondary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isIncoming) {
                    Button(
                        onClick = onAttendCall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeutralEmerald,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("attend_call_button"),
                    ) {
                        Text("ATTEND", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Button(
                    onClick = onDeclineOrEndCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazardRed,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("decline_end_call_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = if (isIncoming) "Decline call" else "End call",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CockpitControlModules(
    state: DashboardUiState,
    onSelectFocus: (SwitchgearFocus) -> Unit,
    onPlayPause: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onAdjustVolume: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleDayNight: () -> Unit,
    onTriggerCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val musicFocused = state.focusedModule == SwitchgearFocus.MUSIC
    val brightnessFocused = state.focusedModule == SwitchgearFocus.BRIGHTNESS
    val callFocused = state.focusedModule == SwitchgearFocus.CALL_HUD

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Music System Control Card
        val track = state.musicState.currentTrack
        val progress = (state.musicState.elapsedSeconds.toFloat() / track.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (musicFocused) palette.cardFocused else palette.card)
                .border(
                    width = if (musicFocused) 1.5.dp else 1.dp,
                    color = if (musicFocused) RainCyan else palette.border,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable { onSelectFocus(SwitchgearFocus.MUSIC) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("music_module_card"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Bluetooth media player",
                        tint = RainCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("music_track_title"),
                        )
                        Text(
                            text = "${track.artist} • VOL ${state.musicState.volumePercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustVolume(-10) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("music_vol_down"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Decrease music volume",
                            tint = palette.textSecondary,
                        )
                    }
                    IconButton(
                        onClick = onPrevTrack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("music_prev"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = palette.textPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(RainCyan)
                            .clickable { onPlayPause() }
                            .padding(8.dp)
                            .testTag("music_play_pause"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.musicState.isPlaying) "Pause music" else "Play music",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onNextTrack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("music_next"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next track",
                            tint = palette.textPrimary,
                        )
                    }
                    IconButton(
                        onClick = { onAdjustVolume(10) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("music_vol_up"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Increase music volume",
                            tint = palette.textSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = RainCyan,
                trackColor = palette.segmentInactive,
            )
        }

        // Brightness + Call Trigger Dual Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Screen Brightness Control Card
            Column(
                modifier = Modifier
                    .weight(1.45f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (brightnessFocused) palette.cardFocused else palette.card)
                    .border(
                        width = if (brightnessFocused) 1.5.dp else 1.dp,
                        color = if (brightnessFocused) RallyAmber else palette.border,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelectFocus(SwitchgearFocus.BRIGHTNESS) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = "Screen brightness",
                            tint = RallyAmber,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "BRIGHTNESS ${(state.screenBrightness * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            modifier = Modifier.testTag("brightness_readout"),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.bezel)
                            .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                            .clickable { onToggleDayNight() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("day_night_toggle"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = if (state.isNightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle day or night TFT mode",
                                tint = RallyAmber,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = if (state.isNightMode) "NIGHT" else "DAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                            )
                        }
                    }
                }

                Slider(
                    value = state.screenBrightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.15f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = RallyAmber,
                        activeTrackColor = RallyAmber,
                        inactiveTrackColor = palette.segmentInactive,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("brightness_slider"),
                )
            }

            // Call Attend Trigger Card
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (callFocused) palette.cardFocused else palette.card)
                    .border(
                        width = if (callFocused) 1.5.dp else 1.dp,
                        color = if (callFocused) NeutralEmerald else palette.border,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onTriggerCall() }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("call_trigger_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Simulate incoming helmet call",
                    tint = NeutralEmerald,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (state.callState.status) {
                        CallStatus.IDLE -> "BT CALL"
                        CallStatus.INCOMING -> "RINGING"
                        CallStatus.ACTIVE -> "IN CALL"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                )
                Text(
                    text = "TAP / TOGGLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuSimulatorSheet(
    state: DashboardUiState,
    onDismiss: () -> Unit,
    onAdjustFuel: (Float) -> Unit,
    onSetBattery: (Float?) -> Unit,
    onSetTemp: (Int?) -> Unit,
    onTriggerCall: () -> Unit,
) {
    val palette = LocalCockpitPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.bezel,
        contentColor = palette.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "ECU TELEMETRY & SENSOR TEST BENCH",
                style = MaterialTheme.typography.titleMedium,
                color = YezdiCrimson,
            )

            // Fuel Level Adjust
            Column {
                Text(
                    text = "FUEL TANK (${(state.fuelPercent * 100).roundToInt()}% • ${String.format(Locale.US, "%.1fL", state.fuelLiters)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                )
                Slider(
                    value = state.fuelPercent,
                    onValueChange = onAdjustFuel,
                    valueRange = 0.04f..1.0f,
                )
            }

            // Battery Voltage presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BATTERY VOLTAGE TEST", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSetBattery(11.4f) }) { Text("LOW 11.4V") }
                    OutlinedButton(onClick = { onSetBattery(12.8f) }) { Text("IDLE 12.8V") }
                    OutlinedButton(onClick = { onSetBattery(null) }) { Text("AUTO 14.2V") }
                }
            }

            // Coolant Temp presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("COOLANT TEMPERATURE TEST", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSetTemp(108) }) { Text("HOT 108°C") }
                    OutlinedButton(onClick = { onSetTemp(85) }) { Text("NORM 85°C") }
                    OutlinedButton(onClick = { onSetTemp(null) }) { Text("AUTO") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        onTriggerCall()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeutralEmerald, contentColor = Color.Black),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("INCOMING CALL TEST")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("CLOSE")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
