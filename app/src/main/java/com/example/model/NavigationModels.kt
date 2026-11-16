package com.example.model

enum class ManeuverType {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    ROUNDABOUT,
    U_TURN,
    ARRIVE;

    companion object {
        fun fromGoogleManeuver(raw: String?): ManeuverType {
            return when (raw?.lowercase()) {
                "turn-left" -> TURN_LEFT
                "turn-right" -> TURN_RIGHT
                "turn-slight-left", "fork-left", "keep-left", "ramp-left" -> SLIGHT_LEFT
                "turn-slight-right", "fork-right", "keep-right", "ramp-right" -> SLIGHT_RIGHT
                "turn-sharp-left" -> SHARP_LEFT
                "turn-sharp-right" -> SHARP_RIGHT
                "roundabout-left", "roundabout-right" -> ROUNDABOUT
                "uturn-left", "uturn-right" -> U_TURN
                else -> STRAIGHT
            }
        }
    }
}

data class NavStep(
    val instruction: String,
    val roadName: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val maneuver: ManeuverType,
)

data class NavDestination(
    val id: String,
    val name: String,
    val subtitle: String,
    val query: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val defaultSteps: List<NavStep>,
)

data class NavigationState(
    val isActive: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val destination: NavDestination? = null,
    val steps: List<NavStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val remainingStepMeters: Float = 0f,
    val voiceEnabled: Boolean = true,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val hasLocationPermission: Boolean = false,
    val routeSource: String = "GOOGLE MAPS",
    val showDestinationPicker: Boolean = false,
    val highlightedDestinationIndex: Int = 0,
    val customQuery: String = "",
) {
    val currentStep: NavStep?
        get() = steps.getOrNull(currentStepIndex)

    val nextStep: NavStep?
        get() = steps.getOrNull(currentStepIndex + 1)

    val totalRemainingMeters: Int
        get() {
            if (!isActive || steps.isEmpty()) return 0
            var total = remainingStepMeters.toInt().coerceAtLeast(0)
            for (i in (currentStepIndex + 1) until steps.size) {
                total += steps[i].distanceMeters
            }
            return total
        }

    val totalRemainingMinutes: Int
        get() {
            if (!isActive || steps.isEmpty()) return 0
            val current = currentStep
            val currentRatio = if (current != null && current.distanceMeters > 0) {
                (remainingStepMeters / current.distanceMeters.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            var sec = ((current?.durationSeconds ?: 0) * currentRatio).toInt()
            for (i in (currentStepIndex + 1) until steps.size) {
                sec += steps[i].durationSeconds
            }
            return (sec / 60).coerceAtLeast(1)
        }
}

object AdventureDestinations {
    val presets: List<NavDestination> = listOf(
        NavDestination(
            id = "nandi_hills",
            name = "Nandi Hills Summit",
            subtitle = "Mountain Switchbacks • 58 km",
            query = "Nandi Hills, Karnataka",
            latitude = 13.3702,
            longitude = 77.6835,
            defaultSteps = listOf(
                NavStep("Head north on Bellary Rd / NH-44", "NH-44", 650, 55, ManeuverType.STRAIGHT),
                NavStep("Keep left toward Devanahalli Airport Hwy", "NH-44", 1200, 90, ManeuverType.SLIGHT_LEFT),
                NavStep("Take the exit right onto Nandi Hills Rd", "Nandi Hills Rd", 850, 75, ManeuverType.TURN_RIGHT),
                NavStep("At the roundabout, take the 2nd exit", "Karahalli Cross", 420, 45, ManeuverType.ROUNDABOUT),
                NavStep("Sharp left up the Nandi Hairpin Ascent", "Nandi Ghat Rd", 1450, 140, ManeuverType.SHARP_LEFT),
                NavStep("Arrive at Nandi Hills Viewpoint Parking", "Summit Plaza", 180, 30, ManeuverType.ARRIVE),
            ),
        ),
        NavDestination(
            id = "leh_khardung",
            name = "Khardung La Pass",
            subtitle = "High-Altitude Trail • 39 km",
            query = "Khardung La, Ladakh",
            latitude = 34.2787,
            longitude = 77.6047,
            defaultSteps = listOf(
                NavStep("Ride north on Khardung La Rd", "Leh-Nubra Hwy", 900, 85, ManeuverType.STRAIGHT),
                NavStep("Turn right past South Pullu Checkpost", "High Altitude Pass", 1600, 180, ManeuverType.TURN_RIGHT),
                NavStep("Bear slight left onto gravel ridge track", "Khardung Trail", 1100, 150, ManeuverType.SLIGHT_LEFT),
                NavStep("Sharp right climb toward Summit Marker", "Khardung La Top", 550, 80, ManeuverType.SHARP_RIGHT),
                NavStep("Arrive at Khardung La Summit (5,359m)", "Summit Pass", 150, 25, ManeuverType.ARRIVE),
            ),
        ),
        NavDestination(
            id = "lonavala_ghat",
            name = "Lonavala Tiger Point",
            subtitle = "Western Ghats Twisties • 44 km",
            query = "Tiger Point, Lonavala",
            latitude = 18.6787,
            longitude = 73.3854,
            defaultSteps = listOf(
                NavStep("Continue on Old Mumbai-Pune Hwy", "NH-48", 800, 65, ManeuverType.STRAIGHT),
                NavStep("Turn left toward INS Shivaji / Bhushi Dam", "INS Shivaji Rd", 680, 60, ManeuverType.TURN_LEFT),
                NavStep("Slight right onto Amby Valley Ghat Rd", "Amby Valley Rd", 1300, 115, ManeuverType.SLIGHT_RIGHT),
                NavStep("Arrive at Tiger Point Cliff Overlook", "Tiger Point", 200, 25, ManeuverType.ARRIVE),
            ),
        ),
        NavDestination(
            id = "fuel_station",
            name = "Nearest Fuel Station",
            subtitle = "Quick Refuel Stop • 3.2 km",
            query = "petrol pump near me",
            defaultSteps = listOf(
                NavStep("Continue straight for 450 m", "Main Arterial", 450, 40, ManeuverType.STRAIGHT),
                NavStep("Turn right at Junction Signal", "Station Link Rd", 520, 50, ManeuverType.TURN_RIGHT),
                NavStep("Turn left into Fuel Forecourt", "Fuel Station", 240, 30, ManeuverType.TURN_LEFT),
                NavStep("Arrive at Nearest Fuel Pump", "Forecourt", 80, 15, ManeuverType.ARRIVE),
            ),
        ),
        NavDestination(
            id = "yezdi_service",
            name = "Yezdi Service Center",
            subtitle = "Authorized Workshop • 7.4 km",
            query = "Jawa Yezdi service center near me",
            defaultSteps = listOf(
                NavStep("Head east toward Ring Road", "Outer Ring Rd", 700, 60, ManeuverType.STRAIGHT),
                NavStep("At the roundabout, take the 1st exit", "Service Access Rd", 480, 45, ManeuverType.ROUNDABOUT),
                NavStep("Turn left onto Auto Hub Lane", "Auto Hub", 350, 35, ManeuverType.TURN_LEFT),
                NavStep("Arrive at Jawa Yezdi Workshop", "Service Bay", 100, 15, ManeuverType.ARRIVE),
            ),
        ),
    )
}
