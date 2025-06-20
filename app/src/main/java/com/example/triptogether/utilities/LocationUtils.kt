package com.example.triptogether.utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LocationUtils {

    suspend fun getCoordinatesFromLocationName(location: String, apiKey: String): Pair<Double, Double>? {
        val encodedLocation = java.net.URLEncoder.encode(location, "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedLocation&key=$apiKey"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream.bufferedReader().use { it.readText() }

                // תוסיפי את זה לזיהוי באגים:
                android.util.Log.d("GeocodingResponse", inputStream)

                val json = JSONObject(inputStream)

                val results = json.getJSONArray("results")
                if (results.length() > 0) {
                    val locationObj = results.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONObject("location")
                    val lat = locationObj.getDouble("lat")
                    val lng = locationObj.getDouble("lng")
                    return@withContext Pair(lat, lng)
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}
