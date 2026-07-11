import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Safely resolve an Activity from the current Context (may be a ContextThemeWrapper in previews)
    val activity: Activity? = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }
    var showRationale by remember {
        mutableStateOf(
            false
        )
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
        } else {
            hasPermission = true
        }
    }

    if (hasPermission) {
        content() // Show your Camera View
    } else {
        if (showRationale) {
            // Show your custom UI/Dialog explaining why scanning is cool
            AlertDialog(
                onDismissRequest = { showRationale = false },
                title = { Text("Camera Access") },
                text = { Text("We need the camera to scan expiry dates so you don't have to type them manually!") },
                confirmButton = {
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Try Again")
                    }
                }
            )
        } else {
            // First time or permanently denied
            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
