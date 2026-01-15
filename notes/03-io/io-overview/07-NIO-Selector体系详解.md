# Java NIO Selector体系详解

## 一、Selector体系架构

### 1.1 Selector核心概念

**Selector作用**：
- 多路复用器（Multiplexer）
- 单线程管理多个Channel
- 检测多个Channel的IO事件
- 非阻塞IO的核心组件

**工作原理**：
```
Selector（选择器）
├── 管理多个SelectableChannel
├── 监听Channel的IO事件
├── 返回就绪的Channel
└── 通过SelectionKey表示Channel与Selector的注册关系
```

### 1.2 Selector三个核心集合

**三个集合**：
1. **Key Set（键集合）**：所有已注册的SelectionKey
2. **Selected-Key Set（已选择键集合）**：就绪的SelectionKey
3. **Cancelled-Key Set（已取消键集合）**：已取消但未注销的SelectionKey

**集合关系**：
```
Key Set（所有已注册的键）
├── Selected-Key Set（就绪的键）
└── Cancelled-Key Set（已取消的键）
```

## 二、SelectionKey详解

### 2.1 SelectionKey概述

**SelectionKey特点**：
- 表示Channel与Selector的注册关系
- 包含两个操作集：interest set和ready set
- 支持附件（attachment）
- 线程安全

### 2.2 操作集（Operation Sets）

#### 2.2.1 Interest Set（兴趣集）

**作用**：指定下次选择操作时要测试的操作类别

**设置方法**：
```java
public abstract SelectionKey interestOps(int ops);
```

**获取方法**：
```java
public abstract int interestOps();
```

#### 2.2.2 Ready Set（就绪集）

**作用**：标识Channel已准备好的操作类别

**获取方法**：
```java
public abstract int readyOps();
```

**特点**：只能由Selector更新，不能直接修改

### 2.3 操作类型（Operation Types）

#### 2.3.1 OP_READ - 读就绪

```java
public static final int OP_READ = 1 << 0;
```

**适用Channel**：
- SocketChannel
- ServerSocketChannel（accept）
- DatagramChannel
- Pipe.SinkChannel

#### 2.3.2 OP_WRITE - 写就绪

```java
public static final int OP_WRITE = 1 << 2;
```

**适用Channel**：
- SocketChannel
- DatagramChannel
- Pipe.SourceChannel

#### 2.3.3 OP_CONNECT - 连接就绪

```java
public static final int OP_CONNECT = 1 << 3;
```

**适用Channel**：
- SocketChannel（非阻塞模式）

#### 2.3.4 OP_ACCEPT - 接受就绪

```java
public static final int OP_ACCEPT = 1 << 4;
```

**适用Channel**：
- ServerSocketChannel

### 2.4 核心方法

#### 2.4.1 channel() - 获取Channel

```java
public abstract SelectableChannel channel();
```

#### 2.4.2 selector() - 获取Selector

```java
public abstract Selector selector();
```

#### 2.4.3 isValid() - 是否有效

```java
public abstract boolean isValid();
```

**失效条件**：
- 调用cancel()方法
- 关闭Channel
- 关闭Selector

#### 2.4.4 cancel() - 取消注册

```java
public abstract void cancel();
```

**作用**：
- 将SelectionKey添加到Cancelled-Key Set
- 下次选择操作时注销Channel

#### 2.4.5 interestOps() - 获取兴趣集

```java
public abstract int interestOps();
```

#### 2.4.6 interestOps(int ops) - 设置兴趣集

```java
public abstract SelectionKey interestOps(int ops);
```

**特点**：原子操作

#### 2.4.7 readyOps() - 获取就绪集

```java
public abstract int readyOps();
```

#### 2.4.8 isReadable() - 是否可读

```java
public final boolean isReadable() {
    return (readyOps() & OP_READ) != 0;
}
```

#### 2.4.9 isWritable() - 是否可写

```java
public final boolean isWritable() {
    return (readyOps() & OP_WRITE) != 0;
}
```

#### 2.4.10 isConnectable() - 是否可连接

```java
public final boolean isConnectable() {
    return (readyOps() & OP_CONNECT) != 0;
}
```

#### 2.4.11 isAcceptable() - 是否可接受

```java
public final boolean isAcceptable() {
    return (readyOps() & OP_ACCEPT) != 0;
}
```

#### 2.4.12 attach(Object ob) - 附加对象

```java
public final Object attach(Object ob) {
    return attachmentUpdater.getAndSet(this, ob);
}
```

#### 2.4.13 attachment() - 获取附加对象

```java
public final Object attachment() {
    return attachment;
}
```

### 2.5 使用示例

```java
// 注册Channel到Selector
SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);

// 设置兴趣集
key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

// 检查就绪状态
if (key.isReadable()) {
    // 读取数据
}

if (key.isWritable()) {
    // 写入数据
}

// 附加对象
key.attach("Connection-1");
String attachment = (String) key.attachment();

// 取消注册
key.cancel();
```

## 三、Selector详解

### 3.1 Selector概述

**Selector特点**：
- 多路复用器
- 管理多个SelectableChannel
- 检测Channel的IO事件
- 支持阻塞和非阻塞选择
- 线程安全

### 3.2 核心方法

#### 3.2.1 open() - 打开Selector

```java
public static Selector open() throws IOException {
    return SelectorProvider.provider().openSelector();
}
```

**特点**：
- 使用系统默认的SelectorProvider
- 返回新的Selector实例

#### 3.2.2 isOpen() - 是否打开

```java
public abstract boolean isOpen();
```

#### 3.2.3 keys() - 获取键集合

```java
public abstract Set<SelectionKey> keys();
```

**特点**：
- 返回所有已注册的SelectionKey
- 返回的Set不可直接修改

#### 3.2.4 selectedKeys() - 获取已选择键集合

```java
public abstract Set<SelectionKey> selectedKeys();
```

**特点**：
- 返回就绪的SelectionKey
- 返回的Set可以直接修改（移除）
- 每次选择操作后需要手动清空

#### 3.2.5 select() - 阻塞选择

```java
public abstract int select() throws IOException;
```

**特点**：
- 阻塞直到至少有一个Channel就绪
- 返回就绪的Channel数量

#### 3.2.6 select(long timeout) - 带超时的阻塞选择

```java
public abstract int select(long timeout) throws IOException;
```

**参数说明**：
- **timeout**：超时时间（毫秒）

**特点**：
- 阻塞直到至少有一个Channel就绪或超时
- 返回就绪的Channel数量

#### 3.2.7 selectNow() - 非阻塞选择

```java
public abstract int selectNow() throws IOException;
```

**特点**：
- 立即返回
- 返回就绪的Channel数量（可能为0）

#### 3.2.8 wakeup() - 唤醒Selector

```java
public abstract Selector wakeup();
```

**特点**：
- 唤醒正在阻塞的select()操作
- 如果没有阻塞的select()操作，下次select()会立即返回

#### 3.2.9 close() - 关闭Selector

```java
public abstract void close() throws IOException;
```

**特点**：
- 关闭Selector
- 注销所有已注册的Channel

### 3.3 选择操作流程

**选择操作三个步骤**：

1. **处理已取消的键**：
   - 将Cancelled-Key Set中的键从Key Set中移除
   - 注销对应的Channel
   - 清空Cancelled-Key Set

2. **查询操作系统**：
   - 查询每个Channel的就绪状态
   - 如果Channel就绪：
     - 如果键不在Selected-Key Set中，添加到Selected-Key Set
     - 更新ready set
   - 如果所有键的interest set为空，不更新任何集合

3. **处理新取消的键**：
   - 如果在步骤2中有新取消的键，处理为步骤1

### 3.4 使用示例

#### 3.4.1 基本使用

```java
try (Selector selector = Selector.open();
     SocketChannel channel = SocketChannel.open()) {
    
    // 配置为非阻塞模式
    channel.configureBlocking(false);
    
    // 注册到Selector
    SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
    
    // 选择操作
    while (selector.select() > 0) {
        // 获取已选择的键
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey selectedKey = iterator.next();
            iterator.remove();
            
            if (selectedKey.isReadable()) {
                // 读取数据
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                channel.read(buffer);
                buffer.flip();
                System.out.println(new String(buffer.array(), 0, buffer.limit()));
            }
        }
    }
}
```

#### 3.4.2 服务器示例

```java
try (Selector selector = Selector.open();
     ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
    
    // 配置为非阻塞模式
    serverChannel.configureBlocking(false);
    
    // 绑定到本地地址
    serverChannel.bind(new InetSocketAddress(8080));
    
    // 注册到Selector
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    
    // 选择操作
    while (true) {
        selector.select();
        
        // 获取已选择的键
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            
            if (key.isAcceptable()) {
                // 接受连接
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel client = server.accept();
                client.configureBlocking(false);
                client.register(selector, SelectionKey.OP_READ);
            }
            
            if (key.isReadable()) {
                // 读取数据
                SocketChannel client = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = client.read(buffer);
                if (bytesRead == -1) {
                    // 客户端关闭
                    key.cancel();
                    client.close();
                } else {
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                }
            }
        }
    }
}
```

#### 3.4.3 客户端示例

```java
try (Selector selector = Selector.open();
     SocketChannel channel = SocketChannel.open()) {
    
    // 配置为非阻塞模式
    channel.configureBlocking(false);
    
    // 发起连接
    channel.connect(new InetSocketAddress("localhost", 8080));
    
    // 注册到Selector
    channel.register(selector, SelectionKey.OP_CONNECT);
    
    // 选择操作
    while (true) {
        selector.select();
        
        // 获取已选择的键
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            
            if (key.isConnectable()) {
                // 完成连接
                SocketChannel client = (SocketChannel) key.channel();
                if (client.finishConnect()) {
                    // 连接成功，注册读事件
                    key.interestOps(SelectionKey.OP_READ);
                    
                    // 写入数据
                    ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
                    client.write(buffer);
                }
            }
            
            if (key.isReadable()) {
                // 读取数据
                SocketChannel client = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = client.read(buffer);
                if (bytesRead == -1) {
                    // 服务器关闭
                    key.cancel();
                    client.close();
                } else {
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                }
            }
        }
    }
}
```

#### 3.4.4 使用附件

```java
// 定义连接状态类
class Connection {
    private SocketChannel channel;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    
    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(1024);
        this.writeBuffer = ByteBuffer.allocate(1024);
    }
    
    public SocketChannel getChannel() {
        return channel;
    }
    
    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }
    
    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }
}

// 使用附件
try (Selector selector = Selector.open();
     ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
    
    serverChannel.configureBlocking(false);
    serverChannel.bind(new InetSocketAddress(8080));
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    
    while (true) {
        selector.select();
        
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel client = server.accept();
                client.configureBlocking(false);
                
                // 创建连接对象并附加
                Connection connection = new Connection(client);
                client.register(selector, SelectionKey.OP_READ, connection);
            }
            
            if (key.isReadable()) {
                // 获取附件
                Connection connection = (Connection) key.attachment();
                SocketChannel client = connection.getChannel();
                ByteBuffer buffer = connection.getReadBuffer();
                
                int bytesRead = client.read(buffer);
                if (bytesRead == -1) {
                    key.cancel();
                    client.close();
                } else {
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                }
            }
        }
    }
}
```

## 四、SelectableChannel详解

### 4.1 SelectableChannel概述

**SelectableChannel特点**：
- 可选择通道
- 支持非阻塞模式
- 可以注册到Selector
- 支持多个Selector

### 4.2 核心方法

#### 4.2.1 provider() - 获取SelectorProvider

```java
public abstract SelectorProvider provider();
```

#### 4.2.2 isBlocking() - 是否为阻塞模式

```java
public abstract boolean isBlocking();
```

#### 4.2.3 blockingLock() - 获取阻塞锁

```java
public abstract Object blockingLock();
```

#### 4.2.4 configureBlocking(boolean block) - 配置阻塞模式

```java
public abstract SelectableChannel configureBlocking(boolean block) 
    throws IOException;
```

**特点**：
- 配置为非阻塞模式后，才能注册到Selector
- 已注册到Selector的Channel不能修改阻塞模式

#### 4.2.5 isRegistered() - 是否已注册

```java
public abstract boolean isRegistered();
```

#### 4.2.6 keyFor(Selector sel) - 获取SelectionKey

```java
public abstract SelectionKey keyFor(Selector sel);
```

**特点**：返回Channel在指定Selector上的SelectionKey

#### 4.2.7 register(Selector sel, int ops) - 注册到Selector

```java
public final SelectionKey register(Selector sel, int ops) 
    throws ClosedChannelException {
    return register(sel, ops, null);
}
```

#### 4.2.8 register(Selector sel, int ops, Object att) - 注册到Selector（带附件）

```java
public abstract SelectionKey register(Selector sel, int ops, Object att) 
    throws ClosedChannelException;
```

**参数说明**：
- **sel**：Selector
- **ops**：兴趣集
- **att**：附件对象

## 五、性能优化

### 5.1 使用非阻塞模式

```java
// 配置为非阻塞模式
channel.configureBlocking(false);
```

### 5.2 使用Selector多路复用

```java
// 单线程管理多个Channel
while (selector.select() > 0) {
    // 处理就绪的Channel
}
```

### 5.3 合理设置兴趣集

```java
// 只监听需要的事件
key.interestOps(SelectionKey.OP_READ);
```

### 5.4 使用附件存储状态

```java
// 使用附件存储连接状态
channel.register(selector, SelectionKey.OP_READ, connection);
```

### 5.5 及时处理已选择的键

```java
// 及时从selectedKeys中移除已处理的键
iterator.remove();
```

## 六、最佳实践

### 6.1 使用try-with-resources

```java
// 推荐
try (Selector selector = Selector.open()) {
    // 使用selector
}
```

### 6.2 配置为非阻塞模式

```java
// 推荐
channel.configureBlocking(false);
```

### 6.3 及时移除已选择的键

```java
// 推荐
while (iterator.hasNext()) {
    SelectionKey key = iterator.next();
    iterator.remove();
    // 处理key
}
```

### 6.4 使用附件存储状态

```java
// 推荐
channel.register(selector, SelectionKey.OP_READ, connection);
```

### 6.5 处理连接关闭

```java
// 推荐
if (bytesRead == -1) {
    key.cancel();
    channel.close();
}
```

## 七、常见问题与解决方案

### 7.1 Selector.select()一直阻塞

**原因**：没有就绪的Channel

**解决方案**：
```java
// 使用带超时的select()
selector.select(1000);  // 1秒超时

// 或使用selectNow()
selector.selectNow();
```

### 7.2 处理连接关闭

**原因**：客户端或服务器关闭连接

**解决方案**：
```java
if (bytesRead == -1) {
    key.cancel();
    channel.close();
}
```

### 7.3 处理并发修改

**原因**：多线程同时修改selectedKeys

**解决方案**：
```java
// 使用迭代器遍历
Iterator<SelectionKey> iterator = selectedKeys.iterator();
while (iterator.hasNext()) {
    SelectionKey key = iterator.next();
    iterator.remove();
    // 处理key
}
```

## 八、相关源码位置

| 类名 | 源码路径 |
|------|----------|
| Selector | src/main/jdk8/java/nio/channels/Selector.java |
| SelectionKey | src/main/jdk8/java/nio/channels/SelectionKey.java |
| SelectableChannel | src/main/jdk8/java/nio/channels/SelectableChannel.java |
| SelectorProvider | src/main/jdk8/java/nio/channels/spi/SelectorProvider.java |
| AbstractSelector | src/main/jdk8/java/nio/channels/spi/AbstractSelector.java |
| AbstractSelectableChannel | src/main/jdk8/java/nio/channels/spi/AbstractSelectableChannel.java |
| AbstractSelectionKey | src/main/jdk8/java/nio/channels/spi/AbstractSelectionKey.java |
