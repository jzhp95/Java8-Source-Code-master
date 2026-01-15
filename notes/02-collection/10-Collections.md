# Collections 源码解读

## 一、类概述

Collections是Java集合框架中的一个工具类,包含大量操作集合的静态方法。它提供了多态算法、集合包装器(返回由指定集合支持的新集合)以及其他一些实用功能。

### 1.1 核心特性

- **静态工具类**: 所有方法都是静态的,不能实例化
- **多态算法**: 提供排序、搜索、洗牌等算法
- **包装器方法**: 提供不可修改、同步、类型检查等包装
- **空集合**: 提供各种空集合的单例
- **单例集合**: 提供单元素集合的便捷方法
- **通用性**: 适用于各种集合实现

### 1.2 适用场景

- 集合排序和搜索
- 创建线程安全的集合
- 创建不可修改的集合
- 类型安全的集合操作
- 集合算法的便捷调用

## 二、设计原理

### 2.1 工具类模式

Collections采用工具类模式:

```java
// 私有构造方法,防止实例化
private Collections() {
}

// 所有方法都是静态的
public static <T extends Comparable<? super T>> void sort(List<T> list) {
    list.sort(null);
}
```

### 2.2 性能调优参数

Collections为不同算法定义了性能调优参数:

```java
// 二分查找阈值
private static final int BINARYSEARCH_THRESHOLD = 5000;

// 反转阈值
private static final int REVERSE_THRESHOLD = 18;

// 洗牌阈值
private static final int SHUFFLE_THRESHOLD = 5;

// 填充阈值
private static final int FILL_THRESHOLD = 25;

// 旋转阈值
private static final int ROTATE_THRESHOLD = 100;

// 复制阈值
private static final int COPY_THRESHOLD = 10;

// 替换所有阈值
private static final int REPLACEALL_THRESHOLD = 11;

// 子列表索引阈值
private static final int INDEXOFSUBLIST_THRESHOLD = 35;
```

**设计要点**:
- 根据集合类型(RandomAccess vs Sequential)选择不同算法
- 根据集合大小选择最优算法
- 阈值经过经验确定,适用于LinkedList等顺序访问列表

### 2.3 算法选择策略

```java
// RandomAccess列表: 使用基于索引的算法
if (list instanceof RandomAccess) {
    // 使用索引访问,性能更好
} else {
    // 使用迭代器访问
}
```

## 三、排序算法

### 3.1 sort - 排序

```java
// 自然排序
public static <T extends Comparable<? super T>> void sort(List<T> list) {
    list.sort(null);
}

// 自定义排序
public static <T> void sort(List<T> list, Comparator<? super T> c) {
    list.sort(c);
}
```

**方法要点**:
- 委托给List的sort方法
- 稳定排序: 相等元素不会重新排序
- 时间复杂度: O(n log n)
- 空间复杂度: 取决于具体实现

**示例**:
```java
List<String> list = new ArrayList<>(Arrays.asList("banana", "apple", "cherry"));
Collections.sort(list);  // [apple, banana, cherry]

// 降序排序
Collections.sort(list, Comparator.reverseOrder());  // [cherry, banana, apple]

// 按字符串长度排序
Collections.sort(list, Comparator.comparingInt(String::length));
```

## 四、搜索算法

### 4.1 binarySearch - 二分查找

```java
// 自然排序的二分查找
public static <T>
int binarySearch(List<? extends Comparable<? super T>> list, T key) {
    if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD)
        return Collections.indexedBinarySearch(list, key);
    else
        return Collections.iteratorBinarySearch(list, key);
}

// 自定义Comparator的二分查找
public static <T> int binarySearch(List<? extends T> list, T key,
                                    Comparator<? super T> c) {
    if (c == null)
        return binarySearch((List<? extends Comparable<? super T>>) list, key);

    if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD)
        return Collections.indexedBinarySearch(list, key, c);
    else
        return Collections.iteratorBinarySearch(list, key, c);
}
```

**方法要点**:
- 列表必须已排序
- 时间复杂度: O(log n) for RandomAccess, O(n) for Sequential
- 返回值: 找到返回索引,未找到返回(-(插入点) - 1)

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 3, 5, 7, 9));

// 查找存在的元素
int index = Collections.binarySearch(list, 5);  // 返回2

// 查找不存在的元素
index = Collections.binarySearch(list, 4);  // 返回-3 (插入点为2)
```

### 4.2 indexOfSubList - 查找子列表

```java
public static int indexOfSubList(List<?> source, List<?> target) {
    int sourceSize = source.size();
    int targetSize = target.size();
    int maxCandidate = sourceSize - targetSize;

    if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
        (source instanceof RandomAccess && target instanceof RandomAccess)) {
        // 使用索引访问
        nextCand:
        for (int candidate = 0; candidate <= maxCandidate; candidate++) {
            for (int i = 0, j = candidate; i < targetSize; i++, j++) {
                if (!eq(source.get(j), target.get(i)))
                    continue nextCand;
            }
            return candidate;
        }
    } else {
        // 使用迭代器访问
        ListIterator<?> si = source.listIterator();
    nextCand:
        for (int candidate = 0; candidate <= maxCandidate; candidate++) {
            ListIterator<?> ti = target.listIterator();
            for (int i = 0; i < targetSize; i++) {
                if (!eq(si.next(), ti.next()))
                    continue nextCand;
            }
            return candidate;
        }
    }
    return -1;
}
```

**方法要点**:
- 查找target在source中第一次出现的位置
- 时间复杂度: O(n * m)
- 根据集合类型选择最优算法

**示例**:
```java
List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 3, 4, 5);
List<Integer> target = Arrays.asList(3, 4, 5);

int index = Collections.indexOfSubList(source, target);  // 返回2
```

### 4.3 lastIndexOfSubList - 查找子列表最后一次出现

```java
public static int lastIndexOfSubList(List<?> source, List<?> target) {
    int sourceSize = source.size();
    int targetSize = target.size();
    int maxCandidate = sourceSize - targetSize;

    if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
        targetSize == 0) {
        // 小列表或空目标,使用简单算法
        for (int candidate = maxCandidate; candidate >= 0; candidate--) {
            boolean found = true;
            for (int i = 0, j = candidate; i < targetSize; i++, j++) {
                if (!eq(source.get(j), target.get(i))) {
                    found = false;
                    break;
                }
            }
            if (found)
                return candidate;
        }
    } else {
        // 大列表,使用迭代器
        ListIterator<?> si = source.listIterator(maxCandidate);
    nextCand:
        for (int candidate = maxCandidate; candidate >= 0; candidate--) {
            ListIterator<?> ti = target.listIterator();
            for (int i = 0; i < targetSize; i++) {
                if (!eq(si.previous(), ti.next()))
                    continue nextCand;
            }
            return candidate;
        }
    }
    return -1;
}
```

## 五、修改算法

### 5.1 reverse - 反转列表

```java
public static void reverse(List<?> list) {
    int size = list.size();
    if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
        // 小列表或RandomAccess列表,使用索引交换
        for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--)
            swap(list, i, j);
    } else {
        // 大列表,使用迭代器
        ListIterator fwd = list.listIterator();
        ListIterator rev = list.listIterator(size);
        for (int i = 0, mid = list.size() >> 1; i < mid; i++) {
            Object tmp = fwd.next();
            fwd.set(rev.previous());
            rev.set(tmp);
        }
    }
}
```

**方法要点**:
- 原地反转列表
- 时间复杂度: O(n)
- 根据列表类型选择最优算法

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.reverse(list);  // [5, 4, 3, 2, 1]
```

### 5.2 shuffle - 洗牌

```java
// 使用默认随机源
public static void shuffle(List<?> list) {
    Random rnd = r;
    if (rnd == null)
        r = rnd = new Random();
    shuffle(list, rnd);
}

// 使用指定随机源
public static void shuffle(List<?> list, Random rnd) {
    int size = list.size();
    if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
        // 小列表或RandomAccess列表
        for (int i = size; i > 1; i--)
            swap(list, i - 1, rnd.nextInt(i));
    } else {
        // 大列表,使用数组
        Object arr[] = list.toArray();
        for (int i = size; i > 1; i--)
            swap(arr, i - 1, rnd.nextInt(i));
        ListIterator it = list.listIterator();
        for (int i = 0; i < arr.length; i++) {
            it.next();
            it.set(arr[i]);
        }
    }
}
```

**方法要点**:
- 随机打乱列表顺序
- 使用Fisher-Yates算法
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.shuffle(list);  // 随机顺序
```

### 5.3 rotate - 旋转列表

```java
public static void rotate(List<?> list, int distance) {
    if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD)
        rotate1(list, distance);
    else
        rotate2(list, distance);
}

// RandomAccess或小列表
private static void rotate1(List<?> list, int distance) {
    int size = list.size();
    if (size == 0)
        return;
    distance = distance % size;
    if (distance < 0)
        distance += size;
    if (distance == 0)
        return;

    for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
        Object displaced = list.get(cycleStart);
        int j = cycleStart;
        for (int i = cycleStart; i != cycleStart - distance; ) {
            i = (i + distance) % size;
            displaced = list.set(i, displaced);
            nMoved++;
        }
    }
}

// 大列表
private static void rotate2(List<?> list, int distance) {
    int size = list.size();
    if (size == 0)
        return;
    int mid = -distance % size;
    if (mid < 0)
        mid += size;
    if (mid == 0)
        return;

    reverse(list.subList(0, mid));
    reverse(list.subList(mid, size));
    reverse(list);
}
```

**方法要点**:
- 将列表元素向右移动distance个位置
- 使用三次反转实现(大列表)
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.rotate(list, 2);  // [4, 5, 1, 2, 3]
```

### 5.4 swap - 交换元素

```java
public static void swap(List<?> list, int i, int j) {
    final List l = list;
    l.set(i, l.set(j, l.get(i)));
}
```

**方法要点**:
- 交换列表中两个位置的元素
- 时间复杂度: O(1) for RandomAccess, O(n) for Sequential

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.swap(list, 0, 4);  // [5, 2, 3, 4, 1]
```

### 5.5 fill - 填充列表

```java
public static <T> void fill(List<? super T> list, T obj) {
    int size = list.size();
    if (size < FILL_THRESHOLD || list instanceof RandomAccess) {
        // 小列表或RandomAccess列表
        for (int i = 0; i < size; i++)
            list.set(i, obj);
    } else {
        // 大列表,使用迭代器
        ListIterator<? super T> itr = list.listIterator();
        for (int i = 0; i < size; i++) {
            itr.next();
            itr.set(obj);
        }
    }
}
```

**方法要点**:
- 用指定元素填充整个列表
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.fill(list, 0);  // [0, 0, 0, 0, 0]
```

### 5.6 copy - 复制列表

```java
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    int srcSize = src.size();
    if (srcSize > dest.size())
        throw new IllegalArgumentException("Source does not fit in dest");

    if (srcSize < COPY_THRESHOLD ||
        (src instanceof RandomAccess && dest instanceof RandomAccess)) {
        // 小列表或RandomAccess列表
        for (int i = 0; i < srcSize; i++)
            dest.set(i, src.get(i));
    } else {
        // 大列表,使用迭代器
        ListIterator<? super T> di = dest.listIterator();
        ListIterator<? extends T> si = src.listIterator();
        for (int i = 0; i < srcSize; i++) {
            di.next();
            di.set(si.next());
        }
    }
}
```

**方法要点**:
- 将src的所有元素复制到dest
- dest必须至少有src.size()个元素
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> src = Arrays.asList(1, 2, 3);
List<Integer> dest = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0));
Collections.copy(dest, src);  // [1, 2, 3, 0, 0]
```

### 5.7 replaceAll - 替换所有

```java
public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
    boolean result = false;
    int size = list.size();
    if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
        // 小列表或RandomAccess列表
        for (int i = 0; i < size; i++) {
            if (oldVal == list.get(i) || oldVal != null && oldVal.equals(list.get(i))) {
                list.set(i, newVal);
                result = true;
            }
        }
    } else {
        // 大列表,使用迭代器
        ListIterator<T> itr = list.listIterator();
        for (int i = 0; i < size; i++) {
            if (oldVal == itr.next() || oldVal != null && oldVal.equals(itr.previous())) {
                itr.set(newVal);
                result = true;
                itr.next();
            }
        }
    }
    return result;
}
```

**方法要点**:
- 替换列表中所有等于oldVal的元素为newVal
- 返回是否进行了替换
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 2, 4));
Collections.replaceAll(list, 2, 9);  // [1, 9, 3, 9, 4]
```

## 六、极值算法

### 6.1 min - 查找最小值

```java
// 自然排序
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

// 自定义Comparator
public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
    if (comp == null)
        return (T)min((Collection<Comparable>)coll);

    Iterator<? extends T> i = coll.iterator();
    T candidate = i.next();

    while (i.hasNext()) {
        T next = i.next();
        if (comp.compare(next, candidate) < 0)
            candidate = next;
    }
    return candidate;
}
```

**方法要点**:
- 返回集合中最小的元素
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
int min = Collections.min(list);  // 1
```

### 6.2 max - 查找最大值

```java
// 自然排序
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

// 自定义Comparator
public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
    if (comp == null)
        return (T)max((Collection<Comparable>)coll);

    Iterator<? extends T> i = coll.iterator();
    T candidate = i.next();

    while (i.hasNext()) {
        T next = i.next();
        if (comp.compare(next, candidate) > 0)
            candidate = next;
    }
    return candidate;
}
```

**方法要点**:
- 返回集合中最大的元素
- 时间复杂度: O(n)

**示例**:
```java
List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
int max = Collections.max(list);  // 5
```

## 七、不可修改包装器

### 7.1 unmodifiableCollection - 不可修改集合

```java
public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
    return new UnmodifiableCollection<>(c);
}

static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 1820017752578914078L;

    final Collection<? extends E> c;

    UnmodifiableCollection(Collection<? extends E> c) {
        if (c == null)
            throw new NullPointerException();
        this.c = c;
    }

    public int size() {return c.size();}
    public boolean isEmpty() {return c.isEmpty();}
    public boolean contains(Object o) {return c.contains(o);}
    public Object[] toArray() {return c.toArray();}
    public <T> T[] toArray(T[] a) {return c.toArray(a);}

    public String toString() {return c.toString();}

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<? extends E> i = c.iterator();

            public boolean hasNext() {return i.hasNext();}
            public E next() {return i.next();}
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> coll) {
        return c.containsAll(coll);
    }
    public boolean addAll(Collection<? extends E> coll) {
        throw new UnsupportedOperationException();
    }
    public boolean removeAll(Collection<?> coll) {
        throw new UnsupportedOperationException();
    }
    public boolean retainAll(Collection<?> coll) {
        throw new UnsupportedOperationException();
    }
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
```

**方法要点**:
- 返回不可修改的集合视图
- 所有修改操作抛出UnsupportedOperationException
- 原集合的修改会影响视图

**示例**:
```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
List<String> unmodifiable = Collections.unmodifiableList(list);

unmodifiable.add("d");  // UnsupportedOperationException

list.add("d");  // OK,unmodifiable也能看到
```

### 7.2 unmodifiableList - 不可修改列表

```java
public static <T> List<T> unmodifiableList(List<? extends T> list) {
    return (list instanceof RandomAccess ?
            new UnmodifiableRandomAccessList<>(list) :
            new UnmodifiableList<>(list));
}

static class UnmodifiableList<E> extends UnmodifiableCollection<E>
                                  implements List<E> {
    final List<? extends E> list;

    UnmodifiableList(List<? extends E> list) {
        super(list);
        this.list = list;
    }

    public boolean equals(Object o) {return o == this || list.equals(o);}
    public int hashCode() {return list.hashCode();}

    public E get(int index) {return list.get(index);}
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }
    public int indexOf(Object o) {return list.indexOf(o);}
    public int lastIndexOf(Object o) {return list.lastIndexOf(o);}
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }
    public ListIterator<E> listIterator() {return listIterator(0);}
    public ListIterator<E> listIterator(final int index) {
        return new ListIterator<E>() {
            private final ListIterator<? extends E> i = list.listIterator(index);

            public boolean hasNext() {return i.hasNext();}
            public E next() {return i.next();}
            public boolean hasPrevious() {return i.hasPrevious();}
            public E previous() {return i.previous();}
            public int nextIndex() {return i.nextIndex();}
            public int previousIndex() {return i.previousIndex();}
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public void set(E e) {
                throw new UnsupportedOperationException();
            }
            public void add(E e) {
                throw new UnsupportedOperationException();
            }
        };
    }
    public List<E> subList(int fromIndex, int toIndex) {
        return new UnmodifiableList<>(list.subList(fromIndex, toIndex));
    }
}
```

### 7.3 unmodifiableSet - 不可修改集合

```java
public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
    return new UnmodifiableSet<>(s);
}

static class UnmodifiableSet<E> extends UnmodifiableCollection<E>
                             implements Set<E> {
    UnmodifiableSet(Set<? extends E> s) {super(s);}

    public boolean equals(Object o) {return o == this || c.equals(o);}
    public int hashCode() {return c.hashCode();}
}
```

### 7.4 unmodifiableMap - 不可修改映射

```java
public static <K,V> Map<K,V> unmodifiableMap(Map<? extends K, ? extends V> m) {
    return new UnmodifiableMap<>(m);
}

private static class UnmodifiableMap<K,V> implements Map<K,V>, Serializable {
    private static final long serialVersionUID = -1034234728574286014L;

    private final Map<? extends K, ? extends V> m;

    UnmodifiableMap(Map<? extends K, ? extends V> m) {
        if (m == null)
            throw new NullPointerException();
        this.m = m;
    }

    public int size() {return m.size();}
    public boolean isEmpty() {return m.isEmpty();}
    public boolean containsKey(Object key) {return m.containsKey(key);}
    public boolean containsValue(Object val) {return m.containsValue(val);}
    public V get(Object key) {return m.get(key);}

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K,V>> entrySet = null;
    private transient Collection<V> values = null;

    public Set<K> keySet() {
        if (keySet==null)
            keySet = unmodifiableSet(m.keySet());
        return keySet;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        if (entrySet==null)
            entrySet = new UnmodifiableEntrySet<>(m.entrySet());
        return entrySet;
    }

    public Collection<V> values() {
        if (values==null)
            values = unmodifiableCollection(m.values());
        return values;
    }

    public boolean equals(Object o) {return o == this || m.equals(o);}
    public int hashCode() {return m.hashCode();}
    public String toString() {return m.toString();}
}
```

## 八、同步包装器

### 8.1 synchronizedCollection - 同步集合

```java
public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
    return new SynchronizedCollection<>(c);
}

static class SynchronizedCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 3053995032091335093L;

    final Collection<E> c;
    final Object mutex;

    SynchronizedCollection(Collection<E> c) {
        this.c = Objects.requireNonNull(c);
        mutex = this;
    }

    SynchronizedCollection(Collection<E> c, Object mutex) {
        this.c = Objects.requireNonNull(c);
        this.mutex = Objects.requireNonNull(mutex);
    }

    public int size() {
        synchronized (mutex) {return c.size();}
    }
    public boolean isEmpty() {
        synchronized (mutex) {return c.isEmpty();}
    }
    public boolean contains(Object o) {
        synchronized (mutex) {return c.contains(o);}
    }
    public Object[] toArray() {
        synchronized (mutex) {return c.toArray();}
    }
    public <T> T[] toArray(T[] a) {
        synchronized (mutex) {return c.toArray(a);}
    }

    public Iterator<E> iterator() {
        return c.iterator();
    }

    public boolean add(E e) {
        synchronized (mutex) {return c.add(e);}
    }
    public boolean remove(Object o) {
        synchronized (mutex) {return c.remove(o);}
    }

    public boolean containsAll(Collection<?> coll) {
        synchronized (mutex) {return c.containsAll(coll);}
    }
    public boolean addAll(Collection<? extends E> coll) {
        synchronized (mutex) {return c.addAll(coll);}
    }
    public boolean removeAll(Collection<?> coll) {
        synchronized (mutex) {return c.removeAll(coll);}
    }
    public boolean retainAll(Collection<?> coll) {
        synchronized (mutex) {return c.retainAll(coll);}
    }
    public void clear() {
        synchronized (mutex) {c.clear();}
    }

    public String toString() {
        synchronized (mutex) {return c.toString();}
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        synchronized (mutex) {s.defaultWriteObject();}
    }
}
```

**方法要点**:
- 返回线程安全的集合
- 所有操作都通过synchronized同步
- 迭代时需要手动同步

**示例**:
```java
List<String> list = new ArrayList<>();
List<String> syncList = Collections.synchronizedList(list);

// 添加元素是线程安全的
syncList.add("a");

// 迭代时需要手动同步
synchronized (syncList) {
    for (String s : syncList) {
        System.out.println(s);
    }
}
```

### 8.2 synchronizedList - 同步列表

```java
public static <T> List<T> synchronizedList(List<T> list) {
    return (list instanceof RandomAccess ?
            new SynchronizedRandomAccessList<>(list) :
            new SynchronizedList<>(list));
}

static class SynchronizedList<E>
    extends SynchronizedCollection<E>
    implements List<E> {
    final List<E> list;

    SynchronizedList(List<E> list) {
        super(list);
        this.list = list;
    }

    SynchronizedList(List<E> list, Object mutex) {
        super(list, mutex);
        this.list = list;
    }

    public boolean equals(Object o) {
        synchronized (mutex) {return list.equals(o);}
    }
    public int hashCode() {
        synchronized (mutex) {return list.hashCode();}
    }

    public E get(int index) {
        synchronized (mutex) {return list.get(index);}
    }
    public E set(int index, E element) {
        synchronized (mutex) {return list.set(index, element);}
    }
    public void add(int index, E element) {
        synchronized (mutex) {list.add(index, element);}
    }
    public E remove(int index) {
        synchronized (mutex) {return list.remove(index);}
    }
    public int indexOf(Object o) {
        synchronized (mutex) {return list.indexOf(o);}
    }
    public int lastIndexOf(Object o) {
        synchronized (mutex) {return list.lastIndexOf(o);}
    }
    public boolean addAll(int index, Collection<? extends E> c) {
        synchronized (mutex) {return list.addAll(index, c);}
    }

    public ListIterator<E> listIterator() {
        return list.listIterator();
    }
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        synchronized (mutex) {
            return new SynchronizedList<>(list.subList(fromIndex, toIndex), mutex);
        }
    }
}
```

### 8.3 synchronizedMap - 同步映射

```java
public static <K,V> Map<K,V> synchronizedMap(Map<K,V> m) {
    return new SynchronizedMap<>(m);
}

private static class SynchronizedMap<K,V>
    implements Map<K,V>, Serializable {
    private static final long serialVersionUID = 1978198479659022715L;

    private final Map<K,V> m;
    final Object mutex;

    SynchronizedMap(Map<K,V> m) {
        if (m == null)
            throw new NullPointerException();
        this.m = m;
        mutex = this;
    }

    SynchronizedMap(Map<K,V> m, Object mutex) {
        this.m = m;
        this.mutex = mutex;
    }

    public int size() {
        synchronized (mutex) {return m.size();}
    }
    public boolean isEmpty() {
        synchronized (mutex) {return m.isEmpty();}
    }
    public boolean containsKey(Object key) {
        synchronized (mutex) {return m.containsKey(key);}
    }
    public boolean containsValue(Object value) {
        synchronized (mutex) {return m.containsValue(value);}
    }
    public V get(Object key) {
        synchronized (mutex) {return m.get(key);}
    }

    public V put(K key, V value) {
        synchronized (mutex) {return m.put(key, value);}
    }
    public V remove(Object key) {
        synchronized (mutex) {return m.remove(key);}
    }
    public void putAll(Map<? extends K, ? extends V> map) {
        synchronized (mutex) {m.putAll(map);}
    }
    public void clear() {
        synchronized (mutex) {m.clear();}
    }

    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K,V>> entrySet = null;
    private transient Collection<V> values = null;

    public Set<K> keySet() {
        synchronized (mutex) {
            if (keySet==null)
                keySet = new SynchronizedSet<>(m.keySet(), mutex);
            return keySet;
        }
    }

    public Set<Map.Entry<K,V>> entrySet() {
        synchronized (mutex) {
            if (entrySet==null)
                entrySet = new SynchronizedSet<>(m.entrySet(), mutex);
            return entrySet;
        }
    }

    public Collection<V> values() {
        synchronized (mutex) {
            if (values==null)
                values = new SynchronizedCollection<>(m.values(), mutex);
            return values;
        }
    }

    public boolean equals(Object o) {
        synchronized (mutex) {return m.equals(o);}
    }
    public int hashCode() {
        synchronized (mutex) {return m.hashCode();}
    }
    public String toString() {
        synchronized (mutex) {return m.toString();}
    }
}
```

## 九、类型检查包装器

### 9.1 checkedCollection - 类型检查集合

```java
public static <E> Collection<E> checkedCollection(Collection<E> c,
                                                Class<E> type) {
    return new CheckedCollection<>(c, type);
}

static class CheckedCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 1578914078182001775L;

    final Collection<E> c;
    final Class<E> type;

    CheckedCollection(Collection<E> c, Class<E> type) {
        this.c = Objects.requireNonNull(c, "c");
        this.type = Objects.requireNonNull(type, "type");
    }

    public int size() {return c.size();}
    public boolean isEmpty() {return c.isEmpty();}
    public boolean contains(Object o) {return c.contains(o);}
    public Object[] toArray() {return c.toArray();}
    public <T> T[] toArray(T[] a) {return c.toArray(a);}

    public String toString() {return c.toString();}

    public boolean add(E e) {
        typeCheck(e);
        return c.add(e);
    }

    public boolean remove(Object o) {return c.remove(o);}

    public boolean containsAll(Collection<?> coll) {
        return c.containsAll(coll);
    }

    public boolean addAll(Collection<? extends E> coll) {
        for (E e : coll)
            typeCheck(e);
        return c.addAll(coll);
    }

    public boolean removeAll(Collection<?> coll) {return c.removeAll(coll);}
    public boolean retainAll(Collection<?> coll) {return c.retainAll(coll);}
    public void clear() {c.clear();}

    @SuppressWarnings("unchecked")
    E typeCheck(Object o) {
        if (o != null && !type.isInstance(o))
            throw new ClassCastException(badElementMsg(o));
        return (E) o;
    }

    private String badElementMsg(Object o) {
        return "Attempt to insert " + o.getClass() +
               " element into collection with element type " + type;
    }
}
```

**方法要点**:
- 返回运行时类型安全的集合
- 在添加元素时检查类型
- 防止ClassCastException

**示例**:
```java
List<String> list = new ArrayList<>();
List<String> checkedList = Collections.checkedList(list, String.class);

checkedList.add("hello");  // OK
checkedList.add(123);      // ClassCastException
```

## 十、空集合

### 10.1 emptySet - 空集合

```java
public static final Set EMPTY_SET = new EmptySet<>();

public static final <T> Set<T> emptySet() {
    return (Set<T>) EMPTY_SET;
}

private static class EmptySet extends AbstractSet<Object>
    implements Serializable {
    private static final long serialVersionUID = 1582296315990362920L;

    public Iterator<Object> iterator() {
        return emptyIterator();
    }

    public int size() {return 0;}

    public boolean isEmpty() {return true;}

    public boolean contains(Object obj) {return false;}

    public boolean containsAll(Collection<?> c) {return c.isEmpty();}

    public Object[] toArray() {return new Object[0];}

    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }

    private Object readResolve() {
        return EMPTY_SET;
    }
}
```

### 10.2 emptyList - 空列表

```java
public static final List EMPTY_LIST = new EmptyList<>();

public static final <T> List<T> emptyList() {
    return (List<T>) EMPTY_LIST;
}

private static class EmptyList extends AbstractList<Object>
    implements RandomAccess, Serializable {
    private static final long serialVersionUID = 8842843931221139166L;

    public Iterator<Object> iterator() {
        return emptyIterator();
    }

    public ListIterator<Object> listIterator() {
        return emptyListIterator();
    }

    public int size() {return 0;}

    public boolean isEmpty() {return true;}

    public boolean contains(Object obj) {return false;}

    public boolean containsAll(Collection<?> c) {return c.isEmpty();}

    public Object get(int index) {
        throw new IndexOutOfBoundsException("Index: "+index);
    }

    private Object readResolve() {
        return EMPTY_LIST;
    }
}
```

### 10.3 emptyMap - 空映射

```java
public static final Map EMPTY_MAP = new EmptyMap<>();

public static final <K,V> Map<K,V> emptyMap() {
    return (Map<K,V>) EMPTY_MAP;
}

private static class EmptyMap extends AbstractMap<Object,Object>
    implements Serializable {
    private static final long serialVersionUID = 6428348081125593813L;

    public int size() {return 0;}

    public boolean isEmpty() {return true;}

    public boolean containsKey(Object key) {return false;}

    public boolean containsValue(Object value) {return false;}

    public Object get(Object key) {return null;}

    public Set<Object> keySet() {return emptySet();}

    public Collection<Object> values() {return emptyList();}

    public Set<Map.Entry<Object,Object>> entrySet() {return emptySet();}

    private Object readResolve() {
        return EMPTY_MAP;
    }
}
```

**使用场景**:
- 避免返回null
- 作为方法默认返回值
- 减少对象创建

**示例**:
```java
public List<String> getItems() {
    if (items == null) {
        return Collections.emptyList();
    }
    return items;
}
```

## 十一、单例集合

### 11.1 singleton - 单元素集合

```java
public static <T> Set<T> singleton(T o) {
    return new SingletonSet<>(o);
}

private static class SingletonSet<E> extends AbstractSet<E>
    implements Serializable {
    private static final long serialVersionUID = 3193687207550431679L;

    private final E element;

    SingletonSet(E e) {element = e;}

    public Iterator<E> iterator() {
        return singletonIterator(element);
    }

    public int size() {return 1;}

    public boolean isEmpty() {return false;}

    public boolean contains(Object o) {return eq(o, element);}

    public boolean remove(Object o) {
        return eq(o, element);
    }
}
```

### 11.2 singletonList - 单元素列表

```java
public static <T> List<T> singletonList(T o) {
    return new SingletonList<>(o);
}

private static class SingletonList<E>
    extends AbstractList<E>
    implements RandomAccess, Serializable {

    private static final long serialVersionUID = 3093736618740652951L;

    private final E element;

    SingletonList(E e) {element = e;}

    public Iterator<E> iterator() {
        return singletonIterator(element);
    }

    public int size() {return 1;}

    public E get(int index) {
        if (index != 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
        return element;
    }
}
```

### 11.3 singletonMap - 单元素映射

```java
public static <K,V> Map<K,V> singletonMap(K key, V value) {
    return new SingletonMap<>(key, value);
}

private static class SingletonMap<K,V>
    extends AbstractMap<K,V>
    implements Serializable {

    private static final long serialVersionUID = -6979724477215052911L;

    private final K k;
    private final V v;

    SingletonMap(K key, V value) {
        k = key;
        v = value;
    }

    public int size() {return 1;}

    public boolean isEmpty() {return false;}

    public boolean containsKey(Object key) {return eq(key, k);}

    public boolean containsValue(Object value) {return eq(value, v);}

    public V get(Object key) {
        return (eq(key, k) ? v : null);
    }

    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K,V>> entrySet = null;
    private transient Collection<V> values = null;

    public Set<K> keySet() {
        if (keySet==null)
            keySet = singleton(k);
        return keySet;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        if (entrySet==null)
            entrySet = Collections.<Map.Entry<K,V>>singleton(
                new SimpleImmutableEntry<>(k, v));
        return entrySet;
    }

    public Collection<V> values() {
        if (values==null)
            values = singleton(v);
        return values;
    }
}
```

**使用场景**:
- 需要单元素集合
- 避免创建不必要的集合
- 作为方法参数

**示例**:
```java
Set<String> set = Collections.singleton("hello");
List<Integer> list = Collections.singletonList(123);
Map<String, Integer> map = Collections.singletonMap("key", 456);
```

## 十二、其他实用方法

### 12.1 nCopies - 创建n个副本的列表

```java
public static <T> List<T> nCopies(int n, T o) {
    if (n < 0)
        throw new IllegalArgumentException("List length = " + n);
    return new CopiesList<>(n, o);
}

private static class CopiesList<E>
    extends AbstractList<E>
    implements RandomAccess, Serializable {
    private static final long serialVersionUID = 2739099268398711800L;

    final int n;
    final E element;

    CopiesList(int n, E e) {
        if (n < 0)
            throw new IllegalArgumentException("List length = " + n);
        this.n = n;
        element = e;
    }

    public int size() {
        return n;
    }

    public boolean contains(Object obj) {
        return n != 0 && eq(obj, element);
    }

    public E get(int index) {
        if (index < 0 || index >= n)
            throw new IndexOutOfBoundsException("Index: "+index+
                                            ", Size: "+n);
        return element;
    }
}
```

**示例**:
```java
List<String> list = Collections.nCopies(5, "hello");
System.out.println(list);  // [hello, hello, hello, hello, hello]
```

### 12.2 reverseOrder - 反序比较器

```java
public static <T> Comparator<T> reverseOrder() {
    return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
}

private static class ReverseComparator
    implements Comparator<Comparable<Object>>, Serializable {

    private static final long serialVersionUID = 7207038068494060240L;

    static final ReverseComparator REVERSE_ORDER = new ReverseComparator();

    public int compare(Comparable<Object> c1, Comparable<Object> c2) {
        return c2.compareTo(c1);
    }

    private Object readResolve() { return reverseOrder(); }
}

public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
    if (cmp == null)
        return reverseOrder();

    if (cmp instanceof ReverseComparator2)
        return ((ReverseComparator2<T>)cmp).cmp;

    return new ReverseComparator2<>(cmp);
}

private static class ReverseComparator2<T> implements Comparator<T>,
    Serializable
{
    private static final long serialVersionUID = 4374092139857L;

    final Comparator<? super T> cmp;

    ReverseComparator2(Comparator<? super T> cmp) {
        this.cmp = cmp;
    }

    public int compare(T t1, T t2) {
        return cmp.compare(t2, t1);
    }
}
```

**示例**:
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
Collections.sort(list, Collections.reverseOrder());  // [5, 4, 3, 2, 1]
```

### 12.3 frequency - 计算元素出现频率

```java
public static int frequency(Collection<?> c, Object o) {
    int result = 0;
    if (o == null) {
        for (Object e : c)
            if (e == null)
                result++;
    } else {
        for (Object e : c)
            if (o.equals(e))
                result++;
    }
    return result;
}
```

**示例**:
```java
List<Integer> list = Arrays.asList(1, 2, 3, 2, 4, 2, 5);
int freq = Collections.frequency(list, 2);  // 3
```

### 12.4 disjoint - 检查两个集合是否不相交

```java
public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
    Collection<?> contains = c2;
    Collection<?> iterate = c1;

    if (c1 instanceof Set && !(c2 instanceof Set) ||
        (c1.size() > c2.size())) {
        iterate = c2;
        contains = c1;
    }

    for (Object e : iterate) {
        if (contains.contains(e))
            return false;
    }
    return true;
}
```

**示例**:
```java
List<Integer> list1 = Arrays.asList(1, 2, 3);
List<Integer> list2 = Arrays.asList(4, 5, 6);
boolean disjoint = Collections.disjoint(list1, list2);  // true
```

### 12.5 addAll - 批量添加

```java
public static <T> boolean addAll(Collection<? super T> c, T... elements) {
    boolean result = false;
    for (T element : elements)
        result |= c.add(element);
    return result;
}
```

**示例**:
```java
List<String> list = new ArrayList<>();
Collections.addAll(list, "a", "b", "c");  // [a, b, c]
```

### 12.6 newSetFromMap - 从Map创建Set

```java
public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
    return new SetFromMap<>(map);
}

private static class SetFromMap<E> extends AbstractSet<E>
    implements Serializable {
    private static final long serialVersionUID = 2454657884131932992L;

    private final Map<E, Boolean> m;
    private transient Set<E> s;

    SetFromMap(Map<E, Boolean> map) {
        if (!map.isEmpty())
            throw new IllegalArgumentException("Map is non-empty");
        this.m = map;
        this.s = map.keySet();
    }

    public int size()                {return s.size();}
    public boolean isEmpty()         {return s.isEmpty();}
    public boolean contains(Object o)   {return s.contains(o);}
    public boolean remove(Object o)  {return s.remove(o);}
    public boolean add(E e) {return m.put(e, Boolean.TRUE) == null;}
    public Iterator<E> iterator()   {return s.iterator();}
    public void clear()               {m.clear();}
}
```

**示例**:
```java
Map<String, Boolean> map = new ConcurrentHashMap<>();
Set<String> set = Collections.newSetFromMap(map);
```

### 12.7 asLifoQueue - 将Deque转换为LIFO队列

```java
public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
    return new AsLIFOQueue<>(deque);
}

static class AsLIFOQueue<E> extends AbstractQueue<E>
    implements Serializable {
    private static final long serialVersionUID = 1802017725587941708L;
    private final Deque<E> q;

    AsLIFOQueue(Deque<E> q) {this.q = q;}

    public boolean add(E e)         {q.addFirst(e); return true;}
    public boolean offer(E e)        {return q.offerFirst(e);}
    public E poll()                 {return q.pollFirst();}
    public E remove()               {return q.removeFirst();}
    public E peek()                 {return q.peekFirst();}
    public E element()              {return q.getFirst();}
    public void clear()             {q.clear();}
    public int size()               {return q.size();}
}
```

**示例**:
```java
Deque<String> deque = new ArrayDeque<>();
Queue<String> lifoQueue = Collections.asLifoQueue(deque);
lifoQueue.add("a");  // 添加到头部
lifoQueue.add("b");  // 添加到头部
System.out.println(lifoQueue.poll());  // b (后进先出)
```

## 十三、设计模式

### 13.1 装饰器模式

包装器方法使用装饰器模式:

```java
// 同步包装器
public static <T> List<T> synchronizedList(List<T> list) {
    return new SynchronizedList<>(list);
}

// 不可修改包装器
public static <T> List<T> unmodifiableList(List<T> list) {
    return new UnmodifiableList<>(list);
}

// 类型检查包装器
public static <T> List<T> checkedList(List<T> list, Class<T> type) {
    return new CheckedList<>(list, type);
}
```

### 13.2 代理模式

包装器作为原集合的代理:

```java
static class SynchronizedList<E> {
    final List<E> list;  // 被代理的列表

    public E get(int index) {
        synchronized (mutex) {
            return list.get(index);  // 委托给原列表
        }
    }
}
```

### 13.3 工厂模式

提供各种集合的工厂方法:

```java
// 空集合工厂
public static final <T> Set<T> emptySet() {
    return (Set<T>) EMPTY_SET;
}

// 单例集合工厂
public static <T> Set<T> singleton(T o) {
    return new SingletonSet<>(o);
}

// 多副本集合工厂
public static <T> List<T> nCopies(int n, T o) {
    return new CopiesList<>(n, o);
}
```

## 十四、面试常见问题

### 14.1 Collections与Collection的区别?

| 特性 | Collections | Collection |
|------|-------------|-------------|
| 类型 | 工具类 | 接口 |
| 实例化 | 不能实例化 | 可以被实现 |
| 方法 | 静态方法 | 实例方法 |
| 用途 | 操作集合 | 定义集合契约 |

### 14.2 Arrays.asList与Collections.singletonList的区别?

```java
// Arrays.asList: 返回固定大小的列表
List<String> list1 = Arrays.asList("a", "b", "c");
list1.set(0, "x");  // OK
list1.add("d");      // UnsupportedOperationException

// Collections.singletonList: 返回不可修改的单元素列表
List<String> list2 = Collections.singletonList("a");
list2.set(0, "x");  // UnsupportedOperationException
list2.add("b");      // UnsupportedOperationException
```

### 14.3 Collections.sort的时间复杂度?

- 时间复杂度: O(n log n)
- 空间复杂度: 取决于具体实现
- 稳定性: 稳定排序

### 14.4 Collections.synchronizedList是线程安全的吗?

基本操作是线程安全的,但迭代时需要手动同步:

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// 迭代时需要手动同步
synchronized (list) {
    for (String s : list) {
        System.out.println(s);
    }
}
```

### 14.5 Collections.unmodifiableList真的不可修改吗?

包装的列表本身不可修改,但原列表可以修改:

```java
List<String> original = new ArrayList<>(Arrays.asList("a", "b"));
List<String> unmodifiable = Collections.unmodifiableList(original);

unmodifiable.add("c");  // UnsupportedOperationException
original.add("c");     // OK,unmodifiable也能看到
```

### 14.6 Collections.emptyList会创建新对象吗?

不会,返回同一个单例对象:

```java
List<String> list1 = Collections.emptyList();
List<String> list2 = Collections.emptyList();
System.out.println(list1 == list2);  // true
```

### 14.7 Collections.binarySearch对未排序的列表会怎样?

结果未定义,可能返回错误的索引:

```java
List<Integer> list = Arrays.asList(5, 3, 1, 4, 2);
int index = Collections.binarySearch(list, 3);  // 结果未定义
```

### 14.8 如何创建线程安全的Set?

```java
// 方法1: 使用synchronizedSet
Set<String> set = Collections.synchronizedSet(new HashSet<>());

// 方法2: 使用CopyOnWriteArraySet
Set<String> set = new CopyOnWriteArraySet<>();

// 方法3: 使用ConcurrentHashMap.newKeySet
Set<String> set = ConcurrentHashMap.newKeySet();
```

## 十五、使用场景

### 15.1 排序和搜索

```java
List<String> list = new ArrayList<>(Arrays.asList("banana", "apple", "cherry"));

// 排序
Collections.sort(list);

// 搜索
int index = Collections.binarySearch(list, "banana");
```

### 15.2 创建线程安全集合

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());
Set<String> set = Collections.synchronizedSet(new HashSet<>());
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
```

### 15.3 创建不可修改集合

```java
List<String> list = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")));
Set<String> set = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
Map<String, Integer> map = Collections.unmodifiableMap(new HashMap<>());
```

### 15.4 类型安全集合

```java
List<String> list = new ArrayList<>();
List<String> checkedList = Collections.checkedList(list, String.class);

checkedList.add("hello");  // OK
checkedList.add(123);      // ClassCastException
```

### 15.5 空集合作为返回值

```java
public List<String> getItems() {
    if (items == null) {
        return Collections.emptyList();
    }
    return items;
}
```

### 15.6 单元素集合

```java
Set<String> set = Collections.singleton("hello");
List<Integer> list = Collections.singletonList(123);
Map<String, Integer> map = Collections.singletonMap("key", 456);
```

## 十六、注意事项

### 16.1 同步集合的迭代

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// 错误: 迭代时不同步
for (String s : list) {
    System.out.println(s);
}

// 正确: 迭代时同步
synchronized (list) {
    for (String s : list) {
        System.out.println(s);
    }
}
```

### 16.2 不可修改集合的修改

```java
List<String> original = new ArrayList<>(Arrays.asList("a", "b"));
List<String> unmodifiable = Collections.unmodifiableList(original);

// 包装的列表不可修改
unmodifiable.add("c");  // UnsupportedOperationException

// 原列表可以修改,会影响包装列表
original.add("c");     // OK
```

### 16.3 类型检查集合的泛型擦除

```java
List<String> list = new ArrayList<>();
List<String> checkedList = Collections.checkedList(list, String.class);

List rawList = checkedList;
rawList.add(123);  // 运行时抛出ClassCastException
```

### 16.4 二分查找的前提条件

```java
List<Integer> list = Arrays.asList(5, 3, 1, 4, 2);

// 错误: 未排序就二分查找
int index = Collections.binarySearch(list, 3);  // 结果未定义

// 正确: 先排序再查找
Collections.sort(list);
index = Collections.binarySearch(list, 3);  // 正确结果
```

## 十七、最佳实践

### 17.1 使用空集合避免null

```java
// 不推荐
public List<String> getItems() {
    return items == null ? null : items;
}

// 推荐
public List<String> getItems() {
    return items == null ? Collections.emptyList() : items;
}
```

### 17.2 使用单例集合减少对象创建

```java
// 不推荐
List<String> list = new ArrayList<>();
list.add("hello");
process(list);

// 推荐
process(Collections.singletonList("hello"));
```

### 17.3 使用不可修改集合保护数据

```java
public class MyClass {
    private final List<String> items = new ArrayList<>();

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }
}
```

### 17.4 使用类型检查集合增强类型安全

```java
public void processList(List<String> list) {
    List<String> checkedList = Collections.checkedList(list, String.class);
    // 使用checkedList,确保类型安全
}
```

### 17.5 使用同步包装器实现线程安全

```java
public class MyClass {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    public void addItem(String item) {
        list.add(item);  // 线程安全
    }

    public void processItems() {
        synchronized (list) {  // 迭代时需要同步
            for (String item : list) {
                System.out.println(item);
            }
        }
    }
}
```

## 十八、总结

Collections是Java集合框架的重要工具类,提供了丰富的集合操作方法。

### 核心要点

1. **工具类模式**: 所有方法都是静态的,不能实例化
2. **多态算法**: 提供排序、搜索、洗牌等算法
3. **包装器方法**: 提供不可修改、同步、类型检查等包装
4. **性能优化**: 根据集合类型和大小选择最优算法
5. **空集合**: 提供各种空集合的单例
6. **单例集合**: 提供单元素集合的便捷方法
7. **装饰器模式**: 包装器使用装饰器模式

### 主要功能

- **排序和搜索**: sort, binarySearch, min, max
- **修改操作**: reverse, shuffle, rotate, swap, fill, copy, replaceAll
- **包装器**: unmodifiableXxx, synchronizedXxx, checkedXxx
- **空集合**: emptySet, emptyList, emptyMap
- **单例集合**: singleton, singletonList, singletonMap
- **其他方法**: nCopies, reverseOrder, frequency, disjoint, addAll

### 使用建议

- 使用空集合避免返回null
- 使用不可修改集合保护数据
- 使用同步包装器实现线程安全
- 使用类型检查集合增强类型安全
- 根据场景选择合适的集合包装器
- 注意同步集合的迭代需要手动同步
- 理解包装器与原集合的关系
