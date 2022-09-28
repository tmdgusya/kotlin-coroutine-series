package chapter03

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

suspend fun main(): Unit = coroutineScope {
    val superCoroutineName = CoroutineName("Super")
    println("[Super Ctx] Name : ${superCoroutineName[CoroutineName]?.name}")

    val subCoroutine = Job()
    println("[Sub Ctx] Name: ${subCoroutine[CoroutineName]?.name}")

    val mergedCtx = superCoroutineName + subCoroutine
    println("[Merged Ctx] Name : ${mergedCtx[CoroutineName]?.name}")

    val coroutineName2 = CoroutineName("CoroutineName2")
    val mergedCtx2 = subCoroutine + coroutineName2
    println("[Merged Ctx2] Name : ${mergedCtx2[CoroutineName]?.name}")
}
