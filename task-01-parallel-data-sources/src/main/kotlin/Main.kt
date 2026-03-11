import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Serializable
data class UsersPayload(val users: List<String>)

@Serializable
data class SalesPayload(val sales: Map<String, Int>)

@Serializable
data class WeatherPayload(val weather: List<WeatherEntry>)

@Serializable
data class WeatherEntry(val city: String, val tempC: Int)

sealed class LoadResult<out T> {
    data class Success<T>(val data: T) : LoadResult<T>()
    data class Failure(val message: String) : LoadResult<Nothing>()
}

private val json = Json { ignoreUnknownKeys = true }

fun main() = runBlocking {
    println("Task 01: Parallel data sources")

    val elapsedMs = measureTimeMillis {
        val results = supervisorScope {
            val usersDeferred = async { safeLoad("Users") { loadUsers() } }
            val salesDeferred = async { safeLoad("Sales") { loadSales() } }
            val weatherDeferred = async { safeLoad("Weather") { loadWeather() } }

            Triple(usersDeferred.await(), salesDeferred.await(), weatherDeferred.await())
        }

        printUsers(results.first)
        printSales(results.second)
        printWeather(results.third)
    }

    println("Total execution time: ${elapsedMs} ms")
}

private suspend fun loadUsers(): List<String> {
    delay(1_800)
    maybeFail("users")
    val payload = readResource("users.json")
    return json.decodeFromString<UsersPayload>(payload).users
}

private suspend fun loadSales(): Map<String, Int> {
    delay(1_200)
    maybeFail("sales")
    val payload = readResource("sales.json")
    return json.decodeFromString<SalesPayload>(payload).sales
}

private suspend fun loadWeather(): List<String> {
    delay(2_500)
    maybeFail("weather")
    val payload = readResource("weather.json")
    return json.decodeFromString<WeatherPayload>(payload).weather.map { "${it.city}: ${it.tempC}°C" }
}

private suspend fun <T> safeLoad(name: String, block: suspend () -> T): LoadResult<T> {
    return try {
        val result = block()
        LoadResult.Success(result)
    } catch (e: Exception) {
        LoadResult.Failure("$name source failed: ${e.message}")
    }
}

private fun maybeFail(source: String) {
    val forced = System.getenv("FORCE_FAIL_SOURCE")
    if (forced != null && forced.equals(source, ignoreCase = true)) {
        error("forced failure for $source")
    }

    if (Random.nextInt(100) < 15) {
        error("random failure while loading $source")
    }
}

private fun readResource(name: String): String {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(name)
        ?: error("Resource '$name' not found")
    return stream.bufferedReader().use { it.readText() }
}

private fun printUsers(result: LoadResult<List<String>>) {
    when (result) {
        is LoadResult.Success -> println("Users: ${result.data.joinToString()}")
        is LoadResult.Failure -> println("Users: ${result.message}")
    }
}

private fun printSales(result: LoadResult<Map<String, Int>>) {
    when (result) {
        is LoadResult.Success -> println("Sales: ${result.data.entries.joinToString { "${it.key}=${it.value}" }}")
        is LoadResult.Failure -> println("Sales: ${result.message}")
    }
}

private fun printWeather(result: LoadResult<List<String>>) {
    when (result) {
        is LoadResult.Success -> println("Weather: ${result.data.joinToString()}")
        is LoadResult.Failure -> println("Weather: ${result.message}")
    }
}
