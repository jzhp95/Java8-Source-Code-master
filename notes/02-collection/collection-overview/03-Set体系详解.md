# Java集合框架体系源码深度解析——Set体系详解

## 三、Set体系详解

### 3.1 Set体系概述

#### 3.1.1 Set接口特性

Set是不包含重复元素的集合,它继承自Collection接口,但语义上不允许重复元素。

**Set的核心特性:**

1. **唯一性**: 不包含重复元素,最多包含一个null。

2. **无序性**: 不保证元素的顺序(除了LinkedHashSet、TreeSet等特殊实现)。

3. **查找效率**: 基于哈希的Set查找效率高,时间复杂度O(1)。

4. **继承Collection**: Set接口继承自Collection,没有新增方法,只是语义上的约束。

```
Set接口体系
├── AbstractSet (抽象类)
│   ├── HashSet (哈希表实现)
│   ├── LinkedHashSet (保持插入顺序的HashSet)
│   └── TreeSet (基于TreeMap,有序)
├── EnumSet (枚举类型专用Set)
└── CopyOnWriteArraySet (写时复制Set,线程安全)
```

#### 3.1.2 Set接口核心方法

```java
public interface Set<E> extends Collection<E> {
    // 继承Collection的所有方法
    // 没有新增方法,只是语义上的约束
    
    // 默认实现
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }
    
    // equals方法: 两个Set相等当且仅当包含相同元素
    boolean equals(Object o);
    
    // hashCode方法: Set的hashCode等于所有元素的hashCode之和
    int hashCode();
}
```

### 3.2 HashSet深度解析

#### 3.2.1 HashSet概述

HashSet是基于哈希表实现的Set,它提供了快速的添加、删除和查找操作。HashSet内部使用HashMap来存储元素,将元素作为HashMap的key,value使用一个固定的Object。

**HashSet的核心特点:**

1. **快速查找**: 基于哈希表,查找时间复杂度O(1)。

2. **允许null**: 可以包含一个null元素。

3. **无序性**: 不保证元素的顺序,可能随时间变化。

4. **非线程安全**: 不是线程安全的,多线程环境下需要同步。

```java
public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {
    
    // 内部使用HashMap存储元素
    private transient HashMap<E,Object> map;
    
    // 用于HashMap的value,占位对象
    private static final Object PRESENT = new Object();
}
```

#### 3.2.2 核心构造方法

```java
// 构造一个空的HashSet,默认初始容量16,负载因子0.75
public HashSet() {
    map = new HashMap<>();
}

// 构造一个空的HashSet,指定初始容量
public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}

// 构造一个空的HashSet,指定初始容量和负载因子
public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}

// 构造一个包含指定集合元素的HashSet
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}
```

**负载因子说明:**

负载因子(loadFactor)是哈希表的一个重要参数,它表示哈希表在自动扩容之前可以达到多满的程度。默认值是0.75,这是时间和空间成本上的一个折衷。

- 负载因子越大: 空间利用率越高,但哈希冲突越多,查找性能下降
- 负载因子越小: 哈希冲突越少,查找性能越好,但空间利用率越低

#### 3.2.3 add操作详解

```java
// 添加元素到集合
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```

**add操作的时间复杂度:**

- 平均情况: O(1) - 哈希函数均匀分布,无冲突
- 最坏情况: O(n) - 所有元素哈希到同一个桶,退化为链表

**HashMap的put方法分析:**

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
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) { // existing mapping for key
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

#### 3.2.4 remove操作详解

```java
// 从集合中移除元素
public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
```

**remove操作的时间复杂度:**

- 平均情况: O(1)
- 最坏情况: O(n)

#### 3.2.5 contains操作详解

```java
// 检查集合是否包含元素
public boolean contains(Object o) {
    return map.containsKey(o);
}
```

**contains操作的时间复杂度:**

- 平均情况: O(1)
- 最坏情况: O(n)

### 3.3 LinkedHashSet深度解析

#### 3.3.1 LinkedHashSet概述

LinkedHashSet是HashSet的子类,它使用链表维护元素的插入顺序。LinkedHashSet内部使用LinkedHashMap来存储元素,在保持哈希表快速查找的同时,维护元素的插入顺序。

**LinkedHashSet的核心特点:**

1. **保持插入顺序**: 按照元素插入的顺序进行迭代。

2. **快速查找**: 基于哈希表,查找时间复杂度O(1)。

3. **额外开销**: 需要维护链表,比HashSet占用更多内存。

4. **非线程安全**: 不是线程安全的。

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {
    
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }
    
    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }
    
    public LinkedHashSet() {
        super(16, .75f, true);
    }
    
    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }
    
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
```

**注意:** LinkedHashSet的构造方法调用父类HashSet的构造方法,第三个参数dummy为true,表示使用LinkedHashMap。

#### 3.3.2 HashSet的protected构造方法

```java
// HashSet的protected构造方法,用于LinkedHashSet
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

这个构造方法是protected的,专门用于LinkedHashSet创建LinkedHashMap实例。

#### 3.3.3 LinkedHashMap的访问顺序

LinkedHashMap维护了一个双向链表,按照元素的插入顺序或访问顺序排列。

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
    
    // 链表节点
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }
}
```

**LinkedHashSet使用插入顺序:**

```java
// LinkedHashSet的构造方法
public LinkedHashSet() {
    super(16, .75f, true); // 第三个参数true表示使用LinkedHashMap
}

// HashSet的构造方法
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}

// LinkedHashMap的构造方法
public LinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false; // false表示按插入顺序
}
```

### 3.4 TreeSet深度解析

#### 3.4.1 TreeSet概述

TreeSet是基于TreeMap实现的有序Set,它使用红黑树来存储元素,保证元素的自然顺序或自定义顺序。

**TreeSet的核心特点:**

1. **有序性**: 按照元素的自然顺序或自定义顺序排列。

2. **查找效率**: 基于红黑树,查找时间复杂度O(log n)。

3. **不允许null**: 不允许null元素(除非使用自定义Comparator)。

4. **非线程安全**: 不是线程安全的。

```java
public class TreeSet<E> extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable {
    
    // 内部使用NavigableMap存储元素
    private transient NavigableMap<E,Object> m;
    
    // 用于NavigableMap的value,占位对象
    private static final Object PRESENT = new Object();
}
```

#### 3.4.2 核心构造方法

```java
// 构造一个空的TreeSet,使用元素的自然顺序
public TreeSet() {
    this(new TreeMap<E,Object>());
}

// 构造一个空的TreeSet,使用指定的Comparator
public TreeSet(Comparator<? super E> comparator) {
    this(new TreeMap<>(comparator));
}

// 构造一个包含指定集合元素的TreeSet
public TreeSet(Collection<? extends E> c) {
    this();
    addAll(c);
}

// 构造一个包含指定SortedSet元素的TreeSet
public TreeSet(SortedSet<E> s) {
    this(s.comparator());
    addAll(s);
}

// 构造一个包含指定NavigableMap元素的TreeSet
TreeSet(NavigableMap<E,Object> m) {
    this.m = m;
}
```

#### 3.4.3 add操作详解

```java
// 添加元素到集合
public boolean add(E e) {
    return m.put(e, PRESENT)==null;
}
```

**TreeMap的put方法分析:**

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

**add操作的时间复杂度:**

- O(log n): 红黑树的高度

#### 3.4.4 remove操作详解

```java
// 从集合中移除元素
public boolean remove(Object o) {
    return m.remove(o)==PRESENT;
}
```

**remove操作的时间复杂度:**

- O(log n): 红黑树的高度

#### 3.4.5 contains操作详解

```java
// 检查集合是否包含元素
public boolean contains(Object o) {
    return m.containsKey(o);
}
```

**contains操作的时间复杂度:**

- O(log n): 红黑树的高度

### 3.5 EnumSet深度解析

#### 3.5.1 EnumSet概述

EnumSet是专门为枚举类型设计的Set,它使用位向量(bit vector)来存储元素,非常高效。

**EnumSet的核心特点:**

1. **类型安全**: 只能包含指定枚举类型的元素。

2. **高效存储**: 使用位向量,内存占用极小。

3. **快速操作**: 所有操作都是位操作,非常高效。

4. **有序性**: 按照枚举类型的声明顺序排列。

5. **不允许null**: 不允许null元素。

```java
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E>
    implements Cloneable, java.io.Serializable {
    
    // 元素的类型
    final Class<E> elementType;
    
    // 枚举常量的数量
    final Enum<?>[] universe;
}
```

#### 3.5.2 EnumSet的创建

EnumSet是抽象类,不能直接实例化,需要通过工厂方法创建:

```java
// 创建一个包含指定枚举类型所有元素的EnumSet
public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
    Enum<?>[] universe = getUniverse(elementType);
    EnumSet<E> result = new RegularEnumSet<>(elementType, universe);
    result.addAll();
    return result;
}

// 创建一个空的EnumSet
public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    Enum<?>[] universe = getUniverse(elementType);
    if (universe.length <= 64)
        return new RegularEnumSet<>(elementType, universe);
    else
        return new JumboEnumSet<>(elementType, universe);
}

// 创建一个包含指定元素的EnumSet
@SafeVarargs
public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    EnumSet<E> result = noneOf(first.getDeclaringClass());
    result.add(first);
    for (E e : rest)
        result.add(e);
    return result;
}

// 创建一个包含指定集合元素的EnumSet
public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    if (c instanceof EnumSet) {
        return ((EnumSet<E>)c).clone();
    } else {
        if (c.isEmpty())
            throw new IllegalArgumentException("Collection is empty");
        Iterator<E> i = c.iterator();
        E first = i.next();
        EnumSet<E> result = noneOf(first.getDeclaringClass());
        result.add(first);
        while (i.hasNext())
            result.add(i.next());
        return result;
    }
}

// 创建一个包含指定EnumSet元素的EnumSet
public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
    return s.clone();
}
```

#### 3.5.3 RegularEnumSet实现

当枚举常量数量不超过64时,使用RegularEnumSet,使用long作为位向量:

```java
private static class RegularEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private long elements = 0L;
    
    RegularEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
    }
    
    void addAll() {
        if (universe.length != 0)
            elements = -1L >>> -universe.length;
    }
    
    public boolean add(E e) {
        typeCheck(e);
        long oldElements = elements;
        elements |= (1L << ((Enum<?>)e).ordinal());
        return elements != oldElements;
    }
    
    public boolean remove(Object o) {
        if (o == null)
            return false;
        Class<?> oClass = o.getClass();
        if (oClass != elementType && oClass.getSuperclass() != elementType)
            return false;
        long oldElements = elements;
        elements &= ~(1L << ((Enum<?>)o).ordinal());
        return elements != oldElements;
    }
    
    public boolean contains(Object o) {
        if (o == null)
            return false;
        Class<?> oClass = o.getClass();
        if (oClass != elementType && oClass.getSuperclass() != elementType)
            return false;
        return (elements & (1L << ((Enum<?>)o).ordinal())) != 0;
    }
}
```

#### 3.5.4 JumboEnumSet实现

当枚举常量数量超过64时,使用JumboEnumSet,使用long数组作为位向量:

```java
private static class JumboEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private long[] elements;
    
    JumboEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
        elements = new long[(universe.length + 63) >>> 6];
    }
    
    void addAll() {
        for (int i = 0; i < elements.length; i++)
            elements[i] = -1L;
        elements[elements.length - 1] >>>= -universe.length;
    }
    
    public boolean add(E e) {
        typeCheck(e);
        int ordinal = ((Enum<?>)e).ordinal();
        int wordIndex = ordinal >>> 6;
        long oldElements = elements[wordIndex];
        elements[wordIndex] |= (1L << ordinal);
        return elements[wordIndex] != oldElements;
    }
    
    public boolean remove(Object o) {
        if (o == null)
            return false;
        Class<?> oClass = o.getClass();
        if (oClass != elementType && oClass.getSuperclass() != elementType)
            return false;
        int ordinal = ((Enum<?>)o).ordinal();
        int wordIndex = ordinal >>> 6;
        long oldElements = elements[wordIndex];
        elements[wordIndex] &= ~(1L << ordinal);
        return elements[wordIndex] != oldElements;
    }
    
    public boolean contains(Object o) {
        if (o == null)
            return false;
        Class<?> oClass = o.getClass();
        if (oClass != elementType && oClass.getSuperclass() != elementType)
            return false;
        int ordinal = ((Enum<?>)o).ordinal();
        int wordIndex = ordinal >>> 6;
        return (elements[wordIndex] & (1L << ordinal)) != 0;
    }
}
```

### 3.6 Set体系对比

#### 3.6.1 性能对比

| 操作 | HashSet | LinkedHashSet | TreeSet | EnumSet |
|------|---------|---------------|---------|---------|
| add | O(1) | O(1) | O(log n) | O(1) |
| remove | O(1) | O(1) | O(log n) | O(1) |
| contains | O(1) | O(1) | O(log n) | O(1) |
| iterator | O(n) | O(n) | O(n) | O(n) |

#### 3.6.2 特性对比

| 特性 | HashSet | LinkedHashSet | TreeSet | EnumSet |
|------|---------|---------------|---------|---------|
| 有序性 | 无 | 插入顺序 | 自然顺序/自定义顺序 | 枚举顺序 |
| 允许null | 是 | 是 | 否(除非自定义Comparator) | 否 |
| 线程安全 | 否 | 否 | 否 | 否 |
| 内存占用 | 中等 | 较大 | 较大 | 最小 |
| 适用场景 | 快速查找 | 保持插入顺序 | 需要排序 | 枚举类型 |

### 3.7 Set使用最佳实践

#### 3.7.1 选择合适的Set实现

```java
// 场景1: 需要快速查找
Set<String> set = new HashSet<>();

// 场景2: 需要保持插入顺序
Set<String> orderedSet = new LinkedHashSet<>();

// 场景3: 需要排序
Set<String> sortedSet = new TreeSet<>();

// 场景4: 枚举类型
enum Color { RED, GREEN, BLUE }
Set<Color> colorSet = EnumSet.allOf(Color.class);
```

#### 3.7.2 避免常见错误

**错误1: 使用==比较Set元素**

```java
// 错误示例
Set<String> set = new HashSet<>();
set.add(new String("Hello"));

if (set.contains("Hello" == "Hello")) { // 可能返回false
    // ...
}

// 正确示例
if (set.contains("Hello")) { // 正确
    // ...
}
```

**错误2: 修改可变元素**

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

Set<Person> set = new HashSet<>();
Person p = new Person("Alice", 25);
set.add(p);
p.age = 26; // 修改了age,hashCode也变了
set.contains(p); // 返回false
```

**错误3: 在迭代时修改Set**

```java
// 错误示例
Set<String> set = new HashSet<>();
set.add("A");
set.add("B");
set.add("C");

for (String s : set) {
    if (s.equals("B")) {
        set.remove(s); // ConcurrentModificationException
    }
}

// 正确示例1: 使用Iterator
Iterator<String> iterator = set.iterator();
while (iterator.hasNext()) {
    String s = iterator.next();
    if (s.equals("B")) {
        iterator.remove();
    }
}

// 正确示例2: 使用removeIf
set.removeIf(s -> s.equals("B"));
```

#### 3.7.3 性能优化建议

**1. 预估集合大小**

```java
// 错误示例
Set<String> set = new HashSet<>();

// 正确示例
Set<String> set = new HashSet<>(1000);
```

**2. 使用合适的Set类型**

```java
// 场景1: 只需要快速查找
Set<String> set = new HashSet<>();

// 场景2: 需要保持插入顺序
Set<String> orderedSet = new LinkedHashSet<>();

// 场景3: 需要排序
Set<String> sortedSet = new TreeSet<>();

// 场景4: 枚举类型
enum Color { RED, GREEN, BLUE }
Set<Color> colorSet = EnumSet.allOf(Color.class);
```

### 3.8 小结

Set体系是Java集合框架中的重要部分,理解其实现原理对于编写高效、正确的代码至关重要。

**核心要点总结:**

第一,**HashSet**: 基于哈希表,快速查找,无序,允许null。

第二,**LinkedHashSet**: 基于LinkedHashMap,保持插入顺序,快速查找。

第三,**TreeSet**: 基于TreeMap,有序,查找O(log n),不允许null。

第四,**EnumSet**: 专门为枚举类型设计,使用位向量,非常高效。

第五,**性能对比**: HashSet/LinkedHashSet查找O(1),TreeSet查找O(log n)。

第六,**最佳实践**: 选择合适的Set类型、避免常见错误、优化性能。
