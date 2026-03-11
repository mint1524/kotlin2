package com.example.task09

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WeatherReportWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val cities = listOf("Moscow", "Saint Petersburg", "Kazan", "Sochi")

    override suspend fun doWork(): Result {
        createChannel()
        setForeground(foregroundInfo("Starting weather job"))

        val completed = mutableListOf<Pair<String, Int>>()
        val mutex = Mutex()

        return try {
            val results = coroutineScope {
                cities.map { city ->
                    async {
                        val temp = fetchWeather(city)
                        mutex.withLock {
                            completed.add(city to temp)
                            val done = completed.size
                            val status = "Loaded $city (${done}/${cities.size})"
                            val progressData = Data.Builder()
                                .putInt("done", done)
                                .putInt("total", cities.size)
                                .putString("status", status)
                                .build()
                            setProgress(progressData)
                            setForeground(foregroundInfo(status))
                        }
                        city to temp
                    }
                }.map { it.await() }
            }

            val avg = results.map { it.second }.average()
            val report = results.joinToString { "${it.first}: ${it.second}°C" }
            setForeground(foregroundInfo("Building final report"))
            delay(500)

            Result.success(
                Data.Builder()
                    .putDouble("avg", avg)
                    .putString("report", report)
                    .putString("status", "Completed")
                    .build()
            )
        } catch (e: Exception) {
            Result.failure(Data.Builder().putString("error", e.message ?: "unknown").build())
        }
    }

    private suspend fun fetchWeather(city: String): Int = withContext(Dispatchers.Default) {
        delay(Random.nextLong(800, 2000))
        Random.nextInt(-10, 31)
    }

    private fun foregroundInfo(text: String): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Weather pipeline")
            .setContentText(text)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Weather work", NotificationManager.IMPORTANCE_LOW)
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "weather_report_channel"
        private const val NOTIFICATION_ID = 9001
    }
}
