# LinkedHashSet 源码解读

## 一、类概述

LinkedHashSet是Set接口的哈希表和链表实现，具有可预测的迭代顺序。这个实现与HashSet不同，它维护了一个贯穿所有条目的双向链表。这个链表定义了迭代顺序，即元素被插入到集合中的顺序（插入顺序）。注意，如果一个元素被重新插入到集合中，插入顺序不受影响。

### 1.1 核心特性

- **哈希表+链表**: 基于HashMap实现，维护插入顺序
- **插入顺序**: 按元素插入的顺序迭代
- **可预测顺序**: 迭代顺序是可预测的
- **支持null**: 允许null元素
- **非同步**: 非线程安全，需要外部同步
- **Fail-Fast**: 迭代器实现快速失败机制
- **性能特点**: 基本操作时间复杂度为O(1)，迭代时间复杂度为O(n)

### 1.2 适用场景

- 需要保持插入顺序的集合
- 需要快速查找的有序集合
- 需要LRU缓存实现
- 需要按访问顺序迭代的集合

## 二、设计原理

### 2.1 基于HashMap的实现

LinkedHashSet继承自HashSet，底层使用LinkedHashMap:

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {
    
    // 构造方法中传递true给HashSet
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);  // true表示使用LinkedHashMap
    }
}
```

**设计要点**:
- 继承HashSet的所有功能
- 通过构造方法参数控制使用LinkedHashMap
- 复用HashMap的实现

### 2.2 插入顺序维护

LinkedHashSet通过LinkedHashMap维护插入顺序:

```java
// LinkedHashMap维护双向链表
// 新元素添加到链表尾部
// 迭代时按链表顺序遍历
```

**设计要点**:
- 使用双向链表维护顺序
- 新元素添加到尾部
- 重新插入不影响顺序

### 2.3 与HashSet的关系

```java
// HashSet内部构造方法
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

**设计要点**:
- LinkedHashSet调用HashSet的特殊构造方法
- HashSet根据dummy参数选择HashMap或LinkedHashMap
- dummy=true时使用LinkedHashMap

## 三、继承结构

```
HashSet<E>
    ↑
LinkedHashSet<E>
    implements Set<E>, Cloneable, Serializable
```

### 3.1 继承的方法

- HashSet提供的基础Set操作
- Set接口定义的契约方法
- Cloneable支持克隆
- Serializable支持序列化

## 四、核心字段

LinkedHashSet本身没有额外的核心字段，所有字段都继承自HashSet：

```java
// 继承自HashSet的字段
private transient HashMap<E,Object> map;  // 实际使用LinkedHashMap
private static final Object PRESENT = new Object();  // 值对象
```

## 五、构造方法

### 5.1 指定初始容量和负载因子

```java
public LinkedHashSet(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor, true);
}
```

**方法要点**:
- 创建空的LinkedHashSet
- 指定初始容量和负载因子
- 传递true给HashSet，使用LinkedHashMap

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>(16, 0.75f);
```

### 5.2 指定初始容量

```java
public LinkedHashSet(int initialCapacity) {
    super(initialCapacity, .75f, true);
}
```

**方法要点**:
- 创建空的LinkedHashSet
- 指定初始容量
- 使用默认负载因子0.75

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>(16);
```

### 5.3 默认构造方法

```java
public LinkedHashSet() {
    super(16, .75f, true);
}
```

**方法要点**:
- 创建空的LinkedHashSet
- 使用默认初始容量16
- 使用默认负载因子0.75

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>();
```

### 5.4 从Collection构造

```java
public LinkedHashSet(Collection<? extends E> c) {
    super(Math.max(2*c.size(), 11), .75f, true);
    addAll(c);
}
```

**方法要点**:
- 从指定集合创建LinkedHashSet
- 初始容量为max(2*c.size(), 11)
- 保持插入顺序

**示例**:
```java
List<String> list = Arrays.asList("a", "b", "c");
LinkedHashSet<String> set = new LinkedHashSet<>(list);
```

## 六、核心方法

LinkedHashSet的核心方法都继承自HashSet，底层委托给LinkedHashMap：

### 6.1 add - 添加元素

```java
// 继承自HashSet
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```

**方法要点**:
- 委托给LinkedHashMap的put方法
- 使用PRESENT作为值
- 返回true表示添加成功
- 时间复杂度: O(1)

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");
set.add("a");  // 返回false，不影响顺序
```

### 6.2 remove - 移除元素

```java
// 继承自HashSet
public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
```

**方法要点**:
- 委托给LinkedHashMap的remove方法
- 返回true表示移除成功
- 时间复杂度: O(1)

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.remove("a");  // 返回true
```

### 6.3 contains - 检查包含

```java
// 继承自HashSet
public boolean contains(Object o) {
    return map.containsKey(o);
}
```

**方法要点**:
- 委托给LinkedHashMap的containsKey方法
- 时间复杂度: O(1)

**示例**:
```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
boolean contains = set.contains("a");  // true
```

### 6.4 size - 获取大小

```java
// 继承自HashSet
public int size() {
    return map.size();
}
```

**时间复杂度**: O(1)

### 6.5 isEmpty - 检查是否为空

```java
// 继承自HashSet
public boolean isEmpty() {
    return map.isEmpty();
}
```

**时间复杂度**: O(1)

### 6.6 clear - 清空集合

```java
// 继承自HashSet
public void clear() {
    map.clear();
}
```

**时间复杂度**: O(n)

## 七、迭代器实现

### 7.1 迭代器特性

```java
// LinkedHashSet的迭代器特点：
// 1. 按插入顺序迭代
// 2. Fail-Fast: 会抛出ConcurrentModificationException
// 3. 支持remove操作
```

### 7.2 迭代顺序

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("c");
set.add("a");
set.add("b");

// 迭代顺序: c, a, b (按插入顺序)
for (String s : set) {
    System.out.println(s);
}
```

## 八、Spliterator实现

### 8.1 spliterator方法

```java
@Override
public Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
}
```

**方法要点**:
- 返回DISTINCT和ORDERED特性的Spliterator
- DISTINCT: 元素唯一
- ORDERED: 有序

## 九、设计模式

### 9.1 继承模式

LinkedHashSet继承HashSet:

```java
public class LinkedHashSet<E> extends HashSet<E> {
    // 复用HashSet的实现
}
```

**优点**:
- 代码复用
- 维护简单
- 行为一致

### 9.2 委托模式

LinkedHashSet委托给LinkedHashMap:

```java
// HashSet内部
private transient HashMap<E,Object> map;

public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```

## 十、面试常见问题

### 10.1 LinkedHashSet与HashSet的区别?

| 特性 | LinkedHashSet | HashSet |
|------|---------------|---------|
| 迭代顺序 | 插入顺序 | 无序 |
| 底层实现 | LinkedHashMap | HashMap |
| 性能 | 稍低(维护链表) | 更高 |
| 内存开销 | 较高(维护链表) | 较低 |
| LRU缓存 | 支持 | 不支持 |

### 10.2 LinkedHashSet与TreeSet的区别?

| 特性 | LinkedHashSet | TreeSet |
|------|---------------|---------|
| 迭代顺序 | 插入顺序 | 排序顺序 |
| 底层实现 | LinkedHashMap | TreeMap(红黑树) |
| 时间复杂度 | O(1) | O(log n) |
| null元素 | 允许 | 不允许(自然排序) |
| 排序 | 不支持 | 支持 |

### 10.3 LinkedHashSet如何维护插入顺序?

通过LinkedHashMap的双向链表:

```java
// LinkedHashMap维护双向链表
// 新元素添加到链表尾部
// 迭代时按链表顺序遍历
```

### 10.4 LinkedHashSet支持null元素吗?

支持:

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add(null);  // OK
```

### 10.5 LinkedHashSet是线程安全的吗?

不是。需要外部同步:

```java
Set<String> syncSet = Collections.synchronizedSet(new LinkedHashSet<>());
```

### 10.6 LinkedHashSet的迭代器是fail-fast的吗?

是的。如果在迭代过程中集合被结构性修改，会抛出ConcurrentModificationException:

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");

Iterator<String> it = set.iterator();
set.add("c");  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 10.7 LinkedHashSet的基本操作时间复杂度?

- add: O(1)
- remove: O(1)
- contains: O(1)
- size: O(1)
- isEmpty: O(1)
- clear: O(n)

### 10.8 LinkedHashSet的迭代时间复杂度?

- 迭代时间复杂度: O(n)
- 与容量无关，只与元素数量有关

## 十一、使用场景

### 11.1 需要保持插入顺序

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("c");
set.add("a");
set.add("b");

// 迭代顺序: c, a, b (按插入顺序)
for (String s : set) {
    System.out.println(s);
}
```

### 11.2 实现LRU缓存

```java
LinkedHashSet<String> cache = new LinkedHashSet<>();

public void access(String item) {
    cache.remove(item);  // 移除
    cache.add(item);     // 添加到尾部
}

public void evict() {
    if (cache.size() > MAX_SIZE) {
        String oldest = cache.iterator().next();  // 获取最旧的元素
        cache.remove(oldest);
    }
}
```

### 11.3 去重并保持顺序

```java
List<String> list = Arrays.asList("a", "b", "c", "a", "b");

// 去重并保持顺序
LinkedHashSet<String> set = new LinkedHashSet<>(list);
List<String> uniqueList = new ArrayList<>(set);
// [a, b, c]
```

### 11.4 按访问顺序迭代

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");
set.add("c");

// 访问元素
set.contains("a");  // 不影响顺序

// 重新插入会影响顺序
set.remove("b");
set.add("b");  // b移动到尾部
```

### 11.5 复制集合并保持顺序

```java
public <T> Set<T> copySet(Set<T> original) {
    return new LinkedHashSet<>(original);
}
```

## 十二、注意事项

### 12.1 重新插入不影响顺序

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");
set.add("c");

set.remove("b");
set.add("b");  // b移动到尾部，不影响其他元素顺序
```

### 12.2 null元素

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add(null);  // OK
```

### 12.3 线程安全

```java
LinkedHashSet<String> set = new LinkedHashSet<>();

// 需要外部同步
Set<String> syncSet = Collections.synchronizedSet(set);
```

### 12.4 迭代器一致性

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");

Iterator<String> it = set.iterator();
set.add("c");  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 12.5 性能考虑

- 基本操作比HashSet稍慢(维护链表)
- 迭代比HashSet快(与容量无关)
- 内存开销比HashSet高(维护链表)

## 十三、最佳实践

### 13.1 使用LinkedHashSet保持顺序

```java
// 推荐: 使用LinkedHashSet保持顺序
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("a");
set.add("b");

// 不推荐: 使用HashSet(无序)
HashSet<String> set = new HashSet<>();
set.add("a");
set.add("b");
```

### 13.2 使用LinkedHashSet去重

```java
List<String> list = Arrays.asList("a", "b", "c", "a", "b");

// 推荐: 使用LinkedHashSet去重并保持顺序
LinkedHashSet<String> set = new LinkedHashSet<>(list);
List<String> uniqueList = new ArrayList<>(set);
```

### 13.3 使用LinkedHashSet实现LRU

```java
LinkedHashSet<String> cache = new LinkedHashSet<>();

public void access(String item) {
    cache.remove(item);
    cache.add(item);
}
```

### 13.4 合理设置初始容量

```java
// 推荐: 根据预期大小设置初始容量
int expectedSize = 1000;
LinkedHashSet<String> set = new LinkedHashSet<>(expectedSize * 2);

// 不推荐: 使用默认容量
LinkedHashSet<String> set = new LinkedHashSet<>();
```

### 13.5 使用LinkedHashSet复制集合

```java
public <T> Set<T> copySet(Set<T> original) {
    // 推荐: 使用LinkedHashSet保持顺序
    return new LinkedHashSet<>(original);
}
```

## 十四、性能优化

### 14.1 合理设置初始容量

```java
// 根据预期大小设置初始容量，避免频繁扩容
int expectedSize = 1000;
LinkedHashSet<String> set = new LinkedHashSet<>(expectedSize * 2);
```

### 14.2 使用LinkedHashSet迭代

```java
// LinkedHashSet迭代时间复杂度为O(n)，与容量无关
LinkedHashSet<String> set = new LinkedHashSet<>();
for (String s : set) {
    // 处理元素
}
```

### 14.3 避免频繁的remove和add

```java
// 不推荐: 频繁的remove和add会影响性能
set.remove(item);
set.add(item);

// 推荐: 如果元素已存在，不操作
if (!set.contains(item)) {
    set.add(item);
}
```

## 十五、总结

LinkedHashSet是Set接口的哈希表和链表实现，维护插入顺序，基于LinkedHashMap实现。

### 核心要点

1. **哈希表+链表**: 基于LinkedHashMap实现
2. **插入顺序**: 按元素插入的顺序迭代
3. **可预测顺序**: 迭代顺序是可预测的
4. **支持null**: 允许null元素
5. **非同步**: 非线程安全，需要外部同步
6. **Fail-Fast**: 迭代器实现快速失败机制
7. **性能特点**: 基本操作O(1)，迭代O(n)

### 适用场景

- 需要保持插入顺序的集合
- 需要快速查找的有序集合
- 需要LRU缓存实现
- 需要按访问顺序迭代的集合

### 不适用场景

- 需要排序顺序(使用TreeSet)
- 需要最高性能(使用HashSet)
- 多线程环境(需外部同步)
- 需要null键(使用HashMap)

### 性能特点

- 基本操作: O(1)
- 迭代: O(n)(与容量无关)
- 内存开销: 比HashSet高(维护链表)

### 与HashSet的选择

- 需要保持顺序: LinkedHashSet
- 不需要顺序: HashSet
- 需要迭代性能: LinkedHashSet(与容量无关)
- 需要最高性能: HashSet

### 与TreeSet的选择

- 需要插入顺序: LinkedHashSet
- 需要排序顺序: TreeSet
- 需要null元素: LinkedHashSet
- 需要排序: TreeSet
