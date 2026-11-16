package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallStatus
import com.example.model.DashboardUiState
import com.example.model.RideMode
import com.example.model.TurnSignalState
import com.example.ui.theme.HazardRed
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.SwitchgearCap
import com.example.ui.theme.SwitchgearMetal
import com.example.ui.theme.YezdiCrimson

data class SwitchgearCallbacks(
    val onToggleIgnition: () -> Unit,
    val onToggleSideStand: () -> Unit,
    val onGearUp: () -> Unit,
    val onGearDown: () -> Unit,
    val onThrottleHold: (Boolean) -> Unit,
    val onCruiseToggle: () -> Unit,
    val onBrakeHold: (Boolean) -> Unit,
    val onCycleRideMode: () -> Unit,
    val onToggleLeftTurn: () -> Unit,
    val onToggleRightTurn: () -> Unit,
    val onToggleHazard: () -> Unit,
    val onUp: () -> Unit,
    val onDown: () -> Unit,
    val onLeft: () -> Unit,
    val onRight: () -> Unit,
    val onSet: () -> Unit,
    val onSetLongPress: () -> Unit,
    val onBack: () -> Unit,
    val onNavButton: () -> Unit,
)

@Composable
fun BottomHandlebarSwitchgear(
    state: DashboardUiState,
    callbacks: SwitchgearCallbacks,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val modeColor = when (state.rideMode) {
        RideMode.ROAD -> YezdiCrimson
        RideMode.RAIN -> RainCyan
        RideMode.OFF_ROAD -> RallyAmber
    }
    val inMenuOrNav = state.isFocusEditing || state.navigationState.showDestinationPicker

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(SwitchgearMetal)
            .border(
                width = 1.dp,
                color = palette.border,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Feedback Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BIKE NAV: ${state.focusedModule.label}" + if (state.isFocusEditing) " [EDIT]" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isFocusEditing) NeutralEmerald else Color(0xFF94A3B8),
            )
            Text(
                text = state.lastToggleFeedback,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.interlockAlert != null) RallyAmber else RainCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("toggle_feedback_text"),
            )
        }

        // Row 1: 5-Way Bike Navigation D-Pad (L/◄, UP, SET, DN, R/►)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SwitchIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = if (inMenuOrNav) "PREV" else "L ◄",
                contentDescription = if (inMenuOrNav) "Previous option" else "Left turn signal",
                ledActive = state.turnSignal == TurnSignalState.LEFT,
                ledColor = NeutralEmerald,
                onClick = callbacks.onLeft,
                modifier = Modifier.weight(1f).testTag("btn_turn_left"),
            )
            SwitchIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                label = "UP",
                contentDescription = "Bike button up",
                ledActive = state.isFocusEditing || state.navigationState.showDestinationPicker,
                ledColor = RainCyan,
                onClick = callbacks.onUp,
                modifier = Modifier.weight(1f).testTag("btn_up"),
            )
            SetLongPressButton(
                label = when {
                    state.callState.status == CallStatus.INCOMING -> "ATTEND"
                    state.navigationState.showDestinationPicker -> "GO"
                    else -> "SET"
                },
                subLabel = if (state.isFocusEditing) "LOCK/ACT" else "HOLD OPT",
                ledActive = state.callState.status == CallStatus.INCOMING || state.isFocusEditing,
                ledColor = if (state.callState.status == CallStatus.INCOMING) NeutralEmerald else RainCyan,
                onClick = callbacks.onSet,
                onLongClick = callbacks.onSetLongPress,
                modifier = Modifier.weight(1.2f).testTag("btn_set"),
            )
            SwitchIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "DN",
                contentDescription = "Bike button down",
                ledActive = state.isFocusEditing || state.navigationState.showDestinationPicker,
                ledColor = RainCyan,
                onClick = callbacks.onDown,
                modifier = Modifier.weight(1f).testTag("btn_down"),
            )
            SwitchIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = if (inMenuOrNav) "NEXT" else "R ►",
                contentDescription = if (inMenuOrNav) "Next option" else "Right turn signal",
                ledActive = state.turnSignal == TurnSignalState.RIGHT,
                ledColor = NeutralEmerald,
                onClick = callbacks.onRight,
                modifier = Modifier.weight(1f).testTag("btn_turn_right"),
            )
        }

        // Row 2: NAV, HAZARD, MODE, GEAR -, GEAR +, BACK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SwitchTextButton(
                label = "NAV",
                icon = Icons.Default.Navigation,
                ledActive = state.navigationState.isActive,
                ledColor = NeutralEmerald,
                accentBorder = if (state.navigationState.isActive) NeutralEmerald else null,
                onClick = callbacks.onNavButton,
                modifier = Modifier.weight(1f).testTag("btn_nav"),
            )
            SwitchTextButton(
                label = "HAZ",
                icon = Icons.Default.Warning,
                ledActive = state.turnSignal == TurnSignalState.HAZARD,
                ledColor = HazardRed,
                accentBorder = if (state.turnSignal == TurnSignalState.HAZARD) HazardRed else null,
                onClick = callbacks.onToggleHazard,
                modifier = Modifier.weight(0.95f).testTag("btn_hazard"),
            )
            SwitchTextButton(
                label = "MODE",
                ledActive = true,
                ledColor = modeColor,
                accentBorder = modeColor.copy(alpha = 0.7f),
                onClick = callbacks.onCycleRideMode,
                modifier = Modifier.weight(1f).testTag("btn_mode"),
            )
            SwitchTextButton(
                label = "G -",
                ledActive = false,
                ledColor = palette.textMuted,
                onClick = callbacks.onGearDown,
                modifier = Modifier.weight(0.9f).testTag("btn_gear_down"),
            )
            SwitchTextButton(
                label = "G +",
                ledActive = false,
                ledColor = palette.textMuted,
                onClick = callbacks.onGearUp,
                modifier = Modifier.weight(0.9f).testTag("btn_gear_up"),
            )
            SwitchTextButton(
                label = if (state.callState.status != CallStatus.IDLE) "END" else "BACK",
                ledActive = state.callState.status != CallStatus.IDLE || state.isFocusEditing,
                ledColor = if (state.callState.status != CallStatus.IDLE) HazardRed else RallyAmber,
                onClick = callbacks.onBack,
                modifier = Modifier.weight(1f).testTag("btn_back"),
            )
        }

        // Row 3: Ignition, Side Stand, Cruise, Holdable Throttle & ABS Brake
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SwitchTextButton(
                label = if (state.ignitionOn) "IGN ON" else "IGN OFF",
                icon = Icons.Default.PowerSettingsNew,
                ledActive = state.ignitionOn,
                ledColor = NeutralEmerald,
                onClick = callbacks.onToggleIgnition,
                modifier = Modifier.weight(1f).testTag("btn_ignition"),
            )
            SwitchTextButton(
                label = if (state.sideStandDown) "STAND DN" else "STAND UP",
                ledActive = state.sideStandDown,
                ledColor = RallyAmber,
                accentBorder = if (state.sideStandDown) RallyAmber else null,
                onClick = callbacks.onToggleSideStand,
                modifier = Modifier.weight(1f).testTag("btn_side_stand"),
            )
            SwitchTextButton(
                label = if (state.cruiseTargetKmh != null) "CRS ${state.cruiseTargetKmh}" else "CRUISE",
                icon = Icons.Default.Speed,
                ledActive = state.cruiseTargetKmh != null,
                ledColor = NeutralEmerald,
                onClick = callbacks.onCruiseToggle,
                modifier = Modifier.weight(1f).testTag("btn_cruise"),
            )
            HoldableSwitchButton(
                label = "THROTTLE",
                active = state.throttleActive,
                activeColor = NeutralEmerald,
                onHoldChange = callbacks.onThrottleHold,
                modifier = Modifier.weight(1.1f).testTag("btn_throttle"),
            )
            HoldableSwitchButton(
                label = "BRAKE",
                active = state.brakeActive,
                activeColor = YezdiCrimson,
                onHoldChange = callbacks.onBrakeHold,
                modifier = Modifier.weight(0.95f).testTag("btn_brake"),
            )
        }
    }
}

@Composable
fun LeftHandlebarPod(
    state: DashboardUiState,
    callbacks: SwitchgearCallbacks,
    modifier: Modifier = Modifier,
) {
    val inMenuOrNav = state.isFocusEditing || state.navigationState.showDestinationPicker
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SwitchgearMetal)
            .border(1.dp, Color(0xFF2B3442), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("LEFT POD", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

        SwitchIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            label = "UP",
            contentDescription = "Handlebar up toggle",
            ledActive = state.isFocusEditing || state.navigationState.showDestinationPicker,
            ledColor = RainCyan,
            onClick = callbacks.onUp,
            modifier = Modifier.fillMaxWidth().testTag("btn_up"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SwitchIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = if (inMenuOrNav) "PRV" else "L",
                contentDescription = "Left handlebar button",
                ledActive = state.turnSignal == TurnSignalState.LEFT,
                ledColor = NeutralEmerald,
                onClick = callbacks.onLeft,
                modifier = Modifier.weight(1f).testTag("btn_turn_left"),
            )
            SetLongPressButton(
                label = if (state.callState.status == CallStatus.INCOMING) "ATT" else "SET",
                subLabel = "HOLD",
                ledActive = state.isFocusEditing || state.callState.status == CallStatus.INCOMING,
                ledColor = NeutralEmerald,
                onClick = callbacks.onSet,
                onLongClick = callbacks.onSetLongPress,
                modifier = Modifier.weight(1.2f).testTag("btn_set"),
            )
            SwitchIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = if (inMenuOrNav) "NXT" else "R",
                contentDescription = "Right handlebar button",
                ledActive = state.turnSignal == TurnSignalState.RIGHT,
                ledColor = NeutralEmerald,
                onClick = callbacks.onRight,
                modifier = Modifier.weight(1f).testTag("btn_turn_right"),
            )
        }
        SwitchIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            label = "DN",
            contentDescription = "Handlebar down toggle",
            ledActive = state.isFocusEditing || state.navigationState.showDestinationPicker,
            ledColor = RainCyan,
            onClick = callbacks.onDown,
            modifier = Modifier.fillMaxWidth().testTag("btn_down"),
        )
        SwitchTextButton(
            label = "NAV / ROUTE",
            icon = Icons.Default.Navigation,
            ledActive = state.navigationState.isActive,
            ledColor = NeutralEmerald,
            onClick = callbacks.onNavButton,
            modifier = Modifier.fillMaxWidth().testTag("btn_nav"),
        )
        SwitchTextButton(
            label = "HAZARD",
            icon = Icons.Default.Warning,
            ledActive = state.turnSignal == TurnSignalState.HAZARD,
            ledColor = HazardRed,
            accentBorder = if (state.turnSignal == TurnSignalState.HAZARD) HazardRed else null,
            onClick = callbacks.onToggleHazard,
            modifier = Modifier.fillMaxWidth().testTag("btn_hazard"),
        )
        SwitchTextButton(
            label = if (state.sideStandDown) "STAND DN" else "STAND UP",
            ledActive = state.sideStandDown,
            ledColor = RallyAmber,
            onClick = callbacks.onToggleSideStand,
            modifier = Modifier.fillMaxWidth().testTag("btn_side_stand"),
        )
        SwitchTextButton(
            label = if (state.callState.status != CallStatus.IDLE) "END CALL" else "BACK",
            ledActive = state.callState.status != CallStatus.IDLE,
            ledColor = HazardRed,
            onClick = callbacks.onBack,
            modifier = Modifier.fillMaxWidth().testTag("btn_back"),
        )
    }
}

@Composable
fun RightHandlebarPod(
    state: DashboardUiState,
    callbacks: SwitchgearCallbacks,
    modifier: Modifier = Modifier,
) {
    val modeColor = when (state.rideMode) {
        RideMode.ROAD -> YezdiCrimson
        RideMode.RAIN -> RainCyan
        RideMode.OFF_ROAD -> RallyAmber
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SwitchgearMetal)
            .border(1.dp, Color(0xFF2B3442), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("RIGHT POD", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

        SwitchTextButton(
            label = if (state.ignitionOn) "IGN ON" else "IGN OFF",
            icon = Icons.Default.PowerSettingsNew,
            ledActive = state.ignitionOn,
            ledColor = NeutralEmerald,
            onClick = callbacks.onToggleIgnition,
            modifier = Modifier.fillMaxWidth().testTag("btn_ignition"),
        )
        SwitchTextButton(
            label = "MODE: ${state.rideMode.displayName}",
            ledActive = true,
            ledColor = modeColor,
            accentBorder = modeColor,
            onClick = callbacks.onCycleRideMode,
            modifier = Modifier.fillMaxWidth().testTag("btn_mode"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SwitchTextButton(
                label = "GEAR -",
                ledActive = false,
                ledColor = Color.Gray,
                onClick = callbacks.onGearDown,
                modifier = Modifier.weight(1f).testTag("btn_gear_down"),
            )
            SwitchTextButton(
                label = "GEAR +",
                ledActive = false,
                ledColor = Color.Gray,
                onClick = callbacks.onGearUp,
                modifier = Modifier.weight(1f).testTag("btn_gear_up"),
            )
        }
        SwitchTextButton(
            label = if (state.cruiseTargetKmh != null) "CRUISE ${state.cruiseTargetKmh}" else "CRUISE",
            icon = Icons.Default.Speed,
            ledActive = state.cruiseTargetKmh != null,
            ledColor = NeutralEmerald,
            onClick = callbacks.onCruiseToggle,
            modifier = Modifier.fillMaxWidth().testTag("btn_cruise"),
        )
        HoldableSwitchButton(
            label = "THROTTLE (HOLD)",
            active = state.throttleActive,
            activeColor = NeutralEmerald,
            onHoldChange = callbacks.onThrottleHold,
            modifier = Modifier.fillMaxWidth().testTag("btn_throttle"),
        )
        HoldableSwitchButton(
            label = "ABS BRAKE (HOLD)",
            active = state.brakeActive,
            activeColor = YezdiCrimson,
            onHoldChange = callbacks.onBrakeHold,
            modifier = Modifier.fillMaxWidth().testTag("btn_brake"),
        )
    }
}

@Composable
private fun SwitchIconButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    ledActive: Boolean,
    ledColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SwitchgearCap)
            .border(
                width = 1.dp,
                color = if (ledActive) ledColor else Color(0xFF394454),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ledActive) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ledColor),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color(0xFFF1F5F9),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SwitchTextButton(
    label: String,
    ledActive: Boolean,
    ledColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentBorder: Color? = null,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (ledActive) ledColor.copy(alpha = 0.16f) else SwitchgearCap)
            .border(
                width = 1.dp,
                color = accentBorder ?: if (ledActive) ledColor else Color(0xFF394454),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (ledActive) ledColor else Color(0xFF475569)),
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (ledActive) ledColor else Color(0xFFCBD5E1),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetLongPressButton(
    label: String,
    subLabel: String,
    ledActive: Boolean,
    ledColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (ledActive) ledColor.copy(alpha = 0.25f) else Color(0xFF333D4D))
            .border(
                width = 1.5.dp,
                color = if (ledActive) ledColor else Color(0xFF64748B),
                shape = RoundedCornerShape(10.dp),
            )
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = subLabel,
                fontSize = 7.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HoldableSwitchButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) activeColor.copy(alpha = 0.30f) else SwitchgearCap)
            .border(
                width = 1.5.dp,
                color = if (active) activeColor else Color(0xFF394454),
                shape = RoundedCornerShape(10.dp),
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHoldChange(true)
                        tryAwaitRelease()
                        onHoldChange(false)
                    },
                )
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) activeColor else Color(0xFFF1F5F9),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
