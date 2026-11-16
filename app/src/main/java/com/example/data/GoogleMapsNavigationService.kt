package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.model.ManeuverType
import com.example.model.NavDestination
import com.example.model.NavStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class DirectionsFetchResult(
    val steps: List<NavStep>,
    val sourceLabel: String,
)

object GoogleMapsNavigationService {

    fun hasValidDirectionsApiKey(): Boolean {
        val key = BuildConfig.GOOGLE_MAPS_API_KEY
        return key.isNotBlank() && key != "YOUR_GOOGLE_MAPS_API_KEY"
    }

    suspend fun fetchRoute(
        destination: NavDestination,
        originLat: Double?,
        originLng: Double?,
    ): DirectionsFetchResult = withContext(Dispatchers.IO) {
        if (!hasValidDirectionsApiKey()) {
            return@withContext DirectionsFetchResult(
                steps = destination.defaultSteps,
                sourceLabel = "GOOGLE MAPS LINKED",
            )
        }

        val originStr = if (originLat != null && originLng != null) {
            "$originLat,$originLng"
        } else {
            "12.9716,77.5946"
        }

        val destStr = if (destination.latitude != null && destination.longitude != null) {
            "${destination.latitude},${destination.longitude}"
        } else {
            URLEncoder.encode(destination.query, "UTF-8")
        }

        val urlStr = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=$originStr&destination=$destStr&mode=driving&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"

        val parsed = runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            conn.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                if (json.optString("status") != "OK") return@runCatching null
                val routes = json.optJSONArray("routes") ?: return@runCatching null
                if (routes.length() == 0) return@runCatching null
                val legs = routes.getJSONObject(0).optJSONArray("legs") ?: return@runCatching null
                if (legs.length() == 0) return@runCatching null
                val stepsJson = legs.getJSONObject(0).optJSONArray("steps") ?: return@runCatching null

                val list = mutableListOf<NavStep>()
                for (i in 0 until stepsJson.length()) {
                    val obj = stepsJson.getJSONObject(i)
                    val html = obj.optString("html_instructions", "Continue")
                    val cleanText = html.replace(Regex("<[^>]*>"), " ")
                        .replace("&nbsp;", " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    val dist = obj.optJSONObject("distance")?.optInt("value", 250) ?: 250
                    val dur = obj.optJSONObject("duration")?.optInt("value", 30) ?: 30
                    val maneuverStr = obj.optString("maneuver", "")
                    val isLast = i == stepsJson.length() - 1
                    val maneuver = if (isLast && maneuverStr.isBlank()) {
                        ManeuverType.ARRIVE
                    } else {
                        ManeuverType.fromGoogleManeuver(maneuverStr)
                    }
                    list.add(
                        NavStep(
                            instruction = cleanText,
                            roadName = destination.name,
                            distanceMeters = dist.coerceAtLeast(50),
                            durationSeconds = dur.coerceAtLeast(10),
                            maneuver = maneuver,
                        ),
                    )
                }
                list.ifEmpty { null }
            }
        }.getOrNull()

        if (parsed != null) {
            DirectionsFetchResult(steps = parsed, sourceLabel = "GOOGLE DIRECTIONS API")
        } else {
            DirectionsFetchResult(steps = destination.defaultSteps, sourceLabel = "GOOGLE MAPS LINKED")
        }
    }

    fun launchGoogleMapsTurnByTurn(context: Context, destination: NavDestination): Boolean {
        val target = if (destination.latitude != null && destination.longitude != null) {
            "${destination.latitude},${destination.longitude}"
        } else {
            Uri.encode(destination.query)
        }
        val navIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$target&mode=d"),
        ).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pm = context.packageManager
        if (navIntent.resolveActivity(pm) != null) {
            context.startActivity(navIntent)
            return true
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$target&travelmode=driving"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(webIntent)
            true
        }.getOrDefault(false)
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best?.let { it.latitude to it.longitude }
    }
}

class VoiceGuidanceSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    ready = true
                }
            }
        }
    }

    fun speak(message: String) {
        if (!ready) return
        runCatching {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "yezdi_nav_${System.currentTimeMillis()}")
        }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
