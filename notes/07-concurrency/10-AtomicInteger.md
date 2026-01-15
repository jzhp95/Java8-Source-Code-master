# AtomicInteger 源码解读

## 类概述

`AtomicInteger` 是一个可以原子更新的 `int` 值。用于应用程序中如原子递增计数器,但不能用作 `Integer` 的替代品。但是,此类扩展了 `Number` 以允许处理基于数值的类的工具和实用程序进行统一访问。

```java
public class AtomicInteger extends Number implements java.io.Serializable {
```

## 核心特性

### 1. 原子性

- 所有操作都是原子的
- 使用 CAS (Compare-And-Swap) 操作实现
- 适用于多线程环境下的计数器、累加器等场景
- 不能替代 `Integer`,因为 `Integer` 是不可变的

### 2. 基于 Unsafe

- 使用 `sun.misc.Unsafe` 类实现底层操作
- 通过 `Unsafe.compareAndSwapInt()` 实现 CAS 操作
- 通过 `Unsafe.getAndAddInt()` 实现原子加法
- 直接操作内存,绕过 JVM 的安全检查

### 3. volatile 字段

```java
private volatile int value;
```

- `value` 字段声明为 `volatile`
- 保证可见性:一个线程的修改对其他线程立即可见
- 保证有序性:禁止指令重排序

## 核心实现

### Unsafe 初始化

```java
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;

static {
    try {
        valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}
```

- **unsafe**: 获取 Unsafe 实例
- **valueOffset**: 获取 `value` 字段在对象中的偏移量
- **作用**: 允许直接通过偏移量访问和修改 `value` 字段
- **初始化**: 在静态初始化块中完成,只执行一次

### value 字段

```java
private volatile int value;
```

- **volatile**: 保证可见性和有序性
- **private**: 外部不能直接访问
- **int**: 存储的值

## 核心方法

### 构造方法

#### 带初始值的构造方法

```java
public AtomicInteger(int initialValue) {
    value = initialValue;
}
```

- **参数**: `initialValue` - 初始值
- **功能**: 创建具有给定初始值的新 AtomicInteger

#### 默认构造方法

```java
public AtomicInteger() {
}
```

- **功能**: 创建初始值为 0 的新 AtomicInteger

### get() - 获取当前值

```java
public final int get() {
    return value;
}
```

- **功能**: 获取当前值
- **返回值**: 当前值
- **特点**: 直接读取 volatile 字段,保证可见性

### set() - 设置新值

```java
public final void set(int newValue) {
    value = newValue;
}
```

- **参数**: `newValue` - 新值
- **功能**: 设置为给定值
- **特点**: 直接写入 volatile 字段,保证可见性和有序性

### lazySet() - 延迟设置

```java
public final void lazySet(int newValue) {
    unsafe.putOrderedInt(this, valueOffset, newValue);
}
```

- **参数**: `newValue` - 新值
- **功能**: 最终设置为给定值
- **特点**:
  - 使用 `Unsafe.putOrderedInt()` 方法
  - 不保证立即可见性,但保证有序性
  - 性能优于 `set()`,因为不强制立即刷新到主存
- **用途**: 当不需要立即可见性时使用,以提高性能

### getAndSet() - 获取并设置

```java
public final int getAndSet(int newValue) {
    return unsafe.getAndSetInt(this, valueOffset, newValue);
}
```

- **参数**: `newValue` - 新值
- **返回值**: 之前的值
- **功能**: 原子地设置为给定值并返回旧值
- **特点**:
  - 使用 `Unsafe.getAndSetInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回设置之前的值

### compareAndSet() - 比较并设置(CAS)

```java
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

- **参数**:
  - `expect` - 期望值
  - `update` - 新值
- **返回值**: 如果成功返回 `true`,否则返回 `false`
- **功能**: 原子地设置值为给定更新值,如果当前值等于期望值
- **特点**:
  - 使用 `Unsafe.compareAndSwapInt()` 方法
  - CAS 操作,无锁实现并发
  - 如果当前值不等于期望值,操作失败
  - 如果操作失败,返回 `false`,表示实际值不等于期望值

#### CAS 操作流程

1. 读取当前值
2. 比较当前值是否等于期望值
3. 如果相等,将值更新为新值
4. 如果不相等,操作失败
5. 整个操作是原子的

### weakCompareAndSet() - 弱比较并设置

```java
public final boolean weakCompareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

- **参数**:
  - `expect` - 期望值
  - `update` - 新值
- **返回值**: 如果成功返回 `true`
- **功能**: 原子地设置值为给定更新值,如果当前值等于期望值
- **特点**:
  - 与 `compareAndSet()` 相同
  - 可能虚假失败,不提供排序保证
  - 很少是 `compareAndSet()` 的合适替代品
  - 在某些平台上可能有更好的性能

### getAndIncrement() - 获取并递增

```java
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}
```

- **返回值**: 之前的值
- **功能**: 原子地将当前值递增 1
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回递增之前的值

### getAndDecrement() - 获取并递减

```java
public final int getAndDecrement() {
    return unsafe.getAndAddInt(this, valueOffset, -1);
}
```

- **返回值**: 之前的值
- **功能**: 原子地将当前值递减 1
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回递减之前的值

### getAndAdd() - 获取并添加

```java
public final int getAndAdd(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta);
}
```

- **参数**: `delta` - 要添加的值
- **返回值**: 之前的值
- **功能**: 原子地将给定值添加到当前值
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回添加之前的值

### incrementAndGet() - 递增并获取

```java
public final int incrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}
```

- **返回值**: 更新后的值
- **功能**: 原子地将当前值递增 1
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回递增之后的值

### decrementAndGet() - 递减并获取

```java
public final int decrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
}
```

- **返回值**: 更新后的值
- **功能**: 原子地将当前值递减 1
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回递减之后的值

### addAndGet() - 添加并获取

```java
public final int addAndGet(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
}
```

- **参数**: `delta` - 要添加的值
- **返回值**: 更新后的值
- **功能**: 原子地将给定值添加到当前值
- **特点**:
  - 使用 `Unsafe.getAndAddInt()` 方法
  - 原子操作,保证只有一个线程成功
  - 返回添加之后的值

### getAndUpdate() - 获取并更新

```java
public final int getAndUpdate(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

- **参数**: `updateFunction` - 无副作用的函数
- **返回值**: 之前的值
- **功能**: 原子地使用应用给定函数的结果更新当前值,返回之前的值
- **特点**:
  - 使用 CAS 循环实现
  - 函数应该是无副作用的,因为当由于线程间的竞争导致尝试更新失败时,可能会重新应用
  - 返回更新之前的值

#### 执行流程

1. 获取当前值
2. 应用更新函数计算新值
3. 使用 CAS 尝试更新
4. 如果 CAS 失败,重复步骤 1-3
5. 返回之前的值

### updateAndGet() - 更新并获取

```java
public final int updateAndGet(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

- **参数**: `updateFunction` - 无副作用的函数
- **返回值**: 更新后的值
- **功能**: 原子地使用应用给定函数的结果更新当前值,返回更新后的值
- **特点**:
  - 使用 CAS 循环实现
  - 函数应该是无副作用的,因为当由于线程间的竞争导致尝试更新失败时,可能会重新应用
  - 返回更新之后的值

### getAndAccumulate() - 获取并累加

```java
public final int getAndAccumulate(int x,
                                      IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

- **参数**:
  - `x` - 更新值
  - `accumulatorFunction` - 两个参数的无副作用的函数
- **返回值**: 之前的值
- **功能**: 原子地使用应用给定函数到当前值和给定值的结果更新当前值,返回之前的值
- **特点**:
  - 使用 CAS 循环实现
  - 函数应该是无副作用的,因为当由于线程间的竞争导致尝试更新失败时,可能会重新应用
  - 函数以当前值作为第一个参数,给定更新作为第二个参数应用
  - 返回更新之前的值

### accumulateAndGet() - 累加并获取

```java
public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```

- **参数**:
  - `x` - 更新值
  - `accumulatorFunction` - 两个参数的无副作用的函数
- **返回值**: 更新后的值
- **功能**: 原子地使用应用给定函数到当前值和给定值的结果更新当前值,返回更新后的值
- **特点**:
  - 使用 CAS 循环实现
  - 函数应该是无副作用的,因为当由于线程间的竞争导致尝试更新失败时,可能会重新应用
  - 函数以当前值作为第一个参数,给定更新作为第二个参数应用
  - 返回更新之后的值

### toString() - 字符串表示

```java
public String toString() {
    return Integer.toString(get());
}
```

- **返回值**: 当前值的字符串表示
- **功能**: 返回当前值的字符串表示

### intValue() - 获取 int 值

```java
public int intValue() {
    return get();
}
```

- **返回值**: int 值
- **功能**: 返回此 AtomicInteger 的值作为 int

### longValue() - 获取 long 值

```java
public long longValue() {
    return (long)get();
}
```

- **返回值**: long 值
- **功能**: 返回此 AtomicInteger 的值作为 long(通过拓宽原始转换)

### floatValue() - 获取 float 值

```java
public float floatValue() {
    return (float)get();
}
```

- **返回值**: float 值
- **功能**: 返回此 AtomicInteger 的值作为 float(通过拓宽原始转换)

### doubleValue() - 获取 double 值

```java
public double doubleValue() {
    return (double)get();
}
```

- **返回值**: double 值
- **功能**: 返回此 AtomicInteger 的值作为 double(通过拓宽原始转换)

## 使用示例

### 示例 1: 原子计数器

```java
AtomicInteger counter = new AtomicInteger(0);

// 多个线程递增计数器
counter.incrementAndGet();

// 获取当前计数
int count = counter.get();
```

### 示例 2: CAS 操作

```java
AtomicInteger atomicInt = new AtomicInteger(0);

// 尝试更新值
boolean success = atomicInt.compareAndSet(0, 1);
if (success) {
    // 更新成功
} else {
    // 更新失败,值已被其他线程修改
}
```

### 示例 3: 函数式更新

```java
AtomicInteger atomicInt = new AtomicInteger(10);

// 使用函数更新
int newValue = atomicInt.updateAndGet(x -> x * 2);

// 使用累加器更新
int result = atomicInt.accumulateAndGet(5, (x, y) -> x + y);
```

### 示例 4: 自旋锁

```java
AtomicInteger lock = new AtomicInteger(0);

// 获取锁
while (!lock.compareAndSet(0, 1)) {
    // 自旋等待
}

try {
    // 临界区
} finally {
    // 释放锁
    lock.set(0);
}
```

## 内存一致性效果

- 对 `volatile` 字段的写入操作 happens-before 后续对该字段的读取操作
- CAS 操作提供完整的内存屏障效果
- `lazySet()` 不提供立即可见性保证,但保证有序性

## 与 synchronized 的区别

| 特性 | AtomicInteger | synchronized |
|------|--------------|-------------|
| 性能 | 高(无锁) | 低(需要加锁) |
| 原子性 | 原子操作 | 原子代码块 |
| 功能 | 有限的原子操作 | 任意代码 |
| 适用场景 | 简单的计数器、累加器 | 复杂的同步逻辑 |
| 死锁 | 不会死锁 | 可能死锁 |

## 关键点总结

1. **Unsafe 实现**: 使用 Unsafe 类实现底层 CAS 操作
2. **volatile 字段**: value 字段声明为 volatile,保证可见性和有序性
3. **CAS 操作**: 使用 compareAndSwapInt 实现无锁并发
4. **CAS 循环**: getAndUpdate、updateAndGet 等方法使用 CAS 循环实现
5. **函数式更新**: Java 8 引入的函数式更新方法
6. **性能**: 无锁实现,性能优于 synchronized
7. **适用场景**: 适用于简单的计数器、累加器等场景
8. **局限性**: 不能替代 Integer,功能有限

## 相关类

- `AtomicLong`: long 类型的原子类
- `AtomicBoolean`: boolean 类型的原子类
- `AtomicReference`: 引用类型的原子类
- `AtomicIntegerArray`: int 数组的原子类
- `AtomicIntegerFieldUpdater`: int 字段的原子更新器
- `Unsafe`: 底层不安全操作类
