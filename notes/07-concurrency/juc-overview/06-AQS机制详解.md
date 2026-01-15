# Java并发编程(JUC)体系源码深度解析——AQS机制详解

## 六、AQS机制深度详解

### 6.1 AQS概述与设计哲学

#### 6.1.1 AQS的定义与地位

AbstractQueuedSynchronizer（简称AQS）是Java并发包（JUC）的核心抽象类,JDoug Lea设计的一个基于FIFO等待队列的同步器框架。AQS是Java并发编程的基石,几乎所有重要的同步器都是基于AQS构建的,包括ReentrantLock、ReentrantReadWriteLock、Semaphore、CountDownLatch、CyclicBarrier等。

AQS的核心价值在于它提供了一套通用的同步框架,将同步管理的核心逻辑（等待队列管理、状态管理、线程唤醒等）与具体同步器的特定逻辑（状态含义、获取/释放策略）分离。这种设计遵循了"开闭原则"和"依赖倒置原则",使得开发者可以轻松创建各种自定义的同步器。

AQS在JUC中的地位可以从以下几个方面理解：第一,**它是锁和同步器的基础框架**,所有基于JUC的显式锁和同步器都继承自AQS；第二,**它实现了复杂但通用的同步逻辑**,包括等待队列的维护、线程的阻塞和唤醒、公平性处理等；第三,**它提供了灵活的状态管理机制**,子类只需要定义几个关键方法即可实现完整的同步器。

#### 6.1.2 AQS的设计哲学

AQS的设计体现了几个重要的软件工程原则：

**模板方法模式**是AQS最核心的设计模式。AQS定义了获取锁和释放锁的通用流程（模板方法）,如acquire、release、acquireShared、releaseShared等。这些方法调用抽象方法tryAcquire、tryRelease、tryAcquireShared、tryReleaseShared等,这些抽象方法由子类实现。这种设计将通用的同步逻辑与特定同步器的策略分离。

**组合优于继承**的原则在AQS的实现中得到了很好的体现。AQS内部组合了等待队列、状态变量等核心组件,而不是通过继承来实现。这种设计使得同步器可以灵活地组合不同的组件,实现各种复杂的功能。

**策略模式**体现在公平锁和非公平锁的实现上。虽然公平和非公平策略的实现差异很大,但它们都遵循AQS定义的框架,只需实现不同的tryAcquire逻辑。

**状态模式**体现在state变量对不同同步器的不同含义：AQS本身不规定state的含义,而是由子类来解释。在互斥锁中,state=0表示锁可用,state>0表示锁被占用；在信号量中,state表示可用许可数；在倒计时锁存器中,state表示还需等待的计数。

#### 6.1.3 AQS的核心组件

AQS的核心组件包括以下几个部分：

**同步状态（state）**是AQS最核心的状态变量：

```java
private volatile int state;
```

state是一个volatile的int类型变量,用于表示同步器的状态。子类根据具体的同步器类型解释state的含义。通过getState、setState、compareAndSetState方法安全地访问和修改state。

**等待队列**是AQS管理等待线程的数据结构：

```java
private transient volatile Node head;
private transient volatile Node tail;
```

等待队列是一个基于Node类的FIFO双向链表。head是队列的头节点,通常是一个dummy节点；tail是队列的尾节点。新加入的节点被添加到tail后面,当线程被唤醒时从头节点开始处理。

**Node类**是队列的基本组成单元：

```java
static final class Node {
    // 等待状态常量
    static final int CANCELLED =  1;  // 节点已取消
    static final int SIGNAL    = -1;  // 后继节点需要被唤醒
    static final int CONDITION = -2;  // 节点在条件队列中等待
    static final int PROPAGATE = -3;  // 共享模式下传播
    
    volatile int waitStatus;  // 等待状态
    volatile Node prev;       // 前驱节点
    volatile Node next;       // 后继节点
    volatile Thread thread;   // 关联的线程
    Node nextWaiter;          // 条件队列中的下一个节点
    
    // 模式标记
    static final Node SHARED = new Node();
    static final Node EXCLUSIVE = null;
}
```

Node类使用volatile关键字修饰关键字段,保证多线程环境下的可见性。waitStatus字段是节点状态的核心,不同状态值表示不同的含义。

**ConditionObject**是AQS的内部类,实现了Condition接口：

```java
public class ConditionObject implements Condition, java.io.Serializable {
    private transient Node firstWaiter;
    private transient Node lastWaiter;
}
```

ConditionObject维护一个条件队列,用于实现Condition的await/signal机制。条件队列与等待队列是两个独立的队列,Condition通过transferAfterSignal方法将节点从条件队列转移到等待队列。

### 6.2 AQS状态管理详解

#### 6.2.1 状态的设计与访问

AQS的state变量被设计为一个32位的整数,使用volatile保证可见性：

```java
private volatile int state;

protected final int getState() {
    return state;
}

protected final void setState(int newState) {
    state = newState;
}

protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

这三个方法提供了对state的安全访问。getState和setState是普通的读取和写入,依赖于volatile的语义保证可见性。compareAndSetState使用CAS操作保证状态更新的原子性。

**状态语义由子类定义**,这是AQS状态管理的重要特点。不同的同步器对state有不同的解释：

在**ReentrantLock**中,state表示锁被重入的次数。state=0表示锁可用,state>0表示锁被占用,state的值表示重入次数。

在**Semaphore**中,state表示可用许可的数量。每次acquire减少state,每次release增加state。

在**CountDownLatch**中,state表示还需等待的次数。每次countDown减少state,当state减到0时唤醒等待线程。

在**ReentrantReadWriteLock**中,state的高16位表示读锁的持有数量,低16位表示写锁的重入次数。

#### 6.2.2 状态修改的原子性保证

AQS使用多种技术保证状态修改的原子性：

**CAS操作**是最基础的原子性保证。对于简单的状态更新（如tryAcquire中的状态修改）,AQS直接使用compareAndSetState方法：

```java
protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

CAS操作通过硬件支持保证原子性,如果更新失败,调用者可以决定重试或采取其他策略。

**CAS循环模式**用于复杂的更新逻辑。当一次CAS操作可能失败时,通常使用循环不断重试：

```java
private boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (tryAcquire(arg) && p == head) {
                // 获取成功
                setHead(node);
                p.next = null;
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

这个方法在自旋中不断尝试获取锁,直到成功或被中断。

**LockSupport.park**用于线程的阻塞和唤醒。虽然CAS可以保证状态修改的原子性,但当锁不可用时,线程需要进入等待状态。LockSupport提供了线程阻塞和唤醒的能力。

#### 6.2.3 状态的内存语义

volatile变量state具有特殊的内存语义,这是AQS实现正确同步的基础：

**可见性保证**：对state的写入happens-before后续对state的读取。这意味着一个线程对state的修改对其他线程是立即可见的。

**有序性保证**：禁止指令重排序。volatile的写操作和读操作之间不能重排序,保证了同步逻辑的正确性。

**happens-before关系**：线程A调用release操作修改state happens-before线程B调用acquire操作读取到修改后的state。

这些内存语义保证了多线程环境下同步器的正确行为。

### 6.3 等待队列管理详解

#### 6.3.1 CLH队列的变体

AQS的等待队列是CLH队列的一种变体。CLH队列是一种自旋锁队列,具有高效、公平的优点。AQS对其进行了扩展,增加了对取消和Condition的支持。

CLH队列的核心思想是：将等待获取锁的线程组织成一个FIFO队列,每个线程自旋检查前驱节点的状态,当前驱节点释放锁时,后继节点被唤醒。

**CLH队列的特点**包括：第一,**自旋检查前驱**,而不是自旋检查锁状态,减少了CPU消耗；第二,**无锁入队**,新节点通过CAS操作加入队列尾部；第三,**公平性**,等待时间最长的线程优先获取锁。

#### 6.3.2 节点结构详解

Node类的设计是等待队列的核心：

```java
static final class Node {
    // 等待状态常量
    static final int CANCELLED =  1;  // 节点已取消,值为正数
    static final int SIGNAL    = -1;  // 后继节点需要被唤醒,值为负数
    static final int CONDITION = -2;  // 节点在条件队列中等待
    static final int PROPAGATE = -3;  // 共享模式下传播唤醒
    static final int WAITING   = -1;  // 有效等待状态的基准值
    
    volatile int waitStatus;
    volatile Node prev;
    volatile Node next;
    volatile Thread thread;
    Node nextWaiter;
    
    static final Node SHARED = new Node();
    static final Node EXCLUSIVE = null;
}
```

**waitStatus字段**是节点状态的核心,其含义如下：

**CANCELLED（1）**：表示节点已超时或被取消。当线程在等待过程中被中断或超时时,会进入这个状态。处于CANCELLED状态的节点会从队列中移除,其nextWaiter指向自己,形成一个无效节点。

**SIGNAL（-1）**：表示后继节点需要被当前节点唤醒。这是等待队列中最重要的状态。当一个节点被设置为SIGNAL状态时,意味着当它释放锁时需要唤醒其后继节点。

**CONDITION（-2）**：表示节点在条件队列中等待。这个状态只用于ConditionObject的队列。当线程调用Condition.await()时,节点会从等待队列转移到条件队列,并设置为此状态。

**PROPAGATE（-3）**：用于共享模式下的传播唤醒。在共享模式中,当一个节点被唤醒时,它可能需要继续唤醒其后继节点,这个状态用于帮助完成这种传播。

**线程字段**（thread）保存了与该节点关联的线程。当线程获取锁失败时,会被包装成Node加入队列；当线程获取锁成功时,thread字段被设置为null,表示节点不再关联任何线程。

**nextWaiter字段**在两种场景下使用：在等待队列中,它指向共享模式标记（SHARED）或null（独占模式）；在条件队列中,它指向条件队列中的下一个节点。

#### 6.3.3 入队操作详解

当线程尝试获取锁失败时,会被加入等待队列。入队操作包括addWaiter和enq两个方法：

```java
private Node addWaiter(Node mode) {
    // 创建新节点
    Node node = new Node(mode);
    
    // 快速路径：尝试将节点加入队尾
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    
    // 慢速路径：通过自旋将节点加入队尾
    enq(node);
    return node;
}

private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) {
            // 队列为空,初始化队列
            if (compareAndSetHead(new Node())) {
                tail = head;
            }
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

**addWaiter方法**首先尝试快速路径：如果尾节点不为空,则直接将新节点链接到尾节点后,然后通过CAS操作更新tail指针。如果CAS成功,则完成入队；如果CAS失败（说明有竞争）,则进入enq方法。

**enq方法**通过自旋将节点加入队列。它首先检查队列是否为空：如果为空,则通过CAS创建头节点；如果不为空,则通过CAS将新节点加入队尾。由于CAS可能失败,整个过程在一个循环中不断重试。

**入队操作的原子性**由CAS保证。compareAndSetTail方法使用CAS操作更新tail指针,确保只有一个线程能够成功将节点加入队列。

#### 6.3.4 出队操作详解

当持有锁的线程释放锁时,会唤醒等待队列中的后继节点。出队操作主要在acquireQueued和setHead方法中完成：

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (tryAcquire(arg) && p == head) {
                // 获取成功,出队
                setHead(node);
                p.next = null; // 帮助GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}
```

**acquireQueued方法**是等待队列中线程获取锁的核心逻辑。线程在循环中检查前驱节点：如果前驱节点是头节点,说明头节点即将释放锁,当前线程尝试获取；如果获取成功,则调用setHead方法出队。

**setHead方法**将当前节点设为新的头节点。它将head指向当前节点,并将node的thread和prev字段设置为null。这些操作帮助GC回收不再使用的节点和线程对象。

#### 6.3.5 取消节点处理

当线程在等待过程中被中断或超时时,需要取消该节点：

```java
private void cancelAcquire(Node node) {
    if (node == null)
        return;
    
    node.thread = null;
    
    // 跳过已取消的前驱节点
    Node pred = node.prev;
    while (pred.waitStatus > 0) {
        node.prev = pred = pred.prev;
    }
    Node predNext = pred.next;
    
    // 将当前节点标记为已取消
    node.waitStatus = Node.CANCELLED;
    
    // 如果当前节点是尾节点,移除从当前节点开始的所有已取消节点
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null);
    } else {
        // 如果前驱节点不是头节点,尝试唤醒后继节点
        int ws = pred.waitStatus;
        if (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))
            unparkSuccessor(node);
        
        // 否则,直接将后继节点的next指向predNext
        node.next = predNext;
        if (predNext != null && predNext.waitStatus > 0)
            predNext = null; // 标记predNext也需要清理
    }
}
```

**cancelAcquire方法**处理节点的取消。主要步骤包括：清除节点的thread字段；跳过已取消的前驱节点；将当前节点标记为CANCELLED；更新队列链接。

### 6.4 独占模式详解

#### 6.4.1 独占模式获取流程

独占模式是互斥锁的基础。在这种模式下,同一时刻只有一个线程能够获取锁。独占模式的获取通过acquire方法实现：

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

acquire方法的执行流程分为三步：

**第一步：tryAcquire尝试获取锁**。tryAcquire是抽象方法,需要子类实现。它检查当前状态是否允许获取锁,如果允许则更新状态并返回true。不同的同步器有不同的tryAcquire实现。

**第二步：addWaiter加入等待队列**。如果tryAcquire失败,说明锁不可用,当前线程需要等待。此时,将当前线程包装成EXCLUSIVE模式的节点,加入等待队列尾部。

**第三步：acquireQueued在队列中等待**。节点加入队列后,线程进入自旋状态,不断尝试获取锁,直到成功或被中断。

#### 6.4.2 tryAcquire方法解析

tryAcquire是子类必须实现的核心方法。以ReentrantLock为例：

**非公平锁的tryAcquire实现**：

```java
protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}

final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 锁可用,尝试CAS获取
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        // 当前线程已持有锁,可重入
        int nextc = c + acquires;
        if (nextc < 0) // 防止整数溢出
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

非公平锁首先检查state是否为0：如果为0,说明锁可用,尝试CAS获取；如果不为0但当前线程是持有者,则更新state（可重入）；否则获取失败。

**公平锁的tryAcquire实现**：

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 检查是否有前驱节点
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

公平锁与非公平锁的唯一区别是：在state=0时,公平锁会先检查hasQueuedPredecessors(),只有当没有前驱节点时才尝试获取锁。

#### 6.4.3 独占模式释放流程

独占模式的释放通过release方法实现：

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

release方法首先调用tryRelease尝试释放锁。如果释放成功（tryRelease返回true）,则唤醒后继节点。

**tryRelease方法（以ReentrantLock为例）**：

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

tryRelease首先检查调用线程是否是锁的持有者。然后计算新的state值：如果c==0,说明锁完全释放,将持有者设为null；否则只是减少重入次数。只有完全释放时才返回true。

**unparkSuccessor方法**：

```java
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);
    
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        // 从尾节点向前查找最接近头节点的有效节点
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

unparkSuccessor方法唤醒后继节点。它首先清除头节点的waitStatus,然后从后继开始查找需要唤醒的节点。如果后继节点为空或已取消,则从尾节点向前查找最接近头节点的有效节点。

### 6.5 共享模式详解

#### 6.5.1 共享模式获取流程

共享模式允许多个线程同时获取锁。共享模式的获取通过acquireShared方法实现：

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

acquireShared的执行流程与acquire类似,但有两个重要区别：tryAcquireShared返回负数表示获取失败,返回0或正数表示获取成功；如果获取成功,需要向后传播,唤醒后续等待的共享节点。

#### 6.5.2 tryAcquireShared方法解析

tryAcquireShared返回负数表示获取失败,返回0表示获取成功但不传播,返回正数表示获取成功且需要传播。

**以Semaphore为例的tryAcquireShared实现**：

```java
protected int tryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```

Semaphore的tryAcquireShared计算剩余许可数：如果remaining<0,说明许可不足,返回负数；如果CAS成功,返回remaining（可能是0或正数）。

**公平版本的额外检查**：

```java
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
```

公平版本在获取前检查hasQueuedPredecessors(),如果有前驱节点则直接返回-1（获取失败）。

#### 6.5.3 doAcquireShared方法解析

```java
private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null;
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

doAcquireShared与acquireQueued的主要区别在于获取成功后的处理：调用setHeadAndPropagate方法,不仅设置当前节点为头节点,还向后传播唤醒。

#### 6.5.4 共享模式释放流程

共享模式的释放通过releaseShared方法实现：

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}

private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h.waitStatus != 0) {
            unparkSuccessor(h);
            if (h == head)
                break;
        }
        if (h == head)
            break;
    }
}
```

doReleaseShared方法循环检查头节点：如果头节点存在且waitStatus不为0,则唤醒后继节点；然后检查头节点是否变化,如果没变则退出循环。这种循环设计确保所有等待的共享节点都能被唤醒。

### 6.6 Condition机制详解

#### 6.6.1 Condition概述

Condition接口提供了与Object的wait/notify/notifyAll类似的线程协调机制,但功能更强大。一个Lock可以创建多个Condition,每个Condition可以独立地等待和通知。

```java
public interface Condition {
    void await() throws InterruptedException;
    void awaitUninterruptibly();
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    boolean awaitUntil(Date deadline) throws InterruptedException;
    void signal();
    void signalAll();
}
```

Condition相比Object的监视器方法有以下优势：

第一,**多个条件队列**。一个Lock可以创建多个Condition,每个Condition是独立的条件队列,可以更精细地控制线程的等待和通知。

第二,**公平性控制**。使用ReentrantLock创建Condition时,可以指定公平性策略。

第三,**可中断性**。Condition.await()支持可中断等待,而Object.wait()需要通过额外的try-catch处理中断。

#### 6.6.2 ConditionObject结构

ConditionObject是Condition接口的实现,它是AQS的内部类：

```java
public class ConditionObject implements Condition, java.io.Serializable {
    private transient Node firstWaiter;
    private transient Node lastWaiter;
}
```

ConditionObject维护一个独立的条件队列,firstWaiter和lastWaiter分别是队列的头节点和尾节点。与等待队列不同,条件队列是单向链表,只使用nextWaiter指针。

#### 6.6.3 await方法详解

当线程调用Condition.await()时,会释放持有的锁,并进入条件队列等待：

```java
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    // 将当前线程加入条件队列
    Node node = addConditionWaiter();
    // 完全释放锁
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 检查是否已被转移到同步队列
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // 重新获取锁
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

**await方法的执行流程**：

第一步,**检查中断**。如果线程已被中断,立即抛出InterruptedException。

第二步,**加入条件队列**。调用addConditionWaiter方法将当前线程包装成Node加入条件队列。

第三步,**释放锁**。调用fullyRelease方法完全释放持有的锁。如果释放失败,会抛出异常。

第四步,**等待**。在循环中检查节点是否已被转移到同步队列（通过isOnSyncQueue方法）。如果不在同步队列上,则调用LockSupport.park阻塞当前线程。

第五,**被唤醒后重新获取锁**。线程被唤醒后,调用acquireQueued方法重新获取锁。

第六,**清理**。调用unlinkCancelledWaiters方法清理条件队列中已取消的节点。

#### 6.6.4 signal方法详解

当线程调用Condition.signal()时,会唤醒条件队列中的第一个等待节点：

```java
public final void signal() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}

private void doSignal(Node first) {
    do {
        firstWaiter = first.nextWaiter;
        if (firstWaiter == null)
            lastWaiter = null;
        if (transferForSignal(first))
            break;
    } while (firstWaiter != null);
}

final boolean transferForSignal(Node node) {
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;
    
    // 将节点加入同步队列
    enq(node);
    
    // 检查是否需要唤醒
    int ws = node.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(node, ws, Node.SIGNAL))
        LockSupport.unpark(node.thread);
    return true;
}
```

**signal方法的执行流程**：

第一步,**检查锁持有**。调用isHeldExclusively方法检查当前线程是否持有锁。

第二步,**获取条件队列头节点**。从firstWaiter开始遍历条件队列。

第三步,**转移节点**。调用transferForSignal方法将节点从条件队列转移到同步队列。转移过程包括：修改节点的waitStatus；将节点加入同步队列的尾部；如果原节点状态异常或CAS失败,直接唤醒线程。

#### 6.6.5 signalAll方法详解

signalAll方法唤醒条件队列中的所有等待节点：

```java
public final void signalAll() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignalAll(first);
}

private void doSignalAll(Node first) {
    lastWaiter = firstWaiter = null;
    for (Node node = first; node != null; node = node.nextWaiter) {
        Node next = node.nextWaiter;
        node.nextWaiter = null;
        transferForSignal(node);
    }
}
```

signalAll方法遍历整个条件队列,将所有节点转移到同步队列。这会导致所有等待的线程都被唤醒。

### 6.7 AQS应用场景与源码深度解析

#### 6.7.1 ReentrantLock源码解析

ReentrantLock是AQS最典型的应用。它的核心结构如下：

```java
public class ReentrantLock implements Lock {
    private final Sync sync;
    
    abstract static class Sync extends AbstractQueuedSynchronizer {
        abstract void lock();
        
        // 共享的非公平获取逻辑
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) throw new Error("Maximum lock count exceeded");
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
    }
    
    static final class NonfairSync extends Sync {
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
    
    static final class FairSync extends Sync {
        final void lock() {
            acquire(1);
        }
        
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires))
                    setExclusiveOwnerThread(current);
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) throw new Error("Maximum lock count exceeded");
                setState(nextc);
            }
            return false;
        }
    }
}
```

ReentrantLock的源码展示了AQS的应用模式：定义Sync抽象类实现通用逻辑,分别定义NonfairSync和FairSync实现不同的获取策略。

#### 6.7.2 CountDownLatch源码解析

CountDownLatch是使用AQS实现倒计时锁存器的典型例子：

```java
public class CountDownLatch {
    private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int count) {
            setState(count);
        }
        
        int getCount() {
            return getState();
        }
        
        protected int tryAcquireShared(int acquires) {
            return getState() == 0 ? 1 : -1;
        }
        
        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
    
    private final Sync sync;
    
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }
    
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
    
    public void countDown() {
        sync.releaseShared(1);
    }
}
```

CountDownLatch使用AQS的共享模式。state表示还需等待的次数。tryAcquireShared返回1（获取成功）当且仅当state为0；tryReleaseShared每次将state减1,当state变为0时返回true以唤醒等待线程。

#### 6.7.3 Semaphore源码解析

Semaphore使用AQS实现信号量：

```java
public class Semaphore implements java.io.Serializable {
    private final Sync sync;
    
    abstract static class Sync extends AbstractQueuedSynchronizer {
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
    }
    
    static final class NonfairSync extends Sync {
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }
    
    static final class FairSync extends Sync {
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
}
```

Semaphore使用AQS的共享模式。state表示可用许可的数量。tryAcquireShared计算remaining,如果remaining<0则返回负数（获取失败）；如果CAS成功则返回remaining。

### 6.8 AQS设计模式深度解析

#### 6.8.1 模板方法模式的应用

AQS是模板方法模式的典型应用。acquire、release、acquireShared、releaseShared等方法是模板方法,定义了获取/释放锁的通用流程；tryAcquire、tryRelease、tryAcquireShared、tryReleaseShared是抽象方法,需要子类实现。

这种设计的优点是：将通用的同步逻辑集中在父类中实现,避免代码重复；子类只需要实现特定的行为,降低了复杂度；保证了所有同步器行为的一致性。

#### 6.8.2 策略模式的应用

公平锁和非公平锁是策略模式的典型应用。它们实现了相同的接口（tryAcquire）,但采用了不同的策略：公平锁检查等待队列,非公平锁直接尝试获取。

这种设计使得切换策略变得非常容易,只需要使用不同的Sync实现即可。

#### 6.8.3 状态模式的应用

state变量对不同同步器有不同的含义,体现了状态模式的思想。AQS本身不解释state的含义,而是由子类来定义。这种设计使得AQS可以支持各种不同的同步器类型。

### 6.9 小结

AQS是Java并发编程的核心框架,理解AQS的原理对于深入掌握JUC至关重要。

**核心要点总结：**

第一,**AQS的设计哲学**体现了模板方法模式和策略模式的精髓,通过分离通用逻辑和特定实现,实现了高度的可扩展性和复用性。

第二,**状态管理**是AQS的核心,state变量被不同同步器解释为不同的含义：互斥锁表示持有者,信号量表示可用许可数,倒计时锁存器表示还需等待的计数。

第三,**等待队列**使用CLH队列的变体管理等待线程,通过CAS操作保证入队的原子性。

第四,**独占模式**和**共享模式**分别用于不同的同步场景：独占模式用于互斥访问,共享模式用于并行访问。

第五,**Condition机制**提供了比Object监视器方法更灵活的线程协调能力。

第六,**ReentrantLock、Semaphore、CountDownLatch**等同步器都是基于AQS构建的,它们展示了AQS的应用模式。
