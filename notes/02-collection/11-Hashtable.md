# Hashtable 源码解读

## 一、类概述

Hashtable是Java集合框架中的一个古老的类,它实现了基于哈希表的Map接口。Hashtable是线程安全的,所有公共方法都使用synchronized修饰。它不允许null键和null值。

### 1.1 核心特性

- **线程安全**: 所有公共方法都使用synchronized修饰
- **不允许null**: 不允许null键和null值
- **拉链法**: 使用链表解决哈希冲突
- **古老实现**: JDK 1.0就存在,设计较为陈旧
- **同步开销**: 使用粗粒度锁,并发性能较差
- **遗留类**: 官方推荐使用HashMap或ConcurrentHashMap替代

### 1.2 适用场景

- 需要线程安全的简单映射(但不推荐)
- 遗留代码维护
- 需要与旧API兼容的场景

### 1.3 与HashMap的区别

| 特性 | Hashtable | HashMap |
|------|-----------|---------|
| 线程安全 | 是(synchronized) | 否 |
| null键值 | 不允许 | 允许 |
| 继承 | Dictionary<K,V> | AbstractMap<K,V> |
| 迭代器 | fail-fast | fail-fast |
| 性能 | 较低(同步开销) | 较高 |
| 推荐 | 不推荐 | 推荐 |

## 二、设计原理

### 2.1 哈希表结构

Hashtable使用拉链法实现哈希表:

```
table数组:
[0] -> Entry1 -> Entry2 -> null
[1] -> null
[2] -> Entry3 -> null
...
```

### 2.2 索引计算

```java
int hash = key.hashCode();
int index = (hash & 0x7FFFFFFF) % table.length;
```

- 使用key.hashCode()获取哈希码
- 与0x7FFFFFFF进行与运算,确保为正数
- 使用取模运算计算索引

### 2.3 扩容机制

当元素数量超过阈值时扩容:
- 新容量 = 旧容量 * 2 + 1
- 重新计算所有元素的索引
- 使用头插法插入新表

## 三、继承结构

```
Dictionary<K,V>  (抽象类,已过时)
    ↑
Hashtable<K,V>
    implements Map<K,V>, Cloneable, Serializable
```

### 3.1 继承的类

- Dictionary: 抽象类,已过时,提供键值映射的基本操作

### 3.2 实现的接口

- Map: Map接口
- Cloneable: 可克隆
- Serializable: 可序列化

## 四、核心字段分析

### 4.1 实例字段

```java
// 哈希表数组
private transient Entry<?,?>[] table;

// 键值映射数量
private transient int count;

// 扩容阈值 (capacity * loadFactor)
private int threshold;

// 负载因子
private float loadFactor;

// 结构修改次数,用于fail-fast机制
private transient int modCount = 0;
```

### 4.2 常量字段

```java
// 数组最大大小
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

// 序列化版本号
private static final long serialVersionUID = 1421746759512286392L;
```

### 4.3 字段设计要点

1. **table数组**: 存储Entry链表头节点
2. **count**: 实际元素数量
3. **threshold**: 扩容阈值
4. **loadFactor**: 负载因子,默认0.75
5. **modCount**: 实现快速失败(fail-fast)机制

## 五、内部类Entry

### 5.1 Entry定义

```java
private static class Entry<K,V> implements Map.Entry<K,V> {
    final int hash;   // 键的哈希码
    final K key;     // 键
    V value;         // 值
    Entry<K,V> next; // 链表下一个节点

    protected Entry(int hash, K key, V value, Entry<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }

    @SuppressWarnings("unchecked")
    protected Object clone() {
        return new Entry<>(hash, key, value,
                              (next==null ? null : (Entry<K,V>) next.clone()));
    }

    // Map.Entry Ops

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        if (value == null)
            throw new NullPointerException();

        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;

        return (key==null ? e.getKey()==null : key.equals(e.getKey())) &&
               (value==null ? e.getValue()==null : value.equals(e.getValue()));
    }

    public int hashCode() {
        return hash ^ Objects.hashCode(value);
    }

    public String toString() {
        return key.toString()+"="+value.toString();
    }
}
```

### 5.2 Entry设计要点

1. **不可变字段**: hash和key是final的
2. **可变字段**: value和next可以修改
3. **链表结构**: next字段实现拉链法
4. **null检查**: setValue()不允许null值

## 六、核心方法解析

### 6.1 构造方法

```java
// 指定初始容量和负载因子
public Hashtable(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal Load: "+loadFactor);

    if (initialCapacity==0)
        initialCapacity = 1;
    this.loadFactor = loadFactor;
    table = new Entry<?,?>[initialCapacity];
    threshold = (int)Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
}

// 指定初始容量,使用默认负载因子
public Hashtable(int initialCapacity) {
    this(initialCapacity, 0.75f);
}

// 默认构造方法
public Hashtable() {
    this(11, 0.75f);  // 默认容量11,负载因子0.75
}

// 从另一个Map构造
public Hashtable(Map<? extends K, ? extends V> t) {
    this(Math.max(2*t.size(), 11), 0.75f);
    putAll(t);
}
```

### 6.2 size - 获取大小

```java
public synchronized int size() {
    return count;
}
```

### 6.3 isEmpty - 检查是否为空

```java
public synchronized boolean isEmpty() {
    return count == 0;
}
```

### 6.4 keys - 获取键枚举

```java
public synchronized Enumeration<K> keys() {
    return this.<K>getEnumeration(KEYS);
}
```

### 6.5 elements - 获取值枚举

```java
public synchronized Enumeration<V> elements() {
    return this.<V>getEnumeration(VALUES);
}
```

### 6.6 contains - 检查值是否存在

```java
public synchronized boolean contains(Object value) {
    if (value == null) {
        throw new NullPointerException();
    }

    Entry<?,?> tab[] = table;
    for (int i = tab.length ; i-- > 0 ;) {
        for (Entry<?,?> e = tab[i] ; e != null ; e = e.next) {
            if (e.value.equals(value)) {
                return true;
            }
        }
    }
    return false;
}
```

**方法要点**:
- 不允许null值
- 遍历所有链表查找值
- 时间复杂度O(n)

### 6.7 containsKey - 检查键是否存在

```java
public synchronized boolean containsKey(Object key) {
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<?,?> e = tab[index] ; e != null ; e = e.next) {
        if ((e.hash == hash) && e.key.equals(key)) {
            return true;
        }
    }
    return false;
}
```

### 6.8 get - 获取值

```java
@SuppressWarnings("unchecked")
public synchronized V get(Object key) {
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<?,?> e = tab[index] ; e != null ; e = e.next) {
        if ((e.hash == hash) && e.key.equals(key)) {
            return (V)e.value;
        }
    }
    return null;
}
```

**方法要点**:
- 计算哈希值和索引
- 遍历链表查找匹配的键
- 使用synchronized保证线程安全

### 6.9 rehash - 扩容

```java
@SuppressWarnings("unchecked")
protected void rehash() {
    int oldCapacity = table.length;
    Entry<?,?>[] oldMap = table;

    // 新容量 = 旧容量 * 2 + 1
    int newCapacity = (oldCapacity << 1) + 1;
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
        if (oldCapacity == MAX_ARRAY_SIZE)
            // 保持MAX_ARRAY_SIZE个桶
            return;
        newCapacity = MAX_ARRAY_SIZE;
    }
    Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];

    modCount++;
    threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    table = newMap;

    // 迁移所有条目
    for (int i = oldCapacity ; i-- > 0 ;) {
        for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
            Entry<K,V> e = old;
            old = old.next;

            // 重新计算索引
            int index = (e.hash & 0x7FFFFFFF) % newCapacity;
            // 头插法
            e.next = (Entry<K,V>)newMap[index];
            newMap[index] = e;
        }
    }
}
```

**方法要点**:
- 新容量为旧容量的2倍加1
- 重新计算所有Entry的索引
- 使用头插法插入新表

### 6.10 addEntry - 添加条目

```java
private void addEntry(int hash, K key, V value, int index) {
    modCount++;

    Entry<?,?> tab[] = table;
    if (count >= threshold) {
        // 超过阈值,扩容
        rehash();

        tab = table;
        hash = key.hashCode();
        index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // 创建新Entry,头插法
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>) tab[index];
    tab[index] = new Entry<>(hash, key, value, e);
    count++;
}
```

### 6.11 put - 插入键值对

```java
public synchronized V put(K key, V value) {
    // 确保值不为null
    if (value == null) {
        throw new NullPointerException();
    }

    // 确保键不在哈希表中
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> entry = (Entry<K,V>)tab[index];
    for(; entry != null ; entry = entry.next) {
        if ((entry.hash == hash) && entry.key.equals(key)) {
            V old = entry.value;
            entry.value = value;
            return old;  // 返回旧值
        }
    }

    addEntry(hash, key, value, index);
    return null;
}
```

**方法要点**:
- 不允许null键和null值
- 使用synchronized保证线程安全
- 如果键已存在,更新值
- 如果键不存在,添加新条目

### 6.12 remove - 移除条目

```java
public synchronized V remove(Object key) {
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for(Entry<K,V> prev = null ; e != null ; prev = e, e = e.next) {
        if ((e.hash == hash) && e.key.equals(key)) {
            modCount++;
            if (prev != null) {
                prev.next = e.next;
            } else {
                tab[index] = e.next;
            }
            count--;
            V oldValue = e.value;
            e.value = null;
            return oldValue;
        }
    }
    return null;
}
```

### 6.13 putAll - 批量插入

```java
public synchronized void putAll(Map<? extends K, ? extends V> t) {
    for (Map.Entry<? extends K, ? extends V> e : t.entrySet())
        put(e.getKey(), e.getValue());
}
```

### 6.14 clear - 清空Map

```java
public synchronized void clear() {
    Entry<?,?> tab[] = table;
    modCount++;
    for (int index = tab.length; --index >= 0; )
        tab[index] = null;
    count = 0;
}
```

### 6.15 clone - 克隆

```java
public synchronized Object clone() {
    try {
        Hashtable<?,?> t = (Hashtable<?,?>)super.clone();
        t.table = new Entry<?,?>[table.length];
        for (int i = table.length ; i-- > 0 ; ) {
            t.table[i] = (table[i] != null)
                ? (Entry<?,?>) table[i].clone() : null;
        }
        t.keySet = null;
        t.entrySet = null;
        t.values = null;
        t.modCount = 0;
        return t;
    } catch (CloneNotSupportedException e) {
        // 这不应该发生,因为我们是Cloneable
        throw new InternalError(e);
    }
}
```

### 6.16 equals - 相等性比较

```java
public synchronized boolean equals(Object o) {
    if (o == this)
        return true;

    if (!(o instanceof Map))
        return false;
    Map<?,?> t = (Map<?,?>) o;
    if (t.size() != size())
        return false;

    try {
        Iterator<Map.Entry<K,V>> i = entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            if (value == null) {
                if (!(t.get(key)==null && t.containsKey(key)))
                    return false;
            } else {
                if (!value.equals(t.get(key)))
                    return false;
            }
        }
    } catch (ClassCastException unused)   {
        return false;
    } catch (NullPointerException unused) {
        return false;
    }

    return true;
}
```

### 6.17 hashCode - 哈希码计算

```java
public synchronized int hashCode() {
    /*
     * 此代码检测由计算自引用哈希表的哈希码引起的递归,
     * 并防止否则会发生的堆栈溢出。
     * 这允许某些1.1时代的具有自引用哈希表的小程序工作。
     * 此代码滥用loadFactor字段作为哈希码进行中标志,
     * 以便不恶化空间性能。
     * 负负载因子表示哈希码计算正在进行。
     */
    int h = 0;
    if (count == 0 || loadFactor < 0)
        return h;  // 返回零

    loadFactor = -loadFactor;  // 标记哈希码计算进行中
    Entry<?,?>[] tab = table;
    for (Entry<?,?> entry : tab) {
        while (entry != null) {
            h += entry.hashCode();
            entry = entry.next;
        }
    }

    loadFactor = -loadFactor;  // 标记哈希码计算完成

    return h;
}
```

**注意**: 此方法使用loadFactor作为标志来检测递归调用,防止堆栈溢出。

### 6.18 getOrDefault - 获取值或默认值

```java
@Override
public synchronized V getOrDefault(Object key, V defaultValue) {
    V result = get(key);
    return (null == result) ? defaultValue : result;
}
```

### 6.19 forEach - 遍历

```java
@SuppressWarnings("unchecked")
@Override
public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
    Objects.requireNonNull(action);     // 显式检查,以防表为空
    final int expectedModCount = modCount;

    Entry<?, ?>[] tab = table;
    for (Entry<?, ?> entry : tab) {
        while (entry != null) {
            action.accept((K)entry.key, (V)entry.value);
            entry = entry.next;

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
```

### 6.20 replaceAll - 替换所有值

```java
@SuppressWarnings("unchecked")
@Override
public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Objects.requireNonNull(function);     // 显式检查,以防表为空
    final int expectedModCount = modCount;

    Entry<K, V>[] tab = (Entry<K, V>[])table;
    for (Entry<K, V> entry : tab) {
        while (entry != null) {
            entry.value = Objects.requireNonNull(
                function.apply(entry.key, entry.value));
            entry = entry.next;

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
```

### 6.21 putIfAbsent - 键不存在时插入

```java
@Override
public synchronized V putIfAbsent(K key, V value) {
    Objects.requireNonNull(value);

    // 确保键不在哈希表中
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> entry = (Entry<K,V>)tab[index];
    for (; entry != null; entry = entry.next) {
        if ((entry.hash == hash) && entry.key.equals(key)) {
            V old = entry.value;
            if (old == null) {
                entry.value = value;
            }
            return old;
        }
    }

    addEntry(hash, key, value, index);
    return null;
}
```

### 6.22 remove - 键值匹配时移除

```java
@Override
public synchronized boolean remove(Object key, Object value) {
    Objects.requireNonNull(value);

    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
        if ((e.hash == hash) && e.key.equals(key) && e.value.equals(value)) {
            modCount++;
            if (prev != null) {
                prev.next = e.next;
            } else {
                tab[index] = e.next;
            }
            count--;
            e.value = null;
            return true;
        }
    }
    return false;
}
```

### 6.23 replace - 值匹配时替换

```java
@Override
public synchronized boolean replace(K key, V oldValue, V newValue) {
    Objects.requireNonNull(oldValue);
    Objects.requireNonNull(newValue);
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (; e != null; e = e.next) {
        if ((e.hash == hash) && e.key.equals(key)) {
            if (e.value.equals(oldValue)) {
                e.value = newValue;
                return true;
            } else {
                return false;
            }
        }
    }
    return false;
}
```

### 6.24 replace - 替换值

```java
@Override
public synchronized V replace(K key, V value) {
    Objects.requireNonNull(value);
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (; e != null; e = e.next) {
        if ((e.hash == hash) && e.key.equals(key)) {
            V oldValue = e.value;
            e.value = value;
            return oldValue;
        }
    }
    return null;
}
```

### 6.25 computeIfAbsent - 键不存在时计算

```java
@Override
public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);

    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (; e != null; e = e.next) {
        if (e.hash == hash && e.key.equals(key)) {
            // Hashtable不接受null值
            return e.value;
        }
    }

    V newValue = mappingFunction.apply(key);
    if (newValue != null) {
        addEntry(hash, key, newValue, index);
    }

    return newValue;
}
```

### 6.26 computeIfPresent - 键存在时计算

```java
@Override
public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);

    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
        if (e.hash == hash && e.key.equals(key)) {
            V newValue = remappingFunction.apply(key, e.value);
            if (newValue == null) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
            } else {
                e.value = newValue;
            }
            return newValue;
        }
    }
    return null;
}
```

### 6.27 compute - 计算值

```java
@Override
public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);

    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
        if (e.hash == hash && Objects.equals(e.key, key)) {
            V newValue = remappingFunction.apply(key, e.value);
            if (newValue == null) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
            } else {
                e.value = newValue;
            }
            return newValue;
        }
    }

    V newValue = remappingFunction.apply(key, null);
    if (newValue != null) {
        addEntry(hash, key, newValue, index);
    }

    return newValue;
}
```

### 6.28 merge - 合并值

```java
@Override
public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);

    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>)tab[index];
    for (Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
        if (e.hash == hash && e.key.equals(key)) {
            V newValue = remappingFunction.apply(e.value, value);
            if (newValue == null) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
            } else {
                e.value = newValue;
            }
            return newValue;
        }
    }

    if (value != null) {
        addEntry(hash, key, value, index);
    }

    return value;
}
```

## 七、迭代器实现

### 7.1 Enumerator - 枚举器/迭代器

```java
private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
    Entry<?,?>[] table = Hashtable.this.table;
    int index = table.length;
    Entry<?,?> entry;
    Entry<?,?> lastReturned;
    int type;

    // 指示此Enumerator是作为Iterator还是Enumeration
    boolean iterator;

    // 迭代器认为支持Hashtable应该具有的modCount值。
    // 如果违反此期望,迭代器已检测到并发修改。
    protected int expectedModCount = modCount;

    Enumerator(int type, boolean iterator) {
        this.type = type;
        this.iterator = iterator;
    }

    public boolean hasMoreElements() {
        Entry<?,?> e = entry;
        int i = index;
        Entry<?,?>[] t = table;
        /* 使用局部变量以加快循环迭代 */
        while (e == null && i > 0) {
            e = t[--i];
        }
        entry = e;
        index = i;
        return e != null;
    }

    @SuppressWarnings("unchecked")
    public T nextElement() {
        Entry<?,?> et = entry;
        int i = index;
        Entry<?,?>[] t = table;
        /* 使用局部变量以加快循环迭代 */
        while (et == null && i > 0) {
            et = t[--i];
        }
        entry = et;
        index = i;
        if (et != null) {
            Entry<?,?> e = lastReturned = entry;
            entry = e.next;
            return type == KEYS ? (T)e.key : (type == VALUES ? (T)e.value : (T)e;
        }
        throw new NoSuchElementException("Hashtable Enumerator");
    }

    // Iterator方法
    public boolean hasNext() {
        return hasMoreElements();
    }

    public T next() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        return nextElement();
    }

    public void remove() {
        if (!iterator)
            throw new UnsupportedOperationException();
        if (lastReturned == null)
            throw new IllegalStateException("Hashtable Enumerator");
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();

        synchronized(Hashtable.this) {
            Entry<?,?>[] tab = Hashtable.this.table;
            int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

            @SuppressWarnings("unchecked")
            Entry<K,V> e = (Entry<K,V>)tab[index];
            for(Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
                if (e == lastReturned) {
                    modCount++;
                    expectedModCount++;
                    if (prev == null)
                        tab[index] = e.next;
                    else
                        prev.next = e.next;
                    count--;
                    lastReturned = null;
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }
    }
}
```

**设计要点**:
- 实现Enumeration和Iterator接口
- 支持fail-fast机制
- 可以禁用Iterator方法,仅作为Enumeration使用

## 八、视图实现

### 8.1 KeySet - 键集合视图

```java
private class KeySet extends AbstractSet<K> {
    public Iterator<K> iterator() {
        return getIterator(KEYS);
    }
    public int size() {
        return count;
    }
    public boolean contains(Object o) {
        return containsKey(o);
    }
    public boolean remove(Object o) {
        return Hashtable.this.remove(o) != null;
    }
    public void clear() {
        Hashtable.this.clear();
    }
}
```

### 8.2 EntrySet - 条目集合视图

```java
private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return getIterator(ENTRIES);
    }

    public boolean add(Map.Entry<K,V> o) {
        return super.add(o);
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        Object key = entry.getKey();
        Entry<?,?>[] tab = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;

        for (Entry<?,?> e = tab[index]; e != null; e = e.next)
            if (e.hash==hash && e.equals(entry))
                return true;
        return false;
    }

    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
        Object key = entry.getKey();
        Entry<?,?>[] tab = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;

        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>)tab[index];
        for(Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash==hash && e.equals(entry)) {
                modCount++;
                if (prev != null)
                    prev.next = e.next;
                else
                    tab[index] = e.next;

                count--;
                e.value = null;
                return true;
            }
        }
        return false;
    }

    public int size() {
        return count;
    }

    public void clear() {
        Hashtable.this.clear();
    }
}
```

### 8.3 ValueCollection - 值集合视图

```java
private class ValueCollection extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return getIterator(VALUES);
    }
    public int size() {
        return count;
    }
    public boolean contains(Object o) {
        return containsValue(o);
    }
    public void clear() {
        Hashtable.this.clear();
    }
}
```

## 九、设计模式

### 9.1 模板方法模式

Dictionary定义了键值映射的基本骨架,Hashtable实现具体操作:
- Dictionary: 提供通用抽象方法
- Hashtable: 实现具体行为

### 9.2 迭代器模式

提供多种迭代器:
- Enumerator: 同时实现Enumeration和Iterator
- 可以禁用Iterator方法,仅作为Enumeration使用

### 9.3 视图模式

提供Map的不同视图:
- keySet(): 键集合视图
- values(): 值集合视图
- entrySet(): 条目集合视图

### 9.4 原型模式

实现Cloneable接口,支持浅拷贝:
```java
public synchronized Object clone() {
    Hashtable<?,?> t = (Hashtable<?,?>)super.clone();
    t.table = new Entry<?,?>[table.length];
    for (int i = table.length ; i-- > 0 ; ) {
        t.table[i] = (table[i] != null)
            ? (Entry<?,?>) table[i].clone() : null;
    }
    return t;
}
```

## 十、面试常见问题

### 10.1 Hashtable与HashMap的区别?

| 特性 | Hashtable | HashMap |
|------|-----------|---------|
| 线程安全 | 是(synchronized) | 否 |
| null键值 | 不允许 | 允许 |
| 继承 | Dictionary<K,V> | AbstractMap<K,V> |
| 迭代器 | fail-fast | fail-fast |
| 性能 | 较低(同步开销) | 较高 |
| 推荐 | 不推荐 | 推荐 |
| 初始容量 | 11 | 16 |
| 扩容 | 2n+1 | 2n |

### 10.2 Hashtable是线程安全的吗?

是的,所有公共方法都使用synchronized修饰。但是:
- 使用粗粒度锁,并发性能较差
- 官方推荐使用ConcurrentHashMap替代

### 10.3 Hashtable允许null键和null值吗?

不允许。put()方法会检查:
```java
if (value == null) {
    throw new NullPointerException();
}
```

### 10.4 Hashtable的扩容机制?

当count >= threshold时扩容:
- 新容量 = 旧容量 * 2 + 1
- 重新计算所有Entry的索引
- 使用头插法插入新表

### 10.5 Hashtable的迭代器是fail-fast的吗?

Iterator是fail-fast的,但Enumeration不是:
```java
// Iterator会抛出ConcurrentModificationException
public T next() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
    return nextElement();
}

// Enumeration不会
public T nextElement() {
    // 不会检查modCount
}
```

### 10.6 Hashtable的hashCode()方法有什么特殊之处?

使用loadFactor作为标志来检测递归调用,防止堆栈溢出:
```java
if (count == 0 || loadFactor < 0)
    return h;  // 返回零

loadFactor = -loadFactor;  // 标记哈希码计算进行中
// ... 计算哈希码 ...
loadFactor = -loadFactor;  // 标记哈希码计算完成
```

### 10.7 为什么Hashtable不推荐使用?

1. **性能问题**: 使用粗粒度锁,并发性能差
2. **设计陈旧**: 继承自Dictionary,设计较旧
3. **null限制**: 不允许null键和null值
4. **更好的替代**: ConcurrentHashMap性能更好

### 10.8 Hashtable的初始容量是多少?

默认初始容量为11,负载因子为0.75:
```java
public Hashtable() {
    this(11, 0.75f);
}
```

### 10.9 Hashtable的索引计算方式?

```java
int hash = key.hashCode();
int index = (hash & 0x7FFFFFFF) % table.length;
```

- 使用key.hashCode()获取哈希码
- 与0x7FFFFFFF进行与运算,确保为正数
- 使用取模运算计算索引

### 10.10 Hashtable的Enumerator有什么特点?

- 同时实现Enumeration和Iterator接口
- 可以禁用Iterator方法,仅作为Enumeration使用
- 支持fail-fast机制(仅Iterator模式)

## 十一、使用场景

### 11.1 遗留代码维护

```java
// 遗留代码中使用Hashtable
Hashtable<String, String> table = new Hashtable<>();
table.put("key", "value");
```

### 11.2 需要与旧API兼容

```java
// 某些旧API要求使用Hashtable
public void process(Hashtable<String, Object> data) {
    // 处理数据
}
```

### 11.3 简单的线程安全映射(不推荐)

```java
// 简单场景下可以使用,但不推荐
Hashtable<String, Object> cache = new Hashtable<>();
```

## 十二、注意事项

### 12.1 null键和null值

```java
Hashtable<String, String> table = new Hashtable<>();

// 错误: 不允许null键
// table.put(null, "value");  // NullPointerException

// 错误: 不允许null值
// table.put("key", null);  // NullPointerException
```

### 12.2 性能问题

```java
// Hashtable使用粗粒度锁,并发性能差
Hashtable<String, Object> table = new Hashtable<>();

// 推荐: 使用ConcurrentHashMap
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
```

### 12.3 迭代器类型

```java
Hashtable<String, String> table = new Hashtable<>();
table.put("key1", "value1");
table.put("key2", "value2");

// Enumeration不是fail-fast
Enumeration<String> keys = table.keys();
while (keys.hasMoreElements()) {
    String key = keys.nextElement();
    // 可以在迭代期间修改table,不会抛出异常
}

// Iterator是fail-fast
Iterator<String> iterator = table.keySet().iterator();
while (iterator.hasNext()) {
    String key = iterator.next();
    // 不能在迭代期间修改table,否则抛出ConcurrentModificationException
}
```

### 12.4 扩容性能

```java
// 设置合适的初始容量,避免频繁扩容
int expectedSize = 1000;
Hashtable<String, Object> table = new Hashtable<>(expectedSize);
```

## 十三、与其他Map的对比

### 13.1 Hashtable vs HashMap

```java
// Hashtable: 线程安全,不允许null
Hashtable<String, String> table = new Hashtable<>();
table.put("key", "value");

// HashMap: 非线程安全,允许null
HashMap<String, String> map = new HashMap<>();
map.put("key", "value");
map.put(null, "value");  // 允许null键
map.put("key", null);  // 允许null值
```

### 13.2 Hashtable vs ConcurrentHashMap

| 特性 | Hashtable | ConcurrentHashMap |
|------|-----------|---------------------|
| 线程安全 | 是(synchronized) | 是(分段锁) |
| 并发性能 | 低 | 高 |
| null键值 | 不允许 | 不允许 |
| 迭代器 | fail-fast | 弱一致性 |
| 推荐 | 不推荐 | 推荐 |

### 13.3 Hashtable vs Properties

Properties继承自Hashtable,增加了字符串类型的方法:
```java
Properties props = new Properties();
props.setProperty("key", "value");  // 字符串方法
props.getProperty("key");  // 字符串方法
```

## 十四、最佳实践

### 14.1 选择合适的Map

```java
// 单线程环境: HashMap
Map<String, Object> map = new HashMap<>();

// 多线程环境: ConcurrentHashMap
Map<String, Object> concurrentMap = new ConcurrentHashMap<>();

// 遗留代码: Hashtable(不推荐)
Hashtable<String, Object> table = new Hashtable<>();

// 需要保持顺序: LinkedHashMap
Map<String, Object> orderedMap = new LinkedHashMap<>();

// 需要排序: TreeMap
Map<String, Object> sortedMap = new TreeMap<>();
```

### 14.2 设置合适的初始容量

```java
// 根据预期大小设置初始容量
int expectedSize = 1000;
Hashtable<String, Object> table = new Hashtable<>(expectedSize);
```

### 14.3 避免null键和null值

```java
Hashtable<String, String> table = new Hashtable<>();

// 错误: 不允许null键
// table.put(null, "value");  // NullPointerException

// 错误: 不允许null值
// table.put("key", null);  // NullPointerException

// 正确: 使用特殊值表示null
table.put("key", "");
```

### 14.4 使用Enumeration

```java
Hashtable<String, String> table = new Hashtable<>();
table.put("key1", "value1");
table.put("key2", "value2");

// 使用Enumeration遍历
Enumeration<String> keys = table.keys();
while (keys.hasMoreElements()) {
    String key = keys.nextElement();
    System.out.println(key + " = " + table.get(key));
}
```

### 14.5 迁移到ConcurrentHashMap

```java
// 旧代码
Hashtable<String, Object> table = new Hashtable<>();

// 新代码: 迁移到ConcurrentHashMap
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
```

## 十五、总结

Hashtable是一个古老的线程安全Map实现,使用synchronized方法实现线程安全。由于设计陈旧和性能问题,官方不推荐使用,建议使用HashMap或ConcurrentHashMap替代。

### 核心要点

1. **线程安全**: 所有公共方法都使用synchronized修饰
2. **不允许null**: 不允许null键和null值
3. **拉链法**: 使用链表解决哈希冲突
4. **古老实现**: JDK 1.0就存在,设计较为陈旧
5. **同步开销**: 使用粗粒度锁,并发性能较差
6. **遗留类**: 官方推荐使用HashMap或ConcurrentHashMap替代
7. **初始容量**: 默认11,负载因子0.75

### 适用场景

- 遗留代码维护
- 需要与旧API兼容
- 简单的线程安全映射(但不推荐)

### 不适用场景

- 高并发环境(推荐ConcurrentHashMap)
- 需要null键或null值
- 高性能要求
- 新项目开发

### 替代方案

- **单线程**: HashMap
- **多线程**: ConcurrentHashMap
- **需要顺序**: LinkedHashMap
- **需要排序**: TreeMap
