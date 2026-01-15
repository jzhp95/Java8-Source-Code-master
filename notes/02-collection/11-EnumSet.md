# EnumSet 源码解读

## 一、类概述

EnumSet是Java集合框架中专门用于枚举类型的Set实现。所有元素必须来自单个枚举类型,该类型在创建集合时指定或隐式确定。EnumSet内部使用位向量表示,这种表示方式极其紧凑和高效。

### 1.1 核心特性

- **枚举专用**: 只能存储枚举类型的元素
- **位向量表示**: 使用位向量存储,极其紧凑
- **高性能**: 所有基本操作都是O(1)时间复杂度
- **类型安全**: 编译时类型检查
- **有序性**: 按枚举常量的声明顺序迭代
- **非同步**: 非线程安全,需要外部同步
- **Weakly Consistent**: 迭代器不会抛出ConcurrentModificationException

### 1.2 适用场景

- 枚举类型的集合操作
- 替代传统的int位标志
- 需要高性能的枚举集合
- 需要类型安全的枚举操作

## 二、设计原理

### 2.1 位向量表示

EnumSet使用位向量存储枚举元素:

```java
// 枚举常量与位的对应关系
enum Color {
    RED,    // bit 0
    GREEN,  // bit 1
    BLUE    // bit 2
}

// EnumSet内部表示
// RED    -> 001 (二进制) -> 1
// GREEN  -> 010 (二进制) -> 2
// BLUE   -> 100 (二进制) -> 4

// {RED, BLUE} -> 101 (二进制) -> 5
```

### 2.2 两种实现

EnumSet根据枚举常量数量选择不同的实现:

```java
// 枚举常量 <= 64: 使用RegularEnumSet
// 枚举常量 > 64: 使用JumboEnumSet

public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    Enum<?>[] universe = getUniverse(elementType);
    if (universe == null)
        throw new ClassCastException(elementType + " not an enum");

    if (universe.length <= 64)
        return new RegularEnumSet<>(elementType, universe);
    else
        return new JumboEnumSet<>(elementType, universe);
}
```

**设计要点**:
- RegularEnumSet: 使用单个long变量(64位)
- JumboEnumSet: 使用long数组
- 自动选择最优实现

### 2.3 枚举常量数组

EnumSet缓存枚举常量数组:

```java
final Enum<?>[] universe;  // 所有枚举常量

private static <E extends Enum<E>> E[] getUniverse(Class<E> elementType) {
    return SharedSecrets.getJavaLangAccess()
                                    .getEnumConstantsShared(elementType);
}
```

**设计要点**:
- 缓存枚举常量,提高性能
- 所有实例共享同一个数组
- 用于位索引到枚举常量的映射

## 三、继承结构

```
AbstractSet<E>
    ↑
EnumSet<E> (抽象类)
    ↑
RegularEnumSet<E>  (枚举常量 <= 64)
JumboEnumSet<E>    (枚举常量 > 64)
```

### 3.1 抽象类设计

EnumSet是抽象类,定义了公共接口:

```java
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E>
    implements Cloneable, java.io.Serializable {
    
    final Class<E> elementType;  // 枚举类型
    final Enum<?>[] universe;    // 枚举常量数组
    
    // 抽象方法,由子类实现
    abstract void addAll();
    abstract void addRange(E from, E to);
    abstract void complement();
}
```

### 3.2 子类职责

- **RegularEnumSet**: 使用单个long变量
- **JumboEnumSet**: 使用long数组

## 四、核心字段分析

### 4.1 枚举类型

```java
final Class<E> elementType;
```

**字段设计要点**:
- 存储枚举类型
- 用于类型检查
- final修饰,不可修改

### 4.2 枚举常量数组

```java
final Enum<?>[] universe;
```

**字段设计要点**:
- 存储所有枚举常量
- 用于位索引到枚举常量的映射
- 缓存提高性能

### 4.3 空数组

```java
private static Enum<?>[] ZERO_LENGTH_ENUM_ARRAY = new Enum<?>[0];
```

**用途**:
- 用于toArray方法
- 避免创建新数组

## 五、工厂方法

### 5.1 noneOf - 创建空集合

```java
public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    Enum<?>[] universe = getUniverse(elementType);
    if (universe == null)
        throw new ClassCastException(elementType + " not an enum");

    if (universe.length <= 64)
        return new RegularEnumSet<>(elementType, universe);
    else
        return new JumboEnumSet<>(elementType, universe);
}
```

**方法要点**:
- 创建空的EnumSet
- 根据枚举常量数量选择实现
- 时间复杂度: O(1)

**示例**:
```java
EnumSet<Color> set = EnumSet.noneOf(Color.class);
```

### 5.2 allOf - 创建包含所有元素的集合

```java
public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
    EnumSet<E> result = noneOf(elementType);
    result.addAll();
    return result;
}
```

**方法要点**:
- 创建包含所有枚举常量的集合
- 时间复杂度: O(1)

**示例**:
```java
EnumSet<Color> set = EnumSet.allOf(Color.class);
// {RED, GREEN, BLUE}
```

### 5.3 copyOf - 复制集合

```java
// 从EnumSet复制
public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
    return s.clone();
}

// 从Collection复制
public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    if (c instanceof EnumSet) {
        return ((EnumSet<E>)c).clone();
    } else {
        if (c.isEmpty())
            throw new IllegalArgumentException("Collection is empty");
        Iterator<E> i = c.iterator();
        E first = i.next();
        EnumSet<E> result = EnumSet.of(first);
        while (i.hasNext())
            result.add(i.next());
        return result;
    }
}
```

**方法要点**:
- 创建集合的副本
- 如果是EnumSet,直接克隆
- 如果是普通Collection,逐个添加

**示例**:
```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = EnumSet.copyOf(set1);

List<Color> list = Arrays.asList(Color.RED, Color.BLUE);
EnumSet<Color> set3 = EnumSet.copyOf(list);
```

### 5.4 complementOf - 创建补集

```java
public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
    EnumSet<E> result = copyOf(s);
    result.complement();
    return result;
}
```

**方法要点**:
- 创建包含所有不在指定集合中的枚举常量的集合
- 时间复杂度: O(1)

**示例**:
```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = EnumSet.complementOf(set1);
// {BLUE}
```

### 5.5 of - 创建包含指定元素的集合

```java
// 单个元素
public static <E extends Enum<E>> EnumSet<E> of(E e) {
    EnumSet<E> result = noneOf(e.getDeclaringClass());
    result.add(e);
    return result;
}

// 两个元素
public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2) {
    EnumSet<E> result = noneOf(e1.getDeclaringClass());
    result.add(e1);
    result.add(e2);
    return result;
}

// 三个元素
public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3) {
    EnumSet<E> result = noneOf(e1.getDeclaringClass());
    result.add(e1);
    result.add(e2);
    result.add(e3);
    return result;
}

// 四个元素
public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4) {
    EnumSet<E> result = noneOf(e1.getDeclaringClass());
    result.add(e1);
    result.add(e2);
    result.add(e3);
    result.add(e4);
    return result;
}

// 五个元素
public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    EnumSet<E> result = noneOf(e1.getDeclaringClass());
    result.add(e1);
    result.add(e2);
    result.add(e3);
    result.add(e4);
    result.add(e5);
    return result;
}

// 可变参数
@SafeVarargs
public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    EnumSet<E> result = noneOf(first.getDeclaringClass());
    result.add(first);
    for (E e : rest)
        result.add(e);
    return result;
}
```

**方法要点**:
- 提供多个重载,避免可变参数开销
- 可变参数版本性能稍低
- 时间复杂度: O(n)

**示例**:
```java
EnumSet<Color> set1 = EnumSet.of(Color.RED);
EnumSet<Color> set2 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set3 = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
EnumSet<Color> set4 = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
EnumSet<Color> set5 = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK);
EnumSet<Color> set6 = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK, Color.WHITE);
```

### 5.6 range - 创建范围集合

```java
public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
    if (from.compareTo(to) > 0)
        throw new IllegalArgumentException(from + " > " + to);
    EnumSet<E> result = noneOf(from.getDeclaringClass());
    result.addRange(from, to);
    return result;
}
```

**方法要点**:
- 创建包含指定范围内所有枚举常量的集合
- 包含端点
- 时间复杂度: O(1)

**示例**:
```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

EnumSet<Day> workDays = EnumSet.range(Day.MONDAY, Day.FRIDAY);
// {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}
```

## 六、抽象方法

### 6.1 addAll - 添加所有元素

```java
abstract void addAll();
```

**实现要点**:
- 添加所有枚举常量
- 由子类实现
- RegularEnumSet: 设置所有位
- JumboEnumSet: 设置所有数组元素

### 6.2 addRange - 添加范围

```java
abstract void addRange(E from, E to);
```

**实现要点**:
- 添加指定范围内的所有枚举常量
- 由子类实现
- 使用位操作高效实现

### 6.3 complement - 补集操作

```java
abstract void complement();
```

**实现要点**:
- 将集合转换为补集
- 由子类实现
- 使用位取反操作

## 七、核心方法

### 7.1 typeCheck - 类型检查

```java
final void typeCheck(E e) {
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
        throw new ClassCastException(eClass + " != " + elementType);
}
```

**方法要点**:
- 检查元素类型是否匹配
- 允许子类枚举
- 抛出ClassCastException

### 7.2 clone - 克隆

```java
@SuppressWarnings("unchecked")
public EnumSet<E> clone() {
    try {
        return (EnumSet<E>) super.clone();
    } catch(CloneNotSupportedException e) {
        throw new AssertionError(e);
    }
}
```

**方法要点**:
- 浅拷贝
- 返回EnumSet实例
- 时间复杂度: O(1)

**示例**:
```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = set1.clone();
```

## 八、序列化

### 8.1 序列化代理模式

EnumSet使用序列化代理模式:

```java
private static class SerializationProxy <E extends Enum<E>>
    implements java.io.Serializable {
    private final Class<E> elementType;
    private final Enum<?>[] elements;

    SerializationProxy(EnumSet<E> set) {
        elementType = set.elementType;
        elements = set.toArray(ZERO_LENGTH_ENUM_ARRAY);
    }

    @SuppressWarnings("unchecked")
    private Object readResolve() {
        EnumSet<E> result = EnumSet.noneOf(elementType);
        for (Enum<?> e : elements)
            result.add((E)e);
        return result;
    }

    private static final long serialVersionUID = 362491234563181265L;
}

Object writeReplace() {
    return new SerializationProxy<>(this);
}

private void readObject(java.io.ObjectInputStream stream)
    throws java.io.InvalidObjectException {
    throw new java.io.InvalidObjectException("Proxy required");
}
```

**设计要点**:
- 使用序列化代理隐藏实现细节
- 反序列化时使用工厂方法
- 保证跨版本兼容性

## 九、RegularEnumSet实现

### 9.1 核心字段

```java
class RegularEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private long elements = 0L;

    RegularEnumSet(Class<E>elementType, Enum<?>[] universe) {
        super(elementType, universe);
    }
}
```

**字段设计要点**:
- 使用单个long变量存储位向量
- 最多支持64个枚举常量
- 每个位对应一个枚举常量

### 9.2 add - 添加元素

```java
public boolean add(E e) {
    typeCheck(e);

    long oldElements = elements;
    elements |= (1L << e.ordinal());
    return elements != oldElements;
}
```

**方法要点**:
- 使用位或操作设置位
- 返回是否添加成功
- 时间复杂度: O(1)

### 9.3 remove - 移除元素

```java
public boolean remove(Object e) {
    if (e == null)
        return false;
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
        return false;

    long oldElements = elements;
    elements &= ~(1L << ((Enum<?>)e).ordinal());
    return elements != oldElements;
}
```

**方法要点**:
- 使用位与清除操作清除位
- 返回是否移除成功
- 时间复杂度: O(1)

### 9.4 contains - 检查包含

```java
public boolean contains(Object e) {
    if (e == null)
        return false;
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
        return false;

    return (elements & (1L << ((Enum<?>)e).ordinal())) != 0;
}
```

**方法要点**:
- 使用位与操作检查位
- 时间复杂度: O(1)

### 9.5 addAll - 添加所有元素

```java
void addAll() {
    if (universe.length != 0)
        elements = -1L >>> (64 - universe.length);
}
```

**方法要点**:
- 设置所有有效位
- 时间复杂度: O(1)

### 9.6 complement - 补集操作

```java
void complement() {
    if (universe.length != 0) {
        elements ^= -1L >>> (64 - universe.length);
    }
}
```

**方法要点**:
- 使用位异或操作
- 时间复杂度: O(1)

### 9.7 addRange - 添加范围

```java
void addRange(E from, E to) {
    elements |= (-1L >>> (from.ordinal() - to.ordinal() - 1)) << from.ordinal();
}
```

**方法要点**:
- 使用位操作高效实现
- 时间复杂度: O(1)

## 十、JumboEnumSet实现

### 10.1 核心字段

```java
class JumboEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private long[] elements;

    JumboEnumSet(Class<E>elementType, Enum<?>[] universe) {
        super(elementType, universe);
        elements = new long[(universe.length + 63) >>> 6];
    }
}
```

**字段设计要点**:
- 使用long数组存储位向量
- 支持超过64个枚举常量
- 每个long存储64位

### 10.2 数组索引计算

```java
private int bitIndex(E e) {
    int ordinal = e.ordinal();
    return ordinal >>> 6;  // ordinal / 64
}

private long bitMask(E e) {
    int ordinal = e.ordinal();
    return 1L << (ordinal & 0x3F);  // ordinal % 64
}
```

**设计要点**:
- 数组索引: ordinal / 64
- 位掩码: 1L << (ordinal % 64)

### 10.3 add - 添加元素

```java
public boolean add(E e) {
    typeCheck(e);

    int wordIndex = e.ordinal() >>> 6;
    long oldElements = elements[wordIndex];
    elements[wordIndex] |= (1L << e.ordinal());
    return elements[wordIndex] != oldElements;
}
```

**方法要点**:
- 计算数组索引
- 设置对应位
- 时间复杂度: O(1)

### 10.4 remove - 移除元素

```java
public boolean remove(Object e) {
    if (e == null)
        return false;
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
        return false;

    int wordIndex = ((Enum<?>)e).ordinal() >>> 6;
    long oldElements = elements[wordIndex];
    elements[wordIndex] &= ~(1L << ((Enum<?>)e).ordinal());
    return elements[wordIndex] != oldElements;
}
```

**方法要点**:
- 计算数组索引
- 清除对应位
- 时间复杂度: O(1)

### 10.5 contains - 检查包含

```java
public boolean contains(Object e) {
    if (e == null)
        return false;
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
        return false;

    int wordIndex = ((Enum<?>)e).ordinal() >>> 6;
    return (elements[wordIndex] & (1L << ((Enum<?>)e).ordinal())) != 0;
}
```

**方法要点**:
- 计算数组索引
- 检查对应位
- 时间复杂度: O(1)

## 十一、迭代器实现

### 11.1 迭代器特性

```java
// EnumSet的迭代器特点:
// 1. 按枚举常量的声明顺序迭代
// 2. Weakly Consistent: 不会抛出ConcurrentModificationException
// 3. 可能或可能不显示迭代过程中的修改
```

### 11.2 迭代顺序

```java
enum Color {
    RED, GREEN, BLUE
}

EnumSet<Color> set = EnumSet.of(Color.BLUE, Color.RED);
// 迭代顺序: RED, BLUE (按声明顺序,而非插入顺序)
```

## 十二、设计模式

### 12.1 工厂模式

EnumSet使用静态工厂方法:

```java
// 工厂方法
public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    // 根据枚举常量数量选择实现
    if (universe.length <= 64)
        return new RegularEnumSet<>(elementType, universe);
    else
        return new JumboEnumSet<>(elementType, universe);
}
```

### 12.2 策略模式

根据枚举常量数量选择不同实现:

```java
// RegularEnumSet: <= 64个枚举常量
// JumboEnumSet: > 64个枚举常量
```

### 12.3 代理模式

序列化使用代理模式:

```java
private static class SerializationProxy<E extends Enum<E>>
    implements java.io.Serializable {
    // 序列化代理
}
```

## 十三、面试常见问题

### 13.1 EnumSet与HashSet的区别?

| 特性 | EnumSet | HashSet |
|------|----------|---------|
| 元素类型 | 仅枚举 | 任意对象 |
| 内部表示 | 位向量 | 哈希表 |
| 时间复杂度 | O(1) | O(1)平均 |
| 空间复杂度 | 极低 | 较高 |
| 迭代顺序 | 枚举声明顺序 | 无序 |
| 类型安全 | 编译时 | 运行时 |

### 13.2 EnumSet为什么性能高?

1. **位向量表示**: 使用位操作,极其高效
2. **类型安全**: 编译时检查,避免运行时类型转换
3. **紧凑存储**: 每个枚举常量只需1位
4. **缓存优化**: 缓存枚举常量数组
5. **批量操作**: 使用位运算实现批量操作

### 13.3 EnumSet支持null元素吗?

不支持。尝试添加null元素会抛出NullPointerException:

```java
EnumSet<Color> set = EnumSet.noneOf(Color.class);
set.add(null);  // NullPointerException
```

### 13.4 EnumSet是线程安全的吗?

不是。需要外部同步:

```java
Set<Color> syncSet = Collections.synchronizedSet(EnumSet.noneOf(Color.class));
```

### 13.5 EnumSet的迭代器是fail-fast的吗?

不是。迭代器是weakly consistent的,不会抛出ConcurrentModificationException:

```java
EnumSet<Color> set = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
Iterator<Color> it = set.iterator();
set.add(Color.YELLOW);  // 不会抛出异常
while (it.hasNext()) {
    System.out.println(it.next());  // 可能或可能不显示YELLOW
}
```

### 13.6 EnumSet如何存储元素?

使用位向量存储:

```java
enum Color {
    RED,    // bit 0
    GREEN,  // bit 1
    BLUE    // bit 2
}

EnumSet<Color> set = EnumSet.of(Color.RED, Color.BLUE);
// 内部表示: 101 (二进制) -> 5
```

### 13.7 EnumSet支持多少个枚举常量?

- **RegularEnumSet**: 最多64个
- **JumboEnumSet**: 超过64个

自动选择最优实现:

```java
// <= 64: RegularEnumSet
// > 64: JumboEnumSet
```

### 13.8 EnumSet的批量操作时间复杂度?

如果参数也是EnumSet,批量操作是O(1):

```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = EnumSet.of(Color.BLUE, Color.YELLOW);

// O(1)时间复杂度
set1.addAll(set2);
set1.retainAll(set2);
set1.removeAll(set2);
```

## 十四、使用场景

### 14.1 替代位标志

```java
// 传统方式: 使用int位标志
int flags = 0;
flags |= 1;  // 设置RED
flags |= 4;  // 设置BLUE
if ((flags & 1) != 0) { /* RED */ }

// EnumSet方式: 类型安全
EnumSet<Color> flags = EnumSet.noneOf(Color.class);
flags.add(Color.RED);
flags.add(Color.BLUE);
if (flags.contains(Color.RED)) { /* RED */ }
```

### 14.2 枚举集合操作

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// 工作日
EnumSet<Day> workDays = EnumSet.range(Day.MONDAY, Day.FRIDAY);

// 周末
EnumSet<Day> weekend = EnumSet.complementOf(workDays);

// 添加假期
workDays.add(Day.SATURDAY);
```

### 14.3 权限管理

```java
enum Permission {
    READ, WRITE, EXECUTE, DELETE, ADMIN
}

// 用户权限
EnumSet<Permission> userPermissions = EnumSet.of(Permission.READ, Permission.WRITE);

// 检查权限
if (userPermissions.contains(Permission.READ)) {
    // 允许读取
}
```

### 14.4 状态管理

```java
enum State {
    STARTED, RUNNING, PAUSED, STOPPED, ERROR
}

EnumSet<State> activeStates = EnumSet.of(State.STARTED, State.RUNNING);

if (activeStates.contains(currentState)) {
    // 系统处于活动状态
}
```

### 14.5 配置管理

```java
enum Feature {
    DARK_MODE, NOTIFICATIONS, AUTO_SAVE, CLOUD_SYNC
}

EnumSet<Feature> enabledFeatures = EnumSet.noneOf(Feature.class);

// 启用功能
enabledFeatures.add(Feature.DARK_MODE);
enabledFeatures.add(Feature.AUTO_SAVE);

// 检查功能是否启用
if (enabledFeatures.contains(Feature.DARK_MODE)) {
    // 启用暗黑模式
}
```

## 十五、注意事项

### 15.1 类型检查

```java
EnumSet<Color> set = EnumSet.noneOf(Color.class);
set.add(Color.RED);  // OK
set.add(Day.MONDAY);  // 编译错误
```

### 15.2 null元素

```java
EnumSet<Color> set = EnumSet.noneOf(Color.class);
set.add(null);  // NullPointerException
```

### 15.3 迭代顺序

```java
EnumSet<Color> set = EnumSet.of(Color.BLUE, Color.RED);
// 迭代顺序: RED, BLUE (按声明顺序)
```

### 15.4 线程安全

```java
EnumSet<Color> set = EnumSet.of(Color.RED, Color.GREEN);

// 需要外部同步
Set<Color> syncSet = Collections.synchronizedSet(set);
```

### 15.5 迭代器一致性

```java
EnumSet<Color> set = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
Iterator<Color> it = set.iterator();
set.add(Color.YELLOW);  // 不会抛出异常
while (it.hasNext()) {
    System.out.println(it.next());  // 可能或可能不显示YELLOW
}
```

## 十六、最佳实践

### 16.1 使用EnumSet替代位标志

```java
// 不推荐: 使用int位标志
int flags = 0;
flags |= 1;

// 推荐: 使用EnumSet
EnumSet<Color> flags = EnumSet.noneOf(Color.class);
flags.add(Color.RED);
```

### 16.2 使用工厂方法创建

```java
// 推荐: 使用工厂方法
EnumSet<Color> set = EnumSet.of(Color.RED, Color.GREEN);

// 不推荐: 使用构造方法(不可访问)
// EnumSet<Color> set = new EnumSet<>();  // 编译错误
```

### 16.3 使用范围操作

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// 推荐: 使用range
EnumSet<Day> workDays = EnumSet.range(Day.MONDAY, Day.FRIDAY);

// 不推荐: 逐个添加
EnumSet<Day> workDays = EnumSet.noneOf(Day.class);
workDays.add(Day.MONDAY);
workDays.add(Day.TUESDAY);
// ...
```

### 16.4 使用补集操作

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

EnumSet<Day> workDays = EnumSet.range(Day.MONDAY, Day.FRIDAY);

// 推荐: 使用complementOf
EnumSet<Day> weekend = EnumSet.complementOf(workDays);

// 不推荐: 手动计算
EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
```

### 16.5 使用批量操作

```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = EnumSet.of(Color.BLUE, Color.YELLOW);

// 推荐: 使用批量操作
set1.addAll(set2);

// 不推荐: 逐个添加
for (Color color : set2) {
    set1.add(color);
}
```

## 十七、性能优化

### 17.1 使用RegularEnumSet

```java
// <= 64个枚举常量: 使用RegularEnumSet(单个long)
enum Color {
    RED, GREEN, BLUE
}
EnumSet<Color> set = EnumSet.noneOf(Color.class);  // RegularEnumSet
```

### 17.2 使用位操作

```java
// EnumSet内部使用位操作,性能极高
EnumSet<Color> set = EnumSet.of(Color.RED, Color.GREEN);
set.contains(Color.RED);  // O(1)
set.add(Color.BLUE);     // O(1)
```

### 17.3 使用批量操作

```java
EnumSet<Color> set1 = EnumSet.of(Color.RED, Color.GREEN);
EnumSet<Color> set2 = EnumSet.of(Color.BLUE, Color.YELLOW);

// 批量操作: O(1)
set1.addAll(set2);
set1.retainAll(set2);
set1.removeAll(set2);
```

## 十八、总结

EnumSet是专门用于枚举类型的Set实现,使用位向量存储,性能极高。

### 核心要点

1. **枚举专用**: 只能存储枚举类型的元素
2. **位向量表示**: 使用位向量存储,极其紧凑
3. **高性能**: 所有基本操作都是O(1)时间复杂度
4. **类型安全**: 编译时类型检查
5. **有序性**: 按枚举常量的声明顺序迭代
6. **两种实现**: RegularEnumSet(<=64)和JumboEnumSet(>64)
7. **Weakly Consistent**: 迭代器不会抛出ConcurrentModificationException

### 适用场景

- 枚举类型的集合操作
- 替代传统的int位标志
- 需要高性能的枚举集合
- 需要类型安全的枚举操作

### 不适用场景

- 非枚举类型的元素
- 需要null元素
- 多线程环境(需外部同步)

### 性能特点

- 基本操作: O(1)
- 批量操作(参数是EnumSet): O(1)
- 空间复杂度: 极低(每个枚举常量1位)

### 与HashSet的选择

- 枚举类型: EnumSet
- 非枚举类型: HashSet
- 需要类型安全: EnumSet
- 需要null元素: HashSet
