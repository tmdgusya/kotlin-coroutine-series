package chapter06

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

suspend fun main() = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    val child1 = withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }

    val child2 = withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }

}