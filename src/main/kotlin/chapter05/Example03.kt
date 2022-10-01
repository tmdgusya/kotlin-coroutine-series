package chapter05

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
    println("Start")
    test()
    println("End")
}

suspend fun test(): Unit = coroutineScope {

    launch(SupervisorJob()) {
        launch {
            println("[1-1] Job!")
        }

        launch {
            throw Error("Error!!!!")
        }

        launch {
            println("[1-2] Job!")
        }
    }

    launch {
        delay(1000)
        println("[2-1] Job!")
    }

    delay(2000)
}