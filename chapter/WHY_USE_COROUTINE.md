# 왜 코루틴을 사용해야 할까?

Java 에는 이미 멀티스레딩을 잘 지원하는 JavaFX 나 Reactor 와 같은 표준화된 라이브러리가 존재한다. 
그럼에도 우리가 코루틴을 사용해야 하는 이유는 무엇일까? 코루틴은 이미 Lisp 와 같은 언어에서 예전에 구현된 적 있었으나,
별 다른 흥행을 얻지 못했었다. 그 이유는 실생활의 케이스에서 사용하기 적합하지 않았기 때문이라고 생각하는데, 코틀린 코루틴은 
이러한 단점을 보완하여 실생활에서 좀 더 사용하기 적합하게 만들어졌다.

또한 코루틴은 Multi-Platform 언어로 코루틴으로 작성하면 해당 Platform 에 맞는 형태로 코드가 변경된다. 
어떻게 보면 RxJava 나 Reactor 를 모르더라도 Coroutine 으로 코드를 작성하기만 하면 되는 것이다.

(아래 글 부터 특별히 언급하지 않고 코루틴이라고 한다면, 코틀린 코루틴이라고 인지하면 된다.) 

## Use-Case On Android

코틀린 코루틴 하면 가장 많이 나오는 예시는 안드로이드와 관련된 예시인데, 사실 서버 어플리케이션에 관한 글로 예시를 풀어볼까 했는데, 
제일 적절한게 안드로이드 인거 같아서 안드로이드 예시로 적어본다.

클라이언트 사이드에서는 View 를 Rendering 하는 과정에서 Main Thread 가 Blocking 되지 않는 것이 가장 중요하다.
만약 우리가 특정 페이지를 Scripting 해온 데이터를 통해서, view 에 보여줘야 한다고 해보자.
```kotlin
fun renderSomeView() {
    val data = getSomePageScripting() // -- API Call
    val processedData = preProcess(data) // -- Processing..
    view.show(processedData)
}
```

위와 같은 코드를 짜게 될 경우 API Call 을 하는 부분과 Processing 을 하는 부분에서 Thread 가 오랜시간 Blocking 될 수도 있으며, 
이는 사용자에게 별로 좋지 않은 경험을 선사할 것이다. 이런 부분을 어떻게 풀어낼 수 있을까? 일단, Multi-Threading 을 통한 방법으로 해소해 볼 수 있을 것이다.
Thread Context Switching 을 통해 이를 해소해보자.

```kotlin
fun renderSomeView() {
    thread {
        val data = getSomePageScripting() // -- API Call
        val processedData = preProcess(data) // -- Processing..
        view.show(processedData)
    }
}
```

위와 같은 방식은 아래와 같은 몇가지 문제를 야기할 수 있다.
- **Multi-Thread 프로그래밍을 할때 항상 다른 Thread 를 Interrupt 할 수 있는 수단이 존재해야 하는데 존재하지 않아 Memory Leak 을 유발할 수 있다.**
- **잦은 Context Switching 은 관리하는 것이 어렵다.**

Deep Dive Book 에서는 View 를 빠르게 닫고 열고를 반복할때 정상적으로 Cancelling 되지 않아, Memory Leak 을 유발할 수 있다고 하는데 이는 실제로 
전에 있던 회사에서 타사 라이브러리를 이용할때 우리 안드로이드 팀이 겪었었던 문제로 기억한다. 여하튼 자원 정리 그리고 Interrupt 등을 잘 이해하고 사용해야 하는데 
이는 상당히 어렵고 실수하게 될 경우 위와 같은 문제를 야기하기 쉽다.

그렇다면 다르게 문제를 풀 수 있는 방법은 어떤 것이 존재할까? 바로 우리에게 익숙한 **"Callback Pattern"** 이다.

## Callback

Callback 의 Concept 을 이해하기 위해서는 Blocking 과 NonBlocking 에 대한 이해가 필요하니, 모른다면 글을 따로 읽어보고 오길 바란다.
(개인적으로 정말 Blocking 과 NonBlocking 을 이해하려면 유튜브에 코드스피츠 Promise 를 검색해서 영상을 보면 좋다.)
일단 컨셉 자체는 우리가 **해당 Process 가 종료됬을때 실행하길 원하는 Function 을 넘겨준뒤, 그 Process 가 종료 되면 우리가 넘긴 Function 을 실행하게끔 하는 것**이다.
말이 어렵다고 느끼는 사람도 있으니 요건 코드로 설명하겠다. Callback 은 코루틴을 이해하는데 엄청 중요하므로 반드시 이해해야 한다.

```kotlin
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
```

위와 같이 process 내에서 도는 함수가 다 끝난뒤 넘겨 받은 **callback 을 invoke()** 하게 된다. Callback Architecture 를 이용한다면 우리는 Callback Function 에 Cancellation 에 해당하는 과정들을 같이 넘겨줘야 할 것이다.
Callback Solution 으로 문제를 간단하게 푼것 같지만 Callback 을 이용할때 하나의 로직이 수행되면 다음로직이 수행되고.. 실패하면 복구되고.. 와 같은 일련의 유사 트랜잭션 작업을 진행하게 되면 Depth 가 엄청나게 깊어지는 것을 확인할 수 있다. 
웃긴 표현으로 이걸 **"아도겐 코드"** 라고도 한다.. ㅋㅋㅋ. 

```kotlin
fun doSome() {
    f1(context) {
        f2 (context) {
            f3 (context) {
                doSomething()
            }
        }
    }
}
```

## Reactive Streams

Java 진영에서 Reactor 는 이를 **Reactive Streams** 로 좀 더 좋은 방향으로 풀어냈다. Stream 을 이용하여 
Data 를 Processing 하고, Pub-Sub 하고, Cancellation 까지 Streams 안에 구현하도록 했다.

```kotlin
fun renderSomeView() {
    getSomePageScripting()
        .subScribeOn(Scheduler.io())
        .map (preprocess(it))
        .subscribe { view.show(it) } 
        .doOnError { e -> println(e.message) }
}
```

위와 같이 Callback Architecture 처럼 깊어지지 않고, 하나의 Streams 안에서 해야할 일을 명시하기에 더 깔끔하고 좋아보인다. 
하지만 Reactor 를 사용하기 위한 러닝커브나 기존의 동기적으로 돌아가던 코드를 비동기로 바꾸기 위해 Reactor 가 명시하는 Form 에 맞춰야 하는 등의 작업들이 필요할 것이다.
사실 Reactor 를 모르고 딱 보기에 이게 뭐 비동기인지 뭔지 알 수 없다. Reactor 를 알고 나서야 어떻게 Multi-Thread 프로그래밍을 하는지 알 수 있을 것 이다.
여러개의 Api 를 호출하면 zip 과 같은 메소드 이용등으로 더 복잡해진다. Reactive Programming 의 문제는 항상 코드의 Flow 를 이해하는 것이 어렵다.

## Kotlin Coroutines

코틀린의 코루틴은 위와 같은 문제를 어떻게 해결했을까? 코틀린에서는 Suspend 와 Resume 을 이용하여 이 문제를 해결했다.
여기서 부터 진짜 중요하다. 이 개념을 이해하지 못하면 코루틴을 이해할 수 없다. 개념적으로 먼져 살펴보면, **suspend point (중단 지점)** 에서 코루틴은 중단 될 수 있고, 
다시 해당 **suspend point** 에서 재개(resume) 할 수 있다. 이 메커니즘이 가장 중요하며, 이를 **suspend mechanism** 이라고 부른다. 이해가 가지 않는다면, 대략적으로 우리가 
게임을 하다가 게임을 일시정지하고, 밥을 먹고오고 게임을 다시 Resume 을 눌러서 재개하는 것을 생각하면 된다. 이때 생각해보면 우리는 한쉬도 쉬지 않고, 게임도 하고 밥도먹고 다시 게임을 한다. 
따라서 suspend 와 resume 을 쓰는 메커니즘에서 스레드는 Blocking 되는 것이 아니라, 다른 일을 할 수 있게 되는 것이다.

말이 어려우니 위의 예시를 다시 가져와서 Coroutine 으로 설명해보자.

```kotlin

suspend fun getPageScriptApi(): Data {
    return data
}

suspend fun getSomePageScripting(): Data {
    val data = getPageScriptApi()
    return data
}

suspend fun preProcess(data: Data): Data {
    // some process
    return data
}

suspend fun renderSomeView() {
    val data = getSomePageScripting() //
    val processedData = preProcess(data) // -- Processing..
    view.show(processedData)
}

suspend fun renderSomeView2() {
    val data = getLocalData()
    view.show(data)
}

suspend fun main() {
    launch { renderSomeView() }
    launch { renderSomeView2() }
}
```

위와 같은 코드가 있을때 Main Thread 의 움직임을 확인해보자.

<img width="1940" alt="image" src="https://user-images.githubusercontent.com/57784077/192140587-d9f199df-4827-4614-9a2c-0791cd60fbcb.png">

첫번째로 Main Thread 는 가장 위의 `renderSomeView()` 를 실행 시킨다. 그 다음 `renderSomeView()` 의 내부에서 `getSomePageScripting()` 이라는 suspend 함수를 실행시키는데, 
이 함수는 api 를 통한 network call 을 하는 함수로 응답을 기다리게 되면 Thread 를 Block 되는 함수이다. 여기서 코루틴은 요청을 보내고 해당 지점에서 **suspend** 하게 된다. suspend 하게 되면 
위에서 설명한대로 Main Thread 가 Block 되는 것이 아닌 다른 일을 할 수 있게 되는 것이므로, Main Thread 는 곧장 같은 코루틴 컨텍스트를 공유하는 `renderSomeView2()` 를 실행시킨다. renderSomeView 에서는 
별도의 **suspension point (중단지점) 이 존재하지 않으므로 끝까지 실행**시키게 된다. 그런 다음 아까 suspend 를 했던 지점인 `getPageScriptApi()` 로 돌아와서 실행을 재개한다.

위의 코드를 보면 어떤 느낌이 드는가? 아까 Callback Pattern 이나 Reactive Streams 보다 훨씬 더 간결하고, 목적이 명확한 코드로 보인다. Coroutine 이 해결하고자 하는 문제가 보이는가? 
좀 더 간편하고 General 한 느낌의 코드를 작성하여 Multi-Thread Programming 을 도와준다.

## How to work?

위의 내용에서 어렵지만 왜 코루틴을 이용하는 지를 대략적으로 이해했을 것 이다. 하지만, 내부 동작은 이해하기 힘들었을 것 이다. 이제 그 개념을 이해해 보자.
우리가 앞전에서 예시로 말한 "비디오 게임" 예시를 다시 가져와보자. 예제에서 나의 움직임을 순서대로 나열해보자.

1. **비디오 게임을 하다가 Chapter01 에서 일시중단을 누른다.**
2. **밥을 먹으러 간다.**
3. **일시 중단한 지점에서 Resume 을 눌러 게임을 재개한다.**

위와 같이 나는 한틈도 쉬지 않고, 게임도 하고 밥도 먹고 있다. 근데 이를 코드로 나타내면 어떻게 나타내야 할까?
일단 우리가 위에서 배운 **Callback Pattern** 으로 아래와 같이 코드를 작성해 볼 수도 있을 것 이다.

```kotlin
suspend fun playVideoGame(videoGame: VideoGame) {
    println("play the game..")
    videoGame.nextChapter()
}

suspend fun stopVideoGame(videoGame: VideoGame) {
    println("Stop This Game [Current Chapter : ${videoGame.checkPoint}]")
}

suspend fun eatMeals(videoGame: VideoGame, resumeVideoGame: ((v: VideoGame) -> Unit)) {
    println("done eating Meals")
    resumeVideoGame.invoke(videoGame)
}

suspend fun main() {
    val lastOfEarth = VideoGame(0)
    val resumeFunc = { videoGame: VideoGame -> println("resume Chapter${videoGame.checkPoint}...") }

    playVideoGame(lastOfEarth)
    stopVideoGame(lastOfEarth)
    eatMeals(lastOfEarth, resumeFunc)
}
```

위의 예시를 보면 한가지 중요한 사실을 알 수 있는데, 재개(Resume) 할때는 내가 멈췄던 CheckPoint 정보가 필요하다는 것 이다. 즉, Suspend 되는 당시의 정보를 알아야 할 필요가 생긴다. 
코루틴에서는 이와 같이 **suspend 되는 순간의 정보를 Continuation** 이라고 부른다. 우리는 VideoGame 을 마치 Continuation 처럼 이용한 것이다. 이를 MultiThread 로 생각해보면, Thread 가 
Switching 될때 Context 를 주고 받는데 이 Context 를 Continuation 에 저장하고 있다고 생각하면 된다.

하지만 우리가 원하는 코루틴 방식은 끝날때 Callback 을 호출하는 것이 아닌, **Coroutine 이 Thread 를 얻을때 재개(Resume) 하는 것** 이다. 
개인적으로 이 개념이 상당히 중요하다고 생각한다. 잘 이해가 가지 않는다면 아래 코드를 보면 이해가 더 빠를 것 이다.
**아래 예시는 대부분 Kotlin Coroutine DeepDive 의 예시와 유사하다. (KotlinKt Site 에서도 확인 가능하다.)**

```kotlin
suspend fun main() {
    println("Start")
    println("End")
}
```

위 코드를 실행시켜보면 "Start" 와 "End" 가 잘 출력되는 것을 확인할 수 있다. **자 그렇다면 저 사이에서 Coroutine 을 suspend 시킨다면 어떻게 될까?** 
한번 시켜보자.

```kotlin
suspend fun main() {
    println("Start")
    suspendCoroutine<Unit> {  }
    println("End")
}
```

위 코드를 실행시키면 Start 만 찍히고, 영원히 main() Function 이 끝나지 않는 것을 확인할 수 있다. 이유는 무엇일까? 바로 우리가 재개(Resume) 해주지 않았기 때문이다.
우리는 예시를 위해 `suspendCoroutine` 을 이용하였는데, `suspendCoroutine` 은 현재 Coroutine 을 얻고, Suspend 시킨다. 
이 Argument 로 전달되는 lambda function 은 suspend 되기 전 실행되며 continuation 을 인자로 받는다. 그렇다면 이 lambda 의 특정 동작을 추가해보자.

```kotlin
suspend fun main() {
    println("Start")
    suspendCoroutine<Unit> { continuation ->  println("Before Suspend [$continuation]") }
    println("End")
}

Start
Before Suspend [SafeContinuation for Continuation at chapter01.SuspendAndResumeKt.main(SuspendAndResume.kt:7)]
```

출력 이 Start 와 Before Suspend 가 되는 것을 확인할 수 있다. 이렇게 contination 을 이용하는 lambda 는 continuation 에 객체를 저장하거나 할 수 있다. 
위와 같이 suspend 된 coroutine 을 재개하기 위해서는 resume function 을 이용해야 한다.

그럼 위의 continuation 에 videoGame 을 넣고 위의 Callback 처럼 되는지 확인해보도록 하자.

```kotlin
val context = Dispatchers.Default + VideoGame(0)


suspend fun main() = withContext(context) {
    playTheGame(this.coroutineContext)
    stopTheGam(this.coroutineContext)
    suspendCoroutine<Unit> { continuation ->
        eatMeals(continuation.context)
        continuation.resume(resumeGame(continuation.context))
    }
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
```

위와 같이 코드를 작성하게 되면 CoroutineContext 에 VideoGame 을 넣고 이용할 수 있다. 이렇게 되면 Thread 가 바뀌어도, Continuation 에 있는 
VideoGame 을 이용하므로 중단된 지점의 checkpoint 가 잘 나올 것 이다. 근데 만약 VideoGame 을 Lamda 안에서 Return 하고 싶다면 어떻게 코드를 작성해야 할까? 
아주 쉽게 Unit 을 VideoGame 으로만 교체해주면 된다.

```kotlin
suspend fun main() = withContext(context) {
    playTheGame(this.coroutineContext)
    stopTheGam(this.coroutineContext)
    val currentVideoGame = suspendCoroutine<VideoGame> { continuation ->
        eatMeals(continuation.context)
        continuation.resume(continuation.context[VideoGame]!!)
    }

    resumeGame(currentVideoGame)
}
```

위와 같이 `resume` 은 coroutineScope 안에서 재개한 뒤 특정 Value 를 리턴할 수 있음을 확인할 수 있다. `resumeWith` 나 `resumeWithException` 또한 있으니 
한번씩 사용해봐도 좋다. 보통은 `resumeWith` 를 많이 사용하는 것 같다. resumeWithException 은 단순히 resumeWith 를 이용하여 확장한 함수이다. 
여기까지 잘 이해됬다면 우리가 network 를 support 해주는 Library 를 어떻게 Coroutine 을 이용하도록 바꿀지 간단하게 생각해볼 수도 있다.
**Network Request 를 보내고 -> Suspend 하고 -> Network Support Library 가 돌고 있고, 완료되면 -> 어떤 Thread 가 와서 Resume 에 Data 를 담아서 호출** 한다.
아래 Feign Client 에 Kotlin Coroutine 을 지원하도록 코드를 작성한것이 있는데 요걸 대략적으로 읽어봐도 좋다.

[Feign Clients Support Coroutines](https://github.com/PlaytikaOSS/feign-reactive/pull/486/files)

## 끝내며

여기까지 잘 이해했다면 아래 함수가 어떻게 실행할지 예측해보자.

```kotlin
var cont: Continuation<Unit>? = null

suspend fun test() {
    suspendCoroutine<Unit> { continuation ->
        cont = continuation
    }
}

suspend fun main() {
    println("Start")
    test()
    cont?.resume(Unit)
    println("End")
}
```

정답을 모르겠다면 위의 글을 잘 이해하지 못한 것일 확률이 높다.

<details>
    <summary>해설</summary>

해당 함수는 "Start" 만 보이고 영원히 끝나지 않는다. 그 이유는 위에서 설명했듯 `suspendCoroutine` 은 해당 Coroutine 의 Continuation 정보를 사용할 수 있고, 
해당 Coroutine 을 Suspend 시키기 때문이다. 그래서 해당 Scope 내에서 `resume()` 을 실행시키거나, 다른 스레드에서 `resume()` 이 호출되어야 한다. 
따라서 실행시키는 방법은 많겠지만 아래와 같은 방법으로 재개가 가능하다.

### 1번 방법
```kotlin
var cont: Continuation<Unit>? = null

suspend fun test() {
    suspendCoroutine<Unit> { continuation ->
        cont = continuation
        continuation.resume(Unit)
    }
}

suspend fun main() {
    println("Start")
    test()
    println("End")
}
```

### 2번 방법 (다른 스레드 이용)

```kotlin
var cont: Continuation<Unit>? = null

suspend fun test() {
    suspendCoroutine<Unit> { continuation ->
        cont = continuation
    }
}

suspend fun main() = coroutineScope {
    println("Start")

    launch {
        delay(500)
        cont?.resume(Unit)
    }

    test()
    println("End")
}
```
</details>

오늘 Series 에서는 Coroutine 의 핵심이 되는 동작원리에 대해서 간략하게 알아보았다. 다음에는 좀 더 심층적으로 코드가 어떻게 바뀌는지 한번 보도록 하자











