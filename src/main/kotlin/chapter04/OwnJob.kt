package chapter04

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


suspend fun main(): Unit = coroutineScope {

    val parentJob = coroutineContext[Job]
    println("[ParentJob] ${parentJob}")

    val job = launch(parentJob!!) {
        val childJob = coroutineContext[Job]
        println(parentJob == childJob) // false
        println(parentJob.children.first() == childJob) // true
    }

    job.join()
}