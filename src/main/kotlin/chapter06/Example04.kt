package chapter06

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.lang.Error
import kotlin.coroutines.EmptyCoroutineContext

suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    withContext(SupervisorJob()) {
        launch(SupervisorJob()) {
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