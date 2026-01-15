# HashMap 源码解读

## 一、类概述

HashMap是基于哈希表的Map接口实现。这个实现提供了所有可选的map操作，并且允许null值和null键。HashMap类大致等同于Hashtable，除了它是不同步的并且允许null。这个类不保证map的顺序；特别地，它不保证顺序会随时间保持恒定。

### 1.1 核心特性

- **哈希表实现**: 基于哈希表存储键值对
- **允许null**: 允许null键和null值
- **非同步**: 不同步实现，需要外部同步
- **无序**: 不保证迭代顺序
- **高性能**: 基本操作（get和put）的常量时间性能
- **动态扩容**: 自动调整容量
- **红黑树优化**: JDK 8引入，当链表过长时转换为红黑树

### 1.2 适用场景

- 需要快速查找的键值映射
- 不需要保持顺序
- 需要高性能的插入、删除、查找
- 允许null键和null值

## 二、设计原理

### 2.1 哈希表+链表/红黑树

HashMap使用数组+链表/红黑树的结构：

```java
// 基本结构
table[index] -> Node -> Node -> Node -> null
              -> Node -> Node -> Node -> null

// 红黑树结构（当链表长度 >= 8时）
table[index] -> TreeNode -> TreeNode -> TreeNode -> null
```

### 2.2 扰动函数

HashMap使用扰动函数减少哈希冲突：

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**设计要点**:
- 高16位与低16位异或
- 减少哈希冲突
- 均匀分布哈希值

### 2.3 索引计算

```java
// 计算数组索引
int index = (n - 1) & hash;
```

**设计要点**:
- 使用位运算代替模运算
- n必须是2的幂
- 效率更高

### 2.4 扩容机制

```java
// 扩容条件
if (size >= threshold)
    resize();

// 扩容后容量翻倍
int newCapacity = oldCapacity << 1;
```

**设计要点**:
- 容量翻倍增长
- 重新计算所有元素的索引
- 阈值保持不变

### 2.5 红黑树转换

JDK 8引入红黑树优化：

```java
// 链表转红黑树条件
if (binCount >= TREEIFY_THRESHOLD)  // 8
    treeifyBin(tab, i);

// 红黑树转链表条件
if (tab.length < MIN_TREEIFY_CAPACITY)  // 64
    untreeify(tab);
```

**设计要点**:
- 链表长度 >= 8时转换为红黑树
- 数组长度 < 64时转换回链表
- 提高最坏情况下的性能

## 三、继承结构

```
AbstractMap<K,V>
    ↑
HashMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
```

### 3.1 接口实现

- **Map**: Map接口定义的契约方法
- **Cloneable**: 支持克隆
- **Serializable**: 支持序列化

## 四、核心字段

### 4.1 哈希表数组

```java
transient Node<K,V>[] table;
```

**字段设计要点**:
- 存储哈希表桶
- transient修饰，自定义序列化
- 长度总是2的幂

### 4.2 大小

```java
transient int size;
```

**字段设计要点**:
- 存储键值对数量
- transient修饰，自定义序列化

### 4.3 扩容阈值

```java
int threshold;
```

**字段设计要点**:
- 扩容阈值 = capacity * loadFactor
- 当size >= threshold时扩容

### 4.4 负载因子

```java
final float loadFactor;
```

**字段设计要点**:
- 负载因子
- 默认值0.75
- 影响扩容时机

### 4.5 修改次数

```java
transient int modCount;
```

**字段设计要点**:
- 结构修改次数
- 用于fail-fast机制
- transient修饰，自定义序列化

## 五、常量字段

### 5.1 默认初始容量

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
```

**设计要点**:
- 默认初始容量为16
- 必须是2的幂

### 5.2 最大容量

```java
static final int MAXIMUM_CAPACITY = 1 << 30;
```

**设计要点**:
- 最大容量为2^30
- 约为1073741824

### 5.3 默认负载因子

```java
static final float DEFAULT_LOAD_FACTOR = 0.75f;
```

**设计要点**:
- 默认负载因子为0.75
- 平衡时间和空间成本

### 5.4 树化阈值

```java
static final int TREEIFY_THRESHOLD = 8;
```

**设计要点**:
- 链表长度 >= 8时转换为红黑树
- 提高最坏情况性能

### 5.5 去树化阈值

```java
static final int UNTREEIFY_THRESHOLD = 6;
```

**设计要点**:
- 红黑树节点 <= 6时转换为链表
- 避免红黑树的开销

### 5.6 最小树化容量

```java
static final int MIN_TREEIFY_CAPACITY = 64;
```

**设计要点**:
- 数组长度 >= 64时才进行树化
- 避免频繁转换

## 六、Node内部类

### 6.1 Node定义

```java
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

    public final K getKey()        { return key; }
    public final V getValue()      { return value; }
    public final String toString() { return key + "=" + value; }
    public final int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }
    public final V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }
}
```

**设计要点**:
- 基本的链表节点
- 实现Map.Entry接口
- 包含hash、key、value、next

## 七、构造方法

### 7.1 指定初始容量和负载因子

```java
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
```

**方法要点**:
- 创建空的HashMap
- 指定初始容量和负载因子
- 计算扩容阈值

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>(16, 0.75f);
```

### 7.2 指定初始容量

```java
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
```

**方法要点**:
- 创建空的HashMap
- 指定初始容量
- 使用默认负载因子0.75

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>(16);
```

### 7.3 默认构造方法

```java
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
}
```

**方法要点**:
- 创建空的HashMap
- 使用默认初始容量16
- 使用默认负载因子0.75

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>();
```

### 7.4 从Map构造

```java
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}
```

**方法要点**:
- 从指定Map创建HashMap
- 复制所有映射
- 使用默认负载因子0.75

**示例**:
```java
Map<String, Integer> original = new HashMap<>();
original.put("a", 1);

HashMap<String, Integer> map = new HashMap<>(original);
```

## 八、核心方法

### 8.1 put - 添加键值对

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

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
                        treeifyBin(tab, i);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) {
            if (onlyIfAbsent)
                return null;
            if (evict)
                afterNodeInsertion(false, e);
            return null;
        }
        else {
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
    afterNodeInsertion(evict, e);
    return null;
}
```

**方法要点**:
- 计算哈希值
- 查找或创建节点
- 处理链表和红黑树
- 检查是否需要扩容
- 时间复杂度: O(1)平均

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.put("b", 2);
```

### 8.2 get - 获取值

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}
```

**方法要点**:
- 计算哈希值
- 委托给getNode方法
- 时间复杂度: O(1)平均

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", 1);
Integer value = map.get("a");  // 1
```

### 8.3 getNode - 获取节点

```java
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        if (first.hash == hash && // always check first node
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

**方法要点**:
- 遍历链表或红黑树
- 比较hash和key
- 时间复杂度: O(1)平均

### 8.4 remove - 移除键值对

```java
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false)) == null ?
        null : e.value;
}
```

**方法要点**:
- 计算哈希值
- 委托给removeNode方法
- 时间复杂度: O(1)平均

**示例**:
```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", 1);
Integer value = map.remove("a");  // 1
```

### 8.5 containsKey - 检查包含键

```java
public boolean containsKey(Object key) {
    return getNode(hash(key), key) != null;
}
```

**方法要点**:
- 委托给getNode方法
- 时间复杂度: O(1)平均

### 8.6 size - 获取大小

```java
public int size() {
    return size;
}
```

**时间复杂度**: O(1)

### 8.7 isEmpty - 检查是否为空

```java
public boolean isEmpty() {
    return size == 0;
}
```

**时间复杂度**: O(1)

### 8.8 clear - 清空Map

```java
public void clear() {
    Node<K,V>[] tab;
    modCount++;
    if ((tab = table) != null && size > 0) {
        size = 0;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
    }
}
```

**时间复杂度**: O(n)

## 九、扩容方法

### 9.1 resize - 扩容

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        if (oldThr > 0)
            newCap = oldThr;
        else {
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
    }
    else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
             oldCap >= DEFAULT_INITIAL_CAPACITY)
        newThr = oldThr << 1;
    else
        newCap = MAXIMUM_CAPACITY;
        newThr = Integer.MAX_VALUE;
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
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
                        }
                        else {
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
                    else {
                        if (hiTail != null)
                            hiHead.next = null;
                        else
                            hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    ++modCount;
    if (oldThr > 0)
        size = oldThr;
    return newTab;
}
```

**方法要点**:
- 容量翻倍增长
- 重新分配所有元素
- 使用位运算判断新位置
- 时间复杂度: O(n)

## 十、红黑树优化

### 10.1 treeifyBin - 链表转红黑树

```java
final void treeifyBin(Node<K,V>[] tab, int index) {
    TreeNode<K,V> root = null;
    for (Node<K,V> e = tab[index], next; e != null; e = next) {
        TreeNode<K,V> p = replacementTreeNode(e, null);
        if (root == null)
            root = p;
        else {
            TreeNode<K,V> x = root;
            for (;;) {
                int dir = tieBreakOrder(p.hash, x.hash);
                if (dir < 0) {
                    if (x.left == null) {
                        x.left = p;
                        break;
                    }
                    x = x.left;
                } else {
                    if (x.right == null) {
                        x.right = p;
                        break;
                    }
                    x = x.right;
                }
            }
            p.parent = x;
        }
    }
    moveRootToFront(tab, root);
}
```

**方法要点**:
- 链表长度 >= 8时调用
- 将链表转换为红黑树
- 提高最坏情况性能

### 10.2 untreeify - 红黑树转链表

```java
final void untreeify(HashMap<K,V> map) {
    Node<K,V>[] tab = map.table;
    TreeNode<K,V> root = (TreeNode<K,V>)tab[0];
    for (TreeNode<K,V> e = root, next; e != null; e = next) {
        tab[indexFor(e.hash, tab.length)] = e.next = new Node<>(e.hash, e.key, e.value, null);
    }
}
```

**方法要点**:
- 红黑树节点 <= 6时调用
- 将红黑树转换为链表
- 避免红黑树的开销

## 十一、设计模式

### 11.1 哈希表模式

HashMap使用哈希表模式:

```java
// 数组+链表/红黑树
table[index] -> Node -> Node -> Node -> null
```

### 11.2 模板方法模式

HashMap提供钩子方法给子类：

```java
// LinkedHashMap可以重写这些方法
void afterNodeAccess(Node<K,V> p) {}
void afterNodeInsertion(boolean evict, Node<K,V> p) {}
void afterNodeRemoval(Node<K,V> p) {}
```

## 十二、面试常见问题

### 12.1 HashMap的工作原理?

1. **计算哈希值**: 使用扰动函数
2. **计算索引**: 使用位运算
3. **处理冲突**: 使用链表或红黑树
4. **自动扩容**: 当size >= threshold时扩容

### 12.2 HashMap的扩容机制?

- **扩容条件**: size >= threshold
- **扩容方式**: 容量翻倍
- **重新分配**: 所有元素重新计算索引
- **阈值计算**: threshold = capacity * loadFactor

### 12.3 HashMap的负载因子?

- **默认值**: 0.75
- **作用**: 控制扩容时机
- **影响**: 
  - 较小: 空间利用率高，但冲突多
  - 较大: 空间利用率低，但冲突少

### 12.4 HashMap的扰动函数?

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**作用**: 减少哈希冲突，使哈希值分布更均匀

### 12.5 HashMap的线程安全吗?

不是。需要外部同步:

```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
```

### 12.6 HashMap的迭代器是fail-fast的吗?

是的。如果在迭代过程中Map被结构性修改，会抛出ConcurrentModificationException:

```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.put("b", 2);

Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
map.put("c", 3);  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 12.7 HashMap的get和put时间复杂度?

- **平均情况**: O(1)
- **最坏情况**: O(n)链表，O(log n)红黑树

### 12.8 HashMap支持null键和null值吗?

支持:

```java
HashMap<String, Integer> map = new HashMap<>();
map.put(null, 0);  // OK
map.put("a", null);  // OK
```

### 12.9 HashMap的初始容量?

- **默认值**: 16
- **要求**: 必须是2的幂
- **影响**: 影响第一次扩容时机

### 12.10 JDK 8中HashMap的优化?

JDK 8引入了红黑树优化：

- **链表转红黑树**: 当链表长度 >= 8时
- **红黑树转链表**: 当红黑树节点 <= 6时
- **最坏情况**: 从O(n)优化到O(log n)

## 十三、使用场景

### 13.1 缓存实现

```java
HashMap<String, Object> cache = new HashMap<>();

public Object get(String key) {
    return cache.computeIfAbsent(key, k -> loadFromDB(k));
}
```

### 13.2 计数器

```java
HashMap<String, Integer> counter = new HashMap<>();

public void increment(String key) {
    counter.put(key, counter.getOrDefault(key, 0) + 1);
}
```

### 13.3 配置管理

```java
HashMap<String, String> config = new HashMap<>();
config.put("db.url", "jdbc:mysql://localhost:3306/mydb");
config.put("cache.size", "1024");
```

### 13.4 数据索引

```java
HashMap<Integer, List<User>> index = new HashMap<>();

public void addUser(User user) {
    int hash = user.getId() % 100;
    index.computeIfAbsent(hash, k -> new ArrayList<>()).add(user);
}
```

### 13.5 会话管理

```java
HashMap<String, Session> sessions = new HashMap<>();

public Session createSession(String userId) {
    Session session = new Session();
    sessions.put(userId, session);
    return session;
}
```

## 十四、注意事项

### 14.1 null键和null值

```java
HashMap<String, Integer> map = new HashMap<>();
map.put(null, 0);  // OK
map.put("a", null);  // OK
```

### 14.2 线程安全

```java
HashMap<String, Integer> map = new HashMap<>();

// 需要外部同步
Map<String, Integer> syncMap = Collections.synchronizedMap(map);
```

### 14.3 迭代器一致性

```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.put("b", 2);

Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
map.put("c", 3);  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 14.4 初始容量设置

```java
// 推荐: 根据预期大小设置初始容量
int expectedSize = 1000;
HashMap<String, Integer> map = new HashMap<>(expectedSize * 2);

// 不推荐: 使用默认容量
HashMap<String, Integer> map = new HashMap<>();
```

### 14.5 负载因子设置

```java
// 推荐: 使用默认负载因子0.75
HashMap<String, Integer> map = new HashMap<>(16, 0.75f);

// 特殊情况: 调整负载因子
HashMap<String, Integer> map = new HashMap<>(16, 0.5f);  // 更频繁扩容
```

## 十五、最佳实践

### 15.1 合理设置初始容量

```java
// 推荐: 根据预期大小设置初始容量
int expectedSize = 1000;
HashMap<String, Integer> map = new HashMap<>(expectedSize * 2);
```

### 15.2 使用正确的hashCode和equals

```java
class User {
    private int id;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

### 15.3 避免在迭代时修改

```java
HashMap<String, Integer> map = new HashMap<>();

// 不推荐: 迭代时修改
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    if (entry.getValue() > 100) {
        map.remove(entry.getKey());  // ConcurrentModificationException
    }
}

// 推荐: 使用迭代器的remove方法
Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
while (it.hasNext()) {
    Map.Entry<String, Integer> entry = it.next();
    if (entry.getValue() > 100) {
        it.remove();  // OK
    }
}
```

### 15.4 使用computeIfAbsent

```java
HashMap<String, Integer> map = new HashMap<>();

// 推荐: 使用computeIfAbsent
map.computeIfAbsent("key", k -> expensiveOperation(k));

// 不推荐: 手动检查
if (!map.containsKey("key")) {
    map.put("key", expensiveOperation("key"));
}
```

### 15.5 使用merge

```java
HashMap<String, Integer> map = new HashMap<>();

// 推荐: 使用merge
map.merge("key", 1, Integer::sum);

// 不推荐: 手动处理
map.put("key", map.getOrDefault("key", 0) + 1);
```

## 十六、性能优化

### 16.1 设置合理的初始容量

```java
// 根据预期大小设置初始容量，避免频繁扩容
int expectedSize = 1000;
HashMap<String, Integer> map = new HashMap<>(expectedSize * 2);
```

### 16.2 使用正确的hashCode

```java
// 好的hashCode: 均匀分布
@Override
public int hashCode() {
    return Objects.hash(field1, field2, field3);
}

// 坏的hashCode: 所有对象返回相同值
@Override
public int hashCode() {
    return 1;
}
```

### 16.3 使用computeIfAbsent

```java
// 推荐: 使用computeIfAbsent，避免重复计算
map.computeIfAbsent("key", k -> expensiveOperation(k));
```

### 16.4 使用merge

```java
// 推荐: 使用merge，简化代码
map.merge("key", 1, Integer::sum);
```

## 十七、总结

HashMap是基于哈希表的Map接口实现，提供快速的键值对存储和查找。

### 核心要点

1. **哈希表实现**: 使用数组+链表/红黑树
2. **允许null**: 允许null键和null值
3. **非同步**: 不同步实现，需要外部同步
4. **无序**: 不保证迭代顺序
5. **高性能**: 基本操作O(1)平均
6. **动态扩容**: 自动调整容量
7. **红黑树优化**: JDK 8引入，提高最坏情况性能

### 适用场景

- 需要快速查找的键值映射
- 不需要保持顺序
- 需要高性能的插入、删除、查找
- 允许null键和null值

### 不适用场景

- 需要保持插入顺序(使用LinkedHashMap)
- 需要排序顺序(使用TreeMap)
- 多线程环境(使用ConcurrentHashMap)
- 需要弱引用(使用WeakHashMap)

### 性能特点

- 基本操作: O(1)平均
- 最坏情况: O(n)链表，O(log n)红黑树
- 扩容: O(n)
- 空间复杂度: O(n)

### 与其他Map的选择

- 通用映射: HashMap
- 保持插入顺序: LinkedHashMap
- 需要排序: TreeMap
- 线程安全: ConcurrentHashMap
- 枚举键: EnumMap
- 弱引用键: WeakHashMap
