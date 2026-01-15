# ForkJoinPool 源码解读

## 类概述

`ForkJoinPool` 是一个用于执行 `ForkJoinTask` 的 `ExecutorService` 实现。它采用 **工作窃取(work-stealing)** 算法,是 Java 7 引入的并行计算框架的核心。

```java
@sun.misc.Contended
public class ForkJoinPool extends AbstractExecutorService {
```

## 核心特性

### 1. 工作窃取算法

ForkJoinPool 与其他 ExecutorService 的主要区别在于采用工作窃取:

- 所有线程尝试查找和执行提交到池中的任务以及其他活动任务创建的子任务
- 如果没有任务,线程会阻塞等待
- 当大多数任务生成其他子任务时,这种设计能够高效处理
- 也适用于从外部客户端提交大量小任务

### 2. Common Pool

静态的 `commonPool()` 可用于大多数应用程序:

```java
static final ForkJoinPool common;
```

- 任何未显式提交到指定池的 ForkJoinTask 都使用公共池
- 减少资源使用(线程在不使用期间缓慢回收,后续使用时重新激活)
- 可以通过系统属性配置:
  - `java.util.concurrent.ForkJoinPool.common.parallelism`: 并行级别
  - `java.util.concurrent.ForkJoinPool.common.threadFactory`: 线程工厂
  - `java.util.concurrent.ForkJoinPool.common.exceptionHandler`: 异常处理器

### 3. 动态线程管理

池通过动态添加、挂起或恢复内部工作线程来维持足够的活动线程:

- 默认并行度等于可用处理器数量
- 即使某些任务因等待其他任务而停滞,也会尝试调整
- 对于阻塞 I/O 或其他未管理的同步,不保证调整成功

## 核心数据结构

### WorkQueue - 工作窃取队列

```java
@sun.misc.Contended
static final class WorkQueue {
    volatile int scanState;    // 版本化,<0: 非活动; 奇数: 扫描中
    int stackPred;             // 池栈(ctl)前驱
    int nsteals;               // 窃取数量
    int hint;                  // 随机化和窃取者索引提示
    int config;                // 池索引和模式
    volatile int qlock;        // 1: 锁定, < 0: 终止; 否则 0
    volatile int base;         // 下一个 poll 槽的索引
    int top;                   // 下一个 push 槽的索引
    ForkJoinTask<?>[] array;   // 元素数组(初始未分配)
    final ForkJoinPool pool;   // 包含的池(可能为 null)
    final ForkJoinWorkerThread owner; // 拥有者线程或共享时为 null
    volatile Thread parker;    // == 调用 park 时的 owner; 否则 null
    volatile ForkJoinTask<?> currentJoin;  // awaitJoin 中正在等待的任务
    volatile ForkJoinTask<?> currentSteal; // 主要由 helpStealer 使用
}
```

#### 队列操作

WorkQueue 是特殊的双端队列,支持三种端操作:

1. **push** - 推入任务(仅拥有者线程)
2. **pop** - 弹出任务(LIFO,仅拥有者线程)
3. **poll** - 窃取任务(FIFO,其他线程)

#### push 操作

```java
final void push(ForkJoinTask<?> task) {
    ForkJoinTask<?>[] a; ForkJoinPool p;
    int b = base, s = top, n;
    if ((a = array) != null) {
        int m = a.length - 1;
        U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
        U.putOrderedInt(this, QTOP, s + 1);
        if ((n = s - b) <= 1) {
            if ((p = pool) != null)
                p.signalWork(p.workQueues, this);
        }
        else if (n >= m)
            growArray();
    }
}
```

- 使用有序写入保证任务可见性
- 任务数 <= 1 时信号通知工作线程
- 队列满时扩容

#### pop 操作(LIFO)

```java
final ForkJoinTask<?> pop() {
    ForkJoinTask<?>[] a; ForkJoinTask<?> t; int m;
    if ((a = array) != null && (m = a.length - 1) >= 0) {
        for (int s; (s = top - 1) - base >= 0;) {
            long j = ((m & s) << ASHIFT) + ABASE;
            if ((t = (ForkJoinTask<?>)U.getObject(a, j)) == null)
                break;
            if (U.compareAndSwapObject(a, j, t, null)) {
                U.putOrderedInt(this, QTOP, s);
                return t;
            }
        }
    }
    return null;
}
```

- 仅由拥有者线程调用
- 从 top 端弹出,利用栈的局部性
- 使用 CAS 保证原子性

#### poll 操作(FIFO)

```java
final ForkJoinTask<?> poll() {
    ForkJoinTask<?>[] a; int b; ForkJoinTask<?> t;
    while ((b = base) - top < 0 && (a = array) != null) {
        int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
        t = (ForkJoinTask<?>)U.getObjectVolatile(a, j);
        if (base == b) {
            if (t != null) {
                if (U.compareAndSwapObject(a, j, t, null)) {
                    base = b + 1;
                    return t;
                }
            }
            else if (b + 1 == top)
                break;
        }
    }
    return null;
}
```

- 通常由窃取者线程调用
- 从 base 端获取,减少竞争
- 需要检查 base 是否变化

## 核心控制字段

### ctl - 主控制变量

```java
volatile long ctl;
```

ctl 包含 64 位,打包了 4 个 16 位子字段:

- **AC** (Active Count): 活动工作线程数减去目标并行度
- **TC** (Total Count): 总工作线程数减去目标并行度
- **SS** (Stack State): 顶部等待线程的版本计数和状态
- **ID** (Index): Treiber 等待者栈顶的池索引

#### ctl 常量

```java
// 活动计数
private static final int  AC_SHIFT   = 48;
private static final long AC_UNIT    = 0x0001L << AC_SHIFT;
private static final long AC_MASK    = 0xffffL << AC_SHIFT;

// 总计数
private static final int  TC_SHIFT   = 32;
private static final long TC_UNIT    = 0x0001L << TC_SHIFT;
private static final long TC_MASK    = 0xffffL << TC_SHIFT;
private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15);
```

#### 判断规则

- ac < 0: 活动工作线程不足
- tc < 0: 总工作线程不足
- sp != 0: 有等待的工作线程

### runState - 运行状态

```java
volatile int runState;
```

```java
private static final int  RSLOCK     = 1;        // 锁定位
private static final int  RSIGNAL    = 1 << 1;   // 信号位
private static final int  STARTED    = 1 << 2;   // 已启动
private static final int  STOP       = 1 << 29;  // 停止
private static final int  TERMINATED = 1 << 30;  // 已终止
private static final int  SHUTDOWN   = 1 << 31;  // 关闭(必须为负)
```

### workQueues - 工作队列数组

```java
volatile WorkQueue[] workQueues;
```

- 工作队列存储在 workQueues 数组中
- 数组大小始终是 2 的幂
- 工作者队列位于奇数索引
- 共享(提交)队列位于偶数索引,最多 64 个槽

## 线程管理

### 创建工作线程

```java
private boolean createWorker() {
    ForkJoinWorkerThreadFactory fac = factory;
    Throwable ex = null;
    ForkJoinWorkerThread wt = null;
    try {
        if (fac != null && (wt = fac.newThread(this)) != null) {
            wt.start();
            return true;
        }
    } catch (Throwable rex) {
        ex = rex;
    }
    deregisterWorker(wt, ex);
    return false;
}
```

- 通过工厂创建 ForkJoinWorkerThread
- 启动线程
- 失败时注销工作线程

### 线程状态管理

#### scanState - 扫描状态

```java
static final int SCANNING     = 1;        // 运行任务时为 false
static final int INACTIVE     = 1 << 31;  // 必须为负
static final int SS_SEQ       = 1 << 16;  // 版本计数
```

- **SCANNING**: 扫描任务
- **INACTIVE**: 非活动(可能阻塞等待信号)
- **SS_SEQ**: 版本计数,防止 ABA 问题

#### 队列空闲工作线程

- 使用 Treiber 栈结构
- 按最近使用顺序激活线程
- 改善性能和局部性

### 线程补偿(Compensation)

当工作线程因等待其他任务而阻塞时:

1. **帮助(Helping)**: 安排等待者执行如果没有被窃取时它会运行的任务
2. **补偿(Compensating)**: 如果没有足够的活动线程,创建或重新激活备用线程

#### helpStealer - 线性帮助

```java
// 每个工作线程记录:
volatile ForkJoinTask<?> currentSteal;  // 最近窃取的任务
volatile ForkJoinTask<?> currentJoin;   // 当前正在等待的任务
```

- 使用这些标记尝试找到要帮助的工作线程
- 窃取并执行能够加速完成当前等待任务的任务
- 保守的"线性帮助"方法

## 任务执行方法

### 任务执行方法总结

| 操作 | 从非 fork/join 客户端调用 | 从 fork/join 计算内部调用 |
|------|-------------------------|------------------------|
| 安排异步执行 | `execute(ForkJoinTask)` | `ForkJoinTask.fork` |
| 等待并获取结果 | `invoke(ForkJoinTask)` | `ForkJoinTask.invoke` |
| 安排执行并获取 Future | `submit(ForkJoinTask)` | `ForkJoinTask.fork` |

### runTask - 运行任务

```java
final void runTask(ForkJoinTask<?> task) {
    if (task != null) {
        scanState &= ~SCANNING; // 标记为忙碌
        (currentSteal = task).doExec();
        U.putOrderedObject(this, QCURRENTSTEAL, null); // 释放以 GC
        execLocalTasks();
        ForkJoinWorkerThread thread = owner;
        if (++nsteals < 0)      // 溢出时收集
            transferStealCount(pool);
        scanState |= SCANNING;
        if (thread != null)
            thread.afterTopLevelExec();
    }
}
```

- 标记为忙碌状态
- 执行窃取的任务
- 执行所有本地任务
- 更新窃取计数
- 恢复扫描状态

## 内存一致性

### 内存顺序

- 队列操作和 ctl 状态更新需要全栅栏 CAS
- 数组槽使用 Unsafe 提供的 volatile 模拟读取
- 从其他线程访问 WorkQueue 的 base、top 和 array 需要先对这些中的任何一个进行 volatile 加载

### happens-before 关系

- 在提交任务之前的操作与该任务执行的操作之间存在 happens-before 关系
- 任务执行的操作与通过 Future.get() 检索结果之间存在 happens-before 关系

## 配置常量

### 队列容量

```java
static final int INITIAL_QUEUE_CAPACITY = 1 << 13;  // 8192
static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26;  // 64M
```

### 超时配置

```java
private static final long IDLE_TIMEOUT = 2000L * 1000L * 1000L;  // 2秒
private static final long TIMEOUT_SLOP = 20L * 1000L * 1000L;    // 20ms
```

### 边界限制

```java
static final int SMASK        = 0xffff;        // 短位 == 最大索引
static final int MAX_CAP      = 0x7fff;        // 最大工作线程数 - 1
static final int EVENMASK     = 0xfffe;        // 偶数短位
static final int SQMASK       = 0x007e;        // 最大 64(偶数)槽
```

## 模式配置

```java
static final int MODE_MASK    = 0xffff << 16;  // int 的上半部分
static final int LIFO_QUEUE   = 0;             // LIFO 模式
static final int FIFO_QUEUE   = 1 << 16;       // FIFO 模式
static final int SHARED_QUEUE = 1 << 31;       // 共享队列(必须为负)
```

## 关键算法

### 工作窃取算法

1. 工作线程优先从自己的队列获取任务(LIFO)
2. 如果自己的队列为空,随机选择其他队列窃取任务(FIFO)
3. 使用 CAS 保证操作的原子性
4. 避免竞争,提高缓存局部性

### 线程激活/停用

#### 停用

1. 工作线程找不到任务时计算队列状态的校验和
2. 校验和稳定后才放弃并尝试停用
3. 重复扫描直到再次稳定
4. 使用自适应本地自旋后阻塞(park)

#### 激活

1. 向之前(可能)为空的队列 push 任务时,如果有空闲工作线程则信号通知
2. 如果工作线程少于目标并行度级别,则创建新工作线程
3. 当其他线程从队列移除任务并注意到还有其他任务时,也会发送信号

### 线程修剪

1. 池在空闲期间开始等待的工作线程会在 IDLE_TIMEOUT 后超时并终止
2. 超时期限随着线程数减少而增加
3. 最终移除所有工作线程
4. 当存在超过两个备用线程时,在下一个空闲点立即终止多余线程

## 关键点总结

1. **工作窃取**: 采用工作窃取算法,提高任务并行处理效率
2. **双端队列**: WorkQueue 支持 push、pop、poll 三种操作
3. **动态管理**: 动态添加、挂起、恢复工作线程
4. **ctl 控制**: 使用 64 位 ctl 变量原子控制线程池状态
5. **Common Pool**: 提供静态公共池,减少资源使用
6. **内存一致性**: 使用 Unsafe 操作保证内存顺序
7. **线程补偿**: 通过帮助和补偿机制处理阻塞情况
8. **可配置性**: 支持多种模式和配置选项

## 相关类

- `ForkJoinTask`: ForkJoin 框架的任务基类
- `ForkJoinWorkerThread`: ForkJoinPool 的工作线程
- `RecursiveAction`: 无返回值的递归任务
- `RecursiveTask`: 有返回值的递归任务
- `CountedCompleter`: 带计数的完成器任务
