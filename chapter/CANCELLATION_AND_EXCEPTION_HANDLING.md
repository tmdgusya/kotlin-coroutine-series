# Cancellation

Cancellation 은 Coroutine 에서 정말 중요한 기능 중 하나이다. 앞서 Job Life Cycle 에 대해 우리가 공부했을때, 
Job 이 취소 될 수 있다는 것을 공부했었다. 또한 취소될때 곧바로 취소되는 것이 아니라 Cancelling State 에서 취소 되기전 
Resource 반납등의 작업을 한다는 것 또한 알게 되었다. 따라서 코루틴을 활용한 Library 나 Framework 를 구성하게 될때, 
Cancelling 상태에서 어떤 작업을 할지를 잘 구성해야 자원을 낭비하지 않게 잘 구성될 수 있을 것 이다.

이번장은 중요한 만큼 Coroutine 코드도 많고, 내부 동작을 자세하게 들여볼 예정이다.

## Cancel

우리가 앞전에 봤던 Job 에는 `cancel()` 이라는 메소드를 제공한다. 이 cancel 메소드가 발생했을때 일어나는 일을 아래와 같다. 

- **현재 Job 이 자식을 가지고 있으면 모든 자식 Job 도 모두 취소된다.**
- **코루틴이 첫번째 일시 중단지점에서 종료된다.**

위와 같은 동작이 어떻게 가능한 것일까? 이제 천천히 알아보도록 하자. 아래 코드를 한번 보자, 우리는 아래와 같이 cancel 이라는 method 호출만으로 코루틴을 cancel() 시킬 수 있다.

```kotlin
suspend fun main() = coroutineScope {

    val job1 = launch {
        println("TESTEST")
    }

    job1.cancel()
}
```

### CancellationException

cancel 이 발생했을때 우리가 사용하고 있는 자원을 반납하는 등의 작업을 해야 한다고 배웠는데 해당 작업은 어디서 진행해야 할까? 
제일 간단한 방법으로는, 아래 코드와 같이 **try..catch..finally 에서 자원 반납과 같은 작업등을 진행**해야 한다.

```kotlin
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
}
```

특별하게 Handler 를 구성해서 자원 반납등의 작업을 할 것 같지만, 간단하게 **CancellationException** 이라는 에러가 발생하게 될때 자원 반납등의 작업을 하는 
것을 확인할 수 있다. 내 코드에서는 CancellationException 이 발생하는 부분이 없는데 어디서 발생하는 것일까? 한번 내부 과정을 알아보자.

사실 `cancel(cause: CancellationException?)` 메소드는 CancellationException 을 입력하게 되어 있다. 즉, 어떤 예외로 취소 시킬 것인지 적어줘야 하는데 아무것도 적지 않으면 
**DefaultCancellationException** 을 기본적으로 넣어주게 되어 있다. 즉, cancel 메소드가 동작할때 기본적으로 CancellationException 이 발생함을 알 수 있는 것 이다.

```kotlin
cancelInternal(cause ?: defaultCancellationException())
```

위 코드와 같이 내부적으로 CancellationException 을 이용하게 되어 있다.

### 즉시 Cancel 이 가능할까?

우리가 `cancel()` 메소드를 실행시키면 그 즉시 코루틴이 정지 될까? 아마 LifeCycle 챕터를 유심히 보았다면 completing -> completed 가 되야 결국 코루틴이 종료된 다는 것을 알게 되었을 것 이다. 
아래 코드를 테스트 겸 한번 실행시켜 보자.

```kotlin
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
    println("Job is Done? ${job1.isCompleted}")
    println("Job is Cancelled ? ${job1.isCancelled}")
}
```

위 코드를 실행시키면 결과가 아래와 같이 나온다. 

```kotlin
Job is Done? false
Job is Cancelled ? true
kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled;
Resource Closing...
```

Resource Closing 이 제일 늦는걸 보면 `cancel()` 메소드가 동작하자마자 Job 이 멈추는게 아니라는 걸 확인할 수 있다. 우리가 해당 Job 의 
종료를 기다리기 위해서는 어떻게 해야 할까? 바로 앞서서 배운 `join()` 메소드를 활용해야 한다.

```kotlin
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
    job1.join() // join 메소드 추가
    println("Job is Done? ${job1.isCompleted}")
    println("Job is Cancelled ? ${job1.isCancelled}")
}
```

```shell
kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelling}@638bec04
Resource Closing...
Job is Done? true
Job is Cancelled ? true
```

Join Method 를 추가하자 우리가 원한대로 launch 가 모두 종료되고 나서 이후 과정이 실행되는 것을 확인할 수 있다.

### InvokeOnCompletion

우리가 해당 Job 이 끝난 후 자원을 반납하거나 등의 후처리 작업을 해줘야 한다면, InvokeOnCompletion 을 이용 하는 방법도 있다. 
InvokeOnCompletion 은 코루틴이 종료된 후 실행되는 하나의 Handler 이다. 매개변수로 cause 를 받는데, 만약 코루틴이 
정상적으로 종료됬다면 cause 가 null 이고, cancel 됬다면 `cause is CancellationException` 이 true 이다.  

아래 코드를 한번 같이 보자

```kotlin
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

    job1.invokeOnCompletion { cause ->
        if (cause == null) {
            println("Finished Normally") // 실행되지 않음.
        }

        if (cause is CancellationException) {
            println("Coroutine Cancelled") // 실행됨
        }
        
        println("Resource Finished!!!!!!") // 실행됨
    }

    job1.cancel()
    job1.join()
    println("Job is Done? ${job1.isCompleted}")
    println("Job is Cancelled ? ${job1.isCancelled}")
}
```

실행결과는 아래와 같다.

```kotlin
kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelling}@753dcbde
Resource Closing...
Coroutine Cancelled
Resource Finished!!!!!!
Job is Done? true
Job is Cancelled ? true
```

## Cancel at first suspension point

첫번째 중단지점에서 종료된다는 점이 상당히 중요하다. 동시성 프로그래밍에서 흔히 말하는 hot-spot 코드 안에 만약 일시 중단 지점이 없다면, 
해당 Job 은 hot-spot 코드를 끝까지 반복하게 된다. 

```kotlin
suspend fun main() = coroutineScope {
    val job = Job()
    launch(job) {
        repeat(100) { i ->
            Thread.sleep(100)
            println("Printing $i")
        }
    }

    delay(10)
    job.cancelAndJoin()
    println("Cancelled Successfully")
}
```

위의 코드를 한번 보자. 정확히 10 millis 이후에 cancel 되면서 해당 Job 이 종료되야 할 것 같아 보이지만, 종료되지 않고 100 까지 모두 출력한다.  
그 이유는 위에서 얘기했듯이 Coroutine 은 첫번째 일시 중단 지점에서 cancel 되기 때문이다. 그렇다면 어떻게 이 코드를 중단 시킬 수 있을까? 
방법은 여러가지로 그냥 의미없는 suspension point 를 만들어줄 수도 있다.

```kotlin
suspend fun main() = coroutineScope {
    val job = Job()
    launch(job) {
        repeat(100) { i ->
            yield()
            Thread.sleep(100)
            println("Printing $i")
        }
    }

    delay(10)
    job.cancelAndJoin()
    println("Cancelled Successfully")
}
```

위와 같이 yield() 를 통해서 suspension point 를 만들어 주었다. 위와 같은 방법을 이용할 수도 있으나, 우리가 멀티스레딩 프로그래밍에서 Interrupted 됬는지를 확인하는 것과 같이 
아래처럼 코드를 작성할 수도 있다.

```kotlin
suspend fun main() = coroutineScope {
    val job = Job()
    launch(job) {
        repeat(100) { i ->
            if (job.isCancelled) return@launch
            yield()
            Thread.sleep(100)
            println("Printing $i")
        }
    }

    delay(10)
    job.cancelAndJoin()
    println("Cancelled Successfully")
}
```

이와는 별개로 `ensureActive()` 와 같은 방법도 있으나 따로 설명은 하지는 않겠다. 혹시 궁금하면 찾아봐도 좋다. 

## 전파 (Propagation)

Coroutine 을 실행하다 예기치 못한 예외가 발생하면 예외는 상위로 전파되어 Scope 내의 모든 Parent 와 Children 을 Cancel 시킨다. 말이 어려울 수 있으니 아래 예제를 한번 보자.

```kotlin
suspend fun main(): Unit = coroutineScope {

    launch {
        launch {
            println("[1-1] Job!")
        }

        launch {
            throw Error("[1-2] Error!!!!")
        }

        launch {
            println("[1-3] Job!")
        }
    }

    launch {
        delay(1000)
        println("[2-1] Job!")
    }

}
```

위의 코드를 실행해보면 "[1-1] Job!" 만 실행되고 다른 것들은 전부 실행되지 않는 것을 확인할 수 있다. 즉, 예외가 전파되어서 다른 Job 들도 취소 된 것 이다. 
여기서 우리는 "[1-2]" 에서 발생한 Error 가 최상위 coroutineScope 까지 전파되어 모든 Job 을 취소 시켰음을 알 수 있다. 또한, 우리는 앞서 배웠듯이 
부모에서 예외가 발생하면 모든 자식도 취소되고, 자식에서 Error 가 발생하면 부모도 취소 됨을 알 수 있었다. 따라서 **Exception 이 전파 되는 방향은 양방향(bi-directional) 임을 확인**할 수 있다.

## SupervisorJob

만약에 코루틴 안에서 try..catch 를 사용하는 방법도 있을 것이다. 아예 예외를 잡아서 처리하여 바깥 코루틴이 무너지지 않도록 하는 것 이다. 
하지만 특정 Scope 안에서 자식 Scope 들안에서만 예외를 전파하게 만들고 싶을 수도 있다. **즉, 양방향이 아닌 단방향으로 (부모 -> 자식) 으로만 예외가 전파되게 하는 경우**이다. 
아래 코드에서 우리는 "**a scope" 안에서 예외가 발생할 경우 a scope 내의 모든 자식들을 취소하고 싶다고 해보자. 반대로, b scope 는 취소하기 싫은 것이다.** 이럴 경우 우리가 어떻게 해야할까? 
try..catch 로 a scope 를 감싸야 할까? 이는 통하지 않고 별로 좋은 방법또한 아니다. 이렇게 **Exception 의 전파를 부모 -> 자식 단방향으로 만들고 싶을 때 SupervisorJob 을 이용**한다. 
아래 예시를 한번 보자.

```kotlin
// parent scope
suspend fun test(): Unit = coroutineScope {

    // a scope
    launch(SupervisorJob()) {
        launch {
            println("[1-1] Job!")
        }

        launch {
            throw Error("[1-2] Error!!!!")
        }

        launch {
            println("[1-3] Job!")
        }
    }

    // b scope
    launch {
        delay(1000)
        println("[2-1] Job!")
    }

    delay(2000)
}
```

위의 코드를 실행시킨 실행결과를 한번 보자. 

```shell
[1-1] Job!
Exception in thread "DefaultDispatcher-worker-3" java.lang.Error: Error!!!!
at chapter05.Example03Kt$test$2$1$2.invokeSuspend(Example03.kt:22)
at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:570)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:750)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:677)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:664)
Suppressed: kotlinx.coroutines.DiagnosticCoroutineContextException: [StandaloneCoroutine{Cancelling}@52c151fe, Dispatchers.Default]
[2-1] Job!
```

위의 결과를 살펴보면 참 특이한 점을 찾아볼 수 있는데, [1-2] 에서 예외가 발생했지만 b scope 의 [2-1] 은 정상적으로 수행되었음을 확인할 수 있다. 
또한, 우리가 원한대로 예외 발생이후 [1-3] 이 실행되지 않았음을 확인할 수 있다. 이를 그림으로 그려보면 아래와 같을 것 이다.

<img width="933" alt="image" src="https://user-images.githubusercontent.com/57784077/193401383-fae4fc09-0934-4754-a2f8-bb9a50122632.png">

위 그림처럼 상위로는 Exception 이 전파되지 않는 모습을 확인할 수 있다.

## ExceptionHandler

Spring 의 @GlobalExceptionHandler 와 비슷하게 다른 Framework 나 Library 에서도 Exception 이 발생할때 처리하는 Callback 함수를 Handler 로 지정하여 편리하게 이용할 수 있게 끔 기능을 제공하기도 한다.  
Coroutine 에서도 위와 비슷하게 편리하게 ExceptionHandler 를 만들어서 사용할 수 있도록 해준다. 

```kotlin
val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    println("Current Context : [${coroutineContext}]") // Context
    println("Exception cause : [${throwable.message}]") // Error!!
}

suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    handlerScope.launch {
        delay(100)
        throw Error("Error!!")
    }

    handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

}
```

만드는 방법은 위와 같이 상당히 쉬운데 CoroutineExceptionHandler 의 인자로 `(coroutineContext, throwable) -> {}` 을 넘겨주면 된다. 
또한 CoroutineContext 여서 '+' 연산을 통해서 다른 Context 와 합쳐 이용할 수 있으므로 이용하기도 편하다. 

위와 같이 ExceptionHandler 를 깔끔하게 이용할 수 있는 부분도 있지만 ExceptionHandler 를 사용할 수 없는 부분도 있다.

## launch 와 async 의 예외 일임처리

launch 는 기본적으로 예외를 자동으로 상위로 전파해주는 기능을 가지고 있다. 따라서 예외가 자동으로 전파되어 이에 적합한 ExceptionHandler 가 이를 처리할 수 있다. 
하지만 async 의 경우는 exception 을 같이 wrapping 하여 Client Side 에서 이를 처리하도록 한다. 이건 코드를 보면 더 쉬우니 코드를 한번 보도록 하자.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    val deffer = handlerScope.async {
        delay(100)
        throw Error("Error!!")
    }

    deffer.await()

    handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

}
```

위의 코드는 아까와 같지만 단순히 launch 가 async 로 바뀌었을 뿐이다. 이 코드를 실행시켜보면 Error!! 가 발생되고 아무것도 실행되지 않는다. 
즉, 에러가 발생하여 모든 코루틴이 취소되었음을 알 수 있다. 이를 어떻게 해결할 수 있을까? 일단 제일 간단한 방법은 try..catch 를 이용하는 방법일 것 이다.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    val deffer = handlerScope.async {
        delay(100)
        throw Error("Error!!")
    }

    try {
        deffer.await()
    } catch (e: Error) {
        println(e.message)
    }

    handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

}
```

위의 tryCatch 가 보기 싫다면 runCatching 을 사용해도 된다.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val handlerScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    val deffer = handlerScope.async {
        delay(100)
        throw Error("Error!!")
    }

    kotlin.runCatching {
        deffer.await()
    }.onSuccess { println("Success") }
        .onFailure { println("Failed Job..") }

    handlerScope.launch {
        delay(150)
        println("Print?") // Print
    }

    delay(1000)

}
```

혹은 async block 안에서 handling 을 해주는 방법도 있지만, 이건 취사 선택이라 


## 부록

### ServiceLoader 를 이용한 Exception Handler 등록

코루틴 공식문서에서 최상위 코루틴 ExceptionHandler 를 등록할때 JVM 환경 기준으로는 ServiceLoader 를 이용해서도 등록할 수 있다고도 한다. 다만 저렇게 하는게 어플리케이션 레벨에서 좋은 방법인지는 잘 모르겠다.

[오라클 공식문서](https://www.oracle.com/technical-resources/articles/javase/extensible.html)

### How to wait children?

우리가 앞서서 Job 의 LifeCycle 을 살펴보았었다. cancel() 되는 과정에서도 finalState 라는
하나의 상태값을 이용하게 되는데 cancel() 메소드가 발생했을때 가지는 상태는 아래와 같다.

- **COMPLETING_ALREADY -- 이미 completing 이거나 completed 일때**
- **COMPLETING_RETRY -- interference 로 인해 retry 가 필요할때**
- **COMPLETING_WAITING_CHILDREN -- 현재 job 은 completing 상태고 자식들을 기다리고 있을때**

즉, 이러한 상태값을 내부적으로 관리하기 때문에 자식들을 기다릴 수 있게 되는 것이다. 또한 Cancelled 상태에서 왜 completed 도 true 인지 이제 어느정도는 알 수 있을 것이라고 생각한다.