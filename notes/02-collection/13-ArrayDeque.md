# ArrayDeque 源码解读

## 一、类概述

ArrayDeque是Deque接口的可变数组实现。ArrayDeque没有容量限制,它们会根据需要增长以支持使用。它们不是线程安全的,在没有外部同步的情况下,不支持多线程并发访问。禁止null元素。作为栈使用时,这个类可能比Stack更快;作为队列使用时,可能比LinkedList更快。

### 1.1 核心特性

- **可变数组**: 使用数组存储,可动态扩容
- **双端队列**: 支持在两端插入和删除
- **高性能**: 大多数操作的分摊时间复杂度为O(1)
- **无容量限制**: 自动扩容
- **禁止null**: 不允许null元素
- **线程不安全**: 非同步实现,需要外部同步
- **Fail-Fast**: 迭代器实现快速失败机制

### 1.2 适用场景

- 需要双端队列操作
- 需要高性能的栈操作
- 需要高性能的队列操作
- 需要频繁的插入和删除

## 二、设计原理

### 2.1 循环数组

ArrayDeque使用循环数组实现:

```java
// 循环数组结构
// [null, null, null, null, null, null, null, null, null]
//  ^head                           ^tail

// 添加元素到尾部
// [null, null, null, null, null, null, null, A]
//  ^head                           ^tail

// 添加元素到头部
// [B, null, null, null, null, null, null, A]
//  ^head                           ^tail
```

### 2.2 head和tail指针

```java
transient int head;  // 头部元素的索引
transient int tail;  // 下一个要添加到尾部的索引
```

**设计要点**:
- head指向头部元素
- tail指向下一个要添加的位置
- 当head == tail时,队列为空
- 当(head + 1) % length == tail时,队列已满

### 2.3 扩容机制

```java
private void doubleCapacity() {
    assert head == tail;
    int p = head;
    int n = elements.length;
    int r = n - p; // number of elements to the right of p
    int newCapacity = n << 1; // 容量翻倍
    Object[] a = new Object[newCapacity];
    System.arraycopy(elements, p, a, 0, r);
    System.arraycopy(elements, 0, a, r, p);
    elements = a;
    head = 0;
    tail = n;
}
```

**设计要点**:
- 容量翻倍增长
- 重新排列元素
- head重置为0
- tail设置为旧容量

## 三、继承结构

```
AbstractCollection<E>
    ↑
ArrayDeque<E>
    implements Deque<E>, Cloneable, Serializable
```

### 3.1 接口实现

- **Deque**: 双端队列接口
- **Cloneable**: 支持克隆
- **Serializable**: 支持序列化

## 四、核心字段分析

### 4.1 元素数组

```java
transient Object[] elements;
```

**字段设计要点**:
- 存储队列元素
- 容量总是2的幂
- transient修饰,自定义序列化
- 非private,简化嵌套类访问

### 4.2 头部指针

```java
transient int head;
```

**字段设计要点**:
- 指向头部元素
- 用于删除头部元素
- transient修饰,自定义序列化

### 4.3 尾部指针

```java
transient int tail;
```

**字段设计要点**:
- 指向下一个要添加的位置
- 用于添加尾部元素
- transient修饰,自定义序列化

### 4.4 最小初始容量

```java
private static final int MIN_INITIAL_CAPACITY = 8;
```

**设计要点**:
- 最小初始容量为8
- 必须是2的幂

## 五、构造方法

### 5.1 默认构造方法

```java
public ArrayDeque() {
    elements = new Object[16];
}
```

**方法要点**:
- 创建空的ArrayDeque
- 初始容量为16

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
```

### 5.2 指定初始容量的构造方法

```java
public ArrayDeque(int numElements) {
    allocateElements(numElements);
}

private void allocateElements(int numElements) {
    int initialCapacity = MIN_INITIAL_CAPACITY;
    // Find the best power of two to hold elements.
    // Tests "<=" because arrays aren't kept full.
    if (numElements >= initialCapacity) {
        initialCapacity = numElements;
        initialCapacity |= (initialCapacity >>> 1);
        initialCapacity |= (initialCapacity >>> 2);
        initialCapacity |= (initialCapacity >>> 4);
        initialCapacity |= (initialCapacity >>> 8);
        initialCapacity |= (initialCapacity >>> 16);
        initialCapacity++;

        if (initialCapacity < 0)   // Too many elements, must back off
            initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
    }
    elements = new Object[initialCapacity];
}
```

**方法要点**:
- 创建指定初始容量的ArrayDeque
- 容量调整为2的幂
- 最小容量为8

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>(20);  // 实际容量为32
```

## 六、核心方法

### 6.1 addFirst - 添加到头部

```java
public void addFirst(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[head = (head - 1) & (elements.length - 1)] = e;
    if (head == tail)
        doubleCapacity();
}
```

**方法要点**:
- 不允许null元素
- 使用位运算实现循环
- 如果队列已满,扩容
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addFirst("a");
deque.addFirst("b");
// [b, a]
```

### 6.2 addLast - 添加到尾部

```java
public void addLast(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[tail] = e;
    if ( (tail = (tail + 1) & (elements.length - 1)) == head)
        doubleCapacity();
}
```

**方法要点**:
- 不允许null元素
- 使用位运算实现循环
- 如果队列已满,扩容
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addLast("a");
deque.addLast("b");
// [a, b]
```

### 6.3 pollFirst - 移除并返回头部元素

```java
public E pollFirst() {
    int h = head;
    E result = (E) elements[h];
    // Element is null if deque empty
    if (result == null)
        return null;
    elements[h] = null;  // Must null out slot
    head = (h + 1) & (elements.length - 1);
    return result;
}
```

**方法要点**:
- 如果队列为空,返回null
- 清除元素
- 更新head指针
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addLast("a");
deque.addLast("b");

String first = deque.pollFirst();  // "a"
```

### 6.4 pollLast - 移除并返回尾部元素

```java
public E pollLast() {
    int t = (tail - 1) & (elements.length - 1);
    E result = (E) elements[t];
    if (result == null)
        return null;
    elements[t] = null;
    tail = t;
    return result;
}
```

**方法要点**:
- 如果队列为空,返回null
- 清除元素
- 更新tail指针
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addLast("a");
deque.addLast("b");

String last = deque.pollLast();  // "b"
```

### 6.5 peekFirst - 查看头部元素

```java
public E peekFirst() {
    return (E) elements[head];
}
```

**方法要点**:
- 如果队列为空,返回null
- 不移除元素
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addLast("a");
deque.addLast("b");

String first = deque.peekFirst();  // "a"
```

### 6.6 peekLast - 查看尾部元素

```java
public E peekLast() {
    return (E) elements[(tail - 1) & (elements.length - 1)];
}
```

**方法要点**:
- 如果队列为空,返回null
- 不移除元素
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addLast("a");
deque.addLast("b");

String last = deque.peekLast();  // "b"
```

### 6.7 removeFirst - 移除头部元素

```java
public E removeFirst() {
    E x = pollFirst();
    if (x == null)
        throw new NoSuchElementException();
    return x;
}
```

**方法要点**:
- 如果队列为空,抛出NoSuchElementException
- 委托给pollFirst
- 时间复杂度: O(1)

### 6.8 removeLast - 移除尾部元素

```java
public E removeLast() {
    E x = pollLast();
    if (x == null)
        throw new NoSuchElementException();
    return x;
}
```

**方法要点**:
- 如果队列为空,抛出NoSuchElementException
- 委托给pollLast
- 时间复杂度: O(1)

### 6.9 push - 压栈

```java
public void push(E e) {
    addFirst(e);
}
```

**方法要点**:
- 作为栈使用
- 委托给addFirst
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> stack = new ArrayDeque<>();
stack.push("a");
stack.push("b");

String top = stack.pop();  // "b"
```

### 6.10 pop - 出栈

```java
public E pop() {
    return removeFirst();
}
```

**方法要点**:
- 作为栈使用
- 委托给removeFirst
- 时间复杂度: O(1)

**示例**:
```java
ArrayDeque<String> stack = new ArrayDeque<>();
stack.push("a");
stack.push("b");

String top = stack.pop();  // "b"
```

### 6.11 offer - 添加到尾部

```java
public boolean offer(E e) {
    return offerLast(e);
}

public boolean offerLast(E e) {
    addLast(e);
    return true;
}
```

**方法要点**:
- 作为队列使用
- 委托给addLast
- 总是返回true
- 时间复杂度: O(1)

### 6.12 poll - 移除并返回头部元素

```java
public E poll() {
    return pollFirst();
}
```

**方法要点**:
- 作为队列使用
- 委托给pollFirst
- 时间复杂度: O(1)

### 6.13 peek - 查看头部元素

```java
public E peek() {
    return peekFirst();
}
```

**方法要点**:
- 作为队列使用
- 委托给peekFirst
- 时间复杂度: O(1)

## 七、辅助方法

### 7.1 doubleCapacity - 扩容

```java
private void doubleCapacity() {
    assert head == tail;
    int p = head;
    int n = elements.length;
    int r = n - p; // number of elements to the right of p
    int newCapacity = n << 1;
    if (newCapacity < 0)
        throw new IllegalStateException("Sorry, deque too big");
    Object[] a = new Object[newCapacity];
    System.arraycopy(elements, p, a, 0, r);
    System.arraycopy(elements, 0, a, r, p);
    elements = a;
    head = 0;
    tail = n;
}
```

**方法要点**:
- 容量翻倍
- 重新排列元素
- head重置为0
- tail设置为旧容量
- 时间复杂度: O(n)

### 7.2 copyElements - 复制元素

```java
private <T> T[] copyElements(T[] a) {
    if (head < tail) {
        System.arraycopy(elements, head, a, 0, size());
    } else if (head > tail) {
        int headPortionLen = elements.length - head;
        System.arraycopy(elements, head, a, 0, headPortionLen);
        System.arraycopy(elements, 0, a, headPortionLen, tail);
    }
    return a;
}
```

**方法要点**:
- 按顺序复制元素
- 处理循环数组
- 时间复杂度: O(n)

### 7.3 allocateElements - 分配数组

```java
private void allocateElements(int numElements) {
    int initialCapacity = MIN_INITIAL_CAPACITY;
    // Find the best power of two to hold elements.
    // Tests "<=" because arrays aren't kept full.
    if (numElements >= initialCapacity) {
        initialCapacity = numElements;
        initialCapacity |= (initialCapacity >>> 1);
        initialCapacity |= (initialCapacity >>> 2);
        initialCapacity |= (initialCapacity >>> 4);
        initialCapacity |= (initialCapacity >>> 8);
        initialCapacity |= (initialCapacity >>> 16);
        initialCapacity++;

        if (initialCapacity < 0)   // Too many elements, must back off
            initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
    }
    elements = new Object[initialCapacity];
}
```

**方法要点**:
- 计算合适的容量
- 容量必须是2的幂
- 最小容量为8

## 八、其他方法

### 8.1 size - 获取大小

```java
public int size() {
    return (tail - head) & (elements.length - 1);
}
```

**方法要点**:
- 使用位运算计算大小
- 处理循环数组
- 时间复杂度: O(1)

### 8.2 isEmpty - 检查是否为空

```java
public boolean isEmpty() {
    return head == tail;
}
```

**方法要点**:
- 检查head是否等于tail
- 时间复杂度: O(1)

### 8.3 clear - 清空队列

```java
public void clear() {
    int h = head;
    int t = tail;
    if (h != t) {
        head = tail = 0;
        int i = h;
        int mask = elements.length - 1;
        do {
            elements[i] = null;
            i = (i + 1) & mask;
        } while (i != t);
    }
}
```

**方法要点**:
- 清空所有元素
- 重置head和tail
- 时间复杂度: O(n)

### 8.4 contains - 检查包含

```java
public boolean contains(Object o) {
    if (o == null)
        return false;
    int mask = elements.length - 1;
    int i = head;
    Object x;
    while ( (x = elements[i]) != null) {
        if (o.equals(x))
            return true;
        i = (i + 1) & mask;
        if (i == head)
            break;
    }
    return false;
}
```

**方法要点**:
- 不允许null元素
- 遍历所有元素
- 时间复杂度: O(n)

### 8.5 remove - 移除元素

```java
public boolean remove(Object o) {
    if (o == null)
        return false;
    int mask = elements.length - 1;
    int i = head;
    Object x;
    while ( (x = elements[i]) != null) {
        if (o.equals(x)) {
            delete(i);
            return true;
        }
        i = (i + 1) & mask;
        if (i == head)
            break;
    }
    return false;
}

private boolean delete(int i) {
    checkInvariants();
    final Object[] elements = this.elements;
    final int mask = elements.length - 1;
    final int h = head;
    final int t = tail;
    final int front = (i - h) & mask;
    final int back  = (t - i) & mask;

    // Invariant: head <= i < tail mod circularity
    if (front >= ((t - h) & mask))
        throw new ConcurrentModificationException();

    // Optimize for most common case: i == front
    if (front < back) {
        if (h <= i) {
            System.arraycopy(elements, h, elements, h + 1, front);
        } else { // Wrap around
            System.arraycopy(elements, 0, elements, 1, i);
            elements[0] = elements[mask];
            System.arraycopy(elements, h, elements, h + 1, mask - h);
        }
        elements[h] = null;
        head = (h + 1) & mask;
        return false;
    }

    // Optimize for least common case: i == back
    if (front > back) {
        if (i < t) {
            System.arraycopy(elements, i + 1, elements, i, back);
        } else { // Wrap around
            System.arraycopy(elements, i + 1, elements, i, mask - i);
            elements[mask] = elements[0];
            System.arraycopy(elements, 0, elements, 1, t);
        }
        elements[t] = null;
        tail = (t - 1) & mask;
        return false;
    }

    // Move elements to fill gap
    if (front < back) {
        if (front <= back / 2) {
            // Move front elements forward
            if (h <= i) {
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements, 0, elements, 1, i);
                elements[0] = elements[mask];
                System.arraycopy(elements, h, elements, h + 1, mask - h);
            }
            elements[h] = null;
            head = (h + 1) & mask;
        } else {
            // Move back elements backward
            if (i < t) {
                System.arraycopy(elements, i + 1, elements, i, back);
            } else { // Wrap around
                System.arraycopy(elements, i + 1, elements, i, mask - i);
                elements[mask] = elements[0];
                System.arraycopy(elements, 0, elements, 1, t);
            }
            elements[t] = null;
            tail = (t - 1) & mask;
        }
    } else {
        // front > back
        if ((front - back) < (mask - (front - back))) {
            // Move front elements forward
            if (h <= i) {
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements, 0, elements, 1, i);
                elements[0] = elements[mask];
                System.arraycopy(elements, h, elements, h + 1, mask - h);
            }
            elements[h] = null;
            head = (h + 1) & mask;
        } else {
            // Move back elements backward
            if (i < t) {
                System.arraycopy(elements, i + 1, elements, i, back);
            } else { // Wrap around
                System.arraycopy(elements, i + 1, elements, i, mask - i);
                elements[mask] = elements[0];
                System.arraycopy(elements, 0, elements, 1, t);
            }
            elements[t] = null;
            tail = (t - 1) & mask;
        }
    }

    checkInvariants();
    return true;
}
```

**方法要点**:
- 遍历查找元素
- 删除时移动元素填补空隙
- 时间复杂度: O(n)

## 九、迭代器实现

### 9.1 迭代器特性

```java
// ArrayDeque的迭代器特点:
// 1. Fail-Fast: 会抛出ConcurrentModificationException
// 2. 按从头到尾的顺序迭代
// 3. 支持remove操作
```

### 9.2 迭代器实现

```java
private class DeqIterator implements Iterator<E> {
    int cursor = head;
    int fence = tail;
    int lastRet = -1;

    DeqIterator() {} // default constructor

    final void cursorFence() {
        if (cursor != fence) {
            throw new ConcurrentModificationException();
        }
    }

    public boolean hasNext() {
        return cursor != fence;
    }

    public E next() {
        if (cursor == fence)
            throw new NoSuchElementException();
        E result = (E) elements[cursor];
        // This check doesn't catch all possible comodifications,
        // but does catch the ones that corrupt traversal
        if (tail != fence || result == null)
            throw new ConcurrentModificationException();
        lastRet = cursor;
        cursor = (cursor + 1) & (elements.length - 1);
        return result;
    }

    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        if (delete(lastRet)) { // if left-shifted, undo increment in next()
            cursor = (cursor - 1) & (elements.length - 1);
            fence = tail;
        }
        lastRet = -1;
    }
}
```

## 十、设计模式

### 10.1 循环数组模式

ArrayDeque使用循环数组实现双端队列:

```java
// 循环数组结构
// [null, null, null, null, null, null, null, null, null]
//  ^head                           ^tail
```

### 10.2 适配器模式

ArrayDeque适配为栈和队列:

```java
// 作为栈
public void push(E e) {
    addFirst(e);
}

public E pop() {
    return removeFirst();
}

// 作为队列
public boolean offer(E e) {
    return offerLast(e);
}

public E poll() {
    return pollFirst();
}
```

## 十一、面试常见问题

### 11.1 ArrayDeque与LinkedList的区别?

| 特性 | ArrayDeque | LinkedList |
|------|------------|-----------|
| 内部实现 | 循环数组 | 双向链表 |
| 时间复杂度 | O(1)分摊 | O(1)分摊 |
| 空间复杂度 | 较低 | 较高 |
| 随机访问 | 支持 | 不支持 |
| null元素 | 不允许 | 允许 |
| 性能 | 更高 | 较低 |

### 11.2 ArrayDeque与Stack的区别?

| 特性 | ArrayDeque | Stack |
|------|------------|-------|
| 内部实现 | 循环数组 | 数组 |
| 线程安全 | 否 | 是 |
| 性能 | 更高 | 较低 |
| 接口 | Deque | Stack |
| 推荐使用 | 是 | 否 |

### 11.3 ArrayDeque支持null元素吗?

不支持。尝试添加null元素会抛出NullPointerException:

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.add(null);  // NullPointerException
```

### 11.4 ArrayDeque是线程安全的吗?

不是。需要外部同步:

```java
Deque<String> syncDeque = Collections.synchronizedDeque(new ArrayDeque<>());
```

### 11.5 ArrayDeque的迭代器是fail-fast的吗?

是的。如果在迭代过程中队列被结构性修改,会抛出ConcurrentModificationException:

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.add("a");
deque.add("b");

Iterator<String> it = deque.iterator();
deque.add("c");  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 11.6 ArrayDeque如何实现双端队列?

使用循环数组和head、tail指针:

```java
// head指向头部元素
// tail指向下一个要添加的位置
// 使用位运算实现循环
```

### 11.7 ArrayDeque的扩容机制?

容量翻倍增长:

```java
private void doubleCapacity() {
    int newCapacity = n << 1;  // 容量翻倍
    Object[] a = new Object[newCapacity];
    // 复制元素
    elements = a;
}
```

### 11.8 ArrayDeque的基本操作时间复杂度?

- addFirst/addLast: O(1)分摊
- pollFirst/pollLast: O(1)
- peekFirst/peekLast: O(1)
- push/pop: O(1)
- offer/poll/peek: O(1)
- size/isEmpty: O(1)
- contains/remove: O(n)

## 十二、使用场景

### 12.1 作为栈使用

```java
ArrayDeque<String> stack = new ArrayDeque<>();
stack.push("a");
stack.push("b");
stack.push("c");

String top = stack.pop();  // "c"
```

### 12.2 作为队列使用

```java
ArrayDeque<String> queue = new ArrayDeque<>();
queue.offer("a");
queue.offer("b");
queue.offer("c");

String front = queue.poll();  // "a"
```

### 12.3 作为双端队列使用

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addFirst("a");
deque.addLast("b");

String first = deque.pollFirst();  // "a"
String last = deque.pollLast();   // "b"
```

### 12.4 实现LRU缓存

```java
ArrayDeque<String> lru = new ArrayDeque<>();
Set<String> cache = new HashSet<>();

public void access(String item) {
    if (cache.contains(item)) {
        lru.remove(item);
    } else {
        if (cache.size() >= MAX_SIZE) {
            String oldest = lru.pollLast();
            cache.remove(oldest);
        }
        cache.add(item);
    }
    lru.addFirst(item);
}
```

### 12.5 实现滑动窗口

```java
ArrayDeque<Integer> window = new ArrayDeque<>();
int[] nums = {1, 2, 3, 4, 5};
int k = 3;

for (int num : nums) {
    window.addLast(num);
    if (window.size() > k) {
        window.pollFirst();
    }
    // 处理窗口
}
```

## 十三、注意事项

### 13.1 null元素

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.add(null);  // NullPointerException
```

### 13.2 线程安全

```java
ArrayDeque<String> deque = new ArrayDeque<>();

// 需要外部同步
Deque<String> syncDeque = Collections.synchronizedDeque(deque);
```

### 13.3 迭代器一致性

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.add("a");
deque.add("b");

Iterator<String> it = deque.iterator();
deque.add("c");  // ConcurrentModificationException
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 13.4 容量限制

```java
// ArrayDeque没有容量限制
// 但可以指定初始容量
ArrayDeque<String> deque = new ArrayDeque<>(100);
```

## 十四、最佳实践

### 14.1 使用ArrayDeque替代Stack

```java
// 不推荐: 使用Stack
Stack<String> stack = new Stack<>();
stack.push("a");

// 推荐: 使用ArrayDeque
Deque<String> stack = new ArrayDeque<>();
stack.push("a");
```

### 14.2 使用ArrayDeque替代LinkedList

```java
// 不推荐: 使用LinkedList作为队列
Queue<String> queue = new LinkedList<>();

// 推荐: 使用ArrayDeque
Queue<String> queue = new ArrayDeque<>();
```

### 14.3 指定合适的初始容量

```java
// 推荐: 指定初始容量
ArrayDeque<String> deque = new ArrayDeque<>(100);

// 不推荐: 使用默认容量
ArrayDeque<String> deque = new ArrayDeque<>();
```

### 14.4 使用双端队列操作

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addFirst("a");  // 添加到头部
deque.addLast("b");   // 添加到尾部

String first = deque.pollFirst();  // 移除头部
String last = deque.pollLast();   // 移除尾部
```

### 14.5 使用栈操作

```java
ArrayDeque<String> stack = new ArrayDeque<>();
stack.push("a");  // 压栈
String top = stack.pop();  // 出栈
```

## 十五、性能优化

### 15.1 使用循环数组

```java
// ArrayDeque使用循环数组,避免频繁的数组复制
// head和tail指针使用位运算实现循环
```

### 15.2 容量翻倍

```java
// ArrayDeque容量翻倍增长,减少扩容次数
private void doubleCapacity() {
    int newCapacity = n << 1;  // 容量翻倍
}
```

### 15.3 使用位运算

```java
// 使用位运算实现循环
head = (head - 1) & (elements.length - 1);
tail = (tail + 1) & (elements.length - 1);
```

## 十六、总结

ArrayDeque是Deque接口的可变数组实现,使用循环数组存储元素,性能极高。

### 核心要点

1. **可变数组**: 使用数组存储,可动态扩容
2. **双端队列**: 支持在两端插入和删除
3. **高性能**: 大多数操作的分摊时间复杂度为O(1)
4. **循环数组**: 使用head和tail指针实现循环
5. **禁止null**: 不允许null元素
6. **线程不安全**: 非同步实现,需要外部同步
7. **Fail-Fast**: 迭代器实现快速失败机制

### 适用场景

- 需要双端队列操作
- 需要高性能的栈操作
- 需要高性能的队列操作
- 需要频繁的插入和删除

### 不适用场景

- 需要null元素
- 多线程环境(需外部同步)
- 需要随机访问(虽然支持,但不如ArrayList)

### 性能特点

- 基本操作: O(1)分摊
- contains/remove: O(n)
- 空间复杂度: 较低

### 与LinkedList的选择

- 需要高性能: ArrayDeque
- 需要null元素: LinkedList
- 需要随机访问: ArrayDeque
- 需要频繁插入删除: ArrayDeque

### 与Stack的选择

- 推荐使用: ArrayDeque
- 不推荐使用: Stack(已过时)
- 需要线程安全: 使用Collections.synchronizedDeque
