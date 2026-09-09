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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
                ?: "Weather & Air Quality service currently updating (Location: ${resolved.locationName})"
            "Location: ${resolved.locationName} (Lat: ${String.format(Locale.US, "%.4f", resolved.latitude)}, Lon: ${String.format(Locale.US, "%.4f", resolved.longitude)}, via ${resolved.source})\n$weather"
        } else {
            "Location: Location currently unavailable. Weather & Air Quality: Unable to determine without location."
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

        // 5. GPS / Network / IP Location & Live Weather & Air Quality (IKU)
        val resolved = resolveBestLocation(context)
        if (resolved != null) {
            sb.append("- Location: ${resolved.locationName} (Lat: ${String.format(Locale.US, "%.4f", resolved.latitude)}, Lon: ${String.format(Locale.US, "%.4f", resolved.longitude)}, source: ${resolved.source})\n")
            val weather = fetchLiveWeather(resolved.latitude, resolved.longitude)
            if (!weather.isNullOrBlank()) {
                sb.append("- Live Weather & Air Quality (IKU):\n")
                weather.lines().forEach { line ->
                    sb.append("  * $line\n")
                }
            } else {
                sb.append("- Live Weather & Air Quality (IKU): Clear / Weather service temporarily unreachable\n")
            }
        } else {
            sb.append("- Location: Location service not ready\n")
            sb.append("- Live Weather & Air Quality (IKU): Weather data pending location fix\n")
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
     * Fetches comprehensive real-time weather and air quality (IKU) data from Open-Meteo free APIs.
     */
    private suspend fun fetchLiveWeather(lat: Double, lon: Double): String? = coroutineScope {
        val cacheKey = "${String.format(Locale.US, "%.2f", lat)}_${String.format(Locale.US, "%.2f", lon)}"
        val cached = weatherCacheMap[cacheKey]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.timestamp) < WEATHER_CACHE_DURATION_MS) {
            return@coroutineScope cached.weatherSummary
        }

        val weatherDeferred = async { fetchWeatherForecast(lat, lon) }
        val aqiDeferred = async { fetchAirQuality(lat, lon) }

        val weatherData = weatherDeferred.await()
        val aqiData = aqiDeferred.await()

        if (weatherData == null && aqiData == null) {
            return@coroutineScope null
        }

        val summary = buildString {
            if (weatherData != null) {
                append(weatherData)
            }
            if (aqiData != null) {
                if (isNotEmpty()) append("\n")
                append(aqiData)
            }
        }.trim()

        if (summary.isNotBlank()) {
            weatherCacheMap[cacheKey] = WeatherCache(now, summary)
            summary
        } else {
            null
        }
    }

    private fun fetchWeatherForecast(lat: Double, lon: Double): String? {
        return try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max&timezone=auto"
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
                val daily = rootJson.optJSONObject("daily")

                if (current != null) {
                    val temp = current.optDouble("temperature_2m", Double.NaN)
                    val apparentTemp = current.optDouble("apparent_temperature", Double.NaN)
                    val humidity = current.optInt("relative_humidity_2m", -1)
                    val isDay = current.optInt("is_day", -1)
                    val weatherCode = current.optInt("weather_code", 0)
                    val cloudCover = current.optInt("cloud_cover", -1)
                    val precipitation = current.optDouble("precipitation", Double.NaN)
                    val rain = current.optDouble("rain", Double.NaN)
                    val showers = current.optDouble("showers", Double.NaN)
                    val snowfall = current.optDouble("snowfall", Double.NaN)
                    val pressure = current.optDouble("pressure_msl", Double.NaN)
                    val surfacePressure = current.optDouble("surface_pressure", Double.NaN)
                    val windSpeed = current.optDouble("wind_speed_10m", Double.NaN)
                    val windDir = current.optDouble("wind_direction_10m", Double.NaN)
                    val windGusts = current.optDouble("wind_gusts_10m", Double.NaN)

                    val weatherDesc = mapWeatherCodeToDescription(weatherCode)
                    val dayNightStr = when (isDay) {
                        1 -> "Daytime ☀️"
                        0 -> "Nighttime 🌙"
                        else -> ""
                    }

                    var tempMinStr = ""
                    var tempMaxStr = ""
                    var sunriseStr = ""
                    var sunsetStr = ""
                    var rainProbStr = ""

                    if (daily != null) {
                        val maxTemps = daily.optJSONArray("temperature_2m_max")
                        val minTemps = daily.optJSONArray("temperature_2m_min")
                        val sunrises = daily.optJSONArray("sunrise")
                        val sunsets = daily.optJSONArray("sunset")
                        val rainProbs = daily.optJSONArray("precipitation_probability_max")

                        if (minTemps != null && minTemps.length() > 0) {
                            val minT = minTemps.optDouble(0, Double.NaN)
                            if (!minT.isNaN()) tempMinStr = "${String.format(Locale.US, "%.1f", minT)}°C"
                        }
                        if (maxTemps != null && maxTemps.length() > 0) {
                            val maxT = maxTemps.optDouble(0, Double.NaN)
                            if (!maxT.isNaN()) tempMaxStr = "${String.format(Locale.US, "%.1f", maxT)}°C"
                        }
                        if (sunrises != null && sunrises.length() > 0) {
                            val rawSunrise = sunrises.optString(0, "")
                            sunriseStr = rawSunrise.substringAfter("T", rawSunrise)
                        }
                        if (sunsets != null && sunsets.length() > 0) {
                            val rawSunset = sunsets.optString(0, "")
                            sunsetStr = rawSunset.substringAfter("T", rawSunset)
                        }
                        if (rainProbs != null && rainProbs.length() > 0) {
                            val prob = rainProbs.optInt(0, -1)
                            if (prob >= 0) rainProbStr = "$prob%"
                        }
                    }

                    val sb = StringBuilder()
                    val conditionSuffix = if (dayNightStr.isNotBlank()) " ($dayNightStr)" else ""
                    sb.append("Weather Condition: $weatherDesc$conditionSuffix\n")

                    val tempStr = if (!temp.isNaN()) "${String.format(Locale.US, "%.1f", temp)}°C" else ""
                    val feelsLikeStr = if (!apparentTemp.isNaN()) " (Feels like: ${String.format(Locale.US, "%.1f", apparentTemp)}°C)" else ""
                    val rangeStr = if (tempMinStr.isNotBlank() && tempMaxStr.isNotBlank()) " | Today's Min: $tempMinStr, Max: $tempMaxStr" else ""
                    sb.append("Temperature: $tempStr$feelsLikeStr$rangeStr\n")

                    val humidityStr = if (humidity >= 0) "Humidity: $humidity%" else ""
                    val cloudStr = if (cloudCover >= 0) "Cloud Cover: $cloudCover%" else ""
                    val rainProbPart = if (rainProbStr.isNotBlank()) "Precipitation Probability: $rainProbStr" else ""
                    val precipVal = if (!precipitation.isNaN()) precipitation else if (!rain.isNaN()) rain else Double.NaN
                    val precipPart = if (!precipVal.isNaN()) "Precipitation: ${String.format(Locale.US, "%.1f", precipVal)} mm" else ""
                    val atmosList = listOf(humidityStr, cloudStr, rainProbPart, precipPart).filter { it.isNotBlank() }
                    if (atmosList.isNotEmpty()) {
                        sb.append(atmosList.joinToString(" | ")).append("\n")
                    }

                    if (!showers.isNaN() && showers > 0.0) {
                        sb.append("Rain Showers: ${String.format(Locale.US, "%.1f", showers)} mm\n")
                    }
                    if (!snowfall.isNaN() && snowfall > 0.0) {
                        sb.append("Snowfall: ${String.format(Locale.US, "%.1f", snowfall)} cm\n")
                    }

                    if (!windSpeed.isNaN()) {
                        val dirCard = if (!windDir.isNaN()) "${mapDegreesToCardinal(windDir)} (${String.format(Locale.US, "%.0f", windDir)}°)" else ""
                        val gustsPart = if (!windGusts.isNaN()) ", Gusts: ${String.format(Locale.US, "%.1f", windGusts)} km/h" else ""
                        sb.append("Wind: ${String.format(Locale.US, "%.1f", windSpeed)} km/h $dirCard$gustsPart\n")
                    }

                    if (!pressure.isNaN()) {
                        val surfPart = if (!surfacePressure.isNaN()) " (Surface: ${String.format(Locale.US, "%.1f", surfacePressure)} hPa)" else ""
                        sb.append("Atmospheric Pressure: ${String.format(Locale.US, "%.1f", pressure)} hPa$surfPart\n")
                    }

                    if (sunriseStr.isNotBlank() || sunsetStr.isNotBlank()) {
                        sb.append("Sun Schedule: Sunrise at $sunriseStr, Sunset at $sunsetStr\n")
                    }

                    sb.toString().trim()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchAirQuality(lat: Double, lon: Double): String? {
        return try {
            val urlString = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${lat}&longitude=${lon}&current=us_aqi,european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,uv_index,dust"
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
                    val usAqi = current.optInt("us_aqi", -1)
                    val euAqi = current.optInt("european_aqi", -1)
                    val pm25 = current.optDouble("pm2_5", Double.NaN)
                    val pm10 = current.optDouble("pm10", Double.NaN)
                    val co = current.optDouble("carbon_monoxide", Double.NaN)
                    val no2 = current.optDouble("nitrogen_dioxide", Double.NaN)
                    val so2 = current.optDouble("sulphur_dioxide", Double.NaN)
                    val o3 = current.optDouble("ozone", Double.NaN)
                    val uv = current.optDouble("uv_index", Double.NaN)
                    val dust = current.optDouble("dust", Double.NaN)

                    val sb = StringBuilder()
                    sb.append("Air Quality / IKU (Indeks Kualitas Udara):\n")

                    if (usAqi >= 0) {
                        val status = mapAqiToCategory(usAqi)
                        sb.append("  - US AQI (IKU Utama): $usAqi ($status)\n")
                    }
                    if (euAqi >= 0) {
                        sb.append("  - European AQI: $euAqi\n")
                    }
                    if (!pm25.isNaN()) {
                        sb.append("  - PM2.5 (Partikel Halus): ${String.format(Locale.US, "%.1f", pm25)} µg/m³\n")
                    }
                    if (!pm10.isNaN()) {
                        sb.append("  - PM10 (Partikel Kasar): ${String.format(Locale.US, "%.1f", pm10)} µg/m³\n")
                    }
                    if (!uv.isNaN()) {
                        val uvCategory = mapUvIndexToCategory(uv)
                        sb.append("  - UV Index: ${String.format(Locale.US, "%.1f", uv)} ($uvCategory)\n")
                    }
                    if (!o3.isNaN()) {
                        sb.append("  - Ozon (O3): ${String.format(Locale.US, "%.1f", o3)} µg/m³\n")
                    }
                    if (!no2.isNaN()) {
                        sb.append("  - Nitrogen Dioksida (NO2): ${String.format(Locale.US, "%.1f", no2)} µg/m³\n")
                    }
                    if (!so2.isNaN()) {
                        sb.append("  - Sulfur Dioksida (SO2): ${String.format(Locale.US, "%.1f", so2)} µg/m³\n")
                    }
                    if (!co.isNaN()) {
                        sb.append("  - Karbon Monoksida (CO): ${String.format(Locale.US, "%.1f", co)} µg/m³\n")
                    }
                    if (!dust.isNaN()) {
                        sb.append("  - Partikel Debu (Dust): ${String.format(Locale.US, "%.1f", dust)} µg/m³\n")
                    }

                    sb.toString().trimEnd()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun mapAqiToCategory(usAqi: Int): String {
        return when {
            usAqi <= 50 -> "Baik / Good 🟢"
            usAqi <= 100 -> "Sedang / Moderate 🟡"
            usAqi <= 150 -> "Tidak Sehat untuk Kelompok Sensitif / Sensitive Groups 🟠"
            usAqi <= 200 -> "Tidak Sehat / Unhealthy 🔴"
            usAqi <= 300 -> "Sangat Tidak Sehat / Very Unhealthy 🟣"
            else -> "Berbahaya / Hazardous 🟤"
        }
    }

    private fun mapUvIndexToCategory(uv: Double): String {
        return when {
            uv < 3.0 -> "Rendah / Low 🟢"
            uv < 6.0 -> "Sedang / Moderate 🟡"
            uv < 8.0 -> "Tinggi / High 🟠"
            uv < 11.0 -> "Sangat Tinggi / Very High 🔴"
            else -> "Ekstrem / Extreme 🟣"
        }
    }

    private fun mapDegreesToCardinal(degrees: Double): String {
        val normalized = (degrees % 360 + 360) % 360
        return when {
            normalized >= 337.5 || normalized < 22.5 -> "Utara (N)"
            normalized < 67.5 -> "Timur Laut (NE)"
            normalized < 112.5 -> "Timur (E)"
            normalized < 157.5 -> "Tenggara (SE)"
            normalized < 202.5 -> "Selatan (S)"
            normalized < 247.5 -> "Barat Daya (SW)"
            normalized < 292.5 -> "Barat (W)"
            else -> "Barat Laut (NW)"
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
