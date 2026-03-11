package com.example.task04

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import kotlin.random.Random

enum class PostCardState { Loading, Ready, Error }

data class Post(
    val id: Int,
    val author: String,
    val text: String
)

data class PostUi(
    val post: Post,
    val state: PostCardState = PostCardState.Loading,
    val avatar: String? = null,
    val comments: List<String> = emptyList(),
    val error: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: FeedViewModel = viewModel()
                FeedScreen(vm)
            }
        }
    }
}

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = kotlinx.coroutines.flow.MutableStateFlow<List<PostUi>>(emptyList())
    val items = _items.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val posts = loadPosts()
            val commentsMap = loadComments()
            _items.value = posts.map { PostUi(post = it, state = PostCardState.Loading) }

            posts.forEach { post ->
                launch {
                    try {
                        val avatar = loadAvatar(post.author)
                        val comments = loadPostComments(post.id, commentsMap)
                        updatePost(post.id) {
                            it.copy(state = PostCardState.Ready, avatar = avatar, comments = comments, error = null)
                        }
                    } catch (e: Exception) {
                        updatePost(post.id) {
                            it.copy(state = PostCardState.Error, error = e.message ?: "Unknown error")
                        }
                    }
                }
            }
        }
    }

    private fun updatePost(id: Int, transform: (PostUi) -> PostUi) {
        _items.value = _items.value.map { item -> if (item.post.id == id) transform(item) else item }
    }

    private fun loadPosts(): List<Post> {
        val raw = getApplication<Application>().assets.open("posts.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        return List(arr.length()) { idx ->
            val o = arr.getJSONObject(idx)
            Post(o.getInt("id"), o.getString("author"), o.getString("text"))
        }
    }

    private fun loadComments(): Map<Int, List<String>> {
        val raw = getApplication<Application>().assets.open("comments.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        return buildMap {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                put(o.getInt("postId"), listOf(o.getString("comment1"), o.getString("comment2")))
            }
        }
    }

    private suspend fun loadAvatar(author: String): String {
        delay(Random.nextLong(350, 1200))
        if (Random.nextInt(100) < 20) {
            error("Avatar loading failed")
        }
        return author.take(1).uppercase()
    }

    private suspend fun loadPostComments(postId: Int, commentsMap: Map<Int, List<String>>): List<String> {
        delay(Random.nextLong(300, 1300))
        if (Random.nextInt(100) < 15) {
            error("Comments loading failed")
        }
        return commentsMap[postId].orEmpty()
    }
}

@Composable
private fun FeedScreen(vm: FeedViewModel) {
    val items by vm.items.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Social Feed", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = vm::refresh) { Text("Обновить") }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.post.author, fontWeight = FontWeight.Bold)
                            Text(item.post.text)

                            when (item.state) {
                                PostCardState.Loading -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
                                        Text("Loading...")
                                    }
                                }
                                PostCardState.Ready -> {
                                    Text("Avatar: ${item.avatar}")
                                    Text("Comments: ${item.comments.joinToString()}")
                                }
                                PostCardState.Error -> {
                                    Text("Error: ${item.error}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
