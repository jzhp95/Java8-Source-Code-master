# WeakHashMap 源码解读

## 一、类概述

WeakHashMap是Java集合框架中的一种特殊Map实现,它使用弱引用(WeakReference)来存储键。当键不再被外部强引用时,该键及其对应的值会自动从Map中移除。

### 1.1 核心特性

- **弱引用键**: 键通过WeakReference存储,不阻止垃圾回收
- **自动清理**: 垃圾回收后自动清理失效条目
- **引用队列**: 使用ReferenceQueue跟踪被回收的键
- **性能特点**: 类似HashMap,支持null键和null值
- **线程不安全**: 非同步实现,需要外部同步

### 1.2 适用场景

- 缓存实现: 临时缓存,自动清理不再使用的条目
- 监听器注册表: 避免内存泄漏
- 元数据映射: 与对象生命周期绑定的元数据

## 二、设计原理

### 2.1 弱引用机制

WeakHashMap的核心设计基于Java的弱引用机制:

```
强引用 → 普通对象引用,阻止GC回收
弱引用 → 不阻止GC回收,GC后引用变为null
软引用 → 内存不足时才回收
虚引用 → 无法通过引用获取对象,仅用于跟踪GC
```

### 2.2 引用队列工作流程

```
1. 创建Entry时,将WeakReference注册到ReferenceQueue
2. 当键被GC回收时,WeakReference被加入队列
3. 调用expungeStaleEntries()清理队列中的失效条目
4. 从哈希表中移除失效的Entry
```

### 2.3 数据结构

```java
Entry<K,V>[] table  // 哈希表数组,存储Entry链表
ReferenceQueue<Object> queue  // 引用队列,跟踪被回收的键
int size  // 实际条目数
int threshold  // 扩容阈值
float loadFactor  // 负载因子
```

## 三、继承结构

```
AbstractMap<K,V>
    ↑
WeakHashMap<K,V>
    implements Map<K,V>
```

### 3.1 继承的方法

- AbstractMap提供的基础Map操作
- Map接口定义的契约方法

## 四、核心字段分析

### 4.1 常量字段

```java
// 默认初始容量,必须是2的幂
private static final int DEFAULT_INITIAL_CAPACITY = 16;

// 最大容量,必须是2的幂且 <= 1<<30
private static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认负载因子
private static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 表示null键的特殊对象
private static final Object NULL_KEY = new Object();
```

### 4.2 实例字段

```java
// 哈希表数组,长度必须是2的幂
Entry<K,V>[] table;

// 键值映射数量
private int size;

// 扩容阈值 (capacity * loadFactor)
private int threshold;

// 负载因子
private final float loadFactor;

// 引用队列,用于跟踪被清除的WeakEntries
private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

// 结构修改次数,用于fail-fast机制
int modCount;
```

### 4.3 字段设计要点

1. **table数组**: 使用拉链法解决哈希冲突
2. **queue引用队列**: 关键机制,跟踪被GC回收的键
3. **NULL_KEY对象**: 允许null键,内部用特殊对象表示
4. **modCount**: 实现快速失败(fail-fast)机制

## 五、内部类Entry

### 5.1 Entry定义

```java
private static class Entry<K,V> extends WeakReference<Object>
    implements Map.Entry<K,V> {
    V value;           // 值,使用强引用
    final int hash;    // 键的哈希值
    Entry<K,V> next;   // 链表下一个节点

    Entry(Object key, V value,
          ReferenceQueue<Object> queue,
          int hash, Entry<K,V> next) {
        super(key, queue);  // 将键注册到引用队列
        this.value = value;
        this.hash = hash;
        this.next = next;
    }

    public K getKey() {
        return (K) WeakHashMap.unmaskNull(get());
    }

    public V getValue() {
        return value;
    }

    public V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        K k1 = getKey();
        Object k2 = e.getKey();
        if (k1 == k2 || (k1 != null && k1.equals(k2))) {
            V v1 = getValue();
            Object v2 = e.getValue();
            if (v1 == v2 || (v1 != null && v1.equals(v2)))
                return true;
        }
        return false;
    }

    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return Objects.hashCode(k) ^ Objects.hashCode(v);
    }

    public String toString() {
        return getKey() + "=" + getValue();
    }
}
```

### 5.2 Entry设计要点

1. **继承WeakReference**: 将键作为弱引用的referent
2. **强引用值**: 值使用强引用,可能导致内存泄漏
3. **链表结构**: next字段实现拉链法
4. **缓存哈希值**: hash字段避免重复计算

### 5.3 内存泄漏风险

```java
// 危险: 值强引用键
map.put(key, value);  // 如果value引用key,会导致key无法被回收

// 安全: 值也使用弱引用
map.put(key, new WeakReference<>(value));

// 获取时解包
Value value = ((WeakReference<Value>)map.get(key)).get();
```

## 六、核心方法解析

### 6.1 构造方法

```java
// 指定初始容量和负载因子
public WeakHashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;

    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal Load factor: "+
                                           loadFactor);

    // 计算大于等于initialCapacity的最小2的幂
    int capacity = 1;
    while (capacity < initialCapacity)
        capacity <<= 1;

    table = newTable(capacity);
    this.loadFactor = loadFactor;
    threshold = (int)(capacity * loadFactor);
}

// 指定初始容量,使用默认负载因子
public WeakHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}

// 默认构造方法
public WeakHashMap() {
    this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
}

// 从另一个Map构造
public WeakHashMap(Map<? extends K, ? extends V> m) {
    this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
            DEFAULT_INITIAL_CAPACITY),
         DEFAULT_LOAD_FACTOR);
    putAll(m);
}
```

### 6.2 辅助方法

```java
// null键处理: 用NULL_KEY代替null
private static Object maskNull(Object key) {
    return (key == null) ? NULL_KEY : key;
}

// null键恢复: 将NULL_KEY转回null
static Object unmaskNull(Object key) {
    return (key == NULL_KEY) ? null : key;
}

// 相等性比较
private static boolean eq(Object x, Object y) {
    return x == y || x.equals(y);
}

// 哈希函数: 扰动函数,减少哈希冲突
final int hash(Object k) {
    int h = k.hashCode();

    // 扰动函数: 确保哈希码在低位上有足够的差异
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}

// 计算数组索引
private static int indexFor(int h, int length) {
    return h & (length-1);  // length是2的幂,等价于h % length
}
```

### 6.3 expungeStaleEntries - 清理失效条目

```java
private void expungeStaleEntries() {
    // 遍历引用队列,取出所有被GC回收的Entry
    for (Object x; (x = queue.poll()) != null; ) {
        synchronized (queue) {
            @SuppressWarnings("unchecked")
                Entry<K,V> e = (Entry<K,V>) x;
            int i = indexFor(e.hash, table.length);

            Entry<K,V> prev = table[i];
            Entry<K,V> p = prev;
            while (p != null) {
                Entry<K,V> next = p.next;
                if (p == e) {
                    // 从链表中移除失效Entry
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;

                    // 帮助GC
                    e.value = null;
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }
}
```

**方法要点**:
- 在每次操作前自动调用
- 从ReferenceQueue中取出失效的WeakReference
- 从哈希表中移除对应的Entry
- 减少size计数

### 6.4 getTable - 获取清理后的表

```java
private Entry<K,V>[] getTable() {
    expungeStaleEntries();  // 先清理失效条目
    return table;
}
```

### 6.5 get - 获取值

```java
public V get(Object key) {
    Object k = maskNull(key);  // 处理null键
    int h = hash(k);            // 计算哈希
    Entry<K,V>[] tab = getTable();  // 获取清理后的表
    int index = indexFor(h, tab.length);  // 计算索引
    Entry<K,V> e = tab[index];

    // 遍历链表查找
    while (e != null) {
        if (e.hash == h && eq(k, e.get()))  // e.get()获取弱引用的键
            return e.value;
        e = e.next;
    }
    return null;
}
```

**方法要点**:
- 先调用getTable()清理失效条目
- 使用e.get()获取弱引用的键
- 如果键已被回收,e.get()返回null

### 6.6 put - 插入键值对

```java
public V put(K key, V value) {
    Object k = maskNull(key);
    int h = hash(k);
    Entry<K,V>[] tab = getTable();
    int i = indexFor(h, tab.length);

    // 查找是否已存在该键
    for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
        if (h == e.hash && eq(k, e.get())) {
            V oldValue = e.value;
            if (value != oldValue)
                e.value = value;
            return oldValue;  // 返回旧值
        }
    }

    // 创建新Entry
    modCount++;
    Entry<K,V> e = tab[i];
    tab[i] = new Entry<>(k, value, queue, h, e);  // 头插法

    // 检查是否需要扩容
    if (++size >= threshold)
        resize(tab.length * 2);
    return null;
}
```

**方法要点**:
- 使用头插法插入新Entry
- 新Entry自动注册到引用队列
- 达到阈值时自动扩容

### 6.7 resize - 扩容

```java
void resize(int newCapacity) {
    Entry<K,V>[] oldTable = getTable();
    int oldCapacity = oldTable.length;

    if (oldCapacity == MAXIMUM_CAPACITY) {
        threshold = Integer.MAX_VALUE;
        return;
    }

    Entry<K,V>[] newTable = newTable(newCapacity);
    transfer(oldTable, newTable);  // 迁移条目
    table = newTable;

    // 如果清理导致大量条目失效,可能需要回退
    if (size >= threshold / 2) {
        threshold = (int)(newCapacity * loadFactor);
    } else {
        expungeStaleEntries();
        transfer(newTable, oldTable);
        table = oldTable;
    }
}

private void transfer(Entry<K,V>[] src, Entry<K,V>[] dest) {
    for (int j = 0; j < src.length; ++j) {
        Entry<K,V> e = src[j];
        src[j] = null;
        while (e != null) {
            Entry<K,V> next = e.next;
            Object key = e.get();  // 获取弱引用的键
            if (key == null) {
                // 键已被回收,清理Entry
                e.next = null;
                e.value = null;
                size--;
            } else {
                // 重新计算索引并插入
                int i = indexFor(e.hash, dest.length);
                e.next = dest[i];
                dest[i] = e;
            }
            e = next;
        }
    }
}
```

**方法要点**:
- 扩容时重新计算所有Entry的索引
- 迁移过程中清理失效条目
- 可能因大量条目失效而回退扩容

### 6.8 remove - 移除条目

```java
public V remove(Object key) {
    Object k = maskNull(key);
    int h = hash(k);
    Entry<K,V>[] tab = getTable();
    int i = indexFor(h, tab.length);
    Entry<K,V> prev = tab[i];
    Entry<K,V> e = prev;

    while (e != null) {
        Entry<K,V> next = e.next;
        if (h == e.hash && eq(k, e.get())) {
            modCount++;
            size--;
            if (prev == e)
                tab[i] = next;
            else
                prev.next = next;
            return e.value;
        }
        prev = e;
        e = next;
    }

    return null;
}
```

### 6.9 clear - 清空Map

```java
public void clear() {
    // 清空引用队列
    while (queue.poll() != null)
        ;

    modCount++;
    Arrays.fill(table, null);  // 清空哈希表
    size = 0;

    // 可能因GC导致新的失效条目
    while (queue.poll() != null)
        ;
}
```

### 6.10 size - 获取大小

```java
public int size() {
    if (size == 0)
        return 0;
    expungeStaleEntries();  // 先清理失效条目
    return size;
}
```

## 七、迭代器实现

### 7.1 HashIterator - 抽象迭代器

```java
private abstract class HashIterator<T> implements Iterator<T> {
    private int index;
    private Entry<K,V> entry;
    private Entry<K,V> lastReturned;
    private int expectedModCount = modCount;

    // 强引用,避免在hasNext和next之间键被回收
    private Object nextKey;

    // 强引用,避免在nextEntry和使用Entry之间键被回收
    private Object currentKey;

    HashIterator() {
        index = isEmpty() ? 0 : table.length;
    }

    public boolean hasNext() {
        Entry<K,V>[] t = table;

        while (nextKey == null) {
            Entry<K,V> e = entry;
            int i = index;
            while (e == null && i > 0)
                e = t[--i];
            entry = e;
            index = i;
            if (e == null) {
                currentKey = null;
                return false;
            }
            nextKey = e.get();  // 持有强引用
            if (nextKey == null)
                entry = entry.next;
        }
        return true;
    }

    protected Entry<K,V> nextEntry() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        if (nextKey == null && !hasNext())
            throw new NoSuchElementException();

        lastReturned = entry;
        entry = entry.next;
        currentKey = nextKey;
        nextKey = null;
        return lastReturned;
    }

    public void remove() {
        if (lastReturned == null)
            throw new IllegalStateException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();

        WeakHashMap.this.remove(currentKey);
        expectedModCount = modCount;
        lastReturned = null;
        currentKey = null;
    }
}
```

**设计要点**:
- 使用强引用(nextKey, currentKey)避免键在迭代过程中被回收
- 实现fail-fast机制
- 跳过已失效的条目

### 7.2 具体迭代器

```java
// 值迭代器
private class ValueIterator extends HashIterator<V> {
    public V next() {
        return nextEntry().value;
    }
}

// 键迭代器
private class KeyIterator extends HashIterator<K> {
    public K next() {
        return nextEntry().getKey();
    }
}

// 条目迭代器
private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
    public Map.Entry<K,V> next() {
        return nextEntry();
    }
}
```

## 八、视图实现

### 8.1 KeySet - 键集合视图

```java
private class KeySet extends AbstractSet<K> {
    public Iterator<K> iterator() {
        return new KeyIterator();
    }

    public int size() {
        return WeakHashMap.this.size();
    }

    public boolean contains(Object o) {
        return containsKey(o);
    }

    public boolean remove(Object o) {
        if (containsKey(o)) {
            WeakHashMap.this.remove(o);
            return true;
        }
        else
            return false;
    }

    public void clear() {
        WeakHashMap.this.clear();
    }

    public Spliterator<K> spliterator() {
        return new KeySpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
    }
}
```

### 8.2 Values - 值集合视图

```java
private class Values extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return new ValueIterator();
    }

    public int size() {
        return WeakHashMap.this.size();
    }

    public boolean contains(Object o) {
        return containsValue(o);
    }

    public void clear() {
        WeakHashMap.this.clear();
    }

    public Spliterator<V> spliterator() {
        return new ValueSpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
    }
}
```

### 8.3 EntrySet - 条目集合视图

```java
private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator();
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        Entry<K,V> candidate = getEntry(e.getKey());
        return candidate != null && candidate.equals(e);
    }

    public boolean remove(Object o) {
        return removeMapping(o);
    }

    public int size() {
        return WeakHashMap.this.size();
    }

    public void clear() {
        WeakHashMap.this.clear();
    }

    // 深拷贝,避免返回的Entry被修改
    private List<Map.Entry<K,V>> deepCopy() {
        List<Map.Entry<K,V>> list = new ArrayList<>(size());
        for (Map.Entry<K,V> e : this)
            list.add(new AbstractMap.SimpleEntry<>(e));
        return list;
    }

    public Object[] toArray() {
        return deepCopy().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return deepCopy().toArray(a);
    }

    public Spliterator<Map.Entry<K,V>> spliterator() {
        return new EntrySpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
    }
}
```

## 九、设计模式

### 9.1 模板方法模式

AbstractMap定义了Map的基本骨架,WeakHashMap实现具体操作:
- AbstractMap: 提供通用实现
- WeakHashMap: 实现特定行为(弱引用)

### 9.2 迭代器模式

提供多种迭代器:
- KeyIterator: 键迭代
- ValueIterator: 值迭代
- EntryIterator: 条目迭代

### 9.3 视图模式

提供Map的不同视图:
- keySet(): 键集合视图
- values(): 值集合视图
- entrySet(): 条目集合视图

### 9.4 代理模式

Entry继承WeakReference,代理键的访问:
- Entry作为键的代理
- 通过WeakReference间接引用键

## 十、面试常见问题

### 10.1 WeakHashMap与HashMap的区别?

| 特性 | WeakHashMap | HashMap |
|------|-------------|---------|
| 键引用类型 | 弱引用 | 强引用 |
| 自动清理 | 是 | 否 |
| 内存管理 | 自动 | 手动 |
| 适用场景 | 缓存、监听器 | 通用映射 |
| 性能 | 稍低(需要清理) | 更高 |

### 10.2 WeakHashMap的键什么时候被回收?

当键不再被任何强引用引用时,垃圾回收器会回收该键,WeakHashMap会自动清理对应的条目。

### 10.3 WeakHashMap会内存泄漏吗?

可能!如果值强引用了键,会导致键无法被回收:
```java
// 危险
map.put(key, value);  // value引用key

// 安全
map.put(key, new WeakReference<>(value));
```

### 10.4 WeakHashMap是线程安全的吗?

不是。需要外部同步:
```java
Map<K,V> map = Collections.synchronizedMap(new WeakHashMap<>());
```

### 10.5 WeakHashMap的size()准确吗?

不一定准确!因为垃圾回收可能在任何时候发生,size()返回的是快照值。

### 10.6 WeakHashMap支持null键和null值吗?

支持。null键内部用NULL_KEY对象表示,null值直接存储。

### 10.7 WeakHashMap的迭代器是fail-fast的吗?

是的。如果在迭代过程中Map被结构性修改,会抛出ConcurrentModificationException。

### 10.8 WeakHashMap的扩容机制?

与HashMap类似:
- 当size >= threshold时扩容
- 新容量为旧容量的2倍
- 扩容时重新计算所有Entry的索引

## 十一、使用场景

### 11.1 缓存实现

```java
// 临时缓存,自动清理
WeakHashMap<String, Object> cache = new WeakHashMap<>();

public Object getData(String key) {
    return cache.computeIfAbsent(key, k -> loadFromDB(k));
}
```

### 11.2 监听器注册表

```java
// 避免监听器内存泄漏
WeakHashMap<Object, List<EventListener>> listeners = new WeakHashMap<>();

public void addListener(Object source, EventListener listener) {
    listeners.computeIfAbsent(source, k -> new ArrayList<>()).add(listener);
}
```

### 11.3 元数据映射

```java
// 与对象生命周期绑定的元数据
WeakHashMap<Object, Map<String, Object>> metadata = new WeakHashMap<>();

public void setMetadata(Object obj, String key, Object value) {
    metadata.computeIfAbsent(obj, k -> new HashMap<>()).put(key, value);
}
```

## 十二、注意事项

### 12.1 值强引用键的问题

```java
// 错误示例
WeakHashMap<Key, Value> map = new WeakHashMap<>();
class Key { Value value; }
class Value { Key key; }

Key key = new Key();
Value value = new Value();
key.value = value;
value.key = key;  // 循环引用,导致key无法被回收

map.put(key, value);
```

### 12.2 GC的不确定性

```java
WeakHashMap<Object, Object> map = new WeakHashMap<>();
Object key = new Object();
map.put(key, "value");

key = null;  // 移除强引用

System.gc();  // 建议GC,但不保证立即执行

// 条目可能仍然存在
System.out.println(map.size());  // 可能输出1
```

### 12.3 迭代时的强引用

```java
// 迭代器持有强引用,防止键被回收
for (Map.Entry<K,V> entry : map.entrySet()) {
    // 在迭代期间,entry的键不会被回收
}
```

### 12.4 性能考虑

- 每次操作前需要清理失效条目
- 频繁GC会导致频繁清理
- 不适合高性能场景

## 十三、与其他Map的对比

### 13.1 HashMap vs WeakHashMap

```java
// HashMap: 键不会被自动回收
HashMap<Object, Object> hashMap = new HashMap<>();
Object key = new Object();
hashMap.put(key, "value");
key = null;
System.gc();
System.out.println(hashMap.size());  // 输出1

// WeakHashMap: 键会被自动回收
WeakHashMap<Object, Object> weakMap = new WeakHashMap<>();
key = new Object();
weakMap.put(key, "value");
key = null;
System.gc();
System.out.println(weakMap.size());  // 输出0
```

### 13.2 LinkedHashMap vs WeakHashMap

| 特性 | LinkedHashMap | WeakHashMap |
|------|---------------|-------------|
| 键引用 | 强引用 | 弱引用 |
| 顺序 | 插入顺序/访问顺序 | 无序 |
| 自动清理 | 否 | 是 |
| LRU缓存 | 支持 | 不支持 |

### 13.3 ConcurrentHashMap vs WeakHashMap

| 特性 | ConcurrentHashMap | WeakHashMap |
|------|-------------------|-------------|
| 线程安全 | 是 | 否 |
| 键引用 | 强引用 | 弱引用 |
| 并发性能 | 高 | 低 |
| 适用场景 | 高并发缓存 | 单线程缓存 |

## 十四、最佳实践

### 14.1 选择合适的Map

```java
// 通用映射: HashMap
Map<String, Object> map = new HashMap<>();

// 需要保持顺序: LinkedHashMap
Map<String, Object> orderedMap = new LinkedHashMap<>();

// 需要自动清理: WeakHashMap
Map<Object, Object> cache = new WeakHashMap<>();

// 线程安全: ConcurrentHashMap
Map<String, Object> concurrentMap = new ConcurrentHashMap<>();

// 需要排序: TreeMap
Map<String, Object> sortedMap = new TreeMap<>();
```

### 14.2 避免内存泄漏

```java
// 值也使用弱引用
WeakHashMap<Key, WeakReference<Value>> map = new WeakHashMap<>();

map.put(key, new WeakReference<>(value));

// 获取时解包
Value value = map.get(key).get();
if (value == null) {
    // 值已被回收
    map.remove(key);
}
```

### 14.3 正确处理null

```java
WeakHashMap<String, Object> map = new WeakHashMap<>();

// null键和null值都支持
map.put(null, null);
map.put("key", null);
map.put(null, "value");

// 检查null键
boolean hasNullKey = map.containsKey(null);
```

### 14.4 外部同步

```java
// 创建同步的WeakHashMap
Map<K,V> synchronizedMap = Collections.synchronizedMap(new WeakHashMap<>());

// 使用时同步
synchronized (synchronizedMap) {
    for (Map.Entry<K,V> entry : synchronizedMap.entrySet()) {
        // 处理条目
    }
}
```

## 十五、总结

WeakHashMap是一个特殊的Map实现,通过弱引用实现键的自动回收,非常适合缓存和监听器等场景。理解其工作原理和注意事项对于正确使用至关重要。

### 核心要点

1. **弱引用机制**: 键通过WeakReference存储,不阻止GC
2. **引用队列**: 使用ReferenceQueue跟踪被回收的键
3. **自动清理**: expungeStaleEntries()清理失效条目
4. **内存泄漏风险**: 值强引用键会导致内存泄漏
5. **GC不确定性**: 条目可能在任何时候被回收
6. **线程不安全**: 需要外部同步
7. **性能考虑**: 频繁GC会影响性能

### 适用场景

- 临时缓存
- 监听器注册表
- 元数据映射
- 避免内存泄漏的场景

### 不适用场景

- 高性能要求
- 需要精确控制内存
- 多线程环境(需外部同步)
- 需要保持顺序
