# Collections.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.util.Collections` 是 Java 集合框架中最核心的工具类之一，它完全由静态方法组成，提供对集合操作的各种算法和包装器。这个类自 JDK 1.2 起就是 Java 标准库的一部分，由著名的 Java 大师 Josh Bloch 和 Neal Gafter 设计编写。Collections 类的存在使得开发者可以方便地对各种集合进行排序、搜索、反转、洗牌等操作，而无需自行实现这些算法。

Collections 类的设计理念是提供一套完整的多态算法（polymorphic algorithms），这些算法可以应用于不同类型的集合。类名使用复数形式"Collections"而非单数"Collection"，强调它操作的是集合框架中的各种接口和类，而非单一类型。

### 1.2 核心设计原则

Collections 类的设计体现了几个重要的软件工程原则。**多态算法**是Collections最核心的设计理念，同一个算法（如排序）可以应用于不同的集合类型（List、Set等），这依赖于Java的多态机制。Collections类中的方法参数多使用接口类型（如List、Collection），而非具体实现类，这使得算法具有广泛的适用性。

**委托模式**的广泛应用是另一个重要特征。Collections类本身不直接实现复杂的集合操作，而是将大部分工作委托给底层集合对象。例如，`unmodifiableCollection()`方法返回的包装对象在执行查询操作时，会"穿透"到被包装的集合。

**不可实例化**原则也得到了严格执行。Collections类的私有构造函数确保了它不能被实例化，这符合工具类的典型设计模式：

```java
// Suppresses default constructor, ensuring non-instantiability.
private Collections() {
}
```

**性能优化**是Collections类设计的另一个亮点。通过设置各种阈值参数（如BINARYSEARCH_THRESHOLD = 5000），Collections类可以根据集合的大小和访问特性选择最优的算法实现。例如，对于随机访问列表使用索引访问的二分搜索，对于顺序访问列表则使用基于迭代器的二分搜索。

### 1.3 主要功能分类

Collections类提供的功能可以分为以下几个主要类别：**排序算法**（sort、reverse、shuffle、rotate）用于对列表元素进行重新排列；**搜索算法**（binarySearch）在有序列表中进行高效搜索；**极值查找**（min、max）用于找到集合中的最小或最大值；**修改操作**（fill、copy、swap、replaceAll）对集合内容进行修改；**子列表搜索**（indexOfSubList、lastIndexOfSubList）在列表中查找子列表的位置；**包装器**（unmodifiable、synchronized、checked）提供各种视图包装；**集合操作**（disjoint、addAll等）提供集合论操作。

## 二、核心字段与常量详解

### 2.1 算法调优参数

Collections类定义了一组用于算法性能调优的常量阈值，这些阈值基于经验测试确定，用于在不同的算法实现之间进行选择：

```java
private static final int BINARYSEARCH_THRESHOLD   = 5000;
private static final int REVERSE_THRESHOLD        =   18;
private static final int SHUFFLE_THRESHOLD        =    5;
private static final int FILL_THRESHOLD           =   25;
private static final int ROTATE_THRESHOLD         =  100;
private static final int COPY_THRESHOLD           =   10;
private static final int REPLACEALL_THRESHOLD     =   11;
private static final int INDEXOFSUBLIST_THRESHOLD =   35;
```

**BINARYSEARCH_THRESHOLD = 5000** 是二分搜索算法的关键阈值。当列表大小小于5000时，即使列表不支持RandomAccess接口，也使用基于索引的二分搜索。这是因为5000个元素的线性遍历开销仍然可以接受，而基于迭代器的二分搜索在每次获取中间元素时都需要O(n)的遍历。

**REVERSE_THRESHOLD = 18** 控制反转算法的实现选择。当列表大小小于18或支持RandomAccess时，使用简单的索引交换方式；否则使用双向迭代器方式进行反转。这个较小的阈值表明对于小列表，索引访问的性能优势不明显。

**SHUFFLE_THRESHOLD = 5** 对于洗牌操作也有类似的选择逻辑。当列表大小小于5时，无论是否支持RandomAccess都使用简单的交换算法。

**FILL_THRESHOLD = 25** 控制填充算法的实现。较小的列表使用set方法直接填充，较长的列表则使用迭代器以减少索引计算开销。

**ROTATE_THRESHOLD = 100** 控制旋转算法的选择。小列表使用单元素轮换算法，大列表则使用三次反转算法。

**COPY_THRESHOLD = 10** 和 **REPLACEALL_THRESHOLD = 11** 控制复制和替换算法的选择。这些较小的阈值表明在小规模操作中，基于索引的访问方式通常更高效。

**INDEXOFSUBLIST_THRESHOLD = 35** 控制子列表查找算法的实现。对于较大的列表，优先选择随机访问的实现方式。

### 2.2 随机数生成器

```java
private static Random r;
```

这个静态字段用于存储默认的随机数生成器。在`shuffle()`方法中，如果没有提供Random对象，则使用这个共享的随机数生成器：

```java
public static void shuffle(List<?> list) {
    Random rnd = r;
    if (rnd == null)
        r = rnd = new Random(); // harmless race.
    shuffle(list, rnd);
}
```

这里使用了延迟初始化和"无害竞争"（harmless race）的技巧。由于Random对象的创建成本较高，延迟初始化可以避免在类加载时就创建对象。同时，由于`r = rnd = new Random()`这条赋值语句在Java中是原子性的，即使多个线程同时进入也不会造成严重问题，只是可能创建多个Random对象（但最终只保留一个）。

## 三、排序算法详解

### 3.1 List排序

```java
public static <T extends Comparable<? super T>> void sort(List<T> list) {
    list.sort(null);
}

public static <T> void sort(List<T> list, Comparator<? super T> c) {
    list.sort(c);
}
```

Collections类的`sort()`方法实际上委托给了List接口的`sort()`方法。在Java 8之前，这里会使用归并排序算法实现；从Java 8开始，直接使用List接口中定义的方法。这种设计体现了"组合优于继承"的原则，Collections类不再维护自己的排序实现，而是利用List接口的能力。

排序方法的主要特性包括：**稳定性**，相等元素不会因为排序而改变相对顺序；**通用性**，可以接受任何实现了Comparable接口的元素，或使用自定义Comparator；**适用性**，列表必须是可修改的，但不一定需要可变大小（如Arrays.asList()返回的固定大小列表也可以排序）。

### 3.2 列表反转

```java
public static void reverse(List<?> list) {
    int size = list.size();
    if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
        for (int i=0, mid=size>>1, j=size-1; i<mid; i++, j--)
            swap(list, i, j);
    } else {
        ListIterator fwd = list.listIterator();
        ListIterator rev = list.listIterator(size);
        for (int i=0, mid=list.size()>>1; i<mid; i++) {
            Object tmp = fwd.next();
            fwd.set(rev.previous());
            rev.set(tmp);
        }
    }
}
```

反转算法根据列表的访问特性选择不同的实现策略。对于小列表或随机访问列表，使用简单的索引交换：从两端向中间遍历，交换对称位置的元素。对于大顺序访问列表，使用双向迭代器：前向迭代器和后向迭代器同时遍历列表，交换它们访问的元素。

这种设计的精妙之处在于它最小化了遍历次数。使用迭代器方法只需要遍历列表的一半（mid次），而不是完整遍历两次。迭代器方法的代码虽然看起来更复杂，但避免了每次迭代时的索引计算开销，对于LinkedList等顺序访问列表特别有利。

### 3.3 列表洗牌

```java
public static void shuffle(List<?> list) {
    Random rnd = r;
    if (rnd == null)
        r = rnd = new Random();
    shuffle(list, rnd);
}

public static void shuffle(List<?> list, Random rnd) {
    int size = list.size();
    if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
        for (int i=size; i>1; i--)
            swap(list, i-1, rnd.nextInt(i));
    } else {
        Object arr[] = list.toArray();
        for (int i=size; i>1; i--)
            swap(arr, i-1, rnd.nextInt(i));
        ListIterator it = list.listIterator();
        for (int i=0; i<arr.length; i++) {
            it.next();
            it.set(arr[i]);
        }
    }
}
```

洗牌算法采用Fisher-Yates算法的变体，从后向前遍历列表，将每个元素与当前位置之前的随机位置的元素交换。这种算法的特点是每个元素被交换到的每个位置的概率相等，产生均匀的随机排列。

对于不支持RandomAccess的大列表，算法首先将列表转换为数组，在数组上完成洗牌后再将结果复制回列表。这种"先转换后操作"的策略避免了顺序访问列表上原地洗牌的O(n²)性能问题（因为每次交换可能需要O(n)的遍历）。

## 四、搜索算法详解

### 4.1 二分搜索核心实现

```java
public static <T>
int binarySearch(List<? extends Comparable<? super T>> list, T key) {
    if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD)
        return Collections.indexedBinarySearch(list, key);
    else
        return Collections.iteratorBinarySearch(list, key);
}
```

二分搜索是Collections类中最具代表性的算法之一，它展示了如何根据集合特性选择最优实现。搜索算法的核心思想是在有序列表中通过反复将搜索范围减半来快速定位目标元素。

**基于索引的二分搜索**实现如下：

```java
private static <T>
int indexedBinarySearch(List<? extends Comparable<? super T>> list, T key) {
    int low = 0;
    int high = list.size()-1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        Comparable<? super T> midVal = list.get(mid);
        int cmp = midVal.compareTo(key);

        if (cmp < 0)
            low = mid + 1;
        else if (cmp > 0)
            high = mid - 1;
        else
            return mid; // key found
    }
    return -(low + 1);  // key not found
}
```

使用`>>>`（无符号右移）计算中间索引可以避免在low和high很大时可能发生的整数溢出。返回值的编码方式非常巧妙：找到时返回索引值（>= 0），未找到时返回插入点的负值减一，这样可以通过检查返回值是否非负来判断搜索是否成功。

**基于迭代器的二分搜索**用于顺序访问列表：

```java
private static <T>
int iteratorBinarySearch(List<? extends Comparable<? super T>> list, T key) {
    int low = 0;
    int high = list.size()-1;
    ListIterator<? extends Comparable<? super T>> i = list.listIterator();

    while (low <= high) {
        int mid = (low + high) >>> 1;
        Comparable<? super T> midVal = get(i, mid);
        int cmp = midVal.compareTo(key);

        if (cmp < 0)
            low = mid + 1;
        else if (cmp > 0)
            high = mid - 1;
        else
            return mid; // key found
    }
    return -(low + 1);  // key not found
}
```

这里的关键是`get()`方法，它通过移动迭代器来获取指定索引的元素：

```java
private static <T> T get(ListIterator<? extends T> i, int index) {
    T obj = null;
    int pos = i.nextIndex();
    if (pos <= index) {
        do {
            obj = i.next();
        } while (pos++ < index);
    } else {
        do {
            obj = i.previous();
        } while (--pos > index);
    }
    return obj;
}
```

`get()`方法智能地选择向前或向后移动迭代器，以最小化遍历距离。这种优化对于LinkedList等顺序访问列表至关重要。

## 五、极值查找算法

### 5.1 最小值查找

```java
public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll) {
    Iterator<? extends T> i = coll.iterator();
    T candidate = i.next();

    while (i.hasNext()) {
        T next = i.next();
        if (next.compareTo(candidate) < 0)
            candidate = next;
    }
    return candidate;
}
```

`min()`方法遍历整个集合，维护当前最小值候选者。泛型声明中的`T extends Object & Comparable<? super T>`是一个有趣的技巧，它确保类型T同时满足Object（确保不是基本类型）和Comparable两个约束。

### 5.2 最大值查找

```java
public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
    Iterator<? extends T> i = coll.iterator();
    T candidate = i.next();

    while (i.hasNext()) {
        T next = i.next();
        if (next.compareTo(candidate) > 0)
            candidate = next;
    }
    return candidate;
}
```

`max()`方法的实现与`min()`完全对称，只是比较方向相反。这两个算法都是O(n)时间复杂度，只需遍历一次集合。

## 六、包装器详解

### 6.1 不可修改包装器

```java
public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
    return new UnmodifiableCollection<>(c);
}

static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 1820017752578914078L;

    final Collection<? extends E> c;

    UnmodifiableCollection(Collection<? extends E> c) {
        if (c==null)
            throw new NullPointerException();
        this.c = c;
    }

    public int size()                   {return c.size();}
    public boolean isEmpty()            {return c.isEmpty();}
    public boolean contains(Object o)   {return c.contains(o);}
    // ... 其他查询方法透传到 c ...

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<? extends E> i = c.iterator();

            public boolean hasNext() {return i.hasNext();}
            public E next()          {return i.next();}
            public void remove() {
                throw new UnsupportedOperationException();
            }
            // ...
        };
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }
    // ... 其他修改方法都抛出异常 ...
}
```

`UnmodifiableCollection`是Collections类中最重要的内部类之一，它实现了"只读视图"的设计模式。这种包装器的核心思想是：查询操作"穿透"到被包装的集合（"read-through"），而任何修改操作都会抛出`UnsupportedOperationException`。

这种设计有几个重要特点：**透明性**，调用者可以像使用普通集合一样使用只读视图；**安全性**，防止意外的集合修改；**实时性**，对原始集合的修改会立即反映到只读视图中；**防御性**，模块可以向外部提供内部集合的只读访问，而不暴露修改能力。

Java 8还为包装器添加了新的默认方法实现：

```java
@Override
public void forEach(Consumer<? super E> action) {
    c.forEach(action);
}
@Override
public boolean removeIf(Predicate<? super T> filter) {
    throw new UnsupportedOperationException();
}
```

这些实现确保了新的集合操作也能正确处理只读视图。

### 6.2 同步包装器

```java
public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
    return new SynchronizedCollection<>(c);
}

static class SynchronizedCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 3053995032091335093L;

    final Collection<E> c;
    final Object mutex;

    SynchronizedCollection(Collection<E> c) {
        this.c = c;
        mutex = this;
    }
    // ...
}
```

同步包装器为集合操作添加了线程安全性。它通过在每个方法调用时获取锁（默认使用自身作为锁）来确保操作的原子性。使用同步包装器时需要注意：

```java
Collection<T> syncColl = Collections.synchronizedCollection(myCollection);
synchronized(syncColl) {
    Iterator<T> i = myCollection.iterator();
    while (i.hasNext()) {
        process(i.next());
    }
}
```

仅使用包装后的集合并不足以保证线程安全，遍历操作仍需要在外部同步。

### 6.3 类型检查包装器

```java
public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E> type) {
    return new CheckedCollection<>(c, type);
}

static class CheckedCollection<E> implements Collection<E>, Serializable {
    private final Collection<E> c;
    private final Class<E> type;
    // ...
}
```

类型检查包装器在运行时检查添加到集合中的元素类型，在类型不匹配时抛出ClassCastException。这对于需要在运行时确保类型安全（尽管编译时已经有类型检查）的场景很有用。

## 七、设计模式分析

### 7.1 包装器模式

Collections类中的各种"unmodifiable"、"synchronized"、"checked"方法都体现了包装器模式的应用。包装器模式的核心思想是：**不改变原有对象，而是创建一个新对象来添加新的行为**。

在Collections中，包装器模式的应用非常精妙：

```java
List<String> original = new ArrayList<>();
List<String> unmodifiable = Collections.unmodifiableList(original);

// unmodifiable 是 original 的一个"视图"，而非副本
original.add("hello");  // 成功
unmodifiable.add("world");  // 抛出 UnsupportedOperationException
```

这种设计允许模块提供"受控访问"——让外部代码可以读取内部数据，但阻止外部代码修改它。

### 7.2 策略模式

Collections类中的算法通过策略模式实现不同场景下的最优行为选择。核心实现通常有两个或多个版本，根据集合的特性（大小、是否支持RandomAccess）动态选择：

```java
public static void rotate(List<?> list, int distance) {
    if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD)
        rotate1(list, distance);  // 策略1：直接交换
    else
        rotate2(list, distance);  // 策略2：三次反转
}
```

这种设计使得同一个操作可以根据运行时的情况选择最高效的算法实现。

### 7.3 迭代器模式

Collections类广泛使用迭代器来进行集合遍历，特别是在需要双向遍历或需要"快照"语义时：

```java
ListIterator fwd = list.listIterator();
ListIterator rev = list.listIterator(size);
for (int i=0, mid=list.size()>>1; i<mid; i++) {
    Object tmp = fwd.next();
    fwd.set(rev.previous());
    rev.set(tmp);
}
```

ListIterator允许同时创建前向和后向两个迭代器，这在反转算法中实现了只遍历一次的高效实现。

## 八、常见使用模式

### 8.1 集合排序与搜索

```java
List<Integer> numbers = Arrays.asList(5, 2, 8, 1, 9);
Collections.sort(numbers);  // 自然排序
int index = Collections.binarySearch(numbers, 8);  // 二分搜索
Collections.reverse(numbers);  // 反转
```

### 8.2 创建只读集合

```java
public class ImmutableDataHolder {
    private final Set<String> data;
    
    public ImmutableDataHolder(Set<String> dataToWrap) {
        // 防御性复制
        this.data = new HashSet<>(dataToWrap);
    }
    
    public Set<String> getData() {
        // 返回只读视图，阻止外部修改
        return Collections.unmodifiableSet(data);
    }
}
```

### 8.3 线程安全集合

```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// 在遍历时需要外部同步
synchronized(syncMap) {
    for (Map.Entry<String, Integer> entry : syncMap.entrySet()) {
        // 处理每个条目
    }
}
```

### 8.4 洗牌算法在测试中的应用

```java
List<TestCase> testCases = Arrays.asList(...);
Collections.shuffle(testCases, new Random(42));  // 使用固定种子保证可重复性
// 或使用默认随机源进行随机测试
Collections.shuffle(testCases);
```

## 九、常见问题与注意事项

### 9.1 二分搜索的前提条件

二分搜索要求列表**必须是有序的**，且排序顺序必须与搜索时使用的比较器一致：

```java
List<String> list = Arrays.asList("a", "c", "e", "g");
// 错误：列表无序，结果未定义
int index = Collections.binarySearch(list, "b");
```

### 9.2 包装器的"浅"特性

不可修改包装器提供的是"浅"保护——它只阻止对集合结构的修改，不阻止对集合元素的修改：

```java
List<List<String>> nested = new ArrayList<>();
nested.add(new ArrayList<>());

List<List<String>> unmodifiable = Collections.unmodifiableList(nested);
unmodifiable.get(0).add("element");  // 这会成功！
```

### 9.3 同步包装器的外部同步需求

同步包装器只保证单个操作的原子性，不保证复合操作的原子性：

```java
Collection<String> syncColl = Collections.synchronizedCollection(new ArrayList<>());

// 下面的复合操作不是线程安全的！
if (!syncColl.contains("test")) {
    syncColl.add("test");
}
```

### 9.4 避免空指针异常

Collections类的大多数方法在传入null集合时会抛出NullPointerException。进行防御性编程时需要显式检查或使用Objects.requireNonNull()：

```java
public void processCollection(Collection<String> input) {
    if (input == null) {
        throw new IllegalArgumentException("Collection must not be null");
    }
    // 或者使用 Java 7+ 的方法
    Objects.requireNonNull(input, "Collection must not be null");
    // 继续处理...
}
```

## 十、面试常见问题

### 10.1 Collections和Collection的区别？

Collection是Java集合框架的根接口之一，定义了集合的基本操作（add、remove、size等）。Collections是包含静态工具方法的工具类，用于操作Collection接口的实现。简单记忆：Collection是"名词"，表示集合的概念；Collections是"工具"，提供操作集合的方法。

### 10.2 Collections.sort()的稳定性指的是什么？

稳定性是指相等元素的相对顺序在排序后保持不变。例如，如果列表中有两个相等的元素"A1"和"A2"（按自然顺序相等），排序前"A1"在"A2"前面，排序后"A1"仍然在"A2"前面。这在多次排序或需要保留原始顺序时很重要。

### 10.3 Collections.unmodifiableList()与Collections.unmodifiableCollection()的区别？

unmodifiableList()返回的视图额外实现了List接口，提供了基于索引的访问方法（get、set等）。unmodifiableCollection()只实现基本的Collection接口，不提供列表特有的功能。选择哪个取决于需要提供何种访问能力。

### 10.4 如何实现线程安全的遍历？

有几种方法：使用同步包装器并在遍历时对整个集合加锁；使用并发集合（如ConcurrentHashMap）提供更细粒度的并发控制；使用CopyOnWriteArrayList适合读多写少的场景。

### 10.5 Collections类中算法的时间复杂度是多少？

主要算法的时间复杂度：sort()是O(n log n)；binarySearch()是O(log n)；reverse()、shuffle()、fill()都是O(n)；min()和max()是O(n)；rotate()是O(n)。

### 10.6 为什么Collections类使用私有构造函数？

私有构造函数防止类被实例化，因为Collections类只包含静态方法，不需要实例。这种设计符合工具类的最佳实践，明确表示类的设计意图。

## 十一、与其他类的对比

### 11.1 Collections与Stream API

Java 8引入的Stream API提供了另一种处理集合数据的方式：

```java
// Collections方式
Collections.sort(list);
int min = Collections.min(list);

// Stream API方式
list.stream().sorted().collect(Collectors.toList());
int min = list.stream().mapToInt(Integer::intValue).min().getAsInt();
```

Stream API更函数式，适合复杂的数据处理管道；Collections的工具方法更直接，适合简单的集合操作。

### 11.2 Collections与Guava的Lists/Maps

Google Guava库提供了更丰富的集合工具类：

```java
// Guava的便利方法
List<List<T>> partitions = Lists.partition(originalList, size);
List<T> reversed = Lists.reverse(originalList);

// Collections需要更多代码
List<T> reversed = new ArrayList<>(originalList);
Collections.reverse(reversed);
```

Guava的工具通常更便捷，但引入外部依赖增加了项目的复杂性。

### 11.3 Collections与Apache Commons Collections

Apache Commons Collections是另一个流行的集合工具库：

```java
// Apache Commons的方法
ListUtils.predicatedList(new ArrayList<>(), predicate);
BidiMap<String, Integer> bidiMap = new DualHashBidiMap<>();
```

Apache Commons提供了Collections所没有的数据结构（如BidiMap、Bag等），但同样需要引入依赖。

