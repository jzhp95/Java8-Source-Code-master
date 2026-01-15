# Runnable 源码解读

## 一、接口概述

Runnable接口应该由任何打算由线程执行的类实现。该类必须定义一个名为run的无参方法。

### 1.1 核心特性

- **函数式接口**: JDK 8引入，支持Lambda表达式
- **单一方法**: 只有一个抽象方法run()
- **线程执行**: 用于定义线程的执行逻辑
- **无返回值**: run方法不返回值
- **无参数**: run方法不接受参数
- **轻量级**: 接口简单，易于实现
- **广泛使用**: Java并发编程的基础接口

### 1.2 适用场景

- 需要在线程中执行任务
- 需要使用Lambda表达式
- 需要传递任务给线程池
- 需要定义可执行逻辑

## 二、设计原理

### 2.1 函数式接口

```java
@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
```

**设计要点**:
- @FunctionalInterface注解
- 只有一个抽象方法
- 支持Lambda表达式
- 支持方法引用

### 2.2 与Thread的关系

```java
// Thread实现了Runnable接口
public class Thread implements Runnable {
    private Runnable target;

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }
}
```

**设计要点**:
- Thread实现了Runnable
- Thread可以持有Runnable目标
- Thread的run方法调用target的run方法

### 2.3 与Callable的区别

```java
// Runnable: 无返回值，不抛出受检异常
public interface Runnable {
    void run();
}

// Callable: 有返回值，抛出受检异常
public interface Callable<V> {
    V call() throws Exception;
}
```

**设计要点**:
- Runnable: 简单任务
- Callable: 复杂任务，需要返回值

## 三、接口定义

### 3.1 接口声明

```java
@FunctionalInterface
public interface Runnable {
    /**
     * 当一个实现Runnable接口的对象被用来创建线程时，
     * 启动线程会导致在该单独执行的线程中调用对象的run方法。
     * <p>
     * run方法的一般约定是它可以采取任何操作。
     *
     * @see java.lang.Thread#run()
     */
    public abstract void run();
}
```

**接口要点**:
- 函数式接口
- 单一抽象方法
- 无返回值
- 无参数
- 不抛出受检异常

## 四、使用方式

### 4.1 实现Runnable接口

```java
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task is running");
    }
}

// 使用
Runnable task = new MyTask();
Thread thread = new Thread(task);
thread.start();
```

### 4.2 使用匿名类

```java
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Anonymous task is running");
    }
};

Thread thread = new Thread(task);
thread.start();
```

### 4.3 使用Lambda表达式

```java
Runnable task = () -> {
    System.out.println("Lambda task is running");
};

Thread thread = new Thread(task);
thread.start();
```

### 4.4 使用方法引用

```java
class TaskExecutor {
    public void executeTask() {
        System.out.println("Task is running");
    }
}

TaskExecutor executor = new TaskExecutor();
Runnable task = executor::executeTask;

Thread thread = new Thread(task);
thread.start();
```

### 4.5 传递给线程池

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

Runnable task = () -> {
    System.out.println("Task is running in thread pool");
};

executor.submit(task);
```

## 五、与Thread的关系

### 5.1 Thread实现Runnable

```java
public class Thread implements Runnable {
    private Runnable target;

    public Thread(Runnable target) {
        this.target = target;
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }
}
```

**关系说明**:
- Thread实现了Runnable
- Thread可以持有Runnable目标
- Thread的run方法调用target的run方法

### 5.2 使用Runnable创建Thread

```java
// 方式1：直接传递Runnable
Runnable task = () -> System.out.println("Task");
Thread thread = new Thread(task);
thread.start();

// 方式2：继承Thread并重写run
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread is running");
    }
}

Thread thread = new MyThread();
thread.start();
```

### 5.3 Runnable的优势

| 特性 | Runnable | 继承Thread |
|------|----------|-------------|
| 继承 | 不需要继承 | 需要继承Thread |
| 复用 | 可以复用逻辑 | 不易复用 |
| 灵活性 | 高 | 低 |
| 推荐 | 推荐 | 不推荐 |

## 六、与Callable的区别

### 6.1 接口定义

```java
// Runnable
@FunctionalInterface
public interface Runnable {
    void run();
}

// Callable
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

### 6.2 主要区别

| 特性 | Runnable | Callable |
|------|----------|----------|
| 返回值 | 无 | 有 |
| 异常 | 不抛出受检异常 | 抛出受检异常 |
| 使用场景 | 简单任务 | 复杂任务 |
| 线程池 | submit(Runnable) | submit(Callable) |

### 6.3 使用示例

```java
// Runnable示例
Runnable runnableTask = () -> {
    System.out.println("Runnable task");
};
executor.submit(runnableTask);

// Callable示例
Callable<String> callableTask = () -> {
    return "Callable result";
};
Future<String> future = executor.submit(callableTask);
String result = future.get();
```

## 七、设计模式

### 7.1 命令模式

Runnable是命令模式的典型应用：

```java
// 命令接口
public interface Runnable {
    void run();
}

// 具体命令
class PrintCommand implements Runnable {
    private String message;

    public PrintCommand(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        System.out.println(message);
    }
}

// 调用者
class ThreadInvoker {
    private Thread thread;

    public void invoke(Runnable command) {
        thread = new Thread(command);
        thread.start();
    }
}
```

### 7.2 策略模式

Runnable可以作为策略使用：

```java
// 策略接口
public interface Runnable {
    void run();
}

// 具体策略
class FastStrategy implements Runnable {
    @Override
    public void run() {
        System.out.println("Fast strategy");
    }
}

class SlowStrategy implements Runnable {
    @Override
    public void run() {
        System.out.println("Slow strategy");
    }
}

// 上下文
class Context {
    private Runnable strategy;

    public void setStrategy(Runnable strategy) {
        this.strategy = strategy;
    }

    public void execute() {
        new Thread(strategy).start();
    }
}
```

## 八、面试常见问题

### 8.1 Runnable和Thread的区别?

| 特性 | Runnable | Thread |
|------|----------|---------|
| 类型 | 接口 | 类 |
| 继承 | 不需要继承 | 需要继承Thread |
| 复用 | 可以复用逻辑 | 不易复用 |
| 灵活性 | 高 | 低 |
| 推荐 | 推荐 | 不推荐 |

### 8.2 Runnable和Callable的区别?

| 特性 | Runnable | Callable |
|------|----------|----------|
| 返回值 | 无 | 有 |
| 异常 | 不抛出受检异常 | 抛出受检异常 |
| 使用场景 | 简单任务 | 复杂任务 |

### 8.3 为什么推荐使用Runnable?

1. **避免单继承**: Java不支持多继承
2. **逻辑复用**: Runnable可以复用
3. **灵活性高**: 可以传递给线程池
4. **解耦合**: 任务与线程分离

### 8.4 Runnable的run方法可以抛出异常吗?

可以抛出非受检异常，但不能抛出受检异常：

```java
Runnable task = () -> {
    // 可以抛出非受检异常
    throw new RuntimeException("Error");

    // 不能抛出受检异常
    // throw new IOException("Error");  // 编译错误
};
```

### 8.5 Runnable的run方法有返回值吗?

没有。run方法返回void：

```java
public abstract void run();
```

### 8.6 如何获取Runnable的执行结果?

Runnable本身不返回结果，需要使用其他机制：

```java
// 方式1：使用共享变量
final int[] result = new int[1];
Runnable task = () -> {
    result[0] = 42;
};
new Thread(task).start();
// 等待线程结束
System.out.println(result[0]);

// 方式2：使用Callable
Callable<Integer> task = () -> 42;
Future<Integer> future = executor.submit(task);
Integer result = future.get();
```

### 8.7 Runnable支持Lambda表达式吗?

支持。Runnable是函数式接口：

```java
Runnable task = () -> {
    System.out.println("Lambda task");
};
```

### 8.8 Runnable可以重复执行吗?

可以。Runnable可以被多次执行：

```java
Runnable task = () -> {
    System.out.println("Task");
};

// 执行多次
new Thread(task).start();
new Thread(task).start();
new Thread(task).start();
```

## 九、使用场景

### 9.1 简单任务

```java
Runnable task = () -> {
    System.out.println("Simple task");
};

Thread thread = new Thread(task);
thread.start();
```

### 9.2 后台任务

```java
Runnable backgroundTask = () -> {
    while (true) {
        // 执行后台任务
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            break;
        }
    }
};

Thread thread = new Thread(backgroundTask);
thread.setDaemon(true);
thread.start();
```

### 9.3 批量任务

```java
List<Runnable> tasks = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    final int index = i;
    tasks.add(() -> {
        System.out.println("Task " + index);
    });
}

ExecutorService executor = Executors.newFixedThreadPool(5);
for (Runnable task : tasks) {
    executor.submit(task);
}
```

### 9.4 定时任务

```java
Runnable task = () -> {
    System.out.println("Scheduled task");
};

ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
```

### 9.5 异步任务

```java
Runnable task = () -> {
    System.out.println("Async task");
};

ExecutorService executor = Executors.newCachedThreadPool();
executor.submit(task);
```

## 十、注意事项

### 10.1 run方法不抛出受检异常

```java
// 不正确：抛出受检异常
Runnable task = () -> {
    throw new IOException("Error");  // 编译错误
};

// 正确：抛出非受检异常
Runnable task = () -> {
    throw new RuntimeException("Error");  // OK
};
```

### 10.2 run方法无返回值

```java
// Runnable的run方法返回void
public abstract void run();
```

### 10.3 正确处理异常

```java
Runnable task = () -> {
    try {
        // 可能抛出异常的代码
    } catch (Exception e) {
        // 处理异常
    }
};
```

### 10.4 正确使用线程池

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

Runnable task = () -> {
    System.out.println("Task");
};

executor.submit(task);
executor.shutdown();
```

### 10.5 避免共享状态

```java
// 不推荐：共享状态
int counter = 0;
Runnable task = () -> {
    counter++;  // 线程不安全
};

// 推荐：使用线程安全的方式
AtomicInteger counter = new AtomicInteger(0);
Runnable task = () -> {
    counter.incrementAndGet();
};
```

## 十一、最佳实践

### 11.1 优先使用Runnable

```java
// 推荐：使用Runnable
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task");
    }
}

// 不推荐：继承Thread
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread");
    }
}
```

### 11.2 使用Lambda表达式

```java
// 推荐：使用Lambda
Runnable task = () -> {
    System.out.println("Task");
};

// 不推荐：使用匿名类
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Task");
    }
};
```

### 11.3 正确处理异常

```java
Runnable task = () -> {
    try {
        // 可能抛出异常的代码
    } catch (Exception e) {
        // 处理异常
    }
};
```

### 11.4 使用线程池

```java
// 推荐：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> {
    System.out.println("Task");
});

// 不推荐：手动创建线程
new Thread(() -> {
    System.out.println("Task");
}).start();
```

### 11.5 避免共享状态

```java
// 推荐：使用线程安全的方式
AtomicInteger counter = new AtomicInteger(0);
Runnable task = () -> {
    counter.incrementAndGet();
};
```

## 十二、总结

Runnable是Java并发编程的基础接口，用于定义线程的执行逻辑。

### 核心要点

1. **函数式接口**: JDK 8引入，支持Lambda表达式
2. **单一方法**: 只有一个抽象方法run()
3. **无返回值**: run方法不返回值
4. **无参数**: run方法不接受参数
5. **轻量级**: 接口简单，易于实现
6. **广泛使用**: Java并发编程的基础接口

### 适用场景

- 需要在线程中执行任务
- 需要使用Lambda表达式
- 需要传递任务给线程池
- 需要定义可执行逻辑

### 不适用场景

- 需要返回值(使用Callable)
- 需要抛出受检异常(使用Callable)
- 需要复杂任务(使用Callable)

### 与其他接口的选择

- 简单任务: Runnable
- 复杂任务: Callable
- 需要返回值: Callable
- 不需要返回值: Runnable

### 与Thread的选择

- 推荐使用: Runnable
- 不推荐: 继承Thread
- 灵活性: Runnable更高
- 复用性: Runnable更好
