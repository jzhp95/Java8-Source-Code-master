# Java集合框架体系源码深度解析——List体系详解

## 二、List体系详解

### 2.1 List体系概述

#### 2.1.1 List接口特性

List是有序集合(Collection),允许重复元素,支持基于索引的访问。List接口继承自Collection,增加了位置相关的操作。

**List的核心特性:**

1. **有序性**: 元素按照插入顺序存储,可以通过索引访问。

2. **可重复**: 可以包含重复元素,包括null。

3. **索引访问**: 支持通过索引快速访问元素。

4. **动态大小**: 可以动态添加和删除元素。

```
List接口体系
├── AbstractList (抽象类)
│   ├── ArrayList (数组实现)
│   ├── Vector (线程安全的数组实现,已过时)
│   └── AbstractSequentialList (抽象顺序列表)
│       └── LinkedList (链表实现)
└── Stack (栈实现,已过时)
```

#### 2.1.2 List接口核心方法

```java
public interface List<E> extends Collection<E> {
    // 位置访问操作
    E get(int index);
    E set(int index, E element);
    void add(int index, E element);
    E remove(int index);
    
    // 查询操作
    int indexOf(Object o);
    int lastIndexOf(Object o);
    List<E> subList(int fromIndex, int toIndex);
    
    // 列表迭代器
    ListIterator<E> listIterator();
    ListIterator<E> listIterator(int index);
    
    // 批量操作
    default void sort(Comparator<? super E> c);
    default void replaceAll(UnaryOperator<E> operator);
    default Spliterator<E> spliterator();
}
```

### 2.2 ArrayList深度解析

#### 2.2.1 ArrayList概述

ArrayList是基于动态数组实现的List,它提供了快速的随机访问,但在插入和删除时可能需要移动大量元素。

**ArrayList的核心特点:**

1. **快速随机访问**: 基于数组实现,通过索引直接访问元素,时间复杂度O(1)。

2. **动态扩容**: 当数组容量不足时,自动扩容为原来的1.5倍。

3. **非线程安全**: 不是线程安全的,多线程环境下需要同步。

4. **允许null**: 可以存储null元素。

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    
    // 默认初始容量
    private static final int DEFAULT_CAPACITY = 10;
    
    // 空数组实例
    private static final Object[] EMPTY_ELEMENTDATA = {};
    
    // 默认空数组实例
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    
    // 存储ArrayList元素的数组缓冲区
    transient Object[] elementData;
    
    // ArrayList的大小(包含的元素数量)
    private int size;
}
```

#### 2.2.2 核心构造方法

```java
// 构造一个初始容量为10的空列表
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

// 构造一个指定初始容量的空列表
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    }
}

// 构造一个包含指定集合元素的列表
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    if ((size = elementData.length) != 0) {
        // 如果toArray()返回的不是Object[],需要转换
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, size, Object[].class);
    } else {
        // 使用空数组
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

#### 2.2.3 add操作详解

```java
// 在列表末尾添加元素
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // 确保容量足够
    elementData[size++] = e;          // 添加元素
    return true;
}

// 在指定位置插入元素
public void add(int index, E element) {
    rangeCheckForAdd(index);          // 检查索引范围
    ensureCapacityInternal(size + 1);  // 确保容量足够
    System.arraycopy(elementData, index, elementData, index + 1,
                     size - index);   // 移动元素
    elementData[index] = element;    // 插入元素
    size++;
}

// 确保容量足够
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    // 如果所需容量大于当前容量,需要扩容
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

// 扩容方法
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);  // 扩容为1.5倍
    if (newCapacity - minCapacity < 0)  // 溢出检查
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // 复制到新数组
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

**扩容机制详解:**

1. **扩容倍数**: 新容量 = 旧容量 + 旧容量/2 = 1.5倍

2. **最小容量**: 如果1.5倍仍不够,使用所需的最小容量

3. **最大容量**: Integer.MAX_VALUE - 8

4. **数组复制**: 使用Arrays.copyOf复制到新数组

#### 2.2.4 get操作详解

```java
// 获取指定位置的元素
public E get(int index) {
    rangeCheck(index);  // 检查索引范围
    return elementData(index);
}

// 检查索引是否越界
private void rangeCheck(int index) {
    if (index >= size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

// 获取元素(无边界检查)
E elementData(int index) {
    return (E) elementData[index];
}
```

**get操作的时间复杂度:**

- O(1): 直接通过索引访问数组元素

#### 2.2.5 remove操作详解

```java
// 删除指定位置的元素
public E remove(int index) {
    rangeCheck(index);
    modCount++;
    E oldValue = elementData(index);
    
    int numMoved = size - index - 1;
    if (numMoved > 0)
        // 移动元素
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--size] = null;  // 清空最后一个位置,帮助GC
    
    return oldValue;
}

// 删除指定元素
public boolean remove(Object o) {
    if (o == null) {
        // 删除null元素
        for (int index = 0; index < size; index++)
            if (elementData[index] == null) {
                fastRemove(index);
                return true;
            }
    } else {
        // 删除非null元素
        for (int index = 0; index < size; index++)
            if (o.equals(elementData[index])) {
                fastRemove(index);
                return true;
            }
    }
    return false;
}

// 快速删除(不返回值,不检查索引)
private void fastRemove(int index) {
    modCount++;
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--size] = null;
}
```

**remove操作的时间复杂度:**

- O(n): 需要移动后续元素

#### 2.2.6 modCount与快速失败

```java
protected transient int modCount = 0;
```

modCount用于记录结构修改次数,实现快速失败(fail-fast)机制。

```java
// ArrayList的迭代器
private class Itr implements Iterator<E> {
    int cursor;       // 下一个要返回的元素的索引
    int lastRet = -1; // 上一个返回的元素的索引
    int expectedModCount = modCount; // 期望的modCount
    
    public E next() {
        checkForComodification();  // 检查并发修改
        int i = cursor;
        if (i >= size)
            throw new NoSuchElementException();
        Object[] elementData = ArrayList.this.elementData;
        if (i >= elementData.length)
            throw new ConcurrentModificationException();
        cursor = i + 1;
        return (E) elementData[lastRet = i];
    }
    
    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();
        
        try {
            ArrayList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }
    
    // 检查并发修改
    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

**快速失败机制:**

1. **记录修改次数**: 每次结构修改(modCount++)

2. **迭代器保存期望值**: 迭代器创建时保存expectedModCount

3. **每次操作检查**: 每次迭代时检查modCount是否改变

4. **抛出异常**: 如果不一致,抛出ConcurrentModificationException

### 2.3 LinkedList深度解析

#### 2.3.1 LinkedList概述

LinkedList是基于双向链表实现的List和Deque,它支持高效的插入和删除操作,但随机访问性能较差。

**LinkedList的核心特点:**

1. **双向链表**: 每个节点包含前驱和后继指针。

2. **高效插入删除**: 在已知位置插入删除是O(1)。

3. **实现Deque**: 同时实现了List和Deque接口。

4. **非线程安全**: 不是线程安全的。

```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    
    // 节点类
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
        
        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
    
    // 链表头节点
    transient Node<E> first;
    
    // 链表尾节点
    transient Node<E> last;
    
    // 链表大小
    transient int size = 0;
}
```

#### 2.3.2 add操作详解

```java
// 在链表末尾添加元素
public boolean add(E e) {
    linkLast(e);
    return true;
}

// 在链表末尾链接节点
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}

// 在指定位置插入元素
public void add(int index, E element) {
    checkPositionIndex(index);
    
    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));
}

// 在指定节点前插入元素
void linkBefore(E e, Node<E> succ) {
    final Node<E> pred = succ.prev;
    final Node<E> newNode = new Node<>(pred, e, succ);
    succ.prev = newNode;
    if (pred == null)
        first = newNode;
    else
        pred.next = newNode;
    size++;
    modCount++;
}

// 获取指定位置的节点
Node<E> node(int index) {
    // 优化: 从较近的一端开始遍历
    if (index < (size >> 1)) {
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

**add操作的时间复杂度:**

- add(e): O(1) - 直接在末尾添加
- add(index, e): O(n) - 需要遍历到指定位置

#### 2.3.3 get操作详解

```java
// 获取指定位置的元素
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}
```

**get操作的时间复杂度:**

- O(n): 需要遍历链表

**优化技巧:**

LinkedList的node方法有优化:如果索引小于size/2,从头开始遍历;否则从尾开始遍历。

#### 2.3.4 remove操作详解

```java
// 删除指定位置的元素
public E remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));
}

// 删除指定节点
E unlink(Node<E> x) {
    final E element = x.item;
    final Node<E> next = x.next;
    final Node<E> prev = x.prev;
    
    if (prev == null) {
        first = next;
    } else {
        prev.next = next;
        x.prev = null;
    }
    
    if (next == null) {
        last = prev;
    } else {
        next.prev = prev;
        x.next = null;
    }
    
    x.item = null;
    size--;
    modCount++;
    return element;
}

// 删除指定元素
public boolean remove(Object o) {
    if (o == null) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}
```

**remove操作的时间复杂度:**

- remove(index): O(n) - 需要遍历到指定位置
- remove(Object): O(n) - 需要遍历查找元素

#### 2.3.5 Deque操作

LinkedList实现了Deque接口,支持双端队列操作:

```java
// 在队列头部添加元素
public void addFirst(E e) {
    linkFirst(e);
}

void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;
    else
        f.prev = newNode;
    size++;
    modCount++;
}

// 在队列尾部添加元素
public void addLast(E e) {
    linkLast(e);
}

// 获取并移除队列头部元素
public E removeFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return unlinkFirst(f);
}

private E unlinkFirst(Node<E> f) {
    final E element = f.item;
    final Node<E> next = f.next;
    f.item = null;
    f.next = null;
    first = next;
    if (next == null)
        last = null;
    else
        next.prev = null;
    size--;
    modCount++;
    return element;
}

// 获取并移除队列尾部元素
public E removeLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return unlinkLast(l);
}

private E unlinkLast(Node<E> l) {
    final E element = l.item;
    final Node<E> prev = l.prev;
    l.item = null;
    l.prev = null;
    last = prev;
    if (prev == null)
        first = null;
    else
        prev.next = null;
    size--;
    modCount++;
    return element;
}
```

### 2.4 ArrayList vs LinkedList对比

#### 2.4.1 性能对比

| 操作 | ArrayList | LinkedList |
|------|-----------|------------|
| add(e) | O(1) | O(1) |
| add(index, e) | O(n) | O(n) |
| get(index) | O(1) | O(n) |
| remove(index) | O(n) | O(n) |
| remove(Object) | O(n) | O(n) |
| contains(Object) | O(n) | O(n) |
| iterator().next() | O(1) | O(1) |

#### 2.4.2 内存占用对比

**ArrayList:**

- 连续内存,缓存友好
- 每个元素占用固定空间
- 可能有未使用的容量

**LinkedList:**

- 非连续内存,缓存不友好
- 每个节点需要额外存储prev和next指针
- 没有未使用的容量

#### 2.4.3 使用场景

**ArrayList适用场景:**

1. 需要快速随机访问
2. 主要在末尾添加元素
3. 内存敏感
4. 单线程环境

**LinkedList适用场景:**

1. 需要频繁在头部或中间插入删除
2. 需要使用Deque功能
3. 不需要随机访问
4. 单线程环境

### 2.5 List使用最佳实践

#### 2.5.1 选择合适的List实现

```java
// 场景1: 需要快速随机访问
List<String> list = new ArrayList<>();

// 场景2: 需要频繁插入删除
Deque<String> deque = new LinkedList<>();

// 场景3: 需要线程安全
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// 场景4: 需要读写分离
List<String> copyOnWriteList = new CopyOnWriteArrayList<>();
```

#### 2.5.2 避免常见错误

**错误1: 使用Vector**

```java
// 错误示例
Vector<String> vector = new Vector<>();

// 正确示例
List<String> list = new ArrayList<>();
```

**错误2: 遍历时修改集合**

```java
// 错误示例
for (String s : list) {
    if (s.equals("target"))
        list.remove(s); // ConcurrentModificationException
}

// 正确示例1: 使用Iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("target"))
        it.remove();
}

// 正确示例2: 使用removeIf
list.removeIf(s -> s.equals("target"));
```

**错误3: 使用LinkedList进行随机访问**

```java
// 错误示例
LinkedList<String> list = new LinkedList<>();
for (int i = 0; i < list.size(); i++) {
    String s = list.get(i); // O(n)操作
}

// 正确示例
for (String s : list) {
    // O(1)操作
}
```

#### 2.5.3 性能优化建议

**1. 预估集合大小**

```java
// 错误示例
List<String> list = new ArrayList<>();

// 正确示例
List<String> list = new ArrayList<>(1000);
```

**2. 使用批量操作**

```java
// 错误示例
for (String s : sourceList) {
    list.add(s);
}

// 正确示例
list.addAll(sourceList);
```

**3. 使用subList时注意**

```java
List<String> subList = list.subList(0, 10);
// subList的修改会影响原list
subList.add("new"); // 也会添加到原list
```

### 2.6 小结

List体系是Java集合框架中最常用的部分,理解其实现原理对于编写高效代码至关重要。

**核心要点总结:**

第一,**ArrayList**: 基于动态数组,快速随机访问,扩容为1.5倍。

第二,**LinkedList**: 基于双向链表,高效插入删除,实现Deque接口。

第三,**性能对比**: ArrayList适合随机访问,LinkedList适合频繁插入删除。

第四,**快速失败**: 使用modCount实现并发修改检测。

第五,**最佳实践**: 选择合适的实现、避免常见错误、优化性能。
