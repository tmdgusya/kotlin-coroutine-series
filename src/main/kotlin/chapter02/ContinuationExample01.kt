package chapter02

import kotlinx.coroutines.delay
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

suspend fun originalFunction() {
    var thisLocalVariable: Int = 10
    var thisLocalVariable2: String = "Local Value"

    println("Start!!")
    delay(1000)
    println(thisLocalVariable)
    println(thisLocalVariable2)
    println("End")
}

class OriginalFunctionContination(
    val completion: Continuation<Unit>
):  Continuation<Unit> {
    override val context: CoroutineContext
        get() = completion.context

    override fun resumeWith(result: Result<Unit>) {
        this.result = result
        val res = try {
            val r = originalFunction(continuation = this)
            if (r == "COROUTINE_SUSPEND") return
            Result.success(r as Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
        completion.resumeWith(res)
    }

    var result: Result<Unit>? = null
    var label = 0
    var thisLocalVariable = 10
    var thisLocalVariable2 = "Local Value"
}

@JvmName("originalFunction1")
fun originalFunction(continuation: Continuation<Unit>): Any {

    continuation as OriginalFunctionContination

    if (continuation.label == 0) {
        continuation.thisLocalVariable = 10
        continuation.thisLocalVariable2 = "Local Value"

        println("Start!!")

        continuation.label = 1

        if (delay(1000, continuation) == "COROUTINE_SUSPEND") {
            return "COROUTINE_SUSPEND"
        }
    }

    if (continuation.label == 1) {
        println(continuation.thisLocalVariable)
        println(continuation.thisLocalVariable2)
        println("End")
        return Unit
    }

    error("정상적인 종료가 아님")
}

/**
 * 예제를 위한 임시 구현 함수
 */
fun delay(delayTime: Int, continuation: Continuation<Unit>): String {
    return "COROUTINE_SUSPEND"
}