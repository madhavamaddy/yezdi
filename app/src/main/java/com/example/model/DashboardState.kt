package com.example.model

enum class RideMode(
    val displayName: String,
    val absSummary: String,
    val efficiencyKmPerL: Float,
    val torqueFactor: Float,
) {
    ROAD(
        displayName = "ROAD",
        absSummary = "DUAL ABS ON",
        efficiencyKmPerL = 28.0f,
        torqueFactor = 1.0f,
    ),
    RAIN(
        displayName = "RAIN",
        absSummary = "RAIN ABS HIGH",
        efficiencyKmPerL = 30.5f,
        torqueFactor = 0.78f,
    ),
    OFF_ROAD(
        displayName = "OFF-ROAD",
        absSummary = "REAR ABS OFF",
        efficiencyKmPerL = 22.5f,
        torqueFactor = 1.06f,
    );

    fun next(): RideMode = when (this) {
        ROAD -> RAIN
        RAIN -> OFF_ROAD
        OFF_ROAD -> ROAD
    }
}

enum class TurnSignalState {
    OFF,
    LEFT,
    RIGHT,
    HAZARD,
}

enum class TripId {
    A,
    B,
}

enum class ShiftCue {
    NONE,
    UPSHIFT,
    DOWNSHIFT,
}

enum class SwitchgearFocus(val label: String) {
    NAVIGATION("NAV"),
    TRIP_METER("TRIP"),
    RIDE_MODE("MODE"),
    BRIGHTNESS("DISPLAY"),
    MUSIC("MUSIC"),
    CALL_HUD("CALL");

    fun next(): SwitchgearFocus {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    fun previous(): SwitchgearFocus {
        val all = entries
        return all[(ordinal - 1 + all.size) % all.size]
    }
}

enum class CallStatus {
    IDLE,
    INCOMING,
    ACTIVE,
}

data class CallState(
    val status: CallStatus = CallStatus.IDLE,
    val callerName: String = "Vikram (Trail Lead)",
    val callerNumber: String = "+91 98450 22190",
    val durationSeconds: Int = 0,
)

data class MusicTrack(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
)

data class MusicState(
    val tracks: List<MusicTrack> = defaultTracks,
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val elapsedSeconds: Int = 42,
    val volumePercent: Int = 70,
) {
    val currentTrack: MusicTrack
        get() = tracks[currentIndex.coerceIn(tracks.indices)]

    companion object {
        val defaultTracks = listOf(
            MusicTrack("Ladakh Horizon", "Highway Nomads", 234),
            MusicTrack("Single Cylinder Thunder", "Moto FM", 198),
            MusicTrack("Spiti Valley Dawn", "Acoustic Trails", 265),
            MusicTrack("Midnight Tarmac", "Apex Radio", 212),
        )
    }
}

data class DashboardUiState(
    val ignitionOn: Boolean = true,
    val bootSweepProgress: Float = 0f,
    val isBooting: Boolean = false,
    val engineRunning: Boolean = true,
    val speedKmh: Float = 0f,
    val rpm: Int = 1450,
    val maxRpm: Int = 10000,
    val redlineRpm: Int = 8000,
    val gear: Int = 0, // 0 = Neutral (N), 1..6 = Gears
    val throttleActive: Boolean = false,
    val brakeActive: Boolean = false,
    val cruiseTargetKmh: Int? = null,
    val sideStandDown: Boolean = false,
    val interlockAlert: String? = null,
    val rideMode: RideMode = RideMode.ROAD,
    val absSlipActive: Boolean = false,
    val turnSignal: TurnSignalState = TurnSignalState.OFF,
    val blinkOn: Boolean = false,
    val odometerKm: Double = 1428.6,
    val activeTrip: TripId = TripId.A,
    val tripAKm: Double = 124.4,
    val tripASeconds: Long = 5820L,
    val tripBKm: Double = 412.8,
    val tripBSeconds: Long = 18900L,
    val fuelPercent: Float = 0.74f, // 0f..1f of 15.5L tank
    val tankCapacityLiters: Float = 15.5f,
    val batteryVoltage: Float = 14.1f,
    val coolantTempC: Int = 84,
    val ambientTempC: Int = 26,
    val screenBrightness: Float = 0.85f,
    val isNightMode: Boolean = true,
    val focusedModule: SwitchgearFocus = SwitchgearFocus.NAVIGATION,
    val isFocusEditing: Boolean = false,
    val musicState: MusicState = MusicState(),
    val callState: CallState = CallState(),
    val navigationState: NavigationState = NavigationState(),
    val showEcuDrawer: Boolean = false,
    val lastToggleFeedback: String = "BIKE BUTTONS READY • DPAD / TOGGLES",
) {
    val gearLabel: String
        get() = if (gear == 0) "N" else gear.toString()

    val fuelLiters: Float
        get() = fuelPercent * tankCapacityLiters

    val isLowFuel: Boolean
        get() = fuelPercent <= 0.18f

    val distanceToEmptyKm: Int
        get() = (fuelLiters * rideMode.efficiencyKmPerL).toInt().coerceAtLeast(0)

    val isHighTemp: Boolean
        get() = coolantTempC >= 102

    val isLowBattery: Boolean
        get() = batteryVoltage < 12.0f

    val shiftCue: ShiftCue
        get() = when {
            !engineRunning || gear == 0 -> ShiftCue.NONE
            rpm >= 7400 && gear < 6 -> ShiftCue.UPSHIFT
            rpm <= 2300 && gear > 1 && speedKmh > 12f -> ShiftCue.DOWNSHIFT
            else -> ShiftCue.NONE
        }

    val activeTripKm: Double
        get() = if (activeTrip == TripId.A) tripAKm else tripBKm

    val activeTripSeconds: Long
        get() = if (activeTrip == TripId.A) tripASeconds else tripBSeconds

    val activeTripAvgKmh: Int
        get() {
            val hours = activeTripSeconds / 3600.0
            return if (hours > 0.01) (activeTripKm / hours).toInt() else 0
        }
}
