# PriorityQueue.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.util.PriorityQueue` 是 Java 1.5 引入的基于优先级堆（Priority Heap）的无界队列实现。它继承自 `AbstractQueue`，实现了 `Serializable` 接口，提供了基于元素优先级的队列操作。PriorityQueue 的核心特性是：队首元素（head）总是优先级最高（或最低，取决于排序规则）的元素，这使得它非常适合需要按优先级处理元素的场景。

PriorityQueue 内部使用二叉堆（Binary Heap）数据结构来维护元素的优先级顺序。二叉堆是一种完全二叉树，其中每个节点的值都小于（最小堆）或大于（最大堆）其所有子节点的值。PriorityQueue 实现的是最小堆，因此队首元素总是最小的元素。

### 1.2 核心设计原则

PriorityQueue 的设计体现了几个重要的软件工程原则。首先是**数据结构适配**原则，通过二叉堆这种高效的数据结构来实现优先队列的语义。二叉堆的插入、删除操作时间复杂度为 O(log n)，查找最小元素为 O(1)，这使得 PriorityQueue 能够高效地支持队列的基本操作。

其次是**泛型与比较器分离**原则。PriorityQueue 可以使用元素的自然顺序（通过 Comparable 接口），也可以使用自定义的 Comparator。这种设计提供了极大的灵活性，允许开发者根据业务需求定义优先级规则。

第三是**容量自动管理**原则。虽然 PriorityQueue 是"无界"的（理论上可以容纳任意数量的元素），但它内部使用数组存储，因此有实际的容量限制。PriorityQueue 采用按需扩容的策略，当容量不足时自动增长，对调用者完全透明。

第四是**迭代器弱一致性**原则。PriorityQueue 的迭代器不保证按任何特定顺序遍历元素，这是为了保持迭代器的高效性。如果需要有序遍历，应该先转换为数组再排序。

### 1.3 与相关类的关系

PriorityQueue 在 Java 集合框架中与多个类存在密切的协作关系。`AbstractQueue` 是 PriorityQueue 的父类，提供了 Queue 接口的部分默认实现。`Queue` 接口定义了队列的基本操作（offer、poll、peek 等），PriorityQueue 实现了这些接口。`PriorityBlockingQueue` 是 PriorityQueue 的线程安全版本，位于 `java.util.concurrent` 包中，适用于多线程环境。`SortedSet` 是有序集合接口，PriorityQueue 与 SortedSet 类似，都维护元素的顺序，但 SortedSet 不允许重复元素且提供范围查询，而 PriorityQueue 允许重复且只提供队首访问。

### 1.4 应用场景

PriorityQueue 在以下场景中被广泛使用：任务调度系统，按优先级执行任务；事件驱动系统，按时间或优先级处理事件；Dijkstra 最短路径算法、Prim 最小生成树算法等图算法；Huffman 编码、堆排序等算法实现；以及任何需要按优先级处理元素的场景。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.util.AbstractCollection
        └─ java.util.AbstractQueue
            └─ java.util.PriorityQueue
```

PriorityQueue 继承自 `AbstractQueue`，而 AbstractQueue 又继承自 `AbstractCollection`。这种继承层次使得 PriorityQueue 获得了集合框架的基础功能，同时专注于队列操作的实现。

### 2.2 实现的接口

PriorityQueue 实现了 `Serializable` 接口，这意味着它可以被序列化存储或传输。PriorityQueue 提供了自定义的 `writeObject` 和 `readObject` 方法来处理序列化过程，确保反序列化后堆结构仍然有效。

### 2.3 与 Queue 接口的关系

PriorityQueue 实现了 `Queue` 接口的所有核心方法：

- `offer(E e)` - 将元素插入队列，成功返回 true
- `poll()` - 获取并移除队首元素，队列为空时返回 null
- `peek()` - 获取但不移除队首元素，队列为空时返回 null
- `add(E e)` - 将元素插入队列，失败时抛出异常（PriorityQueue 中等同于 offer）

## 三、核心字段详解

### 3.1 内部数组

```java
transient Object[] queue;
```

`queue` 字段是存储元素的内部数组。PriorityQueue 使用数组来实现二叉堆，这种实现方式被称为"基于数组的二叉堆"（Array-based Binary Heap）。数组索引与堆节点的关系如下：

- 对于索引为 i 的节点：
  - 左子节点索引为 2*i + 1
  - 右子节点索引为 2*i + 2
  - 父节点索引为 (i-1) / 2

这种表示方式非常高效，因为可以通过简单的算术运算在父节点和子节点之间移动，无需指针操作。

### 3.2 当前大小

```java
private int size = 0;
```

`size` 字段记录队列中当前存储的元素数量。这个字段与 `queue.length`（数组容量）不同，size 表示实际使用的元素数，而 queue.length 表示数组的总容量。

### 3.3 比较器

```java
private final Comparator<? super E> comparator;
```

`comparator` 字段用于元素比较。如果 comparator 为 null，则使用元素的自然顺序（Comparable）；否则使用指定的 Comparator 进行比较。这个字段是 final 的，一旦初始化就不能改变，这确保了队列的排序规则在整个生命周期内保持一致。

### 3.4 修改计数器

```java
transient int modCount = 0;
```

`modCount` 字段用于快速失败（fail-fast）迭代器。每次结构性修改（添加、删除元素）都会递增这个计数器。迭代器在创建时记录当前的 modCount，在每次操作时检查是否与队列当前的 modCount 一致，如果不一致则抛出 `ConcurrentModificationException`。

### 3.5 默认初始容量

```java
private static final int DEFAULT_INITIAL_CAPACITY = 11;
```

默认初始容量为 11，这是一个经验值。选择 11 而不是 10 或 16 是基于性能测试的结果。对于小规模的优先队列，11 的初始容量可以避免过早触发扩容，同时又不会浪费太多内存。

### 3.6 最大数组大小

```java
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

`MAX_ARRAY_SIZE` 定义了数组的最大大小限制。设置为 `Integer.MAX_VALUE - 8` 是为了考虑某些虚拟机在数组头部保留一些字节的实现细节。尝试分配更大的数组会导致 `OutOfMemoryError`。

## 四、构造函数详解

### 4.1 默认构造函数

```java
public PriorityQueue() {
    this(DEFAULT_INITIAL_CAPACITY, null);
}
```

默认构造函数创建一个空的 PriorityQueue，使用默认初始容量（11）和元素的自然顺序。这是最常用的构造方式，适用于元素实现了 Comparable 接口的场景。

### 4.2 指定初始容量

```java
public PriorityQueue(int initialCapacity) {
    this(initialCapacity, null);
}
```

这个构造函数允许指定初始容量，使用元素的自然顺序。如果能够预估队列的大致大小，指定合适的初始容量可以避免扩容操作，提高性能。

### 4.3 指定比较器

```java
public PriorityQueue(Comparator<? super E> comparator) {
    this(DEFAULT_INITIAL_CAPACITY, comparator);
}
```

这个构造函数创建一个使用指定比较器的 PriorityQueue。当元素没有实现 Comparable 接口，或者需要自定义排序规则时，应该使用这个构造函数。

### 4.4 完整参数构造函数

```java
public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.queue = new Object[initialCapacity];
    this.comparator = comparator;
}
```

这是所有构造函数的底层实现。它验证初始容量必须至少为 1（虽然技术上可以接受 0，但为了保持与 Java 1.5 的兼容性，这个限制被保留），然后创建指定大小的数组并保存比较器。

### 4.5 从集合初始化

```java
public PriorityQueue(Collection<? extends E> c) {
    if (c instanceof SortedSet<?>) {
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator();
        initElementsFromCollection(ss);
    } else if (c instanceof PriorityQueue<?>) {
        PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator();
        initFromPriorityQueue(pq);
    } else {
        this.comparator = null;
        initFromCollection(c);
    }
}
```

这个构造函数从现有集合创建 PriorityQueue。它智能地处理不同类型的集合：

- 如果是 `SortedSet`，直接使用其比较器，因为 SortedSet 已经是有序的
- 如果是另一个 `PriorityQueue`，复制其比较器和内部结构
- 如果是普通集合，使用自然顺序并执行堆化（heapify）操作

### 4.6 从 SortedSet 初始化

```java
public PriorityQueue(SortedSet<? extends E> c) {
    this.comparator = (Comparator<? super E>) c.comparator();
    initElementsFromCollection(c);
}
```

这个构造函数专门用于从 SortedSet 创建 PriorityQueue。由于 SortedSet 已经是有序的，可以直接复制元素，无需堆化操作。

## 五、核心方法详解

### 5.1 添加元素

```java
public boolean add(E e) {
    return offer(e);
}

public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    if (i >= queue.length)
        grow(i + 1);
    size = i + 1;
    if (i == 0)
        queue[0] = e;
    else
        siftUp(i, e);
    return true;
}
```

`add` 和 `offer` 方法用于向队列添加元素。PriorityQueue 不允许 null 元素，传入 null 会抛出 NullPointerException。添加操作的时间复杂度为 O(log n)，因为可能需要执行 siftUp（上浮）操作来维护堆的性质。

添加过程如下：
1. 检查元素是否为 null
2. 递增 modCount
3. 检查是否需要扩容
4. 如果是第一个元素，直接放在位置 0
5. 否则调用 siftUp 将元素"上浮"到合适的位置

### 5.2 获取队首元素

```java
public E peek() {
    return (size == 0) ? null : (E) queue[0];
}
```

`peek` 方法返回但不移除队首元素。由于队首元素（queue[0]）总是最小的元素，这个操作的时间复杂度为 O(1)。如果队列为空，返回 null。

### 5.3 移除队首元素

```java
public E poll() {
    if (size == 0)
        return null;
    int s = --size;
    modCount++;
    E result = (E) queue[0];
    E x = (E) queue[s];
    queue[s] = null;
    if (s != 0)
        siftDown(0, x);
    return result;
}
```

`poll` 方法获取并移除队首元素。移除操作的时间复杂度为 O(log n)，因为需要执行 siftDown（下沉）操作来重新维护堆的性质。

移除过程如下：
1. 检查队列是否为空
2. 递减 size（队列大小减 1）
3. 递增 modCount
4. 保存队首元素作为返回值
5. 将最后一个元素移动到队首位置
6. 调用 siftDown 将元素"下沉"到合适的位置

### 5.4 移除指定元素

```java
public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1)
        return false;
    else {
        removeAt(i);
        return true;
    }
}
```

`remove` 方法移除队列中指定的元素。这个操作的时间复杂度为 O(n)，因为需要先线性搜索元素的位置，然后再执行堆调整操作。

### 5.5 包含检查

```java
public boolean contains(Object o) {
    return indexOf(o) != -1;
}
```

`contains` 方法检查队列是否包含指定元素。由于需要线性搜索，时间复杂度为 O(n)。

### 5.6 堆化操作

```java
private void heapify() {
    for (int i = (size >>> 1) - 1; i >= 0; i--)
        siftDown(i, (E) queue[i]);
}
```

`heapify` 方法将无序的数组转换为有效的堆结构。它从最后一个非叶子节点开始，向前依次对每个节点执行 siftDown 操作。这个过程的时间复杂度为 O(n)，比逐个插入的 O(n log n) 更高效。

### 5.7 上浮操作

```java
private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x);
    else
        siftUpComparable(k, x);
}

private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        if (key.compareTo((E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}
```

`siftUp` 方法将元素"上浮"到合适的位置，以维护堆的性质。在最小堆中，如果子节点小于父节点，就交换它们的位置，直到子节点不小于父节点或到达根节点。

### 5.8 下沉操作

```java
private void siftDown(int k, E x) {
    if (comparator != null)
        siftDownUsingComparator(k, x);
    else
        siftDownComparable(k, x);
}

private void siftDownComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>)x;
    int half = size >>> 1;
    while (k < half) {
        int child = (k << 1) + 1;
        Object c = queue[child];
        int right = child + 1;
        if (right < size &&
            ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
            c = queue[child = right];
        if (key.compareTo((E) c) <= 0)
            break;
        queue[k] = c;
        k = child;
    }
    queue[k] = key;
}
```

`siftDown` 方法将元素"下沉"到合适的位置。在最小堆中，如果父节点大于任一子节点，就与较小的子节点交换，直到父节点不大于子节点或成为叶子节点。

## 六、容量管理与扩容机制

### 6.1 扩容策略

```java
private void grow(int minCapacity) {
    int oldCapacity = queue.length;
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                     (oldCapacity + 2) :
                                     (oldCapacity >> 1));
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    queue = Arrays.copyOf(queue, newCapacity);
}
```

PriorityQueue 的扩容策略与 ArrayList 类似，但略有不同：

- 对于小数组（容量小于 64），新容量 = 旧容量 + 2
- 对于大数组，新容量 = 旧容量 * 1.5（旧容量右移 1 位相当于除以 2，然后加上旧容量）

这种策略在小数组时增长较慢，避免浪费内存；在大数组时增长较快，减少扩容次数。

### 6.2 巨大容量处理

```java
private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0)
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```

当需要分配超大数组时，`hugeCapacity` 方法处理边界情况。如果最小容量已经溢出（为负数），直接抛出 OutOfMemoryError；否则返回 MAX_ARRAY_SIZE 或 Integer.MAX_VALUE。

## 七、迭代器实现

### 7.1 迭代器结构

```java
private final class Itr implements Iterator<E> {
    private int cursor = 0;
    private int lastRet = -1;
    private ArrayDeque<E> forgetMeNot = null;
    private E lastRetElt = null;
    private int expectedModCount = modCount;
}
```

PriorityQueue 的迭代器是一个内部类 `Itr`，它实现了 `Iterator<E>` 接口。迭代器维护了几个关键字段：

- `cursor`：下一个要返回的元素的索引
- `lastRet`：最近一次 next 返回的元素的索引（用于 remove）
- `forgetMeNot`：存储在迭代过程中被"不幸移除"的元素（需要 siftUp 而非 siftDown）
- `lastRetElt`：最近一次 next 返回的元素（如果来自 forgetMeNot）
- `expectedModCount`：预期的修改计数器，用于快速失败检测

### 7.2 快速失败机制

```java
public E next() {
    if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
    if (cursor < size)
        return (E) queue[lastRet = cursor++];
    if (forgetMeNot != null) {
        lastRet = -1;
        lastRetElt = forgetMeNot.poll();
        if (lastRetElt != null)
            return lastRetElt;
    }
    throw new NoSuchElementException();
}
```

迭代器在每次操作时检查 `expectedModCount` 是否等于队列当前的 `modCount`，如果不相等则抛出 `ConcurrentModificationException`。这种机制确保了迭代器能够快速检测到并发修改，避免产生不确定的行为。

### 7.3 迭代器顺序

需要注意的是，PriorityQueue 的迭代器不保证按任何特定顺序遍历元素。这是设计上的权衡：如果要保证有序遍历，每次迭代都需要维护堆的性质，这会严重影响性能。如果需要有序遍历，应该：

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
// ... 添加元素

// 不好的方式：迭代器顺序不确定
for (Integer i : pq) {
    System.out.println(i);
}

// 好的方式：转换为数组再排序
Integer[] arr = pq.toArray(new Integer[0]);
Arrays.sort(arr);
for (Integer i : arr) {
    System.out.println(i);
}
```

## 八、设计模式分析

### 8.1 策略模式

PriorityQueue 通过 `Comparator` 接口实现了策略模式。元素的比较策略可以在运行时指定，这使得同一个 PriorityQueue 可以适应不同的排序需求：

```java
// 使用自然顺序
PriorityQueue<Integer> pq1 = new PriorityQueue<>();

// 使用逆序
PriorityQueue<Integer> pq2 = new PriorityQueue<>(Collections.reverseOrder());

// 使用自定义比较器
PriorityQueue<String> pq3 = new PriorityQueue<>(
    Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
```

### 8.2 模板方法模式

`siftUp` 和 `siftDown` 方法体现了模板方法模式。这两个方法定义了上浮和下沉的算法骨架，但具体的比较操作委托给 `siftUpComparable`、`siftUpUsingComparator`、`siftDownComparable`、`siftDownUsingComparator` 四个方法实现。这种设计避免了代码重复，同时保持了算法的统一性。

### 8.3 迭代器模式

PriorityQueue 的迭代器实现了经典的迭代器模式。迭代器提供了一种统一的方式来遍历集合元素，而不暴露集合的内部结构。PriorityQueue 的迭代器虽然不保证顺序，但仍然提供了 hasNext、next、remove 等标准方法。

## 九、常见使用模式

### 9.1 任务调度

```java
PriorityQueue<Task> taskQueue = new PriorityQueue<>(
    Comparator.comparing(Task::getPriority).reversed()
);

taskQueue.offer(new Task("Task1", 5));
taskQueue.offer(new Task("Task2", 3));
taskQueue.offer(new Task("Task3", 8));

while (!taskQueue.isEmpty()) {
    Task task = taskQueue.poll();
    task.execute();
}
```

### 9.2 优先级事件处理

```java
PriorityQueue<Event> eventQueue = new PriorityQueue<>(
    Comparator.comparing(Event::getTimestamp));

eventQueue.offer(new Event("Event1", 1000));
eventQueue.offer(new Event("Event2", 500));
eventQueue.offer(new Event("Event3", 1500));

while (!eventQueue.isEmpty()) {
    Event event = eventQueue.poll();
    event.process();
}
```

### 9.3 Top-K 问题

```java
public List<Integer> findTopK(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
    
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }
    
    return new ArrayList<>(minHeap);
}
```

### 9.4 合并有序列表

```java
public List<Integer> mergeSortedLists(List<List<Integer>> lists) {
    PriorityQueue<int[]> minHeap = new PriorityQueue<>(
        Comparator.comparingInt(a -> a[0]));
    
    for (int i = 0; i < lists.size(); i++) {
        if (!lists.get(i).isEmpty()) {
            minHeap.offer(new int[]{lists.get(i).get(0), i, 0});
        }
    }
    
    List<Integer> result = new ArrayList<>();
    while (!minHeap.isEmpty()) {
        int[] top = minHeap.poll();
        result.add(top[0]);
        int listIndex = top[1];
        int elementIndex = top[2];
        if (elementIndex + 1 < lists.get(listIndex).size()) {
            int nextVal = lists.get(listIndex).get(elementIndex + 1);
            minHeap.offer(new int[]{nextVal, listIndex, elementIndex + 1});
        }
    }
    
    return result;
}
```

## 十、常见问题与注意事项

### 10.1 不允许 null 元素

PriorityQueue 不允许 null 元素，尝试添加 null 会抛出 NullPointerException：

```java
PriorityQueue<String> pq = new PriorityQueue<>();
pq.offer(null);  // 抛出 NullPointerException
```

### 10.2 迭代器顺序不确定

PriorityQueue 的迭代器不保证按任何特定顺序遍历元素。如果需要有序遍历，应该转换为数组再排序：

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
// ... 添加元素

// 迭代器顺序不确定
for (Integer i : pq) {
    System.out.println(i);
}

// 有序遍历
Integer[] arr = pq.toArray(new Integer[0]);
Arrays.sort(arr);
for (Integer i : arr) {
    System.out.println(i);
}
```

### 10.3 线程安全性

PriorityQueue 不是线程安全的，不应在多线程环境中共享使用。如果需要线程安全的优先队列，应该使用 `PriorityBlockingQueue`：

```java
// 错误：多线程共享 PriorityQueue
PriorityQueue<Integer> shared = new PriorityQueue<>();
// 多个线程同时访问 shared 会导致数据损坏

// 正确：使用 PriorityBlockingQueue
PriorityBlockingQueue<Integer> blockingQueue = new PriorityBlockingQueue<>();
```

### 10.4 时间复杂度

PriorityQueue 各操作的时间复杂度：
- offer/add：O(log n)
- poll/remove（队首）：O(log n)
- peek/element：O(1)
- remove（指定元素）：O(n)
- contains：O(n)

理解这些时间复杂度对于选择合适的数据结构很重要。

### 10.5 内存占用

PriorityQueue 的内存占用主要来自内部数组。即使队列为空，也会占用初始容量的内存空间。对于大容量队列，应该考虑内存使用：

```java
// 不好的实践：创建超大容量的队列
PriorityQueue<Integer> pq = new PriorityQueue<>(1000000);

// 好的实践：使用合理的初始容量
PriorityQueue<Integer> pq = new PriorityQueue<>(100);
```

## 十一、面试常见问题

### 11.1 PriorityQueue 的底层实现是什么？

PriorityQueue 使用二叉堆（Binary Heap）作为底层实现。二叉堆是一种完全二叉树，其中每个节点的值都小于其所有子节点的值（最小堆）。PriorityQueue 使用数组来实现二叉堆，通过索引关系来表示父子关系。

### 11.2 PriorityQueue 的 offer 和 add 有什么区别？

在 PriorityQueue 中，`offer` 和 `add` 方法功能完全相同，都返回 true。`add` 方法只是简单调用 `offer` 方法。这种设计是为了保持与 Queue 接口的一致性，`add` 方法在失败时抛出异常，而 `offer` 方法返回 false（但在 PriorityQueue 中不会失败）。

### 11.3 PriorityQueue 的迭代器顺序是什么？

PriorityQueue 的迭代器不保证按任何特定顺序遍历元素。这是为了保持迭代器的高效性。如果需要有序遍历，应该先转换为数组再排序。

### 11.4 PriorityQueue 的时间复杂度是多少？

offer/add：O(log n)；poll/remove（队首）：O(log n)；peek/element：O(1)；remove（指定元素）：O(n)；contains：O(n)。

### 11.5 PriorityQueue 是线程安全的吗？

PriorityQueue 不是线程安全的，不应在多线程环境中共享使用。如果需要线程安全的优先队列，应该使用 `PriorityBlockingQueue`。

### 11.6 PriorityQueue 如何实现扩容？

PriorityQueue 使用按需扩容的策略。当容量不足时，新容量计算为：小数组（容量小于 64）时，新容量 = 旧容量 + 2；大数组时，新容量 = 旧容量 * 1.5。这种策略在小数组时增长较慢，避免浪费内存；在大数组时增长较快，减少扩容次数。

## 十二、与其他类的对比

### 12.1 PriorityQueue 与 LinkedList

LinkedList 也实现了 Queue 接口，但它实现的是先进先出（FIFO）队列，而 PriorityQueue 实现的是优先级队列。LinkedList 的 offer/add 在尾部添加，poll/remove 从头部移除，时间复杂度为 O(1)；PriorityQueue 的 offer/add 和 poll/remove 的时间复杂度为 O(log n)。

### 12.2 PriorityQueue 与 ArrayDeque

ArrayDeque 是双端队列，支持在两端高效地添加和移除元素，时间复杂度为 O(1)。PriorityQueue 只支持从队首移除，时间复杂度为 O(log n)。ArrayDeque 适合实现栈和队列，而 PriorityQueue 适合实现优先队列。

### 12.3 PriorityQueue 与 TreeSet

TreeSet 和 PriorityQueue 都维护元素的顺序，但有几个重要区别：TreeSet 不允许重复元素，PriorityQueue 允许重复；TreeSet 提供范围查询（subSet、headSet、tailSet），PriorityQueue 不提供；TreeSet 的迭代器按有序顺序遍历，PriorityQueue 的迭代器顺序不确定；TreeSet 的 add/remove 时间复杂度为 O(log n)，PriorityQueue 的 offer/poll 为 O(log n)，但 remove（指定元素）为 O(n)。

### 12.4 PriorityQueue 与 PriorityBlockingQueue

PriorityBlockingQueue 是 PriorityQueue 的线程安全版本，位于 `java.util.concurrent` 包中。PriorityBlockingQueue 的所有操作都是线程安全的，使用 ReentrantLock 实现同步。PriorityBlockingQueue 的 offer 方法在队列满时会阻塞（虽然理论上是无界的），而 PriorityQueue 的 offer 方法永远不会阻塞（会扩容）。

### 12.5 PriorityQueue 与 Guava 的 MinMaxPriorityQueue

Google Guava 库提供了 `MinMaxPriorityQueue`，它同时支持获取最小和最大元素：

```java
MinMaxPriorityQueue<Integer> mmPq = MinMaxPriorityQueue.orderedBy(Comparator.naturalOrder()).create();
mmPq.offer(5);
mmPq.offer(3);
mmPq.offer(8);

mmPq.peekFirst();   // 3（最小）
mmPq.peekLast();    // 8（最大）
```

PriorityQueue 只支持获取最小元素（队首），而 MinMaxPriorityQueue 同时支持获取最小和最大元素。但引入 Guava 增加了项目依赖。
