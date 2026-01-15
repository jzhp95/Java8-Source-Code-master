# Semaphore 源码解读

## 类概述

`Semaphore` 是一个计数信号量。从概念上讲,信号量维护一组许可。每个 `acquire()` 在必要时阻塞直到有许可可用,然后获取它。每个 `release()` 添加一个许可,可能释放一个阻塞的获取者。

```java
public class Semaphore implements java.io.Serializable {
```

## 核心特性

### 1. 计数信号量

- 信号量维护一组许可
- 没有使用实际的许可对象
- Semaphore 只是保持可用数量的计数并相应地行动
- 许可数可以为负,在这种情况下必须先 release 才能 acquire

### 2. 资源访问控制

信号量通常用于限制可以访问某些(物理或逻辑)资源的线程数量:

```java
class Pool {
  private static final int MAX_AVAILABLE = 100;
  private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

  public Object getItem() throws InterruptedException {
    available.acquire();
    return getNextAvailableItem();
  }

  public void putItem(Object x) {
    if (markAsUnused(x))
      available.release();
  }
}
```

**说明**:
- 在获取项目之前,每个线程必须从信号量获取许可
- 保证有项目可供使用
- 当线程完成项目时,将其返回到池中
- 将许可返回到信号量,允许另一个线程获取该项目

### 3. 二元信号量

初始化为 1 的信号量,最多只有一个许可可用,可以用作互斥锁:

- 更常见的称为 **二元信号量**
- 只有两个状态:一个许可可用,或零个许可可用
- 与许多 Lock 实现不同,"锁"可以由所有者以外的线程释放
- 因为信号量没有所有权的概念
- 这在某些专门的情况下很有用,例如死锁恢复

### 4. 公平性(Fairness)

构造方法可以选择接受公平性参数:

- **fair = false**: 不保证线程获取许可的顺序
  - 允许 **插队(barging)**
  - 调用 `acquire()` 的线程可以在等待的线程之前分配许可
  - 新线程逻辑上将自己放在等待线程队列的头部
- **fair = true**: 保证线程按照调用方法的顺序获取许可(FIFO)
  - FIFO 排序必然适用于这些方法内的特定内部执行点
  - 一个线程可能在另一个线程之前调用 `acquire()`,但在另一个线程之后到达排序点

**使用建议**:
- 用于控制资源访问的信号量应该初始化为公平的
- 确保没有线程被饿死,无法访问资源
- 用于其他类型的同步控制时,非公平排序的吞吐量优势通常超过公平性考虑

### 5. 批量操作

提供便捷方法一次获取和释放多个许可:

- `acquire(int permits)`
- `release(int permits)`
- 警告:在没有设置公平性为 true 时使用这些方法会增加无限期推迟的风险

## 核心实现

### Sync - 同步实现

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 1192457210091910933L;

    Sync(int permits) {
        setState(permits);
    }

    final int getPermits() {
        return getState();
    }

    final int nonfairTryAcquireShared(int acquires) {
        for (;;) {
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }

    protected final boolean tryReleaseShared(int releases) {
        for (;;) {
            int current = getState();
            int next = current + releases;
            if (next < current) // overflow
                throw new Error("Maximum permit count exceeded");
            if (compareAndSetState(current, next))
                return true;
        }
    }

    final void reducePermits(int reductions) {
        for (;;) {
            int current = getState();
            int next = current - reductions;
            if (next > current) // underflow
                throw new Error("Permit count underflow");
            if (compareAndSetState(current, next))
                return;
        }
    }

    final int drainPermits() {
        for (;;) {
            int current = getState();
            if (current == 0 || compareAndSetState(current, 0))
                return current;
        }
    }
}
```

#### AQS 状态使用

- 使用 AQS 的 `state` 字段表示许可数量
- `setState(permits)`: 初始化许可数量
- `getState()`: 获取当前许可数量

#### nonfairTryAcquireShared - 非公平尝试获取

```java
final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```

- **返回值**: 剩余的许可数(可能为负)
- **逻辑**:
  1. 获取当前可用许可数
  2. 计算剩余许可数
  3. 如果剩余 < 0 或 CAS 成功,返回剩余数
- **特点**: 不检查是否有等待线程,直接尝试获取

#### tryReleaseShared - 尝试释放

```java
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```

- **返回值**: 总是返回 true
- **逻辑**:
  1. 获取当前许可数
  2. 计算新的许可数
  3. 检查溢出
  4. CAS 更新状态
- **特点**: 使用 CAS 循环保证原子性

#### reducePermits - 减少许可

```java
final void reducePermits(int reductions) {
    for (;;) {
        int current = getState();
        int next = current - reductions;
        if (next > current) // underflow
            throw new Error("Permit count underflow");
        if (compareAndSetState(current, next))
            return;
    }
}
```

- **功能**: 减少可用许可数量
- **用途**: 跟踪变得不可用的资源
- **特点**: 不阻塞等待许可变得可用

#### drainPermits - 排空许可

```java
final int drainPermits() {
    for (;;) {
        int current = getState();
        if (current == 0 || compareAndSetState(current, 0))
            return current;
    }
}
```

- **功能**: 获取并返回所有立即可用的许可
- **返回值**: 获取的许可数量
- **特点**: 原子操作,一次性获取所有可用许可

### NonfairSync - 非公平版本

```java
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -2694183684443567898L;

    NonfairSync(int permits) {
        super(permits);
    }

    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }
}
```

- **特点**: 直接调用 `nonfairTryAcquireShared`
- **行为**: 允许插队,不保证获取顺序
- **优势**: 更高的吞吐量

### FairSync - 公平版本

```java
static final class FairSync extends Sync {
    private static final long serialVersionUID = 2014338818796000944L;

    FairSync(int permits) {
        super(permits);
    }

    protected int tryAcquireShared(int acquires) {
        for (;;) {
            if (hasQueuedPredecessors())
                return -1;
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }
}
```

- **特点**: 在获取前检查是否有等待的前驱线程
- **行为**: 保证 FIFO 获取顺序
- **劣势**: 较低的吞吐量

#### hasQueuedPredecessors

```java
// AQS 方法
public final boolean hasQueuedPredecessors() {
    return (head != tail && ((sync = head.next) != null &&
                         sync.thread != Thread.currentThread()));
}
```

- **功能**: 检查是否有线程在当前线程之前排队
- **返回值**: 如果有前驱线程返回 true

## 核心方法

### 构造方法

#### 默认构造方法(非公平)

```java
public Semaphore(int permits) {
    sync = new NonfairSync(permits);
}
```

- **参数**: `permits` - 初始可用许可数
- **特点**: 默认使用非公平策略

#### 公平构造方法

```java
public Semaphore(int permits, boolean fair) {
    sync = fair ? new FairSync(permits) : new NonfairSync(permits);
}
```

- **参数**:
  - `permits`: 初始可用许可数
  - `fair`: 如果为 true,则保证竞争下的先进先出授予许可;否则为 false
- **特点**: 根据公平参数选择 Sync 实现

### acquire() - 获取许可(可中断)

```java
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

- **功能**: 从信号量获取一个许可,阻塞直到有许可可用,或线程被中断
- **行为**:
  - 如果有许可可用,立即获取并返回,减少可用许可数
  - 如果没有许可可用,当前线程被禁用并休眠,直到:
    - 其他线程为此信号量调用 `release()` 方法,且当前线程是下一个被分配许可的线程
    - 其他线程中断当前线程
- **异常**: `InterruptedException` - 如果当前线程被中断

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
2. 尝试获取共享锁
3. 如果失败,进入等待队列

### acquireUninterruptibly() - 获取许可(不可中断)

```java
public void acquireUninterruptibly() {
    sync.acquireShared(1);
}
```

- **功能**: 从信号量获取一个许可,阻塞直到有许可可用
- **行为**:
  - 如果有许可可用,立即获取并返回
  - 如果没有许可可用,当前线程被禁用并休眠,直到其他线程调用 `release()` 方法
- **中断处理**: 如果当前线程在等待许可时被中断,它将继续等待
  - 但分配许可的时间可能与没有发生中断时不同
  - 当线程从此方法返回时,其中断状态将被设置

### tryAcquire() - 尝试获取许可

```java
public boolean tryAcquire() {
    return sync.nonfairTryAcquireShared(1) >= 0;
}
```

- **功能**: 从信号量获取一个许可,仅在调用时有许可可用
- **返回值**:
  - `true`: 获取了许可
  - `false`: 没有获取许可
- **行为**:
  - 如果有许可可用,立即获取并返回 `true`,减少可用许可数
  - 如果没有许可可用,立即返回 `false`
- **公平性**: 即使信号量设置为使用公平排序策略,`tryAcquire()` 也会立即获取许可(如果有可用),无论是否有其他线程正在等待
  - 这种"插队"行为在某些情况下很有用,尽管它破坏了公平性
  - 如果想遵守公平性设置,使用 `tryAcquire(0, TimeUnit.SECONDS)`

### tryAcquire(timeout, unit) - 超时尝试获取

```java
public boolean tryAcquire(long timeout, TimeUnit unit)
    throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
}
```

- **功能**: 从信号量获取一个许可,如果在给定等待时间内有许可可用且当前线程未被中断
- **参数**:
  - `timeout`: 最大等待时间
  - `unit`: 超时参数的时间单位
- **返回值**:
  - `true`: 获取了许可
  - `false`: 在获取许可之前等待时间已过
- **异常**: `InterruptedException` - 如果当前线程被中断

### release() - 释放许可

```java
public void release() {
    sync.releaseShared(1);
}
```

- **功能**: 释放一个许可,将其返回到信号量
- **行为**:
  - 释放一个许可,增加可用许可数
  - 如果有线程正在尝试获取许可,则选择一个并给予刚刚释放的许可
  - 该线程被(重新)启用以进行线程调度
- **所有权**: 没有要求释放许可的线程必须通过调用 `acquire()` 获取该许可
  - 信号量的正确使用由应用程序中的编程约定建立

### acquire(int permits) - 批量获取许可

```java
public void acquire(int permits) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException();
    sync.acquireSharedInterruptibly(permits);
}
```

- **功能**: 从信号量获取给定数量的许可,阻塞直到所有许可都可用,或线程被中断
- **参数**: `permits` - 要获取的许可数量
- **异常**:
  - `InterruptedException` - 如果当前线程被中断
  - `IllegalArgumentException` - 如果 permits 为负

### acquireUninterruptibly(int permits) - 批量获取许可(不可中断)

```java
public void acquireUninterruptibly(int permits) {
    if (permits < 0) throw new IllegalArgumentException();
    sync.acquireShared(permits);
}
```

- **功能**: 从信号量获取给定数量的许可,阻塞直到所有许可都可用
- **参数**: `permits` - 要获取的许可数量
- **异常**: `IllegalArgumentException` - 如果 permits 为负

### tryAcquire(int permits) - 批量尝试获取

```java
public boolean tryAcquire(int permits) {
    if (permits < 0) throw new IllegalArgumentException();
    return sync.nonfairTryAcquireShared(permits) >= 0;
}
```

- **功能**: 从信号量获取给定数量的许可,仅在调用时所有许可都可用
- **参数**: `permits` - 要获取的许可数量
- **返回值**:
  - `true`: 获取了许可
  - `false`: 没有获取许可
- **异常**: `IllegalArgumentException` - 如果 permits 为负

### tryAcquire(int permits, timeout, unit) - 批量超时尝试获取

```java
public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
    throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException();
    return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
}
```

- **功能**: 从信号量获取给定数量的许可,如果在给定等待时间内所有许可都可用且当前线程未被中断
- **参数**:
  - `permits`: 要获取的许可数量
  - `timeout`: 最大等待时间
  - `unit`: 超时参数的时间单位
- **返回值**:
  - `true`: 获取了所有许可
  - `false`: 在获取所有许可之前等待时间已过
- **异常**:
  - `InterruptedException` - 如果当前线程被中断
  - `IllegalArgumentException` - 如果 permits 为负

### release(int permits) - 批量释放许可

```java
public void release(int permits) {
    if (permits < 0) throw new IllegalArgumentException();
    sync.releaseShared(permits);
}
```

- **功能**: 释放给定数量的许可,将其返回到信号量
- **参数**: `permits` - 要释放的许可数量
- **行为**:
  - 释放给定数量的许可,增加可用许可数
  - 如果有线程正在尝试获取许可,则选择一个并给予刚刚释放的许可
  - 如果可用许可数满足该线程的请求,则该线程被(重新)启用
  - 否则线程将等待直到有足够的许可可用
  - 如果在该线程的请求被满足后仍有许可可用,则这些许可依次分配给其他尝试获取许可的线程
- **异常**: `IllegalArgumentException` - 如果 permits 为负

### availablePermits() - 获取可用许可数

```java
public int availablePermits() {
    return sync.getPermits();
}
```

- **功能**: 返回此信号量中当前可用的许可数量
- **用途**: 通常用于调试和测试

### drainPermits() - 排空许可

```java
public int drainPermits() {
    return sync.drainPermits();
}
```

- **功能**: 获取并返回所有立即可用的许可
- **返回值**: 获取的许可数量

### reducePermits(int reduction) - 减少许可

```java
protected void reducePermits(int reduction) {
    if (reduction < 0) throw new IllegalArgumentException();
    sync.reducePermits(reduction);
}
```

- **功能**: 将可用许可数量减少指示的减少量
- **用途**: 在使用信号量跟踪变得不可用的资源的子类中很有用
- **区别**: 与 `acquire()` 不同,它不阻塞等待许可变得可用
- **异常**: `IllegalArgumentException` - 如果 reduction 为负

### isFair() - 检查公平性

```java
public boolean isFair() {
    return sync instanceof FairSync;
}
```

- **功能**: 返回此信号量是否设置了公平性为 true
- **返回值**: 如果此信号量设置了公平性为 true 则返回 `true`

### hasQueuedThreads() - 检查是否有等待线程

```java
public final boolean hasQueuedThreads() {
    return sync.hasQueuedThreads();
}
```

- **功能**: 查询是否有线程正在等待获取
- **返回值**: 如果可能有其他线程正在等待获取锁则返回 `true`
- **注意**: 因为取消可能随时发生,`true` 返回不保证任何其他线程将获取

### getQueueLength() - 获取等待线程数

```java
public final int getQueueLength() {
    return sync.getQueueLength();
}
```

- **功能**: 返回正在等待获取的线程数的估计值
- **返回值**: 正在等待此锁的线程的估计数
- **注意**: 该值只是一个估计值,因为在此方法遍历内部数据结构时线程数可能会动态变化

### getQueuedThreads() - 获取等待线程集合

```java
protected Collection<Thread> getQueuedThreads() {
    return sync.getQueuedThreads();
}
```

- **功能**: 返回包含可能正在等待获取的线程的集合
- **返回值**: 线程集合
- **注意**:
  - 因为实际线程集可能在构造结果时动态变化
  - 返回的集合只是一个尽力而为的估计
  - 返回集合的元素没有特定顺序

### toString() - 字符串表示

```java
public String toString() {
    return super.toString() + "[Permits = " + sync.getPermits() + "]";
}
```

- **功能**: 返回标识此信号量及其状态的字符串
- **格式**: `Semaphore[Permits = n]`

## 内存一致性效果

线程中调用"release"方法(如 `release()`)之前的操作与另一个线程中相应"acquire"方法(如 `acquire()`)成功返回之后的操作之间存在 **happens-before** 关系。

## 使用示例

### 示例 1: 资源池控制

```java
class Pool {
  private static final int MAX_AVAILABLE = 100;
  private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

  public Object getItem() throws InterruptedException {
    available.acquire();
    return getNextAvailableItem();
  }

  public void putItem(Object x) {
    if (markAsUnused(x))
      available.release();
  }

  protected Object[] items = ...;
  protected boolean[] used = new boolean[MAX_AVAILABLE];

  protected synchronized Object getNextAvailableItem() {
    for (int i = 0; i < MAX_AVAILABLE; ++i) {
      if (!used[i]) {
         used[i] = true;
         return items[i];
      }
    }
    return null;
  }

  protected synchronized boolean markAsUnused(Object item) {
    for (int i = 0; i < MAX_AVAILABLE; ++i) {
      if (item == items[i]) {
         if (used[i]) {
           used[i] = false;
           return true;
         } else
           return false;
      }
    }
    return false;
  }
}
```

**说明**:
- 使用信号量控制对项目池的访问
- 获取项目前必须先获取许可
- 返回项目时释放许可
- 信号量封装了限制访问池所需的同步

### 示例 2: 二元信号量(互斥锁)

```java
Semaphore mutex = new Semaphore(1);

// 线程 1
mutex.acquire();
try {
    // 临界区
} finally {
    mutex.release();
}

// 线程 2
mutex.acquire();
try {
    // 临界区
} finally {
    mutex.release();
}
```

**说明**:
- 初始化为 1 的信号量可以用作互斥锁
- 与 Lock 不同,可以由非所有者线程释放
- 适用于死锁恢复等特殊情况

## 关键点总结

1. **AQS 实现**: 基于 AbstractQueuedSynchronizer,使用 state 表示许可数
2. **公平性**: 支持公平和非公平两种模式
3. **非公平模式**: 允许插队,吞吐量高
4. **公平模式**: 保证 FIFO 顺序,防止饥饿
5. **批量操作**: 支持一次获取/释放多个许可
6. **CAS 操作**: 使用 CAS 循环保证原子性
7. **二元信号量**: 初始化为 1 可用作互斥锁
8. **内存一致性**: 保证 happens-before 关系

## 相关类

- `AbstractQueuedSynchronizer`: 同步器框架
- `ReentrantLock`: 可重入锁
- `CountDownLatch`: 倒计时锁存器
- `CyclicBarrier`: 循环屏障
