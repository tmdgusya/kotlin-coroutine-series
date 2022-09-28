package chapter04

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope {

    val parentJob = launch {

        println("Parent Job is Start!!")

        val childJob = launch {
            delay(200)
            println("Child Job 1 is Start!!")
        }

        val childJob2 = launch {
            delay(300)
            println("Child Job 2 is Start!!")
        }

        println("Child Job1 is Finished ? ${childJob.isCompleted}")
        println("Child Job2 is Finished ? ${childJob2.isCompleted}")

        println("Parent Job is Done!!")
    }

    delay(100)

    parentJob.printChildJobState()
    println("[IMPORTANT-LOG-1] Parent Job is Finished ? ${parentJob.isCompleted}")

    delay(300)
    parentJob.printChildJobState()
    println("[IMPORTANT-LOG-2] Parent Job is Finished ? ${parentJob.isCompleted}")
}

fun Job.printChildJobState() {
    if (this.children.firstOrNull() == null) println("All ChildrenJob is finished")
    var count = 1
    for (childJob in this.children) {
        println("Child Job$count is Finished ? ${childJob.isCompleted}")
        count++
    }
}