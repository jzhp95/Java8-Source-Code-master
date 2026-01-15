# Vector 源码解读

## 一、类概述

Vector是Java集合框架中的一个古老类,它实现了可增长的对象数组。Vector是线程安全的,所有公共方法都使用synchronized修饰。它实现了List接口,是ArrayList的线程安全版本。

### 1.1 核心特性

- **线程安全**: 所有公共方法都使用synchronized修饰
- **动态数组**: 内部使用Object[]数组,可自动扩容
- **容量管理**: 支持capacityIncrement,控制扩容增量
- **古老实现**: JDK 1.0就存在,设计较为陈旧
- **同步开销**: 使用粗粒度锁,并发性能较差
- **遗留类**: 官方推荐使用ArrayList替代

### 1.2 适用场景

- 需要线程安全的动态数组(但不推荐)
- 遗留代码维护
- 需要与旧API兼容的场景

### 1.3 与ArrayList的区别

| 特性 | Vector | ArrayList |
|------|--------|-----------|
| 线程安全 | 是(synchronized) | 否 |
| 扩容增量 | 可配置capacityIncrement | 默认1.5倍 |
| 初始容量 | 默认10 | 默认10 |
| 性能 | 较低(同步开销) | 较高 |
| 推荐 | 不推荐 | 推荐 |
| 继承 | AbstractList | AbstractList |

## 二、设计原理

### 2.1 动态数组结构

Vector使用Object[]数组存储元素:

```
elementData数组:
[0]   element1
[1]   element2
[2]   element3
...
[n-1] elementN
[n]   null
[n+1] null
...
```

### 2.2 容量管理

Vector维护两个容量相关参数:
- **capacity**: 内部数组的长度
- **capacityIncrement**: 容量增量,如果<=0则容量翻倍

### 2.3 扩容机制

当元素数量超过容量时扩容:
- 如果capacityIncrement > 0: 新容量 = 旧容量 + capacityIncrement
- 如果capacityIncrement <= 0: 新容量 = 旧容量 * 2

## 三、继承结构

```
AbstractList<E>
    ↑
Vector<E>
    implements List<E>, RandomAccess, Cloneable, Serializable
```

### 3.1 继承的类

- AbstractList: 抽象类,提供List的基本实现

### 3.2 实现的接口

- List: List接口
- RandomAccess: 支持快速随机访问
- Cloneable: 可克隆
- Serializable: 可序列化

## 四、核心字段分析

### 4.1 实例字段

```java
// 内部数组缓冲区,存储Vector的元素
protected Object[] elementData;

// Vector中有效元素的数量
protected int elementCount;

// Vector容量自动增量
// 当Vector大小超过容量时,容量自动增加的量
// 如果容量增量小于或等于零,每次需要增长时Vector的容量翻倍
protected int capacityIncrement;
```

### 4.2 常量字段

```java
// 数组最大大小
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

// 序列化版本号
private static final long serialVersionUID = -2767605614048989439L;
```

### 4.3 字段设计要点

1. **elementData数组**: 存储元素,容量可能大于实际元素数量
2. **elementCount**: 实际元素数量
3. **capacityIncrement**: 控制扩容增量,如果<=0则容量翻倍
4. **protected访问**: 字段都是protected,便于子类访问

## 五、核心方法解析

### 5.1 构造方法

```java
// 指定初始容量和容量增量
public Vector(int initialCapacity, int capacityIncrement) {
    super();
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    this.elementData = new Object[initialCapacity];
    this.capacityIncrement = capacityIncrement;
}

// 指定初始容量,容量增量为0
public Vector(int initialCapacity) {
    this(initialCapacity, 0);
}

// 默认构造方法
public Vector() {
    this(10);  // 默认容量10,容量增量0
}

// 从另一个Collection构造
public Vector(Collection<? extends E> c) {
    elementData = c.toArray();
    elementCount = elementData.length;
    // c.toArray可能(不正确地)不返回Object[](参见6260652)
    if (elementData.getClass() != Object[].class)
        elementData = Arrays.copyOf(elementData, elementCount, Object[].class);
}
```

### 5.2 copyInto - 复制到数组

```java
public synchronized void copyInto(Object[] anArray) {
    System.arraycopy(elementData, 0, anArray, 0, elementCount);
}
```

### 5.3 trimToSize - 裁剪容量

```java
public synchronized void trimToSize() {
    modCount++;
    int oldCapacity = elementData.length;
    if (elementCount < oldCapacity) {
        elementData = Arrays.copyOf(elementData, elementCount);
    }
}
```

**方法要点**:
- 将容量调整为当前大小
- 释放未使用的空间

### 5.4 ensureCapacity - 确保容量

```java
public synchronized void ensureCapacity(int minCapacity) {
    if (minCapacity > 0) {
        modCount++;
        ensureCapacityHelper(minCapacity);
    }
}

private void ensureCapacityHelper(int minCapacity) {
    // 溢出感知代码
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```

### 5.5 grow - 扩容

```java
private void grow(int minCapacity) {
    // 溢出感知代码
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                         capacityIncrement : oldCapacity);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // 溢出
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
}
```

**方法要点**:
- 如果capacityIncrement > 0: 新容量 = 旧容量 + capacityIncrement
- 如果capacityIncrement <= 0: 新容量 = 旧容量 * 2
- 确保新容量至少为minCapacity

### 5.6 setSize - 设置大小

```java
public synchronized void setSize(int newSize) {
    modCount++;
    if (newSize > elementCount) {
        ensureCapacityHelper(newSize);
    } else {
        for (int i = newSize ; i < elementCount ; i++) {
            elementData[i] = null;
        }
    }
    elementCount = newSize;
}
```

### 5.7 capacity - 获取容量

```java
public synchronized int capacity() {
    return elementData.length;
}
```

### 5.8 size - 获取大小

```java
public synchronized int size() {
    return elementCount;
}
```

### 5.9 isEmpty - 检查是否为空

```java
public synchronized boolean isEmpty() {
    return elementCount == 0;
}
```

### 5.10 elements - 获取元素枚举

```java
public Enumeration<E> elements() {
    return new Enumeration<E>() {
        int count = 0;

        public boolean hasMoreElements() {
            return count < elementCount;
        }

        public E nextElement() {
            synchronized (Vector.this) {
                if (count < elementCount) {
                    return elementData(count++);
                }
            }
            throw new NoSuchElementException("Vector Enumeration");
        }
    };
}
```

**注意**: Enumeration不是fail-fast的。

### 5.11 contains - 检查是否包含元素

```java
public boolean contains(Object o) {
    return indexOf(o, 0) >= 0;
}
```

### 5.12 indexOf - 查找元素索引

```java
public int indexOf(Object o) {
    return indexOf(o, 0);
}

public synchronized int indexOf(Object o, int index) {
    if (o == null) {
        for (int i = index ; i < elementCount ; i++)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = index ; i < elementCount ; i++)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}
```

### 5.13 lastIndexOf - 从后往前查找

```java
public synchronized int lastIndexOf(Object o) {
    return lastIndexOf(o, elementCount-1);
}

public synchronized int lastIndexOf(Object o, int index) {
    if (index >= elementCount)
        throw new IndexOutOfBoundsException(index + " >= "+ elementCount);

    if (o == null) {
        for (int i = index; i >= 0; i--)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = index; i >= 0; i--)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}
```

### 5.14 elementAt - 获取指定索引的元素

```java
public synchronized E elementAt(int index) {
    if (index >= elementCount) {
        throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
    }

    return elementData(index);
}
```

### 5.15 firstElement - 获取第一个元素

```java
public synchronized E firstElement() {
    if (elementCount == 0) {
        throw new NoSuchElementException();
    }
    return elementData(0);
}
```

### 5.16 lastElement - 获取最后一个元素

```java
public synchronized E lastElement() {
    if (elementCount == 0) {
        throw new NoSuchElementException();
    }
    return elementData(elementCount - 1);
}
```

### 5.17 setElementAt - 设置指定索引的元素

```java
public synchronized void setElementAt(E obj, int index) {
    if (index >= elementCount) {
        throw new ArrayIndexOutOfBoundsException(index + " >= " +
                                                     elementCount);
    }
    elementData[index] = obj;
}
```

### 5.18 removeElementAt - 移除指定索引的元素

```java
public synchronized void removeElementAt(int index) {
    modCount++;
    if (index >= elementCount) {
        throw new ArrayIndexOutOfBoundsException(index + " >= " +
                                                     elementCount);
    }
    else if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
    }
    int j = elementCount - index - 1;
    if (j > 0) {
        System.arraycopy(elementData, index + 1, elementData, index, j);
    }
    elementCount--;
    elementData[elementCount] = null; /* 让gc做它的工作 */
}
```

### 5.19 insertElementAt - 在指定位置插入元素

```java
public synchronized void insertElementAt(E obj, int index) {
    modCount++;
    if (index > elementCount) {
        throw new ArrayIndexOutOfBoundsException(index
                                                     + " > " + elementCount);
    }
    ensureCapacityHelper(elementCount + 1);
    System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
    elementData[index] = obj;
    elementCount++;
}
```

### 5.20 addElement - 添加元素到末尾

```java
public synchronized void addElement(E obj) {
    modCount++;
    ensureCapacityHelper(elementCount + 1);
    elementData[elementCount++] = obj;
}
```

### 5.21 removeElement - 移除指定元素

```java
public synchronized boolean removeElement(Object obj) {
    modCount++;
    int i = indexOf(obj);
    if (i >= 0) {
        removeElementAt(i);
        return true;
    }
    return false;
}
```

### 5.22 removeAllElements - 移除所有元素

```java
public synchronized void removeAllElements() {
    modCount++;
    // 让gc做它的工作
    for (int i = 0; i < elementCount; i++)
        elementData[i] = null;

    elementCount = 0;
}
```

### 5.23 clone - 克隆

```java
public synchronized Object clone() {
    try {
        @SuppressWarnings("unchecked")
            Vector<E> v = (Vector<E>) super.clone();
        v.elementData = Arrays.copyOf(elementData, elementCount);
        v.modCount = 0;
        return v;
    } catch (CloneNotSupportedException e) {
        // 这不应该发生,因为我们是Cloneable
        throw new InternalError(e);
    }
}
```

### 5.24 toArray - 转换为数组

```java
public synchronized Object[] toArray() {
    return Arrays.copyOf(elementData, elementCount);
}

@SuppressWarnings("unchecked")
public synchronized <T> T[] toArray(T[] a) {
    if (a.length < elementCount)
        return (T[]) Arrays.copyOf(elementData, elementCount, a.getClass());

    System.arraycopy(elementData, 0, a, 0, elementCount);

    if (a.length > elementCount)
        a[elementCount] = null;

    return a;
}
```

### 5.25 get - 获取指定索引的元素

```java
public synchronized E get(int index) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    return elementData(index);
}
```

### 5.26 set - 设置指定索引的元素

```java
public synchronized E set(int index, E element) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
}
```

### 5.27 add - 添加元素

```java
public synchronized boolean add(E e) {
    modCount++;
    ensureCapacityHelper(elementCount + 1);
    elementData[elementCount++] = e;
    return true;
}
```

### 5.28 remove - 移除指定索引的元素

```java
public synchronized E remove(int index) {
    modCount++;
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);

    int numMoved = elementCount - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
    elementData[--elementCount] = null; // 让gc做它的工作
    return oldValue;
}
```

### 5.29 clear - 清空

```java
public void clear() {
    removeAllElements();
}
```

### 5.30 addAll - 批量添加

```java
public synchronized boolean addAll(Collection<? extends E> c) {
    modCount++;
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityHelper(elementCount + numNew);
    System.arraycopy(a, 0, elementData, elementCount, numNew);
    elementCount += numNew;
    return numNew != 0;
}

public synchronized boolean addAll(int index, Collection<? extends E> c) {
    modCount++;
    if (index < 0 || index > elementCount)
        throw new ArrayIndexOutOfBoundsException(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityHelper(elementCount + numNew);

    int numMoved = elementCount - index;
    if (numMoved > 0)
        System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);
    System.arraycopy(a, 0, elementData, index, numNew);
    elementCount += numNew;
    return numNew != 0;
}
```

## 六、设计模式

### 6.1 模板方法模式

AbstractList定义了List的基本骨架,Vector实现具体操作:
- AbstractList: 提供通用实现
- Vector: 实现特定行为(线程安全)

### 6.2 枚举器模式

提供Enumeration接口:
```java
public Enumeration<E> elements() {
    return new Enumeration<E>() {
        int count = 0;

        public boolean hasMoreElements() {
            return count < elementCount;
        }

        public E nextElement() {
            synchronized (Vector.this) {
                if (count < elementCount) {
                    return elementData(count++);
                }
            }
            throw new NoSuchElementException("Vector Enumeration");
        }
    };
}
```

### 6.3 原型模式

实现Cloneable接口,支持浅拷贝:
```java
public synchronized Object clone() {
    Vector<E> v = (Vector<E>) super.clone();
    v.elementData = Arrays.copyOf(elementData, elementCount);
    v.modCount = 0;
    return v;
}
```

## 七、面试常见问题

### 7.1 Vector与ArrayList的区别?

| 特性 | Vector | ArrayList |
|------|--------|-----------|
| 线程安全 | 是(synchronized) | 否 |
| 扩容增量 | 可配置capacityIncrement | 默认1.5倍 |
| 初始容量 | 默认10 | 默认10 |
| 性能 | 较低(同步开销) | 较高 |
| 推荐 | 不推荐 | 推荐 |
| 枚举器 | 支持Enumeration | 不支持 |

### 7.2 Vector是线程安全的吗?

是的,所有公共方法都使用synchronized修饰。但是:
- 使用粗粒度锁,并发性能较差
- 官方推荐使用ArrayList或CopyOnWriteArrayList替代

### 7.3 Vector的扩容机制?

当elementCount >= elementData.length时扩容:
- 如果capacityIncrement > 0: 新容量 = 旧容量 + capacityIncrement
- 如果capacityIncrement <= 0: 新容量 = 旧容量 * 2

### 7.4 Vector的初始容量是多少?

默认初始容量为10:
```java
public Vector() {
    this(10);
}
```

### 7.5 Vector支持Enumeration吗?

支持,提供了elements()方法:
```java
public Enumeration<E> elements() {
    return new Enumeration<E>() { ... };
}
```

### 7.6 Vector的Enumeration是fail-fast的吗?

不是。Enumeration不会检查并发修改:
```java
public E nextElement() {
    synchronized (Vector.this) {
        if (count < elementCount) {
            return elementData(count++);
        }
    }
    throw new NoSuchElementException("Vector Enumeration");
}
```

### 7.7 Vector的capacityIncrement有什么作用?

控制扩容增量:
- 如果capacityIncrement > 0: 每次扩容增加capacityIncrement
- 如果capacityIncrement <= 0: 每次扩容容量翻倍

### 7.8 为什么Vector不推荐使用?

1. **性能问题**: 使用粗粒度锁,并发性能差
2. **设计陈旧**: JDK 1.0就存在,设计较旧
3. **更好的替代**: ArrayList性能更好
4. **并发场景**: CopyOnWriteArrayList更适合

### 7.9 Vector的trimToSize方法有什么作用?

将容量调整为当前大小,释放未使用的空间:
```java
public synchronized void trimToSize() {
    modCount++;
    int oldCapacity = elementData.length;
    if (elementCount < oldCapacity) {
        elementData = Arrays.copyOf(elementData, elementCount);
    }
}
```

### 7.10 Vector的ensureCapacity方法有什么作用?

确保Vector至少能容纳指定数量的元素,如果需要则扩容:
```java
public synchronized void ensureCapacity(int minCapacity) {
    if (minCapacity > 0) {
        modCount++;
        ensureCapacityHelper(minCapacity);
    }
}
```

## 八、使用场景

### 8.1 遗留代码维护

```java
// 遗留代码中使用Vector
Vector<String> vector = new Vector<>();
vector.addElement("value");
```

### 8.2 需要与旧API兼容

```java
// 某些旧API要求使用Vector
public void process(Vector<String> data) {
    // 处理数据
}
```

### 8.3 简单的线程安全列表(不推荐)

```java
// 简单场景下可以使用,但不推荐
Vector<String> vector = new Vector<>();
```

## 九、注意事项

### 9.1 性能问题

```java
// Vector使用粗粒度锁,并发性能差
Vector<String> vector = new Vector<>();

// 推荐: 使用CopyOnWriteArrayList
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
```

### 9.2 容量管理

```java
// 设置合适的初始容量和容量增量
int initialCapacity = 100;
int capacityIncrement = 50;
Vector<String> vector = new Vector<>(initialCapacity, capacityIncrement);
```

### 9.3 枚举器使用

```java
Vector<String> vector = new Vector<>();
vector.addElement("value1");
vector.addElement("value2");

// 使用Enumeration遍历
Enumeration<String> elements = vector.elements();
while (elements.hasMoreElements()) {
    String value = elements.nextElement();
    System.out.println(value);
}
```

### 9.4 扩容性能

```java
// 设置合适的初始容量,避免频繁扩容
int expectedSize = 1000;
Vector<String> vector = new Vector<>(expectedSize);
```

## 十、与其他List的对比

### 10.1 Vector vs ArrayList

```java
// Vector: 线程安全
Vector<String> vector = new Vector<>();
vector.addElement("value");

// ArrayList: 非线程安全
ArrayList<String> list = new ArrayList<>();
list.add("value");
```

### 10.2 Vector vs CopyOnWriteArrayList

| 特性 | Vector | CopyOnWriteArrayList |
|------|--------|---------------------|
| 线程安全 | 是(synchronized) | 是(写时复制) |
| 并发性能 | 低 | 高(读多写少) |
| 内存开销 | 低 | 高(每次写都复制) |
| 适用场景 | 简单同步 | 读多写少 |

### 10.3 Vector vs Stack

Stack继承自Vector,增加了栈操作:
```java
Stack<String> stack = new Stack<>();
stack.push("value1");
stack.push("value2");
String value = stack.pop();
```

## 十一、最佳实践

### 11.1 选择合适的List

```java
// 单线程环境: ArrayList
List<String> list = new ArrayList<>();

// 多线程环境(读多写少): CopyOnWriteArrayList
List<String> copyOnWriteList = new CopyOnWriteArrayList<>();

// 遗留代码: Vector(不推荐)
Vector<String> vector = new Vector<>();

// 需要栈操作: Stack
Stack<String> stack = new Stack<>();
```

### 11.2 设置合适的初始容量

```java
// 根据预期大小设置初始容量
int expectedSize = 1000;
Vector<String> vector = new Vector<>(expectedSize);
```

### 11.3 使用Enumeration

```java
Vector<String> vector = new Vector<>();
vector.addElement("value1");
vector.addElement("value2");

// 使用Enumeration遍历
Enumeration<String> elements = vector.elements();
while (elements.hasMoreElements()) {
    String value = elements.nextElement();
    System.out.println(value);
}
```

### 11.4 优化容量管理

```java
// 设置合适的容量增量
int initialCapacity = 100;
int capacityIncrement = 50;
Vector<String> vector = new Vector<>(initialCapacity, capacityIncrement);
```

### 11.5 迁移到CopyOnWriteArrayList

```java
// 旧代码
Vector<String> vector = new Vector<>();

// 新代码: 迁移到CopyOnWriteArrayList
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
```

## 十二、总结

Vector是一个古老的线程安全List实现,使用synchronized方法实现线程安全。由于设计陈旧和性能问题,官方不推荐使用,建议使用ArrayList或CopyOnWriteArrayList替代。

### 核心要点

1. **线程安全**: 所有公共方法都使用synchronized修饰
2. **动态数组**: 内部使用Object[]数组,可自动扩容
3. **容量管理**: 支持capacityIncrement,控制扩容增量
4. **古老实现**: JDK 1.0就存在,设计较为陈旧
5. **同步开销**: 使用粗粒度锁,并发性能较差
6. **遗留类**: 官方推荐使用ArrayList或CopyOnWriteArrayList替代
7. **初始容量**: 默认10

### 适用场景

- 遗留代码维护
- 需要与旧API兼容
- 简单的线程安全列表(但不推荐)

### 不适用场景

- 高并发环境(推荐CopyOnWriteArrayList)
- 高性能要求
- 新项目开发
- 读多写少场景

### 替代方案

- **单线程**: ArrayList
- **多线程(读多写少)**: CopyOnWriteArrayList
- **需要栈操作**: Stack
- **需要同步访问**: Collections.synchronizedList()
