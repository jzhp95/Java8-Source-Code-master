# Java NIO Channel体系详解

## 一、Channel体系架构

### 1.1 Channel层次结构

```
Channel（接口）
├── FileChannel（文件通道）
├── SocketChannel（套接字通道）
├── ServerSocketChannel（服务器套接字通道）
├── DatagramChannel（数据报通道）
├── Pipe.SinkChannel（管道写入通道）
├── Pipe.SourceChannel（管道读取通道）
└── AsynchronousChannel（异步通道基类）
    ├── AsynchronousFileChannel（异步文件通道）
    ├── AsynchronousSocketChannel（异步套接字通道）
    └── AsynchronousServerSocketChannel（异步服务器套接字通道）
```

### 1.2 Channel核心概念

**Channel特点**：
- 类似流，但更强大
- 支持双向传输（读写）
- 支持非阻塞IO
- 支持异步IO
- 支持文件映射
- 支持锁机制

**与流的区别**：
| 特性 | Stream | Channel |
|------|--------|---------|
| 方向 | 单向（输入/输出） | 双向（读写） |
| 阻塞 | 阻塞 | 支持非阻塞 |
| 异步 | 不支持 | 支持异步 |
| 缓冲 | 无缓冲 | 需要Buffer |
| 选择器 | 不支持 | 支持Selector |

## 二、Channel接口详解

### 2.1 Channel接口定义

```java
public interface Channel extends Closeable {
    boolean isOpen();
    void close() throws IOException;
}
```

**核心方法**：
- **isOpen()**：检查通道是否打开
- **close()**：关闭通道

### 2.2 ReadableByteChannel接口

```java
public interface ReadableByteChannel extends Channel {
    int read(ByteBuffer dst) throws IOException;
}
```

**特点**：可读的通道

### 2.3 WritableByteChannel接口

```java
public interface WritableByteChannel extends Channel {
    int write(ByteBuffer src) throws IOException;
}
```

**特点**：可写的通道

### 2.4 ByteChannel接口

```java
public interface ByteChannel extends ReadableByteChannel, WritableByteChannel {
}
```

**特点**：可读可写的通道

### 2.5 ScatteringByteChannel接口

```java
public interface ScatteringByteChannel extends ReadableByteChannel {
    long read(ByteBuffer[] dsts) throws IOException;
    long read(ByteBuffer[] dsts, int offset, int length) throws IOException;
}
```

**特点**：支持分散读取（从多个Buffer读取）

### 2.6 GatheringByteChannel接口

```java
public interface GatheringByteChannel extends WritableByteChannel {
    long write(ByteBuffer[] srcs) throws IOException;
    long write(ByteBuffer[] srcs, int offset, int length) throws IOException;
}
```

**特点**：支持聚集写入（写入多个Buffer）

### 2.7 SeekableByteChannel接口

```java
public interface SeekableByteChannel extends ByteChannel {
    int position() throws IOException;
    SeekableByteChannel position(long newPosition) throws IOException;
    long size() throws IOException;
    SeekableByteChannel truncate(long size) throws IOException;
}
```

**特点**：可定位的通道（支持随机访问）

## 三、FileChannel详解

### 3.1 FileChannel概述

**FileChannel特点**：
- 连接到文件的通道
- 支持随机访问（可定位）
- 支持文件映射（内存映射）
- 支持文件锁
- 支持零拷贝传输
- 线程安全

### 3.2 核心方法

#### 3.2.1 position() - 获取当前位置

```java
public abstract long position() throws IOException;
```

#### 3.2.2 position(long newPosition) - 设置当前位置

```java
public abstract FileChannel position(long newPosition) throws IOException;
```

**特点**：可以跳转到文件的任意位置

#### 3.2.3 size() - 获取文件大小

```java
public abstract long size() throws IOException;
```

#### 3.2.4 truncate(long size) - 截断文件

```java
public abstract FileChannel truncate(long size) throws IOException;
```

**特点**：
- 如果size < 当前大小，截断文件
- 如果size > 当前大小，扩展文件

#### 3.2.5 force(boolean metaData) - 强制写入磁盘

```java
public abstract void force(boolean metaData) throws IOException;
```

**参数说明**：
- **metaData**：是否同步元数据

**作用**：确保数据写入磁盘，防止系统崩溃导致数据丢失

#### 3.2.6 transferTo(long position, long count, WritableByteChannel target) - 零拷贝传输

```java
public abstract long transferTo(long position, long count, 
    WritableByteChannel target) throws IOException;
```

**特点**：
- 零拷贝传输（直接从文件系统缓存传输）
- 高性能
- 适用于大文件传输

#### 3.2.7 transferFrom(ReadableByteChannel src, long position, long count) - 零拷贝接收

```java
public abstract long transferFrom(ReadableByteChannel src, 
    long position, long count) throws IOException;
```

**特点**：
- 零拷贝接收
- 高性能

#### 3.2.8 read(ByteBuffer dst) - 读取数据

```java
public abstract int read(ByteBuffer dst) throws IOException;
```

#### 3.2.9 read(ByteBuffer dst, long position) - 在指定位置读取数据

```java
public abstract int read(ByteBuffer dst, long position) throws IOException;
```

**特点**：不影响当前position

#### 3.2.10 write(ByteBuffer src) - 写入数据

```java
public abstract int write(ByteBuffer src) throws IOException;
```

#### 3.2.11 write(ByteBuffer src, long position) - 在指定位置写入数据

```java
public abstract int write(ByteBuffer src, long position) throws IOException;
```

**特点**：不影响当前position

#### 3.2.12 map(MapMode mode, long position, long size) - 内存映射

```java
public abstract MappedByteBuffer map(MapMode mode, long position, long size) 
    throws IOException;
```

**MapMode说明**：
- **MapMode.READ_ONLY**：只读映射
- **MapMode.READ_WRITE**：读写映射
- **MapMode.PRIVATE**：私有映射（写时复制）

**特点**：
- 将文件映射到内存
- 直接操作内存即可操作文件
- 高性能
- 适用于大文件

#### 3.2.13 lock() - 获取独占锁

```java
public final FileLock lock() throws IOException {
    return lock(0L, Long.MAX_VALUE, false);
}
```

#### 3.2.14 lock(long position, long size, boolean shared) - 获取文件锁

```java
public abstract FileLock lock(long position, long size, boolean shared) 
    throws IOException;
```

**参数说明**：
- **position**：锁的起始位置
- **size**：锁的大小
- **shared**：是否为共享锁

**锁类型**：
- **独占锁**：只允许一个进程访问
- **共享锁**：允许多个进程读取

#### 3.2.15 tryLock() - 尝试获取独占锁

```java
public final FileLock tryLock() throws IOException {
    return tryLock(0L, Long.MAX_VALUE, false);
}
```

#### 3.2.16 tryLock(long position, long size, boolean shared) - 尝试获取文件锁

```java
public abstract FileLock tryLock(long position, long size, boolean shared) 
    throws IOException;
```

**特点**：如果锁不可用，立即返回null，不会阻塞

### 3.3 使用示例

#### 3.3.1 文件读写

```java
try (FileChannel channel = FileChannel.open(
        Paths.get("test.txt"), 
        StandardOpenOption.READ, 
        StandardOpenOption.WRITE)) {
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int bytesRead = channel.read(buffer);
    
    buffer.flip();
    channel.write(buffer);
}
```

#### 3.3.2 随机访问

```java
try (FileChannel channel = FileChannel.open(
        Paths.get("test.txt"), 
        StandardOpenOption.READ, 
        StandardOpenOption.WRITE)) {
    
    // 跳转到第100个字节
    channel.position(100);
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    channel.read(buffer);
}
```

#### 3.3.3 内存映射

```java
try (FileChannel channel = FileChannel.open(
        Paths.get("largefile.dat"), 
        StandardOpenOption.READ, 
        StandardOpenOption.WRITE)) {
    
    // 映射文件到内存
    MappedByteBuffer buffer = channel.map(
        FileChannel.MapMode.READ_WRITE, 
        0, 
        channel.size());
    
    // 直接操作内存
    buffer.putInt(0, 123);
    buffer.putInt(4, 456);
}
```

#### 3.3.4 零拷贝传输

```java
try (FileChannel srcChannel = FileChannel.open(
        Paths.get("src.txt"), 
        StandardOpenOption.READ);
     FileChannel destChannel = FileChannel.open(
        Paths.get("dest.txt"), 
        StandardOpenOption.WRITE, 
        StandardOpenOption.CREATE)) {
    
    // 零拷贝传输
    srcChannel.transferTo(0, srcChannel.size(), destChannel);
}
```

#### 3.3.5 文件锁

```java
try (FileChannel channel = FileChannel.open(
        Paths.get("test.txt"), 
        StandardOpenOption.READ, 
        StandardOpenOption.WRITE)) {
    
    // 获取独占锁
    FileLock lock = channel.lock();
    
    try {
        // 临界区操作
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
    } finally {
        // 释放锁
        lock.release();
    }
}
```

## 四、SocketChannel详解

### 4.1 SocketChannel概述

**SocketChannel特点**：
- TCP套接字通道
- 支持非阻塞模式
- 支持异步连接
- 支持选择器（Selector）
- 支持Socket选项配置

### 4.2 核心方法

#### 4.2.1 open() - 打开SocketChannel

```java
public static SocketChannel open() throws IOException {
    return SelectorProvider.provider().openSocketChannel();
}
```

#### 4.2.2 open(SocketAddress remote) - 打开并连接

```java
public static SocketChannel open(SocketAddress remote) throws IOException {
    SocketChannel sc = open();
    try {
        sc.connect(remote);
    } catch (Throwable x) {
        try {
            sc.close();
        } catch (Throwable suppressed) {
            x.addSuppressed(suppressed);
        }
        throw x;
    }
    assert sc.isConnected();
    return sc;
}
```

#### 4.2.3 connect(SocketAddress remote) - 连接到远程地址

```java
public abstract boolean connect(SocketAddress remote) throws IOException;
```

**特点**：
- 阻塞模式：阻塞直到连接成功或失败
- 非阻塞模式：立即返回false，连接在后台进行

#### 4.2.4 finishConnect() - 完成连接

```java
public abstract boolean finishConnect() throws IOException;
```

**特点**：
- 用于非阻塞模式
- 完成异步连接操作
- 返回true表示连接成功

#### 4.2.5 isConnected() - 是否已连接

```java
public abstract boolean isConnected();
```

#### 4.2.6 isConnectionPending() - 连接是否在进行中

```java
public abstract boolean isConnectionPending();
```

#### 4.2.7 read(ByteBuffer dst) - 读取数据

```java
public abstract int read(ByteBuffer dst) throws IOException;
```

#### 4.2.8 write(ByteBuffer src) - 写入数据

```java
public abstract int write(ByteBuffer src) throws IOException;
```

#### 4.2.9 configureBlocking(boolean block) - 配置阻塞模式

```java
public abstract SelectableChannel configureBlocking(boolean block) 
    throws IOException;
```

**参数说明**：
- **block**：true为阻塞模式，false为非阻塞模式

#### 4.2.10 isValid() - 通道是否有效

```java
public final boolean isValid() {
    return (fd != null) && fd.valid();
}
```

### 4.3 使用示例

#### 4.3.1 阻塞模式

```java
try (SocketChannel channel = SocketChannel.open()) {
    // 连接到服务器
    channel.connect(new InetSocketAddress("localhost", 8080));
    
    // 写入数据
    ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
    channel.write(buffer);
    
    // 读取数据
    buffer.clear();
    channel.read(buffer);
    buffer.flip();
    System.out.println(new String(buffer.array(), 0, buffer.limit()));
}
```

#### 4.3.2 非阻塞模式

```java
try (SocketChannel channel = SocketChannel.open()) {
    // 配置为非阻塞模式
    channel.configureBlocking(false);
    
    // 发起连接
    channel.connect(new InetSocketAddress("localhost", 8080));
    
    // 完成连接
    while (!channel.finishConnect()) {
        // 等待连接完成
    }
    
    // 写入数据
    ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
    channel.write(buffer);
}
```

#### 4.3.3 配合Selector使用

```java
try (Selector selector = Selector.open();
     SocketChannel channel = SocketChannel.open()) {
    
    // 配置为非阻塞模式
    channel.configureBlocking(false);
    
    // 注册到Selector
    channel.register(selector, SelectionKey.OP_CONNECT);
    
    // 发起连接
    channel.connect(new InetSocketAddress("localhost", 8080));
    
    // 等待事件
    while (true) {
        selector.select();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            
            if (key.isConnectable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                sc.finishConnect();
                sc.register(selector, SelectionKey.OP_READ);
            }
            
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                sc.read(buffer);
                buffer.flip();
                System.out.println(new String(buffer.array(), 0, buffer.limit()));
            }
        }
    }
}
```

## 五、ServerSocketChannel详解

### 5.1 ServerSocketChannel概述

**ServerSocketChannel特点**：
- TCP服务器套接字通道
- 支持非阻塞模式
- 支持选择器（Selector）
- 用于监听客户端连接

### 5.2 核心方法

#### 5.2.1 open() - 打开ServerSocketChannel

```java
public static ServerSocketChannel open() throws IOException {
    return SelectorProvider.provider().openServerSocketChannel();
}
```

#### 5.2.2 bind(SocketAddress local) - 绑定到本地地址

```java
public final ServerSocketChannel bind(SocketAddress local) throws IOException {
    return bind(local, 0);
}
```

#### 5.2.3 bind(SocketAddress local, int backlog) - 绑定到本地地址

```java
public abstract ServerSocketChannel bind(SocketAddress local, int backlog) 
    throws IOException;
```

**参数说明**：
- **local**：本地地址
- **backlog**：等待连接队列的最大长度

#### 5.2.4 accept() - 接受连接

```java
public abstract SocketChannel accept() throws IOException;
```

**特点**：
- 阻塞模式：阻塞直到有连接
- 非阻塞模式：立即返回null（如果没有连接）

#### 5.2.5 socket() - 获取ServerSocket

```java
public abstract ServerSocket socket();
```

### 5.3 使用示例

#### 5.3.1 阻塞模式

```java
try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
    // 绑定到本地地址
    serverChannel.bind(new InetSocketAddress(8080));
    
    // 接受连接
    while (true) {
        SocketChannel clientChannel = serverChannel.accept();
        
        // 处理客户端连接
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        clientChannel.read(buffer);
        buffer.flip();
        System.out.println(new String(buffer.array(), 0, buffer.limit()));
        
        clientChannel.close();
    }
}
```

#### 5.3.2 非阻塞模式

```java
try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
    // 配置为非阻塞模式
    serverChannel.configureBlocking(false);
    
    // 绑定到本地地址
    serverChannel.bind(new InetSocketAddress(8080));
    
    // 接受连接
    SocketChannel clientChannel = serverChannel.accept();
    if (clientChannel != null) {
        // 处理客户端连接
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        clientChannel.read(buffer);
        buffer.flip();
        System.out.println(new String(buffer.array(), 0, buffer.limit()));
        
        clientChannel.close();
    }
}
```

#### 5.3.3 配合Selector使用

```java
try (Selector selector = Selector.open();
     ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
    
    // 配置为非阻塞模式
    serverChannel.configureBlocking(false);
    
    // 绑定到本地地址
    serverChannel.bind(new InetSocketAddress(8080));
    
    // 注册到Selector
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    
    // 等待事件
    while (true) {
        selector.select();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            
            if (key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel clientChannel = ssc.accept();
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);
            }
            
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                sc.read(buffer);
                buffer.flip();
                System.out.println(new String(buffer.array(), 0, buffer.limit()));
            }
        }
    }
}
```

## 六、DatagramChannel详解

### 6.1 DatagramChannel概述

**DatagramChannel特点**：
- UDP数据报通道
- 支持非阻塞模式
- 支持选择器（Selector）
- 无连接，数据报独立传输

### 6.2 核心方法

#### 6.2.1 open() - 打开DatagramChannel

```java
public static DatagramChannel open() throws IOException {
    return SelectorProvider.provider().openDatagramChannel();
}
```

#### 6.2.2 bind(SocketAddress local) - 绑定到本地地址

```java
public abstract DatagramChannel bind(SocketAddress local) throws IOException;
```

#### 6.2.3 connect(SocketAddress remote) - 连接到远程地址

```java
public abstract DatagramChannel connect(SocketAddress remote) throws IOException;
```

**特点**：
- 连接后只能与指定地址通信
- 可以使用read()和write()方法

#### 6.2.4 disconnect() - 断开连接

```java
public abstract DatagramChannel disconnect() throws IOException;
```

#### 6.2.5 receive(ByteBuffer dst) - 接收数据报

```java
public abstract SocketAddress receive(ByteBuffer dst) throws IOException;
```

**特点**：返回发送方的地址

#### 6.2.6 send(ByteBuffer src, SocketAddress target) - 发送数据报

```java
public abstract int send(ByteBuffer src, SocketAddress target) throws IOException;
```

### 6.3 使用示例

#### 6.3.1 无连接模式

```java
try (DatagramChannel channel = DatagramChannel.open()) {
    // 绑定到本地地址
    channel.bind(new InetSocketAddress(8080));
    
    // 接收数据报
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    SocketAddress sender = channel.receive(buffer);
    buffer.flip();
    System.out.println("Received from " + sender + ": " + 
        new String(buffer.array(), 0, buffer.limit()));
    
    // 发送数据报
    buffer.clear();
    buffer.put("Hello".getBytes());
    buffer.flip();
    channel.send(buffer, sender);
}
```

#### 6.3.2 连接模式

```java
try (DatagramChannel channel = DatagramChannel.open()) {
    // 绑定到本地地址
    channel.bind(new InetSocketAddress(8080));
    
    // 连接到远程地址
    channel.connect(new InetSocketAddress("localhost", 9090));
    
    // 接收数据报
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    channel.read(buffer);
    buffer.flip();
    System.out.println("Received: " + 
        new String(buffer.array(), 0, buffer.limit()));
    
    // 发送数据报
    buffer.clear();
    buffer.put("Hello".getBytes());
    buffer.flip();
    channel.write(buffer);
}
```

## 七、性能优化

### 7.1 使用直接缓冲区

```java
// 使用直接缓冲区减少拷贝
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
channel.read(buffer);
```

### 7.2 使用零拷贝传输

```java
// 使用transferTo进行零拷贝传输
srcChannel.transferTo(0, srcChannel.size(), destChannel);
```

### 7.3 使用内存映射

```java
// 使用内存映射提高大文件访问性能
MappedByteBuffer buffer = channel.map(
    FileChannel.MapMode.READ_WRITE, 
    0, 
    channel.size());
```

### 7.4 使用非阻塞模式

```java
// 使用非阻塞模式提高并发性能
channel.configureBlocking(false);
```

## 八、最佳实践

### 8.1 使用try-with-resources

```java
// 推荐
try (FileChannel channel = FileChannel.open(
        Paths.get("test.txt"), 
        StandardOpenOption.READ)) {
    // 使用channel
}
```

### 8.2 使用直接缓冲区

```java
// 推荐：使用直接缓冲区
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
```

### 8.3 使用零拷贝传输

```java
// 推荐：使用零拷贝传输
srcChannel.transferTo(0, srcChannel.size(), destChannel);
```

### 8.4 配合Selector使用

```java
// 推荐：配合Selector使用非阻塞模式
channel.configureBlocking(false);
channel.register(selector, SelectionKey.OP_READ);
```

## 九、相关源码位置

| 类名 | 源码路径 |
|------|----------|
| Channel | src/main/jdk8/java/nio/channels/Channel.java |
| FileChannel | src/main/jdk8/java/nio/channels/FileChannel.java |
| SocketChannel | src/main/jdk8/java/nio/channels/SocketChannel.java |
| ServerSocketChannel | src/main/jdk8/java/nio/channels/ServerSocketChannel.java |
| DatagramChannel | src/main/jdk8/java/nio/channels/DatagramChannel.java |
| Pipe.SinkChannel | src/main/jdk8/java/nio/channels/Pipe.java |
| Pipe.SourceChannel | src/main/jdk8/java/nio/channels/Pipe.java |
| SelectableChannel | src/main/jdk8/java/nio/channels/SelectableChannel.java |
| SelectorProvider | src/main/jdk8/java/nio/channels/spi/SelectorProvider.java |
