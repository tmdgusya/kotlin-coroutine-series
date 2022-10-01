import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope {

    val job1 = launch {
        try {
            delay(1000)
            println("TESTEST")
        } catch (e: CancellationException) {
            println(e)
            throw e
        } finally {
            println("Resource Closing...")
        }
    }
    job1.cancel()
    job1.join()
    println("Job is Done? ${job1.isCompleted}")
    println("Job is Cancelled ? ${job1.isCancelled}")
}