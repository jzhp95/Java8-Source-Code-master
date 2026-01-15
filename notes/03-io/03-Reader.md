# Reader 源码深度解析

## 一、类概述

### 1.1 基本信息

Reader 是 Java I/O 流体系中的抽象超类，位于 java.io 包中，用于表示所有字符输入流。与 InputStream 处理字节数据不同，Reader 专门处理字符数据，内部自动处理字符编码问题，将字节解码为字符。Reader 是 Java 1.1 引入的重要类，它的出现解决了早期 Java I/O 处理文本数据时的编码问题。

Reader 的设计同样采用了模板方法模式。抽象类定义了一组核心方法的骨架，而将具体的字符读取逻辑留给子类实现。与 InputStream 类似，Reader 只声明了一个抽象方法 read(char[], int, int)，其他方法如 read()、read(char[])、skip、ready 等都提供了默认实现。这种设计使得框架与实现分离，开发者可以方便地扩展新的字符输入流类型。

```java
public abstract class Reader implements Readable, Closeable
```

从类的声明可以看出，Reader 实现了两个重要的接口：Readable 和 Closeable。Readable 接口是 Java 5 引入的，它定义了可以从字符流中读取字符到 CharBuffer 的能力。Closeable 接口确保了所有字符流都可以被正确关闭，释放底层资源。Reader 的 close() 方法是抽象方法，要求子类必须实现资源释放逻辑。

Reader 的直接子类包括 BufferedReader（缓冲字符输入流）、CharArrayReader（字符数组输入流）、FilterReader（过滤字符输入流）、InputStreamReader（字节流到字符流的转换桥）、PipedReader（管道字符输入流）、StringReader（字符串输入流）等。Reader 体系的设计使得处理文本数据变得简单直观，开发者无需关心字符编码的底层细节。

Reader 相比 InputStream 的最大优势在于它原生支持字符数据。InputStream 处理的是原始字节（0-255），不适合直接处理文本，因为文本涉及字符编码问题。Reader 在内部使用指定的字符集将字节解码为字符，提供了正确的文本处理能力。这使得 Reader 成为处理文本数据的首选。

### 1.2 在 I/O 体系中的位置

Reader 在 Java I/O 体系中与 InputStream 处于对称的位置，两者共同构成了完整的输入流体系。InputStream 处理字节数据，Reader 处理字符数据。两者都采用了抽象类加模板方法模式的设计，都支持装饰器模式进行功能扩展，都实现了 Closeable 接口进行资源管理。

Reader 与 InputStream 的关系通过 InputStreamReader 和 OutputStreamWriter 这两个桥梁类来连接。InputStreamReader 包装一个 InputStream，使用指定的字符集将字节解码为字符。OutputStreamWriter 包装一个 OutputStream，将字符编码为字节后写入。这两个类完成了字节流与字符流之间的转换，是理解 Java I/O 流体系的关键。

从装饰器模式的角度来看，Reader 同样支持功能的动态组合。BufferedReader 是最常用的装饰器之一，它在底层 Reader 的基础上添加了缓冲功能和便捷的 readLine() 方法。LineNumberReader 跟踪行号的 Reader。PushbackReader 允许将字符推回流中。FilterReader 是装饰器的基类，可以实现各种自定义过滤功能。

Reader 体系的另一个重要设计是 Readable 接口。这个接口定义了 read(CharBuffer) 方法，允许从字符流直接读取到 NIO 的 CharBuffer 中。这为 Reader 与 NIO 的集成提供了支持。虽然大多数 Reader 使用 read(char[], int, int) 作为核心方法，但 Readable 接口使得 API 更加灵活。

### 1.3 Readable 接口的作用

Readable 接口是 Java 5 引入的一个简单接口，它为字符流提供了一种与 NIO Buffer 交互的方式。

```java
public interface Readable {
    int read(java.nio.CharBuffer cb) throws IOException;
}
```

Readable 接口只有 read(CharBuffer) 一个方法。这个方法尝试将字符读入指定的 CharBuffer 中，返回实际读取的字符数，或者到达流末尾时返回 -1。这个接口的设计使得字符流可以与 NIO 的 Buffer 系统无缝集成。

Reader 实现了 Readable 接口，其 read(CharBuffer) 方法的实现非常直接：创建一个临时字符数组，调用 read(char[], int, int) 方法读取数据，然后将数据放入 CharBuffer 中。这种实现虽然效率不是最优的（因为涉及临时数组的分配和复制），但它提供了一致的 API。

```java
public int read(java.nio.CharBuffer target) throws IOException {
    int len = target.remaining();
    char[] cbuf = new char[len];
    int n = read(cbuf, 0, len);
    if (n > 0)
        target.put(cbuf, 0, n);
    return n;
}
```

Readable 接口的主要用途是使得一些接受 Readable 参数的 API（如 Scanner、Pattern 的 matcher 等）可以直接使用 Reader 或其子类。这提高了 API 的灵活性和一致性。

## 二、核心设计思想

### 2.1 模板方法模式的应用

Reader 是模板方法模式的典型应用案例。与 InputStream 类似，Reader 只声明了一个抽象方法 read(char[], int, int)，要求子类提供具体的字符读取实现。其他方法如 read()、read(char[])、skip 等都基于 read(char[], int, int) 提供了默认实现。

```java
abstract public int read(char cbuf[], int off, int len) throws IOException;

public int read(char cbuf[]) throws IOException {
    return read(cbuf, 0, cbuf.length);
}

public int read() throws IOException {
    char cb[] = new char[1];
    if (read(cb, 0, 1) == -1)
        return -1;
    else
        return cb[0];
}
```

read() 方法的默认实现展示了模板方法模式的工作方式。它创建一个单字符数组，然后调用 read(char[], int, int) 方法读取。这种实现虽然效率不高（每次读取一个字符都需要分配临时数组和调用方法），但它提供了正确的行为框架。子类可以通过重写 read() 方法来提供更高效的实现。

这种设计的好处在于：任何新的 Reader 子类只需要实现 read(char[], int, int) 方法，就可以获得完整的读取功能支持。同时，子类也可以选择重写其他方法（如 read()、read(CharBuffer)）来提供更高效的实现。例如，BufferedReader 重写了 read() 方法，直接从缓冲区中返回字符，避免了方法调用的开销。

### 2.2 线程安全的同步机制

Reader 的一个重要设计特征是它提供了显式的同步机制。与 InputStream 使用 synchronized 方法不同，Reader 使用 lock 对象进行同步，这提供了更大的灵活性。

```java
protected Object lock;

protected Reader() {
    this.lock = this;
}

protected Reader(Object lock) {
    if (lock == null) {
        throw new NullPointerException();
    }
    this.lock = lock;
}
```

lock 字段用于同步对 Reader 的操作。默认情况下，lock 指向 Reader 本身，这意味着使用 this 对象作为同步监视器。构造函数允许传入自定义的 lock 对象，这在需要跨多个流进行同步时非常有用。

```java
public long skip(long n) throws IOException {
    if (n < 0L)
        throw new IllegalArgumentException("skip value is negative");
    int nn = (int) Math.min(n, maxSkipBufferSize);
    synchronized (lock) {
        if ((skipBuffer == null) || (skipBuffer.length < nn))
            skipBuffer = new char[nn];
        long r = n;
        while (r > 0) {
            int nc = read(skipBuffer, 0, (int)Math.min(r, nn));
            if (nc == -1)
                break;
            r -= nc;
        }
        return n - r;
    }
}
```

skip() 方法的实现展示了同步机制的使用。它在 synchronized 块中使用 lock 对象来保护对共享状态（skipBuffer）的访问。这种设计确保了多线程环境下的线程安全，同时允许使用自定义的锁对象来进行更细粒度的控制。

### 2.3 字符编码处理的设计

Reader 的核心职责之一是处理字符编码。与 InputStream 处理原始字节不同，Reader 在内部将字节解码为字符。这个过程涉及字符集（Charset）和编码器（CharsetEncoder）的使用。

InputStreamReader 是 Reader 的重要子类，它充当字节流与字符流之间的桥梁。InputStreamReader 使用指定的字符集将底层 InputStream 中的字节解码为字符。创建 InputStreamReader 时可以指定字符集，如果不指定则使用平台默认字符集。

```java
// 使用指定字符集
InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

// 使用系统默认字符集（不推荐）
InputStreamReader reader = new InputStreamReader(inputStream);
```

字符编码的正确处理对于文本数据的完整性至关重要。不同的字符编码使用不同的字节序列来表示字符。例如，UTF-8 使用 1 到 4 个字节表示一个字符，而 GBK 使用 2 个字节表示一个中文字符。如果编码和解码使用的字符集不一致，会产生乱码。

Reader 的设计使得字符编码的处理变得透明。开发者只需要在创建 InputStreamReader 时指定正确的字符集，之后的读取操作就可以像处理普通字符流一样处理文本数据。这种设计简化了文本处理的复杂性，提高了代码的可移植性。

## 三、继承结构与接口实现

### 3.1 类继承层次

Reader 的继承层次与 InputStream 对称，但增加了一些字符流特有的功能。抽象类 Reader 定义了字符输入流的基本契约，子类根据不同的数据源实现具体的读取逻辑。

BufferedReader 是最常用的 Reader 子类之一。它在底层 Reader 的基础上添加了缓冲功能和一个非常便捷的 readLine() 方法。缓冲功能减少了底层读取操作的次数，提高了性能。readLine() 方法每次读取一行文本，返回的字符串不包含行尾分隔符。

```java
public class BufferedReader extends Reader {
    private char[] cb;
    private int nChars, nextChar;
    
    public BufferedReader(Reader in, int sz) {
        super(in);
        this cb = new char[sz];
        // 初始化...
    }
    
    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize); // 默认 8192
    }
    
    public String readLine() throws IOException {
        // 实现...
    }
}
```

CharArrayReader 从内部维护的字符数组中提供数据。这使得可以方便地将 char[] 转换为 Reader，用于需要 Reader 接口的 API。CharArrayReader 的 read() 方法直接从内部数组中读取，不需要任何 I/O 操作，因此永远不会阻塞或抛出 IOException（除非手动关闭）。

StringReader 是另一个重要的子类，它从字符串中提供数据。这使得 String 可以像流一样被读取，适用于需要处理字符串内容的场景。StringReader 的实现直接操作字符串的内部字符数组，读取操作永远不会阻塞或抛出 IO 异常。

### 3.2 Readable 接口详解

Readable 接口为字符流提供了一种与 NIO Buffer 交互的方式。这个接口虽然简单，但它对于集成 Reader 和 NIO 系统非常重要。

```java
public interface Readable {
    int read(java.nio.CharBuffer cb) throws IOException;
}
```

Readable 接口的定义非常简洁。read(CharBuffer) 方法尝试将字符读入到 CharBuffer 中，返回实际读取的字符数。如果到达流末尾，返回 -1。这个方法在 CharBuffer 还有剩余空间时会尝试读取数据，直到缓冲区满或流结束。

Reader 对 Readable 接口的实现是标准化的：首先计算 CharBuffer 的剩余空间，创建一个临时字符数组来接收数据，然后调用核心的 read(char[], int, int) 方法，最后将读取的字符放入 CharBuffer 中。

Readable 接口的主要应用场景是与其他接受 Readable 参数的 API 集成。例如，Scanner 的构造函数可以接受 Readable，这使得 Scanner 可以从任何 Reader（包括 InputStreamReader、BufferedReader 等）读取数据。Pattern 的 matcher() 方法也接受 Readable 作为输入源。

### 3.3 Closeable 接口详解

Reader 实现了 Closeable 接口，这是资源管理的基础。Closeable 接口定义了一个 void close() throws IOException 方法，要求实现者释放所有持有的资源。

```java
abstract public void close() throws IOException;
```

与 InputStream 的空实现不同，Reader 的 close() 方法是抽象方法，要求子类必须实现资源释放逻辑。这种设计反映了字符流通常与某种外部资源（如文件、网络连接）关联的事实，需要显式的资源释放。

正确实现 close() 方法需要注意几个关键点。首先，应该只关闭资源一次，多次关闭应该被安全地处理（幂等性）。其次，close() 方法应该释放所有相关资源，包括底层的字符缓冲区、底层的字节流等。第三，在关闭过程中发生的异常应该被适当处理。

对于包装了其他 Reader 的装饰器（如 BufferedReader），close() 方法应该首先关闭底层的 Reader。这确保了资源被正确释放，底层的资源链被正确断开。

```java
// BufferedReader 的 close() 实现示例
public void close() throws IOException {
    synchronized (lock) {
        if (in == null)
            return;
        try {
            in.close();
        } finally {
            in = null;
            cb = null;
        }
    }
}
```

## 四、核心方法深度剖析

### 4.1 read() 方法族

read() 方法是 Reader 的核心，有多个重载版本。与 InputStream 类似，这些方法形成了从低层到高层的抽象层次。

read(char[], int, int) 是核心抽象方法，子类必须实现。它从流中读取最多 len 个字符到字符数组的指定位置，返回实际读取的字符数。这个方法是所有其他 read() 方法的基础。

```java
abstract public int read(char cbuf[], int off, int len) throws IOException;
```

read(char[]) 方法将数据读取到整个字符数组中，内部调用 read(cbuf, 0, cbuf.length)。这提供了使用上的便利性。

```java
public int read(char cbuf[]) throws IOException {
    return read(cbuf, 0, cbuf.length);
}
```

read() 方法读取单个字符，返回值是字符的 Unicode 值（0-65535），或者到达末尾时返回 -1。注意返回类型是 int 而不是 char，这是为了能够返回 -1 作为特殊的结束标记。

```java
public int read() throws IOException {
    char cb[] = new char[1];
    if (read(cb, 0, 1) == -1)
        return -1;
    else
        return cb[0];
}
```

这个方法的默认实现效率不高，因为它需要分配临时数组。但它提供了正确的行为框架，子类可以通过重写这个方法来提供更高效的实现。例如，BufferedReader 重写了 read() 方法，直接从缓冲区中返回字符，避免了临时数组的分配。

### 4.2 skip() 方法详解

skip() 方法用于跳过流中的 n 个字符，返回实际跳过的字符数。这个方法与 InputStream 的 skip() 类似，但处理的是字符而不是字节。

```java
public long skip(long n) throws IOException {
    if (n < 0L)
        throw new IllegalArgumentException("skip value is negative");
    int nn = (int) Math.min(n, maxSkipBufferSize);
    synchronized (lock) {
        if ((skipBuffer == null) || (skipBuffer.length < nn))
            skipBuffer = new char[nn];
        long r = n;
        while (r > 0) {
            int nc = read(skipBuffer, 0, (int)Math.min(r, nn));
            if (nc == -1)
                break;
            r -= nc;
        }
        return n - r;
    }
}
```

Reader 中 skip() 的默认实现使用了"读取并丢弃"的策略。它分配一个缓冲区（最大 8192 字符），循环读取数据但不保存，直到跳过了足够的字符或到达流末尾。这种实现效率不高，但适用于任何 Reader 子类。

skip() 方法在同步块中执行，使用 lock 对象进行同步。这确保了多线程环境下的线程安全。skipBuffer 被延迟分配，只有在第一次跳过操作时才创建，这避免了不必要的内存分配。

子类可以提供更高效的跳过实现。例如，StringReader 可能直接操作内部字符串的索引来跳过字符。FileReader 可能使用底层的字符编码知识来精确跳过字节。

### 4.3 ready() 方法详解

ready() 方法表示流是否准备好被读取，即下一个 read() 调用是否会阻塞。

```java
public boolean ready() throws IOException {
    return false;
}
```

默认实现返回 false，表示流可能阻塞。这是一种保守的估计，确保调用者不会假设流已经准备好。子类根据其数据源的特性提供更准确的返回值。

FileReader 的 ready() 可能返回文件是否还有更多数据。StringReader 的 ready() 总是返回 true，因为字符串数据总是立即可用。BufferedReader 的 ready() 返回内部缓冲区是否还有数据。

调用者应该将 ready() 的返回值视为一个提示而非保证。返回 true 并不保证下一个 read() 一定不会阻塞（可能在读取过程中其他线程消费了数据），返回 false 也不保证下一个 read() 一定会阻塞（可能有数据恰好到达）。

### 4.4 mark() 与 reset() 方法详解

mark() 和 reset() 方法提供了流的回溯能力。与 InputStream 类似，默认实现不支持这个功能，子类需要显式实现。

```java
public boolean markSupported() {
    return false;
}

public void mark(int readAheadLimit) throws IOException {
    throw new IOException("mark() not supported");
}

public void reset() throws IOException {
    throw new IOException("reset() not supported");
}
```

默认实现返回 false 表示不支持 mark/reset，mark() 和 reset() 都抛出 IOException。子类如果想要支持这个功能，必须重写所有这三个方法。

BufferedReader 支持 mark/reset 功能，内部使用缓冲区来存储从标记点开始读取的数据。markSupported() 返回 true，mark() 记录当前位置和缓冲区使用状态，reset() 从缓冲区中重新提供数据。

### 4.5 close() 方法详解

close() 方法关闭流并释放相关资源。Reader 的 close() 是抽象方法，要求子类必须实现。

```java
abstract public void close() throws IOException;
```

对于包装了底层 Reader 的装饰器（如 BufferedReader），close() 应该关闭底层的 Reader。这确保了资源被正确释放，底层的资源链被正确断开。

```java
public void close() throws IOException {
    synchronized (lock) {
        if (in == null)
            return;
        try {
            in.close();
        } finally {
            in = null;
            cb = null;
        }
    }
}
```

正确关闭 Reader 是避免资源泄漏的关键。对于文件相关的 Reader（如 FileReader、BufferedReader 包装的 FileReader），不关闭会导致文件句柄泄漏。对于网络相关的 Reader，不关闭会导致连接泄漏。

## 五、常见问题与面试题

### 5.1 Reader 基础问题

**Reader 和 InputStream 有什么区别？**

Reader 和 InputStream 是 Java I/O 流体系中的两个分支。InputStream 处理字节数据（0-255），适用于任何二进制数据。Reader 处理字符数据，内部自动将字节解码为字符，适用于文本数据。Reader 解决了字符编码问题，使用指定的字符集将字节转换为正确的字符。从使用场景看，处理文本应该使用 Reader，处理二进制数据应该使用 InputStream。两者可以通过 InputStreamReader 和 OutputStreamWriter 进行转换。

**Reader 的 read() 方法为什么返回 int 而不是 char？**

read() 返回 int 而不是 char 是因为 char 的范围是 0-65535，而流结束需要特殊标记。使用 int 可以返回 -1 来表示流结束，同时可以返回所有有效的 Unicode 字符值。这种设计简化了 API，使得可以区分"没有更多数据"和"读取到特殊字符"两种情况。

**Reader 的 lock 对象有什么作用？**

lock 对象用于同步多线程对 Reader 的访问。默认情况下，lock 指向 Reader 本身，这意味着使用 this 对象作为同步监视器。构造函数允许传入自定义的 lock 对象，这在需要跨多个流进行同步时非常有用。同步机制确保了多线程环境下的线程安全。

### 5.2 字符编码相关问题

**如何正确处理字符编码？**

处理字符编码时应该始终明确指定字符集，不要依赖平台默认编码。创建 InputStreamReader 时应该指定字符集，使用 StandardCharsets 常量可以避免编码名称错误。例如：`new InputStreamReader(new FileInputStream("file.txt"), StandardCharsets.UTF_8)`。在读取文本数据时，确保写入和读取使用相同的字符集。

**InputStreamReader 和 FileReader 有什么区别？**

InputStreamReader 是字节流与字符流之间的桥梁，使用指定的字符集将字节解码为字符。FileReader 是 InputStreamReader 的便捷子类，它使用平台默认字符集创建文件读取器。FileReader 内部包装了 FileInputStream 并创建了 InputStreamReader。由于 FileReader 使用默认字符集，可能在不同平台产生不同的结果，推荐使用 InputStreamReader 并显式指定字符集。

**为什么不应该使用 FileReader？**

FileReader 使用平台默认字符集，这可能导致跨平台兼容性问题。例如，在 Windows 上默认字符集可能是 GBK，在 Linux 上可能是 UTF-8。使用 FileReader 读取在不同系统上创建的文件可能导致乱码。推荐使用 `new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)` 来代替 FileReader。

### 5.3 性能与使用问题

**BufferedReader 有什么优势？**

BufferedReader 有两个主要优势：第一，缓冲功能减少了底层读取操作的次数，提高了性能。默认缓冲区大小是 8192 字符，对于大多数场景是合适的。第二，readLine() 方法提供了便捷的按行读取能力，可以一次性读取一行文本，返回的字符串不包含行尾分隔符。这使得处理文本文件变得非常简单。

**Reader 的 readLine() 方法有什么特点？**

readLine() 是 BufferedReader 的方法，不是 Reader 接口的方法。readLine() 读取一行文本，以换行符（\n）或回车换行符（\r\n）为行分隔符。返回的字符串不包含行分隔符。如果到达流末尾且没有更多行，返回 null。readLine() 不保留行尾分隔符，如果需要保留，需要自行处理。

**如何高效地读取大文件？**

高效读取大文件的推荐方式是使用 BufferedReader 包装 InputStreamReader：

```java
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream("largefile.txt"), StandardCharsets.UTF_8))) {
    String line;
    while ((line = reader.readLine()) != null) {
        // 处理每一行
    }
}
```

这种方式结合了缓冲的性能优势和字符编码的正确处理。对于超大文件，这种方式可以逐行处理而不会将整个文件加载到内存中。

### 5.4 资源管理问题

**Reader 使用后需要关闭吗？**

是的，Reader 使用后需要关闭以释放底层资源。对于文件相关的 Reader，不关闭会导致文件句柄泄漏。对于包装了底层 Reader 的装饰器，关闭最外层的 Reader 应该会自动关闭底层的 Reader。推荐使用 try-with-resources 语句来自动管理 Reader 的关闭。

**关闭 Reader 时会发生什么？**

关闭 Reader 时会发生几个操作。首先，任何缓冲区中剩余的数据会被丢弃（与 OutputStream 不同，Reader 的 close() 不调用 flush()）。其次，底层的资源会被释放，如文件描述符、网络连接等。第三，流被标记为关闭状态，后续的读取操作会抛出 IOException。

**try-with-resources 如何处理 Reader？**

try-with-resources 是管理 Reader 生命周期的推荐方式。它确保资源在退出 try 块时自动关闭，即使发生异常也是如此。对于包装了其他 Reader 的装饰器（如 BufferedReader），关闭最外层的 Reader 应该会自动关闭底层的 Reader。

```java
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream("file.txt")))) {
    String line = reader.readLine();
    // 处理数据
} catch (IOException e) {
    // 处理异常
}
```

## 六、应用场景与最佳实践

### 6.1 典型应用场景

Reader 在处理文本数据的场景中广泛使用。在文件处理场景中，BufferedReader 包装 InputStreamReader 是读取文本文件的标准方式。readLine() 方法使得按行读取文本文件变得非常简单。

```java
// 读取文本文件的推荐方式
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream("logfile.txt"), StandardCharsets.UTF_8))) {
    String line;
    while ((line = reader.readLine()) != null) {
        // 处理每一行日志
    }
} catch (IOException e) {
    // 处理异常
}
```

在网络编程中，从网络连接读取文本数据时使用 InputStreamReader 包装 Socket 的输入流。字符编码需要与协议约定一致，常见的是 UTF-8。

在解析场景中，Reader 用于读取配置文件、脚本文件等。配合 Scanner 或自定义解析器，可以实现复杂的文本解析功能。StringReader 用于从字符串内容创建 Reader，适用于测试或处理内存中的文本数据。

### 6.2 最佳实践

**始终使用 try-with-resources**：这是管理 Reader 资源的推荐方式。它确保资源被正确关闭，避免资源泄漏。关闭操作会自动传播到被包装的底层 Reader。

**使用 BufferedReader 包装底层 Reader**：这可以显著提高读取效率，特别是对于大量小读取操作。默认的缓冲区大小（8192 字符）对于大多数场景是合适的。BufferedReader 还提供了便捷的 readLine() 方法。

**始终明确指定字符编码**：使用 InputStreamReader 时始终指定字符集，使用 StandardCharsets 常量可以避免编码名称错误。不要依赖平台默认编码，这可能导致跨平台兼容性问题。

```java
// 推荐：明确指定字符编码
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream("config.txt"), StandardCharsets.UTF_8))) {
    // 使用 reader
}

// 不推荐：依赖平台默认编码
try (BufferedReader reader = new BufferedReader(new FileReader("config.txt"))) {
    // 不推荐：FileReader 使用默认编码
}
```

**按行读取时处理空行**：readLine() 返回 null 表示到达流末尾，但对于空行会返回空字符串（""）。在处理配置文件或数据文件时，需要注意区分空行和文件结束。

**考虑使用 Files.newBufferedReader()**：Java 7+ 提供了 Files.newBufferedReader() 方法，它是创建 BufferedReader 的便捷方式，自动处理了字符编码和缓冲区创建。

```java
try (BufferedReader reader = Files.newBufferedReader(Paths.get("file.txt"), StandardCharsets.UTF_8)) {
    // 使用 reader
}
```

### 6.3 常见错误与避免方法

**错误一：不关闭 Reader 导致资源泄漏**。这会导致文件句柄泄漏，长期运行可能导致系统资源耗尽。避免方法是始终使用 try-with-resources。

**错误二：使用 FileReader 导致编码问题**。FileReader 使用平台默认字符集，可能在不同平台产生不同的结果。避免方法是使用 InputStreamReader 并显式指定字符集。

**错误三：混淆字节和字符**。Reader 处理字符，不是字节。在处理二进制数据（如图片、音频）时应该使用 InputStream，而不是 Reader。错误使用 Reader 处理二进制数据会导致数据损坏。

**错误四：不处理 readLine() 的返回值为 null**。readLine() 返回 null 表示到达流末尾。在循环中使用 while ((line = reader.readLine()) != null) 来正确处理文件结束。

**错误五：假设 read() 返回 char**。read() 返回 int，范围是 0-65535 或 -1（表示结束）。需要将返回值转换为 char 来获取字符：`char c = (char) reader.read()`。

## 七、与其他类的对比

### 7.1 Reader 与 InputStream 对比

Reader 和 InputStream 是 Java I/O 流体系中的两个对称分支。InputStream 处理字节数据，Reader 处理字符数据。两者都采用了抽象类加模板方法模式的设计，都支持装饰器模式进行功能扩展，都实现了 Closeable 接口。

从功能角度看，InputStream 处理原始字节，适用于任何二进制数据。Reader 处理字符数据，自动处理字符编码，将字节解码为字符。从使用场景看，处理文本应该使用 Reader，处理二进制数据应该使用 InputStream。两者通过 InputStreamReader 和 OutputStreamWriter 进行转换。

从性能角度看，Reader 的 read() 操作通常比 InputStream 的 read() 操作更复杂，因为涉及字符编码的解码过程。使用 BufferedReader 可以显著提高性能，这同样适用于 BufferedInputStream。

从 API 设计角度看，Reader 提供了与 InputStream 类似的方法（read、skip、available、close 等），同时增加了 Readable 接口支持以与 NIO 集成。Reader 的同步机制使用 lock 对象，比 InputStream 的 synchronized 方法更灵活。

### 7.2 不同 Reader 子类的选择

Java 提供了多种 Reader 子类，适用于不同的数据源和使用场景。选择正确的子类对于代码的效率和正确性至关重要。

BufferedReader 是最常用的 Reader，用于提高读取效率和提供 readLine() 方法。它包装另一个 Reader，添加缓冲功能和行读取能力。适用于几乎所有需要读取文本的场景。

InputStreamReader 是字节流与字符流之间的桥梁。它包装 InputStream，使用指定的字符集将字节解码为字符。是处理来自文件、网络等字节源的文本数据的必经之路。

CharArrayReader 从内部维护的字符数组中提供数据。适用于将 char[] 转换为 Reader 或测试场景。不需要关闭，不会抛出 IO 异常。

StringReader 从字符串中提供数据。适用于处理字符串内容的场景，如解析器输入、测试数据等。不需要关闭，不会抛出 IO 异常。

### 7.3 Reader 与 Scanner 的对比

Scanner 和 BufferedReader 都是用于读取文本数据的工具，但有不同的特点和适用场景。

Scanner 提供了丰富的解析能力，可以直接解析基本类型和字符串，使用正则表达式分隔符。适合需要解析结构化文本的场景。但 Scanner 的性能通常比 BufferedReader 差，因为它需要更多的解析工作。

BufferedReader 专注于高效的字符读取和行读取。它的 readLine() 方法简洁高效，但不支持直接解析基本类型。适合需要逐行处理文本的场景，如日志分析、CSV 处理等。

选择使用哪个取决于具体需求。如果需要解析数字、跳过分隔符、使用正则表达式，Scanner 更方便。如果只需要高效地按行读取文本，BufferedReader 更高效。

## 八、总结

Reader 是 Java I/O 流体系中的核心抽象类，定义了字符输入流的基本契约。它采用模板方法模式，只要求子类实现 read(char[], int, int) 方法，其他方法基于该方法提供默认实现。这种设计既保证了接口的简洁性，又为子类提供了灵活扩展的能力。

Reader 的设计体现了几个重要的软件工程原则。模板方法模式将算法的骨架与具体实现分离。装饰器模式允许动态组合功能，BufferedReader 是最常用的装饰器。lock 对象的同步机制提供了比 synchronized 方法更灵活的线程安全保障。Readable 接口使得 Reader 可以与 NIO 系统集成。

Reader 与 InputStream 的关系是 Java I/O 流体系的核心概念之一。InputStream 处理字节，Reader 处理字符，两者通过 InputStreamReader 和 OutputStreamWriter 进行转换。正确理解两者的区别和联系对于正确使用 Java I/O 至关重要。

正确使用 Reader 需要注意几个关键点：使用 try-with-resources 确保资源正确关闭、使用 BufferedReader 提高读取效率、明确指定字符编码避免跨平台问题、按行读取时正确处理 null 返回值。

在现代 Java 开发中，Reader 仍然是处理文本数据的首选工具。虽然 NIO 提供了更强大的功能，但对于大多数场景，Reader 的简单性和直观性使其成为首选。理解 Reader 的设计原理和使用方法，是掌握 Java I/O 编程的基础。
