# Java并发编程(JUC)体系源码深度解析——异步编程与CompletableFuture详解

## 七、异步编程与CompletableFuture详解

### 7.1 异步编程概述

#### 7.1.1 为什么需要异步编程

在传统的同步编程模型中,代码按照顺序执行,每个操作必须等待前一个操作完成后才能开始。这种模型简单直观,但在处理IO密集型任务时会导致线程阻塞,降低系统吞吐量。异步编程模型允许任务在后台执行,主线程可以继续处理其他工作,大大提高了系统的并发性能和响应速度。

考虑一个典型的Web服务场景:处理一个请求需要查询数据库、调用外部API、进行复杂计算等操作。如果使用同步模型,每个请求会占用一个线程直到所有操作完成,在高并发场景下可能导致线程池耗尽。使用异步模型,线程可以在等待IO时释放,去处理其他请求,显著提高系统的并发能力。

Java 5引入的Future接口提供了异步编程的基础支持,但它的功能有限:只能通过get()方法阻塞获取结果,无法方便地组合多个异步任务,也无法处理任务完成后的回调。Java 8引入的CompletableFuture解决了这些问题,提供了强大的异步编程能力。

#### 7.1.2 CompletableFuture概述

CompletableFuture是Java 8引入的Future接口的增强实现,它提供了丰富的API来支持异步编程。与传统的Future相比,CompletableFuture具有以下优势:

**非阻塞式结果获取**: 可以通过回调函数处理任务完成后的结果,而不需要阻塞等待。

**任务链式组合**: 可以将多个异步任务串联起来,前一个任务的结果作为后一个任务的输入。

**多任务组合**: 可以组合多个CompletableFuture,实现并行执行和结果聚合。

**异常处理**: 提供了灵活的异常处理机制,可以在任务链中捕获和处理异常。

CompletableFuture实现了Future和CompletionStage接口。CompletionStage接口定义了任务链式组合的规范,CompletableFuture提供了具体实现。

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    // 结果值
    private volatile Object result;
    
    // 栈结构,用于保存依赖当前Future的回调
    private volatile Completion stack;
}
```

result字段保存任务的结果或异常,stack字段保存依赖当前Future的回调操作。当Future完成时,会触发stack中的回调。

### 7.2 CompletableFuture核心API详解

#### 7.2.1 创建CompletableFuture

CompletableFuture提供了多种静态方法来创建异步任务:

```java
// 创建一个已完成的Future
public static <U> CompletableFuture<U> completedFuture(U value) {
    return new CompletableFuture<U>((value == null) ? NIL : value);
}

// 创建一个异步任务,使用ForkJoinPool.commonPool()
public static CompletableFuture<Void> runAsync(Runnable runnable) {
    return asyncRunStage(asyncPool, runnable);
}

// 创建一个异步任务,使用指定的Executor
public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
    return asyncRunStage(screenExecutor(executor), runnable);
}

// 创建一个有返回值的异步任务,使用ForkJoinPool.commonPool()
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
    return asyncSupplyStage(asyncPool, supplier);
}

// 创建一个有返回值的异步任务,使用指定的Executor
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
    return asyncSupplyStage(screenExecutor(executor), supplier);
}
```

**使用示例:**

```java
// 使用runAsync执行无返回值的任务
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Task running in: " + Thread.currentThread().getName());
});

// 使用supplyAsync执行有返回值的任务
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello, CompletableFuture!";
});

// 使用自定义线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Task executed in custom executor";
}, executor);
```

#### 7.2.2 结果获取与处理

CompletableFuture提供了多种方式获取和处理结果:

```java
// 阻塞获取结果
public T get() throws InterruptedException, ExecutionException {
    Object r;
    return reportGet((r = result) == null ? waitingGet(true) : r);
}

// 带超时的阻塞获取
public T get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    Object r;
    long nanos = unit.toNanos(timeout);
    return reportGet((r = result) == null ? timedGet(nanos) : r);
}

// 非阻塞获取结果,如果未完成返回null
public T getNow(T valueIfAbsent) {
    Object r;
    return ((r = result) == null) ? valueIfAbsent : reportGet(r);
}

// 如果未完成,使用给定值完成
public boolean complete(T value) {
    boolean triggered = completeValue(value);
    postComplete();
    return triggered;
}

// 如果未完成,使用异常完成
public boolean completeExceptionally(Throwable ex) {
    boolean triggered = internalComplete(new AltResult(ex));
    postComplete();
    return triggered;
}
```

**使用示例:**

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    return "Result";
});

// 阻塞获取结果
String result = future.get(); // 阻塞直到完成

// 带超时的获取
try {
    String result = future.get(500, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
    System.out.println("Timeout");
}

// 非阻塞获取
String result = future.getNow("Default"); // 如果未完成返回"Default"

// 手动完成
CompletableFuture<String> manualFuture = new CompletableFuture<>();
manualFuture.complete("Manual result");
```

#### 7.2.3 回调处理

CompletableFuture提供了丰富的回调方法,可以在任务完成时执行回调:

```java
// 任务完成后执行回调
public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
    return uniAcceptStage(null, action);
}

// 异步执行回调
public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
    return uniAcceptStage(asyncPool, action);
}

// 使用指定Executor异步执行回调
public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
    return uniAcceptStage(screenExecutor(executor), action);
}

// 对结果进行转换
public <U> CompletableFuture<U> thenApply(Function<? super T,? extends U> fn) {
    return uniApplyStage(null, fn);
}

// 异步转换结果
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn) {
    return uniApplyStage(asyncPool, fn);
}

// 使用指定Executor异步转换结果
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn, Executor executor) {
    return uniApplyStage(screenExecutor(executor), fn);
}

// 对结果执行操作并返回新的CompletableFuture
public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
    return uniComposeStage(null, fn);
}

// 异步执行thenCompose
public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
    return uniComposeStage(asyncPool, fn);
}

// 使用指定Executor异步执行thenCompose
public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
    return uniComposeStage(screenExecutor(executor), fn);
}
```

**使用示例:**

```java
CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenApply(s -> s + ", World!")
  .thenApply(String::toUpperCase)
  .thenAccept(System.out::println); // 输出: HELLO, WORLD!

CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenCompose(s -> {
    return CompletableFuture.supplyAsync(() -> s + ", World!");
}).thenAccept(System.out::println); // 输出: Hello, World!
```

**thenApply vs thenCompose的区别:**

thenApply的Function返回的是普通值,thenCompose的Function返回的是CompletionStage。如果回调本身是异步的,应该使用thenCompose。

```java
// thenApply: 返回普通值
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + ", World!"); // 返回 "Hello, World!"

// thenCompose: 返回CompletableFuture
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "Hello")
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + ", World!")); // 返回 "Hello, World!"
```

#### 7.2.4 异常处理

CompletableFuture提供了灵活的异常处理机制:

```java
// 捕获异常并处理
public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
    return uniExceptionallyStage(fn);
}

// 无论成功或失败都执行
public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
    return uniWhenCompleteStage(null, action);
}

// 异步执行whenComplete
public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
    return uniWhenCompleteStage(asyncPool, action);
}

// 使用指定Executor异步执行whenComplete
public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return uniWhenCompleteStage(screenExecutor(executor), action);
}

// 处理结果或异常
public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    return uniHandleStage(null, fn);
}

// 异步执行handle
public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
    return uniHandleStage(asyncPool, fn);
}

// 使用指定Executor异步执行handle
public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
    return uniHandleStage(screenExecutor(executor), fn);
}
```

**使用示例:**

```java
CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Random error");
    }
    return "Success";
}).exceptionally(ex -> {
    System.out.println("Exception caught: " + ex.getMessage());
    return "Default value";
}).thenAccept(System.out::println);

CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Random error");
    }
    return "Success";
}).whenComplete((result, ex) -> {
    if (ex != null) {
        System.out.println("Exception: " + ex.getMessage());
    } else {
        System.out.println("Result: " + result);
    }
});

CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Random error");
    }
    return "Success";
}).handle((result, ex) -> {
    if (ex != null) {
        return "Recovered from: " + ex.getMessage();
    }
    return result.toUpperCase();
}).thenAccept(System.out::println);
```

### 7.3 多任务组合详解

#### 7.3.1 allOf与anyOf

CompletableFuture提供了组合多个Future的方法:

```java
// 等待所有Future完成
public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
    return andTree(cfs, 0, cfs.length - 1);
}

// 等待任意一个Future完成
public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
    return orTree(cfs, 0, cfs.length - 1);
}
```

**allOf使用示例:**

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task 1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return "Task 2";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    sleep(1500);
    return "Task 3";
});

// 等待所有任务完成
CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

allFutures.thenRun(() -> {
    try {
        System.out.println("All tasks completed");
        System.out.println("Result 1: " + future1.get());
        System.out.println("Result 2: " + future2.get());
        System.out.println("Result 3: " + future3.get());
    } catch (Exception e) {
        e.printStackTrace();
    }
});
```

**anyOf使用示例:**

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(3000);
    return "Task 1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task 2";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return "Task 3";
});

// 等待任意一个任务完成
CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(future1, future2, future3);

anyFuture.thenAccept(result -> {
    System.out.println("First completed: " + result);
});
```

#### 7.3.2 thenCombine与thenAcceptBoth

```java
// 组合两个Future的结果
public <U,V> CompletableFuture<V> thenCombine(
    CompletionStage<? extends U> other,
    BiFunction<? super T,? super U,? extends V> fn) {
    return biApplyStage(null, other, fn);
}

// 异步组合两个Future的结果
public <U,V> CompletableFuture<V> thenCombineAsync(
    CompletionStage<? extends U> other,
    BiFunction<? super T,? super U,? extends V> fn) {
    return biApplyStage(asyncPool, other, fn);
}

// 使用指定Executor异步组合
public <U,V> CompletableFuture<V> thenCombineAsync(
    CompletionStage<? extends U> other,
    BiFunction<? super T,? super U,? extends V> fn,
    Executor executor) {
    return biApplyStage(screenExecutor(executor), other, fn);
}

// 消费两个Future的结果
public <U> CompletableFuture<Void> thenAcceptBoth(
    CompletionStage<? extends U> other,
    BiConsumer<? super T,? super U> action) {
    return biAcceptStage(null, other, action);
}

// 异步消费两个Future的结果
public <U> CompletableFuture<Void> thenAcceptBothAsync(
    CompletionStage<? extends U> other,
    BiConsumer<? super T,? super U> action) {
    return biAcceptStage(asyncPool, other, action);
}

// 使用指定Executor异步消费
public <U> CompletableFuture<Void> thenAcceptBothAsync(
    CompletionStage<? extends U> other,
    BiConsumer<? super T,? super U> action,
    Executor executor) {
    return biAcceptStage(screenExecutor(executor), other, action);
}
```

**使用示例:**

```java
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

// 组合两个Future的结果
CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> a + b);
combined.thenAccept(System.out::println); // 输出: 30

// 消费两个Future的结果
future1.thenAcceptBoth(future2, (a, b) -> {
    System.out.println("Sum: " + (a + b));
});
```

#### 7.3.3 applyToEither与acceptEither

```java
// 使用两个Future中先完成的那个结果
public <U> CompletableFuture<U> applyToEither(
    CompletionStage<? extends T> other,
    Function<? super T, U> fn) {
    return orApplyStage(null, other, fn);
}

// 异步使用先完成的那个结果
public <U> CompletableFuture<U> applyToEitherAsync(
    CompletionStage<? extends T> other,
    Function<? super T, U> fn) {
    return orApplyStage(asyncPool, other, fn);
}

// 使用指定Executor异步使用先完成的那个结果
public <U> CompletableFuture<U> applyToEitherAsync(
    CompletionStage<? extends T> other,
    Function<? super T, U> fn,
    Executor executor) {
    return orApplyStage(screenExecutor(executor), other, fn);
}

// 消费两个Future中先完成的那个结果
public CompletableFuture<Void> acceptEither(
    CompletionStage<? extends T> other,
    Consumer<? super T> action) {
    return orAcceptStage(null, other, action);
}

// 异步消费先完成的那个结果
public CompletableFuture<Void> acceptEitherAsync(
    CompletionStage<? extends T> other,
    Consumer<? super T> action) {
    return orAcceptStage(asyncPool, other, action);
}

// 使用指定Executor异步消费先完成的那个结果
public CompletableFuture<Void> acceptEitherAsync(
    CompletionStage<? extends T> other,
    Consumer<? super T> action,
    Executor executor) {
    return orAcceptStage(screenExecutor(executor), other, action);
}
```

**使用示例:**

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(3000);
    return "Task 1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task 2";
});

// 使用先完成的那个结果
future1.applyToEither(future2, s -> s + " completed first")
       .thenAccept(System.out::println); // 输出: Task 2 completed first

// 消费先完成的那个结果
future1.acceptEither(future2, System.out::println); // 输出: Task 2
```

### 7.4 CompletableFuture内部实现原理

#### 7.4.1 核心数据结构

CompletableFuture的核心数据结构包括result和stack:

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    volatile Object result; // 结果或异常
    
    static final class AltResult {
        final Throwable ex; // 异常
        AltResult(Throwable x) { this.ex = x; }
    }
    
    static final class Completion extends ForkJoinTask<Void>
        implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;
        CompletableFuture<?> dep;
        CompletableFuture<?> src;
        
        abstract CompletableFuture<?> tryFire(int mode);
        abstract boolean isLive();
    }
    
    volatile Completion stack; // 依赖栈
}
```

result字段保存任务的结果或异常。如果任务正常完成,result保存结果值;如果任务异常完成,result保存AltResult对象,其中包含异常信息。

stack字段是一个栈结构,保存依赖当前Future的Completion对象。当Future完成时,会触发stack中的Completion。

#### 7.4.2 任务完成机制

CompletableFuture的任务完成通过completeValue和completeExceptionally方法实现:

```java
final boolean completeValue(T value) {
    return UNSAFE.compareAndSwapObject(this, RESULT_OFFSET,
                                       null, (value == null) ? NIL : value);
}

final boolean completeExceptionally(Throwable ex) {
    return internalComplete(new AltResult(ex));
}

final boolean internalComplete(Object r) {
    return UNSAFE.compareAndSwapObject(this, RESULT_OFFSET, null, r);
}
```

这些方法使用CAS操作原子地设置result字段。如果设置成功,会调用postComplete方法触发依赖栈中的回调。

```java
final void postComplete() {
    CompletableFuture<?> f = this;
    Completion h;
    while ((h = f.stack) != null ||
           (f != this && (h = (f = this).stack) != null)) {
        CompletableFuture<?> d;
        Completion t;
        if (f.stack == h) {
            if ((f.stack = t = h.next) == null) {
                if (f != this) {
                    f = this;
                    continue;
                }
                break;
            }
        } else {
            if (CAS_STACK(f, h, t = h.next)) {
                if (f != this) {
                    f = this;
                    continue;
                }
                break;
            }
            continue;
        }
        f = (d = h.tryFire(NESTED)) == null ? this : d;
    }
}
```

postComplete方法遍历stack,触发每个Completion的tryFire方法。tryFire方法执行实际的回调逻辑。

#### 7.4.3 Completion实现

Completion是CompletableFuture的内部类,实现了ForkJoinTask和Runnable接口:

```java
abstract static class Completion extends ForkJoinTask<Void>
    implements Runnable, AsynchronousCompletionTask {
    volatile Completion next;
    CompletableFuture<?> dep;
    CompletableFuture<?> src;
    
    abstract CompletableFuture<?> tryFire(int mode);
    abstract boolean isLive();
    
    public final void run() {
        tryFire(ASYNC);
    }
    
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
    
    public final boolean isDone() {
        return true;
    }
    
    public final Void get() {
        return null;
    }
}
```

Completion有多种实现,对应不同的回调类型:

```java
static final class UniApply<T,V> extends Completion {
    Function<? super T,? extends V> fn;
    UniApply(CompletableFuture<V> dep, CompletableFuture<T> src,
             Function<? super T,? extends V> fn) {
        this.dep = dep;
        this.src = src;
        this.fn = fn;
    }
    
    final CompletableFuture<V> tryFire(int mode) {
        CompletableFuture<V> d;
        CompletableFuture<T> a;
        if ((d = dep) == null || !d.uniApply(this, a = src, fn, mode))
            return null;
        dep = null;
        src = null;
        fn = null;
        return d.postFire(a, mode);
    }
}
```

tryFire方法执行实际的回调逻辑,并返回下一个需要触发的CompletableFuture。

### 7.5 实战案例

#### 7.5.1 并发查询多个服务

```java
public class AggregationService {
    
    public CompletableFuture<User> getUser(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟数据库查询
            sleep(100);
            return new User(userId, "User" + userId);
        });
    }
    
    public CompletableFuture<List<Order>> getOrders(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟查询订单
            sleep(150);
            return Arrays.asList(
                new Order(1L, userId, BigDecimal.valueOf(100)),
                new Order(2L, userId, BigDecimal.valueOf(200))
            );
        });
    }
    
    public CompletableFuture<List<Product>> getProducts(List<Long> productIds) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟查询商品
            sleep(120);
            return productIds.stream()
                .map(id -> new Product(id, "Product" + id, BigDecimal.valueOf(50)))
                .collect(Collectors.toList());
        });
    }
    
    public CompletableFuture<UserDashboard> getUserDashboard(Long userId) {
        // 并发查询用户、订单、商品
        CompletableFuture<User> userFuture = getUser(userId);
        CompletableFuture<List<Order>> ordersFuture = getOrders(userId);
        
        return userFuture.thenCombine(ordersFuture, (user, orders) -> {
            List<Long> productIds = orders.stream()
                .map(Order::getProductId)
                .distinct()
                .collect(Collectors.toList());
            return getProducts(productIds).thenApply(products -> {
                return new UserDashboard(user, orders, products);
            });
        }).thenCompose(x -> x);
    }
}
```

#### 7.5.2 异步流水线处理

```java
public class AsyncPipeline {
    
    public CompletableFuture<String> process(String input) {
        return CompletableFuture.supplyAsync(() -> {
            // 阶段1: 数据清洗
            return cleanData(input);
        }).thenApplyAsync(data -> {
            // 阶段2: 数据转换
            return transformData(data);
        }).thenApplyAsync(data -> {
            // 阶段3: 数据验证
            return validateData(data);
        }).thenApplyAsync(data -> {
            // 阶段4: 数据存储
            return storeData(data);
        }).exceptionally(ex -> {
            // 异常处理
            System.err.println("Pipeline failed: " + ex.getMessage());
            return "Error: " + ex.getMessage();
        });
    }
    
    private String cleanData(String input) {
        return input.trim();
    }
    
    private String transformData(String data) {
        return data.toUpperCase();
    }
    
    private String validateData(String data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Empty data");
        }
        return data;
    }
    
    private String storeData(String data) {
        // 模拟存储
        return "Stored: " + data;
    }
}
```

### 7.6 CompletableFuture最佳实践

#### 7.6.1 线程池选择

```java
// CPU密集型任务: 使用固定大小线程池
ExecutorService cpuPool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);

// IO密集型任务: 使用较大线程池
ExecutorService ioPool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2
);

CompletableFuture.supplyAsync(() -> {
    // CPU密集型任务
    return compute();
}, cpuPool);

CompletableFuture.supplyAsync(() -> {
    // IO密集型任务
    return fetchFromDatabase();
}, ioPool);
```

#### 7.6.2 超时处理

```java
public CompletableFuture<String> withTimeout(CompletableFuture<String> future, 
                                           long timeout, TimeUnit unit) {
    final CompletableFuture<String> result = new CompletableFuture<>();
    
    // 超时任务
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final Future<?> timeoutFuture = scheduler.schedule(() -> {
        if (!future.isDone()) {
            result.completeExceptionally(new TimeoutException());
        }
    }, timeout, unit);
    
    // 正常完成
    future.whenComplete((value, ex) -> {
        timeoutFuture.cancel(false);
        if (ex != null) {
            result.completeExceptionally(ex);
        } else {
            result.complete(value);
        }
        scheduler.shutdown();
    });
    
    return result;
}
```

#### 7.6.3 错误重试

```java
public <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> supplier,
                                           int maxRetries) {
    CompletableFuture<T> result = new CompletableFuture<>();
    retry(supplier, maxRetries, result);
    return result;
}

private <T> void retry(Supplier<CompletableFuture<T>> supplier, 
                        int retries, CompletableFuture<T> result) {
    supplier.get().whenComplete((value, ex) -> {
        if (ex == null) {
            result.complete(value);
        } else if (retries > 0) {
            System.out.println("Retrying... attempts left: " + retries);
            retry(supplier, retries - 1, result);
        } else {
            result.completeExceptionally(ex);
        }
    });
}
```

### 7.7 小结

CompletableFuture是Java 8引入的强大异步编程工具,它提供了丰富的API来支持异步任务的创建、组合和异常处理。

**核心要点总结:**

第一,**异步任务创建**: 使用supplyAsync和runAsync创建异步任务,可以指定线程池。

第二,**回调处理**: 使用thenApply、thenAccept、thenCompose等方法处理任务完成后的结果。

第三,**异常处理**: 使用exceptionally、handle、whenComplete等方法处理异常。

第四,**任务组合**: 使用allOf、anyOf、thenCombine等方法组合多个异步任务。

第五,**内部实现**: 使用栈结构管理依赖关系,通过CAS操作保证线程安全。

第六,**最佳实践**: 合理选择线程池、处理超时、实现重试机制等。
