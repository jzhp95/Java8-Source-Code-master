# String 类源码深度解析

## 一、类的概述

### 1.1 基本信息

`String` 类是 Java 中用于表示字符串的类，它是 Java 语言中使用频率最高的类之一。在 Java 程序中，所有的字符串字面量（如 `"abc"`）都是 `String` 类的实例。

**源码位置**：`src/main/jdk8/java/lang/String.java`

**类的声明**：

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence
```

**类的定位**：

- `String` 是 Java 中表示字符串的最终类，不能被继承
- 实现了三个重要接口：`Serializable`（支持序列化）、`Comparable<String>`（支持排序比较）、`CharSequence`（字符序列接口）
- 字符串在 Java 中是不可变的（Immutable），这是 String 类的核心特性
- Java 语言对字符串字面量有特殊支持，编译器会自动创建 String 对象

### 1.2 设计理念

`String` 类的设计体现了以下核心理念：

**1. 不可变性（Immutability）**

String 类的不可变性是其最重要的设计决策。这种不可变性通过以下方式实现：

- `value` 数组被声明为 `private final char[]`，初始化后不能被修改
- 没有提供任何可以修改 value 数组内容的方法
- 所有看似"修改"字符串的方法实际上都是返回新的 String 对象

```java
public final class String {
    private final char value[];  // 核心字段，不可变
    private int hash;  // 缓存的哈希值
}
```

**2. 字符串常量池（String Pool）**

为了优化字符串的使用效率，Java 虚拟机维护了一个字符串常量池：

```java
// 字符串常量池的存在使得以下代码高效
String s1 = "hello";
String s2 = "hello";
// s1 和 s2 指向常量池中的同一个对象
System.out.println(s1 == s2);  // true
```

**3. 线程安全**

由于 String 的不可变性，它天然是线程安全的：

```java
// String 可以安全地在多线程间共享
public class ThreadSafeExample {
    private final String sharedString = "shared";  // 线程安全
}
```

**4. 性能优化**

String 类在多个层面进行了性能优化：

- 缓存 `hash` 值，避免重复计算
- 字符串拼接使用 StringBuilder 内部实现
- intern() 方法支持字符串常量池共享

### 1.3 版本信息

- **JDK 版本**：JDK 1.0
- **作者**：Lee Boynton, Arthur van Hoff, Martin Buchholz, Ulf Zibis
- **重大变更**：
  - JDK 1.4：添加了 contentEquals() 方法
  - JDK 1.5：添加了 replace(CharSequence, CharSequence) 方法
  - JDK 1.7：底层实现从 char[] 变为 byte[]（在某些 UTF-8 场景下更节省内存）
  - JDK 1.8：在当前项目中仍然使用 char[] 实现

### 1.4 Unicode 支持

String 类支持完整的 Unicode 字符集：

```java
// UTF-16 编码，一个字符可能占用 1-2 个 char
String chinese = "中文";  // 4 个 char
String emoji = "😀";  // 2 个 char（surrogate pair）
```

## 二、类的继承结构

### 2.1 UML 类图

```
┌─────────────────────────────────────────────────────────────┐
│                      java.lang.String                       │
├─────────────────────────────────────────────────────────────┤
│ - value: char[]                                            │
│ - hash: int                                                │
│ - serialVersionUID: long                                   │
├─────────────────────────────────────────────────────────────┤
│ + String()                                                 │
│ + String(String original)                                  │
│ + String(char[] value)                                     │
│ + String(char[] value, int offset, int count)              │
│ + String(int[] codePoints, int offset, int count)          │
│ + String(byte[] bytes)                                     │
│ + String(byte[] bytes, String charsetName)                 │
│ + String(StringBuffer buffer)                              │
│ + String(StringBuilder builder)                            │
├─────────────────────────────────────────────────────────────┤
│ + length(): int                                            │
│ + charAt(int index): char                                  │
│ + compareTo(String anotherString): int                     │
│ + equals(Object anObject): boolean                         │
│ + hashCode(): int                                          │
│ + indexOf(int ch): int                                     │
│ + indexOf(String str): int                                 │
│ + substring(int beginIndex): String                        │
│ + substring(int beginIndex, int endIndex): String          │
│ + concat(String str): String                               │
│ + replace(char oldChar, newChar): String                   │
│ + toLowerCase(): String                                    │
│ + toUpperCase(): String                                    │
│ + trim(): String                                           │
│ + intern(): String                                         │
├─────────────────────────────────────────────────────────────┤
│ ^ implements Serializable                                  │
│ ^ implements Comparable<String>                            │
│ ^ implements CharSequence                                  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 String 的关系图

```
Serializable (接口)
    △
    │
    ├─ String
    ├─ StringBuffer
    ├─ StringBuilder
    └─ 其他可序列化类

Comparable<String> (接口)
    △
    │
    └─ String

CharSequence (接口)
    △
    │
    ├─ String
    ├─ StringBuffer
    ├─ StringBuilder
    ├─ CharBuffer
    └─ 其他字符序列类
```

### 2.3 String 与相关类的关系

**String vs StringBuffer vs StringBuilder**：

| 特性 | String | StringBuffer | StringBuilder |
|------|--------|--------------|---------------|
| 线程安全 | 是 | 是 | 否 |
| 可变性 | 不可变 | 可变 | 可变 |
| 性能 | 低（每次修改创建新对象） | 中等 | 高 |
| 使用场景 | 常量字符串 | 多线程环境 | 单线程环境 |
| 引入版本 | JDK 1.0 | JDK 1.0 | JDK 1.5 |

```java
// String 不可变 - 每次操作都创建新对象
String str = "a";
str = str + "b";  // 创建新对象

// StringBuffer 可变且线程安全
StringBuffer sb = new StringBuffer("a");
sb.append("b");  // 修改同一个对象，线程安全

// StringBuilder 可变但非线程安全
StringBuilder sbu = new StringBuilder("a");
sbu.append("b");  // 修改同一个对象，性能更高
```

## 三、核心字段解析

### 3.1 value 字段

```java
private final char value[];
```

**设计意图**：

- `value` 是 String 类的核心存储字段，存储字符串的实际字符内容
- 被声明为 `final`，确保 String 对象创建后内容不可修改
- 使用数组而非其他数据结构，是为了高效的字符访问和内存管理

**重要特性**：

```java
// String 对象共享 value 数组的场景
String s1 = new String(new char[]{'a', 'b', 'c'});
String s2 = s1.substring(0, 2);

// 注意：substring 在 JDK 7u6 之前会共享原数组
// 之后会创建新的数组，避免内存泄漏问题
```

**内存布局**：

```java
// 一个 String 对象在内存中的结构
public class String {
    private final char value[];  // 引用（4/8 字节）
    private int hash;            // 4 字节
    // 对象头（8/16 字节）
}
// 总开销约 24-40 字节 + 字符数组的内存
```

### 3.2 hash 字段

```java
private int hash; // Default to 0
```

**设计意图**：

- 缓存字符串的哈希码，避免重复计算
- 默认值为 0，表示尚未计算哈希值
- 只有在首次调用 `hashCode()` 方法时才会计算并缓存

**优化原理**：

```java
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;
        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}

// 优化效果：
// - 空字符串的 hashCode() 永远返回 0（无需计算）
// - 多次调用 hashCode() 只计算一次
// - 在哈希表中重复查找同一字符串时，性能显著提升
```

### 3.3 serialVersionUID 字段

```java
private static final long serialVersionUID = -6849794470754667710L;
```

**设计意图**：

- 用于 Java 序列化机制中的版本控制
- 确保序列化和反序列化时类的兼容性

### 3.4 serialPersistentFields 字段

```java
private static final ObjectStreamField[] serialPersistentFields =
    new ObjectStreamField[0];
```

**设计意图**：

- 指定序列化时应该持久化的字段
- String 类通过自定义的序列化逻辑优化性能

## 四、构造方法分析

### 4.1 默认构造方法

```java
public String() {
    this.value = "".value;
}
```

**特点**：

- 创建空字符串
- 直接使用字符串常量池中的空字符串对象，节省内存
- 实际使用中很少直接调用，因为 `""` 字面量已经足够

```java
// 等价于
String empty = "";
```

### 4.2 String 拷贝构造方法

```java
public String(String original) {
    this.value = original.value;
    this.hash = original.hash;
}
```

**设计特点**：

- 直接共享 `value` 数组引用，没有进行数组拷贝
- 复制 `hash` 值，避免重复计算
- 由于 String 不可变，这种共享是安全的

```java
String original = new String("hello");
String copy = new String(original);
// original 和 copy 共享同一个 value 数组
// 但由于不可变性，这是安全的
```

### 4.3 char[] 构造方法

```java
public String(char value[]) {
    this.value = Arrays.copyOf(value, value.length);
}
```

**特点**：

- 创建字符数组的拷贝，确保 String 的不可变性
- 保护性拷贝，防止外部修改原数组影响 String

```java
char[] chars = {'h', 'e', 'l', 'l', 'o'};
String str = new String(chars);
chars[0] = 'H';  // 不会影响 str，str 仍然是 "hello"
```

### 4.4 char[] 子数组构造方法

```java
public String(char value[], int offset, int count) {
    if (offset < 0) {
        throw new StringIndexOutOfBoundsException(offset);
    }
    if (count <= 0) {
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        if (offset <= value.length) {
            this.value = "".value;
            return;
        }
    }
    if (offset > value.length - count) {
        throw new StringIndexOutOfBoundsException(offset + count);
    }
    this.value = Arrays.copyOfRange(value, offset, offset + count);
}
```

**边界检查**：

- 检查 offset 是否为负
- 检查 count 是否为负或零
- 检查 offset + count 是否越界

### 4.5 int[] codePoints 构造方法

```java
public String(int[] codePoints, int offset, int count) {
    // 第一遍：计算需要的 char 数组大小
    int n = count;
    for (int i = offset; i < offset + count; i++) {
        int c = codePoints[i];
        if (Character.isBmpCodePoint(c))
            continue;
        else if (Character.isValidCodePoint(c))
            n++;  // 补充字符需要两个 char
        else
            throw new IllegalArgumentException(Integer.toString(c));
    }

    // 第二遍：分配并填充 char 数组
    final char[] v = new char[n];
    for (int i = offset, j = 0; i < offset + count; i++, j++) {
        int c = codePoints[i];
        if (Character.isBmpCodePoint(c))
            v[j] = (char) c;
        else
            Character.toSurrogates(c, v, j++);
    }

    this.value = v;
}
```

**功能说明**：

- 支持从 Unicode 码点数组创建字符串
- 补充字符（U+10000 及以上）需要两个 char
- 需要两遍处理：第一遍计算大小，第二遍填充

```java
// 使用示例
int[] codePoints = {0x0041, 0x0042, 0x1D306};  // A, B, 𝌆
String str = new String(codePoints, 0, 3);  // "AB𝌆"
```

### 4.6 byte[] 构造方法家族

```java
// 使用平台默认编码
public String(byte bytes[]) {
    this(bytes, 0, bytes.length);
}

// 使用指定编码
public String(byte bytes[], String charsetName)
    throws UnsupportedEncodingException {
    this(bytes, 0, bytes.length, charsetName);
}

// 使用指定 Charset
public String(byte bytes[], Charset charset) {
    this(bytes, 0, bytes.length, charset);
}

// 使用指定编码和范围
public String(byte bytes[], int offset, int length, String charsetName)
    throws UnsupportedEncodingException {
    if (charsetName == null)
        throw new NullPointerException("charsetName");
    checkBounds(bytes, offset, length);
    this.value = StringCoding.decode(charsetName, bytes, offset, length);
}
```

**编码处理**：

- `StringCoding.decode()` 负责字节到字符的解码
- 支持多种字符编码（UTF-8、GBK、ISO-8859-1 等）
- 自动处理编码异常

```java
// 编码示例
byte[] utf8Bytes = "中文".getBytes("UTF-8");
String str = new String(utf8Bytes, "UTF-8");
```

### 4.7 StringBuffer 和 StringBuilder 构造方法

```java
// StringBuffer 构造方法（线程安全）
public String(StringBuffer buffer) {
    synchronized(buffer) {
        this.value = Arrays.copyOf(buffer.getValue(), buffer.length());
    }
}

// StringBuilder 构造方法（非线程安全，更快）
public String(StringBuilder builder) {
    this.value = Arrays.copyOf(builder.getValue(), builder.length());
}
```

**设计差异**：

- StringBuffer 使用 synchronized 保证线程安全
- StringBuilder 直接复制，不需要同步

### 4.8 包级私有共享构造方法

```java
String(char[] value, boolean share) {
    // assert share : "unshared not supported";
    this.value = value;
}
```

**设计目的**：

- 这是一个优化构造方法，用于 String 内部操作
- `share` 参数实际上被忽略（始终为 true）
- 避免了数组拷贝，提高性能
- 仅供 String 内部使用，如 substring()、concat() 等方法

```java
// concat() 方法使用此构造方法
public String concat(String str) {
    int otherLen = str.length();
    if (otherLen == 0) {
        return this;
    }
    int len = value.length;
    char buf[] = Arrays.copyOf(value, len + otherLen);
    str.getChars(buf, len);
    return new String(buf, true);  // 共享数组
}
```

## 五、核心方法详解

### 5.1 length() 方法

```java
public int length() {
    return value.length;
}
```

**特点**：

- 直接返回 value 数组的长度，时间复杂度 O(1)
- 字符数等于 `char` 数组长度（补充字符占两个位置）

```java
String str = "中文";
System.out.println(str.length());  // 2（两个 char）

String emoji = "😀";
System.out.println(emoji.length());  // 2（surrogate pair）
```

### 5.2 isEmpty() 方法

```java
public boolean isEmpty() {
    return value.length == 0;
}
```

**引入版本**：JDK 1.6

**特点**：

- 检查字符串是否为空（长度为 0）
- 与 `length() == 0` 等价，但语义更清晰

```java
String empty = "";
String notEmpty = " ";
System.out.println(empty.isEmpty());   // true
System.out.println(notEmpty.isEmpty()); // false（空格不是空）
```

### 5.3 charAt() 方法

```java
public char charAt(int index) {
    if ((index < 0) || (index >= value.length)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return value[index];
}
```

**特点**：

- 直接访问 value 数组，时间复杂度 O(1)
- 边界检查抛出 `StringIndexOutOfBoundsException`

```java
String str = "Hello";
char c = str.charAt(1);  // 'e'
// str.charAt(5);  // 抛出异常
```

### 5.4 codePointAt() 方法

```java
public int codePointAt(int index) {
    if ((index < 0) || (index >= value.length)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return Character.codePointAtImpl(value, index, value.length);
}
```

**功能**：

- 返回指定索引处的 Unicode 码点
- 对于补充字符，返回完整的码点值（不是 surrogate pair）

```java
String emoji = "😀";
int codePoint = emoji.codePointAt(0);  // 0x1F600
System.out.println(Integer.toHexString(codePoint));  // 1f600
```

### 5.5 getChars() 方法

```java
public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
    if (srcBegin < 0) {
        throw new StringIndexOutOfBoundsException(srcBegin);
    }
    if (srcEnd > value.length) {
        throw new StringIndexOutOfBoundsException(srcEnd);
    }
    if (srcBegin > srcEnd) {
        throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
    }
    System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
}
```

**特点**：

- 使用 `System.arraycopy` 高效复制字符
- 批量复制，比逐个 `charAt()` 效率高得多

```java
String str = "Hello World";
char[] chars = new char[5];
str.getChars(0, 5, chars, 0);  // chars = ['H', 'e', 'l', 'l', 'o']
```

### 5.6 getBytes() 方法

```java
public byte[] getBytes(Charset charset) {
    if (charset == null) throw new NullPointerException();
    return StringCoding.encode(charset, value, 0, value.length);
}
```

**功能**：

- 将字符串编码为指定字符集的字节数组
- `StringCoding.encode()` 负责编码过程

```java
String str = "中文";
byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
byte[] gbk = str.getBytes("GBK");
```

### 5.7 equals() 方法

```java
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String) anObject;
        int n = value.length;
        if (n == anotherString.value.length) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = 0;
            while (n-- != 0) {
                if (v1[i] != v2[i])
                    return false;
                i++;
            }
            return true;
        }
    }
    return false;
}
```

**比较过程**：

1. 检查引用是否相同（快速路径）
2. 检查类型是否为 String
3. 检查长度是否相同
4. 逐个字符比较

**性能优化**：

```java
// 长度检查在比较字符之前，避免不必要的字符比较
if (n == anotherString.value.length) {
    // 只在长度相同时才比较字符
    for (int i = 0; i < n; i++) {
        if (v1[i] != v2[i])
            return false;
    }
}
```

### 5.8 contentEquals() 方法

```java
public boolean contentEquals(CharSequence cs) {
    if (cs instanceof AbstractStringBuilder) {
        if (cs instanceof StringBuffer) {
            synchronized(cs) {
                return nonSyncContentEquals((AbstractStringBuilder) cs);
            }
        } else {
            return nonSyncContentEquals((AbstractStringBuilder) cs);
        }
    }
    if (cs instanceof String) {
        return equals(cs);
    }
    // 其他 CharSequence 实现
    char v1[] = value;
    int n = v1.length;
    if (n != cs.length()) {
        return false;
    }
    for (int i = 0; i < n; i++) {
        if (v1[i] != cs.charAt(i)) {
            return false;
        }
    }
    return true;
}
```

**功能**：

- 比较 String 与 StringBuffer、StringBuilder 或其他 CharSequence 的内容
- 对 StringBuffer 进行同步处理
- 支持比较任意实现 CharSequence 接口的对象

```java
String str = "hello";
StringBuffer sb = new StringBuffer("hello");
System.out.println(str.contentEquals(sb));  // true
```

### 5.9 equalsIgnoreCase() 方法

```java
public boolean equalsIgnoreCase(String anotherString) {
    return (this == anotherString) ? true
            : (anotherString != null)
            && (anotherString.value.length == value.length)
            && regionMatches(true, 0, anotherString, 0, value.length);
}
```

**特点**：

- 使用 `regionMatches(true, ...)` 进行忽略大小写的比较
- 仍然先检查长度，提升效率

```java
String s1 = "Hello";
String s2 = "HELLO";
System.out.println(s1.equalsIgnoreCase(s2));  // true
```

### 5.10 compareTo() 方法

```java
public int compareTo(String anotherString) {
    int len1 = value.length;
    int len2 = anotherString.value.length;
    int lim = Math.min(len1, len2);
    char v1[] = value;
    char v2[] = anotherString.value;

    int k = 0;
    while (k < lim) {
        char c1 = v1[k];
        char c2 = v2[k];
        if (c1 != c2) {
            return c1 - c2;  // 返回字符差值
        }
        k++;
    }
    return len1 - len2;  // 长度差值
}
```

**字典序比较**：

- 从第一个字符开始逐个比较
- 遇到不同字符时返回字符差值
- 所有字符相同时返回长度差值

```java
String a = "apple";
String b = "banana";
System.out.println(a.compareTo(b));  // 负数（'a' < 'b'）

String c = "test";
String d = "testing";
System.out.println(c.compareTo(d));  // 负数（长度更短）
```

### 5.11 regionMatches() 方法

```java
public boolean regionMatches(int toffset, String other, int ooffset,
        int len) {
    char ta[] = value;
    int to = toffset;
    char pa[] = other.value;
    int po = ooffset;

    if ((ooffset < 0) || (toffset < 0)
            || (toffset > (long) value.length - len)
            || (ooffset > (long) other.value.length - len)) {
        return false;
    }

    while (len-- > 0) {
        if (ta[to++] != pa[po++]) {
            return false;
        }
    }
    return true;
}
```

**功能**：

- 比较两个字符串的指定区域
- 区域由起始偏移量和长度定义

```java
String str1 = "Hello World";
String str2 = "World";
// 比较 str1 的后 5 个字符和 str2
System.out.println(str1.regionMatches(6, str2, 0, 5));  // true
```

### 5.12 startsWith() 和 endsWith() 方法

```java
public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
}

public boolean startsWith(String prefix, int toffset) {
    char ta[] = value;
    int to = toffset;
    char pa[] = prefix.value;
    int po = 0;
    int pc = prefix.value.length;

    if ((toffset < 0) || (toffset > value.length - pc)) {
        return false;
    }

    while (--pc >= 0) {
        if (ta[to++] != pa[po++]) {
            return false;
        }
    }
    return true;
}

public boolean endsWith(String suffix) {
    return startsWith(suffix, value.length - suffix.value.length);
}
```

**实现优化**：

- 使用 `toffset` 参数支持任意位置检查
- `endsWith()` 复用 `startsWith()` 实现

```java
String str = "Hello.txt";
System.out.println(str.startsWith("Hello"));    // true
System.out.println(str.endsWith(".txt"));       // true
System.out.println(str.endsWith(".jpg"));       // false
```

### 5.13 hashCode() 方法

```java
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}
```

**哈希算法**：

```java
// 算法公式：s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
// 例如 "abc" = 'a'*31^2 + 'b'*31^1 + 'c'*31^0
//       = 97*961 + 98*31 + 99
//       = 93170 + 3038 + 99
//       = 96307
```

**选择 31 作为乘数的原因**：

1. 31 是质数，可以减少哈希冲突
2. 31 的乘法可以用位移优化：`31 * h` = `(h << 5) - h`
3. 31 的值不会太大导致整数溢出，也不会太小导致分布不均

```java
// 验证 hashCode 算法
String str = "abc";
int hash = 0;
for (int i = 0; i < str.length(); i++) {
    hash = 31 * hash + str.charAt(i);
}
System.out.println(hash);  // 96307
System.out.println(str.hashCode());  // 96307
```

### 5.14 indexOf() 方法家族

```java
public int indexOf(int ch) {
    return indexOf(ch, 0);
}

public int indexOf(int ch, int fromIndex) {
    final int max = value.length;
    if (fromIndex < 0) {
        fromIndex = 0;
    } else if (fromIndex >= max) {
        return -1;
    }

    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        // BMP 字符，直接遍历查找
        final char[] value = this.value;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == ch) {
                return i;
            }
        }
        return -1;
    } else {
        // 补充字符，需要特殊处理
        return indexOfSupplementary(ch, fromIndex);
    }
}
```

**查找策略**：

- 基本多语言平面（BMP）字符使用简单遍历
- 补充字符使用 surrogate pair 查找
- 从指定位置开始查找

```java
String str = "Hello World";
System.out.println(str.indexOf('l'));        // 2
System.out.println(str.indexOf('l', 3));     // 3
System.out.println(str.indexOf('x'));        // -1（未找到）
```

### 5.15 lastIndexOf() 方法家族

```java
public int lastIndexOf(int ch) {
    return lastIndexOf(ch, value.length - 1);
}

public int lastIndexOf(int ch, int fromIndex) {
    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        final char[] value = this.value;
        int i = Math.min(fromIndex, value.length - 1);
        for (; i >= 0; i--) {
            if (value[i] == ch) {
                return i;
            }
        }
        return -1;
    } else {
        return lastIndexOfSupplementary(ch, fromIndex);
    }
}
```

**特点**：

- 从后向前搜索
- `fromIndex` 默认值为 `length - 1`（最后一个字符）

```java
String str = "Hello World";
System.out.println(str.lastIndexOf('l'));      // 9（从后往前第一个 'l'）
System.out.println(str.lastIndexOf('l', 5));   // 3（只搜索前 5 个字符）
```

### 5.16 substring() 方法

```java
public String substring(int beginIndex) {
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    int subLen = value.length - beginIndex;
    if (subLen < 0) {
        throw new StringIndexOutOfBoundsException(subLen);
    }
    return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
}

public String substring(int beginIndex, int endIndex) {
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > value.length) {
        throw new StringIndexOutOfBoundsException(endIndex);
    }
    int subLen = endIndex - beginIndex;
    if (subLen < 0) {
        throw new StringIndexOutOfBoundsException(subLen);
    }
    return ((beginIndex == 0) && (endIndex == value.length)) ? this
            : new String(value, beginIndex, subLen);
}
```

**实现特点**：

- JDK 7u6 之前，substring 可能共享原字符串的 char 数组
- JDK 7u6 之后，创建新的 char 数组，避免潜在的内存泄漏
- 如果截取整个字符串，返回原对象本身（优化）

```java
String str = "Hello World";
String sub1 = str.substring(6);     // "World"
String sub2 = str.substring(0, 5);  // "Hello"
String sub3 = str.substring(0);     // 与 str 相同（返回 this）
```

### 5.17 concat() 方法

```java
public String concat(String str) {
    int otherLen = str.length();
    if (otherLen == 0) {
        return this;
    }
    int len = value.length;
    char buf[] = Arrays.copyOf(value, len + otherLen);
    str.getChars(buf, len);
    return new String(buf, true);
}
```

**实现特点**：

- 只在需要时创建新数组（当 str 不为空时）
- 使用 `Arrays.copyOf` 复制原数组
- 使用内部共享构造方法避免额外拷贝

```java
String str = "Hello";
String result = str.concat(" World");  // "Hello World"
```

### 5.18 replace() 方法家族

```java
public String replace(char oldChar, char newChar) {
    if (oldChar != newChar) {
        int len = value.length;
        int i = -1;
        char[] val = value;

        // 查找第一个需要替换的位置
        while (++i < len) {
            if (val[i] == oldChar) {
                break;
            }
        }

        // 如果找到了，创建新数组并替换
        if (i < len) {
            char buf[] = new char[len];
            for (int j = 0; j < i; j++) {
                buf[j] = val[j];
            }
            while (i < len) {
                char c = val[i];
                buf[i] = (c == oldChar) ? newChar : c;
                i++;
            }
            return new String(buf, true);
        }
    }
    return this;  // 没有找到，不变
}
```

**特点**：

- 找到第一个匹配位置后才创建新数组
- 只替换一次遍历，高效

```java
String str = "Hello";
String result = str.replace('l', 'L');  // "HeLLo"
```

### 5.19 replace(CharSequence, CharSequence) 方法

```java
public String replace(CharSequence target, CharSequence replacement) {
    return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
            this).replaceAll(Matcher.quoteReplacement(replacement.toString()));
}
```

**功能**：

- 替换所有匹配的字符序列
- 使用正则表达式实现，但进行字面量转义

```java
String str = "a*b*c";
String result = str.replace("*", "-");  // "a-b-c"
```

### 5.20 matches() 方法

```java
public boolean matches(String regex) {
    return Pattern.matches(regex, this);
}
```

**功能**：

- 检查字符串是否完全匹配正则表达式

```java
String str = "12345";
System.out.println(str.matches("\\d+"));  // true
System.out.println(str.matches("\\d*"));  // false（有内容，不匹配空）
```

### 5.21 replaceFirst() 和 replaceAll() 方法

```java
public String replaceFirst(String regex, String replacement) {
    return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
}

public String replaceAll(String regex, String replacement) {
    return Pattern.compile(regex).matcher(this).replaceAll(replacement);
}
```

**区别**：

- `replaceFirst`：只替换第一个匹配
- `replaceAll`：替换所有匹配

```java
String str = "a1b2c3";
System.out.println(str.replaceFirst("\\d", "-"));  // "a-b2c3"
System.out.println(str.replaceAll("\\d", "-"));    // "a-b-c-"
```

### 5.22 split() 方法

```java
public String[] split(String regex, int limit) {
    // 快速路径：单字符且不是正则元字符
    char ch = 0;
    if (((regex.value.length == 1 &&
         ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
         (regex.length() == 2 &&
          regex.charAt(0) == '\\' &&
          (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
          ((ch-'a')|('z'-ch)) < 0 &&
          ((ch-'A')|('Z'-ch)) < 0)) &&
        (ch < Character.MIN_HIGH_SURROGATE ||
         ch > Character.MAX_LOW_SURROGATE))
    {
        int off = 0;
        int next = 0;
        boolean limited = limit > 0;
        ArrayList<String> list = new ArrayList<>();
        while ((next = indexOf(ch, off)) != -1) {
            if (!limited || list.size() < limit - 1) {
                list.add(substring(off, next));
                off = next + 1;
            } else {
                // 最后一个元素，包含剩余所有内容
                list.add(substring(off, value.length));
                off = value.length;
                break;
            }
        }
        // ...
    }
    return Pattern.compile(regex).split(this, limit);
}
```

**limit 参数的作用**：

| limit 值 | 行为 |
|---------|------|
| n > 0 | 最多分割 n-1 次，数组长度不超过 n |
| n < 0 | 尽可能多地分割，保留所有结果（包括空字符串） |
| n = 0 | 尽可能多地分割，丢弃尾部空字符串 |

```java
String str = "a:b:c:d";
System.out.println(Arrays.toString(str.split(":", 2)));  // ["a", "b:c:d"]
System.out.println(Arrays.toString(str.split(":", -2))); // ["a", "b", "c", "d"]
System.out.println(Arrays.toString(str.split(":", 0)));  // ["a", "b", "c"]（尾部空被丢弃）
```

### 5.23 join() 方法（Java 8 新增）

```java
public static String join(CharSequence delimiter, CharSequence... elements) {
    Objects.requireNonNull(delimiter);
    Objects.requireNonNull(elements);
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence cs : elements) {
        joiner.add(cs);
    }
    return joiner.toString();
}

public static String join(CharSequence delimiter,
        Iterable<? extends CharSequence> elements) {
    Objects.requireNonNull(delimiter);
    Objects.requireNonNull(elements);
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence cs : elements) {
        joiner.add(cs);
    }
    return joiner.toString();
}
```

**引入版本**：JDK 1.8

**特点**：

- 方便地连接多个字符串
- 使用 `StringJoiner` 内部实现

```java
// 使用可变参数
String result1 = String.join("-", "a", "b", "c");  // "a-b-c"

// 使用 Iterable
List<String> list = Arrays.asList("a", "b", "c");
String result2 = String.join("-", list);  // "a-b-c"
```

### 5.24 toLowerCase() 和 toUpperCase() 方法

```java
public String toLowerCase(Locale locale) {
    // 优化：先检查是否需要转换
    for (int firstUpper = 0; firstUpper < value.length; ) {
        char c = value[firstUpper];
        if (c != Character.toLowerCase(c)) {
            break scan;  // 发现需要转换的字符
        }
        firstUpper++;
    }
    return this;  // 不需要转换，返回原字符串

    // 需要转换时创建新数组...
}
```

**特点**：

- 土耳其语等特殊语言有大小写转换规则
- 默认使用系统 locale

```java
String str = "Istanbul";
// 土耳其语中，小写的 "i" 对应大写的 "İ"（带点）
System.out.println(str.toLowerCase(new Locale("tr")));  // istanbul

// 避免 locale 影响，使用 ROOT
System.out.println(str.toLowerCase(Locale.ROOT));  // istanbul
```

### 5.25 trim() 方法

```java
public String trim() {
    int len = value.length;
    int st = 0;
    char[] val = value;

    while ((st < len) && (val[st] <= ' ')) {
        st++;
    }
    while ((st < len) && (val[len - 1] <= ' ')) {
        len--;
    }
    return ((st > 0) || (len < value.length)) ? substring(st, len) : this;
}
```

**特点**：

- 移除字符串两端的空白字符（ASCII <= 32）
- 空白定义：`val[st] <= ' '`（包括空格、制表符、换行符等）
- 不移除非空白字符

```java
String str = "  Hello World  ";
System.out.println(str.trim());  // "Hello World"
System.out.println("\t\n\r".trim());  // ""（全是空白）
```

**注意**：`trim()` 只移除 ASCII 空白，不移除全角空格等 Unicode 空白

```java
// trim() 不处理的情况
String str = "　Hello　";  // 全角空格
System.out.println(str.trim());  // "　Hello　"（全角空格未被移除）
// 需要使用 strip()（JDK 11+）或正则表达式
```

### 5.26 toString() 方法

```java
public String toString() {
    return this;
}
```

**特点**：

- String 的 `toString()` 返回自身
- 这是合理的，因为 String 本身就是字符串表示

### 5.27 toCharArray() 方法

```java
public char[] toCharArray() {
    char result[] = new char[value.length];
    System.arraycopy(value, 0, result, 0, value.length);
    return result;
}
```

**特点**：

- 创建新的字符数组并复制内容
- 返回的数组与原 String 独立

```java
String str = "Hello";
char[] chars = str.toCharArray();
chars[0] = 'h';  // 不影响原字符串
System.out.println(str);  // "Hello"
```

### 5.28 valueOf() 静态方法家族

```java
public static String valueOf(Object obj) {
    return (obj == null) ? "null" : obj.toString();
}

public static String valueOf(char data[]) {
    return new String(data);
}

public static String valueOf(char data[], int offset, int count) {
    return new String(data, offset, count);
}

public static String valueOf(boolean b) {
    return b ? "true" : "false";
}

public static String valueOf(char c) {
    char data[] = {c};
    return new String(data, true);
}

public static String valueOf(int i) {
    return Integer.toString(i);
}

public static String valueOf(long l) {
    return Long.toString(l);
}

public static String valueOf(float f) {
    return Float.toString(f);
}

public static String valueOf(double d) {
    return Double.toString(d);
}
```

**特点**：

- 将各种类型转换为 String
- 对基本类型使用对应的包装类的 `toString()` 方法
- `valueOf(Object)` 对 null 进行特殊处理

```java
System.out.println(String.valueOf(123));       // "123"
System.out.println(String.valueOf(3.14));      // "3.14"
System.out.println(String.valueOf(true));      // "true"
System.out.println(String.valueOf(null));      // "null"
```

### 5.29 format() 静态方法

```java
public static String format(String format, Object... args) {
    return new Formatter().format(format, args).toString();
}

public static String format(Locale l, String format, Object... args) {
    return new Formatter(l).format(format, args).toString();
}
```

**功能**：

- 使用指定格式格式化字符串
- 支持多种格式化选项

```java
String result = String.format("Hello, %s! Today is %tA.", "World", new Date());
// "Hello, World! Today is 星期五."
```

### 5.30 intern() 方法

```java
public native String intern();
```

**功能**：

- 返回字符串的规范表示
- 如果常量池中已存在相等的字符串，返回池中的引用
- 否则，将当前字符串添加到常量池并返回引用

```java
// JDK 1.7+ 字符串常量池移到堆中
String s1 = new String("hello");
String s2 = new String("hello");

System.out.println(s1 == s2);           // false（不同对象）
System.out.println(s1.intern() == s2.intern());  // true（intern 后指向同一对象）

String s3 = "hello";  // 字面量，会自动 intern
System.out.println(s1.intern() == s3);  // true

// intern() 的典型应用场景
public class InternDemo {
    public static void main(String[] args) {
        String s1 = new String("a") + new String("b");
        String s2 = "ab";
        System.out.println(s1 == s2);           // false
        System.out.println(s1.intern() == s2);  // true
    }
}
```

## 六、字符串常量池深度解析

### 6.1 常量池的演进

**JDK 1.6 及之前**：

- 字符串常量池位于永久代（PermGen）
- 大小固定，受 JVM 参数 `-XX:PermSize` 和 `-XX:MaxPermSize` 限制
- 容易发生 OutOfMemoryError: PermGen space

**JDK 1.7**：

- 字符串常量池移到堆（Heap）中
- 大小受堆内存限制，更灵活
- 减少了永久代溢出的问题

**JDK 1.8**：

- 移除了永久代，改为元空间（Metaspace）
- 字符串常量池仍在堆中
- 减少了 OOM 的风险

### 6.2 字符串创建与常量池

```java
// 1. 字面量方式 - 使用常量池
String s1 = "hello";  // 先检查常量池，再决定是否创建

// 2. new 方式 - 总是创建新对象
String s2 = new String("hello");  // 可能在常量池创建，也可能在堆创建

// 3. intern() 手动入池
String s3 = s2.intern();
```

### 6.3 字符串拼接与常量池

```java
// 编译时常量拼接 - 使用常量池
String a = "a" + "b";  // 编译时优化为 "ab"
String ab = "ab";
System.out.println(a == ab);  // true

// 运行时拼接 - 不使用常量池
String c = new String("c");
String d = new String("d");
String cd = c + d;  // 运行时使用 StringBuilder，不入池
System.out.println(cd == "cd");  // false

// 编译时常量表达式 - 使用常量池
final String e = "e";
String f = e + "f";  // 编译时确定，使用常量池
System.out.println(f == "ef");  // true
```

### 6.4 常量池的内存结构

```
┌─────────────────────────────────────┐
│             Heap Memory              │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐    │
│  │   String Table (Hashtable)   │    │
│  │   存储字符串引用             │    │
│  │   "hello" → 引用             │    │
│  │   "world" → 引用             │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │   字符串对象（char[]）       │    │
│  │   实际字符内容               │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

### 6.5 intern() 的性能影响

**正面影响**：

- 减少重复字符串的内存占用
- 加速字符串比较（使用 `==` 代替 `equals()`）

**负面影响**：

- 首次 intern 操作可能较慢
- 常量池过大可能影响 GC
- 过度使用可能导致内存膨胀

```java
// intern() 的最佳实践
public class InternBestPractice {
    // 适用于大量重复字符串的场景
    private static final Set<String> DEDUP_SET = new HashSet<>();
    
    public void processStrings(List<String> input) {
        for (String s : input) {
            String interned = s.intern();
            // 使用 interned 进行后续操作
        }
    }
}
```

## 七、字符串不可变性详解

### 7.1 不可变性的实现机制

```java
public final class String {
    private final char value[];  // 1. final 声明，不可重新赋值
    private int hash;            // 2. 缓存哈希值
    
    // 3. 没有暴露修改 value 的方法
    // 4. 所有"修改"操作都返回新对象
}
```

### 7.2 不可变性的五大优势

**1. 线程安全**

```java
// String 可以安全地在多线程间共享
public class ThreadSafeClass {
    private final String sharedData;
    
    public ThreadSafeClass(String data) {
        this.sharedData = data;  // 发布后不会被修改
    }
}
```

**2. 哈希缓存**

```java
// String 的 hashCode 可以安全缓存
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        // ... 计算并缓存
        hash = h;
    }
    return h;
}
// 由于不可变，缓存的哈希值永远有效
```

**3. 安全性**

```java
// 字符串常用于网络连接、文件路径等敏感场景
// 不可变性防止这些值被恶意修改
public void connect(String host, int port) {
    // host 参数不会被方法内部修改
    // 保证了连接的安全性
}
```

**4. 字符串常量池优化**

```java
String s1 = "hello";
String s2 = "hello";
// 由于不可变，s1 和 s2 可以安全地共享同一个对象
```

**5. 缓存对象复用**

```java
// HashMap 的 key 可以安全使用 String
Map<String, Integer> map = new HashMap<>();
map.put("key1", 1);
// 由于 String 不可变，key 不会被修改
```

### 7.3 不可变性的注意事项

**1. 反射可能破坏不可变性**

```java
// 警告：这种操作是不安全的，应该避免
String str = "hello";
Field valueField = String.class.getDeclaredField("value");
valueField.setAccessible(true);
char[] chars = (char[]) valueField.get(str);
chars[0] = 'H';  // 破坏了 String 的不可变性

// 危害：
// - 同一常量池中的其他字符串也可能受影响
// - 导致难以调试的 bug
// - 在新版本 JDK 中可能被禁止
```

**2. 子字符串共享数组的历史问题**

```java
// JDK 7u6 之前
String str = "hello world";
String sub = str.substring(0, 5);
// sub 和 str 共享同一个 char 数组
// 如果 sub 很大，原字符串不会被回收（内存泄漏）

// JDK 7u6 修复后
// substring() 创建新的 char 数组，避免了内存泄漏
```

## 八、常见面试题

### 面试题 1：String 是基本数据类型吗？

**答案**：

不是。String 是引用类型，是 `java.lang.String` 类的实例。Java 的基本数据类型只有 8 种：`byte`、`short`、`int`、`long`、`float`、`double`、`char`、`boolean`。

### 面试题 2：String 的不可变性是如何实现的？

**答案**：

1. 类声明为 `final`，不能被继承
2. 核心字段 `value` 声明为 `private final char[]`
3. 没有提供任何修改 `value` 的方法
4. 所有看似修改的方法（如 `concat()`、`replace()`）都返回新的 String 对象

### 面试题 3：String、StringBuffer、StringBuilder 的区别？

**答案**：

| 特性 | String | StringBuffer | StringBuilder |
|------|--------|--------------|---------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 是 | 是 | 否 |
| 性能 | 低 | 中等 | 高 |
| 适用场景 | 常量字符串 | 多线程环境 | 单线程环境 |

### 面试题 4：String s = new String("abc") 创建了几个对象？

**答案**：

- `"abc"` 字面量：如果常量池中不存在，创建 1 个对象
- `new String("abc")`：总是创建 1 个新对象（堆中）

所以最多创建 2 个对象，最少创建 1 个对象（如果常量池已存在）。

```java
// 示例
String s1 = "abc";  // 1. 常量池中创建 "abc"
String s2 = new String("abc");  // 2. 堆中创建新 String 对象
String s3 = new String("abc");  // 2. 再创建一个新对象

System.out.println(s1 == s2);  // false
System.out.println(s2 == s3);  // false
System.out.println(s1 == s2.intern());  // true
```

### 面试题 5：String 的 hashCode() 为什么选择 31 作为乘数？

**答案**：

1. **质数特性**：31 是质数，可以减少哈希冲突
2. **性能优化**：JVM 可以优化 `31 * h` 为 `(h << 5) - h`
3. **溢出处理**：31 的值不会太大导致整数快速溢出，也不会太小导致分布不均
4. **历史原因**：从 Java 早期就使用这个值，已被广泛采用

### 面试题 6：intern() 方法的作用是什么？

**答案**：

`intern()` 方法用于字符串常量池管理：

- 如果常量池中已存在相等的字符串，返回池中的引用
- 如果不存在，将当前字符串添加到常量池，返回其引用

```java
String s1 = new String("hello");
String s2 = s1.intern();
String s3 = "hello";

System.out.println(s1 == s2);  // false
System.out.println(s2 == s3);  // true
```

### 面试题 7：如何高效地拼接大量字符串？

**答案**：

使用 `StringBuilder`（单线程）或 `StringBuffer`（多线程）：

```java
// 不推荐：每次 + 都会创建新 String 对象
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 效率低
}

// 推荐：使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

### 面试题 8：String 类的常用方法有哪些？

**答案**：

| 分类 | 方法 |
|------|------|
| 长度 | `length()` |
| 访问 | `charAt(int)`, `codePointAt(int)` |
| 比较 | `equals()`, `compareTo()`, `equalsIgnoreCase()` |
| 查找 | `indexOf()`, `lastIndexOf()`, `contains()` |
| 截取 | `substring()`, `subSequence()` |
| 修改 | `replace()`, `trim()`, `toLowerCase()`, `toUpperCase()` |
| 分割 | `split()` |
| 拼接 | `concat()`, `join()` |
| 转换 | `getBytes()`, `toCharArray()`, `valueOf()` |

### 面试题 9：String 有长度限制吗？

**答案**：

有。String 的长度受 `int` 类型最大值限制：

```java
// 理论上最大长度：Integer.MAX_VALUE = 2,147,483,647
// 但实际上受限于可用内存

// 实际限制：
// - 数组长度：value.length 是 int，所以最大约 21 亿
// - 内存限制：一个 char 占 2 字节，21 亿 char 需要约 4GB 内存
// - JVM 限制：通常单个字符串不能超过几 GB

// 实际使用中，String 长度通常限制在几百万字符以内
```

### 面试题 10：如何判断字符串是否为空？

**答案**：

```java
// 方法1：使用 isEmpty()（推荐，JDK 1.6+）
if (str != null && !str.isEmpty()) {
    // 不为空
}

// 方法2：使用 length()
if (str != null && str.length() > 0) {
    // 不为空
}

// 区别：
// isEmpty() 更语义化，效率略高
// length() 更通用，兼容旧版本
```

## 九、实践应用场景

### 9.1 字符串工具类封装

```java
public class StringUtils {
    
    // 判断字符串是否为 null 或空
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
    // 判断字符串是否为 null、空或只包含空白
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
    
    // 安全的字符串比较
    public static boolean equals(String a, String b) {
        return a == b || (a != null && a.equals(b));
    }
    
    // 首字母大写
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            return Character.toUpperCase(firstChar) + str.substring(1);
        }
        return str;
    }
    
    // 截取字符串，超出部分用省略号表示
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }
}
```

### 9.2 字符串缓存策略

```java
public class StringCacheDemo {
    
    // 使用 intern() 实现字符串去重
    public static String deduplicate(String input) {
        return input.intern();
    }
    
    // 使用 StringBuilder 进行高效拼接
    public static String buildList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
```

### 9.3 字符串解析与验证

```java
public class StringParser {
    
    // 解析逗号分隔的值
    public static List<String> parseCSV(String line) {
        return Arrays.asList(line.split(","));
    }
    
    // 解析带引号的 CSV
    public static List<String> parseQuotedCSV(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }
    
    // 验证邮箱格式
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    }
}
```

### 9.4 性能优化实践

```java
public class StringPerformance {
    
    // 预分配 StringBuilder 容量
    public static String buildWithCapacity(int expectedLength) {
        StringBuilder sb = new StringBuilder(expectedLength);
        // ... 填充内容
        return sb.toString();
    }
    
    // 使用 StringBuilder.reverse() 反转字符串
    public static String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }
    
    // 批量字符串拼接（推荐使用 StringJoiner 或 Stream）
    public static String joinWithStream(List<String> items) {
        return items.stream()
                .collect(Collectors.joining(", "));
    }
}
```

## 十、注意事项和陷阱

### 10.1 字符串比较陷阱

```java
// 陷阱1：使用 == 比较字符串内容
String s1 = new String("hello");
String s2 = new String("hello");
System.out.println(s1 == s2);  // false（比较引用）

// 正确做法：使用 equals()
System.out.println(s1.equals(s2));  // true

// 陷阱2：null 安全
String s3 = null;
System.out.println(s3.equals("test"));  // NullPointerException

// 正确做法：使用 Objects.equals() 或常量在前
System.out.println(Objects.equals(s3, "test"));  // false
System.out.println("test".equals(s3));  // false
```

### 10.2 字符串拼接陷阱

```java
// 陷阱：在循环中使用 + 拼接字符串
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 每次循环都创建新的 String 对象
}

// 正确做法：使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

### 10.3 子字符串内存泄漏（历史问题）

```java
// JDK 7u6 之前的潜在问题
String str = "这是一个很长的字符串...".substring(0, 5);

// 修复后（JDK 7u6+）：substring() 创建新数组
// 不再有内存泄漏问题
```

### 10.4 编码处理陷阱

```java
// 陷阱：使用默认编码，可能导致跨平台问题
byte[] bytes = str.getBytes();  // 使用平台默认编码

// 正确做法：明确指定编码
byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
byte[] gbk = str.getBytes("GBK");
```

### 10.5 空格处理陷阱

```java
// trim() 只移除 ASCII 空白
String str = "  Hello  World 　";  // 包含全角空格
System.out.println(str.trim());  // "Hello  World　"（全角空格未移除）

// JDK 11+ 使用 strip() 移除所有 Unicode 空白
// str.strip()  // 移除所有空白

// 旧版本使用正则表达式
str.replaceAll("^\\s+|\\s+$", "");
```

### 10.6 字符串常量池陷阱

```java
// 陷阱：过度使用 intern() 可能导致内存问题
public class InternTrap {
    // 大量动态生成的字符串入池
    String processInput(String input) {
        return input.intern();  // 如果输入量大，可能导致常量池膨胀
    }
}

// 正确做法：谨慎使用 intern()
String useIntern(String input) {
    // 只对确定会重复使用的字符串使用
    if (shouldUsePool(input)) {
        return input.intern();
    }
    return input;
}
```

## 十一、String 相关类的对比

### 11.1 String vs StringBuffer vs StringBuilder

**详细对比**：

```java
// String 不可变
String str = "a";
str = str + "b";  // 创建新对象

// StringBuffer 可变，线程安全
StringBuffer sb = new StringBuffer("a");
sb.append("b");  // 修改同一个对象

// StringBuilder 可变，非线程安全
StringBuilder sbu = new StringBuilder("a");
sbu.append("b");  // 修改同一个对象
```

**性能测试**：

```java
public class PerformanceTest {
    public static void main(String[] args) {
        int iterations = 100000;
        
        // String 拼接 - 最慢
        long start = System.currentTimeMillis();
        String str = "";
        for (int i = 0; i < iterations; i++) {
            str += "a";
        }
        System.out.println("String: " + (System.currentTimeMillis() - start) + "ms");
        
        // StringBuffer - 中等
        start = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < iterations; i++) {
            sb.append("a");
        }
        System.out.println("StringBuffer: " + (System.currentTimeMillis() - start) + "ms");
        
        // StringBuilder - 最快
        start = System.currentTimeMillis();
        StringBuilder sbu = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sbu.append("a");
        }
        System.out.println("StringBuilder: " + (System.currentTimeMillis() - start) + "ms");
    }
}
```

### 11.2 String vs CharSequence

```java
// CharSequence 接口
public interface CharSequence {
    int length();
    char charAt(int index);
    CharSequence subSequence(int start, int end);
    String toString();
}

// String 实现了 CharSequence
String str = "hello";

// CharSequence 的其他实现
StringBuffer sb = new StringBuffer("hello");
StringBuilder sbu = new StringBuilder("hello");

// 可以使用 CharSequence 统一处理
public void process(CharSequence cs) {
    System.out.println(cs.length());
    System.out.println(cs.charAt(0));
}
```

### 11.3 String vs StringTokenizer

```java
// String.split() vs StringTokenizer
String str = "a,b,c,d";

// split() - 使用正则表达式，功能强大但可能较慢
String[] parts1 = str.split(",");
Arrays.stream(parts1).forEach(System.out::println);

// StringTokenizer - 专为分割设计，更高效
StringTokenizer tokenizer = new StringTokenizer(str, ",");
while (tokenizer.hasMoreTokens()) {
    System.out.println(tokenizer.nextToken());
}
```

## 十二、总结

### 12.1 核心要点

1. **不可变性**：String 是不可变类，这是其设计的核心
2. **字符串常量池**：JVM 维护的字符串常量池优化了字符串的内存使用
3. **字符编码**：UTF-16 编码，支持完整 Unicode
4. **线程安全**：不可变性使 String 天生线程安全
5. **性能优化**：缓存哈希值、共享数组、内部优化构造方法

### 12.2 学习建议

1. **理解不可变性**：深入理解 String 不可变性的实现原理和优势
2. **掌握常用方法**：熟悉 String 的常用方法及其时间复杂度
3. **了解常量池**：理解字符串常量池的工作原理和 intern() 方法
4. **性能意识**：了解不同字符串操作的性能差异
5. **编码处理**：掌握字符编码和解码的正确方法

### 12.3 进阶学习

- 学习正则表达式的高级用法
- 研究 Pattern 和 Matcher 类的实现
- 了解 StringJoiner 和 Stream API 的字符串操作
- 探索 Java 9+ 的 String 实现优化（Compact Strings）

## 参考资料

- JDK 8 官方文档：https://docs.oracle.com/javase/8/docs/api/java/lang/String.html
- 《Java 核心技术 卷 I》
- 《Effective Java》- Joshua Bloch
- JLS（Java Language Specification）- String Literals

---

**笔记创建时间**：2025-12-26

**最后更新时间**：2025-12-26

**版本**：1.0
