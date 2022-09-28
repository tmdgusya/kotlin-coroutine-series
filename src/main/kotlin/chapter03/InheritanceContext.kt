package chapter03

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val superScopeContext = CoroutineName("Super!!")

suspend fun main(): Unit = withContext(superScopeContext) {

    val ctx = this.coroutineContext[CoroutineName]
    println("Parent Job : ${ctx?.name}")

    launch {
        val childCtx = this@launch.coroutineContext[CoroutineName]
        println("Child Job : ${childCtx?.name}")
    }
}