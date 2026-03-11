package com.example.task14

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class CompassState(
    val azimuth: Float = 0f,
    val hasSensors: Boolean = true,
    val error: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: CompassViewModel = viewModel()
                CompassScreen(vm)
            }
        }
    }
}

class CompassViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravityValues = FloatArray(3)
    private val magneticValues = FloatArray(3)

    private val _state = MutableStateFlow(
        CompassState(
            hasSensors = accelerometer != null && magnetometer != null,
            error = if (accelerometer == null || magnetometer == null) "Required sensors are not available" else ""
        )
    )
    val state: StateFlow<CompassState> = _state.asStateFlow()

    fun start() {
        if (!_state.value.hasSensors) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravityValues[0] = event.values[0]
                gravityValues[1] = event.values[1]
                gravityValues[2] = event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues[0] = event.values[0]
                magneticValues[1] = event.values[1]
                magneticValues[2] = event.values[2]
            }
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val ok = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)
        if (ok) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRadians = orientation[0]
            val azimuthDegrees = (Math.toDegrees(azimuthRadians.toDouble()).toFloat() + 360f) % 360f
            _state.value = _state.value.copy(azimuth = azimuthDegrees)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

@Composable
private fun CompassScreen(vm: CompassViewModel) {
    val state by vm.state.collectAsState()
    val rotation by animateFloatAsState(targetValue = -state.azimuth, label = "compass_rotation")

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.start()
                Lifecycle.Event.ON_PAUSE -> vm.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stop()
        }
    }

    Scaffold(
        containerColor = Color(0xFF101418),
        contentColor = Color(0xFFE6F0F5)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!state.hasSensors) {
                Text(state.error)
                return@Column
            }

            Text(
                text = "${state.azimuth.roundToInt()}°",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color(0xFF1E2A32)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.rotate(rotation)
                )
            }
            Text("North")
        }
    }
}
