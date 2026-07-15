package app.vidown.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.vidown.data.model.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelector(
    formats: List<VideoFormat>,
    selectedFormat: VideoFormat?,
    onFormatSelected: (VideoFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        SingleChoiceSegmentedButtonRow {
            formats.forEachIndexed { index, format ->
                val isSelected = format.formatId == selectedFormat?.formatId
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, formats.size),
                    onClick = { onFormatSelected(format) },
                    selected = isSelected,
                    icon = {}
                ) {
                    Text(
                        text = format.note,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
