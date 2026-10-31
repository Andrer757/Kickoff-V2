package ${configs.packageName}.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ${configs.packageName}.sdk.ui.SdkBannerState
import ${configs.packageName}.sdk.ui.SdkStatusBanner
import kotlinx.coroutines.launch

class SampleMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SampleApplication
        val sdkClient = app.sdkClient

        setContent {
            MaterialTheme {
                val status by sdkClient.status.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${configs.projectName} SDK Sample",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SdkStatusBanner(state = SdkBannerState(status = status))

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = {
                            scope.launch {
                                sdkClient.initialize()
                            }
                        }) {
                            Text(text = "Re-Initialize SDK")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            sdkClient.shutdown()
                        }) {
                            Text(text = "Shutdown SDK")
                        }
                    }
                }
            }
        }
    }
}
