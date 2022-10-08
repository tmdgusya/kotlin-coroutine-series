package chapter06

import chapter04.printChildJobState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.lang.Error

suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    supervisorScope {
        val job = coroutineContext[Job]

        println(job!!::class)
        launch {
            throw Error("!!!!")
            10
        }

        launch {
            delay(500)
            println("Executed?")
        }
    }

    withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
}