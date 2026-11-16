package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.RideMode
import com.example.model.TripId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dashboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yezdi_dashboard_prefs",
)

data class PersistedTelemetry(
    val odometerKm: Double = 1428.6,
    val tripAKm: Double = 124.4,
    val tripASeconds: Long = 5820L,
    val tripBKm: Double = 412.8,
    val tripBSeconds: Long = 18900L,
    val activeTrip: TripId = TripId.A,
    val fuelPercent: Float = 0.74f,
    val rideMode: RideMode = RideMode.ROAD,
    val screenBrightness: Float = 0.85f,
    val isNightMode: Boolean = true,
)

class DashboardPreferencesRepository(private val context: Context) {

    private object Keys {
        val ODOMETER_KM = doublePreferencesKey("odometer_km")
        val TRIP_A_KM = doublePreferencesKey("trip_a_km")
        val TRIP_A_SEC = longPreferencesKey("trip_a_sec")
        val TRIP_B_KM = doublePreferencesKey("trip_b_km")
        val TRIP_B_SEC = longPreferencesKey("trip_b_sec")
        val ACTIVE_TRIP = stringPreferencesKey("active_trip")
        val FUEL_PERCENT = floatPreferencesKey("fuel_percent")
        val RIDE_MODE = stringPreferencesKey("ride_mode")
        val BRIGHTNESS = floatPreferencesKey("screen_brightness")
        val NIGHT_MODE = booleanPreferencesKey("is_night_mode")
    }

    val telemetryFlow: Flow<PersistedTelemetry> = context.dashboardDataStore.data.map { prefs ->
        val modeName = prefs[Keys.RIDE_MODE] ?: RideMode.ROAD.name
        val tripName = prefs[Keys.ACTIVE_TRIP] ?: TripId.A.name
        PersistedTelemetry(
            odometerKm = prefs[Keys.ODOMETER_KM] ?: 1428.6,
            tripAKm = prefs[Keys.TRIP_A_KM] ?: 124.4,
            tripASeconds = prefs[Keys.TRIP_A_SEC] ?: 5820L,
            tripBKm = prefs[Keys.TRIP_B_KM] ?: 412.8,
            tripBSeconds = prefs[Keys.TRIP_B_SEC] ?: 18900L,
            activeTrip = runCatching { TripId.valueOf(tripName) }.getOrDefault(TripId.A),
            fuelPercent = (prefs[Keys.FUEL_PERCENT] ?: 0.74f).coerceIn(0.02f, 1.0f),
            rideMode = runCatching { RideMode.valueOf(modeName) }.getOrDefault(RideMode.ROAD),
            screenBrightness = (prefs[Keys.BRIGHTNESS] ?: 0.85f).coerceIn(0.15f, 1.0f),
            isNightMode = prefs[Keys.NIGHT_MODE] ?: true,
        )
    }

    suspend fun saveDistances(
        odometerKm: Double,
        tripAKm: Double,
        tripASeconds: Long,
        tripBKm: Double,
        tripBSeconds: Long,
        fuelPercent: Float,
    ) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.ODOMETER_KM] = odometerKm
            prefs[Keys.TRIP_A_KM] = tripAKm
            prefs[Keys.TRIP_A_SEC] = tripASeconds
            prefs[Keys.TRIP_B_KM] = tripBKm
            prefs[Keys.TRIP_B_SEC] = tripBSeconds
            prefs[Keys.FUEL_PERCENT] = fuelPercent.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun saveSettings(
        activeTrip: TripId,
        rideMode: RideMode,
        screenBrightness: Float,
        isNightMode: Boolean,
    ) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_TRIP] = activeTrip.name
            prefs[Keys.RIDE_MODE] = rideMode.name
            prefs[Keys.BRIGHTNESS] = screenBrightness.coerceIn(0.15f, 1.0f)
            prefs[Keys.NIGHT_MODE] = isNightMode
        }
    }
}
