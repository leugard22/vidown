package app.vidown.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultQuality by viewModel.defaultQuality.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsStateWithLifecycle()
    val customDownloadDir by viewModel.customDownloadDir.collectAsStateWithLifecycle()
    val pythonVersion by viewModel.pythonVersion.collectAsStateWithLifecycle()
    val ytdlVersion by viewModel.ytdlVersion.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()

    val youtubeLoggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle()
    val instagramLoggedIn by viewModel.instagramLoggedIn.collectAsStateWithLifecycle()
    val twitterLoggedIn by viewModel.twitterLoggedIn.collectAsStateWithLifecycle()
    val tiktokLoggedIn by viewModel.tiktokLoggedIn.collectAsStateWithLifecycle()

    var isQualityExpanded by remember { mutableStateOf(false) }
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isConcurrentExpanded by remember { mutableStateOf(false) }

    val qualityRotationAngle by animateFloatAsState(
        targetValue = if (isQualityExpanded) 180f else 0f,
        label = "QualityArrowRotation"
    )
    val themeRotationAngle by animateFloatAsState(
        targetValue = if (isThemeExpanded) 180f else 0f,
        label = "ThemeArrowRotation"
    )
    val concurrentRotationAngle by animateFloatAsState(
        targetValue = if (isConcurrentExpanded) 180f else 0f,
        label = "ConcurrentArrowRotation"
    )

    val qualityOptions = mapOf(
        "best" to "Best",
        "1080p" to "1080p Video",
        "720p" to "720p Video",
        "480p" to "480p Video",
        "audio" to "Audio"
    )

    val themeOptions = mapOf(
        "system" to "System Default",
        "light" to "Light",
        "dark" to "Dark"
    )

    val context = LocalContext.current
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.updateCustomDownloadDir(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkLoginStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("General Preferences")
        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Default Quality",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = qualityOptions[defaultQuality] ?: "Best",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(qualityRotationAngle)
                            .size(24.dp)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    trailingIconColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.clickable { isQualityExpanded = !isQualityExpanded }
            )

            if (isQualityExpanded) {
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                qualityOptions.forEach { (key, label) ->
                    val isSelected = key == defaultQuality
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateDefaultQuality(key)
                                    isQualityExpanded = false
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.clickable {
                            viewModel.updateDefaultQuality(key)
                            isQualityExpanded = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ListItem(
                headlineContent = {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = themeOptions[appTheme] ?: "System Default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(themeRotationAngle)
                            .size(24.dp)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    trailingIconColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.clickable { isThemeExpanded = !isThemeExpanded }
            )

            if (isThemeExpanded) {
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                themeOptions.forEach { (key, label) ->
                    val isSelected = key == appTheme
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateAppTheme(key)
                                    isThemeExpanded = false
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.clickable {
                            viewModel.updateAppTheme(key)
                            isThemeExpanded = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ListItem(
                headlineContent = {
                    Text(
                        text = "Max Concurrent Downloads",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = "$maxConcurrentDownloads active downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(concurrentRotationAngle)
                            .size(24.dp)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    trailingIconColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.clickable { isConcurrentExpanded = !isConcurrentExpanded }
            )

            if (isConcurrentExpanded) {
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                listOf(1, 2, 3, 4, 5).forEach { limit ->
                    val isSelected = limit == maxConcurrentDownloads
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "$limit Active Download${if (limit > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateMaxConcurrentDownloads(limit)
                                    isConcurrentExpanded = false
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.clickable {
                            viewModel.updateMaxConcurrentDownloads(limit)
                            isConcurrentExpanded = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            val dirName = if (!customDownloadDir.isNullOrEmpty()) {
                app.vidown.data.storage.CustomStorageHelper.getDirectoryName(context, customDownloadDir!!) ?: "Custom Folder"
            } else {
                "Default (Movies / Music)"
            }

            ListItem(
                headlineContent = {
                    Text(
                        text = "Download Location",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = dirName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    if (!customDownloadDir.isNullOrEmpty()) {
                        IconButton(onClick = { viewModel.updateCustomDownloadDir(null) }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Location",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.clickable { dirPickerLauncher.launch(null) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            val embedSubtitles by viewModel.embedSubtitles.collectAsStateWithLifecycle()
            ListItem(
                headlineContent = {
                    Text(
                        text = "Embed Subtitles",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = "Download and mux subtitles into video files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = embedSubtitles,
                        onCheckedChange = { viewModel.updateEmbedSubtitles(it) }
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Site Accounts")
        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val platforms = listOf(
                "YouTube" to youtubeLoggedIn,
                "Instagram" to instagramLoggedIn,
                "Twitter" to twitterLoggedIn,
                "TikTok" to tiktokLoggedIn
            )

            platforms.forEachIndexed { index, (name, isLoggedIn) ->
                ListItem(
                    headlineContent = {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text(
                            text = if (isLoggedIn) "Logged In" else "Logged Out",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoggedIn) {
                                TextButton(onClick = { viewModel.logoutSite(name) }) {
                                    Text("Logout", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            TextButton(onClick = {
                                val intent = Intent(context, LoginActivity::class.java).apply {
                                    putExtra("site_name", name)
                                }
                                loginLauncher.launch(intent)
                            }) {
                                Text(if (isLoggedIn) "Re-login" else "Login")
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (index < platforms.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("System")
        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Python Engine (Chaquopy)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = if (pythonVersion == "...") "Loading..." else "v$pythonVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ListItem(
                headlineContent = {
                    Text(
                        text = "yt-dlp Engine",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    val statusText = when (val status = updateStatus) {
                        UpdateStatus.Idle -> if (ytdlVersion == "...") "Loading..." else "v$ytdlVersion"
                        UpdateStatus.Checking -> "Checking for updates..."
                        is UpdateStatus.UpdateAvailable -> "Update available (v${status.latestVersion})"
                        UpdateStatus.Downloading -> "Downloading update..."
                        UpdateStatus.UpToDate -> "Engine is up to date (v$ytdlVersion)"
                        is UpdateStatus.Error -> "Update failed: ${status.message}"
                    }
                    val statusColor = when (updateStatus) {
                        is UpdateStatus.Error -> MaterialTheme.colorScheme.error
                        is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    when (updateStatus) {
                        UpdateStatus.Checking, UpdateStatus.Downloading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is UpdateStatus.UpdateAvailable -> {
                            TextButton(
                                onClick = { viewModel.downloadUpdate() }
                            ) {
                                Text("Update", fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            TextButton(
                                onClick = { viewModel.checkForUpdates() }
                            ) {
                                Text("Check")
                            }
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
