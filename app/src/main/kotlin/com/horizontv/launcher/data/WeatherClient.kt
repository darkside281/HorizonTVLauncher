package com.horizontv.launcher.data

import com.horizontv.launcher.model.DailyForecast
import com.horizontv.launcher.model.WeatherCondition
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherLocation
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface WeatherDataSource {
  suspend fun searchLocations(query: String): Result<List<WeatherLocation>>
  suspend fun fetchForecast(
    locationName: String,
    latitude: Double,
    longitude: Double,
    useFahrenheit: Boolean,
  ): Result<WeatherData>
}

class WeatherClient : WeatherDataSource {

  override suspend fun searchLocations(query: String): Result<List<WeatherLocation>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleaned = query.trim()
        require(cleaned.length >= 2) { "Enter a city or town name (at least 2 letters)" }
        val encoded = URLEncoder.encode(cleaned, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
        val json = JSONObject(httpGet(url))
        val results = json.optJSONArray("results") ?: return@runCatching emptyList()
        buildList {
          for (i in 0 until results.length()) {
            val obj = results.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue
            val admin1 = obj.optString("admin1", "").trim()
            val country = obj.optString("country", "").trim()
            val lat = obj.optDouble("latitude", Double.NaN)
            val lon = obj.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            add(
              WeatherLocation(
                name = name,
                region = admin1,
                country = country,
                latitude = lat,
                longitude = lon,
              ),
            )
          }
        }
      }
    }

  override suspend fun fetchForecast(
    locationName: String,
    latitude: Double,
    longitude: Double,
    useFahrenheit: Boolean,
  ): Result<WeatherData> = withContext(Dispatchers.IO) {
    runCatching {
      val unitParam = if (useFahrenheit) "fahrenheit" else "celsius"
      val url = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=$latitude&longitude=$longitude" +
        "&current=temperature_2m,weather_code" +
        "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
        "&temperature_unit=$unitParam" +
        "&forecast_days=7" +
        "&timezone=auto"

      val json = JSONObject(httpGet(url))
      val current = json.getJSONObject("current")
      val currentTemp = current.getDouble("temperature_2m").roundToInt()
      val currentCode = current.optInt("weather_code", 0)

      val dailyObj = json.getJSONObject("daily")
      val dates = dailyObj.getJSONArray("time")
      val codes = dailyObj.getJSONArray("weather_code")
      val maxTemps = dailyObj.getJSONArray("temperature_2m_max")
      val minTemps = dailyObj.getJSONArray("temperature_2m_min")

      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())

      val forecasts = buildList {
        val count = minOf(dates.length(), codes.length(), maxTemps.length(), minTemps.length(), 7)
        for (i in 0 until count) {
          val dateStr = dates.optString(i, "")
          val dayLabel = if (i == 0) {
            "Today"
          } else {
            runCatching {
              val parsed = parser.parse(dateStr)
              if (parsed != null) dayFmt.format(parsed) else "D+${i}"
            }.getOrDefault("D+$i")
          }
          add(
            DailyForecast(
              dayLabel = dayLabel,
              condition = mapWmoCode(codes.optInt(i, 0)),
              tempMax = maxTemps.optDouble(i, 0.0).roundToInt(),
              tempMin = minTemps.optDouble(i, 0.0).roundToInt(),
            ),
          )
        }
      }

      WeatherData(
        locationName = locationName,
        currentTemp = currentTemp,
        condition = mapWmoCode(currentCode),
        unitSymbol = if (useFahrenheit) "°F" else "°C",
        daily = forecasts,
      )
    }
  }

  private fun httpGet(urlString: String): String {
    val conn = try {
      (URL(urlString).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "HorizonTVLauncher/1.7.3")
      }
    } catch (e: UnknownHostException) {
      throw IllegalStateException(
        "Network/DNS blocked for Horizon — check Wi-Fi or Android Settings → Apps → Horizon TV Launcher → Mobile data & Wi-Fi → Allow network access.",
      )
    }
    try {
      val code = try {
        conn.responseCode
      } catch (e: UnknownHostException) {
        throw IllegalStateException(
          "Network/DNS blocked for Horizon — check Wi-Fi or Android Settings → Apps → Horizon TV Launcher → Mobile data & Wi-Fi → Allow network access.",
        )
      }
      if (code !in 200..299) {
        throw IllegalStateException("Weather service returned HTTP $code")
      }
      return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
    } finally {
      conn.disconnect()
    }
  }

  companion object {
    fun mapWmoCode(code: Int): WeatherCondition = when (code) {
      0 -> WeatherCondition.CLEAR
      1, 2 -> WeatherCondition.PARTLY_CLOUDY
      3 -> WeatherCondition.CLOUDY
      45, 48 -> WeatherCondition.FOG
      in 51..57 -> WeatherCondition.DRIZZLE
      in 61..67, in 80..82 -> WeatherCondition.RAIN
      in 71..77, 85, 86 -> WeatherCondition.SNOW
      95, 96, 99 -> WeatherCondition.STORM
      else -> WeatherCondition.PARTLY_CLOUDY
    }
  }
}
