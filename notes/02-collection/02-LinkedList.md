# LinkedList 源码深度解析

## 一、类概述与设计理念

### 1.1 基本信息

LinkedList 是 Java 集合框架中基于双向链表实现的 List 和 Deque 接口的实现类，位于 `java.util` 包中。作为 ArrayList 的重要补充，LinkedList 提供了另一种序列存储方式，特别适合频繁在列表两端进行插入和删除操作的场景。

LinkedList 的设计理念体现了"链表结构"的核心思想。与 ArrayList 使用连续数组存储元素不同，LinkedList 使用节点（Node）链式存储，每个节点包含元素值以及指向前后节点的指针。这种非连续存储方式决定了 LinkedList 具有独特的性能特征：在任意位置插入和删除的时间复杂度为 O(1)，但随机访问的时间复杂度为 O(n)。

### 1.2 设计特点

LinkedList 的设计体现了以下几个核心特点：

首先是**双向链表结构**。每个节点都包含指向前一个节点的引用（prev）和指向后一个节点的引用（next），形成双向链表。这种设计使得从任意方向遍历列表都很高效，同时也简化了插入和删除操作。

其次是**双端队列支持**。LinkedList 实现了 Deque 接口，提供了在列表两端进行高效插入和删除的能力。这使得 LinkedList 可以同时作为 List 和 Queue（特别是 Deque）使用。

第三是**无容量限制**。与 ArrayList 需要预先分配容量不同，LinkedList 不存在容量问题，理论上可以存储任意数量的元素（受内存限制）。

第四是**允许存储 null**。与 ArrayList 一样，LinkedList 允许存储 null 元素，也允许存储重复元素。

第五是**fail-fast 迭代器**。LinkedList 的迭代器是 fail-fast 的，在迭代过程中检测到结构性修改时会抛出 ConcurrentModificationException。

### 1.3 适用场景

LinkedList 适用于多种特定场景，理解其适用边界对于正确选择数据结构至关重要。

在**频繁两端操作**的场景中，LinkedList 是最佳选择。`addFirst()`、`addLast()`、`removeFirst()`、`removeLast()`、`peekFirst()`、`peekLast()` 等方法的时间复杂度都是 O(1)，非常适合实现栈或队列。

在**频繁中间插入删除**的场景中，LinkedList 优于 ArrayList。由于链表结构的特性，在已知节点位置的情况下，插入和删除操作只需要修改相邻节点的指针，不需要移动元素。

在**不需要随机访问**的场景中，LinkedList 是合理的。如果程序主要进行顺序遍历或只操作列表两端，应该优先选择 LinkedList。

然而，在**需要频繁随机访问**的场景中，LinkedList 不适合。`get(int index)` 操作需要遍历链表，时间复杂度为 O(n)，性能远低于 ArrayList 的 O(1)。

在**内存敏感**的场景中，LinkedList 需要谨慎使用。每个节点需要额外存储两个指针（prev 和 next），内存开销比 ArrayList 大。

### 1.4 性能特征总结

从时间复杂度的角度分析 LinkedList 的性能特征：

| 操作类型 | 时间复杂度 | 说明 |
|---------|-----------|------|
| 随机访问（get/set） | O(n) | 需要遍历链表 |
| 头部插入删除 | O(1) | 直接操作 first 指针 |
| 尾部插入删除 | O(1) | 直接操作 last 指针 |
| 中间插入删除（已知位置） | O(1) | 只需修改相邻节点 |
| 搜索（contains/indexOf） | O(n) | 需要遍历链表 |
| 遍历 | O(n) | 顺序遍历 |

## 二、继承结构与接口实现

### 2.1 类继承体系

LinkedList 的继承结构体现了 Java 集合框架的优秀设计思想：

```
AbstractSequentialList<E>
    └── LinkedList<E>
```

LinkedList 继承自 `AbstractSequentialList`，这是一个专门为基于序列访问（如链表）设计的抽象类。与直接继承自 `AbstractList` 的 ArrayList 不同，`AbstractSequentialList` 提供了更适合链表的默认实现。

### 2.2 接口实现关系

LinkedList 实现了以下核心接口：

**List<E> 接口**：定义了有序集合的基本操作，包括 `add()`、`remove()`、`get()`、`set()`、`size()` 等核心方法。LinkedList 完全遵循 List 接口的契约，保证元素的插入顺序和访问顺序一致。

**Deque<E> 接口**：双端队列接口，提供了在列表两端进行操作的方法，如 `addFirst()`、`addLast()`、`removeFirst()`、`removeLast()`、`peekFirst()`、`peekLast()` 等。

**Cloneable 接口**：表明 LinkedList 支持克隆操作。通过调用 `clone()` 方法可以创建 LinkedList 的浅拷贝。

**Serializable 接口**：表明 LinkedList 支持序列化。LinkedList 实现了自定义的序列化和反序列化逻辑。

### 2.3 AbstractSequentialList 的贡献

AbstractSequentialList 为 LinkedList 提供了重要的基础实现。这个类实现了部分 List 方法，将另一些方法委托给子类实现：

**默认实现**：对于 `listIterator(int index)` 方法，AbstractSequentialList 提供了默认实现。其他方法如 `get(int index)`、`set(int index, E element)`、`add(int index, E element)`、`remove(int index)` 则依赖于子类实现的 `listIterator()` 方法。

**设计理念**：这种设计体现了模板方法模式，`AbstractSequentialList` 定义了算法的骨架，而具体的遍历逻辑由 `LinkedList` 的 `ListIterator` 实现提供。

## 三、核心内部类

### 3.1 Node 节点类

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

Node 是 LinkedList 的核心数据结构，代表链表中的每个节点。它是一个私有静态内部类，封装了元素的存储和节点的链接关系。

**item 字段**：存储节点持有的元素值。这是节点的"数据"部分，可以是任何对象，包括 null。

**next 字段**：指向后继节点的引用。对于最后一个节点，这个字段为 null。

**prev 字段**：指向前驱节点的引用。对于第一个节点，这个字段为 null。

**构造方法**：Node 的构造方法接收前驱节点、元素值和后继节点，一次性完成所有字段的初始化。这种设计确保了节点创建后就是完整链接的状态。

Node 类使用私有访问修饰符，不允许外部直接访问。这种封装确保了链表结构的完整性只能通过 LinkedList 的方法来维护。

### 3.2 ListItr 迭代器类

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
}
```

ListItr 是 LinkedList 的列表迭代器实现，继承自 ListIterator 接口，提供了双向遍历和修改能力。

**lastReturned 字段**：记录上一次返回的节点，用于支持 `remove()` 和 `set()` 操作。当 lastReturned 为 null 时，表示尚未进行任何遍历操作。

**next 字段**：指向下一个要返回的节点。这是迭代器的核心遍历指针。

**nextIndex 字段**：记录 next 节点的索引位置，用于 `nextIndex()` 和 `previousIndex()` 方法的实现。

**expectedModCount 字段**：记录迭代器创建时的 modCount 值，用于检测并发修改。迭代器会定期检查这个值，如果与当前的 modCount 不一致，会抛出 ConcurrentModificationException。

ListItr 相比普通 Iterator 增加了以下能力：

- `previous()`：向前遍历
- `hasPrevious()`：检查是否有前一个元素
- `previousIndex()`：返回前一个元素的索引
- `set(E e)`：修改上一个返回的元素
- `add(E e)`：在当前位置添加元素

### 3.3 DescendingIterator 逆向迭代器

```java
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

DescendingIterator 是专为从尾部向前遍历设计的适配器。它内部复用 ListItr，从列表的末尾开始向前遍历。

这种设计体现了**适配器模式**的应用，通过复用现有的 ListItr 实现了新的功能，避免了代码重复。

### 3.4 LLSpliterator 可分割迭代器

```java
static final class LLSpliterator<E> implements Spliterator<E> {
    static final int BATCH_UNIT = 1 << 10;
    static final int MAX_BATCH = 1 << 25;
    final LinkedList<E> list;
    Node<E> current;
    int est;
    int expectedModCount;
    int batch;
}
```

LLSpliterator 是 Java 8 引入的可分割迭代器，用于支持并行流操作。它具有以下特点：

**批量处理**：支持批量获取元素进行并行处理，提高遍历效率。

**延迟初始化**：size 估计值（est）和其他字段在首次使用时才初始化。

**线程安全检查**：在遍历过程中检查 modCount，确保 fail-fast 行为。

**分割支持**：`trySplit()` 方法可以将迭代器分割为两部分，支持并行处理。

## 四、核心字段解析

### 4.1 元素数量字段

```java
transient int size = 0;
```

size 字段记录当前链表中元素的数量。这个字段被声明为 transient，表示在序列化时不会被自动保存。

维护 size 字段的原因：

- 提供 O(1) 时间复杂度的 `size()` 方法
- 在插入和删除操作时更新，用于快速判断边界条件
- 在节点查找时用于优化遍历策略

### 4.2 头尾指针字段

```java
transient Node<E> first;
transient Node<E> last;
```

first 和 last 字段分别指向链表的第一个和最后一个节点。这是双向链表的关键字段，提供了 O(1) 时间复杂度的头尾操作能力。

**first 字段的不变式**：
- 如果链表为空，则 first 为 null，last 也为 null
- 如果链表非空，则 first 不为 null，且 first.prev 为 null

**last 字段的不变式**：
- 如果链表为空，则 last 为 null，first 也为 null
- 如果链表非空，则 last 不为 null，且 last.next 为 null

这两个字段使用 transient 修饰符的原因与 size 相同：LinkedList 使用自定义的序列化逻辑，不需要自动序列化这些字段。

### 4.3 结构性修改计数器

```java
transient int modCount = 0;
```

modCount 字段继承自 AbstractList，记录链表发生结构性修改的次数。结构性修改包括任何改变链表元素数量的操作，如 `add()`、`remove()`、`clear()` 等。

这个字段用于实现 fail-fast 机制。当迭代器创建时，会记录当前的 modCount 值（存储在 expectedModCount 中）。每次迭代操作前都会检查这个值是否发生变化，如果变化则抛出 ConcurrentModificationException。

## 五、构造方法分析

### 5.1 默认构造方法

```java
public LinkedList() {
}
```

默认构造方法创建一个空的链表。内部字段保持初始值：size 为 0，first 和 last 为 null。

这是一个轻量级的操作，没有分配任何内存。在首次添加元素时，才会创建第一个节点。

### 5.2 集合构造方法

```java
public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

这个构造方法接受一个 Collection，将其中的所有元素按迭代顺序添加到新创建的链表中。

执行流程：首先调用默认构造方法初始化空链表，然后调用 `addAll(c)` 将集合中的所有元素添加到链表末尾。

这种设计将实际工作委托给 `addAll()` 方法，避免了代码重复。需要注意的是，如果集合为空，这个方法不会创建任何节点，保持链表为空状态。

## 六、核心方法详解

### 6.1 节点链接方法

#### 6.1.1 linkFirst - 头部插入

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

linkFirst() 方法将元素插入到链表头部。这是 O(1) 时间复杂度的操作。

执行步骤：
1. 保存当前 first 节点引用
2. 创建新节点，新节点的 prev 为 null（新节点将成为第一个），next 指向原 first
3. 更新 first 指针指向新节点
4. 如果原 first 为 null（链表为空），同时更新 last 指针
5. 否则，将原 first 的 prev 指向新节点
6. 更新 size 和 modCount

#### 6.1.2 linkLast - 尾部插入

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

linkLast() 方法将元素插入到链表尾部。这也是 O(1) 时间复杂度的操作。

执行步骤与 linkFirst() 类似，不过是操作 last 指针：
1. 保存当前 last 节点引用
2. 创建新节点，新节点的 prev 指向原 last，next 为 null（新节点将成为最后一个）
3. 更新 last 指针指向新节点
4. 如果原 last 为 null（链表为空），同时更新 first 指针
5. 否则，将原 last 的 next 指向新节点
6. 更新 size 和 modCount

#### 6.1.3 linkBefore - 中间插入

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

linkBefore() 方法在指定节点之前插入元素。时间复杂度为 O(1)，前提是已经知道要插入位置的目标节点。

执行步骤：
1. 获取目标节点的前驱节点
2. 创建新节点，前驱为 pred，后继为 succ
3. 将目标节点的前驱引用指向新节点
4. 如果 pred 为 null，说明在头部插入，需要更新 first 指针
5. 否则，将 pred 的 next 指向新节点
6. 更新 size 和 modCount

### 6.2 节点解除链接方法

#### 6.2.1 unlinkFirst - 移除头部

```java
private E unlinkFirst(Node<E> f) {
    final E element = f.item;
    final Node<E> next = f.next;
    f.item = null;
    f.next = null; // help GC
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

unlinkFirst() 方法移除并返回链表的头部元素。这是 O(1) 时间复杂度的操作。

执行步骤：
1. 保存头部元素值和下一个节点引用
2. 将被移除节点的 item 和 next 置为 null，帮助垃圾回收
3. 更新 first 指针指向 next 节点
4. 如果 next 为 null（链表只有一个元素），同时更新 last 为 null
5. 否则，将 next 的 prev 置为 null
6. 更新 size 和 modCount，返回原元素值

#### 6.2.2 unlinkLast - 移除尾部

```java
private E unlinkLast(Node<E> l) {
    final E element = l.item;
    final Node<E> prev = l.prev;
    l.item = null;
    l.prev = null; // help GC
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

unlinkLast() 方法移除并返回链表的尾部元素。这也是 O(1) 时间复杂度的操作。

执行步骤与 unlinkFirst() 对称，不过是操作 last 指针：
1. 保存尾部元素值和前一个节点引用
2. 将被移除节点的 item 和 prev 置为 null
3. 更新 last 指针指向前一个节点
4. 如果 prev 为 null（链表只有一个元素），同时更新 first 为 null
5. 否则，将 prev 的 next 置为 null
6. 更新 size 和 modCount，返回原元素值

#### 6.2.3 unlink - 移除任意节点

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

unlink() 方法移除任意节点并返回其元素值。这是 O(1) 时间复杂度的操作（前提是已经持有节点的引用）。

执行步骤：
1. 保存被移除节点的前驱和后继节点引用
2. 处理前驱节点的链接：如果 prev 为 null，说明是头部节点，更新 first；否则将 prev.next 指向 next
3. 处理后继节点的链接：如果 next 为 null，说明是尾部节点，更新 last；否则将 next.prev 指向 prev
4. 将被移除节点的 item、next、prev 置为 null，帮助垃圾回收
5. 更新 size 和 modCount，返回原元素值

### 6.3 访问方法

#### 6.3.1 getFirst 和 getLast

```java
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
```

getFirst() 和 getLast() 方法分别返回链表的头部和尾部元素。如果链表为空，抛出 NoSuchElementException。

这两个方法的时间复杂度都是 O(1)，因为直接访问 first 和 last 指针即可。

#### 6.3.2 get - 按索引访问

```java
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}
```

get() 方法返回指定位置的元素。与 ArrayList 不同，LinkedList 的随机访问需要遍历链表。

实现依赖于 `node()` 方法，该方法根据索引选择从头部或尾部开始遍历以优化性能。

### 6.4 节点查找方法

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

node() 方法是 LinkedList 的核心查找方法，返回指定索引处的节点。

**优化策略**：比较索引与 size/2 的大小，选择从头部或尾部开始遍历。这种优化使得平均遍历距离减半，最多为 n/2 步。

**时间复杂度**：O(n)，但平均只需遍历 n/2 个节点。

### 6.5 插入方法

#### 6.5.1 add - 尾部添加

```java
public boolean add(E e) {
    linkLast(e);
    return true;
}
```

add() 方法将元素添加到链表尾部。直接调用 linkLast()，时间复杂度 O(1)。

#### 6.5.2 addFirst 和 addLast

```java
public void addFirst(E e) {
    linkFirst(e);
}

public void addLast(E e) {
    linkLast(e);
}
```

addFirst() 和 addLast() 分别在头部和尾部插入元素，时间复杂度都是 O(1)。

#### 6.5.3 add(int index, E element) - 指定位置插入

```java
public void add(int index, E element) {
    checkPositionIndex(index);

    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));
}
```

add(int index, E element) 方法在指定位置插入元素。

**边界处理**：如果 index 等于 size，在尾部插入（等价于 add）；否则在指定位置前插入。

### 6.6 删除方法

#### 6.6.1 remove - 按对象删除

```java
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

remove(Object o) 方法移除第一次出现的指定元素。遍历链表，找到匹配的元素后调用 unlink() 移除。

**null 处理**：分别处理 o 为 null 和非 null 的情况，避免 NullPointerException。

**返回值**：如果找到并移除返回 true，否则返回 false。

#### 6.6.2 removeFirst 和 removeLast

```java
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

removeFirst() 和 removeLast() 分别移除并返回头部和尾部元素。如果链表为空，抛出 NoSuchElementException。

### 6.7 Queue 操作方法

LinkedList 实现了 Queue 接口，提供了一组队列操作方法：

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

这些方法的行为与 Queue 接口规范一致：
- peek() 和 element()：获取但不移除队头，element() 在空队列时抛异常
- poll() 和 remove()：获取并移除队头，remove() 在空队列时抛异常
- offer()：尝试将元素加入队尾

### 6.8 Deque 操作方法

作为 Deque 的实现，LinkedList 提供了丰富的双端操作方法：

```java
public boolean offerFirst(E e) {
    addFirst(e);
    return true;
}

public boolean offerLast(E e) {
    addLast(e);
    return true;
}

public E peekFirst() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}

public E peekLast() {
    final Node<E> l = last;
    return (l == null) ? null : l.item;
}

public E pollFirst() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}

public E pollLast() {
    final Node<E> l = last;
    return (l == null) ? null : unlinkLast(l);
}

public void push(E e) {
    addFirst(e);
}

public E pop() {
    return removeFirst();
}
```

这些方法提供了完整的双端队列功能，支持在列表的两端进行插入、删除和访问操作。

### 6.9 搜索方法

#### 6.9.1 indexOf - 正向搜索

```java
public int indexOf(Object o) {
    int index = 0;
    if (o == null) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null)
                return index;
            index++;
        }
    } else {
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item))
                return index;
            index++;
        }
    }
    return -1;
}
```

indexOf() 方法返回指定元素第一次出现的索引。时间复杂度为 O(n)。

#### 6.9.2 lastIndexOf - 反向搜索

```java
public int lastIndexOf(Object o) {
    int index = size;
    if (o == null) {
        for (Node<E> x = last; x != null; x = x.prev) {
            index--;
            if (x.item == null)
                return index;
        }
    } else {
        for (Node<E> x = last; x != null; x = x.prev) {
            index--;
            if (o.equals(x.item))
                return index;
        }
    }
    return -1;
}
```

lastIndexOf() 方法返回指定元素最后一次出现的索引。从尾部开始遍历，时间复杂度为 O(n)。

### 6.10 批量操作方法

#### 6.10.1 addAll - 批量添加

```java
public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    if (numNew == 0)
        return false;

    Node<E> pred, succ;
    if (index == size) {
        succ = null;
        pred = last;
    } else {
        succ = node(index);
        pred = succ.prev;
    }

    for (Object o : a) {
        @SuppressWarnings("unchecked") E e = (E) o;
        Node<E> newNode = new Node<>(pred, e, null);
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        pred = newNode;
    }

    if (succ == null) {
        last = pred;
    } else {
        pred.next = succ;
        succ.prev = pred;
    }

    size += numNew;
    modCount++;
    return true;
}
```

addAll() 方法将集合中的所有元素插入到指定位置。这是一个相对复杂的操作，但仍然是 O(n) 时间复杂度。

**优化策略**：一次性创建所有新节点，然后将它们作为整体链接到链表中，避免多次遍历。

### 6.11 清空方法

```java
public void clear() {
    for (Node<E> x = first; x != null; ) {
        Node<E> next = x.next;
        x.item = null;
        x.next = null;
        x.prev = null;
        x = next;
    }
    first = last = null;
    size = 0;
    modCount++;
}
```

clear() 方法移除链表中的所有元素。

**内存优化**：遍历所有节点，将每个节点的 item、next、prev 置为 null，帮助垃圾回收器回收内存。

## 七、迭代器实现详解

### 7.1 ListItr 详细实现

ListItr 是 LinkedList 的列表迭代器实现，提供了双向遍历和修改能力。

#### 7.1.1 遍历方法

```java
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
```

这些方法实现了双向遍历：
- next()：向后遍历，返回当前元素，移动到下一个
- hasNext()：检查是否还有下一个元素
- previous()：向前遍历，返回当前元素，移动到前一个
- hasPrevious()：检查是否还有前一个元素

#### 7.1.2 修改方法

```java
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
```

这些方法提供了迭代过程中的修改能力：
- remove()：移除上一次返回的元素
- set()：修改上一次返回的元素
- add()：在当前位置添加元素

#### 7.1.3 并发检查

```java
final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

每次迭代操作前都会检查 modCount 和 expectedModCount 是否一致。如果在迭代过程中对链表进行了结构性修改（通过非迭代器方法），会抛出 ConcurrentModificationException。

### 7.2 DescendingIterator 逆向迭代

```java
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

DescendingIterator 提供了从尾部向前遍历的能力。它内部复用 ListItr，从 size 位置开始，通过 previous() 方法实现逆向遍历。

### 7.3 Spliterator 可分割迭代器

LLSpliterator 是 Java 8 引入的并行流支持：

```java
public Spliterator<E> spliterator() {
    return new LLSpliterator<E>(this, -1, 0);
}
```

LLSpliterator 支持以下特性：
- **ORDERED**：维护元素顺序
- **SIZED**：可以估计大小
- **SUBSIZED**：支持分割成更小的部分
- **批量处理**：通过 batch 字段支持批量获取元素

## 八、常见面试问题与解答

### 8.1 LinkedList 与 ArrayList 的区别

这是最常见的面试问题之一。

**底层数据结构**：ArrayList 使用动态数组，LinkedList 使用双向链表。

**随机访问性能**：ArrayList 的 get(int index) 是 O(1)，LinkedList 是 O(n)。

**插入删除性能**：在头部或尾部，LinkedList 的插入删除是 O(1)。在中间位置，LinkedList 需要先找到位置（O(n)），然后修改指针（O(1)），总体 O(n)；ArrayList 需要移动元素，平均 O(n)。

**内存使用**：ArrayList 只有元素存储，LinkedList 额外需要两个指针（prev 和 next）。

**迭代器安全性**：在迭代过程中修改，ArrayList 的迭代器可以安全删除当前元素，LinkedList 也可以，但两者都不是线程安全的。

### 8.2 LinkedList 是线程安全的吗

LinkedList 不是线程安全的。在多线程环境下，并发修改可能导致各种问题：

- 数据不一致
- 迭代抛出 ConcurrentModificationException
- 更严重的内存损坏

如果需要在并发环境中使用链表，应该使用 `Collections.synchronizedList()` 包装或使用 `ConcurrentLinkedQueue`。

### 8.3 LinkedList 为什么能实现 O(1) 的头部和尾部操作

LinkedList 维护了 first 和 last 两个指针，直接指向链表的头尾节点。插入和删除操作只需要修改这几个指针，不需要遍历或移动其他元素。

### 8.4 LinkedList 的 get(int index) 性能问题

LinkedList 的 get(int index) 需要遍历链表：
- 如果 index 靠近头部，从前往后遍历
- 如果 index 靠近尾部，从后往前遍历

对于大型链表和频繁的随机访问，ArrayList 是更好的选择。

### 8.5 LinkedList 的节点为什么使用双向链接

双向链接的设计有以下优点：
- 支持双向遍历
- O(1) 时间复杂度的前向和后向删除
- 简化插入操作（不需要知道前一个节点）

单链接链表也可以实现，但删除操作需要找到前一个节点，增加了时间复杂度。

### 8.6 LinkedList 的 fail-fast 机制

LinkedList 的迭代器使用 modCount 实现 fail-fast：

- 迭代器创建时记录 expectedModCount = modCount
- 每次迭代操作前检查 modCount == expectedModCount
- 如果在迭代过程中通过非迭代器方法修改链表，modCount 会增加
- 检查不通过时抛出 ConcurrentModificationException

这是最佳努力（best-effort）机制，不能保证在所有并发修改情况下都抛出异常。

### 8.7 LinkedList 可以作为栈使用吗

是的，LinkedList 可以作为栈使用：

```java
LinkedList<Integer> stack = new LinkedList<>();
stack.push(1);  // push 在头部添加
stack.push(2);
Integer top = stack.pop();  // pop 从头部移除并返回
```

LinkedList 的 `push()` 和 `pop()` 方法实现了栈的 LIFO 语义。

### 8.8 LinkedList 可以作为队列使用吗

是的，LinkedList 实现了 Deque 接口，可以作为队列使用：

```java
LinkedList<Integer> queue = new LinkedList<>();
queue.offer(1);  // offer 在尾部添加
queue.offer(2);
Integer head = queue.poll();  // poll 从头部移除并返回
```

LinkedList 提供了完整的 Queue 和 Deque 操作方法。

### 8.9 LinkedList 的内存泄漏问题

与 ThreadLocal 类似，LinkedList 也可能存在内存泄漏问题：

- 如果节点存储了大对象，移除后如果没有被垃圾回收，会占用内存
- 循环引用可能导致垃圾回收器无法回收

但 LinkedList 的 unlink 操作会将节点的 item 置为 null，帮助垃圾回收。

### 8.10 什么时候应该使用 LinkedList

在以下情况下应该使用 LinkedList：
- 主要在列表两端进行操作
- 需要频繁在中间位置插入删除（且已知的节点位置）
- 不需要随机访问，只需要顺序遍历
- 实现栈、队列或双端队列

在以下情况下不应该使用 LinkedList：
- 需要频繁随机访问
- 主要进行查询操作
- 内存敏感的场景

## 九、实践应用场景

### 9.1 实现栈

```java
public class LinkedListStack<E> {
    private final LinkedList<E> stack = new LinkedList<>();

    public void push(E e) {
        stack.push(e);  // 或 stack.addFirst(e)
    }

    public E pop() {
        return stack.pop();  // 或 stack.removeFirst()
    }

    public E peek() {
        return stack.peekFirst();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }
}
```

### 9.2 实现队列

```java
public class LinkedListQueue<E> {
    private final LinkedList<E> queue = new LinkedList<>();

    public void offer(E e) {
        queue.offer(e);  // 或 queue.add(e)
    }

    public E poll() {
        return queue.poll();  // 或 queue.removeFirst()
    }

    public E peek() {
        return queue.peek();  // 或 queue.peekFirst()
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
```

### 9.3 实现双端队列

```java
public class LinkedListDeque<E> {
    private final LinkedList<E> deque = new LinkedList<>();

    // 头部操作
    public void addFirst(E e) {
        deque.addFirst(e);
    }

    public E removeFirst() {
        return deque.removeFirst();
    }

    public E getFirst() {
        return deque.getFirst();
    }

    // 尾部操作
    public void addLast(E e) {
        deque.addLast(e);
    }

    public E removeLast() {
        return deque.removeLast();
    }

    public E getLast() {
        return deque.getLast();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
```

### 9.4 实现LRU缓存

```java
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final LinkedList<Node<K, V>> list;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.list = new LinkedList<>();
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            list.remove(node);
            list.addFirst(node);
            return node.value;
        }
        return null;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            list.remove(node);
            list.addFirst(node);
        } else {
            if (map.size() >= capacity) {
                Node<K, V> last = list.removeLast();
                map.remove(last.key);
            }
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            list.addFirst(newNode);
        }
    }
}
```

### 9.5 浏览器前进后退功能

```java
public class BrowserHistory {
    private final LinkedList<String> backStack = new LinkedList<>();
    private final LinkedList<String> forwardStack = new LinkedList<>();
    private String currentPage;

    public void visit(String page) {
        if (currentPage != null) {
            backStack.push(currentPage);
        }
        forwardStack.clear();
        currentPage = page;
    }

    public String back() {
        if (backStack.isEmpty()) return null;
        forwardStack.push(currentPage);
        currentPage = backStack.pop();
        return currentPage;
    }

    public String forward() {
        if (forwardStack.isEmpty()) return null;
        backStack.push(currentPage);
        currentPage = forwardStack.pop();
        return currentPage;
    }
}
```

## 十、常见陷阱与最佳实践

### 10.1 避免频繁的随机访问

LinkedList 的随机访问性能较差，避免在循环中频繁调用 get() 方法。如果需要按索引访问元素，应该使用 ArrayList。

```java
// 错误的做法
for (int i = 0; i < list.size(); i++) {
    process(list.get(i));  // 每次都是 O(n)
}

// 正确的做法
for (Object item : list) {
    process(item);  // 使用迭代器，O(n)
}
```

### 10.2 选择正确的数据结构

根据操作类型选择数据结构：
- 随机访问为主：使用 ArrayList
- 头尾操作为主：使用 LinkedList
- 中间插入删除为主：使用 LinkedList（如果已知道位置）

### 10.3 正确使用迭代器

使用迭代器进行遍历和修改，而不是在遍历中使用 get() 和 remove()：

```java
// 推荐
Iterator<Integer> it = list.iterator();
while (it.hasNext()) {
    Integer item = it.next();
    if (shouldRemove(item)) {
        it.remove();
    }
}

// 不推荐
for (int i = 0; i < list.size(); i++) {
    if (shouldRemove(list.get(i))) {
        list.remove(i);
        i--;  // 修正索引
    }
}
```

### 10.4 线程安全问题

LinkedList 不是线程安全的，在多线程环境中需要同步：

```java
LinkedList<Integer> list = new LinkedList<>();
// 包装成同步列表
List<Integer> synchronizedList = Collections.synchronizedList(list);

// 或者使用 ConcurrentLinkedQueue（如果适合）
```

### 10.5 内存使用优化

LinkedList 的每个节点都有额外的指针开销，在内存敏感的场景中要谨慎使用。如果不需要频繁的插入删除操作，ArrayList 是更节省内存的选择。

### 10.6 最佳实践总结

了解数据结构的特性，选择合适的工具。使用迭代器进行遍历，避免频繁的随机访问。注意线程安全问题，根据场景选择同步策略。考虑内存使用，在性能和内存之间取得平衡。使用 clear() 方法时注意帮助 GC。

## 十一、与其他类的比较

### 11.1 LinkedList vs ArrayList

| 特性 | LinkedList | ArrayList |
|-----|------------|-----------|
| 底层结构 | 双向链表 | 动态数组 |
| 随机访问 | O(n) | O(1) |
| 头部插入删除 | O(1) | O(n) |
| 尾部插入删除 | O(1) | O(1) amortized |
| 中间插入删除 | O(n) | O(n) |
| 内存开销 | 高（每个节点2个指针） | 低 |
| 迭代安全性 | fail-fast | fail-fast |
| 适用场景 | 头尾操作、已知位置操作 | 随机访问为主 |

### 11.2 LinkedList vs ArrayDeque

| 特性 | LinkedList | ArrayDeque |
|-----|------------|-----------|
| 实现接口 | List, Deque | Deque |
| 随机访问 | O(n) | 不支持 |
| 两端操作 | O(1) | O(1) |
| 内存分配 | 按节点分配 | 连续数组 |
| 扩容 | 无需扩容 | 需要扩容（2倍） |
| 适用场景 | 需要 List 操作 | 仅需要 Deque 操作 |

### 11.3 LinkedList vs Vector

| 特性 | LinkedList | Vector |
|-----|------------|--------|
| 线程安全 | 否 | 是（同步方法） |
| 性能 | 高 | 低（同步开销） |
| 扩容策略 | 无需扩容 | 翻倍或增量 |
| 内存使用 | 较高 | 较低（可能浪费） |
| 迭代器 | fail-fast | fail-fast |

## 十二、源码学习总结

### 12.1 设计模式应用

LinkedList 源码中体现了多种设计模式的应用：

**迭代器模式**：通过 ListItr 内部类实现了双向迭代器，封装了遍历逻辑。

**适配器模式**：DescendingIterator 复用 ListItr 实现逆向遍历。

**模板方法模式**：AbstractSequentialList 提供了算法骨架，由 LinkedList 提供具体实现。

### 12.2 核心算法亮点

LinkedList 的实现中有几个值得学习的亮点：

**双向链表设计**：first 和 last 指针的设计使得头尾操作都是 O(1)。

**node() 方法优化**：根据索引位置选择遍历方向，将平均遍历距离减半。

**批量操作优化**：addAll() 方法一次性创建所有节点，减少遍历次数。

**内存清理**：unlink 操作将节点的 item 和指针置为 null，帮助 GC。

### 12.3 工程实践启示

从 LinkedList 源码可以学到很多工程实践知识：

**封装性**：Node 类私有化，确保链表结构只能通过 LinkedList 方法操作。

**边界处理**：对空链表的情况进行了仔细处理，确保不变式成立。

**辅助方法**：linkFirst、linkLast、unlinkFirst、unlinkLast 等方法将复杂操作分解为简单步骤。

**fail-fast 机制**：通过 modCount 实现迭代器的并发检测。

理解 LinkedList 的源码不仅有助于更好地使用这个数据结构，更能学习到链表实现的核心原理和优秀的代码设计技巧。这些知识对于编写高效、可靠的 Java 程序非常重要。