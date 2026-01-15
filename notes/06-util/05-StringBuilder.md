# StringBuilder.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.lang.StringBuilder` 是 Java 1.5 引入的可变字符序列类，它是 `AbstractStringBuilder` 的直接子类，实现了 `Serializable` 和 `CharSequence` 接口。StringBuilder 的设计目的是提供一个高效、可变的字符串构建工具，作为 `StringBuffer` 的非同步替代品，在单线程环境下提供更好的性能。

StringBuilder 是 Java 字符串处理体系中的核心组件，它弥补了 `String` 类不可变性的不足。传统的 String 对象一旦创建就无法修改，任何看似修改 String 的操作实际上都是创建了新的 String 对象，这在大量字符串操作时会造成严重的性能开销和内存浪费。StringBuilder 通过内部维护可动态扩展的字符数组，实现了真正的原地修改能力。

StringBuilder 的 API 设计完全兼容 StringBuffer，但移除了 StringBuffer 中的同步机制。由于移除了同步开销，StringBuilder 在单线程环境下的性能比 StringBuffer 高出约 30% 至 50%，这使得 StringBuilder 成为 Java 8 时代进行字符串拼接操作的首选工具。

### 1.2 核心设计原则

StringBuilder 的设计体现了几个重要的软件工程原则。首先是**可变性与性能的统一**，通过维护一个可动态扩展的字符数组缓冲区，StringBuilder 避免了字符串拼接时频繁创建中间 String 对象的问题。所有 append 和 insert 操作都在内部缓冲区上执行，只有在需要生成最终结果时才会创建 String 对象。

其次是**接口一致性**，StringBuilder 实现了 `CharSequence` 接口，这使得它可以与任何接受 CharSequence 参数的 API 互操作，包括正则表达式匹配、字符序列处理等。同时，StringBuilder 的大部分方法都返回 `this`，支持流畅的链式调用风格。

第三是**渐进式容量管理**，StringBuilder 采用"按需扩展"的容量策略。初始容量为 16 个字符（或根据初始化内容自动计算），当内容长度超过容量时，自动进行扩容。这种策略避免了预先分配过大内存造成的浪费，同时在扩容时采用指数增长策略（通常是翻倍），确保摊销时间复杂度为 O(1)。

第四是**线程安全性权衡**，StringBuilder 明确定位于单线程使用场景，因此不提供任何同步措施。这种设计选择使得 StringBuilder 比 StringBuffer 更快，但也要求开发者明确知道何时应该使用 StringBuilder（单线程）而非 StringBuffer（多线程）。

### 1.3 与相关类的关系

StringBuilder 在 Java 字符串体系中与多个类存在密切的协作和继承关系。`AbstractStringBuilder` 是 StringBuilder 的父类，包含了字符串缓冲区的核心实现逻辑，所有实际的字符操作都在这个类中完成。`StringBuffer` 是 StringBuilder 的"兄弟类"，两者 API 完全兼容，但 StringBuffer 提供了同步机制以支持多线程环境。`String` 是最终的不可变字符串表示，当需要将 StringBuilder 的内容转换为不可变字符串时调用 `toString()` 方法。`CharSequence` 是 StringBuilder 实现的接口之一，使得 StringBuilder 可以作为字符序列被各种 API 接受。

### 1.4 应用场景

StringBuilder 在以下场景中被广泛使用：大量字符串拼接操作，如循环中构建 SQL 语句或日志消息；需要频繁修改的字符串内容，如动态生成 JSON、XML；构建格式化的字符串输出；字符处理算法中需要可变的字符序列；以及任何不需要线程安全的字符串操作场景。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.lang.AbstractStringBuilder
        └─ java.lang.StringBuilder
```

StringBuilder 直接继承自 `AbstractStringBuilder`，没有复杂的继承层次。这种设计使得核心的缓冲区管理逻辑集中在父类中，StringBuilder 只负责提供更友好的 API 和正确的返回类型。

### 2.2 实现的接口

`Serializable` 接口使得 StringBuilder 可以被序列化存储或传输。StringBuilder 提供了 `serialVersionUID = 4383685877147921099L` 来确保序列化兼容性。需要注意的是，StringBuilder 的序列化行为与 String 不同：StringBuilder 序列化的是其内部字符数组的内容，而不是字符序列的字符串表示。

`CharSequence` 接口定义了字符序列的通用操作，包括 `length()`、`charAt()`、`subSequence()`、`toString()` 等方法。实现这个接口使得 StringBuilder 可以与正则表达式引擎、各种文本处理 API 无缝集成。

### 2.3 父类 AbstractStringBuilder 的核心职责

AbstractStringBuilder 是所有可变字符串实现的基础类，它包含以下核心字段和方法：

```java
abstract class AbstractStringBuilder {
    char[] value;  // 存储字符值的数组
    int count;     // 当前字符数量
    
    public AbstractStringBuilder(int capacity) {
        value = new char[capacity];
        count = 0;
    }
    
    // 核心的 append、insert、delete、reverse 等方法实现
}
```

value 数组是实际存储字符的容器，它的容量决定了在需要扩容之前可以存储的字符数。count 字段记录当前使用的字符数量，这比每次计算 value.length 更高效。

## 三、构造函数详解

### 3.1 默认构造函数

```java
public StringBuilder() {
    super(16);
}
```

默认构造函数创建一个空的 StringBuilder，初始容量为 16 个字符。这个设计基于经验观察：大多数字符串操作涉及的字符数不会太多，16 个字符的初始容量可以避免在常见场景下立即触发扩容。对于不确定最终长度的字符串构建场景，默认初始容量是一个合理的起点。

### 3.2 指定容量构造函数

```java
public StringBuilder(int capacity) {
    super(capacity);
}
```

当开发者能够预估最终字符串的大致长度时，应该使用这个构造函数指定初始容量。预先分配足够的容量可以避免后续的扩容操作，提高性能：

```java
// 已知最终需要约 100 个字符
StringBuilder sb = new StringBuilder(100);
```

需要注意的是，如果传入负数容量，会抛出 `NegativeArraySizeException`。

### 3.3 从 String 初始化

```java
public StringBuilder(String str) {
    super(str.length() + 16);
    append(str);
}
```

这个构造函数基于一个已存在的 String 创建 StringBuilder。初始容量设置为字符串长度加 16，为后续可能的追加操作预留空间。内部调用 `append` 方法将字符串内容复制到内部缓冲区。

### 3.4 从 CharSequence 初始化

```java
public StringBuilder(CharSequence seq) {
    this(seq.length() + 16);
    append(seq);
}
```

这个构造函数接受任何实现 CharSequence 接口的对象，包括 String、StringBuilder 自己、StringBuffer、CharBuffer 等。初始容量同样是长度加 16，然后通过 `append` 方法复制内容。

## 四、核心方法详解

### 4.1 追加操作

StringBuilder 提供了丰富的 `append` 方法重载，可以追加各种类型的数据：

```java
public StringBuilder append(Object obj) {
    return append(String.valueOf(obj));
}

public StringBuilder append(String str) {
    super.append(str);
    return this;
}

public StringBuilder append(StringBuffer sb) {
    super.append(sb);
    return this;
}

public StringBuilder append(boolean b) {
    super.append(b);
    return this;
}

public StringBuilder append(char c) {
    super.append(c);
    return this;
}

public StringBuilder append(int i) {
    super.append(i);
    return this;
}

public StringBuilder append(long lng) {
    super.append(lng);
    return this;
}

public StringBuilder append(float f) {
    super.append(f);
    return this;
}

public StringBuilder append(double d) {
    super.append(d);
    return this;
}

public StringBuilder appendCodePoint(int codePoint) {
    super.appendCodePoint(codePoint);
    return this;
}
```

所有 `append` 方法都返回 `this`，支持链式调用。这种设计使得代码更加简洁：

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello")
  .append(" ")
  .append("World")
  .append("!")
  .append(123);
```

append 方法的内部实现涉及容量检查和字符数组复制。当需要追加的字符串长度加上当前 count 超过 value 数组的容量时，会触发扩容操作。

### 4.2 插入操作

`insert` 方法系列提供了在指定位置插入字符序列的能力：

```java
public StringBuilder insert(int offset, String str) {
    super.insert(offset, str);
    return this;
}

public StringBuilder insert(int offset, Object obj) {
    super.insert(offset, obj);
    return this;
}

public StringBuilder insert(int offset, char[] str) {
    super.insert(offset, str);
    return this;
}

public StringBuilder insert(int dstOffset, CharSequence s) {
    super.insert(dstOffset, s);
    return this;
}

public StringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
    super.insert(dstOffset, s, start, end);
    return this;
}

public StringBuilder insert(int offset, boolean b) {
    super.insert(offset, b);
    return this;
}

public StringBuilder insert(int offset, char c) {
    super.insert(offset, c);
    return this;
}

public StringBuilder insert(int offset, int i) {
    super.insert(offset, i);
    return this;
}

public StringBuilder insert(int offset, long l) {
    super.insert(offset, l);
    return this;
}

public StringBuilder insert(int offset, float f) {
    super.insert(offset, f);
    return this;
}

public StringBuilder insert(int offset, double d) {
    super.insert(offset, d);
    return this;
}
```

插入操作要求 offset 参数在有效范围内（0 到 length 之间），否则会抛出 `StringIndexOutOfBoundsException`。与 append 类似，所有 insert 方法也返回 this，支持链式调用。

### 4.3 删除操作

```java
public StringBuilder delete(int start, int end) {
    super.delete(start, end);
    return this;
}

public StringBuilder deleteCharAt(int index) {
    super.deleteCharAt(index);
    return this;
}
```

`delete` 方法删除指定范围内的字符（从 start 到 end-1）。`deleteCharAt` 方法删除指定位置的单个字符。这些方法同样返回 this 以支持链式调用。

### 4.4 反转操作

```java
public StringBuilder reverse() {
    super.reverse();
    return this;
}
```

`reverse` 方法将字符序列反转。这个方法在某些场景下很有用，如回文检查、特殊格式转换等。反转操作是就地进行的，不需要额外的缓冲区。

### 4.5 替换操作

```java
public StringBuilder replace(int start, int end, String str) {
    super.replace(start, end, str);
    return this;
}
```

`replace` 方法将指定范围内的字符替换为新的字符串。这个操作比先 delete 再 insert 更高效，因为它只需要移动一次数据。

### 4.6 索引查找

```java
public int indexOf(String str) {
    return super.indexOf(str);
}

public int indexOf(String str, int fromIndex) {
    return super.indexOf(str, fromIndex);
}

public int lastIndexOf(String str) {
    return super.lastIndexOf(str);
}

public int lastIndexOf(String str, int fromIndex) {
    return super.lastIndexOf(str, fromIndex);
}
```

这些方法提供了在 StringBuilder 中查找子字符串的能力。`indexOf` 从前往后搜索，`lastIndexOf` 从后往前搜索。它们返回子字符串第一次出现的索引，如果未找到则返回 -1。

### 4.7 转换为 String

```java
@Override
public String toString() {
    return new String(value, 0, count);
}
```

`toString` 方法创建并返回一个新的 String 对象，包含 StringBuilder 中的所有字符。这个操作会复制字符数组的内容，因为 String 是不可变的。这是从可变 StringBuilder 转换为不可变 String 的标准方式。

## 五、容量管理与扩容机制

### 5.1 容量与长度的区别

理解 StringBuilder 的容量（capacity）和长度（length）是正确使用它的关键。容量是内部字符数组的大小，表示在不需要扩容的情况下可以存储的字符数。长度是当前存储的字符数量，等于 count 字段的值。

```java
StringBuilder sb = new StringBuilder(100);  // 容量为 100
System.out.println(sb.capacity());          // 100
System.out.println(sb.length());            // 0

sb.append("Hello");
System.out.println(sb.capacity());          // 100（未触发扩容）
System.out.println(sb.length());            // 5
```

### 5.2 扩容策略

当容量不足以容纳新的字符时，StringBuilder 会触发扩容。AbstractStringBuilder 中的扩容逻辑大致如下：

```java
void expandCapacity(int minimumCapacity) {
    int newCapacity = value.length * 2 + 2;
    if (newCapacity < minimumCapacity)
        newCapacity = minimumCapacity;
    value = Arrays.copyOf(value, newCapacity);
}
```

新的容量计算策略是"原容量翻倍加 2"。这种指数增长策略确保了摊销复杂度分析中，每次 append 操作的均摊时间复杂度为 O(1)。同时设置下限确保即使翻倍后仍然不足，也能满足最小容量需求。

### 5.3 容量管理的最佳实践

合理管理 StringBuilder 的容量可以提高性能：如果能够预估最终字符串的大致长度，在构造时指定初始容量可以避免频繁扩容。对于动态增长的字符串，保持适度的初始容量（如 64 或 128）通常比使用默认的 16 更好。

```java
// 不好的实践：循环中反复追加，可能触发多次扩容
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i).append(",");
}

// 好的实践：预估大小，预分配容量
StringBuilder sb = new StringBuilder(3000);  // 预估需要约 3000 字符
for (int i = 0; i < 1000; i++) {
    sb.append(i).append(",");
}
```

## 六、设计模式分析

### 6.1 可变对象模式

StringBuilder 是可变对象模式的典型实现。与 String 的不可变性不同，StringBuilder 允许创建后修改其内容。这种设计在需要频繁修改字符串的场景下提供了显著的性能优势。可变对象模式的核心优势在于：避免创建中间对象，减少内存分配和垃圾回收压力；支持原地修改，操作更高效；提供清晰的资源所有权语义。

### 6.2 建造者模式

虽然 StringBuilder 不是经典的建造者模式实现，但它体现了建造者模式的核心思想。StringBuilder 允许通过一系列的 `append` 或 `insert` 操作逐步构建复杂的字符串，最终通过 `toString()` 生成结果。这种"分步构建"的思想与建造者模式是一致的。

### 6.3 流畅接口模式

StringBuilder 的方法都返回 `this`，支持链式调用：

```java
sb.append("Hello")
  .append(" ")
  .append("World")
  .toString();
```

这种设计被称为"流畅接口"（Fluent Interface），它使得代码更具可读性和表达力。链式调用的风格在现代 Java API 设计中被广泛采用。

### 6.4 策略模式

通过 `AbstractStringBuilder` 的设计，StringBuilder 采用了策略模式的核心思想。核心的字符串操作逻辑在父类中实现，子类只需要提供正确的返回类型。这种设计使得 StringBuffer 和 StringBuilder 可以共享大部分实现代码，同时保持各自的特点。

## 七、常见使用模式

### 7.1 基本字符串拼接

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(" ");
sb.append("World");
String result = sb.toString();
```

### 7.2 循环中的字符串构建

```java
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100; i++) {
    if (i > 0) sb.append(",");
    sb.append(i);
}
String result = sb.toString();  // "0,1,2,...,99"
```

### 7.3 格式化字符串构建

```java
StringBuilder sb = new StringBuilder();
sb.append("Name: ").append(name)
  .append(", Age: ").append(age)
  .append(", City: ").append(city);
String formatted = sb.toString();
```

### 7.4 SQL 语句构建

```java
StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
if (name != null) {
    sql.append(" AND name = '").append(name).append("'");
}
if (age > 0) {
    sql.append(" AND age > ").append(age);
}
```

### 7.5 JSON 构建

```java
StringBuilder json = new StringBuilder();
json.append("{");
json.append("\"name\":\"").append(escapeJson(name)).append("\",");
json.append("\"age\":").append(age);
json.append("}");
```

## 八、常见问题与注意事项

### 8.1 线程安全性

StringBuilder 不是线程安全的，不应在多线程环境中共享使用。如果需要在多线程环境下进行字符串拼接，应该使用 StringBuffer，或者在每个线程中使用独立的 StringBuilder。

```java
// 错误：多线程共享 StringBuilder
StringBuilder shared = new StringBuilder();
// 多个线程同时访问 shared.append() 会导致数据损坏

// 正确：使用 StringBuffer
StringBuffer synchronized = new StringBuffer();
// 或每个线程使用独立的 StringBuilder
```

### 8.2 容量泄漏

如果不正确管理 StringBuilder 的容量，可能导致内存浪费：

```java
// 大量追加后，capacity 可能远大于 length
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100000; i++) {
    sb.append("some text ");
}
// 此时 capacity 可能是 131072，而 length 只有约 1100000

// 可以通过 trimToSize 释放多余空间
sb.trimToSize();
```

### 8.3 toString 的调用时机

`toString()` 方法会创建新的 String 对象，如果后续还需要继续修改 StringBuilder 的内容，应该避免过早调用 `toString()`：

```java
// 不好的实践
StringBuilder sb = new StringBuilder();
sb.append("Hello");
String result = sb.toString();  // 创建了 String 对象
sb.append(" World");  // 继续修改 StringBuilder
return sb.toString();  // 又创建了一个 String 对象

// 好的实践
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(" World");
return sb.toString();  // 只创建一次 String 对象
```

### 8.4 空指针异常

与 StringBuffer 不同，StringBuilder 的方法对 null 参数的处理是不安全的。传入 null 会导致 NullPointerException：

```java
StringBuilder sb = new StringBuilder();
sb.append(null);  // 抛出 NullPointerException
```

### 8.5 索引边界

所有涉及索引的方法（insert、delete、charAt、indexOf 等）都会检查索引的有效性，无效索引会抛出 StringIndexOutOfBoundsException 或 IndexOutOfBoundsException：

```java
StringBuilder sb = new StringBuilder("Hello");
sb.charAt(10);  // 抛出 IndexOutOfBoundsException
sb.delete(2, 1);  // 抛出 StringIndexOutOfBoundsException（start > end）
```

## 九、面试常见问题

### 9.1 StringBuilder 和 StringBuffer 的区别？

StringBuilder 和 StringBuffer 的 API 完全相同，核心区别在于线程安全性。StringBuffer 是线程安全的，因为它的所有方法都使用 synchronized 修饰；StringBuilder 不提供同步，不是线程安全的。在单线程环境下，StringBuilder 性能更好；在多线程环境下，应该使用 StringBuffer。

### 9.2 StringBuilder 和 String 的区别？

String 是不可变类，每次"修改"都会创建新的 String 对象；StringBuilder 是可变的，可以在原对象上修改。String 的拼接操作（如 + 运算符）在编译后也会使用 StringBuilder 实现，但每次循环都会创建新的 StringBuilder 对象。StringBuilder 适合大量字符串操作，可以避免频繁的对象创建和垃圾回收。

### 9.3 StringBuilder 的初始容量是多少？

默认构造函数创建的 StringBuilder 初始容量为 16 个字符。如果使用 `new StringBuilder(String str)` 或 `new StringBuilder(CharSequence seq)` 初始化，初始容量为参数长度加 16。如果使用 `new StringBuilder(int capacity)` 初始化，初始容量等于指定的 capacity。

### 9.4 StringBuilder 如何实现高效的字符串拼接？

StringBuilder 通过维护一个可动态扩展的字符数组来实现高效拼接。append 操作在内部字符数组上进行，只有在容量不足时才扩容。扩容采用翻倍策略，确保摊销时间复杂度为 O(1)。所有操作都在原对象上进行，避免了创建中间 String 对象的开销。

### 9.5 StringBuilder 的 toString 方法每次都创建新对象吗？

是的，每次调用 `toString()` 都会创建新的 String 对象。这是因为 String 是不可变的，必须复制 StringBuilder 的内部字符数组来创建新的 String 对象。如果需要多次调用 toString 且内容不变，应该缓存结果。

### 9.6 什么情况下应该使用 StringBuilder？

单线程环境下的字符串拼接操作；循环中的字符串构建；需要频繁修改的字符串内容；构建格式化的字符串输出；任何不需要线程安全的字符串操作场景。

## 十、与其他类的对比

### 10.1 StringBuilder 与 StringBuffer

StringBuilder 和 StringBuffer 都是可变的字符序列，但 StringBuffer 提供同步。在 Java 5 引入 StringBuilder 之前，StringBuffer 是唯一的选择。现代 Java 开发中，除非明确需要在多线程环境下共享，否则都应该使用 StringBuilder。

性能对比方面，StringBuilder 的 append 操作通常比 StringBuffer 快 30% 到 50%，因为没有同步锁的开销。在高并发场景下，这种性能差异更加明显。

### 10.2 StringBuilder 与 String

StringBuilder 相比 String 的优势在于可变性带来的性能优化。对于 n 次字符串拼接操作，使用 String 的时间复杂度是 O(n²)（每次拼接都创建新对象），而使用 StringBuilder 的时间复杂度是 O(n)（均摊分析）。

```java
// 使用 String 的低效方式
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 每次循环都创建新的 String 对象
}

// 使用 StringBuilder 的高效方式
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);  // 在原对象上追加
}
String result = sb.toString();
```

### 10.3 StringBuilder 与 StringJoiner

Java 8 引入的 `StringJoiner` 是另一种字符串拼接工具，它专门用于处理分隔符：

```java
// 使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 3; i++) {
    if (i > 0) sb.append(",");
    sb.append(i);
}

// 使用 StringJoiner（更简洁）
StringJoiner joiner = new StringJoiner(",");
for (int i = 0; i < 3; i++) {
    joiner.add(String.valueOf(i));
}
```

StringJoiner 在需要添加前缀、后缀和分隔符的场景下更方便，但 StringBuilder 更通用。

### 10.4 StringBuilder 与字符数组

直接操作字符数组可以实现类似 StringBuilder 的功能，但 StringBuilder 提供了更友好的 API 和更好的封装：

```java
// 使用字符数组（繁琐且容易出错）
char[] buffer = new char[100];
int count = 0;
buffer[count++] = 'H';
buffer[count++] = 'i';
String result = new String(buffer, 0, count);

// 使用 StringBuilder（简洁安全）
StringBuilder sb = new StringBuilder();
sb.append('H').append('i');
String result = sb.toString();
```

### 10.5 StringBuilder 与 Apache Commons Lang 的 StringUtils

Apache Commons Lang 提供了丰富的字符串工具，但 StringBuilder 是标准库的一部分：

```java
// Apache Commons Lang
String result = StringUtils.join(list, ", ");

// StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < list.size(); i++) {
    if (i > 0) sb.append(", ");
    sb.append(list.get(i));
}

String result = sb.toString();
```

对于简单的连接操作，StringBuilder 更直接；对于复杂的字符串处理，Apache Commons Lang 提供了更多便利方法。

