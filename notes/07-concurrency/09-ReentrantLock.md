# ReentrantLock 源码解读

## 类概述

`ReentrantLock` 是一个可重入的互斥锁,具有与使用 `synchronized` 方法和语句访问的隐式监视器锁相同的基本行为和语义,但具有扩展功能。

```java
public class ReentrantLock implements Lock, java.io.Serializable {
```

## 核心特性

### 1. 可重入性

- ReentrantLock 由最后成功锁定但尚未解锁的线程 **拥有**
- 线程调用 `lock()` 时,如果锁未被另一个线程持有,将返回并成功获取锁
- 如果当前线程已经拥有锁,则方法立即返回
- 可以通过 `isHeldByCurrentThread()` 和 `getHoldCount()` 方法检查

### 2. 公平性(Fairness)

构造方法接受可选的公平性参数:

- **fair = false**: 不保证任何特定的访问顺序
  - 允许 **插队(barging)**
  - `tryLock()` 不遵守公平性设置
- **fair = true**: 在竞争下,锁倾向于将访问权授予等待时间最长的线程
  - 使用公平锁的程序可能显示较低的整体吞吐量
  - 但获取锁的时间变化较小,保证没有饥饿
  - **注意**: 锁的公平性不保证线程调度的公平性

### 3. 最大递归限制

- 此锁支持同一线程最多 2147483647 次递归锁
- 超过此限制的尝试将导致锁定方法抛出 `Error`

### 4. 推荐使用模式

```java
class X {
  private final ReentrantLock lock = new ReentrantLock();
  // ...

  public void m() {
    lock.lock();  // 阻塞直到条件成立
    try {
      // ... 方法体
    } finally {
      lock.unlock()
    }
  }
}
```

## 核心实现

### Sync - 同步控制基类

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = -5179523762034025860L;

    /**
     * 执行 Lock#lock。子类化的主要原因是允许非公平版本的快速路径。
     */
    abstract void lock();

    /**
     * 执行非公平的 tryLock。tryAcquire 在子类中实现,
     * 但两者都需要非公平的 try 用于 trylock 方法。
     */
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();

        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }

        return false;
    }

    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }

    protected final boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    final ConditionObject newCondition() {
        return new ConditionObject();
    }

    // 从外部类中继的方法

    final Thread getOwner() {
        return getState() == 0 ? null : getExclusiveOwnerThread();
    }

    final int getHoldCount() {
        return isHeldExclusively() ? getState() : 0;
    }

    final boolean isLocked() {
        return getState() != 0;
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        setState(0); // 重置为未锁定状态
    }
}
```

#### AQS 状态使用

- 使用 AQS 的 `state` 字段表示锁上的持有次数
- `state = 0`: 锁未被持有
- `state > 0`: 锁被持有,值表示重入次数

#### nonfairTryAcquire - 非公平尝试获取

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();

    if (c == 0) {
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }

    return false;
}
```

- **返回值**: `true` 表示获取成功,`false` 表示获取失败
- **逻辑**:
  1. 获取当前线程和锁状态
  2. 如果锁未被持有(c == 0):
     - 尝试 CAS 设置状态
     - 成功则设置拥有者线程
  3. 如果当前线程是锁的拥有者:
     - 计算新的持有次数
     - 检查溢出
     - 设置新状态(可重入)
  4. 否则返回 false
- **特点**: 不检查是否有等待线程,直接尝试获取

#### tryRelease - 尝试释放

```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

- **返回值**: `true` 表示锁被完全释放,`false` 表示锁仍被持有
- **逻辑**:
  1. 计算释放后的状态
  2. 检查当前线程是否是锁的拥有者
  3. 如果状态变为 0,清除拥有者线程
  4. 设置新状态
- **异常**: `IllegalMonitorStateException` - 如果当前线程不持有锁

### NonfairSync - 非公平版本

```java
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = 7316153563782823691L;

    final void lock() {
        if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
    }

    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
}
```

#### lock() 实现

```java
final void lock() {
    if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            acquire(1);
}
```

- **快速路径**: 直接尝试 CAS 获取锁
- **慢速路径**: 如果快速路径失败,调用 AQS 的 `acquire()` 方法
- **特点**: 允许插队,不保证获取顺序

### FairSync - 公平版本

```java
static final class FairSync extends Sync {
    private static final long serialVersionUID = -3000897897090466540L;

    final void lock() {
        acquire(1);
    }

    /**
     * tryAcquire 的公平版本。除非是递归调用或没有等待者或是第一个,
     * 否则不授予访问权。
     */
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

#### lock() 实现

```java
final void lock() {
    acquire(1);
}
```

- **特点**: 直接调用 AQS 的 `acquire()` 方法
- **效果**: 保证公平性,按 FIFO 顺序获取锁

#### tryAcquire - 公平尝试获取

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

- **与非公平版本的区别**:
  - 在获取锁前检查 `hasQueuedPredecessors()`
  - 如果有前驱线程在等待,则不获取锁
- **公平性保证**: 保证 FIFO 获取顺序

## 核心方法

### 构造方法

#### 默认构造方法(非公平)

```java
public ReentrantLock() {
    sync = new NonfairSync();
}
```

- **特点**: 默认使用非公平策略
- **优势**: 更高的吞吐量

#### 公平构造方法

```java
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

- **参数**: `fair` - 如果为 true,则锁应使用公平排序策略
- **特点**: 根据公平参数选择 Sync 实现

### lock() - 获取锁

```java
public void lock() {
    sync.lock();
}
```

- **功能**: 获取锁
- **行为**:
  - 如果锁未被另一个线程持有,立即获取并返回,将锁持有次数设置为 1
  - 如果当前线程已经持有锁,则持有次数增加 1,方法立即返回
  - 如果锁被另一个线程持有,当前线程被禁用并休眠,直到获取锁
- **特点**: 支持可重入

### lockInterruptibly() - 可中断获取锁

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}
```

- **功能**: 获取锁,除非当前线程被中断
- **行为**:
  - 如果锁未被另一个线程持有,立即获取并返回
  - 如果当前线程已经持有锁,则持有次数增加 1,方法立即返回
  - 如果锁被另一个线程持有,当前线程被禁用并休眠,直到:
    - 当前线程获取锁
    - 其他线程中断当前线程
- **异常**: `InterruptedException` - 如果当前线程被中断
- **特点**: 优先响应中断而不是正常或可重入获取锁

### tryLock() - 尝试获取锁

```java
public boolean tryLock() {
    return sync.nonfairTryAcquire(1);
}
```

- **功能**: 仅在调用时锁未被另一个线程持有时获取锁
- **返回值**:
  - `true`: 锁是空闲的并被当前线程获取,或锁已被当前线程持有
  - `false`: 否则
- **行为**:
  - 如果锁未被另一个线程持有,立即获取并返回 `true`,将锁持有次数设置为 1
  - 即使锁已设置为使用公平排序策略,`tryLock()` 也会立即获取锁(如果有可用)
  - 如果当前线程已经持有锁,则持有次数增加 1,方法返回 `true`
  - 如果锁被另一个线程持有,方法立即返回 `false`
- **公平性**: 不遵守公平性设置,允许插队

### tryLock(timeout, unit) - 超时尝试获取锁

```java
public boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
```

- **功能**: 如果在给定等待时间内锁未被另一个线程持有且当前线程未被中断,则获取锁
- **参数**:
  - `timeout`: 最大等待时间
  - `unit`: 超时参数的时间单位
- **返回值**:
  - `true`: 锁是空闲的并被当前线程获取,或锁已被当前线程持有
  - `false`: 在可以获取锁之前等待时间已过
- **异常**:
  - `InterruptedException` - 如果当前线程被中断
  - `NullPointerException` - 如果时间单位为 null
- **公平性**: 如果锁已设置为使用公平排序策略,则可用锁不会在其他线程正在等待锁时被获取
  - 这与 `tryLock()` 方法相反

### unlock() - 释放锁

```java
public void unlock() {
    sync.release(1);
}
```

- **功能**: 尝试释放此锁
- **行为**:
  - 如果当前线程是此锁的持有者,则持有次数递减
  - 如果持有次数现在为零,则锁被释放
  - 如果当前线程不是此锁的持有者,则抛出 `IllegalMonitorStateException`
- **异常**: `IllegalMonitorStateException` - 如果当前线程不持有此锁

### newCondition() - 创建条件变量

```java
public Condition newCondition() {
    return sync.newCondition();
}
```

- **功能**: 返回用于此 Lock 实例的 Condition 实例
- **返回值**: Condition 对象
- **特点**:
  - 返回的 Condition 实例支持与内置监视器锁相同的用法
  - 支持 `wait()`、`notify()` 和 `notifyAll()`
  - 等待线程按 FIFO 顺序被信号通知

### getHoldCount() - 获取持有次数

```java
public int getHoldCount() {
    return sync.getHoldCount();
}
```

- **功能**: 查询当前线程在此锁上的持有次数
- **返回值**: 当前线程在此锁上的持有次数,如果当前线程不持有此锁则为 0
- **用途**: 通常仅用于测试和调试

### isHeldByCurrentThread() - 检查是否被当前线程持有

```java
public boolean isHeldByCurrentThread() {
    return sync.isHeldExclusively();
}
```

- **功能**: 查询此锁是否被当前线程持有
- **返回值**: 如果当前线程持有此锁则返回 `true`,否则返回 `false`
- **用途**: 通常用于调试和测试

### isLocked() - 检查是否被锁定

```java
public boolean isLocked() {
    return sync.isLocked();
}
```

- **功能**: 查询此锁是否被任何线程持有
- **返回值**: 如果任何线程持有此锁则返回 `true`,否则返回 `false`
- **用途**: 设计用于系统状态监控,而不是同步控制

### isFair() - 检查公平性

```java
public final boolean isFair() {
    return sync instanceof FairSync;
}
```

- **功能**: 返回此锁是否设置了公平性为 true
- **返回值**: 如果此锁设置了公平性为 true 则返回 `true`

### getOwner() - 获取拥有者线程

```java
protected Thread getOwner() {
    return sync.getOwner();
}
```

- **功能**: 返回当前拥有此锁的线程,如果未被拥有则返回 `null`
- **返回值**: 拥有者线程,如果未被拥有则为 `null`
- **注意**: 当此方法被非拥有者线程调用时,返回值反映当前锁状态的最佳努力近似值

### hasQueuedThreads() - 检查是否有等待线程

```java
public final boolean hasQueuedThreads() {
    return sync.hasQueuedThreads();
}
```

- **功能**: 查询是否有线程正在等待获取此锁
- **返回值**: 如果可能有其他线程正在等待获取此锁则返回 `true`
- **注意**: 因为取消可能随时发生,`true` 返回不保证任何其他线程将获取此锁

### hasQueuedThread(Thread) - 检查指定线程是否在等待

```java
public final boolean hasQueuedThread(Thread thread) {
    return sync.isQueued(thread);
}
```

- **功能**: 查询给定线程是否正在等待获取此锁
- **参数**: `thread` - 线程
- **返回值**: 如果给定线程正在排队等待此锁则返回 `true`
- **异常**: `NullPointerException` - 如果线程为 null
- **注意**: 因为取消可能随时发生,`true` 返回不保证此线程将获取此锁

### getQueueLength() - 获取等待线程数

```java
public final int getQueueLength() {
    return sync.getQueueLength();
}
```

- **功能**: 返回正在等待获取此锁的线程数的估计值
- **返回值**: 正在等待此锁的线程的估计数
- **注意**: 该值只是一个估计值,因为在此方法遍历内部数据结构时线程数可能会动态变化

### getQueuedThreads() - 获取等待线程集合

```java
protected Collection<Thread> getQueuedThreads() {
    return sync.getQueuedThreads();
}
```

- **功能**: 返回包含可能正在等待获取此锁的线程的集合
- **返回值**: 线程集合
- **注意**:
  - 因为实际线程集可能在构造结果时动态变化
  - 返回的集合只是一个尽力而为的估计
  - 返回集合的元素没有特定顺序

### hasWaiters(Condition) - 检查条件上是否有等待线程

```java
public boolean hasWaiters(Condition condition) {
    if (condition == null)
            throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
    return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```

- **功能**: 查询是否有线程正在等待与此锁关联的给定条件
- **参数**: `condition` - 条件
- **返回值**: 如果有任何等待线程则返回 `true`
- **异常**:
  - `IllegalMonitorStateException` - 如果此锁未被持有
  - `IllegalArgumentException` - 如果给定条件不与此锁关联
  - `NullPointerException` - 如果条件为 null
- **注意**: 因为超时和中断可能随时发生,`true` 返回不保证未来的 `signal()` 将唤醒任何线程

### getWaitQueueLength(Condition) - 获取条件等待线程数

```java
public int getWaitQueueLength(Condition condition) {
    if (condition == null)
            throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
    return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```

- **功能**: 返回正在等待与此锁关联的给定条件的线程数的估计值
- **参数**: `condition` - 条件
- **返回值**: 等待线程的估计数
- **异常**:
  - `IllegalMonitorStateException` - 如果此锁未被持有
  - `IllegalArgumentException` - 如果给定条件不与此锁关联
  - `NullPointerException` - 如果条件为 null
- **注意**: 因为超时和中断可能随时发生,估计值仅作为实际等待者数的上界

### getWaitingThreads(Condition) - 获取条件等待线程集合

```java
protected Collection<Thread> getWaitingThreads(Condition condition) {
    if (condition == null)
            throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
    return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
}
```

- **功能**: 返回包含可能正在等待与此锁关联的给定条件的线程的集合
- **参数**: `condition` - 条件
- **返回值**: 线程集合
- **异常**:
  - `IllegalMonitorStateException` - 如果此锁未被持有
  - `IllegalArgumentException` - 如果给定条件不与此锁关联
  - `NullPointerException` - 如果条件为 null
- **注意**:
  - 因为实际线程集可能在构造结果时动态变化
  - 返回的集合只是一个尽力而为的估计
  - 返回集合的元素没有特定顺序

### toString() - 字符串表示

```java
public String toString() {
    Thread o = sync.getOwner();
    return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
}
```

- **功能**: 返回标识此锁及其锁状态的字符串
- **格式**:
  - `ReentrantLock[Unlocked]` - 未锁定
  - `ReentrantLock[Locked by thread xxx]` - 已锁定

## 使用示例

### 示例 1: 基本使用

```java
class X {
  private final ReentrantLock lock = new ReentrantLock();

  public void m() {
    lock.lock();
    try {
      // ... 方法体
    } finally {
      lock.unlock();
    }
  }
}
```

### 示例 2: 使用条件变量

```java
class BoundedBuffer {
  final Lock lock = new ReentrantLock();
  final Condition notFull = lock.newCondition();
  final Condition notEmpty = lock.newCondition();

  final Object[] items = new Object[100];
  int putptr, takeptr, count;

  public void put(Object x) throws InterruptedException {
    lock.lock();
    try {
      while (count == items.length)
        notFull.await();
      items[putptr] = x;
      if (++putptr == items.length) putptr = 0;
      ++count;
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
  }

  public Object take() throws InterruptedException {
    lock.lock();
    try {
      while (count == 0)
        notEmpty.await();
      Object x = items[takeptr];
      if (++takeptr == items.length) takeptr = 0;
      --count;
      notFull.signal();
      return x;
    } finally {
      lock.unlock();
    }
  }
}
```

### 示例 3: 公平锁

```java
ReentrantLock fairLock = new ReentrantLock(true);

fairLock.lock();
try {
  // 临界区
} finally {
  fairLock.unlock();
}
```

### 示例 4: 超时获取

```java
ReentrantLock lock = new ReentrantLock();
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
      // 成功获取锁
    } finally {
      lock.unlock();
    }
} else {
    // 获取锁失败
}
```

## 与 synchronized 的区别

| 特性 | ReentrantLock | synchronized |
|------|--------------|-------------|
| 可重入 | 支持 | 支持 |
| 公平性 | 可选公平/非公平 | 非公平 |
| 中断响应 | lockInterruptibly() | 不支持 |
| 超时获取 | tryLock(timeout) | 不支持 |
| 条件变量 | Condition | wait/notify |
| 获取状态检查 | isLocked()、isHeldByCurrentThread() | 不支持 |
| 灵活性 | 高 | 低 |

## 关键点总结

1. **AQS 实现**: 基于 AbstractQueuedSynchronizer,使用 state 表示持有次数
2. **可重入性**: 支持同一线程多次获取锁
3. **公平性**: 支持公平和非公平两种模式
4. **非公平模式**: 允许插队,吞吐量高
5. **公平模式**: 保证 FIFO 顺序,防止饥饿
6. **条件变量**: 支持 Condition,功能比 wait/notify 更强大
7. **监控方法**: 提供丰富的状态检查方法
8. **最大递归**: 支持最多 2147483647 次递归

## 相关类

- `Lock`: 锁接口
- `AbstractQueuedSynchronizer`: 同步器框架
- `Condition`: 条件变量接口
- `ReentrantReadWriteLock`: 可重入读写锁
- `synchronized`: 内置监视器锁
