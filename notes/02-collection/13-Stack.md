# Stack 源码解读

## 一、类概述

Stack是Java集合框架中的一个古老类,它表示后进先出(LIFO)栈的对象集合。Stack继承自Vector,提供了栈操作方法。Stack是线程安全的,因为Vector的所有公共方法都使用synchronized修饰。

### 1.1 核心特性

- **LIFO结构**: 后进先出(Last-In-First-Out)
- **继承Vector**: 继承自Vector,拥有Vector的所有功能
- **线程安全**: 继承Vector的线程安全特性
- **古老实现**: JDK 1.0就存在,设计较为陈旧
- **简单实现**: 代码非常简洁,只有141行
- **遗留类**: 官方推荐使用Deque接口替代

### 1.2 适用场景

- 需要LIFO数据结构(但不推荐)
- 遗留代码维护
- 需要与旧API兼容的场景

### 1.3 与Deque的区别

| 特性 | Stack | Deque(ArrayDeque) |
|------|-------|-------------------|
| 数据结构 | 继承Vector | 双端队列 |
| 线程安全 | 是(继承Vector) | 否 |
| 性能 | 较低(同步开销) | 较高 |
| 功能 | 仅栈操作 | 栈和队列操作 |
| 推荐 | 不推荐 | 推荐 |
| 初始容量 | 继承Vector(默认10) | 默认16 |

## 二、设计原理

### 2.1 LIFO结构

Stack使用Vector作为底层存储,栈顶是Vector的最后一个元素:

```
Vector存储:
[0]   element1  (栈底)
[1]   element2
[2]   element3
...
[n-1] elementN  (栈顶)
```

### 2.2 栈操作

- **push**: 将元素压入栈顶(Vector末尾)
- **pop**: 从栈顶弹出元素(Vector末尾)
- **peek**: 查看栈顶元素但不移除
- **empty**: 检查栈是否为空
- **search**: 查找元素在栈中的位置

## 三、继承结构

```
Vector<E>
    ↑
Stack<E>
```

### 3.1 继承的类

- Vector: 动态数组,线程安全的List实现

### 3.2 继承的特性

- 线程安全: 继承Vector的synchronized方法
- 动态扩容: 继承Vector的扩容机制
- 随机访问: 继承Vector的RandomAccess特性

## 四、核心方法解析

### 4.1 构造方法

```java
/**
 * 创建一个空栈。
 */
public Stack() {
}
```

**方法要点**:
- 无参构造方法
- 调用Vector的默认构造方法
- 初始容量为10,容量增量为0

### 4.2 push - 压入元素

```java
/**
 * 将一个项压入此栈的顶部。这与以下操作具有完全相同的效果:
 * <blockquote><pre>
 * addElement(item)</pre></blockquote>
 *
 * @param   item   要压入此栈的项。
 * @return  <code>item</code>参数。
 * @see     java.util.Vector#addElement
 */
public E push(E item) {
    addElement(item);

    return item;
}
```

**方法要点**:
- 调用Vector的addElement()方法
- 将元素添加到Vector末尾(栈顶)
- 返回压入的元素
- 时间复杂度O(1)(均摊)

### 4.3 pop - 弹出元素

```java
/**
 * 移除此栈顶部的对象并将该对象作为此函数的值返回。
 *
 * @return 此栈顶部的对象(Vector对象的最后一项)。
 * @throws EmptyStackException  如果此栈为空。
 */
public synchronized E pop() {
    E       obj;
    int     len = size();

    obj = peek();
    removeElementAt(len - 1);

    return obj;
}
```

**方法要点**:
- 先调用peek()获取栈顶元素
- 再调用removeElementAt()移除栈顶元素
- 如果栈为空,peek()会抛出EmptyStackException
- 时间复杂度O(n)(因为需要移动元素)

### 4.4 peek - 查看栈顶元素

```java
/**
 * 查看此栈顶部的对象,而不从栈中移除它。
 *
 * @return 此栈顶部的对象(Vector对象的最后一项)。
 * @throws EmptyStackException  如果此栈为空。
 */
public synchronized E peek() {
    int     len = size();

    if (len == 0)
        throw new EmptyStackException();
    return elementAt(len - 1);
}
```

**方法要点**:
- 获取Vector最后一个元素(栈顶)
- 如果栈为空,抛出EmptyStackException
- 不移除元素
- 时间复杂度O(1)

### 4.5 empty - 检查是否为空

```java
/**
 * 测试此栈是否为空。
 *
 * @return  <code>true</code>当且仅当此栈不包含任何项时;
 *          <code>false</code>否则。
 */
public boolean empty() {
    return size() == 0;
}
```

**方法要点**:
- 调用Vector的size()方法
- 检查元素数量是否为0
- 时间复杂度O(1)

### 4.6 search - 查找元素

```java
/**
 * 返回对象在此栈上的基于1的位置。
 * 如果对象<tt>o</tt>作为此栈中的项出现,此方法返回
 * 距离栈顶最近的那个出现项的位置;
 * 栈顶项被认为位于距离<tt>1</tt>。<tt>equals</tt>
 * 方法用于将<tt>o</tt>与此栈中的项进行比较。
 *
 * @param   o   所需对象。
 * @return  距离栈顶的基于1的位置,对象位于此位置;
 *          返回值<code>-1</code>指示对象不在栈上。
 */
public synchronized int search(Object o) {
    int i = lastIndexOf(o);

    if (i >= 0) {
        return size() - i;
    }
    return -1;
}
```

**方法要点**:
- 使用Vector的lastIndexOf()从后往前查找
- 返回距离栈顶的位置(1-based)
- 如果找不到,返回-1
- 时间复杂度O(n)

## 五、设计模式

### 5.1 继承模式

Stack继承Vector,复用Vector的功能:
- Vector: 提供动态数组和线程安全
- Stack: 添加栈操作方法

### 5.2 适配器模式

Stack将Vector适配为栈:
- Vector: 动态数组
- Stack: 栈操作接口

## 六、面试常见问题

### 6.1 Stack与Deque的区别?

| 特性 | Stack | Deque(ArrayDeque) |
|------|-------|-------------------|
| 数据结构 | 继承Vector | 双端队列 |
| 线程安全 | 是(继承Vector) | 否 |
| 性能 | 较低(同步开销) | 较高 |
| 功能 | 仅栈操作 | 栈和队列操作 |
| 推荐 | 不推荐 | 推荐 |
| 初始容量 | 继承Vector(默认10) | 默认16 |

### 6.2 Stack是线程安全的吗?

是的,继承自Vector,Vector的所有公共方法都使用synchronized修饰。但是:
- 使用粗粒度锁,并发性能较差
- 官方推荐使用Deque接口替代

### 6.3 Stack的push方法如何实现?

调用Vector的addElement()方法:
```java
public E push(E item) {
    addElement(item);  // 添加到Vector末尾
    return item;
}
```

### 6.4 Stack的pop方法如何实现?

先调用peek()获取栈顶元素,再调用removeElementAt()移除:
```java
public synchronized E pop() {
    E       obj;
    int     len = size();

    obj = peek();  // 获取栈顶元素
    removeElementAt(len - 1);  // 移除栈顶元素

    return obj;
}
```

### 6.5 Stack的peek方法如何实现?

获取Vector最后一个元素:
```java
public synchronized E peek() {
    int     len = size();

    if (len == 0)
        throw new EmptyStackException();
    return elementAt(len - 1);  // 获取最后一个元素
}
```

### 6.6 Stack的search方法返回什么?

返回元素距离栈顶的位置(1-based):
- 如果元素在栈顶,返回1
- 如果元素在栈顶下方,返回2,3,...
- 如果元素不在栈中,返回-1

### 6.7 为什么Stack不推荐使用?

1. **性能问题**: 继承Vector,使用粗粒度锁,并发性能差
2. **设计陈旧**: JDK 1.0就存在,设计较旧
3. **功能有限**: 仅支持栈操作,不支持队列操作
4. **更好的替代**: Deque接口功能更强大,性能更好

### 6.8 Stack的初始容量是多少?

继承Vector,默认初始容量为10:
```java
public Stack() {
    // 调用Vector的默认构造方法
}
```

### 6.9 Stack的扩容机制?

继承Vector的扩容机制:
- 如果Vector的capacityIncrement > 0: 新容量 = 旧容量 + capacityIncrement
- 如果Vector的capacityIncrement <= 0: 新容量 = 旧容量 * 2

### 6.10 Stack的时间复杂度?

- push(): O(1)(均摊)
- pop(): O(n)(因为需要移动元素)
- peek(): O(1)
- empty(): O(1)
- search(): O(n)

## 七、使用场景

### 7.1 遗留代码维护

```java
// 遗留代码中使用Stack
Stack<String> stack = new Stack<>();
stack.push("value1");
stack.push("value2");
String value = stack.pop();
```

### 7.2 需要与旧API兼容

```java
// 某些旧API要求使用Stack
public void process(Stack<String> data) {
    // 处理数据
}
```

### 7.3 简单的LIFO结构(不推荐)

```java
// 简单场景下可以使用,但不推荐
Stack<String> stack = new Stack<>();
stack.push("value");
```

## 八、注意事项

### 8.1 性能问题

```java
// Stack继承Vector,使用粗粒度锁,并发性能差
Stack<String> stack = new Stack<>();

// 推荐: 使用ArrayDeque
Deque<String> deque = new ArrayDeque<>();
deque.push("value");
```

### 8.2 空栈异常

```java
Stack<String> stack = new Stack<>();

// 错误: 栈为空时pop或peek会抛出EmptyStackException
// stack.pop();  // EmptyStackException
// stack.peek();  // EmptyStackException

// 正确: 先检查是否为空
if (!stack.empty()) {
    String value = stack.pop();
}
```

### 8.3 search方法的使用

```java
Stack<String> stack = new Stack<>();
stack.push("value1");
stack.push("value2");
stack.push("value3");

// search返回距离栈顶的位置
int position = stack.search("value2");  // 返回2
int position2 = stack.search("value3");  // 返回1
int position3 = stack.search("value4");  // 返回-1
```

### 8.4 线程安全

```java
// Stack是线程安全的,因为继承Vector
Stack<String> stack = new Stack<>();

// 多线程环境下可以安全使用
// 但并发性能较差
```

## 九、与其他数据结构的对比

### 9.1 Stack vs ArrayDeque

```java
// Stack: 继承Vector,线程安全,性能较差
Stack<String> stack = new Stack<>();
stack.push("value");
String value = stack.pop();

// ArrayDeque: 非线程安全,性能较好
Deque<String> deque = new ArrayDeque<>();
deque.push("value");
String value = deque.pop();
```

### 9.2 Stack vs LinkedList

| 特性 | Stack | LinkedList(作为Deque) |
|------|-------|---------------------|
| 底层实现 | Vector | 双向链表 |
| 线程安全 | 是 | 否 |
| 随机访问 | 支持 | 不支持 |
| 性能 | 较低 | 较高 |
| 内存开销 | 较低 | 较高(节点对象) |

### 9.3 Stack vs Queue

Stack是LIFO结构,Queue是FIFO结构:
```java
// Stack: 后进先出
Stack<String> stack = new Stack<>();
stack.push("value1");
stack.push("value2");
String value = stack.pop();  // 返回"value2"

// Queue: 先进先出
Queue<String> queue = new LinkedList<>();
queue.offer("value1");
queue.offer("value2");
String value = queue.poll();  // 返回"value1"
```

## 十、最佳实践

### 10.1 选择合适的数据结构

```java
// LIFO结构: 推荐使用Deque
Deque<String> stack = new ArrayDeque<>();
stack.push("value");
String value = stack.pop();

// FIFO结构: 使用Queue
Queue<String> queue = new LinkedList<>();
queue.offer("value");
String value = queue.poll();

// 遗留代码: Stack(不推荐)
Stack<String> stack = new Stack<>();
stack.push("value");
```

### 10.2 处理空栈异常

```java
Stack<String> stack = new Stack<>();

// 推荐: 先检查是否为空
if (!stack.empty()) {
    String value = stack.pop();
} else {
    // 处理空栈情况
}

// 或者使用try-catch
try {
    String value = stack.pop();
} catch (EmptyStackException e) {
    // 处理空栈情况
}
```

### 10.3 使用search方法

```java
Stack<String> stack = new Stack<>();
stack.push("value1");
stack.push("value2");
stack.push("value3");

// search返回距离栈顶的位置
int position = stack.search("value2");
if (position > 0) {
    System.out.println("元素在栈顶下方" + (position - 1) + "个位置");
} else {
    System.out.println("元素不在栈中");
}
```

### 10.4 迁移到Deque

```java
// 旧代码
Stack<String> stack = new Stack<>();
stack.push("value");
String value = stack.pop();

// 新代码: 迁移到Deque
Deque<String> deque = new ArrayDeque<>();
deque.push("value");
String value = deque.pop();
```

### 10.5 线程安全考虑

```java
// Stack是线程安全的,但性能较差
Stack<String> stack = new Stack<>();

// 如果需要线程安全且高性能,考虑使用ConcurrentLinkedDeque
Deque<String> deque = new ConcurrentLinkedDeque<>();
```

## 十一、总结

Stack是一个古老的LIFO栈实现,继承自Vector,提供了栈操作方法。由于设计陈旧和性能问题,官方不推荐使用,建议使用Deque接口(ArrayDeque或LinkedList)替代。

### 核心要点

1. **LIFO结构**: 后进先出(Last-In-First-Out)
2. **继承Vector**: 继承Vector的所有功能和线程安全特性
3. **简单实现**: 代码非常简洁,只有141行
4. **古老实现**: JDK 1.0就存在,设计较为陈旧
5. **同步开销**: 继承Vector的粗粒度锁,并发性能较差
6. **遗留类**: 官方推荐使用Deque接口替代
7. **功能有限**: 仅支持栈操作,不支持队列操作

### 适用场景

- 遗留代码维护
- 需要与旧API兼容
- 简单的LIFO结构(但不推荐)

### 不适用场景

- 高并发环境(推荐ConcurrentLinkedDeque)
- 高性能要求
- 需要队列操作
- 新项目开发

### 替代方案

- **LIFO结构**: ArrayDeque或LinkedList(作为Deque)
- **线程安全LIFO**: ConcurrentLinkedDeque
- **FIFO结构**: LinkedList或ArrayDeque(作为Queue)
- **线程安全FIFO**: ConcurrentLinkedQueue或LinkedBlockingQueue
