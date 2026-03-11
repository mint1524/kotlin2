package com.example.task10

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationUiState(
    val loading: Boolean = false,
    val address: String = "",
    val coords: String = "",
    val error: String = ""
)

class MainActivity : ComponentActivity() {
    private val hasPermission = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission.value = result.values.any { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasPermission.value = checkLocationPermission()

        setContent {
            MaterialTheme {
                val vm: LocationViewModel = viewModel()
                LocationScreen(
                    vm = vm,
                    hasPermission = hasPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    fun loadAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = LocationUiState(loading = true)
            try {
                val context = getApplication<Application>()
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val location = fused.lastLocation.await() ?: error("Location is unavailable. Turn on GPS and retry.")
                val lat = location.latitude
                val lon = location.longitude

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressLine = addresses?.firstOrNull()?.getAddressLine(0)
                    ?: "Address not found"

                _state.value = LocationUiState(
                    loading = false,
                    address = addressLine,
                    coords = "lat: %.5f, lon: %.5f".format(lat, lon),
                    error = ""
                )
            } catch (e: Exception) {
                _state.value = LocationUiState(
                    loading = false,
                    error = e.message ?: "Failed to get location"
                )
            }
        }
    }
}

@Composable
private fun LocationScreen(
    vm: LocationViewModel,
    hasPermission: State<Boolean>,
    onRequestPermission: () -> Unit
) {
    val state by vm.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Get my address", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = {
                if (!hasPermission.value) onRequestPermission() else vm.loadAddress()
            }) {
                Text("Получить мой адрес")
            }

            if (state.loading) {
                CircularProgressIndicator()
            }
            if (state.address.isNotBlank()) {
                Text("Address:")
                Text(state.address, style = MaterialTheme.typography.titleMedium)
                Text("Coordinates: ${state.coords}")
            }
            if (state.error.isNotBlank()) {
                Text("Error: ${state.error}")
            }
            if (!hasPermission.value) {
                Text("Location permissions are required.")
            }
        }
    }
}
