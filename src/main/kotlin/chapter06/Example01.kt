package chapter06

import chapter04.printChildJobState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

suspend fun main() = withContext(CoroutineName("Chpater06")) {

    val parentJob = coroutineContext[Job]

    val a = coroutineScope {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }

    println("A is Done!!")

    val b = coroutineScope {
        delay(1000)
        println(coroutineContext[CoroutineName])
        20
    }

    println(a) // 1초 지연
    println(b) // 1초 지연
}