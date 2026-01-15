# EnumMap 源码解读

## 一、类概述

EnumMap是Java集合框架中专门用于枚举类型键的Map实现。所有键必须来自单个枚举类型,该类型在创建映射时指定或隐式确定。EnumMap内部使用数组表示,这种表示方式极其紧凑和高效。

### 1.1 核心特性

- **枚举专用**: 键必须是枚举类型
- **数组表示**: 使用数组存储键值对
- **高性能**: 所有基本操作都是O(1)时间复杂度
- **类型安全**: 编译时类型检查
- **有序性**: 按枚举常量的声明顺序迭代
- **非同步**: 非线程安全,需要外部同步
- **Weakly Consistent**: 迭代器不会抛出ConcurrentModificationException
- **支持null值**: 允许null值,但不允许null键

### 1.2 适用场景

- 枚举类型作为键的映射
- 需要高性能的枚举映射
- 需要类型安全的枚举操作
- 需要保持键的顺序

## 二、设计原理

### 2.1 数组表示

EnumMap使用数组存储键值对:

```java
enum Color {
    RED,    // index 0
    GREEN,  // index 1
    BLUE    // index 2
}

// EnumMap内部表示
// vals[0] -> RED对应的值
// vals[1] -> GREEN对应的值
// vals[2] -> BLUE对应的值
```

### 2.2 枚举常量数组

EnumMap缓存枚举常量数组:

```java
private transient K[] keyUniverse;  // 所有枚举常量

private static <K extends Enum<K>> K[] getKeyUniverse(Class<K> keyType) {
    return SharedSecrets.getJavaLangAccess()
                                    .getEnumConstantsShared(keyType);
}
```

**设计要点**:
- 缓存枚举常量,提高性能
- 用于索引到枚举常量的映射
- 所有实例共享同一个数组

### 2.3 null值处理

EnumMap使用特殊对象表示null值:

```java
private static final Object NULL = new Object() {
    public int hashCode() {
        return 0;
    }

    public String toString() {
        return "java.util.EnumMap.NULL";
    }
};

private Object maskNull(Object value) {
    return (value == null ? NULL : value);
}

@SuppressWarnings("unchecked")
private V unmaskNull(Object value) {
    return (V)(value == NULL ? null : value);
}
```

**设计要点**:
- 使用NULL对象区分null值和未映射的键
- maskNull: 将null转换为NULL对象
- unmaskNull: 将NULL对象转换回null

## 三、继承结构

```
AbstractMap<K, V>
    ↑
EnumMap<K extends Enum<K>, V>
    implements Serializable, Cloneable
```

### 3.1 继承的方法

- AbstractMap提供的基础Map操作
- Serializable支持序列化
- Cloneable支持克隆

## 四、核心字段分析

### 4.1 键类型

```java
private final Class<K> keyType;
```

**字段设计要点**:
- 存储枚举键的类型
- 用于类型检查
- final修饰,不可修改

### 4.2 枚举常量数组

```java
private transient K[] keyUniverse;
```

**字段设计要点**:
- 存储所有枚举常量
- 用于索引到枚举常量的映射
- transient修饰,自定义序列化

### 4.3 值数组

```java
private transient Object[] vals;
```

**字段设计要点**:
- 存储所有键对应的值
- 索引对应枚举常量的ordinal
- null表示未映射,NULL表示映射到null

### 4.4 大小

```java
private transient int size = 0;
```

**字段设计要点**:
- 存储映射的数量
- transient修饰,自定义序列化

### 4.5 NULL对象

```java
private static final Object NULL = new Object() {
    public int hashCode() {
        return 0;
    }

    public String toString() {
        return "java.util.EnumMap.NULL";
    }
};
```

**设计要点**:
- 特殊对象,表示null值
- 区分null值和未映射的键
- 单例对象

## 五、构造方法

### 5.1 指定键类型的构造方法

```java
public EnumMap(Class<K> keyType) {
    this.keyType = keyType;
    keyUniverse = getKeyUniverse(keyType);
    vals = new Object[keyUniverse.length];
}
```

**方法要点**:
- 创建空的EnumMap
- 初始化键类型和枚举常量数组
- 创建值数组

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
```

### 5.2 从EnumMap构造

```java
public EnumMap(EnumMap<K, ? extends V> m) {
    keyType = m.keyType;
    keyUniverse = m.keyUniverse;
    vals = m.vals.clone();
    size = m.size;
}
```

**方法要点**:
- 复制另一个EnumMap
- 克隆值数组
- 复制大小

**示例**:
```java
EnumMap<Color, String> map1 = new EnumMap<>(Color.class);
map1.put(Color.RED, "红色");

EnumMap<Color, String> map2 = new EnumMap<>(map1);
```

### 5.3 从Map构造

```java
public EnumMap(Map<K, ? extends V> m) {
    if (m instanceof EnumMap) {
        EnumMap<K, ? extends V> em = (EnumMap<K, ? extends V>) m;
        keyType = em.keyType;
        keyUniverse = em.keyUniverse;
        vals = em.vals.clone();
        size = em.size;
    } else {
        if (m.isEmpty())
            throw new IllegalArgumentException("Specified map is empty");
        keyType = m.keySet().iterator().next().getDeclaringClass();
        keyUniverse = getKeyUniverse(keyType);
        vals = new Object[keyUniverse.length];
        putAll(m);
    }
}
```

**方法要点**:
- 如果是EnumMap,直接复制
- 如果是普通Map,从第一个键确定类型
- 逐个添加映射

**示例**:
```java
Map<Color, String> map = new HashMap<>();
map.put(Color.RED, "红色");
map.put(Color.GREEN, "绿色");

EnumMap<Color, String> enumMap = new EnumMap<>(map);
```

## 六、查询操作

### 6.1 size - 获取大小

```java
public int size() {
    return size;
}
```

**时间复杂度**: O(1)

### 6.2 containsKey - 检查包含键

```java
public boolean containsKey(Object key) {
    return isValidKey(key) && vals[((Enum<?>)key).ordinal()] != null;
}
```

**方法要点**:
- 检查键是否有效
- 检查对应索引的值是否为null
- 时间复杂度: O(1)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");

boolean contains = map.containsKey(Color.RED);  // true
```

### 6.3 containsValue - 检查包含值

```java
public boolean containsValue(Object value) {
    value = maskNull(value);

    for (Object val : vals)
        if (value.equals(val))
            return true;

    return false;
}
```

**方法要点**:
- 遍历值数组
- 使用maskNull处理null值
- 时间复杂度: O(n)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");

boolean contains = map.containsValue("红色");  // true
```

### 6.4 get - 获取值

```java
public V get(Object key) {
    return (isValidKey(key) ?
                unmaskNull(vals[((Enum<?>)key).ordinal()]) : null);
}
```

**方法要点**:
- 检查键是否有效
- 使用ordinal作为索引
- 使用unmaskNull处理NULL对象
- 时间复杂度: O(1)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");

String value = map.get(Color.RED);  // "红色"
```

## 七、修改操作

### 7.1 put - 添加映射

```java
public V put(K key, V value) {
    typeCheck(key);

    int index = key.ordinal();
    Object oldValue = vals[index];
    vals[index] = maskNull(value);
    if (oldValue == null)
        size++;
    return unmaskNull(oldValue);
}
```

**方法要点**:
- 检查键类型
- 使用ordinal作为索引
- 使用maskNull处理null值
- 如果是新的映射,增加size
- 时间复杂度: O(1)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
map.put(Color.GREEN, "绿色");
```

### 7.2 remove - 移除映射

```java
public V remove(Object key) {
    if (!isValidKey(key))
        return null;
    int index = ((Enum<?>)key).ordinal();
    Object oldValue = vals[index];
    vals[index] = null;
    if (oldValue != null)
        size--;
    return unmaskNull(oldValue);
}
```

**方法要点**:
- 检查键是否有效
- 清除对应索引的值
- 如果存在映射,减少size
- 时间复杂度: O(1)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");

String value = map.remove(Color.RED);  // "红色"
```

### 7.3 putAll - 批量添加

```java
public void putAll(Map<? extends K, ? extends V> m) {
    if (m instanceof EnumMap) {
        EnumMap<?, ?> em = (EnumMap<?, ?>)m;
        if (em.keyType != keyType) {
            if (em.isEmpty())
                return;
            throw new ClassCastException(em.keyType + " != " + keyType);
        }

        for (int i = 0; i < keyUniverse.length; i++) {
            Object emValue = em.vals[i];
            if (emValue != null) {
                if (vals[i] == null)
                    size++;
                vals[i] = emValue;
            }
        }
    } else {
        super.putAll(m);
    }
}
```

**方法要点**:
- 如果是EnumMap,直接复制数组
- 如果是普通Map,逐个添加
- 时间复杂度: O(n)

**示例**:
```java
EnumMap<Color, String> map1 = new EnumMap<>(Color.class);
map1.put(Color.RED, "红色");

EnumMap<Color, String> map2 = new EnumMap<>(Color.class);
map2.putAll(map1);
```

### 7.4 clear - 清空映射

```java
public void clear() {
    Arrays.fill(vals, null);
    size = 0;
}
```

**方法要点**:
- 清空值数组
- 重置大小
- 时间复杂度: O(n)

**示例**:
```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
map.clear();
```

## 八、辅助方法

### 8.1 isValidKey - 检查键是否有效

```java
private boolean isValidKey(Object key) {
    if (key == null)
        return false;

    // Cheaper than instanceof Enum followed by getDeclaringClass
    Class<?> keyClass = key.getClass();
    return keyClass == keyType || keyClass.getSuperclass() == keyType;
}
```

**方法要点**:
- 检查键是否为null
- 检查键类型是否匹配
- 允许子类枚举

### 8.2 typeCheck - 类型检查

```java
private void typeCheck(K key) {
    Class<?> keyClass = key.getClass();
    if (keyClass != keyType && keyClass.getSuperclass() != keyType)
        throw new ClassCastException(keyClass + " != " + keyType);
}
```

**方法要点**:
- 检查键类型是否匹配
- 抛出ClassCastException

### 8.3 maskNull - 掩盖null

```java
private Object maskNull(Object value) {
    return (value == null ? NULL : value;
}
```

**方法要点**:
- 将null转换为NULL对象
- 用于存储

### 8.4 unmaskNull - 揭示null

```java
@SuppressWarnings("unchecked")
private V unmaskNull(Object value) {
    return (V)(value == NULL ? null : value);
}
```

**方法要点**:
- 将NULL对象转换回null
- 用于返回

## 九、视图实现

### 9.1 keySet - 键集合视图

```java
public Set<K> keySet() {
    Set<K> ks = keySet;
    if (ks != null)
        return ks;
    else
        return keySet = new KeySet();
}

private class KeySet extends AbstractSet<K> {
    public Iterator<K> iterator() {
        return new KeyIterator();
    }
    public int size() {
        return size;
    }
    public boolean contains(Object o) {
        return containsKey(o);
    }
    public boolean remove(Object o) {
        int oldSize = size;
        EnumMap.this.remove(o);
        return size != oldSize;
    }
    public void clear() {
        EnumMap.this.clear();
    }
}
```

**方法要点**:
- 返回键的集合视图
- 按枚举常量的声明顺序迭代
- 视图修改会影响原映射

### 9.2 values - 值集合视图

```java
public Collection<V> values() {
    Collection<V> vs = values;
    if (vs != null)
        return vs;
    else
        return values = new Values();
}

private class Values extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return new ValueIterator();
    }
    public int size() {
        return size;
    }
    public boolean contains(Object o) {
        return containsValue(o);
    }
    public boolean remove(Object o) {
        o = maskNull(o);

        for (int i = 0; i < vals.length; i++) {
            if (o.equals(vals[i])) {
                vals[i] = null;
                size--;
                return true;
            }
        }
        return false;
    }
    public void clear() {
        EnumMap.this.clear();
    }
}
```

**方法要点**:
- 返回值的集合视图
- 按键的顺序迭代
- 视图修改会影响原映射

### 9.3 entrySet - 条目集合视图

```java
public Set<Map.Entry<K,V>> entrySet() {
    Set<Map.Entry<K,V>> es = entrySet;
    if (es != null)
        return es;
    else
        return entrySet = new EntrySet();
}

private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator();
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        return containsMapping(entry.getKey(), entry.getValue());
    }
    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        return removeMapping(entry.getKey(), entry.getValue());
    }
    public int size() {
        return size;
    }
    public void clear() {
        EnumMap.this.clear();
    }
    public Object[] toArray() {
        return fillEntryArray(new Object[size]);
    }
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            a = (T[])java.lang.reflect.Array
                    .newInstance(a.getClass().getComponentType(), size);
        if (a.length > size)
            a[size] = null;
        return (T[]) fillEntryArray(a);
    }
    private Object[] fillEntryArray(Object[] a) {
        int j = 0;
        for (int i = 0; i < vals.length; i++)
            if (vals[i] != null)
                a[j++] = new AbstractMap.SimpleEntry<>(
                    keyUniverse[i], unmaskNull(vals[i]));
        return a;
    }
}
```

**方法要点**:
- 返回条目的集合视图
- 按键的顺序迭代
- 视图修改会影响原映射

## 十、迭代器实现

### 10.1 EnumMapIterator - 抽象迭代器

```java
private abstract class EnumMapIterator<T> implements Iterator<T> {
    // Lower bound on index of next element to return
    int index = 0;

    // Index of last returned element, or -1 if none
    int lastReturnedIndex = -1;

    public boolean hasNext() {
        while (index < vals.length && vals[index] == null)
            index++;
        return index != vals.length;
    }

    public void remove() {
        checkLastReturnedIndex();

        if (vals[lastReturnedIndex] != null) {
            vals[lastReturnedIndex] = null;
            size--;
        }
        lastReturnedIndex = -1;
    }

    private void checkLastReturnedIndex() {
        if (lastReturnedIndex < 0)
            throw new IllegalStateException();
    }
}
```

**设计要点**:
- 跳过null值
- 支持remove操作
- Weakly Consistent

### 10.2 KeyIterator - 键迭代器

```java
private class KeyIterator extends EnumMapIterator<K> {
    public K next() {
        if (!hasNext())
            throw new NoSuchElementException();
        lastReturnedIndex = index++;
        return keyUniverse[lastReturnedIndex];
    }
}
```

### 10.3 ValueIterator - 值迭代器

```java
private class ValueIterator extends EnumMapIterator<V> {
    public V next() {
        if (!hasNext())
            throw new NoSuchElementException();
        lastReturnedIndex = index++;
        return unmaskNull(vals[lastReturnedIndex]);
    }
}
```

### 10.4 EntryIterator - 条目迭代器

```java
private class EntryIterator extends EnumMapIterator<Map.Entry<K,V>> {
    private Entry lastReturnedEntry;

    public Map.Entry<K,V> next() {
        if (!hasNext())
            throw new NoSuchElementException();
        lastReturnedEntry = new Entry(index++);
        return lastReturnedEntry;
    }

    public void remove() {
        lastReturnedIndex =
                ((null == lastReturnedEntry) ? -1 : lastReturnedEntry.index);
        super.remove();
        lastReturnedEntry.index = lastReturnedIndex;
        lastReturnedEntry = null;
    }

    private class Entry implements Map.Entry<K,V> {
        private int index;

        private Entry(int index) {
            this.index = index;
        }

        public K getKey() {
            checkIndexForEntryUse();
            return keyUniverse[index];
        }

        public V getValue() {
            checkIndexForEntryUse();
            return unmaskNull(vals[index]);
        }

        public V setValue(V value) {
            checkIndexForEntryUse();
            V oldValue = unmaskNull(vals[index]);
            vals[index] = maskNull(value);
            return oldValue;
        }

        public boolean equals(Object o) {
            if (index < 0)
                return o == this;

            if (!(o instanceof Map.Entry))
                return false;

            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            V ourValue = unmaskNull(vals[index]);
            Object hisValue = e.getValue();
            return (e.getKey() == keyUniverse[index] &&
                    (ourValue == hisValue ||
                     (ourValue != null && ourValue.equals(hisValue))));
        }

        public int hashCode() {
            if (index < 0)
                return super.hashCode();

            return entryHashCode(index);
        }

        public String toString() {
            if (index < 0)
                return super.toString();

            return keyUniverse[index] + "="
                    + unmaskNull(vals[index]);
        }

        private void checkIndexForEntryUse() {
            if (index < 0)
                throw new IllegalStateException("Entry was removed");
        }
    }
}
```

## 十一、比较和哈希

### 11.1 equals - 相等性比较

```java
public boolean equals(Object o) {
    if (this == o)
        return true;
    if (o instanceof EnumMap)
        return equals((EnumMap<?,?>)o);
    if (!(o instanceof Map))
        return false;

    Map<?,?> m = (Map<?,?>)o;
    if (size != m.size())
        return false;

    for (int i = 0; i < keyUniverse.length; i++) {
        if (null != vals[i]) {
            K key = keyUniverse[i];
            V value = unmaskNull(vals[i]);
            if (null == value) {
                if (!((null == m.get(key)) && m.containsKey(key)))
                       return false;
            } else {
               if (!value.equals(m.get(key))))
                      return false;
            }
        }
    }

    return true;
}

private boolean equals(EnumMap<?,?> em) {
    if (em.keyType != keyType)
        return size == 0 && em.size == 0;

    // Key types match, compare each value
    for (int i = 0; i < keyUniverse.length; i++) {
        Object ourValue =    vals[i];
        Object hisValue = em.vals[i];
        if (hisValue != ourValue &&
                (hisValue == null || !hisValue.equals(ourValue)))
            return false;
    }
    return true;
}
```

**方法要点**:
- 先比较类型
- 再比较大小
- 最后比较每个映射

### 11.2 hashCode - 哈希码

```java
public int hashCode() {
    int h = 0;

    for (int i = 0; i < keyUniverse.length; i++) {
        if (null != vals[i]) {
            h += entryHashCode(i);
        }
    }

    return h;
}

private int entryHashCode(int index) {
    return (keyUniverse[index].hashCode() ^ vals[index].hashCode());
}
```

**方法要点**:
- 计算所有条目的哈希码之和
- 使用异或运算

## 十二、克隆

### 12.1 clone - 克隆

```java
@SuppressWarnings("unchecked")
public EnumMap<K, V> clone() {
    EnumMap<K, V> result = null;
    try {
        result = (EnumMap<K, V>) super.clone();
    } catch(CloneNotSupportedException e) {
        throw new AssertionError();
    }
    result.vals = result.vals.clone();
    result.entrySet = null;
    return result;
}
```

**方法要点**:
- 浅拷贝
- 克隆值数组
- 重置entrySet

**示例**:
```java
EnumMap<Color, String> map1 = new EnumMap<>(Color.class);
map1.put(Color.RED, "红色");

EnumMap<Color, String> map2 = map1.clone();
```

## 十三、序列化

### 13.1 writeObject - 序列化

```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException
{
    // Write out key type and any hidden stuff
    s.defaultWriteObject();

    // Write out size (number of Mappings)
    s.writeInt(size);

    // Write out keys and values (alternating)
    int entriesToBeWritten = size;
    for (int i = 0; entriesToBeWritten > 0; i++) {
        if (null != vals[i]) {
            s.writeObject(keyUniverse[i]);
            s.writeObject(unmaskNull(vals[i]));
            entriesToBeWritten--;
        }
    }
}
```

**序列化格式**:
1. 默认字段
2. 大小(映射数量)
3. 键和值(交替)

### 13.2 readObject - 反序列化

```java
@SuppressWarnings("unchecked")
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException
{
    // Read in key type and any hidden stuff
    s.defaultReadObject();

    keyUniverse = getKeyUniverse(keyType);
    vals = new Object[keyUniverse.length];

    // Read in size (number of Mappings)
    int size = s.readInt();

    // Read in keys and values, and put mappings in HashMap
    for (int i = 0; i < size; i++) {
        K key = (K) s.readObject();
        V value = (V) s.readObject();
        put(key, value);
    }
}
```

## 十四、设计模式

### 14.1 视图模式

EnumMap提供键、值、条目的视图:

```java
// 键集合视图
public Set<K> keySet() {
    return new KeySet();
}

// 值集合视图
public Collection<V> values() {
    return new Values();
}

// 条目集合视图
public Set<Map.Entry<K,V>> entrySet() {
    return new EntrySet();
}
```

### 14.2 迭代器模式

提供多种迭代器:

```java
// 键迭代器
private class KeyIterator extends EnumMapIterator<K>

// 值迭代器
private class ValueIterator extends EnumMapIterator<V>

// 条目迭代器
private class EntryIterator extends EnumMapIterator<Map.Entry<K,V>>
```

## 十五、面试常见问题

### 15.1 EnumMap与HashMap的区别?

| 特性 | EnumMap | HashMap |
|------|----------|---------|
| 键类型 | 仅枚举 | 任意对象 |
| 内部表示 | 数组 | 哈希表 |
| 时间复杂度 | O(1) | O(1)平均 |
| 空间复杂度 | 极低 | 较高 |
| 迭代顺序 | 枚举声明顺序 | 无序 |
| 类型安全 | 编译时 | 运行时 |
| null键 | 不允许 | 允许 |
| null值 | 允许 | 允许 |

### 15.2 EnumMap为什么性能高?

1. **数组表示**: 使用数组存储,直接索引访问
2. **类型安全**: 编译时检查,避免运行时类型转换
3. **紧凑存储**: 每个枚举常量只需一个数组元素
4. **缓存优化**: 缓存枚举常量数组
5. **O(1)操作**: 所有基本操作都是O(1)

### 15.3 EnumMap支持null键吗?

不支持。尝试添加null键会抛出NullPointerException:

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(null, "value");  // NullPointerException
```

### 15.4 EnumMap支持null值吗?

支持。使用NULL对象表示null值:

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, null);  // OK
```

### 15.5 EnumMap是线程安全的吗?

不是。需要外部同步:

```java
Map<Color, String> syncMap = Collections.synchronizedMap(new EnumMap<>(Color.class));
```

### 15.6 EnumMap的迭代器是fail-fast的吗?

不是。迭代器是weakly consistent的,不会抛出ConcurrentModificationException:

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
map.put(Color.GREEN, "绿色");

Iterator<Color> it = map.keySet().iterator();
map.put(Color.BLUE, "蓝色");  // 不会抛出异常
while (it.hasNext()) {
    System.out.println(it.next());  // 可能或可能不显示BLUE
}
```

### 15.7 EnumMap如何存储键值对?

使用数组存储:

```java
enum Color {
    RED,    // index 0
    GREEN,  // index 1
    BLUE    // index 2
}

EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
map.put(Color.BLUE, "蓝色");

// 内部表示:
// vals[0] -> "红色"
// vals[1] -> null
// vals[2] -> "蓝色"
```

### 15.8 EnumMap的基本操作时间复杂度?

- put: O(1)
- get: O(1)
- remove: O(1)
- containsKey: O(1)
- containsValue: O(n)
- size: O(1)
- isEmpty: O(1)

## 十六、使用场景

### 16.1 枚举作为键的映射

```java
enum Color {
    RED, GREEN, BLUE
}

EnumMap<Color, String> colorMap = new EnumMap<>(Color.class);
colorMap.put(Color.RED, "红色");
colorMap.put(Color.GREEN, "绿色");
colorMap.put(Color.BLUE, "蓝色");
```

### 16.2 状态机

```java
enum State {
    STARTED, RUNNING, PAUSED, STOPPED, ERROR
}

EnumMap<State, String> stateMap = new EnumMap<>(State.class);
stateMap.put(State.STARTED, "已启动");
stateMap.put(State.RUNNING, "运行中");
stateMap.put(State.PAUSED, "已暂停");
stateMap.put(State.STOPPED, "已停止");
stateMap.put(State.ERROR, "错误");
```

### 16.3 配置管理

```java
enum ConfigKey {
    DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD,
    CACHE_SIZE, TIMEOUT
}

EnumMap<ConfigKey, String> config = new EnumMap<>(ConfigKey.class);
config.put(ConfigKey.DATABASE_URL, "jdbc:mysql://localhost:3306/mydb");
config.put(ConfigKey.CACHE_SIZE, "1024");
```

### 16.4 权限管理

```java
enum Permission {
    READ, WRITE, EXECUTE, DELETE, ADMIN
}

EnumMap<Permission, Boolean> permissions = new EnumMap<>(Permission.class);
permissions.put(Permission.READ, true);
permissions.put(Permission.WRITE, true);
permissions.put(Permission.EXECUTE, false);
```

### 16.5 颜色管理

```java
enum Color {
    RED, GREEN, BLUE, YELLOW, BLACK, WHITE
}

EnumMap<Color, String> colorNames = new EnumMap<>(Color.class);
colorNames.put(Color.RED, "红色");
colorNames.put(Color.GREEN, "绿色");
colorNames.put(Color.BLUE, "蓝色");
```

## 十七、注意事项

### 17.1 类型检查

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");  // OK
map.put(Day.MONDAY, "星期一");  // 编译错误
```

### 17.2 null键

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(null, "value");  // NullPointerException
```

### 17.3 null值

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, null);  // OK
```

### 17.4 迭代顺序

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.BLUE, "蓝色");
map.put(Color.RED, "红色");

// 迭代顺序: RED, BLUE (按声明顺序)
```

### 17.5 线程安全

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);

// 需要外部同步
Map<Color, String> syncMap = Collections.synchronizedMap(map);
```

## 十八、最佳实践

### 18.1 使用EnumMap替代HashMap

```java
// 不推荐: 使用HashMap
Map<Color, String> map = new HashMap<>();
map.put(Color.RED, "红色");

// 推荐: 使用EnumMap
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
```

### 18.2 使用枚举作为键

```java
enum Color {
    RED, GREEN, BLUE
}

EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");
```

### 18.3 使用视图操作

```java
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");

// 推荐: 使用keySet视图
for (Color color : map.keySet()) {
    System.out.println(color + ": " + map.get(color));
}
```

### 18.4 使用批量操作

```java
EnumMap<Color, String> map1 = new EnumMap<>(Color.class);
map1.put(Color.RED, "红色");
map1.put(Color.GREEN, "绿色");

EnumMap<Color, String> map2 = new EnumMap<>(Color.class);

// 推荐: 使用putAll
map2.putAll(map1);

// 不推荐: 逐个添加
for (Map.Entry<Color, String> entry : map1.entrySet()) {
    map2.put(entry.getKey(), entry.getValue());
}
```

### 18.5 使用克隆

```java
EnumMap<Color, String> map1 = new EnumMap<>(Color.class);
map1.put(Color.RED, "红色");

// 推荐: 使用clone
EnumMap<Color, String> map2 = map1.clone();

// 不推荐: 手动复制
EnumMap<Color, String> map2 = new EnumMap<>(Color.class);
for (Map.Entry<Color, String> entry : map1.entrySet()) {
    map2.put(entry.getKey(), entry.getValue());
}
```

## 十九、性能优化

### 19.1 使用数组索引

```java
// EnumMap内部使用数组索引,性能极高
EnumMap<Color, String> map = new EnumMap<>(Color.class);
map.put(Color.RED, "红色");  // O(1)
map.get(Color.RED);          // O(1)
```

### 19.2 缓存枚举常量

```java
// EnumMap缓存枚举常量数组,避免重复创建
private transient K[] keyUniverse;
```

### 19.3 使用NULL对象

```java
// EnumMap使用NULL对象表示null值,避免歧义
private static final Object NULL = new Object();
```

## 二十、总结

EnumMap是专门用于枚举类型键的Map实现,使用数组存储,性能极高。

### 核心要点

1. **枚举专用**: 键必须是枚举类型
2. **数组表示**: 使用数组存储键值对
3. **高性能**: 所有基本操作都是O(1)时间复杂度
4. **类型安全**: 编译时类型检查
5. **有序性**: 按枚举常量的声明顺序迭代
6. **支持null值**: 允许null值,但不允许null键
7. **Weakly Consistent**: 迭代器不会抛出ConcurrentModificationException

### 适用场景

- 枚举类型作为键的映射
- 需要高性能的枚举映射
- 需要类型安全的枚举操作
- 需要保持键的顺序

### 不适用场景

- 非枚举类型的键
- 需要null键
- 多线程环境(需外部同步)

### 性能特点

- 基本操作: O(1)
- containsValue: O(n)
- 空间复杂度: 极低(每个枚举常量1个数组元素)

### 与HashMap的选择

- 枚举类型键: EnumMap
- 非枚举类型键: HashMap
- 需要类型安全: EnumMap
- 需要null键: HashMap
- 需要保持顺序: EnumMap
