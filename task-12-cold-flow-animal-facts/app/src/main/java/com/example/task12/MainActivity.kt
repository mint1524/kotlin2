package com.example.task12

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: FactsViewModel = viewModel()
                FactsScreen(vm)
            }
        }
    }
}

class FactsViewModel : ViewModel() {
    private val facts = listOf(
        "Octopuses have three hearts.",
        "Dolphins have names for each other.",
        "A group of flamingos is called a flamboyance.",
        "Wombat poop is cube-shaped.",
        "Otters hold hands while sleeping.",
        "Sea horses are monogamous during a season.",
        "Elephants can recognize themselves in mirrors.",
        "Crows can remember human faces.",
        "Polar bears have black skin.",
        "Koalas sleep up to 20 hours a day.",
        "Mantis shrimp can see polarized light.",
        "Giraffes have the same number of neck bones as humans.",
        "Penguins propose with pebbles.",
        "Honey never spoils.",
        "Frogs absorb water through their skin."
    )

    private val _currentFact = MutableStateFlow("Нажмите 'Новый факт!'")
    val currentFact = _currentFact.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun getRandomFact(): Flow<String> = flow {
        delay(Random.nextLong(1_500, 3_001))
        emit(facts.random())
    }

    fun requestNewFact() {
        viewModelScope.launch {
            _loading.value = true
            getRandomFact().collect { fact ->
                _currentFact.value = fact
                _loading.value = false
            }
        }
    }
}

@Composable
private fun FactsScreen(vm: FactsViewModel) {
    val loading by vm.loading.collectAsState()
    val fact by vm.currentFact.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cold Flow animal facts", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = vm::requestNewFact, enabled = !loading) {
                Text("Новый факт!")
            }

            if (loading) {
                CircularProgressIndicator()
            }

            Card {
                Text(
                    text = fact,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
