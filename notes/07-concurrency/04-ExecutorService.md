# ExecutorService 源码解读

## 类概述

`ExecutorService` 是 `Executor` 接口的扩展,提供了管理终止和跟踪异步任务进度的方法。它是 Java 并发包中线程池管理的核心接口。

```java
public interface ExecutorService extends Executor {
```

## 核心功能

### 1. 生命周期管理

ExecutorService 提供了完整的生命周期管理方法:

#### shutdown() - 优雅关闭

```java
void shutdown();
```

- **功能**: 启动有序关闭,执行已提交的任务,但不接受新任务
- **特点**: 
  - 不等待已提交任务完成
  - 如果已关闭则无效果
  - 需要配合 `awaitTermination()` 使用
- **安全检查**: 可能抛出 `SecurityException`

#### shutdownNow() - 强制关闭

```java
List<Runnable> shutdownNow();
```

- **功能**: 尝试停止所有正在执行的任务,停止等待任务的处理,返回等待执行的任务列表
- **特点**:
  - 不等待正在执行的任务终止
  - 返回未开始执行的任务列表
  - 通过 `Thread.interrupt()` 尝试停止任务
  - 不保证能成功停止所有任务
- **返回值**: 未开始执行的任务列表

#### isShutdown() - 检查关闭状态

```java
boolean isShutdown();
```

- **功能**: 返回执行器是否已关闭
- **返回值**: `true` 表示已关闭

#### isTerminated() - 检查终止状态

```java
boolean isTerminated();
```

- **功能**: 返回关闭后所有任务是否已完成
- **特点**: 只有在调用 `shutdown()` 或 `shutdownNow()` 后才可能返回 `true`

#### awaitTermination() - 等待终止

```java
boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
```

- **功能**: 阻塞直到所有任务在关闭请求后完成执行,或超时,或当前线程被中断
- **参数**:
  - `timeout`: 最大等待时间
  - `unit`: 时间单位
- **返回值**: 
  - `true`: 执行器已终止
  - `false`: 超时前未终止
- **异常**: `InterruptedException` - 等待时被中断

### 2. 任务提交

ExecutorService 提供了多种任务提交方式:

#### submit(Callable<T>) - 提交有返回值任务

```java
<T> Future<T> submit(Callable<T> task);
```

- **功能**: 提交一个返回值的任务,返回表示任务待完成结果的 Future
- **特点**:
  - Future 的 `get()` 方法在成功完成后返回任务结果
  - 可以立即阻塞等待: `result = exec.submit(aCallable).get();`
- **异常**:
  - `RejectedExecutionException`: 任务无法调度执行
  - `NullPointerException`: 任务为 null

#### submit(Runnable, T) - 提交带结果的任务

```java
<T> Future<T> submit(Runnable task, T result);
```

- **功能**: 提交 Runnable 任务,返回表示该任务的 Future
- **特点**: Future 的 `get()` 方法在成功完成后返回给定的 result
- **异常**:
  - `RejectedExecutionException`: 任务无法调度执行
  - `NullPointerException`: 任务为 null

#### submit(Runnable) - 提交无返回值任务

```java
Future<?> submit(Runnable task);
```

- **功能**: 提交 Runnable 任务,返回表示该任务的 Future
- **特点**: Future 的 `get()` 方法在成功完成后返回 `null`
- **异常**:
  - `RejectedExecutionException`: 任务无法调度执行
  - `NullPointerException`: 任务为 null

### 3. 批量执行

#### invokeAll() - 执行所有任务

```java
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
    throws InterruptedException;

<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                              long timeout, TimeUnit unit)
    throws InterruptedException;
```

- **功能**: 执行给定任务,返回所有任务完成时持有其状态和结果的 Future 列表
- **特点**:
  - 返回列表中每个元素的 `Future.isDone()` 都为 `true`
  - 完成的任务可能正常终止或抛出异常
  - 带超时版本: 超时后未完成的任务被取消
- **异常**:
  - `InterruptedException`: 等待时被中断,未完成的任务被取消
  - `NullPointerException`: 任务或任何元素为 null
  - `RejectedExecutionException`: 任何任务无法调度执行

#### invokeAny() - 执行任意一个任务

```java
<T> T invokeAny(Collection<? extends Callable<T>> tasks)
    throws InterruptedException, ExecutionException;

<T> T invokeAny(Collection<? extends Callable<T>> tasks,
                long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException;
```

- **功能**: 执行给定任务,返回其中一个成功完成的任务结果
- **特点**:
  - 返回成功完成(未抛出异常)的任务结果
  - 正常或异常返回时,未完成的任务被取消
  - 带超时版本: 超时前没有任务成功完成则抛出 `TimeoutException`
- **异常**:
  - `InterruptedException`: 等待时被中断
  - `ExecutionException`: 没有任务成功完成
  - `TimeoutException`: 超时前没有任务成功完成(仅带超时版本)
  - `NullPointerException`: 任务或任何元素为 null
  - `IllegalArgumentException`: 任务集合为空
  - `RejectedExecutionException`: 任务无法调度执行

## 使用示例

### 1. 网络服务示例

```java
class NetworkService implements Runnable {
  private final ServerSocket serverSocket;
  private final ExecutorService pool;

  public NetworkService(int port, int poolSize) throws IOException {
    serverSocket = new ServerSocket(port);
    pool = Executors.newFixedThreadPool(poolSize);
  }

  public void run() {
    try {
      for (;;) {
        pool.execute(new Handler(serverSocket.accept()));
      }
    } catch (IOException ex) {
      pool.shutdown();
    }
  }
}

class Handler implements Runnable {
  private final Socket socket;
  Handler(Socket socket) { this.socket = socket; }
  public void run() {
    // 读取并处理 socket 请求
  }
}
```

### 2. 优雅关闭示例

```java
void shutdownAndAwaitTermination(ExecutorService pool) {
  pool.shutdown(); // 禁止提交新任务
  try {
    // 等待现有任务终止
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
      pool.shutdownNow(); // 取消当前执行的任务
      // 等待任务响应取消
      if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
    }
  } catch (InterruptedException ie) {
    // (重新)取消如果当前线程也被中断
    pool.shutdownNow();
    // 保持中断状态
    Thread.currentThread().interrupt();
  }
}
```

## 内存一致性效果

在 `ExecutorService` 提交 `Runnable` 或 `Callable` 任务之前的操作,与该任务执行的操作之间存在 **happens-before** 关系,而任务执行的操作又与通过 `Future.get()` 检索结果之间存在 **happens-before** 关系。

## 设计模式

### 1. 模板方法模式

ExecutorService 定义了线程池操作的模板方法,具体实现由子类(如 `ThreadPoolExecutor`)完成。

### 2. 策略模式

不同的拒绝策略、任务队列策略等可以通过不同的实现类来配置。

### 3. 工厂模式

`Executors` 类提供了创建各种 ExecutorService 的工厂方法。

## 与 Executor 的区别

| 特性 | Executor | ExecutorService |
|------|----------|-----------------|
| 生命周期管理 | 无 | shutdown, shutdownNow, isShutdown, isTerminated, awaitTermination |
| 任务提交 | execute(Runnable) | submit(Runnable/Callable), invokeAll, invokeAny |
| 结果跟踪 | 无 | 返回 Future 对象 |
| 批量执行 | 无 | invokeAll, invokeAny |

## 关键点总结

1. **生命周期管理**: 提供了完整的线程池生命周期管理方法
2. **任务提交**: 支持多种任务提交方式,返回 Future 跟踪结果
3. **批量执行**: 提供 invokeAll 和 invokeAny 方法处理批量任务
4. **优雅关闭**: shutdown 优雅关闭,shutdownNow 强制关闭
5. **内存一致性**: 保证了任务提交、执行和结果获取的 happens-before 关系
6. **异常处理**: 明确定义了各种异常情况的处理方式
7. **灵活性**: 通过工厂方法和策略模式提供了高度的可配置性

## 相关类

- `Executor`: 基础执行器接口
- `ThreadPoolExecutor`: ExecutorService 的核心实现
- `Executors`: ExecutorService 的工厂类
- `Future`: 表示异步计算的结果
- `Callable`: 有返回值的任务接口
- `ExecutorCompletionService`: 自定义批量执行变体
