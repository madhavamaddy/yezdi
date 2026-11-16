package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DashboardPreferencesRepository
import com.example.data.GoogleMapsNavigationService
import com.example.data.HardwareAudioBridge
import com.example.data.VoiceGuidanceSpeaker
import com.example.model.AdventureDestinations
import com.example.model.CallStatus
import com.example.model.DashboardUiState
import com.example.model.ManeuverType
import com.example.model.NavDestination
import com.example.model.NavStep
import com.example.model.RideMode
import com.example.model.SwitchgearFocus
import com.example.model.TripId
import com.example.model.TurnSignalState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardViewModel(
    private val repository: DashboardPreferencesRepository?,
    private val audioBridge: HardwareAudioBridge?,
    private val voiceSpeaker: VoiceGuidanceSpeaker? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _launchMapsEvent = MutableSharedFlow<NavDestination>(extraBufferCapacity = 1)
    val launchMapsEvent: SharedFlow<NavDestination> = _launchMapsEvent.asSharedFlow()

    private var customBatteryOverride: Float? = null
    private var customTempOverride: Int? = null
    private var lastPersistTick = 0L

    // Gear ratios defining speed-per-1000-RPM on the 334cc Yezdi Adventure powertrain
    private val gearKmhPer1000Rpm = floatArrayOf(0f, 7.6f, 11.4f, 14.8f, 17.6f, 20.1f, 22.8f)

    init {
        viewModelScope.launch {
            repository?.let { repo ->
                val saved = repo.telemetryFlow.first()
                _uiState.update { state ->
                    state.copy(
                        odometerKm = saved.odometerKm,
                        tripAKm = saved.tripAKm,
                        tripASeconds = saved.tripASeconds,
                        tripBKm = saved.tripBKm,
                        tripBSeconds = saved.tripBSeconds,
                        activeTrip = saved.activeTrip,
                        fuelPercent = saved.fuelPercent,
                        rideMode = saved.rideMode,
                        screenBrightness = saved.screenBrightness,
                        isNightMode = saved.isNightMode,
                        musicState = state.musicState.copy(
                            volumePercent = audioBridge?.currentVolumePercent() ?: 70,
                        ),
                    )
                }
            }
            triggerBootSelfCheck()
            runTelemetryLoop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceSpeaker?.shutdown()
    }

    private suspend fun triggerBootSelfCheck() {
        _uiState.update {
            it.copy(
                isBooting = true,
                bootSweepProgress = 0f,
                lastToggleFeedback = "ECU SELF-CHECK • TFT INITIALIZING",
            )
        }
        val steps = 16
        for (i in 1..steps) {
            delay(35L)
            val progress = i / steps.toFloat()
            _uiState.update { it.copy(bootSweepProgress = progress) }
        }
        for (i in steps downTo 0) {
            delay(28L)
            val progress = i / steps.toFloat()
            _uiState.update { it.copy(bootSweepProgress = progress) }
        }
        _uiState.update {
            it.copy(
                isBooting = false,
                bootSweepProgress = 0f,
                lastToggleFeedback = "BIKE BUTTONS READY • DPAD / TOGGLES",
            )
        }
    }

    private suspend fun runTelemetryLoop() {
        var tickCount = 0L
        while (true) {
            delay(100L)
            tickCount++

            var spokenPrompt: String? = null

            _uiState.update { state ->
                if (!state.ignitionOn) {
                    return@update state.copy(
                        speedKmh = (state.speedKmh - 2.5f).coerceAtLeast(0f),
                        rpm = 0,
                        engineRunning = false,
                        blinkOn = false,
                        batteryVoltage = customBatteryOverride ?: 12.6f,
                    )
                }

                val blinkOn = if (state.turnSignal != TurnSignalState.OFF) {
                    ((tickCount / 5L) % 2L) == 0L
                } else {
                    false
                }

                val maxGearSpeed = when (state.gear) {
                    0 -> 0f
                    1 -> 44f
                    2 -> 68f
                    3 -> 94f
                    4 -> 115f
                    5 -> 132f
                    else -> 146f
                }

                var newSpeed = state.speedKmh
                if (state.engineRunning && state.gear > 0 && !state.sideStandDown) {
                    val modeAccel = 2.6f * state.rideMode.torqueFactor
                    when {
                        state.brakeActive -> {
                            newSpeed = (newSpeed - 4.4f).coerceAtLeast(0f)
                        }
                        state.throttleActive -> {
                            newSpeed = (newSpeed + modeAccel).coerceAtMost(maxGearSpeed)
                        }
                        state.cruiseTargetKmh != null -> {
                            val target = state.cruiseTargetKmh.toFloat().coerceAtMost(maxGearSpeed)
                            val diff = target - newSpeed
                            newSpeed = when {
                                abs(diff) < 0.8f -> target
                                diff > 0f -> (newSpeed + 1.5f * state.rideMode.torqueFactor).coerceAtMost(target)
                                else -> (newSpeed - 1.2f).coerceAtLeast(target)
                            }
                        }
                        else -> {
                            val idleCreep = state.gear * 4.5f
                            newSpeed = if (newSpeed > idleCreep) {
                                (newSpeed - 0.65f).coerceAtLeast(idleCreep)
                            } else {
                                (newSpeed + 0.35f).coerceAtMost(idleCreep)
                            }
                        }
                    }
                } else {
                    val decel = if (state.brakeActive) 4.2f else 1.2f
                    newSpeed = (newSpeed - decel).coerceAtLeast(0f)
                }

                val targetRpm = when {
                    !state.engineRunning -> 0
                    state.gear == 0 -> {
                        if (state.throttleActive) 6200 else 1450 + ((tickCount % 3) * 25).toInt()
                    }
                    else -> {
                        val ratio = gearKmhPer1000Rpm[state.gear]
                        ((newSpeed / ratio) * 1000f).roundToInt().coerceIn(1450, state.maxRpm)
                    }
                }
                val smoothedRpm = if (!state.engineRunning) {
                    0
                } else {
                    (state.rpm + ((targetRpm - state.rpm) * 0.35f)).roundToInt().coerceIn(0, state.maxRpm)
                }

                val deltaKm = if (newSpeed > 0.2f) newSpeed / 36000.0 else 0.0
                val deltaMeters = (newSpeed / 3.6f) * 0.1f
                val isOneSecondTick = (tickCount % 10L) == 0L

                val newOdo = state.odometerKm + deltaKm
                val newTripA = state.tripAKm + deltaKm
                val newTripB = state.tripBKm + deltaKm
                val newTripASec = if (isOneSecondTick && newSpeed > 0.5f) state.tripASeconds + 1 else state.tripASeconds
                val newTripBSec = if (isOneSecondTick && newSpeed > 0.5f) state.tripBSeconds + 1 else state.tripBSeconds

                val fuelBurn = (deltaKm / (state.tankCapacityLiters * state.rideMode.efficiencyKmPerL)).toFloat()
                val newFuel = (state.fuelPercent - fuelBurn).coerceIn(0.02f, 1.0f)

                val newVoltage = customBatteryOverride ?: if (state.engineRunning) {
                    if (smoothedRpm > 2200) 14.2f else 13.8f
                } else {
                    12.7f
                }

                val targetTemp = customTempOverride ?: when {
                    !state.engineRunning -> 54
                    smoothedRpm > 7200 -> 97
                    smoothedRpm > 4500 -> 89
                    else -> 84
                }
                val newTemp = if (isOneSecondTick) {
                    when {
                        state.coolantTempC < targetTemp -> state.coolantTempC + 1
                        state.coolantTempC > targetTemp -> state.coolantTempC - 1
                        else -> state.coolantTempC
                    }
                } else {
                    state.coolantTempC
                }

                val updatedMusic = if (isOneSecondTick && state.musicState.isPlaying) {
                    val nextElapsed = state.musicState.elapsedSeconds + 1
                    if (nextElapsed >= state.musicState.currentTrack.durationSeconds) {
                        val nextIdx = (state.musicState.currentIndex + 1) % state.musicState.tracks.size
                        state.musicState.copy(currentIndex = nextIdx, elapsedSeconds = 0)
                    } else {
                        state.musicState.copy(elapsedSeconds = nextElapsed)
                    }
                } else {
                    state.musicState
                }

                val updatedCall = if (isOneSecondTick && state.callState.status == CallStatus.ACTIVE) {
                    state.callState.copy(durationSeconds = state.callState.durationSeconds + 1)
                } else {
                    state.callState
                }

                // Turn-by-turn navigation live countdown synced to bike speed
                val nav = state.navigationState
                val updatedNav = if (nav.isActive && nav.steps.isNotEmpty() && newSpeed > 0.4f) {
                    val remaining = nav.remainingStepMeters - deltaMeters
                    if (remaining <= 0f) {
                        val nextIdx = nav.currentStepIndex + 1
                        if (nextIdx < nav.steps.size) {
                            val nextStep = nav.steps[nextIdx]
                            if (nav.voiceEnabled) {
                                spokenPrompt = "In ${nextStep.distanceMeters} meters, ${nextStep.instruction}"
                            }
                            nav.copy(
                                currentStepIndex = nextIdx,
                                remainingStepMeters = nextStep.distanceMeters.toFloat(),
                            )
                        } else {
                            if (nav.voiceEnabled) {
                                spokenPrompt = "You have arrived at ${nav.destination?.name ?: "your destination"}"
                            }
                            nav.copy(
                                isActive = false,
                                remainingStepMeters = 0f,
                            )
                        }
                    } else {
                        nav.copy(remainingStepMeters = remaining)
                    }
                } else {
                    nav
                }

                val absSlip = state.brakeActive && newSpeed > 18f && state.rideMode != RideMode.OFF_ROAD

                state.copy(
                    speedKmh = newSpeed,
                    rpm = smoothedRpm,
                    blinkOn = blinkOn,
                    odometerKm = newOdo,
                    tripAKm = newTripA,
                    tripASeconds = newTripASec,
                    tripBKm = newTripB,
                    tripBSeconds = newTripBSec,
                    fuelPercent = newFuel,
                    batteryVoltage = newVoltage,
                    coolantTempC = newTemp,
                    musicState = updatedMusic,
                    callState = updatedCall,
                    navigationState = updatedNav,
                    absSlipActive = absSlip,
                )
            }

            spokenPrompt?.let { voiceSpeaker?.speak(it) }

            if (tickCount - lastPersistTick >= 30L) {
                lastPersistTick = tickCount
                persistDistances()
            }
        }
    }

    // --- Vehicle Handlebar Toggle Inputs ---

    fun onToggleIgnition() {
        val turningOn = !_uiState.value.ignitionOn
        _uiState.update { state ->
            if (turningOn) {
                state.copy(
                    ignitionOn = true,
                    engineRunning = state.gear == 0 || !state.sideStandDown,
                    interlockAlert = null,
                    lastToggleFeedback = "IGNITION ON • ENGINE RUNNING",
                )
            } else {
                state.copy(
                    ignitionOn = false,
                    engineRunning = false,
                    throttleActive = false,
                    cruiseTargetKmh = null,
                    turnSignal = TurnSignalState.OFF,
                    lastToggleFeedback = "IGNITION OFF",
                )
            }
        }
        if (turningOn) {
            viewModelScope.launch { triggerBootSelfCheck() }
        }
    }

    fun onToggleSideStand() {
        _uiState.update { state ->
            val nextStandDown = !state.sideStandDown
            val cutsEngine = nextStandDown && state.gear > 0
            state.copy(
                sideStandDown = nextStandDown,
                engineRunning = if (cutsEngine) false else state.ignitionOn,
                throttleActive = if (cutsEngine) false else state.throttleActive,
                cruiseTargetKmh = if (cutsEngine) null else state.cruiseTargetKmh,
                interlockAlert = if (cutsEngine) "SIDE STAND DEPLOYED — ENGINE CUTOFF" else null,
                lastToggleFeedback = if (nextStandDown) "SIDE STAND DOWN" else "SIDE STAND UP",
            )
        }
    }

    fun onGearUp() {
        _uiState.update { state ->
            if (!state.ignitionOn) {
                return@update state.copy(lastToggleFeedback = "IGNITION IS OFF")
            }
            if (state.sideStandDown && state.gear == 0) {
                return@update state.copy(
                    interlockAlert = "RAISE SIDE STAND TO ENGAGE GEAR",
                    lastToggleFeedback = "INTERLOCK: SIDE STAND DOWN",
                )
            }
            val nextGear = (state.gear + 1).coerceAtMost(6)
            state.copy(
                gear = nextGear,
                interlockAlert = null,
                lastToggleFeedback = "GEAR → $nextGear",
            )
        }
    }

    fun onGearDown() {
        _uiState.update { state ->
            if (!state.ignitionOn) {
                return@update state.copy(lastToggleFeedback = "IGNITION IS OFF")
            }
            val nextGear = (state.gear - 1).coerceAtLeast(0)
            val label = if (nextGear == 0) "N" else nextGear.toString()
            state.copy(
                gear = nextGear,
                engineRunning = if (nextGear == 0 && state.ignitionOn) true else state.engineRunning,
                cruiseTargetKmh = if (nextGear == 0) null else state.cruiseTargetKmh,
                interlockAlert = null,
                lastToggleFeedback = "GEAR → $label",
            )
        }
    }

    fun onThrottleHold(held: Boolean) {
        _uiState.update { state ->
            if (!state.ignitionOn) return@update state
            if (held && state.sideStandDown && state.gear > 0) {
                return@update state.copy(
                    interlockAlert = "RAISE SIDE STAND TO RIDE",
                    lastToggleFeedback = "SIDE STAND INTERLOCK",
                )
            }
            state.copy(
                throttleActive = held,
                cruiseTargetKmh = if (held) null else state.cruiseTargetKmh,
                lastToggleFeedback = if (held) "THROTTLE OPEN" else state.lastToggleFeedback,
            )
        }
    }

    fun onCruiseStepToggle() {
        _uiState.update { state ->
            if (!state.ignitionOn) return@update state.copy(lastToggleFeedback = "TURN IGNITION ON FIRST")
            if (state.sideStandDown) {
                return@update state.copy(
                    interlockAlert = "RAISE SIDE STAND TO CRUISE",
                    lastToggleFeedback = "INTERLOCK: SIDE STAND DOWN",
                )
            }
            val nextGear = if (state.gear == 0) 2 else state.gear
            val nextCruise = when (state.cruiseTargetKmh) {
                null -> 45
                45 -> 82
                82 -> 110
                else -> null
            }
            val targetGear = when (nextCruise) {
                45 -> 3
                82 -> 5
                110 -> 6
                else -> nextGear
            }
            state.copy(
                gear = targetGear,
                engineRunning = true,
                cruiseTargetKmh = nextCruise,
                interlockAlert = null,
                lastToggleFeedback = if (nextCruise != null) "CRUISE SET → $nextCruise KM/H" else "CRUISE CANCELLED",
            )
        }
    }

    fun onBrakeHold(held: Boolean) {
        _uiState.update { state ->
            state.copy(
                brakeActive = held,
                cruiseTargetKmh = if (held) null else state.cruiseTargetKmh,
                lastToggleFeedback = if (held) "ABS BRAKING" else state.lastToggleFeedback,
            )
        }
    }

    fun onCycleRideMode() {
        _uiState.update { state ->
            val nextMode = state.rideMode.next()
            state.copy(
                rideMode = nextMode,
                lastToggleFeedback = "RIDE MODE → ${nextMode.displayName} (${nextMode.absSummary})",
            )
        }
        persistSettings()
    }

    fun onToggleLeftTurn() {
        _uiState.update { state ->
            val next = if (state.turnSignal == TurnSignalState.LEFT) TurnSignalState.OFF else TurnSignalState.LEFT
            state.copy(
                turnSignal = next,
                lastToggleFeedback = if (next == TurnSignalState.LEFT) "LEFT INDICATOR ON" else "INDICATOR OFF",
            )
        }
    }

    fun onToggleRightTurn() {
        _uiState.update { state ->
            val next = if (state.turnSignal == TurnSignalState.RIGHT) TurnSignalState.OFF else TurnSignalState.RIGHT
            state.copy(
                turnSignal = next,
                lastToggleFeedback = if (next == TurnSignalState.RIGHT) "RIGHT INDICATOR ON" else "INDICATOR OFF",
            )
        }
    }

    fun onToggleHazard() {
        _uiState.update { state ->
            val next = if (state.turnSignal == TurnSignalState.HAZARD) TurnSignalState.OFF else TurnSignalState.HAZARD
            state.copy(
                turnSignal = next,
                blinkOn = next == TurnSignalState.HAZARD,
                lastToggleFeedback = if (next == TurnSignalState.HAZARD) "HAZARD SYSTEM ACTIVE" else "HAZARD SYSTEM OFF",
            )
        }
    }

    // --- Full Bike-Button Menu & Cockpit Navigation (UP / DOWN / LEFT / RIGHT / SET / BACK) ---

    fun onSwitchgearUp() {
        val current = _uiState.value
        if (current.navigationState.showDestinationPicker) {
            val total = AdventureDestinations.presets.size
            val nextIdx = (current.navigationState.highlightedDestinationIndex - 1 + total) % total
            _uiState.update {
                it.copy(
                    navigationState = it.navigationState.copy(highlightedDestinationIndex = nextIdx),
                    lastToggleFeedback = "ROUTE → ${AdventureDestinations.presets[nextIdx].name}",
                )
            }
            return
        }
        if (current.callState.status == CallStatus.INCOMING) {
            onAttendCall()
            return
        }
        if (current.isFocusEditing) {
            when (current.focusedModule) {
                SwitchgearFocus.NAVIGATION -> onNextNavStep()
                SwitchgearFocus.TRIP_METER -> toggleActiveTrip()
                SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
                SwitchgearFocus.BRIGHTNESS -> onAdjustBrightness((current.screenBrightness + 0.1f).coerceAtMost(1.0f))
                SwitchgearFocus.MUSIC -> onAdjustVolume(10)
                SwitchgearFocus.CALL_HUD -> onTriggerIncomingCall()
            }
        } else {
            val prev = current.focusedModule.previous()
            _uiState.update {
                it.copy(
                    focusedModule = prev,
                    lastToggleFeedback = "FOCUS → ${prev.label} (PRESS SET)",
                )
            }
        }
    }

    fun onSwitchgearDown() {
        val current = _uiState.value
        if (current.navigationState.showDestinationPicker) {
            val total = AdventureDestinations.presets.size
            val nextIdx = (current.navigationState.highlightedDestinationIndex + 1) % total
            _uiState.update {
                it.copy(
                    navigationState = it.navigationState.copy(highlightedDestinationIndex = nextIdx),
                    lastToggleFeedback = "ROUTE → ${AdventureDestinations.presets[nextIdx].name}",
                )
            }
            return
        }
        if (current.callState.status == CallStatus.INCOMING) {
            onDeclineOrEndCall()
            return
        }
        if (current.isFocusEditing) {
            when (current.focusedModule) {
                SwitchgearFocus.NAVIGATION -> onPrevNavStep()
                SwitchgearFocus.TRIP_METER -> toggleActiveTrip()
                SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
                SwitchgearFocus.BRIGHTNESS -> onAdjustBrightness((current.screenBrightness - 0.1f).coerceAtLeast(0.15f))
                SwitchgearFocus.MUSIC -> onAdjustVolume(-10)
                SwitchgearFocus.CALL_HUD -> onDeclineOrEndCall()
            }
        } else {
            val next = current.focusedModule.next()
            _uiState.update {
                it.copy(
                    focusedModule = next,
                    lastToggleFeedback = "FOCUS → ${next.label} (PRESS SET)",
                )
            }
        }
    }

    fun onSwitchgearLeft() {
        val current = _uiState.value
        if (current.navigationState.showDestinationPicker) {
            onCloseDestinationPicker()
            return
        }
        if (current.isFocusEditing) {
            when (current.focusedModule) {
                SwitchgearFocus.NAVIGATION -> onPrevNavStep()
                SwitchgearFocus.MUSIC -> onMusicPrev()
                SwitchgearFocus.BRIGHTNESS -> onAdjustBrightness((current.screenBrightness - 0.1f).coerceAtLeast(0.15f))
                SwitchgearFocus.TRIP_METER -> toggleActiveTrip()
                SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
                SwitchgearFocus.CALL_HUD -> onDeclineOrEndCall()
            }
        } else {
            onToggleLeftTurn()
        }
    }

    fun onSwitchgearRight() {
        val current = _uiState.value
        if (current.navigationState.showDestinationPicker) {
            onSwitchgearSet()
            return
        }
        if (current.isFocusEditing) {
            when (current.focusedModule) {
                SwitchgearFocus.NAVIGATION -> onNextNavStep()
                SwitchgearFocus.MUSIC -> onMusicNext()
                SwitchgearFocus.BRIGHTNESS -> onAdjustBrightness((current.screenBrightness + 0.1f).coerceAtMost(1.0f))
                SwitchgearFocus.TRIP_METER -> toggleActiveTrip()
                SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
                SwitchgearFocus.CALL_HUD -> onTriggerIncomingCall()
            }
        } else {
            onToggleRightTurn()
        }
    }

    fun onSwitchgearSet() {
        val current = _uiState.value
        if (current.navigationState.showDestinationPicker) {
            val picked = AdventureDestinations.presets[current.navigationState.highlightedDestinationIndex]
            onStartNavigation(picked)
            return
        }
        if (current.callState.status == CallStatus.INCOMING) {
            onAttendCall()
            return
        }
        when (current.focusedModule) {
            SwitchgearFocus.NAVIGATION -> {
                if (!current.navigationState.isActive) {
                    onOpenDestinationPicker()
                } else if (!current.isFocusEditing) {
                    _uiState.update {
                        it.copy(
                            isFocusEditing = true,
                            lastToggleFeedback = "NAV EDIT • UP/DN STEP • SET LAUNCH G-MAPS",
                        )
                    }
                } else {
                    onRequestOpenGoogleMaps()
                }
            }
            SwitchgearFocus.TRIP_METER -> {
                if (current.isFocusEditing) {
                    toggleActiveTrip()
                } else {
                    _uiState.update {
                        it.copy(
                            isFocusEditing = true,
                            lastToggleFeedback = "TRIP EDIT • UP/DN SWITCH • HOLD SET RESET",
                        )
                    }
                }
            }
            SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
            SwitchgearFocus.BRIGHTNESS -> {
                val editing = !current.isFocusEditing
                _uiState.update {
                    it.copy(
                        isFocusEditing = editing,
                        lastToggleFeedback = if (editing) "BRIGHTNESS ADJUST • UP/DN" else "BRIGHTNESS SAVED",
                    )
                }
            }
            SwitchgearFocus.MUSIC -> onMusicPlayPause()
            SwitchgearFocus.CALL_HUD -> {
                when (current.callState.status) {
                    CallStatus.IDLE -> onTriggerIncomingCall()
                    CallStatus.INCOMING -> onAttendCall()
                    CallStatus.ACTIVE -> onDeclineOrEndCall()
                }
            }
        }
    }

    fun onSwitchgearSetLongPress() {
        val current = _uiState.value
        when (current.focusedModule) {
            SwitchgearFocus.NAVIGATION -> {
                if (current.navigationState.isActive) {
                    onOpenDestinationPicker()
                } else {
                    val defaultDest = AdventureDestinations.presets.first()
                    onStartNavigation(defaultDest)
                }
            }
            SwitchgearFocus.TRIP_METER -> onResetActiveTrip()
            SwitchgearFocus.BRIGHTNESS -> onToggleDayNight()
            SwitchgearFocus.MUSIC -> onMusicNext()
            SwitchgearFocus.RIDE_MODE -> onCycleRideMode()
            SwitchgearFocus.CALL_HUD -> onTriggerIncomingCall()
        }
    }

    fun onSwitchgearBack() {
        val current = _uiState.value
        if (current.showEcuDrawer) {
            _uiState.update { it.copy(showEcuDrawer = false) }
            return
        }
        if (current.navigationState.showDestinationPicker) {
            onCloseDestinationPicker()
            return
        }
        if (current.callState.status != CallStatus.IDLE) {
            onDeclineOrEndCall()
            return
        }
        if (current.isFocusEditing) {
            _uiState.update {
                it.copy(
                    isFocusEditing = false,
                    lastToggleFeedback = "MENU LOCKED • UP/DN SELECT MODULE",
                )
            }
        } else if (current.focusedModule == SwitchgearFocus.NAVIGATION && current.navigationState.isActive) {
            onStopNavigation()
        } else {
            _uiState.update {
                it.copy(
                    focusedModule = SwitchgearFocus.NAVIGATION,
                    lastToggleFeedback = "HOME • NAV CLUSTER FOCUSED",
                )
            }
        }
    }

    fun onSelectFocusModule(focus: SwitchgearFocus) {
        _uiState.update {
            it.copy(
                focusedModule = focus,
                isFocusEditing = true,
                lastToggleFeedback = "SELECTED → ${focus.label}",
            )
        }
    }

    // --- Turn-by-Turn Google Maps Navigation Controls ---

    fun onOpenDestinationPicker() {
        _uiState.update {
            it.copy(
                focusedModule = SwitchgearFocus.NAVIGATION,
                navigationState = it.navigationState.copy(showDestinationPicker = true),
                lastToggleFeedback = "PICK ROUTE • UP/DN BROWSE • SET START",
            )
        }
    }

    fun onCloseDestinationPicker() {
        _uiState.update {
            it.copy(
                navigationState = it.navigationState.copy(showDestinationPicker = false),
                lastToggleFeedback = "ROUTE PICKER CLOSED",
            )
        }
    }

    fun onStartNavigation(destination: NavDestination) {
        val gpsLat = _uiState.value.navigationState.gpsLat
        val gpsLng = _uiState.value.navigationState.gpsLng
        _uiState.update {
            it.copy(
                focusedModule = SwitchgearFocus.NAVIGATION,
                isFocusEditing = true,
                navigationState = it.navigationState.copy(
                    isLoadingRoute = true,
                    showDestinationPicker = false,
                    destination = destination,
                ),
                lastToggleFeedback = "FETCHING GOOGLE MAPS ROUTE...",
            )
        }

        viewModelScope.launch {
            val result = GoogleMapsNavigationService.fetchRoute(destination, gpsLat, gpsLng)
            val firstStep = result.steps.firstOrNull()
            _uiState.update { state ->
                state.copy(
                    navigationState = state.navigationState.copy(
                        isActive = true,
                        isLoadingRoute = false,
                        destination = destination,
                        steps = result.steps,
                        currentStepIndex = 0,
                        remainingStepMeters = (firstStep?.distanceMeters ?: 0).toFloat(),
                        routeSource = result.sourceLabel,
                    ),
                    lastToggleFeedback = "NAV ACTIVE → ${destination.name}",
                )
            }
            if (_uiState.value.navigationState.voiceEnabled && firstStep != null) {
                voiceSpeaker?.speak("Starting navigation to ${destination.name}. ${firstStep.instruction}")
            }
        }
    }

    fun onStartCustomDestination(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val custom = NavDestination(
            id = "custom_${trimmed.hashCode()}",
            name = trimmed,
            subtitle = "Google Maps Route",
            query = trimmed,
            defaultSteps = listOf(
                NavStep("Head toward $trimmed via main corridor", trimmed, 600, 50, ManeuverType.STRAIGHT),
                NavStep("Turn right onto connector road toward $trimmed", trimmed, 850, 75, ManeuverType.TURN_RIGHT),
                NavStep("At roundabout take exit toward $trimmed", trimmed, 450, 45, ManeuverType.ROUNDABOUT),
                NavStep("Arrive at $trimmed", trimmed, 150, 20, ManeuverType.ARRIVE),
            ),
        )
        onStartNavigation(custom)
    }

    fun onNextNavStep() {
        _uiState.update { state ->
            val nav = state.navigationState
            if (!nav.isActive || nav.steps.isEmpty()) return@update state
            val nextIdx = (nav.currentStepIndex + 1).coerceAtMost(nav.steps.size - 1)
            val step = nav.steps[nextIdx]
            if (nav.voiceEnabled) {
                voiceSpeaker?.speak(step.instruction)
            }
            state.copy(
                navigationState = nav.copy(
                    currentStepIndex = nextIdx,
                    remainingStepMeters = step.distanceMeters.toFloat(),
                ),
                lastToggleFeedback = "STEP ${nextIdx + 1}/${nav.steps.size} • ${step.instruction}",
            )
        }
    }

    fun onPrevNavStep() {
        _uiState.update { state ->
            val nav = state.navigationState
            if (!nav.isActive || nav.steps.isEmpty()) return@update state
            val prevIdx = (nav.currentStepIndex - 1).coerceAtLeast(0)
            val step = nav.steps[prevIdx]
            state.copy(
                navigationState = nav.copy(
                    currentStepIndex = prevIdx,
                    remainingStepMeters = step.distanceMeters.toFloat(),
                ),
                lastToggleFeedback = "STEP ${prevIdx + 1}/${nav.steps.size} • ${step.instruction}",
            )
        }
    }

    fun onToggleNavVoice() {
        _uiState.update { state ->
            val nextVoice = !state.navigationState.voiceEnabled
            state.copy(
                navigationState = state.navigationState.copy(voiceEnabled = nextVoice),
                lastToggleFeedback = if (nextVoice) "HELMET VOICE NAV ON" else "HELMET VOICE NAV MUTED",
            )
        }
    }

    fun onStopNavigation() {
        _uiState.update { state ->
            state.copy(
                isFocusEditing = false,
                navigationState = state.navigationState.copy(
                    isActive = false,
                    destination = null,
                    steps = emptyList(),
                    currentStepIndex = 0,
                    remainingStepMeters = 0f,
                ),
                lastToggleFeedback = "NAVIGATION ENDED",
            )
        }
    }

    fun onRequestOpenGoogleMaps() {
        val dest = _uiState.value.navigationState.destination ?: AdventureDestinations.presets.first()
        _launchMapsEvent.tryEmit(dest)
        _uiState.update {
            it.copy(lastToggleFeedback = "LAUNCHING GOOGLE MAPS → ${dest.name}")
        }
    }

    fun onUpdateGpsCoordinates(lat: Double?, lng: Double?, hasPermission: Boolean) {
        _uiState.update { state ->
            state.copy(
                navigationState = state.navigationState.copy(
                    gpsLat = lat,
                    gpsLng = lng,
                    hasLocationPermission = hasPermission,
                ),
                lastToggleFeedback = if (lat != null) "GPS LOCKED" else state.lastToggleFeedback,
            )
        }
    }

    // --- Trip / Brightness / Music / Call / ECU ---

    fun toggleActiveTrip() {
        _uiState.update { state ->
            val next = if (state.activeTrip == TripId.A) TripId.B else TripId.A
            state.copy(
                activeTrip = next,
                lastToggleFeedback = "ACTIVE → TRIP ${next.name}",
            )
        }
        persistSettings()
    }

    fun onResetActiveTrip() {
        _uiState.update { state ->
            if (state.activeTrip == TripId.A) {
                state.copy(
                    tripAKm = 0.0,
                    tripASeconds = 0L,
                    lastToggleFeedback = "TRIP A RESET TO 0.0 KM",
                )
            } else {
                state.copy(
                    tripBKm = 0.0,
                    tripBSeconds = 0L,
                    lastToggleFeedback = "TRIP B RESET TO 0.0 KM",
                )
            }
        }
        persistDistances()
    }

    fun onAdjustBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.15f, 1.0f)
        _uiState.update {
            it.copy(
                screenBrightness = clamped,
                lastToggleFeedback = "BRIGHTNESS → ${(clamped * 100).roundToInt()}%",
            )
        }
        persistSettings()
    }

    fun onToggleDayNight() {
        _uiState.update { state ->
            val nextNight = !state.isNightMode
            state.copy(
                isNightMode = nextNight,
                lastToggleFeedback = if (nextNight) "TFT THEME → NIGHT COCKPIT" else "TFT THEME → DAY RALLY",
            )
        }
        persistSettings()
    }

    fun onMusicPlayPause() {
        audioBridge?.dispatchPlayPause()
        _uiState.update { state ->
            val nextPlaying = !state.musicState.isPlaying
            state.copy(
                musicState = state.musicState.copy(isPlaying = nextPlaying),
                lastToggleFeedback = if (nextPlaying) "MUSIC → PLAYING" else "MUSIC → PAUSED",
            )
        }
    }

    fun onMusicNext() {
        audioBridge?.dispatchNext()
        _uiState.update { state ->
            val nextIdx = (state.musicState.currentIndex + 1) % state.musicState.tracks.size
            val track = state.musicState.tracks[nextIdx]
            state.copy(
                musicState = state.musicState.copy(currentIndex = nextIdx, elapsedSeconds = 0, isPlaying = true),
                lastToggleFeedback = "TRACK → ${track.title}",
            )
        }
    }

    fun onMusicPrev() {
        audioBridge?.dispatchPrevious()
        _uiState.update { state ->
            val prevIdx = (state.musicState.currentIndex - 1 + state.musicState.tracks.size) % state.musicState.tracks.size
            val track = state.musicState.tracks[prevIdx]
            state.copy(
                musicState = state.musicState.copy(currentIndex = prevIdx, elapsedSeconds = 0, isPlaying = true),
                lastToggleFeedback = "TRACK → ${track.title}",
            )
        }
    }

    fun onAdjustVolume(delta: Int) {
        audioBridge?.adjustMusicVolume(delta)
        _uiState.update { state ->
            val nextVol = (state.musicState.volumePercent + delta).coerceIn(0, 100)
            state.copy(
                musicState = state.musicState.copy(volumePercent = nextVol),
                lastToggleFeedback = "MEDIA VOL → $nextVol%",
            )
        }
    }

    fun onTriggerIncomingCall() {
        _uiState.update { state ->
            state.copy(
                focusedModule = SwitchgearFocus.CALL_HUD,
                callState = state.callState.copy(
                    status = CallStatus.INCOMING,
                    durationSeconds = 0,
                ),
                lastToggleFeedback = "INCOMING CALL • PRESS SET TO ATTEND",
            )
        }
    }

    fun onAttendCall() {
        _uiState.update { state ->
            val wasPlaying = state.musicState.isPlaying
            state.copy(
                callState = state.callState.copy(status = CallStatus.ACTIVE, durationSeconds = 0),
                musicState = if (wasPlaying) state.musicState.copy(isPlaying = false) else state.musicState,
                lastToggleFeedback = "CALL CONNECTED • BT HELMET AUDIO",
            )
        }
    }

    fun onDeclineOrEndCall() {
        _uiState.update { state ->
            val msg = if (state.callState.status == CallStatus.INCOMING) "CALL DECLINED" else "CALL ENDED"
            state.copy(
                callState = state.callState.copy(status = CallStatus.IDLE, durationSeconds = 0),
                lastToggleFeedback = msg,
            )
        }
    }

    fun onToggleEcuDrawer() {
        _uiState.update { it.copy(showEcuDrawer = !it.showEcuDrawer) }
    }

    fun onAdjustFuel(percent: Float) {
        val clamped = percent.coerceIn(0.03f, 1.0f)
        _uiState.update {
            it.copy(
                fuelPercent = clamped,
                lastToggleFeedback = "FUEL LEVEL → ${(clamped * 100).roundToInt()}%",
            )
        }
        persistDistances()
    }

    fun onSetCustomBattery(voltage: Float?) {
        customBatteryOverride = voltage
        _uiState.update {
            it.copy(
                batteryVoltage = voltage ?: if (it.engineRunning) 14.1f else 12.7f,
                lastToggleFeedback = if (voltage != null) "BATTERY → ${String.format("%.1f", voltage)}V" else "BATTERY AUTO",
            )
        }
    }

    fun onSetCustomTemp(tempC: Int?) {
        customTempOverride = tempC
        _uiState.update {
            it.copy(
                coolantTempC = tempC ?: 84,
                lastToggleFeedback = if (tempC != null) "COOLANT → ${tempC}°C" else "COOLANT AUTO",
            )
        }
    }

    private fun persistDistances() {
        val repo = repository ?: return
        val s = _uiState.value
        viewModelScope.launch {
            repo.saveDistances(
                odometerKm = s.odometerKm,
                tripAKm = s.tripAKm,
                tripASeconds = s.tripASeconds,
                tripBKm = s.tripBKm,
                tripBSeconds = s.tripBSeconds,
                fuelPercent = s.fuelPercent,
            )
        }
    }

    private fun persistSettings() {
        val repo = repository ?: return
        val s = _uiState.value
        viewModelScope.launch {
            repo.saveSettings(
                activeTrip = s.activeTrip,
                rideMode = s.rideMode,
                screenBrightness = s.screenBrightness,
                isNightMode = s.isNightMode,
            )
        }
    }

    class Factory(
        private val repository: DashboardPreferencesRepository,
        private val audioBridge: HardwareAudioBridge,
        private val voiceSpeaker: VoiceGuidanceSpeaker,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, audioBridge, voiceSpeaker) as T
        }
    }
}
