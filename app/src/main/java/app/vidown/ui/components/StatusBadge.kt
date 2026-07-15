package app.vidown.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import app.vidown.data.model.DownloadStatus

@Composable
fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, label) = when (status) {
        DownloadStatus.QUEUED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline, "Queued")
        DownloadStatus.DOWNLOADING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "Downloading")
        DownloadStatus.MERGING -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary, "Merging")
        DownloadStatus.DONE -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "Done")
        DownloadStatus.FAILED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Failed")
        DownloadStatus.CANCELLED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Cancelled")
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
