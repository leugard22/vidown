package app.vidown.ui.downloads

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.CircleShape
import app.vidown.ui.util.formatDuration
import app.vidown.ui.util.formatFileSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vidown.data.model.DownloadItem
import app.vidown.data.model.DownloadStatus
import app.vidown.ui.components.ProgressBar
import app.vidown.ui.components.StatusBadge
import app.vidown.ui.components.VideoThumbnail

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
    onNavigateHome: (() -> Unit)? = null
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val tabTitles = listOf("Active", "Completed", "Failed")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    val activeCount = downloads.count { it.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.MERGING) }
    val completedCount = downloads.count { it.status == DownloadStatus.DONE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${downloads.size} total · ${downloads.count { it.status == DownloadStatus.DONE }} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (pagerState.currentPage == 1 && completedCount > 0) {
                IconButton(onClick = { viewModel.clearCompleted() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Completed",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (pagerState.currentPage == 0 && activeCount > 0) {
                IconButton(onClick = { viewModel.cancelAllActive() }) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancel All",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                androidx.compose.material3.TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage, matchContentSize = true),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageDownloads = when (page) {
                0 -> downloads.filter { it.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.MERGING) }
                1 -> downloads.filter { it.status == DownloadStatus.DONE }
                else -> downloads.filter { it.status in listOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED) }
            }

            if (pageDownloads.isEmpty()) {
                EmptyPlaceholder(tabIndex = page, onNavigateHome = onNavigateHome)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pageDownloads, key = { it.id }) { item ->
                        if (page > 0) {
                            @Suppress("DEPRECATION")
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.delete(item.id)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                content = {
                                    DownloadCard(
                                        item = item,
                                        onCancel = { viewModel.cancel(item.id) },
                                        onDelete = { viewModel.delete(item.id) },
                                        onRetry = { viewModel.retry(item.id) }
                                    )
                                }
                            )
                        } else {
                            DownloadCard(
                                item = item,
                                onCancel = { viewModel.cancel(item.id) },
                                onDelete = { viewModel.delete(item.id) },
                                onRetry = { viewModel.retry(item.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = item.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.MERGING)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .aspectRatio(16f / 9f)
                    ) {
                        VideoThumbnail(
                            url = item.thumbnailUrl,
                            contentDescription = item.title,
                            showPlayIcon = false,
                            modifier = Modifier.matchParentSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            StatusBadge(status = item.status)
                        }
                    }

                    Column(modifier = Modifier.weight(0.65f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val subText = when (item.status) {
                            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.MERGING -> item.selectedQuality
                            DownloadStatus.DONE -> {
                                val sizeText = if (item.fileSizeBytes > 0) " · ${item.fileSizeBytes.formatFileSize()}" else ""
                                val durationText = if (item.durationSeconds > 0) " · ${item.durationSeconds.formatDuration()}" else ""
                                "${item.selectedQuality}$sizeText$durationText"
                            }
                            DownloadStatus.FAILED -> item.errorMessage ?: "Failed to download"
                            DownloadStatus.CANCELLED -> "Cancelled"
                        }

                        val textColor = if (item.status == DownloadStatus.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        }

                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val showButtons = isActive || item.status in listOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)
                if (showButtons) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isActive) {
                            FilledTonalIconButton(
                                onClick = onCancel,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            FilledTonalIconButton(
                                onClick = onRetry,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isActive) {
                ProgressBar(
                    status = item.status,
                    progressPercent = item.progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(
    tabIndex: Int,
    onNavigateHome: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val title = when (tabIndex) {
        0 -> "Queue is Empty"
        1 -> "No Downloads Yet"
        else -> "All Clean"
    }

    val subtitle = when (tabIndex) {
        0 -> "Find your favorite videos and paste their links on the Home tab to start downloading."
        1 -> "Your downloaded videos and audios will appear here once they are complete."
        else -> "No failed or cancelled downloads recorded in your history."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SlowMotionVideo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (tabIndex < 2 && onNavigateHome != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateHome,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Search Videos", fontWeight = FontWeight.Bold)
            }
        }
    }
}
