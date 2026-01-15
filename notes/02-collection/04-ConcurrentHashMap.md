# ConcurrentHashMap 源码深度解析

## 一、类概述与设计理念

### 1.1 基本信息

ConcurrentHashMap 是 Java 并发包 java.util.concurrent 中的核心类，位于 java.util.concurrent 包中。作为线程安全的高性能哈希表实现，ConcurrentHashMap 提供了与 HashMap 相似的功能，但支持高并发的读写操作。它是 Java 并发编程中使用最广泛的并发集合类之一。

ConcurrentHashMap 的设计目标是在高并发环境下提供优秀的性能表现。其主要设计目标包括：支持高并发的读取操作而不阻塞、支持高并发的更新操作并尽量减少锁竞争、保持与 HashMap 和 Hashtable 类似的 API 接口、允许在并发更新时进行安全的遍历操作。

### 1.2 设计特点

ConcurrentHashMap 的设计体现了以下几个核心特点：

**桶级锁机制（Java 8）**：Java 8 对 ConcurrentHashMap 进行了重大重构，移除了分段锁结构，转而使用桶级锁和 CAS 操作。对于常见的插入第一个节点的操作，使用 CAS 而非锁；对于链表或红黑树的更新操作，则使用 synchronized 锁住桶的第一个节点。

**弱一致性迭代器**：与 HashMap 的 fail-fast 迭代器不同，ConcurrentHashMap 的迭代器是弱一致性的。迭代器可能反映在迭代器创建时或创建之后的映射修改，不会抛出 ConcurrentModificationException。

**不支持 null 键值**：与 HashMap 不同，ConcurrentHashMap 不允许使用 null 作为键或值。这一设计决策是因为在并发环境中，null 值可能与"找不到值"的情况混淆。

**高效的空间利用**：在保持线程安全的同时，ConcurrentHashMap 尽量减少额外的空间开销。

### 1.3 与其他 Map 的比较

ConcurrentHashMap 与 Hashtable、Collections.synchronizedMap(new HashMap()) 等线程安全 Map 的主要区别在于并发粒度：

| 特性 | Hashtable | Collections.synchronizedMap | ConcurrentHashMap |
|-----|-----------|---------------------------|-------------------|
| 锁粒度 | 整个表 | 整个表 | 单个桶 |
| 读操作阻塞 | 是 | 是 | 否 |
| 写操作阻塞 | 是 | 是 | 仅冲突时 |
| 迭代器一致性 | fail-fast | fail-fast | 弱一致 |
| null 键值 | 不允许 | 允许 | 不允许 |
| 性能 | 低 | 低 | 高 |

### 1.4 性能特征总结

从时间复杂度的角度分析 ConcurrentHashMap 的性能特征：

| 操作类型 | 平均时间复杂度 | 最坏情况时间复杂度 | 说明 |
|---------|--------------|------------------|------|
| get(Object key) | O(1) | O(1) | 无锁操作 |
| put(K key, V value) | O(1) | O(log n) | CAS 或桶锁 |
| remove(Object key) | O(1) | O(log n) | CAS 或桶锁 |
| containsKey(Object key) | O(1) | O(1) | 同 get |
| size() | O(1) 或 O(n) | O(1) 或 O(n) | 近似值 |
| containsValue(Object value) | O(n) | O(n) | 需要遍历 |
| 迭代 | O(n) | O(n) | 弱一致性 |

## 二、核心设计概述

### 2.1 整体架构

ConcurrentHashMap 内部采用哈希表加链表/红黑树的结构，与 HashMap 类似。主要区别在于并发控制机制的实现。

**表（Table）**：核心是一个 Node 数组（table），延迟初始化，第一次插入时分配。数组大小始终是 2 的幂次方。

**节点类型**：根据哈希值和用途的不同，节点分为多种类型：
- **Node**：普通键值对节点
- **TreeNode**：红黑树节点
- **TreeBin**：红黑树的根节点
- **ForwardingNode**：扩容转发节点
- **ReservationNode**：保留节点，用于 computeIfAbsent 等操作

**控制位**：节点的哈希字段使用特殊值进行控制：
- MOVED (-1)：ForwardingNode 的哈希值
- TREEBIN (-2)：TreeBin 的哈希值
- RESERVED (-3)：ReservationNode 的哈希值
- 正常节点使用正数哈希值

### 2.2 并发控制机制

ConcurrentHashMap 使用多种技术实现高效的并发控制：

**CAS 操作**：使用 Unsafe 类的 compareAndSwapObject 方法进行无锁更新。CAS 操作用于向空桶插入第一个节点、更新 CounterCell 的计数、初始化表等。

**synchronized 锁**：对于需要更新链表或红黑树的场景，使用 synchronized 锁住桶的第一个节点。锁粒度从整个表细化到单个桶，大大减少了锁竞争。

**Volatile 读写**：使用 volatile 修饰关键字段（table、nextTable、baseCount、sizeCtl、transferIndex、cellsBusy、counterCells），保证内存可见性。

**帮助扩容**：当线程发现正在扩容时，可以协助完成扩容操作，提高并发度。

### 2.3 计数机制

ConcurrentHashMap 使用改进的 LongAdder 机制来维护元素数量：

**baseCount**：基础计数器，使用 CAS 更新。在没有竞争的情况下更新此值。

**CounterCell[]**：计数器单元格数组。当 CAS 更新 baseCount 失败时，会尝试在 CounterCell 上进行更新。通过分散更新，减少竞争。

**sumCount()**：计算总数量，返回 baseCount 加上所有 CounterCell 的值。这是一个近似值，在高并发情况下可能略有波动。

## 三、核心常量解析

### 3.1 容量相关常量

```java
private static final int MAXIMUM_CAPACITY = 1 << 30;
private static final int DEFAULT_CAPACITY = 16;
private static final float LOAD_FACTOR = 0.75f;
```

**MAXIMUM_CAPACITY**：最大表容量，设置为 2 的 30 次方。选择 30 而不是 31 是为了在哈希计算中保留控制位的空间。

**DEFAULT_CAPACITY**：默认初始容量为 16。

**LOAD_FACTOR**：负载因子设置为 0.75。与 HashMap 相同，在空间利用率和性能之间取得平衡。

### 3.2 树化阈值

```java
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
static final int MIN_TREEIFY_CAPACITY = 64;
```

**TREEIFY_THRESHOLD**：链表转红黑树的阈值。当桶中节点数达到 8 个时，考虑转换为红黑树。

**UNTREEIFY_THRESHOLD**：红黑树转链表的阈值。设置为 6 而不是 8 是为了避免在阈值边界频繁转换。

**MIN_TREEIFY_CAPACITY**：链表树化的最小表容量。设置为 64，确保在足够的空间下才进行树化。

### 3.3 扩容相关常量

```java
private static final int MIN_TRANSFER_STRIDE = 16;
private static final int RESIZE_STAMP_BITS = 16;
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;
```

**MIN_TRANSFER_STRIDE**：每个线程帮助扩容时至少处理的桶数。设置为 16，减少线程间竞争。

**RESIZE_STAMP_BITS**：sizeCtl 中用于存储 resize stamp 的位数。

**MAX_RESIZERS**：最大并发扩容线程数。

### 3.4 特殊哈希值

```java
static final int MOVED = -1;      // hash for forwarding nodes
static final int TREEBIN = -2;     // hash for roots of trees
static final int RESERVED = -3;    // hash for transient reservations
static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash
```

这些负数哈希值用于区分不同类型的特殊节点。正常节点的哈希值经过 spread() 方法处理后，最高位为 0（正数）。

## 四、核心字段解析

### 4.1 表相关字段

```java
transient volatile Node<K,V>[] table;
private transient volatile Node<K,V>[] nextTable;
```

**table**：存储键值对的哈希表数组。延迟初始化，第一次插入时创建。数组大小始终是 2 的幂次方。使用 volatile 保证可见性。

**nextTable**：扩容时使用的新表。扩容完成后置为 null。

### 4.2 计数相关字段

```java
private transient volatile long baseCount;
private transient volatile int sizeCtl;
private transient volatile int transferIndex;
private transient volatile int cellsBusy;
private transient volatile CounterCell[] counterCells;
```

**baseCount**：基础计数器，记录元素数量的基值。在无竞争时使用 CAS 更新。

**sizeCtl**：多用途控制字段。负值表示表正在初始化或扩容，正值表示扩容阈值。

**transferIndex**：下一个要处理的桶索引，用于协调多个线程的扩容工作。

**cellsBusy**：用于保护 CounterCell 数组的忙标志。使用 CAS 设置。

**counterCells**：CounterCell 数组，用于分散计数更新，减少竞争。

### 4.3 视图字段

```java
private transient KeySetView<K,V> keySet;
private transient ValuesView<K,V> values;
private transient EntrySetView<K,V> entrySet;
```

这三个字段缓存了 map 的键集、值集合和键值对集合的视图对象。使用懒加载模式，首次访问时创建。

## 五、核心内部类

### 5.1 Node 节点类

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    public final K getKey() { return key; }
    public final V getValue() { return val; }
    public final int hashCode() { return key.hashCode() ^ val.hashCode(); }
    public final String toString() { return key + "=" + val; }

    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == (u = val) || v.equals(u)));
    }

    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        if (k != null) {
            do {
                K ek;
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}
```

Node 是 ConcurrentHashMap 的基础节点类。与 HashMap 的 Node 类似，但有以下重要区别：

**volatile 关键字**：val 和 next 字段使用 volatile 修饰，保证可见性。这是实现无锁读取的关键。

**find() 方法**：提供了在链表或树中查找节点的能力。子类可以覆盖此方法提供更高效的查找。

### 5.2 ForwardingNode 转发节点

```java
static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;
    ForwardingNode(Node<K,V>[] nextTable) {
        super(MOVED, null, null, null);
        this.nextTable = nextTable;
    }

    Node<K,V> find(int h, Object k) {
        outer: for (Node<K,V>[] tab = nextTable;;) {
            Node<K,V> e; int n;
            if (k == null || tab == null || (n = tab.length) == 0 ||
                (e = tabAt(tab, (n - 1) & h)) == null)
                return null;
            for (;;) {
                int eh; K ek;
                if ((eh = e.hash) == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
                if (eh < 0) {
                    if (e instanceof ForwardingNode) {
                        continue outer;
                    }
                    return e.find(h, k);
                }
                if ((e = e.next) == null)
                    return null;
            }
        }
    }
}
```

ForwardingNode 在扩容时作为桶的头节点，表示该桶的数据已经迁移到新表。其 hash 字段为 MOVED (-1)。

### 5.3 TreeBin 红黑树根节点

```java
static final class TreeBin<K,V> extends Node<K,V> {
    TreeNode<K,V> root;
    volatile TreeNode<K,V> first;
    volatile int waiters;
    volatile int lockState;
}
```

TreeBin 是红黑树的根节点包装类，用于管理红黑树结构。hash 字段为 TREEBIN (-2)。

主要特点：使用 lockState 字段实现轻量级读写锁。写操作需要获取锁，读操作不需要。

### 5.4 CounterCell 计数器单元格

```java
static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```

CounterCell 用于分散计数更新，减少竞争。每个单元格存储一个计数值，所有单元格的值之和加上 baseCount 就是元素总数。

## 六、核心方法详解

### 6.1 哈希计算 spread()

```java
static final int spread(int h) {
    return (h ^ (h >>> 16)) & HASH_BITS;
}
```

spread() 方法对哈希码进行扰动处理，确保高位信息能够传播到低位。与 HashMap 类似，但额外与 HASH_BITS (0x7fffffff) 进行与操作，确保结果为正数。

### 6.2 表访问方法

```java
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}

static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}

static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
}
```

这些方法使用 Unsafe API 实现对数组元素的原子/ volatile 访问。

### 6.3 获取操作 get()

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

get() 方法实现了无锁读取。执行流程：计算哈希值，定位桶位置，检查第一个节点，如果需要则遍历链表或调用 find 方法。

### 6.4 插入操作 put()

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

putVal() 方法是核心的插入方法，处理多种情况：表未初始化、空桶、正在扩容、普通桶等。

### 6.5 删除操作 remove()

```java
public V remove(Object key) {
    return replaceNode(key, null, null);
}

final V replaceNode(Object key, V value, Object cv) {
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            (f = tabAt(tab, i = (n - 1) & hash)) == null)
            break;
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            boolean validated = false;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        validated = true;
                        for (Node<K,V> e = f, pred = null;;) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                V ev = e.val;
                                if (cv == null || cv == ev ||
                                    (ev != null && cv.equals(ev))) {
                                    oldVal = ev;
                                    if (value != null)
                                        e.val = value;
                                    else if (pred != null)
                                        pred.next = e.next;
                                    else
                                        setTabAt(tab, i, e.next);
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                    else if (f instanceof TreeBin) {
                        validated = true;
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> r, p;
                        if ((r = t.root) != null &&
                            (p = r.findTreeNode(hash, key, null)) != null) {
                            V pv = p.val;
                            if (cv == null || cv == pv ||
                                (pv != null && cv.equals(pv))) {
                                oldVal = pv;
                                if (value != null)
                                    p.val = value;
                                else if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
            }
            if (validated) {
                if (oldVal != null) {
                    if (value == null)
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}
```

replaceNode() 方法实现了删除和替换功能。使用 synchronized 锁住桶进行处理。

### 6.6 初始化 initTable()

```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield();
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc == 0) ? DEFAULT_CAPACITY : sc > 0 ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = (int)(n * LOAD_FACTOR);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

initTable() 方法使用 CAS 确保只有一个线程进行表初始化。

### 6.7 扩容协助 helpTransfer()

```java
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                transfer(tab, nextTab, rs);
                break;
            }
        }
    }
    return table;
}
```

helpTransfer() 方法允许线程帮助完成正在进行的扩容操作。

## 七、常见面试问题与解答

### 7.1 ConcurrentHashMap 的并发原理

ConcurrentHashMap 通过以下机制实现高并发：

**锁分离**：不使用整个表的锁，而是将锁粒度细化到单个桶。不同线程访问不同桶时不会产生锁竞争。

**CAS 操作**：对于简单的插入操作（向空桶添加第一个节点），使用 CAS 而非锁，完全无锁。

**synchronized 桶锁**：对于需要修改链表或红黑树的复杂操作，使用 synchronized 锁住桶的第一个节点。

**帮助扩容**：扩容时，其他线程可以协助完成迁移，而不是等待。

### 7.2 get() 方法为什么不需要锁

get() 方法不需要锁的原因：

**volatile 读取**：Node 的 val 和 next 字段都使用 volatile 修饰，保证读取最新写入的值。

**内存屏障**：volatile 读取会插入内存屏障，确保看到其他线程写入的值。

**无修改操作**：get() 只读取不修改，不需要互斥访问。

### 7.3 ConcurrentHashMap 与 Hashtable 的区别

| 特性 | Hashtable | ConcurrentHashMap |
|-----|-----------|-------------------|
| 锁粒度 | 整个表 | 单个桶 |
| 读锁 | 需要 | 不需要 |
| 写锁 | 需要 | 桶级别 |
| null 键值 | 不允许 | 不允许 |
| 迭代器 | fail-fast | 弱一致 |
| 性能 | 低 | 高 |

### 7.4 为什么 ConcurrentHashMap 不支持 null

主要原因：

**语义清晰**：在并发环境中，null 值可能表示"键不存在"或"值为 null"。这种歧义会导致问题。

**API 一致性**：get() 返回 null 可能表示键不存在或值为 null，难以区分。

### 7.5 迭代器的弱一致性

ConcurrentHashMap 的迭代器是弱一致的：

**不抛出异常**：迭代过程中修改 map，不会抛出 ConcurrentModificationException。

**反映修改**：迭代器会反映在迭代器创建时或创建之后的修改。

### 7.6 size() 方法的准确性

size() 方法返回的是近似值：

**baseCount + CounterCell**：累加 baseCount 和所有 CounterCell 的值。

**可能不精确**：在并发更新过程中，计数可能在累加过程中发生变化。

**适用场景**：适合监控和估计，不适合需要精确计数的场景。

## 八、实践应用场景

### 8.1 缓存实现

```java
ConcurrentHashMap<String, Data> cache = new ConcurrentHashMap<>();

public Data getData(String key) {
    return cache.computeIfAbsent(key, k -> loadData(k));
}
```

使用 computeIfAbsent 实现线程安全的缓存。

### 8.2 计数器

```java
ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

public void increment(String key) {
    counters.computeIfAbsent(key, k -> new LongAdder()).increment();
}
```

使用 ConcurrentHashMap 实现分布式计数器。

## 九、最佳实践

### 9.1 选择合适的初始容量

根据预估的元素数量设置初始容量：

```java
int expectedSize = 10000;
int initialCapacity = (int) (expectedSize / 0.75) + 1;
ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>(initialCapacity);
```

### 9.2 合理使用 computeIfAbsent

避免在 computeIfAbsent 的 mappingFunction 中有复杂操作或副作用。

### 9.3 最佳实践总结

根据预估大小设置初始容量。了解 size() 的近似性质。合理使用 computeIfAbsent 等高级方法。注意批量操作不是原子的。迭代时修改不会抛异常，但需谨慎。

## 十、源码学习总结

### 10.1 设计模式应用

ConcurrentHashMap 源码中体现了多种设计模式的应用：

**锁分离模式**：将锁粒度从整个表细化到单个桶，减少竞争。

**CAS 模式**：使用 CAS 实现无锁更新。

**弱一致迭代器**：使用弱一致性保证并发性能。

### 10.2 核心算法亮点

ConcurrentHashMap 的实现亮点：

**CAS 无锁更新**：向空桶添加节点时使用 CAS。

**桶级 synchronized**：复杂操作使用 synchronized 桶锁。

**LongAdder 计数**：使用 CounterCell 数组分散计数更新。

**渐进式扩容**：多线程协作完成扩容。

理解 ConcurrentHashMap 的源码有助于学习高并发编程的核心原理和设计思想。
