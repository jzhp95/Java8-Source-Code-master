# Java并发编程(JUC)体系源码深度解析——ThreadLocalRandom详解

## 十二、ThreadLocalRandom详解

### 12.1 ThreadLocalRandom概述

#### 12.1.1 为什么需要ThreadLocalRandom

在Java并发编程中,随机数生成是一个常见需求。传统的Random类使用AtomicLong作为种子,在多线程环境下会导致竞争,影响性能。虽然可以通过ThreadLocal包装Random来避免竞争,但这需要手动管理,不够方便。

Java 7引入的ThreadLocalRandom类解决了这个问题。ThreadLocalRandom使用ThreadLocal机制,每个线程维护自己的随机数生成器,避免了多线程竞争,同时提供了更好的随机数质量。

ThreadLocalRandom的核心优势包括:

**线程隔离**: 每个线程有自己的随机数生成器,避免竞争。

**高性能**: 不需要同步,性能优于Random。

**更好的随机性**: 使用改进的算法,随机性更好。

**简单易用**: 提供了丰富的API,使用方便。

#### 12.1.2 ThreadLocalRandom的设计理念

ThreadLocalRandom的设计理念基于"线程本地存储"(ThreadLocal)的概念。每个线程维护自己的随机数生成器,包括种子和其他状态。当线程调用ThreadLocalRandom的方法时,使用当前线程的随机数生成器,不需要同步。

ThreadLocalRandom的核心特点包括:

**ThreadLocal机制**: 使用ThreadLocalRandom作为ThreadLocal的key,存储每个线程的随机数生成器。

**惰性初始化**: 随机数生成器在第一次使用时才初始化,避免不必要的开销。

**改进的算法**: 使用XORShift算法,随机性更好,性能更高。

**丰富的API**: 提供了生成各种类型随机数的方法。

```java
public class ThreadLocalRandom extends Random {
    // ThreadLocalRandom实例
    private static final ThreadLocalRandom instance = new ThreadLocalRandom();
    
    // 探测值
    private static final long probeGenerator;
    
    // 种子
    private long rnd;
    
    // 探测值
    private int initialized;
    private long probe;
}
```

### 12.2 ThreadLocalRandom核心API详解

#### 12.2.1 获取实例

```java
// 获取当前线程的ThreadLocalRandom实例
public static ThreadLocalRandom current() {
    if (UNSAFE.getInt(Thread.currentThread(), PROBE) == 0)
        localInit();
    return instance;
}

// 初始化当前线程的随机数生成器
static final void localInit() {
    int p = probeGenerator.addAndGet(PROBE_INCREMENT);
    int probe = (p == 0) ? 1 : p;
    long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
    Thread t = Thread.currentThread();
    UNSAFE.putLong(t, SEED, seed);
    UNSAFE.putInt(t, PROBE, probe);
}
```

**使用示例:**

```java
// 获取当前线程的ThreadLocalRandom实例
ThreadLocalRandom random = ThreadLocalRandom.current();

// 生成随机数
int randomInt = random.nextInt();
long randomLong = random.nextLong();
double randomDouble = random.nextDouble();
boolean randomBoolean = random.nextBoolean();
```

#### 12.2.2 生成随机数

```java
// 生成int类型随机数
public int nextInt() {
    return mix32(nextSeed());
}

// 生成指定范围内的int类型随机数
public int nextInt(int bound) {
    if (bound <= 0)
        throw new IllegalArgumentException(BAD_BOUND);
    int r = mix32(nextSeed());
    int m = bound - 1;
    if ((bound & m) == 0)
        r &= m;
    else {
        for (int u = r >>> 1; u + m - (r = u % bound) < 0; u = u >>> 1)
            ;
    }
    return r;
}

// 生成指定范围内的int类型随机数
public int nextInt(int origin, int bound) {
    if (origin >= bound)
        throw new IllegalArgumentException(BAD_RANGE);
    return nextInt(bound - origin) + origin;
}

// 生成long类型随机数
public long nextLong() {
    return mix64(nextSeed());
}

// 生成指定范围内的long类型随机数
public long nextLong(long bound) {
    if (bound <= 0)
        throw new IllegalArgumentException(BAD_BOUND);
    long r = mix64(nextSeed());
    long m = bound - 1;
    if ((bound & m) == 0)
        r &= m;
    else {
        for (long u = r >>> 1; u + m - (r = u % bound) < 0; u = u >>> 1)
            ;
    }
    return r;
}

// 生成指定范围内的long类型随机数
public long nextLong(long origin, long bound) {
    if (origin >= bound)
        throw new IllegalArgumentException(BAD_RANGE);
    return nextLong(bound - origin) + origin;
}

// 生成double类型随机数
public double nextDouble() {
    return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
}

// 生成指定范围内的double类型随机数
public double nextDouble(double bound) {
    if (!(bound > 0.0))
        throw new IllegalArgumentException(BAD_BOUND);
    return nextDouble() * bound;
}

// 生成指定范围内的double类型随机数
public double nextDouble(double origin, double bound) {
    if (!(origin < bound))
        throw new IllegalArgumentException(BAD_RANGE);
    return nextDouble() * (bound - origin) + origin;
}

// 生成boolean类型随机数
public boolean nextBoolean() {
    return mix32(nextSeed()) < 0;
}

// 生成float类型随机数
public float nextFloat() {
    return (mix32(nextSeed()) >>> 8) * FLOAT_UNIT;
}

// 生成指定范围内的float类型随机数
public float nextFloat(float bound) {
    if (!(bound > 0.0f))
        throw new IllegalArgumentException(BAD_BOUND);
    return nextFloat() * bound;
}

// 生成指定范围内的float类型随机数
public float nextFloat(float origin, float bound) {
    if (!(origin < bound))
        throw new IllegalArgumentException(BAD_RANGE);
    return nextFloat() * (bound - origin) + origin;
}
```

**使用示例:**

```java
ThreadLocalRandom random = ThreadLocalRandom.current();

// 生成int类型随机数
int randomInt = random.nextInt();
int randomIntBounded = random.nextInt(100);
int randomIntRange = random.nextInt(10, 20);

// 生成long类型随机数
long randomLong = random.nextLong();
long randomLongBounded = random.nextLong(1000);
long randomLongRange = random.nextLong(100, 200);

// 生成double类型随机数
double randomDouble = random.nextDouble();
double randomDoubleBounded = random.nextDouble(100.0);
double randomDoubleRange = random.nextDouble(10.0, 20.0);

// 生成boolean类型随机数
boolean randomBoolean = random.nextBoolean();

// 生成float类型随机数
float randomFloat = random.nextFloat();
float randomFloatBounded = random.nextFloat(100.0f);
float randomFloatRange = random.nextFloat(10.0f, 20.0f);
```

#### 12.2.3 生成随机数组

```java
// 填充byte数组
public void nextBytes(byte[] bytes) {
    int i = 0;
    int len = bytes.length;
    for (int k = len; k > 0; k -= 4) {
        int r = nextInt();
        bytes[i++] = (byte)r;
        if (k > 1) bytes[i++] = (byte)(r >>> 8);
        if (k > 2) bytes[i++] = (byte)(r >>> 16);
        if (k > 3) bytes[i++] = (byte)(r >>> 24);
    }
}
```

**使用示例:**

```java
ThreadLocalRandom random = ThreadLocalRandom.current();

byte[] bytes = new byte[16];
random.nextBytes(bytes);
```

### 12.3 ThreadLocalRandom内部实现原理

#### 12.3.1 ThreadLocal机制

ThreadLocalRandom使用ThreadLocal机制来存储每个线程的随机数生成器:

```java
// Thread类的字段
public class Thread implements Runnable {
    private static final AtomicInteger nextThreadLocalRandomSeed;
    private static final AtomicInteger nextThreadLocalRandomProbe;
    private static final int THREADLOCALRANDOM_PROBE_INCREMENT;
    
    long threadLocalRandomSeed;
    int threadLocalRandomProbe;
    int threadLocalRandomSecondarySeed;
}
```

**ThreadLocalRandom的字段:**

```java
public class ThreadLocalRandom extends Random {
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    
    static {
        try {
            SEED = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
```

**获取当前线程的种子:**

```java
private long nextSeed() {
    Thread t;
    long r;
    UNSAFE.putLong(t = Thread.currentThread(), SEED,
                  r = UNSAFE.getLong(t, SEED) + GAMMA);
    return r;
}
```

#### 12.3.2 XORShift算法

ThreadLocalRandom使用XORShift算法生成随机数,这是一种高效的伪随机数生成算法:

```java
// mix64方法
private static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    z = z ^ (z >>> 33);
    return z;
}

// mix32方法
private static int mix32(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    return (int)(((z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L) >>> 32);
}
```

**XORShift算法的特点:**

1. 使用异或和移位操作,效率高
2. 随机性好,周期长
3. 不需要同步,适合并发环境

### 12.4 ThreadLocalRandom与Random对比

#### 12.4.1 性能对比

**Random的实现:**

```java
public class Random implements java.io.Serializable {
    private final AtomicLong seed;
    
    protected int next(int bits) {
        long oldseed, nextseed;
        AtomicLong seed = this.seed;
        do {
            oldseed = seed.get();
            nextseed = (oldseed * multiplier + addend) & mask;
        } while (!seed.compareAndSet(oldseed, nextseed));
        return (int)(nextseed >>> (48 - bits));
    }
}
```

Random使用AtomicLong作为种子,每次生成随机数都需要CAS操作,在多线程环境下会导致竞争。

**ThreadLocalRandom的实现:**

```java
private long nextSeed() {
    Thread t;
    long r;
    UNSAFE.putLong(t = Thread.currentThread(), SEED,
                  r = UNSAFE.getLong(t, SEED) + GAMMA);
    return r;
}
```

ThreadLocalRandom每个线程有自己的种子,不需要CAS操作,性能更高。

#### 12.4.2 性能测试

```java
public class RandomPerformanceTest {
    private static final int THREADS = 10;
    private static final int ITERATIONS = 1000000;
    
    public static void main(String[] args) throws InterruptedException {
        testRandom();
        testThreadLocalRandom();
    }
    
    private static void testRandom() throws InterruptedException {
        Random random = new Random();
        CountDownLatch latch = new CountDownLatch(THREADS);
        
        long start = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    random.nextInt();
                }
                latch.countDown();
            }).start();
        }
        
        latch.await();
        long end = System.nanoTime();
        System.out.println("Random: " + (end - start) / 1_000_000 + "ms");
    }
    
    private static void testThreadLocalRandom() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREADS);
        
        long start = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < ITERATIONS; j++) {
                    random.nextInt();
                }
                latch.countDown();
            }).start();
        }
        
        latch.await();
        long end = System.nanoTime();
        System.out.println("ThreadLocalRandom: " + (end - start) / 1_000_000 + "ms");
    }
}
```

在多线程环境下,ThreadLocalRandom的性能通常是Random的数倍。

### 12.5 ThreadLocalRandom使用场景

#### 12.5.1 并发随机数生成

```java
public class ConcurrentRandomGeneration {
    private static final int THREADS = 10;
    private static final int ITERATIONS = 100000;
    
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREADS);
        
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < ITERATIONS; j++) {
                    int value = random.nextInt(100);
                    process(value);
                }
                latch.countDown();
            }).start();
        }
        
        latch.await();
        System.out.println("All threads completed");
    }
    
    private static void process(int value) {
        // 处理随机数
    }
}
```

#### 12.5.2 随机采样

```java
public class RandomSampling {
    private final List<String> items;
    
    public RandomSampling(List<String> items) {
        this.items = items;
    }
    
    public String sample() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int index = random.nextInt(items.size());
        return items.get(index);
    }
    
    public List<String> sample(int n) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int index = random.nextInt(items.size());
            result.add(items.get(index));
        }
        return result;
    }
}
```

#### 12.5.3 随机延迟

```java
public class RandomDelay {
    public void executeWithRandomDelay(Runnable task) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long delay = random.nextLong(100, 1000);
        
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }
}
```

### 12.6 ThreadLocalRandom最佳实践

#### 12.6.1 正确使用ThreadLocalRandom

```java
// 正确示例: 每次使用时获取current()
public class CorrectUsage {
    public void process() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int value = random.nextInt(100);
    }
}

// 错误示例: 缓存ThreadLocalRandom实例
public class IncorrectUsage {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    
    public void process() {
        int value = random.nextInt(100);
    }
}
```

#### 12.6.2 使用指定范围的随机数

```java
public class BoundedRandom {
    public void generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // 生成0-99的随机数
        int value1 = random.nextInt(100);
        
        // 生成10-19的随机数
        int value2 = random.nextInt(10, 20);
        
        // 生成0.0-99.0的随机数
        double value3 = random.nextDouble(100.0);
        
        // 生成10.0-19.0的随机数
        double value4 = random.nextDouble(10.0, 20.0);
    }
}
```

### 12.7 小结

ThreadLocalRandom是Java并发编程中用于生成随机数的工具,它使用ThreadLocal机制,每个线程维护自己的随机数生成器,避免了多线程竞争。

**核心要点总结:**

第一,**线程隔离**: 每个线程有自己的随机数生成器,避免竞争。

第二,**高性能**: 不需要同步,性能优于Random。

第三,**惰性初始化**: 随机数生成器在第一次使用时才初始化。

第四,**改进的算法**: 使用XORShift算法,随机性更好,性能更高。

第五,**丰富的API**: 提供了生成各种类型随机数的方法。

第六,**使用场景**: 适合并发环境下的随机数生成,如并发随机数生成、随机采样、随机延迟等。

第七,**最佳实践**: 每次使用时获取current(),不要缓存ThreadLocalRandom实例。
