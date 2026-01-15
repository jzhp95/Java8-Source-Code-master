# Integer 类源码深度解析

## 一、类的概述

### 1.1 基本信息

`Integer` 类是 Java 语言中用于包装基本类型 `int` 的包装类，属于 Java 核心类库中最常用的类之一。在 Java 程序中，`Integer` 对象封装了一个 `int` 类型的值，提供了将 `int` 转换为 `String` 以及将 `String` 转换为 `int` 的多种方法，同时包含了许多与 `int` 类型相关的常量和实用方法。

**源码位置**：`src/main/jdk8/java/lang/Integer.java`

**类的声明**：

```java
public final class Integer extends Number implements Comparable<Integer>
```

**类的定位**：

- `Integer` 是 Java 包装类体系的代表类之一，展示了享元模式在 Java 标准库中的典型应用
- 通过自动装箱（Autoboxing）和拆箱（Unboxing）机制，`int` 与 `Integer` 之间可以无缝转换
- `Integer` 类实现了 `Number` 抽象类，支持转换为各种数值类型
- 实现了 `Comparable<Integer>` 接口，支持排序操作
- 被声明为 `final`，不可被继承

**类的职责**：

`Integer` 类承担了以下核心职责：

- 提供 `int` 基本类型与对象之间的桥梁转换
- 实现 `-128` 到 `127` 之间的整数缓存，减少内存占用
- 提供多种进制转换方法（二进制、八进制、十进制、十六进制）
- 提供字符串解析方法，支持多种进制
- 实现位操作相关方法，支持底层位级别的操作
- 支持无符号整数的计算和比较

### 1.2 设计理念

`Integer` 类的设计体现了以下核心理念：

**1. 享元模式的应用**

`Integer` 类最著名的设计就是其整数缓存机制。在 JVM 启动时，会预先创建 `-128` 到 `127` 之间的 `Integer` 对象并缓存起来，当程序使用这个范围内的整数时，直接返回缓存的对象引用，而不是创建新对象。这种设计大大减少了内存占用和对象创建的开销。

```java
Integer a = 100;
Integer b = 100;
System.out.println(a == b);  // true，指向同一个缓存对象
```

**2. 性能优化的极致追求**

`Integer` 类中的许多方法都经过精心优化，特别是字符串转换方法。例如，`toString()` 方法使用了"不变除法乘法技巧"（Invariant Division by Multiplication Trick），避免了除法操作，显著提升了性能。

**3. 兼容性与扩展性**

`Integer` 类保持了向后兼容性，同时在 Java 8 中引入了无符号整数操作的支持，扩展了 `int` 类型的应用范围。

**4. 安全性考虑**

`Integer` 类的字段 `value` 被声明为 `private final`，确保包装的值一旦创建就不能被修改。同时，缓存池的大小可以通过 JVM 参数 `AutoBoxCacheMax` 进行配置，提供了灵活性。

### 1.3 版本信息

- **JDK 版本**：JDK 1.0
- **作者**：Lee Boynton, Arthur van Hoff, Josh Bloch, Joseph D. Darcy
- **重大变更**：
  - JDK 1.1：添加了 `TYPE` 字段
  - JDK 1.2：添加了 `compareTo()` 方法
  - JDK 1.5：引入自动装箱（Autoboxing）和 `valueOf()` 方法，添加位操作方法
  - JDK 1.7：添加了 `compare()` 静态方法
  - JDK 1.8：添加了无符号整数相关方法（`compareUnsigned`、`divideUnsigned`、`remainderUnsigned` 等）

### 1.4 与其他包装类的关系

`Integer` 是 `Number` 类的子类，与其他数值包装类（`Byte`、`Short`、`Long`、`Float`、`Double`）共享相似的设计模式，但每种包装类都有其特定的缓存范围：

| 包装类 | 缓存范围 | 默认最大值 |
|--------|----------|------------|
| Byte | -128 到 127 | 127（固定） |
| Short | -128 到 127 | 127（可配置） |
| Integer | -128 到 127 | 127（可配置到 Integer.MAX_VALUE） |
| Long | -128 到 127 | 127（可配置） |
| Character | 0 到 127 | 127（固定） |
| Float | 无缓存 | 不适用 |
| Double | 无缓存 | 不适用 |

## 二、类的继承结构

### 2.1 UML 类图

```
┌─────────────────────────────────────────────────────────────┐
│                      java.lang.Integer                      │
├─────────────────────────────────────────────────────────────┤
│ - value: int                                                │
│ + MIN_VALUE: int = -2147483648                              │
│ + MAX_VALUE: int = 2147483647                               │
│ + SIZE: int = 32                                            │
│ + BYTES: int = 4                                            │
│ + TYPE: Class<Integer>                                      │
├─────────────────────────────────────────────────────────────┤
│ + Integer(int value)                                        │
│ + Integer(String s) throws NumberFormatException            │
├─────────────────────────────────────────────────────────────┤
│ + intValue(): int                                           │
│ + byteValue(): byte                                         │
│ + shortValue(): short                                       │
│ + longValue(): long                                         │
│ + floatValue(): float                                       │
│ + doubleValue(): double                                     │
├─────────────────────────────────────────────────────────────┤
│ + toString(): String                                        │
│ + toString(int i): String                                   │
│ + toString(int i, int radix): String                        │
│ + parseInt(String s): int                                   │
│ + parseInt(String s, int radix): int                        │
│ + valueOf(String s): Integer                                │
│ + valueOf(int i): Integer                                   │
├─────────────────────────────────────────────────────────────┤
│ + hashCode(): int                                           │
│ + hashCode(int value): int                                  │
│ + equals(Object obj): boolean                               │
│ + compareTo(Integer anotherInteger): int                    │
│ + compare(int x, int y): int                                │
├─────────────────────────────────────────────────────────────┤
│ ^ extends Number                                            │
│ ^ implements Comparable<Integer>                            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 继承关系图示

```
java.lang.Object
    │
    └── java.lang.Number
            │
            ├── java.lang.Byte
            ├── java.lang.Short
            ├── java.lang.Integer  ← 重点学习
            ├── java.lang.Long
            ├── java.lang.Float
            └── java.lang.Double
```

### 2.3 接口实现说明

**Comparable<Integer> 接口**：

`Integer` 实现了 `Comparable<Integer>` 接口，提供了自然排序的能力：

```java
public int compareTo(Integer anotherInteger) {
    return compare(this.value, anotherInteger.value);
}

public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
}
```

**Number 抽象类**：

`Number` 抽象类定义了将包装类型转换为各种基本类型的方法：

```java
public abstract class Number implements java.io.Serializable {
    public abstract int intValue();
    public abstract long longValue();
    public abstract float floatValue();
    public abstract double doubleValue();
    public abstract byte byteValue();
    public abstract short shortValue();
}
```

## 三、核心字段解析

### 3.1 静态常量

```java
public static final int MIN_VALUE = 0x80000000;
public static final int MAX_VALUE = 0x7fffffff;
```

**MIN_VALUE（最小值）**：

- 值：`0x80000000`（十进制：-2147483648）
- 表示 `int` 类型能够表示的最小值
- 二进制表示：`10000000 00000000 00000000 00000000`
- 负数最小值的特殊处理：`Integer.MIN_VALUE == -2147483648`

```java
// MIN_VALUE 的特殊性
System.out.println(Integer.MIN_VALUE);        // -2147483648
System.out.println(-Integer.MIN_VALUE);       // -2147483648（仍然是负数，溢出）
System.out.println(Math.abs(Integer.MIN_VALUE)); // -2147483648（溢出）
System.out.println(Integer.MIN_VALUE + 1);    // -2147483647
System.out.println(Integer.MIN_VALUE - 1);    // 2147483647（正溢出）
```

**MAX_VALUE（最大值）**：

- 值：`0x7fffffff`（十进制：2147483647）
- 表示 `int` 类型能够表示的最大值
- 二进制表示：`01111111 11111111 11111111 11111111`
- 溢出处理：`MAX_VALUE + 1 = MIN_VALUE`（绕回）

```java
// MAX_VALUE 的溢出特性
System.out.println(Integer.MAX_VALUE);        // 2147483647
System.out.println(Integer.MAX_VALUE + 1);    // -2147483648
System.out.println(Integer.MAX_VALUE + 2);    // -2147483647
```

### 3.2 类型标识常量

```java
@SuppressWarnings("unchecked")
public static final Class<Integer> TYPE = (Class<Integer>) Class.getPrimitiveClass("int");
```

**设计意图**：

- `TYPE` 字段用于获取 `int` 基本类型的 `Class` 对象
- 主要用于反射操作和泛型处理
- 类似于 `int.class` 的功能

```java
// TYPE 的使用场景
Class<?> intType = Integer.TYPE;
System.out.println(intType == int.class);  // true

// 在反射中的应用
Method[] methods = Integer.TYPE.getMethods();
Field[] fields = Integer.TYPE.getFields();
```

### 3.3 字符映射表

```java
final static char[] digits = {
    '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9' ,
    'a' , 'b' , 'c' , 'd' , 'e' , 'f' , 'g' , 'h' , 'i' , 'j' ,
    'k' , 'l' , 'm' , 'n' , 'o' , 'p' , 'q' , 'r' , 's' , 't' ,
    'u' , 'v' , 'w' , 'x' , 'y' , 'z'
};
```

**设计意图**：

- 用于进制转换的数字字符表
- 包含 36 个字符（0-9, a-z）
- 支持从二进制（radix 2）到三十六进制（radix 36）的转换

```java
// digits 数组的索引对应数值
// digits[0] = '0', digits[1] = '1', ..., digits[35] = 'z'

// 使用示例：将 255 转换为十六进制
int value = 255;
int hexDigit = (value >>> 4) & 0xF;
char hexChar = Integer.digits[hexDigit];
// hexChar = 'f'
```

### 3.4 数字查找表

```java
final static char[] DigitTens = {
    '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
    '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
    '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
    '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
    '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
    '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
    '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
    '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
    '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
    '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
};

final static char[] DigitOnes = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
};
```

**设计意图**：

- `DigitTens[n]` 返回十位数
- `DigitOnes[n]` 返回个位数
- 用于 `toString()` 方法的快速转换
- 避免除法和取模运算，提升性能

```java
// 使用示例：将数字 42 转换为字符
int n = 42;
char tensChar = DigitTens[n];  // '4'
char onesChar = DigitOnes[n];  // '2'
```

### 3.5 长度查找表

```java
final static int[] sizeTable = {
    9, 99, 999, 9999, 99999, 999999, 9999999,
    99999999, 999999999, Integer.MAX_VALUE
};
```

**设计意图**：

- 用于确定整数转换为字符串后的长度
- 通过二分查找或顺序查找确定位数
- 避免反复计算或动态分配过大数组

```java
// stringSize 方法实现
static int stringSize(int x) {
    for (int i = 0; ; i++)
        if (x <= sizeTable[i])
            return i + 1;
}

// 使用示例
System.out.println(stringSize(0));       // 1
System.out.println(stringSize(9));       // 1
System.out.println(stringSize(10));      // 2
System.out.println(stringSize(999));     // 3
System.out.println(stringSize(1000));    // 4
```

### 3.6 实例字段

```java
private final int value;
```

**设计意图**：

- 存储 Integer 对象包装的实际 int 值
- 被声明为 `final`，确保不可变性
- 是 Integer 对象的核心数据

**特点**：

- 不可修改：一旦创建，值不能改变
- 内存占用：int 类型占用 4 字节，加上对象开销（约 16 字节），每个 Integer 对象约 20 字节

## 四、IntegerCache 深度解析

### 4.1 缓存机制概述

`IntegerCache` 是 `Integer` 类的内部静态类，实现了著名的整数缓存机制：

```java
private static class IntegerCache {
    static final int low = -128;
    static final int high;
    static final Integer cache[];

    static {
        int h = 127;
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                h = Math.min(i, Integer.MAX_VALUE - (-low) - 1);
            } catch(NumberFormatException nfe) {
            }
        }
        high = h;
        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);
    }

    private IntegerCache() {}
}
```

### 4.2 缓存范围

**默认范围**：`-128` 到 `127`

- `-128` 是固定的下界
- `127` 是默认的上界，但可以通过 JVM 参数配置

**可配置范围**：

```bash
# 通过 JVM 参数配置缓存最大值
java -XX:AutoBoxCacheMax=256 YourClass

# 或者通过系统属性配置
java -Djava.lang.Integer.IntegerCache.high=256 YourClass
```

```java
// 验证配置是否生效
System.out.println(Integer.MAX_VALUE);  // 查看是否可以配置
```

### 4.3 缓存工作原理

**缓存初始化**：

```java
// 1. 创建缓存数组
cache = new Integer[(high - low) + 1];

// 2. 填充缓存
for(int k = 0; k < cache.length; k++)
    cache[k] = new Integer(j++);

// 3. 索引计算：value + 128 = 数组索引
return IntegerCache.cache[i + (-IntegerCache.low)];
// 例如：i = 100，则 index = 100 + 128 = 228
```

**valueOf 方法实现**：

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

### 4.4 缓存的内存结构

```
┌─────────────────────────────────────────────────┐
│           IntegerCache 内存结构                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  cache[] 数组:                                  │
│  ┌───────────────────────────────────────┐     │
│  │ 索引 0    → Integer(-128)             │     │
│  │ 索引 1    → Integer(-127)             │     │
│  │ ...       → ...                       │     │
│  │ 索引 127  → Integer(-1)               │     │
│  │ 索引 128  → Integer(0)                │     │
│  │ 索引 129  → Integer(1)                │     │
│  │ ...       → ...                       │     │
│  │ 索引 255  → Integer(127)              │     │
│  └───────────────────────────────────────┘     │
│                                                 │
│  总共: (high - low + 1) 个对象                  │
│  默认: 256 个对象（-128 到 127）                │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 4.5 缓存的作用

**1. 减少内存占用**：

```java
// 不使用缓存：每次都创建新对象
Integer a = new Integer(100);
Integer b = new Integer(100);
System.out.println(a == b);  // false（两个不同对象）

// 使用缓存：复用已有对象
Integer c = 100;  // 自动装箱
Integer d = 100;
System.out.println(c == d);  // true（同一个缓存对象）
```

**2. 提升性能**：

- 避免了对象创建的开销
- 减少了 GC 压力
- 加快了比较操作（引用比较 vs 内容比较）

**3. 保证语义一致性**：

```java
// 享元模式确保相等的值有相同的引用
Integer a = 127;
Integer b = 127;
System.out.println(a == b);  // true

Integer c = 128;
Integer d = 128;
System.out.println(c == d);  // false（超出缓存范围）
```

### 4.6 自动装箱与缓存

```java
// 自动装箱调用 valueOf 方法
Integer a = 100;  // 编译后：Integer.valueOf(100)

// 编译器优化
// 自动装箱：Integer.valueOf(100)
// 字面量：先在常量池查找，找不到则放入常量池
```

**自动装箱的边界情况**：

```java
// 边界情况 1：方法参数
public void test(Integer i) {
    System.out.println(i);
}
test(100);  // 使用缓存
test(128);  // 不使用缓存

// 边界情况 2：循环
for (int i = 0; i < 200; i++) {
    Integer boxed = i;  // 前 128 个使用缓存，后 72 个创建新对象
}

// 边界情况 3：计算表达式
Integer a = 1 + 1;  // 编译后：Integer.valueOf(2)
Integer b = 1000 + 1000;  // 结果超出缓存范围
```

## 五、构造方法分析

### 5.1 int 构造方法

```java
public Integer(int value) {
    this.value = value;
}
```

**特点**：

- 直接存储传入的 int 值
- 不使用缓存，总是创建新对象
- 简单高效

```java
// 总是创建新对象
Integer a = new Integer(100);
Integer b = new Integer(100);
System.out.println(a == b);  // false

// 与 valueOf 的区别
Integer c = Integer.valueOf(100);  // 可能返回缓存对象
```

### 5.2 String 构造方法

```java
public Integer(String s) throws NumberFormatException {
    this.value = parseInt(s, 10);
}
```

**特点**：

- 使用 `parseInt(s, 10)` 将字符串解析为 int
- 解析失败抛出 `NumberFormatException`
- 支持正负号和十进制数字

```java
// 正确用法
Integer a = new Integer("123");
Integer b = new Integer("-456");

// 错误用法
Integer c = new Integer("12.34");  // 抛出 NumberFormatException
Integer d = new Integer("abc");    // 抛出 NumberFormatException
Integer e = new Integer("");       // 抛出 NumberFormatException
```

**构造方法 vs valueOf**：

```java
// 构造方法：总是创建新对象
Integer constructor = new Integer("100");

// valueOf(String)：先解析，再决定是否使用缓存
Integer valueOf = Integer.valueOf("100");

// 对于 "100"，两者结果不同
System.out.println(constructor == valueOf);  // false
System.out.println(constructor.equals(valueOf));  // true
```

## 六、核心方法详解

### 6.1 类型转换方法

**intValue 系列方法**：

```java
public byte byteValue() {
    return (byte)value;
}

public short shortValue() {
    return (short)value;
}

public int intValue() {
    return value;
}

public long longValue() {
    return (long)value;
}

public float floatValue() {
    return (float)value;
}

public double doubleValue() {
    return (double)value;
}
```

**特点**：

- 继承自 `Number` 抽象类
- 宽化转换（Widening Conversion）自动进行，无精度损失
- 窄化转换（Narrowing Conversion）可能有精度损失

```java
Integer i = 1000;

byte b = i.byteValue();    // 1000 % 256 = 232（溢出）
short s = i.shortValue();  // 1000
int i2 = i.intValue();     // 1000
long l = i.longValue();    // 1000
float f = i.floatValue();  // 1000.0
double d = i.doubleValue(); // 1000.0

// 大数值的窄化转换
Integer big = 2147483647;  // Integer.MAX_VALUE
byte overflow = big.byteValue();  // -1
```

**toString 方法**：

```java
public String toString() {
    return toString(value);
}

public static String toString(int i) {
    if (i == Integer.MIN_VALUE)
        return "-2147483648";
    int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
    char[] buf = new char[size];
    getChars(i, size, buf);
    return new String(buf, true);
}
```

**实现优化**：

- 特殊处理 `MIN_VALUE`（无法取负）
- 预计算字符串长度
- 使用 `getChars` 方法进行高效转换

```java
// toString 性能优化测试
long start = System.nanoTime();
for (int i = 0; i < 100000; i++) {
    Integer.toString(i);
}
long end = System.nanoTime();
System.out.println("Time: " + (end - start) / 1000 + " microseconds");
```

### 6.2 进制转换方法

**toString(int i, int radix)**：

```java
public static String toString(int i, int radix) {
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        radix = 10;

    if (radix == 10) {
        return toString(i);
    }

    char buf[] = new char[33];
    boolean negative = (i < 0);
    int charPos = 32;

    if (!negative) {
        i = -i;
    }

    while (i <= -radix) {
        buf[charPos--] = digits[-(i % radix)];
        i = i / radix;
    }
    buf[charPos] = digits[-i];

    if (negative) {
        buf[--charPos] = '-';
    }

    return new String(buf, charPos, (33 - charPos));
}
```

**特点**：

- 支持 2 到 36 进制转换
- radix 超出范围时默认使用 10
- 内部使用负数计算避免溢出
- 使用 `digits` 字符数组进行映射

```java
// 进制转换示例
System.out.println(Integer.toString(255, 2));    // "11111111"
System.out.println(Integer.toString(255, 8));    // "377"
System.out.println(Integer.toString(255, 10));   // "255"
System.out.println(Integer.toString(255, 16));   // "ff"
System.out.println(Integer.toString(255, 36));   // "73"
```

**toHexString、toOctalString、toBinaryString**：

```java
public static String toHexString(int i) {
    return toUnsignedString0(i, 4);
}

public static String toOctalString(int i) {
    return toUnsignedString0(i, 3);
}

public static String toBinaryString(int i) {
    return toUnsignedString0(i, 1);
}
```

**特点**：

- `toHexString`：十六进制，无符号
- `toOctalString`：八进制，无符号
- `toBinaryString`：二进制，无符号

```java
// 便捷方法示例
System.out.println(Integer.toHexString(255));   // "ff"
System.out.println(Integer.toOctalString(255)); // "377"
System.out.println(Integer.toBinaryString(255)); // "11111111"
```

### 6.3 字符串解析方法

**parseInt(String s)**：

```java
public static int parseInt(String s, int radix) throws NumberFormatException {
    if (s == null) {
        throw new NumberFormatException("null");
    }

    if (radix < Character.MIN_RADIX) {
        throw new NumberFormatException("radix " + radix +
                                        " less than Character.MIN_RADIX");
    }

    if (radix > Character.MAX_RADIX) {
        throw new NumberFormatException("radix " + radix +
                                        " greater than Character.MAX_RADIX");
    }

    int result = 0;
    boolean negative = false;
    int i = 0, len = s.length();
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
        char firstChar = s.charAt(0);
        if (firstChar < '0') {  // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
            } else if (firstChar != '+')
                throw NumberFormatException.forInputString(s);

            if (len == 1)  // Cannot have lone "+" or "-"
                throw NumberFormatException.forInputString(s);
            i++;
        }
        multmin = limit / radix;
        while (i < len) {
            digit = Character.digit(s.charAt(i++), radix);
            if (digit < 0) {
                throw NumberFormatException.forInputString(s);
            }
            if (result < multmin) {
                throw NumberFormatException.forInputString(s);
            }
            result *= radix;
            if (result < limit + digit) {
                throw NumberFormatException.forInputString(s);
            }
            result -= digit;
        }
    } else {
        throw NumberFormatException.forInputString(s);
    }
    return negative ? result : -result;
}
```

**解析过程**：

1. **null 检查**：抛出 `NumberFormatException`
2. **radix 验证**：确保进制在 2 到 36 之间
3. **符号处理**：识别正负号
4. **累加计算**：使用负数累加避免溢出
5. **溢出检测**：防止越界

**优化策略**：

- 使用负数计算避免 `MIN_VALUE` 溢出问题
- 提前检测溢出条件
- 使用 `Character.digit()` 进行字符验证

```java
// parseInt 使用示例
int a = Integer.parseInt("123");      // 十进制
int b = Integer.parseInt("11111111", 2);  // 二进制
int c = Integer.parseInt("ff", 16);   // 十六进制
int d = Integer.parseInt("377", 8);   // 八进制

// 异常处理
try {
    int invalid = Integer.parseInt("abc");
} catch (NumberFormatException e) {
    System.out.println("Invalid number: " + e.getMessage());
}
```

**parseUnsignedInt(String s, int radix)**：

```java
public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
    if (s == null) {
        throw new NumberFormatException("null");
    }

    int len = s.length();
    if (len > 0) {
        char firstChar = s.charAt(0);
        if (firstChar == '-') {
            throw new NumberFormatException(
                String.format("Illegal leading minus sign on unsigned string %s.", s));
        } else {
            if (len <= 5 || (radix == 10 && len <= 9)) {
                return parseInt(s, radix);
            } else {
                long ell = Long.parseLong(s, radix);
                if ((ell & 0xffff_ffff_0000_0000L) == 0) {
                    return (int) ell;
                } else {
                    throw new NumberFormatException(
                        String.format("String value %s exceeds range of unsigned int.", s));
                }
            }
        }
    } else {
        throw NumberFormatException.forInputString(s);
    }
}
```

**特点**：

- 支持无符号整数解析
- 最大支持解析到 4294967295（2^32 - 1）
- 使用 `long` 类型进行中间计算

```java
// 无符号解析示例
int maxUnsigned = Integer.parseUnsignedInt("4294967295");  // 2^32 - 1
int overflow = Integer.parseUnsignedInt("4294967296");     // 抛出异常
```

### 6.4 valueOf 方法

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}

public static Integer valueOf(String s) throws NumberFormatException {
    return Integer.valueOf(parseInt(s, 10));
}

public static Integer valueOf(String s, int radix) throws NumberFormatException {
    return Integer.valueOf(parseInt(s, radix));
}
```

**与 new Integer 的区别**：

```java
// new Integer：总是创建新对象
Integer a = new Integer(100);

// valueOf(i)：可能返回缓存对象
Integer b = Integer.valueOf(100);

// 自动装箱：等价于 valueOf
Integer c = 100;

// 性能对比
long start = System.nanoTime();
for (int i = 0; i < 100000; i++) {
    Integer n = new Integer(i);
}
long end = System.nanoTime();
System.out.println("new: " + (end - start) / 1000 + " microseconds");

start = System.nanoTime();
for (int i = 0; i < 100000; i++) {
    Integer v = Integer.valueOf(i);
}
end = System.nanoTime();
System.out.println("valueOf: " + (end - start) / 1000 + " microseconds");
```

### 6.5 比较方法

**compareTo 方法**：

```java
public int compareTo(Integer anotherInteger) {
    return compare(this.value, anotherInteger.value);
}

public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
}
```

**特点**：

- 实现了 `Comparable<Integer>` 接口
- 支持自然排序
- 返回值：负数、零、正数

```java
// compareTo 使用示例
Integer a = 10;
Integer b = 20;
System.out.println(a.compareTo(b));   // -1
System.out.println(b.compareTo(a));   // 1
System.out.println(a.compareTo(a));   // 0

// 在集合中使用
List<Integer> list = Arrays.asList(3, 1, 2);
Collections.sort(list);
System.out.println(list);  // [1, 2, 3]
```

**compareUnsigned 方法**：

```java
public static int compareUnsigned(int x, int y) {
    return compare(x + MIN_VALUE, y + MIN_VALUE);
}
```

**特点**：

- 将两个有符号整数当作无符号整数比较
- 负数通过加上 `MIN_VALUE` 的绝对值转换为正数

```java
// 无符号比较示例
int a = -1;    // 0xFFFFFFFF
int b = 1;     // 0x00000001

// 有符号比较
System.out.println(Integer.compare(a, b));      // 1（-1 > 1）

// 无符号比较
System.out.println(Integer.compareUnsigned(a, b));  // 1（0xFFFFFFFF > 0x00000001）
```

### 6.6 hashCode 方法

```java
@Override
public int hashCode() {
    return Integer.hashCode(value);
}

public static int hashCode(int value) {
    return value;
}
```

**特点**：

- `Integer` 的哈希码就是其包装的值本身
- 简单高效，无需计算
- 保持与 `equals` 的一致性

```java
// hashCode 验证
Integer i = 100;
System.out.println(i.hashCode());  // 100
System.out.println(i.equals(100));  // true
System.out.println(i.hashCode() == 100);  // true
```

### 6.7 equals 方法

```java
public boolean equals(Object obj) {
    if (obj instanceof Integer) {
        return value == ((Integer)obj).intValue();
    }
    return false;
}
```

**特点**：

- 只接受 `Integer` 类型的比较
- 比较包装的 int 值
- 不进行类型转换，避免 ClassCastException

```java
// equals 使用示例
Integer a = 100;
Integer b = 100;
Integer c = new Integer(100);
Integer d = 200;

System.out.println(a.equals(b));   // true
System.out.println(a.equals(c));   // true
System.out.println(a.equals(d));   // false
System.out.println(a.equals("100")); // false（类型不同）
```

### 6.8 位操作方法

**highestOneBit 方法**：

```java
public static int highestOneBit(int i) {
    i |= (i >>  1);
    i |= (i >>  2);
    i |= (i >>  4);
    i |= (i >>  8);
    i |= (i >> 16);
    return i - (i >>> 1);
}
```

**原理**：

- 通过位移将最高位的 1 传播到所有低位
- 然后减去移位后的值，保留最高位的 1

```java
// 示例
System.out.println(Integer.highestOneBit(0b00101000));  // 0b00100000
System.out.println(Integer.highestOneBit(0));           // 0
```

**lowestOneBit 方法**：

```java
public static int lowestOneBit(int i) {
    return i & -i;
}
```

**原理**：

- 使用补码特性：`n & -n` 保留最低位的 1

```java
// 示例
System.out.println(Integer.lowestOneBit(0b00101000));  // 0b00001000
System.out.println(Integer.lowestOneBit(0));           // 0
```

**numberOfLeadingZeros 方法**：

```java
public static int numberOfLeadingZeros(int i) {
    if (i == 0)
        return 32;
    int n = 1;
    if (i >>> 16 == 0) { n += 16; i <<= 16; }
    if (i >>> 24 == 0) { n +=  8; i <<=  8; }
    if (i >>> 28 == 0) { n +=  4; i <<=  4; }
    if (i >>> 30 == 0) { n +=  2; i <<=  2; }
    n -= i >>> 31;
    return n;
}
```

**计算前导零的数量**：

```java
System.out.println(Integer.numberOfLeadingZeros(0b00001000_00000000_00000000_00000000));  // 27
System.out.println(Integer.numberOfLeadingZeros(1));  // 31
System.out.println(Integer.numberOfLeadingZeros(0));  // 32
```

**numberOfTrailingZeros 方法**：

```java
public static int numberOfTrailingZeros(int i) {
    if (i == 0) return 32;
    int n = 31;
    y = i <<16; if (y != 0) { n = n -16; i = y; }
    y = i << 8; if (y != 0) { n = n - 8; i = y; }
    y = i << 4; if (y != 0) { n = n - 4; i = y; }
    y = i << 2; if (y != 0) { n = n - 2; i = y; }
    return n - ((i << 1) >>> 31);
}
```

**计算尾随零的数量**：

```java
System.out.println(Integer.numberOfTrailingZeros(0b00000000_00000000_00000000_00001000));  // 3
System.out.println(Integer.numberOfTrailingZeros(8));  // 3
System.out.println(Integer.numberOfTrailingZeros(0));  // 32
```

**bitCount 方法**：

```java
public static int bitCount(int i) {
    i = i - ((i >>> 1) & 0x55555555);
    i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
    i = (i + (i >>> 4)) & 0x0f0f0f0f;
    i = i + (i >>> 8);
    i = i + (i >>> 16);
    return i & 0x3f;
}
```

**计算二进制中 1 的个数（人口计数）**：

```java
System.out.println(Integer.bitCount(0b1111));  // 4
System.out.println(Integer.bitCount(0b1010));  // 2
System.out.println(Integer.bitCount(0));       // 0
System.out.println(Integer.bitCount(-1));      // 32（所有位都是 1）
```

### 6.9 无符号运算方法

**toUnsignedLong 方法**：

```java
public static long toUnsignedLong(int x) {
    return ((long) x) & 0xffffffffL;
}
```

**特点**：

- 将 int 转换为 long
- 高 32 位补 0，低 32 位保持原值
- 负数转换为大正数

```java
int negative = -1;
long unsigned = Integer.toUnsignedLong(negative);
System.out.println(unsigned);  // 4294967295（2^32 - 1）
```

**divideUnsigned 和 remainderUnsigned 方法**：

```java
public static int divideUnsigned(int dividend, int divisor) {
    return (int)(toUnsignedLong(dividend) / toUnsignedLong(divisor));
}

public static int remainderUnsigned(int dividend, int divisor) {
    return (int)(toUnsignedLong(dividend) % toUnsignedLong(divisor));
}
```

**无符号除法和取模**：

```java
int a = -1;
int b = 2;

// 有符号除法
System.out.println(a / b);  // 0（-1 / 2 = 0）

// 无符号除法
System.out.println(Integer.divideUnsigned(a, b));  // 2147483647（2^31 - 1）
```

### 6.10 其他实用方法

**decode 方法**：

```java
public static Integer decode(String nm) throws NumberFormatException {
    int radix = 10;
    int index = 0;
    boolean negative = false;
    Integer result;

    if (nm.length() == 0)
        throw new NumberFormatException("Zero length string");
    char firstChar = nm.charAt(0);
    if (firstChar == '-') {
        negative = true;
        index++;
    } else if (firstChar == '+')
        index++;

    if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
        index += 2;
        radix = 16;
    }
    else if (nm.startsWith("#", index)) {
        index++;
        radix = 16;
    }
    else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
        index++;
        radix = 8;
    }

    if (nm.startsWith("-", index) || nm.startsWith("+", index))
        throw new NumberFormatException("Sign character in wrong position");

    try {
        result = Integer.valueOf(nm.substring(index), radix);
        result = negative ? Integer.valueOf(-result.intValue()) : result;
    } catch (NumberFormatException e) {
        String constant = negative ? ("-" + nm.substring(index))
                                   : nm.substring(index);
        result = Integer.valueOf(constant, radix);
    }
    return result;
}
```

**支持多种前缀的解码**：

```java
// 十进制
Integer a = Integer.decode("42");      // 42

// 十六进制
Integer b = Integer.decode("0x2A");    // 42
Integer c = Integer.decode("#2A");     // 42

// 八进制
Integer d = Integer.decode("052");     // 42

// 带符号
Integer e = Integer.decode("-2A");     // -42
Integer f = Integer.decode("+2A");     // 42
```

**getInteger 方法**：

```java
public static Integer getInteger(String nm) {
    return getInteger(nm, null);
}

public static Integer getInteger(String nm, int val) {
    Integer result = getInteger(nm, null);
    return (result == null) ? Integer.valueOf(val) : result;
}

public static Integer getInteger(String nm, Integer val) {
    String v = null;
    try {
        v = System.getProperty(nm);
    } catch (IllegalArgumentException | NullPointerException e) {
    }
    if (v != null) {
        try {
            return Integer.decode(v);
        } catch (NumberFormatException e) {
        }
    }
    return val;
}
```

**从系统属性获取整数值**：

```java
// 设置系统属性
System.setProperty("app.maxConnections", "100");

// 读取系统属性
Integer maxConn = Integer.getInteger("app.maxConnections", 50);
System.out.println(maxConn);  // 100
```

## 七、自动装箱与拆箱详解

### 7.1 自动装箱机制

**概念**：自动装箱（Autoboxing）是指 Java 编译器自动将基本类型转换为对应的包装类的过程。

**实现原理**：

```java
// Java 源代码
Integer a = 100;

// 编译后的等效代码
Integer a = Integer.valueOf(100);
```

**装箱调用链**：

```java
// 赋值语句
Integer a = 100;

// 字节码等效
INVOKESTATIC Integer.valueOf(I)Ljava/lang/Integer;
```

### 7.2 自动拆箱机制

**概念**：自动拆箱（Unboxing）是指 Java 编译器自动将包装类转换为对应的基本类型的过程。

**实现原理**：

```java
// Java 源代码
Integer a = 100;
int b = a;

// 编译后的等效代码
int b = a.intValue();
```

**拆箱调用链**：

```java
// 运算表达式
Integer a = 100;
Integer b = 200;
int sum = a + b;

// 字节码等效
ALOAD a
INVOKEVIRTUAL Integer.intValue()I
ALOAD b
INVOKEVIRTUAL Integer.intValue()I
IADD
```

### 7.3 装箱与拆箱的时机

**装箱时机**：

1. 赋值给包装类变量
2. 作为方法参数传递（方法接收包装类）
3. 集合操作（Collection 只接受对象）
4. 三元运算符

```java
// 赋值装箱
Integer a = 100;

// 方法参数装箱
public void process(Integer num) { }
process(100);

// 集合装箱
List<Integer> list = new ArrayList<>();
list.add(100);  // 自动装箱

// 三元运算符装箱
Boolean flag = true;
Integer result = flag ? 100 : 200;
```

**拆箱时机**：

1. 赋值给基本类型变量
2. 作为方法参数传递（方法接收基本类型）
3. 算术运算
4. 比较运算

```java
// 赋值拆箱
Integer a = 100;
int b = a;

// 方法参数拆箱
public int add(int a, int b) { return a + b; }
add(new Integer(10), new Integer(20));

// 算术运算拆箱
Integer a = 100;
Integer b = 200;
int sum = a + b;  // a.intValue() + b.intValue()

// 比较运算拆箱
Integer a = 100;
System.out.println(a < 200);  // a.intValue() < 200
```

### 7.4 装箱与拆箱的陷阱

**陷阱一：空指针异常**：

```java
Integer a = null;
int b = a;  // 抛出 NullPointerException

// 编译后
int b = a.intValue();  // NullPointerException
```

**解决方案**：

```java
// 方案1：添加空检查
Integer a = null;
int b = (a != null) ? a : 0;

// 方案2：使用 Optional
Optional<Integer> optional = Optional.ofNullable(a);
int b = optional.orElse(0).intValue();
```

**陷阱二：== 比较的陷阱**：

```java
Integer a = 100;
Integer b = 100;
System.out.println(a == b);  // true（缓存范围内）

Integer c = 200;
Integer d = 200;
System.out.println(c == d);  // false（超出缓存范围）

Integer e = new Integer(100);
Integer f = 100;
System.out.println(e == f);  // false（new 创建新对象）

// 正确比较方式
System.out.println(a.equals(b));  // true
```

**陷阱三：循环中的性能问题**：

```java
// 低效：每次循环都装箱
Integer sum = 0;
for (int i = 0; i < 10000; i++) {
    sum += i;  // sum = sum + i; 
    // 每次迭代：拆箱 -> 加法 -> 装箱
}

// 高效：使用基本类型
int sum = 0;
for (int i = 0; i < 10000; i++) {
    sum += i;  // 直接使用基本类型
}
```

**陷阱四：泛型与基本类型**：

```java
// 不能使用基本类型作为泛型参数
List<int> invalid = new ArrayList<>();  // 编译错误

// 必须使用包装类
List<Integer> valid = new ArrayList<>();

// 自动装箱的影响
valid.add(1);  // 自动装箱为 Integer
int first = valid.get(0);  // 自动拆箱为 int
```

## 八、常见面试题

### 面试题 1：Integer 的缓存机制原理是什么？

**答案**：

`Integer` 类在 JVM 启动时会创建 `-128` 到 `127` 之间的整数缓存，存储在 `IntegerCache` 内部类中。当调用 `Integer.valueOf(int)` 方法或发生自动装箱时，如果值在缓存范围内，直接返回缓存的对象引用；否则创建新的 `Integer` 对象。

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

缓存范围可以通过 JVM 参数 `AutoBoxCacheMax` 配置上限。

### 面试题 2：下面的代码输出什么？

```java
Integer a = 127;
Integer b = 127;
Integer c = 128;
Integer d = 128;
System.out.println(a == b);
System.out.println(c == d);
```

**答案**：

```
true
false
```

原因：`127` 在默认缓存范围内（-128 到 127），`a` 和 `b` 指向同一个缓存对象。`128` 超出缓存范围，`c` 和 `d` 分别创建新对象，引用不同。

### 面试题 3：new Integer(100) 和 Integer.valueOf(100) 的区别？

**答案**：

- `new Integer(100)`：总是创建新的 `Integer` 对象
- `Integer.valueOf(100)`：如果值在缓存范围内，返回缓存对象；否则创建新对象

```java
Integer a = new Integer(100);
Integer b = new Integer(100);
System.out.println(a == b);  // false

Integer c = Integer.valueOf(100);
Integer d = Integer.valueOf(100);
System.out.println(c == d);  // true（缓存范围内）

Integer e = Integer.valueOf(200);
Integer f = Integer.valueOf(200);
System.out.println(e == f);  // false（超出缓存范围）
```

### 面试题 4：下面代码会输出什么？

```java
public class AutoboxingDemo {
    public static void main(String[] args) {
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        
        System.out.println(c == d);
        System.out.println(e == f);
    }
}
```

**答案**：

```
true
false
```

原因：`3` 在缓存范围内，`c` 和 `d` 指向同一个对象。`321` 超出缓存范围，`e` 和 `f` 创建新对象，引用不同。

### 面试题 5：Integer.parseInt() 和 Integer.valueOf() 的区别？

**答案**：

- `parseInt()`：返回 `int` 基本类型
- `valueOf()`：返回 `Integer` 包装类

```java
int a = Integer.parseInt("100");     // 返回 int
Integer b = Integer.valueOf("100");  // 返回 Integer，可能使用缓存

System.out.println(a == b);  // false（类型不同）
```

### 面试题 6：如何正确比较两个 Integer 对象？

**答案**：

使用 `equals()` 方法或转换为基本类型后比较：

```java
Integer a = 100;
Integer b = 100;

// 推荐：使用 equals
System.out.println(a.equals(b));  // true

// 不推荐：== 比较（可能因为缓存产生意外结果）
System.out.println(a == b);  // true（缓存范围内）
```

### 面试题 7：Integer 的最大值和最小值是多少？

**答案**：

- `Integer.MAX_VALUE`：2147483647（2^31 - 1）
- `Integer.MIN_VALUE`：-2147483648（-2^31）

```java
System.out.println(Integer.MAX_VALUE);  // 2147483647
System.out.println(Integer.MIN_VALUE);  // -2147483648

// 溢出演示
int max = Integer.MAX_VALUE;
int overflow = max + 1;
System.out.println(overflow);  // -2147483648（绕回最小值）
```

### 面试题 8：下面代码会抛出异常吗？

```java
Integer a = null;
System.out.println(a + 1);
```

**答案**：

会抛出 `NullPointerException`。

原因：执行 `a + 1` 时，`a` 需要拆箱为 `int`，调用 `a.intValue()`，由于 `a` 是 `null`，抛出空指针异常。

### 面试题 9：Integer.bitCount() 方法的作用是什么？

**答案**：

计算整数二进制表示中 1 的个数（人口计数）：

```java
System.out.println(Integer.bitCount(15));    // 4（0b1111）
System.out.println(Integer.bitCount(10));    // 2（0b1010）
System.out.println(Integer.bitCount(-1));    // 32（全 1）
```

### 面试题 10：Java 8 中 Integer 新增了哪些方法？

**答案**：

Java 8 为 `Integer` 添加了以下无符号整数相关方法：

- `toUnsignedString(int)`：返回无符号字符串表示
- `compareUnsigned(int x, int y)`：无符号比较
- `divideUnsigned(int dividend, int divisor)`：无符号除法
- `remainderUnsigned(int dividend, int divisor)`：无符号取模
- `toUnsignedLong(int x)`：转换为无符号 long
- `hashCode(int value)`：静态哈希码方法

## 九、实践应用场景

### 9.1 字符串转整数工具类

```java
public class IntegerUtils {
    
    // 安全解析，默认值
    public static int parseIntOrDefault(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // 安全解析，抛出自定义异常
    public static int parseIntOrThrow(String str) throws NumberFormatException {
        if (str == null || str.trim().isEmpty()) {
            throw new NumberFormatException("Empty or null string");
        }
        return Integer.parseInt(str.trim());
    }
    
    // 判断是否可以解析
    public static boolean isParsable(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // 范围安全的解析
    public static Integer parseIntInRange(String str, int min, int max) {
        int value = parseIntOrDefault(str, Integer.MIN_VALUE);
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("Value %d out of range [%d, %d]", value, min, max));
        }
        return value;
    }
}
```

### 9.2 进制转换工具

```java
public class RadixUtils {
    
    // 二进制转十进制
    public static int binaryToDecimal(String binary) {
        return Integer.parseInt(binary, 2);
    }
    
    // 十六进制转十进制
    public static int hexToDecimal(String hex) {
        return Integer.parseInt(hex, 16);
    }
    
    // 十进制转二进制字符串
    public static String toBinaryString(int value) {
        return Integer.toBinaryString(value);
    }
    
    // 十进制转十六进制字符串
    public static String toHexString(int value) {
        return Integer.toHexString(value).toUpperCase();
    }
    
    // 任意进制转换
    public static String convert(int value, int fromRadix, int toRadix) {
        String intermediate = Integer.toString(value, fromRadix);
        return Integer.toString(Integer.parseInt(intermediate, fromRadix), toRadix);
    }
}
```

### 9.3 位操作实用工具

```java
public class BitUtils {
    
    // 获取第 n 位的值（0 或 1）
    public static int getBit(int value, int bitIndex) {
        return (value >> bitIndex) & 1;
    }
    
    // 设置第 n 位为 1
    public static int setBit(int value, int bitIndex) {
        return value | (1 << bitIndex);
    }
    
    // 设置第 n 位为 0
    public static int clearBit(int value, int bitIndex) {
        return value & ~(1 << bitIndex);
    }
    
    // 翻转第 n 位
    public static int toggleBit(int value, int bitIndex) {
        return value ^ (1 << bitIndex);
    }
    
    // 检查是否是 2 的幂次方
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
    
    // 计算需要多少位表示这个数
    public static int bitLength(int value) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(value);
    }
}
```

### 9.4 无符号整数处理

```java
public class UnsignedUtils {
    
    // 将 int 转换为 long（无符号）
    public static long toUnsignedLong(int value) {
        return Integer.toUnsignedLong(value);
    }
    
    // 无符号比较
    public static int compareUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b);
    }
    
    // 无符号除法
    public static int divideUnsigned(int dividend, int divisor) {
        return Integer.divideUnsigned(dividend, divisor);
    }
    
    // 无符号取模
    public static int remainderUnsigned(int dividend, int divisor) {
        return Integer.remainderUnsigned(dividend, divisor);
    }
    
    // 将无符号字符串解析为 int
    public static int parseUnsignedInt(String str) {
        return Integer.parseUnsignedInt(str);
    }
    
    // 将 int 格式化为无符号字符串
    public static String toUnsignedString(int value) {
        return Integer.toUnsignedString(value);
    }
}
```

## 十、注意事项和陷阱

### 10.1 缓存范围的误解

```java
// 误解：认为 Integer 缓存只有 -128 到 127
// 实际上：可以通过 JVM 参数配置

// 验证缓存范围
public class CacheRangeDemo {
    public static void main(String[] args) {
        // 默认范围
        Integer a = 127;
        Integer b = 127;
        System.out.println(a == b);  // true
        
        Integer c = 128;
        Integer d = 128;
        System.out.println(c == d);  // false
        
        // 如果配置了 -XX:AutoBoxCacheMax=200
        // 那么 128-200 也会被缓存
    }
}
```

### 10.2 循环中的装箱问题

```java
// 问题：大量循环中的自动装箱
Long sum = 0L;
for (int i = 0; i < 1000000; i++) {
    sum += i;  // 每次都进行装箱和拆箱
}

// 优化：使用基本类型
long sumOptimized = 0;
for (int i = 0; i < 1000000; i++) {
    sumOptimized += i;
}
```

### 10.3 空指针异常

```java
// 问题：拆箱时的空指针异常
public class NullPointerTrap {
    public static void main(String[] args) {
        Integer a = null;
        
        // 这些操作都会抛出 NullPointerException
        // int b = a;           // 拆箱
        // a < 10;              // 拆箱比较
        // a + 5;               // 拆箱运算
        // System.out.println(a); // toString() 不会，但其他操作会
        
        // 安全的方式
        if (a != null) {
            int b = a;
        }
    }
}
```

### 10.4 字符串拼接的陷阱

```java
// 问题：字符串拼接中的自动装箱
Integer a = 100;
String s = "" + a;  // 会调用 a.toString()

// 性能问题：多次拼接
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 每次都创建 Integer 和 String 对象
}

// 优化：使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
```

### 10.5 比较操作的陷阱

```java
// 问题：混合比较
Integer a = 100;
int b = 100;
System.out.println(a == b);  // true（Integer 自动拆箱）

Integer c = 200;
int d = 200;
System.out.println(c == d);  // true（自动拆箱）

Integer e = 100;
Integer f = 100;
System.out.println(e == f);  // true（缓存）

Integer g = 200;
Integer h = 200;
System.out.println(g == h);  // false（超出缓存）

// 正确做法：始终使用 equals
System.out.println(g.equals(h));  // true
```

### 10.6 溢出处理

```java
// 问题：整数溢出
int max = Integer.MAX_VALUE;
int overflow = max + 1;
System.out.println(overflow);  // -2147483648（绕回）

// 检测溢出
public static int safeAdd(int a, int b) {
    int result = a + b;
    if ((a > 0 && b > 0 && result < 0) ||
        (a < 0 && b < 0 && result > 0)) {
        throw new ArithmeticException("Integer overflow");
    }
    return result;
}

// 使用 Math.addExact（JDK 8+）
int sum = Math.addExact(max, 1);  // 抛出 ArithmeticException
```

## 十一、与其他包装类的对比

### 11.1 缓存范围对比

| 包装类 | 缓存范围 | 默认最大值 | 可配置性 |
|--------|----------|------------|----------|
| Byte | -128 到 127 | 127 | 不可配置 |
| Short | -128 到 127 | 127 | 可配置 |
| Integer | -128 到 127 | 127 | 可配置 |
| Long | -128 到 127 | 127 | 可配置 |
| Character | 0 到 127 | 127 | 不可配置 |
| Float | 无 | 不适用 | 不适用 |
| Double | 无 | 不适用 | 不适用 |

### 11.2 方法对比

| 方法 | Byte | Short | Integer | Long | Character |
|------|------|-------|---------|------|-----------|
| bitCount | ✓ | ✓ | ✓ | ✓ | ✗ |
| rotateLeft/Right | ✓ | ✓ | ✓ | ✓ | ✗ |
| highestOneBit | ✓ | ✓ | ✓ | ✓ | ✗ |
| lowestOneBit | ✓ | ✓ | ✓ | ✓ | ✗ |
| compareUnsigned | ✗ | ✗ | ✓ | ✓ | ✗ |

### 11.3 字节大小对比

| 类型 | 包装类大小（估算） | 基本类型大小 |
|------|-------------------|--------------|
| byte | 24 字节 | 1 字节 |
| short | 24 字节 | 2 字节 |
| int | 24 字节 | 4 字节 |
| long | 24 字节 | 8 字节 |
| float | 24 字节 | 4 字节 |
| double | 24 字节 | 8 字节 |
| char | 24 字节 | 2 字节 |
| boolean | 24 字节 | 1 位（实际 1 字节） |

## 十二、总结

### 12.1 核心要点

1. **IntegerCache 机制**：`Integer` 类实现了享元模式，通过 `-128` 到 `127` 的缓存减少内存占用和对象创建开销。

2. **自动装箱与拆箱**：Java 5 引入的特性，使得基本类型和包装类可以无缝转换，但需要注意性能和空指针问题。

3. **进制转换支持**：`Integer` 提供了丰富的进制转换方法，支持 2 到 36 进制的转换。

4. **位操作方法**：包括人口计数、最高/最低位、位移等底层操作，在算法和系统编程中非常有用。

5. **无符号整数支持**：Java 8 引入了无符号整数操作，扩展了 `int` 类型的应用范围。

### 12.2 最佳实践

1. **优先使用基本类型**：在性能敏感的场景中，优先使用 `int` 而不是 `Integer`。

2. **谨慎使用自动装箱**：在循环和大数据量处理中注意装箱带来的性能开销。

3. **使用 equals 比较**：比较两个 `Integer` 对象时，始终使用 `equals()` 方法而不是 `==`。

4. **注意空指针安全**：在可能为 `null` 的 `Integer` 对象上进行拆箱操作时，要先进行空检查。

5. **合理配置缓存范围**：在高并发场景中，可以根据需要调整 `IntegerCache` 的上限。

### 12.3 进阶学习方向

1. **学习其他包装类**：深入研究 `Long`、`Short`、`Byte` 等包装类的实现和缓存机制。

2. **学习并发集合**：`ConcurrentHashMap` 等并发集合中大量使用了缓存和原子操作。

3. **学习 JVM 层面**：了解 JIT 编译器对自动装箱/拆箱的优化。

4. **学习性能调优**：掌握 JMH 等性能测试工具，对比不同实现的性能差异。

## 参考资料

- JDK 8 官方文档：https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html
- 《Java 核心技术 卷 I》
- 《Effective Java》- Joshua Bloch
- JLS（Java Language Specification）- Autoboxing

---

**笔记创建时间**：2025-12-26

**最后更新时间**：2025-12-26

**版本**：1.0
