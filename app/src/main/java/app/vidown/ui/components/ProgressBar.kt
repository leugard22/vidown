package app.vidown.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vidown.data.model.DownloadStatus

@Composable
fun ProgressBar(
    status: DownloadStatus,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val isIndeterminate = status == DownloadStatus.QUEUED
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        animationSpec = spring(stiffness = 200f),
        label = "DownloadProgress"
    )

    val barColor = when (status) {
        DownloadStatus.MERGING -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.DONE -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when (status) {
        DownloadStatus.QUEUED -> "Queued..."
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.MERGING -> "Muxing streams"
        DownloadStatus.DONE -> "Complete"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

    Column(modifier = modifier) {
        if (isIndeterminate) {
            LinearProgressIndicator(
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        } else {
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
            if (!isIndeterminate && status == DownloadStatus.DOWNLOADING) {
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
