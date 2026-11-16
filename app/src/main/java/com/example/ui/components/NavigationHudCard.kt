package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdventureDestinations
import com.example.model.DashboardUiState
import com.example.model.ManeuverType
import com.example.model.NavDestination
import com.example.model.SwitchgearFocus
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.YezdiCrimson
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NavigationHudCard(
    state: DashboardUiState,
    onSelectFocus: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onToggleVoice: () -> Unit,
    onStopNav: () -> Unit,
    onRequestGpsSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val nav = state.navigationState
    val isFocused = state.focusedModule == SwitchgearFocus.NAVIGATION
    val borderColor = when {
        nav.isActive && isFocused -> NeutralEmerald
        isFocused -> RainCyan
        nav.isActive -> NeutralEmerald.copy(alpha = 0.7f)
        else -> palette.border
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isFocused) palette.cardFocused else palette.card)
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onSelectFocus() }
            .padding(12.dp)
            .testTag("navigation_hud_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Google Maps Turn-by-Turn Navigation",
                    tint = if (nav.isActive) NeutralEmerald else RainCyan,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (nav.isActive) nav.routeSource else "GOOGLE MAPS TURN-BY-TURN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nav.isActive) NeutralEmerald else palette.textSecondary,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // GPS Lock Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.bezel)
                        .border(1.dp, if (nav.hasLocationPermission) NeutralEmerald else palette.border, RoundedCornerShape(6.dp))
                        .clickable { onRequestGpsSync() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("nav_gps_chip"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Sync GPS location",
                            tint = if (nav.hasLocationPermission) NeutralEmerald else palette.textMuted,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = if (nav.gpsLat != null) "GPS" else "LOC",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (nav.hasLocationPermission) NeutralEmerald else palette.textMuted,
                        )
                    }
                }

                // Voice Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.bezel)
                        .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                        .clickable { onToggleVoice() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("nav_voice_toggle"),
                ) {
                    Icon(
                        imageVector = if (nav.voiceEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = "Toggle voice turn guidance",
                        tint = if (nav.voiceEnabled) RainCyan else palette.textMuted,
                        modifier = Modifier.size(13.dp),
                    )
                }

                if (nav.isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(YezdiCrimson.copy(alpha = 0.18f))
                            .border(1.dp, YezdiCrimson, RoundedCornerShape(6.dp))
                            .clickable { onStopNav() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("nav_stop_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop navigation",
                            tint = YezdiCrimson,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        if (nav.isLoadingRoute) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = RainCyan,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CALCULATING GOOGLE MAPS ROUTE...",
                    style = MaterialTheme.typography.labelMedium,
                    color = RainCyan,
                )
            }
        } else if (nav.isActive && nav.currentStep != null) {
            val step = nav.currentStep!!
            val remainingMeters = nav.remainingStepMeters.roundToInt().coerceAtLeast(0)
            val isImminent = remainingMeters <= 180
            val turnColor = if (isImminent) RallyAmber else NeutralEmerald

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Big Turn-by-Turn Maneuver Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(turnColor.copy(alpha = 0.16f))
                        .border(1.5.dp, turnColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = maneuverIcon(step.maneuver),
                        contentDescription = step.instruction,
                        tint = turnColor,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // Distance Countdown + Turn Instruction
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = formatMeters(remainingMeters),
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 30.sp,
                            color = turnColor,
                            modifier = Modifier.testTag("nav_step_distance"),
                        )
                        Text(
                            text = "STEP ${nav.currentStepIndex + 1}/${nav.steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("nav_instruction_text"),
                    )
                    nav.nextStep?.let { upcoming ->
                        Text(
                            text = "THEN: ${upcoming.instruction}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Mini Polyline Radar Canvas
                MiniRouteRadar(
                    maneuver = step.maneuver,
                    accentColor = turnColor,
                    trackColor = palette.segmentInactive,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bezel)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
                )
            }

            // Route Summary + Bike-Button Step & Google Maps Launch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = nav.destination?.name ?: "Destination",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = "${formatMeters(nav.totalRemainingMeters)} • ${nav.totalRemainingMinutes} MIN ETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = RainCyan,
                        modifier = Modifier.testTag("nav_eta_text"),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPrevStep,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_prev_step"),
                    ) {
                        Text("PREV", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onNextStep,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("nav_next_step"),
                    ) {
                        Text("NEXT", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onOpenGoogleMaps,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeutralEmerald,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("open_google_maps_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open in Google Maps",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("G-MAPS", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            // Idle Navigation Ready State
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RainCyan.copy(alpha = 0.14f))
                            .border(1.dp, RainCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Ready for turn-by-turn navigation",
                            tint = RainCyan,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NO ACTIVE ROUTE",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "PRESS SET OR TAP ROUTE TO START GOOGLE MAPS NAV",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }
                }

                Button(
                    onClick = onOpenPicker,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RainCyan,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("pick_route_button"),
                ) {
                    Text("ROUTES", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MiniRouteRadar(
    maneuver: ManeuverType,
    accentColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Subtle radar ring
            drawCircle(
                color = trackColor,
                radius = w * 0.44f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )

            // Route path vector preview
            val routePath = Path().apply {
                moveTo(cx, h * 0.9f)
                lineTo(cx, cy)
                when (maneuver) {
                    ManeuverType.TURN_LEFT, ManeuverType.SHARP_LEFT -> lineTo(w * 0.15f, cy)
                    ManeuverType.SLIGHT_LEFT -> lineTo(w * 0.25f, h * 0.2f)
                    ManeuverType.TURN_RIGHT, ManeuverType.SHARP_RIGHT -> lineTo(w * 0.85f, cy)
                    ManeuverType.SLIGHT_RIGHT -> lineTo(w * 0.75f, h * 0.2f)
                    ManeuverType.ROUNDABOUT -> {
                        lineTo(w * 0.78f, h * 0.3f)
                    }
                    ManeuverType.U_TURN -> {
                        lineTo(w * 0.25f, cy)
                        lineTo(w * 0.25f, h * 0.85f)
                    }
                    ManeuverType.STRAIGHT, ManeuverType.ARRIVE -> lineTo(cx, h * 0.12f)
                }
            }
            drawPath(
                path = routePath,
                color = accentColor,
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Rider bike position chevron dot at bottom
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(cx, h * 0.86f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationPickerSheet(
    highlightedIndex: Int,
    onPickDestination: (NavDestination) -> Unit,
    onStartCustomQuery: (String) -> Unit,
    onOpenInGoogleMaps: (NavDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalCockpitPalette.current
    var customText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.bezel,
        contentColor = palette.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .testTag("destination_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "GOOGLE MAPS DESTINATIONS",
                        style = MaterialTheme.typography.titleMedium,
                        color = RainCyan,
                    )
                    Text(
                        text = "USE BIKE UP/DN TO BROWSE • SET TO START • BACK TO CLOSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close destination picker")
                }
            }

            // Optional custom search query input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("Enter city, pass, or address...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("custom_destination_input"),
                )
                Button(
                    onClick = { onStartCustomQuery(customText) },
                    enabled = customText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeutralEmerald, contentColor = Color.Black),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_destination_go"),
                ) {
                    Text("GO", style = MaterialTheme.typography.labelMedium)
                }
            }

            AdventureDestinations.presets.forEachIndexed { idx, dest ->
                val isHighlighted = idx == highlightedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isHighlighted) RainCyan.copy(alpha = 0.16f) else palette.card)
                        .border(
                            width = if (isHighlighted) 1.5.dp else 1.dp,
                            color = if (isHighlighted) RainCyan else palette.border,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onPickDestination(dest) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("dest_item_${dest.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dest.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isHighlighted) RainCyan else palette.textPrimary,
                        )
                        Text(
                            text = dest.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onOpenInGoogleMaps(dest) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Launch Google Maps for ${dest.name}",
                                tint = NeutralEmerald,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun maneuverIcon(type: ManeuverType): ImageVector = when (type) {
    ManeuverType.STRAIGHT -> Icons.Default.Straight
    ManeuverType.TURN_LEFT -> Icons.Default.TurnLeft
    ManeuverType.TURN_RIGHT -> Icons.Default.TurnRight
    ManeuverType.SLIGHT_LEFT -> Icons.Default.TurnSlightLeft
    ManeuverType.SLIGHT_RIGHT -> Icons.Default.TurnSlightRight
    ManeuverType.SHARP_LEFT -> Icons.Default.TurnSharpLeft
    ManeuverType.SHARP_RIGHT -> Icons.Default.TurnSharpRight
    ManeuverType.ROUNDABOUT -> Icons.Default.RoundaboutRight
    ManeuverType.U_TURN -> Icons.Default.UTurnLeft
    ManeuverType.ARRIVE -> Icons.Default.Flag
}

private fun formatMeters(meters: Int): String {
    return if (meters >= 1000) {
        String.format(Locale.US, "%.1f KM", meters / 1000f)
    } else {
        "$meters M"
    }
}
