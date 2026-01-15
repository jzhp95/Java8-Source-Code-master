# Java NIO Buffer体系详解

## 一、Buffer体系架构

### 1.1 Buffer层次结构

```
Buffer（抽象基类）
├── ByteBuffer（字节缓冲区）
│   ├── HeapByteBuffer（堆内字节缓冲区）
│   ├── DirectByteBuffer（直接字节缓冲区）
│   └── MappedByteBuffer（内存映射字节缓冲区）
├── CharBuffer（字符缓冲区）
├── ShortBuffer（短整型缓冲区）
├── IntBuffer（整型缓冲区）
├── LongBuffer（长整型缓冲区）
├── FloatBuffer（浮点数缓冲区）
└── DoubleBuffer（双精度浮点数缓冲区）
```

### 1.2 Buffer核心概念

**四个核心属性**：
1. **capacity（容量）**：缓冲区的大小，一旦创建不能改变
2. **limit（限制）**：第一个不应该读取或写入的元素的索引
3. **position（位置）**：下一个要读取或写入的元素的索引
4. **mark（标记）**：调用reset()时position将重置到的索引

**不变式**：
```
0 <= mark <= position <= limit <= capacity
```

## 二、Buffer抽象类详解

### 2.1 核心字段

```java
public abstract class Buffer {
    // 容量
    private int capacity;
    
    // 限制
    private int limit;
    
    // 位置
    private int position = 0;
    
    // 标记
    private int mark = -1;
}
```

### 2.2 核心方法

#### 2.2.1 capacity() - 获取容量

```java
public final int capacity() {
    return capacity;
}
```

**特点**：容量一旦创建不能改变

#### 2.2.2 position() - 获取位置

```java
public final int position() {
    return position;
}
```

#### 2.2.3 position(int newPosition) - 设置位置

```java
public final Buffer position(int newPosition) {
    if ((newPosition > limit) || (newPosition < 0))
        throw new IllegalArgumentException();
    position = newPosition;
    if (mark > position) mark = -1;
    return this;
}
```

**设置流程**：
1. 检查newPosition是否合法
2. 更新position
3. 如果mark > position，丢弃mark

#### 2.2.4 limit() - 获取限制

```java
public final int limit() {
    return limit;
}
```

#### 2.2.5 limit(int newLimit) - 设置限制

```java
public final Buffer limit(int newLimit) {
    if ((newLimit > capacity) || (newLimit < 0))
        throw new IllegalArgumentException();
    limit = newLimit;
    if (position > limit) position = limit;
    if (mark > limit) mark = -1;
    return this;
}
```

**设置流程**：
1. 检查newLimit是否合法
2. 更新limit
3. 如果position > limit，调整position
4. 如果mark > limit，丢弃mark

#### 2.2.6 mark() - 标记位置

```java
public final Buffer mark() {
    mark = position;
    return this;
}
```

**特点**：记录当前position为mark

#### 2.2.7 reset() - 重置到标记位置

```java
public final Buffer reset() {
    int m = mark;
    if (m < 0)
        throw new InvalidMarkException();
    position = m;
    return this;
}
```

**重置流程**：
1. 检查mark是否有效
2. 将position重置为mark

#### 2.2.8 clear() - 清空缓冲区

```java
public final Buffer clear() {
    position = 0;
    limit = capacity;
    mark = -1;
    return this;
}
```

**作用**：准备缓冲区用于新的读取或写入操作

**状态变化**：
- position = 0
- limit = capacity
- mark = -1

#### 2.2.9 flip() - 翻转缓冲区

```java
public final Buffer flip() {
    limit = position;
    position = 0;
    mark = -1;
    return this;
}
```

**作用**：准备缓冲区用于写入操作（从读取模式切换到写入模式）

**状态变化**：
- limit = position（之前写入的数据量）
- position = 0
- mark = -1

**使用场景**：写入数据后，调用flip()准备读取数据

#### 2.2.10 rewind() - 重绕缓冲区

```java
public final Buffer rewind() {
    position = 0;
    mark = -1;
    return this;
}
```

**作用**：准备缓冲区用于重新读取数据

**状态变化**：
- position = 0
- limit不变
- mark = -1

**使用场景**：不改变limit的情况下，重新读取数据

#### 2.2.11 remaining() - 获取剩余元素数量

```java
public final int remaining() {
    return limit - position;
}
```

#### 2.2.12 hasRemaining() - 是否有剩余元素

```java
public final boolean hasRemaining() {
    return position < limit;
}
```

#### 2.2.13 isReadOnly() - 是否为只读缓冲区

```java
public abstract boolean isReadOnly();
```

**特点**：抽象方法，子类必须实现

#### 2.2.14 hasArray() - 是否有底层数组

```java
public abstract boolean hasArray();
```

#### 2.2.15 array() - 获取底层数组

```java
public abstract Object array();
```

#### 2.2.16 arrayOffset() - 获取数组偏移量

```java
public abstract int arrayOffset();
```

#### 2.2.17 isDirect() - 是否为直接缓冲区

```java
public abstract boolean isDirect();
```

## 三、ByteBuffer详解

### 3.1 ByteBuffer概述

**ByteBuffer特点**：
- 最常用的缓冲区类型
- 支持字节序（大端/小端）
- 支持视图缓冲区（将字节缓冲区视为其他类型缓冲区）
- 支持直接缓冲区（堆外内存）

### 3.2 核心字段

```java
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {
    // 字节数组
    final byte[] hb;
    
    // 数组偏移量
    final int offset;
    
    // 是否为只读
    boolean isReadOnly;
}
```

### 3.3 创建ByteBuffer

#### 3.3.1 allocate(int capacity) - 分配堆内缓冲区

```java
public static ByteBuffer allocate(int capacity) {
    if (capacity < 0)
        throw new IllegalArgumentException();
    return new HeapByteBuffer(capacity, capacity);
}
```

**特点**：
- 在JVM堆内分配内存
- 受GC管理
- 访问速度快
- 可能受GC影响

#### 3.3.2 allocateDirect(int capacity) - 分配直接缓冲区

```java
public static ByteBuffer allocateDirect(int capacity) {
    return new DirectByteBuffer(capacity);
}
```

**特点**：
- 在堆外内存分配
- 不受GC管理
- 访问速度稍慢
- 适合IO操作，减少拷贝

#### 3.3.3 wrap(byte[] array) - 包装字节数组

```java
public static ByteBuffer wrap(byte[] array) {
    return wrap(array, 0, array.length);
}
```

#### 3.3.4 wrap(byte[] array, int offset, int length) - 包装字节数组的指定范围

```java
public static ByteBuffer wrap(byte[] array, int offset, int length) {
    try {
        return new HeapByteBuffer(array, offset, length);
    } catch (IllegalArgumentException x) {
        throw new IndexOutOfBoundsException();
    }
}
```

**特点**：
- 不分配新内存
- 直接使用现有数组
- 修改缓冲区会影响原数组

### 3.4 字节序（Byte Order）

#### 3.4.1 order() - 获取字节序

```java
public final ByteOrder order() {
    return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
}
```

#### 3.4.2 order(ByteOrder bo) - 设置字节序

```java
public final ByteBuffer order(ByteOrder bo) {
    bigEndian = (bo == ByteOrder.BIG_ENDIAN);
    nativeByteOrder = bigEndian == Bits.byteOrder();
    this.order(bo);
    return this;
}
```

**字节序说明**：
- **BIG_ENDIAN（大端）**：高位字节在前（网络字节序）
- **LITTLE_ENDIAN（小端）**：低位字节在前（x86架构）

### 3.5 读取方法

#### 3.5.1 get() - 读取单个字节

```java
public abstract byte get();
```

#### 3.5.2 get(int index) - 读取指定位置的字节

```java
public abstract byte get(int index);
```

#### 3.5.3 get(byte[] dst) - 读取字节数组

```java
public ByteBuffer get(byte[] dst) {
    return get(dst, 0, dst.length);
}
```

#### 3.5.4 get(byte[] dst, int offset, int length) - 读取字节数组的指定范围

```java
public ByteBuffer get(byte[] dst, int offset, int length) {
    checkBounds(offset, length, dst.length);
    if (length > remaining())
        throw new BufferUnderflowException();
    int end = offset + length;
    for (int i = offset; i < end; i++)
        dst[i] = get();
    return this;
}
```

#### 3.5.5 getChar() - 读取字符

```java
public char getChar() {
    return Bits.getChar(this, nextGetIndex(2), bigEndian);
}
```

#### 3.5.6 getChar(int index) - 读取指定位置的字符

```java
public char getChar(int index) {
    return Bits.getChar(this, checkIndex(index, 2), bigEndian);
}
```

#### 3.5.7 getShort() - 读取短整型

```java
public short getShort() {
    return Bits.getShort(this, nextGetIndex(2), bigEndian);
}
```

#### 3.5.8 getInt() - 读取整型

```java
public int getInt() {
    return Bits.getInt(this, nextGetIndex(4), bigEndian);
}
```

#### 3.5.9 getLong() - 读取长整型

```java
public long getLong() {
    return Bits.getLong(this, nextGetIndex(8), bigEndian);
}
```

#### 3.5.10 getFloat() - 读取浮点数

```java
public float getFloat() {
    return Float.intBitsToFloat(getInt());
}
```

#### 3.5.11 getDouble() - 读取双精度浮点数

```java
public double getDouble() {
    return Double.longBitsToDouble(getLong());
}
```

### 3.6 写入方法

#### 3.6.1 put(byte b) - 写入单个字节

```java
public abstract ByteBuffer put(byte b);
```

#### 3.6.2 put(int index, byte b) - 在指定位置写入字节

```java
public abstract ByteBuffer put(int index, byte b);
```

#### 3.6.3 put(byte[] src) - 写入字节数组

```java
public final ByteBuffer put(byte[] src) {
    return put(src, 0, src.length);
}
```

#### 3.6.4 put(byte[] src, int offset, int length) - 写入字节数组的指定范围

```java
public ByteBuffer put(byte[] src, int offset, int length) {
    checkBounds(offset, length, src.length);
    if (length > remaining())
        throw new BufferOverflowException();
    int end = offset + length;
    for (int i = offset; i < end; i++)
        this.put(src[i]);
    return this;
}
```

#### 3.6.5 put(ByteBuffer src) - 写入另一个ByteBuffer

```java
public ByteBuffer put(ByteBuffer src) {
    if (src == this)
        throw new IllegalArgumentException();
    if (isReadOnly())
        throw new ReadOnlyBufferException();
    int n = src.remaining();
    if (n > remaining())
        throw new BufferOverflowException();
    for (int i = 0; i < n; i++)
        put(src.get());
    return this;
}
```

#### 3.6.6 putChar(char value) - 写入字符

```java
public ByteBuffer putChar(char value) {
    Bits.putChar(this, nextPutIndex(2), value, bigEndian);
    return this;
}
```

#### 3.6.7 putShort(short value) - 写入短整型

```java
public ByteBuffer putShort(short value) {
    Bits.putShort(this, nextPutIndex(2), value, bigEndian);
    return this;
}
```

#### 3.6.8 putInt(int value) - 写入整型

```java
public ByteBuffer putInt(int value) {
    Bits.putInt(this, nextPutIndex(4), value, bigEndian);
    return this;
}
```

#### 3.6.9 putLong(long value) - 写入长整型

```java
public ByteBuffer putLong(long value) {
    Bits.putLong(this, nextPutIndex(8), value, bigEndian);
    return this;
}
```

#### 3.6.10 putFloat(float value) - 写入浮点数

```java
public ByteBuffer putFloat(float value) {
    putInt(Float.floatToIntBits(value));
    return this;
}
```

#### 3.6.11 putDouble(double value) - 写入双精度浮点数

```java
public ByteBuffer putDouble(double value) {
    putLong(Double.doubleToLongBits(value));
    return this;
}
```

### 3.7 视图缓冲区

#### 3.7.1 asCharBuffer() - 转换为字符缓冲区

```java
public abstract CharBuffer asCharBuffer();
```

#### 3.7.2 asShortBuffer() - 转换为短整型缓冲区

```java
public abstract ShortBuffer asShortBuffer();
```

#### 3.7.3 asIntBuffer() - 转换为整型缓冲区

```java
public abstract IntBuffer asIntBuffer();
```

#### 3.7.4 asLongBuffer() - 转换为长整型缓冲区

```java
public abstract LongBuffer asLongBuffer();
```

#### 3.7.5 asFloatBuffer() - 转换为浮点数缓冲区

```java
public abstract FloatBuffer asFloatBuffer();
```

#### 3.7.6 asDoubleBuffer() - 转换为双精度浮点数缓冲区

```java
public abstract DoubleBuffer asDoubleBuffer();
```

### 3.8 其他方法

#### 3.8.1 compact() - 压缩缓冲区

```java
public abstract ByteBuffer compact();
```

**作用**：将未读取的数据复制到缓冲区开头

**状态变化**：
- position = limit - position
- limit = capacity
- mark = -1

#### 3.8.2 slice() - 切片缓冲区

```java
public abstract ByteBuffer slice();
```

**作用**：创建一个新的缓冲区，共享原缓冲区的数据

#### 3.8.3 duplicate() - 复制缓冲区

```java
public abstract ByteBuffer duplicate();
```

**作用**：创建一个新的缓冲区，共享原缓冲区的数据，但独立维护position、limit、mark

#### 3.8.4 asReadOnlyBuffer() - 创建只读缓冲区

```java
public abstract ByteBuffer asReadOnlyBuffer();
```

**作用**：创建一个只读的缓冲区视图

## 四、HeapByteBuffer详解

### 4.1 HeapByteBuffer概述

**HeapByteBuffer特点**：
- 在JVM堆内分配内存
- 使用byte[]作为底层存储
- 访问速度快
- 受GC管理

### 4.2 核心字段

```java
class HeapByteBuffer extends ByteBuffer {
    protected final byte[] hb;
    protected final int offset;
    
    HeapByteBuffer(int cap, int lim) {
        super(-1, 0, lim, cap, new byte[cap], 0);
        this.hb = hb;
        this.offset = 0;
    }
    
    HeapByteBuffer(byte[] buf, int off, int len) {
        super(-1, off, off + len, buf.length, buf, 0);
        this.hb = buf;
        this.offset = off;
    }
}
```

### 4.3 核心方法

#### 4.3.1 get() - 读取单个字节

```java
public byte get() {
    return hb[ix(nextGetIndex())];
}
```

#### 4.3.2 get(int index) - 读取指定位置的字节

```java
public byte get(int i) {
    return hb[ix(checkIndex(i))];
}
```

#### 4.3.3 put(byte b) - 写入单个字节

```java
public ByteBuffer put(byte x) {
    hb[ix(nextPutIndex())] = x;
    return this;
}
```

#### 4.3.4 put(int index, byte b) - 在指定位置写入字节

```java
public ByteBuffer put(int i, byte x) {
    hb[ix(checkIndex(i))] = x;
    return this;
}
```

#### 4.3.5 ix(int i) - 计算实际索引

```java
protected int ix(int i) {
    return i + offset;
}
```

**作用**：计算相对于底层字节数组的实际索引

## 五、DirectByteBuffer详解

### 5.1 DirectByteBuffer概述

**DirectByteBuffer特点**：
- 在堆外内存分配
- 使用Unsafe类直接操作内存
- 不受GC管理
- 适合IO操作，减少拷贝
- 访问速度稍慢

### 5.2 核心字段

```java
class DirectByteBuffer extends MappedByteBuffer implements DirectBuffer {
    protected static final Unsafe unsafe = Bits.unsafe();
    
    private static final long arrayBaseOffset = unsafe.arrayBaseOffset(byte[].class);
    
    private final long address;
    
    DirectByteBuffer(int cap) {
        super(-1, 0, cap, cap);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);
        
        long base = 0;
        try {
            base = unsafe.allocateMemory(size);
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        unsafe.setMemory(base, size, (byte) 0);
        if (pa && (base % ps != 0)) {
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        att = null;
    }
}
```

### 5.3 核心方法

#### 5.3.1 get() - 读取单个字节

```java
public byte get() {
    try {
        return ((unsafe.getByte(ix(nextGetIndex())));
    } finally {
        Reference.reachabilityFence(this);
    }
}
```

#### 5.3.2 get(int index) - 读取指定位置的字节

```java
public byte get(int i) {
    try {
        return ((unsafe.getByte(ix(checkIndex(i))));
    } finally {
        Reference.reachabilityFence(this);
    }
}
```

#### 5.3.3 put(byte b) - 写入单个字节

```java
public ByteBuffer put(byte x) {
    try {
        unsafe.putByte(ix(nextPutIndex()), ((x)));
        return this;
    } finally {
        Reference.reachabilityFence(this);
    }
}
```

#### 5.3.4 put(int index, byte b) - 在指定位置写入字节

```java
public ByteBuffer put(int i, byte x) {
    try {
        unsafe.putByte(ix(checkIndex(i)), ((x)));
        return this;
    } finally {
        Reference.reachabilityFence(this);
    }
}
```

### 5.4 内存释放

**DirectByteBuffer使用Cleaner自动释放内存**：

```java
private static class Deallocator implements Runnable {
    private static Unsafe unsafe = unsafe;
    
    private long address;
    private long size;
    private int capacity;
    
    private Deallocator(long address, long size, int capacity) {
        assert (address != 0);
        this.address = address;
        this.size = size;
        this.capacity = capacity;
    }
    
    public void run() {
        if (address == 0) {
            return;
        }
        unsafe.freeMemory(address);
        address = 0;
        Bits.unreserveMemory(size, capacity);
    }
}
```

## 六、使用示例

### 6.1 基本使用

```java
// 分配缓冲区
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 写入数据
buffer.putInt(123);
buffer.putDouble(99.99);
buffer.putChar('A');

// 翻转缓冲区
buffer.flip();

// 读取数据
int value = buffer.getInt();
double price = buffer.getDouble();
char ch = buffer.getChar();
```

### 6.2 直接缓冲区

```java
// 分配直接缓冲区
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

// 写入数据
buffer.putInt(123);
buffer.flip();

// 读取数据
int value = buffer.getInt();
```

### 6.3 视图缓冲区

```java
// 创建字节缓冲区
ByteBuffer byteBuffer = ByteBuffer.allocate(16);

// 转换为整型缓冲区
IntBuffer intBuffer = byteBuffer.asIntBuffer();

// 写入整型数据
intBuffer.put(1);
intBuffer.put(2);
intBuffer.put(3);
intBuffer.put(4);

// 翻转并读取
intBuffer.flip();
while (intBuffer.hasRemaining()) {
    System.out.println(intBuffer.get());
}
```

### 6.4 Buffer操作

```java
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 写入数据
buffer.putInt(123);
buffer.putInt(456);

// 翻转：准备读取
buffer.flip();

// 读取部分数据
int value1 = buffer.getInt();

// 重绕：重新读取
buffer.rewind();
int value2 = buffer.getInt();

// 清空：准备写入
buffer.clear();
```

## 七、性能对比

### 7.1 HeapByteBuffer vs DirectByteBuffer

| 特性 | HeapByteBuffer | DirectByteBuffer |
|------|----------------|------------------|
| 内存位置 | 堆内 | 堆外 |
| GC管理 | 是 | 否 |
| 访问速度 | 快 | 稍慢 |
| IO性能 | 需要拷贝 | 零拷贝 |
| 适用场景 | 一般用途 | 高性能IO |

### 7.2 使用建议

**使用HeapByteBuffer的场景**：
- 一般数据处理
- 需要频繁访问
- 数据量不大

**使用DirectByteBuffer的场景**：
- 高性能IO操作
- 大数据量
- 需要与本地代码交互

## 八、最佳实践

### 8.1 使用try-with-resources

```java
// DirectByteBuffer会自动释放内存
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
// 使用buffer
// buffer会在GC时自动释放
```

### 8.2 正确使用flip()

```java
// 写入数据
buffer.putInt(123);

// 翻转：准备读取
buffer.flip();

// 读取数据
int value = buffer.getInt();
```

### 8.3 使用compact()替代clear()

```java
// 读取部分数据后，使用compact()保留未读取的数据
buffer.compact();

// 继续写入数据
buffer.putInt(456);
buffer.flip();
```

### 8.4 使用视图缓冲区

```java
// 使用视图缓冲区简化类型转换
IntBuffer intBuffer = byteBuffer.asIntBuffer();
intBuffer.put(123);
```

## 九、相关源码位置

| 类名 | 源码路径 |
|------|----------|
| Buffer | src/main/jdk8/java/nio/Buffer.java |
| ByteBuffer | src/main/jdk8/java/nio/ByteBuffer.java |
| HeapByteBuffer | src/main/jdk8/java/nio/HeapByteBuffer.java |
| DirectByteBuffer | src/main/jdk8/java/nio/DirectByteBuffer.java |
| MappedByteBuffer | src/main/jdk8/java/nio/MappedByteBuffer.java |
| CharBuffer | src/main/jdk8/java/nio/CharBuffer.java |
| ShortBuffer | src/main/jdk8/java/nio/ShortBuffer.java |
| IntBuffer | src/main/jdk8/java/nio/IntBuffer.java |
| LongBuffer | src/main/jdk8/java/nio/LongBuffer.java |
| FloatBuffer | src/main/jdk8/java/nio/FloatBuffer.java |
| DoubleBuffer | src/main/jdk8/java/nio/DoubleBuffer.java |
| ByteOrder | src/main/jdk8/java/nio/ByteOrder.java |
