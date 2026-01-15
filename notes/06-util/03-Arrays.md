# Arrays.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.util.Arrays` 是 Java 标准库中专门用于操作数组的核心工具类，它包含各种静态方法来对数组进行排序、搜索、比较、填充等操作。这个类与 `java.util.Collections` 遥相呼应，Collections 负责集合框架的操作，而 Arrays 负责原生数组的操作。Arrays 类由 Java 集合框架的创始人 Josh Bloch、Neal Gafter 和 John Rose 共同设计编写，自 JDK 1.2 起就是 Java 标准库的重要组成部分。

Arrays 类的设计目标是提供一套完整、高效、类型安全的数组操作工具。与 Collections 类一样，Arrays 类也采用完全静态方法的設計，不允许实例化。类名使用复数形式"Arrays"暗示它处理的是数组这种数据结构，而非单个数组对象。

### 1.2 核心设计原则

Arrays 类的设计体现了几个重要的软件工程原则。**类型特化**是 Arrays 最显著的特征，Arrays 类为每种基本类型（int、long、short、char、byte、float、double）以及对象类型提供了独立的重载方法。这种设计避免了自动装箱拆箱的性能开销，同时提供了最直接高效的原生类型操作。

**算法效率优先**是另一个核心原则。Arrays 类中的排序算法采用了业界最优的实现：Java 7 引入的双轴快速排序（Dual-Pivot Quicksort）算法，由 Vladimir Yaroslavskiy、Jon Bentley 和 Josh Bloch 共同发明，该算法在大多数数据集上比传统单轴快速排序更快。Java 8 引入的并行排序（parallelSort）方法利用 ForkJoin 框架实现多线程并行排序，充分利用多核处理器的优势。

**泛型与重载的平衡**也体现在 Arrays 的设计中。对于对象数组，Arrays 使用泛型来保证类型安全；对于基本类型数组，通过提供完整的方法重载来避免装箱拆箱的开销。这种设计虽然导致代码量较大，但提供了最佳的性能和类型安全性。

### 1.3 主要功能分类

Arrays 类提供的功能可以分为以下几个主要类别：**排序算法**（sort、parallelSort）对数组元素进行排序；**搜索算法**（binarySearch）在有序数组中进行高效搜索；**比较操作**（equals、deepEquals）比较数组内容；**填充操作**（fill）用指定值填充数组；**复制操作**（copyOf、copyOfRange）创建数组副本；**转换操作**（asList、stream）将数组转换为集合或流；**字符串表示**（toString、deepToString）获取数组的字符串形式；**哈希计算**（hashCode、deepHashCode）计算数组的哈希值。

## 二、核心字段与常量详解

### 2.1 并行排序粒度阈值

```java
private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;
```

`MIN_ARRAY_SORT_GRAN` 常量定义为 2 的 13 次方，即 8192。这个常量表示并行排序算法的最小任务粒度。当数组长度小于等于 8192 时，parallelSort 方法会退化为普通的顺序排序，因为对于太小的数组，并行化带来的额外开销（如任务创建、线程同步）会超过并行化带来的性能收益。

这个阈值的设置基于经验测试：小于这个粒度的子数组会产生过多的并行任务，导致内存竞争加剧，反而降低性能。选择 8192 作为阈值是因为它提供了足够的元素数量来分摊并行化的开销，同时又不会导致单一任务过大而无法充分利用多核优势。

### 2.2 自然排序比较器

```java
static final class NaturalOrder implements Comparator<Object> {
    @SuppressWarnings("unchecked")
    public int compare(Object first, Object second) {
        return ((Comparable<Object>)first).compareTo(second);
    }
    static final NaturalOrder INSTANCE = new NaturalOrder();
}
```

`NaturalOrder` 是 Arrays 类中用于对象数组排序的默认比较器实现。它实现了 `Comparator<Object>` 接口，使用元素的自然顺序（Comparable 接口）进行比较。这种设计允许在排序方法中统一处理有比较器和无比较器的情况：传入 null 比较器时使用 NaturalOrder.INSTANCE。

选择将比较器实现为内部类并使用单例模式（INSTANCE）有几个好处：避免了每次排序时创建新比较器对象的开销；由于比较器是无状态的，可以安全地被多个线程共享；保持了代码组织的紧凑性。

### 2.3 范围检查方法

```java
private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
        throw new IllegalArgumentException(
                "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
    if (fromIndex < 0) {
        throw new ArrayIndexOutOfBoundsException(fromIndex);
    }
    if (toIndex > arrayLength) {
        throw new ArrayIndexOutOfBoundsException(toIndex);
    }
}
```

`rangeCheck` 方法是 Arrays 类中处理范围参数的通用验证逻辑。它执行三个检查：确保 fromIndex 不大于 toIndex；确保 fromIndex 非负；确保 toIndex 不超过数组长度。这种统一的验证逻辑保证了所有接受范围参数的方法具有一致的行为和错误提示。

## 三、排序算法详解

### 3.1 双轴快速排序算法

```java
public static void sort(int[] a) {
    DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
}
```

Arrays 类中的基本类型排序方法都委托给 `DualPivotQuicksort` 类实现。双轴快速排序是由 Vladimir Yaroslavskiy 在 2009 年为 Java 7 开发的创新算法，它相比传统单轴快速排序具有更好的平均性能。

双轴快速排序的核心思想是在每次分区时使用两个轴心（pivot），将数组分成三个部分：小于左轴心的元素、介于两个轴心之间的元素、大于右轴心的元素。这种三分区策略相比传统的二分区策略能够更均匀地划分数据，减少最坏情况的发生概率。

算法的主要优势包括：**更好的分区均匀性**，两个轴心使数据分布更均匀，减少了递归深度；**更少的比较次数**，三分区策略减少了不必要的元素比较；**更好的缓存局部性**，相邻元素的访问模式更有利于 CPU 缓存预取；**稳定性**，对于相等元素的处理更加合理，避免了不必要的交换。

### 3.2 并行排序实现

```java
public static void parallelSort(int[] a) {
    int n = a.length, p, g;
    if (n <= MIN_ARRAY_SORT_GRAN ||
        (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    else
        new ArraysParallelSortHelpers.FJInt.Sorter
            (null, a, new int[n], 0, n, 0,
             ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
             MIN_ARRAY_SORT_GRAN : g).invoke();
}
```

`parallelSort` 方法是 Java 8 引入的并行排序功能，它利用 ForkJoin 框架实现多线程并行排序。算法的工作原理是：**任务分解**，将大数组递归分解成足够小的子数组；**并行排序**，每个子数组在单独的线程中排序；**结果合并**，将排序后的子数组合并成完整的有序数组。

并行排序的决策逻辑包括：首先检查数组长度是否小于最小粒度阈值，如果是则使用顺序排序；然后检查系统的并行度（CPU 核心数），如果是单核系统则退化为顺序排序；否则创建 ForkJoin 任务进行并行排序。

```java
int g = n / (p << 2);  // 计算粒度，p << 2 约等于 4p
g = (g <= MIN_ARRAY_SORT_GRAN) ? MIN_ARRAY_SORT_GRAN : g;
```

这段代码计算每个并行任务的粒度（子数组大小）。粒度的计算策略是：将数组长度除以 4 倍的并行度，然后与最小粒度阈值比较取较大值。这种计算方式确保每个线程获得大致相等的负载。

### 3.3 对象数组排序

```java
public static void sort(Object[] a) {
    if (LegacyMergeSort.lRequested)
        legacyMergeSort(a);
    else
        TimSort.sort(a, 0, a.length, null, 0, 0);
}
```

对象数组的排序使用 `TimSort` 算法，这是一种自适应的归并排序，由 Python 的创始人 Tim Peters 为 Python 语言开发，后被 Java 采用。TimSort 的特点包括：**自适应**，在部分有序的数据上表现更好；**稳定性**，相等元素的相对顺序保持不变；**最坏情况保障**，保证 O(n log n) 的最坏时间复杂度。

TimSort 算法利用了数据中已经存在的有序片段（runs），通过延长或创建新的 runs 来减少需要排序的数据量。对于随机数据，它仍然能够提供接近 O(n log n) 的性能；对于部分有序的数据，性能显著优于普通归并排序。

## 四、搜索算法详解

### 4.1 二分搜索实现

Arrays 类的 `binarySearch` 方法提供在有序数组中快速定位元素的能力：

```java
public static int binarySearch(int[] a, int key) {
    return binarySearch0(a, 0, a.length, key);
}

private static int binarySearch0(int[] a, int fromIndex, int toIndex, int key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        int midVal = a[mid];

        if (midVal < key)
            low = mid + 1;
        else if (midVal > key)
            high = mid - 1;
        else
            return mid;  // key found
    }
    return -(low + 1);  // key not found
}
```

二分搜索算法的核心思想是通过反复将搜索范围减半来快速定位目标元素。每次迭代的时间复杂度是 O(1)，整个算法的时间复杂度是 O(log n)，这使得搜索大规模有序数组非常高效。

返回值的编码方式与 Collections 类中的 binarySearch 相同：找到元素时返回其索引（大于等于 0）；未找到时返回负值，表示元素应该插入的位置（-(插入点+1)）。这种设计允许调用者通过检查返回值是否非负来判断搜索是否成功。

### 4.2 对象数组的二分搜索

```java
public static int binarySearch(Object[] a, Object key) {
    return binarySearch0(a, 0, a.length, key);
}

private static int binarySearch0(Object[] a, int fromIndex, int toIndex, Object key) {
    int low = fromIndex;
    int high = toIndex - 1;
    while (low <= high) {
        int mid = (low + high) >>> 1;
        Object midVal = a[mid];
        @SuppressWarnings("unchecked")
        int cmp = ((Comparable<Object>)midVal).compareTo(key);

        if (cmp < 0)
            low = mid + 1;
        else if (cmp > 0)
            high = mid - 1;
        else
            return mid;
    }
    return -(low + 1);
}
```

对象数组的二分搜索使用 `Comparable.compareTo` 方法进行比较。需要注意的是，数组元素必须是有序的，并且排序使用的比较规则必须与搜索时使用的比较规则一致，否则搜索结果未定义。

## 五、数组操作方法详解

### 5.1 数组复制

```java
public static <T> T[] copyOf(Integer[] original, int newLength) {
    T[] copy = ((Object)newLength == (Object)original.length)
        ? original
        : (T[]) Array.newInstance(original.getClass().getComponentType(), newLength);
    System.arraycopy(original, 0, copy, 0,
                     Math.min(original.length, newLength));
    return copy;
}
```

`copyOf` 方法用于创建数组的副本，支持改变数组长度。当新长度大于原数组时，新增元素被填充为对应类型的默认值（null 对于对象类型，0 对于数值类型）；当新长度小于原数组时，副本只包含前面的新长度个元素。

内部实现使用 `System.arraycopy` 进行高效的内存复制，这是一个 native 方法，直接操作内存，效率远高于循环复制。`Array.newInstance` 用于动态创建指定类型和长度的数组。

### 5.2 数组转列表

```java
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}

private static class ArrayList<E> extends AbstractList<E>
    implements RandomAccess, java.io.Serializable
{
    private final E[] a;

    ArrayList(E[] array) {
        a = array;
    }
    // ...
}
```

`asList` 方法将数组转换为 List 视图。返回的 List 是一个固定大小的列表，基于原数组——对返回列表的修改会直接反映到原数组中，反之亦然。这种"视图"设计避免了不必要的数据复制，但需要注意以下限制：返回的列表大小固定，不能添加或删除元素；返回的列表支持 set 操作修改元素；对原始数组的修改会反映到列表中。

### 5.3 数组相等比较

```java
public static boolean equals(int[] a, int[] a2) {
    if (a==a2)
        return true;
    if (a==null || a2==null)
        return false;

    int length = a.length;
    if (a2.length != length)
        return false;

    for (int i=0; i<length; i++)
        if (a[i] != a2[i])
            return false;

    return true;
}
```

`equals` 方法用于比较两个基本类型数组的内容是否相等。比较过程包括：首先检查两个数组是否是同一个对象；然后检查是否有任一数组为 null；接着比较长度是否相同；最后逐元素比较内容。对于对象数组，使用 `Objects.equals` 进行比较，能够正确处理 null 元素。

```java
public static boolean deepEquals(Object[] a1, Object[] a2) {
    if (a1 == a2)
        return true;
    if (a1 == null || a2 == null)
        return false;
    int length = a1.length;
    if (a2.length != length)
        return false;

    for (int i = 0; i < length; i++) {
        Object e1 = a1[i];
        Object e2 = a2[i];

        if (!(e1==null ? e2==null : e1.equals(e2))) {
            if (!(e1 instanceof Object[] && e2 instanceof Object[] &&
                  deepEquals0((Object[]) e1, (Object[]) e2)))
                return false;
        }
    }
    return true;
}
```

`deepEquals` 方法用于比较多维数组或包含数组元素的数组。它递归地比较每个元素，对于嵌套数组会深入比较其内容。这种递归比较一直进行到所有嵌套层级都被检查。

## 六、设计模式分析

### 6.1 工具类模式

Arrays 类是工具类模式的典型实现。工具类模式的核心特征是：类只包含静态方法，不能被实例化，通过类名直接调用方法。Arrays 类的私有构造函数明确表达了这种设计意图：

```java
private Arrays() {}
```

这种模式的优势在于：使用简单直接，无需创建对象；实例方法调用开销被消除；单例模式下状态管理更简单。

### 6.2 策略模式

Arrays 类的排序和比较操作体现了策略模式的应用。排序时使用的比较策略（自然顺序或自定义 Comparator）可以被灵活替换。对于对象数组的排序，Arrays.sort() 接受 Comparator 参数，允许调用者指定不同的比较策略：

```java
Arrays.sort(array, Comparator.reverseOrder());
```

这种设计使得排序算法与比较逻辑解耦，可以独立变化。

### 6.3 模板方法模式

双轴快速排序和并行排序的实现中可以看到模板方法模式的应用。基本排序流程（选择轴心、分区、递归排序）在父类中定义，具体的数据类型特化在子类中实现。这种设计减少了重复代码，同时保持了算法的统一性。

### 6.4 外观模式

Arrays 类为开发者提供了一个统一的接口来操作各种类型的数组。底层实现细节（DualPivotQuicksort、TimSort、ForkJoin 并行处理等）对调用者完全透明。开发者只需要调用 Arrays.sort() 或 Arrays.binarySearch()，无需了解具体的算法实现。

## 七、常见使用模式

### 7.1 数组排序与搜索

```java
int[] numbers = {5, 2, 8, 1, 9, 3};
Arrays.sort(numbers);  // 排序：[1, 2, 3, 5, 8, 9]
int index = Arrays.binarySearch(numbers, 8);  // 搜索：返回 4
```

### 7.2 对象数组排序

```java
String[] words = {"banana", "apple", "cherry"};
Arrays.sort(words);  // 自然排序：[apple, banana, cherry]
Arrays.sort(words, Collections.reverseOrder());  // 逆序：[cherry, banana, apple]
```

### 7.3 数组复制与转换

```java
int[] original = {1, 2, 3, 4, 5};
int[] extended = Arrays.copyOf(original, 10);  // 扩展长度：[1, 2, 3, 4, 5, 0, 0, 0, 0, 0]

List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);  // 数组转列表
int[] array = list.stream().mapToInt(i -> i).toArray();  // 列表转数组
```

### 7.4 多维数组处理

```java
int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
System.out.println(Arrays.deepToString(matrix));
// 输出：[[1, 2, 3], [4, 5, 6], [7, 8, 9]]

int[][] copy = Arrays.copyOf(matrix, matrix.length);
```

## 八、常见问题与注意事项

### 8.1 排序的稳定性

Arrays.sort() 对于对象数组使用的是 TimSort 算法，它是稳定的——相等元素的相对顺序在排序后保持不变。但对于基本类型数组，稳定性没有意义，因为基本类型没有"相等但不同"的概念。

```java
String[] arr = {"a1", "b", "a2", "c"};
Arrays.sort(arr);  // 稳定排序，a1 和 a2 的相对顺序保持
```

### 8.2 浮点数的特殊处理

浮点数（float 和 double）有一些特殊的比较行为，Arrays 类在排序时正确处理了这些情况：

```java
// -0.0f 和 0.0f 的比较
float[] arr = {-0.0f, 0.0f, Float.NaN};
Arrays.sort(arr);
// 结果：-0.0f 在 0.0f 之前，NaN 在最后
```

浮点数的排序遵循 `Float.compareTo` 定义的顺序：-0.0f 被视为小于 0.0f；NaN 被视为大于任何值，包括其自身。

### 8.3 asList 的陷阱

`Arrays.asList()` 返回的列表有几个需要注意的点：

```java
int[] intArray = {1, 2, 3};
List<int[]> list = Arrays.asList(intArray);  // 错误：得到的是 List<int[]> 而非 List<Integer>

// 正确做法
Integer[] integerArray = {1, 2, 3};
List<Integer> list = Arrays.asList(integerArray);  // 正确

// 或者使用流转换
List<Integer> list = Arrays.stream(intArray).boxed().collect(Collectors.toList());
```

### 8.4 并行排序的适用场景

`parallelSort()` 并非在所有情况下都比 `sort()` 更快。以下情况适合使用并行排序：大数组（通常大于 10000 个元素）；多核处理器环境；排序操作是性能瓶颈。

以下情况适合使用顺序排序：小数组；单核环境；排序操作不是主要性能瓶颈。

## 九、面试常见问题

### 9.1 Arrays.sort() 和 Collections.sort() 的区别？

Arrays.sort() 操作原生数组，直接在原始数组上进行排序；Collections.sort() 操作 List 接口，内部实际调用 List.sort() 或 Arrays.sort()。Arrays.sort() 对基本类型使用双轴快速排序，效率更高；Collections.sort() 对 List 排序，最终也会调用 Arrays.sort() 或使用 TimSort。

### 9.2 Arrays 类如何实现不同类型数组的排序？

Arrays 类为每种基本类型提供了独立的 sort() 方法重载，避免了装箱拆箱的开销。对于对象数组，使用 TimSort 算法并支持自定义 Comparator。这种设计保证了每种类型都能获得最优的性能。

### 9.3 parallelSort() 的原理是什么？

parallelSort() 使用 ForkJoin 框架将大数组分成多个子数组，并行排序后合并。当数组长度小于最小粒度阈值（8192）或系统是单核时，退化为顺序排序。优势在于充分利用多核处理器，适合大数组排序。

### 9.4 二分搜索的前提条件是什么？

数组必须是有序的，且排序顺序必须与搜索时使用的比较规则一致。对于基本类型数组，必须先调用 Arrays.sort()；对于对象数组，排序使用的比较器必须与搜索使用的一致。

### 9.5 Arrays.asList() 返回的列表有什么限制？

返回的列表是固定大小的，不支持 add() 或 remove() 操作；返回的列表是对原数组的视图，修改原数组会影响列表；基本类型数组会被当作单一元素处理，需要使用包装类型数组。

### 9.6 Arrays.copyOf() 和 System.arraycopy() 的区别？

Arrays.copyOf() 是高级方法，可以改变数组长度，内部使用 System.arraycopy() 进行实际复制；System.arraycopy() 是低级方法，复制速度更快，但不能改变数组长度。Arrays.copyOf() 更灵活，System.arraycopy() 更高效。

## 十、与其他类的对比

### 10.1 Arrays 与 Collections

Arrays 和 Collections 是 Java 中操作集合数据的两大工具类。Arrays 操作原生数组，Collections 操作集合框架（Collection、List、Set、Map 等）。Arrays 提供了数组特有的操作（如复制、填充、字符串表示），Collections 提供了集合特有的操作（如不可修改包装、同步包装）。两者都提供了排序和搜索功能，但针对不同的数据结构。

### 10.2 Arrays.sort() 与 List.sort()

Java 8 引入的 List.sort() 方法与 Arrays.sort() 类似，但作用于 List 接口。List.sort() 内部调用 Arrays.sort() 进行实际排序。主要区别在于：Arrays.sort() 直接修改原数组；List.sort() 直接修改原列表。两者都支持自定义 Comparator。

### 10.3 Arrays 与 Guava 的 Lists/Maps

Google Guava 库提供了更丰富的数组和集合工具：

```java
// Guava 的便利方法
int[] array = Ints.toArray(list);
List<Integer> list = Ints.asList(array);
int[] array = Ints.concat(arr1, arr2);

// Arrays 需要更多代码
int[] array = list.stream().mapToInt(Integer::intValue).toArray();
```

Guava 提供了类型安全的数组操作，避免了 Arrays 中大量的方法重载。但引入 Guava 增加了项目依赖。

### 10.4 Arrays 与 Apache Commons Lang 的 ArrayUtils

Apache Commons Lang 也提供了数组工具：

```java
// ArrayUtils 的方法
int[] array = ArrayUtils.EMPTY_INT_ARRAY;
int[] array = ArrayUtils.add(array, 1);
int[] array = ArrayUtils.removeElement(array, 1);
boolean contains = ArrayUtils.contains(array, 1);

// Arrays 需要更多代码
int[] array = new int[0];
array = Arrays.copyOf(array, array.length + 1);
array[array.length - 1] = 1;
```

Apache Commons Lang 提供了更多实用方法，但同样需要引入外部依赖。

