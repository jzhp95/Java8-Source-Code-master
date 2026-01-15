# LinkedHashMap 源码深度解析

## 一、类概述与设计理念

### 1.1 基本信息

LinkedHashMap 是 Java 集合框架中 HashMap 的子类，位于 java.util 包中。它在 HashMap 的基础上维护了一个双向链表，记录了元素的插入顺序或访问顺序，从而提供可预测的迭代顺序。与 HashMap 的无序性不同，LinkedHashMap 保证元素的迭代顺序。

LinkedHashMap 的设计理念体现了"在保持 HashMap 高效性能的同时，提供有序性保证"的核心思想。它继承了 HashMap 的所有优秀特性，包括常数时间复杂度的基本操作（add、contains、remove）、优秀的空间利用率和可调整的负载因子。同时，通过维护双向链表，LinkedHashMap 提供了两种迭代顺序：插入顺序（默认）和访问顺序。

### 1.2 设计特点

LinkedHashMap 的设计体现了以下几个核心特点：

**双重数据结构**：LinkedHashMap 同时使用了哈希表和双向链表两种数据结构。哈希表提供 O(1) 时间复杂度的键值查找，双向链表维护元素的顺序关系。这种设计虽然增加了一定的空间开销和维护成本，但提供了 HashMap 所不具备的有序性保证。

**两种迭代顺序**：LinkedHashMap 支持两种迭代顺序。插入顺序（默认）按照元素被添加到 Map 的顺序进行迭代，即使是键已存在时的更新操作也不会改变顺序。访问顺序按照元素最后一次被访问的时间进行迭代，最近访问的元素排在链表末尾，这种模式特别适合实现 LRU 缓存。

**可预测的迭代顺序**：与 HashMap 的不确定迭代顺序不同，LinkedHashMap 提供完全可预测的迭代顺序。无论是遍历整个 Map、获取键集合还是值集合，元素的顺序都是一致的和可预期的。

**回调机制**：LinkedHashMap 提供了一系列回调方法（afterNodeAccess、afterNodeInsertion、afterNodeRemoval），允许子类在节点被访问、插入或移除时执行自定义逻辑。这为实现 LRU 缓存等功能提供了便利。

**继承灵活性**：作为 HashMap 的子类，LinkedHashMap 复用了 HashMap 的所有核心逻辑，同时通过覆盖关键方法插入了自己的链表维护逻辑。这种设计既避免了代码重复，又提供了足够的灵活性。

### 1.3 适用场景

LinkedHashMap 适用于多种特定场景，理解其适用边界对于正确选择数据结构至关重要。

在**需要保持插入顺序**的场景中，LinkedHashMap 是最佳选择。例如，在 Web 开发中需要按照请求参数定义的顺序处理参数，或者在数据处理流程中需要按照数据到达的顺序进行处理。

在**实现 LRU 缓存**的场景中，LinkedHashMap 是天然的选择。通过设置 accessOrder 为 true 并覆盖 removeEldestEntry 方法，可以轻松实现一个固定大小的 LRU 缓存。当缓存满时，最久未被访问的元素会被自动淘汰。

在**需要有序遍历**的场景中，LinkedHashMap 提供了优于 TreeMap 的性能。虽然 TreeMap 也提供有序遍历，但其时间复杂度为 O(log n)，而 LinkedHashMap 的遍历时间与容量无关，只与元素数量成正比。

在**FIFO 队列实现**的场景中，可以利用 LinkedHashMap 的插入顺序特性实现简单的 FIFO 队列。新元素添加到尾部，旧元素从头部移除。

然而，在**只需要高效查找**的场景中，LinkedHashMap 不是最佳选择，因为维护链表需要额外的开销。此时应该使用 HashMap 以获得更好的性能。

### 1.4 性能特征总结

从时间复杂度的角度分析 LinkedHashMap 的性能特征：

| 操作类型 | 平均时间复杂度 | 最坏时间复杂度 | 说明 |
|---------|--------------|--------------|------|
| get(Object key) | O(1) | O(1) | 与 HashMap 相同 |
| put(K key, V value) | O(1) | O(1) | 包含链表更新 |
| remove(Object key) | O(1) | O(1) | 包含链表更新 |
| containsKey(Object key) | O(1) | O(1) | 与 HashMap 相同 |
| 遍历（迭代器） | O(n) | O(n) | 与元素数量成正比 |
| 容量相关操作 | O(1) | O(1) | 由 HashMap 处理 |

与 HashMap 相比，LinkedHashMap 的主要性能差异在于：遍历性能更优（与 size 成正比而非与 capacity 成正比），但写入操作有轻微额外开销（用于维护双向链表）。

## 二、继承结构与接口实现

### 2.1 类继承体系

LinkedHashMap 的继承结构设计体现了 Java 集合框架的优秀设计思想：

```
AbstractMap<K,V>
    └── HashMap<K,V>
        └── LinkedHashMap<K,V>
```

LinkedHashMap 直接继承自 HashMap，而 HashMap 继承自 AbstractMap。这种继承结构使得 LinkedHashMap 能够复用 HashMap 的所有核心功能，同时通过覆盖关键方法插入自己的逻辑。

AbstractMap 提供了 Map 接口的基础实现，包括 equals()、hashCode()、toString() 等方法的默认实现。HashMap 在此基础上实现了完整的哈希表逻辑，包括哈希计算、桶定位、扩容机制等。LinkedHashMap 则通过覆盖这些方法，添加了双向链表的维护逻辑。

### 2.2 接口实现关系

LinkedHashMap 实现了 Map 接口，提供了所有 Map 操作的标准实现。由于继承自 HashMap，它自动继承了 HashMap 实现的 Cloneable 和 Serializable 接口。

作为 Map 接口的实现，LinkedHashMap 提供了 get()、put()、remove()、containsKey()、containsValue()、size()、isEmpty() 等核心方法。所有这些方法都直接使用 HashMap 的实现，不需要额外修改。

LinkedHashMap 还支持 Map 接口提供的各种视图操作，包括 keySet()、values()、entrySet()。这些视图的迭代器会按照 LinkedHashMap 的顺序遍历元素。

### 2.3 与 HashMap 的关系

LinkedHashMap 与 HashMap 的关系是"扩展"而非"重写"。LinkedHashMap 复用了 HashMap 的几乎所有功能，只在以下关键点插入自己的逻辑：

**节点创建**：覆盖 newNode() 和 newTreeNode() 方法，在创建新节点时将其添加到双向链表的末尾。

**节点移除**：覆盖 afterNodeRemoval() 方法，从双向链表中移除节点。

**节点访问**：覆盖 afterNodeAccess() 方法（仅在访问顺序模式下），将访问的节点移动到链表末尾。

**节点插入后**：覆盖 afterNodeInsertion() 方法，检查是否需要移除最老的元素。

这种设计模式被称为"模板方法模式"：HashMap 提供了算法骨架，LinkedHashMap 通过覆盖回调方法来修改特定行为。

## 三、核心字段解析

### 3.1 双向链表字段

```java
transient LinkedHashMap.Entry<K,V> head;
transient LinkedHashMap.Entry<K,V> tail;
```

head 和 tail 是 LinkedHashMap 的核心字段，分别指向双向链表的头节点（最老）和尾节点（最新）。

**head 字段**：指向链表中的第一个节点，也就是最老的元素。在插入顺序模式下，是最先插入的元素；在访问顺序模式下，是最久未被访问的元素。当链表为空时，head 为 null。

**tail 字段**：指向链表中的最后一个节点，也就是最新的元素。在插入顺序模式下，是最后插入的元素；在访问顺序模式下，是最近访问的元素。当链表为空时，tail 为 null。

这两个字段使用 transient 修饰，表示在序列化时不会被自动保存。LinkedHashMap 通过 custom 的 writeObject 和 readObject 方法来处理序列化问题。

链表的不变式：对于非空链表，head 不为 null 且 head.before 为 null，tail 不为 null 且 tail.after 为 null。对于每个中间节点，节点的 before 和 after 字段都指向链表中的相邻节点。

### 3.2 迭代顺序标志

```java
final boolean accessOrder;
```

accessOrder 字段决定了 LinkedHashMap 的迭代顺序。这是一个 final 字段，一旦创建就不能修改。

**accessOrder = false（默认值）**：使用插入顺序进行迭代。元素按照被添加到 Map 的顺序进行迭代，即使是更新已有键的操作也不会改变顺序。这是 LinkedHashMap 的默认行为。

**accessOrder = true**：使用访问顺序进行迭代。每次调用 get() 方法或调用 put() 方法更新已有键时，被访问的节点会被移动到链表的末尾。因此，链表的头部始终是最近最久未被访问的元素。

这个字段在构造方法中设置，对于不同的构造方法有不同的默认值。所有接受 accessOrder 参数的构造方法都允许显式指定迭代顺序。

### 3.3 Entry 节点类

```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;

    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

LinkedHashMap.Entry 是 LinkedHashMap 中使用的节点类，继承自 HashMap.Node。它增加了两个指向相邻节点的指针：

**before 字段**：指向链表中当前节点的前一个节点。对于链表中的第一个节点（head），before 为 null。

**after 字段**：指向链表中当前节点的后一个节点。对于链表中的最后一个节点（tail），after 为 null。

通过 before 和 after 指针，所有 Entry 形成一个双向链表。插入、删除、移动操作都需要更新这些指针以维护链表的完整性。

继承 HashMap.Node 使得 LinkedHashMap 的 Entry 可以直接作为 HashMap 的节点使用，复用了 HashMap 的哈希计算、桶定位等逻辑。

## 四、构造方法分析

### 4.1 插入顺序构造方法

```java
public LinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
}

public LinkedHashMap(int initialCapacity) {
    super(initialCapacity);
    accessOrder = false;
}

public LinkedHashMap() {
    super();
    accessOrder = false;
}

public LinkedHashMap(Map<? extends K, ? extends V> m) {
    super();
    accessOrder = false;
    putMapEntries(m, false);
}
```

这些构造方法创建使用插入顺序的 LinkedHashMap。accessOrder 被设置为 false，这是 LinkedHashMap 的默认模式。

**默认构造方法**：创建一个空的 LinkedHashMap，使用默认初始容量（16）和默认负载因子（0.75）。

**指定初始容量构造方法**：创建一个空的 LinkedHashMap，使用指定的初始容量和默认负载因子。

**指定初始容量和负载因子构造方法**：创建一个空的 LinkedHashMap，使用指定的初始容量和负载因子。

**从 Map 构造方法**：创建一个 LinkedHashMap，包含指定 Map 中的所有映射。映射的顺序按照指定 Map 的 entrySet 迭代顺序确定。

### 4.2 访问顺序构造方法

```java
public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}
```

这是 LinkedHashMap 唯一允许指定 accessOrder 的构造方法。通过设置 accessOrder 为 true，可以创建使用访问顺序的 LinkedHashMap。

这个构造方法通常用于实现 LRU 缓存。在访问顺序模式下，每次访问元素（通过 get() 方法或更新已存在的键）都会将元素移动到链表的末尾，使得链表头部始终是最近最久未被访问的元素。

## 五、核心方法详解

### 5.1 节点创建方法

```java
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p =
        new LinkedHashMap.Entry<K,V>(hash, key, value, e);
    linkNodeLast(p);
    return p;
}

TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
    TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
    linkNodeLast(p);
    return p;
}
```

newNode() 和 newTreeNode() 方法在 HashMap 创建新节点时被调用。LinkedHashMap 覆盖了这些方法，在创建节点后将其添加到双向链表的末尾。

linkNodeLast() 方法负责将新节点添加到链表的末尾：

```java
private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    LinkedHashMap.Entry<K,V> last = tail;
    tail = p;
    if (last == null)
        head = p;
    else {
        p.before = last;
        last.after = p;
    }
}
```

这个方法首先保存当前的 tail 节点，然后将新节点设置为 tail。如果链表为空（last 为 null），同时更新 head 指针。否则，将新节点链接到原 tail 节点之后。

### 5.2 节点移除回调

```java
void afterNodeRemoval(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p =
        (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
    p.before = p.after = null;
    if (b == null)
        head = a;
    else
        b.after = a;
    if (a == null)
        tail = b;
    else
        a.before = b;
}
```

afterNodeRemoval() 方法在 HashMap 从哈希表中移除节点后被调用。它负责从双向链表中移除该节点。

移除操作的步骤：首先获取被移除节点的前驱（b）和后继（a），然后将被移除节点的指针置为 null。接下来，如果前驱为 null，说明被移除的是 head，需要更新 head 指向后继；否则将前驱的 after 指向后继。类似地，如果后继为 null，说明被移除的是 tail，需要更新 tail 指向前驱；否则将后继的 before 指向前驱。

这个方法确保了即使在并发情况下（虽然 LinkedHashMap 本身不是线程安全的），链表也能保持正确的状态。

### 5.3 节点插入后回调

```java
void afterNodeInsertion(boolean evict) {
    LinkedHashMap.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        removeNode(hash(key), key, null, false, true);
    }
}
```

afterNodeInsertion() 方法在 HashMap 完成节点插入后被调用。它检查是否需要移除最老的元素，从而支持实现固定大小的缓存。

removeEldestEntry() 方法是一个受保护的方法，默认实现始终返回 false：

```java
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false;
}
```

子类可以覆盖这个方法来实现缓存淘汰策略。例如，实现 LRU 缓存时，可以检查当前大小是否超过预设的缓存容量：

```java
@Override
protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
    return size() > MAX_CACHE_SIZE;
}
```

### 5.4 节点访问回调

```java
void afterNodeAccess(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
```

afterNodeAccess() 方法在 HashMap 访问节点后被调用，仅在 accessOrder 为 true 时执行操作。这个方法将访问的节点移动到链表的末尾，使其成为最新的元素。

访问操作包括 get() 方法和 put() 方法中对已存在键的更新。由于这些操作都会调用 afterNodeAccess()，因此链表会反映元素的访问顺序。

需要注意的是，这个方法会增加 modCount，这在某种程度上与 HashMap 的 fail-fast 机制相关。

### 5.5 节点替换回调

```java
Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
    LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
    LinkedHashMap.Entry<K,V> t =
        new LinkedHashMap.Entry<K,V>(q.hash, q.key, q.value, next);
    transferLinks(q, t);
    return t;
}

TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
    LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
    TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
    transferLinks(q, t);
    return t;
}
```

replacementNode() 和 replacementTreeNode() 方法在 HashMap 进行节点替换时被调用，例如在 resize 操作中。这些方法创建新节点并复制原节点的链表指针。

transferLinks() 方法负责复制链表链接：

```java
private void transferLinks(LinkedHashMap.Entry<K,V> src,
                           LinkedHashMap.Entry<K,V> dst) {
    LinkedHashMap.Entry<K,V> b = dst.before = src.before;
    LinkedHashMap.Entry<K,V> a = dst.after = src.after;
    if (b == null)
        head = dst;
    else
        b.after = dst;
    if (a == null)
        tail = dst;
    else
        a.before = dst;
}
```

这个方法将 src 节点的 before 和 after 链接复制到 dst 节点，同时更新相邻节点的指针，使其指向 dst 而不是 src。

### 5.6 获取方法覆盖

```java
public V get(Object key) {
    Node<K,V> e;
    if ((e = getNode(hash(key), key)) == null)
        return null;
    if (accessOrder)
        afterNodeAccess(e);
    return e.value;
}
```

LinkedHashMap 覆盖了 get() 方法。如果 accessOrder 为 true，在找到节点后会调用 afterNodeAccess() 将其移动到链表末尾。

对于插入顺序的 LinkedHashMap（accessOrder = false），这个方法的行为与 HashMap 完全相同。

## 六、常见面试问题与解答

### 6.1 LinkedHashMap 与 HashMap 的区别

这是最常见的面试问题之一，需要从多个维度进行比较。

**底层数据结构**：HashMap 只有一个哈希表数组，LinkedHashMap 在此基础上增加了双向链表。双向链表用于维护元素的顺序。

**迭代顺序**：HashMap 不保证任何迭代顺序，元素的遍历顺序是不确定的。LinkedHashMap 保证按照插入顺序或访问顺序进行遍历，顺序是完全可预测的。

**性能差异**：由于需要维护双向链表，LinkedHashMap 在写入操作时有轻微的性能开销（O(1) vs O(1)，但常数因子更大）。但在遍历操作上，LinkedHashMap 更快（O(n) vs O(n + capacity)），因为只遍历实际存在的元素。

**继承关系**：LinkedHashMap 继承自 HashMap，复用了 HashMap 的所有核心逻辑。HashMap 不继承任何 Map 实现类。

**典型应用**：HashMap 适用于只需要高效查找的场景。LinkedHashMap 适用于需要保持顺序的场景，特别是实现 LRU 缓存。

### 6.2 LinkedHashMap 如何实现 LRU 缓存

LinkedHashMap 是实现 LRU 缓存的理想选择，因为它天然支持访问顺序。主要步骤如下：

**第一步：创建访问顺序的 LinkedHashMap**：

```java
LinkedHashMap<Integer, String> cache = new LinkedHashMap<>(16, 0.75f, true);
```

**第二步：覆盖 removeEldestEntry 方法**：

```java
@Override
protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
    return size() > MAX_CAPACITY;
}
```

**第三步：使用缓存**：

```java
public V get(K key) {
    return cache.get(key);  // 访问操作会将元素移到末尾
}

public void put(K key, V value) {
    cache.put(key, value);  // 如果键已存在，会将元素移到末尾
}
```

当缓存满时，下次调用 put() 方法时，removeEldestEntry() 会返回 true，最老的元素（链表头部的元素）会被自动移除。

### 6.3 LinkedHashMap 的两种顺序模式

LinkedHashMap 支持两种顺序模式，由 accessOrder 字段控制。

**插入顺序模式（accessOrder = false，默认）**：

元素的迭代顺序与插入顺序相同。更新已存在键的值不会改变其位置。新插入的元素总是添加到链表的末尾。

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
map.put("A", 10);  // 更新" A"，但顺序不变

// 迭代结果：A -> B -> C
```

**访问顺序模式（accessOrder = true）**：

元素的迭代顺序按照最后访问时间排序，最近访问的元素排在最后。访问操作包括 get() 和 put() 对已存在键的更新。

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
map.get("A");  // 访问" A"，将其移到末尾

// 迭代结果：B -> C -> A
```

### 6.4 LinkedHashMap 是线程安全的吗

LinkedHashMap 不是线程安全的。与 HashMap 一样，如果多个线程并发访问 LinkedHashMap，且至少有一个线程进行结构性修改，必须在外部进行同步。

```java
// 使用 Collections.synchronizedMap 包装
Map<K, V> synchronizedMap = Collections.synchronizedMap(new LinkedHashMap<>());

// 在多线程环境中使用时进行同步
synchronized (synchronizedMap) {
    Iterator<Map.Entry<K, V>> i = synchronizedMap.entrySet().iterator();
    while (i.hasNext()) {
        // 处理元素
    }
}
```

如果需要在并发环境中使用支持访问顺序的缓存，可以考虑使用 LinkedBlockingQueue 或 ConcurrentLinkedQueue，或者使用 guava 的 Cache 实现。

### 6.5 LinkedHashMap 的遍历性能

LinkedHashMap 的遍历性能优于 HashMap，特别是在容量远大于元素数量时。

**HashMap 的遍历**：需要遍历哈希表的所有桶，即使大多数桶是空的。遍历时间是 O(n + capacity)。

**LinkedHashMap 的遍历**：只遍历实际存在的元素，通过双向链表直接访问。遍历时间是 O(n)。

因此，当需要频繁遍历 Map 时，LinkedHashMap 是更好的选择。

```java
// HashMap 遍历：可能遍历很多空桶
for (Map.Entry<K, V> entry : hashMap.entrySet()) {
    // 处理 entry
}

// LinkedHashMap 遍历：只遍历存在的元素
for (Map.Entry<K, V> entry : linkedHashMap.entrySet()) {
    // 处理 entry
}
```

### 6.6 removeEldestEntry 方法的作用

removeEldestEntry() 是 LinkedHashMap 提供的钩子方法，用于实现缓存淘汰策略。

**默认实现**：始终返回 false，不淘汰任何元素。

```java
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false;
}
```

**使用场景**：覆盖此方法可以实现固定大小的缓存。当方法返回 true 时，LinkedHashMap 会自动移除最老的元素（链表头部的元素）。

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCache(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
```

### 6.7 LinkedHashMap 如何维护双向链表

LinkedHashMap 通过以下方式维护双向链表：

**插入时**：newNode() 方法在创建新节点后调用 linkNodeLast()，将新节点添加到链表末尾。

**访问时**：afterNodeAccess() 方法（仅在 accessOrder 为 true 时）将访问的节点从当前位置移除，然后添加到链表末尾。

**移除时**：afterNodeRemoval() 方法将节点从链表中移除，更新相邻节点的指针。

**扩容时**：replacementNode() 和 replacementTreeNode() 方法使用 transferLinks() 复制链表链接。

所有这些操作都是 O(1) 时间复杂度，因此维护链表的开销是常数级别的。

### 6.8 LinkedHashMap 的节点结构

LinkedHashMap 的 Entry 节点继承自 HashMap.Node，增加了两个指针：

```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;

    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

**before 指针**：指向前一个节点，用于反向遍历和快速访问。

**after 指针**：指向后一个节点，用于正向遍历。

双向链表形成一个循环结构吗？不，LinkedHashMap 的链表不是循环的。head.before 为 null，tail.after 为 null。这种设计简化了边界处理。

### 6.9 LinkedHashMap 的序列化

LinkedHashMap 需要特殊处理序列化，因为它需要保存链表顺序。

默认序列化会丢失链表信息。LinkedHashMap 通过覆盖 writeObject() 和 readObject() 方法来处理序列化：

```java
private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeInt(size);
    for (Map.Entry<K,V> e : entrySet()) {
        s.writeObject(e.getKey());
        s.writeObject(e.getValue());
    }
}
```

在序列化时，LinkedHashMap 按照链表的顺序写出键值对。在反序列化时，会重新构建链表。

### 6.10 LinkedHashMap 与 LinkedHashSet 的关系

LinkedHashSet 使用 LinkedHashMap 实现，类似于 HashSet 使用 HashMap 实现：

```java
public class LinkedHashSet<E> extends HashSet<E>
    implements Set<E>, Cloneable, Serializable {

    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);  // true 表示访问顺序
    }
}
```

LinkedHashSet 调用父类 HashSet 的构造方法，传入一个 dummy 参数为 true，这个参数最终传递给 LinkedHashMap 的构造方法，设置 accessOrder 为 true。

## 七、实践应用场景

### 7.1 实现 LRU 缓存

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;

    public LRUCache(int maxEntries) {
        super(16, 0.75f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

    public V get(Object key) {
        return super.get(key);
    }

    public V put(K key, V value) {
        return super.put(key, value);
    }
}
```

### 7.2 实现有序配置管理

```java
public class OrderedConfig {
    private final LinkedHashMap<String, String> config = new LinkedHashMap<>();

    public void setProperty(String key, String value) {
        config.put(key, value);
    }

    public String getProperty(String key) {
        return config.get(key);
    }

    public Iterator<Map.Entry<String, String>> getOrderedProperties() {
        return config.entrySet().iterator();
    }
}
```

### 7.3 实现 FIFO 队列

```java
public class FIFOQueue<E> {
    private final LinkedHashMap<Integer, E> queue = new LinkedHashMap<>();
    private int counter = 0;

    public void offer(E element) {
        queue.put(counter++, element);
    }

    public E poll() {
        Map.Entry<Integer, E> first = queue.entrySet().iterator().next();
        E element = first.getValue();
        queue.remove(first.getKey());
        return element;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
```

### 7.4 实现访问顺序的缓存装饰器

```java
public class CacheDecorator<K, V> implements Map<K, V> {
    private final Map<K, V> target;
    private final int maxSize;
    private final LinkedHashMap<K, V> accessOrder;

    public CacheDecorator(Map<K, V> target, int maxSize) {
        this.target = target;
        this.maxSize = maxSize;
        this.accessOrder = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    public V get(Object key) {
        V value = target.get(key);
        if (value != null) {
            accessOrder.get(key);  // 记录访问
        }
        return value;
    }

    @Override
    public V put(K key, V value) {
        if (accessOrder.size() >= maxSize && !accessOrder.containsKey(key)) {
            K eldestKey = accessOrder.keySet().iterator().next();
            target.remove(eldestKey);
            accessOrder.remove(eldestKey);
        }
        target.put(key, value);
        accessOrder.put(key, value);
        return value;
    }

    // 其他方法委托给 target...
}
```

## 八、常见陷阱与最佳实践

### 8.1 理解两种顺序模式的区别

在创建 LinkedHashMap 时，需要明确选择正确的顺序模式。默认的插入顺序适用于大多数场景。如果需要 LRU 行为，必须将 accessOrder 设置为 true。

```java
// 插入顺序（默认）
LinkedHashMap<K, V> insertionOrder = new LinkedHashMap<>();

// 访问顺序（LRU）
LinkedHashMap<K, V> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
```

### 8.2 正确实现 removeEldestEntry

removeEldestEntry() 方法在每次插入后被调用。实现时需要注意：

- 不要在 removeEldestEntry() 中进行复杂的计算或 I/O 操作
- 返回 true 表示应该移除元素，返回 false 表示保留
- 方法接收的参数是当前最老的元素

```java
// 正确的实现
@Override
protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > MAX_SIZE;
}
```

### 8.3 线程安全问题

LinkedHashMap 不是线程安全的。在多线程环境中使用时需要进行外部同步：

```java
// 好的做法：创建时包装
Map<K, V> syncMap = Collections.synchronizedMap(new LinkedHashMap<>());

// 使用时注意迭代的同步
synchronized (syncMap) {
    for (Map.Entry<K, V> entry : syncMap.entrySet()) {
        // 处理 entry
    }
}
```

### 8.4 避免不必要的访问顺序

如果不需要访问顺序（LRU 行为），不要使用 accessOrder = true。访问顺序模式会在每次 get() 操作时触发 afterNodeAccess()，增加不必要的开销。

```java
// 如果只需要保持插入顺序，使用默认值
LinkedHashMap<K, V> map = new LinkedHashMap<>();  // accessOrder = false

// 如果需要 LRU 行为，显式设置
LinkedHashMap<K, V> lru = new LinkedHashMap<>(16, 0.75f, true);
```

### 8.5 合理设置初始容量

与 HashMap 一样，根据预估的元素数量设置初始容量可以避免频繁扩容：

```java
// 根据预估大小设置初始容量
int expectedSize = 1000;
int initialCapacity = (int) (expectedSize / 0.75) + 1;
LinkedHashMap<K, V> map = new LinkedHashMap<>(initialCapacity);
```

### 8.6 最佳实践总结

根据需求选择正确的顺序模式。使用默认的插入顺序，除非需要 LRU 行为。在多线程环境中使用 Collections.synchronizedMap() 包装。根据预估大小设置初始容量。覆盖 removeEldestEntry() 时保持简单。理解 get() 操作在访问顺序模式下的副作用。遍历时使用 entrySet() 而非 keySet() + get()，更高效。

## 九、与其他类的比较

### 9.1 LinkedHashMap vs HashMap

| 特性 | HashMap | LinkedHashMap |
|-----|---------|---------------|
| 迭代顺序 | 无序 | 插入顺序或访问顺序 |
| 底层结构 | 哈希表 | 哈希表 + 双向链表 |
| 遍历性能 | O(n + capacity) | O(n) |
| 写入开销 | 较低 | 较高（链表维护） |
| 内存使用 | 较低 | 较高（额外指针） |
| 继承关系 | AbstractMap | HashMap 的子类 |
| 适用场景 | 高效查找 | 需要保持顺序 |

### 9.2 LinkedHashMap vs TreeMap

| 特性 | LinkedHashMap | TreeMap |
|-----|--------------|---------|
| 迭代顺序 | 插入顺序/访问顺序 | 排序顺序 |
| 排序依据 | 插入时间/访问时间 | 自然排序/Comparator |
| 查找性能 | O(1) | O(log n) |
| 遍历性能 | O(n) | O(n) |
| 内存使用 | 较低 | 较高（红黑树） |
| 适用场景 | 保持插入或访问顺序 | 需要范围查询/排序 |

### 9.3 LinkedHashMap vs LinkedHashSet

| 特性 | LinkedHashMap | LinkedHashSet |
|-----|--------------|---------------|
| 存储内容 | 键值对 | 元素 |
| 底层实现 | LinkedHashMap | LinkedHashMap |
| 迭代顺序 | 插入顺序/访问顺序 | 插入顺序/访问顺序 |
| API | Map 接口 | Set 接口 |
| 适用场景 | 键值对存储且需顺序 | 元素集合且需顺序 |

### 9.4 LinkedHashMap vs ConcurrentHashMap

| 特性 | LinkedHashMap | ConcurrentHashMap |
|-----|--------------|-------------------|
| 线程安全 | 否 | 是 |
| 迭代顺序 | 有序 | 无序 |
| 锁粒度 | 无 | 桶级锁 |
| 适用场景 | 单线程有序存储 | 多线程高效存储 |

## 十、源码学习总结

### 10.1 设计模式应用

LinkedHashMap 源码中体现了多种设计模式的应用：

**模板方法模式**：HashMap 提供了算法骨架，LinkedHashMap 通过覆盖回调方法修改特定行为。afterNodeAccess、afterNodeInsertion、afterNodeRemoval 等方法就是模板方法的具体实现。

**委托模式**：LinkedHashMap 将哈希表的操作委托给 HashMap 处理，自己只负责链表的维护。

**策略模式**：通过 accessOrder 字段支持两种不同的迭代策略（插入顺序和访问顺序）。

### 10.2 核心算法亮点

LinkedHashMap 的实现中有几个值得学习的亮点：

**回调机制**：通过在关键点插入回调方法，实现了行为的扩展，而不需要修改父类代码。这种设计使得 LinkedHashMap 可以优雅地扩展 HashMap。

**双向链表维护**：linkNodeLast()、afterNodeRemoval()、afterNodeAccess() 等方法实现了高效的链表维护操作。所有的链表操作都是 O(1) 时间复杂度。

**LRU 实现**：通过覆盖 removeEldestEntry() 方法，结合访问顺序模式，简洁地实现了 LRU 缓存淘汰策略。

### 10.3 工程实践启示

从 LinkedHashMap 源码可以学到很多工程实践知识：

**继承的价值**：通过继承扩展功能，而不是重写所有方法。LinkedHashMap 只覆盖了少量关键方法，就实现了完全不同的行为。

**可扩展性设计**：提供回调钩子（removeEldestEntry）允许子类自定义行为，提高了框架的灵活性。

**性能权衡**：在保持 HashMap 高效查找的同时，通过链表提供了有序性。这种设计体现了空间换时间的思想。

**代码复用**：作为 HashMap 的子类，LinkedHashMap 复用了父类的所有核心逻辑，避免了代码重复。

理解 LinkedHashMap 的源码不仅有助于更好地使用这个类，更能学习到如何通过继承和回调机制扩展现有类的功能，以及如何在性能和功能之间取得平衡。