package com.ryzumi.miraiai.ui.screen.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzumi.miraiai.R

data class OpenSourceComponent(
    val name: String,
    val developer: String,
    val version: String,
    val license: String,
    val description: String,
    val url: String,
    val licenseNotice: String = APACHE_2_0_NOTICE
)

const val APACHE_2_0_NOTICE = """Copyright Google LLC / Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

const val MIT_NOTICE = """Copyright (c) Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

const val OPEN_METEO_NOTICE = """Weather and Air Quality data provided by Open-Meteo.com under Non-Commercial Open Database License (ODbL) and Creative Commons Attribution 4.0 International (CC BY 4.0).
Free public meteorological forecasting without API keys.
Visit: https://open-meteo.com/"""

val APP_COMPONENTS = listOf(
    OpenSourceComponent(
        name = "Jetpack Compose & Material 3",
        developer = "Google LLC & Android Open Source Project",
        version = "BOM 2024.10.00",
        license = "Apache-2.0",
        description = "Modern declarative UI toolkit for Android, providing Material Design 3 tokens, dynamic Monet colors, and fluid micro-animations.",
        url = "https://developer.android.com/jetpack/compose",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "Kotlin Coroutines & Flow",
        developer = "JetBrains s.r.o.",
        version = "2.0.21",
        license = "Apache-2.0",
        description = "Structured concurrency library enabling asynchronous non-blocking operations, reactive StateFlow channels, and smooth stream management.",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "AndroidX Room Persistence",
        developer = "Google LLC",
        version = "2.8.4",
        license = "Apache-2.0",
        description = "Robust SQLite object mapping library providing local database persistence, migrations, and compile-time SQL verification.",
        url = "https://developer.android.com/training/data-storage/room",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "AndroidX DataStore Preferences",
        developer = "Google LLC",
        version = "1.1.1",
        license = "Apache-2.0",
        description = "Asynchronous key-value storage replacing SharedPreferences, backed by Kotlin coroutines and transactional guarantees.",
        url = "https://developer.android.com/topic/libraries/architecture/datastore",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "AndroidX Navigation Compose",
        developer = "Google LLC",
        version = "2.8.3",
        license = "Apache-2.0",
        description = "Type-safe navigation framework for Compose apps supporting deep linking, parameter passing, and animated screen transitions.",
        url = "https://developer.android.com/guide/navigation",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "AndroidX WorkManager",
        developer = "Google LLC",
        version = "2.9.1",
        license = "Apache-2.0",
        description = "Graceful background job scheduling architecture respecting Android battery optimization and network constraints.",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "Retrofit",
        developer = "Square, Inc.",
        version = "2.11.0",
        license = "Apache-2.0",
        description = "Type-safe HTTP client for Android and Kotlin facilitating structured REST communication with AI inference servers.",
        url = "https://github.com/square/retrofit",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "OkHttp & Server-Sent Events (SSE)",
        developer = "Square, Inc.",
        version = "4.12.0",
        license = "Apache-2.0",
        description = "High-performance HTTP/2 client powering live real-time token streaming and server-sent events for AI generation.",
        url = "https://github.com/square/okhttp",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "Gson",
        developer = "Google LLC",
        version = "2.10.1",
        license = "Apache-2.0",
        description = "JSON parser and serializer used for LLM tool calling schema definitions, app backup bundles, and model configs.",
        url = "https://github.com/google/gson",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "Coil (Coroutine Image Loader)",
        developer = "Coil Contributors",
        version = "2.7.0",
        license = "Apache-2.0",
        description = "Fast, lightweight image loading library for Android powered by Kotlin Coroutines, memory pooling, and disk caching.",
        url = "https://github.com/coil-kt/coil",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "AndroidX ExifInterface",
        developer = "Google LLC",
        version = "1.3.7",
        license = "Apache-2.0",
        description = "Metadata parser for extracting and normalizing image orientation before processing and encoding multimodal vision inputs.",
        url = "https://developer.android.com/jetpack/androidx/releases/exifinterface",
        licenseNotice = APACHE_2_0_NOTICE
    ),
    OpenSourceComponent(
        name = "Open-Meteo Weather & Air Quality API",
        developer = "Open-Meteo GmbH",
        version = "Free Public Endpoint",
        license = "CC BY 4.0 / ODbL",
        description = "Open-source meteorological and Air Quality (IKU/AQI, PM2.5, UV) provider powering AI environmental awareness without API keys.",
        url = "https://open-meteo.com/",
        licenseNotice = OPEN_METEO_NOTICE
    )
)

/**
 * Standalone About Screen with Scaffold & TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Mirai AI") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        AboutAppView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Reusable About App View (suitable as a Tab inside SettingsScreen or in standalone screens).
 */
@Composable
fun AboutAppView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedComponentForLicense by remember { mutableStateOf<OpenSourceComponent?>(null) }

    val appVersionInfo = remember(context) { getAppVersionInfo(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Header Card (Icon, Name, Version, Tagline)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Icon with elegant glow
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIconImage(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Mirai AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Version Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${appVersionInfo.versionName} (Build ${appVersionInfo.versionCode})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Intelligent Personalized AI Companion & Assistant",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Designed with rich persona interactions, multimodal vision support, real-time device telemetry & IKU weather awareness, and secure local backups.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 2. Open Source Section Header
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Source Components & Licenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "This application is built with the following open-source components and libraries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Components List
        items(APP_COMPONENTS, key = { it.name }) { comp ->
            ComponentLicenseCard(
                component = comp,
                onClick = { selectedComponentForLicense = comp },
                onOpenUrl = { openWebUrl(context, comp.url) }
            )
        }
    }

    // License Detail Dialog
    if (selectedComponentForLicense != null) {
        val comp = selectedComponentForLicense!!
        AlertDialog(
            onDismissRequest = { selectedComponentForLicense = null },
            title = {
                Column {
                    Text(
                        text = comp.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "License: ${comp.license} • ${comp.developer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = comp.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Notice & Terms:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = comp.licenseNotice,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedComponentForLicense = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        openWebUrl(context, comp.url)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Repository")
                }
            }
        )
    }
}

@Composable
private fun ComponentLicenseCard(
    component: OpenSourceComponent,
    onClick: () -> Unit,
    onOpenUrl: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = component.license,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${component.developer} • v${component.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = component.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View License", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                    onClick = onOpenUrl,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Link",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class AppVersionInfo(
    val versionName: String,
    val versionCode: String
)

private fun getAppVersionInfo(context: Context): AppVersionInfo {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val vName = packageInfo?.versionName ?: "1.0.5"
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "6"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "6"
        }
        AppVersionInfo(vName, vCode)
    } catch (e: Exception) {
        AppVersionInfo("1.0.5", "6")
    }
}

private fun openWebUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun AppIconImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appBitmap = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (appBitmap != null) {
        Image(
            bitmap = appBitmap,
            contentDescription = "Mirai AI App Icon",
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Mirai AI App Icon",
            modifier = modifier
        )
    }
}
