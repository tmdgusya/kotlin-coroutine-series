package example07

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

suspend fun main(): Unit = coroutineScope {
    launch {
        printCoroutineTime(Dispatchers.IO)
    }

    launch {
        printCoroutineTime(Dispatchers.IO.limitedParallelism(100))
    }
}

@OptIn(ExperimentalTime::class)
suspend fun printCoroutineTime(dispatcher: CoroutineDispatcher) {

    val test = measureTime {
        coroutineScope {
            repeat(100) {
                launch(dispatcher) {
                    Thread.sleep(1000) // 1 초가 걸리는 Task
                }
            }
        }
    }

    println("$dispatcher took: $test")

    Dispatchers.Unconfined

}