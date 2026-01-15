# HashSet 源码深度解析

## 一、类概述与设计理念

### 1.1 基本信息

HashSet 是 Java 集合框架中实现 Set 接口的最常用实现类，位于 `java.util` 包中。作为基于哈希表的无序集合，HashSet 提供了常数时间复杂度的基本操作，包括添加、删除、包含检查和获取大小。HashSet 在日常开发中使用频率极高，是实现去重和快速成员检查的理想选择。

HashSet 的设计理念体现了"委托模式"的核心思想。HashSet 本身并不直接实现哈希表的逻辑，而是将所有操作委托给内部的 HashMap 实例来完成。这种设计不仅简化了 HashSet 的实现，还确保了与 HashMap 行为的一致性。通过将元素作为键存储在 HashMap 中，并使用一个固定的占位对象作为值，HashSet 完美地利用了 HashMap 的哈希表机制来实现集合功能。

### 1.2 设计特点

HashSet 的设计体现了以下几个核心特点，这些特点共同构成了其高效性和易用性的基础。

首先是**基于 HashMap 的实现**。HashSet 内部使用 HashMap 作为存储结构，元素本身作为 HashMap 的键，而一个静态常量对象 PRESENT 作为值。这种设计巧妙地利用了 HashMap 键的唯一性特性来保证集合中元素的唯一性。

其次是**无序性**。HashSet 不保证元素的迭代顺序，这意味着元素的添加顺序和遍历顺序可能不同。对于需要保持插入顺序的场景，应该使用 LinkedHashSet。

第三是**允许存储 null**。与 HashMap 类似，HashSet 允许存储一个 null 元素。这是因为 null 可以作为有效的键存储在 HashMap 中。

第四是**fail-fast 迭代器**。HashSet 的迭代器是 fail-fast 的，如果在迭代过程中检测到结构性修改，会抛出 ConcurrentModificationException 异常。

第五是**非线程安全**。与 HashMap 一样，HashSet 不是线程安全的。如果需要在多线程环境中使用，需要进行外部同步或使用 Collections.synchronizedSet() 包装。

### 1.3 适用场景

HashSet 适用于多种开发场景，理解其适用边界对于正确选择数据结构至关重要。

在**去重场景**中，HashSet 是最自然的选择。无论是去除列表中的重复元素，还是确保集合中不存在重复数据，HashSet 都能高效地完成任务。

在**快速成员检查**场景中，HashSet 的 contains() 方法提供 O(1) 平均时间复杂度的查找性能。这使得 HashSet 成为实现缓存、过滤器等功能的理想选择。

在**集合运算**场景中，HashSet 支持交集、并集、差集等集合运算。结合 retainAll()、addAll()、removeAll() 等方法，可以方便地实现各种集合操作。

在**去重计数**场景中，可以使用 HashSet 配合 size() 方法快速计算不重复元素的数量。

然而，在**需要保持顺序**的场景中，HashSet 不适合使用。此时应该考虑使用 LinkedHashSet（保持插入顺序）或 TreeSet（保持排序顺序）。

在**需要范围查询**的场景中，HashSet 不支持范围操作。如果需要范围查询功能，应该使用 TreeSet 或其他支持范围的数据结构。

### 1.4 性能特征总结

从时间复杂度的角度分析 HashSet 的性能特征：

| 操作类型 | 平均时间复杂度 | 最坏时间复杂度 | 说明 |
|---------|--------------|--------------|------|
| add(E e) | O(1) | O(log n) | 取决于 HashMap.put() |
| remove(Object o) | O(1) | O(log n) | 取决于 HashMap.remove() |
| contains(Object o) | O(1) | O(log n) | 取决于 HashMap.containsKey() |
| size() | O(1) | O(1) | 直接返回 map.size() |
| isEmpty() | O(1) | O(1) | 直接返回 map.isEmpty() |
| iterator() | O(1) | O(1) | 返回 keySet 迭代器 |
| 遍历 | O(n + m) | O(n + m) | n 为元素数，m 为桶数 |

从空间复杂度角度分析，HashSet 的空间开销主要来自内部 HashMap，包括哈希表数组和每个 Entry 的存储空间。

## 二、继承结构与接口实现

### 2.1 类继承体系

HashSet 的继承结构设计体现了 Java 集合框架的优秀设计思想：

```
AbstractSet<E>
    └── HashSet<E>
```

HashSet 直接继承自 `AbstractSet`，这是专门为 Set 接口设计的抽象类。AbstractSet 提供了 Set 接口的部分实现，包括 equals()、hashCode()、removeAll() 等方法的默认实现。这种设计避免了代码重复，同时确保了不同 Set 实现之间行为的一致性。

### 2.2 接口实现关系

HashSet 实现了以下核心接口：

**Set<E> 接口**：定义了集合的基本操作，包括 add()、remove()、contains()、size() 等核心方法。HashSet 完全遵循 Set 接口的契约，特别是不允许存储重复元素。

**Cloneable 接口**：表明 HashSet 支持克隆操作。通过调用 clone() 方法可以创建 HashSet 的浅拷贝。

**Serializable 接口**：表明 HashSet 支持序列化。HashSet 实现了自定义的序列化和反序列化逻辑，包括 writeObject() 和 readObject() 方法。

### 2.3 AbstractSet 的关键贡献

AbstractSet 为 HashSet 提供了重要的基础实现。在相等性判断方面，AbstractSet 实现了基于内容的 equals() 方法。两个 Set 被认为是相等的当且仅当它们包含相同的元素。这种实现被 HashSet 直接复用。

在哈希计算方面，AbstractSet 实现了 hashCode() 方法，基于所有元素的哈希值计算总哈希值。这个实现也被 HashSet 直接复用。

在批量操作方面，AbstractSet 提供了 removeAll()、retainAll() 等批量操作方法的默认实现，这些方法被 HashSet 直接使用内部 HashMap 的能力来完成。

## 三、核心字段解析

### 3.1 内部 HashMap

```java
private transient HashMap<E,Object> map;
```

map 是 HashSet 的核心存储字段，它是一个 HashMap 实例，类型参数为 E（元素类型）和 Object（值类型）。

这个字段被声明为 transient，意味着在默认序列化过程中不会被自动序列化。HashSet 实现了自定义的序列化逻辑，单独处理这个字段。

HashMap 作为 HashSet 的内部存储结构，承担了所有元素的存储工作。HashSet 的所有操作实际上都是通过操作这个 HashMap 来完成的。

### 3.2 占位对象

```java
private static final Object PRESENT = new Object();
```

PRESENT 是一个静态常量对象，作为 HashMap 的值使用。由于 HashSet 只关心键（元素）的唯一性，值本身可以是任意固定对象。

选择新建一个 Object 实例作为占位值，而不是使用 null 或其他常量，有以下考虑：

**明确的语义**：使用明确的占位对象比使用 null 更清晰地表达设计意图。null 可能有特殊含义（如"无值"），而 PRESENT 明确表示"这是一个占位值"。

**避免空指针异常**：使用 PRESENT 作为值可以避免在需要获取值的地方出现空指针异常。虽然 HashSet 本身不暴露这个值，但内部操作可能需要比较值。

**不可变性**：作为 static final 常量，PRESENT 在类加载时创建，之后不会被修改，确保了整个运行期间的一致性。

### 3.3 序列化 UID

```java
static final long serialVersionUID = -5024744406713321676L;
```

serialVersionUID 是序列化版本标识符，用于在反序列化时验证发送方和接收方加载的类的版本是否兼容。这个值是在 HashSet 实现时确定的，之后不应更改。

## 四、构造方法分析

### 4.1 默认构造方法

```java
public HashSet() {
    map = new HashMap<>();
}
```

默认构造方法创建一个空的 HashSet，内部的 HashMap 使用默认的初始容量（16）和默认的负载因子（0.75）。

这是一个轻量级的操作，只是简单地创建了一个新的 HashMap 实例。实际分配内存的工作由 HashMap 的构造方法完成。

### 4.2 集合构造方法

```java
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}
```

这个构造方法接受一个 Collection，将其中的所有元素添加到新创建的 HashSet 中。

容量计算策略：使用 Math.max((int) (c.size()/.75f) + 1, 16) 计算初始容量。这个公式确保了 HashMap 有足够的容量来容纳所有元素，避免在添加过程中频繁扩容。公式 c.size()/.75f 计算的是在负载因子 0.75 下容纳 c.size() 个元素所需的容量，加 1 是为了处理边界情况，最小值为 16。

然后调用 addAll() 方法将集合中的所有元素添加到 HashSet 中。addAll() 方法内部会调用 add() 方法，逐个检查并添加元素。

### 4.3 指定容量和负载因子构造方法

```java
public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}
```

这个构造方法允许同时指定初始容量和负载因子。适用于对内存使用有特定要求或能预估元素数量的场景。

较高的初始容量可以减少扩容次数，但会增加内存占用。较高的负载因子可以提高内存利用率，但会增加哈希冲突的可能性。

### 4.4 指定初始容量构造方法

```java
public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}
```

这个构造方法只指定初始容量，使用默认的负载因子（0.75）。适用于能预估元素数量但对负载因子没有特殊要求的场景。

### 4.5 包级私有 LinkedHashSet 构造方法

```java
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

这是一个包级私有的构造方法，仅供 LinkedHashSet 使用。boolean 类型的 dummy 参数仅用于区分不同的构造方法重载，实际值被忽略。

这个构造方法创建的内部 Map 是 LinkedHashMap 而不是 HashMap，从而为 LinkedHashSet 提供了保持插入顺序的能力。

## 五、核心方法详解

### 5.1 添加元素 add()

```java
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```

add() 方法将指定的元素添加到集合中，如果集合中尚不存在该元素。

实现原理：调用内部 HashMap 的 put() 方法，将元素作为键，PRESENT 作为值。HashMap.put() 方法的返回值是被替换的旧值，如果返回 null 表示这是一个新添加的键（原来不存在）。

返回值：true 表示元素被成功添加（原来不存在），false 表示元素已存在（没有重复添加）。

这个实现简洁地利用了 HashMap 的特性：HashMap 的键是唯一的，当尝试添加已存在的键时，put() 方法会返回原来的值而不是 null。

### 5.2 检查包含 contains()

```java
public boolean contains(Object o) {
    return map.containsKey(o);
}
```

contains() 方法检查集合中是否包含指定的元素。

实现原理：直接调用内部 HashMap 的 containsKey() 方法。HashMap.containsKey() 方法的时间复杂度为 O(1) 平均情况。

性能特点：这是 HashSet 中最常用的操作之一，其性能直接决定了 HashSet 作为成员检查数据结构的实用性。

### 5.3 删除元素 remove()

```java
public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
```

remove() 方法从集合中移除指定的元素（如果存在）。

实现原理：调用内部 HashMap 的 remove() 方法。HashMap.remove() 方法返回被移除的值，如果返回 PRESENT 表示元素确实存在于集合中。

返回值：true 表示元素被成功移除（原来存在），false 表示元素不存在于集合中。

### 5.4 清空集合 clear()

```java
public void clear() {
    map.clear();
}
```

clear() 方法移除集合中的所有元素，使集合变为空。

实现原理：直接调用内部 HashMap 的 clear() 方法。HashMap.clear() 方法会将内部数组的所有桶清空，并重置 size 为 0。

### 5.5 大小和空检查

```java
public int size() {
    return map.size();
}

public boolean isEmpty() {
    return map.isEmpty();
}
```

size() 和 isEmpty() 方法分别返回集合中的元素数量和检查集合是否为空。这两个方法都直接委托给内部 HashMap 的对应方法。

### 5.6 迭代器 iterator()

```java
public Iterator<E> iterator() {
    return map.keySet().iterator();
}
```

iterator() 方法返回集合元素的迭代器。

实现原理：调用内部 HashMap.keySet() 方法获取键集，然后返回该键集的迭代器。keySet() 方法返回的迭代器支持 fail-fast 机制。

遍历顺序：HashSet 不保证迭代顺序，元素的遍历顺序可能与添加顺序不同。如果需要保持插入顺序，应该使用 LinkedHashSet。

### 5.7 克隆操作 clone()

```java
@SuppressWarnings("unchecked")
public Object clone() {
    try {
        HashSet<E> newSet = (HashSet<E>) super.clone();
        newSet.map = (HashMap<E, Object>) map.clone();
        return newSet;
    } catch (CloneNotSupportedException e) {
        throw new InternalError(e);
    }
}
```

clone() 方法创建 HashSet 的浅拷贝。

实现步骤：首先调用 super.clone() 进行浅拷贝，这会复制 HashSet 对象本身的基本字段。然后调用 map.clone() 复制内部的 HashMap。由于 HashMap 也实现了 Cloneable 接口，这个操作会创建 HashMap 的新实例，包含相同的键值对。

浅拷贝的含义：克隆后的 HashSet 与原始 HashSet 共享相同的元素对象（浅拷贝），而不是创建元素的新副本。这意味着修改元素对象的状态会影响两个 HashSet 中对应的元素。

### 5.8 序列化 writeObject()

```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
    s.defaultWriteObject();

    s.writeInt(map.capacity());
    s.writeFloat(map.loadFactor());

    s.writeInt(map.size());

    for (E e : map.keySet())
        s.writeObject(e);
}
```

writeObject() 方法实现了自定义的序列化逻辑。

序列化步骤：首先调用 defaultWriteObject() 写出非静态和非 transient 的字段。然后写出 HashMap 的容量和负载因子。接着写出集合的大小。最后遍历所有元素并将它们写入流中。

自定义序列化的原因：HashMap 的 map 字段是 transient 的，不会被自动序列化。需要单独处理以确保正确恢复 HashMap 的状态。写出容量和负载因子是为了在反序列化时能够正确重建 HashMap。

### 5.9 反序列化 readObject()

```java
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();

    int capacity = s.readInt();
    if (capacity < 0) {
        throw new InvalidObjectException("Illegal capacity: " +
                                         capacity);
    }

    float loadFactor = s.readFloat();
    if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
        throw new InvalidObjectException("Illegal load factor: " +
                                         loadFactor);
    }

    int size = s.readInt();
    if (size < 0) {
        throw new InvalidObjectException("Illegal size: " +
                                         size);
    }

    capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
            HashMap.MAXIMUM_CAPACITY);

    map = (((HashSet<?>)this) instanceof LinkedHashSet ?
           new LinkedHashMap<E,Object>(capacity, loadFactor) :
           new HashMap<E,Object>(capacity, loadFactor));

    for (int i=0; i<size; i++) {
        @SuppressWarnings("unchecked")
            E e = (E) s.readObject();
        map.put(e, PRESENT);
    }
}
```

readObject() 方法实现了自定义的反序列化逻辑。

反序列化步骤：首先读取并验证非静态和非 transient 的字段。然后读取并验证 HashMap 的容量和负载因子。接着读取并验证集合的大小。计算实际的容量，确保 HashMap 至少有 25% 的利用率。创建新的 HashMap 或 LinkedHashMap 实例。最后读取所有元素并添加到新的 HashMap 中。

容量计算策略：capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f), HashMap.MAXIMUM_CAPACITY)。这个公式确保了计算出的容量至少能容纳所有元素，同时不会超过最大容量。

类型判断：在创建 HashMap 时，检查当前实例是否是 LinkedHashSet 的实例。如果是，创建 LinkedHashMap 以保持顺序；否则创建普通的 HashMap。

### 5.10 可分割迭代器 spliterator()

```java
public Spliterator<E> spliterator() {
    return new HashMap.KeySpliterator<E,Object>(map, 0, -1, 0, 0);
}
```

spliterator() 方法返回 Java 8 引入的可分割迭代器，用于支持并行流操作。

实现原理：创建一个 HashMap.KeySpliterator 实例，委托给内部 HashMap 的键分割迭代器。

## 六、常见面试问题与解答

### 6.1 HashSet 的实现原理

HashSet 内部使用 HashMap 实现，将元素作为 HashMap 的键存储，使用一个静态常量 PRESENT 作为值。HashMap 键的唯一性保证了 HashSet 元素的唯一性。HashSet 的所有操作（add、remove、contains 等）都委托给 HashMap 的对应方法完成。

这种设计体现了委托模式的应用：HashSet 不直接实现哈希表逻辑，而是将工作委托给 HashMap，既简化了实现，又确保了行为的一致性。

### 6.2 HashSet 如何保证元素唯一性

HashSet 通过 HashMap 来保证元素唯一性。当添加元素时，HashSet 调用 HashMap.put() 方法。HashMap.put() 方法在添加新键时会检查该键是否已存在：

- 如果键不存在，返回 null，put() 操作成功，HashSet.add() 返回 true
- 如果键已存在，返回原来的值，put() 操作会覆盖，HashSet.add() 返回 false

HashMap 判断键是否相等的逻辑是：首先比较哈希码，然后使用 equals() 方法比较内容。只有哈希码相等且 equals() 返回 true，才认为两个键相等。这确保了 HashSet 中不会有相等的元素。

### 6.3 HashSet 与 HashMap 的区别

HashSet 和 HashMap 虽然名称相似，但用途和内部结构完全不同：

| 特性 | HashSet | HashMap |
|-----|---------|---------|
| 接口 | Set | Map |
| 存储内容 | 元素 | 键值对 |
| 内部结构 | 包装 HashMap | 哈希表 |
| 唯一性保证 | 通过键唯一性 | N/A |
| 值的使用 | 使用占位对象 PRESENT | 存储实际值 |
| 核心方法 | add(), remove(), contains() | put(), get(), remove() |
| 遍历方式 | 迭代元素 | 迭代键或键值对 |

### 6.4 HashSet 与 TreeSet 的区别

HashSet 和 TreeSet 都是 Set 接口的实现，但底层实现和特性不同：

| 特性 | HashSet | TreeSet |
|-----|---------|---------|
| 底层实现 | HashMap | TreeMap |
| 元素顺序 | 无序 | 有序（自然排序或 Comparator） |
| 插入删除查找 | O(1) 平均 | O(log n) |
| 范围操作 | 不支持 | 支持（subSet, headSet, tailSet） |
| 适用场景 | 快速查找、去重 | 需要排序、范围查询 |
| 元素要求 | 需要 hashCode 和 equals | 需要 Comparable 或 Comparator |

### 6.5 HashSet 与 LinkedHashSet 的区别

LinkedHashSet 是 HashSet 的子类，增加了保持插入顺序的能力：

| 特性 | HashSet | LinkedHashSet |
|-----|---------|---------------|
| 迭代顺序 | 无序 | 插入顺序 |
| 内部实现 | HashMap | LinkedHashMap |
| 内存开销 | 较低 | 较高（双向链表） |
| 适用场景 | 只需要去重 | 去重 + 保持顺序 |

### 6.6 HashSet 可以存储 null 吗

是的，HashSet 可以存储一个 null 元素。这是因为 HashMap 允许存储一个 null 键，HashSet 利用了这个特性。

但是，由于 HashSet 中不能有重复元素，因此只能存储一个 null。如果尝试添加第二个 null，添加操作会被忽略（返回 false）。

### 6.7 HashSet 是线程安全的吗

HashSet 不是线程安全的。在多线程环境下，并发修改可能导致以下问题：

- 数据不一致
- 迭代抛出 ConcurrentModificationException
- 更严重的内存损坏

如果需要在多线程环境中使用 HashSet，可以选择以下方案：

```java
// 方案1：使用 Collections.synchronizedSet
Set<E> synchronizedSet = Collections.synchronizedSet(new HashSet<>());

// 方案2：使用 ConcurrentHashMap
Set<E> concurrentSet = ConcurrentHashMap.newKeySet();

// 方案3：使用 ConcurrentHashMap 包装
Map<E, Object> map = new ConcurrentHashMap<>();
Set<E> set = map.keySet();
```

### 6.8 HashSet 的 loadFactor 有什么作用

负载因子（loadFactor）决定了 HashSet 何时扩容。默认值为 0.75，表示当元素数量达到容量的 75% 时触发扩容。

较高的负载因子（如 0.9）会减少扩容次数，节省内存，但会增加哈希冲突的可能性，降低查找性能。较低的负载因子（如 0.5）会提高查找性能，但增加内存浪费。

选择合适的负载因子需要根据实际场景权衡：对于读多写少的场景，可以选择较高的负载因子；对于写多或对性能要求高的场景，可以选择较低的负载因子。

### 6.9 HashSet 的初始容量如何选择

初始容量应该在创建 HashSet 时根据预估的元素数量设置。如果能预估元素数量，应该设置合适的初始容量以避免频繁扩容。

计算公式：initialCapacity = (int) (expectedSize / loadFactor + 1)

例如，如果预计有 1000 个元素，使用默认负载因子 0.75，则 initialCapacity = (int) (1000 / 0.75 + 1) ≈ 1334。然后使用最近的 2 的幂次方（HashMap 的要求），即 2048。

### 6.10 HashSet 的迭代器是 fail-fast 的吗

是的，HashSet 的迭代器是 fail-fast 的。如果在迭代过程中通过迭代器自身以外的任何方式修改了集合，迭代器会抛出 ConcurrentModificationException。

这是通过在迭代器中检查 modCount 来实现的：迭代器创建时会记录当前的 modCount 值，每次操作前检查是否发生变化。如果发生变化，抛出 ConcurrentModificationException。

需要注意的是，fail-fast 只是尽最大努力检测并发修改，不能保证在所有情况下都抛出异常。因此，不应该依赖这个机制来保证正确性。

## 七、实践应用场景

### 7.1 列表去重

```java
public <T> List<T> removeDuplicates(List<T> list) {
    return new ArrayList<>(new HashSet<>(list));
}
```

使用 HashSet 可以高效地去除列表中的重复元素。HashSet 的 contains() 操作是 O(1)，因此整个去重过程的时间复杂度为 O(n)，比双重循环的 O(n²) 效率高得多。

### 7.2 成员检查

```java
public class PermissionChecker {
    private final HashSet<String> allowedPermissions;

    public PermissionChecker(Collection<String> permissions) {
        this.allowedPermissions = new HashSet<>(permissions);
    }

    public boolean hasPermission(String permission) {
        return allowedPermissions.contains(permission);
    }
}
```

HashSet 的 contains() 方法提供 O(1) 的查找性能，非常适合实现权限检查、过滤器等功能。

### 7.3 统计不重复元素数量

```java
public long countUniqueElements(Collection<?> collection) {
    return new HashSet<>(collection).size();
}
```

使用 HashSet 可以快速计算集合中不重复元素的数量。

### 7.4 集合运算

```java
public class SetOperations {
    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }

    public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.addAll(set2);
        return result;
    }

    public static <T> Set<T> difference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.removeAll(set2);
        return result;
    }
}
```

HashSet 支持各种集合运算：交集（retainAll）、并集（addAll）、差集（removeAll）。

### 7.5 实现布隆过滤器

```java
public class BloomFilter {
    private final int size;
    private final HashSet<HashFunction> hashFunctions;
    private final boolean[] bitset;

    public BloomFilter(int size, int numHashFunctions) {
        this.size = size;
        this.bitset = new boolean[size];
        this.hashFunctions = new HashSet<>();
        for (int i = 0; i < numHashFunctions; i++) {
            hashFunctions.add(new HashFunction(i, size));
        }
    }

    public void add(String element) {
        for (HashFunction hf : hashFunctions) {
            int index = hf.hash(element);
            bitset[index] = true;
        }
    }

    public boolean mightContain(String element) {
        for (HashFunction hf : hashFunctions) {
            int index = hf.hash(element);
            if (!bitset[index]) {
                return false;
            }
        }
        return true;
    }
}
```

HashSet 可以作为布隆过滤器的组成部分，用于高效地检测元素是否可能存在于集合中。

## 八、常见陷阱与最佳实践

### 8.1 正确实现 hashCode 和 equals

存储在 HashSet 中的自定义对象必须正确实现 hashCode() 和 equals() 方法。

```java
public class Person {
    private String name;
    private int age;

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }
}
```

hashCode() 的实现应该与 equals() 方法保持一致：相等的对象必须有相等的哈希码。

### 8.2 避免在遍历时修改

在迭代 HashSet 时修改集合会导致 ConcurrentModificationException：

```java
// 错误
for (Object item : set) {
    if (shouldRemove(item)) {
        set.remove(item);  // 抛出 ConcurrentModificationException
    }
}

// 正确
Iterator<Object> it = set.iterator();
while (it.hasNext()) {
    Object item = it.next();
    if (shouldRemove(item)) {
        it.remove();  // 使用迭代器的 remove 方法
    }
}
```

### 8.3 选择合适的初始容量

根据预估的元素数量设置初始容量，避免频繁扩容：

```java
// 预估有 10000 个元素
int expectedSize = 10000;
Set<E> set = new HashSet<>(expectedSize);
```

HashSet 的构造方法会计算合适的容量，确保能容纳预期数量的元素而无需立即扩容。

### 8.4 线程安全问题

HashSet 不是线程安全的，在多线程环境中需要同步：

```java
// 不推荐：每个操作单独同步
Set<E> set = new HashSet<>();
synchronized (set) {
    set.add(e);
}

// 推荐：使用 Collections.synchronizedSet
Set<E> set = Collections.synchronizedSet(new HashSet<>());

// 推荐：使用 ConcurrentHashMap.newKeySet()
Set<E> set = ConcurrentHashMap.newKeySet();
```

### 8.5 注意内存使用

HashSet 的内存使用取决于内部 HashMap。对于大型数据集，应该注意：

- 设置合适的初始容量，避免频繁扩容
- 选择合适的负载因子，平衡内存和性能
- 考虑使用更节省内存的数据结构（如 BitSet 用于布尔值）

### 8.6 最佳实践总结

为存储在 HashSet 中的自定义对象正确实现 hashCode() 和 equals()。使用迭代器的 remove() 方法而不是集合的 remove() 方法进行遍历中删除。根据预估的元素数量设置初始容量。在多线程环境中使用线程安全的替代方案。注意 HashSet 的无序性，根据需要选择 LinkedHashSet 或 TreeSet。定期检查和审计 HashSet 的使用，确保没有内存泄漏。

## 九、与其他类的比较

### 9.1 HashSet vs TreeSet

| 特性 | HashSet | TreeSet |
|-----|---------|---------|
| 底层实现 | HashMap | TreeMap |
| 元素顺序 | 无序 | 有序（自然排序或 Comparator） |
| add/remove/contains | O(1) 平均 | O(log n) |
| 空间开销 | 较低 | 较高（红黑树） |
| 范围操作 | 不支持 | 支持 |
| 适用场景 | 快速查找、去重 | 需要排序、范围查询 |

### 9.2 HashSet vs LinkedHashSet

| 特性 | HashSet | LinkedHashSet |
|-----|---------|---------------|
| 迭代顺序 | 无序 | 插入顺序 |
| 内存开销 | 较低 | 较高（双向链表） |
| 性能 | 略高 | 略低 |
| 适用场景 | 只需要去重 | 去重 + 保持顺序 |

### 9.3 HashSet vs EnumSet

| 特性 | HashSet | EnumSet |
|-----|---------|---------|
| 元素类型 | 任意对象 | 枚举类型 |
| 性能 | O(1) | O(1)，更高 |
| 内存使用 | 较高 | 非常紧凑 |
| 适用场景 | 通用 | 仅枚举值 |

### 9.4 HashSet vs CopyOnWriteArraySet

| 特性 | HashSet | CopyOnWriteArraySet |
|-----|---------|---------------------|
| 线程安全 | 否 | 是 |
| 写入性能 | 高 | 低（复制数组） |
| 读取性能 | 高 | 高（无需同步） |
| 适用场景 | 单线程或同步环境 | 读多写少并发环境 |

## 十、源码学习总结

### 10.1 设计模式应用

HashSet 源码中体现了多种设计模式的应用：

**委托模式**：HashSet 将所有操作委托给内部的 HashMap 完成，这是最核心的设计模式。通过委托，HashSet 复用了 HashMap 的哈希表实现，同时保持了 Set 接口的简洁性。

**策略模式**：通过构造函数参数（initialCapacity 和 loadFactor），用户可以选择不同的哈希表策略。

**模板方法模式**：继承自 AbstractSet，使用了 AbstractSet 提供的模板方法实现。

### 10.2 核心实现亮点

HashSet 的实现中有几个值得学习的亮点：

**简洁的实现**：HashSet 的核心方法（add、remove、contains）都非常简洁，每行代码都经过精心设计。

**占位对象设计**：使用 static final Object 作为占位值，既保证了不可变性，又避免了 null 可能带来的问题。

**自定义序列化**：通过 writeObject() 和 readObject() 方法实现了高效的序列化逻辑。

**LinkedHashSet 支持**：通过特殊构造方法支持 LinkedHashSet，体现了良好的可扩展性。

### 10.3 工程实践启示

从 HashSet 源码可以学到很多工程实践知识：

**复用已有实现**：当需要实现新功能时，首先考虑是否可以利用已有的实现。HashSet 复用 HashMap 的设计是典型的例子。

**接口与实现分离**：HashSet 实现了 Set 接口，内部使用 HashMap 实现。这种分离使得可以替换底层实现而不影响客户端代码（如 LinkedHashSet）。

**清晰的语义**：使用明确的占位对象 PRESENT 而不是 null，使代码意图更加清晰。

**考虑扩展性**：为 LinkedHashSet 预留了专门的构造方法，使得可以在不修改 HashSet 代码的情况下支持新的需求。

理解 HashSet 的源码不仅有助于更好地使用这个类，更能学习到 Java 库设计中关于委托、扩展性和工程实践的优秀思想。这些知识可以应用于自己的代码设计和系统架构中。