package com.example.task03

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: RepoSearchViewModel = viewModel()
                SearchScreen(vm)
            }
        }
    }
}

data class RepoItem(
    val name: String,
    val description: String,
    val language: String
)

data class RepoUiState(
    val query: String = "",
    val loading: Boolean = false,
    val items: List<RepoItem> = emptyList(),
    val loaded: Boolean = false
)

class RepoSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(RepoUiState())
    val state: StateFlow<RepoUiState> = _state.asStateFlow()

    private var allRepos: List<RepoItem> = emptyList()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allRepos = loadReposFromAssets(application)
            _state.update { it.copy(loaded = true) }
        }
    }

    fun onQueryChange(input: String) {
        _state.update { it.copy(query = input) }
        searchJob?.cancel()

        if (input.isBlank()) {
            _state.update { it.copy(loading = false, items = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(loading = true) }
            delay(500)

            val normalized = input.trim().lowercase()
            val filtered = allRepos.filter {
                it.name.lowercase().contains(normalized) ||
                    it.description.lowercase().contains(normalized) ||
                    it.language.lowercase().contains(normalized)
            }
            _state.update { it.copy(loading = false, items = filtered) }
        }
    }

    private fun loadReposFromAssets(application: Application): List<RepoItem> {
        val json = application.assets.open("github_repos.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    RepoItem(
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        language = obj.getString("language")
                    )
                )
            }
        }
    }
}

@Composable
private fun SearchScreen(vm: RepoSearchViewModel) {
    val state by vm.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("GitHub Search (debounce)", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                label = { Text("Search by name / description / language") },
                modifier = Modifier.fillMaxWidth()
            )

            when {
                state.loading -> CircularProgressIndicator()
                state.query.isBlank() -> Text("Start typing to search repositories")
                state.items.isEmpty() -> Text("Nothing found")
                else -> RepoList(state.items)
            }
        }
    }
}

@Composable
private fun RepoList(items: List<RepoItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { repo ->
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(repo.name, fontWeight = FontWeight.Bold)
                    Text(repo.description)
                    Text("Language: ${repo.language}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
