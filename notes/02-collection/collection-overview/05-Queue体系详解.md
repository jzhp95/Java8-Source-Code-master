# Java集合框架体系源码深度解析——Queue体系详解

## 五、Queue体系详解

### 5.1 Queue体系概述

#### 5.1.1 Queue接口特性

Queue是队列接口,它继承自Collection,定义了队列的基本操作。队列遵循FIFO(先进先出)原则,元素从尾部添加,从头部移除。

**Queue的核心特性:**

1. **FIFO原则**: 先进先出,元素按照插入顺序移除。

2. **双端操作**: Deque接口支持在头部和尾部进行插入和删除。

3. **优先级支持**: PriorityQueue支持按优先级排序。

4. **阻塞支持**: BlockingQueue支持阻塞操作,用于生产者-消费者模式。

```
Queue接口体系
├── Queue (接口) - 单端队列
│   ├── PriorityQueue - 优先级队列
│   └── LinkedList - 实现了Queue
├── Deque (接口) - 双端队列
│   ├── ArrayDeque - 数组实现
│   ├── LinkedList - 链表实现
│   └── LinkedBlockingDeque - 并发实现
└── BlockingQueue (接口) - 阻塞队列
    ├── ArrayBlockingQueue - 数组实现
    ├── LinkedBlockingQueue - 链表实现
    ├── PriorityBlockingQueue - 优先级队列
    ├── DelayQueue - 延迟队列
    └── SynchronousQueue - 同步队列
```

#### 5.1.2 Queue接口核心方法

```java
public interface Queue<E> extends Collection<E> {
    // 添加元素
    boolean add(E e);
    boolean offer(E e);
    
    // 移除元素
    E remove();
    E poll();
    
    // 查询元素
    E element();
    E peek();
}
```

**方法对比:**

| 方法 | 成功时 | 失败时 | 队列空时 |
|------|--------|--------|---------|
| add | 返回true | 抛出异常 | 抛出异常 |
| offer | 返回true | 返回false | 返回false |
| remove | 返回元素 | 抛出异常 | 抛出异常 |
| poll | 返回元素 | 返回null | 返回null |
| element | 返回元素 | 抛出异常 | 抛出异常 |
| peek | 返回元素 | 返回null | 返回null |

#### 5.1.3 Deque接口核心方法

```java
public interface Deque<E> extends Queue<E> {
    // 头部操作
    void addFirst(E e);
    void addLast(E e);
    boolean offerFirst(E e);
    boolean offerLast(E e);
    E removeFirst();
    E removeLast();
    E pollFirst();
    E pollLast();
    E getFirst();
    E getLast();
    E peekFirst();
    E peekLast();
    
    // 队列操作
    boolean add(E e);
    boolean offer(E e);
    E remove();
    E poll();
    E element();
    E peek();
    
    // 栈操作
    void push(E e);
    E pop();
    
    // 批量操作
    boolean removeFirstOccurrence(Object o);
    boolean removeLastOccurrence(Object o);
}
```

### 5.2 PriorityQueue深度解析

#### 5.2.1 PriorityQueue概述

PriorityQueue是基于堆(优先队列)实现的Queue,它不遵循FIFO原则,而是按照元素的自然顺序或自定义Comparator的顺序排列。

**PriorityQueue的核心特点:**

1. **优先级排序**: 按照元素的自然顺序或自定义Comparator排序。

2. **堆结构**: 使用二叉堆实现,堆顶元素是最小(或最大)元素。

3. **无界队列**: 默认是无界的,可以动态扩容。

4. **非线程安全**: 不是线程安全的,多线程环境下需要同步。

5. **不允许null**: 不允许null元素。

```java
public class PriorityQueue<E> extends AbstractQueue<E>
    implements java.io.Serializable {
    
    // 默认初始容量
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    
    // 堆数组
    transient Object[] queue;
    
    // 队列大小
    private int size = 0;
    
    // 比较器
    private final Comparator<? super E> comparator;
}
```

#### 5.2.2 核心构造方法

```java
// 构造一个默认初始容量的PriorityQueue,使用自然顺序
public PriorityQueue() {
    this(DEFAULT_INITIAL_CAPACITY, null);
}

// 构造一个指定初始容量的PriorityQueue,使用自然顺序
public PriorityQueue(int initialCapacity) {
    this(initialCapacity, null);
}

// 构造一个指定初始容量的PriorityQueue,使用指定的Comparator
public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.queue = new Object[initialCapacity];
    this.comparator = comparator;
}

// 构造一个包含指定集合元素的PriorityQueue
public PriorityQueue(Collection<? extends E> c) {
    if (c instanceof SortedSet) {
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator();
        initElementsFromCollection(ss);
    } else if (c instanceof PriorityQueue) {
        PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator();
        initFromPriorityQueue(pq);
    } else {
        this.comparator = null;
        initFromCollection(c);
    }
}
```

#### 5.2.3 add操作详解

```java
// 添加元素到队列
public boolean add(E e) {
    return offer(e);
}

// 添加元素到队列
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    if (i >= queue.length)
        grow(i + 1);
    siftUp(i, e);
    size = i + 1;
    return true;
}
```

**siftUp方法详解:**

```java
// 向上调整堆
private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x, queue, comparator);
    else
        siftUpComparable(k, x, queue);
}

// 使用Comparable向上调整
private static <T> void siftUpComparable(int k, T x, Object[] arr) {
    Comparable<? super T> key = (Comparable<? super T>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = arr[parent];
        if (key.compareTo((T) e) >= 0)
            break;
        arr[k] = e;
        k = parent;
    }
    arr[k] = x;
}

// 使用Comparator向上调整
private static <T> void siftUpUsingComparator(int k, T x, Object[] arr,
                                               Comparator<? super T> cmp) {
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = arr[parent];
        if (cmp.compare(x, (T) e) >= 0)
            break;
        arr[k] = e;
        k = parent;
    }
    arr[k] = x;
}
```

**add操作的时间复杂度:**

- O(log n): 需要向上调整堆

#### 5.2.4 poll操作详解

```java
// 获取并移除队列头部元素
public E poll() {
    if (size == 0)
        return null;
    int s = --size;
    modCount++;
    E result = (E) queue[0];
    E x = (E) queue[s];
    queue[s] = null;
    if (s != 0)
        siftDown(0, x, s);
    return result;
}
```

**siftDown方法详解:**

```java
// 向下调整堆
private void siftDown(int k, E x, int n) {
    if (comparator != null)
        siftDownUsingComparator(k, x, n, queue, comparator);
    else
        siftDownComparable(k, x, n, queue);
}

// 使用Comparable向下调整
private static <T> void siftDownComparable(int k, T x, int n, Object[] arr) {
    if (n > 0) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = arr[child];
            int right = child + 1;
            if (right < n &&
                ((Comparable<? super T>) c).compareTo((T) arr[right]) > 0)
                c = arr[child = right];
            if (key.compareTo((T) c) >= 0)
                break;
            arr[k] = c;
            k = child;
        }
        arr[k] = key;
    }
}

// 使用Comparator向下调整
private static <T> void siftDownUsingComparator(int k, T x, int n,
                                                 Object[] arr,
                                                 Comparator<? super T> cmp) {
    if (n > 0) {
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = arr[child];
            int right = child + 1;
            if (right < n &&
                cmp.compare(c, arr[right]) > 0)
                c = arr[child = right];
            if (cmp.compare(x, (T) c) >= 0)
                break;
            arr[k] = c;
            k = child;
        }
        arr[k] = x;
    }
}
```

**poll操作的时间复杂度:**

- O(log n): 需要向下调整堆

#### 5.2.5 peek操作详解

```java
// 获取但不移除队列头部元素
public E peek() {
    return (size == 0) ? null : (E) queue[0];
}
```

**peek操作的时间复杂度:**

- O(1): 直接访问堆顶元素

### 5.3 ArrayDeque深度解析

#### 5.3.1 ArrayDeque概述

ArrayDeque是基于数组实现的双端队列,它支持在头部和尾部进行高效的插入和删除操作。

**ArrayDeque的核心特点:**

1. **双端操作**: 支持在头部和尾部进行插入和删除。

2. **高效操作**: 大部分操作时间复杂度O(1)。

3. **可变数组**: 使用可变数组存储元素,动态扩容。

4. **非线程安全**: 不是线程安全的。

5. **不允许null**: 不允许null元素。

6. **实现Deque**: 同时实现了Deque接口,可以用作栈和队列。

```java
public class ArrayDeque<E> extends AbstractCollection<E>
    implements Deque<E>, Cloneable, java.io.Serializable {
    
    // 最小初始容量
    private static final int MIN_INITIAL_CAPACITY = 8;
    
    // 元素数组
    transient Object[] elements;
    
    // 头部索引
    transient int head;
    
    // 尾部索引
    transient int tail;
}
```

#### 5.3.2 核心构造方法

```java
// 构造一个空的ArrayDeque,初始容量为16
public ArrayDeque() {
    elements = new Object[16];
}

// 构造一个空的ArrayDeque,指定初始容量
public ArrayDeque(int numElements) {
    allocateElements(numElements);
}

// 构造一个包含指定集合元素的ArrayDeque
public ArrayDeque(Collection<? extends E> c) {
    this(c.size());
    addAll(c);
}

// 分配元素数组
private void allocateElements(int numElements) {
    int initialCapacity = MIN_INITIAL_CAPACITY;
    if (numElements >= initialCapacity) {
        initialCapacity = numElements;
        initialCapacity |= (initialCapacity >>> 1);
        initialCapacity |= (initialCapacity >>> 2);
        initialCapacity |= (initialCapacity >>> 4);
        initialCapacity += 1;
        if (initialCapacity < 0)
            initialCapacity >>>= 1;
    }
    elements = new Object[initialCapacity];
}
```

#### 5.3.3 addFirst操作详解

```java
// 在队列头部添加元素
public void addFirst(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[head = (head - 1) & (elements.length - 1)] = e;
    if (head == tail)
        doubleCapacity();
}
```

**head索引计算:**

```java
head = (head - 1) & (elements.length - 1);
```

使用位运算实现循环数组,当head减到0时,自动跳到数组末尾。

#### 5.3.4 addLast操作详解

```java
// 在队列尾部添加元素
public void addLast(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[tail] = e;
    tail = (tail + 1) & (elements.length - 1);
    if (head == tail)
        doubleCapacity();
}
```

**tail索引计算:**

```java
tail = (tail + 1) & (elements.length - 1);
```

同样使用位运算实现循环数组,当tail加到数组末尾时,自动跳到数组开头。

#### 5.3.5 pollFirst操作详解

```java
// 获取并移除队列头部元素
public E pollFirst() {
    int h = head;
    E result = (E) elements[h];
    if (result == null)
        return null;
    elements[h] = null;
    head = (h + 1) & (elements.length - 1);
    if (head == tail)
        pollLast(); // 如果只有一个元素,同时清空
    return result;
}
```

#### 5.3.6 pollLast操作详解

```java
// 获取并移除队列尾部元素
public E pollLast() {
    int t = (tail - 1) & (elements.length - 1);
    E result = (E) elements[t];
    if (result == null)
        return null;
    elements[t] = null;
    tail = t;
    if (head == tail)
        pollFirst(); // 如果只有一个元素,同时清空
    return result;
}
```

#### 5.3.7 扩容机制

```java
// 扩容为原来的2倍
private void doubleCapacity() {
    assert head == tail;
    int p = head;
    int n = elements.length;
    int newCapacity = n << 1;
    if (newCapacity < 0)
        throw new IllegalStateException("Sorry, deque too big");
    Object[] a = new Object[newCapacity];
    if (head < tail) {
        System.arraycopy(elements, head, a, 0, n - head);
        System.arraycopy(elements, 0, a, n - head, tail);
    } else {
        System.arraycopy(elements, head, a, 0, n - head);
        System.arraycopy(elements, head, a, n - head, tail);
    }
    elements = a;
    head = 0;
    tail = n;
}
```

### 5.4 Queue体系对比

#### 5.4.1 性能对比

| 操作 | PriorityQueue | ArrayDeque | LinkedList |
|------|--------------|------------|------------|
| add | O(log n) | O(1) | O(1) |
| offer | O(log n) | O(1) | O(1) |
| remove | O(n) | O(n) | O(1) |
| poll | O(log n) | O(1) | O(1) |
| peek | O(1) | O(1) | O(1) |

#### 5.4.2 特性对比

| 特性 | PriorityQueue | ArrayDeque | LinkedList |
|------|--------------|------------|------------|
| 有序性 | 优先级排序 | 插入顺序 | 插入顺序 |
| 双端操作 | 不支持 | 支持 | 支持 |
| 栈操作 | 不支持 | 支持 | 支持 |
| 允许null | 否 | 否 | 是 |
| 线程安全 | 否 | 否 | 否 |

### 5.5 Queue使用最佳实践

#### 5.5.1 选择合适的Queue实现

```java
// 场景1: 需要优先级排序
Queue<Task> queue = new PriorityQueue<>();

// 场景2: 需要双端操作
Deque<String> deque = new ArrayDeque<>();

// 场景3: 需要频繁插入删除
Deque<String> deque = new LinkedList<>();

// 场景4: 生产者-消费者模式
BlockingQueue<String> queue = new LinkedBlockingQueue<>();
```

#### 5.5.2 避免常见错误

**错误1: 使用null元素**

```java
// 错误示例
PriorityQueue<String> queue = new PriorityQueue<>();
queue.add(null); // NullPointerException

// 正确示例
queue.add("Hello");
```

**错误2: 使用==比较元素**

```java
// 错误示例
Queue<String> queue = new PriorityQueue<>();
queue.add(new String("Hello"));

if (queue.peek() == "Hello") { // 可能返回false
    // ...
}

// 正确示例
if (queue.peek().equals("Hello")) { // 正确
    // ...
}
```

**错误3: 在迭代时修改队列**

```java
// 错误示例
Queue<String> queue = new LinkedList<>();
queue.add("A");
queue.add("B");
queue.add("C");

for (String s : queue) {
    if (s.equals("B")) {
        queue.remove(s); // ConcurrentModificationException
    }
}

// 正确示例1: 使用Iterator
Iterator<String> iterator = queue.iterator();
while (iterator.hasNext()) {
    String s = iterator.next();
    if (s.equals("B")) {
        iterator.remove();
    }
}

// 正确示例2: 使用poll
while (!queue.isEmpty()) {
    String s = queue.poll();
    if (s.equals("B")) {
        break;
    }
}
```

#### 5.5.3 性能优化建议

**1. 预估队列大小**

```java
// 错误示例
PriorityQueue<String> queue = new PriorityQueue<>();

// 正确示例
PriorityQueue<String> queue = new PriorityQueue<>(1000);
```

**2. 使用合适的队列类型**

```java
// 场景1: 需要优先级排序
Queue<Task> queue = new PriorityQueue<>();

// 场景2: 需要双端操作
Deque<String> deque = new ArrayDeque<>();

// 场景3: 需要频繁插入删除
Deque<String> deque = new LinkedList<>();
```

### 5.6 小结

Queue体系是Java集合框架中的重要部分,理解其实现原理对于编写高效、正确的代码至关重要。

**核心要点总结:**

第一,**PriorityQueue**: 基于堆,优先级排序,查找O(1),插入删除O(log n)。

第二,**ArrayDeque**: 基于数组,双端操作,大部分操作O(1)。

第三,**LinkedList**: 基于链表,双端操作,插入删除O(1),查找O(n)。

第四,**性能对比**: PriorityQueue适合优先级排序,ArrayDeque适合双端操作,LinkedList适合频繁插入删除。

第五,**最佳实践**: 选择合适的Queue类型、避免常见错误、优化性能。
