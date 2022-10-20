package example07

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

suspend fun main() = withContext(Dispatchers.IO) {
    repeat(1000) {
        launch {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}