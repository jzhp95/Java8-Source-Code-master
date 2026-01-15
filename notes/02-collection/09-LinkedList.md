# LinkedList.java 源码深度解析

## 一、类的概述与设计定位

### 1.1 基本信息

`java.util.LinkedList` 是 Java 集合框架中基于双向链表（Doubly-Linked List）实现的 List 和 Deque 接口。它继承自 `AbstractSequentialList`，实现了 `List`、`Deque`、`Cloneable` 和 `Serializable` 接口。LinkedList 的核心特性是：允许 null 元素；支持在列表两端高效地插入和删除元素；实现了 List 和 Deque 的所有操作。

LinkedList 是 Java 1.2 引入的经典数据结构实现，与 ArrayList 形成互补。ArrayList 基于数组，适合随机访问；LinkedList 基于链表，适合频繁的插入和删除操作。

### 1.2 核心设计原则

LinkedList 的设计体现了几个重要的软件工程原则。首先是**数据结构适配**原则，通过双向链表这种数据结构来实现 List 和 Deque 的语义。双向链表使得在列表两端插入和删除元素的时间复杂度为 O(1)，这是 LinkedList 相比 ArrayList 的核心优势。

其次是**接口多态性**原则。LinkedList 同时实现了 List 和 Deque 接口，这意味着它可以用作列表、队列、双端队列或栈。这种多态性使得同一个数据结构可以适应不同的使用场景。

第三是**迭代器快速失败**原则。LinkedList 的迭代器实现了快速失败（fail-fast）机制，当检测到并发修改时抛出 `ConcurrentModificationException`。虽然快速失败不能保证在所有并发修改场景下都有效，但它能够快速检测到大多数并发修改问题。

第四是**内存效率优先**原则。LinkedList 在插入和删除元素时不需要移动其他元素，只需要调整指针，这使得它在频繁修改的场景下比 ArrayList 更高效。但 LinkedList 的每个节点都需要额外的空间存储前后指针，这在内存使用上是一个权衡。

### 1.3 与相关类的关系

LinkedList 在 Java 集合框架中与多个类存在密切的协作和对比关系。`AbstractSequentialList` 是 LinkedList 的父类，提供了顺序访问列表的部分默认实现。`ArrayList` 是 LinkedList 的主要对比对象，两者都实现 List 接口，但内部实现不同。`ArrayDeque` 是基于数组实现的双端队列，与 LinkedList 的 Deque 功能类似但性能特性不同。`Deque` 是接口，定义了双端队列的标准操作。

### 1.4 应用场景

LinkedList 在以下场景中被广泛使用：需要频繁在列表两端插入和删除元素；实现队列（先进先出）或双端队列；实现栈（后进先出）；需要频繁插入和删除但随机访问较少的场景；以及任何需要 List 和 Deque 双重功能的场景。

## 二、继承结构与接口实现

### 2.1 类的继承层次

```
java.lang.Object
    └─ java.util.AbstractCollection
        └─ java.util.AbstractList
            └─ java.util.AbstractSequentialList
                └─ java.util.LinkedList
```

LinkedList 继承自 `AbstractSequentialList`，而 AbstractSequentialList 又继承自 AbstractList。这种继承层次使得 LinkedList 获得了 List 接口的部分默认实现，同时专注于顺序访问列表的特性。

### 2.2 实现的接口

LinkedList 实现了多个接口，每个接口都提供了不同的功能：

`List` 接口定义了列表的基本操作（get、set、add、remove 等），LinkedList 提供了这些操作的链表实现。`Deque` 接口定义了双端队列的操作（addFirst、addLast、pollFirst、pollLast 等），LinkedList 通过双向链表高效地支持这些操作。`Cloneable` 接口表示 LinkedList 可以被克隆。`Serializable` 接口表示 LinkedList 可以被序列化存储或传输。

### 2.3 与 ArrayList 的对比

LinkedList 和 ArrayList 都实现 List 接口，但内部实现和性能特性有显著差异：

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 内部结构 | 动态数组 | 双向链表 |
| 随机访问 | O(1) | O(n) |
| 头尾插入删除 | O(n) | O(1) |
| 中间插入删除 | O(n) | O(n) |
| 内存开销 | 较小 | 较大（每个节点两个指针）|
| 缓存局部性 | 好 | 差 |

## 三、核心字段详解

### 3.1 节点结构

```java
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
```

`Node` 是 LinkedList 的内部类，表示链表中的一个节点。每个节点包含三个字段：`item` 存储节点元素；`next` 指向下一个节点；`prev` 指向前一个节点。这种双向链表结构使得可以在两个方向上高效地遍历列表。

### 3.2 首尾指针

```java
transient Node<E> first;
transient Node<E> last;
```

`first` 指针指向链表的第一个节点，`last` 指针指向链表的最后一个节点。这两个指针使得在链表头部和尾部插入和删除元素的时间复杂度为 O(1)。如果链表为空，则 first 和 last 都为 null。

### 3.3 大小字段

```java
transient int size = 0;
```

`size` 字段记录链表中当前存储的节点数量。这个字段使得 `size()` 方法的时间复杂度为 O(1)，无需遍历整个链表。

### 3.4 修改计数器

```java
transient int modCount = 0;
```

`modCount` 字段用于快速失败迭代器。每次结构性修改（添加或删除节点）都会递增这个计数器。迭代器在创建时记录当前的 modCount，在每次操作时检查是否与链表当前的 modCount 一致，如果不一致则抛出 `ConcurrentModificationException`。

## 四、核心方法详解

### 4.1 添加元素到头部

```java
private void linkFirst(E e) {
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
```

`linkFirst` 方法将元素插入到链表头部。操作步骤如下：保存当前的首节点；创建新节点，其 prev 为 null，next 为当前首节点；将 first 指向新节点；如果链表原为空，则 last 也指向新节点；否则原首节点的 prev 指向新节点；递增 size 和 modCount。

### 4.2 添加元素到尾部

```java
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
```

`linkLast` 方法将元素插入到链表尾部。操作步骤与 `linkFirst` 对称：保存当前的尾节点；创建新节点，其 prev 为当前尾节点，next 为 null；将 last 指向新节点；如果链表原为空，则 first 也指向新节点；否则原尾节点的 next 指向新节点；递增 size 和 modCount。

### 4.3 在指定节点前插入

```java
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
```

`linkBefore` 方法在指定节点 succ 之前插入新元素。操作步骤：获取 succ 的前驱节点 pred；创建新节点，其 prev 为 pred，next 为 succ；将 succ 的 prev 指向新节点；如果 pred 为 null（succ 是首节点），则 first 指向新节点；否则 pred 的 next 指向新节点；递增 size 和 modCount。

### 4.4 移除首节点

```java
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
```

`unlinkFirst` 方法移除链表的首节点。操作步骤：保存节点元素和后继节点；清空被移除节点的引用（帮助 GC）；将 first 指向后继节点；如果后继为 null（链表只有一个节点），则 last 也设为 null；否则后继节点的 prev 设为 null；递减 size，递增 modCount，返回被移除的元素。

### 4.5 移除尾节点

```java
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

`unlinkLast` 方法移除链表的尾节点。操作步骤与 `unlinkFirst` 对称：保存节点元素和前驱节点；清空被移除节点的引用；将 last 指向前驱节点；如果前驱为 null（链表只有一个节点），则 first 也设为 null；否则前驱节点的 next 设为 null；递减 size，递增 modCount，返回被移除的元素。

### 4.6 移除指定节点

```java
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
```

`unlink` 方法移除指定的节点。操作步骤：保存节点元素、后继和前驱；如果前驱为 null（x 是首节点），则 first 指向后继；否则前驱的 next 指向后继；如果后继为 null（x 是尾节点），则 last 指向前驱；否则后继的 prev 指向前驱；清空被移除节点的所有引用；递减 size，递增 modCount，返回被移除的元素。

### 4.7 获取指定索引的节点

```java
Node<E> node(int index) {
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

`node` 方法获取指定索引的节点。这个方法体现了 LinkedList 的一个重要优化：根据索引位置选择从前往后还是从后往前遍历。如果索引小于 size/2，则从前往后遍历；否则从后往前遍历。这种优化使得平均遍历距离为 size/4，而不是 size/2。

### 4.8 List 接口操作

```java
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}

public E set(int index, E element) {
    checkElementIndex(index);
    Node<E> x = node(index);
    E oldVal = x.item;
    x.item = element;
    return oldVal;
}

public void add(int index, E element) {
    checkPositionIndex(index);
    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));
}

public E remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));
}
```

这些 List 接口方法都通过 `node` 方法定位到指定索引的节点，然后执行相应操作。`get` 和 `set` 的时间复杂度为 O(n)，因为需要先遍历到指定索引；`add` 和 `remove` 的时间复杂度也是 O(n)，但实际插入或删除操作本身是 O(1) 的。

### 4.9 Deque 接口操作

```java
public void addFirst(E e) {
    linkFirst(e);
}

public void addLast(E e) {
    linkLast(e);
}

public E getFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return f.item;
}

public E getLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return l.item;
}

public E removeFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return unlinkFirst(f);
}

public E removeLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return unlinkLast(l);
}
```

这些 Deque 接口方法直接操作链表的首尾节点，时间复杂度都是 O(1)。这使得 LinkedList 非常适合用作双端队列。

### 4.10 Queue 接口操作

```java
public E peek() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}

public E element() {
    return getFirst();
}

public E poll() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}

public E remove() {
    return removeFirst();
}

public boolean offer(E e) {
    return add(e);
}
```

这些 Queue 接口方法都是对 Deque 方法的简单封装。`peek` 和 `poll` 在队列为空时返回 null，而 `element` 和 `remove` 会抛出异常。

## 五、迭代器实现

### 5.1 ListIterator 结构

```java
private class ListItr implements ListIterator<E> {
    private Node<E> lastReturned;
    private Node<E> next;
    private int nextIndex;
    private int expectedModCount = modCount;

    ListItr(int index) {
        next = (index == size) ? null : node(index);
        nextIndex = index;
    }

    public boolean hasNext() {
        return nextIndex < size;
    }

    public E next() {
        checkForComodification();
        if (!hasNext())
            throw new NoSuchElementException();
        lastReturned = next;
        next = next.next;
        nextIndex++;
        return lastReturned.item;
    }

    public boolean hasPrevious() {
        return nextIndex > 0;
    }

    public E previous() {
        checkForComodification();
        if (!hasPrevious())
            throw new NoSuchElementException();
        lastReturned = next = (next == null) ? last : next.prev;
        nextIndex--;
        return lastReturned.item;
    }

    public void remove() {
        checkForComodification();
        if (lastReturned == null)
            throw new IllegalStateException();
        Node<E> lastNext = lastReturned.next;
        unlink(lastReturned);
        if (next == lastReturned)
            next = lastNext;
        else
            nextIndex--;
        lastReturned = null;
        expectedModCount++;
    }

    public void set(E e) {
        if (lastReturned == null)
            throw new IllegalStateException();
        checkForComodification();
        lastReturned.item = e;
    }

    public void add(E e) {
        checkForComodification();
        lastReturned = null;
        if (next == null)
            linkLast(e);
        else
            linkBefore(e, next);
        nextIndex++;
        expectedModCount++;
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

`ListItr` 是 LinkedList 的内部类，实现了 `ListIterator` 接口。迭代器维护了几个关键字段：`lastReturned` 记录最近一次 next 或 previous 返回的节点；`next` 记录下一个要返回的节点；`nextIndex` 记录 next 方法将返回的索引；`expectedModCount` 记录预期的修改计数器，用于快速失败检测。

### 5.2 快速失败机制

迭代器的 `checkForComodification` 方法在每次操作时检查 `expectedModCount` 是否等于链表当前的 `modCount`。如果不相等，则抛出 `ConcurrentModificationException`。这种机制确保了迭代器能够快速检测到并发修改，避免产生不确定的行为。

### 5.3 降序迭代器

```java
public Iterator<E> descendingIterator() {
    return new DescendingIterator();
}

private class DescendingIterator implements Iterator<E> {
    private final ListItr itr = new ListItr(size());

    public boolean hasNext() {
        return itr.hasPrevious();
    }

    public E next() {
        return itr.previous();
    }

    public void remove() {
        itr.remove();
    }
}
```

`DescendingIterator` 是一个适配器，它通过反向使用 `ListItr` 来实现降序遍历。这种设计避免了重复实现迭代器逻辑。

## 六、设计模式分析

### 6.1 迭代器模式

LinkedList 的迭代器实现了经典的迭代器模式。迭代器提供了一种统一的方式来遍历集合元素，而不暴露集合的内部结构。LinkedList 的迭代器支持双向遍历（通过 ListIterator），并且实现了快速失败机制。

### 6.2 适配器模式

`DescendingIterator` 体现了适配器模式。它将 `ListItr` 的 `previous` 方法适配为标准 `Iterator` 接口的 `next` 方法，实现了降序遍历的功能。

### 6.3 模板方法模式

LinkedList 的 `node` 方法体现了模板方法模式的思想。虽然它不是一个典型的模板方法，但它根据索引位置选择不同的遍历策略（前往后或后往前），这种策略选择在运行时动态进行。

## 七、常见使用模式

### 7.1 作为队列使用

```java
Queue<String> queue = new LinkedList<>();
queue.offer("task1");
queue.offer("task2");
queue.offer("task3");

while (!queue.isEmpty()) {
    String task = queue.poll();
    process(task);
}
```

### 7.2 作为双端队列使用

```java
Deque<String> deque = new LinkedList<>();
deque.addFirst("first");
deque.addLast("last");
deque.addFirst("second");

String first = deque.pollFirst();
String last = deque.pollLast();
```

### 7.3 作为栈使用

```java
Deque<String> stack = new LinkedList<>();
stack.push("item1");
stack.push("item2");
stack.push("item3");

String top = stack.pop();
```

### 7.4 作为列表使用

```java
List<String> list = new LinkedList<>();
list.add("item1");
list.add("item2");
list.add("item3");

String item = list.get(1);  // "item2"
list.remove(1);
```

## 八、常见问题与注意事项

### 8.1 线程安全性

LinkedList 不是线程安全的，不应在多线程环境中共享使用。如果需要在多线程环境下使用，应该使用 `Collections.synchronizedList` 包装或使用并发集合如 `ConcurrentLinkedDeque`。

### 8.2 随机访问性能

LinkedList 的随机访问性能较差，时间复杂度为 O(n)。如果需要频繁的随机访问，应该使用 ArrayList。

### 8.3 内存开销

LinkedList 的每个节点都需要额外的空间存储前后指针，这在内存使用上是一个开销。对于大量小元素，ArrayList 的内存效率更高。

### 8.4 迭代器快速失败

LinkedList 的迭代器实现了快速失败机制，但这种机制不能保证在所有并发修改场景下都有效。快速失败只能用于检测 bug，不能用于线程安全。

### 8.5 序列化

LinkedList 的序列化会保存所有元素，但不会保存节点之间的链接关系。反序列化时会重新构建链表结构。

## 九、面试常见问题

### 9.1 LinkedList 和 ArrayList 的区别？

LinkedList 基于双向链表，ArrayList 基于动态数组。LinkedList 在头尾插入删除为 O(1)，中间插入删除为 O(n)；ArrayList 在头尾插入删除为 O(n)，中间插入删除为 O(n)。LinkedList 的随机访问为 O(n)，ArrayList 为 O(1)。LinkedList 的内存开销较大（每个节点两个指针），ArrayList 较小。

### 9.2 LinkedList 的时间复杂度是多少？

add/addLast/addFirst：O(1)；getFirst/getLast：O(1)；removeFirst/removeLast：O(1)；get/set：O(n)；add(index)/remove(index)：O(n)；indexOf/lastIndexOf：O(n)；contains：O(n)。

### 9.3 LinkedList 是线程安全的吗？

LinkedList 不是线程安全的。如果需要在多线程环境下使用，应该使用 `Collections.synchronizedList` 包装或使用并发集合如 `ConcurrentLinkedDeque`。

### 9.4 LinkedList 如何实现高效的节点定位？

`node` 方法根据索引位置选择从前往后还是从后往前遍历。如果索引小于 size/2，则从前往后遍历；否则从后往前遍历。这种优化使得平均遍历距离为 size/4。

### 9.5 LinkedList 的迭代器如何实现快速失败？

迭代器在创建时记录当前的 modCount，在每次操作时检查是否与链表当前的 modCount 一致。如果不一致，则抛出 `ConcurrentModificationException`。

### 9.6 什么情况下应该使用 LinkedList？

需要频繁在列表两端插入和删除元素；实现队列、双端队列或栈；需要频繁插入和删除但随机访问较少的场景；以及任何需要 List 和 Deque 双重功能的场景。

## 十、与其他类的对比

### 10.1 LinkedList 与 ArrayList

LinkedList 和 ArrayList 都实现 List 接口，但内部实现和性能特性有显著差异。LinkedList 基于双向链表，适合频繁的插入和删除；ArrayList 基于动态数组，适合频繁的随机访问。

### 10.2 LinkedList 与 ArrayDeque

ArrayDeque 是基于数组实现的双端队列，与 LinkedList 的 Deque 功能类似但性能特性不同。ArrayDeque 的所有操作都是 O(1)，但插入删除可能需要扩容；LinkedList 的头尾操作是 O(1)，但中间操作是 O(n)。

### 10.3 LinkedList 与 ConcurrentLinkedDeque

`ConcurrentLinkedDeque` 是 LinkedList 的线程安全版本，位于 `java.util.concurrent` 包中。ConcurrentLinkedDeque 的所有操作都是线程安全的，使用无锁算法实现。

### 10.4 LinkedList 与 Stack

`Stack` 是 Java 早期提供的栈实现，继承自 Vector。Stack 是线程安全的，但性能较差。LinkedList 通过 Deque 接口提供了更好的栈实现（push、pop、peek），且不是线程安全的，性能更好。

### 10.5 LinkedList 与 Guava 的 Lists

Google Guava 库提供了 `Lists` 工具类，其中包含一些便利方法：

```java
// Guava 的方法
List<Integer> list = Lists.newArrayList();
List<Integer> list = Lists.newArrayList(1, 2, 3);
List<Integer> list = Lists.newLinkedList();

// Java 标准库 LinkedList
List<Integer> list = new LinkedList<>();
list.add(1);
list.add(2);
list.add(3);
```

Guava 提供了更简洁的创建方式，但引入 Guava 增加了项目依赖。
