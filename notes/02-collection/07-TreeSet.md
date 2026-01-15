# TreeSet 源码解读

## 一、类概述

TreeSet是Java集合框架中的一种有序Set实现,基于TreeMap实现,使用红黑树存储元素。元素按照自然顺序或自定义Comparator进行排序。

### 1.1 核心特性

- **有序性**: 元素按照自然顺序或Comparator排序
- **基于TreeMap**: 内部使用TreeMap存储元素
- **唯一性**: 不允许重复元素
- **红黑树**: 使用红黑树实现,保证O(log n)的时间复杂度
- **NavigableSet**: 实现NavigableSet接口,提供导航方法
- **线程不安全**: 非同步实现,需要外部同步
- **Fail-Fast**: 迭代器实现快速失败机制

### 1.2 适用场景

- 需要排序的集合
- 需要快速查找最小/最大元素
- 需要范围查询
- 需要导航操作(前驱/后继)

## 二、设计原理

### 2.1 基于TreeMap的实现

TreeSet的核心设计思想是委托给TreeMap:

```java
// TreeSet内部使用TreeMap存储元素
private transient NavigableMap<E,Object> m;

// 使用固定对象PRESENT作为值
private static final Object PRESENT = new Object();

// 添加元素时,将元素作为键,PRESENT作为值
public boolean add(E e) {
    return m.put(e, PRESENT)==null;
}
```

### 2.2 红黑树特性

TreeSet通过TreeMap的红黑树实现获得以下特性:

```
- 自平衡: 红黑树始终保持平衡
- O(log n): 基本操作的时间复杂度
- 有序存储: 中序遍历得到有序序列
- 范围查询: 支持高效的子集操作
```

### 2.3 排序机制

TreeSet支持两种排序方式:

```java
// 1. 自然排序: 元素实现Comparable接口
TreeSet<String> set = new TreeSet<>();  // String实现Comparable

// 2. 自定义排序: 提供Comparator
TreeSet<String> set = new TreeSet<>(Comparator.reverseOrder());
```

## 三、继承结构

```
AbstractSet<E>
    ↑
TreeSet<E>
    implements NavigableSet<E>, Cloneable, Serializable
```

### 3.1 接口实现

- **Set**: 基本集合接口
- **SortedSet**: 有序集合接口
- **NavigableSet**: 导航集合接口,提供更多导航方法
- **Cloneable**: 支持克隆
- **Serializable**: 支持序列化

### 3.2 继承的方法

- AbstractSet: 提供Set的基本实现
- NavigableSet: 提供导航方法
- SortedSet: 提供有序集合方法

## 四、核心字段分析

### 4.1 实例字段

```java
// 底层Map,用于存储元素
private transient NavigableMap<E,Object> m;
```

**字段设计要点**:
- 使用NavigableMap而非普通Map,支持导航操作
- transient修饰,自定义序列化
- 泛型E表示元素类型,Object表示值类型

### 4.2 常量字段

```java
// 固定对象,作为Map的值
private static final Object PRESENT = new Object();
```

**设计要点**:
- 所有键值对使用同一个值对象
- 节省内存,避免创建多个对象
- 值不重要,只关心键的唯一性

### 4.3 序列化字段

```java
private static final long serialVersionUID = -2479143000061671589L;
```

## 五、构造方法

### 5.1 默认构造方法

```java
// 使用自然排序
public TreeSet() {
    this(new TreeMap<E,Object>());
}
```

**使用场景**:
- 元素实现Comparable接口
- 使用元素的自然顺序排序

### 5.2 指定Comparator的构造方法

```java
// 使用自定义Comparator排序
public TreeSet(Comparator<? super E> comparator) {
    this(new TreeMap<>(comparator));
}
```

**使用场景**:
- 元素未实现Comparable接口
- 需要自定义排序规则
- 需要改变自然排序顺序

### 5.3 从Collection构造

```java
// 从Collection构造,使用自然排序
public TreeSet(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

**使用场景**:
- 将其他集合转换为TreeSet
- 保持元素的自然顺序

### 5.4 从SortedSet构造

```java
// 从SortedSet构造,保持相同的排序
public TreeSet(SortedSet<E> s) {
    this(s.comparator());
    addAll(s);
}
```

**使用场景**:
- 复制SortedSet
- 保持原有的排序规则

### 5.5 包级构造方法

```java
// 从NavigableMap构造,包级访问权限
TreeSet(NavigableMap<E,Object> m) {
    this.m = m;
}
```

**使用场景**:
- 内部使用,创建子集视图
- descendingSet()等方法使用

## 六、核心方法解析

### 6.1 add - 添加元素

```java
public boolean add(E e) {
    return m.put(e, PRESENT)==null;
}
```

**方法要点**:
- 委托给TreeMap的put方法
- 使用PRESENT作为值
- 返回true表示添加成功(元素不存在)
- 返回false表示元素已存在

**时间复杂度**: O(log n)

### 6.2 remove - 移除元素

```java
public boolean remove(Object o) {
    return m.remove(o)==PRESENT;
}
```

**方法要点**:
- 委托给TreeMap的remove方法
- 返回true表示移除成功
- 返回false表示元素不存在

**时间复杂度**: O(log n)

### 6.3 contains - 检查包含

```java
public boolean contains(Object o) {
    return m.containsKey(o);
}
```

**方法要点**:
- 委托给TreeMap的containsKey方法
- 返回true表示元素存在
- 返回false表示元素不存在

**时间复杂度**: O(log n)

### 6.4 size - 获取大小

```java
public int size() {
    return m.size();
}
```

**时间复杂度**: O(1)

### 6.5 isEmpty - 检查是否为空

```java
public boolean isEmpty() {
    return m.isEmpty();
}
```

**时间复杂度**: O(1)

### 6.6 clear - 清空集合

```java
public void clear() {
    m.clear();
}
```

**时间复杂度**: O(n)

### 6.7 addAll - 批量添加

```java
public boolean addAll(Collection<? extends E> c) {
    // 优化: 如果TreeSet为空且c是SortedSet,使用线性时间添加
    if (m.size()==0 && c.size() > 0 &&
        c instanceof SortedSet &&
        m instanceof TreeMap) {
        SortedSet<? extends E> set = (SortedSet<? extends E>) c;
        TreeMap<E,Object> map = (TreeMap<E, Object>) m;
        Comparator<?> cc = set.comparator();
        Comparator<? super E> mc = map.comparator();
        if (cc==mc || (cc != null && cc.equals(mc))) {
            // 使用TreeMap的优化方法
            map.addAllForTreeSet(set, PRESENT);
            return true;
        }
    }
    return super.addAll(c);
}
```

**方法要点**:
- 特殊优化: 当TreeSet为空且源集合是SortedSet时,使用线性时间
- 比较Comparator是否相同
- 使用TreeMap的addAllForTreeSet方法优化性能

**时间复杂度**:
- 优化情况: O(n)
- 一般情况: O(n log n)

### 6.8 iterator - 获取迭代器

```java
public Iterator<E> iterator() {
    return m.navigableKeySet().iterator();
}
```

**方法要点**:
- 返回升序迭代器
- 基于TreeMap的键集迭代器
- Fail-Fast机制

### 6.9 descendingIterator - 降序迭代器

```java
public Iterator<E> descendingIterator() {
    return m.descendingKeySet().iterator();
}
```

**方法要点**:
- 返回降序迭代器
- 从大到小遍历元素

## 七、NavigableSet API

### 7.1 first - 获取第一个元素

```java
public E first() {
    return m.firstKey();
}
```

**返回**: 最小元素(升序)

**时间复杂度**: O(log n)

### 7.2 last - 获取最后一个元素

```java
public E last() {
    return m.lastKey();
}
```

**返回**: 最大元素(升序)

**时间复杂度**: O(log n)

### 7.3 lower - 严格小于

```java
public E lower(E e) {
    return m.lowerKey(e);
}
```

**返回**: 严格小于e的最大元素

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
set.lower(5);  // 返回3
set.lower(1);  // 返回null
```

### 7.4 floor - 小于等于

```java
public E floor(E e) {
    return m.floorKey(e);
}
```

**返回**: 小于等于e的最大元素

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
set.floor(5);  // 返回5
set.floor(4);  // 返回3
```

### 7.5 ceiling - 大于等于

```java
public E ceiling(E e) {
    return m.ceilingKey(e);
}
```

**返回**: 大于等于e的最小元素

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
set.ceiling(5);  // 返回5
set.ceiling(6);  // 返回7
```

### 7.6 higher - 严格大于

```java
public E higher(E e) {
    return m.higherKey(e);
}
```

**返回**: 严格大于e的最小元素

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
set.higher(5);  // 返回7
set.higher(9);  // 返回null
```

### 7.7 pollFirst - 移除并返回第一个元素

```java
public E pollFirst() {
    Map.Entry<E,?> e = m.pollFirstEntry();
    return (e == null) ? null : e.getKey();
}
```

**返回**: 最小元素,并从集合中移除

**时间复杂度**: O(log n)

### 7.8 pollLast - 移除并返回最后一个元素

```java
public E pollLast() {
    Map.Entry<E,?> e = m.pollLastEntry();
    return (e == null) ? null : e.getKey();
}
```

**返回**: 最大元素,并从集合中移除

**时间复杂度**: O(log n)

## 八、子集操作

### 8.1 subSet - 获取子集

```java
// NavigableSet版本,支持边界包含
public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                              E toElement,   boolean toInclusive) {
    return new TreeSet<>(m.subMap(fromElement, fromInclusive,
                                   toElement,   toInclusive));
}

// SortedSet版本,左闭右开
public SortedSet<E> subSet(E fromElement, E toElement) {
    return subSet(fromElement, true, toElement, false);
}
```

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

// [3, 7] - 包含3和7
NavigableSet<Integer> sub1 = set.subSet(3, true, 7, true);

// [3, 7) - 包含3,不包含7
NavigableSet<Integer> sub2 = set.subSet(3, 7);
```

**注意事项**:
- 子集是视图,修改会影响原集合
- fromElement必须小于等于toElement

### 8.2 headSet - 获取前缀子集

```java
// NavigableSet版本
public NavigableSet<E> headSet(E toElement, boolean inclusive) {
    return new TreeSet<>(m.headMap(toElement, inclusive));
}

// SortedSet版本,不包含toElement
public SortedSet<E> headSet(E toElement) {
    return headSet(toElement, false);
}
```

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

// 小于等于5的元素
NavigableSet<Integer> head1 = set.headSet(5, true);  // [1, 2, 3, 4, 5]

// 严格小于5的元素
NavigableSet<Integer> head2 = set.headSet(5);       // [1, 2, 3, 4]
```

### 8.3 tailSet - 获取后缀子集

```java
// NavigableSet版本
public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
    return new TreeSet<>(m.tailMap(fromElement, inclusive));
}

// SortedSet版本,包含fromElement
public SortedSet<E> tailSet(E fromElement) {
    return tailSet(fromElement, true);
}
```

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

// 大于等于5的元素
NavigableSet<Integer> tail1 = set.tailSet(5, true);  // [5, 6, 7, 8, 9, 10]

// 严格大于5的元素
NavigableSet<Integer> tail2 = set.tailSet(5, false); // [6, 7, 8, 9, 10]
```

### 8.4 descendingSet - 降序视图

```java
public NavigableSet<E> descendingSet() {
    return new TreeSet<>(m.descendingMap());
}
```

**示例**:
```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
NavigableSet<Integer> desc = set.descendingSet();  // [5, 4, 3, 2, 1]
```

## 九、其他方法

### 9.1 comparator - 获取比较器

```java
public Comparator<? super E> comparator() {
    return m.comparator();
}
```

**返回**:
- 返回使用的Comparator
- 如果使用自然排序,返回null

### 9.2 clone - 克隆

```java
@SuppressWarnings("unchecked")
public Object clone() {
    TreeSet<E> clone;
    try {
        clone = (TreeSet<E>) super.clone();
    } catch (CloneNotSupportedException e) {
        throw new InternalError(e);
    }

    clone.m = new TreeMap<>(m);
    return clone;
}
```

**方法要点**:
- 浅拷贝: 元素本身不被克隆
- 创建新的TreeMap
- 保持相同的Comparator

### 9.3 spliterator - 分割迭代器

```java
public Spliterator<E> spliterator() {
    return TreeMap.keySpliteratorFor(m);
}
```

**特性**:
- SIZED: 已知大小
- DISTINCT: 元素唯一
- SORTED: 有序
- ORDERED: 有序

## 十、序列化

### 10.1 writeObject - 序列化

```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
    // 写入默认字段
    s.defaultWriteObject();

    // 写入Comparator
    s.writeObject(m.comparator());

    // 写入大小
    s.writeInt(m.size());

    // 按顺序写入所有元素
    for (E e : m.keySet())
        s.writeObject(e);
}
```

**序列化格式**:
1. 默认字段
2. Comparator对象
3. 集合大小
4. 所有元素(按顺序)

### 10.2 readObject - 反序列化

```java
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // 读取默认字段
    s.defaultReadObject();

    // 读取Comparator
    @SuppressWarnings("unchecked")
        Comparator<? super E> c = (Comparator<? super E>) s.readObject();

    // 创建TreeMap
    TreeMap<E,Object> tm = new TreeMap<>(c);
    m = tm;

    // 读取大小
    int size = s.readInt();

    // 读取元素
    tm.readTreeSet(size, s, PRESENT);
}
```

## 十一、设计模式

### 11.1 委托模式

TreeSet将所有操作委托给TreeMap:

```java
// TreeSet委托给TreeMap
public boolean add(E e) {
    return m.put(e, PRESENT)==null;
}

public boolean remove(Object o) {
    return m.remove(o)==PRESENT;
}
```

**优点**:
- 代码简洁,避免重复
- 复用TreeMap的实现
- 易于维护

### 11.2 适配器模式

TreeSet将Map适配为Set:

```java
// 将TreeMap的键集合适配为Set
public Iterator<E> iterator() {
    return m.navigableKeySet().iterator();
}
```

### 11.3 视图模式

子集操作返回视图:

```java
// 子集是原集合的视图
public NavigableSet<E> subSet(E from, boolean fromInclusive,
                              E to,   boolean toInclusive) {
    return new TreeSet<>(m.subMap(from, fromInclusive, to, toInclusive));
}
```

## 十二、面试常见问题

### 12.1 TreeSet与HashSet的区别?

| 特性 | TreeSet | HashSet |
|------|---------|---------|
| 底层实现 | TreeMap(红黑树) | HashMap(哈希表) |
| 有序性 | 有序 | 无序 |
| 时间复杂度 | O(log n) | O(1) |
| 排序 | 支持自定义排序 | 不支持 |
| null元素 | 不允许(自然排序) | 允许 |
| 性能 | 较低 | 较高 |

### 12.2 TreeSet如何保证元素唯一性?

TreeSet通过TreeMap保证元素唯一性:
- TreeMap的键唯一
- 添加重复元素时,put返回旧值
- TreeSet根据返回值判断是否添加成功

### 12.3 TreeSet如何保证有序性?

TreeSet通过TreeMap的红黑树保证有序性:
- 红黑树的中序遍历是有序的
- 使用Comparable或Comparator比较元素
- 迭代器按中序遍历返回元素

### 12.4 TreeSet允许null元素吗?

取决于Comparator:
- 自然排序: 不允许null
- 自定义Comparator: 取决于Comparator是否支持null

```java
// 自然排序: 抛出NullPointerException
TreeSet<String> set = new TreeSet<>();
set.add(null);  // NullPointerException

// 自定义Comparator: 可以支持null
TreeSet<String> set = new TreeSet<>(Comparator.nullsFirst(String::compareTo));
set.add(null);  // OK
```

### 12.5 TreeSet是线程安全的吗?

不是。需要外部同步:

```java
// 创建同步的TreeSet
SortedSet<String> set = Collections.synchronizedSortedSet(new TreeSet<>());

// 使用时同步
synchronized (set) {
    Iterator<String> it = set.iterator();
    while (it.hasNext()) {
        System.out.println(it.next());
    }
}
```

### 12.6 TreeSet的迭代器是fail-fast的吗?

是的。如果在迭代过程中集合被结构性修改,会抛出ConcurrentModificationException。

```java
TreeSet<String> set = new TreeSet<>();
set.add("a");
set.add("b");
set.add("c");

Iterator<String> it = set.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("b")) {
        set.add("d");  // ConcurrentModificationException
    }
}
```

### 12.7 TreeSet的基本操作时间复杂度?

- add: O(log n)
- remove: O(log n)
- contains: O(log n)
- first/last: O(log n)
- lower/floor/ceiling/higher: O(log n)
- pollFirst/pollLast: O(log n)
- size: O(1)
- isEmpty: O(1)

### 12.8 TreeSet的子集操作是视图还是副本?

是视图。修改子集会影响原集合:

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
NavigableSet<Integer> sub = set.subSet(2, true, 4, true);

sub.add(3);  // 不影响,元素已存在
sub.add(10); // IllegalArgumentException: 10不在范围内
set.add(3);  // sub中也能看到
```

## 十三、使用场景

### 13.1 需要排序的场景

```java
// 自动排序的集合
TreeSet<String> words = new TreeSet<>();
words.add("banana");
words.add("apple");
words.add("cherry");

// 自动按字母顺序排序
System.out.println(words);  // [apple, banana, cherry]
```

### 13.2 需要快速查找最小/最大元素

```java
TreeSet<Integer> numbers = new TreeSet<>(Arrays.asList(5, 2, 8, 1, 9));

// 快速获取最小/最大元素
int min = numbers.first();   // 1
int max = numbers.last();    // 9

// 移除并获取最小/最大元素
int min2 = numbers.pollFirst();  // 1
int max2 = numbers.pollLast();   // 9
```

### 13.3 需要范围查询

```java
TreeSet<Integer> numbers = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

// 获取[3, 7]范围内的元素
NavigableSet<Integer> range = numbers.subSet(3, true, 7, true);
System.out.println(range);  // [3, 4, 5, 6, 7]

// 获取小于等于5的元素
NavigableSet<Integer> head = numbers.headSet(5, true);
System.out.println(head);   // [1, 2, 3, 4, 5]

// 获取大于等于5的元素
NavigableSet<Integer> tail = numbers.tailSet(5, true);
System.out.println(tail);   // [5, 6, 7, 8, 9, 10]
```

### 13.4 需要导航操作

```java
TreeSet<Integer> numbers = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

// 查找前驱和后继
int lower = numbers.lower(5);     // 3 (严格小于)
int floor = numbers.floor(5);      // 5 (小于等于)
int ceiling = numbers.ceiling(5);  // 5 (大于等于)
int higher = numbers.higher(5);    // 7 (严格大于)
```

### 13.5 自定义排序

```java
// 降序排序
TreeSet<String> set = new TreeSet<>(Comparator.reverseOrder());
set.add("a");
set.add("b");
set.add("c");
System.out.println(set);  // [c, b, a]

// 按字符串长度排序
TreeSet<String> set2 = new TreeSet<>(Comparator.comparingInt(String::length));
set2.add("aaa");
set2.add("b");
set2.add("cc");
System.out.println(set2);  // [b, cc, aaa]
```

## 十四、注意事项

### 14.1 Comparable与equals的一致性

如果TreeSet使用自然排序,元素的compareTo方法必须与equals方法一致:

```java
// 错误示例: compareTo与equals不一致
class Person implements Comparable<Person> {
    String name;
    int age;

    public int compareTo(Person other) {
        return this.age - other.age;  // 按年龄比较
    }

    public boolean equals(Object obj) {
        return this.name.equals(((Person)obj).name);  // 按姓名比较
    }
}

TreeSet<Person> set = new TreeSet<>();
Person p1 = new Person("Alice", 20);
Person p2 = new Person("Bob", 20);

set.add(p1);
set.add(p2);  // 不会添加,因为compareTo认为相等
// 但equals认为不相等,违反Set契约
```

### 14.2 修改已存在元素的排序属性

修改已存在元素的排序属性会导致TreeSet行为异常:

```java
class Person implements Comparable<Person> {
    String name;
    int age;

    public int compareTo(Person other) {
        return this.age - other.age;
    }
}

TreeSet<Person> set = new TreeSet<>();
Person p = new Person("Alice", 20);
set.add(p);

p.age = 30;  // 修改排序属性

set.contains(p);  // 可能返回false
set.remove(p);    // 可能无法移除
```

### 14.3 子集操作的边界限制

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));

// 错误: fromElement > toElement
set.subSet(5, 3);  // IllegalArgumentException

// 错误: 添加超出范围的元素
NavigableSet<Integer> sub = set.subSet(2, true, 4, true);
sub.add(5);  // IllegalArgumentException
```

### 14.4 并发修改异常

```java
TreeSet<String> set = new TreeSet<>();
set.add("a");
set.add("b");
set.add("c");

// 错误: 迭代时修改集合
for (String s : set) {
    if (s.equals("b")) {
        set.remove(s);  // ConcurrentModificationException
    }
}

// 正确: 使用迭代器的remove方法
Iterator<String> it = set.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("b")) {
        it.remove();  // OK
    }
}
```

## 十五、与其他Set的对比

### 15.1 HashSet vs TreeSet

```java
// HashSet: 无序,性能高
HashSet<String> hashSet = new HashSet<>();
hashSet.add("banana");
hashSet.add("apple");
hashSet.add("cherry");
System.out.println(hashSet);  // 顺序不确定

// TreeSet: 有序,性能稍低
TreeSet<String> treeSet = new TreeSet<>();
treeSet.add("banana");
treeSet.add("apple");
treeSet.add("cherry");
System.out.println(treeSet);  // [apple, banana, cherry]
```

### 15.2 LinkedHashSet vs TreeSet

| 特性 | LinkedHashSet | TreeSet |
|------|---------------|---------|
| 有序性 | 插入顺序 | 排序顺序 |
| 底层实现 | LinkedHashMap | TreeMap |
| 时间复杂度 | O(1) | O(log n) |
| 排序 | 不支持 | 支持 |
| 导航操作 | 不支持 | 支持 |

### 15.3 CopyOnWriteArraySet vs TreeSet

| 特性 | CopyOnWriteArraySet | TreeSet |
|------|---------------------|---------|
| 线程安全 | 是 | 否 |
| 底层实现 | 数组 | 红黑树 |
| 写操作 | 复制整个数组 | O(log n) |
| 读操作 | O(n) | O(log n) |
| 有序性 | 插入顺序 | 排序顺序 |

## 十六、最佳实践

### 16.1 选择合适的Set

```java
// 通用场景: HashSet
Set<String> set = new HashSet<>();

// 需要保持插入顺序: LinkedHashSet
Set<String> linkedSet = new LinkedHashSet<>();

// 需要排序: TreeSet
Set<String> treeSet = new TreeSet<>();

// 需要自定义排序
Set<String> customSet = new TreeSet<>(Comparator.reverseOrder());

// 线程安全: 使用同步包装
Set<String> syncSet = Collections.synchronizedSet(new TreeSet<>());
```

### 16.2 正确实现Comparable

```java
class Person implements Comparable<Person> {
    private String name;
    private int age;

    // compareTo必须与equals一致
    public int compareTo(Person other) {
        // 先按姓名,再按年龄
        int nameCompare = this.name.compareTo(other.name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return Integer.compare(this.age, other.age);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Person)) return false;
        Person other = (Person) obj;
        return this.name.equals(other.name) && this.age == other.age;
    }

    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

### 16.3 使用Comparator.nullsFirst/Comparator.nullsLast

```java
// 允许null元素
TreeSet<String> set = new TreeSet<>(Comparator.nullsFirst(String::compareTo));
set.add("b");
set.add(null);
set.add("a");
System.out.println(set);  // [null, a, b]
```

### 16.4 使用NavigableSet的导航方法

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

// 查找最接近的元素
int target = 4;
int floor = set.floor(target);    // 3
int ceiling = set.ceiling(target);  // 5

// 选择更接近的元素
int closest = (target - floor) <= (ceiling - target) ? floor : ceiling;
```

### 16.5 使用子集进行范围操作

```java
TreeSet<Integer> set = new TreeSet<>();
for (int i = 1; i <= 100; i++) {
    set.add(i);
}

// 获取[10, 20]范围内的元素
NavigableSet<Integer> range = set.subSet(10, true, 20, true);

// 批量操作
range.clear();  // 清除范围内的元素
```

## 十七、性能优化

### 17.1 批量添加优化

```java
// 从SortedSet批量添加,使用优化路径
SortedSet<Integer> source = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
TreeSet<Integer> target = new TreeSet<>(source);  // 使用优化构造方法

// 比逐个添加快
TreeSet<Integer> target2 = new TreeSet<>();
target2.addAll(source);  // 也会优化
```

### 17.2 避免频繁的导航操作

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));

// 不推荐: 多次导航操作
for (int i = 0; i < 1000; i++) {
    set.floor(i);
}

// 推荐: 转换为数组
Integer[] array = set.toArray(new Integer[0]);
// 使用二分查找
```

### 17.3 合理使用子集视图

```java
TreeSet<Integer> set = new TreeSet<>();
for (int i = 1; i <= 10000; i++) {
    set.add(i);
}

// 使用子集视图,避免创建新集合
NavigableSet<Integer> range = set.subSet(1000, true, 2000, true);

// 操作子集
range.remove(1500);  // 从原集合中移除
```

## 十八、总结

TreeSet是一个基于TreeMap实现的有序Set,使用红黑树存储元素,提供O(log n)的基本操作时间复杂度。

### 核心要点

1. **基于TreeMap**: 内部委托给TreeMap实现
2. **有序性**: 元素按自然顺序或Comparator排序
3. **红黑树**: 使用红黑树保证平衡和性能
4. **NavigableSet**: 提供丰富的导航方法
5. **子集视图**: 支持高效的子集操作
6. **线程不安全**: 需要外部同步
7. **Fail-Fast**: 迭代器实现快速失败机制

### 适用场景

- 需要排序的集合
- 需要快速查找最小/最大元素
- 需要范围查询
- 需要导航操作

### 不适用场景

- 需要高性能(O(1)操作)
- 不需要排序
- 多线程环境(需外部同步)
- 需要null元素(自然排序)

### 性能特点

- 基本操作: O(log n)
- 子集操作: O(1)创建视图
- 内存开销: 比HashSet高(红黑树节点)

### 与其他Set的选择

- 通用场景: HashSet
- 保持插入顺序: LinkedHashSet
- 需要排序: TreeSet
- 线程安全: Collections.synchronizedSet或CopyOnWriteArraySet
