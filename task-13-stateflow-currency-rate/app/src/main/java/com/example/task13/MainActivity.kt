package com.example.task13

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Direction { UP, DOWN, SAME }

data class RateState(
    val current: Double = 90.0,
    val previous: Double = 90.0,
    val direction: Direction = Direction.SAME
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: CurrencyViewModel = viewModel()
                CurrencyScreen(vm)
            }
        }
    }
}

class CurrencyViewModel : ViewModel() {
    private val _rate = MutableStateFlow(RateState())
    val rate: StateFlow<RateState> = _rate.asStateFlow()

    private var tickerJob: Job? = null

    init {
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                updateRate()
            }
        }
    }

    fun refreshNow() {
        updateRate()
    }

    private fun updateRate() {
        val old = _rate.value.current
        val next = (old + Random.nextDouble(-1.5, 1.5)).coerceIn(70.0, 130.0)
        val direction = when {
            next > old -> Direction.UP
            next < old -> Direction.DOWN
            else -> Direction.SAME
        }
        _rate.value = RateState(current = next, previous = old, direction = direction)
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}

@Composable
private fun CurrencyScreen(vm: CurrencyViewModel) {
    val state by vm.rate.collectAsState()

    val arrow = when (state.direction) {
        Direction.UP -> "↑"
        Direction.DOWN -> "↓"
        Direction.SAME -> "→"
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("USD/RUB", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${"%.2f".format(state.current)} ₽  $arrow",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Previous: ${"%.2f".format(state.previous)} ₽")

            Button(onClick = vm::refreshNow) {
                Text("Обновить сейчас")
            }
        }
    }
}
