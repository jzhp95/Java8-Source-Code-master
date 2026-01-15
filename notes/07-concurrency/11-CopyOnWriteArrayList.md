# CopyOnWriteArrayList 源码解读

## 类概述

`CopyOnWriteArrayList` 是 `ArrayList` 的线程安全变体,其中所有修改操作(`add`、`set` 等)都通过创建底层数组的新副本来实现。

```java
public class CopyOnWriteArrayList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
```

## 核心特性

### 1. 写时复制(Copy-On-Write)

- 所有修改操作都通过创建底层数组的新副本来实现
- 读取操作不需要加锁,直接访问数组
- 修改操作需要获取锁,创建新数组,复制元素,修改新数组,然后替换旧数组

### 2. 适用场景

- 通常代价很高,但当遍历操作远多于修改操作时,可能比替代方案更高效
- 当不能或不想同步遍历,但需要排除并发线程之间的干扰时很有用
- 适用于读多写少的场景

### 3. 快照迭代器

- "快照"风格的迭代器方法使用对迭代器创建时数组状态的引用
- 该数组在迭代器的生命周期内永远不会改变
- 不可能发生干扰,迭代器保证不会抛出 `ConcurrentModificationException`
- 迭代器不会反映自迭代器创建以来对列表的添加、删除或更改
- 迭代器本身的元素更改操作(`remove`、`set` 和 `add`)不受支持

### 4. 内存一致性

- 线程中将对象放入 `CopyOnWriteArrayList` 之前的操作 happens-before 另一个线程中访问或从 `CopyOnWriteArrayList` 中删除该元素之后的操作

## 核心数据结构

### 核心字段

```java
/** 保护所有修改器的锁 */
final transient ReentrantLock lock = new ReentrantLock();

/** 数组,仅通过 getArray/setArray 访问 */
private transient volatile Object[] array;
```

#### lock - 修改锁

- **类型**: `ReentrantLock`
- **作用**: 保护所有修改操作
- **特点**: 可重入锁,保证修改操作的原子性

#### array - 底层数组

- **类型**: `volatile Object[]`
- **作用**: 存储列表元素
- **volatile**: 保证可见性,修改后立即可见
- **访问**: 仅通过 `getArray()` 和 `setArray()` 方法访问

### 辅助方法

```java
/** 获取数组。非私有以便也可以从 CopyOnWriteArraySet 类访问 */
final Object[] getArray() {
    return array;
}

/** 设置数组 */
final void setArray(Object[] a) {
    array = a;
}
```

- **getArray()**: 获取当前数组引用
- **setArray()**: 设置新的数组引用

## 核心方法

### 构造方法

#### 默认构造方法

```java
public CopyOnWriteArrayList() {
    setArray(new Object[0]);
}
```

- **功能**: 创建空列表
- **实现**: 设置为空数组

#### 从集合构造

```java
public CopyOnWriteArrayList(Collection<? extends E> c) {
    Object[] elements;
    if (c.getClass() == CopyOnWriteArrayList.class)
        elements = ((CopyOnWriteArrayList<?>)c).getArray();
    else {
        elements = c.toArray();
        // c.toArray 可能(不正确地)不返回 Object[](参见 6260652)
        if (elements.getClass() != Object[].class)
            elements = Arrays.copyOf(elements, elements.length, Object[].class);
    }
    setArray(elements);
}
```

- **参数**: `c` - 初始持有的元素集合
- **功能**: 创建包含指定集合元素的列表,按集合迭代器返回的顺序
- **优化**: 如果是 CopyOnWriteArrayList,直接获取其数组
- **异常**: `NullPointerException` - 如果指定集合为 null

#### 从数组构造

```java
public CopyOnWriteArrayList(E[] toCopyIn) {
    setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
}
```

- **参数**: `toCopyIn` - 数组(此数组的副本用作内部数组)
- **功能**: 创建保存给定数组副本的列表
- **异常**: `NullPointerException` - 如果指定数组为 null

### 读取操作

#### size() - 获取大小

```java
public int size() {
    return getArray().length;
}
```

- **功能**: 返回此列表中的元素数量
- **返回值**: 元素数量
- **特点**: 不需要加锁,直接读取数组长度

#### isEmpty() - 检查是否为空

```java
public boolean isEmpty() {
    return size() == 0;
}
```

- **功能**: 如果此列表不包含元素则返回 `true`
- **返回值**: 如果此列表不包含元素则返回 `true`

#### get() - 获取元素

```java
public E get(int index) {
    return get(getArray(), index);
}

@SuppressWarnings("unchecked")
private E get(Object[] a, int index) {
    return (E) a[index];
}
```

- **参数**: `index` - 要返回的元素的索引
- **返回值**: 指定位置的元素
- **异常**: `IndexOutOfBoundsException` - 如果索引超出范围
- **特点**: 不需要加锁,直接访问数组

#### contains() - 检查包含

```java
public boolean contains(Object o) {
    Object[] elements = getArray();
    return indexOf(o, elements, 0, elements.length) >= 0;
}
```

- **参数**: `o` - 要检查其是否存在的元素
- **返回值**: 如果此列表包含指定元素则返回 `true`
- **特点**: 不需要加锁,使用 indexOf 方法

#### indexOf() - 查找索引

```java
public int indexOf(Object o) {
    Object[] elements = getArray();
    return indexOf(o, elements, 0, elements.length);
}

private static int indexOf(Object o, Object[] elements,
                           int index, int fence) {
    if (o == null) {
        for (int i = index; i < fence; i++)
            if (elements[i] == null)
                return i;
    } else {
        for (int i = index; i < fence; i++)
            if (o.equals(elements[i]))
                return i;
    }
    return -1;
}
```

- **参数**: `o` - 要搜索的元素
- **返回值**: 第一次出现的索引,如果不存在则返回 -1
- **特点**: 不需要加锁,线性搜索

### 修改操作

#### set() - 设置元素

```java
public E set(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        E oldValue = get(elements, index);

        if (oldValue != element) {
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            newElements[index] = element;
            setArray(newElements);
        } else {
            // 不是完全无操作;确保 volatile 写入语义
            setArray(elements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

- **参数**:
  - `index`: 要替换的元素的索引
  - `element`: 要存储在指定位置的元素
- **返回值**: 之前在指定位置的元素
- **异常**: `IndexOutOfBoundsException` - 如果索引超出范围
- **实现**:
  1. 获取锁
  2. 获取当前数组
  3. 如果新值与旧值不同:
     - 创建新数组副本
     - 修改新数组
     - 设置新数组
  4. 否则,重新设置数组以确保 volatile 写入语义
  5. 释放锁

#### add() - 添加元素

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `e` - 要附加到此列表的元素
- **返回值**: `true`(如 Collection#add 所指定)
- **实现**:
  1. 获取锁
  2. 获取当前数组
  3. 创建新数组(长度+1)
  4. 添加元素到末尾
  5. 设置新数组
  6. 释放锁

#### add(int, E) - 指定位置添加

```java
public void add(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+len);
        Object[] newElements;
        int numMoved = len - index;
        if (numMoved == 0)
            newElements = Arrays.copyOf(elements, len + 1);
        else {
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1,
                             numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
```

- **参数**:
  - `index`: 要在其中插入指定元素的索引
  - `element`: 要插入的元素
- **异常**: `IndexOutOfBoundsException` - 如果索引超出范围
- **实现**:
  1. 获取锁
  2. 获取当前数组
  3. 检查索引范围
  4. 创建新数组
  5. 复制元素(需要时移动)
  6. 插入新元素
  7. 设置新数组
  8. 释放锁

#### remove(int) - 按索引删除

```java
public E remove(int index) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        E oldValue = get(elements, index);
        int numMoved = len - index - 1;
        if (numMoved == 0)
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                             numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `index` - 要删除的元素的索引
- **返回值**: 从列表中删除的元素
- **异常**: `IndexOutOfBoundsException` - 如果索引超出范围
- **实现**:
  1. 获取锁
  2. 获取当前数组
  3. 获取旧值
  4. 创建新数组(长度-1)
  5. 复制元素(跳过删除的元素)
  6. 设置新数组
  7. 释放锁

#### remove(Object) - 按元素删除

```java
public boolean remove(Object o) {
    Object[] snapshot = getArray();
    int index = indexOf(o, snapshot, 0, snapshot.length);
    return (index < 0) ? false : remove(o, snapshot, index);
}

private boolean remove(Object o, Object[] snapshot, int index) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] current = getArray();
        int len = current.length;
        if (snapshot != current) {
            findIndex: {
                int prefix = Math.min(index, len);
                for (int i = 0; i < prefix; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;
                        break findIndex;
                    }
                }
                if (index >= len)
                    return false;
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)
                    return false;
            }
        }
        Object[] newElements = new Object[len - 1];
        System.arraycopy(current, 0, newElements, 0, index);
        System.arraycopy(current, index + 1,
                         newElements, index,
                         len - index - 1);
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `o` - 要从此列表中删除的元素(如果存在)
- **返回值**: 如果此列表包含指定元素则返回 `true`
- **实现**:
  1. 获取快照(无锁)
  2. 查找元素索引
  3. 如果存在,调用带快照的 remove 方法
  4. 在带锁的 remove 中:
     - 检查快照是否改变
     - 如果改变,重新查找索引
     - 创建新数组并复制元素
     - 设置新数组

#### clear() - 清空列表

```java
public void clear() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        setArray(new Object[0]);
    } finally {
        lock.unlock();
    }
}
```

- **功能**: 删除此列表中的所有元素
- **实现**:
  1. 获取锁
  2. 设置为空数组
  3. 释放锁

### 批量操作

#### addAll() - 添加所有元素

```java
public boolean addAll(Collection<? extends E> c) {
    Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
        ((CopyOnWriteArrayList<?>)c).getArray() : c.toArray();
    if (cs.length == 0)
        return false;
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len == 0 && cs.getClass() == Object[].class)
            setArray(cs);
        else {
            Object[] newElements = Arrays.copyOf(elements, len + cs.length);
            System.arraycopy(cs, 0, newElements, len, cs.length);
            setArray(newElements);
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `c` - 包含要添加到此列表的元素的集合
- **返回值**: 如果此列表作为调用的结果而更改则返回 `true`
- **异常**: `NullPointerException` - 如果指定集合为 null
- **优化**: 如果是 CopyOnWriteArrayList,直接获取其数组

#### removeAll() - 删除所有包含的元素

```java
public boolean removeAll(Collection<?> c) {
    if (c == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // 临时数组保存我们要保留的元素
            int newlen = 0;
            Object[] temp = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object element = elements[i];
                if (!c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                setArray(Arrays.copyOf(temp, newlen));
                return true;
            }
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `c` - 包含要从此列表中删除的元素的集合
- **返回值**: 如果此列表作为调用的结果而更改则返回 `true`
- **特点**: 此类中的操作特别昂贵,因为需要内部临时数组

#### retainAll() - 保留所有包含的元素

```java
public boolean retainAll(Collection<?> c) {
    if (c == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // 临时数组保存我们要保留的元素
            int newlen = 0;
            Object[] temp = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object element = elements[i];
                if (c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                setArray(Arrays.copyOf(temp, newlen));
                return true;
            }
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `c` - 包含要在此列表中保留的元素的集合
- **返回值**: 如果此列表作为调用的结果而更改则返回 `true`
- **特点**: 删除此列表中不包含在指定集合中的所有元素

### Java 8 新增方法

#### forEach() - 遍历

```java
public void forEach(Consumer<? super E> action) {
    if (action == null) throw new NullPointerException();
    Object[] elements = getArray();
    int len = elements.length;
    for (int i = 0; i < len; ++i) {
        @SuppressWarnings("unchecked") E e = (E) elements[i];
        action.accept(e);
    }
}
```

- **参数**: `action` - 要对每个元素执行的操作
- **特点**: 不需要加锁,直接遍历数组

#### removeIf() - 条件删除

```java
public boolean removeIf(Predicate<? super E> filter) {
    if (filter == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            int newlen = 0;
            Object[] temp = new Object[len];
            for (int i = 0; i < len; ++i) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                if (!filter.test(e))
                    temp[newlen++] = e;
            }
            if (newlen != len) {
                setArray(Arrays.copyOf(temp, newlen));
                return true;
            }
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `filter` - 返回 true 时要删除元素的谓词
- **返回值**: 如果删除了任何元素则返回 `true`

#### replaceAll() - 替换所有元素

```java
public void replaceAll(UnaryOperator<E> operator) {
    if (operator == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len);
        for (int i = 0; i < len; ++i) {
            @SuppressWarnings("unchecked") E e = (E) elements[i];
            newElements[i] = operator.apply(e);
        }
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `operator` - 应用于每个元素的操作符
- **特点**: 创建新数组并应用操作符

#### sort() - 排序

```java
public void sort(Comparator<? super E> c) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        Object[] newElements = Arrays.copyOf(elements, elements.length);
        @SuppressWarnings("unchecked") E[] es = (E[])newElements;
        Arrays.sort(es, c);
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
```

- **参数**: `c` - 用于比较列表元素的 Comparator
- **特点**: 创建新数组并排序

## 使用示例

### 示例 1: 基本使用

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// 添加元素
list.add("A");
list.add("B");
list.add("C");

// 读取元素(不需要加锁)
for (String s : list) {
    System.out.println(s);
}

// 修改元素
list.set(1, "B2");
list.remove(2);
```

### 示例 2: 并发访问

```java
CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();

// 多个线程读取
new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        for (Integer value : list) {
            System.out.println(value);
        }
    }
}).start();

// 多个线程写入
new Thread(() -> {
    for (int i = 0; i < 100; i++) {
        list.add(i);
    }
}).start();
```

### 示例 3: 监听器列表

```java
CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

// 添加监听器
listeners.add(new EventListener());

// 通知所有监听器
for (EventListener listener : listeners) {
    listener.onEvent(event);
}

// 可以在遍历时添加新监听器,不会影响当前遍历
```

## 与 ArrayList 的区别

| 特性 | CopyOnWriteArrayList | ArrayList |
|------|----------------------|----------|
| 线程安全 | 线程安全 | 非线程安全 |
| 读取性能 | 高(无锁) | 高 |
| 写入性能 | 低(复制数组) | 高 |
| 迭代器 | 快照迭代器,不会抛出 CME | 快速失败,可能抛出 CME |
| 适用场景 | 读多写少 | 通用 |
| 内存占用 | 高(每次写都复制) | 低 |

## 关键点总结

1. **写时复制**: 所有修改操作都创建新数组副本
2. **读取无锁**: 读取操作不需要加锁,直接访问 volatile 数组
3. **快照迭代**: 迭代器使用快照,不会抛出 ConcurrentModificationException
4. **ReentrantLock**: 使用 ReentrantLock 保护所有修改操作
5. **volatile 数组**: array 字段声明为 volatile,保证可见性
6. **适用场景**: 读多写少的场景,遍历操作远多于修改操作
7. **内存开销**: 每次修改都需要复制整个数组,内存开销较大
8. **Java 8 支持**: 支持 forEach、removeIf、replaceAll、sort 等方法

## 相关类

- `ArrayList`: 非线程安全的列表
- `Vector`: 线程安全的列表(同步方法)
- `CopyOnWriteArraySet`: 写时复制的集合
- `ReentrantLock`: 可重入锁
- `Collections.synchronizedList()`: 同步列表包装器
