# TreeMap.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.util.TreeMap` 是 Java 集合框架中基于红黑树（Red-Black Tree）实现的 `NavigableMap` 接口。它继承自 `AbstractMap`，实现了 `NavigableMap`、`Cloneable` 和 `Serializable` 接口。TreeMap 的核心特性是：键值对按键排序存储；提供 O(log n) 时间复杂度的基本操作；支持范围查询（subMap、headMap、tailMap 等）；支持降序视图。

TreeMap 是 Java 1.2 引入的经典数据结构实现，与 HashMap 形成互补。HashMap 基于哈希表，提供 O(1) 的平均访问时间；TreeMap 基于红黑树，提供 O(log n) 的访问时间，但按键有序存储。

### 1.2 核心设计原则

TreeMap 的设计体现了几个重要的软件工程原则。首先是**数据结构适配**原则，通过红黑树这种自平衡二叉搜索树来实现有序映射的语义。红黑树保证了在最坏情况下仍然是 O(log n) 的时间复杂度，同时维护了平衡性。

其次是**比较器策略**原则。TreeMap 可以使用元素的自然顺序（通过 Comparable 接口），也可以使用自定义的 Comparator。这种设计提供了极大的灵活性，允许开发者根据业务需求定义排序规则。

第三是**视图模式**原则。TreeMap 提供了多个视图（keySet、values、entrySet），这些视图是"后端"的，对视图的修改会直接反映到原 map 中。这种设计避免了数据复制，提高了内存效率。

第四是**迭代器快速失败**原则。TreeMap 的迭代器实现了快速失败（fail-fast）机制，当检测到并发修改时抛出 `ConcurrentModificationException`。虽然快速失败不能保证在所有并发修改场景下都有效，但它能够快速检测到大多数并发修改问题。

### 1.3 与相关类的关系

TreeMap 在 Java 集合框架中与多个类存在密切的协作和对比关系。`AbstractMap` 是 TreeMap 的父类，提供了 Map 接口的部分默认实现。`HashMap` 是 TreeMap 的主要对比对象，两者都实现 Map 接口，但内部实现和性能特性不同。`NavigableMap` 接口定义了有序映射的扩展操作，TreeMap 实现了这个接口。`SortedMap` 是 NavigableMap 的父接口，TreeMap 也实现了这个接口。

### 1.4 应用场景

TreeMap 在以下场景中被广泛使用：需要按键排序存储键值对；需要范围查询（获取某个范围内的所有键）；需要降序遍历；需要获取最接近某个键的键；以及任何需要有序映射的场景。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.util.AbstractMap
        └─ java.util.TreeMap
```

TreeMap 继承自 `AbstractMap`，而 AbstractMap 又继承自 Object。这种继承层次使得 TreeMap 获得了 Map 接口的部分默认实现，同时专注于有序映射的特性。

### 2.2 实现的接口

TreeMap 实现了多个接口，每个接口都提供了不同的功能：

`NavigableMap` 接口定义了有序映射的扩展操作，如 `lowerEntry`、`floorEntry`、`ceilingEntry`、`higherEntry` 等。`SortedMap` 接口定义了有序映射的基本操作，如 `firstKey`、`lastKey`、`headMap`、`tailMap` 等。`Cloneable` 接口表示 TreeMap 可以被克隆。`Serializable` 接口表示 TreeMap 可以被序列化存储或传输。

### 2.3 与 HashMap 的对比

TreeMap 和 HashMap 都实现 Map 接口，但内部实现和性能特性有显著差异：

| 特性 | HashMap | TreeMap |
|------|---------|----------|
| 内部结构 | 哈希表 | 红黑树 |
| 键顺序 | 无序 | 有序（自然顺序或自定义比较器）|
| 访问时间 | O(1) 平均，O(n) 最坏 | O(log n) |
| 插入/删除 | O(1) 平均，O(n) 最坏 | O(log n) |
| 范围查询 | 不支持 | 支持（subMap、headMap 等）|
| 线程安全 | 不安全 | 不安全 |

## 三、核心字段详解

### 3.1 比较器

```java
private final Comparator<? super K> comparator;
```

`comparator` 字段用于键的比较。如果 comparator 为 null，则使用键的自然顺序（Comparable）；否则使用指定的 Comparator 进行比较。这个字段是 final 的，一旦初始化就不能改变，这确保了映射的排序规则在整个生命周期内保持一致。

### 3.2 根节点

```java
private transient Entry<K,V> root;
```

`root` 字段指向红黑树的根节点。红黑树是一种自平衡的二叉搜索树，它通过颜色标记（红或黑）和旋转操作来维护平衡性，确保树的高度始终为 O(log n)。

### 3.3 大小字段

```java
private transient int size = 0;
```

`size` 字段记录映射中当前存储的键值对数量。这个字段使得 `size()` 方法的时间复杂度为 O(1)，无需遍历整个树。

### 3.4 修改计数器

```java
private transient int modCount = 0;
```

`modCount` 字段用于快速失败迭代器。每次结构性修改（添加或删除映射）都会递增这个计数器。迭代器在创建时记录当前的 modCount，在每次操作时检查是否与映射当前的 modCount 一致，如果不一致则抛出 `ConcurrentModificationException`。

### 3.5 Entry 节点结构

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK;
    
    Entry(K key, V value, Entry<K,V> parent) {
        this.key = key;
        this.value = value;
        this.parent = parent;
        this.left = null;
        this.right = null;
        this.color = BLACK;
    }
}
```

`Entry` 是 TreeMap 的内部类，表示红黑树中的一个节点。每个节点包含五个字段：`key` 存储键；`value` 存储值；`left` 指向左子节点；`right` 指向右子节点；`parent` 指向父节点；`color` 表示节点的颜色（红或黑）。

## 四、核心方法详解

### 4.1 查找指定键的节点

```java
final Entry<K,V> getEntry(Object key) {
    if (comparator != null)
        return getEntryUsingComparator(key);
    if (key == null)
        throw new NullPointerException();
    @SuppressWarnings("unchecked")
    Comparable<? super K> k = (Comparable<? super K>) key;
    Entry<K,V> p = root;
    while (p != null) {
        int cmp = k.compareTo(p.key);
        if (cmp < 0)
            p = p.left;
        else if (cmp > 0)
            p = p.right;
        else
            return p;
    }
    return null;
}
```

`getEntry` 方法在红黑树中查找指定键的节点。这是一个标准的二叉搜索树查找算法，时间复杂度为 O(log n)。如果使用自然顺序，则使用 Comparable 进行比较；如果使用比较器，则调用 `getEntryUsingComparator` 方法。

### 4.2 插入键值对

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {
        compare(key, key);
        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    } else {
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;
    else
        parent.right = e;
    fixAfterInsertion(e);
    size++;
    modCount++;
    return null;
}
```

`put` 方法将键值对插入到映射中。如果映射已存在，则更新值；否则插入新的键值对。操作步骤如下：如果树为空，创建根节点；否则查找插入位置；如果键已存在，更新值；否则创建新节点并插入；调用 `fixAfterInsertion` 修复红黑树的平衡性。

### 4.3 删除键值对

```java
public V remove(Object key) {
    Entry<K,V> p = getEntry(key);
    if (p == null)
        return null;
    V oldValue = p.value;
    deleteEntry(p);
    return oldValue;
}
```

`remove` 方法从映射中删除指定键的映射。操作步骤如下：查找键对应的节点；保存旧值；调用 `deleteEntry` 删除节点；返回旧值。

### 4.4 范围查询方法

TreeMap 提供了多个范围查询方法：

```java
public Map.Entry<K,V> lowerEntry(K key) {
    return exportEntry(getLowerEntry(key));
}

public Map.Entry<K,V> floorEntry(K key) {
    return exportEntry(getFloorEntry(key));
}

public Map.Entry<K,V> ceilingEntry(K key) {
    return exportEntry(getCeilingEntry(key));
}

public Map.Entry<K,V> higherEntry(K key) {
    return exportEntry(getHigherEntry(key));
}
```

这些方法分别返回：小于指定键的最大键；小于等于指定键的最大键；大于等于指定键的最小键；大于指定键的最小键。

### 4.5 范围映射方法

```java
public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                   K toKey,   boolean toInclusive) {
    return new AscendingSubMap<>(this,
                                     false, fromKey, fromInclusive,
                                     false, toKey,   toInclusive);
}

public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
    return new AscendingSubMap<>(this,
                                     false, toKey, inclusive,
                                     true,  null,    true);
}

public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
    return new AscendingSubMap<>(this,
                                     false, fromKey, inclusive,
                                     true,  null,    true);
}
```

这些方法返回映射的子映射视图，视图中的键在指定范围内。

## 五、红黑树操作详解

### 5.1 修复插入后的平衡

```java
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;
    while (x != null && x != root && x.parent.color == RED) {
        if (parentOf(x) == leftOf(parentOf(x))) {
            setColor(parentOf(x), BLACK);
            x = parentOf(x);
        } else {
            setColor(x, BLACK);
            x = parentOf(x);
        }
    }
    root.color = BLACK;
}
```

`fixAfterInsertion` 方法在插入新节点后修复红黑树的性质。红黑树有五个性质需要维护：节点是红色或黑色；根节点是黑色；所有叶子节点（NIL 节点）是黑色；如果一个节点是红色，则其两个子节点都是黑色；从任一节点到其所有叶子节点的路径都包含相同数量的黑色节点。

### 5.2 删除节点

```java
private void deleteEntry(Entry<K,V> p) {
    modCount++;
    size--;
    if (p.left == null && p.right == null) {
        if (p.parent != null)
            root = null;
        else if (p.parent.left == p)
            p.parent.left = null;
        else
            p.parent.right = null;
    } else if (p.left != null) {
        Entry<K,V> replacement = successor(p);
        p.key = replacement.key;
        p.value = replacement.value;
        p.right = replacement.right;
        if (replacement.parent != null)
            root = replacement;
        else if (p == replacement.parent.left)
            replacement.parent.left = replacement;
        else
            replacement.parent.right = replacement;
        p.left = replacement.left;
        if (p.left != null)
            replacement.parent = p;
        fixAfterDeletion(replacement);
    } else {
        Entry<K,V> s = successor(p);
        if (p.parent != null) {
            root = s;
        } else if (p.parent.right == s) {
            p.parent.right = s;
        } else {
            p.parent.left = s;
        }
        if (s.right != null) {
            p.key = s.key;
            p.value = s.value;
            p.right = s.right;
            if (s.parent == p)
                s.parent = p;
        } else {
            transplant(p, s);
            fixAfterDeletion(p);
        }
    }
}
```

`deleteEntry` 方法从红黑树中删除指定节点。删除操作比插入操作更复杂，需要处理多种情况：节点是叶子节点；节点只有一个子节点；节点有两个子节点。

### 5.3 左旋和右旋

红黑树通过旋转操作来维护平衡性。左旋和右旋是红黑树的基本操作。

## 六、视图实现

### 6.1 键集合视图

TreeMap 提供了键集合的视图，这个视图是"后端"的，对视图的修改会直接反映到原 map 中。

### 6.2 值集合视图

TreeMap 提供了值集合的视图，这个视图也是"后端"的。

### 6.3 条目集合视图

TreeMap 提供了条目集合的视图，这个视图包含所有键值对。

## 七、设计模式分析

### 7.1 策略模式

TreeMap 通过 `Comparator` 接口实现了策略模式。元素的比较策略可以在运行时指定，这使得同一个 TreeMap 可以适应不同的排序需求。

### 7.2 模板方法模式

红黑树的各种操作（插入、删除、旋转）体现了模板方法模式。虽然不是典型的模板方法，但这些操作都遵循相同的算法框架，只是在具体实现上有所不同。

### 7.3 迭代器模式

TreeMap 的迭代器实现了经典的迭代器模式。迭代器提供了一种统一的方式来遍历映射元素，而不暴露映射的内部结构。

## 八、常见使用模式

### 8.1 有序映射

```java
TreeMap<String, Integer> map = new TreeMap<>();
map.put("apple", 3);
map.put("banana", 2);
map.put("cherry", 5);

for (String key : map.keySet()) {
    System.out.println(key + ": " + map.get(key));
}
```

### 8.2 范围查询

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "one");
map.put(3, "three");
map.put(5, "five");
map.put(7, "seven");

SortedMap<Integer, String> subMap = map.subMap(3, true, 7, true);
```

### 8.3 降序遍历

```java
TreeMap<String, Integer> map = new TreeMap<>();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);

NavigableMap<String, Integer> descending = map.descendingMap();
for (Map.Entry<String, Integer> entry : descending.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

## 九、常见问题与注意事项

### 9.1 线程安全性

TreeMap 不是线程安全的，不应在多线程环境中共享使用。如果需要在多线程环境下使用，应该使用 `ConcurrentSkipListMap`。

### 9.2 比较器的一致性

TreeMap 要求比较器必须与 equals 方法一致。如果两个键通过比较器比较相等，则它们的 equals 方法也应该返回 true。如果不满足这个条件，TreeMap 的行为可能不符合 Map 接口的契约。

### 9.3 null 键的处理

TreeMap 不允许 null 键，尝试插入 null 键会抛出 NullPointerException。

### 9.4 修改计数器

TreeMap 的修改计数器用于快速失败迭代器。这个机制不能保证在所有并发修改场景下都有效。

## 十、面试常见问题

### 10.1 TreeMap 的底层实现是什么？

TreeMap 使用红黑树（Red-Black Tree）作为底层实现。红黑树是一种自平衡的二叉搜索树，它通过颜色标记和旋转操作来维护平衡性，确保树的高度始终为 O(log n)。

### 10.2 TreeMap 和 HashMap 的区别？

TreeMap 基于红黑树，按键有序存储；HashMap 基于哈希表，无序存储。TreeMap 的访问时间为 O(log n)，HashMap 为 O(1) 平均。TreeMap 支持范围查询，HashMap 不支持。TreeMap 的迭代器按键有序遍历，HashMap 的迭代器顺序不确定。

### 10.3 TreeMap 的时间复杂度是多少？

put/get/remove：O(log n)；firstKey/lastKey：O(log n)；lowerKey/floorKey/ceilingKey/higherKey：O(log n)；subMap/headMap/tailMap：O(log n)。

### 10.4 TreeMap 是线程安全的吗？

TreeMap 不是线程安全的。如果需要在多线程环境下使用，应该使用 `ConcurrentSkipListMap`。

### 10.5 TreeMap 如何实现红黑树？

TreeMap 通过 Entry 节点的颜色标记（红或黑）和旋转操作来维护红黑树的五个性质。插入和删除操作后，TreeMap 会调用 `fixAfterInsertion` 和 `fixAfterDeletion` 方法来修复树的平衡性。

### 10.6 TreeMap 的比较器如何工作？

TreeMap 可以使用元素的自然顺序（通过 Comparable 接口），也可以使用自定义的 Comparator。如果 comparator 为 null，则使用自然顺序；否则使用指定的 Comparator 进行键的比较。

## 十一、与其他类的对比

### 11.1 TreeMap 与 HashMap

TreeMap 和 HashMap 都实现 Map 接口，但内部实现和性能特性有显著差异。TreeMap 基于红黑树，按键有序存储；HashMap 基于哈希表，无序存储。TreeMap 的访问时间为 O(log n)，HashMap 为 O(1) 平均。

### 11.2 TreeMap 与 LinkedHashMap

LinkedHashMap 是 HashMap 的有序版本，它维护插入顺序或访问顺序。TreeMap 维护键的自然顺序或自定义比较器顺序。LinkedHashMap 的访问时间为 O(1)，TreeMap 为 O(log n)。

### 11.3 TreeMap 与 ConcurrentSkipListMap

`ConcurrentSkipListMap` 是 TreeMap 的线程安全版本，位于 `java.util.concurrent` 包中。ConcurrentSkipListMap 的所有操作都是线程安全的，使用跳表（Skip List）实现。

### 11.4 TreeMap 与 Guava 的 TreeMultimap

Google Guava 库提供了 `TreeMultimap`，它允许一个键对应多个值：

```java
// Guava 的 TreeMultimap
TreeMultimap<String, Integer> multimap = TreeMultimap.create();
multimap.put("key", 1);
multimap.put("key", 2);
multimap.put("key", 3);

Collection<Integer> values = multimap.get("key");
```

TreeMap 只允许一个键对应一个值，而 TreeMultimap 允许一个键对应多个值。但引入 Guava 增加了项目依赖。
