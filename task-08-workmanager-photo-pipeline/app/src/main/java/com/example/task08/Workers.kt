package com.example.task08

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.io.File

class CompressWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        setProgress(Data.Builder().putString("step", "Compressing photo").putInt("progress", 20).build())
        delay(1200)
        val file = File(applicationContext.filesDir, "compressed_photo.txt")
        file.writeText("compressed")
        return Result.success(
            Data.Builder()
                .putString("file_path", file.absolutePath)
                .putString("step", "Compression done")
                .build()
        )
    }
}

class WatermarkWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val inputPath = inputData.getString("file_path") ?: return Result.failure(
            Data.Builder().putString("error", "Missing input file").build()
        )

        setProgress(Data.Builder().putString("step", "Adding watermark").putInt("progress", 55).build())
        delay(1000)

        val watermarked = File(applicationContext.filesDir, "watermarked_photo.txt")
        watermarked.writeText(File(inputPath).readText() + " + watermark")

        return Result.success(
            Data.Builder()
                .putString("file_path", watermarked.absolutePath)
                .putString("step", "Watermark done")
                .build()
        )
    }
}

class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val inputPath = inputData.getString("file_path") ?: return Result.failure(
            Data.Builder().putString("error", "Missing watermarked file").build()
        )

        setProgress(Data.Builder().putString("step", "Uploading").putInt("progress", 85).build())
        delay(1400)

        val result = "Uploaded: ${File(inputPath).name}"
        return Result.success(
            Data.Builder()
                .putString("result", result)
                .putString("step", "Upload finished")
                .build()
        )
    }
}
