package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardUiState
import com.example.model.SwitchgearFocus
import com.example.ui.theme.BatteryBlue
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.YezdiCrimson
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TelemetryGrid(
    state: DashboardUiState,
    onToggleTrip: () -> Unit,
    onResetTrip: () -> Unit,
    onSelectTripFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Fuel + DTE | Temperature + Battery Voltage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FuelAndDteCard(
                state = state,
                modifier = Modifier.weight(1f),
            )
            ThermalAndBatteryCard(
                state = state,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Odometer & Active Trip A/B Card
        OdoAndTripCard(
            state = state,
            onToggleTrip = onToggleTrip,
            onResetTrip = onResetTrip,
            onSelectTripFocus = onSelectTripFocus,
        )
    }
}

@Composable
private fun FuelAndDteCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val fuelAccent = when {
        state.fuelPercent <= 0.12f -> YezdiCrimson
        state.isLowFuel -> RallyAmber
        else -> NeutralEmerald
    }
    val filledSegments = (state.fuelPercent * 8f).roundToInt().coerceIn(0, 8)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, if (state.isLowFuel) fuelAccent else palette.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
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
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = "Fuel gauge",
                    tint = fuelAccent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "FUEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
            Text(
                text = "${(state.fuelPercent * 100).roundToInt()}% • ${String.format(Locale.US, "%.1fL", state.fuelLiters)}",
                style = MaterialTheme.typography.labelMedium,
                color = fuelAccent,
                modifier = Modifier.testTag("fuel_readout"),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 8-segment Fuel Level Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (seg in 1..8) {
                val active = seg <= filledSegments
                val segColor = when {
                    !active -> palette.segmentInactive
                    seg <= 2 -> if (state.isLowFuel) YezdiCrimson else RallyAmber
                    else -> NeutralEmerald
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Distance To Empty (DTE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "DTE",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
            )
            Text(
                text = "${state.distanceToEmptyKm} KM",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp,
                color = if (state.distanceToEmptyKm < 45) RallyAmber else palette.textPrimary,
                modifier = Modifier.testTag("dte_readout"),
            )
        }
    }
}

@Composable
private fun ThermalAndBatteryCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val tempColor = when {
        state.isHighTemp -> YezdiCrimson
        state.coolantTempC >= 95 -> RallyAmber
        else -> RainCyan
    }
    val battColor = when {
        state.isLowBattery -> YezdiCrimson
        state.engineRunning -> NeutralEmerald
        else -> BatteryBlue
    }
    val tempBars = (((state.coolantTempC - 40).coerceIn(0, 75)) / 12.5f).roundToInt().coerceIn(1, 6)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(
                1.dp,
                if (state.isHighTemp || state.isLowBattery) YezdiCrimson else palette.border,
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
    ) {
        // Temperature Monitor Header
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
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = "Coolant temperature",
                    tint = tempColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "TEMP",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
            Text(
                text = "${state.coolantTempC}°C / AIR ${state.ambientTempC}°",
                style = MaterialTheme.typography.labelMedium,
                color = tempColor,
                modifier = Modifier.testTag("temp_readout"),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 6-segment Coolant Thermal Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (seg in 1..6) {
                val active = seg <= tempBars
                val segColor = when {
                    !active -> palette.segmentInactive
                    seg >= 6 -> YezdiCrimson
                    seg == 5 -> RallyAmber
                    else -> RainCyan
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Battery Indication with Voltage
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
                    imageVector = if (state.engineRunning) {
                        Icons.Default.BatteryChargingFull
                    } else {
                        Icons.Default.BatteryStd
                    },
                    contentDescription = "Battery voltage indicator",
                    tint = battColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (state.engineRunning) "CHG" else "BATT",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
            }
            Text(
                text = String.format(Locale.US, "%.1fV", state.batteryVoltage),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp,
                color = battColor,
                modifier = Modifier.testTag("battery_readout"),
            )
        }
    }
}

@Composable
private fun OdoAndTripCard(
    state: DashboardUiState,
    onToggleTrip: () -> Unit,
    onResetTrip: () -> Unit,
    onSelectTripFocus: () -> Unit,
) {
    val palette = LocalCockpitPalette.current
    val isFocused = state.focusedModule == SwitchgearFocus.TRIP_METER
    val mins = (state.activeTripSeconds / 60L) % 60L
    val hrs = state.activeTripSeconds / 3600L
    val timeStr = String.format(Locale.US, "%02d:%02d", hrs, mins)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) palette.cardFocused else palette.card)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) RainCyan else palette.border,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable {
                onSelectTripFocus()
                onToggleTrip()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Odometer
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = "Odometer",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "ODOMETER",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%08.1f KM", state.odometerKm),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 18.sp,
                color = palette.textPrimary,
                modifier = Modifier.testTag("odo_readout"),
            )
        }

        // Trip A / B Active Meter
        Column(horizontalAlignment = Alignment.End) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TRIP ${state.activeTrip.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = RainCyan,
                )
                Text(
                    text = "$timeStr • ${state.activeTripAvgKmh} KM/H",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.bezel)
                        .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                        .clickable { onResetTrip() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("trip_reset_button"),
                ) {
                    Text(
                        text = "RST",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%.1f KM", state.activeTripKm),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 18.sp,
                color = palette.textPrimary,
                modifier = Modifier.testTag("trip_readout"),
            )
        }
    }
}
