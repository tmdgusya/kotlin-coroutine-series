# Coroutine Scope

코루틴에서 영역(Scope) 는 코루틴 컨텍스트와 코루틴의 생명주기(LifeCycle) 을 관리하기 위해 사용된다. 
따라서 코루틴은, 앞서 배웠던 **구조화된 동시성(Structured Concurrency)** 를 위해서, 자신만의 `Job` 을 보유하게 된다.
GlobalScope 를 왠만해서 사용하지 말라고 하는 이유 또한, 코루틴의 시작과 종료를 관리하기 어려운 상태로 빠져들 수 있으므로 `Memory Leak` 현상을 초래할 수 있게 된다. 

## coroutineScope 함수

`coroutineScope(block)` 함수는 새롭게 코루틴을 만드는 역할을 한다. coroutineScope 의 특이한 특성은 **새로운 코루틴이 끝나기전까지, 
이전 코루틴을 일시중단**시키게 된다. 예를 들기 위한 아래 코드를 함께 보자.

```kotlin
suspend fun main() = withContext(CoroutineName("Chpater06")) {

    val a = coroutineScope {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
    
    println("A is Done!!")

    val b = coroutineScope {
        delay(1000)
        println(coroutineContext[CoroutineName])
        20
    }

    println(a) // 1초 지연
    println(b) // 1초 지연
}
```

위 코드의 실행시간이 얼마나 걸릴지 예측 가능한가? 결과적으로, 위의 코드는 총 **1초 + 1초 = 2초** 가 걸리게 된다. 아래 출력값을 한번 보도록 하자.

```shell
CoroutineName(Chpater06)
A is Done!!
CoroutineName(Chpater06)
10
20
```

출력값을 보게 되면 정확히 `a` 를 구성하는 코루틴이 종료된 후, "A is Done!!" 이 출력되는 것을 보아 **코드가 순차적(Sequential)으로 실행되는 것**을 알 수 있다. 
또한, **코루틴 이름(CoroutineName)의 출력값을 통해서 부모의 코루틴 컨텍스트(Coroutine Context) 를 상속받는 것**을 알 수 있다. 이렇게 순차적으로 실행되는 이유는, 
위에서 설명한 **"새로운 코루틴이 끝나기전까지, 이전 코루틴을 일시중단 시키게 된다"** 라는 특성 덕분이다. `runBlocking` 과 반대로 `coroutineScope` 는 Thread 를 Blocking 하지 않는다. 
따라서 coroutineScope 챕터까지 잘 왔다면 **코루틴에서 runBlocking 을 사용할 시기는 많지 않다는 것을 깨달을 수 있을 것** 이다.

## withContext

`withContext` 또한 coroutineScope 와 동작하는 방식은 같으나, **추가적으로 Context 를 함수의 인자(Argument) 로 전달**할 수 있도록 해준다. 
아래 코드를 보면 좀 더 명확하게 알 수 있을 것 이다.

```kotlin
suspend fun main() = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    val child1 = withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }

    val child2 = withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }

}
```

위 코드를 실행하게 되면 아래와 같은 결과가 출력된다.

```shell
Parent Coroutine Name : null
CoroutineName(Child)
CoroutineName(Child)
```

결과를 보면 알 수 있듯이, `withContext` 를 통해서 코루틴을 새롭게 만들때, 인자로 받은 `CoroutineContext` 를 결합하여 사용하는 것을 알 수 있다. 

## supervisorScope

`supervisorScope` 도 또한 coroutineScope 함수와 거의 같은 동작방식인데, 앞서 Cancellation 부분에서 봤듯이 **"예외를 단방향으로 규정짓는 특성이 있다.(부모 -> 자식)"** 
이것이 가능한 이유는 supervisorScope 는 coroutineScope 를 만들때, **SupervisorJob 으로 context 의 Job 을 override** 하게 된다. 아래 코드를 한번 보자.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    supervisorScope {
        launch {
            throw Error("!!!!")
            10
        }
    }

    withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
}
```

해당 코드를 결과는 아래와 같다.

```kotlin
Parent Coroutine Name : null
Exception in thread "DefaultDispatcher-worker-1" java.lang.Error: !!!! // 나머지 에러 메세지는 생략..
CoroutineName(Child)
```

Exception 이 터져도 부모에게 전파되지 않는 것을 확인할 수 있다. 
여기서 좀 더 심화적인 이해를 돕기 위해 문제를 하나 풀어보도록 하자. 아래 코드는 어떻게 실행될까 예측해보자.

### supervisorScope 심화 예시

```kotlin
suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    supervisorScope {
        launch {
            throw Error("!!!!")
            10
        }

        launch {
            delay(500)
            println("Executed?")
        }
    }

    withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
}
```

우리가 앞서 본 결과로 예측해봤을때는 아래와 같이 실행될 것이다.

```kotlin
Parent Coroutine Name : null
error!
Executed?
CoroutineName("Child")
```

실제로도 위와 같이 실행된다. 그렇다면 아래 코드는 어떻게 실행될까?

```kotlin
suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    withContext(SupervisorJob()) {
        val job = coroutineContext[Job]

        println(job!!::class)

        launch {
            throw Error("!!!!")
            10
        }

        launch {
            delay(500)
            println("Executed?")
        }
    }

    withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
}
```

withContext 가 SupervisorJob() 을 인자로 받고 있으니 supervisorScope 를 사용할때와 동일한 결과가 도출될까? 
결론부터 말하자면 아래와 같은 결과가 나온다.

```shell
Parent Coroutine Name : null
class kotlinx.coroutines.UndispatchedCoroutine (Kotlin reflection is not available)
Exception in thread "main" java.lang.Error: !!!!
```

결과를 보면 자식에서 예외가 전파되어 부모까지 올라간것을 확인할 수 있다. 왜 이런 결과가 나올까?  
일단 supervisorScope 는 위에서도 말했듯이, **launch 의 context 를 SupervisorCoroutine 으로 결합(combine) 시킴**을 알 수 있다. 코드 상으로는 아래와 같이 되어 있다.

<img width="1447" alt="image" src="https://user-images.githubusercontent.com/57784077/194692001-c94bef9c-723c-4be9-9d37-2e06d93a5533.png">

withContext 의 경우 아래와 같이 launch 의 context 를 구성할때 결합되는 context 가 SupervisorCoroutine 이 아님을 알 수 있다.

<img width="1775" alt="image" src="https://user-images.githubusercontent.com/57784077/194692064-0bd858e0-0eb0-4712-9f97-42793988a138.png">

직접적인 실험을 위해 아래와 같은 예시로 한번더 보여주겠다.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val coroutineName = CoroutineName("Child")

    println("Parent Coroutine Name : ${coroutineContext[CoroutineName]}")

    withContext(SupervisorJob()) {
        launch(SupervisorJob()) {
            throw Error("!!!!")
            10
        }

        launch {
            delay(500)
            println("Executed?")
        }
    }

    withContext(coroutineName) {
        delay(1000)
        println(coroutineContext[CoroutineName])
        10
    }
}
```

이 코드가 어떻게 실행될지 모르겠다면 앞 챕터를 다시 공부하고 와야 한다. 일단, 위 코드를 실행시켜서 결합된 컨텍스트(combined context) 가 어떻게 출력되는지 보자. 

<img width="1779" alt="image" src="https://user-images.githubusercontent.com/57784077/194692142-c00217d8-239a-4ff0-baf0-d2b0ea74753d.png">

SupervisorCoroutine 은 아니지만 SupervisorJobImpl 이 보임을 확인할 수 있다. 해당 코드는 위의 supervisorScope 결과와 동일하게 출력된다. 
그이유는 아래를 보면 좀 더 정확하게 알 수 있을 것 이다.

<img width="784" alt="image" src="https://user-images.githubusercontent.com/57784077/194692213-5c342aa0-08fa-4270-9d45-aa9d5373f63c.png">

둘다 **childCancelled 를 false 로 override** 하고 있다.

## 요약

코루틴 스코프(coroutineScope) 함수들을 사용하여 기존에 사용하던 `runBlocking` 을 일부 대치하는 방법과, 몇가지 코루틴 스코프 함수들에 대해 알아보았다.  
이제 대략적으로 어떤 상황에서 이 챕터에서 배운 코루틴 스코프 함수들을 써야 하는지 알게 되었을 것 이다. 