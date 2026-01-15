# Thread 源码解读

## 一、类概述

Thread是Java中用于执行线程的类。Java虚拟机允许应用程序有多个线程并发执行。

### 1.1 核心特性

- **线程执行**: 封装线程的执行逻辑
- **优先级**: 线程具有优先级，高优先级优先执行
- **守护线程**: 可以设置为守护线程
- **线程组**: 属于某个线程组
- **上下文类加载器**: 每个线程有自己的上下文类加载器
- **线程本地存储**: 支持ThreadLocal
- **中断机制**: 支持线程中断
- **生命周期**: NEW、RUNNABLE、RUNNING、BLOCKED、WAITING、TERMINATED

### 1.2 适用场景

- 需要多线程并发执行
- 需要控制线程执行顺序
- 需要后台任务执行
- 需要线程间通信

## 二、设计原理

### 2.1 线程状态

```java
// 线程状态机
NEW -> RUNNABLE -> RUNNING
              ↓
         BLOCKED/WAITING -> RUNNABLE
              ↓
         TERMINATED
```

**状态说明**:
- **NEW**: 新创建，尚未启动
- **RUNNABLE**: 可运行，等待CPU调度
- **RUNNING**: 正在执行
- **BLOCKED**: 等待获取锁
- **WAITING**: 等待notify/notifyAll
- **TERMINATED**: 线程结束

### 2.2 线程优先级

```java
// 优先级范围：1-10
// 优先级越高，越优先执行
Thread thread = new Thread();
thread.setPriority(Thread.MAX_PRIORITY);  // 10
```

**设计要点**:
- 优先级范围：1-10
- 默认优先级：5
- 高优先级不保证先执行

### 2.3 守护线程

```java
// 守护线程：JVM退出时不会等待其结束
Thread thread = new Thread();
thread.setDaemon(true);
```

**设计要点**:
- 守护线程：JVM退出时不等待
- 非守护线程：JVM退出时等待其结束
- 默认：非守护线程

### 2.4 线程组

```java
// 线程组：线程的集合，可以统一管理
ThreadGroup group = new ThreadGroup("MyGroup");
Thread thread = new Thread(group, runnable);
```

## 三、继承结构

```
Object
    ↑
Thread
    implements Runnable
```

### 3.1 接口实现

- **Runnable**: 定义线程执行逻辑

## 四、核心字段

### 4.1 线程名称

```java
private volatile char name[];
```

**字段设计要点**:
- 线程名称，用于标识
- volatile修饰，保证可见性
- 默认名称：Thread-n

### 4.2 优先级

```java
private int priority;
```

**字段设计要点**:
- 线程优先级，范围1-10
- 默认值：5
- 影响调度顺序

### 4.3 线程组

```java
private ThreadGroup group;
```

**字段设计要点**:
- 线程所属的线程组
- 用于统一管理线程
- 默认：父线程的线程组

### 4.4 目标任务

```java
private Runnable target;
```

**字段设计要点**:
- 线程要执行的任务
- 实现Runnable接口
- 可以为null

### 4.5 守护线程标志

```java
private boolean daemon = false;
```

**字段设计要点**:
- 是否为守护线程
- 默认值：false
- 影响JVM退出行为

### 4.6 线程本地存储

```java
ThreadLocal.ThreadLocalMap threadLocals = null;
ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
```

**字段设计要点**:
- 存储线程本地变量
- inheritableThreadLocals：可继承的线程本地变量
- 由ThreadLocal类维护

### 4.7 上下文类加载器

```java
private ClassLoader contextClassLoader;
```

**字段设计要点**:
- 线程的上下文类加载器
- 用于加载类和资源
- 默认：父线程的上下文类加载器

### 4.8 栈大小

```java
private long stackSize;
```

**字段设计要点**:
- 线程栈大小
- 影响递归深度
- 0表示使用默认值

## 五、构造方法

### 5.1 默认构造方法

```java
public Thread() {
    init(null, null, "Thread-" + nextThreadNum(), 0);
}
```

**方法要点**:
- 创建新线程
- 自动生成名称
- 默认优先级：5
- 默认守护线程：false

**示例**:
```java
Thread thread = new Thread();
thread.start();
```

### 5.2 指定Runnable的构造方法

```java
public Thread(Runnable target) {
    init(null, target, "Thread-" + nextThreadNum(), 0);
}
```

**方法要点**:
- 创建新线程并指定任务
- 自动生成名称
- 默认优先级：5

**示例**:
```java
Runnable task = () -> {
    System.out.println("Hello from thread");
};
Thread thread = new Thread(task);
thread.start();
```

### 5.3 指定名称的构造方法

```java
public Thread(String name) {
    init(null, null, name, 0);
}
```

**方法要点**:
- 创建新线程并指定名称
- 默认优先级：5

**示例**:
```java
Thread thread = new Thread("MyThread");
thread.start();
```

### 5.4 指定Runnable和名称的构造方法

```java
public Thread(Runnable target, String name) {
    init(null, target, name, 0);
}
```

**方法要点**:
- 创建新线程并指定任务和名称
- 默认优先级：5

**示例**:
```java
Runnable task = () -> System.out.println("Task");
Thread thread = new Thread(task, "WorkerThread");
thread.start();
```

### 5.5 指定线程组的构造方法

```java
public Thread(ThreadGroup group, Runnable target, String name) {
    init(group, target, name, 0);
}
```

**方法要点**:
- 创建新线程并指定线程组、任务和名称
- 默认优先级：5

**示例**:
```java
ThreadGroup group = new ThreadGroup("MyGroup");
Runnable task = () -> System.out.println("Task");
Thread thread = new Thread(group, task, "WorkerThread");
thread.start();
```

### 5.6 指定栈大小的构造方法

```java
public Thread(ThreadGroup group, Runnable target, String name,
               long stackSize) {
    init(group, target, name, stackSize);
}
```

**方法要点**:
- 创建新线程并指定栈大小
- 栈大小影响递归深度
- 0表示使用默认值

**示例**:
```java
Thread thread = new Thread(null, null, "DeepThread", 1024 * 1024);
thread.start();
```

## 六、核心方法

### 6.1 start - 启动线程

```java
public synchronized void start() {
    if (threadStatus != 0)
        throw new IllegalThreadStateException();

    group.add(this);

    boolean started = false;
    try {
        start0();
        started = true;
    } finally {
        if (!started) {
            group.threadStartFailed(this);
        }
    }
}

private native void start0();
```

**方法要点**:
- 只能启动一次
- 调用native方法start0()
- 添加到线程组
- 时间复杂度：O(1)

**示例**:
```java
Thread thread = new Thread(() -> System.out.println("Running"));
thread.start();
```

### 6.2 run - 执行线程

```java
@Override
public void run() {
    if (target != null) {
        target.run();
    }
}
```

**方法要点**:
- 执行target的run方法
- 子类可以重写此方法
- 默认什么都不做

**示例**:
```java
Thread thread = new Thread() {
    @Override
    public void run() {
        System.out.println("Thread is running");
    }
};
thread.start();
```

### 6.3 sleep - 睡眠

```java
public static void sleep(long millis) throws InterruptedException {
    sleep(millis, 0);
}

public static void sleep(long millis, int nanos)
    throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                            "nanosecond timeout value out of range");
    }

    if (millis == 0 && nanos == 0) {
        return; // 立即返回
    }

    JNANativeSupport.sleepNanos(millis, nanos);
}
```

**方法要点**:
- 静态方法
- 让当前线程休眠指定时间
- 抛出InterruptedException
- 时间精度：毫秒+纳秒

**示例**:
```java
try {
    Thread.sleep(1000);  // 睡眠1秒
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 6.4 join - 等待线程结束

```java
public final void join() throws InterruptedException {
    join(0);
}

public final void join(long millis) throws InterruptedException {
    join(0, millis);
}

public final void join(long millis, int nanos)
    throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                            "nanosecond timeout value out of range");
    }

    if (millis == 0 && nanos == 0) {
        if (isAlive()) {
            while (true) {
                long delay = blocker.blockOnDeath(this, 0);
                if (delay > 0) {
                    return; // timeout
                }
            }
        }
        return;
    }

    // Fast path for zero nanos
    long base = System.currentTimeMillis();
    long now = base;
    long deadline = millis + base;
    if (millis == 0) {
        deadline = base; // avoid overflow
    }
    delay = blocker.blockOnDeath(this, deadline - now);
    if (delay > 0) {
        throw new InterruptedException("timeout");
    }
}
```

**方法要点**:
- 等待线程结束
- 可以指定超时时间
- 抛出InterruptedException
- 时间精度：毫秒+纳秒

**示例**:
```java
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        // 处理中断
    }
});
thread.start();
thread.join();  // 等待线程结束
```

### 6.5 interrupt - 中断线程

```java
public void interrupt() {
    if (this != Thread.currentThread())
        checkAccess();

    synchronized (blockerLock) {
        Interruptible b = blocker;
        if (b != null) {
            interrupt0();  // Just to set the interrupt flag
            b.interrupt(this);
            return;
        }
    }

    interrupt0();
}
```

**方法要点**:
- 设置中断标志
- 唤醒等待/休眠的线程
- 抛出InterruptedException
- 不保证立即停止线程

**示例**:
```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
});
thread.start();
thread.interrupt();  // 中断线程
```

### 6.6 yield - 让出CPU

```java
public static void yield() {
    Thread t = currentThread();
    if (t.priority == t.getPriority())
        return;
    yield0();
}

private static native void yield0();
```

**方法要点**:
- 静态方法
- 让当前线程让出CPU
- 只是建议，不保证
- 用于调度优化

**示例**:
```java
for (int i = 0; i < 10; i++) {
    Thread.yield();  // 让出CPU
}
```

### 6.7 stop - 停止线程（已过时）

```java
@Deprecated
public final void stop() {
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        checkAccess();
        if (this != Thread.currentThread()) {
            security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
        }
    }

    if (threadStatus != 0) {
        resume(); // 唤醒线程如果被挂起
    }

    stop0(new ThreadDeath());
}

private native void stop0(Throwable o);
```

**方法要点**:
- 已过时，不推荐使用
- 强制停止线程
- 可能导致数据不一致
- 抛出ThreadDeath

**示例**:
```java
// 不推荐
thread.stop();
```

## 七、辅助方法

### 7.1 currentThread - 获取当前线程

```java
public static native Thread currentThread();
```

**方法要点**:
- 静态native方法
- 返回当前执行的线程

**示例**:
```java
Thread current = Thread.currentThread();
System.out.println(current.getName());
```

### 7.2 isAlive - 检查线程是否存活

```java
public final native boolean isAlive();
```

**方法要点**:
- native方法
- 检查线程是否已启动且未结束

**示例**:
```java
Thread thread = new Thread(() -> {});
thread.start();
System.out.println(thread.isAlive());  // true
```

### 7.3 isInterrupted - 检查是否被中断

```java
public boolean isInterrupted() {
    return interruptor != 0;
}
```

**方法要点**:
- 检查中断标志
- 非线程安全

**示例**:
```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
});
thread.start();
thread.interrupt();
```

### 7.4 getState - 获取线程状态

```java
public enum State {
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,
    TERMINATED;
}

public State getState() {
    return sun.misc.VM.toThreadState(threadStatus);
}
```

**方法要点**:
- 返回线程的当前状态
- JDK 6新增
- 便于调试和监控

**示例**:
```java
Thread thread = new Thread();
System.out.println(thread.getState());  // NEW
thread.start();
System.out.println(thread.getState());  // RUNNABLE
```

## 八、设计模式

### 8.1 模板方法模式

Thread定义了run方法作为模板：

```java
public class Thread implements Runnable {
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }
}
```

**设计要点**:
- 子类重写run方法
- 执行target的run方法
- 灵活控制执行逻辑

### 8.2 工厂模式

通过构造方法创建线程：

```java
// 创建线程的工厂方法
public Thread createThread(Runnable task) {
    return new Thread(task);
}
```

## 九、面试常见问题

### 9.1 Thread的run和start的区别?

| 特性 | run | start |
|------|-----|-------|
| 作用 | 执行线程逻辑 | 启动线程 |
| 调用次数 | 可以多次调用 | 只能调用一次 |
| 线程状态 | 不改变 | NEW -> RUNNABLE |
| 执行线程 | 当前线程执行 | 新线程执行 |

### 9.2 Thread的sleep和wait的区别?

| 特性 | sleep | wait |
|------|------|-----|
| 所属类 | Thread | Object |
| 释放锁 | 不释放 | 释放 |
| 唤醒方式 | 时间到或中断 | notify/notifyAll或中断 |
| 使用场景 | 暂停 | 线程间通信 |

### 9.3 Thread的interrupt的作用?

- 设置中断标志
- 唤醒等待/休眠的线程
- 抛出InterruptedException
- 不保证立即停止线程

### 9.4 Thread的join的作用?

- 等待线程结束
- 可以指定超时时间
- 抛出InterruptedException
- 用于线程同步

### 9.5 Thread的yield的作用?

- 让出CPU
- 只是建议，不保证
- 用于调度优化
- 不释放锁

### 9.6 守护线程的特点?

- JVM退出时不等待其结束
- 用于后台任务
- GC线程是守护线程
- 默认：非守护线程

### 9.7 Thread的优先级?

- 范围：1-10
- 默认：5
- 高优先级不保证先执行
- 影响调度顺序

### 9.8 Thread的线程组?

- 线程的集合
- 可以统一管理
- 有层次结构
- 用于批量操作

## 十、使用场景

### 10.1 创建线程

```java
// 方式1：继承Thread
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread is running");
    }
}

MyThread thread = new MyThread();
thread.start();

// 方式2：实现Runnable
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task is running");
    }
}

Thread thread = new Thread(new MyTask());
thread.start();
```

### 10.2 等待线程结束

```java
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        // 处理中断
    }
});
thread.start();
thread.join();  // 等待线程结束
```

### 10.3 中断线程

```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
});
thread.start();
thread.interrupt();  // 中断线程
```

### 10.4 设置守护线程

```java
Thread thread = new Thread(() -> {
    while (true) {
        // 后台任务
    }
});
thread.setDaemon(true);  // 设置为守护线程
thread.start();
```

### 10.5 设置线程优先级

```java
Thread thread = new Thread(() -> {
    // 高优先级任务
});
thread.setPriority(Thread.MAX_PRIORITY);  // 10
thread.start();
```

## 十一、注意事项

### 11.1 不要使用stop方法

```java
// 不推荐：已过时，不安全
thread.stop();

// 推荐：使用中断
thread.interrupt();
```

### 11.2 正确处理中断

```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
});
thread.start();
thread.interrupt();
```

### 11.3 正确使用join

```java
Thread thread = new Thread(() -> {});
thread.start();
thread.join();  // 等待线程结束
```

### 11.4 正确使用sleep

```java
try {
    Thread.sleep(1000);  // 睡眠1秒
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 11.5 线程安全

```java
// Thread本身不是线程安全的
// 需要外部同步
synchronized (lock) {
    // 操作共享资源
}
```

## 十二、最佳实践

### 12.1 优先使用Runnable

```java
// 推荐：使用Runnable
class MyTask implements Runnable {
    @Override
    public void run() {
        // 执行任务
    }
}

Thread thread = new Thread(new MyTask());

// 不推荐：继承Thread
class MyThread extends Thread {
    @Override
    public void run() {
        // 执行任务
    }
}
```

### 12.2 正确处理中断

```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
});
thread.start();
thread.interrupt();
```

### 12.3 使用线程池

```java
// 推荐：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> {
    // 执行任务
});

// 不推荐：手动创建线程
for (int i = 0; i < 100; i++) {
    new Thread(() -> {}).start();
}
```

### 12.4 使用ThreadLocal

```java
ThreadLocal<String> threadLocal = new ThreadLocal<>();

Thread thread = new Thread(() -> {
    threadLocal.set("value");
    System.out.println(threadLocal.get());
});
thread.start();
```

### 12.5 正确使用join

```java
Thread thread = new Thread(() -> {});
thread.start();
thread.join();  // 等待线程结束
```

## 十三、总结

Thread是Java中用于执行线程的类，封装了线程的执行逻辑和生命周期管理。

### 核心要点

1. **线程执行**: 封装线程的执行逻辑
2. **生命周期**: NEW、RUNNABLE、RUNNING、BLOCKED、WAITING、TERMINATED
3. **优先级**: 范围1-10，影响调度顺序
4. **守护线程**: JVM退出时不等待其结束
5. **中断机制**: 支持线程中断
6. **线程组**: 用于统一管理线程
7. **线程本地存储**: 支持ThreadLocal

### 适用场景

- 需要多线程并发执行
- 需要控制线程执行顺序
- 需要后台任务执行
- 需要线程间通信

### 不适用场景

- 简单任务：使用线程池
- 需要高性能：使用异步框架
- 需要复杂调度：使用ExecutorService

### 性能特点

- 创建线程：O(1)
- 启动线程：O(1)
- 上下文切换：取决于操作系统
- 内存开销：每个线程约1MB栈空间

### 与Runnable的选择

- 需要继承Thread：继承Thread
- 需要复用逻辑：实现Runnable
- 推荐：实现Runnable
