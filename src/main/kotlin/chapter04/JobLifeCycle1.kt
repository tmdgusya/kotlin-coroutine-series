package chapter04

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {

    val newStateJob = launch(start = CoroutineStart.LAZY) {
        println("[LAZY-JOB] START!")
        delay(1000)
        println("[LAZY-JOB] END!")
    }

    val defaultJob = launch {
        println("[DEFAULT-JOB] START!")
        delay(1000)
        println("[DEFAULT-JOB] END!")
    }

    defaultJob.join()
    newStateJob.join()
}