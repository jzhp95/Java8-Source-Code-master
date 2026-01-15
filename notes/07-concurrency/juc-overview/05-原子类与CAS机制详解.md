# Java并发编程(JUC)体系源码深度解析——原子类与CAS机制详解

## 五、原子类与CAS机制详解

### 5.1 原子操作概述

#### 5.1.1 原子性的重要性

在多线程环境中,原子性是保证数据一致性的基础。所谓原子性,指的是一个操作要么完全执行,要么完全不执行,不会被其他线程干扰。在Java中,像i++这样的复合操作看似简单,实际上包含三个独立的步骤：读取变量的值、将值加1、写回变量。在多线程环境下,如果没有适当的同步机制,这个看似简单的操作可能导致数据不一致。

考虑一个经典的计数器问题：两个线程同时执行count++操作。假设count的初始值为0,正确的执行结果应该是2,但由于操作不是原子的,可能出现以下执行序列：线程1读取count（0）、线程2读取count（0）、线程1将count加1并写回（1）、线程2将count加1并写回（1）。最终count的值为1,而不是预期的2。这就是著名的"计数丢失"问题。

传统的解决方案是使用synchronized关键字或Lock来保护count++操作。这种方法虽然能够保证原子性,但存在明显的性能问题：每次访问共享变量都需要获取和释放锁,导致线程阻塞和上下文切换的开销。在高并发场景下,这种开销可能成为系统的性能瓶颈。

Java 5引入的java.util.concurrent.atomic包提供了另一种解决方案：使用CAS（Compare-And-Swap）操作实现无锁算法。这种方法通过硬件级别的原子指令保证操作的原子性,同时避免了线程阻塞的开销。

#### 5.1.2 原子类分类

JUC的原子类可以分为以下几类：

**基础类型原子类**: AtomicInteger、AtomicLong、AtomicBoolean。它们分别封装了int、long、boolean类型的基本值,提供原子的读写和更新操作。

**引用类型原子类**: AtomicReference、AtomicStampedReference、AtomicMarkableReference。它们封装了对象的引用,提供引用的原子更新。

**数组类型原子类**: AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray。它们封装了数组,提供数组元素的原子更新。

**字段更新器**: AtomicIntegerFieldUpdater、AtomicLongFieldUpdater、AtomicReferenceFieldUpdater。它们提供对对象字段的原子更新能力,适用于需要原子更新对象某个字段的场景。

**累加器**: LongAdder、LongAccumulator、DoubleAdder、DoubleAccumulator。它们专门用于高并发场景下的累加操作,性能优于AtomicLong。

**Striped64**: LongAdder和DoubleAdder的父类,采用分段累加策略实现高并发性能。

#### 5.1.3 原子操作的优势

与传统的锁机制相比,原子操作具有以下优势：

第一,**性能更高**。CAS操作是用户态的,不需要进入内核态,避免了系统调用的开销。同时,CAS操作失败后可以立即重试,不会阻塞线程,减少了线程切换的开销。

第二,**可扩展性更好**。在高度竞争的场景下,原子操作的性能下降是渐进的,而锁机制可能导致线程长时间阻塞。

第三,**编程更简单**。原子类封装了底层的CAS逻辑,开发者只需要调用简单的API即可实现线程安全的操作,无需手动编写复杂的同步代码。

第四,**死锁免疫**。由于不使用锁,原子操作不会出现死锁问题,提高了程序的可靠性。

### 5.2 CAS机制原理

#### 5.2.1 CAS操作定义

CAS（Compare-And-Swap）是一种CPU级别的原子指令,其基本语义是：比较内存位置的值与期望值,如果相等,则将该位置的值更新为新值。整个比较和更新操作是原子的,不会被其他线程中断。

CAS操作通常接受三个参数：内存地址（V）、期望的旧值（A）和要设置的新值（B）。操作的语义是：如果V当前的值等于A,则将V的值更新为B；如果不相等,则什么也不做。无论成功与否,CAS都返回V的当前值。

在Java中,底层CAS操作通过sun.misc.Unsafe类实现。Unsafe类提供了直接操作内存的能力,是实现原子类的基础。

#### 5.2.2 CAS的硬件支持

CAS操作依赖于现代CPU提供的硬件支持。不同架构的CPU提供了不同的指令来实现CAS：

在x86架构下,CAS操作通常使用LOCK CMPXCHG指令。LOCK前缀确保指令执行的原子性,通过总线锁定或缓存一致性协议防止其他CPU核心同时访问同一内存地址。

在ARM架构下,CAS操作使用LDREX和STREX指令对。LDREX（Load-Exclusive）加载内存值并标记访问的内存地址；STREX（Store-Exclusive）尝试存储新值,只有当内存地址没有被其他CPU核心修改时才成功。

这种硬件级别的支持保证了CAS操作的原子性和效率。Java的JVM会根据运行平台选择合适的CPU指令来实现CAS。

#### 5.2.3 CAS的三大问题

尽管CAS是一种高效的同步机制,但它也存在一些固有的问题：

**ABA问题**是CAS最常见的问题。考虑以下场景：线程1读取变量值为A,准备进行CAS更新；在线程1执行CAS之前,线程2将变量值改为B,又改回A；当线程1执行CAS时,发现变量值仍为A,于是更新成功。但实际上,变量在此期间经历了两次变化。ABA问题在某些场景下可能导致逻辑错误。

解决ABA问题的常见方法是使用版本号。每次修改变量时,同时增加一个版本号,CAS操作不仅比较值,还要比较版本号。Java中的AtomicStampedReference和AtomicMarkableReference就是用来解决ABA问题的。

**自旋开销**是CAS的另一个问题。当CAS操作失败时,通常需要重试。如果CAS操作频繁失败且竞争激烈,会导致CPU空转,浪费计算资源。解决方案是限制自旋次数,或者使用自适应自旋（在JVM内部实现）。

**只能保证单个变量的原子性**是CAS的固有局限。CAS操作只能保证一个共享变量的原子更新。如果需要同时更新多个变量,就需要使用其他机制,如锁或事务。

#### 5.2.4 CAS在Java中的实现

Java中的CAS操作主要通过sun.misc.Unsafe类实现。Unsafe类提供了以下CAS方法：

```java
public final class Unsafe {
    // 比较并设置int值
    public final native boolean compareAndSwapInt(Object obj, long offset, int expect, int update);
    
    // 比较并设置long值
    public final native boolean compareAndSwapLong(Object obj, long offset, long expect, long update);
    
    // 比较并设置对象引用
    public final native boolean compareAndSwapObject(Object obj, long offset, Object expect, Object update);
    
    // 获取并增加int值
    public final native int getAndAddInt(Object obj, long offset, int delta);
    
    // 获取并设置int值
    public final native int getAndSetInt(Object obj, long offset, int newValue);
    
    // 获取并增加long值
    public final native long getAndAddLong(Object obj, long offset, long delta);
    
    // 获取并设置long值
    public final native long getAndSetLong(Object obj, long offset, long newValue);
    
    // 获取并设置对象引用
    public final native Object getAndSetObject(Object obj, long offset, Object newValue);
}
```

这些方法都是native方法,直接调用CPU的原子指令。其中,compareAndSwapInt、compareAndSwapLong和compareAndSwapObject是最基础的CAS操作；getAndAddInt、getAndSetInt等是封装了"读取-修改-写回"模式的复合操作。

### 5.3 Unsafe类详解

#### 5.3.1 Unsafe类概述

sun.misc.Unsafe是Java核心类库中一个特殊的存在。它提供了低层次的、接近硬件的内存操作能力,是实现高性能并发工具的基础。虽然名为"Unsafe",但这个类在正确的使用下是安全可靠的。

Unsafe类的设计目的是支持Java虚拟机内部和JDK核心库的底层操作。它提供了以下能力：直接访问和修改内存、直接操作对象字段、线程挂起和恢复、CAS操作等。这些能力对于实现高效的数据结构和并发工具至关重要。

由于Unsafe类的操作非常强大,如果使用不当可能导致严重的问题（如JVM崩溃）,因此它对外部代码是受限的。普通代码无法直接获取Unsafe实例,只能通过反射或内部调用来使用。

#### 5.3.2 Unsafe类的获取

在JDK 8及以下版本,可以通过反射获取Unsafe实例：

```java
public class UnsafeAccessor {
    private static final Unsafe unsafe;
    
    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
```

在JDK 9及以后,JVM提供了更安全的API来访问Unsafe的部分功能。但在并发编程领域,使用反射获取Unsafe仍然是常见的做法。

#### 5.3.3 内存操作

Unsafe类提供了直接操作堆内存的能力：

```java
public class UnsafeMemoryDemo {
    private static final Unsafe unsafe = UnsafeAccessor.getUnsafe();
    
    // 分配内存
    public long allocateMemory(long bytes) {
        return unsafe.allocateMemory(bytes);
    }
    
    // 重新分配内存
    public long reallocateMemory(long address, long bytes) {
        return unsafe.reallocateMemory(address, bytes);
    }
    
    // 释放内存
    public void freeMemory(long address) {
        unsafe.freeMemory(address);
    }
    
    // 设置内存值
    public void setMemory(long address, long bytes, byte value) {
        unsafe.setMemory(address, bytes, value);
    }
    
    // 获取内存中的byte值
    public byte getByte(long address) {
        return unsafe.getByte(address);
    }
    
    // 设置内存中的byte值
    public void putByte(long address, byte value) {
        unsafe.putByte(address, value);
    }
}
```

这些内存操作方法直接操作虚拟机的堆内存,效率极高。但在普通应用中使用这些方法需要格外小心,因为错误的内存访问可能导致JVM崩溃。

#### 5.3.4 对象字段操作

Unsafe类可以直接访问和修改对象的字段,包括private字段：

```java
public class UnsafeFieldDemo {
    private int privateField = 42;
    
    public static void main(String[] args) throws Exception {
        Unsafe unsafe = UnsafeAccessor.getUnsafe();
        UnsafeFieldDemo demo = new UnsafeFieldDemo();
        
        // 获取字段偏移量
        long fieldOffset = unsafe.objectFieldOffset(
            UnsafeFieldDemo.class.getDeclaredField("privateField")
        );
        
        // 读取字段值
        int value = unsafe.getInt(demo, fieldOffset);
        System.out.println("Original value: " + value); // 输出42
        
        // 修改字段值
        unsafe.putInt(demo, fieldOffset, 100);
        System.out.println("Modified value: " + demo.privateField); // 输出100
    }
}
```

这种能力使得实现原子字段更新器成为可能。原子字段更新器正是利用Unsafe的对象字段操作能力,实现了对普通对象字段的原子更新。

#### 5.3.5 线程操作

Unsafe类提供了线程挂起和恢复的能力,这是LockSupport的基础：

```java
public class UnsafeThreadDemo {
    public static void main(String[] args) throws Exception {
        Unsafe unsafe = UnsafeAccessor.getUnsafe();
        
        Thread thread = new Thread(() -> {
            System.out.println("Thread running...");
            unsafe.park(false, 0); // 阻塞当前线程
            System.out.println("Thread resumed!");
        });
        
        thread.start();
        Thread.sleep(1000);
        
        // 唤醒线程
        unsafe.unpark(thread);
    }
}
```

park方法使当前线程等待,直到被unpark唤醒或超时。unpark方法唤醒指定线程。这种能力使得实现无锁的数据结构成为可能。

### 5.4 基础类型原子类详解

#### 5.4.1 AtomicInteger深度解析

AtomicInteger是int类型的原子封装类,提供原子的读写和更新操作。

```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    
    static {
        try {
            valueOffset = unsafe.objectFieldOffset(
                AtomicInteger.class.getDeclaredField("value")
            );
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
    
    private volatile int value;
    
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }
    
    public AtomicInteger() {
    }
    
    public final int get() {
        return value;
    }
    
    public final void set(int newValue) {
        value = newValue;
    }
    
    public final void lazySet(int newValue) {
        unsafe.putOrderedInt(this, valueOffset, newValue);
    }
    
    public final int getAndSet(int newValue) {
        return unsafe.getAndSetInt(this, valueOffset, newValue);
    }
    
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
    
    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
    
    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }
    
    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }
    
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }
    
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }
    
    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }
    
    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }
}
```

AtomicInteger的设计非常巧妙,充分利用了volatile和CAS的优势：

**value字段**被声明为volatile,保证了对该字段的读取能够看到其他线程的修改。这是实现原子操作的基础。

**valueOffset**是value字段在对象中的内存偏移量,用于Unsafe的CAS操作。这个偏移量在静态初始化块中计算一次,之后可以重复使用。

**lazySet方法**使用putOrderedInt实现,这是一个"延迟写入"操作。它不保证立即将新值刷新到主内存,但保证有序性。在某些场景下,这可以提供比set方法更好的性能。

**getAndSet方法**封装了getAndSetInt的CAS操作,原子地设置新值并返回旧值。这是实现简单交换操作的基础。

**compareAndSet方法**是CAS操作的标准实现。它原子地比较当前值与期望值,如果相等则更新为新值。这是实现更复杂原子操作的基础。

**getAndIncrement等方法**封装了getAndAddInt的CAS操作。这些方法使用CAS循环来实现原子递增,避免了使用锁的开销。

#### 5.4.2 CAS循环模式

AtomicInteger的许多方法内部使用CAS循环模式来实现原子更新：

```java
public final int getAndAdd(int delta) {
    int current;
    int newValue;
    do {
        current = get();
        newValue = current + delta;
    } while (!compareAndSet(current, newValue));
    return current;
}
```

CAS循环模式的执行流程如下：

首先,读取当前值。然后,计算新值。接着,使用CAS尝试更新：如果当前值没有被其他线程修改（CAS成功）,则退出循环；否则（CAS失败）,重新读取当前值并重试。

CAS循环模式的优点是不需要使用锁,线程不会阻塞。但它的缺点是在高竞争场景下,可能导致大量的重试,浪费CPU资源。

#### 5.4.3 AtomicLong与AtomicBoolean

AtomicLong是long类型的原子封装类,其实现与AtomicInteger类似：

```java
public class AtomicLong extends Number implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    
    static {
        try {
            valueOffset = unsafe.objectFieldOffset(
                AtomicLong.class.getDeclaredField("value")
            );
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
    
    private volatile long value;
    
    // 其他方法与AtomicInteger类似
}
```

AtomicLong在32位系统上有一个特殊的问题：由于long是64位,读写long值在32位系统上可能不是原子的。JVM通过软件方式（锁总线或使用锁）来保证64位操作的原子性。这在竞争激烈的场景下可能导致性能问题。

在JDK 8中引入了@Contended注解和LongAdder来解决高竞争场景下的性能问题。LongAdder使用分段策略,将热点数据分散到多个变量中,减少竞争。

AtomicBoolean是boolean类型的原子封装类：

```java
public class AtomicBoolean extends Number implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    
    static {
        try {
            valueOffset = unsafe.objectFieldOffset(
                AtomicBoolean.class.getDeclaredField("value")
            );
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
    
    private volatile int value;
    
    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }
    
    public AtomicBoolean() {
        value = 0;
    }
    
    public final boolean get() {
        return value != 0;
    }
    
    public final boolean compareAndSet(boolean expect, boolean update) {
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }
    
    // 其他方法...
}
```

AtomicBoolean使用int来存储boolean值（0表示false,1表示true）。这使得它可以使用CASInt操作来实现原子更新。

### 5.5 引用类型原子类详解

#### 5.5.1 AtomicReference

AtomicReference封装了对象引用,提供引用的原子读写和更新。

```java
public class AtomicReference<V> implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
    
    static {
        try {
            valueOffset = unsafe.objectFieldOffset(
                AtomicReference.class.getDeclaredField("value")
            );
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
    
    private volatile V value;
    
    public AtomicReference(V initialValue) {
        value = initialValue;
    }
    
    public AtomicReference() {
    }
    
    public final V get() {
        return value;
    }
    
    public final void set(V newValue) {
        value = newValue;
    }
    
    public final boolean compareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }
    
    public final V getAndSet(V newValue) {
        return unsafe.getAndSetObject(this, valueOffset, newValue);
    }
}
```

AtomicReference可以用于任何对象,提供引用的原子更新。这在实现无锁算法时非常有用,例如实现无锁的栈、队列等数据结构。

**使用示例：无锁栈**

```java
public class LockFreeStack<E> {
    private final AtomicReference<Node<E>> top = new AtomicReference<>();
    
    private static class Node<E> {
        final E item;
        final Node<E> next;
        
        Node(E item, Node<E> next) {
            this.item = item;
            this.next = next;
        }
    }
    
    public void push(E item) {
        Node<E> newNode = new Node<>(item, null);
        Node<E> oldNode;
        do {
            oldNode = top.get();
            newNode.next = oldNode;
        } while (!top.compareAndSet(oldNode, newNode));
    }
    
    public E pop() {
        Node<E> oldNode;
        Node<E> newNode;
        do {
            oldNode = top.get();
            if (oldNode == null)
                return null;
            newNode = oldNode.next;
        } while (!top.compareAndSet(oldNode, newNode));
        return oldNode.item;
    }
}
```

这个无锁栈使用CAS循环实现push和pop操作。每次修改top引用时,都需要确保没有其他线程同时修改。

#### 5.5.2 解决ABA问题的原子引用

AtomicStampedReference和AtomicMarkableReference提供了解决ABA问题的能力。

**AtomicStampedReference**使用整数戳（stamp）来跟踪引用的版本：

```java
public class AtomicStampedReference<V> {
    private static class Pair<T> {
        final T reference;
        final int stamp;
        
        Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }
        
        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<>(reference, stamp);
        }
    }
    
    private volatile Pair<V> pair;
    
    public AtomicStampedReference(V initialRef, int initialStamp) {
        pair = Pair.of(initialRef, initialStamp);
    }
    
    public V getReference() {
        return pair.reference;
    }
    
    public int getStamp() {
        return pair.stamp;
    }
    
    public boolean compareAndSet(V expectedReference, V newReference,
                                 int expectedStamp, int newStamp) {
        Pair<V> current = pair;
        return
            expectedReference == current.reference &&
            expectedStamp == current.stamp &&
            ((newReference == current.reference &&
              newStamp == current.stamp) ||
             casPair(current, Pair.of(newReference, newStamp)));
    }
}
```

AtomicStampedReference在比较引用值的同时比较戳值,只有两者都匹配时才更新。这有效解决了ABA问题,因为即使引用值相同,戳值不同也说明发生了修改。

**使用示例：使用AtomicStampedReference**

```java
public class Account {
    private final AtomicStampedReference<Long> balance;
    private final int initialStamp = 1;
    
    public Account(long initialBalance) {
        balance = new AtomicStampedReference<>(initialBalance, initialStamp);
    }
    
    public boolean withdraw(long amount) {
        int[] stamp = new int[1];
        long currentBalance;
        do {
            currentBalance = balance.get(stamp);
            if (currentBalance < amount)
                return false;
            // 模拟业务处理时间
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        } while (!balance.compareAndSet(currentBalance, currentBalance - amount,
                                         stamp[0], stamp[0] + 1));
        return true;
    }
    
    public long getBalance() {
        return balance.getReference();
    }
}
```

在这个示例中,每次修改余额都会增加戳值。如果在修改过程中余额被其他线程修改并恢复（ABA场景）,戳值的变化会导致CAS失败,需要重试。

**AtomicMarkableReference**使用boolean标记来跟踪引用：

```java
public class AtomicMarkableReference<V> {
    private static class Pair<T> {
        final T reference;
        final boolean mark;
        
        Pair(T reference, boolean mark) {
            this.reference = reference;
            this.mark = mark;
        }
        
        static <T> Pair<T> of(T reference, boolean mark) {
            return new Pair<>(reference, mark);
        }
    }
    
    private volatile Pair<V> pair;
    
    // 类似于AtomicStampedReference,但使用boolean标记
}
```

AtomicMarkableReference只关心标记是否为true或false,不关心标记的变化次数。这在只需要知道"是否被修改过"的场景下更加高效。

### 5.6 数组类型原子类详解

#### 5.6.1 AtomicIntegerArray

AtomicIntegerArray提供数组元素的原子更新能力：

```java
public class AtomicIntegerArray implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int base = unsafe.arrayBaseOffset(int[].class);
    private static final int shift;
    
    static {
        int scale = unsafe.arrayIndexScale(int[].class);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }
    
    private final int[] array;
    
    public AtomicIntegerArray(int length) {
        array = new int[length];
    }
    
    public AtomicIntegerArray(int[] array) {
        this.array = (int[]) array.clone();
    }
    
    private long byteOffset(int i) {
        return ((long) i << shift) + base;
    }
    
    public final int get(int i) {
        return unsafe.getIntVolatile(array, byteOffset(i));
    }
    
    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, byteOffset(i), newValue);
    }
    
    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(array, byteOffset(i), newValue);
    }
    
    public final boolean compareAndSet(int i, int expect, int update) {
        return unsafe.compareAndSwapInt(array, byteOffset(i), expect, update);
    }
}
```

AtomicIntegerArray的构造方法会复制传入的数组,确保内部数组是独立的。这防止了外部对原数组的修改影响AtomicIntegerArray。

**byteOffset计算**是AtomicIntegerArray的关键技术。它使用数组元素的大小计算偏移量：shift = 31 - Integer.numberOfLeadingZeros(scale)。对于int数组,scale=4（二进制100）,shift=31-29=2。因此,byteOffset(i) = (i << 2) + base,这正好是第i个int元素的内存地址。

#### 5.6.2 使用示例：并发计数器数组

```java
public class ConcurrentCounterArray {
    private final AtomicIntegerArray counters;
    
    public ConcurrentCounterArray(int size) {
        counters = new AtomicIntegerArray(size);
    }
    
    public void increment(int index) {
        counters.incrementAndGet(index);
    }
    
    public int get(int index) {
        return counters.get(index);
    }
    
    public void incrementAll() {
        for (int i = 0; i < counters.length(); i++) {
            counters.incrementAndGet(i);
        }
    }
}
```

### 5.7 字段更新器详解

#### 5.7.1 AtomicIntegerFieldUpdater

AtomicIntegerFieldUpdater提供对对象int字段的原子更新能力：

```java
public abstract class AtomicIntegerFieldUpdater<T> {
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                               String fieldName) {
        return new AtomicIntegerFieldUpdater.UpdaterImplementation<U>(
            tclass, fieldName, false);
    }
    
    public abstract boolean compareAndSet(T obj, int expect, int update);
    public abstract boolean weakCompareAndSet(T obj, int expect, int update);
    public abstract void set(T obj, int newValue);
    public abstract void lazySet(T obj, int newValue);
    public abstract int get(T obj);
    public abstract int getAndSet(T obj, int newValue);
    public abstract int getAndIncrement(T obj);
    public abstract int getAndDecrement(T obj);
    public abstract int getAndAdd(T obj, int delta);
    public abstract int incrementAndGet(T obj);
    // ...
}
```

使用AtomicIntegerFieldUpdater需要注意以下几点：

字段必须是volatile的。字段不能是private的,或者调用者必须在同一个包中。对象类型必须与声明的类型匹配。

```java
public class Counter {
    private volatile int count;
    
    public static void main(String[] args) {
        AtomicIntegerFieldUpdater<Counter> updater =
            AtomicIntegerFieldUpdater.newUpdater(Counter.class, "count");
        
        Counter counter = new Counter();
        updater.incrementAndGet(counter);
        System.out.println(counter.count); // 输出1
    }
}
```

### 5.8 累加器详解

#### 5.8.1 LongAdder与LongAccumulator

LongAdder是JDK 8引入的高并发累加器,专门用于高竞争场景：

```java
public class LongAdder extends Striped64 implements Serializable {
    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null ||
            !casBase(b = base, b + x)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[m & hash.get()]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))
                longAccumulateCell(x, null, uncontended);
        }
    }
    
    public long sum() {
        Cell[] as = cells;
        long sum = base;
        if (as != null) {
            for (Cell a : as) {
                if (a != null)
                    sum += a.value;
            }
        }
        return sum;
    }
}
```

LongAdder的核心思想是分段累加：它维护一个Cell数组,每个Cell维护一个独立的累加值。线程根据hash值选择Cell进行更新,减少了竞争。

**hash函数**使用ThreadLocalRandom的probe值,确保线程能够均匀分布到不同的Cell上。

**base变量**是基础值,当没有竞争时,累加操作直接在base上进行,避免创建Cell数组。

**sum方法**返回所有Cell和base的和。由于sum操作期间可能有新的更新,返回的是近似值。但对于统计目的,这种近似是可接受的。

#### 5.8.2 LongAdder vs AtomicLong

在高竞争场景下,LongAdder的性能显著优于AtomicLong。下表展示了理论上的性能对比：

在竞争程度较低时,AtomicLong和LongAdder的性能相近。在中等竞争程度时,LongAdder开始表现出优势。在高竞争程度时,LongAdder的性能是AtomicLong的数倍甚至数十倍。

LongAdder的优势在于：它将热点分散到多个Cell,减少了CAS失败的概率。但它的缺点是：sum操作不是原子操作,返回的是近似值；占用更多的内存空间。

#### 5.8.3 Striped64原理

Striped64是LongAdder和DoubleAdder的父类,实现了分段累加的核心逻辑：

```java
abstract class Striped64 extends Number {
    transient volatile Cell[] cells;
    transient volatile long base;
    transient volatile int cellsBusy;
    
    final void longAccumulateCell(long x, LongBinaryOperator fn, boolean wasUncontended) {
        int n;
        Cell[] as;
        if ((as = cells) == null || (n = as.length) < 1) {
            // 初始化cells数组
            Cell[] rs = new Cell[2];
            rs[0] = new Cell(x);
            cells = rs;
            return;
        }
        
        // 根据hash选择Cell
        int m = n - 1;
        int hash = getProbe();
        for (;;) {
            Cell a;
            if ((a = as[(m & hash)]) != null) {
                if (!a.cas(v = a.value, fn == null ? v + x : fn.applyAsLong(v, x)))
                    continue;
                return;
            }
            
            // 尝试创建新的Cell
            if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {
                        int sz = as.length << 1;
                        Cell[] rs = new Cell[sz];
                        for (int i = 0; i < sz; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                continue;
            }
            hash = hash >>> 1;
        }
    }
    
    static final int getProbe() {
        return ThreadLocalRandom.getProbe();
    }
}
```

Striped64使用cellsBusy锁来保护cells数组的修改。当Cell数组需要扩容时,使用CAS获取cellsBusy锁,然后创建新的更大的数组。

### 5.9 使用最佳实践

#### 5.9.1 选择合适的原子类

**选择AtomicInteger/AtomicLong的情况**: 简单的计数器或累加器,竞争程度较低。

**选择LongAdder的情况**: 高并发场景下的计数器或累加器,可以容忍sum返回近似值。

**选择AtomicReference的情况**: 需要原子更新对象引用。

**选择AtomicStampedReference的情况**: 需要解决ABA问题。

**选择AtomicIntegerArray的情况**: 需要原子更新数组元素。

**选择AtomicIntegerFieldUpdater的情况**: 需要原子更新对象字段,但不想使用包装类。

#### 5.9.2 避免常见错误

**错误1:在循环中多次CAS可能导致活锁**

```java
// 错误示例
while (true) {
    int current = atomicInt.get();
    int next = current + 1;
    if (atomicInt.compareAndSet(current, next))
        break;
}

// 正确示例:限制重试次数
int retries = 0;
while (true) {
    int current = atomicInt.get();
    int next = current + 1;
    if (atomicInt.compareAndSet(current, next))
        break;
    if (++retries > MAX_RETRIES) {
        // 使用备选方案,如获取锁后更新
        break;
    }
}
```

**错误2:忽视ABA问题**

```java
// 危险示例
if (atomicRef.compareAndSet(expected, newValue)) {
    // 操作可能不是线程安全的
}

// 安全示例:使用AtomicStampedReference
int[] stamp = new int[1];
atomicRef.get(stamp);
if (atomicRef.compareAndSet(expected, newValue, stamp[0], stamp[0] + 1)) {
    // 操作是线程安全的
}
```

**错误3:在循环中使用可能变化的值**

```java
// 错误示例
while (!atomicRef.compareAndSet(value1, value2)) {
    // 可能在CAS过程中value1被修改
}

// 正确示例:使用get()获取最新值
V expected;
do {
    expected = atomicRef.get();
} while (!atomicRef.compareAndSet(expected, newValue));
```

#### 5.9.3 性能优化建议

**减少CAS竞争**: 使用LongAdder替代AtomicLong在高竞争场景下可以显著提高性能。

**使用延迟更新**: 对于不需要立即可见的更新,使用lazySet方法可以提高性能。

**批量操作**: 对于需要更新多个原子变量的场景,尽量使用CAS循环一次性更新,而不是多次独立的CAS操作。

**避免过度使用**: 不是所有地方都需要原子类。如果不需要并发访问,使用普通变量更简单高效。

### 5.10 小结

原子类是Java并发编程的重要工具,它提供了高性能的线程安全操作。理解原子类和CAS机制的原理,对于编写高效、正确的并发程序至关重要。

**核心要点总结：**

第一,**CAS机制**是原子类的核心,它通过硬件级别的原子指令实现无锁算法。CAS有三大问题：ABA问题、自旋开销、只能保证单个变量。

第二,**Unsafe类**提供了CAS操作和其他底层能力,是实现原子类的基础。Unsafe直接操作内存和对象字段,功能强大但使用需谨慎。

第三,**基础类型原子类**（AtomicInteger、AtomicLong、AtomicBoolean）封装了基本类型的原子操作。它们使用volatile保证可见性,CAS保证原子性。

第四,**引用类型原子类**（AtomicReference、AtomicStampedReference、AtomicMarkableReference）提供了对象引用的原子更新。StampedReference和MarkableReference可以解决ABA问题。

第五,**累加器**（LongAdder、LongAccumulator）采用分段策略,在高竞争场景下性能显著优于AtomicLong。

第六,**使用最佳实践**包括选择合适的原子类、避免常见错误、优化性能等。
