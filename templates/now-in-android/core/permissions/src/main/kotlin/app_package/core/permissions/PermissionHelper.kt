package ${configs.packageName}.core.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

interface PermissionHandler {
    fun launch(permission: String)
}

@Composable
fun rememberPermissionLauncher(
    onResult: (permission: String, isGranted: Boolean) -> Unit
): PermissionHandler {
    var requestedPermission = remember { "" }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(requestedPermission, isGranted)
    }

    return remember {
        object : PermissionHandler {
            override fun launch(permission: String) {
                requestedPermission = permission
                launcher.launch(permission)
            }
        }
    }
}
