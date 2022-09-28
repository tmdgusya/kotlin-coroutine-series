# COROUTINE CONTEXT

**CoroutineContext 는 Coroutine 안에서 유지되도록 사용자가 별도로 정의한 문맥**이라고 생각하면 좋은데, 따라서 Continuation 과 유사하게 우리가 가져가야 하는 **문맥(Context)** 이다. 
Coroutine Context 는 코드적으로 Map 이라고 생각하면 매우 이해하기 편하다. 따라서, **CoroutineContext.Key 와 CoroutineContext.Element 의 조합으로 이루어져 있으며, 
Single Value 일수도 있고 여러개가 합쳐져 유사 Collection 과 같은 형태로 존재 할수도 있다.**  
이게 위처럼 말하면 사실 코드를 보지 않고는 무슨 소리인지 이해가 어려우므로 코드를 보고 다시 정의를 한번 보도록 하자

## CoroutineContext.Key And CoroutineContext.Element

CoroutineContext 는 위에서 **Coroutine 내에서 유지되야 하는 사용자 정의 문맥(Context)** 였다. 그렇다면 Key 와 Element 는 무엇일까? 

**일단 Element 는 Context 내에서 유지되어야 할 정보이고, 그 정보를 Unique 하게 식별하기 위해 Key 라는 개념이 존재한다.** 

즉, 위에서 **CoroutineContext 가 Single Value 일 수도 있다는 것이 Key 와 Element 를 조합하는 것만으로도 최소한의 Context 가 되기 때문**이다. 
쉽게 말해서 Element 자체가 Context 가 될 수 있다는 것이다. 이해하기 어려울 수 있으니, 아래 코드에서 직접 CustomElement 를 만들어보겠다.

```kotlin
class CustomCoroutineElement : AbstractCoroutineContextElement(CustomCoroutineElement) {
    var name: String = "Custom Coroutine Element #1"

    companion object Key: CoroutineContext.Key<CustomCoroutineElement>
}
```

아래 코드를 보면 엄청 쉬운데, 쉽게 말해 Key 로 `CustomCoroutineElement` 를 이용하고, `Element` 자체는 클래스가 된다. 
현재는 내가 name 이라는 정보만 저장하고 싶은데, 만약에 **특정 Coroutine 내에서 Error 개수를 저장하고 싶어라고 하면 아래와 같이 커스텀해도 된다.**

```kotlin
class CustomCoroutineElement : AbstractCoroutineContextElement(CustomCoroutineElement) {
    var name: String = "Custom Coroutine Element #1"
    var errorCount: Int = 0
    
    fun increaseErrorCount() {
        ++errorCount
    }

    companion object Key: CoroutineContext.Key<CustomCoroutineElement>
}
```

결국, 우리가 담고 싶은 정보를 Class 로 만든다고 생각하면 편하다. 그래서 사용자 정의 문맥이라고 표현하는 것이다. 여기서 Continuation 과 약간 다른 점을 한번 생각해보면 좋은데, Continuation 의 경우 
실제 Function 에서 Thread Stack 영역에 물고 있어야 하는 정보를 저장하고 있음을 우리가 확인할 수 있었다. 반면에 **CoroutineContext 는 사용자가 정의하는 Class 를 담고 있음을 확인할 수 있다.** 
그래서 Kotlin KEEP 문서에서는 아래와 같이 표현한다.

> Coroutine context is a persistent set of user-defined objects that can be attached to the coroutine.

이제 대략적으로 Key 와 Element 를 만드는 것에 대한건 이해가 됬을 것이다. 이제 한번 간단하게 위에서 만든 코드를 활용해보는 코드를 만들어보자.

```kotlin
suspend fun main() = withContext(CustomCoroutineElement()) {
    val myCustomCtx = coroutineContext[CustomCoroutineElement]
    println("Current Job Name : ${myCustomCtx?.name}")
    println("Current Job errorCount : ${myCustomCtx?.errorCount}")

    return@withContext Unit
}
```

`withContext` 라는 함수는 첫번째 인자로 CoroutineContext 를 받는다. 위에서 말했듯이 Element 는 최소한의 CoroutineContext 이므로, 
우리가 만든 CustomCoroutineElement 를 넣을 수 있고, 이를 Coroutine 내에서 위 코드와 같이 활용할 수 있다.

## Passed Coroutine Context

이렇게 Context 를 유지하는 이유는 Continuation 챕터에서 말했듯이 Coroutine 가 연속되어 있을때, 지속되어 유지해야 하는 정보때문이다.  
그래서 suspend 함수를 Java Byte Code 로 바꿔서보면 **Function 마지막 Arguments 에 CoroutineContext 를 받는게 추가**되고 있음을 알 수 있다.  

```kotlin
fun originalFunction(continuation: Continuation<Unit>): Any
```

아래와 같이 코드가 작성되어 있다고 했을때, ChildCtx?.name 에는 어떤 값이 나올까?

```kotlin
val superScopeContext = CoroutineName("Super!!")

suspend fun main(): Unit = withContext(superScopeContext) {

    val ctx = this.coroutineContext[CoroutineName]
    println("Parent Job : ${ctx?.name}")

    launch {
        val childCtx = this@launch.coroutineContext[CoroutineName]
        println("Child Job : ${childCtx?.name}")
    }
}
```

```shell
Parent Job : Super!!
Child Job : Super!!
```

위 코드를 출력하게 되면 Child 의 Ctx 의 Name 도 `Super!!` 가 나옴을 확인할 수 있는데, 이를 통해 자식 Job 인 `lanch {...}` 가 CoroutineContext 를 전달 받았음을 알 수 있다. 
위에서 설명한 것과 같이 마지막 인자의 CoroutineContext 에 전달 받았음을 알 수 있다.

## CoroutineContext Modification

CoroutineContext 의 장점은 수정이 상당히 쉽다는 것 이다. 기본적으로 `plus` 와 같은 operator 가 override 되어 있어서 아래와 같은 코드가 작성가능하다.

```kotlin
suspend fun main(): Unit = Coroutine {
    val superCoroutineName = CoroutineName("Super")
    println("[Super Ctx] Name : ${superCoroutineName[CoroutineName]?.name}")

    val subCoroutine = Job()
    println("[Sub Ctx] Name: ${subCoroutine[CoroutineName]?.name}")

    val mergedCtx = superCoroutineName + subCoroutine
    println("[Merged Ctx] Name : ${mergedCtx[CoroutineName]?.name}")
    
}
```

```shell
[Super Ctx] Name : Super
[Sub Ctx] Name: null
[Merged Ctx] Name : Super
```

위 코드 처럼 Context 끼리 더하기가 가능하다. 위의 코드를 출력 결과를 보면 Job 의 경우 CoroutineName Element 가 없었으나 `plus` 연산을 통해서 
superCoroutineName 의 Context 와 합쳐졌음을 알 수 있다. 위와같이 쉽게 수정이 가능하다. 보통 Thread Policy 와 같은 것들을 적용하거나, Auth 등등 
Coroutine 내에서 Thread 가 바뀌어도 저장될 수 있는 Context 들을 저장한다.

만약 아래와 같이 더하면 CoroutineName 에 어떤게 나올까?

```kotlin
suspend fun main(): Unit = Coroutine {
    val superCoroutineName = CoroutineName("Super")
    println("[Super Ctx] Name : ${superCoroutineName[CoroutineName]?.name}")

    val coroutineName2 = CoroutineName("CoroutineName2")
    val mergedCtx2 = subCoroutine + coroutineName2
    println("[Merged Ctx2] Name : ${mergedCtx2[CoroutineName]?.name}")
}
```

출력이 `CoroutineName2` 가 나오게 되는데, 그 이유는 제일 마지막에 Element 로 덮어쓰기 되기 때문이다. 
쉽게 생각해서 기본적으로 "덮어쓰기" 전략을 취하고 있다고 생각하면 좋다.


