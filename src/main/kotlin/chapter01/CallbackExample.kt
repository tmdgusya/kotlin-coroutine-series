package chapter01

import kotlin.concurrent.thread

fun process1(callback: () -> Unit) = thread {
    Thread.sleep(1000)
    // do some process
    callback.invoke() // invoke callback
}

fun main() {
    val callbackFn = { println(", World!") }
    process1(callbackFn)
    println("Hello")
    Thread.sleep(2000) // join
}