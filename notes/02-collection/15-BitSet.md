# BitSet 源码解读

## 一、类概述

BitSet是Java集合框架中的一个类,它实现了一个按需增长的位向量。BitSet的每个组件都有一个boolean值。BitSet的位由非负整数索引。可以检查、设置或清除单个索引位。BitSet可以通过逻辑AND、逻辑OR和逻辑XOR操作修改另一个BitSet的内容。

### 1.1 核心特性

- **位向量**: 使用long数组存储位,每个long存储64位
- **动态增长**: 按需增长容量
- **位操作**: 支持set、clear、flip等位操作
- **集合操作**: 支持AND、OR、XOR等集合操作
- **非线程安全**: 不是线程安全的
- **高效存储**: 比boolean数组更节省空间

### 1.2 适用场景

- 标记集合
- 布尔运算
- 集合操作
- 空间优化
- 位图表示

### 1.3 与boolean[]的区别

| 特性 | BitSet | boolean[] |
|------|--------|-----------|
| 存储效率 | 高(每个long存储64位) | 低(每个boolean占1字节) |
| 位操作 | 支持 | 不支持 |
| 集合操作 | 支持 | 不支持 |
| 内存占用 | 较低 | 较高 |
| 动态增长 | 支持 | 不支持 |

## 二、设计原理

### 2.1 位存储结构

BitSet使用long数组存储位:
```
words数组:
words[0] -> bits 0-63
words[1] -> bits 64-127
words[2] -> bits 128-191
...
```

### 2.2 位索引计算

```java
// 计算位所在的word索引
private static int wordIndex(int bitIndex) {
    return bitIndex >> ADDRESS_BITS_PER_WORD;  // bitIndex / 64
}

// 计算位在word中的位置
// bitIndex % 64
```

### 2.3 位操作

- **set**: 设置指定位为true
- **clear**: 设置指定位为false
- **flip**: 翻转指定位的值
- **get**: 获取指定位的值

## 三、继承结构

```
Cloneable
    ↑
BitSet
    implements Serializable
```

### 3.1 实现的接口

- Cloneable: 可克隆
- Serializable: 可序列化

## 四、核心字段分析

### 4.1 常量字段

```java
// 每个word的地址位数
private final static int ADDRESS_BITS_PER_WORD = 6;

// 每个word的位数
private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;  // 64

// 位索引掩码
private final static int BIT_INDEX_MASK = BITS_PER_WORD - 1;  // 63

// 用于部分word掩码的左移或右移
private final static long WORD_MASK = 0xffffffffffffffffL;
```

### 4.2 实例字段

```java
// 位集合的位,存储在bits[i/64]的bit位置i
private long[] words;

// 位集合的逻辑大小中的word数
private transient int wordsInUse = 0;

// "words"的大小是否由用户指定
private transient boolean sizeIsSticky = false;
```

### 4.3 字段设计要点

1. **words数组**: 存储位,每个long存储64位
2. **wordsInUse**: 实际使用的word数量
3. **sizeIsSticky**: 标记是否由用户指定大小
4. **transient**: wordsInUse和sizeIsSticky不序列化

## 五、核心方法解析

### 5.1 构造方法

```java
/**
 * 创建一个新的位集合。所有位初始为{@code false}。
 */
public BitSet() {
    initWords(BITS_PER_WORD);  // 初始化为1个word
    sizeIsSticky = false;
}

/**
 * 创建一个位集合,其初始大小足以显式表示
 * 范围{@code 0}到{@code nbits-1}中的位。所有位初始为{@code false}。
 *
 * @param nbits 位集合的初始大小
 * @throws NegativeArraySizeException 如果指定的初始大小为负
 */
public BitSet(int nbits) {
    // nbits不能为负;大小0是可以的
    if (nbits < 0)
        throw new NegativeArraySizeException("nbits < 0: " + nbits);

    initWords(nbits);
    sizeIsSticky = true;
}

private void initWords(int nbits) {
    words = new long[wordIndex(nbits-1) + 1];
}
```

### 5.2 valueOf - 从long数组创建

```java
/**
 * 返回包含给定long数组中所有位的新位集合。
 *
 * <p>更精确地说,
 * <br>{@code BitSet.valueOf(longs).get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
 * <br>对于所有{@code n < 64 * longs.length}。
 *
 * <p>此方法等效于
 * <br>{@code BitSet.valueOf(LongBuffer.wrap(longs))}。
 *
 * @param longs 包含小端表示的long数组
 *        要用作新位集合的初始位的序列
 * @return 包含long数组中所有位的{@code BitSet}
 * @since 1.7
 */
public static BitSet valueOf(long[] longs) {
    int n;
    for (n = longs.length; n > 0 && longs[n - 1] == 0; n--)
        ;
    return new BitSet(Arrays.copyOf(longs, n));
}
```

**方法要点**:
- 移除末尾的零word
- 复制剩余的words
- 创建新的BitSet

### 5.3 valueOf - 从ByteBuffer创建

```java
/**
 * 返回包含给定long缓冲区位置和限制之间所有位的新位集合。
 *
 * <p>更精确地说,
 * <br>{@code BitSet.valueOf(lb).get(n) == ((lb.get(lb.position()+n/64) & (1L<<(n%64))) != 0)}
 * <br>对于所有{@code n < 64 * lb.remaining()}。
 *
 * <p>long缓冲区不被此方法修改,并且不保留对缓冲区的引用。
 *
 * @param lb 包含小端表示的long缓冲区
 *        要用作新位集合的初始位的序列,在其位置和限制之间
 * @return 包含缓冲区指定范围内所有位的{@code BitSet}
 * @since 1.7
 */
public static BitSet valueOf(LongBuffer lb) {
    lb = lb.slice();
    int n;
    for (n = lb.remaining(); n > 0 && lb.get(n - 1) == 0; n--)
        ;
    long[] words = new long[n];
    lb.get(words);
    return new BitSet(words);
}
```

**方法要点**:
- 创建缓冲区的切片
- 计算需要的word数量
- 从缓冲区读取words
- 创建新的BitSet

### 5.4 valueOf - 从byte数组创建

```java
/**
 * 返回包含给定字节数组中所有位的新位集合。
 *
 * <p>更精确地说,
 * <br>{@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
 * <br>对于所有{@code n < 8 * bytes.length}。
 *
 * <p>此方法等效于
 * <br>{@code BitSet.valueOf(ByteBuffer.wrap(bytes))}。
 *
 * @param bytes 包含小端表示的字节数组
 *        要用作新位集合的初始位的序列
 * @return 包含字节数组中所有位的{@code BitSet}
 * @since 1.7
 */
public static BitSet valueOf(byte[] bytes) {
    return BitSet.valueOf(ByteBuffer.wrap(bytes));
}
```

### 5.5 set - 设置位

```java
/**
 * 将指定索引处的位设置为{@code true}。
 *
 * @param bitIndex 位索引
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 * @since JDK1.0
 */
public void set(int bitIndex) {
    if (bitIndex < 0)
        throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
    int wordIndex = wordIndex(bitIndex);
    expandTo(wordIndex);
    words[wordIndex] |= (1L << bitIndex); // 保持不变式
    checkInvariants();
}
```

**方法要点**:
- 检查索引是否为负
- 计算word索引
- 扩容如果需要
- 使用OR操作设置位

### 5.6 get - 获取位

```java
/**
 * 返回具有指定索引的位的值。
 * 如果具有索引{@code bitIndex}的位当前设置在此{@code BitSet}中,
 * 则结果为{@code true};否则,结果为{@code false}。
 *
 * @param bitIndex 位索引
 * @return 具有指定索引的位的值
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 */
public boolean get(int bitIndex) {
    if (bitIndex < 0)
        throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
    checkInvariants();
    int wordIndex = wordIndex(bitIndex);
    return (wordIndex < wordsInUse)
            && ((words[wordIndex] & (1L << bitIndex)) != 0);
}
```

**方法要点**:
- 检查索引是否为负
- 计算word索引
- 检查word是否在使用中
- 使用AND操作获取位

### 5.7 clear - 清除位

```java
/**
 * 将指定索引处的位设置为{@code false}。
 *
 * @param bitIndex 要清除的位的索引
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 * @since JDK1.0
 */
public void clear(int bitIndex) {
    if (bitIndex < 0)
        throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
    int wordIndex = wordIndex(bitIndex);
    if (wordIndex >= wordsInUse)
        return;
    words[wordIndex] &= ~(1L << bitIndex);
    recalculateWordsInUse();
    checkInvariants();
}
```

**方法要点**:
- 检查索引是否为负
- 计算word索引
- 使用NOT和AND操作清除位
- 重新计算wordsInUse

### 5.8 flip - 翻转位

```java
/**
 * 将指定索引处的位设置为其补码。
 *
 * @param bitIndex 要翻转的位的索引
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 * @since 1.4
 */
public void flip(int bitIndex) {
    if (bitIndex < 0)
        throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
    int wordIndex = wordIndex(bitIndex);
    expandTo(wordIndex);
    words[wordIndex] ^= (1L << bitIndex);
    recalculateWordsInUse();
    checkInvariants();
}
```

**方法要点**:
- 检查索引是否为负
- 计算word索引
- 使用XOR操作翻转位
- 重新计算wordsInUse

### 5.9 set - 设置范围

```java
/**
 * 将指定{@code fromIndex}(包含)到指定{@code toIndex}(不包含)的位设置为{@code true}。
 *
 * @param fromIndex 要设置的第一个位的索引
 * @param toIndex 最后一个位之后的索引
 * @throws IndexOutOfBoundsException 如果{@code fromIndex}为负,
 *         或{@code toIndex}为负,或{@code fromIndex}大于{@code toIndex}
 * @since 1.4
 */
public void set(int fromIndex, int toIndex) {
    checkRange(fromIndex, toIndex);
    if (fromIndex == toIndex)
        return;

    // 如果需要,增加容量
    int startWordIndex = wordIndex(fromIndex);
    int endWordIndex   = wordIndex(toIndex - 1);
    expandTo(endWordIndex);

    long firstWordMask = WORD_MASK << fromIndex;
    long lastWordMask  = WORD_MASK >>> -toIndex;
    if (startWordIndex == endWordIndex) {
        // 情况1: 一个word
        words[startWordIndex] |= (firstWordMask & lastWordMask);
    } else {
        // 情况2: 多个words
        // 处理第一个word
        words[startWordIndex] |= firstWordMask;
        // 处理中间words,如果有的话
        for (int i = startWordIndex+1; i < endWordIndex; i++)
            words[i] = WORD_MASK;
        // 处理最后一个word(保持不变式)
        words[endWordIndex] |= lastWordMask;
    }
    checkInvariants();
}
```

**方法要点**:
- 检查索引范围
- 扩容如果需要
- 处理一个word和多个words的情况
- 使用掩码设置位

### 5.10 clear - 清除范围

```java
/**
 * 将指定{@code fromIndex}(包含)到指定{@code toIndex}(不包含)的位设置为{@code false}。
 *
 * @param fromIndex 要清除的第一个位的索引
 * @param toIndex 最后一个位之后的索引
 * @throws IndexOutOfBoundsException 如果{@code fromIndex}为负,
 *         或{@code toIndex}为负,或{@code fromIndex}大于{@code toIndex}
 * @since 1.4
 */
public void clear(int fromIndex, int toIndex) {
    checkRange(fromIndex, toIndex);
    if (fromIndex == toIndex)
        return;

    int startWordIndex = wordIndex(fromIndex);
    if (startWordIndex >= wordsInUse)
        return;
    int endWordIndex = wordIndex(toIndex - 1);
    if (endWordIndex >= wordsInUse) {
        toIndex = length();
        endWordIndex = wordsInUse - 1;
    }

    long firstWordMask = WORD_MASK << fromIndex;
    long lastWordMask  = WORD_MASK >>> -toIndex;
    if (startWordIndex == endWordIndex) {
        // 情况1: 一个word
        words[startWordIndex] &= ~(firstWordMask & lastWordMask);
    } else {
        // 情况2: 多个words
        // 处理第一个word
        words[startWordIndex] &= ~firstWordMask;
        // 处理中间words,如果有的话
        for (int i = startWordIndex+1; i < endWordIndex; i++)
            words[i] = 0;
        // 处理最后一个word(保持不变式)
        words[endWordIndex] &= ~lastWordMask;
    }
    recalculateWordsInUse();
    checkInvariants();
}
```

**方法要点**:
- 检查索引范围
- 处理一个word和多个words的情况
- 使用掩码清除位
- 重新计算wordsInUse

### 5.11 length - 获取逻辑大小

```java
/**
 * 返回此{@code BitSet}的"逻辑大小":最高设置位的索引加一。
 * 如果{@code BitSet}不包含任何设置位,则返回零。
 *
 * @return 此{@code BitSet}的逻辑大小
 * @since 1.2
 */
public int length() {
    if (wordsInUse == 0)
        return 0;
    return BITS_PER_WORD * (wordsInUse - 1) +
           (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
}
```

**方法要点**:
- 如果没有使用words,返回0
- 计算最后一个word中设置的最高位
- 返回逻辑大小(不是wordsInUse * 64)

### 5.12 isEmpty - 检查是否为空

```java
/**
 * 如果此{@code BitSet}不包含任何设置为{@code true}的位,则返回{@code true}。
 *
 * @return boolean 指示此{@code BitSet}是否为空
 * @since 1.4
 */
public boolean isEmpty() {
    return wordsInUse == 0;
}
```

### 5.13 cardinality - 获取设置位的数量

```java
/**
 * 返回此{@code BitSet}中设置为{@code true}的位的数量。
 *
 * @return 此{@code BitSet}中设置为{@code true}的位的数量
 * @since 1.4
 */
public int cardinality() {
    int sum = 0;
    for (int i = 0; i < wordsInUse; i++)
        sum += Long.bitCount(words[i]);
    return sum;
}
```

**方法要点**:
- 遍历所有使用的words
- 使用Long.bitCount()计算每个word中设置的位数
- 返回总位数

### 5.14 intersects - 检查交集

```java
/**
 * 如果此{@code BitSet}与指定的位集合参数具有任何设置为{@code true}的位,
 * 则返回{@code true},这些位在此{@code BitSet}中也设置为{@code true}。
 *
 * @param set 要与之相交的位集合
 * @return boolean 指示此{@code BitSet}是否与指定的{@code BitSet}
 *         相交
 * @since 1.4
 */
public boolean intersects(BitSet set) {
    for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--)
        if ((words[i] & set.words[i]) != 0)
            return true;
    return false;
}
```

**方法要点**:
- 遍历共同的words
- 使用AND操作检查交集
- 如果有任何交集,返回true

### 5.15 and - 逻辑AND

```java
/**
 * 对此目标位集合与参数位集合执行逻辑<b>AND</b>操作。
 * 此位集合被修改,以便其中的每个位仅在以下情况下具有值{@code true}:
 * <ul>
 * <li>它最初具有值{@code true},并且</li>
 * <li>参数位集合中对应的位也具有值{@code true}。</li>
 * </ul>
 *
 * @param set 位集合
 */
public void and(BitSet set) {
    if (this == set)
        return;
    while (wordsInUse > set.wordsInUse)
        words[--wordsInUse] = 0;
    // 对公共words执行逻辑AND
    for (int i = 0; i < wordsInUse; i++)
        words[i] &= set.words[i];
    recalculateWordsInUse();
    checkInvariants();
}
```

**方法要点**:
- 如果是同一个对象,直接返回
- 调整wordsInUse到较小的值
- 对公共words执行AND操作
- 重新计算wordsInUse

### 5.16 or - 逻辑OR

```java
/**
 * 对此位集合与参数位集合执行逻辑<b>OR</b>操作。
 * 此位集合被修改,以便其中的每个位在以下情况下具有值{@code true}:
 * <ul>
 * <li>它最初具有值{@code true},或者</li>
 * <li>参数位集合中对应的位具有值{@code true}。</li>
 * </ul>
 *
 * @param set 位集合
 */
public void or(BitSet set) {
    if (this == set)
        return;
    int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
    if (wordsInUse < set.wordsInUse) {
        ensureCapacity(set.wordsInUse);
        wordsInUse = set.wordsInUse;
    }
    // 对公共words执行逻辑OR
    for (int i = 0; i < wordsInCommon; i++)
        words[i] |= set.words[i];
    // 复制任何剩余words
    if (wordsInCommon < set.wordsInUse)
        System.arraycopy(set.words, wordsInCommon,
                             words, wordsInCommon,
                             wordsInUse - wordsInCommon);
    // recalculateWordsInUse()是不必要的
    checkInvariants();
}
```

**方法要点**:
- 如果是同一个对象,直接返回
- 确保容量足够
- 对公共words执行OR操作
- 复制剩余words

### 5.17 xor - 逻辑XOR

```java
/**
 * 对此位集合与参数位集合执行逻辑<b>XOR</b>操作。
 * 此位集合被修改,以便其中的每个位在以下情况下具有值{@code true}:
 * <ul>
 * <li>该位最初具有值{@code true},并且</li>
 * <li>参数位集合中对应的位也具有值{@code true}。</li>
 * <li>该位最初具有值{@code false},并且</li>
 * <li>参数位集合中对应的位具有值{@code false}。</li>
 * </ul>
 *
 * @param set 位集合
 */
public void xor(BitSet set) {
    int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
    // 对公共words执行逻辑XOR
    for (int i = 0; i < wordsInCommon; i++)
        words[i] ^= set.words[i];
    // 复制任何剩余words
    if (wordsInCommon < set.wordsInUse)
        System.arraycopy(set.words, wordsInCommon,
                             words, wordsInCommon,
                             wordsInUse - wordsInCommon);
    recalculateWordsInUse();
    checkInvariants();
}
```

**方法要点**:
- 对公共words执行XOR操作
- 复制剩余words
- 重新计算wordsInUse

### 5.18 nextSetBit - 查找下一个设置位

```java
/**
 * 返回在或等于指定起始索引之后发生的第一个设置为{@code true}的位的索引。
 * 如果不存在这样的位,则返回{@code -1}。
 *
 * <p>要迭代{@code BitSet}中的{@code true}位,请使用以下循环:
 *
 * <pre>{@code
 * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
 *     // 在此处对索引i进行操作
 *     if (i == Integer.MAX_VALUE) {
 *         break; // 或者(i+1)会溢出
 *     }
 * }</pre>
 *
 * @param fromIndex 要开始检查的索引(包含)
 * @return 在或等于指定起始索引之后发生的第一个设置位的索引,
 *         如果没有这样的位则返回{@code -1}
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 * @since 1.4
 */
public int nextSetBit(int fromIndex) {
    if (fromIndex < 0)
        throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    checkInvariants();
    int u = wordIndex(fromIndex);
    if (u >= wordsInUse)
        return -1;
    long word = words[u] & (WORD_MASK << fromIndex);
    while (true) {
        if (word != 0)
            return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
        if (++u == wordsInUse)
            return -1;
        word = words[u];
    }
}
```

**方法要点**:
- 检查索引是否为负
- 遍历words查找下一个设置位
- 使用Long.numberOfTrailingZeros()计算尾随零

### 5.19 nextClearBit - 查找下一个清除位

```java
/**
 * 返回在或等于指定起始索引之后发生的第一个设置为{@code false}的位的索引。
 * 如果不存在这样的位,则返回{@code -1}。
 *
 * @param fromIndex 要开始检查的索引(包含)
 * @return 在或等于指定起始索引之后发生的第一个清除位的索引,
 *         如果没有这样的位则返回{@code -1}
 * @throws IndexOutOfBoundsException 如果指定的索引为负
 * @since 1.4
 */
public int nextClearBit(int fromIndex) {
    // 规范和实现都不处理最大长度的位集
    // 参见4816253
    if (fromIndex < 0) {
        if (fromIndex == -1)
            return -1;
        throw new IndexOutOfBoundsException(
                "fromIndex < -1: " + fromIndex);
    }
    checkInvariants();
    int u = wordIndex(fromIndex);
    if (u >= wordsInUse)
        return fromIndex;
    long word = ~words[u] & (WORD_MASK >>> -(fromIndex+1));
    while (true) {
        if (word != 0)
            return (u+1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
        if (++u == wordsInUse)
            return wordsInUse * BITS_PER_WORD;
        word = ~words[u];
    }
}
```

**方法要点**:
- 检查索引是否为负
- 遍历words查找下一个清除位
- 使用Long.numberOfLeadingZeros()计算前导零

### 5.20 toByteArray - 转换为字节数组

```java
/**
 * 返回包含此位集中所有位的新字节数组。
 *
 * <p>更精确地说,如果
 * <br>{@code byte[] bytes = s.toByteArray();}
 * <br>那么{@code bytes.length == (s.length()+7)/8}和
 * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
 * <br>对于所有{@code n < 8 * bytes.length}。
 *
 * @return 包含小端表示的所有位的字节数组
 * @since 1.7
 */
public byte[] toByteArray() {
    int n = wordsInUse;
    if (n == 0)
        return new byte[0];
    int len = 8 * (n-1);
    for (long x = words[n - 1]; x != 0; x >>>= 8)
        len++;
    byte[] bytes = new byte[len];
    ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < n - 1; i++)
        bb.putLong(words[i]);
    for (long x = words[n - 1]; x != 0; x >>>= 8)
        bb.put((byte) (x & 0xff));
    return bytes;
}
```

**方法要点**:
- 计算需要的字节数量
- 使用小端序
- 先写入完整的long,再写入剩余的bytes

### 5.21 toLongArray - 转换为long数组

```java
/**
 * 返回包含此位集中所有位的新long数组。
 *
 * <p>更精确地说,如果
 * <br>{@code long[] longs = s.toLongArray();}
 * <br>那么{@code longs.length == (s.length()+63)/64}和
 * <br>{@code s.get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
 * <br>对于所有{@code n < 64 * longs.length}。
 *
 * @return 包含小端表示的所有位的long数组
 * @since 1.7
 */
public long[] toLongArray() {
    return Arrays.copyOf(words, wordsInUse);
}
```

**方法要点**:
- 复制实际使用的words
- 不包括未使用的words

### 5.22 clone - 克隆

```java
public Object clone() {
    try {
        BitSet result = (BitSet) super.clone();
        result.words = words.clone();
        result.checkInvariants();
        return result;
    } catch (CloneNotSupportedException e) {
        throw new InternalError(e);
    }
}
```

**方法要点**:
- 调用super.clone()
- 克隆words数组
- 检查不变式

## 六、设计模式

### 6.1 原型模式

BitSet实现Cloneable接口,支持浅拷贝:
```java
public Object clone() {
    BitSet result = (BitSet) super.clone();
    result.words = words.clone();
    return result;
}
```

### 6.2 策略模式

BitSet使用位操作实现集合操作:
- **AND**: 交集
- **OR**: 并集
- **XOR**: 对称差

### 6.3 适配器模式

BitSet将long数组适配为位集合:
- **long数组**: 存储位
- **BitSet**: 位集合接口

## 七、面试常见问题

### 7.1 BitSet与boolean[]的区别?

| 特性 | BitSet | boolean[] |
|------|--------|-----------|
| 存储效率 | 高(每个long存储64位) | 低(每个boolean占1字节) |
| 位操作 | 支持 | 不支持 |
| 集合操作 | 支持 | 不支持 |
| 内存占用 | 较低 | 较高 |
| 动态增长 | 支持 | 不支持 |

### 7.2 BitSet是线程安全的吗?

不是。BitSet不是线程安全的,需要外部同步。

### 7.3 BitSet如何存储位?

使用long数组存储位,每个long存储64位:
```
words[0] -> bits 0-63
words[1] -> bits 64-127
...
```

### 7.4 BitSet的扩容机制?

当需要访问更大的word索引时扩容:
- 新容量 = 旧容量的2倍或所需容量
- 复制旧words到新数组
- 扩容后重新计算wordsInUse

### 7.5 BitSet支持哪些位操作?

- **set**: 设置位为true
- **clear**: 设置位为false
- **flip**: 翻转位
- **get**: 获取位的值

### 7.6 BitSet支持哪些集合操作?

- **intersects**: 检查交集
- **and**: 逻辑AND(交集)
- **or**: 逻辑OR(并集)
- **xor**: 逻辑XOR(对称差)

### 7.7 BitSet的length()方法返回什么?

返回逻辑大小(最高设置位的索引+1),不是wordsInUse * 64:
```java
public int length() {
    if (wordsInUse == 0)
        return 0;
    return BITS_PER_WORD * (wordsInUse - 1) +
           (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
}
```

### 7.8 BitSet的cardinality()方法返回什么?

返回设置为true的位的数量:
```java
public int cardinality() {
    int sum = 0;
    for (int i = 0; i < wordsInUse; i++)
        sum += Long.bitCount(words[i]);
    return sum;
}
```

### 7.9 BitSet的nextSetBit方法如何工作?

从指定索引开始查找下一个设置位:
```java
public int nextSetBit(int fromIndex) {
    int u = wordIndex(fromIndex);
    long word = words[u] & (WORD_MASK << fromIndex);
    while (true) {
        if (word != 0)
            return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
        word = words[++u];
    }
}
```

### 7.10 BitSet适合什么场景?

- 标记集合
- 布尔运算
- 集合操作
- 空间优化
- 位图表示

## 八、使用场景

### 8.1 标记集合

```java
// 创建BitSet
BitSet flags = new BitSet();

// 设置标记
flags.set(0);  // 标记0
flags.set(1);  // 标记1
flags.set(2);  // 标记2

// 检查标记
if (flags.get(0)) {
    System.out.println("标记0已设置");
}
```

### 8.2 布尔运算

```java
// 创建两个BitSet
BitSet set1 = new BitSet();
BitSet set2 = new BitSet();

set1.set(0);
set1.set(2);
set1.set(4);

set2.set(1);
set2.set(2);

// 交集
BitSet intersection = set1.clone();
intersection.and(set2);

// 并集
BitSet union = set1.clone();
union.or(set2);

// 对称差
BitSet xor = set1.clone();
xor.xor(set2);
```

### 8.3 空间优化

```java
// 使用BitSet代替boolean数组
// boolean[] flags = new boolean[1000];
BitSet flags = new BitSet(1000);

// BitSet更节省空间
```

### 8.4 位图表示

```java
// 使用BitSet表示图的邻接矩阵
BitSet adjacencyMatrix = new BitSet(n * n);

// 设置边
adjacencyMatrix.set(i * n + j);
adjacencyMatrix.set(j * n + i);

// 检查边
if (adjacencyMatrix.get(i * n + j)) {
    System.out.println("节点i和节点j之间有边");
}
```

## 九、注意事项

### 9.1 线程安全

```java
// BitSet不是线程安全的
BitSet bitSet = new BitSet();

// 多线程环境下需要外部同步
synchronized (bitSet) {
    bitSet.set(0);
}
```

### 9.2 内存效率

```java
// BitSet比boolean[]更节省空间
BitSet bitSet = new BitSet(1000);  // 约100字节
boolean[] array = new boolean[1000];  // 约1000字节
```

### 9.3 位索引范围

```java
// 位索引必须为非负
BitSet bitSet = new BitSet();
bitSet.set(0);  // 正确
// bitSet.set(-1);  // IndexOutOfBoundsException
```

### 9.4 集合操作

```java
// 集合操作会修改原BitSet
BitSet set1 = new BitSet();
BitSet set2 = new BitSet();

set1.set(0);
set1.set(1);

// set1被修改
set1.and(set2);
```

## 十、最佳实践

### 10.1 选择合适的数据结构

```java
// 需要位操作: BitSet
BitSet bitSet = new BitSet();

// 需要boolean数组: boolean[]
boolean[] flags = new boolean[10];

// 需要集合操作: Set<Integer>
Set<Integer> set = new HashSet<>();
```

### 10.2 使用位操作

```java
BitSet bitSet = new BitSet();

// 设置位
bitSet.set(0);
bitSet.set(1);
bitSet.set(2);

// 翻转位
bitSet.flip(0);

// 清除位
bitSet.clear(0);
```

### 10.3 使用集合操作

```java
BitSet set1 = new BitSet();
BitSet set2 = new BitSet();

set1.set(0);
set1.set(2);

// 交集
BitSet intersection = set1.clone();
intersection.and(set2);

// 并集
BitSet union = set1.clone();
union.or(set2);
```

### 10.4 线程安全

```java
BitSet bitSet = new BitSet();

// 多线程环境下需要外部同步
synchronized (bitSet) {
    bitSet.set(0);
}
```

### 10.5 性能优化

```java
// 使用BitSet代替boolean数组节省空间
BitSet bitSet = new BitSet(1000);

// 使用valueOf方法从数组创建
byte[] bytes = new byte[16];
BitSet bitSet = BitSet.valueOf(bytes);
```

## 十一、总结

BitSet是一个高效的位集合实现,使用long数组存储位,支持位操作和集合操作。BitSet比boolean[]更节省空间,适合需要位操作和集合操作的场景。

### 核心要点

1. **位向量**: 使用long数组存储位,每个long存储64位
2. **动态增长**: 按需增长容量
3. **位操作**: 支持set、clear、flip等位操作
4. **集合操作**: 支持AND、OR、XOR等集合操作
5. **高效存储**: 比boolean数组更节省空间
6. **非线程安全**: 不是线程安全的,需要外部同步
7. **不变式**: 维护wordsInUse和sizeIsSticky不变式

### 适用场景

- 标记集合
- 布尔运算
- 集合操作
- 空间优化
- 位图表示

### 不适用场景

- 需要线程安全(需外部同步)
- 需要对象存储
- 需要保持顺序

### 替代方案

- **位操作**: BitSet
- **boolean数组**: boolean[]
- **集合操作**: Set<Integer>
- **线程安全位集合**: Collections.synchronizedSet(new BitSet())
