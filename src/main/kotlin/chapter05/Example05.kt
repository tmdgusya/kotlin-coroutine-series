package chapter05

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    val deffer = handlerScope.async {
        delay(100)
        throw Error("Error!!")
    }

    kotlin.runCatching {
        deffer.await()
    }.onSuccess { println("Success") }
        .onFailure { println("Failed Job..") }

    handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

}