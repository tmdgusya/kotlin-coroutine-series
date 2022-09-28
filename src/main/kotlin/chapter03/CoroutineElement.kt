package chapter03

import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CustomCoroutineElement : AbstractCoroutineContextElement(CustomCoroutineElement) {
    var name: String = "Custom Coroutine Element #1"
    var errorCount: Int = 0

    fun increaseErrorCount() {
        ++errorCount
    }

    companion object Key: CoroutineContext.Key<CustomCoroutineElement>
}

suspend fun main() = withContext(CustomCoroutineElement()) {
    val myCustomCtx = coroutineContext[CustomCoroutineElement]
    println("Current Job Name : ${myCustomCtx?.name}")
    println("Current Job errorCount : ${myCustomCtx?.errorCount}")

    return@withContext Unit
}