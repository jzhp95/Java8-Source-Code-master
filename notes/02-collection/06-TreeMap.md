# TreeMap 源码深度解析

## 一、类概述

### 1.1 基本信息

TreeMap 是 Java 集合框架中基于红黑树（Red-Black Tree）实现的 NavigableMap 接口，它维护着键值对的有序存储。与 HashMap 的无序存储不同，TreeMap 中的键按照自然顺序（实现了 Comparable 接口）或自定义比较器（Comparator）进行排序。这种特性使得 TreeMap 成为需要有序键值对场景的理想选择。

TreeMap 的核心价值在于其能够提供 O(log n) 时间复杂度的插入、删除和查找操作，同时保持键的有序性。这使得它特别适用于需要范围查询、找到最近邻元素、按顺序遍历所有元素等场景。与 LinkedHashMap 的插入顺序或访问顺序不同，TreeMap 的顺序是完全由键的比较关系决定的。

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

从类的继承结构可以看出，TreeMap 实现了 NavigableMap 接口，这意味着它提供了丰富的导航方法，如 ceilingEntry、floorEntry、higherEntry、lowerEntry 等。这些方法使得在树中查找特定键附近的元素变得非常便捷。同时，TreeMap 还实现了 Cloneable 和 Serializable 接口，支持克隆和序列化操作。

### 1.2 红黑树基础

在深入 TreeMap 源码之前，有必要先理解红黑树的基本特性。红黑树是一种自平衡的二叉搜索树，它通过在每个节点上增加一个存储位来表示节点的颜色（红色或黑色），从而保证树的基本平衡性质。红黑树必须满足以下五条性质：

第一，每个节点要么是红色，要么是黑色。第二，根节点必须是黑色。第三，所有叶子节点（NIL 节点或空节点）都是黑色。第四，每个红色节点的两个子节点都必须是黑色（这意味着红色节点不能连续出现，路径上不能有两个连续的红色节点）。第五，从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点（这保证了树的高度大致平衡）。

这五条性质的精妙设计使得红黑树的最长路径（从根到叶子的路径）不会超过最短路径的两倍，从而保证了各种操作的时间复杂度为 O(log n)。在 TreeMap 中，这些性质通过旋转和变色操作来维护，这正是 TreeMap 源码中最复杂也最精彩的部分。

红黑树相比普通的二叉搜索树，优势在于能够自动保持平衡，而不需要像 AVL 树那样严格的平衡条件。这使得红黑树在插入和删除操作时需要更少的旋转操作，从而在实际应用中具有更好的性能。TreeMap 选择红黑树作为底层数据结构，正是看中了这种平衡性与性能的完美平衡。

## 二、核心设计思想

### 2.1 有序性与性能的平衡

TreeMap 的核心设计目标是在保持键的有序性的同时，提供高效的动态操作能力。有序性意味着 TreeMap 中的键总是按照某种顺序排列，可以按照自然顺序或自定义比较器进行排序。这种有序性带来的直接好处是可以快速找到某个键的最近邻元素（ceiling、floor、higher、lower），以及对键进行范围查询（subMap）。

为了实现这种有序性，TreeMap 选择了红黑树作为底层数据结构。红黑树是一种自平衡的二叉搜索树，它通过节点颜色的约束和旋转操作来保持树的平衡。与 AVL 树相比，红黑树的平衡条件相对宽松，这使得插入和删除操作时需要调整的次数更少，从而在实际应用中具有更好的平均性能。

TreeMap 在设计上采用了委托模式，将实际的比较操作委托给 comparator 或键的 compareTo 方法。这种设计使得 TreeMap 非常灵活，可以同时支持自然排序（使用 Comparable 接口）和自定义排序（使用 Comparator 接口）。当创建 TreeMap 时没有指定比较器时，TreeMap 会假设所有键都实现了 Comparable 接口，并在比较时使用键的 compareTo 方法。

### 2.2 NavigableMap 接口的实现

TreeMap 实现了 NavigableMap 接口，这是 Java 1.6 引入的一个重要接口。NavigableMap 扩展了 SortedMap 接口，提供了丰富的导航方法，使得在有序映射中查找特定键附近元素变得非常方便。这些导航方法可以分为几个类别：

第一类是获取特定位置元素的方法，包括 firstKey、lastKey、firstEntry、lastEntry 等。这些方法用于获取映射中的最小键或最大键，以及对应的键值对。第二类是导航到特定键附近的方法，包括 ceilingKey/floorKey（不小于/不大于给定键的最小/最大键）、ceilingEntry/floorEntry（对应的键值对）、higherKey/lowerKey（大于/小于给定键的最小/最大键）、higherEntry/lowerEntry（对应的键值对）。

第三类是获取并删除边界元素的方法，包括 pollFirstEntry 和 pollLastEntry。这些方法在返回元素的同时将其从映射中删除，这在实现优先级队列等场景中非常有用。第四类是创建子映射的方法，包括 subMap、headMap、tailMap。这些方法返回原映射的视图，支持开区间、闭区间等多种边界条件。

NavigableMap 接口还提供了 descendingMap 方法，返回映射的逆序视图。这使得可以轻松地按降序遍历映射中的元素，而不需要额外的排序操作。TreeMap 对这些方法都提供了完整的实现，充分利用了红黑树的有序性特性。

### 2.3 视图与代理模式

TreeMap 中的几个核心方法（如 keySet、values、entrySet）返回的不是独立的集合对象，而是原映射的"视图"。这种设计遵循了代理模式（Proxy Pattern）的思想，通过返回一个代理对象来提供对底层数据的访问接口。这些视图与原映射共享相同的数据，因此对视图的修改会直接影响原映射，反之亦然。

这种设计的好处在于节约内存和维护数据一致性。与创建独立的数据副本不同，视图只是原数据的引用，不会在内存中复制整个映射。同时，由于视图与原数据共享底层结构，对视图的操作（如添加、删除元素）会立即反映到原数据中，这保证了数据的一致性。

然而，视图的使用也有一些需要注意的地方。首先，视图不支持添加操作（add/addAll），因为视图只是映射的一部分，没有足够的信息来确定新元素在红黑树中的正确位置。其次，在遍历视图时修改原映射会导致 ConcurrentModificationException，这与 ArrayList 的 fail-fast 机制类似。最后，某些视图操作（如 subMap 返回的视图）有额外的约束条件，不遵守这些条件会抛出 IllegalArgumentException。

## 三、继承结构与接口实现

### 3.1 类继承层次

TreeMap 的继承层次相对简单直接，它继承自 AbstractMap 抽象类，并实现了几个关键接口。AbstractMap 提供了 Map 接口的基本实现框架，包括 equals、hashCode、toString 等通用方法，以及一些简单的视图实现。TreeMap 重写了这些方法以利用红黑树的有序性特性。

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

NavigableMap 接口是 TreeMap 最重要的接口，它定义了有序映射的所有操作。NavigableMap 继承自 SortedMap 接口，而 SortedMap 又继承自 Map 接口。因此，TreeMap 实际上是一个完整的 Map 实现，同时提供了丰富的有序操作能力。

Cloneable 接口是一个标记接口，表明 TreeMap 支持克隆操作。TreeMap 实现了 clone 方法，返回一个浅拷贝的 TreeMap 实例。序列化接口 java.io.Serializable 使得 TreeMap 可以被序列化。需要注意的是，虽然 TreeMap 本身实现了 Serializable，但其键和值必须也是可序列化的，否则序列化会失败。

### 3.2 核心接口解析

NavigableMap 接口是理解 TreeMap 功能的关键。这个接口定义了大量用于导航和操作有序映射的方法，可以大致分为几个功能组。第一组是边界访问方法，用于获取映射中的最小和最大元素，包括 firstKey、firstEntry、lastKey、lastEntry 等。第二组是严格导航方法，用于找到大于或小于给定键的元素，包括 higherKey、higherEntry、lowerKey、lowerEntry。第三组是非严格导航方法，用于找到不小于或不大于给定键的元素，包括 ceilingKey、ceilingEntry、floorKey、floorEntry。

NavigableMap 还定义了获取并删除边界元素的方法：pollFirstEntry 和 pollLastEntry。这些方法在返回键值对的同时将其从映射中删除，是实现优先级队列等数据结构的理想选择。此外，NavigableMap 还定义了获取逆序映射的方法 descendingMap，以及获取各种视图的方法 navigableKeySet、descendingKeySet 等。

SortedMap 接口定义了基本的排序映射操作，包括 comparator（获取比较器）、firstKey/lastKey（获取最小/最大键）、headMap/subMap/tailMap（获取子映射）等。这些方法返回的视图遵循 SortedMap 的约定，键按照比较器的顺序排列。TreeMap 对这些方法都提供了完整的实现。

## 四、核心字段深度分析

### 4.1 私有字段详解

TreeMap 的核心字段非常简单直接，主要包括比较器、根节点、大小和修改计数。这些字段共同维护着红黑树的完整状态。

```java
private final Comparator<? super K> comparator;
private transient Entry<K,V> root;
private transient int size = 0;
private transient int modCount = 0;
```

comparator 字段存储着用于键比较的比较器对象。如果这个字段为 null，则 TreeMap 使用键的自然顺序（即键的 compareTo 方法）进行比较。这个设计使得 TreeMap 可以灵活地支持不同的排序方式。在创建 TreeMap 时，如果不指定比较器，则使用自然顺序；如果指定了比较器，则使用该比较器进行所有键的比较操作。

root 字段是红黑树的根节点，所有其他节点都是通过这个根节点可以到达的。root 为 null 表示树为空。Entry 是 TreeMap 的内部类，它包含键、值、以及指向左右子节点、父节点的指针，还有一个颜色字段。这些指针共同构成了红黑树的结构。

size 字段记录着树中键值对的数量，这个值会被 size()、isEmpty() 等方法使用，也会被各种视图类（如 EntrySet、Values）使用。modCount 字段记录着树被修改的次数，这是一个常见的用于实现 fail-fast 机制的计数器。当通过迭代器遍历视图时，如果检测到 modCount 发生了变化，就会抛出 ConcurrentModificationException。

### 4.2 Entry 内部类结构

Entry 是 TreeMap 的核心内部类，它代表红黑树中的一个节点。Entry 类的设计体现了红黑树节点的所有必要属性。

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK;
}
```

每个 Entry 包含一个键值对（key 和 value），以及指向左子节点、右子节点和父节点的指针。color 字段表示节点的颜色，初始化为 BLACK（黑色）。红黑树的性质要求根节点必须是黑色，所以在创建新节点时将其初始化为黑色是合理的。

Entry 实现了 Map.Entry 接口，这意味着它需要实现 getKey、getValue、setValue 方法。这些方法的实现非常简单，getKey 和 getValue 直接返回对应的字段，setValue 更新 value 字段并返回旧值。需要注意的是，setValue 不会自动维护红黑树的任何属性，调用者需要确保红黑树的性质不被破坏。

Entry 类被设计为 static 内部类，这意味着它不持有外部类（TreeMap）的引用。这种设计有两个好处：第一，避免了内存泄漏的风险，因为静态内部类不会隐式持有外部类的引用；第二，减少了每个 Entry 对象的大小，因为不需要额外的外部类引用字段。在 TreeMap 中，Entry 对象可能会非常多（最多可达数百万个），这种优化对于内存使用是有意义的。

## 五、构造函数全面解析

### 5.1 默认构造函数

TreeMap 提供了多个构造函数，以支持不同的初始化方式和使用场景。默认构造函数创建一个使用自然顺序的空 TreeMap。

```java
public TreeMap() {
    comparator = null;
}
```

这个构造函数非常简单，只是将 comparator 设置为 null。这表示 TreeMap 将使用键的自然顺序进行排序。使用这个构造函数的前提是所有要插入的键都必须实现 Comparable 接口，否则在插入时会抛出 ClassCastException。

自然顺序的排序遵循 Comparable 接口的约定：如果两个对象相等，根据 compareTo 方法返回 0，那么根据 equals 方法也必须返回 true。这意味着如果两个键的 compareTo 返回 0，它们会被视为相等，后插入的键值对不会覆盖先插入的（与 HashMap 的行为一致）。这个约定对于 TreeMap 的正确性至关重要。

### 5.2 带比较器的构造函数

第二个构造函数允许指定一个自定义的比较器，这个比较器将用于所有键的比较操作。

```java
public TreeMap(Comparator<? super K> comparator) {
    this.comparator = comparator;
}
```

使用自定义比较器的好处是灵活性大大增加。可以对没有实现 Comparable 接口的对象进行排序，也可以改变排序的规则（如逆序、忽略大小写等）。此外，使用自定义比较器可以插入 null 键，只要比较器允许即可（自然顺序不允许 null 键，因为 compareTo 方法会抛出 NullPointerException）。

值得注意的是，比较器可以是任意实现了 Comparator 接口的对象，包括 lambda 表达式和方法引用。例如，创建一个按字符串长度排序的 TreeMap：`new TreeMap<>(Comparator.comparingInt(String::length))`。这种灵活性使得 TreeMap 适用于各种复杂的排序场景。

### 5.3 从 Map 初始化的构造函数

第三个构造函数从一个已存在的 Map 中初始化 TreeMap。

```java
public TreeMap(Map<? extends K, ? extends V> m) {
    comparator = null;
    putAll(m);
}
```

这个构造函数首先将 comparator 设置为 null（使用自然顺序），然后调用 putAll 方法将 Map 中的所有键值对插入到 TreeMap 中。putAll 方法会逐个调用 put 方法来插入元素，这保证了所有键值对都会被正确地插入到红黑树中的正确位置。

如果 Map 很大，这个构造函数的效率可能不高，因为它需要执行 n 次插入操作，每次插入的时间复杂度为 O(log n)，总时间复杂度为 O(n log n)。相比之下，如果有一个已排序的 Map，使用 SortedMap 构造函数会更高效。

### 5.4 从 SortedMap 初始化的构造函数

第四个构造函数从一个已排序的 SortedMap 中初始化 TreeMap。

```java
public TreeMap(SortedMap<K, ? extends V> m) {
    comparator = m.comparator();
    try {
        buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
    } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
    }
}
```

这个构造函数是最高效的初始化方式，因为它利用了源 SortedMap 已经排序的特性。buildFromSorted 方法是一个高效的构建算法，它接受一个已排序的键序列，并构建一棵平衡的红黑树，而不是逐个插入。

buildFromSorted 方法使用分治策略来构建平衡的红黑树。它首先找到中间元素作为根节点，然后递归地构建左子树和右子树，最后将子树连接到根节点。这种方法构建出的红黑树是高度平衡的，所有操作的时间复杂度都是最优的 O(log n)。

这个构造函数还保留了源 SortedMap 的比较器，确保新 TreeMap 使用相同的排序规则。如果源 SortedMap 是 TreeMap 或其子类，这个构造函数会复用相同的比较器；如果源 SortedMap 是其他实现，只要它提供了 comparator() 方法，这个构造函数就能正常工作。

## 六、核心方法深度剖析

### 6.1 getEntry 方法族

getEntry 方法是 TreeMap 中最核心的查找方法，它根据键返回对应的 Entry 对象。TreeMap 提供了多个变体来处理不同的场景。

```java
final Entry<K,V> getEntry(Object key) {
    if (comparator != null)
        return getEntryUsingComparator(key);
    if (key == null)
        throw new NullPointerException();
    @SuppressWarnings("unchecked")
    Comparable<? super K> k = (Comparable<? super K>) key;
    Entry<K,V> p = root;
    while (p != null) {
        int cmp = k.compareTo(p.key);
        if (cmp < 0)
            p = p.left;
        else if (cmp > 0)
            p = p.right;
        else
            return p;
    }
    return null;
}
```

getEntry 方法首先检查是否存在自定义比较器。如果存在，就使用 getEntryUsingComparator 方法；否则，使用自然顺序进行查找。在自然顺序查找中，使用 key 的 compareTo 方法与树中节点的 key 进行比较，根据比较结果决定向左子树、右子树移动，还是找到了目标节点。

由于红黑树是一棵二叉搜索树，这个查找过程的时间复杂度是 O(h)，其中 h 是树的高度。由于红黑树是平衡的，h 大约是 log2(n)，所以查找效率很高。如果查找失败（键不存在），方法返回 null；如果查找成功，返回对应的 Entry 对象。

getEntryUsingComparator 方法与 getEntry 类似，但使用自定义比较器进行比较。这种分离设计避免了在每次比较时都进行类型检查，提高了效率。当没有自定义比较器时，不需要在每次比较时都检查 comparator 是否为 null。

```java
final Entry<K,V> getEntryUsingComparator(Object key) {
    @SuppressWarnings("unchecked")
    K k = (K) key;
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = cpr.compare(k, p.key);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else
                return p;
        }
    }
    return null;
}
```

### 6.2 导航方法族

TreeMap 提供了一系列导航方法，用于查找特定键附近（不小于、不大于、大于、小于）的元素。这些方法是 NavigableMap 接口的核心功能。

```java
final Entry<K,V> getCeilingEntry(K key) {
    Entry<K,V> p = root;
    while (p != null) {
        int cmp = compare(key, p.key);
        if (cmp < 0) {
            if (p.left != null)
                p = p.left;
            else
                return p;
        } else if (cmp > 0) {
            if (p.right != null)
                p = p.right;
            else {
                Entry<K,V> parent = p.parent;
                Entry<K,V> ch = p;
                while (parent != null && ch == parent.right) {
                    ch = parent;
                    parent = parent.parent;
                }
                return parent;
            }
        } else
            return p;
    }
    return null;
}
```

getCeilingEntry 方法查找不小于给定键的最小 Entry（中文常译为"上界"或"天花板"）。查找过程从根节点开始，沿着搜索路径向下遍历。当比较结果小于 0 时，说明目标键小于当前节点的键，如果左子树存在，就进入左子树继续查找。当比较结果大于 0 时，说明目标键大于当前节点的键，需要向右上寻找：当向右子树移动后，如果右子树为空，说明当前节点就是目标位置，否则继续向右遍历。

这个算法精妙之处在于它同时维护了两条信息：向下搜索的路径和回溯的路径。当向右移动后遇到空指针时，需要向上回溯找到第一个向右转的位置，那个节点就是不小于目标键的最小节点。

getFloorEntry 方法与 getCeilingEntry 相反，查找不大于给定键的最大节点（中文常译为"下界"或"地板"）。其逻辑是对称的：比较结果大于 0 时进入右子树，比较结果小于 0 时需要向左上回溯。

```java
final Entry<K,V> getFloorEntry(K key) {
    Entry<K,V> p = root;
    while (p != null) {
        int cmp = compare(key, p.key);
        if (cmp > 0) {
            if (p.right != null)
                p = p.right;
            else
                return p;
        } else if (cmp < 0) {
            if (p.left != null)
                p = p.left;
            else {
                Entry<K,V> parent = p.parent;
                Entry<K,V> ch = p;
                while (parent != null && ch == parent.left) {
                    ch = parent;
                    parent = parent.parent;
                }
                return parent;
            }
        } else
            return p;
    }
    return null;
}
```

getHigherEntry 和 getLowerEntry 方法分别查找大于和小于给定键的节点。与 ceiling/floor 不同，higher/lower 是严格比较，不包括等于的情况。这意味着如果树中正好存在与给定键相等的节点，higher 会返回比它大的最小节点，而 ceiling 会返回它本身。

### 6.3 put 方法详解

put 方法负责将键值对插入到红黑树中，这是 TreeMap 中最复杂的操作之一，因为它不仅要在正确位置插入新节点，还要维护红黑树的性质。

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
        @SuppressWarnings("unchecked")
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

put 方法的执行过程可以分为以下几个步骤。第一步，检查树是否为空。如果为空，直接创建根节点并返回。这里调用 compare(key, key) 是为了提前触发可能的 ClassCastException 或 NullPointerException，这在某些场景下比插入后再抛出异常更友好。

第二步，在树中找到新节点的插入位置。这个过程与 getEntry 类似，沿着搜索路径向下移动，但记录父节点的位置。当找到一个空位置时，那就是新节点的插入位置。如果在移动过程中发现相同的键，则更新值并返回旧值。

第三步，创建新节点并插入到正确位置。新节点被初始化为红色（因为红色节点不影响路径上的黑色节点数量，这简化了平衡操作）。根据比较结果将新节点链接为父节点的左子节点或右子节点。

第四步，调用 fixAfterInsertion 方法修复插入后可能违反的红黑树性质。这是红黑树操作中最复杂的部分，需要通过旋转和变色来恢复平衡。

### 6.4 deleteEntry 方法详解

deleteEntry 方法负责从红黑树中删除一个节点，同时维护红黑树的性质。这个方法同样非常复杂，因为它需要处理多种情况。

```java
private void deleteEntry(Entry<K,V> p) {
    modCount++;
    size--;
    if (p.left != null && p.right != null) {
        Entry<K,V> s = successor(p);
        p.key = s.key;
        p.value = s.value;
        p = s;
    }
    Entry<K,V> replacement = (p.left != null) ? p.left : p.right;
    if (replacement != null) {
        replacement.parent = p.parent;
        if (p.parent == null)
            root = replacement;
        else if (p == p.parent.left)
            p.parent.left = replacement;
        else
            p.parent.right = replacement;
        p.left = p.right = p.parent = null;
        if (p.color == BLACK)
            fixAfterDeletion(replacement);
    } else if (p.parent == null) {
        root = null;
    } else {
        if (p.color == BLACK)
            fixAfterDeletion(p);
        if (p.parent != null) {
            if (p == p.parent.left)
                p.parent.left = null;
            else
                p.parent.right = null;
            p.parent = null;
        }
    }
}
```

deleteEntry 的删除策略取决于被删除节点的子节点数量。如果被删除节点有两个子节点，需要找到它的后继节点（successor），用后继节点的值替换被删除节点的值，然后删除后继节点。这是因为后继节点最多只有一个子节点（因为它是中序遍历中的下一个节点），删除它比直接删除原节点更简单。

如果被删除节点只有一个子节点或没有子节点，直接删除它并用子节点（如果有）替换它即可。最复杂的情况是被删除节点是黑色节点且没有子节点（或者只有黑色子节点），这会破坏红黑树的性质，需要调用 fixAfterDeletion 方法进行修复。

fixAfterDeletion 方法通过旋转和变色来恢复红黑树的性质。与 fixAfterInsertion 类似，这个方法也有多种情况需要处理，每种情况对应一种特定的颜色配置和子树结构。

## 七、红黑树平衡操作

### 7.1 旋转操作

红黑树的平衡操作主要依靠两种旋转：左旋（rotateLeft）和右旋（rotateRight）。旋转是一种局部操作，它改变树的结构但不改变中序遍历的顺序，因此保持了二叉搜索树的性质。

```java
private void rotateLeft(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> r = p.right;
        p.right = r.left;
        if (r.left != null)
            r.left.parent = p;
        r.parent = p.parent;
        if (p.parent == null)
            root = r;
        else if (p.parent.left == p)
            p.parent.left = r;
        else
            p.parent.right = r;
        r.left = p;
        p.parent = r;
    }
}
```

左旋操作将节点 p 旋转为其右子节点 r 的位置。在旋转过程中，p 的右子节点变为 r 的左子节点，r 成为 p 的父节点。左旋通常用于处理右子树过高的情况，使右子树的高度降低，左子树的高度增加。

旋转操作的关键是正确更新所有涉及的父指针和子指针。每个节点都有左子节点、右子节点和父节点三个指针，旋转时需要正确更新这九个指针（左子节点的父指针、右子节点的父指针、两个节点的父指针、以及它们之间的父子关系）。

```java
private void rotateRight(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> l = p.left;
        p.left = l.right;
        if (l.right != null)
            l.right.parent = p;
        l.parent = p.parent;
        if (p.parent == null)
            root = l;
        else if (p.parent.right == p)
            p.parent.right = l;
        else
            p.parent.left = l;
        l.right = p;
        p.parent = l;
    }
}
```

右旋操作是左旋的镜像，将节点 p 旋转为其左子节点 l 的位置。右旋通常用于处理左子树过高的情况，使左子树的高度降低，右子树的高度增加。

旋转操作的时间复杂度是 O(1)，因为它只涉及常数个节点的指针更新。这保证了红黑树的插入和删除操作的时间复杂度保持 O(log n)。

### 7.2 fixAfterInsertion 详解

fixAfterInsertion 方法在插入新节点后被调用，负责修复可能违反的红黑树性质。新插入的节点被初始化为红色，这可能会导致两种问题：根节点变成红色，或者出现两个连续的红色节点。

```java
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;
    while (x != null && x != root && x.parent.color == RED) {
        if (parentOf(x) == parentOf(parentOf(x))) {
            Entry<K,V> y = parentOf(x).right;
            if (colorOf(y) == RED) {
                setColor(parentOf(x), BLACK);
                setColor(y, BLACK);
                x = parentOf(x);
            } else {
                if (x == parentOf(x).right) {
                    x = parentOf(x);
                    rotateLeft(x);
                }
                setColor(parentOf(x), BLACK);
                setColor(parentOf(x), BLACK);
                rotateRight(parentOf(x));
            }
        } else {
            Entry<K,V> y = parentOf(x).left;
            if (colorOf(y) == RED) {
                setColor(parentOf(x), BLACK);
                setColor(y, BLACK);
                x = parentOf(x);
            } else {
                if (x == parentOf(x).left) {
                    x = parentOf(x);
                    rotateRight(x);
                }
                setColor(parentOf(x), BLACK);
                setColor(parentOf(x), BLACK);
                rotateLeft(parentOf(x));
            }
        }
    }
    root.color = BLACK;
}
```

这个方法使用了一个 while 循环来处理连续红色节点的问题。循环的条件是当前节点不是根节点且父节点是红色（这意味着出现了两个连续的红色节点，违反了红黑树的性质）。

处理逻辑分为两种主要情况，取决于父节点是祖父节点的左子节点还是右子节点。以父节点是左子节点为例，处理逻辑如下：如果叔节点（父节点的兄弟节点）是红色的，将父节点和叔节点都涂黑，然后将当前节点上移到祖父节点（涂红祖父节点继续向上处理）。如果叔节点是黑色的，且当前节点是右子节点，将当前节点上移到父节点并左旋；然后将父节点涂黑，祖父节点涂红并右旋。

这个算法确保每次迭代后，当前节点在树中的位置都会上移（要么通过颜色变化，要么通过旋转），因此循环最多执行 O(log n) 次，保证了整个操作的时间复杂度为 O(log n)。

### 7.3 fixAfterDeletion 详解

fixAfterDeletion 方法在删除节点后被调用，负责修复可能违反的红黑树性质。删除操作比插入操作更复杂，因为它需要处理更多的情况。当删除一个黑色节点时，从根到该节点的路径上的黑色节点数量会减少一，这需要通过变色和旋转来补偿。

```java
private void fixAfterDeletion(Entry<K,V> x) {
    while (x != null && colorOf(x) == BLACK) {
        if (x == leftOf(parentOf(x))) {
            Entry<K,V> sib = rightOf(parentOf(x));
            if (colorOf(sib) == RED) {
                setColor(sib, BLACK);
                setColor(parentOf(x), RED);
                rotateLeft(parentOf(x));
                sib = rightOf(parentOf(x));
            }
            if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                setColor(sib, RED);
                x = parentOf(x);
            } else {
                if (colorOf(rightOf(sib)) == BLACK) {
                    setColor(leftOf(sib), BLACK);
                    setColor(sib, RED);
                    rotateRight(sib);
                    sib = rightOf(parentOf(x));
                }
                setColor(sib, colorOf(parentOf(x)));
                setColor(parentOf(x), BLACK);
                setColor(rightOf(sib), BLACK);
                rotateLeft(parentOf(x));
                x = root;
            }
        } else {
            Entry<K,V> sib = leftOf(parentOf(x));
            if (colorOf(sib) == RED) {
                setColor(sib, BLACK);
                setColor(parentOf(x), RED);
                rotateRight(parentOf(x));
                sib = leftOf(parentOf(x));
            }
            if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                setColor(sib, RED);
                x = parentOf(x);
            } else {
                if (colorOf(leftOf(sib)) == BLACK) {
                    setColor(rightOf(sib), BLACK);
                    setColor(sib, RED);
                    rotateLeft(sib);
                    sib = leftOf(parentOf(x));
                }
                setColor(sib, colorOf(parentOf(x)));
                setColor(parentOf(x), BLACK);
                setColor(leftOf(sib), BLACK);
                rotateRight(parentOf(x));
                x = root;
            }
        }
    }
    setColor(x, BLACK);
}
```

fixAfterDeletion 的处理逻辑比 fixAfterInsertion 更复杂，因为它需要考虑更多的情况。基本思路是：如果被删除节点的替代节点是红色，将其涂黑即可解决问题（相当于用红色节点来"填补"黑色节点减少的空缺）。如果替代节点是黑色，需要进行一系列的变色和旋转操作来修复。

处理逻辑同样分为左右两种情况（取决于 x 是父节点的左子节点还是右子节点）。以 x 是左子节点为例，首先检查兄弟节点 sib 的颜色。如果兄弟节点是红色，将其涂黑，父节点涂红，然后左旋父节点，这会将情况转化为兄弟节点是黑色的情况。

如果兄弟节点是黑色，需要进一步检查兄弟节点的两个子节点。如果两个子节点都是黑色，将兄弟节点涂红并将问题上移到父节点。如果右子节点是黑色（左子节点可能是红色），将左子节点涂黑，兄弟节点涂红，然后右旋兄弟节点。最后处理右子节点是红色的情况，这是最复杂的情况，需要进行颜色调整和旋转操作。

### 7.4 successor 方法

successor 方法返回给定节点的中序后继节点，这是删除操作中寻找替代节点的关键方法。

```java
static <K,V> TreeMap.Entry<K,V> successor(Entry<K,V> t) {
    if (t == null)
        return null;
    else if (t.right != null) {
        Entry<K,V> p = t.right;
        while (p.left != null)
            p = p.left;
        return p;
    } else {
        Entry<K,V> p = t.parent;
        Entry<K,V> ch = t;
        while (p != null && ch == p.right) {
            ch = p;
            p = p.parent;
        }
        return p;
    }
}
```

中序后继节点的查找分为两种情况。如果给定节点有右子节点，后继节点就是右子树中的最左节点（即右子树中键最小的节点）。这是因为在中序遍历中，一个节点的直接后继应该是它在有序序列中的下一个元素，而右子树中的所有节点都大于当前节点，所以后继在右子树中。

如果给定节点没有右子节点，后继节点需要向上寻找。沿着父节点指针向上移动，直到找到一个节点是其父节点的左子节点的那个节点，那个父节点就是后继节点。如果一直上升到根节点都没有找到这样的节点，说明给定节点是树中的最大节点，没有后继。

## 八、视图类与迭代器

### 8.1 Values 内部类

Values 类是 TreeMap 的值集合视图，它提供了对 TreeMap 中所有值的遍历能力。

```java
class Values extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return new ValueIterator(getFirstEntry());
    }

    public int size() {
        return TreeMap.this.size();
    }

    public boolean contains(Object o) {
        return TreeMap.this.containsValue(o);
    }

    public boolean remove(Object o) {
        for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
            if (valEquals(e.getValue(), o)) {
                deleteEntry(e);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        TreeMap.this.clear();
    }
}
```

Values 类继承自 AbstractCollection，提供了集合的基本操作。iterator() 方法返回一个 ValueIterator，它按升序遍历所有值（因为 TreeMap 的键是有序的，值也按相应顺序出现）。size() 方法直接委托给 TreeMap 的 size 字段。

remove 方法的实现比较特殊，它需要遍历整个树来查找要删除的值。这与 HashMap 的 values().remove() 不同，因为 TreeMap 的值不是按键索引的，无法直接定位到包含特定值的节点。这意味着 Values.remove() 操作的时间复杂度是 O(n)，而不是 O(1)。

### 8.2 EntrySet 内部类

EntrySet 类是 TreeMap 的键值对集合视图，它实现了对映射中所有条目的操作。

```java
class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator(getFirstEntry());
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
        Object value = entry.getValue();
        Entry<K,V> p = getEntry(entry.getKey());
        return p != null && valEquals(p.getValue(), value);
    }

    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
        Object value = entry.getValue();
        Entry<K,V> p = getEntry(entry.getKey());
        if (p != null && valEquals(p.getValue(), value)) {
            deleteEntry(p);
            return true;
        }
        return false;
    }

    public int size() {
        return TreeMap.this.size();
    }

    public void clear() {
        TreeMap.this.clear();
    }
}
```

EntrySet 的 contains 方法首先检查对象是否是 Map.Entry 类型，然后通过 getEntry 方法快速定位到对应的键，最后比较值是否相等。这个操作的时间复杂度是 O(log n)，因为 getEntry 是基于红黑树的查找。

remove 方法的实现同样首先定位到对应的 Entry，然后调用 deleteEntry 方法将其删除。这个操作的时间复杂度也是 O(log n)，包括查找和删除两个步骤。

### 8.3 KeySet 内部类

KeySet 类是 TreeMap 的键集合视图，它比 Values 和 EntrySet 更复杂，因为它实现了 NavigableSet 接口。

```java
static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final NavigableMap<E, ?> m;
    KeySet(NavigableMap<E,?> map) { m = map; }

    public Iterator<E> iterator() {
        if (m instanceof TreeMap)
            return ((TreeMap<E,?>)m).keyIterator();
        else
            return ((TreeMap.NavigableSubMap<E,?>)m).keyIterator();
    }

    public Iterator<E> descendingIterator() {
        if (m instanceof TreeMap)
            return ((TreeMap<E,?>)m).descendingKeyIterator();
        else
            return ((TreeMap.NavigableSubMap<E,?>)m).descendingKeyIterator();
    }
    // ... 更多方法
}
```

KeySet 被实现为静态内部类，这是为了让它可以被 SubMap 复用（SubMap 是 NavigableMap 的非静态内部类）。KeySet 持有一个对 NavigableMap 的引用，这个引用可能是 TreeMap 本身，也可能是某个 SubMap。

KeySet 实现了 NavigableSet 接口，提供了丰富的导航方法，如 lower、floor、ceiling、higher、pollFirst、pollLast 等。这些方法都委托给底层的 NavigableMap 来实现，充分利用了红黑树的有序性特性。

### 8.4 迭代器实现

TreeMap 提供了多种迭代器来支持不同的遍历需求。所有迭代器都继承自 PrivateEntryIterator 基类，这个基类提供了基本的迭代器功能。

```java
abstract class PrivateEntryIterator<T> implements Iterator<T> {
    Entry<K,V> next;
    Entry<K,V> lastReturned;
    int expectedModCount;

    PrivateEntryIterator(Entry<K,V> first) {
        expectedModCount = modCount;
        lastReturned = null;
        next = first;
    }

    public final boolean hasNext() {
        return next != null;
    }

    final Entry<K,V> nextEntry() {
        Entry<K,V> e = next;
        if (e == null)
            throw new NoSuchElementException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        next = successor(e);
        lastReturned = e;
        return e;
    }

    final Entry<K,V> prevEntry() {
        Entry<K,V> e = next;
        if (e == null)
            throw new NoSuchElementException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        next = predecessor(e);
        lastReturned = e;
        return e;
    }
}
```

PrivateEntryIterator 实现了 fail-fast 机制，通过比较 modCount 和 expectedModCount 来检测并发修改。当检测到修改时，抛出 ConcurrentModificationException。

next 方法通过调用 successor 方法来获取下一个节点，这保证了迭代按照中序遍历的顺序进行。prev 方法通过调用 predecessor 方法来获取前一个节点，这支持了降序迭代器的实现。

TreeMap 提供了四种具体的迭代器：EntryIterator（遍历 Entry）、ValueIterator（遍历值）、KeyIterator（遍历键）和 DescendingKeyIterator（降序遍历键）。每种迭代器都继承自 PrivateEntryIterator，并实现了相应的 next 方法来返回正确类型的元素。

## 九、子映射与视图

### 9.1 AscendingSubMap 与 DescendingSubMap

TreeMap 通过 AscendingSubMap 和 DescendingSubMap 两个内部类来实现子映射视图。这两个类分别代表升序和降序的子映射。

```java
static final class AscendingSubMap<K,V> extends NavigableSubMap<K,V> {
    // ... 实现细节
}

static final class DescendingSubMap<K,V> extends NavigableSubMap<K,V> {
    // ... 实现细节
}
```

这些子映射类继承自 NavigableSubMap，后者是一个抽象类，提供了子映射的基本实现。子映射的特点是它们不是独立的数据结构，而是原 TreeMap 的"视图"。对子映射的修改会直接影响原 TreeMap，反之亦然。

subMap、headMap、tailMap 方法都返回相应的子映射视图。例如，subMap(fromKey, toKey) 返回键在 [fromKey, toKey) 范围内的子视图。这些方法的时间复杂度是 O(1)，因为只是创建了一个新的视图对象，而不是复制数据。

### 9.2 视图约束条件

使用 TreeMap 的子视图时需要遵守一些约束条件，否则会抛出 IllegalArgumentException。这些约束确保了视图的行为与原映射保持一致。

第一，视图的范围必须在原映射的有效范围内。如果试图创建一个超出原映射范围的视图，会抛出 IllegalArgumentException。第二，视图的边界必须满足排序要求。例如，subMap(fromKey, toKey) 要求 fromKey 不大于 toKey，否则会抛出异常。第三，对视图的修改必须满足视图的范围约束。例如，不能向 subMap 中添加超出范围的新键。

这些约束在视图类的方法实现中进行检查。例如，在 AscendingSubMap 的任何添加操作中，都会检查新键是否在允许的范围内。如果不在范围内，会抛出 IllegalArgumentException。

### 9.3 导航方法在视图中的应用

子视图同样支持 NavigableMap 接口定义的所有导航方法，如 ceilingEntry、floorEntry 等。这些方法在视图中的行为与在原 TreeMap 中类似，但结果会被限制在视图的范围内。

例如，在 TreeMap 上调用 headMap(toKey) 返回一个包含所有键小于 toKey 的子视图。在这个子视图上调用 lastEntry() 会返回子视图中最大的键值对（即原 TreeMap 中小于 toKey 的最大键值对）。这种设计使得用户可以方便地对子范围进行各种操作，而不需要额外的边界检查代码。

## 十、Java 8 新增方法

### 10.1 replace 方法

Java 8 为 Map 接口添加了新的 replace 方法，TreeMap 实现了这些方法。

```java
@Override
public boolean replace(K key, V oldValue, V newValue) {
    Entry<K,V> p = getEntry(key);
    if (p!=null && Objects.equals(oldValue, p.value)) {
        p.value = newValue;
        return true;
    }
    return false;
}

@Override
public V replace(K key, V value) {
    Entry<K,V> p = getEntry(key);
    if (p!=null) {
        V oldValue = p.value;
        p.value = value;
        return oldValue;
    }
    return null;
}
```

replace(K key, V oldValue, V newValue) 方法只有在键存在且旧值匹配时才进行替换。这是一种条件更新操作，适用于需要原子性地检查和更新的场景。replace(K key, V value) 方法更简单，直接替换键对应的值，如果键不存在则什么也不做。

这些方法相比传统的 get-put 模式更高效和安全，因为它们是原子操作，不会出现竞态条件。在并发场景下，如果使用 ConcurrentHashMap 的视图，应该使用其原子性的 replace 方法。

### 10.2 forEach 方法

Java 8 引入了 forEach 方法来支持函数式风格的遍历。

```java
@Override
public void forEach(BiConsumer<? super K, ? super V> action) {
    Objects.requireNonNull(action);
    int expectedModCount = modCount;
    for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
        action.accept(e.key, e.value);

        if (expectedModCount != modCount) {
            throw new ConcurrentModificationException();
        }
    }
}
```

forEach 方法遍历映射中的所有键值对，并对每个键值对调用提供的 BiConsumer。与传统的迭代器遍历相比，forEach 方法更简洁，代码可读性更好。实现中同样包含了 fail-fast 检查，在遍历过程中如果映射被修改会抛出异常。

### 10.3 replaceAll 方法

replaceAll 方法允许对映射中的所有值进行替换。

```java
@Override
public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Objects.requireNonNull(function);
    int expectedModCount = modCount;

    for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
        e.value = function.apply(e.key, e.value);

        if (expectedModCount != modCount) {
            throw new ConcurrentModificationException();
        }
    }
}
```

replaceAll 接受一个 BiFunction，该函数接收键和旧值作为参数，返回新值。这个方法遍历所有条目并更新值。与 forEach 类似，这个方法也包含 fail-fast 检查。

需要注意的是，replaceAll 只能修改值，不能修改键。如果需要修改键，需要先删除旧条目再插入新条目。

## 十一、常见问题与面试题

### 11.1 红黑树相关问题

**红黑树有哪些性质？**

红黑树必须满足以下五条性质：第一，每个节点要么是红色，要么是黑色。第二，根节点是黑色。第三，所有叶子节点（NIL 节点）都是黑色。第四，每个红色节点的两个子节点都是黑色（不能有两个连续的红色节点）。第五，从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点。这些性质共同保证了红黑树的高度大致为 O(log n)。

**红黑树相比 AVL 树有什么优缺点？**

红黑树的优势在于插入和删除操作通常需要较少的旋转操作，因此在频繁插入和删除的场景中性能更好。红黑树的平衡条件相对宽松，允许左右子树的高度差达到两倍。AVL 树的优势在于查找操作更快，因为 AVL 树是严格平衡的，任何节点的两棵子树高度差不超过一。在 Java 的 TreeMap 和 C++ 的 std::map 中选择红黑树而不是 AVL 树，正是因为实际应用中插入和删除的频率通常高于查找。

**TreeMap 的 put 操作的时间复杂度是多少？**

TreeMap 的 put 操作时间复杂度是 O(log n)，其中 n 是映射中的元素数量。这个复杂度来自两个方面：查找插入位置需要 O(log n) 时间（因为红黑树的高度是 O(log n)），修复红黑树性质（最多两次旋转）需要 O(1) 时间。因此总时间复杂度是 O(log n)。

### 11.2 TreeMap 与其他 Map 的比较

**TreeMap 和 HashMap 有什么区别？**

TreeMap 和 HashMap 是两种完全不同的 Map 实现。HashMap 基于哈希表实现，提供 O(1) 平均时间复杂度的查找和插入，但不保证任何顺序。TreeMap 基于红黑树实现，提供 O(log n) 时间复杂度的查找和插入，但保持键的有序性。HashMap 允许 null 键和 null 值，TreeMap 只能有 null 值，键不能为 null（除非使用允许 null 的 Comparator）。在选择时，如果需要有序遍历或范围查询，使用 TreeMap；如果只需要快速查找，使用 HashMap。

**TreeMap 和 LinkedHashMap 有什么区别？**

LinkedHashMap 继承自 HashMap，使用哈希表和双向链表来维护插入顺序或访问顺序。TreeMap 使用红黑树来维护键的自然顺序或比较器定义的顺序。LinkedHashMap 保持的是元素的插入顺序或访问顺序，而 TreeMap 保持的是键的比较顺序。例如，如果插入 {3,1,4,1,5,9,2,6}，LinkedHashMap 会保持这个插入顺序，而 TreeMap 会按自然顺序排列为 {1,2,3,4,5,6,9}。

**如何选择使用哪种 Map 实现？**

选择 Map 实现主要考虑以下因素：如果需要快速查找（O(1)），选择 HashMap；如果需要保持顺序，选择 LinkedHashMap 或 TreeMap；如果需要有序遍历或范围查询，选择 TreeMap；如果需要线程安全，选择 ConcurrentHashMap 或 Collections.synchronizedMap 包装的 Map；如果需要实现 LRU 缓存，选择 LinkedHashMap。

### 11.3 TreeMap 内部机制问题

**TreeMap 是如何保持平衡的？**

TreeMap 通过红黑树的数据结构特性来保持平衡。每次插入或删除节点后，会调用 fixAfterInsertion 或 fixAfterDeletion 方法来检查和修复红黑树的性质。这些方法通过旋转（左旋、右旋）和变色操作来调整树的结构。旋转操作改变树的结构但不改变中序遍历的顺序，变色操作调整节点的颜色以满足红黑树的性质要求。

**TreeMap 的 comparator 有什么作用？**

comparator 是 TreeMap 用于比较键的对象。如果创建 TreeMap 时指定了 comparator，则使用该 comparator 进行所有键的比较；如果没有指定，则使用键的自然顺序（键必须实现 Comparable 接口）。comparator 决定了 TreeMap 中键的排序方式，也决定了哪些键被认为是相等的（compareTo/compare 返回 0 的键被视为相等）。

**TreeMap 的迭代是 fail-fast 的吗？**

是的，TreeMap 的迭代器是 fail-fast 的。当通过迭代器遍历映射时，如果在遍历过程中对映射进行结构性修改（插入或删除），迭代器会抛出 ConcurrentModificationException。这与 ArrayList 等集合的行为一致。fail-fast 机制通过 modCount 计数器来实现，每次结构性修改都会增加 modCount，迭代器会检查 expectedModCount 是否与 modCount 匹配。

### 11.4 实际应用问题

**如何用 TreeMap 实现范围查询？**

TreeMap 的 subMap 方法可以高效地实现范围查询。例如，查找所有年龄在 18 到 65 之间的人：

```java
TreeMap<Integer, String> peopleByAge = new TreeMap<>();
peopleByAge.put(25, "张三");
peopleByAge.put(30, "李四");
peopleByAge.put(18, "王五");
peopleByAge.put(65, "赵六");
peopleByAge.put(10, "钱七");

NavigableMap<Integer, String> adults = peopleByAge.subMap(18, true, 65, false);
for (Map.Entry<Integer, String> entry : adults.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

这个方法的时间复杂度是 O(log n + k)，其中 n 是总元素数，k 是返回的元素数量。

**如何用 TreeMap 实现优先级队列？**

TreeMap 可以用来实现一个简单的优先级队列，利用其有序性来维护优先级顺序：

```java
class PriorityQueue<K, V> {
    private final TreeMap<K, List<V>> queue = new TreeMap<>();
    
    public void add(K priority, V value) {
        queue.computeIfAbsent(priority, k -> new ArrayList<>()).add(value);
    }
    
    public V poll() {
        if (queue.isEmpty()) return null;
        Map.Entry<K, List<V>> first = queue.firstEntry();
        List<V> list = first.getValue();
        V value = list.remove(list.size() - 1);
        if (list.isEmpty()) {
            queue.pollFirstEntry();
        }
        return value;
    }
}
```

这个实现保持了优先级的有序性，最高优先级（最小键）的元素会被最先取出。

**TreeMap 可以存储多少元素？**

TreeMap 理论上可以存储 Integer.MAX_VALUE 个元素，但实际上受限于可用内存。每个 Entry 对象需要一定的内存空间，包括键、值、三个指针和一个颜色标志。对于一个存储 n 个元素的 TreeMap，内存使用量大约是 O(n)。在 64 位 JVM 上，每个 Entry 对象大约需要 40-48 字节，加上键和值的开销。因此，在处理大规模数据时，需要考虑内存限制。

## 十二、应用场景与最佳实践

### 12.1 适用场景

TreeMap 在以下场景中表现优异：

第一，需要按键有序遍历的场景。当需要按顺序处理所有键值对时，TreeMap 提供了天然的有序遍历能力。相比其他 Map 实现，TreeMap 不需要在使用前进行排序，这节省了 O(n log n) 的排序时间。

第二，需要进行范围查询的场景。subMap 方法可以高效地获取指定范围内的所有元素，时间复杂度是 O(log n + k)，其中 k 是返回的元素数量。这在实现数据库查询、分页等功能时非常有用。

第三，需要找到最近邻元素的场景。TreeMap 提供了 ceiling、floor、higher、lower 等方法，可以快速找到不小于、不大于、大于或小于给定键的元素。这在实现推荐系统、范围搜索等功能时非常有用。

第四，需要维护排序统计信息的场景。firstKey、lastKey 方法可以快速获取最小和最大键，这对于维护统计信息（如价格范围、分数区间）非常方便。

### 12.2 使用注意事项

在使用 TreeMap 时需要注意以下几点：

第一，键必须可比较。如果使用自然顺序，键必须实现 Comparable 接口；如果使用自定义比较器，需要确保比较器能够正确处理所有可能的键。特别注意 null 键的处理：自然顺序不允许 null 键，自定义比较器可能允许或不允许 null 键。

第二，注意性能权衡。虽然 TreeMap 的操作都是 O(log n)，但这个 n 是树的高度，而树的高度取决于元素的分布。如果元素分布不均匀，树可能不够平衡，导致性能下降。在大多数情况下，红黑树的平衡机制能够保证良好的性能。

第三，理解视图的语义。subMap、headMap、tailMap 返回的是原映射的视图，而不是副本。对视图的修改会影响原映射，反之亦然。理解这一点对于正确使用 TreeMap 很重要。

第四，谨慎处理异常。在遍历映射时修改其结构会导致 ConcurrentModificationException。如果需要在遍历时删除元素，应该使用迭代器的 remove 方法，而不是映射的 remove 方法。

### 12.3 性能优化建议

为了最大化 TreeMap 的性能，可以考虑以下优化建议：

第一，预估容量。虽然 TreeMap 不像 HashMap 那样有显式的容量和负载因子，但保持元素分布均匀有助于树的平衡。如果可能，避免插入大量有序数据后再插入大量随机数据。

第二，选择合适的比较器。比较器的实现应该尽可能高效，因为每次查找、插入、删除都会调用比较器。避免在比较器中执行复杂的计算或调用可能抛出异常的方法。

第三，避免频繁的范围查询。如果需要进行大量的小范围查询，考虑使用多个 TreeMap 来组织数据，而不是在一个大 TreeMap 上进行多次查询。

第四，考虑使用专门的库。对于特定的场景（如范围树、区间树），可能有更专业的库提供更好的性能。TreeMap 是一个通用的有序映射实现，对于特定场景可能不是最优选择。

## 十三、总结

TreeMap 是 Java 集合框架中功能最丰富的 Map 实现之一。它基于红黑树这一经典的数据结构，提供了 O(log n) 时间复杂度的插入、删除和查找操作，同时保持键的有序性。TreeMap 实现了 NavigableMap 接口，提供了丰富的导航方法，包括 ceiling、floor、higher、lower 等，以及子映射操作。

红黑树的设计是 TreeMap 的核心。通过旋转和变色操作，红黑树在插入和删除时能够自动保持平衡，确保树的高度大致为 O(log n)。这种自平衡机制使得 TreeMap 在各种操作下都能保持稳定的性能表现。

TreeMap 的视图机制是其另一个重要特性。通过返回原映射的视图（keySet、values、entrySet、subMap 等），TreeMap 提供了灵活的数据访问方式，同时避免了不必要的数据复制。这些视图与原映射共享底层数据结构，因此对视图的操作会立即反映到原映射中。

在实际应用中，TreeMap 适用于需要有序键值对的场景，如范围查询、最近邻搜索、优先级队列等。与 HashMap 相比，TreeMap 的查找性能较低（O(log n) vs O(1)），但提供了有序性保证。与 LinkedHashMap 相比，TreeMap 的顺序由键的比较关系决定，而不是插入或访问顺序。

理解 TreeMap 的内部实现对于正确高效地使用它至关重要。红黑树的工作原理、平衡操作的细节、视图与原映射的关系，这些都是深入理解 TreeMap 的关键。通过源码学习，我们不仅了解了 TreeMap 的实现，还学习了一种经典的数据结构，这对于提升编程能力和算法思维都大有裨益。
