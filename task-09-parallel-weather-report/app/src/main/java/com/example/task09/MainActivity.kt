package com.example.task09

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager

private const val WEATHER_WORK_NAME = "parallel_weather_report"

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            MaterialTheme {
                WeatherScreen(WorkManager.getInstance(this))
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun WeatherScreen(workManager: WorkManager) {
    val infos by workManager.getWorkInfosForUniqueWorkLiveData(WEATHER_WORK_NAME).observeAsState(emptyList())
    val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
    val finished = infos.lastOrNull { it.state.isFinished }

    val status = running?.progress?.getString("status")
        ?: finished?.outputData?.getString("status")
        ?: "Idle"
    val done = running?.progress?.getInt("done", 0) ?: 0
    val total = running?.progress?.getInt("total", 4) ?: 4

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Parallel weather report", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = {
                val request = OneTimeWorkRequestBuilder<WeatherReportWorker>().build()
                workManager.enqueueUniqueWork(WEATHER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
            }) {
                Text("Start weather report")
            }

            Text("Status: $status")
            Text("Progress: $done / $total")

            when (finished?.state) {
                WorkInfo.State.SUCCEEDED -> {
                    Text("Average temp: ${finished.outputData.getDouble("avg", 0.0)}°C")
                    Text("Report: ${finished.outputData.getString("report")}")
                }
                WorkInfo.State.FAILED -> Text("Failed: ${finished.outputData.getString("error") ?: "unknown"}")
                else -> Unit
            }
        }
    }
}
