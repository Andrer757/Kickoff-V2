package ${configs.packageName}.sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ${configs.packageName}.sdk.core.SdkStatus

@Immutable
public data class SdkBannerState(
    public val status: SdkStatus
)

@Composable
public fun SdkStatusBanner(
    state: SdkBannerState,
    modifier: Modifier = Modifier
): Unit {
    val backgroundColor = when (state.status) {
        SdkStatus.READY -> Color(0xFF4CAF50)
        SdkStatus.INITIALIZING -> Color(0xFFFF9800)
        SdkStatus.ERROR -> Color(0xFFE91E63)
        SdkStatus.UNINITIALIZED -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "SDK Status: ${state.status.name}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
