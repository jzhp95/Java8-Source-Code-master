# Java并发编程(JUC)体系源码深度解析——Exchanger详解

## 十、Exchanger详解

### 10.1 Exchanger概述

#### 10.1.1 Exchanger的引入背景

在多线程编程中,有时需要在两个线程之间交换数据。例如,一个线程负责生产数据,另一个线程负责消费数据,它们需要在某个点交换数据。传统的做法是使用共享变量和锁,但这需要手动管理同步,容易出错。

Java 5引入的Exchanger类提供了一种简单的方式,用于在两个线程之间交换数据。Exchanger是一个同步点,两个线程可以在该点交换数据。当一个线程到达exchange点时,它会等待另一个线程也到达exchange点,然后两个线程交换数据并继续执行。

Exchanger的核心优势包括:

**简单易用**: 只需要调用exchange方法,不需要手动管理锁和条件变量。

**线程安全**: 内部使用CAS和锁机制,保证线程安全。

**灵活的数据交换**: 可以交换任意类型的数据。

**支持超时**: exchange方法支持超时参数,可以设置等待超时。

#### 10.1.2 Exchanger的设计理念

Exchanger的设计理念基于"交换点"(Exchange Point)的概念。两个线程在交换点相遇,交换数据后继续执行。Exchanger内部使用一个槽位(Slot)来存储数据,当第一个线程到达时,将数据放入槽位并等待;当第二个线程到达时,从槽位取出第一个线程的数据,并将自己的数据放入槽位,然后两个线程都返回。

Exchanger的核心特点包括:

**点对点交换**: 只支持两个线程之间的数据交换。

**同步点**: 两个线程必须在同一个点相遇才能交换数据。

**阻塞等待**: 如果只有一个线程到达exchange点,它会等待另一个线程到达。

**超时支持**: 可以设置等待超时,避免无限等待。

```java
public class Exchanger<V> {
    // 槽位,用于存储数据
    private volatile Node<V> slot;
    
    // 等待线程
    private volatile Node<V> arena;
    
    // 序列号
    private volatile int sequence;
}
```

### 10.2 Exchanger核心API详解

#### 10.2.1 exchange方法

```java
// 交换数据,阻塞直到另一个线程到达
public V exchange(V x) throws InterruptedException {
    Object v;
    Node<V> a;
    Thread t = Thread.currentThread();
    if ((a = arena) != null)
        v = arenaExchange(a, x, null, 0L);
    else if ((v = slotExchange(x, null, 0L)) != null ||
             Thread.interrupted())
        return (V)v;
    if (arena != null)
        v = arenaExchange(a, x, null, 0L);
    else if ((v = slotExchange(x, null, 0L)) != null ||
             Thread.interrupted())
        return (V)v;
    throw new InterruptedException();
}

// 交换数据,支持超时
public V exchange(V x, long timeout, TimeUnit unit) 
    throws InterruptedException, TimeoutException {
    Object v;
    Node<V> a;
    long nanos = unit.toNanos(timeout);
    if ((a = arena) != null)
        v = arenaExchange(a, x, null, nanos);
    else if ((v = slotExchange(x, null, nanos)) != null ||
             Thread.interrupted())
        return (V)v;
    if (arena != null)
        v = arenaExchange(a, x, null, nanos);
    else if ((v = slotExchange(x, null, nanos)) != null ||
             Thread.interrupted())
        return (V)v;
    throw new TimeoutException();
}
```

**使用示例:**

```java
Exchanger<String> exchanger = new Exchanger<>();

// 线程1
new Thread(() -> {
    try {
        String data = "Data from Thread 1";
        String received = exchanger.exchange(data);
        System.out.println("Thread 1 received: " + received);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

// 线程2
new Thread(() -> {
    try {
        String data = "Data from Thread 2";
        String received = exchanger.exchange(data);
        System.out.println("Thread 2 received: " + received);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
```

#### 10.2.2 Node结构

```java
private static final class Node<V> {
    int index;
    int bound;
    int collides;
    int hash;
    V item;
    volatile Thread waiter;
    volatile V match;
}
```

**Node字段含义:**

- index: 节点在arena中的索引
- bound: arena的边界
- collides: 冲突计数
- hash: 哈希值
- item: 当前线程要交换的数据
- waiter: 等待的线程
- match: 匹配的数据

### 10.3 Exchanger内部实现原理

#### 10.3.1 单槽位交换(slotExchange)

```java
private final V slotExchange(V x, V y, long nanos) {
    Node<V> p = new Node<V>(x);
    Thread t = Thread.currentThread();
    if (t.isInterrupted())
        return null;
    for (Node<V> q;;) {
        if ((q = slot) != null) {
            if (SLOT.compareAndSet(this, null, null)) {
                V v = q.item;
                q.match = x;
                Thread w = q.waiter;
                if (w != null) {
                    LockSupport.unpark(w);
                }
                return v;
            }
        }
        else if (SLOT.compareAndSet(this, null, p)) {
            long end = (nanos == 0L) ? 0L : System.nanoTime() + nanos;
            int spins = (spinsFor(Thread.currentThread())) << SPINS;
            while (p.match == null) {
                if (spins > 0) {
                    spins--;
                }
                else if (p.waiter == null) {
                    p.waiter = t;
                }
                else if (nanos == 0L) {
                    p.waiter = null;
                    SLOT.compareAndSet(this, p, null);
                    return null;
                }
                else if (nanos > SPINS_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanos);
                }
                else {
                    spins = SPINS;
                }
                if (p.match != null) {
                    Thread w = p.waiter;
                    if (w != null) {
                        p.waiter = null;
                        LockSupport.unpark(w);
                    }
                    return (V)p.match;
                }
                if (t.isInterrupted()) {
                    p.waiter = null;
                    SLOT.compareAndSet(this, p, null);
                    return null;
                }
                if (nanos > 0L && (nanos = end - System.nanoTime()) <= 0L) {
                    p.waiter = null;
                    SLOT.compareAndSet(this, p, null);
                    return null;
                }
            }
        }
    }
}
```

**slotExchange的执行流程:**

1. 创建一个Node节点,存储要交换的数据
2. 检查slot是否为空:
   - 如果不为空,说明有其他线程在等待,交换数据并返回
   - 如果为空,将自己的Node放入slot,等待其他线程
3. 等待其他线程到达:
   - 使用自旋等待
   - 如果超时或被中断,取消等待
   - 如果其他线程到达,交换数据并返回

#### 10.3.2 多槽位交换(arenaExchange)

当有多个线程同时使用Exchanger时,单槽位交换可能导致竞争。Exchanger使用arenaExchange方法来处理多线程场景。

```java
private final V arenaExchange(Node<V> item, V x, V y, long nanos) {
    Node<V>[] a = arena;
    int alen = a.length;
    int i = (Thread.currentThread().hashCode() & (alen - 1)) << 1;
    int hi = i + 2;
    for (;;) {
        Node<V> p = a[i];
        if (p != null && p.bound == bound) {
            if (p.index == i && p.match == null) {
                if (ARENA.compareAndSet(a, i, p, null)) {
                    V v = p.item;
                    p.match = x;
                    Thread w = p.waiter;
                    if (w != null) {
                        LockSupport.unpark(w);
                    }
                    return v;
                }
            }
        }
        if (p == null) {
            if (ARENA.compareAndSet(a, i, null, item)) {
                long end = (nanos == 0L) ? 0L : System.nanoTime() + nanos;
                int spins = (spinsFor(Thread.currentThread())) << SPINS;
                while (item.match == null) {
                    if (spins > 0) {
                        spins--;
                    }
                    else if (item.waiter == null) {
                        item.waiter = Thread.currentThread();
                    }
                    else if (nanos == 0L) {
                        item.waiter = null;
                        ARENA.compareAndSet(a, i, item, null);
                        return null;
                    }
                    else if (nanos > SPINS_FOR_TIMEOUT_THRESHOLD) {
                        LockSupport.parkNanos(this, nanos);
                    }
                    else {
                        spins = SPINS;
                    }
                    if (item.match != null) {
                        Thread w = item.waiter;
                        if (w != null) {
                            item.waiter = null;
                            LockSupport.unpark(w);
                        }
                        return (V)item.match;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        item.waiter = null;
                        ARENA.compareAndSet(a, i, item, null);
                        return null;
                    }
                    if (nanos > 0L && (nanos = end - System.nanoTime()) <= 0L) {
                        item.waiter = null;
                        ARENA.compareAndSet(a, i, item, null);
                        return null;
                    }
                }
            }
        }
        i = (i + 2) & (alen - 1);
        if (i == hi) {
            i = (i - 2) & (alen - 1);
        }
    }
}
```

**arenaExchange的执行流程:**

1. 根据线程的hashCode计算在arena数组中的位置
2. 检查该位置是否有其他线程在等待:
   - 如果有,交换数据并返回
   - 如果没有,将自己的Node放入该位置,等待其他线程
3. 如果当前位置没有匹配,移动到下一个位置继续尝试
4. 等待其他线程到达,交换数据并返回

### 10.4 Exchanger使用场景

#### 10.4.1 生产者-消费者模式

```java
public class ProducerConsumer {
    private final Exchanger<String> exchanger = new Exchanger<>();
    
    public void producer() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String data = "Product " + i;
                    System.out.println("Producer produced: " + data);
                    String feedback = exchanger.exchange(data);
                    System.out.println("Producer received feedback: " + feedback);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void consumer() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String data = exchanger.exchange(null);
                    System.out.println("Consumer received: " + data);
                    String feedback = "Feedback for " + data;
                    System.out.println("Consumer sending feedback: " + feedback);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

#### 10.4.2 数据校验

```java
public class DataValidation {
    private final Exchanger<String> exchanger = new Exchanger<>();
    
    public void producer() {
        new Thread(() -> {
            try {
                String data = "Important Data";
                System.out.println("Producer sending data: " + data);
                String validated = exchanger.exchange(data);
                System.out.println("Producer received validated data: " + validated);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void validator() {
        new Thread(() -> {
            try {
                String data = exchanger.exchange(null);
                System.out.println("Validator received data: " + data);
                String validated = validate(data);
                System.out.println("Validator sending validated data: " + validated);
                exchanger.exchange(validated);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private String validate(String data) {
        return data + " [VALIDATED]";
    }
}
```

#### 10.4.3 协同工作

```java
public class CollaborativeWork {
    private final Exchanger<String> exchanger = new Exchanger<>();
    
    public void worker1() {
        new Thread(() -> {
            try {
                String data = "Data from Worker 1";
                System.out.println("Worker 1 sending: " + data);
                String received = exchanger.exchange(data);
                System.out.println("Worker 1 received: " + received);
                process(received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void worker2() {
        new Thread(() -> {
            try {
                String data = "Data from Worker 2";
                System.out.println("Worker 2 sending: " + data);
                String received = exchanger.exchange(data);
                System.out.println("Worker 2 received: " + received);
                process(received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void process(String data) {
        System.out.println("Processing: " + data);
    }
}
```

### 10.5 Exchanger最佳实践

#### 10.5.1 处理超时

```java
public class TimeoutHandling {
    private final Exchanger<String> exchanger = new Exchanger<>();
    
    public void worker() {
        new Thread(() -> {
            try {
                String data = "Data";
                System.out.println("Worker sending: " + data);
                String received = exchanger.exchange(data, 5, TimeUnit.SECONDS);
                System.out.println("Worker received: " + received);
            } catch (TimeoutException e) {
                System.out.println("Timeout waiting for exchange");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

#### 10.5.2 处理中断

```java
public class InterruptHandling {
    private final Exchanger<String> exchanger = new Exchanger<>();
    
    public void worker() {
        Thread thread = new Thread(() -> {
            try {
                String data = "Data";
                System.out.println("Worker sending: " + data);
                String received = exchanger.exchange(data);
                System.out.println("Worker received: " + received);
            } catch (InterruptedException e) {
                System.out.println("Worker interrupted");
                Thread.currentThread().interrupt();
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

### 10.6 Exchanger与其他同步工具对比

#### 10.6.1 功能对比

| 特性 | Exchanger | BlockingQueue | SynchronousQueue |
|------|-----------|---------------|-----------------|
| 数据交换 | 点对点 | 多对多 | 点对点 |
| 数据存储 | 临时存储 | 持久存储 | 不存储 |
| 阻塞等待 | 是 | 是 | 是 |
| 超时支持 | 是 | 是 | 是 |
| 使用复杂度 | 简单 | 中等 | 简单 |

#### 10.6.2 使用场景对比

**Exchanger适用场景:**

- 两个线程之间需要交换数据
- 数据交换是同步的
- 不需要持久存储数据

```java
Exchanger<String> exchanger = new Exchanger<>();
String received = exchanger.exchange(data);
```

**BlockingQueue适用场景:**

- 多个生产者和消费者
- 需要缓冲数据
- 异步处理

```java
BlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put(data);
String received = queue.take();
```

**SynchronousQueue适用场景:**

- 生产者和消费者直接同步
- 不需要缓冲数据
- 点对点传递

```java
SynchronousQueue<String> queue = new SynchronousQueue<>();
queue.put(data);
String received = queue.take();
```

### 10.7 小结

Exchanger是Java并发编程中用于两个线程之间交换数据的同步工具,它提供了简单易用的API,支持超时和中断。

**核心要点总结:**

第一,**点对点交换**: Exchanger只支持两个线程之间的数据交换。

第二,**同步点**: 两个线程必须在同一个点相遇才能交换数据。

第三,**阻塞等待**: 如果只有一个线程到达exchange点,它会等待另一个线程到达。

第四,**超时支持**: 可以设置等待超时,避免无限等待。

第五,**内部实现**: 使用slot和arena两种机制,slot用于单线程场景,arena用于多线程场景。

第六,**使用场景**: 适合两个线程之间需要同步交换数据的场景,如生产者-消费者、数据校验等。
