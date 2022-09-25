package chapter01

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val context = Dispatchers.Default + VideoGame(0)


suspend fun main() = withContext(context) {
    playTheGame(this.coroutineContext)
    stopTheGam(this.coroutineContext)
    val currentVideoGame = suspendCoroutine<VideoGame> { continuation ->
        eatMeals(continuation.context)
        continuation.resume(continuation.context[VideoGame]!!)
    }

    resumeGame(currentVideoGame)
}

fun playTheGame(context: CoroutineContext) {
    val currentGame = context[VideoGame]

    println("play the game..")
    currentGame?.nextChapter()
}

fun stopTheGam(context: CoroutineContext) {
    val currentGame = context[VideoGame]

    println("Stop This Game [Current Chapter : ${currentGame?.checkPoint}]")
}

fun resumeGame(context: CoroutineContext) {
    val currentGame = context[VideoGame]

    println("resume Chapter${currentGame?.checkPoint}...")
}

fun eatMeals(context: CoroutineContext) {
    println("eat mills.. $context")
    println("eat Meals")
}