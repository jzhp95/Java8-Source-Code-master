# IdentityHashMap 源码解读

## 一、类概述

IdentityHashMap是Java集合框架中的一种特殊Map实现,它使用引用相等性(==)而不是对象相等性(equals)来比较键和值。在IdentityHashMap中,两个键k1和k2被认为是相等的,当且仅当k1 == k2。

### 1.1 核心特性

- **引用相等性**: 使用==比较键和值,而不是equals()
- **线性探测哈希表**: 使用开放寻址法,而不是拉链法
- **紧凑存储**: 键和值交替存储在同一个数组中
- **支持null键和null值**: null键内部用NULL_KEY对象表示
- **线程不安全**: 非同步实现,需要外部同步
- **违反Map契约**: 故意违反Map的一般契约

### 1.2 适用场景

- 拓扑保持的对象图转换(如序列化、深拷贝)
- 维护代理对象(如调试工具)
- 需要区分不同对象实例的场景
- 对象图遍历和去重

### 1.3 与HashMap的区别

| 特性 | HashMap | IdentityHashMap |
|------|---------|----------------|
| 键比较 | equals() | == |
| 冲突解决 | 拉链法 | 线性探测 |
| 存储结构 | Entry数组 | 键值交替数组 |
| null处理 | 直接支持 | NULL_KEY对象 |
| 内存效率 | 较低(Entry对象) | 较高(紧凑数组) |

## 二、设计原理

### 2.1 引用相等性

IdentityHashMap使用System.identityHashCode()获取对象的标识哈希码,并使用==比较对象引用:

```java
// HashMap的比较方式
if (key1.equals(key2)) { ... }

// IdentityHashMap的比较方式
if (key1 == key2) { ... }
```

### 2.2 线性探测哈希表

IdentityHashMap使用开放寻址法中的线性探测来解决哈希冲突:

```
索引计算:
1. hash = System.identityHashCode(key)
2. index = ((hash << 1) - (hash << 8)) & (length - 1)

冲突处理:
如果位置index已被占用,则探测index+2, index+4, ...
```

### 2.3 紧凑存储结构

```
table数组结构:
[0]   key1
[1]   value1
[2]   key2
[3]   value2
[4]   key3
[5]   value3
...
```

这种结构提高了缓存局部性,减少了内存占用。

## 三、继承结构

```
AbstractMap<K,V>
    ↑
IdentityHashMap<K,V>
    implements Map<K,V>, Serializable, Cloneable
```

### 3.1 实现的接口

- Map: Map接口
- Serializable: 可序列化
- Cloneable: 可克隆

## 四、核心字段分析

### 4.1 常量字段

```java
// 默认初始容量,必须是2的幂
private static final int DEFAULT_CAPACITY = 32;

// 最小容量,必须是2的幂
private static final int MINIMUM_CAPACITY = 4;

// 最大容量,必须是2的幂且 <= 1<<29
private static final int MAXIMUM_CAPACITY = 1 << 29;

// 表示null键的特殊对象
static final Object NULL_KEY = new Object();
```

### 4.2 实例字段

```java
// 哈希表数组,长度必须是2的幂
// 数组交替存储键和值: [key1, value1, key2, value2, ...]
transient Object[] table;

// 键值映射数量
int size;

// 结构修改次数,用于fail-fast机制
transient int modCount;
```

### 4.3 字段设计要点

1. **table数组**: 键值交替存储,提高缓存局部性
2. **NULL_KEY对象**: 允许null键,内部用特殊对象表示
3. **modCount**: 实现快速失败(fail-fast)机制
4. **容量限制**: 最大容量为MAXIMUM_CAPACITY-1,因为必须保留至少一个空槽

## 五、核心方法解析

### 5.1 构造方法

```java
// 默认构造方法
public IdentityHashMap() {
    init(DEFAULT_CAPACITY);
}

// 指定期望最大大小
public IdentityHashMap(int expectedMaxSize) {
    if (expectedMaxSize < 0)
        throw new IllegalArgumentException("expectedMaxSize is negative: "
                                           + expectedMaxSize);
    init(capacity(expectedMaxSize));
}

// 从另一个Map构造
public IdentityHashMap(Map<? extends K, ? extends V> m) {
    // 允许一定的增长空间
    this((int) ((1 + m.size()) * 1.1));
    putAll(m);
}

// 计算合适的容量
private static int capacity(int expectedMaxSize) {
    return
        (expectedMaxSize > MAXIMUM_CAPACITY / 3) ? MAXIMUM_CAPACITY :
        (expectedMaxSize <= 2 * MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
        Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
}

// 初始化哈希表
private void init(int initCapacity) {
    table = new Object[2 * initCapacity];  // 容量乘以2,因为键值交替存储
}
```

### 5.2 辅助方法

```java
// null键处理: 用NULL_KEY代替null
private static Object maskNull(Object key) {
    return (key == null ? NULL_KEY : key);
}

// null键恢复: 将NULL_KEY转回null
static final Object unmaskNull(Object key) {
    return (key == NULL_KEY ? null : key);
}

// 哈希函数: 使用System.identityHashCode
private static int hash(Object x, int length) {
    int h = System.identityHashCode(x);
    // 乘以-127,并左移以使用最低位作为哈希的一部分
    return ((h << 1) - (h << 8)) & (length - 1);
}

// 循环遍历表
private static int nextKeyIndex(int i, int len) {
    return (i + 2 < len ? i + 2 : 0);  // 步长为2,因为键值交替存储
}
```

### 5.3 get - 获取值

```java
@SuppressWarnings("unchecked")
public V get(Object key) {
    Object k = maskNull(key);
    Object[] tab = table;
    int len = tab.length;
    int i = hash(k, len);

    // 线性探测查找
    while (true) {
        Object item = tab[i];
        if (item == k)  // 使用==比较
            return (V) tab[i + 1];
        if (item == null)  // 找到空槽,说明不存在
            return null;
        i = nextKeyIndex(i, len);  // 探测下一个位置
    }
}
```

**方法要点**:
- 使用==比较键,而不是equals()
- 线性探测直到找到键或遇到空槽
- 步长为2,因为键值交替存储

### 5.4 containsKey - 检查键是否存在

```java
public boolean containsKey(Object key) {
    Object k = maskNull(key);
    Object[] tab = table;
    int len = tab.length;
    int i = hash(k, len);

    while (true) {
        Object item = tab[i];
        if (item == k)
            return true;
        if (item == null)
            return false;
        i = nextKeyIndex(i, len);
    }
}
```

### 5.5 containsValue - 检查值是否存在

```java
public boolean containsValue(Object value) {
    Object[] tab = table;
    // 遍历所有值(奇数索引)
    for (int i = 1; i < tab.length; i += 2)
        if (tab[i] == value && tab[i - 1] != null)  // 使用==比较
            return true;

    return false;
}
```

### 5.6 put - 插入键值对

```java
public V put(K key, V value) {
    final Object k = maskNull(key);

    retryAfterResize: for (;;) {
        final Object[] tab = table;
        final int len = tab.length;
        int i = hash(k, len);

        // 查找是否已存在该键
        for (Object item; (item = tab[i]) != null;
             i = nextKeyIndex(i, len)) {
            if (item == k) {  // 使用==比较
                @SuppressWarnings("unchecked")
                    V oldValue = (V) tab[i + 1];
                tab[i + 1] = value;
                return oldValue;  // 返回旧值
            }
        }

        // 检查是否需要扩容
        final int s = size + 1;
        // 使用优化的3*s形式
        // 下一个容量是len,即当前容量的2倍
        if (s + (s << 1) > len && resize(len))
            continue retryAfterResize;

        // 插入新条目
        modCount++;
        tab[i] = k;
        tab[i + 1] = value;
        size = s;
        return null;
    }
}
```

**方法要点**:
- 使用线性探测查找插入位置
- 如果键已存在,更新值
- 达到阈值时自动扩容
- 扩容后重试插入

### 5.7 resize - 扩容

```java
private boolean resize(int newCapacity) {
    int newLength = newCapacity * 2;  // 键值交替存储,长度乘以2

    Object[] oldTable = table;
    int oldLength = oldTable.length;

    if (oldLength == 2 * MAXIMUM_CAPACITY) {  // 无法继续扩容
        if (size == MAXIMUM_CAPACITY - 1)
            throw new IllegalStateException("Capacity exhausted.");
        return false;
    }
    if (oldLength >= newLength)
        return false;

    Object[] newTable = new Object[newLength];

    // 迁移所有条目
    for (int j = 0; j < oldLength; j += 2) {
        Object key = oldTable[j];
        if (key != null) {
            Object value = oldTable[j+1];
            oldTable[j] = null;
            oldTable[j+1] = null;
            int i = hash(key, newLength);  // 重新计算哈希
            // 线性探测找到空槽
            while (newTable[i] != null)
                i = nextKeyIndex(i, newLength);
            newTable[i] = key;
            newTable[i + 1] = value;
        }
    }
    table = newTable;
    return true;
}
```

**方法要点**:
- 创建新数组,长度为旧数组的2倍
- 重新计算所有条目的哈希值
- 使用线性探测插入新数组

### 5.8 remove - 移除条目

```java
public V remove(Object key) {
    Object k = maskNull(key);
    Object[] tab = table;
    int len = tab.length;
    int i = hash(k, len);

    while (true) {
        Object item = tab[i];
        if (item == k) {
            modCount++;
            size--;
            @SuppressWarnings("unchecked")
                V oldValue = (V) tab[i + 1];
            tab[i + 1] = null;
            tab[i] = null;
            closeDeletion(i);  // 闭合删除间隙
            return oldValue;
        }
        if (item == null)
            return null;
        i = nextKeyIndex(i, len);
    }
}
```

### 5.9 closeDeletion - 闭合删除间隙

```java
private void closeDeletion(int d) {
    // 改编自Knuth第6.4节算法R
    Object[] tab = table;
    int len = tab.length;

    // 查找需要交换到新空槽的条目
    // 从删除位置的下一个索引开始,直到遇到空槽
    Object item;
    for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
         i = nextKeyIndex(i, len)) {
        // 以下测试触发如果位置i的条目(哈希到位置r)
        // 应该占据d腾出的位置。如果是,交换它,
        // 然后继续,现在d在新腾出的i处。
        // 当遇到此运行的末尾的空槽时,此过程将终止。
        // 测试很复杂,因为我们使用循环表。
        int r = hash(item, len);
        if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
            tab[d] = item;
            tab[d + 1] = tab[i + 1];
            tab[i] = null;
            tab[i + 1] = null;
            d = i;
        }
    }
}
```

**方法要点**:
- 删除条目后,需要重新哈希后续条目
- 确保线性探测的正确性
- 避免查找操作失败

### 5.10 clear - 清空Map

```java
public void clear() {
    modCount++;
    Object[] tab = table;
    for (int i = 0; i < tab.length; i++)
        tab[i] = null;
    size = 0;
}
```

### 5.11 equals - 相等性比较

```java
public boolean equals(Object o) {
    if (o == this) {
        return true;
    } else if (o instanceof IdentityHashMap) {
        IdentityHashMap<?,?> m = (IdentityHashMap<?,?>) o;
        if (m.size() != size)
            return false;

        Object[] tab = m.table;
        for (int i = 0; i < tab.length; i+=2) {
            Object k = tab[i];
            if (k != null && !containsMapping(k, tab[i + 1]))
                return false;
        }
        return true;
    } else if (o instanceof Map) {
        Map<?,?> m = (Map<?,?>)o;
        return entrySet().equals(m.entrySet());
    } else {
        return false;  // o不是Map
    }
}
```

**注意**: 由于引用相等性语义,IdentityHashMap与普通Map比较时可能违反对称性和传递性要求。

### 5.12 hashCode - 哈希码计算

```java
public int hashCode() {
    int result = 0;
    Object[] tab = table;
    for (int i = 0; i < tab.length; i +=2) {
        Object key = tab[i];
        if (key != null) {
            Object k = unmaskNull(key);
            result += System.identityHashCode(k) ^
                      System.identityHashCode(tab[i + 1]);
        }
    }
    return result;
}
```

**注意**: 使用System.identityHashCode()而不是对象的hashCode()。

### 5.13 clone - 克隆

```java
public Object clone() {
    try {
        IdentityHashMap<?,?> m = (IdentityHashMap<?,?>) super.clone();
        m.entrySet = null;
        m.table = table.clone();  // 浅拷贝
        return m;
    } catch (CloneNotSupportedException e) {
        throw new InternalError(e);
    }
}
```

## 六、迭代器实现

### 6.1 IdentityHashMapIterator - 抽象迭代器

```java
private abstract class IdentityHashMapIterator<T> implements Iterator<T> {
    int index = (size != 0 ? 0 : table.length);  // 当前槽
    int expectedModCount = modCount;  // 支持快速失败
    int lastReturnedIndex = -1;  // 允许remove()
    boolean indexValid;  // 避免不必要的next计算
    Object[] traversalTable = table;  // 引用主表或副本

    public boolean hasNext() {
        Object[] tab = traversalTable;
        for (int i = index; i < tab.length; i+=2) {
            Object key = tab[i];
            if (key != null) {
                index = i;
                return indexValid = true;
            }
        }
        index = tab.length;
        return false;
    }

    protected int nextIndex() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        if (!indexValid && !hasNext())
            throw new NoSuchElementException();

        indexValid = false;
        lastReturnedIndex = index;
        index += 2;
        return lastReturnedIndex;
    }

    public void remove() {
        if (lastReturnedIndex == -1)
            throw new IllegalStateException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();

        expectedModCount = ++modCount;
        int deletedSlot = lastReturnedIndex;
        lastReturnedIndex = -1;
        // 回退索引以在删除后重新访问新内容
        index = deletedSlot;
        indexValid = false;

        // 删除代码类似于closeDeletion,除了它必须捕获
        // 已经看到的元素被交换到一个空槽中的罕见情况,
        // 该空槽稍后将被此迭代器遍历。
        // 我们不能允许未来的next()调用再次返回它。
        // 在2/3负载因子下发生这种情况的可能性非常小,
        // 但当它发生时,我们必须制作表的其余部分的副本
        // 用于遍历的其余部分。由于这只能发生在接近表末尾时,
        // 即使在这些罕见情况下,这在时间或空间上也不太昂贵。

        Object[] tab = traversalTable;
        int len = tab.length;

        int d = deletedSlot;
        Object key = tab[d];
        tab[d] = null;  // 腾出槽
        tab[d + 1] = null;

        // 如果遍历副本,在真实表中删除。
        // 我们可以跳过副本上的间隙闭合。
        if (tab != IdentityHashMap.this.table) {
            IdentityHashMap.this.remove(key);
            expectedModCount = modCount;
            return;
        }

        size--;

        Object item;
        for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
             i = nextKeyIndex(i, len)) {
            int r = hash(item, len);
            // 参见closeDeletion对此条件的解释
            if ((i < r && (r <= d || d <= i)) ||
                (r <= d && d <= i)) {

                // 如果我们即将交换一个已经看到的元素
                // 到一个稍后可能被next()返回的槽中,
                // 那么克隆表的其余部分用于未来的next()调用。
                // 我们的副本在"错误"的位置有一个间隙是可以的,
                // 因为它永远不会用于搜索。

                if (i < deletedSlot && d >= deletedSlot &&
                    traversalTable == IdentityHashMap.this.table) {
                    int remaining = len - deletedSlot;
                    Object[] newTable = new Object[remaining];
                    System.arraycopy(tab, deletedSlot,
                                     newTable, 0, remaining);
                    traversalTable = newTable;
                    index = 0;
                }

                tab[d] = item;
                tab[d + 1] = tab[i + 1];
                tab[i] = null;
                tab[i + 1] = null;
                d = i;
            }
        }
    }
}
```

**设计要点**:
- 实现fail-fast机制
- 处理删除时的间隙闭合
- 可能创建表的副本以避免重复返回元素

### 6.2 具体迭代器

```java
// 键迭代器
private class KeyIterator extends IdentityHashMapIterator<K> {
    @SuppressWarnings("unchecked")
    public K next() {
        return (K) unmaskNull(traversalTable[nextIndex()]);
    }
}

// 值迭代器
private class ValueIterator extends IdentityHashMapIterator<V> {
    @SuppressWarnings("unchecked")
    public V next() {
        return (V) traversalTable[nextIndex() + 1];
    }
}

// 条目迭代器
private class EntryIterator
    extends IdentityHashMapIterator<Map.Entry<K,V>>
{
    private Entry lastReturnedEntry;

    public Map.Entry<K,V> next() {
        lastReturnedEntry = new Entry(nextIndex());
        return lastReturnedEntry;
    }

    public void remove() {
        lastReturnedIndex =
            ((null == lastReturnedEntry) ? -1 : lastReturnedEntry.index);
        super.remove();
        lastReturnedEntry.index = lastReturnedIndex;
        lastReturnedEntry = null;
    }

    private class Entry implements Map.Entry<K,V> {
        private int index;

        private Entry(int index) {
            this.index = index;
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            checkIndexForEntryUse();
            return (K) unmaskNull(traversalTable[index]);
        }

        @SuppressWarnings("unchecked")
        public V getValue() {
            checkIndexForEntryUse();
            return (V) traversalTable[index+1];
        }

        @SuppressWarnings("unchecked")
        public V setValue(V value) {
            checkIndexForEntryUse();
            V oldValue = (V) traversalTable[index+1];
            traversalTable[index+1] = value;
            // 如果使用副本,强制进入主表
            if (traversalTable != IdentityHashMap.this.table)
                put((K) traversalTable[index], value);
            return oldValue;
        }

        public boolean equals(Object o) {
            if (index < 0)
                return super.equals(o);

            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return (e.getKey() == unmaskNull(traversalTable[index]) &&
                   e.getValue() == traversalTable[index+1]);
        }

        public int hashCode() {
            if (lastReturnedIndex < 0)
                return super.hashCode();

            return (System.identityHashCode(unmaskNull(traversalTable[index])) ^
                   System.identityHashCode(traversalTable[index+1]));
        }

        public String toString() {
            if (index < 0)
                return super.toString();

            return (unmaskNull(traversalTable[index]) + "="
                    + traversalTable[index+1]);
        }

        private void checkIndexForEntryUse() {
            if (index < 0)
                throw new IllegalStateException("Entry was removed");
        }
    }
}
```

## 七、视图实现

### 7.1 KeySet - 键集合视图

```java
private class KeySet extends AbstractSet<K> {
    public Iterator<K> iterator() {
        return new KeyIterator();
    }
    public int size() {
        return size;
    }
    public boolean contains(Object o) {
        return containsKey(o);
    }
    public boolean remove(Object o) {
        int oldSize = size;
        IdentityHashMap.this.remove(o);
        return size != oldSize;
    }
    // 必须从AbstractSet的实现恢复到AbstractCollection的,
    // 因为前者包含一个优化,当c是较小的"正常"(非基于身份的)Set时,
    // 会导致不正确的行为。
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        for (Iterator<K> i = iterator(); i.hasNext(); ) {
            if (c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }
    public void clear() {
        IdentityHashMap.this.clear();
    }
    public int hashCode() {
        int result = 0;
        for (K key : this)
            result += System.identityHashCode(key);
        return result;
    }
    public Object[] toArray() {
        return toArray(new Object[0]);
    }
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int expectedModCount = modCount;
        int size = size();
        if (a.length < size)
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        Object[] tab = table;
        int ti = 0;
        for (int si = 0; si < tab.length; si += 2) {
            Object key;
            if ((key = tab[si]) != null) {  // 键存在?
                // 比预期更多的元素 -> 来自其他线程的并发修改
                if (ti >= size) {
                    throw new ConcurrentModificationException();
                }
                a[ti++] = (T) unmaskNull(key);  // 解除键的掩码
            }
        }
        // 比预期更少的元素或检测到来自其他线程的并发修改
        if (ti < size || expectedModCount != modCount) {
            throw new ConcurrentModificationException();
        }
        // 根据规范的最终null标记
        if (ti < a.length) {
            a[ti] = null;
        }
        return a;
    }

    public Spliterator<K> spliterator() {
        return new KeySpliterator<>(IdentityHashMap.this, 0, -1, 0, 0);
    }
}
```

### 7.2 Values - 值集合视图

```java
private class Values extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return new ValueIterator();
    }
    public int size() {
        return size;
    }
    public boolean contains(Object o) {
        return containsValue(o);
    }
    public boolean remove(Object o) {
        for (Iterator<V> i = iterator(); i.hasNext(); ) {
            if (i.next() == o) {
                i.remove();
                return true;
            }
        }
        return false;
    }
    public void clear() {
        IdentityHashMap.this.clear();
    }
    public Object[] toArray() {
        return toArray(new Object[0]);
    }
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int expectedModCount = modCount;
        int size = size();
        if (a.length < size)
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        Object[] tab = table;
        int ti = 0;
        for (int si = 0; si < tab.length; si += 2) {
            if (tab[si] != null) {  // 键存在?
                // 比预期更多的元素 -> 来自其他线程的并发修改
                if (ti >= size) {
                    throw new ConcurrentModificationException();
                }
                a[ti++] = (T) tab[si+1];  // 复制值
            }
        }
        // 比预期更少的元素或检测到来自其他线程的并发修改
        if (ti < size || expectedModCount != modCount) {
            throw new ConcurrentModificationException();
        }
        // 根据规范的最终null标记
        if (ti < a.length) {
            a[ti] = null;
        }
        return a;
    }

    public Spliterator<V> spliterator() {
        return new ValueSpliterator<>(IdentityHashMap.this, 0, -1, 0, 0);
    }
}
```

### 7.3 EntrySet - 条目集合视图

```java
private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator();
    }
    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        return containsMapping(entry.getKey(), entry.getValue());
    }
    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        return removeMapping(entry.getKey(), entry.getValue());
    }
    public int size() {
        return size;
    }
    public void clear() {
        IdentityHashMap.this.clear();
    }
    // 必须从AbstractSet的实现恢复到AbstractCollection的,
    // 因为前者包含一个优化,当c是较小的"正常"(非基于身份的)Set时,
    // 会导致不正确的行为。
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        for (Iterator<Map.Entry<K,V>> i = iterator(); i.hasNext(); ) {
            if (c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }

    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int expectedModCount = modCount;
        int size = size();
        if (a.length < size)
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        Object[] tab = table;
        int ti = 0;
        for (int si = 0; si < tab.length; si += 2) {
            Object key;
            if ((key = tab[si]) != null) {  // 键存在?
                // 比预期更多的元素 -> 来自其他线程的并发修改
                if (ti >= size) {
                    throw new ConcurrentModificationException();
                }
                a[ti++] = (T) new AbstractMap.SimpleEntry<>(unmaskNull(key), tab[si + 1]);
            }
        }
        // 比预期更少的元素或检测到来自其他线程的并发修改
        if (ti < size || expectedModCount != modCount) {
            throw new ConcurrentModificationException();
        }
        // 根据规范的最终null标记
        if (ti < a.length) {
            a[ti] = null;
        }
        return a;
    }

    public Spliterator<Map.Entry<K,V>> spliterator() {
        return new EntrySpliterator<>(IdentityHashMap.this, 0, -1, 0, 0);
    }
}
```

## 八、设计模式

### 8.1 模板方法模式

AbstractMap定义了Map的基本骨架,IdentityHashMap实现具体操作:
- AbstractMap: 提供通用实现
- IdentityHashMap: 实现特定行为(引用相等性)

### 8.2 迭代器模式

提供多种迭代器:
- KeyIterator: 键迭代
- ValueIterator: 值迭代
- EntryIterator: 条目迭代

### 8.3 视图模式

提供Map的不同视图:
- keySet(): 键集合视图
- values(): 值集合视图
- entrySet(): 条目集合视图

### 8.4 原型模式

实现Cloneable接口,支持浅拷贝:
```java
public Object clone() {
    IdentityHashMap<?,?> m = (IdentityHashMap<?,?>) super.clone();
    m.table = table.clone();
    return m;
}
```

## 九、面试常见问题

### 9.1 IdentityHashMap与HashMap的区别?

| 特性 | HashMap | IdentityHashMap |
|------|---------|----------------|
| 键比较 | equals() | == |
| 哈希函数 | hashCode() | identityHashCode() |
| 冲突解决 | 拉链法 | 线性探测 |
| 存储结构 | Entry数组 | 键值交替数组 |
| 内存效率 | 较低 | 较高 |
| 适用场景 | 通用映射 | 需要区分对象实例 |

### 9.2 IdentityHashMap使用什么哈希函数?

使用System.identityHashCode(),它返回对象的默认哈希码(基于对象地址)。

### 9.3 IdentityHashMap如何解决哈希冲突?

使用开放寻址法中的线性探测:
```
1. 计算初始索引: index = hash(key, length)
2. 如果位置index已被占用,探测index+2, index+4, ...
3. 直到找到空槽或找到匹配的键
```

### 9.4 IdentityHashMap的存储结构是怎样的?

键和值交替存储在同一个数组中:
```
table[0] = key1
table[1] = value1
table[2] = key2
table[3] = value2
...
```

这种结构提高了缓存局部性,减少了内存占用。

### 9.5 IdentityHashMap是线程安全的吗?

不是。需要外部同步:
```java
Map<K,V> map = Collections.synchronizedMap(new IdentityHashMap<>());
```

### 9.6 IdentityHashMap支持null键和null值吗?

支持。null键内部用NULL_KEY对象表示,null值直接存储。

### 9.7 IdentityHashMap的迭代器是fail-fast的吗?

是的。如果在迭代过程中Map被结构性修改,会抛出ConcurrentModificationException。

### 9.8 IdentityHashMap的扩容机制?

当size + (size << 1) > table.length时扩容:
- 新容量为旧容量的2倍
- 重新计算所有条目的哈希值
- 使用线性探测插入新数组

### 9.9 IdentityHashMap违反了Map的契约吗?

是的。它故意违反了Map的一般契约,特别是:
- 使用==而不是equals()比较键
- 使用identityHashCode()而不是hashCode()
- 这可能导致与普通Map比较时违反对称性和传递性

### 9.10 IdentityHashMap的equals()和hashCode()有什么特殊之处?

- equals(): 使用==比较键和值
- hashCode(): 使用System.identityHashCode()计算哈希码
- 这可能导致与普通Map比较时出现问题

## 十、使用场景

### 10.1 对象图遍历

```java
// 深拷贝时跟踪已访问的对象
IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

public Object deepCopy(Object obj) {
    if (visited.containsKey(obj)) {
        return visited.get(obj);
    }
    Object copy = createCopy(obj);
    visited.put(obj, copy);
    // 递归复制子对象
    return copy;
}
```

### 10.2 序列化

```java
// 序列化时跟踪已序列化的对象
IdentityHashMap<Object, Integer> serialized = new IdentityHashMap<>();

public void serialize(Object obj, ObjectOutputStream out) {
    if (serialized.containsKey(obj)) {
        out.writeObject(new Reference(serialized.get(obj)));
        return;
    }
    int id = serialized.size();
    serialized.put(obj, id);
    out.writeObject(new NewObject(id));
    // 序列化对象内容
}
```

### 10.3 调试代理

```java
// 为每个对象创建调试代理
IdentityHashMap<Object, DebugProxy> proxies = new IdentityHashMap<>();

public DebugProxy getProxy(Object obj) {
    return proxies.computeIfAbsent(obj, DebugProxy::new);
}
```

### 10.4 对象去重

```java
// 基于对象引用去重
IdentityHashMap<Object, Object> unique = new IdentityHashMap<>();

public <T> List<T> deduplicate(List<T> list) {
    List<T> result = new ArrayList<>();
    for (T item : list) {
        if (!unique.containsKey(item)) {
            unique.put(item, item);
            result.add(item);
        }
    }
    return result;
}
```

## 十一、注意事项

### 11.1 与普通Map的交互

```java
// 警告: IdentityHashMap与HashMap比较可能违反对称性
IdentityHashMap<String, String> identityMap = new IdentityHashMap<>();
HashMap<String, String> hashMap = new HashMap<>();

String s1 = new String("key");
String s2 = new String("key");

identityMap.put(s1, "value1");
hashMap.put(s2, "value2");

// s1.equals(s2) == true, 但 s1 != s2
System.out.println(identityMap.equals(hashMap));  // 可能返回false
System.out.println(hashMap.equals(identityMap));  // 可能返回true
```

### 11.2 字符串的使用

```java
// 警告: 字符串常量池可能导致意外行为
IdentityHashMap<String, String> map = new IdentityHashMap<>();

String s1 = "key";
String s2 = "key";

map.put(s1, "value1");
System.out.println(map.containsKey(s2));  // 返回true,因为s1 == s2

// 但使用new String()时:
String s3 = new String("key");
System.out.println(map.containsKey(s3));  // 返回false,因为s1 != s3
```

### 11.3 性能考虑

```java
// 线性探测在负载因子高时性能下降
IdentityHashMap<Object, Object> map = new IdentityHashMap<>();

// 建议设置合适的初始容量
int expectedSize = 1000;
IdentityHashMap<Object, Object> map = new IdentityHashMap<>(expectedSize);
```

### 11.4 内存使用

```java
// IdentityHashMap内存效率高,适合大量小对象
IdentityHashMap<Object, Object> map = new IdentityHashMap<>();

// 但不适合存储大对象,因为值是强引用
```

## 十二、与其他Map的对比

### 12.1 HashMap vs IdentityHashMap

```java
// HashMap: 使用equals()比较
HashMap<String, String> hashMap = new HashMap<>();
String s1 = new String("key");
String s2 = new String("key");
hashMap.put(s1, "value");
System.out.println(hashMap.containsKey(s2));  // true

// IdentityHashMap: 使用==比较
IdentityHashMap<String, String> identityMap = new IdentityHashMap<>();
identityMap.put(s1, "value");
System.out.println(identityMap.containsKey(s2));  // false
```

### 12.2 LinkedHashMap vs IdentityHashMap

| 特性 | LinkedHashMap | IdentityHashMap |
|------|---------------|----------------|
| 键比较 | equals() | == |
| 顺序 | 插入顺序/访问顺序 | 无序 |
| 冲突解决 | 拉链法 | 线性探测 |
| 内存效率 | 较低 | 较高 |

### 12.3 ConcurrentHashMap vs IdentityHashMap

| 特性 | ConcurrentHashMap | IdentityHashMap |
|------|---------------------|----------------|
| 线程安全 | 是 | 否 |
| 键比较 | equals() | == |
| 并发性能 | 高 | 低 |
| 适用场景 | 高并发映射 | 单线程映射 |

## 十三、最佳实践

### 13.1 选择合适的Map

```java
// 通用映射: HashMap
Map<String, Object> map = new HashMap<>();

// 需要区分对象实例: IdentityHashMap
Map<Object, Object> identityMap = new IdentityHashMap<>();

// 需要保持顺序: LinkedHashMap
Map<String, Object> orderedMap = new LinkedHashMap<>();

// 线程安全: ConcurrentHashMap
Map<String, Object> concurrentMap = new ConcurrentHashMap<>();

// 需要排序: TreeMap
Map<String, Object> sortedMap = new TreeMap<>();
```

### 13.2 设置合适的初始容量

```java
// 根据预期大小设置初始容量
int expectedSize = 1000;
IdentityHashMap<Object, Object> map = new IdentityHashMap<>(expectedSize);
```

### 13.3 正确处理字符串

```java
// 使用字符串常量时注意引用相等性
IdentityHashMap<String, String> map = new IdentityHashMap<>();

String key = "key";  // 字符串常量
map.put(key, "value");

String anotherKey = "key";  // 相同的字符串常量
System.out.println(map.containsKey(anotherKey));  // true

// 使用new String()时:
String newKey = new String("key");
System.out.println(map.containsKey(newKey));  // false
```

### 13.4 外部同步

```java
// 创建同步的IdentityHashMap
Map<K,V> synchronizedMap = Collections.synchronizedMap(new IdentityHashMap<>());

// 使用时同步
synchronized (synchronizedMap) {
    for (Map.Entry<K,V> entry : synchronizedMap.entrySet()) {
        // 处理条目
    }
}
```

### 13.5 避免与普通Map混淆

```java
// 不要混用IdentityHashMap和普通Map
IdentityHashMap<Object, Object> identityMap = new IdentityHashMap<>();
HashMap<Object, Object> hashMap = new HashMap<>();

// 警告: 比较可能违反对称性
System.out.println(identityMap.equals(hashMap));  // 可能返回false
System.out.println(hashMap.equals(identityMap));  // 可能返回true
```

## 十四、总结

IdentityHashMap是一个特殊的Map实现,使用引用相等性而不是对象相等性来比较键和值。它使用线性探测哈希表和紧凑的存储结构,在特定场景下具有优势。

### 核心要点

1. **引用相等性**: 使用==比较键和值,而不是equals()
2. **线性探测**: 使用开放寻址法解决哈希冲突
3. **紧凑存储**: 键值交替存储在同一个数组中
4. **违反Map契约**: 故意违反Map的一般契约
5. **内存效率**: 比HashMap更节省内存
6. **线程不安全**: 需要外部同步
7. **适用场景**: 对象图遍历、序列化、调试代理等

### 适用场景

- 对象图遍历和去重
- 序列化和深拷贝
- 调试代理
- 需要区分不同对象实例的场景

### 不适用场景

- 需要对象相等性语义
- 需要保持顺序
- 多线程环境(需外部同步)
- 与普通Map混合使用
