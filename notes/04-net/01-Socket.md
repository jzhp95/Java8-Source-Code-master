# Socket.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.net.Socket` 是 Java 网络编程中最核心的类之一，它封装了 TCP 客户端 socket 的完整功能，为应用程序提供了通过网络进行可靠数据传输的能力。作为 Java 网络 API 的基础组件，Socket 类在 JDK 1.0 时就已存在，其设计经历了二十多年的演进，至今仍是 Java 客户端网络通信的首选方案。

Socket 类实现了 `java.io.Closeable` 接口，这意味着它支持资源自动管理，可以配合 try-with-resources 语句使用，确保网络连接在使用完毕后被正确关闭。该类的设计遵循了"策略模式"的核心思想，通过组合 `SocketImpl` 对象来实际完成网络操作，这种设计使得 Socket 类能够灵活地适应不同的网络实现方式，包括普通的 TCP socket、SOCKS 代理 socket，以及未来可能出现的其他实现方式。

### 1.2 核心设计原则

Socket 类的设计体现了以下几个核心原则。首先是**委托模式**的广泛应用，Socket 类本身不直接处理底层网络操作，而是将所有实际操作委托给 `SocketImpl` 对象完成。这种设计的好处是实现了关注点分离，Socket 类负责高层 API 的组织和状态管理，而 SocketImpl 负责底层的 socket 创建、连接、数据传输等具体操作。从代码结构来看，Socket 类约有 900 行代码，而 SocketImpl 及其相关实现则分布在多个类中，这种拆分使得代码更易于维护和扩展。

其次是**状态机模式**的清晰运用。Socket 对象在其生命周期内会经历一系列确定的状态转换：`created`（已创建底层 socket）→ `bound`（已绑定本地地址）→ `connected`（已建立连接）→ `closed`（已关闭）。此外还有输入输出流关闭状态 `shutIn` 和 `shutOut`。这种状态机的设计使得 socket 的行为在任何时刻都是可预测的，同时也便于实现各种状态检查和约束逻辑。

第三是**工厂模式**的支持。Socket 类允许通过 `SocketImplFactory` 自定义 socket 实现的创建方式，这在需要使用自定义 socket 实现（如用于测试或特殊网络环境）的场景中非常有用。默认情况下，Java 使用平台相关的 socket 实现，对于普通应用这通常是透明的。

第四是**安全性设计**。Socket 类在连接和绑定操作中集成了 SecurityManager 检查，确保在安全管理器存在的环境下，网络操作受到适当的安全策略约束。这在 applet 和其他需要沙箱安全的环境中尤为重要。

### 1.3 应用场景

Socket 类在以下场景中被广泛使用：构建 TCP 客户端应用程序，如 HTTP 客户端、FTP 客户端、邮件客户端等；实现自定义的网络协议通信；构建分布式系统中的节点间通信；开发需要实时数据交换的应用，如游戏服务器客户端、聊天应用等；以及进行网络诊断和测试工具的开发。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.net.Socket
```

Socket 类直接继承自 `java.lang.Object`，没有复杂的继承层次。这种设计选择是合理的，因为 socket 操作具有很强的特殊性，不太适合通过继承来扩展功能。相反，Java 网络库采用了组合优于继承的原则，通过委托给 SocketImpl 来实现功能的扩展和定制。

### 2.2 实现的接口

Socket 类实现了以下接口：

`java.io.Closeable` 接口要求实现 `close()` 方法，这使得 Socket 对象可以用于 try-with-resources 结构，确保连接被正确关闭。在 JDK 7 引入的 try-with-resources 特性之前，开发者需要在 finally 块中手动调用 `close()` 方法，而实现 Closeable 接口后，编译器会自动生成关闭资源的代码。

`java.lang.AutoCloseable` 是 Closeable 的父接口，提供了更广泛的资源管理支持。在实际使用中，建议使用 AutoCloseable 类型声明 Socket，以便在 JDK 7 之前的版本中也能获得基本的自动关闭支持。

### 2.3 与相关类的关系

Socket 类与多个网络相关类存在密切的协作关系。`ServerSocket` 类用于监听和接受传入的连接请求，当 `ServerSocket.accept()` 返回时，返回的是一个与客户端建立连接的 `Socket` 对象。`SocketImpl` 是实际执行 socket 操作的核心类，Socket 类中的大多数方法最终都会调用 `impl` 对象的相关方法。`InetAddress` 类用于表示 IP 地址，Socket 的连接和绑定操作都需要使用 `InetAddress` 对象来指定网络地址。`InetSocketAddress` 是 SocketAddress 的子类，用于同时包含 IP 地址和端口号，是 socket 连接目标的标准表示方式。

## 三、核心字段详解

### 3.1 状态标志字段

```java
private boolean created = false;
private boolean bound = false;
private boolean connected = false;
private boolean closed = false;
private boolean shutIn = false;
private boolean shutOut = false;
```

这六个布尔字段构成了 Socket 的状态机，完整地描述了 socket 在其生命周期中所处的各个阶段。

`created` 字段表示底层 socket 是否已创建。当调用 `createImpl(true)` 方法成功创建底层 socket 实现后，此字段被设置为 true。这个状态的含义是操作系统级别的 socket 句柄已经创建，但尚未绑定到本地地址或连接到远程地址。

`bound` 字段表示 socket 是否已绑定到本地地址。通过调用 `bind()` 方法可以将 socket 绑定到特定的本地地址和端口。即使尚未建立连接，socket 也可以处于已绑定状态。在调用 `connect()` 方法时，如果 socket 尚未绑定，系统会自动为其分配一个临时端口和合适的本地地址，这也会将 `bound` 字段设置为 true。

`connected` 字段表示 socket 是否已成功建立到远程地址的连接。这是 socket 最重要的状态之一，只有处于连接状态的 socket 才能进行数据传输。一旦连接建立，`connected` 状态会一直保持，即使后续调用 `close()` 方法关闭 socket。

`closed` 字段表示 socket 是否已被关闭。一旦 socket 被关闭，就不能再次使用，这是不可逆的状态转换。关闭操作会释放底层资源，包括文件描述符和网络连接。

`shutIn` 和 `shutOut` 分别表示输入流和输出流是否已被关闭。这两个状态允许独立关闭输入或输出，这在某些协议场景中很有用，例如 HTTP/1.1 的半关闭连接就利用了这个特性。

### 3.2 同步与线程安全字段

```java
private Object closeLock = new Object();
```

`closeLock` 是一个简单的对象锁，用于保护关闭操作和状态检查的原子性。由于 socket 的关闭操作可能从多个线程并发调用（例如一个线程正在使用 socket，另一个线程决定关闭它），因此需要同步机制来确保线程安全。

这个锁的使用模式非常典型：在执行任何可能受关闭操作影响的状态检查或修改之前，都需要获取 `closeLock`。例如在 `isClosed()` 方法中：

```java
public boolean isClosed() {
    synchronized (closeLock) {
        return closed;
    }
}
```

使用专用对象作为锁比使用 socket 实例本身作为锁更安全，因为这样可以防止外部代码通过获取 socket 的监视器锁来干扰 socket 的内部操作。

### 3.3 核心实现字段

```java
SocketImpl impl;
```

`impl` 字段是 Socket 类的核心，它持有实际执行网络操作的 `SocketImpl` 对象。几乎所有 socket 操作最终都会委托给这个对象。SocketImpl 是一个抽象类，平台相关的具体实现（如 `PlainSocketImpl`）继承它来提供实际的 socket 功能。

选择使用组合而非继承来扩展 socket 功能是一个明智的设计决策。如果 Socket 类直接继承特定的 SocketImpl 实现，就会与特定的平台实现紧密耦合，难以实现自定义的 socket 行为，也难以支持 SOCKS 代理等特殊功能。通过组合方式，Socket 类可以透明地使用不同类型的 SocketImpl 实现。

### 3.4 兼容性字段

```java
private boolean oldImpl = false;
```

`oldImpl` 字段用于标识当前的 SocketImpl 实现是否为"旧版"实现。这个字段的存在是为了向后兼容 Java 1.1 及更早版本中存在的 socket 实现。在旧版实现中，某些方法（如带超时的 connect）的签名与现代实现不同，通过这个字段，Socket 类可以在运行时选择正确的调用方式。

## 四、构造函数深度分析

### 4.1 默认构造函数

```java
public Socket() {
    setImpl();
}
```

这是最简单的构造函数，它创建一个未绑定、未连接的 socket 对象。调用 `setImpl()` 方法来创建默认的 SocketImpl 实现。默认情况下，会创建一个 `SocksSocketImpl` 对象，这是一个支持 SOCKS 代理的 socket 实现。

这个构造函数创建的 socket 处于最原始的状态：`created`、`bound`、`connected` 三个状态字段都为 false。使用这样的 socket，需要先调用 `bind()` 绑定本地地址，然后调用 `connect()` 连接到远程服务器。

### 4.2 直接创建已连接 Socket

```java
public Socket(String host, int port) throws UnknownHostException, IOException {
    this(host != null ? new InetSocketAddress(host, port) : null,
         new InetSocketAddress(0));
}
```

这个构造函数创建一个 socket 并立即连接到指定的主机和端口。它接受字符串形式的主机名，可以是域名或 IP 地址表示。如果主机名为 null，则会抛出 `NullPointerException`。

内部实现通过创建两个 `InetSocketAddress` 对象来工作：第一个是目标地址，第二个是本地地址（使用端口 0 表示让系统自动选择临时端口）。然后调用私有的构造函数来执行实际的创建和连接操作。

### 4.3 带本地地址绑定的构造函数

```java
public Socket(String host, int port, InetAddress localAddr, int localPort)
        throws IOException {
    this(host != null ? new InetSocketAddress(host, port) : null,
         new InetSocketAddress(localAddr, localPort));
}
```

这个构造函数允许在连接之前指定 socket 绑定的本地地址和端口。在某些场景下这是必要的：例如客户端需要从特定的网络接口或 IP 地址发起连接；或者需要使用特定的源端口以满足某些协议的要求。

`localPort` 参数为 0 表示让系统自动选择临时端口；如果指定特定的端口，需要确保该端口当前未被使用且具有足够的权限（绑定 privileged 端口需要管理员权限）。

### 4.4 SOCKS 代理构造函数

```java
public Socket(Proxy proxy) {
    if (proxy == null) {
        throw new IllegalArgumentException("Invalid Proxy");
    }
    Proxy p = proxy == Proxy.NO_PROXY ? new Proxy(Proxy.Type.SOCKET, 
        new InetSocketAddress("0.0.0.0", 0)) : proxy;
    impl = new SocksSocketImpl(p);
    impl.setSocket(this);
    created = true;
}
```

这个构造函数创建一个通过指定代理服务器连接的 socket。`Proxy` 对象定义了代理的类型、地址和端口信息。Java 支持多种代理类型：Socket 代理（SOCKS）、HTTP 代理等。

如果传入 `Proxy.NO_PROXY`，则等价于不使用代理，构造函数会创建一个特殊的代理配置，最终使用直接的 socket 连接。内部创建 `SocksSocketImpl` 对象来处理代理协议，包括与代理服务器的身份验证和数据转发。

### 4.5 私有复合构造函数

```java
private Socket(SocketAddress address, SocketAddress localAddr,
               boolean stream) throws IOException {
    setImpl();

    if (address == null)
        throw new NullPointerException();

    try {
        createImpl(stream);
        if (localAddr != null)
            bind(localAddr);
        connect(address);
    } catch (IOException | IllegalArgumentException | SecurityException e) {
        try {
            close();
        } catch (IOException ce) {
            e.addSuppressed(ce);
        }
        throw e;
    }
}
```

这个私有构造函数是所有其他构造函数的底层实现。它展示了完整的 socket 创建流程：首先设置 socket 实现，然后创建底层 socket，接着绑定本地地址（如果指定），最后建立连接。

特别注意异常处理部分：如果在创建、绑定或连接过程中发生任何异常，构造函数会尝试关闭 socket 以释放资源，并将关闭异常作为被抑制的异常添加到原始异常中。这确保了即使发生错误，也不会泄漏底层资源。

## 五、核心方法详解

### 5.1 SocketImpl 创建与设置

```java
void setImpl() {
    if (factory != null) {
        impl = factory.createSocketImpl();
        checkOldImpl();
    } else {
        impl = new SocksSocketImpl();
    }
    if (impl != null)
        impl.setSocket(this);
}
```

`setImpl()` 方法负责创建 SocketImpl 对象。它首先检查是否存在自定义的 SocketImplFactory：如果有，通过工厂创建实现；否则，创建默认的 `SocksSocketImpl`。创建完成后，调用 `SocketImpl.setSocket()` 将当前 Socket 对象与实现关联起来，这样 SocketImpl 就能在需要时访问其所属的 Socket 对象。

`createImpl()` 方法用于实际创建底层 socket：

```java
void createImpl(boolean stream) throws SocketException {
    if (impl == null)
        setImpl();
    try {
        impl.create(stream);
        created = true;
    } catch (IOException e) {
        throw new SocketException(e.getMessage());
    }
}
```

`stream` 参数指定了 socket 的类型：true 表示 TCP 流 socket，false 表示 UDP 数据报 socket（这个选项已废弃）。

### 5.2 连接操作

```java
public void connect(SocketAddress endpoint, int timeout) throws IOException {
    if (endpoint == null)
        throw new IllegalArgumentException("connect: The address can't be null");

    if (timeout < 0)
        throw new IllegalArgumentException("connect: timeout can't be negative");

    if (isClosed())
        throw new SocketException("Socket is closed");

    if (!oldImpl && isConnected())
        throw new SocketException("already connected");

    if (!(endpoint instanceof InetSocketAddress))
        throw new IllegalArgumentException("Unsupported address type");

    InetSocketAddress epoint = (InetSocketAddress) endpoint;
    InetAddress addr = epoint.getAddress();
    int port = epoint.getPort();
    checkAddress(addr, "connect");

    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        if (epoint.isUnresolved())
            security.checkConnect(epoint.getHostName(), port);
        else
            security.checkConnect(addr.getHostAddress(), port);
    }
    if (!created)
        createImpl(true);
    if (!oldImpl)
        impl.connect(epoint, timeout);
    else if (timeout == 0) {
        if (epoint.isUnresolved())
            impl.connect(addr.getHostName(), port);
        else
            impl.connect(addr, port);
    } else
        throw new UnsupportedOperationException("SocketImpl.connect(addr, timeout)");
    connected = true;
    bound = true;
}
```

`connect()` 方法是 Socket 类最重要的方法之一，它将 socket 连接到指定的远程地址。该方法执行一系列检查和操作：

首先是参数验证，确保 endpoint 不为 null 且 timeout 非负。然后检查 socket 状态，如果已关闭则抛出异常，如果已连接（且不是旧版实现）则抛出异常。接下来验证 endpoint 的类型，必须是 `InetSocketAddress` 的实例。

安全检查是连接过程的重要组成部分。如果存在 SecurityManager，会调用其 `checkConnect()` 方法验证是否有权限连接到指定地址。这允许安全管理器控制网络访问权限。

实际连接通过 `impl.connect()` 完成，根据 impl 的类型选择正确的重载方法。如果 timeout 为 0，连接会无限期阻塞直到连接建立或失败；如果 timeout 大于 0，则会在指定毫秒数后超时。

连接成功后，设置 `connected` 和 `bound` 状态字段。即使之前没有显式调用 `bind()`，连接操作也会使 socket 处于绑定状态，因为操作系统在建立 TCP 连接时会自动分配本地端口。

### 5.3 绑定操作

```java
public void bind(SocketAddress bindpoint) throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!oldImpl && isBound())
        throw new SocketException("Already bound");

    if (bindpoint != null && (!(bindpoint instanceof InetSocketAddress)))
        throw new IllegalArgumentException("Unsupported address type");
    InetSocketAddress epoint = (InetSocketAddress) bindpoint;
    if (epoint != null && epoint.isUnresolved())
        throw new SocketException("Unresolved address");
    if (epoint == null) {
        epoint = new InetSocketAddress(0);
    }
    InetAddress addr = epoint.getAddress();
    int port = epoint.getPort();
    checkAddress(addr, "bind");
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
        security.checkListen(port);
    }
    getImpl().bind (addr, port);
    bound = true;
}
```

`bind()` 方法将 socket 绑定到本地地址。绑定操作在以下场景中是必需的：需要指定客户端使用的源端口；需要从特定的网络接口发起连接；需要在防火墙或 NAT 环境中正确建立连接。

该方法首先检查 socket 状态，然后验证 bindpoint 参数。值得注意的是，如果 bindpoint 为 null，方法会自动创建一个绑定到端口 0 的地址，这意味着让系统自动选择临时端口和合适的本地地址。

安全检查方面，如果存在 SecurityManager，会调用 `checkListen()` 方法验证是否有权限在指定端口上监听。虽然客户端 socket 通常不需要"监听"，但这个检查确保了客户端不会绑定的 privileged 端口上（低于 1024 的端口通常需要管理员权限）。

### 5.4 状态查询方法

Socket 类提供了丰富的状态查询方法，这些方法在网络编程中非常常用：

`isConnected()` 方法检查 socket 是否已建立连接：

```java
public boolean isConnected() {
    return connected;
}
```

注意这个方法的实现非常简单，只是返回 `connected` 字段的值。这意味着即使连接因网络问题而断开（例如服务器崩溃、网络中断），`isConnected()` 仍然会返回 true。要检测连接是否仍然有效，需要使用其他机制，如设置 SO_KEEPALIVE 选项或自行实现心跳检测。

`isBound()` 方法检查 socket 是否已绑定到本地地址：

```java
public boolean isBound() {
    return bound;
}
```

`isClosed()` 方法检查 socket 是否已被关闭：

```java
public boolean isClosed() {
    synchronized (closeLock) {
        return closed;
    }
}
```

这个方法使用 `closeLock` 进行同步，确保在多线程环境下能正确读取状态。

`isInputShutdown()` 和 `isOutputShutdown()` 方法分别检查输入流和输出流是否已关闭：

```java
public boolean isInputShutdown() {
    synchronized (closeLock) {
        return shutIn;
    }
}

public boolean isOutputShutdown() {
    synchronized (closeLock) {
        return shutOut;
    }
}
```

### 5.5 地址和端口获取方法

Socket 类提供了多个获取地址和端口信息的方法：

```java
public InetAddress getInetAddress() {
    if (!isConnected())
        return null;
    try {
        return getImpl().getInetAddress();
    } catch (SocketException e) {
    }
    return null;
}
```

`getInetAddress()` 返回远程连接地址。如果 socket 尚未连接，则返回 null。由于可能发生异常（虽然不太可能，因为已经检查了连接状态），需要捕获并处理 `SocketException`。

```java
public InetAddress getLocalAddress() {
    if (!isBound())
        return InetAddress.anyLocalAddress();
    InetAddress in = null;
    try {
        in = (InetAddress) getImpl().getOption(SocketOptions.SO_BINDADDR);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(in.getHostAddress(), -1);
        if (in.isAnyLocalAddress()) {
            in = InetAddress.anyLocalAddress();
        }
    } catch (SecurityException e) {
        in = InetAddress.getLoopbackAddress();
    } catch (Exception e) {
        in = InetAddress.anyLocalAddress();
    }
    return in;
}
```

`getLocalAddress()` 返回本地绑定的地址。如果 socket 尚未绑定，则返回通配地址（0.0.0.0 或 ::0）。该方法还处理了安全检查，如果安全管理器拒绝访问，则返回回环地址。

```java
public int getPort() {
    if (!isConnected())
        return 0;
    try {
        return getImpl().getPort();
    } catch (SocketException e) {
    }
    return -1;
}

public int getLocalPort() {
    if (!isBound())
        return -1;
    try {
        return getImpl().getLocalPort();
    } catch(SocketException e) {
    }
    return -1;
}
```

`getPort()` 和 `getLocalPort()` 分别返回远程端口和本地端口号。未连接或未绑定的 socket 返回特殊的无效值（0 或 -1）。

## 六、设计模式分析

### 6.1 策略模式

Socket 类是策略模式的典型应用。`SocketImpl` 作为策略接口，定义了 socket 操作的行为规范，而 `SocksSocketImpl`、`PlainSocketImpl` 等具体实现则提供了不同的策略实现。Socket 类在运行时可以选择不同的策略，这使得系统可以灵活地适应不同的网络环境和使用场景。

这种设计的好处是显而易见的：当需要支持新的代理类型或特殊的 socket 实现时，只需要添加新的 SocketImpl 子类，而无需修改 Socket 类本身。这遵循了开放-封闭原则，对扩展开放，对修改关闭。

### 6.2 工厂方法模式

`SocketImplFactory` 接口和 `setSocketImplFactory()` 方法体现了工厂方法模式。通过设置自定义的工厂，应用程序可以控制 SocketImpl 对象的创建过程。这在测试场景中特别有用：可以创建模拟的 SocketImpl 来测试网络代码而无需实际的网络连接。

```java
public static synchronized void setSocketImplFactory(SocketImplFactory fac)
    throws IOException
{
    if (factory != null)
        throw new SocketException("factory already defined");
    SecurityManager security = System.getSecurityManager();
    if (security != null)
        security.checkSetFactory();
    factory = fac;
}
```

工厂方法本身也受到安全管理器的保护，防止恶意代码替换默认的 socket 实现。

### 6.3 状态模式

Socket 对象的状态管理（created、bound、connected、closed 等）可以视为状态模式的应用。虽然没有使用传统状态模式的对象结构，但状态字段的组合确实模拟了一个状态机。每个状态转换都有明确的前置条件检查，确保 socket 只能按合法的方式转换状态。

### 6.4 模板方法模式

私有构造函数中的 socket 创建流程体现了模板方法模式的思想：

```java
private Socket(SocketAddress address, SocketAddress localAddr,
               boolean stream) throws IOException {
    setImpl();
    createImpl(stream);
    if (localAddr != null)
        bind(localAddr);
    connect(address);
}
```

这个固定的流程（设置实现 → 创建 socket → 绑定 → 连接）定义了 socket 创建的算法骨架，而具体的实现细节则由各个方法自行处理。

## 七、常见使用模式

### 7.1 基础客户端连接模式

最基本的使用模式是创建 socket、连接服务器、发送请求、接收响应、关闭连接：

```java
Socket socket = new Socket("example.com", 80);
PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
BufferedReader reader = new BufferedReader(
    new InputStreamReader(socket.getInputStream()));

writer.println("GET / HTTP/1.1");
writer.println("Host: example.com");
writer.println();

String response;
while ((response = reader.readLine()) != null) {
    System.out.println(response);
}

socket.close();
```

这个模式需要注意几个要点：使用 `getOutputStream()` 获取输出流进行发送，使用 `getInputStream()` 获取输入流进行接收；PrintWriter 的 auto-flush 功能对于交互式协议很有用；读取响应时需要知道何时结束（这里简单地按行读取，可能不适用于所有协议）。

### 7.2 带超时的连接模式

对于可能不可达的服务器或需要快速失败的情况，可以使用带超时的连接：

```java
Socket socket = new Socket();
SocketAddress address = new InetSocketAddress("example.com", 80);

try {
    socket.connect(address, 5000);  // 5秒超时
    // 连接成功，继续后续操作
} catch (SocketTimeoutException e) {
    System.out.println("连接超时");
} catch (IOException e) {
    System.out.println("连接失败: " + e.getMessage());
}
```

使用无参构造函数创建 socket，然后在需要时调用带超时的 `connect()` 方法。这种模式允许更灵活地控制连接行为，适合构建响应灵敏的客户端应用。

### 7.3 绑定特定地址的连接

当需要从特定地址发起连接时：

```java
InetAddress localAddr = InetAddress.getByName("192.168.1.100");
Socket socket = new Socket("example.com", 80, localAddr, 0);
```

这在多宿主环境中很有用，机器有多个网络接口时可以选择从特定接口发起连接。

### 7.4 代理连接模式

通过 SOCKS 代理服务器连接：

```java
Proxy proxy = new Proxy(Proxy.Type.SOCKET, 
    new InetSocketAddress("proxy.example.com", 1080));
Socket socket = new Socket(proxy);
socket.connect(new InetSocketAddress("example.com", 80));
```

这种模式常用于企业网络环境中，需要通过代理访问外部网络的情况。

### 7.5 使用 try-with-resources

JDK 7 及以后，推荐使用 try-with-resources 确保 socket 被正确关闭：

```java
try (Socket socket = new Socket("example.com", 80)) {
    // 使用 socket 进行通信
    // socket 会在 try 块结束时自动关闭
} catch (IOException e) {
    e.printStackTrace();
}
```

自动关闭机制调用 `socket.close()` 方法，释放底层资源。这比手动在 finally 块中关闭更简洁，也更不容易出错。

## 八、常见问题与注意事项

### 8.1 连接不释放问题

socket 关闭后底层资源可能不会立即释放，导致连接处于 TIME_WAIT 状态。这在高并发场景下可能导致端口耗尽。解决方法包括设置 `SO_REUSEADDR` 选项（在服务器端使用更常见）：

```java
Socket socket = new Socket();
socket.setReuseAddress(true);
socket.connect(address);
```

### 8.2 连接状态检测

`isConnected()` 只反映是否曾经成功连接过，并不表示当前连接仍然有效。要检测连接是否存活，可以：

使用 `SO_KEEPALIVE` 选项让 TCP 自动检测：

```java
socket.setKeepAlive(true);
```

或者实现应用层心跳机制：

```java
socket.setSoTimeout(30000);  // 设置读超时
try {
    // 定期发送心跳并等待响应
    out.println("HEARTBEAT");
    String response = in.readLine();
} catch (SocketTimeoutException e) {
    // 连接可能已断开
}
```

### 8.3 字节序和网络字节序

TCP/IP 协议使用大端字节序（网络字节序），而 x86 架构使用小端字节序。Java 的 `DataOutputStream` 和 `DataInputStream` 自动处理了字节序问题，但如果是自行解析二进制协议，需要注意这一点：

```java
// 正确：使用 DataInputStream 处理网络数据
DataInputStream dis = new DataInputStream(socket.getInputStream());
int value = dis.readInt();  // 自动转换为本地字节序

// 错误：直接读取可能得到错误的值
InputStream is = socket.getInputStream();
byte[] bytes = new byte[4];
is.read(bytes);
int value = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
```

### 8.4 Nagle 算法与延迟

TCP 默认启用 Nagle 算法，该算法将小数据包合并以减少网络流量，但这会增加延迟。对于需要低延迟的应用（如游戏、实时通信），可以禁用 Nagle 算法：

```java
socket.setTcpNoDelay(true);
```

这会立即发送数据而不等待合并，但可能导致更多的网络包，在某些情况下反而降低性能。

### 8.5 半关闭连接

当发送完所有数据后，可以关闭输出流但保持输入流打开，以实现 TCP 半关闭状态：

```java
socket.shutdownOutput();  // 关闭输出流
// 继续读取服务器的响应
String response = reader.readLine();
```

这发送一个 FIN 包告诉服务器端"我不会再发送数据了"，但仍然可以接收服务器发送的数据。这种模式在 HTTP/1.1 协议中用于实现 keep-alive 连接。

## 九、面试常见问题

### 9.1 Socket 的工作原理是什么？

Socket 本质上是操作系统提供的一种进程间通信机制，用于不同主机之间的通信。在 TCP/IP 协议栈中，socket 对应于传输层的端点。每个 socket 由 IP 地址和端口号的组合唯一标识。

建立 TCP socket 连接的过程包括：服务器端创建 socket 并绑定到指定端口，然后进入监听状态；客户端创建 socket 并调用 connect 发起连接；服务器端通过 accept 接受连接，返回一个新的 socket 用于与该客户端通信；建立连接后，双方通过 read/write（或 recv/send）进行数据传输；通信完成后，双方各自关闭连接。

这个过程中涉及三次握手：客户端发送 SYN，服务器响应 SYN+ACK，客户端发送 ACK 确认，连接建立。关闭连接时涉及四次挥手。

### 9.2 Socket 与 ServerSocket 的区别？

Socket 和 ServerSocket 是 TCP 通信的两端，分别用于客户端和服务器端。ServerSocket 用于服务器端，负责监听指定端口、接受客户端连接请求，每接受一个连接就创建一个新的 Socket 实例来处理该连接。Socket 用于客户端或服务器端处理连接后，负责具体的通信操作。

从使用流程来看，服务器端需要：创建 ServerSocket → 调用 accept() 阻塞等待连接 → 处理连接 → 关闭连接 Socket。客户端需要：创建 Socket → 调用 connect() 连接服务器 → 通信 → 关闭 Socket。

一个重要的区别是：ServerSocket 本身不参与数据传输，它只负责监听和接受连接；实际的通信由 accept() 返回的 Socket 对象完成。

### 9.3 Socket 连接有多少种状态？

TCP 连接有多种状态：LISTEN 表示服务器端在监听端口；SYN_SENT 表示客户端已发送 SYN 正在等待服务器响应；SYN_RECEIVED 表示服务器端收到 SYN 并已发送响应，正在等待客户端的最终确认；ESTABLISHED 表示连接已建立，双方可以传输数据；FIN_WAIT_1 表示主动关闭方已发送 FIN 等待响应；FIN_WAIT_2 表示主动关闭方已收到响应，正在等待对端的关闭请求；TIME_WAIT 表示主动关闭方已收到 FIN 并已发送最后的 ACK，等待可能延迟到达的报文；CLOSE_WAIT 表示被动关闭方已收到 FIN，等待应用层关闭；LAST_ACK 表示被动关闭方已发送 FIN 等待最后的 ACK；CLOSED 表示连接已完全关闭。

在 Java 中，可以通过 `netstat` 或 `lsof` 等工具查看 socket 的状态。Socket 类本身只提供 `isConnected()`、`isBound()`、`isClosed()` 等有限的状态查询。

### 9.4 如何实现 socket 的超时？

有几种方式设置 socket 超时。连接超时通过 `connect(SocketAddress endpoint, int timeout)` 方法设置，timeout 单位为毫秒，0 表示无限等待。读超时通过 `setSoTimeout(int timeout)` 设置，当从输入流读取数据超过指定时间没有数据到达时，会抛出 `SocketTimeoutException`。写超时同样通过 `setSoTimeout()` 影响写操作，因为底层 TCP 的超时行为是类似的。

示例代码：

```java
Socket socket = new Socket();
socket.connect(new InetSocketAddress("example.com", 80), 5000);  // 5秒连接超时
socket.setSoTimeout(10000);  // 10秒读超时
```

### 9.5 Socket 编程中常见的异常有哪些？

`UnknownHostException` 当无法解析主机名时抛出，通常是 DNS 配置问题或主机名错误。`ConnectionRefusedException` 当服务器拒绝连接时抛出，可能是服务器未运行、端口错误或防火墙阻止。`ConnectException` 连接超时时抛出，通常是服务器不可达或网络问题。`SocketTimeoutException` 读操作超时时抛出。`SocketException` 底层的 socket 操作发生错误时抛出，是上述异常的父类。`NoRouteToHostException` 没有到主机的路由时抛出，通常是网络配置问题。

### 9.6 如何优化 socket 性能？

优化 socket 性能可以从多个方面入手。首先是缓冲区调整：使用 `setReceiveBufferSize()` 和 `setSendBufferSize()` 设置合适的缓冲区大小，通常需要根据网络带宽和延迟进行调整。其次是禁用 Nagle 算法：通过 `setTcpNoDelay(true)` 禁用 Nagle 算法，适合对延迟敏感的应用。第三是使用 NIO：对于大量并发连接，使用 Java NIO（Selector、Channel、Buffer）比传统的阻塞 I/O 更高效。第四是连接复用：通过设置 `setReuseAddress(true)` 允许快速重用处于 TIME_WAIT 状态的端口。第五是心跳机制：设置 `setKeepAlive(true)` 让 TCP 自动检测死连接，避免资源泄漏。

## 十、与其他类的对比

### 10.1 Socket 与 DatagramSocket

`DatagramSocket` 用于 UDP 通信，而 `Socket` 用于 TCP 通信。UDP 是无连接、不可靠的协议，不保证数据包的到达顺序或到达性；TCP 是面向连接、可靠的协议，保证数据的完整性和顺序。

使用场景方面，Socket 适合需要可靠传输的应用，如 HTTP、FTP、邮件等。DatagramSocket 适合对实时性要求高但可以容忍少量丢球的场景，如视频流、DNS 查询、实时游戏等。

API 差异方面，Socket 使用 `InputStream`/`OutputStream` 进行流式数据传输。DatagramSocket 使用 `DatagramPacket` 进行数据包传输。

### 10.2 Socket 与 NIO SocketChannel

传统的 `Socket` 是阻塞式的，一个线程一次只能处理一个连接。`SocketChannel` 是 NIO 的一部分，支持非阻塞 I/O 和多路复用。

阻塞模式下，`Socket` 更简单易用，适合连接数较少的场景。非阻塞模式下，`SocketChannel` 配合 `Selector` 可以高效处理大量并发连接。

Java 7 引入了 NIO.2（异步 I/O），`AsynchronousSocketChannel` 提供了真正的异步 I/O，进一步提高了高并发场景下的性能。

### 10.3 Socket 与 URLConnection

`URLConnection` 是更高级别的抽象，用于访问 URL 指向的资源。它在内部使用 `Socket`，但提供了更方便的 API，特别是对于 HTTP 协议。

对于简单的 HTTP 客户端，使用 `HttpURLConnection` 比直接使用 `Socket` 更简单，HTTP 协议细节（如请求头、响应头、cookie）都被自动处理。

对于自定义协议或需要精细控制连接的场景，直接使用 `Socket` 更合适。`Socket` 提供了对底层 TCP 连接的完全控制，包括自定义协议实现、精确的超时控制等。

## 十一、Socket 生命周期详解

### 11.1 状态转换图

Socket 对象经历以下状态转换：

初始状态（NEW）：对象已创建，但底层 socket 尚未创建。此时 `created=false`, `bound=false`, `connected=false`, `closed=false`。

创建状态（CREATED）：调用 `createImpl()` 后，`created=true`。此时操作系统级别的 socket 句柄已创建。

绑定状态（BOUND）：调用 `bind()` 后，`bound=true`。Socket 已绑定到本地地址和端口。

连接状态（CONNECTED）：调用 `connect()` 后，`connected=true`, `bound=true`。Socket 已建立到远程地址的连接，可以进行数据传输。

关闭状态（CLOSED）：调用 `close()` 后，`closed=true`。Socket 已关闭，不能再使用。

### 11.2 状态检查方法

每个状态都有对应的检查方法：`isCreated()` 检查是否已创建，`isBound()` 检查是否已绑定，`isConnected()` 检查是否已连接，`isClosed()` 检查是否已关闭。

这些方法组合使用可以精确判断 Socket 的当前状态：

```java
if (!socket.isClosed()) {
    if (socket.isConnected()) {
        if (socket.isBound()) {
            // socket 已完全就绪，可以进行通信
        }
    }
}
```

### 11.3 状态转换规则

Socket 的状态转换不是任意的，必须遵循一定的规则：只有在 `created=true` 后才能绑定（bind）；只有在 `created=true` 后才能连接（connect）；只有在 `connected=true` 后才能进行数据传输；一旦 `closed=true`，所有其他操作都会抛出异常。

这些规则在各个方法中都有体现，通过状态检查来强制执行。违反状态规则的调用会抛出 `SocketException`，并带有描述性的错误消息，如"Socket is closed"或"Already bound"。

