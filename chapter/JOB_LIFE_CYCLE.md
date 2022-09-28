# Job

우리는 앞서서 Coroutine 의 기본 동작 원리 및 Continuation 과 CoroutineContext 와 같은 문맥을 유지해주는 부분에 
관해서도 공부했다. `Job` 은 CoroutineContext 인데 앞서서 계속 얘기한 Structured Concurrency 를 유지하기 위한 하나의 수단이자 
**코루틴은 모두 자신만의 Job 을 가지고 있다. Context 와 다르게 상속받지 않고, 오로지 자신의 코루틴에만 의존한다.** 이것도 말 보다 
코드를 보는게 백배 이해가 빠르니 아래 코드를 한번 같이 보자.

```kotlin
suspend fun main(): Unit = coroutineScope {

    val parentJob = coroutineContext[Job]
    println("[ParentJob] ${parentJob}")

    val job = launch(parentJob!!) {
        val childJob = coroutineContext[Job]
        println(parentJob == childJob) // false
        println(parentJob.children.first() == childJob) // true
    }

    job.join()
}
```

위 코드와 같이 parentJob 을 launch 에 CoroutineContext 로 넘겨주고 있지만, childJob 은 이를 덮어쓰기 하는 것이 아니라 
자신만의 Job 을 유지하고 있음을 확인할 수 있다. 왜 Job 과 같은 경우는 CoroutineContext Element 인데 덮어쓰지 않을까? 한번 알아 보도록 하자. 

### 특징

위의 코드를 보면 두가지 특성을 알 수 있다.  
- **첫번째로, 코루틴은 자신만의 `Job` 을 가진다.**  
- **두번째로, "ParentJob" 이 children 이라는 Property 에 자식 Coroutine 의 Job Reference 를 저장하고 있음을 아래 코드로 확인할 수 있었다.** 
```kotlin
println(parentJob.children.first() == childJob) // true
```

저 두가지 특성을 가지는 이유를 이제 알아보도록 하자. 앞서 계속 말한 **Structured Concurrency** 를 이제는 배울때다.

## Structured Concurrency

**Structured Concurrency 는 Async 연산이나 Concurrent 연산을 구조화 시키는 방법**을 말한다. 그래서 **부모 연산이 종료되더라도 Child 연산의 작업이 정상적으로 종료되는것을 보장**해야 하며, 
Child 연산이 하나라도 취소됬다면 이후 연산은 실행되지 않도록 보장해줘야 한다. 나는 쉽게 설명하기 위해 이를 JPA 의 Transaction propagation 옵션과 비슷하게 설명한다. (이게 맞는지는 좀 헷갈리기는 하지만 JPA 를 아시는 분들은 곧잘 이해하곤 했다.) 

그렇다면, 왜 Structured Concurrency 와 같은 기술이 도입됬을까? 우리가 Coroutine 을 쓰면 하나의 Task 를 Sub-Task 로 나누어 진행하게 되는데 **사실 호출자(Caller) 입장에서는 하나의 Task 일 뿐**이다. 
즉, Caller 입장에서는 Sub-Task 가 몇개로 나누어져 있든 말든 상관할 빠가 아니고 결국 하나의 Task 가 완료됬냐 안됬냐가 중요한 것이다. 
**따라서, 동시적(Concurrency) 일어나는 Sub-Task 들을 하나의 Task 로 구조화 시켜야 하는데 이를 위해 Structured Concurrency 가 도입된 것**이다. 
그래서 **하나의 자식 코루틴(Sub-Task) 이 취소되더라도, Cancellation 이 전파되는 이유가 결국 하나의 Task 로 봤을때는 안의 Sub-Task 가 실패하는 순간 전부 실패한것과 다름없기 때문**이다. 
그리고 **Child 가 끝나기까지 기다리는 이유도 모든 Sub-Routine 들이 끝나야 하나의 Task 가 되기 때문**이다. 이 정도면 Structured Concurrency 가 어느정도 이해갔을거라고 생각한다. 
이제 어떻게 이 Structured Concurrency 를 코드상으로 구성하는지 한번 알아보도록 하자.

(최대한 내 지식으로 잘 설명해보려고 했는데 틀린게 있다면 Github - Issue 에 피드백 해주면 좋을 것 같다.)

# Job Life Cycle

위에서 설명했듯이, Coroutine 도 하나의 Task 다. 따라서 **해당 Task 가 생성되고, 진행되고, 완료**되고 와 같은 **Life Cycle** 이 존재한다.
Kotlin 의 LifeCycle 을 아는 것은 상당히 중요한데, 우리는 앞서 **구조적(Structured Concurrency)** 에 관해서 배웠었다.
결국 부모 Coroutine 은 Children 의 Job 이 끝나기까지 기다리는데, 이와 같은 구조가 어떻게 가능한 것일까?

## Job Life Cycle

|   State    | isActive | isCompleted | isCancelled |
|:----------:|:--------:|:-----------:|:-----------:|
|    New     |  false   |    false    |    false    |
|   Active   |   true   |    false    |    false    |
| Completing |   true   |    false    |    false    |
| Cancelling |  false   |    false    |    true     |
| Cancelled  |  false   |    true     |    true     |
| Completed  |  false   |    true     |    false    |

### Active

Active 상태는 Coroutine Builder 를 통해 Job 이 생성되면 기본적으로 Active 상태로 생성된다. New 상태가 아닌 이유는 있다가 알아보자. 
Active 상태에서 Coroutine 은 작업을 수행하는 중이다. 즉, Active State 라는 것은 작업(Task) 를 실행중이라는 것을 의미한다.

### New

Coroutine 을 New 로 생성하기 위해서는 좀 특이한 방법을 사용해야 한다. New 상태로 생성 할 수 있는 방법은 그건 coroutineBuilder 의 parameter 로 `start = CoroutineStart.LAZY` 값을 넣어주면 된다. 
New 상태에서 실행되기 위해서는 결국 Active 상태로 이동해야 하는데, Active 상태로 이동하는 것은 구현체 마다 다르겠지만 보통 평가되는 시점에 이동된다. 아래 코드를 보면 launch 를 join 하는 시점에 코드가 실행됨을 알 수 있다. 

```kotlin
suspend fun main(): Unit = coroutineScope {

    val newStateJob = launch(start = CoroutineStart.LAZY) {
        println("[LAZY-JOB] START!")
        delay(1000)
        println("[LAZY-JOB] END!")
    }

    val defaultJob = launch {
        println("[DEFAULT-JOB] START!")
        delay(1000)
        println("[DEFAULT-JOB] END!")
    }

    defaultJob.join()
    newStateJob.join()
}
```

```shell
[DEFAULT-JOB] START!
[DEFAULT-JOB] END!
[LAZY-JOB] START! // lazily init
[LAZY-JOB] END!
```

내가 Lazy 를 썼던 경우 위에서 취소가 났을 경우 이 API 콜을 호출하기는 싫고 부분적으로 Sub-Task 들에서 쓰일때 한쪽에서 Lazy 로 구성해두고 쓰는시점에 평가하는 방식으로 사용했었다.

### Completing

우리는 항상 코루틴에서 **Structured Concurrency 를 어떻게 구현했을까**를 생각해야 한다. 이걸 생각하냐 안하냐가 이 LifeCycle 의 
각 단계가 왜 존재하는지 이해할 수 있다. 일단 Structured Concurrency 개념을 잘 생각하면서 아래 코드를 한번 보자.

```kotlin
suspend fun main() = coroutineScope {

    val parentJob = launch {

        println("Parent Job is Start!!")

        val childJob = launch {
            delay(200)
            println("Child Job 1 is Start!!")
        }

        val childJob2 = launch {
            delay(300)
            println("Child Job 2 is Start!!")
        }

        println("Child Job1 is Finished ? ${childJob.isCompleted}")
        println("Child Job2 is Finished ? ${childJob2.isCompleted}")

        println("Parent Job is Done!!")
    }

    delay(100)

    parentJob.printChildJobState()
    println("[IMPORTANT-LOG-1] Parent Job is Finished ? ${parentJob.isCompleted}")

    delay(300)
    parentJob.printChildJobState()
    println("[IMPORTANT-LOG-2] Parent Job is Finished ? ${parentJob.isCompleted}")
}

fun Job.printChildJobState() {
    if (this.children.firstOrNull() == null) println("All ChildrenJob is finished")
    var count = 1
    for (childJob in this.children) {
        println("Child Job$count is Finished ? ${childJob.isCompleted}")
        count++
    }
}
```

<img width="448" alt="image" src="https://user-images.githubusercontent.com/57784077/192841025-8ea7e838-9c8a-4515-b893-aa3d959cbb6d.png">

위의 코드의 `[IMPORTANT-LOG-1]` 부분의 Log 에서 Parent Job 의 finished 가 True 여야 할거 같지만, `Parent Job is Finished ? false` 로 출력된다. 
그 이유는 무엇일까? 분명 **subParentJob 은 모두 종료**되었다는 로그도 그 앞전에 찍혀있다는 걸 확인할 수 있다. 
사실 SubParentJob 자체의 연산은 성공했을 수 있지만, 내부에 있는 **childJob1, childJob2 의 성공 여부는 아직 확인할 수 없다.**
만약 이를 생각하지 않고, SubParentJob 을 `completed` 상태로 곧바로 바꿔버린다면, 내부에 있는 Job 들의 실행완료를 보장해줄 수 없게 된다. 
따라서 **일단은 Completed 가 아닌 Completing 상태에 머물게 된다.** 그 이후 모든 자식 Job 이 완료되면 Completed 로 바뀌게 된다. 즉, 이러한 메커니즘을 통해서 Structured Concurrency 를 구성하는 것이다.

### Cancelling

**Job 이 수행하는 도중에 종료되거나 예외가 발생하게 되면 Job 은 Cancelling 상태로 이동**하게 된다. 
잘 생각해보면 취소 되면 그냥 취소 시켜도 되지않아? "Transaction 도 하나라도 취소되면 Rollback 시키잖아" 라고 할 수 있다. 
엄밀히 말하면 Rollback 이란 건, 예기지 못한 상황이 발생해서 Rollback 을 통해서 데이터의 정합성을 맞춰주는 것이라고 볼 수 있는데, 
Coroutine 또한 **예기치 못한 상황이 발생하면 바로 Cancelled 로 가는게 아니라 Cancelling 과정에서 "데이터의 정합성을 맞춰주는 과정" 과 같이 
예기치 못한 상황으로 종료되었을때 자원을 반납한다던지의 과정을 이 State 에서 수행**한다. 이러한 과정이 모두 종료되면 "Cancelled" State 로 이동된다.



