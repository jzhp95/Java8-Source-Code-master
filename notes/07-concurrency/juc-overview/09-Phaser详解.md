# Java并发编程(JUC)体系源码深度解析——Phaser详解

## 九、Phaser详解

### 9.1 Phaser概述

#### 9.1.1 Phaser的引入背景

在Java并发编程中,CountDownLatch和CyclicBarrier是两个常用的同步工具。CountDownLatch允许一个或多个线程等待其他线程完成一组操作,但它只能使用一次,不能重复使用。CyclicBarrier允许一组线程互相等待,直到所有线程都到达某个屏障点,它可以重复使用,但灵活性有限。

Phaser是Java 7引入的更强大的同步工具,它结合了CountDownLatch和CyclicBarrier的功能,并提供了更多的灵活性。Phaser支持动态调整参与线程的数量,支持分阶段的同步,支持在到达屏障时执行回调操作。

Phaser的核心优势包括:

**动态注册**: 可以在运行时动态地注册和注销参与者,不需要预先知道参与者的数量。

**多阶段同步**: 支持多个阶段的同步,每个阶段可以有不同数量的参与者。

**分层结构**: 支持将多个Phaser组成树形结构,实现更复杂的同步场景。

**回调机制**: 支持在所有参与者到达屏障时执行回调操作。

#### 9.1.2 Phaser的核心概念

Phaser的核心概念包括"阶段"(Phase)和"参与者"(Party)。

**阶段(Phase)**: Phaser维护一个当前的阶段号,从0开始,每次所有参与者都到达屏障后,阶段号递增。阶段号是一个int值,可以用于区分不同的同步阶段。

**参与者(Party)**: 参与者是需要同步的线程或任务。每个参与者必须调用arrive或arriveAndAwaitAdvance方法来表示它已经到达当前阶段。当所有参与者都到达后,Phaser进入下一个阶段。

```java
public class Phaser implements java.io.Serializable {
    // 阶段号
    private volatile long state;
    
    // 父Phaser
    private final Phaser parent;
    
    // 根Phaser
    private final Phaser root;
    
    // 等待队列
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;
}
```

state字段是一个long类型的变量,它打包了多个信息:

- 高32位: 阶段号(phase)
- 低32位: 参与者数量和未到达的参与者数量

### 9.2 Phaser核心API详解

#### 9.2.1 构造方法

```java
// 创建一个新的Phaser,初始参与者数量为0
public Phaser() {
    this(null, 0);
}

// 创建一个新的Phaser,初始参与者数量为parties
public Phaser(int parties) {
    this(null, parties);
}

// 创建一个新的Phaser,指定父Phaser和初始参与者数量
public Phaser(Phaser parent, int parties) {
    if (parties >>> PARTIES_SHIFT != 0)
        throw new IllegalArgumentException("Illegal number of parties");
    int phase = 0;
    this.parent = parent;
    if (parent != null) {
        final Phaser root = parent.root;
        this.root = root;
        this.evenQ = root.evenQ;
        this.oddQ = root.oddQ;
    } else {
        this.root = this;
        this.evenQ = new AtomicReference<QNode>();
        this.oddQ = new AtomicReference<QNode>();
    }
    this.state = (parties == 0) ? (long)EMPTY :
        ((long)phase << PHASE_SHIFT) |
        ((long)parties << PARTIES_SHIFT) |
        ((long)parties);
}
```

**使用示例:**

```java
// 创建一个Phaser,初始参与者数量为3
Phaser phaser = new Phaser(3);

// 创建一个Phaser,初始参与者数量为0,后续动态注册
Phaser phaser = new Phaser();

// 创建一个Phaser,指定父Phaser
Phaser parent = new Phaser(1);
Phaser child = new Phaser(parent, 2);
```

#### 9.2.2 注册与注销参与者

```java
// 注册一个新的参与者
public int register() {
    return doRegister(1);
}

// 批量注册参与者
public int bulkRegister(int parties) {
    if (parties < 0)
        throw new IllegalArgumentException();
    if (parties == 0)
        return getPhase();
    return doRegister(parties);
}

// 注销一个参与者
public int arriveAndDeregister() {
    return doArrive(ONE_DEREGISTER);
}
```

**使用示例:**

```java
Phaser phaser = new Phaser();

// 线程1注册
phaser.register();

// 线程2注册
phaser.register();

// 线程3注册
phaser.register();

// 线程1到达并注销
phaser.arriveAndDeregister();

// 批量注册5个参与者
phaser.bulkRegister(5);
```

#### 9.2.3 到达与等待

```java
// 到达当前阶段,不等待其他参与者
public int arrive() {
    return doArrive(ONE_ARRIVAL);
}

// 到达当前阶段,并等待其他参与者
public int arriveAndAwaitAdvance() {
    return doArrive(ONE_ARRIVAL | ONE_AWAIT);
}

// 到达当前阶段,并等待其他参与者,支持中断
public int awaitAdvance(int phase) throws InterruptedException {
    final Phaser root = this.root;
    long s = (root == this) ? state : reconcileState();
    int p = (int)(s >>> PHASE_SHIFT);
    if (phase < 0)
        return p;
    if (p == phase)
        return root.awaitAdvance(phase, s, this);
    return p;
}

// 到达当前阶段,并等待其他参与者,支持中断和超时
public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
    final Phaser root = this.root;
    long s = (root == this) ? state : reconcileState();
    int p = (int)(s >>> PHASE_SHIFT);
    if (phase < 0)
        return p;
    if (p == phase)
        return root.awaitAdvanceInterruptibly(phase, s, this);
    return p;
}

// 到达当前阶段,并等待其他参与者,支持超时
public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit)
    throws InterruptedException, TimeoutException {
    long nanos = unit.toNanos(timeout);
    final Phaser root = this.root;
    long s = (root == this) ? state : reconcileState();
    int p = (int)(s >>> PHASE_SHIFT);
    if (phase < 0)
        return p;
    if (p == phase)
        return root.awaitAdvanceInterruptibly(phase, s, this, nanos);
    return p;
}
```

**使用示例:**

```java
Phaser phaser = new Phaser(3);

// 线程1
new Thread(() -> {
    System.out.println("Thread 1 working...");
    phaser.arriveAndAwaitAdvance();
    System.out.println("Thread 1 passed phase 0");
}).start();

// 线程2
new Thread(() -> {
    System.out.println("Thread 2 working...");
    phaser.arriveAndAwaitAdvance();
    System.out.println("Thread 2 passed phase 0");
}).start();

// 线程3
new Thread(() -> {
    System.out.println("Thread 3 working...");
    phaser.arriveAndAwaitAdvance();
    System.out.println("Thread 3 passed phase 0");
}).start();
```

#### 9.2.4 阶段回调

Phaser支持在所有参与者到达屏障时执行回调操作:

```java
// 重写onAdvance方法,在阶段推进时执行
protected boolean onAdvance(int phase, int registeredParties) {
    return registeredParties == 0;
}
```

**使用示例:**

```java
Phaser phaser = new Phaser(3) {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
        System.out.println("Phase " + phase + " completed, " + 
                          registeredParties + " parties remaining");
        return false; // 返回false表示继续下一阶段
    }
};
```

### 9.3 Phaser内部实现原理

#### 9.3.1 状态变量设计

Phaser使用一个long类型的state变量来管理阶段和参与者:

```java
// state的位布局
// 第63-32位: 阶段号(phase)
// 第31-16位: 参与者数量(parties)
// 第15-0位: 未到达的参与者数量(unarrived)

private static final int PHASE_SHIFT = 32;
private static final int PARTIES_SHIFT = 16;
private static final int MAX_PARTIES = 0xffff;
private static final int ONE_ARRIVAL = 1;
private static final int ONE_PARTY = 1 << PARTIES_SHIFT;
private static final int ONE_DEREGISTER = ONE_ARRIVAL | ONE_PARTY;
private static final int EMPTY = 1;

private volatile long state;
```

**state的含义:**

```java
// 提取阶段号
private int phaseOf(long s) {
    return (int)(s >>> PHASE_SHIFT);
}

// 提取参与者数量
private int partiesOf(long s) {
    return (int)s >>> PARTIES_SHIFT & MAX_PARTIES;
}

// 提取未到达的参与者数量
private int unarrivedOf(long s) {
    return (int)s & MAX_PARTIES;
}
```

**示例:**

```java
// 初始状态: phase=0, parties=3, unarrived=3
state = (0L << 32) | (3L << 16) | 3L;

// 线程1到达: phase=0, parties=3, unarrived=2
state = (0L << 32) | (3L << 16) | 2L;

// 线程2到达: phase=0, parties=3, unarrived=1
state = (0L << 32) | (3L << 16) | 1L;

// 线程3到达: phase=1, parties=3, unarrived=3
state = (1L << 32) | (3L << 16) | 3L;
```

#### 9.3.2 到达与等待机制

```java
private int doArrive(int adjust) {
    final Phaser root = this.root;
    for (;;) {
        long s = (root == this) ? state : reconcileState();
        int phase = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        int parties = (int)s >>> PARTIES_SHIFT & MAX_PARTIES;
        int unarrived = (int)s & MAX_PARTIES;
        if (adjust < 0) {
            if (parties == 0)
                return phase;
            else if (unarrived == 0)
                return root.internalAwaitAdvance(phase, null);
        }
        if (unarrived <= 0)
            return phase;
        int nextUnarrived = unarrived - 1;
        long nextState = ((long)phase << PHASE_SHIFT) |
                        ((long)parties << PARTIES_SHIFT) |
                        (long)nextUnarrived;
        if (nextUnarrived == 0)
            nextState |= (long)ONE_ARRIVAL;
        if (STATE.compareAndSet(this, s, nextState))
            return (nextUnarrived == 0) ? 
                   root.advanceTo(phase, nextState) : phase;
    }
}
```

**doArrive方法的执行流程:**

1. 读取当前state
2. 提取phase、parties、unarrived
3. 计算新的unarrived值
4. 如果unarrived减到0,推进到下一阶段
5. 使用CAS更新state

#### 9.3.3 阶段推进机制

```java
private int advanceTo(int phase, long nextState) {
    final Phaser root = this.root;
    int nextPhase = (phase + 1) & MAX_PHASE;
    int nextUnarrived = partiesOf(nextState);
    long next = ((long)nextPhase << PHASE_SHIFT) |
                 ((long)nextUnarrived << PARTIES_SHIFT) |
                 ((long)nextUnarrived);
    if (!STATE.compareAndSet(this, nextState, next))
        return (int)(state >>> PHASE_SHIFT);
    onAdvance(phase, nextUnarrived);
    releaseWaiters(phase);
    releaseWaiters(nextPhase);
    return nextPhase;
}
```

**advanceTo方法的执行流程:**

1. 计算下一阶段的phase
2. 计算下一阶段的unarrived(等于parties)
3. 使用CAS更新state
4. 调用onAdvance回调
5. 唤醒等待的线程

### 9.4 Phaser使用场景

#### 9.4.1 多阶段任务

```java
public class MultiPhaseTask {
    private final Phaser phaser = new Phaser(1);
    
    public void run() {
        System.out.println("Phase 0 start");
        new Thread(() -> {
            phaser.register();
            System.out.println("Worker 1 phase 0");
            phaser.arriveAndAwaitAdvance();
            System.out.println("Worker 1 phase 1");
            phaser.arriveAndAwaitAdvance();
            System.out.println("Worker 1 phase 2");
            phaser.arriveAndDeregister();
        }).start();
        
        new Thread(() -> {
            phaser.register();
            System.out.println("Worker 2 phase 0");
            phaser.arriveAndAwaitAdvance();
            System.out.println("Worker 2 phase 1");
            phaser.arriveAndAwaitAdvance();
            System.out.println("Worker 2 phase 2");
            phaser.arriveAndDeregister();
        }).start();
        
        phaser.arriveAndAwaitAdvance();
        System.out.println("Phase 0 completed");
        phaser.arriveAndAwaitAdvance();
        System.out.println("Phase 1 completed");
        phaser.arriveAndAwaitAdvance();
        System.out.println("Phase 2 completed");
        phaser.arriveAndDeregister();
    }
}
```

#### 9.4.2 动态参与者

```java
public class DynamicParticipants {
    private final Phaser phaser = new Phaser(1);
    
    public void addWorker() {
        new Thread(() -> {
            phaser.register();
            System.out.println("Worker added");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker completed");
            phaser.arriveAndDeregister();
        }).start();
    }
    
    public void run() {
        for (int i = 0; i < 5; i++) {
            addWorker();
        }
        
        while (!phaser.isTerminated()) {
            System.out.println("Waiting for workers, phase: " + phaser.getPhase());
            phaser.arriveAndAwaitAdvance();
        }
        System.out.println("All workers completed");
    }
}
```

#### 9.4.3 分层Phaser

```java
public class HierarchicalPhaser {
    private final Phaser root = new Phaser(1);
    private final Phaser child1 = new Phaser(root, 2);
    private final Phaser child2 = new Phaser(root, 2);
    
    public void run() {
        new Thread(() -> {
            System.out.println("Child1 Worker1");
            child1.arriveAndAwaitAdvance();
            System.out.println("Child1 Worker1 passed");
        }).start();
        
        new Thread(() -> {
            System.out.println("Child1 Worker2");
            child1.arriveAndAwaitAdvance();
            System.out.println("Child1 Worker2 passed");
        }).start();
        
        new Thread(() -> {
            System.out.println("Child2 Worker1");
            child2.arriveAndAwaitAdvance();
            System.out.println("Child2 Worker1 passed");
        }).start();
        
        new Thread(() -> {
            System.out.println("Child2 Worker2");
            child2.arriveAndAwaitAdvance();
            System.out.println("Child2 Worker2 passed");
        }).start();
        
        root.arriveAndAwaitAdvance();
        System.out.println("All workers completed");
        root.arriveAndDeregister();
    }
}
```

### 9.5 Phaser与CountDownLatch、CyclicBarrier对比

#### 9.5.1 功能对比

| 特性 | CountDownLatch | CyclicBarrier | Phaser |
|------|---------------|---------------|--------|
| 一次性使用 | 是 | 否 | 否 |
| 动态参与者 | 否 | 否 | 是 |
| 多阶段同步 | 否 | 是 | 是 |
| 回调机制 | 否 | 是 | 是 |
| 分层结构 | 否 | 否 | 是 |
| 中断支持 | 是 | 是 | 是 |
| 超时支持 | 是 | 是 | 是 |

#### 9.5.2 使用场景对比

**CountDownLatch适用场景:**

- 一个线程等待多个线程完成
- 只需要一次同步
- 参与者数量固定

```java
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        doWork();
        latch.countDown();
    }).start();
}

latch.await();
System.out.println("All workers completed");
```

**CyclicBarrier适用场景:**

- 多个线程互相等待
- 需要重复使用
- 参与者数量固定

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("All workers arrived");
});

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        doWork();
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}
```

**Phaser适用场景:**

- 多个线程互相等待
- 需要重复使用
- 参与者数量动态变化
- 需要多阶段同步

```java
Phaser phaser = new Phaser();

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        phaser.register();
        for (int j = 0; j < 3; j++) {
            doWork();
            phaser.arriveAndAwaitAdvance();
        }
        phaser.arriveAndDeregister();
    }).start();
}
```

### 9.6 Phaser最佳实践

#### 9.6.1 正确注册和注销

```java
public class CorrectPhaserUsage {
    private final Phaser phaser = new Phaser(1);
    
    public void run() {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            new Thread(() -> {
                phaser.register();
                try {
                    doWork(index);
                } finally {
                    phaser.arriveAndDeregister();
                }
            }).start();
        }
        
        while (!phaser.isTerminated()) {
            phaser.arriveAndAwaitAdvance();
        }
    }
    
    private void doWork(int index) {
        System.out.println("Worker " + index + " working");
    }
}
```

#### 9.6.2 处理中断

```java
public class InterruptiblePhaser {
    private final Phaser phaser = new Phaser(1);
    
    public void run() {
        Thread worker = new Thread(() -> {
            phaser.register();
            try {
                for (int i = 0; i < 3; i++) {
                    doWork();
                    phaser.awaitAdvanceInterruptibly(phaser.getPhase());
                }
            } catch (InterruptedException e) {
                System.out.println("Worker interrupted");
                Thread.currentThread().interrupt();
            } finally {
                phaser.arriveAndDeregister();
            }
        });
        
        worker.start();
        
        try {
            Thread.sleep(1000);
            worker.interrupt();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 9.7 小结

Phaser是Java 7引入的强大同步工具,它结合了CountDownLatch和CyclicBarrier的功能,并提供了更多的灵活性。

**核心要点总结:**

第一,**动态参与者**: 支持在运行时动态注册和注销参与者。

第二,**多阶段同步**: 支持多个阶段的同步,每个阶段可以有不同数量的参与者。

第三,**分层结构**: 支持将多个Phaser组成树形结构,实现更复杂的同步场景。

第四,**回调机制**: 支持在所有参与者到达屏障时执行回调操作。

第五,**状态管理**: 使用long类型的state变量,高32位表示阶段号,低32位表示参与者数量和未到达数量。

第六,**使用场景**: 适合需要动态参与者、多阶段同步的场景,比CountDownLatch和CyclicBarrier更灵活。
