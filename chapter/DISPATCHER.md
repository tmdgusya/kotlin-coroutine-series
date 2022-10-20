# Dispatcher

Dispatcher 는 코루틴에서 중요한 기능적 역할을 한다. 코루틴의 Task 가 어떤 Thread 에서 실행할지 결정하는 역할을 해준다. 
앞서 봤듯이, 코틀린에서 우리가 코루틴을 이용하여 코드를 작성하면, 아래와 같이 분기가 쳐지는 것을 확인할 수 있다.

```kotlin
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
    } // Thread Free And Local Data Clear

    if (continuation.label == 1) {
        println(continuation.thisLocalVariable)
        println(continuation.thisLocalVariable2)
        println("End")
        return Unit
    }

    error("정상적인 종료가 아님")
}
```

우리는 앞서 배웠듯이 저 분기처리되어 있는 코드 블럭(Block) 을 실행시키는 스레드는 바뀔 수도 있다는 것을 배웠다.  
예를 들면, `label == 0` 일때 Thread-A 가 해당 블럭을 실행시켰다면, `label == 1` 일때는 Thread-B 가 해당 블럭을 실행시킬 수 있다는 것 이다.  
이렇게 **어떤 스레드가 어떤 블럭(Task) 를 실행시킬지 결정하는 기능적역할을 하는 것이 Dispatcher** 이다.

이제 간략하게 어떤 기능을 하는지 알아봤으니 Dispatcher 종류를 알아보도록 하자.

## Default Dispatchers

기본적으로 아무 Dispatcher 도 설정하지 않았을 경우 **Default Dispatcher** 를 사용하게 된다. 
설명상으로 JVM 스레드를 공유하며, 동시에 작업할 수 있는 양은 **최소 2개 이상에서 최대 CPU Core 의 숫자 만큼 가능**하다고 한다.
실제로 아래 코드에서 Worker 를 생성하고 있다.

특징으로는 CPU 집약적인 작업을 할때 좋다고 한다.

<img width="888" alt="image" src="https://user-images.githubusercontent.com/57784077/196985518-c40208a4-8ac8-4471-9cc4-ae75a38a5c00.png">

코드를 봤을때 **기본적으로는 스레드 풀은 비어있고, On-demand 로 만들고 나중에 정리되는 방식**처럼 보였다.

그럼 이제 우리가 코루틴을 실제로 아무 Dispatchers 도 지정하지 않고, 코루틴을 실행시켜 보자.

```kotlin
suspend fun main() = coroutineScope {
    repeat(1000) {
        launch {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}
```

실행해보면 Default Dispatcher 에 걸렸음을 알 수 있다.

<img width="1009" alt="image" src="https://user-images.githubusercontent.com/57784077/196985814-898f418a-c61e-4d54-a82f-e4da3589301a.png">

만약 아래 코드를 실행시켜도, DefaultDispatcher 가 나올까?

```kotlin
fun main() = runBlocking {
    repeat(1000) {
        launch {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}
```

실제로 실행시켜보면 전부 `main` 스레드를 이용하고 있음을 알 수 있다. 이유는 `runBlocking` 은 스레드를 블락시키기 때문이다. 

## IO Dispatchers

IO Dispatchers 는 I/O Operation 에서 스레드가 블락되는 경우에 사용되도록 설계되었다. 
스레드풀은 시스템 속성인 `kotlinx.coroutines.io.parallelism` 수만큼 제한 되도록 되어있고, 기본적으로는 64개 스레드 또는 
코어수 중 더 큰 값을 선택해 제한됩니다.

<img width="742" alt="image" src="https://user-images.githubusercontent.com/57784077/196992295-44ba3c04-cab2-4643-b44d-b821a9b09f26.png">

아래 코드를 한번 실행시켜 태스트 해보자.

```kotlin
suspend fun main() = withContext(Dispatchers.IO) {
    repeat(1000) {
        launch {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}
```

실행시켜보면 DefaultIOScheduler 에서 Dispatch 되고 있는 것을 확인할 수 있다.

<img width="944" alt="image" src="https://user-images.githubusercontent.com/57784077/196991355-c9e18cb1-cc79-478d-b6bf-a319eab21e52.png">

위의 코드를 실행시켰을때 스레드 이름을 한번 보도록하자. 

`DefaultDispatcher-worker-32`

스레드 이름이 어디서 많이 본것 같지 않은가? 맞다 바로 **Default Dispatcher** 와 같다. 실제로 코드 상에서도, 거의 같다.

<img width="810" alt="image" src="https://user-images.githubusercontent.com/57784077/196992752-12fa6deb-4d87-42d2-b760-96cc1abe753f.png">

실제로 공식문서에서도 **Default Dispatcher 와 IO Dispatcher 는 스레드풀을 공유**한다고 가이드 하고 있다. 
따라서 **다시 Dispatch 되지도 않아도 되고, 스레드의 재사용률도 높아진다.** 이와 같은 이유로 인해 스레드풀을 공유하여 성능적이점을 얻었다고 한다.

### elasticity

탄력성 이라고 해석해야하나? 애매하여 일단 원문으로 적겠다. Dispatcher IO 에는 특별한 특성인 `elasticity` 이 존재한다.
우리가 생각했을때 보통 I/O 콜은 Blocking Time 이 긴 경우가 많다. 따라서 스레드 풀 안의 대부분의 스레드들이 블락되는 상황이 연출될 수 있다.
예를 들어서 우리가 **1초 걸리는 Task `100` 개를 동시에 실행**시켜야 된다고 해보자. 
그렇다면 아까 Dispatcher IO Spec 으로는 스레드를 64개 밖에 못 생성하므로 1초 이상 ~ 2초가 소모될 것이다. 
그런데 만약 스레드 100 개를 만들 수 있다면 어떨까? 1초가 걸려서 모두 끝낼 수 있을 것 이다. 

아래 코드를 한번 보자.

```kotlin
suspend fun main(): Unit = coroutineScope {
    launch {
        printCoroutineTime(Dispatchers.IO)
    }
}

suspend fun printCoroutineTime(dispatcher: CoroutineDispatcher) {

    val test = measureNanoTime {
        coroutineScope {
            repeat(100) {
                launch(dispatcher) {
                    Thread.sleep(1000) // 1 초가 걸리는 Task
                }
            }
        }
    }

    println("$dispatcher took: $test")

}
```

이 코드를 실행시켜보면 결과값이 `Dispatchers.IO took: 2.019316209s` 가 나온다. 
즉, 우리가 예상한대로 2초가 걸리는 것을 확인할 수 있다. 
여기서 Dispatcher IO 의 특별한 기능을 통해 Thread 를 100개로 늘려보자.

Dispatcher IO 는 `limitedParallelism` 함수를 통해서 Thread Pool 안의 Thread 개수를 제한할 수 있는데, 
**다른 Dispatcher 와 다르게 제한(Maximum) 을 높일 수도 있다. 즉, 기본적으로 64개 제한이지만 `limitedParallelism(100)` 이 될 경우 
100개 까지도 생성 가능하게 된 다는 것**이다. 한번 실제로 가능한지 코드로 확인해보자.

```kotlin
suspend fun main(): Unit = coroutineScope {
    launch {
        printCoroutineTime(Dispatchers.IO.limitedParallelism(100))
    }
}

@OptIn(ExperimentalTime::class)
suspend fun printCoroutineTime(dispatcher: CoroutineDispatcher) {

    val test = measureTime {
        coroutineScope {
            repeat(100) {
                launch(dispatcher) {
                    Thread.sleep(1000) // 1 초가 걸리는 Task
                }
            }
        }
    }

    println("$dispatcher took: $test")

}
```

위 코드를 실행시켜보면 `LimitedDispatcher@4b8d8646 took: 1.038252709s` 와 같은 결과가 나오는 것을 확인할 수 있다. 
Dispatcher 이름이 LimitedDispatcher 로 바뀌고, 스레드 또한 100개 정도가 생겨서 1초 걸려서 작업이 완료된것을 확인할 수 있다. 
IO Dispatcher 의 바로 고유한 특성(elasticity)이 이것이다. 

공식 가이드에서 적합하게 사용하기 좋은 부분은, 스레드가 오래 점유될 수 있는 작업일 경우 스레드 풀의 스레드의 개수가 모자라게 될 수 있으므로 
이를 적합한 스레드 개수에 맞게 설정하면 좋다고 한다. 적합한 수준은 성능테스트를 통해 찾아야 할 것이다.



여기까지 배웠으니 복습할겸 하나 테스트 해보자. 아래 코드는 과연 어떻게 실행될까?

```kotlin
fun main() = runBlocking {
    repeat(1000) {
        launch(Dispatchers.IO) {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}
```

실행시켜보면 `DefaultDispatcher-worker-30` 가 나오는 것을 확인할 수 있다. 즉, Dispatcher 를 재정의 해 줌으로써 
상위 Context 를 Override 했음을 알 수 있다. 대부분 `runBlocking` 을 쓰는 상황은 좋지 않으나, 저런 방식으로 내부에서 병렬 실행을 할 수 있다는 것도 알아두면 좋다.

## Unconfined Dispatcher

Unconfined Dispatcher 는 이전 Dispatcher 들과는 다르게 스레드의 어떠한 것도 바꾸지 않는다. 
기본적으로 Caller Thread (호출한 스레드) 에서 수행하게 된다. 
쉽게 말해, Main Thread 에서, `launch(Unconfined) {}` 를 실행하게 되면 호출한 스레드인 `main` 스레드에서 Task 를 실행하게 되는 것이다.

```kotlin
fun main() = runBlocking {
    repeat(1000) {
        launch(Dispatchers.Unconfined) {
            List(1000) { Random.nextLong() }.maxOrNull()

            val threadName = Thread.currentThread().name
            println(threadName)
        }
    }
}
```

위의 코드를 실행시켜보면 전부 `main` 이 찍힘을 확인할 수 있다. Unconfined 는 Caller Frame 에서 바로 실행되기 때문에 
상대적으로 저렴할 수 있다. 하지만 위와 같이 runBlocking 에서 사용하면 Main Thead 를 Blocking 시킬 수도 있다. 
따라서, 정확히 통제하기 힘들어서 기본적으로 `runBlocking` 과 마찬가지로 사용을 지양하는게 좋다. 

## 정리

Continuation Interceptor 는 같이 정리하려고 했으나, 좀 더 내가 잘 설명하기 위해서는 공부를 더 해야할 것 같아서 하지 않았다. 
Dispatcher 부분은 가볍게 봐두고, 나중에 필요할때 차근차근 보면서 공부하는게 더 좋은 것 같다는 생각이 든다.
