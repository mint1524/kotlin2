import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
    val targetDir = args.getOrNull(0)?.let(Path::of) ?: Path.of("./sample-data")
    val timeoutSeconds = args.getOrNull(1)?.toLongOrNull() ?: 5L

    if (!Files.exists(targetDir)) {
        println("Directory does not exist: ${targetDir.pathString}")
        exitProcess(1)
    }

    println("Task 02: Find duplicate JSON files by SHA-256")
    println("Directory: ${targetDir.toAbsolutePath()}")
    println("Timeout: ${timeoutSeconds}s")

    val result = withTimeoutOrNull(timeoutSeconds * 1_000L) {
        val jsonFiles = findJsonFiles(targetDir)
        if (jsonFiles.isEmpty()) {
            println("No .json files found")
            return@withTimeoutOrNull emptyMap<String, List<Path>>()
        }

        val fileHashes = supervisorScope {
            jsonFiles.map { file ->
                async(Dispatchers.IO) {
                    file to computeSha256(file)
                }
            }.awaitAll()
        }

        fileHashes.groupBy({ it.second }, { it.first }).filterValues { it.size > 1 }
    }

    if (result == null) {
        println("Поиск прерван по таймауту")
        return@runBlocking
    }

    if (result.isEmpty()) {
        println("Duplicates were not found")
        return@runBlocking
    }

    println("Found duplicate groups:")
    result.forEach { (hash, files) ->
        println("SHA-256: $hash")
        files.forEach { println(" - ${it.pathString}") }
    }
}

private fun findJsonFiles(root: Path): List<Path> {
    Files.walk(root).use { stream ->
        return stream
            .filter { it.isRegularFile() && it.name.endsWith(".json", ignoreCase = true) }
            .sorted()
            .toList()
    }
}

private suspend fun computeSha256(path: Path): String = withContext(Dispatchers.IO) {
    // Small delay helps demonstrate global timeout cancellation in local tests.
    delay(1_200)
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = input.read(buffer)
        while (read >= 0) {
            if (read > 0) digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    }

    digest.digest().joinToString("") { "%02x".format(it) }
}
