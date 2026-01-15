# Java并发编程(JUC)体系源码深度解析

## 一、JUC体系架构总览

### 1.1 JUC概述

`java.util.concurrent` (简称JUC) 是Java 5引入的并发编程工具包,提供了一系列用于多线程环境下的工具类、容器和同步机制。Doug Lea领导设计和实现了这个包,它是Java并发编程的核心基础。

**设计目标:**
- 提供比synchronized和wait/notify更强大的并发控制能力
- 提供高性能的并发容器
- 提供线程池管理机制
- 提供函数式编程风格的异步编程支持

### 1.2 JUC核心组件分类

```
JUC体系架构
├── 线程池与执行框架
│   ├── Executor接口
│   ├── ExecutorService接口
│   ├── ThreadPoolExecutor
│   ├── ScheduledThreadPoolExecutor
│   ├── ForkJoinPool
│   └── Executors工厂类
│
├── 同步器与锁机制
│   ├── AQS抽象同步器
│   ├── ReentrantLock可重入锁
│   ├── ReentrantReadWriteLock读写锁
│   ├── Semaphore信号量
│   ├── CountDownLatch倒计时锁存器
│   ├── CyclicBarrier循环屏障
│   ├── Phaser阶段同步器
│   └── Exchanger数据交换器
│
├── 并发容器
│   ├── ConcurrentHashMap
│   ├── ConcurrentLinkedQueue
│   ├── ConcurrentLinkedDeque
│   ├── CopyOnWriteArrayList
│   ├── CopyOnWriteArraySet
│   ├── LinkedBlockingQueue
│   ├── ArrayBlockingQueue
│   ├── PriorityBlockingQueue
│   ├── DelayQueue
│   ├── SynchronousQueue
│   ├── LinkedTransferQueue
│   └── ConcurrentSkipListMap/Set
│
├── 原子变量
│   ├── AtomicInteger
│   ├── AtomicLong
│   ├── AtomicBoolean
│   ├── AtomicReference
│   ├── AtomicIntegerArray
│   └── AtomicIntegerFieldUpdater
│
├── 任务Future体系
│   ├── Future接口
│   ├── FutureTask
│   ├── CompletableFuture
│   ├── ForkJoinTask
│   ├── RecursiveTask
│   ├── RecursiveAction
│   └── CountedCompleter
│
└── 并发工具类
    ├── ThreadLocalRandom
    ├── TimeUnit
    ├── ThreadFactory
    └── Collections工具方法
```

### 1.3 JUC设计哲学

#### 1.3.1 分离策略与实现

JUC大量使用接口和工厂模式,将API(策略)与实现分离:

```java
// 接口定义策略
public interface Lock {
    void lock();
    void unlock();
    boolean tryLock();
    Condition newCondition();
}

// 实现提供具体策略
public class ReentrantLock implements Lock {
    private final Sync sync;
    abstract static class Sync extends AbstractQueuedSynchronizer { ... }
    
    // 非公平实现
    static final class NonfairSync extends Sync { ... }
    
    // 公平实现
    static final class FairSync extends Sync { ... }
}
```

#### 1.3.2 依赖倒置原则

高层模块(线程池)不依赖低层模块(具体同步器),而是都依赖于抽象(AQS):

```java
// ThreadPoolExecutor依赖AQS
public class ThreadPoolExecutor extends AbstractExecutorService {
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    // 使用AQS的state来管理线程池状态
}

// Semaphore依赖AQS
public class Semaphore implements java.io.Serializable {
    private final Sync sync;
    abstract static class Sync extends AbstractQueuedSynchronizer { ... }
}
```

#### 1.3.3 组合优于继承

通过组合现有的同步器来实现新的功能:

```java
// ReentrantLock组合AQS实现可重入
class Sync extends AbstractQueuedSynchronizer {
    protected boolean tryAcquire(int acquires) {
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
}
```

### 1.4 JUC核心接口体系

#### 1.4.1 Executor执行框架

```
Executor
    ↓
ExecutorService
    ├─ AbstractExecutorService
    │   └─ ThreadPoolExecutor
    ├─ ScheduledExecutorService
    │   └─ ScheduledThreadPoolExecutor
    └─ ForkJoinPool
```

**接口职责:**
- **Executor**: 执行任务的简单接口,只定义`execute(Runnable)`方法
- **ExecutorService**: 扩展Executor,增加生命周期管理和任务提交能力
- **ScheduledExecutorService**: 支持定时任务和周期任务

#### 1.4.2 Lock锁接口体系

```
Lock
    ↓
ReentrantLock
    │
ReentrantReadWriteLock
    ├─ ReadLock
    └─ WriteLock

Condition (与Lock配合使用)
    ↓
AbstractQueuedSynchronizer.ConditionObject
```

**接口职责:**
- **Lock**: 提供比synchronized更灵活的锁操作
- **Condition**: 提供类似Object监视器方法的Condition接口

#### 1.4.3 Collection集合接口体系

```
Collection
    └─ List
        ├─ CopyOnWriteArrayList
        └─ (其他并发List)

Set
    └─ CopyOnWriteArraySet
        └─ 基于CopyOnWriteArrayList

Queue
    ├─ BlockingQueue
    │   ├─ ArrayBlockingQueue
    │   ├─ LinkedBlockingQueue
    │   ├─ PriorityBlockingQueue
    │   ├─ DelayQueue
    │   ├─ SynchronousQueue
    │   └─ LinkedTransferQueue
    ├─ ConcurrentLinkedQueue
    └─ LinkedBlockingDeque

Deque
    ├─ BlockingDeque
    │   ├─ LinkedBlockingDeque
    │   └─ LinkedTransferQueue
    └─ ConcurrentLinkedDeque

Map
    └─ ConcurrentMap
        ├─ ConcurrentHashMap
        └─ ConcurrentSkipListMap
```

### 1.5 并发编程核心概念

#### 1.5.1 线程安全与可见性

**可见性问题:**
在多线程环境中,一个线程对共享变量的修改可能对其他线程不可见:

```java
// 可见性问题示例
public class VisibilityDemo {
    private static boolean flag = true;
    
    public static void main(String[] args) {
        new Thread(() -> {
            while (flag) {
                // 编译器优化可能导致死循环
                // CPU缓存导致看不到flag的变化
            }
        }).start();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        flag = false; // 可能永远不会被看到
    }
}
```

**JUC解决方案:**
使用volatile和CAS保证可见性:

```java
// 使用volatile保证可见性
private volatile boolean flag = true;

// 使用AtomicBoolean
private static AtomicBoolean flag = new AtomicBoolean(true);
```

#### 1.5.2 原子性与有序性

**原子性问题:**
复合操作(如i++)不是原子的:

```java
// 非原子操作
public class AtomicDemo {
    private int count = 0;
    
    public void increment() {
        count++; // 读取-修改-写入三步,不是原子的
    }
}
```

**JUC解决方案:**
使用CAS和锁保证原子性:

```java
// 使用AtomicInteger
private AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet(); // CAS保证原子性
}

// 使用锁
private ReentrantLock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        count++;
    } finally {
        lock.unlock();
    }
}
```

#### 1.5.3 有序性问题

**指令重排:**
编译器和CPU可能对指令进行重排以优化性能:

```java
// 有序性问题示例
public class OrderingDemo {
    private int a = 0;
    private boolean flag = false;
    
    public void writer() {
        a = 1;           // 操作1
        flag = true;     // 操作2
    }
    
    public void reader() {
        if (flag) {      // 操作3
            int b = a * a; // 操作4
            // b应该是1,但由于重排可能不是
        }
    }
}
```

**JUC解决方案:**
使用内存屏障保证有序性:

```java
// synchronized保证有序性
public class OrderingDemo {
    private int a = 0;
    private boolean flag = false;
    
    public synchronized void writer() {
        a = 1;
        flag = true;
    }
    
    public synchronized void reader() {
        if (flag) {
            int b = a * a;
        }
    }
}

// volatile保证有序性
public class OrderingDemo {
    private int a = 0;
    private volatile boolean flag = false;
    
    public void writer() {
        a = 1;
        flag = true; // volatile阻止重排
    }
    
    public void reader() {
        if (flag) {
            int b = a * a;
        }
    }
}
```

### 1.6 JUC核心设计模式

#### 1.6.1 模板方法模式

AQS定义了获取/释放的模板方法,子类实现tryAcquire/tryRelease:

```java
// AQS模板方法
public abstract class AbstractQueuedSynchronizer {
    // 获取共享锁(可中断)
    public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }
    
    // 子类实现
    protected abstract int tryAcquireShared(int acquires);
    protected abstract boolean tryReleaseShared(int releases);
}
```

#### 1.6.2 策略模式

Lock接口定义了统一的API,不同实现提供不同的策略:

```java
// 公平策略
ReentrantLock fairLock = new ReentrantLock(true);

// 非公平策略(默认)
ReentrantLock nonFairLock = new ReentrantLock(false);

// Semaphore公平策略
Semaphore fairSemaphore = new Semaphore(permits, true);

// Semaphore非公平策略
Semaphore nonFairSemaphore = new Semaphore(permits, false);
```

#### 1.6.3 工厂模式

Executors提供多种工厂方法创建线程池:

```java
// 创建固定大小线程池
ExecutorService fixedPool = Executors.newFixedThreadPool(10);

// 创建缓存线程池
ExecutorService cachedPool = Executors.newCachedThreadPool();

// 创建单线程线程池
ExecutorService singlePool = Executors.newSingleThreadExecutor();

// 创建调度线程池
ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(10);

// 创建工作窃取线程池
ForkJoinPool workStealingPool = Executors.newWorkStealingPool();
```

#### 1.6.4 观察者模式

Future作为异步计算结果的观察者:

```java
// 提交任务,获得Future
Future<String> future = executor.submit(() -> {
    return "result";
});

// 检查是否完成
if (future.isDone()) {
    // 获取结果(阻塞直到完成)
    String result = future.get();
}
```

### 1.7 性能考量

#### 1.7.1 锁的粒度

**粗粒度锁:**
```java
// 粗粒度锁:锁定整个集合
public class CoarseLock {
    private List<String> list = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    
    public void add(String s) {
        lock.lock();
        try {
            list.add(s);
        } finally {
            lock.unlock();
        }
    }
}
```

**细粒度锁:**
```java
// 细粒度锁:分段锁
public class FineLock {
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    
    public void put(String key, String value) {
        map.put(key, value); // 每个桶独立加锁
    }
}
```

#### 1.7.2 无锁设计

使用CAS实现无锁数据结构:

```java
// 无锁队列的CAS操作
public class LockFreeQueue<E> {
    private final AtomicReference<Node<E>> head = new AtomicReference<>();
    private final AtomicReference<Node<E>> tail = new AtomicReference<>();
    
    public boolean offer(E value) {
        Node<E> newNode = new Node<>(value);
        while (true) {
            Node<E> currentTail = tail.get();
            Node<E> next = currentTail.next.get();
            if (currentTail != tail.get()) continue;
            
            if (next != null) {
                // 尾节点不在末尾,帮助推进
                tail.compareAndSet(currentTail, next);
                continue;
            }
            
            // 尝试添加新节点
            if (currentTail.next.compareAndSet(null, newNode)) {
                // 推进尾节点
                tail.compareAndSet(currentTail, newNode);
                return true;
            }
        }
    }
}
```

### 1.8 常见并发问题解决方案

#### 1.8.1 生产者-消费者问题

```java
// 使用BlockingQueue解决生产者-消费者
public class ProducerConsumerDemo {
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(100);
    
    public void producer() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            queue.put(i); // 阻塞直到队列有空间
        }
    }
    
    public void consumer() throws InterruptedException {
        while (true) {
            Integer value = queue.take(); // 阻塞直到队列有元素
            System.out.println("Consumed: " + value);
        }
    }
}
```

#### 1.8.2 读者-写者问题

```java
// 使用ReadWriteLock解决读者-写者问题
public class ReaderWriterDemo {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private int data = 0;
    
    public void reader() {
        readLock.lock();
        try {
            System.out.println("Reading: " + data);
        } finally {
            readLock.unlock();
        }
    }
    
    public void writer(int value) {
        writeLock.lock();
        try {
            data = value;
            System.out.println("Written: " + data);
        } finally {
            writeLock.unlock();
        }
    }
}
```

#### 1.8.3 线程协作问题

```java
// 使用CyclicBarrier同步多个线程
public class BarrierDemo {
    private final CyclicBarrier barrier;
    private final int parties;
    private final int[] data;
    
    public BarrierDemo(int parties) {
        this.parties = parties;
        this.barrier = new CyclicBarrier(parties, () -> {
            System.out.println("All parties reached barrier, merging results");
        });
        this.data = new int[parties];
    }
    
    public void worker(int id) {
        data[id] = id * 10; // 各自处理数据
        try {
            System.out.println("Worker " + id + " waiting at barrier");
            barrier.await(); // 等待所有线程
            System.out.println("Worker " + id + " passed barrier");
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
```

### 1.9 小结

JUC体系是Java并发编程的核心,提供了:

1. **线程池管理**: 高效的线程复用和管理
2. **同步机制**: 灵活的锁和同步器
3. **并发容器**: 高性能的数据结构
4. **原子操作**: 无锁编程支持
5. **异步编程**: Future和CompletableFuture

理解JUC的设计理念和源码实现,对于编写高效、线程安全的并发程序至关重要。后续文档将深入讲解各个组件的源码实现。
