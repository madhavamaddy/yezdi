package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.GoogleMapsNavigationService
import com.example.model.CallStatus
import com.example.model.DashboardUiState
import com.example.model.SwitchgearFocus
import com.example.ui.components.BottomHandlebarSwitchgear
import com.example.ui.components.CallHudBanner
import com.example.ui.components.CockpitControlModules
import com.example.ui.components.DestinationPickerSheet
import com.example.ui.components.EcuSimulatorSheet
import com.example.ui.components.LeftHandlebarPod
import com.example.ui.components.NavigationHudCard
import com.example.ui.components.RightHandlebarPod
import com.example.ui.components.SwitchgearCallbacks
import com.example.ui.components.TachometerCluster
import com.example.ui.components.TellTaleBar
import com.example.ui.components.TelemetryGrid
import com.example.ui.theme.LocalCockpitPalette
import com.example.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCockpitPalette.current
    val context = LocalContext.current

    // Sync initial GPS status if permission was already granted
    LaunchedEffect(Unit) {
        val granted = GoogleMapsNavigationService.hasLocationPermission(context)
        val coords = if (granted) GoogleMapsNavigationService.getLastKnownCoordinates(context) else null
        viewModel.onUpdateGpsCoordinates(coords?.first, coords?.second, granted)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.any { it }
        val coords = if (granted) GoogleMapsNavigationService.getLastKnownCoordinates(context) else null
        viewModel.onUpdateGpsCoordinates(coords?.first, coords?.second, granted)
    }

    val handleGpsSync: () -> Unit = {
        if (GoogleMapsNavigationService.hasLocationPermission(context)) {
            val coords = GoogleMapsNavigationService.getLastKnownCoordinates(context)
            viewModel.onUpdateGpsCoordinates(coords?.first, coords?.second, true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    // BackHandler ensures bike Back / Android Back pops cockpit sub-menus cleanly
    val interceptBack = state.isFocusEditing ||
        state.navigationState.showDestinationPicker ||
        state.showEcuDrawer ||
        state.callState.status != CallStatus.IDLE

    BackHandler(enabled = interceptBack) {
        viewModel.onSwitchgearBack()
    }

    val callbacks = SwitchgearCallbacks(
        onToggleIgnition = viewModel::onToggleIgnition,
        onToggleSideStand = viewModel::onToggleSideStand,
        onGearUp = viewModel::onGearUp,
        onGearDown = viewModel::onGearDown,
        onThrottleHold = viewModel::onThrottleHold,
        onCruiseToggle = viewModel::onCruiseStepToggle,
        onBrakeHold = viewModel::onBrakeHold,
        onCycleRideMode = viewModel::onCycleRideMode,
        onToggleLeftTurn = viewModel::onToggleLeftTurn,
        onToggleRightTurn = viewModel::onToggleRightTurn,
        onToggleHazard = viewModel::onToggleHazard,
        onUp = viewModel::onSwitchgearUp,
        onDown = viewModel::onSwitchgearDown,
        onLeft = viewModel::onSwitchgearLeft,
        onRight = viewModel::onSwitchgearRight,
        onSet = viewModel::onSwitchgearSet,
        onSetLongPress = viewModel::onSwitchgearSetLongPress,
        onBack = viewModel::onSwitchgearBack,
        onNavButton = {
            if (state.navigationState.isActive) {
                viewModel.onRequestOpenGoogleMaps()
            } else {
                viewModel.onOpenDestinationPicker()
            }
        },
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val isWideLandscape = maxWidth >= 720.dp

        if (isWideLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LeftHandlebarPod(
                    state = state,
                    callbacks = callbacks,
                    modifier = Modifier
                        .width(168.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TellTaleBar(
                        state = state,
                        onOpenEcuDrawer = viewModel::onToggleEcuDrawer,
                        onToggleDayNight = viewModel::onToggleDayNight,
                    )

                    CallHudBanner(
                        state = state,
                        onAttendCall = viewModel::onAttendCall,
                        onDeclineOrEndCall = viewModel::onDeclineOrEndCall,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1.05f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TachometerCluster(
                                state = state,
                                onCycleRideMode = viewModel::onCycleRideMode,
                                onSelectMode = { viewModel.onSelectFocusModule(SwitchgearFocus.RIDE_MODE) },
                            )
                            NavigationHudCard(
                                state = state,
                                onSelectFocus = { viewModel.onSelectFocusModule(SwitchgearFocus.NAVIGATION) },
                                onOpenPicker = viewModel::onOpenDestinationPicker,
                                onOpenGoogleMaps = viewModel::onRequestOpenGoogleMaps,
                                onNextStep = viewModel::onNextNavStep,
                                onPrevStep = viewModel::onPrevNavStep,
                                onToggleVoice = viewModel::onToggleNavVoice,
                                onStopNav = viewModel::onStopNavigation,
                                onRequestGpsSync = handleGpsSync,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TelemetryGrid(
                                state = state,
                                onToggleTrip = viewModel::toggleActiveTrip,
                                onResetTrip = viewModel::onResetActiveTrip,
                                onSelectTripFocus = { viewModel.onSelectFocusModule(SwitchgearFocus.TRIP_METER) },
                            )
                            CockpitControlModules(
                                state = state,
                                onSelectFocus = viewModel::onSelectFocusModule,
                                onPlayPause = viewModel::onMusicPlayPause,
                                onPrevTrack = viewModel::onMusicPrev,
                                onNextTrack = viewModel::onMusicNext,
                                onAdjustVolume = viewModel::onAdjustVolume,
                                onBrightnessChange = viewModel::onAdjustBrightness,
                                onToggleDayNight = viewModel::onToggleDayNight,
                                onTriggerCall = viewModel::onTriggerIncomingCall,
                            )
                        }
                    }
                }

                RightHandlebarPod(
                    state = state,
                    callbacks = callbacks,
                    modifier = Modifier
                        .width(168.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TellTaleBar(
                        state = state,
                        onOpenEcuDrawer = viewModel::onToggleEcuDrawer,
                        onToggleDayNight = viewModel::onToggleDayNight,
                    )

                    CallHudBanner(
                        state = state,
                        onAttendCall = viewModel::onAttendCall,
                        onDeclineOrEndCall = viewModel::onDeclineOrEndCall,
                    )

                    NavigationHudCard(
                        state = state,
                        onSelectFocus = { viewModel.onSelectFocusModule(SwitchgearFocus.NAVIGATION) },
                        onOpenPicker = viewModel::onOpenDestinationPicker,
                        onOpenGoogleMaps = viewModel::onRequestOpenGoogleMaps,
                        onNextStep = viewModel::onNextNavStep,
                        onPrevStep = viewModel::onPrevNavStep,
                        onToggleVoice = viewModel::onToggleNavVoice,
                        onStopNav = viewModel::onStopNavigation,
                        onRequestGpsSync = handleGpsSync,
                    )

                    TachometerCluster(
                        state = state,
                        onCycleRideMode = viewModel::onCycleRideMode,
                        onSelectMode = { viewModel.onSelectFocusModule(SwitchgearFocus.RIDE_MODE) },
                    )

                    TelemetryGrid(
                        state = state,
                        onToggleTrip = viewModel::toggleActiveTrip,
                        onResetTrip = viewModel::onResetActiveTrip,
                        onSelectTripFocus = { viewModel.onSelectFocusModule(SwitchgearFocus.TRIP_METER) },
                    )

                    CockpitControlModules(
                        state = state,
                        onSelectFocus = viewModel::onSelectFocusModule,
                        onPlayPause = viewModel::onMusicPlayPause,
                        onPrevTrack = viewModel::onMusicPrev,
                        onNextTrack = viewModel::onMusicNext,
                        onAdjustVolume = viewModel::onAdjustVolume,
                        onBrightnessChange = viewModel::onAdjustBrightness,
                        onToggleDayNight = viewModel::onToggleDayNight,
                        onTriggerCall = viewModel::onTriggerIncomingCall,
                    )
                }

                BottomHandlebarSwitchgear(
                    state = state,
                    callbacks = callbacks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        }
    }

    if (state.navigationState.showDestinationPicker) {
        DestinationPickerSheet(
            highlightedIndex = state.navigationState.highlightedDestinationIndex,
            onPickDestination = viewModel::onStartNavigation,
            onStartCustomQuery = viewModel::onStartCustomDestination,
            onOpenInGoogleMaps = { dest ->
                GoogleMapsNavigationService.launchGoogleMapsTurnByTurn(context, dest)
            },
            onDismiss = viewModel::onCloseDestinationPicker,
        )
    }

    if (state.showEcuDrawer) {
        EcuSimulatorSheet(
            state = state,
            onDismiss = viewModel::onToggleEcuDrawer,
            onAdjustFuel = viewModel::onAdjustFuel,
            onSetBattery = viewModel::onSetCustomBattery,
            onSetTemp = viewModel::onSetCustomTemp,
            onTriggerCall = viewModel::onTriggerIncomingCall,
        )
    }
}
