# Continuation

Coroutine 에서 꼭 알아야 하는 부분 중 하나이지만 생소하지 않은 개념중 하나이다.
`Continuation` 이란 말 자체가 생소해서 정의를 설명하기 보단, Continuation 이란 개념이 왜 만들어지게 됬는지 설명 하면 자연스럽게 이해될 것이라고 생각된다.
이 챕터는 진짜 상당히 어려울 수 있다. 잘 이해하고 넘어가야 왜 코틀린 바이트 코드가 그렇게 구성되는지 이해할 수 있다.

## Call Stack 과 Suspend Function 에서의 문제

아래 코드를 한번 살펴보자.

```kotlin
fun a() {
    val a_temp = 1
    val a_temp_zz = "zz"
    b()
    return "aa"
}
```

위의 코드를 우리가 실행시킨다고 생각해보면, `a()` 함수를 실행시킨 뒤 Thread Stack 에 같이 Thread 만이 가지는 지역 Stack 에 a 함수가 가지고 있는
data 인 `a_temp`, `a_temp_zz` 를 저장한 뒤, `b()` function 을 `call stack` 에 올려야 할 것이다.  
(여기까지 과정이 잘 이해되지 않으면, CS 공부가 필요한 시점이니.. CS 를 공부해보길 바란다.)  
여하튼, 결국 **a -> b -> a** 로 돌아오기 위해서는 `a()` 의 데이터를 어디엔가 저장해야 되고, 그게 Thread Stack 이다.

여기서 문제는 우리가 `suspend function` 을 이용하기 때문이다. 앞서서 얘기했듯이 suspend 되는 순간 Thread 는 Block 되는게 아니라 
코루틴을 떠나게 된다. **즉, Thread Local 을 Clear 시켜야 한다는 것**이다. 위의 상황이 suspend function 이라고 가정해보면, a 의 local variable 정보가 clear 되는 것이다.
위와 같이 `a -> b -> a` 로 넘어 갈때 a 의 local 정보와 같이 연속적으로 물고 가야만 하는 data 들이 생기게 된다. Kotlin Team 은 suspend 를 사용하면서도 
이러한 문제점을 해결하기 위해 `Continuation(연속성)` 이라는 개념을 도입하였다.

여기까지 이야기를 들으면 아 **Continuation** 이 어떤역할을 대략적으로 하는지 생각이 들거나 예측될 것이다. 이제 진짜로 어떻게 돌아가는 녀석인줄 알아보자.

## Continuation

그럼 이제 어떻게 연속적인 Data 를 저장하고 있을지 **Continuation** 을 한번 파보도록 하자.

```kotlin
suspend fun originalFunction() {
    var thisLocalVariable: Int = 10
    var thisLocalVariable2: String = "Local Value"

    println("Start!!")
    delay(1000)
    println(thisLocalVariable)
    println(thisLocalVariable2)
    println("End")
}
```

위의 함수에 첫번째 스레드가 딱 들어왔을때 어떻게 동작할까? 
일단 **thisLocalVariable, thisLocalVariable2 를 Thread Stack 에 담고, SysOut "Start!!" 를 한 뒤  
일시중단 지점인 `delay()` 에서 `free` 가 되고, Thread Local 은 Clear 될 것** 이다. 그리고 이후에 어떤 쓰레드가 들어와서 delay 이후 부터 resume 할 것이다.  
따라서, 이 코드가 정상적으로 실행되려면 `resume` 을 하러 들어오는 thisLocalVariable, thisLocalVariable2 의 정보를 Continuation 으로 부터 받아와야 한다.  
**즉, Continuation 은 suspend function 의 State 를 담아두는 어떤 Structure 라고 생각하면 된다.** 일반적인 함수에서 위와 같은 정보를 담는 Structure 는 Call Stack 이다.   
한번 Java Byte Code 로 바뀌면 어떻게 되는지 보자. **실제적으로는 아래 코드 처럼 되지 않는다. 내가 커스텀한 코드이다!!**

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

대략적으로 위와 같은 코드 형태가 된다. 이건 좀 더 쉽게 알아보게 하기 위해 약간의 수정이 들어간 코드지만 동작 자체는 위와 같다고 생각해도 좋다.
우리가 적었던 함수에서 변경된 점을 확인해보면 아래와 같다. 

첫번째로, continuation 에서 **thisLocalVariable 와 thisLocalVariable2** 를 가져와서 수정하고, 출력하는 구조로 변경됬다. 
이렇게 변경 된 이유는 연속적인 데이터를 다른 Thread 에서도 이용하기 위함이다.  
두번째로, **continuation 이라는 Field 가 arguments 에 하나 생겼다.** 이는 연속적인 데이터를 계속해서 주입 받아 이용하기 위함으로 **CPS Pattern 을 이용한 것**이다.  
세번째로, **Return Type 이 Any 로 변했다.** 그 이유는 COROUTINE_SUSPEND 이라는 특정 값을 리턴하기 때문이다. 이는 현재 Coroutine 이 suspend(일시중단) 되었음을 알려주기 위함이다.

사실 위의 코드가 가능한 이유를 좀 더 잘알기 위해서는 아래 코드를 봐야 한다. 
아래 코드는 현재 **OriginalFunction 에서 가져야 하는 Data 들을 Continuation 으로 만든 것**이다.

```kotlin
class OriginalFunctionContinuation(
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
```

그래서 thisLocalVariable, thisLocalVariable2, label 를 가지고 있는 것을 확인할 수 있다. 
위 Data 들은 Thread 가 변경되어도 알고 있어야 하는 Data 이므로 Continuation 에 저장해야 하므로 위와 같이 Continuation 에 저장되고 있다.
또한 위의 `COROUTINE_SUSPEND` 가 들어오게 되면 그냥 함수를 나가버린다. 즉, Thread 를 Free 시키는 것이다. 
그리고 다시 이 함수를 호출시킬때는 현재 continuation 을 넣어주는 것(`originalFunction(continuation = this)`)을 볼 수 있다. 
위의 코드는 Coroutine 으로 Continuation 의 동작구조를 구현한 것이다.

## 효율

만약 2개의 Thread 로 두 부분을 나눠서 했으면 Local Stack 부분을 Copy 하는 Context Switching 이 일어났을 것 이다. 
Coroutine 에서는 Stack 을 Copy() 하지 않는다. 따라서 Context Switching 을 하는 것 보다 좀 더 비용이 저렴하다고 한다. 
그 이유가 LightWeight Thread 라고 불리는 이유가 아닐까 싶다.

**글을 잘 읽었다면 Star 를 부탁드립니다. 응원 하나하나가 큰 힘이 됩니다 :)**

## 읽으면 좋은 글 

[CPS Pattern](https://devroach.tistory.com/149)

