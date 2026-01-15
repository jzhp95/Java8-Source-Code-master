# CyclicBarrier 源码解读

## 类概述

`CyclicBarrier` 是一个同步辅助工具,允许一组线程全部等待彼此到达一个公共屏障点。CyclicBarrier 在涉及固定大小的线程组必须偶尔等待彼此的程序中很有用。

```java
public class CyclicBarrier {
```

## 核心特性

### 1. 循环可重用

- 屏障被称为 **循环(cyclic)** 的原因是它可以在等待线程被释放后重用
- 与 CountDownLatch 不同,CyclicBarrier 可以重复使用
- 适用于需要多次同步的场景

### 2. 屏障动作(Barrier Action)

CyclicBarrier 支持一个可选的 `Runnable` 命令:

- 在最后一方线程到达后,在任何线程被释放之前,每个屏障点运行一次
- 屏障动作在任何一方继续之前更新共享状态很有用
- 由最后一个到达屏障的线程执行

### 3. 全有或全无破坏模型

CyclicBarrier 使用全有或全无的破坏模型:

- 如果一个线程因中断、失败或超时而过早离开屏障点
- 所有其他在该屏障点等待的线程也将通过 `BrokenBarrierException` 异常离开
- 如果它们也在大约同一时间被中断,则抛出 `InterruptedException`

## 核心数据结构

### Generation - 代

```java
private static class Generation {
    boolean broken = false;
}
```

- 屏障的每次使用都表示为一个 Generation 实例
- 当屏障被触发或重置时,Generation 会改变
- 可能有许多 Generation 与使用屏障的线程相关联
- 但一次只能有一个是活动的(应用 `count` 的那个)
- 其余的要么被破坏要么被触发
- 如果有破坏但没有随后的重置,则不需要活动的 Generation

### 核心字段

```java
/** 守护屏障入口的锁 */
private final ReentrantLock lock = new ReentrantLock();

/** 等待直到触发的条件 */
private final Condition trip = lock.newCondition();

/** 参与方数量 */
private final int parties;

/** 触发时运行的命令 */
private final Runnable barrierCommand;

/** 当前代 */
private Generation generation = new Generation();

/** 仍在等待的参与方数量 */
private int count;
```

#### 字段说明

- **lock**: ReentrantLock,用于保护屏障入口
- **trip**: Condition,用于等待直到触发
- **parties**: 必须调用 await 的线程数量
- **barrierCommand**: 屏障触发时执行的命令(可为 null)
- **generation**: 当前代,用于标识屏障的当前使用
- **count**: 仍在等待的参与方数量,从 parties 倒计时到 0

## 核心方法

### 构造方法

#### 带屏障动作的构造方法

```java
public CyclicBarrier(int parties, Runnable barrierAction) {
    if (parties <= 0) throw new IllegalArgumentException();
    this.parties = parties;
    this.count = parties;
    this.barrierCommand = barrierAction;
}
```

- **参数**:
  - `parties`: 必须调用 `await` 才能触发屏障的线程数量
  - `barrierAction`: 屏障触发时执行的命令,如果没有动作则为 null
- **异常**: `IllegalArgumentException` - 如果 parties 小于 1

#### 不带屏障动作的构造方法

```java
public CyclicBarrier(int parties) {
    this(parties, null);
}
```

- 创建一个不执行预定义动作的 CyclicBarrier

### nextGeneration() - 下一代

```java
private void nextGeneration() {
    // 信号通知上一代完成
    trip.signalAll();
    // 设置下一代
    count = parties;
    generation = new Generation();
}
```

- **功能**: 更新屏障触发时的状态并唤醒所有人
- **调用条件**: 仅在持有锁时调用
- **操作**:
  1. 唤醒所有等待线程
  2. 重置计数为 parties
  3. 创建新的 Generation

### breakBarrier() - 破坏屏障

```java
private void breakBarrier() {
    generation.broken = true;
    count = parties;
    trip.signalAll();
}
```

- **功能**: 将当前屏障代设置为破坏状态并唤醒所有人
- **调用条件**: 仅在持有锁时调用
- **操作**:
  1. 标记当前代为破坏状态
  2. 重置计数为 parties
  3. 唤醒所有等待线程(它们将抛出 BrokenBarrierException)

### dowait() - 核心等待逻辑

```java
private int dowait(boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException,
           TimeoutException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }

        int index = --count;
        if (index == 0) {  // 触发
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction)
                    breakBarrier();
            }
        }

        // 循环直到触发、破坏、中断或超时
        for (;;) {
            try {
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // 我们即将完成等待,即使我们没有
                    // 被中断,所以这个中断被视为
                    // "属于"后续执行。
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

#### 执行流程

1. **获取锁**: 获取 ReentrantLock
2. **检查破坏状态**: 如果当前代已破坏,抛出 BrokenBarrierException
3. **检查中断**: 如果线程被中断,破坏屏障并抛出 InterruptedException
4. **减少计数**: `--count`,获取到达索引
5. **最后一个线程**(index == 0):
   - 执行屏障动作(如果有)
   - 调用 nextGeneration() 唤醒所有线程
   - 返回 0
6. **其他线程**:
   - 在 Condition 上等待(带或不带超时)
   - 处理中断
   - 检查破坏状态
   - 检查代变化
   - 检查超时
7. **释放锁**: finally 块中释放锁

#### 异常处理

- **BrokenBarrierException**: 屏障被破坏
- **InterruptedException**: 线程被中断
- **TimeoutException**: 超时(仅限带超时版本)

### await() - 无限等待

```java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // 不可能发生
    }
}
```

- **功能**: 等待所有 parties 都在此屏障上调用了 await
- **返回值**: 当前线程的到达索引
  - `getParties() - 1`: 第一个到达
  - `0`: 最后一个到达
- **异常**:
  - `InterruptedException`: 如果当前线程在等待时被中断
  - `BrokenBarrierException**: 如果另一个线程在当前线程等待时被中断或超时,或屏障被重置,或调用 await 时屏障已破坏,或屏障动作(如果有)因异常而失败

#### 等待条件

如果当前线程不是最后一个到达,则它被禁用并休眠,直到以下情况之一发生:

- 最后一个线程到达
- 其他线程中断当前线程
- 其他线程中断其他等待线程之一
- 其他线程在等待屏障时超时
- 其他线程在此屏障上调用 reset()

### await(timeout, unit) - 超时等待

```java
public int await(long timeout, TimeUnit unit)
    throws InterruptedException,
           BrokenBarrierException,
           TimeoutException {
    return dowait(true, unit.toNanos(timeout));
}
```

- **功能**: 等待所有 parties 都在此屏障上调用了 await,或指定的等待时间过去
- **参数**:
  - `timeout`: 等待屏障的时间
  - `unit`: 超时参数的时间单位
- **返回值**: 当前线程的到达索引
- **异常**:
  - `InterruptedException`: 如果当前线程在等待时被中断
  - `TimeoutException`: 如果指定的等待时间过去(在这种情况下,屏障将被破坏)
  - `BrokenBarrierException`: 如果另一个线程在当前线程等待时被中断或超时,或屏障被重置,或调用 await 时屏障已破坏,或屏障动作(如果有)因异常而失败

### getParties() - 获取参与方数量

```java
public int getParties() {
    return parties;
}
```

- **功能**: 返回触发此屏障所需的参与方数量
- **返回值**: 触发此屏障所需的参与方数量

### isBroken() - 检查破坏状态

```java
public boolean isBroken() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return generation.broken;
    } finally {
        lock.unlock();
    }
}
```

- **功能**: 查询此屏障是否处于破坏状态
- **返回值**: 如果自构造或上次重置以来,一方或多方因中断或超时而破坏了此屏障,或屏障动作因异常而失败,则返回 `true`;否则返回 `false`

### reset() - 重置屏障

```java
public void reset() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        breakBarrier();   // 破坏当前代
        nextGeneration(); // 开始新一代
    } finally {
        lock.unlock();
    }
}
```

- **功能**: 将屏障重置为其初始状态
- **效果**: 如果有任何参与方当前正在屏障处等待,它们将返回 `BrokenBarrierException`
- **注意事项**:
  - 由于其他原因发生破坏后的重置可能很难执行
  - 线程需要以某种其他方式重新同步
  - 并选择一个执行重置
  - 对于后续使用,可能更倾向于创建一个新屏障

### getNumberWaiting() - 获取等待数量

```java
public int getNumberWaiting() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return parties - count;
    } finally {
        lock.unlock();
    }
}
```

- **功能**: 返回当前在屏障处等待的参与方数量
- **用途**: 主要用于调试和断言
- **返回值**: 当前在 await 中阻塞的参与方数量

## 使用示例

### 示例 1: 并行分解设计

```java
class Solver {
  final int N;
  final float[][] data;
  final CyclicBarrier barrier;

  class Worker implements Runnable {
    int myRow;
    Worker(int row) { myRow = row; }
    public void run() {
      while (!done()) {
        processRow(myRow);

        try {
          barrier.await();
        } catch (InterruptedException ex) {
          return;
        } catch (BrokenBarrierException ex) {
          return;
        }
      }
    }
  }

  public Solver(float[][] matrix) {
    data = matrix;
    N = matrix.length;
    Runnable barrierAction =
      new Runnable() { public void run() { mergeRows(...); }};
    barrier = new CyclicBarrier(N, barrierAction);

    List<Thread> threads = new ArrayList<Thread>(N);
    for (int i = 0; i < N; i++) {
      Thread thread = new Thread(new Worker(i));
      threads.add(thread);
      thread.start();
    }

    // 等待完成
    for (Thread thread : threads)
      thread.join();
  }
}
```

**说明**:
- 每个工作线程处理矩阵的一行,然后在屏障处等待,直到所有行都被处理
- 当所有行都被处理时,提供的 Runnable 屏障动作被执行并合并行
- 如果合并器确定已找到解决方案,则 `done()` 将返回 `true`,每个工作线程将终止

### 示例 2: 使用到达索引

```java
if (barrier.await() == 0) {
  // 记录此迭代的完成
}
```

**说明**:
- 如果屏障动作不依赖于参与方在执行时被挂起
- 那么参与方中的任何线程都可以在释放时执行该动作
- `await()` 返回该线程在屏障处的到达索引
- 可以选择哪个线程应该执行屏障动作

## 内存一致性效果

线程中调用 `await()` 之前的操作与属于屏障动作的操作之间存在 **happens-before** 关系,而屏障动作的操作又与在其他线程中相应 `await()` 成功返回之后的操作之间存在 **happens-before** 关系。

## 与 CountDownLatch 的区别

| 特性 | CyclicBarrier | CountDownLatch |
|------|---------------|---------------|
| 可重用性 | 可重用,可以循环使用 | 一次性,不能重置 |
| 计数方向 | 从 parties 倒计时到 0 | 从 N 倒计时到 0 |
| 使用场景 | 线程互相等待到达屏障点 | 等待事件完成 |
| 线程角色 | 所有线程都是参与者 | 等待线程和工作线程分离 |
| 屏障动作 | 支持可选的屏障动作 | 不支持 |
| 异常处理 | 全有或全无破坏模型 | 不传播异常 |
| 重置 | 可以 reset() 重置 | 不能重置 |

## 设计模式

### 1. 迭代同步模式

```java
CyclicBarrier barrier = new CyclicBarrier(N, () -> {
    // 屏障动作:合并结果
    mergeResults();
});

// N 个工作线程
while (!done()) {
    process();
    barrier.await();  // 等待所有线程完成本轮迭代
}
```

### 2. 分阶段处理模式

```java
CyclicBarrier barrier1 = new CyclicBarrier(N, () -> {
    // 第一阶段完成后的动作
    preparePhase2();
});

CyclicBarrier barrier2 = new CyclicBarrier(N, () -> {
    // 第二阶段完成后的动作
    finalize();
});

// N 个工作线程
phase1();
barrier1.await();
phase2();
barrier2.await();
```

## 关键点总结

1. **循环可重用**: 屏障可以重用,适用于需要多次同步的场景
2. **ReentrantLock**: 使用 ReentrantLock 和 Condition 实现同步
3. **Generation**: 使用 Generation 标识屏障的当前使用
4. **屏障动作**: 支持可选的屏障动作,在最后一方到达后执行
5. **全有或全无**: 使用全有或全无的破坏模型
6. **到达索引**: await() 返回到达索引,可用于选择执行屏障动作的线程
7. **重置机制**: 可以 reset() 重置屏障到初始状态
8. **内存一致性**: 保证 happens-before 关系

## 相关类

- `CountDownLatch`: 一次性倒计时锁存器
- `ReentrantLock`: 可重入锁
- `Condition`: 条件变量
- `Phaser`: 更灵活的同步屏障
