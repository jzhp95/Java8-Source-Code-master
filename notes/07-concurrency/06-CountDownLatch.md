# CountDownLatch 源码解读

## 类概述

`CountDownLatch` 是一个同步辅助工具,允许一个或多个线程等待直到在其他线程中执行的一组操作完成。

```java
public class CountDownLatch {
```

## 核心特性

### 1. 一次性同步

- CountDownLatch 用给定的计数初始化
- `await()` 方法阻塞直到计数因 `countDown()` 调用而达到零
- 计数达到零后,所有等待线程被释放
- **一次性现象**: 计数不能重置
- 如果需要可重置版本,考虑使用 `CyclicBarrier`

### 2. 灵活的同步工具

CountDownLatch 是一个通用的同步工具,可用于多种目的:

- **简单开关/门**: 初始化为 1,所有调用 `await()` 的线程在门打开前等待
- **等待 N 个线程完成**: 初始化为 N,使一个线程等待直到 N 个线程完成某些操作
- **等待操作完成 N 次**: 初始化为 N,使一个线程等待直到某些操作完成 N 次

### 3. 非阻塞特性

CountDownLatch 的一个有用特性是:

- 调用 `countDown()` 的线程不需要等待计数达到零
- 简单地阻止任何线程通过 `await()` 直到所有线程都能通过

## 核心实现

### Sync 内部类

CountDownLatch 使用 `AbstractQueuedSynchronizer`(AQS) 作为同步控制:

```java
private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 4982264981922014374L;

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }

    protected int tryAcquireShared(int acquires) {
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // 减少计数;转换到零时信号通知
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c-1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}
```

#### AQS 状态使用

- 使用 AQS 的 `state` 字段表示计数
- `setState(count)`: 初始化计数
- `getState()`: 获取当前计数

#### tryAcquireShared - 尝试获取共享锁

```java
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```

- **返回 1**: 计数为 0,获取成功,可以继续
- **返回 -1**: 计数 > 0,获取失败,需要等待
- 用于 `await()` 方法

#### tryReleaseShared - 尝试释放共享锁

```java
protected boolean tryReleaseShared(int releases) {
    for (;;) {
        int c = getState();
        if (c == 0)
            return false;
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

- **返回 true**: 计数从 1 变为 0,需要唤醒等待线程
- **返回 false**: 计数已经为 0,或计数仍 > 0
- 使用 CAS 循环保证原子性
- 用于 `countDown()` 方法

## 核心方法

### 构造方法

```java
public CountDownLatch(int count) {
    if (count < 0) throw new IllegalArgumentException("count < 0");
    this.sync = new Sync(count);
}
```

- **参数**: `count` - 必须调用 `countDown()` 的次数
- **异常**: `IllegalArgumentException` - 如果 count 为负

### await() - 无限等待

```java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

- **功能**: 使当前线程等待直到锁存器计数减到零,除非线程被中断
- **行为**:
  - 如果当前计数为零,立即返回
  - 如果当前计数大于零,当前线程被禁用并休眠,直到:
    - 由于 `countDown()` 调用计数达到零
    - 其他线程中断当前线程
- **异常**: `InterruptedException` - 如果等待时被中断

#### acquireSharedInterruptibly 实现

```java
// AQS 方法
public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```

1. 检查中断状态
2. 尝试获取共享锁(调用 `tryAcquireShared`)
3. 如果失败,进入等待队列

### await(timeout, unit) - 超时等待

```java
public boolean await(long timeout, TimeUnit unit)
    throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
}
```

- **功能**: 使当前线程等待直到锁存器计数减到零,除非线程被中断或超时
- **参数**:
  - `timeout`: 最大等待时间
  - `unit`: 超时参数的时间单位
- **返回值**:
  - `true`: 计数达到零
  - `false`: 计数达到零前等待时间已过
- **异常**: `InterruptedException` - 如果等待时被中断

#### tryAcquireSharedNanos 实现

```java
// AQS 方法
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
    throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    return tryAcquireShared(arg) >= 0 ||
        doAcquireSharedNanos(arg, nanosTimeout);
}
```

1. 检查中断状态
2. 尝试获取共享锁
3. 如果失败,进入带超时的等待队列

### countDown() - 减少计数

```java
public void countDown() {
    sync.releaseShared(1);
}
```

- **功能**: 减少锁存器的计数,如果计数达到零则释放所有等待线程
- **行为**:
  - 如果当前计数大于零,则递减
  - 如果新计数为零,则所有等待线程被重新启用
  - 如果当前计数等于零,则什么也不发生

#### releaseShared 实现

```java
// AQS 方法
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

1. 尝试释放共享锁(调用 `tryReleaseShared`)
2. 如果返回 true(计数从 1 变为 0),唤醒等待线程

### getCount() - 获取当前计数

```java
public long getCount() {
    return sync.getCount();
}
```

- **功能**: 返回当前计数
- **用途**: 通常用于调试和测试

### toString() - 字符串表示

```java
public String toString() {
    return super.toString() + "[Count = " + sync.getCount() + "]";
}
```

- **功能**: 返回标识此锁存器及其状态的字符串
- **格式**: `CountDownLatch[Count = n]`

## 使用示例

### 示例 1: 启动信号和完成信号

```java
class Driver {
  void main() throws InterruptedException {
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(N);

    for (int i = 0; i < N; ++i)
      new Thread(new Worker(startSignal, doneSignal)).start();

    doSomethingElse();            // 还不让运行
    startSignal.countDown();      // 让所有线程继续
    doSomethingElse();
    doneSignal.await();           // 等待所有完成
  }
}

class Worker implements Runnable {
  private final CountDownLatch startSignal;
  private final CountDownLatch doneSignal;
  Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
    this.startSignal = startSignal;
    this.doneSignal = doneSignal;
  }
  public void run() {
    try {
      startSignal.await();
      doWork();
      doneSignal.countDown();
    } catch (InterruptedException ex) {}
  }

  void doWork() { ... }
}
```

**说明**:
- `startSignal`: 防止任何工作线程继续,直到驱动程序准备好
- `doneSignal`: 允许驱动程序等待直到所有工作线程完成

### 示例 2: 分割问题

```java
class Driver2 {
  void main() throws InterruptedException {
    CountDownLatch doneSignal = new CountDownLatch(N);
    Executor e = ...

    for (int i = 0; i < N; ++i)
      e.execute(new WorkerRunnable(doneSignal, i));

    doneSignal.await();           // 等待所有完成
  }
}

class WorkerRunnable implements Runnable {
  private final CountDownLatch doneSignal;
  private final int i;
  WorkerRunnable(CountDownLatch doneSignal, int i) {
    this.doneSignal = doneSignal;
    this.i = i;
  }
  public void run() {
    try {
      doWork(i);
      doneSignal.countDown();
    } catch (InterruptedException ex) {}
  }

  void doWork() { ... }
}
```

**说明**:
- 将问题分成 N 部分
- 每部分用一个 Runnable 描述,执行该部分并在锁存器上倒计时
- 将所有 Runnable 排队到 Executor
- 所有子部分完成后,协调线程能够通过 await

## 内存一致性效果

在计数达到零之前,线程中调用 `countDown()` 之前的操作与另一个线程中相应 `await()` 成功返回之后的操作之间存在 **happens-before** 关系。

## 与 CyclicBarrier 的区别

| 特性 | CountDownLatch | CyclicBarrier |
|------|---------------|---------------|
| 可重用性 | 一次性,不能重置 | 可重用,可以重置 |
| 计数方向 | 倒计时,从 N 到 0 | 计数,从 0 到 N |
| 使用场景 | 等待事件完成 | 等待线程到达屏障点 |
| 线程角色 | 等待线程和工作线程分离 | 所有线程都是参与者 |
| 异常处理 | 不传播异常 | 可以传播异常 |

## 设计模式

### 1. 门模式(Count = 1)

```java
CountDownLatch gate = new CountDownLatch(1);

// 工作线程
gate.await();  // 等待门打开
doWork();

// 主线程
prepare();
gate.countDown();  // 打开门
```

### 2. 汇聚模式(Count = N)

```java
CountDownLatch latch = new CountDownLatch(N);

// N 个工作线程
doWork();
latch.countDown();

// 主线程
latch.await();  // 等待所有工作线程完成
```

## 关键点总结

1. **AQS 实现**: 基于 AbstractQueuedSynchronizer,使用 state 表示计数
2. **一次性**: 计数不能重置,是一次性同步工具
3. **共享锁**: 使用共享模式,多个线程可以同时等待
4. **CAS 操作**: 使用 CAS 保证计数的原子性
5. **灵活用途**: 可用作简单开关或等待 N 个线程完成
6. **非阻塞**: countDown() 不需要等待计数达到零
7. **内存一致性**: 保证 happens-before 关系
8. **中断支持**: await() 方法支持中断

## 相关类

- `AbstractQueuedSynchronizer`: 同步器框架
- `CyclicBarrier`: 可重用的循环屏障
- `Semaphore`: 计数信号量
- `Future`: 异步计算结果
