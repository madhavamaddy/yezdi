package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardUiState
import com.example.model.RideMode
import com.example.model.TurnSignalState
import com.example.ui.theme.HazardRed
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.YezdiCrimson

@Composable
fun TellTaleBar(
    state: DashboardUiState,
    onOpenEcuDrawer: () -> Unit,
    onToggleDayNight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val leftActive = state.ignitionOn && state.blinkOn &&
        (state.turnSignal == TurnSignalState.LEFT || state.turnSignal == TurnSignalState.HAZARD)
    val rightActive = state.ignitionOn && state.blinkOn &&
        (state.turnSignal == TurnSignalState.RIGHT || state.turnSignal == TurnSignalState.HAZARD)

    val leftTint by animateColorAsState(
        targetValue = if (leftActive) NeutralEmerald else palette.textMuted.copy(alpha = 0.35f),
        animationSpec = tween(120),
        label = "leftTurnTint",
    )
    val rightTint by animateColorAsState(
        targetValue = if (rightActive) NeutralEmerald else palette.textMuted.copy(alpha = 0.35f),
        animationSpec = tween(120),
        label = "rightTurnTint",
    )

    val absLampOn = state.isBooting ||
        state.absSlipActive ||
        state.rideMode == RideMode.OFF_ROAD

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bezel)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left Turn Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Left turn indicator",
            tint = leftTint,
            modifier = Modifier
                .size(24.dp)
                .testTag("telltale_left_turn"),
        )

        // Center Tell-Tale Lamps
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Neutral Lamp
            TellTalePill(
                label = "N",
                active = state.ignitionOn && (state.gear == 0 || state.isBooting),
                activeColor = NeutralEmerald,
                testTag = "telltale_neutral",
            )

            // ABS Lamp
            TellTalePill(
                label = if (state.rideMode == RideMode.OFF_ROAD) "ABS OFF" else "ABS",
                active = state.ignitionOn && absLampOn,
                activeColor = if (state.absSlipActive) YezdiCrimson else RallyAmber,
                testTag = "telltale_abs",
            )

            // Side Stand Lamp
            if (state.sideStandDown || state.isBooting) {
                TellTalePill(
                    label = "STAND",
                    active = true,
                    activeColor = RallyAmber,
                    testTag = "telltale_stand",
                )
            }

            // Hazard Lamp
            if (state.turnSignal == TurnSignalState.HAZARD) {
                TellTaleIconLamp(
                    icon = Icons.Default.Warning,
                    contentDescription = "Hazard warning active",
                    active = state.blinkOn,
                    activeColor = HazardRed,
                    testTag = "telltale_hazard",
                )
            }

            // Low Fuel Lamp
            if (state.isLowFuel || state.isBooting) {
                TellTaleIconLamp(
                    icon = Icons.Default.LocalGasStation,
                    contentDescription = "Low fuel warning",
                    active = true,
                    activeColor = RallyAmber,
                    testTag = "telltale_fuel",
                )
            }

            // Coolant Overheat Lamp
            if (state.isHighTemp || state.isBooting) {
                TellTaleIconLamp(
                    icon = Icons.Default.Thermostat,
                    contentDescription = "Coolant high temperature warning",
                    active = true,
                    activeColor = YezdiCrimson,
                    testTag = "telltale_temp",
                )
            }

            // Low Battery Lamp
            if (state.isLowBattery || state.isBooting) {
                TellTaleIconLamp(
                    icon = Icons.Default.BatteryAlert,
                    contentDescription = "Low battery voltage warning",
                    active = true,
                    activeColor = YezdiCrimson,
                    testTag = "telltale_battery",
                )
            }

            // Bluetooth & Day/Night quick indicator
            Icon(
                imageVector = Icons.Default.BluetoothConnected,
                contentDescription = "Bluetooth helmet link active",
                tint = RainCyan,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onToggleDayNight() },
            )

            // ECU / Sensor Simulator Drawer Button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                    .clickable { onOpenEcuDrawer() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("ecu_drawer_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Open ECU sensor controls",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "ECU",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
            }
        }

        // Right Turn Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Right turn indicator",
            tint = rightTint,
            modifier = Modifier
                .size(24.dp)
                .testTag("telltale_right_turn"),
        )
    }
}

@Composable
private fun TellTalePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    testTag: String,
) {
    val palette = LocalCockpitPalette.current
    val bgColor = if (active) activeColor.copy(alpha = 0.2f) else palette.card
    val borderColor = if (active) activeColor else palette.border
    val textColor = if (active) activeColor else palette.textMuted.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun TellTaleIconLamp(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color,
    testTag: String,
) {
    val palette = LocalCockpitPalette.current
    val tint = if (active) activeColor else palette.textMuted.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) activeColor.copy(alpha = 0.18f) else Color.Transparent)
            .padding(3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}
