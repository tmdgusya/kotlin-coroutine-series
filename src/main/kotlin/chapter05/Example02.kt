package chapter05

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

suspend fun main() = coroutineScope {
    val job = Job()
    launch(job) {
        repeat(100) { i ->
            if (job.isCancelled) return@launch
            yield()
            Thread.sleep(100)
            println("Printing $i")
        }
    }

    delay(10)
    job.cancelAndJoin()
    println("Cancelled Successfully")
}