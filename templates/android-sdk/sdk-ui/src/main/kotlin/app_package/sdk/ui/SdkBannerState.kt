package ${configs.packageName}.sdk.ui

import androidx.compose.runtime.Immutable
import ${configs.packageName}.sdk.core.SdkStatus

@Immutable
public data class SdkBannerState(
    public val status: SdkStatus,
)
