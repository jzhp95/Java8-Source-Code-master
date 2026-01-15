# ThreadPoolExecutor 源码解读

## 一、类概述

ThreadPoolExecutor是ExecutorService的实现，使用可能多个池化线程之一执行每个提交的任务，通常使用Executors工厂方法配置。

### 1.1 核心特性

- **线程池管理**: 管理线程的创建、复用和销毁
- **任务队列**: 使用BlockingQueue存储待执行任务
- **拒绝策略**: 当任务无法执行时的处理策略
- **核心线程数**: 保持活跃的最小线程数
- **最大线程数**: 允许创建的最大线程数
- **线程复用**: 复用空闲线程，避免频繁创建销毁
- **钩子方法**: 提供beforeExecute、afterExecute等钩子方法
- **状态管理**: RUNNING、SHUTDOWN、STOP、TIDYING、TERMINATED

### 1.2 适用场景

- 需要管理大量并发任务
- 需要控制线程数量
- 需要任务队列
- 需要拒绝策略
- 需要监控和统计

## 二、设计原理

### 2.1 线程池状态机

```java
// 线程池状态
RUNNING -> SHUTDOWN -> STOP -> TIDYING -> TERMINATED
```

**状态说明**:
- **RUNNING**: 接受新任务并处理队列中的任务
- **SHUTDOWN**: 不接受新任务，但处理队列中的任务
- **STOP**: 不接受新任务，不处理队列中的任务，中断正在执行的任务
- **TIDYING**: 所有任务已终止，workerCount为0
- **TERMINATED**: terminated()方法已完成

### 2.2 线程池参数

```java
// 核心参数
int corePoolSize;      // 核心线程数
int maximumPoolSize;   // 最大线程数
long keepAliveTime;    // 线程空闲时间
TimeUnit unit;        // 时间单位
BlockingQueue<Runnable> workQueue;  // 任务队列
ThreadFactory threadFactory;          // 线程工厂
RejectedExecutionHandler handler;      // 拒绝策略
```

### 2.3 任务执行流程

```java
// 任务执行流程
1. 如果线程数 < corePoolSize，创建新线程执行任务
2. 如果线程数 >= corePoolSize，任务加入队列
3. 如果队列已满且线程数 < maximumPoolSize，创建新线程执行任务
4. 如果队列已满且线程数 >= maximumPoolSize，执行拒绝策略
```

### 2.4 ctl原子变量

```java
// ctl是一个AtomicInteger，包含两个概念字段：
// workerCount: 有效线程数
// runState: 运行状态

private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

// 将workerCount和runState打包到一个int中
private static int ctlOf(int rs, int wc) { return rs | wc; }
```

**设计要点**:
- 高3位存储runState
- 低29位存储workerCount
- 使用位运算提高效率

## 三、继承结构

```
AbstractExecutorService
    ↑
ThreadPoolExecutor
    implements ExecutorService
```

### 3.1 接口实现

- **ExecutorService**: 定义线程池的基本操作
- **Executor**: 定义execute方法

## 四、核心字段

### 4.1 控制状态

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

**字段设计要点**:
- 原子变量，保证线程安全
- 打包workerCount和runState
- 高3位：runState
- 低29位：workerCount

### 4.2 核心线程数

```java
private volatile int corePoolSize;
```

**字段设计要点**:
- 核心线程数
- volatile修饰，保证可见性
- 默认值：取决于构造方法

### 4.3 最大线程数

```java
private volatile int maximumPoolSize;
```

**字段设计要点**:
- 最大线程数
- volatile修饰，保证可见性
- 必须大于等于corePoolSize

### 4.4 空闲时间

```java
private volatile long keepAliveTime;
```

**字段设计要点**:
- 线程空闲时间
- volatile修饰，保证可见性
- 超过此时间，空闲线程会被回收

### 4.5 任务队列

```java
private final BlockingQueue<Runnable> workQueue;
```

**字段设计要点**:
- 存储待执行任务
- 必须是BlockingQueue
- 常用实现：LinkedBlockingQueue、ArrayBlockingQueue、SynchronousQueue

### 4.6 线程工厂

```java
private volatile ThreadFactory threadFactory;
```

**字段设计要点**:
- 创建新线程的工厂
- volatile修饰，保证可见性
- 默认：Executors.defaultThreadFactory()

### 4.7 拒绝策略

```java
private volatile RejectedExecutionHandler handler;
```

**字段设计要点**:
- 任务被拒绝时的处理策略
- volatile修饰，保证可见性
- 默认：AbortPolicy

## 五、构造方法

### 5.1 完整构造方法

```java
public ThreadPoolExecutor(int corePoolSize,
                      int maximumPoolSize,
                      long keepAliveTime,
                      TimeUnit unit,
                      BlockingQueue<Runnable> workQueue,
                      ThreadFactory threadFactory,
                      RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

**方法要点**:
- 指定所有核心参数
- 参数校验
- 初始化线程池

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    10,                         // maximumPoolSize
    60,                          // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new LinkedBlockingQueue<>(100),  // workQueue
    Executors.defaultThreadFactory(), // threadFactory
    new ThreadPoolExecutor.AbortPolicy()  // handler
);
```

### 5.2 简化构造方法

```java
public ThreadPoolExecutor(int corePoolSize,
                      int maximumPoolSize,
                      long keepAliveTime,
                      TimeUnit unit,
                      BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}
```

**方法要点**:
- 使用默认线程工厂
- 使用默认拒绝策略

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    10,                         // maximumPoolSize
    60,                          // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new LinkedBlockingQueue<>(100)   // workQueue
);
```

## 六、核心方法

### 6.1 execute - 执行任务

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    
    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }
    
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        if (! isRunning(recheck) &&
            remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    else if (!addWorker(command, false))
        reject(command);
}
```

**方法要点**:
- 如果线程数 < corePoolSize，创建新线程
- 如果线程数 >= corePoolSize，任务加入队列
- 如果队列已满且线程数 < maximumPoolSize，创建新线程
- 如果队列已满且线程数 >= maximumPoolSize，执行拒绝策略
- 时间复杂度：O(1)平均

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));
executor.execute(() -> {
    System.out.println("Task is running");
});
```

### 6.2 submit - 提交任务

```java
public <T> Future<T> submit(Callable<T> task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task);
    execute(ftask);
    return ftask;
}

public Future<?> submit(Runnable task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
}
```

**方法要点**:
- 将任务包装为RunnableFuture
- 委托给execute方法
- 返回Future对象
- 可以获取任务执行结果

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));

Future<String> future = executor.submit(() -> {
    return "Task completed";
});

String result = future.get();
```

### 6.3 shutdown - 优雅关闭

```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        advanceRunState(SHUTDOWN);
        interruptIdleWorkers();
        onShutdown();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
}
```

**方法要点**:
- 不接受新任务
- 继续处理队列中的任务
- 中断空闲线程
- 不等待所有任务完成

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));
executor.shutdown();
```

### 6.4 shutdownNow - 立即关闭

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        advanceRunState(STOP);
        interruptWorkers();
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}
```

**方法要点**:
- 不接受新任务
- 不处理队列中的任务
- 中断所有线程
- 返回未执行的任务列表

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));
List<Runnable> tasks = executor.shutdownNow();
```

### 6.5 awaitTermination - 等待终止

```java
public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        for (;;) {
            if (runStateAtLeast(ctl.get(), TERMINATED))
                return true;
            if (nanos <= 0)
                return false;
            nanos = termination.awaitNanos(nanos);
        }
    } finally {
        mainLock.unlock();
    }
}
```

**方法要点**:
- 等待线程池终止
- 可以指定超时时间
- 返回是否在超时前终止
- 抛出InterruptedException

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));
executor.shutdown();
executor.awaitTermination(60, TimeUnit.SECONDS);
```

## 七、钩子方法

### 7.1 beforeExecute - 任务执行前

```java
protected void beforeExecute(Thread t, Runnable r) {
}
```

**方法要点**:
- 在任务执行前调用
- 可以重写此方法
- 用于初始化ThreadLocals、收集统计等

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100)) {
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        System.out.println("Task starting: " + r);
    }
};
```

### 7.2 afterExecute - 任务执行后

```java
protected void afterExecute(Runnable r, Throwable t) {
}
```

**方法要点**:
- 在任务执行后调用
- 可以重写此方法
- 用于收集统计、添加日志等
- t为null表示正常执行，非null表示抛出异常

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100)) {
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            System.out.println("Task failed: " + t);
        }
    }
};
```

### 7.3 terminated - 线程池终止

```java
protected void terminated() {
}
```

**方法要点**:
- 在线程池终止后调用
- 可以重写此方法
- 用于清理资源

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100)) {
    @Override
    protected void terminated() {
        super.terminated();
        System.out.println("Pool terminated");
    }
};
```

## 八、拒绝策略

### 8.1 AbortPolicy - 抛出异常

```java
public static class AbortPolicy implements RejectedExecutionHandler {
    public AbortPolicy() { }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        throw new RejectedExecutionException("Task " + r.toString() +
                                             " rejected from " +
                                             e.toString());
    }
}
```

**策略说明**:
- 抛出RejectedExecutionException
- 默认策略

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.AbortPolicy());
```

### 8.2 CallerRunsPolicy - 调用者执行

```java
public static class CallerRunsPolicy implements RejectedExecutionHandler {
    public CallerRunsPolicy() { }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
            r.run();
        }
    }
}
```

**策略说明**:
- 由调用execute方法的线程执行任务
- 提供简单的反馈控制机制

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.CallerRunsPolicy());
```

### 8.3 DiscardPolicy - 丢弃任务

```java
public static class DiscardPolicy implements RejectedExecutionHandler {
    public DiscardPolicy() { }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        // Do nothing, discard the task
    }
}
```

**策略说明**:
- 静默丢弃任务
- 不抛出异常

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.DiscardPolicy());
```

### 8.4 DiscardOldestPolicy - 丢弃最旧任务

```java
public static class DiscardOldestPolicy implements RejectedExecutionHandler {
    public DiscardOldestPolicy() { }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
            e.getQueue().poll();
            if (!e.getQueue().offer(r))
                throw new RejectedExecutionException("Queue full");
        }
    }
}
```

**策略说明**:
- 丢弃队列中最旧的任务
- 然后重新尝试执行

**示例**:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.DiscardOldestPolicy());
```

## 九、任务队列

### 9.1 SynchronousQueue - 直接交接

```java
// 无缓冲队列
BlockingQueue<Runnable> queue = new SynchronousQueue<>();
```

**特点**:
- 无缓冲，直接交接任务
- 如果没有可用线程，创建新线程
- 适用于需要立即执行的任务

### 9.2 LinkedBlockingQueue - 无界队列

```java
// 无界队列
BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
```

**特点**:
- 无界队列
- 任务在队列中等待
- 不会创建超过corePoolSize的线程

### 9.3 ArrayBlockingQueue - 有界队列

```java
// 有界队列
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
```

**特点**:
- 有界队列
- 队列满时创建新线程
- 防止资源耗尽

## 十、设计模式

### 10.1 工厂模式

ThreadFactory用于创建线程：

```java
// 自定义线程工厂
ThreadFactory factory = new ThreadFactory() {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("Worker-" + count.incrementAndGet());
        thread.setDaemon(false);
        return thread;
    }
};
```

### 10.2 策略模式

RejectedExecutionHandler作为策略：

```java
// 自定义拒绝策略
RejectedExecutionHandler handler = new RejectedExecutionHandler() {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        // 自定义处理逻辑
    }
};
```

### 10.3 模板方法模式

提供钩子方法：

```java
protected void beforeExecute(Thread t, Runnable r) {}
protected void afterExecute(Runnable r, Throwable t) {}
protected void terminated() {}
```

## 十一、面试常见问题

### 11.1 ThreadPoolExecutor的工作原理?

1. **线程数 < corePoolSize**: 创建新线程执行任务
2. **线程数 >= corePoolSize**: 任务加入队列
3. **队列已满且线程数 < maximumPoolSize**: 创建新线程执行任务
4. **队列已满且线程数 >= maximumPoolSize**: 执行拒绝策略

### 11.2 corePoolSize和maximumPoolSize的区别?

| 特性 | corePoolSize | maximumPoolSize |
|------|-------------|----------------|
| 作用 | 核心线程数 | 最大线程数 |
| 创建时机 | 立即创建 | 队列满时创建 |
| 回收 | 不回收 | 空闲时回收 |
| 关系 | corePoolSize <= maximumPoolSize | - |

### 11.3 keepAliveTime的作用?

- 空闲线程的存活时间
- 超过此时间，空闲线程会被回收
- 只对超过corePoolSize的线程有效
- 默认值：60秒

### 11.4 ThreadPoolExecutor的拒绝策略?

- **AbortPolicy**: 抛出RejectedExecutionException(默认)
- **CallerRunsPolicy**: 由调用者执行
- **DiscardPolicy**: 丢弃任务
- **DiscardOldestPolicy**: 丢弃最旧任务

### 11.5 ThreadPoolExecutor的队列选择?

| 队列类型 | 特点 | 适用场景 |
|----------|------|---------|
| SynchronousQueue | 无缓冲，立即执行 | 需要立即执行的任务 |
| LinkedBlockingQueue | 无界，等待执行 | 独立任务，无依赖 |
| ArrayBlockingQueue | 有界，防止资源耗尽 | 需要控制资源使用 |

### 11.6 ThreadPoolExecutor的钩子方法?

- **beforeExecute**: 任务执行前
- **afterExecute**: 任务执行后
- **terminated**: 线程池终止

### 11.7 ThreadPoolExecutor的关闭方法?

| 方法 | 作用 | 等待任务完成 |
|------|------|---------------|
| shutdown | 不接受新任务，处理队列中的任务 | 是 |
| shutdownNow | 不接受新任务，不处理队列中的任务 | 否 |

### 11.8 ThreadPoolExecutor的ctl变量?

- ctl是一个AtomicInteger
- 打包workerCount和runState
- 高3位：runState
- 低29位：workerCount
- 使用位运算提高效率

## 十二、使用场景

### 12.1 固定大小线程池

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                         // corePoolSize
    10,                         // maximumPoolSize
    60,                          // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new LinkedBlockingQueue<>(100)   // workQueue
);
```

### 12.2 缓存线程池

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    0,                          // corePoolSize
    Integer.MAX_VALUE,             // maximumPoolSize
    60,                          // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new SynchronousQueue<>()         // workQueue
);
```

### 12.3 有界线程池

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    10,                         // maximumPoolSize
    60,                          // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new ArrayBlockingQueue<>(100)    // workQueue
);
```

### 12.4 自定义线程工厂

```java
ThreadFactory factory = new ThreadFactory() {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("Worker-" + count.incrementAndGet());
        thread.setDaemon(false);
        return thread;
    }
};

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    factory,
    new ThreadPoolExecutor.AbortPolicy());
```

### 12.5 自定义拒绝策略

```java
RejectedExecutionHandler handler = new RejectedExecutionHandler() {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        System.out.println("Task rejected: " + r);
        // 自定义处理逻辑
    }
};

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    handler);
```

## 十三、注意事项

### 13.1 合理设置参数

```java
// 推荐：根据任务类型设置参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    cpuCoreCount,                  // corePoolSize = CPU核心数
    cpuCoreCount * 2,             // maximumPoolSize = CPU核心数 * 2
    60,                           // keepAliveTime
    TimeUnit.SECONDS,               // unit
    new LinkedBlockingQueue<>(100)   // workQueue
);
```

### 13.2 正确关闭线程池

```java
// 推荐：优雅关闭
executor.shutdown();
executor.awaitTermination(60, TimeUnit.SECONDS);

// 不推荐：立即关闭
executor.shutdownNow();
```

### 13.3 正确处理异常

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100)) {
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            // 处理异常
        }
    }
};
```

### 13.4 正确使用钩子方法

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100)) {
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        // 任务执行前的处理
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        // 任务执行后的处理
    }

    @Override
    protected void terminated() {
        super.terminated();
        // 线程池终止后的处理
    }
};
```

### 13.5 正确选择队列

```java
// 推荐：根据任务类型选择队列
BlockingQueue<Runnable> queue;

// 需要立即执行：SynchronousQueue
queue = new SynchronousQueue<>();

// 独立任务：LinkedBlockingQueue
queue = new LinkedBlockingQueue<>();

// 需要控制资源：ArrayBlockingQueue
queue = new ArrayBlockingQueue<>(100);
```

## 十四、最佳实践

### 14.1 使用Executors工厂方法

```java
// 推荐：使用工厂方法
ExecutorService executor = Executors.newFixedThreadPool(10);
ExecutorService executor = Executors.newCachedThreadPool();
ExecutorService executor = Executors.newSingleThreadExecutor();

// 不推荐：手动配置(除非有特殊需求)
ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100));
```

### 14.2 合理设置线程池大小

```java
// CPU密集型：核心数 + 1
int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;

// IO密集型：核心数 * 2
int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
```

### 14.3 使用有界队列

```java
// 推荐：使用有界队列
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);

// 不推荐：使用无界队列
BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
```

### 14.4 正确关闭线程池

```java
// 推荐：优雅关闭
executor.shutdown();
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### 14.5 使用自定义线程工厂

```java
ThreadFactory factory = new ThreadFactory() {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("Worker-" + count.incrementAndGet());
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> {
            // 处理未捕获异常
        });
        return thread;
    }
};
```

## 十五、总结

ThreadPoolExecutor是Java并发编程的核心类，提供了灵活的线程池管理功能。

### 核心要点

1. **线程池管理**: 管理线程的创建、复用和销毁
2. **任务队列**: 使用BlockingQueue存储待执行任务
3. **拒绝策略**: 当任务无法执行时的处理策略
4. **核心线程数**: 保持活跃的最小线程数
5. **最大线程数**: 允许创建的最大线程数
6. **线程复用**: 复用空闲线程，避免频繁创建销毁
7. **钩子方法**: 提供beforeExecute、afterExecute等钩子方法
8. **状态管理**: RUNNING、SHUTDOWN、STOP、TIDYING、TERMINATED

### 适用场景

- 需要管理大量并发任务
- 需要控制线程数量
- 需要任务队列
- 需要拒绝策略
- 需要监控和统计

### 不适用场景

- 简单任务：使用Executors工厂方法
- 需要Fork/Join：使用ForkJoinPool
- 需要异步回调：使用CompletableFuture

### 性能特点

- 创建线程：O(1)
- 执行任务：O(1)平均
- 关闭线程池：O(n)
- 内存开销：取决于线程数和队列大小

### 与其他ExecutorService的选择

- 通用场景：ThreadPoolExecutor
- 简单场景：Executors工厂方法
- Fork/Join：ForkJoinPool
- 异步回调：CompletableFuture
