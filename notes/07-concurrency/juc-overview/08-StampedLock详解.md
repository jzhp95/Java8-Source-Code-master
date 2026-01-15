# Java并发编程(JUC)体系源码深度解析——StampedLock详解

## 八、StampedLock详解

### 8.1 StampedLock概述

#### 8.1.1 为什么需要StampedLock

在Java 8之前,ReentrantReadWriteLock是实现读写锁的标准方式。虽然ReadWriteLock在"读多写少"的场景下性能优于互斥锁,但它存在一些性能瓶颈:

第一,**写锁饥饿问题**: 在高并发读场景下,如果一直有读锁持有,写锁可能长时间无法获取。

第二,**读锁互斥**: 读锁之间是共享的,但读锁与写锁是互斥的。即使读操作只是读取数据,也需要获取读锁,这限制了并发性能。

第三,**乐观读不支持**: ReadWriteLock不支持乐观读,每次读取都必须获取锁,即使数据不会被修改。

Java 8引入的StampedLock解决了这些问题。StampedLock采用了一种全新的锁设计,支持三种模式:写锁、悲观读锁和乐观读。乐观读是StampedLock的核心创新,它允许在没有锁的情况下读取数据,只在最后验证数据是否被修改。

#### 8.1.2 StampedLock的设计理念

StampedLock的设计理念基于"版本戳"的概念。每次获取锁时,都会返回一个"印章"(stamp),这个印章是一个long类型的值,用于表示锁的状态和版本。释放锁时,需要传入获取锁时返回的印章,以确保操作的一致性。

StampedLock的核心特点包括:

**三种锁模式**:
- 写锁(Write Lock): 独占锁,与悲观读锁互斥
- 悲观读锁(Pessimistic Read Lock): 共享锁,与写锁互斥
- 乐观读(Optimistic Read): 不加锁,读取后验证版本

**不可重入**: StampedLock是不可重入的,同一个线程不能重复获取同一把锁。

**性能优化**: 乐观读避免了获取锁的开销,在读多写少的场景下性能显著优于ReadWriteLock。

```java
public class StampedLock implements java.io.Serializable {
    // 锁状态
    private transient volatile long state;
    
    // 读者溢出
    private transient int readerOverflow;
    
    // 等待队列
    private transient volatile Node head;
    private transient volatile Node tail;
    
    // 写锁持有者
    private transient Thread writer;
    
    // 写锁重入次数
    private transient int writeHoldCount;
}
```

### 8.2 StampedLock核心API详解

#### 8.2.1 写锁操作

写锁是独占锁,同一时刻只能有一个线程持有写锁。

```java
// 获取写锁,阻塞直到成功
public long writeLock() {
    long s, next;
    return ((((s = state) & ABITS) == 0L &&
            U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
            next : acquireWrite(false, 0L));
}

// 尝试获取写锁,立即返回
public long tryWriteLock() {
    long s, next;
    return ((((s = state) & ABITS) == 0L &&
            U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
            next : 0L);
}

// 带超时的获取写锁
public long tryWriteLock(long time, TimeUnit unit) {
    long nanos = unit.toNanos(time);
    if (!Thread.interrupted()) {
        long next;
        if (((next = tryWriteLock()) != 0L))
            return next;
        if (nanos <= 0L)
            return 0L;
        long deadline = System.nanoTime() + nanos;
        long startTime = 0L;
        long s;
        while (((s = state) & ABITS) != 0L) {
            if (nanos <= SPIN_FOR_TIMEOUT_THRESHOLD)
                LockSupport.parkNanos(this, nanos);
            else
                LockSupport.parkUntil(this, deadline);
            if (Thread.interrupted())
                throw new InterruptedException();
            if ((nanos = deadline - System.nanoTime()) <= 0L)
                return 0L;
        }
        if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
            return next;
    }
    throw new InterruptedException();
}

// 释放写锁
public void unlockWrite(long stamp) {
    WNode h;
    if (stamp != WBIT && (state & SBITS) != stamp)
        throw new IllegalMonitorStateException();
    state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
    if ((h = whead) != null && h.status != 0)
        release(h);
}
```

**使用示例:**

```java
StampedLock lock = new StampedLock();

// 获取写锁
long stamp = lock.writeLock();
try {
    // 修改共享数据
    data = newData;
} finally {
    lock.unlockWrite(stamp);
}

// 尝试获取写锁
long stamp = lock.tryWriteLock();
if (stamp != 0L) {
    try {
        data = newData;
    } finally {
        lock.unlockWrite(stamp);
    }
} else {
    // 获取失败,执行其他逻辑
    handleFailure();
}
```

#### 8.2.2 悲观读锁操作

悲观读锁是共享锁,多个线程可以同时持有悲观读锁,但与写锁互斥。

```java
// 获取悲观读锁,阻塞直到成功
public long readLock() {
    long s = state, next;
    return ((s & ABITS) == 0L && U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
           next : acquireRead(false, 0L);
}

// 尝试获取悲观读锁,立即返回
public long tryReadLock() {
    long s, next;
    return ((((s = state) & ABITS) == 0L &&
            U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
            next : 0L);
}

// 带超时的获取悲观读锁
public long tryReadLock(long time, TimeUnit unit) {
    long nanos = unit.toNanos(time);
    if (!Thread.interrupted()) {
        long next;
        if (((next = tryReadLock()) != 0L))
            return next;
        if (nanos <= 0L)
            return 0L;
        long deadline = System.nanoTime() + nanos;
        long startTime = 0L;
        long s;
        while (((s = state) & ABITS) != 0L) {
            if (nanos <= SPIN_FOR_TIMEOUT_THRESHOLD)
                LockSupport.parkNanos(this, nanos);
            else
                LockSupport.parkUntil(this, deadline);
            if (Thread.interrupted())
                throw new InterruptedException();
            if ((nanos = deadline - System.nanoTime()) <= 0L)
                return 0L;
        }
        if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
            return next;
    }
    throw new InterruptedException();
}

// 释放悲观读锁
public void unlockRead(long stamp) {
    long s, m;
    WNode h;
    while (((s = state) & SBITS) != (stamp & SBITS) ||
           ((stamp & RBITS) == 0L || ((m = (s & RBITS) - (stamp & RBITS)) == 0L))) {
        if ((m & RBITS) == 0L && (s & ABITS) == 0L)
            break;
        if ((m & RBITS) == 0L || U.compareAndSwapLong(this, STATE, s, s - RUNIT))
            break;
    }
    if ((h = whead) != null && h.status != 0)
        release(h);
}
```

**使用示例:**

```java
StampedLock lock = new StampedLock();

// 获取悲观读锁
long stamp = lock.readLock();
try {
    // 读取共享数据
    return data;
} finally {
    lock.unlockRead(stamp);
}

// 尝试获取悲观读锁
long stamp = lock.tryReadLock();
if (stamp != 0L) {
    try {
        return data;
    } finally {
        lock.unlockRead(stamp);
    }
} else {
    // 获取失败,执行其他逻辑
    return defaultData;
}
```

#### 8.2.3 乐观读操作

乐观读是StampedLock的核心特性,它允许在没有锁的情况下读取数据,只在最后验证数据是否被修改。

```java
// 尝试乐观读,返回一个stamp
public long tryOptimisticRead() {
    long s;
    return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
}

// 验证乐观读的stamp是否仍然有效
public boolean validate(long stamp) {
    U.loadFence();
    return (stamp & SBITS) == (state & SBITS);
}
```

**使用示例:**

```java
StampedLock lock = new StampedLock();

// 乐观读模式
long stamp = lock.tryOptimisticRead();
int currentData = data;
if (!lock.validate(stamp)) {
    // 数据被修改,升级为悲观读锁
    stamp = lock.readLock();
    try {
        currentData = data;
    } finally {
        lock.unlockRead(stamp);
    }
}
return currentData;
```

乐观读的执行流程:

1. 调用tryOptimisticRead获取一个stamp
2. 读取共享数据
3. 调用validate验证stamp是否仍然有效
4. 如果验证失败,升级为悲观读锁重新读取

#### 8.2.4 锁转换

StampedLock支持锁的转换,可以从读锁转换为写锁,或者从写锁转换为读锁。

```java
// 将读锁转换为写锁
public long tryConvertToWriteLock(long stamp) {
    long a = stamp & ABITS, m, s, next;
    while (((s = state) & SBITS) == (stamp & SBITS)) {
        if ((m = s & ABITS) == 0L) {
            if (a != 0L)
                break;
            if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                return next;
        }
        else if (m == WBIT) {
            if (a != m)
                break;
            return stamp;
        }
        else if (a == 0L || a >= WBIT)
            break;
        else if (m < RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s - RUNIT + WBIT))
                return stamp & ~RBITS;
        }
        else if ((next = tryIncReaderOverflow(s)) != 0L)
            return next;
    }
    return 0L;
}

// 将写锁转换为读锁
public long tryConvertToReadLock(long stamp) {
    long a = stamp & ABITS, m, s, next;
    WNode h;
    while (((s = state) & SBITS) == (stamp & SBITS)) {
        if ((m = s & ABITS) == 0L) {
            if (a != 0L)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next;
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L)
                return next;
        }
        else if (m == WBIT) {
            if (a != m)
                break;
            state = next = s + WBIT + RUNIT;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return next;
        }
        else if (a == 0L || a >= WBIT)
            break;
        else if (m < RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s - RUNIT + RUNIT))
                return stamp;
        }
        else if ((next = tryIncReaderOverflow(s)) != 0L)
            return next;
    }
    return 0L;
}
```

**使用示例:**

```java
StampedLock lock = new StampedLock();

// 读锁转换为写锁
long stamp = lock.readLock();
try {
    if (shouldUpdate()) {
        long ws = lock.tryConvertToWriteLock(stamp);
        if (ws != 0L) {
            stamp = ws;
            data = newData;
        } else {
            lock.unlockRead(stamp);
            stamp = lock.writeLock();
            try {
                data = newData;
            } finally {
                lock.unlockWrite(stamp);
            }
            return;
        }
    }
} finally {
    lock.unlock(stamp);
}
```

### 8.3 StampedLock内部实现原理

#### 8.3.1 状态变量设计

StampedLock使用一个long类型的state变量来表示锁的状态。state的位布局非常巧妙:

```java
// state的位布局
// 第63位: 写锁标记位
// 第62-17位: 读锁计数(最多2^16-1个读者)
// 第16-1位: 读锁溢出计数

private static final long WBIT  = 1L << 63;  // 写锁位
private static final long RBIT  = 1L;        // 读锁位
private static final long WBITS = WBIT << 1; // 写锁掩码
private static final long RBITS = WBITS - 1; // 读锁掩码
private static final long RFULL = RBITS >> 1; // 读锁满值
private static final long ABITS = WBITS | RBITS; // 所有锁位
private static final long SBITS = ~RBITS; // 序列位

private transient volatile long state;
```

**state的含义:**

- 当state的第63位为1时,表示写锁被持有
- 当state的第62-17位表示读锁的计数
- 当state的第16-1位用于处理读锁溢出

**示例:**

```java
// 初始状态: state = 0,表示没有锁
state = 0;

// 获取一个读锁: state = 1
state = 1;

// 获取两个读锁: state = 2
state = 2;

// 获取写锁: state = 1L << 63
state = 1L << 63;

// 释放写锁: state = 0
state = 0;
```

#### 8.3.2 乐观读实现原理

乐观读的实现非常巧妙,它利用了state的高位作为版本号:

```java
public long tryOptimisticRead() {
    long s;
    return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
}

public boolean validate(long stamp) {
    U.loadFence();
    return (stamp & SBITS) == (state & SBITS);
}
```

**tryOptimisticRead方法:**

1. 读取当前state值
2. 如果写锁位(WBIT)为0,说明没有写锁,返回state的高位(版本号)
3. 如果写锁位为1,说明有写锁,返回0表示乐观读失败

**validate方法:**

1. 使用loadFence建立内存屏障,确保读取的数据是最新的
2. 比较stamp的高位与当前state的高位是否相同
3. 如果相同,说明没有写锁介入,数据有效
4. 如果不同,说明有写锁介入,数据可能被修改

**内存屏障的作用:**

```java
U.loadFence();
```

loadFence是一个LoadLoad屏障,它确保:

1. 在loadFence之前的所有读操作都已完成
2. 在loadFence之后的所有读操作都从主内存读取最新值

这保证了validate方法能够看到最新的state值。

#### 8.3.3 写锁获取流程

```java
public long writeLock() {
    long s, next;
    return ((((s = state) & ABITS) == 0L &&
            U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
            next : acquireWrite(false, 0L));
}
```

**写锁获取流程:**

1. 快速路径: 如果state的ABITS位为0(没有锁),尝试CAS设置写锁位
2. 如果CAS成功,返回新的state值作为stamp
3. 如果CAS失败,调用acquireWrite方法进入慢速路径

**acquireWrite方法:**

```java
private long acquireWrite(boolean interruptible, long deadline) {
    WNode node = null, p;
    for (int spins = (head == null) ? SPINS : 0; ; ) {
        long m, s, ns;
        if (((m = (s = state) & ABITS) == 0L) &&
            U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
            return ns;
        else if (spins > 0) {
            if (spins < MAX_SPINS)
                spins += SPINS;
        }
        else if (p == null) {
            WNode h = head;
            if (h != null && (p = h.next) != null && p.status != 0)
                break;
            node = new WNode(WMODE, null, p);
            if (h == null || (h = head) == null || h.next == null)
                casHead(node, h);
        }
        else if (node.prev != p)
            node.prev = p;
        else if (status >= 0) {
            WNode w;
            if ((w = head) == null || w.status == 0)
                break;
            node.status = WAITING;
            if (w != head)
                break;
        }
        else {
            long time;
            if (deadline == 0L)
                time = 0L;
            else if ((time = deadline - System.nanoTime()) <= 0L)
                return cancelWaiter(node, node, false);
            Thread wt = Thread.currentThread();
            U.putObject(wt, PARKBLOCKER, this);
            node.thread = wt;
            if (p.status < 0 && (p != head || (state & ABITS) != 0L) &&
                whead == head && node.prev == p)
                U.park(false, time);
            node.thread = null;
            U.putObject(wt, PARKBLOCKER, null);
            if (interruptible && Thread.interrupted())
                return cancelWaiter(node, node, true);
        }
    }
}
```

acquireWrite方法使用了自旋+等待队列的策略:

1. 首先自旋尝试获取锁
2. 如果自旋失败,创建等待节点加入队列
3. 使用LockSupport.park阻塞当前线程
4. 被唤醒后重新尝试获取锁

#### 8.3.4 读锁获取流程

```java
public long readLock() {
    long s = state, next;
    return ((s & ABITS) == 0L && U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
           next : acquireRead(false, 0L);
}
```

读锁获取流程与写锁类似,但读锁是共享的,多个线程可以同时持有读锁。

**acquireRead方法:**

```java
private long acquireRead(boolean interruptible, long deadline) {
    WNode node = null, p;
    for (int spins = -1; ; ) {
        WNode h;
        if ((h = head) == null || h.status < 0) {
            long m, s, ns;
            if ((m = (s = state) & ABITS) < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT))
                    return ns;
            }
            else if (m >= WBIT) {
                if (spins < 0)
                    spins = SPINS;
            }
        }
        else if (spins < 0)
            spins = SPINS;
        else if (spins > 0) {
            if (spins < MAX_SPINS)
                spins += SPINS;
        }
        else if (p == null) {
            p = h.next;
            if (p == null || p.status > 0)
                p = null;
            else if (p.prev != h)
                p.prev = h;
        }
        else if (p.status == 0) {
            WNode pp = p.prev;
            if (pp == null || pp.status > 0)
                p = null;
            else {
                long time;
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, p, false);
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);
                node.thread = wt;
                if (p.status < 0 && (p != h || (state & ABITS) == 0L) &&
                    whead == h && p.prev == pp)
                    U.park(false, time);
                node.thread = null;
                U.putObject(wt, PARKBLOCKER, null);
                if (interruptible && Thread.interrupted())
                    return cancelWaiter(node, p, true);
            }
        }
    }
}
```

### 8.4 StampedLock性能对比

#### 8.4.1 与ReentrantReadWriteLock对比

**读多写少场景:**

```java
public class ReadWriteLockBenchmark {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final StampedLock stampedLock = new StampedLock();
    private volatile int data = 0;
    
    public void readWithReadWriteLock() {
        rwLock.readLock().lock();
        try {
            int temp = data;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void readWithStampedLock() {
        long stamp = stampedLock.tryOptimisticRead();
        int temp = data;
        if (!stampedLock.validate(stamp)) {
            stamp = stampedLock.readLock();
            try {
                temp = data;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
    }
}
```

在"读多写少"的场景下,StampedLock的性能显著优于ReadWriteLock:

- 乐观读避免了获取锁的开销
- 即使乐观读失败,升级为悲观读锁的开销也很小
- StampedLock的状态变量设计更紧凑,缓存友好性更好

**写多读少场景:**

在"写多读少"的场景下,StampedLock与ReadWriteLock的性能相近,因为两种锁都需要获取独占锁。

#### 8.4.2 性能测试结果

以下是一个简单的性能测试:

```java
public class LockPerformanceTest {
    private static final int THREADS = 10;
    private static final int ITERATIONS = 1000000;
    
    public static void main(String[] args) throws InterruptedException {
        testReadWriteLock();
        testStampedLock();
    }
    
    private static void testReadWriteLock() throws InterruptedException {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        AtomicInteger counter = new AtomicInteger(0);
        
        long start = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    lock.readLock().lock();
                    try {
                        counter.get();
                    } finally {
                        lock.readLock().unlock();
                    }
                }
                latch.countDown();
            }).start();
        }
        
        latch.await();
        long end = System.nanoTime();
        System.out.println("ReadWriteLock: " + (end - start) / 1_000_000 + "ms");
    }
    
    private static void testStampedLock() throws InterruptedException {
        StampedLock lock = new StampedLock();
        AtomicInteger counter = new AtomicInteger(0);
        
        long start = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    long stamp = lock.tryOptimisticRead();
                    int value = counter.get();
                    if (!lock.validate(stamp)) {
                        stamp = lock.readLock();
                        try {
                            value = counter.get();
                        } finally {
                            lock.unlockRead(stamp);
                        }
                    }
                }
                latch.countDown();
            }).start();
        }
        
        latch.await();
        long end = System.nanoTime();
        System.out.println("StampedLock: " + (end - start) / 1_000_000 + "ms");
    }
}
```

在典型的"读多写少"场景下,StampedLock的性能通常是ReadWriteLock的2-3倍。

### 8.5 StampedLock使用场景与最佳实践

#### 8.5.1 适用场景

**适合使用StampedLock的场景:**

1. **读多写少**: 乐观读可以显著提高读操作的性能
2. **读操作频繁**: 乐观读避免了锁的获取和释放
3. **数据一致性要求不高**: 乐观读需要验证,可能需要重试

**不适合使用StampedLock的场景:**

1. **写操作频繁**: 乐观读的优势无法体现
2. **需要锁重入**: StampedLock不可重入
3. **需要Condition**: StampedLock不支持Condition

#### 8.5.2 最佳实践

**1. 优先使用乐观读:**

```java
public int getData() {
    long stamp = stampedLock.tryOptimisticRead();
    int value = data;
    if (!stampedLock.validate(stamp)) {
        stamp = stampedLock.readLock();
        try {
            value = data;
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }
    return value;
}
```

**2. 正确处理锁转换:**

```java
public void updateData(int newValue) {
    long stamp = stampedLock.readLock();
    try {
        if (shouldUpdate()) {
            long ws = stampedLock.tryConvertToWriteLock(stamp);
            if (ws != 0L) {
                stamp = ws;
                data = newValue;
            } else {
                stampedLock.unlockRead(stamp);
                stamp = stampedLock.writeLock();
                try {
                    data = newValue;
                } finally {
                    stampedLock.unlockWrite(stamp);
                }
            }
        }
    } finally {
        stampedLock.unlock(stamp);
    }
}
```

**3. 避免死锁:**

```java
// 错误示例: 可能导致死锁
public void badExample() {
    long stamp1 = stampedLock.readLock();
    long stamp2 = stampedLock.readLock();
    // stampedLock不可重入,stamp2获取失败
}

// 正确示例: 使用同一个stamp
public void goodExample() {
    long stamp = stampedLock.readLock();
    try {
        // 使用同一个stamp
    } finally {
        stampedLock.unlockRead(stamp);
    }
}
```

### 8.6 小结

StampedLock是Java 8引入的高性能读写锁,它通过乐观读机制显著提高了"读多写少"场景下的性能。

**核心要点总结:**

第一,**三种锁模式**: 写锁(独占)、悲观读锁(共享)、乐观读(无锁)。

第二,**乐观读机制**: 不加锁读取数据,最后验证版本号,避免了锁的开销。

第三,**状态变量设计**: 使用long类型的state,高63位表示写锁,低62位表示读锁计数。

第四,**不可重入**: StampedLock不可重入,同一个线程不能重复获取锁。

第五,**性能优势**: 在"读多写少"场景下,性能显著优于ReadWriteLock。

第六,**使用场景**: 适合读多写少、读操作频繁的场景,不适合需要锁重入或Condition的场景。
