package chapter05

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Error

val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    println("Current Context : [${coroutineContext}]") // Context
    println("Exception cause : [${throwable.message}]") // Error!!
}

suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    val job1 = handlerScope.launch {
        delay(100)
        throw Error("Error!!")
    }

    val job2 = handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

    println(job1.isCancelled)
    println(job2.isCancelled)
}