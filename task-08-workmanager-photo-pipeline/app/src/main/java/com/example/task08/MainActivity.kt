package com.example.task08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager

private const val PIPELINE_NAME = "photo_pipeline"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PipelineScreen(WorkManager.getInstance(this))
            }
        }
    }
}

@Composable
private fun PipelineScreen(workManager: WorkManager) {
    val workInfos by workManager.getWorkInfosForUniqueWorkLiveData(PIPELINE_NAME).observeAsState(emptyList())

    val runningInfo = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
    val finishedInfo = workInfos.lastOrNull { it.state.isFinished }

    val step = runningInfo?.progress?.getString("step") ?: finishedInfo?.outputData?.getString("step") ?: "idle"
    val progress = runningInfo?.progress?.getInt("progress", 0) ?: if (finishedInfo?.state == WorkInfo.State.SUCCEEDED) 100 else 0

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("WorkManager photo pipeline", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = {
                val compress = OneTimeWorkRequestBuilder<CompressWorker>().addTag("compress").build()
                val watermark = OneTimeWorkRequestBuilder<WatermarkWorker>().addTag("watermark").build()
                val upload = OneTimeWorkRequestBuilder<UploadWorker>().addTag("upload").build()

                workManager.beginUniqueWork(PIPELINE_NAME, ExistingWorkPolicy.REPLACE, compress)
                    .then(watermark)
                    .then(upload)
                    .enqueue()
            }) {
                Text("Start pipeline")
            }

            Text("Current step: $step")
            LinearProgressIndicator(progress = { progress / 100f })

            when (finishedInfo?.state) {
                WorkInfo.State.SUCCEEDED -> Text("Result: ${finishedInfo.outputData.getString("result")}")
                WorkInfo.State.FAILED -> Text("Error: ${finishedInfo.outputData.getString("error") ?: "Unknown"}")
                else -> Text("Waiting / Running")
            }
        }
    }
}
