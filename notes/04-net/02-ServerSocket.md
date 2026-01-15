# ServerSocket.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.net.ServerSocket` 是 Java 网络编程中用于服务器端的核心类，它实现了服务器端监听端口、接受客户端连接请求的功能。作为 TCP 服务器端编程的基础组件，ServerSocket 自 JDK 1.0 起就存在于 Java 标准库中，其设计经过多年演进，至今仍是构建 TCP 服务器应用的首选方案。

ServerSocket 类实现了 `java.io.Closeable` 接口，支持资源自动管理，可以配合 try-with-resources 语句使用，确保服务器 socket 在使用完毕后被正确关闭。与客户端 Socket 类一样，ServerSocket 也采用了委托模式，将实际的 socket 操作委托给 `SocketImpl` 对象完成，这种设计保持了网络 API 的一致性和可扩展性。

ServerSocket 的核心职责是：在指定端口上监听连接请求；维护一个待处理连接的队列（backlog）；当有客户端连接时，接受连接并返回与该客户端通信的 Socket 对象；负责处理安全检查和地址绑定等管理性工作。

### 1.2 核心设计原则

ServerSocket 类的设计体现了多个重要的软件设计原则。首先是**关注点分离**原则，ServerSocket 专注于连接监听和接受，而将实际的通信工作交给返回的 Socket 对象处理。这种职责划分使得服务器逻辑清晰：监听 socket 只负责接受连接，每个连接由独立的通信 socket 处理。

其次是**状态管理原则**，ServerSocket 同样采用了状态机模式来管理其生命周期。状态包括：`created`（底层 socket 已创建）、`bound`（已绑定到地址）、`closed`（已关闭）。这些状态的维护确保了操作的合法性和可预测性。

第三是**可扩展性设计**，通过 `SocketImplFactory` 机制，应用程序可以自定义 socket 实现的创建过程。这对于需要特殊 socket 行为的场景（如测试环境、使用自定义网络栈）非常有用。

第四是**安全性集成**，ServerSocket 在关键操作点集成了 SecurityManager 检查，确保在安全管理器存在的环境下，端口监听和连接接受操作受到适当的安全策略约束。

### 1.3 与 Socket 类的关系

理解 ServerSocket 与 Socket 的关系是掌握 Java 网络编程的关键。两者是协作关系而非继承关系：ServerSocket 用于服务器端，负责监听和接受连接；Socket 用于客户端或服务器端的连接处理，负责实际的数据传输。

服务器端的标准工作流程是：创建 ServerSocket 并绑定到指定端口 → 调用 accept() 阻塞等待客户端连接 → accept() 返回一个与客户端通信的 Socket → 使用返回的 Socket 与该客户端进行数据交换 → 通信完成后关闭 Socket → 继续调用 accept() 等待下一个连接。

这种模式被称为"每连接一线程"模式，虽然不是最高效的并发处理方式（在高并发场景下应使用 NIO 或异步 I/O），但对于理解 TCP 服务器的基本工作原理非常有帮助。

### 1.4 应用场景

ServerSocket 在以下场景中被广泛使用：构建 TCP 服务器应用，如 HTTP 服务器、FTP 服务器、邮件服务器等；实现自定义的网络协议服务器；开发游戏服务器、聊天服务器等需要处理多个客户端连接的应用；进行网络服务测试和诊断工具的开发；以及构建分布式系统中的服务节点。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.net.ServerSocket
```

ServerSocket 直接继承自 `java.lang.Object`，没有复杂的继承层次。这种设计选择与 Socket 类一致，反映了网络 socket 操作的高度特殊性，不适合通过继承来扩展功能。

### 2.2 实现的接口

ServerSocket 实现了 `java.io.Closeable` 接口，这是其唯一的接口实现。实现 Closeable 接口意味着 ServerSocket 必须提供 `close()` 方法，用于关闭服务器 socket 并释放相关资源。这个接口的实现在 JDK 7 的 try-with-resources 特性中特别有用：

```java
try (ServerSocket serverSocket = new ServerSocket(8080)) {
    while (true) {
        Socket clientSocket = serverSocket.accept();
        // 处理客户端连接
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### 2.3 与相关类的协作关系

ServerSocket 与多个网络相关类存在密切的协作关系。`Socket` 类是 ServerSocket 的主要协作类，accept() 方法返回的 Socket 对象用于与客户端进行实际的数据通信。`SocketImpl` 类负责底层的 socket 操作，ServerSocket 将大多数操作委托给它执行。`InetSocketAddress` 用于表示绑定的地址（IP + 端口），是 ServerSocket 绑定的标准参数类型。`ServerSocketChannel` 是 NIO 中的对应类，用于非阻塞 I/O 场景，ServerSocket 可以与之关联以支持 NIO 操作。

## 三、核心字段详解

### 3.1 状态标志字段

```java
private boolean created = false;
private boolean bound = false;
private boolean closed = false;
private Object closeLock = new Object();
```

这三个布尔字段构成了 ServerSocket 的状态机，描述了 server socket 在其生命周期中所处的阶段。

`created` 字段表示底层 server socket 是否已创建。当调用 `createImpl()` 方法成功创建底层 socket 实现后，此字段被设置为 true。这个状态的含义是操作系统级别的 socket 句柄已经创建，但尚未绑定到本地地址。

`bound` 字段表示 server socket 是否已绑定到本地地址。通过调用 `bind()` 方法可以将 server socket 绑定到特定的本地地址和端口。这是 server socket 能够接受连接的前提条件。

`closed` 字段表示 server socket 是否已被关闭。一旦 server socket 被关闭，就不能再次接受新的连接，也不能绑定到新的地址。这个状态是不可逆的。

`closeLock` 是一个专用于同步的锁对象，用于保护关闭操作的原子性。由于 server socket 的关闭可能与 accept 操作并发执行，因此需要同步机制来确保状态的一致性。

### 3.2 核心实现字段

```java
SocketImpl impl;
```

`impl` 字段是 ServerSocket 的核心，它持有实际执行 socket 操作的 `SocketImpl` 对象。与 Socket 类一样，ServerSocket 也将底层的监听、接受等操作委托给这个对象完成。

### 3.3 兼容性字段

```java
private boolean oldImpl = false;
```

`oldImpl` 字段用于标识当前的 SocketImpl 实现是否为旧版实现。这个字段主要用于向后兼容 Java 1.1 及更早版本的 socket 实现。在 `isBound()` 方法中，这个字段有特殊的用途：

```java
public boolean isBound() {
    return bound || oldImpl;
}
```

这个逻辑是因为在 JDK 1.3 之前，ServerSocket 在创建时就会自动绑定到地址，因此对于旧版实现，即使 `bound` 字段为 false，也被视为已绑定状态。

## 四、构造函数深度分析

### 4.1 默认构造函数

```java
public ServerSocket() throws IOException {
    setImpl();
}
```

默认构造函数创建一个未绑定的 server socket。这是最灵活的构造函数，允许后续调用 `bind()` 方法来指定绑定的地址和端口。在调用 `bind()` 之前，server socket 不会开始监听任何端口，也不会接受任何连接。

### 4.2 指定端口的构造函数

```java
public ServerSocket(int port) throws IOException {
    this(port, 50, null);
}
```

这个构造函数创建一个 server socket 并自动绑定到指定端口。端口号 0 表示让系统自动分配一个临时端口（通常来自临时端口范围），这在需要临时服务端口的场景中很有用。

`backlog` 参数被设置为默认值 50，表示最多可以有 50 个连接请求在队列中等待被接受。当队列满时，新的连接请求将被拒绝。

### 4.3 指定端口和 backlog 的构造函数

```java
public ServerSocket(int port, int backlog) throws IOException {
    this(port, backlog, null);
}
```

这个构造函数允许同时指定端口和 backlog。backlog 参数控制待处理连接队列的最大长度。当服务器繁忙时，较长的 backlog 可以缓存更多的等待连接，提高系统在突发连接请求下的承受能力。

需要注意的是，backlog 的具体语义是实现相关的，不同的操作系统可能有不同的处理方式。负数或零值会被替换为实现相关的默认值（代码中默认为 50）。

### 4.4 完整参数的构造函数

```java
public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
    setImpl();
    if (port < 0 || port > 0xFFFF)
        throw new IllegalArgumentException("Port value out of range: " + port);
    if (backlog < 1)
        backlog = 50;
    try {
        bind(new InetSocketAddress(bindAddr, port), backlog);
    } catch(SecurityException e) {
        close();
        throw e;
    } catch(IOException e) {
        close();
        throw e;
    }
}
```

这是功能最完整的构造函数，允许指定端口、backlog 和绑定的本地地址。在多宿主主机（有多块网卡或多个 IP 地址的机器）上，可以通过 `bindAddr` 参数指定 server socket 只监听特定的 IP 地址。这在以下场景中很有用：服务器有多个网络接口，但只希望对外提供某个特定接口的服务；需要区分内网和外网服务，使用不同的监听地址。

构造函数内部首先进行参数验证，然后调用 `bind()` 方法完成实际的绑定操作。如果绑定过程中发生异常，构造函数会尝试关闭 server socket 以释放资源。

## 五、核心方法详解

### 5.1 SocketImpl 创建与管理

```java
private void setImpl() {
    if (factory != null) {
        impl = factory.createSocketImpl();
        checkOldImpl();
    } else {
        impl = new SocksSocketImpl();
    }
    if (impl != null)
        impl.setServerSocket(this);
}
```

`setImpl()` 方法负责创建 SocketImpl 对象。与 Socket 类类似，ServerSocket 首先检查是否存在自定义的 SocketImplFactory：如果有，通过工厂创建实现；否则，创建默认的 `SocksSocketImpl`。创建完成后，调用 `SocketImpl.setServerSocket()` 将当前 ServerSocket 对象与实现关联起来。

```java
void createImpl() throws SocketException {
    if (impl == null)
        setImpl();
    try {
        impl.create(true);
        created = true;
    } catch (IOException e) {
        throw new SocketException(e.getMessage());
    }
}
```

`createImpl()` 方法创建底层的 server socket。参数 `true` 表示创建流式 socket（即 TCP socket）。与客户端 Socket 不同，server socket 只需要创建一次，因为它的职责是接受连接而非建立连接。

### 5.2 绑定操作

```java
public void bind(SocketAddress endpoint) throws IOException {
    bind(endpoint, 50);
}

public void bind(SocketAddress endpoint, int backlog) throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!oldImpl && isBound())
        throw new SocketException("Already bound");
    if (endpoint == null)
        endpoint = new InetSocketAddress(0);
    if (!(endpoint instanceof InetSocketAddress))
        throw new IllegalArgumentException("Unsupported address type");
    InetSocketAddress epoint = (InetSocketAddress) endpoint;
    if (epoint.isUnresolved())
        throw new SocketException("Unresolved address");
    if (backlog < 1)
        backlog = 50;
    try {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkListen(epoint.getPort());
        getImpl().bind(epoint.getAddress(), epoint.getPort());
        getImpl().listen(backlog);
        bound = true;
    } catch(SecurityException e) {
        bound = false;
        throw e;
    } catch(IOException e) {
        bound = false;
        throw e;
    }
}
```

`bind()` 方法是 server socket 初始化的核心方法。它执行以下操作：

首先进行状态检查，确保 server socket 未关闭且未绑定。然后验证 endpoint 参数，必须是 `InetSocketAddress` 类型且地址必须可解析。如果 endpoint 为 null，则创建一个绑定到端口 0 的地址，让系统自动选择端口。

接着进行安全检查，如果存在 SecurityManager，调用 `checkListen()` 验证是否有权限在指定端口上监听。低端口号（privileged ports，通常是 1024 以下）通常需要管理员权限。

实际绑定操作通过 `impl.bind()` 完成，它将 socket 绑定到指定的本地地址和端口。然后调用 `impl.listen(backlog)` 开始监听连接请求，并将 backlog 参数传递给操作系统以控制待处理连接队列的大小。

注意异常处理：如果绑定或监听失败，会将 `bound` 重置为 false，确保状态的一致性。

### 5.3 接受连接

```java
public Socket accept() throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!isBound())
        throw new SocketException("Socket is not bound yet");
    Socket s = new Socket((SocketImpl) null);
    implAccept(s);
    return s;
}
```

`accept()` 方法是 server socket 最核心的方法，它阻塞等待客户端连接，并在连接建立后返回一个与该客户端通信的 `Socket` 对象。

该方法首先进行状态检查，确保 server socket 已绑定且未关闭。然后创建一个新的空 Socket 对象（使用 null SocketImpl 构造），最后调用 `implAccept()` 完成实际的接受操作。

```java
protected final void implAccept(Socket s) throws IOException {
    SocketImpl si = null;
    try {
        if (s.impl == null)
            s.setImpl();
        else {
            s.impl.reset();
        }
        si = s.impl;
        s.impl = null;
        si.address = new InetAddress();
        si.fd = new FileDescriptor();
        getImpl().accept(si);

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccept(si.getInetAddress().getHostAddress(),
                                 si.getPort());
        }
    } catch (IOException e) {
        if (si != null)
            si.reset();
        s.impl = si;
        throw e;
    } catch (SecurityException e) {
        if (si != null)
            si.reset();
        s.impl = si;
        throw e;
    }
    s.impl = si;
    s.postAccept();
}
```

`implAccept()` 方法完成了接受连接的底层工作。这个方法被设计为 `protected final`，允许子类覆盖 accept() 返回自定义的 Socket 子类，但接受操作本身的过程是固定的。

方法的主要步骤如下：首先初始化或重置目标 Socket 的 SocketImpl。然后创建一个新的 `InetAddress` 和 `FileDescriptor` 对象，用于存储接受的连接信息。接着调用 `getImpl().accept(si)` 执行底层的 accept 操作，这个调用会阻塞直到有客户端连接到来。连接建立后，进行安全检查，确保接受该连接是被允许的。最后调用 `s.postAccept()` 设置新 Socket 的状态。

### 5.4 关闭操作

```java
public void close() throws IOException {
    synchronized(closeLock) {
        if (isClosed())
            return;
        if (created)
            impl.close();
        closed = true;
    }
}
```

`close()` 方法关闭 server socket，释放相关资源。使用 `closeLock` 进行同步，确保关闭操作的原子性。如果底层 socket 已被创建，则调用 `impl.close()` 关闭它。最后设置 `closed = true` 标记状态。

关闭 server socket 会产生以下影响：任何在 accept() 上阻塞的线程会立即收到 `SocketException`；已经接受的连接（已返回的 Socket 对象）不受影响；新的连接请求将被拒绝。

### 5.5 状态查询方法

ServerSocket 提供了多个状态查询方法：

```java
public boolean isBound() {
    return bound || oldImpl;
}

public boolean isClosed() {
    synchronized(closeLock) {
        return closed;
    }
}
```

`isBound()` 的实现考虑了向后兼容，对于旧版实现，即使 `bound` 字段为 false 也被视为已绑定。`isClosed()` 使用同步锁确保状态读取的线程安全性。

### 5.6 地址和端口获取方法

```java
public InetAddress getInetAddress() {
    if (!isBound())
        return null;
    try {
        InetAddress in = getImpl().getInetAddress();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(in.getHostAddress(), -1);
        return in;
    } catch (SecurityException e) {
        return InetAddress.getLoopbackAddress();
    } catch (SocketException e) {
    }
    return null;
}

public int getLocalPort() {
    if (!isBound())
        return -1;
    try {
        return getImpl().getLocalPort();
    } catch (SocketException e) {
    }
    return -1;
}

public SocketAddress getLocalSocketAddress() {
    if (!isBound())
        return null;
    return new InetSocketAddress(getInetAddress(), getLocalPort());
}
```

这些方法提供了获取 server socket 绑定信息的途径。`getInetAddress()` 返回绑定的本地地址；`getLocalPort()` 返回监听的端口号；`getLocalSocketAddress()` 返回完整的 `SocketAddress` 表示。

### 5.7 Socket 选项配置

```java
public synchronized void setSoTimeout(int timeout) throws SocketException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
}

public synchronized int getSoTimeout() throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
    if (o instanceof Integer) {
        return ((Integer) o).intValue();
    } else {
        return 0;
    }
}
```

`setSoTimeout()` 和 `getSoTimeout()` 用于设置和获取 accept() 方法的超时时间。默认情况下，accept() 会无限期阻塞直到有连接到来。设置超时后，如果超过指定时间没有连接到达，会抛出 `SocketTimeoutException`。

```java
public void setReuseAddress(boolean on) throws SocketException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    getImpl().setOption(SocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
}

public boolean getReuseAddress() throws SocketException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    return ((Boolean) (getImpl().getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
}
```

`SO_REUSEADDR` 选项允许在服务器重启时快速重新绑定到相同的地址和端口。在高可用服务器场景中，这个选项对于实现优雅重启非常重要。

## 六、设计模式分析

### 6.1 工厂方法模式

ServerSocket 类通过 `SocketImplFactory` 机制实现了工厂方法模式。`setSocketImplFactory()` 方法允许应用程序设置自定义的工厂，用于创建 SocketImpl 对象：

```java
public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
    if (factory != null) {
        throw new SocketException("factory already defined");
    }
    SecurityManager security = System.getSecurityManager();
    if (security != null)
        security.checkSetFactory();
    factory = fac;
}
```

工厂方法模式的好处是解耦了 SocketImpl 的创建逻辑与 ServerSocket 的使用逻辑，使得可以灵活替换底层的 socket 实现。

### 6.2 策略模式

与 Socket 类类似，ServerSocket 也使用了策略模式。`SocketImpl` 作为策略接口定义了 socket 操作的行为规范，而具体的实现（如 `SocksSocketImpl`）则提供不同的策略实现。这种设计使得 ServerSocket 可以透明地支持不同的 socket 实现。

### 6.3 模板方法模式

`accept()` 方法的实现体现了模板方法模式的思想：

```java
public Socket accept() throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!isBound())
        throw new SocketException("Socket is not bound yet");
    Socket s = new Socket((SocketImpl) null);
    implAccept(s);
    return s;
}
```

这个方法定义了 accept 操作的骨架（状态检查 → 创建 Socket → 调用 implAccept → 返回 Socket），而具体的接受逻辑由 `implAccept()` 方法提供。子类可以覆盖 `implAccept()` 来改变接受操作的具体行为，同时保持骨架流程不变。

### 6.4 状态模式

ServerSocket 的状态管理（created、bound、closed）可以视为状态模式的应用。每个状态转换都有明确的前置条件约束，确保操作只能在合法状态下执行。

## 七、常见使用模式

### 7.1 基础服务器模式

最基本的 TCP 服务器使用模式是：

```java
ServerSocket serverSocket = new ServerSocket(8080);
while (!serverSocket.isClosed()) {
    Socket clientSocket = serverSocket.accept();
    handleClient(clientSocket);
}

static void handleClient(Socket clientSocket) {
    try (BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
         PrintWriter out = new PrintWriter(
            clientSocket.getOutputStream(), true)) {
        
        String request = in.readLine();
        // 处理请求
        out.println("Response");
        
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

这个模式创建了一个监听 8080 端口的服务器，循环接受连接并处理每个客户端请求。使用 try-with-resources 确保客户端连接被正确关闭。

### 7.2 多线程服务器模式

为了并发处理多个客户端，可以使用线程池：

```java
ExecutorService threadPool = Executors.newFixedThreadPool(10);
ServerSocket serverSocket = new ServerSocket(8080);

while (!serverSocket.isClosed()) {
    Socket clientSocket = serverSocket.accept();
    threadPool.submit(() -> handleClient(clientSocket));
}
```

这种模式避免了为每个连接创建新线程的开销，同时限制了并发处理的数量，防止系统资源耗尽。

### 7.3 带超时的接受模式

如果需要服务器在一定时间后自动检查某些条件，可以使用带超时的 accept：

```java
ServerSocket serverSocket = new ServerSocket(8080);
serverSocket.setSoTimeout(5000);  // 5秒超时

while (!serverSocket.isClosed()) {
    try {
        Socket clientSocket = serverSocket.accept();
        handleClient(clientSocket);
    } catch (SocketTimeoutException e) {
        // 超时，检查是否需要关闭服务器
        if (shouldShutdown()) {
            break;
        }
    }
}
```

这种模式允许服务器定期执行维护任务，如检查关闭标志、清理资源等。

### 7.4 指定绑定地址的模式

在多宿主环境中，可以指定监听特定的 IP 地址：

```java
InetAddress bindAddr = InetAddress.getByName("192.168.1.100");
ServerSocket serverSocket = new ServerSocket(8080, 50, bindAddr);
```

这使得服务器只接受来自指定网络接口的连接请求。

### 7.5 使用 ServerSocketChannel 的 NIO 模式

对于高并发场景，可以使用 NIO 实现非阻塞 I/O：

```java
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.socket().bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);

Selector selector = Selector.open();
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        }
        // 处理读写...
    }
}
```

这种模式可以使用少量线程处理大量并发连接，是构建高性能服务器的基础。

## 八、常见问题与注意事项

### 8.1 端口绑定失败

端口绑定失败是服务器开发中常见的问题。可能的原因包括：端口已被其他进程占用；权限不足（尝试绑定 privileged 端口）；地址已被使用（TIME_WAIT 状态）。

解决方案包括：使用 `SO_REUSEADDR` 选项；检查端口占用情况；使用 `lsof` 或 `netstat` 命令查看端口使用情况；选择其他端口。

```java
ServerSocket serverSocket = new ServerSocket();
serverSocket.setReuseAddress(true);
serverSocket.bind(new InetSocketAddress(8080));
```

### 8.2 backlog 配置

backlog 参数控制等待队列的最大长度，但实际行为依赖于操作系统实现。某些操作系统可能忽略用户设置的 backlog 值，使用系统默认值。

建议在设置 backlog 时考虑以下因素：预期的并发连接峰值；系统的文件描述符限制；网络带宽和延迟特性。

### 8.3 服务器优雅关闭

实现服务器的优雅关闭需要注意：先停止接受新连接（关闭 ServerSocket）；然后等待现有连接处理完成；最后清理资源。

```java
volatile boolean shutdown = false;

// 在主线程中
ServerSocket serverSocket = new ServerSocket(8080);
while (!shutdown) {
    try {
        Socket clientSocket = serverSocket.accept();
        executor.submit(() -> handleClient(clientSocket));
    } catch (SocketException e) {
        if (!shutdown) throw e;
    }
}

// 在关闭钩子或信号处理中
shutdown = true;
serverSocket.close();
executor.shutdown();
executor.awaitTermination(60, TimeUnit.SECONDS);
```

### 8.4 连接队列溢出

当连接请求速率超过服务器处理能力时，连接队列会溢出，新的连接请求会被拒绝。操作系统层面的行为是：某些系统可能直接忽略新连接；某些系统可能发送 RST 包拒绝连接。

监控连接队列状态可以帮助识别和处理这个问题。可以通过 `netstat` 命令查看连接状态分布：

```bash
netstat -an | grep 8080
```

### 8.5 资源泄漏

服务器端资源泄漏通常发生在以下情况：未正确关闭接受的 Socket 连接；未关闭输入输出流；未处理异常情况导致的资源泄漏。

最佳实践是使用 try-with-resources 包装所有资源：

```java
try (Socket clientSocket = serverSocket.accept();
     BufferedReader reader = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));
     PrintWriter writer = new PrintWriter(
        clientSocket.getOutputStream(), true)) {
    // 处理请求
} catch (IOException e) {
    // 错误处理
}
```

## 九、面试常见问题

### 9.1 ServerSocket 的工作原理是什么？

ServerSocket 是 TCP 服务器端的核心类，用于监听指定端口并接受客户端连接。其工作原理包括：创建 ServerSocket 并调用 bind() 绑定到指定端口；调用 listen() 进入监听状态，此时操作系统开始接受连接请求并将其放入 backlog 队列；调用 accept() 方法阻塞等待连接，当有客户端连接时，accept() 返回一个与该客户端通信的 Socket 对象；服务器使用返回的 Socket 与客户端进行数据交换。

底层实现涉及操作系统的 listen() 和 accept() 系统调用。listen() 将 socket 标记为被动套接字，开始监听连接请求。accept() 从已完成连接队列中取出一个连接，如果队列为空则阻塞等待。

### 9.2 backlog 参数的作用是什么？

backlog 参数指定了待处理连接队列的最大长度。当服务器繁忙时，新的连接请求会被放入队列等待被 accept() 处理。如果队列已满，新的连接请求将被拒绝。

需要注意的是，backlog 的实际效果依赖于操作系统实现。某些系统可能施加额外的限制；某些系统可能忽略用户提供的值而使用系统默认值。backlog 至少应设置为预期峰值并发连接数的 1.5 到 2 倍。

### 9.3 ServerSocket 与 Socket 的区别是什么？

ServerSocket 用于服务器端，负责监听端口和接受连接；Socket 用于客户端或服务器端的连接处理，负责实际的数据传输。ServerSocket.accept() 返回一个新的 Socket 对象，用于与特定客户端通信。一个 ServerSocket 可以接受多个连接，每个连接对应一个 Socket。ServerSocket 本身不参与数据传输，只管理连接；Socket 负责实际的数据读写。

### 9.4 如何设置 ServerSocket 的超时？

使用 `setSoTimeout(int timeout)` 方法设置 accept() 的超时时间，单位为毫秒。timeout 为 0 表示无限等待，抛出 `SocketTimeoutException` 表示超时。

```java
ServerSocket serverSocket = new ServerSocket(8080);
serverSocket.setSoTimeout(5000);  // 5秒超时
```

### 9.5 SO_REUSEADDR 选项的作用是什么？

`SO_REUSEADDR` 允许在服务器重启时快速重新绑定到相同的地址和端口。在服务器关闭后，该地址端口可能处于 TIME_WAIT 状态一段时间，启用此选项后，新的绑定可以立即成功，而无需等待 TIME_WAIT 结束。

这对于需要快速重启的服务器（如热备份场景）非常有用。但需要确保没有其他实例正在使用该端口，否则可能导致端口冲突。

### 9.6 ServerSocket 有哪些状态？

ServerSocket 有三种主要状态：未绑定（unbound），socket 已创建但尚未绑定到地址；已绑定（bound），socket 已绑定到特定地址，可以接受连接；已关闭（closed），socket 已关闭，不能再使用。

状态检查方法包括：`isBound()` 检查是否已绑定；`isClosed()` 检查是否已关闭。

### 9.7 如何实现高并发的服务器？

实现高并发服务器的主要方法包括：使用 NIO 或 AIO 实现非阻塞 I/O，使用少量线程处理大量连接；使用线程池管理工作线程，避免频繁创建销毁线程的开销；合理设置 backlog 和 socket 选项；使用高效的数据结构和算法处理请求；考虑使用负载均衡分散请求到多个服务器实例。

Java NIO 提供了 `ServerSocketChannel` 和 `Selector`，可以高效处理大量并发连接。对于最高性能要求，可以考虑使用异步 I/O（`AsynchronousServerSocketChannel`）。

## 十、与其他类的对比

### 10.1 ServerSocket 与 ServerSocketChannel

`ServerSocket` 是传统的阻塞式 I/O API，`ServerSocketChannel` 是 NIO 中的非阻塞式 API。ServerSocket 只能工作于阻塞模式；ServerSocketChannel 可以配置为阻塞或非阻塞模式。ServerSocket 适合简单的单线程服务器；ServerSocketChannel 配合 Selector 可以高效处理大量并发连接。ServerSocket 的 accept() 始终阻塞；ServerSocketChannel 可以使用非阻塞 accept 或异步 accept。

### 10.2 ServerSocket 与 DatagramSocket

`DatagramSocket` 用于 UDP 协议，ServerSocket 用于 TCP 协议。UDP 是无连接的，DatagramSocket 不需要监听和接受过程；TCP 是面向连接的，ServerSocket 需要监听和 accept。ServerSocket 对应 TCP 的监听套接字和已连接套接字；DatagramSocket 对应 UDP 的套接字。ServerSocket 保证数据的可靠传输和顺序；DatagramSocket 不保证这些。

### 10.3 ServerSocket 与 HttpServer

`HttpServer`（位于 `com.sun.net.httpserver` 包）是 JDK 6 引入的轻量级 HTTP 服务器，它在 ServerSocket 之上提供了完整的 HTTP 协议处理能力。ServerSocket 只能处理原始的 TCP 连接，需要自行实现 HTTP 协议；HttpServer 自动处理 HTTP 请求和响应。ServerSocket 更灵活，可以实现任何 TCP 协议；HttpServer 专注于 HTTP 协议。ServerSocket 适合实现自定义协议或需要完全控制连接的场景；HttpServer 适合快速构建简单的 HTTP 服务。

## 十一、ServerSocket 生命周期详解

### 11.1 状态转换图

ServerSocket 对象经历以下状态转换：

初始状态（NEW）：对象已创建，但底层 socket 尚未创建。此时 `created=false`, `bound=false`, `closed=false`。

创建状态（CREATED）：调用 `createImpl()` 后，`created=true`。此时操作系统级别的 socket 句柄已创建。

绑定状态（BOUND）：调用 `bind()` 后，`bound=true`。ServerSocket 已绑定到本地地址和端口，开始监听连接请求。

关闭状态（CLOSED）：调用 `close()` 后，`closed=true`。ServerSocket 已关闭，不能再接受连接。

### 11.2 典型使用流程

标准的 ServerSocket 使用流程如下：

```java
// 1. 创建 ServerSocket
ServerSocket serverSocket = new ServerSocket(8080);

// 2. （可选）设置选项
serverSocket.setReuseAddress(true);
serverSocket.setSoTimeout(5000);

// 3. 接受连接循环
while (!serverSocket.isClosed()) {
    try {
        Socket clientSocket = serverSocket.accept();
        processClient(clientSocket);
    } catch (SocketTimeoutException e) {
        // 处理超时
    }
}

// 4. 关闭
serverSocket.close();
```

这个流程展示了 ServerSocket 从创建到关闭的完整生命周期。

### 11.3 accept() 的行为

`accept()` 方法的行为受多个因素影响：

当 server socket 未绑定时，调用 accept() 会抛出 `SocketException`，提示 "Socket is not bound yet"。

当 server socket 已关闭时，调用 accept() 会抛出 `SocketException`，提示 "Socket is closed"。

当设置了超时且超时到达时，accept() 会抛出 `SocketTimeoutException`，但 server socket 仍然有效，可以继续接受连接。

当 server socket 关闭后，如果有线程在 accept() 中阻塞，该调用会立即抛出 `SocketException`。

