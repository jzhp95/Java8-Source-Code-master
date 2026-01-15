# Java并发编程(JUC)体系源码深度解析——LockSupport详解

## 十一、LockSupport详解

### 11.1 LockSupport概述

#### 11.1.1 LockSupport的引入背景

在Java并发编程中,线程的阻塞和唤醒是基本操作。传统的做法是使用Object的wait/notify/notifyAll方法,但这些方法必须在synchronized块中使用,而且只能与对象的监视器配合使用,灵活性有限。

Java 5引入的LockSupport类提供了一种更灵活的线程阻塞和唤醒机制。LockSupport不依赖于对象的监视器,可以在任何地方使用,而且支持更精细的控制,如设置阻塞的截止时间、支持中断等。

LockSupport的核心优势包括:

**不依赖synchronized**: 不需要在synchronized块中使用,可以在任何地方调用。

**支持中断**: park方法可以被中断,并且可以检查中断状态。

**支持超时**: parkNanos和parkUntil方法支持设置超时时间。

**基于许可证**: 使用许可证(permit)机制,更加灵活。

**高性能**: 底层使用Unsafe类直接操作线程,性能优于Object的wait/notify。

#### 11.1.2 LockSupport的设计理念

LockSupport的设计理念基于"许可证"(Permit)的概念。每个线程都有一个许可证,初始为0。当调用park方法时,如果许可证为0,线程会阻塞;如果许可证为1,线程会消耗许可证并立即返回。当调用unpark方法时,会将许可证设置为1,如果有线程在等待,则唤醒该线程。

LockSupport的核心特点包括:

**基于许可证**: 使用permit机制,而不是监视器。

**线程关联**: park和unpark方法都关联到具体的线程,而不是对象。

**非阻塞唤醒**: unpark方法可以提前调用,即使线程还没有park,也能保证线程下次park时立即返回。

**内存语义**: park和unpark方法具有happens-before关系,保证内存可见性。

```java
public class LockSupport {
    // Unsafe实例
    private static final Unsafe UNSAFE;
    
    // permit字段的偏移量
    private static final long PARKBLOCKER;
    
    static {
        try {
            UNSAFE = Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            PARKBLOCKER = UNSAFE.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
```

### 11.2 LockSupport核心API详解

#### 11.2.1 park方法

```java
// 阻塞当前线程,直到被unpark或中断
public static void park() {
    UNSAFE.park(false, 0L);
}

// 阻塞当前线程,直到被unpark、中断或超时
public static void parkNanos(long nanos) {
    if (nanos > 0)
        UNSAFE.park(false, System.nanoTime() + nanos);
}

// 阻塞当前线程,直到被unpark、中断或到达截止时间
public static void parkUntil(long deadline) {
    UNSAFE.park(false, deadline);
}

// 阻塞当前线程,并设置blocker
public static void park(Object blocker) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    UNSAFE.park(false, 0L);
    setBlocker(t, null);
}

// 阻塞当前线程,并设置blocker,支持超时
public static void parkNanos(Object blocker, long nanos) {
    if (nanos > 0) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, System.nanoTime() + nanos);
        setBlocker(t, null);
    }
}

// 阻塞当前线程,并设置blocker,支持截止时间
public static void parkUntil(Object blocker, long deadline) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    UNSAFE.park(false, deadline);
    setBlocker(t, null);
}
```

**使用示例:**

```java
// 基本park
LockSupport.park();

// parkNanos
LockSupport.parkNanos(1000000000); // 阻塞1秒

// parkUntil
long deadline = System.currentTimeMillis() + 5000;
LockSupport.parkUntil(deadline); // 阻塞5秒

// 带blocker的park
Object blocker = new Object();
LockSupport.park(blocker);
```

#### 11.2.2 unpark方法

```java
// 唤醒指定的线程
public static void unpark(Thread thread) {
    if (thread != null)
        UNSAFE.unpark(thread);
}
```

**使用示例:**

```java
Thread thread = new Thread(() -> {
    System.out.println("Thread started");
    LockSupport.park();
    System.out.println("Thread resumed");
});

thread.start();

try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

LockSupport.unpark(thread);
```

#### 11.2.3 blocker机制

blocker机制用于调试和监控,它记录了线程被阻塞的原因。

```java
private static void setBlocker(Thread t, Object arg) {
    UNSAFE.putObject(t, PARKBLOCKER, arg);
}

// 获取当前线程的blocker
public static Object getBlocker(Thread t) {
    if (t == null)
        throw new NullPointerException();
    return UNSAFE.getObjectVolatile(t, PARKBLOCKER);
}
```

**使用示例:**

```java
public class BlockerDemo {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Thread started");
            LockSupport.park(new BlockerDemo());
            System.out.println("Thread resumed");
        });
        
        thread.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Object blocker = LockSupport.getBlocker(thread);
        System.out.println("Blocker: " + blocker);
        
        LockSupport.unpark(thread);
    }
}
```

### 11.3 LockSupport内部实现原理

#### 11.3.1 Unsafe的park方法

LockSupport的park方法最终调用Unsafe的park方法:

```java
public native void park(boolean isAbsolute, long time);
```

**参数说明:**

- isAbsolute: 如果为true,time表示绝对时间(毫秒);如果为false,time表示相对时间(纳秒)
- time: 阻塞的时间,0表示无限期阻塞

**park方法的语义:**

1. 如果当前线程的permit为1,消耗permit并立即返回
2. 如果permit为0,阻塞当前线程,直到:
   - 其他线程调用unpark唤醒当前线程
   - 当前线程被中断
   - 超时(如果指定了时间)

#### 11.3.2 Unsafe的unpark方法

```java
public native void unpark(Thread thread);
```

**unpark方法的语义:**

1. 如果线程的permit为0,将permit设置为1,如果线程正在park,则唤醒它
2. 如果线程的permit已经为1,不做任何操作
3. 如果线程已经结束,不做任何操作

**unpark的非阻塞特性:**

unpark方法可以提前调用,即使线程还没有park,也能保证线程下次park时立即返回。这是因为unpark会将permit设置为1,而park会检查permit,如果为1则消耗permit并返回。

#### 11.3.3 permit机制

permit是每个线程关联的一个计数器,初始值为0。

**park方法对permit的处理:**

```java
if (permit == 1) {
    permit = 0;
    return;
} else {
    block();
}
```

**unpark方法对permit的处理:**

```java
if (permit == 0) {
    permit = 1;
    if (thread is blocked) {
        wakeup(thread);
    }
} else {
    // permit already 1, do nothing
}
```

**permit的特性:**

1. permit最多为1,多次调用unpark不会累加
2. park消耗permit后,permit变为0
3. unpark可以提前调用,保证下次park立即返回

### 11.4 LockSupport与Object.wait/notify对比

#### 11.4.1 功能对比

| 特性 | LockSupport | Object.wait/notify |
|------|-------------|-------------------|
| 依赖synchronized | 否 | 是 |
| 线程关联 | 是 | 否 |
| 提前唤醒 | 支持 | 不支持 |
| 中断支持 | 支持 | 支持 |
| 超时支持 | 支持 | 支持 |
| 性能 | 高 | 较低 |

#### 11.4.2 使用场景对比

**LockSupport适用场景:**

- 需要在非synchronized块中阻塞线程
- 需要提前唤醒线程
- 需要高性能的线程阻塞和唤醒

```java
// LockSupport示例
LockSupport.park();
LockSupport.unpark(thread);
```

**Object.wait/notify适用场景:**

- 需要在synchronized块中阻塞线程
- 需要与对象的监视器配合使用
- 需要等待多个条件

```java
// Object.wait/notify示例
synchronized (object) {
    object.wait();
    object.notify();
}
```

### 11.5 LockSupport使用场景

#### 11.5.1 实现简单的互斥锁

```java
public class SimpleLock {
    private volatile Thread owner;
    private int count;
    
    public void lock() {
        Thread current = Thread.currentThread();
        if (owner == current) {
            count++;
            return;
        }
        while (owner != null) {
            LockSupport.park(this);
        }
        owner = current;
        count = 1;
    }
    
    public void unlock() {
        Thread current = Thread.currentThread();
        if (owner != current) {
            throw new IllegalMonitorStateException();
        }
        if (--count == 0) {
            owner = null;
            LockSupport.unpark(current);
        }
    }
}
```

#### 11.5.2 实现条件变量

```java
public class SimpleCondition {
    private final Queue<Thread> waiters = new LinkedList<>();
    
    public void await() {
        Thread current = Thread.currentThread();
        waiters.add(current);
        LockSupport.park(this);
        waiters.remove(current);
    }
    
    public void signal() {
        Thread waiter = waiters.peek();
        if (waiter != null) {
            LockSupport.unpark(waiter);
        }
    }
    
    public void signalAll() {
        for (Thread waiter : waiters) {
            LockSupport.unpark(waiter);
        }
    }
}
```

#### 11.5.3 实现自旋锁

```java
public class SpinLock {
    private volatile Thread owner;
    
    public void lock() {
        Thread current = Thread.currentThread();
        while (owner != null) {
            LockSupport.parkNanos(this, 1000); // 自旋1微秒
        }
        owner = current;
    }
    
    public void unlock() {
        Thread current = Thread.currentThread();
        if (owner != current) {
            throw new IllegalMonitorStateException();
        }
        owner = null;
        LockSupport.unpark(current);
    }
}
```

### 11.6 LockSupport在JUC中的应用

#### 11.6.1 AQS中的应用

AQS使用LockSupport来实现线程的阻塞和唤醒:

```java
// AQS中的parkAndCheckInterrupt方法
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
}

// AQS中的unparkSuccessor方法
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);
    
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

#### 11.6.2 Condition中的应用

ConditionObject使用LockSupport来实现await和signal:

```java
// ConditionObject中的await方法
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}

// ConditionObject中的signal方法
public final void signal() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}
```

### 11.7 LockSupport最佳实践

#### 11.7.1 正确处理中断

```java
public class InterruptHandling {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Thread started");
            LockSupport.park();
            if (Thread.interrupted()) {
                System.out.println("Thread was interrupted");
            } else {
                System.out.println("Thread was unparked");
            }
        });
        
        thread.start();
        
        try {
            Thread.sleep(1000);
            thread.interrupt();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 11.7.2 设置合理的超时

```java
public class TimeoutHandling {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Thread started");
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                LockSupport.parkUntil(deadline);
                if (System.currentTimeMillis() >= deadline) {
                    System.out.println("Timeout");
                    break;
                }
            }
            System.out.println("Thread finished");
        });
        
        thread.start();
    }
}
```

#### 11.7.3 使用blocker进行调试

```java
public class BlockerDebug {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Thread started");
            LockSupport.park(new BlockerDebug());
            System.out.println("Thread resumed");
        });
        
        thread.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Object blocker = LockSupport.getBlocker(thread);
        System.out.println("Thread is blocked by: " + blocker);
        
        LockSupport.unpark(thread);
    }
}
```

### 11.8 小结

LockSupport是Java并发编程的基础工具,用于线程的阻塞和唤醒,它是AQS、Condition等实现的基础。

**核心要点总结:**

第一,**不依赖synchronized**: LockSupport不依赖于synchronized,可以在任何地方使用。

第二,**基于许可证**: 使用permit机制,更加灵活。

第三,**线程关联**: park和unpark方法都关联到具体的线程。

第四,**非阻塞唤醒**: unpark可以提前调用,保证线程下次park时立即返回。

第五,**内存语义**: park和unpark具有happens-before关系,保证内存可见性。

第六,**广泛应用**: LockSupport是AQS、Condition、ForkJoinPool等的基础。

第七,**最佳实践**: 正确处理中断、设置合理的超时、使用blocker进行调试。
