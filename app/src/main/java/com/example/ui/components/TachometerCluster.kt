package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardUiState
import com.example.model.RideMode
import com.example.model.ShiftCue
import com.example.model.SwitchgearFocus
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalCockpitPalette
import com.example.ui.theme.NeutralEmerald
import com.example.ui.theme.RainCyan
import com.example.ui.theme.RallyAmber
import com.example.ui.theme.YezdiCrimson
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TachometerCluster(
    state: DashboardUiState,
    onCycleRideMode: () -> Unit,
    onSelectMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val modeAccent by animateColorAsState(
        targetValue = when (state.rideMode) {
            RideMode.ROAD -> YezdiCrimson
            RideMode.RAIN -> RainCyan
            RideMode.OFF_ROAD -> RallyAmber
        },
        label = "modeAccent",
    )

    val effectiveRpmFraction = if (state.isBooting) {
        state.bootSweepProgress
    } else if (!state.ignitionOn) {
        0f
    } else {
        (state.rpm.toFloat() / state.maxRpm.toFloat()).coerceIn(0f, 1f)
    }

    val animatedRpmFraction by animateFloatAsState(
        targetValue = effectiveRpmFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rpmSweep",
    )

    val isModeFocused = state.focusedModule == SwitchgearFocus.RIDE_MODE
    val cardBorder = if (isModeFocused) modeAccent else palette.border

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.card)
            .border(1.5.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        // Header strip: Brand badge + Ride Mode & ABS Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "YEZDI ADVENTURE",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                )
                Text(
                    text = if (state.ignitionOn) {
                        if (state.cruiseTargetKmh != null) "CRUISE ${state.cruiseTargetKmh} KM/H" else "334cc DOHC • TFT"
                    } else {
                        "IGNITION OFF"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.cruiseTargetKmh != null) NeutralEmerald else palette.textMuted,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ABS Status Badge
                val absWarn = state.rideMode == RideMode.OFF_ROAD || state.absSlipActive
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.absSlipActive) YezdiCrimson.copy(alpha = 0.22f)
                            else if (absWarn) RallyAmber.copy(alpha = 0.16f)
                            else palette.bezel
                        )
                        .border(
                            1.dp,
                            if (state.absSlipActive) YezdiCrimson else if (absWarn) RallyAmber else palette.border,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("abs_status_badge"),
                ) {
                    Text(
                        text = if (state.absSlipActive) "ABS ACTIVE!" else state.rideMode.absSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.absSlipActive) YezdiCrimson else if (absWarn) RallyAmber else palette.textSecondary,
                    )
                }

                // Interactive Ride Mode Pill
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(modeAccent.copy(alpha = 0.18f))
                        .border(1.5.dp, modeAccent, RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectMode()
                            onCycleRideMode()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("ride_mode_pill"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.rideMode.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = modeAccent,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Gauge Row: Sweep Tachometer & Speedometer + Gear Position Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left/Center Sweep Tachometer + Digital Speed
            Box(
                modifier = Modifier
                    .weight(1.45f)
                    .aspectRatio(1.18f),
                contentAlignment = Alignment.Center,
            ) {
                val trackInactive = palette.segmentInactive
                val textPrimary = palette.textPrimary
                val textMuted = palette.textMuted
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val strokeWidth = size.minDimension * 0.10f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f + size.height * 0.05f,
                    )
                    val arcSize = Size(diameter, diameter)
                    val startAngle = 145f
                    val totalSweep = 250f

                    // Base track arc
                    drawArc(
                        color = trackInactive,
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )

                    // Redline zone guide (8k - 10k RPM)
                    drawArc(
                        color = YezdiCrimson.copy(alpha = 0.28f),
                        startAngle = startAngle + totalSweep * 0.8f,
                        sweepAngle = totalSweep * 0.2f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Butt),
                    )

                    // Active RPM arc
                    val activeSweep = totalSweep * animatedRpmFraction
                    if (activeSweep > 0.5f) {
                        val activeBrush = Brush.sweepGradient(
                            0.0f to modeAccent,
                            0.72f to modeAccent,
                            0.85f to RallyAmber,
                            1.0f to YezdiCrimson,
                            center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f),
                        )
                        drawArc(
                            brush = activeBrush,
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                    }

                    // 11 RPM tick marks (0 to 10)
                    val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
                    val outerR = diameter / 2f + strokeWidth * 0.52f
                    val innerR = diameter / 2f - strokeWidth * 0.55f
                    for (i in 0..10) {
                        val angleDeg = startAngle + (totalSweep * i / 10f)
                        val rad = (angleDeg * PI / 180.0).toFloat()
                        val p1 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                        val p2 = Offset(center.x + outerR * cos(rad), center.y + outerR * sin(rad))
                        drawLine(
                            color = if (i >= 8) YezdiCrimson else textMuted,
                            start = p1,
                            end = p2,
                            strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                        )
                    }

                    // Needle tip indicator
                    val needleAngleDeg = startAngle + activeSweep
                    val needleRad = (needleAngleDeg * PI / 180.0).toFloat()
                    val nStart = Offset(
                        center.x + (diameter / 2f - strokeWidth * 0.85f) * cos(needleRad),
                        center.y + (diameter / 2f - strokeWidth * 0.85f) * sin(needleRad),
                    )
                    val nEnd = Offset(
                        center.x + (diameter / 2f + strokeWidth * 0.65f) * cos(needleRad),
                        center.y + (diameter / 2f + strokeWidth * 0.65f) * sin(needleRad),
                    )
                    drawLine(
                        color = if (animatedRpmFraction >= 0.8f) YezdiCrimson else textPrimary,
                        start = nStart,
                        end = nEnd,
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Speed & RPM readout centered inside arc
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    val displayedSpeed = if (state.isBooting) {
                        (state.bootSweepProgress * 188f).roundToInt()
                    } else if (!state.ignitionOn) {
                        0
                    } else {
                        state.speedKmh.roundToInt()
                    }
                    Text(
                        text = displayedSpeed.toString(),
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp,
                        lineHeight = 56.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("speed_readout"),
                    )
                    Text(
                        text = "KM/H",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val displayedRpm = if (state.isBooting) {
                        (state.bootSweepProgress * state.maxRpm).roundToInt()
                    } else {
                        state.rpm
                    }
                    Text(
                        text = "$displayedRpm RPM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (displayedRpm >= state.redlineRpm) YezdiCrimson else modeAccent,
                        modifier = Modifier.testTag("rpm_readout"),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Side: Giant Gear Indication + Shift Cue + Gear Position Ladder
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.bezel)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "GEAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )

                // Giant Gear Number + Shift Cue
                val gearColor = when {
                    !state.ignitionOn -> palette.textMuted
                    state.gear == 0 -> NeutralEmerald
                    state.rpm >= state.redlineRpm -> YezdiCrimson
                    else -> palette.textPrimary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (state.ignitionOn) state.gearLabel else "-",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 52.sp,
                        lineHeight = 56.sp,
                        color = gearColor,
                        modifier = Modifier.testTag("gear_readout"),
                    )
                    if (state.shiftCue != ShiftCue.NONE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (state.shiftCue == ShiftCue.UPSHIFT) {
                                Icons.Default.KeyboardDoubleArrowUp
                            } else {
                                Icons.Default.KeyboardDoubleArrowDown
                            },
                            contentDescription = if (state.shiftCue == ShiftCue.UPSHIFT) "Upshift recommended" else "Downshift recommended",
                            tint = if (state.shiftCue == ShiftCue.UPSHIFT) YezdiCrimson else RallyAmber,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 6-Speed Gear Position Indication Ladder (N, 1..6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (g in 0..6) {
                        val isCurrent = state.ignitionOn && state.gear == g
                        val pillColor = when {
                            !isCurrent -> palette.card
                            g == 0 -> NeutralEmerald
                            else -> modeAccent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(pillColor)
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (g == 0) "N" else g.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) Color.White else palette.textMuted,
                            )
                        }
                    }
                }
            }
        }

        // Side Stand Safety Interlock Alert Strip (when triggered)
        if (state.interlockAlert != null || state.sideStandDown) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RallyAmber.copy(alpha = 0.16f))
                    .border(1.dp, RallyAmber, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("side_stand_banner"),
            ) {
                Text(
                    text = state.interlockAlert ?: "SIDE STAND DEPLOYED • NEUTRAL INTERLOCK ACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = RallyAmber,
                )
            }
        }
    }
}
