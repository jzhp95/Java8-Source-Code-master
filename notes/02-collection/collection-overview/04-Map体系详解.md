# Java集合框架体系源码深度解析——Map体系详解

## 四、Map体系详解

### 4.1 Map体系概述

#### 4.1.1 Map接口特性

Map是存储键值对映射的对象,键不能重复,值可以重复。Map接口不继承自Collection,因为它表示的是映射关系而不是集合。

**Map的核心特性:**

1. **键值对**: 存储键到值的映射关系。

2. **键唯一**: 键不能重复,但值可以重复。

3. **快速查找**: 基于哈希的Map查找效率高,时间复杂度O(1)。

4. **允许null**: 大多数Map实现允许一个null键和多个null值。

```
Map接口体系
├── AbstractMap (抽象类)
│   ├── HashMap (哈希表实现)
│   ├── LinkedHashMap (保持插入顺序的HashMap)
│   ├── TreeMap (红黑树实现,有序)
│   ├── WeakHashMap (弱引用Map)
│   └── IdentityHashMap (引用相等而非对象相等)
├── Hashtable (线程安全的HashMap,已过时)
└── EnumMap (枚举类型专用Map)
```

#### 4.1.2 Map接口核心方法

```java
public interface Map<K,V> {
    // 基本操作
    int size();
    boolean isEmpty();
    boolean containsKey(Object key);
    boolean containsValue(Object value);
    V get(Object key);
    
    // 修改操作
    V put(K key, V value);
    V remove(Object key);
    void putAll(Map<? extends K,? extends V> m);
    void clear();
    
    // 视图操作
    Set<K> keySet();
    Collection<V> values();
    Set<Map.Entry<K,V>> entrySet();
    
    // 接口Entry
    interface Entry<K,V> {
        K getKey();
        V getValue();
        V setValue(V value);
        boolean equals(Object o);
        int hashCode();
    }
    
    // 默认方法
    default V getOrDefault(Object key, V defaultValue);
    default V putIfAbsent(K key, V value);
    default boolean remove(Object key, Object value);
    default boolean replace(K key, V oldValue, V newValue);
    default V replace(K key, V value);
    default V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction);
    default V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction);
    default V compute(K key, BiFunction<? super K,? super V,? extends V> remappingFunction);
    default V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction);
}
```

### 4.2 HashMap深度解析

#### 4.2.1 HashMap概述

HashMap是基于哈希表实现的Map,它提供了快速的键值对存储和查找。HashMap是Java集合框架中最常用的Map实现。

**HashMap的核心特点:**

1. **快速查找**: 基于哈希表,查找时间复杂度O(1)。

2. **允许null**: 允许一个null键和多个null值。

3. **无序性**: 不保证键值对的顺序,可能随时间变化。

4. **非线程安全**: 不是线程安全的,多线程环境下需要同步。

5. **负载因子**: 默认负载因子0.75,平衡时间和空间。

```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {
    
    // 默认初始容量
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
    
    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;
    
    // 默认负载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    // 转换为树的阈值
    static final int TREEIFY_THRESHOLD = 8;
    
    // 转换为链表的阈值
    static final int UNTREEIFY_THRESHOLD = 6;
    
    // 哈希表数组
    transient Node<K,V>[] table;
    
    // 键值对数量
    transient int size;
    
    // 扩容阈值
    int threshold;
    
    // 负载因子
    final float loadFactor;
    
    // 修改次数
    transient int modCount;
}
```

#### 4.2.2 核心构造方法

```java
// 构造一个空的HashMap,默认初始容量16,负载因子0.75
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
}

// 构造一个空的HashMap,指定初始容量
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}

// 构造一个空的HashMap,指定初始容量和负载因子
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}

// 构造一个包含指定Map元素的HashMap
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}
```

**tableSizeFor方法:**

```java
// 返回给定目标容量的最小2的幂
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

这个方法返回大于等于cap的最小2的幂,用于确定哈希表的初始容量。

#### 4.2.3 Node节点结构

```java
// 哈希表节点
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
    
    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
    
    public K getKey() { return key; }
    public V getValue() { return value; }
    public V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Map.Entry) {
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            if (Objects.equals(key, e.getKey()) &&
                Objects.equals(value, e.getValue()))
                return true;
        }
        return false;
    }
    
    public int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }
    
    public String toString() {
        return key + "=" + value;
    }
}
```

#### 4.2.4 哈希计算

```java
// 计算key的哈希值
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**哈希计算的优化:**

1. **扰动函数**: 使用异或操作,减少哈希冲突。

2. **高位参与**: 将高16位与低16位异或,让高位也参与哈希计算。

3. **避免冲突**: 通过扰动函数,使哈希值更加均匀分布。

**索引计算:**

```java
// 计算key在table中的索引
n = (tab = table).length;
index = (n - 1) & hash;
```

使用`(n - 1) & hash`而不是`hash % n`,因为n是2的幂,位运算更快。

#### 4.2.5 put操作详解

```java
// 添加键值对
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

// 核心put方法
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

**put操作流程:**

1. **计算哈希**: 使用hash方法计算key的哈希值。

2. **定位桶**: 使用`(n - 1) & hash`计算在table中的索引。

3. **处理冲突**: 
   - 如果桶为空,直接插入新节点
   - 如果桶不为空,遍历链表或红黑树查找key
   - 如果找到key,更新value
   - 如果没找到key,插入新节点

4. **树化判断**: 如果链表长度>=8,转换为红黑树

5. **扩容判断**: 如果size>threshold,扩容

#### 4.2.6 get操作详解

```java
// 获取key对应的value
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

// 核心get方法
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        if (first.hash == hash && 
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

**get操作流程:**

1. **计算哈希**: 使用hash方法计算key的哈希值。

2. **定位桶**: 使用`(n - 1) & hash`计算在table中的索引。

3. **查找节点**: 
   - 如果桶为空,返回null
   - 如果桶不为空,遍历链表或红黑树查找key
   - 如果找到key,返回对应的节点
   - 如果没找到key,返回null

#### 4.2.7 remove操作详解

```java
// 移除key对应的键值对
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false)) == null ?
        null : e.value;
}

// 核心remove方法
final Node<K,V> removeNode(int hash, Object key, Object value,
                              boolean matchValue) {
    Node<K,V>[] tab; Node<K,V> p; int n, index;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (p = tab[index = (n - 1) & hash]) != null) {
        Node<K,V> node = null, e; K k; V v;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k)))) {
            node = p;
        } else if ((e = p.next) != null) {
            if (p instanceof TreeNode)
                node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
            else {
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
        }
        if (node != null && (!matchValue || (v = node.value) == value ||
             (value != null && value.equals(v)))) {
            if (node instanceof TreeNode)
                ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
            else if (node == p)
                tab[index] = node.next;
            else
                p.next = node.next;
            ++modCount;
            --size;
            afterNodeRemoval(node);
            return node;
        }
    }
    return null;
}
```

**remove操作流程:**

1. **计算哈希**: 使用hash方法计算key的哈希值。

2. **定位桶**: 使用`(n - 1) & hash`计算在table中的索引。

3. **查找节点**: 遍历链表或红黑树查找key。

4. **删除节点**: 
   - 如果是链表,调整指针
   - 如果是红黑树,调用红黑树删除方法

5. **更新状态**: 更新size和modCount

#### 4.2.8 resize操作详解

```java
// 扩容方法
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    
    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1;
    }
    else if (oldThr > 0)
        newCap = oldThr;
    else {
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else {
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        } else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

**resize操作流程:**

1. **计算新容量**: 新容量 = 旧容量 * 2(最大不超过MAXIMUM_CAPACITY)

2. **计算新阈值**: 新阈值 = 新容量 * 负载因子

3. **创建新数组**: 创建新的Node数组

4. **重新哈希**: 将旧数组的元素重新哈希到新数组
   - 如果是单个节点,直接移动
   - 如果是红黑树,调用split方法
   - 如果是链表,使用高低位分离

5. **更新table**: 将新数组赋值给table

**高低位分离:**

```java
// 判断节点在新数组中的位置
if ((e.hash & oldCap) == 0) {
    // 低位: 索引不变
    loTail.next = e;
    loTail = e;
} else {
    // 高位: 索引 = 原索引 + oldCap
    hiTail.next = e;
    hiTail = e;
}
```

#### 4.2.9 红黑树转换

当链表长度达到TREEIFY_THRESHOLD(8)时,会转换为红黑树:

```java
// 链表转红黑树
final void treeifyBin(Node<K,V>[] tab, int hash) {
    int n, index; Node<K,V> e;
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
        resize();
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        TreeNode<K,V> hd = null, tl = null;
        do {
            TreeNode<K,V> p = replacementTreeNode(e, null);
            if (tl == null)
                hd = p;
            else {
                p.prev = tl;
                tl.next = p;
            }
            tl = p;
        } while ((e = e.next) != null);
        if ((tab[index] = hd) != null)
            hd.treeify(tab);
    }
}
```

**红黑树节点:**

```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;
    boolean red;
    
    TreeNode(int hash, K key, V val, Node<K,V> next) {
        super(hash, key, val, next);
    }
    
    final TreeNode<K,V> root() {
        for (TreeNode<K,V> r = this, p;;) {
            if ((p = r.parent) == null)
                return r;
            r = p;
        }
    }
    
    static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
        // 将root移动到数组位置
    }
}
```

### 4.3 LinkedHashMap深度解析

#### 4.3.1 LinkedHashMap概述

LinkedHashMap是HashMap的子类,它使用双向链表维护键值对的插入顺序或访问顺序。

**LinkedHashMap的核心特点:**

1. **保持顺序**: 按照插入顺序或访问顺序排列。

2. **快速查找**: 基于HashMap,查找时间复杂度O(1)。

3. **额外开销**: 需要维护双向链表,比HashMap占用更多内存。

4. **非线程安全**: 不是线程安全的。

```java
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{
    // 头节点(最老的)
    transient LinkedHashMap.Entry<K,V> head;
    
    // 尾节点(最新的)
    transient LinkedHashMap.Entry<K,V> tail;
    
    // 访问顺序: true表示按访问顺序,false表示按插入顺序
    final boolean accessOrder;
}
```

#### 4.3.2 Entry节点结构

```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

LinkedHashMap的Entry继承自HashMap.Node,增加了before和after指针,用于维护双向链表。

#### 4.3.3 插入顺序vs访问顺序

```java
// 构造方法,按插入顺序
public LinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
}

// 构造方法,按访问顺序
public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}
```

**插入顺序:** 键值对按照插入的顺序排列。

**访问顺序:** 键值对按照最近访问的顺序排列,最近访问的在尾部。

#### 4.3.4 afterNodeAccess方法

```java
// 节点访问后的回调
void afterNodeAccess(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e,
            b = p.before, a = p.after;
        p.after = a;
        p.before = last;
        if (b == null)
            head = p;
        else
            b.after = p;
        if (a == null)
            tail = p;
        else
            a.before = p;
        ++modCount;
    }
}
```

当accessOrder为true时,每次访问节点都会调用这个方法,将节点移动到链表尾部。

### 4.4 TreeMap深度解析

#### 4.4.1 TreeMap概述

TreeMap是基于红黑树实现的有序Map,它保证键的自然顺序或自定义顺序。

**TreeMap的核心特点:**

1. **有序性**: 按照键的自然顺序或自定义顺序排列。

2. **查找效率**: 基于红黑树,查找时间复杂度O(log n)。

3. **不允许null**: 不允许null键(除非使用自定义Comparator)。

4. **非线程安全**: 不是线程安全的。

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
{
    // 比较器
    private final Comparator<? super K> comparator;
    
    // 红黑树根节点
    private transient Entry<K,V> root;
    
    // 键值对数量
    private transient int size = 0;
    
    // 修改次数
    private transient int modCount = 0;
}
```

#### 4.4.2 Entry节点结构

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
    }
    
    public K getKey() { return key; }
    public V getValue() { return value; }
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }
}
```

#### 4.4.3 put操作详解

```java
// 添加键值对
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

#### 4.4.4 红黑树平衡

```java
// 插入后平衡红黑树
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;
    
    for (Entry<K,V> xp, xpp, xppl, xppr;;) {
        if ((xp = x.parent) == null) {
            x.color = BLACK;
            root = x;
            return;
        }
        if (xp.color == BLACK)
            return;
        
        xpp = xp.parent;
        if (xp == xpp.left) {
            xppr = xpp.right;
            if (xppr != null && xppr.color == RED) {
                xp.color = BLACK;
                xpp.color = RED;
                rotateLeft(xpp);
                xpp = xp.parent;
                xppr = xpp.right;
            }
            if (x == xppr.left) {
                xp.color = BLACK;
                xpp.color = RED;
                rotateRight(xpp);
            } else if (x == xppr.right) {
                x.color = BLACK;
                xp.color = RED;
                rotateLeft(xp);
                xp.color = BLACK;
                rotateRight(xpp);
            }
        } else {
            xppl = xpp.left;
            if (xppl != null && xppl.color == RED) {
                xp.color = BLACK;
                xpp.color = RED;
                rotateRight(xpp);
                xpp = xp.parent;
                xppl = xpp.left;
            }
            if (x == xppl.right) {
                xp.color = BLACK;
                xpp.color = RED;
                rotateLeft(xpp);
            } else if (x == xppl.left) {
                xp.color = BLACK;
                xp.color = RED;
                rotateRight(xp);
                xp.color = BLACK;
                rotateLeft(xpp);
            }
        }
    }
}
```

### 4.5 Map体系对比

#### 4.5.1 性能对比

| 操作 | HashMap | LinkedHashMap | TreeMap | EnumMap |
|------|---------|---------------|---------|---------|
| put | O(1) | O(1) | O(log n) | O(1) |
| get | O(1) | O(1) | O(log n) | O(1) |
| remove | O(1) | O(1) | O(log n) | O(1) |
| containsKey | O(1) | O(1) | O(log n) | O(1) |

#### 4.5.2 特性对比

| 特性 | HashMap | LinkedHashMap | TreeMap | EnumMap |
|------|---------|---------------|---------|---------|
| 有序性 | 无 | 插入顺序/访问顺序 | 自然顺序/自定义顺序 | 枚举顺序 |
| 允许null键 | 是 | 是 | 否(除非自定义Comparator) | 否 |
| 允许null值 | 是 | 是 | 是 | 否 |
| 线程安全 | 否 | 否 | 否 | 否 |
| 内存占用 | 中等 | 较大 | 较大 | 最小 |

### 4.6 Map使用最佳实践

#### 4.6.1 选择合适的Map实现

```java
// 场景1: 需要快速查找
Map<String, Integer> map = new HashMap<>();

// 场景2: 需要保持插入顺序
Map<String, Integer> orderedMap = new LinkedHashMap<>();

// 场景3: 需要按键排序
Map<String, Integer> sortedMap = new TreeMap<>();

// 场景4: 枚举类型
enum Color { RED, GREEN, BLUE }
Map<Color, String> colorMap = new EnumMap<>(Color.class);
```

#### 4.6.2 避免常见错误

**错误1: 使用==比较key**

```java
// 错误示例
Map<String, Integer> map = new HashMap<>();
map.put(new String("Hello"), 1);

if (map.get("Hello" == "Hello")) { // 可能返回null
    // ...
}

// 正确示例
if (map.get("Hello") != null) { // 正确
    // ...
}
```

**错误2: 修改可变key**

```java
// 错误示例
class Person {
    String name;
    int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(age);
    }
}

Map<Person, String> map = new HashMap<>();
Person p = new Person("Alice", 25);
map.put(p, "Value");
p.age = 26; // 修改了age,hashCode也变了
map.get(p); // 返回null
```

#### 4.6.3 性能优化建议

**1. 预估Map大小**

```java
// 错误示例
Map<String, Integer> map = new HashMap<>();

// 正确示例
Map<String, Integer> map = new HashMap<>(1000);
```

**2. 使用合适的初始容量和负载因子**

```java
// 场景1: 需要快速查找,不关心空间
Map<String, Integer> map = new HashMap<>(1000, 0.5f);

// 场景2: 需要节省空间
Map<String, Integer> map = new HashMap<>(1000, 0.9f);
```

### 4.7 小结

Map体系是Java集合框架中最复杂的部分,理解其实现原理对于编写高效、正确的代码至关重要。

**核心要点总结:**

第一,**HashMap**: 基于哈希表,快速查找,无序,允许null。

第二,**LinkedHashMap**: 基于HashMap,保持插入顺序或访问顺序。

第三,**TreeMap**: 基于红黑树,有序,查找O(log n),不允许null。

第四,**EnumMap**: 专门为枚举类型设计,使用数组,非常高效。

第五,**性能对比**: HashMap/LinkedHashMap查找O(1),TreeMap查找O(log n)。

第六,**最佳实践**: 选择合适的Map类型、避免常见错误、优化性能。
