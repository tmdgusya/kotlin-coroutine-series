package chapter01

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class VideoGame(
    var checkPoint: Int
): AbstractCoroutineContextElement(VideoGame) {
    fun nextChapter() {
        this.checkPoint++
    }

    companion object Key: CoroutineContext.Key<VideoGame>
}

private suspend fun playVideoGame(videoGame: VideoGame) {
    println("play the game..")
    videoGame.nextChapter()
}

private suspend fun stopVideoGame(videoGame: VideoGame) {
    println("Stop This Game [Current Chapter : ${videoGame.checkPoint}]")
}

private suspend fun eatMeals(videoGame: VideoGame, resumeVideoGame: ((v: VideoGame) -> Unit)) {
    println("eat Meals")
    resumeVideoGame.invoke(videoGame)
}

suspend fun main() {
    val lastOfEarth = VideoGame(0)
    val resumeFunc = { videoGame: VideoGame -> println("resume Chapter${videoGame.checkPoint}...") }

    playVideoGame(lastOfEarth)
    stopVideoGame(lastOfEarth)
    eatMeals(lastOfEarth, resumeFunc)
}

