package com.ryzumi.miraiai.domain.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

object DeviceContextManager {

    private data class WeatherCache(
        val timestamp: Long,
        val weatherSummary: String
    )

    data class ResolvedLocation(
        val latitude: Double,
        val longitude: Double,
        val locationName: String,
        val source: String, // "GPS", "Network", "IP Geolocation"
        val timestamp: Long = System.currentTimeMillis()
    )

    private val weatherCacheMap = ConcurrentHashMap<String, WeatherCache>()
    private var lastResolvedLocation: ResolvedLocation? = null
    private const val WEATHER_CACHE_DURATION_MS = 10 * 60 * 1000L // 10 Minutes
    private const val LOCATION_CACHE_DURATION_MS = 10 * 60 * 1000L // 10 Minutes

    fun getClockStatus(): String {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(now.time)
        val timezone = TimeZone.getDefault()
        val tzStr = "${timezone.id} (${timezone.getDisplayName(timezone.inDaylightTime(Date()), TimeZone.SHORT)})"
        return "Current Date & Time: $dateStr, Timezone: $tzStr"
    }

    suspend fun getHardwareAndBatteryStatus(context: Context): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = Build.MODEL
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})"
        val battery = getBatteryInfo(context)
        val network = getDetailedNetworkSummary(context)
        return "Device: $manufacturer $model, OS: $androidVersion, Battery: $battery\nNetwork:\n$network"
    }

    suspend fun getLocationAndWeatherStatus(context: Context): String = withContext(Dispatchers.IO) {
        val resolved = resolveBestLocation(context)
        if (resolved != null) {
            val weather = fetchLiveWeather(resolved.latitude, resolved.longitude)
                ?: "Weather service currently updating (Location: ${resolved.locationName})"
            "Location: ${resolved.locationName} (Lat: ${String.format(Locale.US, "%.4f", resolved.latitude)}, Lon: ${String.format(Locale.US, "%.4f", resolved.longitude)}, via ${resolved.source}), Live Weather: $weather"
        } else {
            "Location: Location currently unavailable. Weather: Unable to determine without location."
        }
    }

    /**
     * Gathers real-time OS, device hardware, battery, location, and weather information.
     */
    suspend fun getLiveDeviceContext(context: Context): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()

        // 1. Current Time & Date
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(now.time)
        val timezone = TimeZone.getDefault()
        val tzStr = "${timezone.id} (${timezone.getDisplayName(timezone.inDaylightTime(Date()), TimeZone.SHORT)})"

        sb.append("- Current Local Time: $dateStr\n")
        sb.append("- Timezone: $tzStr\n")

        // 2. Device Hardware & OS
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = Build.MODEL
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})"
        sb.append("- Device Model: $manufacturer $model\n")
        sb.append("- Operating System: $androidVersion\n")

        // 3. Battery Status & Power Management
        val batteryStatus = getBatteryInfo(context)
        sb.append("- Battery: $batteryStatus\n")

        // 4. Detailed Network Status (Local IP, Public IP, Wi-Fi SSID, Cellular)
        val networkStatus = getDetailedNetworkSummary(context)
        sb.append("- Network:\n")
        networkStatus.lines().forEach { line ->
            sb.append("  * $line\n")
        }

        // 5. GPS / Network / IP Location & Live Weather
        val resolved = resolveBestLocation(context)
        if (resolved != null) {
            sb.append("- Location: ${resolved.locationName} (Lat: ${String.format(Locale.US, "%.4f", resolved.latitude)}, Lon: ${String.format(Locale.US, "%.4f", resolved.longitude)}, source: ${resolved.source})\n")
            val weather = fetchLiveWeather(resolved.latitude, resolved.longitude)
            if (!weather.isNullOrBlank()) {
                sb.append("- Live Weather: $weather\n")
            } else {
                sb.append("- Live Weather: Clear / Weather service temporarily unreachable\n")
            }
        } else {
            sb.append("- Location: Location service not ready\n")
            sb.append("- Live Weather: Weather data pending location fix\n")
        }

        sb.toString().trim()
    }

    /**
     * Resolves location reliably through a 3-tier fallback strategy:
     * 1. Cached location (<10 mins)
     * 2. Hardware GPS / Network provider getLastKnownLocation or active one-shot listener
     * 3. Fast IP Geolocation fallback (works indoors and when GPS cache is empty)
     */
    suspend fun resolveBestLocation(context: Context): ResolvedLocation? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = lastResolvedLocation
        if (cached != null && (now - cached.timestamp) < LOCATION_CACHE_DURATION_MS) {
            return@withContext cached
        }

        // Tier 1: Hardware Last Known Location
        val lastKnown = getBestLastKnownLocation(context)
        if (lastKnown != null) {
            val locationName = reverseGeocodeLocation(context, lastKnown.latitude, lastKnown.longitude)
            val result = ResolvedLocation(
                latitude = lastKnown.latitude,
                longitude = lastKnown.longitude,
                locationName = locationName,
                source = "GPS / Network Sensor"
            )
            lastResolvedLocation = result
            return@withContext result
        }

        // Tier 2: Quick single-shot active GPS/Network request with 2.5s timeout
        if (hasLocationPermission(context)) {
            val freshLocation = withTimeoutOrNull(2500L) {
                requestSingleFreshLocation(context)
            }
            if (freshLocation != null) {
                val locationName = reverseGeocodeLocation(context, freshLocation.latitude, freshLocation.longitude)
                val result = ResolvedLocation(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    locationName = locationName,
                    source = "Live GPS Sensor"
                )
                lastResolvedLocation = result
                return@withContext result
            }
        }

        // Tier 3: Fast IP-Based Geolocation Fallback (free, keyless, works indoors)
        val ipLocation = withTimeoutOrNull(3000L) {
            fetchIpGeolocation()
        }
        if (ipLocation != null) {
            lastResolvedLocation = ipLocation
            return@withContext ipLocation
        }

        // If previously had any cached location, return it rather than failing
        lastResolvedLocation
    }

    private suspend fun requestSingleFreshLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (e: Exception) {
                        // Ignored
                    }
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val provider = when {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> LocationManager.PASSIVE_PROVIDER
            }

            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())

            cont.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        } catch (e: SecurityException) {
            cont.resume(null)
        } catch (e: Exception) {
            cont.resume(null)
        }
    }

    private fun fetchIpGeolocation(): ResolvedLocation? {
        val endpoints = listOf(
            "https://ipapi.co/json/",
            "http://ip-api.com/json/?fields=status,country,regionName,city,lat,lon",
            "https://freeipapi.com/api/json"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "MiraiAI-Android/1.0")

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)

                    var lat = json.optDouble("lat", Double.NaN)
                    if (lat.isNaN()) lat = json.optDouble("latitude", Double.NaN)

                    var lon = json.optDouble("lon", Double.NaN)
                    if (lon.isNaN()) lon = json.optDouble("longitude", Double.NaN)

                    val city = json.optString("city", "").ifBlank { json.optString("cityName", "") }
                    val region = json.optString("region", "").ifBlank { json.optString("regionName", "") }
                    val country = json.optString("country", "").ifBlank { json.optString("country_name", json.optString("countryName", "")) }

                    if (!lat.isNaN() && !lon.isNaN()) {
                        val parts = listOfNotNull(city, region, country).filter { it.isNotBlank() }
                        val locName = if (parts.isNotEmpty()) parts.joinToString(", ") else "Coordinates ($lat, $lon)"
                        return ResolvedLocation(
                            latitude = lat,
                            longitude = lon,
                            locationName = locName,
                            source = "Network Geolocation"
                        )
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint
            }
        }
        return null
    }

    private fun getBatteryInfo(context: Context): String {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, intentFilter)
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

                val percentage = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val chargingType = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
                    else -> if (isCharging) "Charging" else "Discharging"
                }

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isPowerSave = powerManager?.isPowerSaveMode == true
                val powerSaveStr = if (isPowerSave) ", Power Save Mode: ON" else ""

                if (percentage >= 0) {
                    "$percentage% (${if (isCharging) "Charging via $chargingType" else "Not Charging"}$powerSaveStr)"
                } else {
                    "Status: ${if (isCharging) "Charging" else "Normal"}$powerSaveStr"
                }
            } else {
                "Standard Battery"
            }
        } catch (e: Exception) {
            "Battery status unavailable"
        }
    }

    private data class PublicIpCache(
        val timestamp: Long,
        val ip: String,
        val isp: String?,
        val location: String?
    )

    private var publicIpCache: PublicIpCache? = null
    private const val PUBLIC_IP_CACHE_DURATION_MS = 10 * 60 * 1000L // 10 Minutes

    private suspend fun fetchPublicIpInfo(): PublicIpCache? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = publicIpCache
        if (cached != null && (now - cached.timestamp) < PUBLIC_IP_CACHE_DURATION_MS) {
            return@withContext cached
        }

        val endpoints = listOf(
            "https://api.ipify.org?format=json",
            "http://ip-api.com/json/?fields=query,status,country,regionName,city,isp,org",
            "https://ipapi.co/json/"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "MiraiAI-Android/1.0")

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val ip = json.optString("ip").ifBlank { json.optString("query") }
                    if (ip.isNotBlank()) {
                        val isp = json.optString("isp").ifBlank { json.optString("org") }.ifBlank { null }
                        val city = json.optString("city").ifBlank { null }
                        val region = json.optString("regionName").ifBlank { json.optString("region") }.ifBlank { null }
                        val country = json.optString("country").ifBlank { json.optString("country_name") }.ifBlank { null }
                        val locParts = listOfNotNull(city, region, country).filter { it.isNotBlank() }
                        val locStr = if (locParts.isNotEmpty()) locParts.joinToString(", ") else null

                        val result = PublicIpCache(now, ip, isp, locStr)
                        publicIpCache = result
                        return@withContext result
                    }
                }
            } catch (e: Exception) {
                // Fallback to next endpoint
            }
        }
        null
    }

    private fun getLocalNetworkInfo(context: Context): Triple<String?, String?, Pair<String?, List<String>>> {
        var localIpv4: String? = null
        var interfaceName: String? = null
        var gateway: String? = null
        val dnsList = mutableListOf<String>()

        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            if (activeNetwork != null) {
                val linkProps = connectivityManager.getLinkProperties(activeNetwork)
                interfaceName = linkProps?.interfaceName
                dnsList.addAll(linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList())
                gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress

                localIpv4 = linkProps?.linkAddresses?.map { it.address }
                    ?.filterIsInstance<Inet4Address>()
                    ?.firstOrNull { !it.isLoopbackAddress }
                    ?.hostAddress
            }

            if (localIpv4 == null) {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            localIpv4 = addr.hostAddress
                            if (interfaceName == null) interfaceName = iface.name
                            break
                        }
                    }
                    if (localIpv4 != null) break
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return Triple(localIpv4, interfaceName, Pair(gateway, dnsList))
    }

    @Suppress("DEPRECATION")
    private fun getWifiDetails(context: Context): Pair<String, List<String>> {
        var connectedSummary = "Not connected"
        val savedSsids = mutableListOf<String>()

        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val activeNetwork = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (isWifi && wifiManager != null) {
                val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    (caps?.transportInfo as? WifiInfo) ?: wifiManager.connectionInfo
                } else {
                    wifiManager.connectionInfo
                }

                var ssid = wifiInfo?.ssid?.removeSurrounding("\"")
                if (ssid == null || ssid == "<unknown ssid>" || ssid == "0x") {
                    val connInfo = wifiManager.connectionInfo
                    val altSsid = connInfo?.ssid?.removeSurrounding("\"")
                    if (!altSsid.isNullOrBlank() && altSsid != "<unknown ssid>" && altSsid != "0x") {
                        ssid = altSsid
                    }
                }

                val rssi = wifiInfo?.rssi ?: -127
                val linkSpeed = wifiInfo?.linkSpeed ?: -1
                val freq = wifiInfo?.frequency ?: 0
                val bandStr = when {
                    freq in 2400..2500 -> "2.4 GHz"
                    freq in 4900..5900 -> "5 GHz"
                    freq in 5925..7125 -> "6 GHz (Wi-Fi 6E/7)"
                    freq > 0 -> "$freq MHz"
                    else -> null
                }

                val detailsList = mutableListOf<String>()
                if (linkSpeed > 0) detailsList.add("Speed: $linkSpeed Mbps")
                if (rssi > -120) detailsList.add("Signal: $rssi dBm")
                if (bandStr != null) detailsList.add("Band: $bandStr")

                val metaSuffix = if (detailsList.isNotEmpty()) " (${detailsList.joinToString(", ")})" else ""

                connectedSummary = if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>" && ssid != "0x") {
                    "SSID: \"$ssid\"$metaSuffix"
                } else {
                    "Connected (SSID hidden by Android OS - requires Location toggle & permission)$metaSuffix"
                }
            }

            try {
                @Suppress("DEPRECATION")
                val configured = wifiManager?.configuredNetworks
                if (!configured.isNullOrEmpty()) {
                    for (config in configured) {
                        val name = config.SSID?.removeSurrounding("\"")
                        if (!name.isNullOrBlank() && name != "<unknown ssid>" && name != "0x") {
                            if (!savedSsids.contains(name)) {
                                savedSsids.add(name)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignored
            }
        } catch (e: Exception) {
            // Ignored
        }

        return Pair(connectedSummary, savedSsids)
    }

    private fun getCellularDetails(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return "Telephony service unavailable"

            val carrierName = telephonyManager.networkOperatorName.ifBlank {
                telephonyManager.simOperatorName.ifBlank { "Unknown Carrier" }
            }

            val countryIso = telephonyManager.networkCountryIso.uppercase().ifBlank {
                telephonyManager.simCountryIso.uppercase().ifBlank { "Unknown" }
            }

            val simStateStr = when (telephonyManager.simState) {
                TelephonyManager.SIM_STATE_READY -> "Ready"
                TelephonyManager.SIM_STATE_ABSENT -> "No SIM"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
                else -> "Active"
            }

            val isRoaming = telephonyManager.isNetworkRoaming
            val roamingStr = if (isRoaming) "Roaming: Yes" else "Roaming: No"

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
            val isCellularActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            val typeStr = if (isCellularActive) {
                val netType = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        telephonyManager.dataNetworkType
                    } else {
                        @Suppress("DEPRECATION")
                        telephonyManager.networkType
                    }
                } catch (e: SecurityException) {
                    TelephonyManager.NETWORK_TYPE_UNKNOWN
                }

                when (netType) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G HSPA/UMTS"
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G EDGE/GPRS"
                    else -> "Mobile Data Active"
                }
            } else {
                "Standby / Mobile Data Inactive"
            }

            "Carrier: $carrierName, Network: $typeStr, SIM: $simStateStr ($countryIso), $roamingStr"
        } catch (e: Exception) {
            "Cellular info unavailable"
        }
    }

    suspend fun getDetailedNetworkSummary(context: Context): String = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val connectionType = when {
            caps == null -> "Disconnected / Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Active"
            else -> "Connected"
        }

        val (localIp, iface, gatewayDns) = getLocalNetworkInfo(context)
        val (gateway, dnsList) = gatewayDns
        val publicIpInfo = fetchPublicIpInfo()
        val (wifiConnected, savedSsids) = getWifiDetails(context)
        val cellularInfo = getCellularDetails(context)

        val sb = StringBuilder()
        sb.append("Connection Type: $connectionType\n")

        if (localIp != null) {
            val ifaceStr = if (iface != null) " (Interface: $iface)" else ""
            sb.append("Local IP: $localIp$ifaceStr\n")
        }
        if (gateway != null) {
            sb.append("Gateway / Router: $gateway\n")
        }
        if (dnsList.isNotEmpty()) {
            sb.append("DNS Servers: ${dnsList.joinToString(", ")}\n")
        }

        if (publicIpInfo != null) {
            val ispStr = if (publicIpInfo.isp != null) " (ISP: ${publicIpInfo.isp})" else ""
            val locStr = if (publicIpInfo.location != null) " [Location: ${publicIpInfo.location}]" else ""
            sb.append("Public IP: ${publicIpInfo.ip}$ispStr$locStr\n")
        } else {
            sb.append("Public IP: Query unavailable / Offline\n")
        }

        sb.append("Wi-Fi Connection: $wifiConnected\n")
        if (savedSsids.isNotEmpty()) {
            sb.append("Saved Wi-Fi Networks: ${savedSsids.joinToString(", ")}\n")
        } else {
            sb.append("Saved Wi-Fi Networks: Restricted by Android OS (available on Android 9 or system apps)\n")
        }

        sb.append("Cellular Network: $cellularInfo")

        sb.toString().trim()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    private fun getBestLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                try {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || (l.time > bestLocation.time && l.accuracy <= bestLocation.accuracy)) {
                        bestLocation = l
                    }
                } catch (e: SecurityException) {
                    // Ignored
                }
            }
            bestLocation
        } catch (e: Exception) {
            null
        }
    }

    private fun reverseGeocodeLocation(context: Context, lat: Double, lon: Double): String {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.subLocality
                    val state = addr.adminArea
                    val country = addr.countryName
                    val parts = listOfNotNull(city, state, country).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        return parts.joinToString(", ")
                    }
                }
            }
            "Coordinates (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)})"
        } catch (e: Exception) {
            "Coordinates (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)})"
        }
    }

    /**
     * Fetches real-time weather from Open-Meteo free API (no key required).
     */
    private fun fetchLiveWeather(lat: Double, lon: Double): String? {
        val cacheKey = "${String.format(Locale.US, "%.2f", lat)}_${String.format(Locale.US, "%.2f", lon)}"
        val cached = weatherCacheMap[cacheKey]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.timestamp) < WEATHER_CACHE_DURATION_MS) {
            return cached.weatherSummary
        }

        return try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "MiraiAI-Android/1.0")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                val current = rootJson.optJSONObject("current")
                if (current != null) {
                    val temp = current.optDouble("temperature_2m", Double.NaN)
                    val apparentTemp = current.optDouble("apparent_temperature", Double.NaN)
                    val humidity = current.optInt("relative_humidity_2m", -1)
                    val windSpeed = current.optDouble("wind_speed_10m", Double.NaN)
                    val weatherCode = current.optInt("weather_code", 0)

                    val weatherDesc = mapWeatherCodeToDescription(weatherCode)
                    val tempStr = if (!temp.isNaN()) "${String.format(Locale.US, "%.1f", temp)}°C" else ""
                    val feelsLikeStr = if (!apparentTemp.isNaN()) " (Feels like ${String.format(Locale.US, "%.1f", apparentTemp)}°C)" else ""
                    val humidityStr = if (humidity >= 0) ", Humidity: $humidity%" else ""
                    val windStr = if (!windSpeed.isNaN()) ", Wind: ${String.format(Locale.US, "%.1f", windSpeed)} km/h" else ""

                    val summary = "$weatherDesc $tempStr$feelsLikeStr$humidityStr$windStr".trim()
                    weatherCacheMap[cacheKey] = WeatherCache(now, summary)
                    summary
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun mapWeatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky ☀️"
            1 -> "Mainly Clear 🌤️"
            2 -> "Partly Cloudy ⛅"
            3 -> "Overcast ☁️"
            45, 48 -> "Foggy 🌫️"
            51, 53, 55 -> "Drizzle 🌦️"
            56, 57 -> "Freezing Drizzle 🌧️"
            61, 63, 65 -> "Rain 🌧️"
            66, 67 -> "Freezing Rain 🌨️"
            71, 73, 75 -> "Snow Fall ❄️"
            77 -> "Snow Grains ❄️"
            80, 81, 82 -> "Rain Showers 🌦️"
            85, 86 -> "Snow Showers 🌨️"
            95 -> "Thunderstorm ⛈️"
            96, 99 -> "Thunderstorm with Hail ⛈️"
            else -> "Clear / Fair 🌤️"
        }
    }
}
