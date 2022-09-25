package chapter01

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

var cont: Continuation<Unit>? = null

suspend fun test() {
    suspendCoroutine<Unit> { continuation ->
        cont = continuation
    }
}

suspend fun main() = coroutineScope {
    println("Start")

    launch {
        delay(500)
        cont?.resume(Unit)
    }

    test()
    println("End")
}