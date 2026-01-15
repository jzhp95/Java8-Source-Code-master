/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the <tt>HashMap</tt> class, including
 * <tt>get</tt> and <tt>put</tt>).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt>
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 *
 * 【中文说明】
 * HashMap是基于哈希表的Map接口实现。
 * 它提供了所有可选的Map操作，并允许null值和null键。
 * (HashMap类大致等同于Hashtable，但它是非同步的并且允许null值。)
 * 
 * 【核心特性】
 * 1. 允许null键和null值
 * 2. 不保证顺序（不保证元素的插入顺序）
 * 3. 非同步，多线程环境下需要外部同步
 * 4. 迭代器是fail-fast的
 * 
 * 【性能特点】
 * - 基本操作（get和put）在哈希函数正确分散元素的情况下提供常数时间性能
 * - 迭代集合视图的时间与HashMap实例的容量（桶数）加上大小（键值对数量）成正比
 * - 不要将初始容量设置得太高（或负载因子太低），如果迭代性能很重要
 * 
 * 【重要参数】
 * 1. 初始容量（initial capacity）：哈希表中的桶数，创建时的容量
 * 2. 负载因子（load factor）：衡量哈希表在自动增加容量前可以有多满的指标
 * 
 * 【扩容机制】
 * 当哈希表中的条目数超过负载因子和当前容量的乘积时，哈希表会被rehashed
 * (即重建内部数据结构)，使哈希表的桶数大约翻倍
 * 
 * 【负载因子选择】
 * 默认负载因子0.75在时间和空间成本之间提供了良好的权衡
 * 较高的值会减少空间开销，但会增加查找成本
 * 
 * 【树化机制】
 * 当桶中的节点数超过TREEIFY_THRESHOLD(8)时，链表会转化为红黑树
 * 这提供了最坏情况O(log n)的操作性能
 * 当树中的节点数少于UNTREEIFY_THRESHOLD(6)时，树会转回链表
 * 
 * 【注意事项】
 * 1. 使用相同hashCode的多个键会显著降低性能
 * 2. 当键是Comparable时，HashMap会使用比较顺序来帮助打破平局
 * 3. 此实现不是同步的，如果多个线程并发访问且至少一个线程修改结构，
 *    则必须在外部同步
 * 4. 迭代器的fail-fast行为不能被保证，只能用于检测bug
 */
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;

    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     *
     * 【实现原理中文说明】
     * 
     * 【数据结构】
     * HashMap通常作为带桶（binned）的哈希表运作，但当桶变得太大时，
     * 它们会转化为TreeNode的桶，每个桶的结构与java.util.TreeMap类似。
     * 大多数方法尝试使用普通桶，但在适用时中继到TreeNode方法
     * （只需通过检查节点 instanceof）。
     * TreeNode的桶可以像其他桶一样遍历和使用，但在节点过多时还支持更快的查找。
     * 然而，由于绝大多数桶在正常使用下不会过度填充，
     * 在表方法执行过程中可能延迟检查树桶的存在。
     * 
     * 【树桶排序规则】
     * 树桶（即元素全部是TreeNode的桶）主要按hashCode排序，
     * 但在出现平局的情况下，如果两个元素是相同的"class C implements Comparable<C>"类型，
     * 则使用它们的compareTo方法进行排序。（我们通过反射保守地检查泛型类型来验证这一点）
     * 
     * 【树化优势】
     * 树桶增加的复杂性是值得的，因为它在最坏情况下提供O(log n)的操作性能，
     * 当键具有不同的哈希值或可排序时。
     * 因此，性能在意外或恶意使用hashCode()方法返回分布不良的值，
     * 或许多个键共享hashCode（只要它们也是Comparable的）的情况下能够优雅地降级。
     * 
     * 【树化阈值】
     * 因为TreeNode的大小大约是常规节点的两倍，
     * 我们只在桶包含足够多的节点以值得使用时才使用它们（参见TREEIFY_THRESHOLD）。
     * 当它们因为移除或调整大小而变得太小时，会转换回普通桶。
     * 
     * 【泊松分布】
     * 在用户hashCode分布良好的情况下，树桶很少使用。
     * 理想情况下，在随机hashCode下，桶中节点的频率遵循泊松分布，
     * 参数平均值约为0.5（默认调整大小阈值为0.75时）。
     * 
     * 【树根位置】
     * 树桶的根节点通常是第一个节点。然而，有时（目前仅在Iterator.remove时），
     * 根可能在其他地方，但可以通过父链接恢复（TreeNode.root()方法）。
     * 
     * 【方法参数约定】
     * 所有适用的内部方法接受哈希码作为参数（通常由公共方法提供），
     * 这样它们可以相互调用而无需重新计算用户hashCode。
     * 大多数内部方法还接受"tab"参数，这通常是当前表，
     * 但在调整大小或转换时可能是新的或旧的表。
     * 
     * 【链表顺序维护】
     * 当桶列表被树化、分割或非树化时，我们保持它们相同的相对访问/遍历顺序
     * （即Node.next字段），以更好地保持局部性，并稍微简化处理分割和调用iterator.remove的遍历。
     * 当在插入时使用比较器时，为了在重新平衡过程中保持总排序，
     * 我们比较类和identityHashCodes作为平局打破者。
     * 
     * 【LinkedHashMap集成】
     * 普通模式与树模式之间的使用和转换因子类LinkedHashMap的存在而变得复杂。
     * 请参阅下面定义的钩子方法，这些方法在插入、移除和访问时被调用，
     * 允许LinkedHashMap内部独立于这些机制。
     * （这还需要将映射实例传递给一些可能创建新节点的实用方法。）
     * 
     * 【编码风格】
     * 类似并发编程的基于SSA的编码风格有助于避免所有复杂指针操作中的混叠错误。
     */

    /**
     * The default initial capacity - MUST be a power of two.
     * 默认初始容量 - 必须是2的幂次方。
     * 16是2的4次方，是经验和理论上的最佳默认值。
     * 初始容量太小会导致频繁扩容，初始容量太大浪费空间。
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     * 最大容量，当构造函数参数指定更高值时使用。
     * 必须是2的幂次方且不超过1<<30（约10亿）。
     * 这个限制是为了避免在计算索引时发生整数溢出。
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     * 构造函数未指定时使用的负载因子。
     * 0.75是经验和理论上的最佳默认值，在时间和空间成本之间取得良好平衡。
     * 
     * 【负载因子详解】
     * 负载因子是衡量哈希表在自动扩容前可以有多满的指标。
     * 负载因子 = 元素数量 / 桶的数量
     * 
     * 较高的负载因子（如0.9）会减少空间开销，但增加查找成本（更多冲突）。
     * 较低的负载因子（如0.5）会减少冲突，但增加空间开销。
     * 
     * 默认值0.75意味着当哈希表填充到75%容量时会触发扩容，
     * 这在大多数场景下提供了良好的性能。
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     * 
     * 【树化阈值】
     * 将桶从链表转换为红黑树的节点数量阈值。
     * 当向包含至少这么多节点的桶中添加元素时，桶会被转化为树。
     * 
     * 【选择8的原因】
     * 1. 根据泊松分布，桶中节点数为8的概率非常低（小于千万分之一）
     * 2. 当链表长度达到8时，说明哈希函数可能存在问题或遭受哈希攻击
     * 3. 配合泊松分布概率，链表长度为8时意味着大多数情况下是异常情况
     * 4. 值必须大于2（因为链表转红黑树至少需要3个节点才有意义）
     * 5. 值至少为8是为了与树移除时关于收缩后转换回普通桶的假设保持一致
     * 
     * 【性能影响】
     * 红黑树提供O(log n)的查找性能，而链表是O(n)。
     * 当节点数达到8时，红黑树的性能优势开始显现。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     * 
     * 【非树化阈值】
     * 在调整大小操作期间对（分割的）桶进行非树化的节点数量阈值。
     * 
     * 【选择6的原因】
     * 1. 必须小于TREEIFY_THRESHOLD（8），以避免在阈值附近频繁转换
     * 2. 最大为6是为了与移除时的收缩检测保持一致
     * 3. 6是8的75%，这意味着在移除操作后，当节点数减少到原来的75%时触发非树化
     * 4. 这个设计避免了链表和树之间的频繁转换
     * 
     * 【设计原理】
     * 这个滞后区间（6-8）避免了在临界点附近频繁地在链表和树之间转换。
     * 类似于哈希表的负载因子设计，通过设置 hysteresis（滞后）来减少振荡。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     * 
     * 【最小树化容量】
     * 桶可以被树化的最小表容量。
     * 
     * 【选择64的原因】
     * 1. 至少是4 * TREEIFY_THRESHOLD（4*8=32），以避免调整大小和树化阈值之间的冲突
     * 2. 64提供了足够的桶数量来分散元素，避免在容量很小时就进行树化
     * 
     * 【设计原理】
     * 当桶中的节点数达到TREEIFY_THRESHOLD（8）时，如果表容量小于MIN_TREEIFY_CAPACITY（64），
     * HashMap会优先选择扩容而不是树化。这是因为在容量较小的情况下，
     * 扩容可以更好地分散元素，而树化的开销相对较大。
     * 
     * 这个设计确保了在表容量较小时优先扩容，在表容量足够大时才进行树化。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     * 
     * 【中文说明】
     * 判断对象是否实现了Comparable接口，如果是则返回该对象的Class。
     * 这是HashMap用于比较键的大小的辅助方法。
     * 
     * 【实现逻辑】
     * 1. 首先检查对象是否是Comparable的实例
     * 2. 如果是String类型，直接返回String.class（String使用频繁）
     * 3. 遍历对象实现的泛型接口，检查是否有Comparable<X>类型参数等于对象自身类型
     * 4. 如果找到匹配的Comparable接口，返回对象类型
     * 5. 如果没找到，返回null
     * 
     * 【为什么需要】
     * 红黑树需要能够比较节点的大小来决定插入位置。
     * 如果键实现了Comparable接口，就使用compareTo方法比较；
     * 如果没有实现，需要使用tieBreakOrder来打破平局。
     * 
     * 【性能特点】
     * 使用了泛型反射来检查接口实现。
     * 对于String类型有特殊优化，直接返回String.class。
     * 
     * @param x 要检查的对象
     * @return 如果实现了Comparable接口返回其Class，否则返回null
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                        ((p = (ParameterizedType)t).getRawType() ==
                         Comparable.class) &&
                        (as = p.getActualTypeArguments()) != null &&
                        as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * 如果x匹配kc（k的可比较类），返回k.compareTo(x)，否则返回0。
     * 
     * 【中文说明】
     * 比较两个对象的大小。
     * 用于在红黑树中确定节点的插入顺序。
     * 
     * 【使用场景】
     * 当两个节点的哈希值相等时，使用此方法进一步比较大小。
     * 只有当x的类型与kc匹配时才会比较，否则返回0。
     * 
     * 【返回值说明】
     * 1. 如果x为null或类型不匹配，返回0（表示无法比较）
     * 2. 如果类型匹配，返回k.compareTo(x)的结果
     * 
     * 【性能特点】
     * 直接调用Comparable的compareTo方法，时间复杂度O(1)。
     * 
     * 【参数说明】
     * @param kc 键的可比较类
     * @param k 第一个对象
     * @param x 第二个对象
     * @return 比较结果，如果无法比较返回0
     */
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * 根据给定的目标容量返回2的幂次方大小。
     * 
     * 【中文说明】
     * 将任意容量值调整为2的幂次方。
     * 这是HashMap初始化时的关键辅助方法。
     * 
     * 【实现原理】
     * 使用位运算将最高位1后面的所有位都设为1，
     * 然后再加1得到最近的2的幂次方。
     * 
     * 【位运算过程】
     * 例如：cap = 10（二进制 1010）
     * n |= n >>> 1  -> n = 1010 | 0101 = 1111 (15)
     * n |= n >>> 2  -> n = 1111 | 0011 = 1111 (15)
     * n |= n >>> 4  -> n = 1111 | 0000 = 1111 (15)
     * n |= n >>> 8  -> n = 1111 | 0000 = 1111 (15)
     * n |= n >>> 16 -> n = 1111 | 0000 = 1111 (15)
     * return n + 1 = 16
     * 
     * 【应用场景】
     * 1. 构造函数中计算初始容量
     * 2. putMapEntries中预计算容量
     * 
     * 【特殊处理】
     * 1. 如果cap为负数，返回1
     * 2. 如果计算结果超过MAXIMUM_CAPACITY，返回MAXIMUM_CAPACITY
     * 3. 先减1再运算，避免cap本身就是2的幂次方时返回cap*2
     * 
     * @param cap 目标容量
     * @return 2的幂次方的容量值
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     * 
     * 【表数组】
     * 哈希表的底层数组，存储所有的桶（bucket）。
     * 首次使用时初始化，根据需要调整大小。
     * 分配时，长度总是2的幂次方。
     * 
     * 【设计原理】
     * 1. 延迟初始化：创建HashMap时不分配，直到第一次使用时才分配
     * 2. 容量为2的幂次方：这样可以通过位运算（hash & (length-1)）来计算索引，比取模更快
     * 3. 可能长度为0：在某些操作中允许长度为0，以支持当前不需要的引导机制
     * 
     * 【与负载因子的关系】
     * 当元素数量超过 capacity * loadFactor 时，会触发扩容
     */
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     * 
     * 【条目集合缓存】
     * 缓存entrySet()返回的集合视图。
     * 注意：keySet()和values()使用AbstractMap中的字段。
     * 
     * 【优化目的】
     * 避免每次调用entrySet()都创建新的集合对象，提高性能。
     * 
     * 【与LinkedHashMap的关系】
     * LinkedHashMap会覆盖此字段以维护插入顺序。
     */
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     * 
     * 【映射数量】
     * 此Map中包含的键值对的数量。
     * 
     * 【与capacity的区别】
     * size是实际存储的元素数量，capacity是表的桶数量。
     * size <= capacity * loadFactor（触发扩容前）。
     * 
     * 【线程安全】
     * 此字段不是同步的，多线程环境下需要外部同步。
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     * 
     * 【结构性修改次数】
     * HashMap被结构性修改的次数。
     * 结构性修改是指改变映射数量的操作或以其他方式修改其内部结构的操作（如rehash）。
     * 
     * 【Fail-fast机制】
     * 此字段用于使HashMap的Collection视图上的迭代器快速失败。
     * 如果在迭代过程中检测到modCount被其他线程修改，会抛出ConcurrentModificationException。
     * 
     * 【使用场景】
     * 1. 检测并发修改：在迭代开始后，任何结构性修改都会导致迭代器快速失败
     * 2. 调试工具：帮助发现并发修改的问题
     * 
     * 【注意事项】
     * fail-fast行为不能被保证，只能用于检测bug，不能依赖它来保证并发安全。
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    
    /**
     * 【扩容阈值】
     * 下一次触发扩容的大小值（capacity * load factor）。
     * 
     * 【双重角色】
     * 1. 当table已分配时：threshold = capacity * loadFactor，表示触发扩容的阈值
     * 2. 当table未分配时：threshold存储初始数组容量，或0表示使用DEFAULT_INITIAL_CAPACITY
     * 
     * 【计算公式】
     * threshold = capacity × loadFactor
     * 例如：capacity=16, loadFactor=0.75，则threshold=12
     * 当size达到12时，会触发扩容
     * 
     * 【与size的区别】
     * size是当前元素数量，threshold是扩容触发点。
     * size达到threshold时，capacity会翻倍，threshold也会相应调整。
     * 
     * 【序列化说明】
     * 在序列化时，此字段的语义保持不变。
     */
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    
    /**
     * 【负载因子】
     * 哈希表的负载因子。
     * 
     * 【作用】
     * 负载因子决定了HashMap在自动扩容前可以有多满。
     * 
     * 【final修饰】
     * 此字段被声明为final，意味着在创建后不能修改。
     * 这是因为HashMap的负载因子在创建时确定，之后不能改变。
     * 
     * 【与threshold的关系】
     * threshold = capacity × loadFactor
     * 每次扩容后，capacity翻倍，threshold也会翻倍。
     * 
     * 【默认值为0.75】
     * 0.75是经验和理论上的最佳默认值，在空间和时间之间取得平衡。
     */
    final float loadFactor;

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     * 
     * 【中文说明】
     * 构造一个具有指定初始容量和负载因子的空HashMap。
     * 
     * 【参数说明】
     * @param initialCapacity 初始容量 - 哈希表中的桶数
     * @param loadFactor 负载因子 - 衡量哈希表在自动扩容前可以有多满的指标
     * 
     * 【异常说明】
     * @throws IllegalArgumentException 如果初始容量为负数或负载因子为非正数
     * 
     * 【实现细节】
     * 1. 验证initialCapacity必须在[0, MAXIMUM_CAPACITY]范围内
     * 2. 验证loadFactor必须大于0且不是NaN
     * 3. 调用tableSizeFor将initialCapacity调整为2的幂次方
     * 4. 将调整后的值存储在threshold字段中（此时table尚未分配）
     * 
     * 【使用建议】
     * 如果已知需要存储的元素数量，建议提前计算合适的初始容量：
     * 初始容量 = 预期元素数量 / 负载因子 + 1
     * 这样可以减少扩容次数，提高性能。
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     * 
     * 【中文说明】
     * 构造一个具有指定初始容量和默认负载因子（0.75）的空HashMap。
     * 
     * 【参数说明】
     * @param initialCapacity 初始容量 - 哈希表中的桶数
     * 
     * 【异常说明】
     * @throws IllegalArgumentException 如果初始容量为负数
     * 
     * 【实现细节】
     * 调用上面的两个参数构造函数，传入默认负载因子DEFAULT_LOAD_FACTOR（0.75）。
     * 
     * 【使用场景】
     * 当你只想指定初始容量，而使用默认负载因子时使用。
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     * 
     * 【中文说明】
     * 构造一个具有默认初始容量（16）和默认负载因子（0.75）的空HashMap。
     * 
     * 【默认参数】
     * 初始容量：16（DEFAULT_INITIAL_CAPACITY）
     * 负载因子：0.75（DEFAULT_LOAD_FACTOR）
     * 
     * 【实现细节】
     * 只设置loadFactor为默认值，其他字段使用默认值。
     * table为null，threshold为0，表示使用默认初始容量。
     * 
     * 【使用场景】
     * 这是最常用的构造函数，适用于不需要预先知道元素数量的场景。
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     * 
     * 【中文说明】
     * 构造一个具有与指定Map相同映射的新HashMap。
     * HashMap使用默认负载因子（0.75）和足以容纳指定Map中映射的初始容量。
     * 
     * 【参数说明】
     * @param m 要复制到此Map中的映射的来源Map
     * 
     * 【异常说明】
     * @throws NullPointerException 如果指定的Map为null
     * 
     * 【实现细节】
     * 1. 设置loadFactor为默认值
     * 2. 调用putMapEntries方法复制所有映射
     * 
     * 【性能特点】
     * 如果指定Map的大小已知，会预先计算合适的初始容量，避免后续扩容。
     * 
     * 【使用场景】
     * 创建现有Map的副本，或者将其他Map实现转换为HashMap。
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    /**
     * Implements Map.putAll and Map constructor
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     * 
     * 【中文说明】
     * 实现Map.putAll和Map构造方法。
     * 
     * 【参数说明】
     * @param m 要复制到此Map的源Map
     * @param evict 在最初构造此Map时为false，否则为true（中继到afterNodeInsertion方法）
     * 
     * 【evict参数的作用】
     * false：表示这是构造函数的调用，此时不应该触发回调（如LinkedHashMap的回调）
     * true：表示这是putAll方法的调用，可以触发回调
     * 
     * 【实现逻辑】
     * 1. 获取源Map的大小
     * 2. 如果源Map为空，直接返回
     * 3. 如果当前table未分配：
     *    - 根据源Map大小预计算需要的容量
     *    - 考虑负载因子，计算threshold
     * 4. 如果源Map非空但threshold为0（表示使用默认初始容量）：
     *    - 根据源Map大小计算合适的初始容量
     * 5. 遍历源Map的所有条目，调用putVal方法插入
     * 
     * 【容量预计算】
     * float ft = ((float)s / loadFactor) + 1.0F;
     * 这个公式确保有足够的容量来存储所有元素。
     * 
     * 【线程安全】
     * 此方法不是同步的。如果多线程并发访问，需要外部同步。
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (table == null) { // pre-size
                float ft = ((float)s / loadFactor) + 1.0F;
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            else if (s > threshold)
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     * 
     * 【中文说明】
     * 返回此Map中键值映射的数量。
     * 
     * 【实现原理】
     * 直接返回内部维护的size字段，这是O(1)时间复杂度的操作。
     * 
     * 【与容量的区别】
     * size是当前存储的元素数量，capacity是哈希表的桶数量。
     * size <= capacity * loadFactor（在触发扩容前）。
     * 
     * 【返回值】
     * 返回键值对的数量，如果Map为空则返回0。
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     * 
     * 【中文说明】
     * 判断此Map是否不包含任何键值映射。
     * 
     * 【实现原理】
     * 直接比较size字段是否为0，这是O(1)时间复杂度的操作。
     * 
     * 【使用场景】
     * 1. 判断Map是否为空
     * 2. 在处理Map之前进行空值检查
     * 
     * 【返回值】
     * 如果Map为空返回true，否则返回false。
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     * 
     * 【中文说明】
     * 返回指定键映射的值，如果此Map不包含该键的映射则返回null。
     * 
     * 【等价定义】
     * 如果此Map包含从键k到值v的映射，且满足(key==null ? k==null : key.equals(k))，
     * 则此方法返回v；否则返回null。（最多只能有一个这样的映射。）
     * 
     * 【null返回值说明】
     * 返回null不一定表示Map不包含该键的映射；
     * 也可能是Map显式地将键映射到null。
     * 如果需要区分这两种情况，可以使用containsKey操作。
     * 
     * 【实现原理】
     * 1. 计算键的哈希码
     * 2. 调用内部方法getNode查找节点
     * 3. 如果找到返回节点的value，否则返回null
     * 
     * 【时间复杂度】
     * 平均O(1)，最坏O(n)（当所有元素都在同一个桶中时）
     * 
     * 【使用场景】
     * 1. 根据键获取对应的值
     * 2. 配合containsKey判断键是否存在
     * 
     * 【与getOrDefault的区别】
     * get(key)在键不存在时返回null，getOrDefault可以指定默认值。
     */
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * Implements Map.get and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     * 
     * 【中文说明】
     * 实现Map.get和相关方法的核心查找逻辑。
     * 
     * 【参数说明】
     * @param hash 键的哈希码（已经过扰动处理）
     * @param key 要查找的键
     * 
     * 【返回值】
     * 找到的节点，如果找不到返回null
     * 
     * 【实现步骤】
     * 1. 检查表是否已初始化且不为空
     * 2. 计算桶索引：(n-1) & hash
     * 3. 获取桶的第一个节点
     * 4. 如果第一个节点就是要找的节点，直接返回
     * 5. 否则遍历链表或红黑树查找
     * 
     * 【查找逻辑】
     * 1. 先检查第一个节点（因为大多数情况下要找的节点是第一个）
     * 2. 如果第一个节点是TreeNode，调用树查找
     * 3. 否则遍历链表查找
     * 
     * 【比较逻辑】
     * 1. 先比较哈希码
     * 2. 再比较键的引用相等性或equals方法
     */
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        // 步骤1：检查表是否已初始化且不为空
        if ((tab = table) != null && (n = tab.length) > 0 &&
            // 步骤2：计算桶索引并获取第一个节点
            (first = tab[(n - 1) & hash]) != null) {
            // 步骤3：检查第一个节点是否就是要找的节点
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            // 步骤4：遍历链表或红黑树查找
            if ((e = first.next) != null) {
                // 如果是红黑树，使用树查找
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                // 否则遍历链表
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * 
     * 【中文说明】
     * 判断此Map是否包含指定键的映射。
     * 
     * 【参数说明】
     * @param key 要测试是否存在的键
     * 
     * 【实现原理】
     * 调用getNode方法，如果返回不为null则表示键存在。
     * 
     * 【与get的区别】
     * get返回键对应的值，containsKey只返回键是否存在。
     * containsKey比get更高效，因为它不需要返回值。
     * 
     * 【时间复杂度】
     * 与get相同，平均O(1)，最坏O(n)。
     * 
     * 【返回值】
     * 如果Map包含该键的映射返回true，否则返回false。
     * 
     * 【使用场景】
     * 1. 检查键是否存在
     * 2. 在更新值之前确认键是否存在
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     * 
     * 【中文说明】
     * 将指定的值与Map中的指定键关联。
     * 如果Map之前包含该键的映射，则旧值会被替换。
     * 
     * 【参数说明】
     * @param key 要与指定值关联的键
     * @param value 要与指定键关联的值
     * 
     * 【返回值】
     * 键之前关联的值，如果之前没有映射则返回null。
     * （如果Map之前将null与该键关联，也可能返回null。）
     * 
     * 【实现原理】
     * 1. 计算键的哈希码
     * 2. 调用内部方法putVal执行实际的插入逻辑
     * 
     * 【覆盖逻辑】
     * 如果键已存在，新值会覆盖旧值，并返回旧值。
     * 如果键不存在，则添加新的键值对，返回null。
     * 
     * 【时间复杂度】
     * 平均O(1)，最坏O(n)。
     * 
     * 【使用场景】
     * 1. 添加新的键值对
     * 2. 更新现有键的值
     * 
     * 【与putIfAbsent的区别】
     * put会无条件覆盖旧值，putIfAbsent只在键不存在时添加。
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     * 
     * 【中文说明】
     * 实现Map.put和相关方法的核心插入逻辑。
     * 
     * 【参数说明】
     * @param hash 键的哈希码（已经过扰动处理）
     * @param key 要插入的键
     * @param value 要插入的值
     * @param onlyIfAbsent 如果为true，不改变已存在的值（即putIfAbsent语义）
     * @param evict 如果为false，表处于创建模式（用于构造函数调用）
     * 
     * 【返回值】
     * 键之前关联的值，如果之前没有映射则返回null。
     * 
     * 【实现步骤】
     * 1. 检查表是否需要初始化或扩容
     * 2. 计算桶索引，检查桶是否为空
     * 3. 如果桶为空，直接创建新节点
     * 4. 如果桶不为空，查找是否已存在相同的键
     * 5. 如果存在相同的键，更新值（根据onlyIfAbsent决定是否覆盖）
     * 6. 如果不存在相同的键，在链表末尾添加新节点
     * 7. 检查是否需要树化
     * 8. 更新modCount和size，如果需要则扩容
     * 
     * 【onlyIfAbsent参数】
     * true：只在新键不存在时插入（putIfAbsent的语义）
     * false：总是覆盖旧值（put的语义）
     * 
     * 【evict参数】
     * false：表示这是构造函数的调用，触发LinkedHashMap的回调
     * true：表示这是put或putAll的调用，触发回调
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        // 步骤1：如果table为null或长度为0，进行初始化或扩容
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 步骤2：计算桶索引，如果桶为空则直接创建新节点
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            Node<K,V> e; K k;
            // 步骤3：查找是否已存在相同的键
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;  // 找到相同的键
            else if (p instanceof TreeNode)
                // 如果是红黑树节点，调用树的插入方法
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 遍历链表查找
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        // 到达链表末尾，添加新节点
                        p.next = newNode(hash, key, value, null);
                        // 如果链表长度达到树化阈值，进行树化
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    // 在链表中找到相同的键
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // 步骤4：更新已存在的节点的值
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                // 只有在不覆盖或旧值为null时才更新
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                // 回调：访问后处理（用于LinkedHashMap）
                afterNodeAccess(e);
                return oldValue;
            }
        }
        // 步骤5：更新modCount
        ++modCount;
        // 步骤6：如果超过阈值，进行扩容
        if (++size > threshold)
            resize();
        // 步骤7：回调：插入后处理（用于LinkedHashMap的LRU策略）
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     * 
     * 【中文说明】
     * 初始化或加倍表的大小。
     * 这是HashMap中最核心的方法之一，负责：
     * 1. 首次插入元素时初始化表
     * 2. 当元素数量超过阈值时进行扩容
     * 
     * 【触发时机】
     * 1. 首次插入元素时（table为null）
     * 2. putVal中size超过threshold时
     * 3. putMapEntries中源Map比当前threshold大时
     * 
     * 【容量计算策略】
     * 根据旧表的状态有三种情况：
     * 1. oldCap > 0：已初始化过，新容量 = oldCap × 2
     * 2. oldThr > 0 && oldCap == 0：使用构造函数指定的初始容量
     * 3. oldCap == 0 && oldThr == 0：使用默认容量16
     * 
     * 【元素重新分配原理】
     * 由于容量总是2的幂次方，扩容时元素的索引计算遵循以下规律：
     * - 新索引 = hash & (newCap - 1)
     * - 旧索引 = hash & (oldCap - 1)
     * - 由于newCap = oldCap × 2，新索引可能是：
     *   - 保持原索引（当hash的第oldCap位为0时）
     *   - 原索引 + oldCap（当hash的第oldCap位为1时）
     * 
     * 【性能影响】
     * 扩容是HashMap中最昂贵的操作，需要：
     * 1. 分配新的数组
     * 2. 重新计算所有元素的索引
     * 3. 可能触发链表和红黑树之间的转换
     * 建议在创建HashMap时预估容量，减少扩容次数。
     */
    final Node<K,V>[] resize() {
        // 保存旧的表和容量
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        
        // ========== 计算新容量和新阈值 ==========
        
        if (oldCap > 0) {
            // 情况1：oldCap > 0，表示表已初始化，需要扩容
            // 如果已经达到最大容量，只调整threshold，不再扩容
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 新容量 = 旧容量 × 2
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                // 新threshold = 旧threshold × 2（翻倍）
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            // 情况2：oldThr > 0 && oldCap == 0，表示调用构造函数时指定了初始容量
            // 此时threshold中存储的是初始容量
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            // 情况3：使用默认参数构造
            newCap = DEFAULT_INITIAL_CAPACITY;  // 默认容量 16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);  // 默认 threshold = 12
        }
        
        // 如果newThr为0，需要重新计算
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        
        // ========== 创建新表 ==========
        
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        
        // 如果是初始化（oldTab == null），直接返回
        if (oldTab == null)
            return newTab;
        
        // ========== 重新分配元素 ==========
        
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            // 获取当前桶的第一个节点
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;  // 帮助GC回收
                
                // 情况1：桶只有一个节点，直接计算新索引并移动
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                
                // 情况2：桶是红黑树，调用树的split方法
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                
                // 情况3：桶是链表，需要拆分链表
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;  // 保持原索引的链表
                    Node<K,V> hiHead = null, hiTail = null;  // 移动到新位置的链表
                    Node<K,V> next;
                    
                    // 遍历链表中的每个节点
                    do {
                        next = e.next;
                        
                        // 计算是否需要移动到新位置
                        // hash & oldCap == 0 表示保持原索引
                        // hash & oldCap != 0 表示移动到 j + oldCap 的位置
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    
                    // 将保持原索引的链表放到新表的相同位置
                    if (loTail != null) {
                        loTail.next = null;  // 断开链表
                        newTab[j] = loHead;
                    }
                    
                    // 将需要移动的链表放到新表的 j + oldCap 位置
                    if (hiTail != null) {
                        hiTail.next = null;  // 断开链表
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     * 
     * 【中文说明】
     * 将指定哈希值对应桶中的所有链表节点替换为红黑树节点，
     * 除非表太小（小于MIN_TREEIFY_CAPACITY），在这种情况下会进行扩容。
     * 
     * 【树化条件】
     * 1. 链表长度达到TREEIFY_THRESHOLD（8）
     * 2. 表容量达到MIN_TREEIFY_CAPACITY（64）
     * 
     * 【实现步骤】
     * 1. 检查表是否为空或容量是否小于MIN_TREEIFY_CAPACITY，如果是则扩容
     * 2. 遍历链表，将每个节点转换为TreeNode
     * 3. 构建双向链表（使用prev和next指针）
     * 4. 调用treeify构建红黑树
     * 
     * 【为什么不直接在链表上构建树】
     * TreeNode需要额外的父、子、左右指针，而普通Node只有next指针。
     * 先创建TreeNode并建立双向链表，再构建树，可以减少中间状态的复杂性。
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        // 条件检查：如果表为空或容量太小，进行扩容而不是树化
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;  // hd: 头节点, tl: 尾节点
            // 步骤1：将链表节点转换为TreeNode，并建立双向链表
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            
            // 步骤2：用双向链表构建红黑树
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     * 
     * 【中文说明】
     * 将指定Map中的所有映射复制到此Map。
     * 这些映射将替换此Map中当前存在的任何键的映射。
     * 
     * 【覆盖行为】
     * 如果此Map已包含某个键的映射，新值会覆盖旧值。
     * 
     * 【实现原理】
     * 调用putMapEntries方法，传入evict=true。
     * 
     * 【异常说明】
     * @throws NullPointerException 如果指定的Map为null
     * 
     * 【时间复杂度】
     * O(n)，其中n是指定Map的大小。
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     * 
     * 【中文说明】
     * 如果存在，从Map中移除指定键的映射。
     * 
     * 【参数说明】
     * @param key 要移除其映射的键
     * 
     * 【返回值】
     * 键之前关联的值，如果之前没有映射则返回null。
     * （如果Map之前将null与该键关联，也可能返回null。）
     * 
     * 【实现原理】
     * 调用removeNode方法执行实际的删除逻辑。
     * 
     * 【使用场景】
     * 1. 移除特定的键值对
     * 2. 配合get使用，先获取值再删除
     */
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     * 
     * 【中文说明】
     * 实现Map.remove和相关方法的核心删除逻辑。
     * 
     * 【参数说明】
     * @param hash 键的哈希码（已经过扰动处理）
     * @param key 要删除的键
     * @param value 如果matchValue为true，要匹配的值；否则忽略
     * @param matchValue 如果为true，只在值相等时才删除
     * @param movable 如果为false，删除时不移动其他节点
     * 
     * 【返回值】
     * 被删除的节点，如果找不到返回null
     * 
     * 【实现步骤】
     * 1. 找到要删除的节点
     * 2. 验证值是否匹配（如果matchValue为true）
     * 3. 从链表中移除节点
     * 4. 更新modCount和size
     * 
     * 【节点类型处理】
     * 1. TreeNode：调用树的removeTreeNode方法
     * 2. 普通Node：从链表中移除
     * 
     * 【链表移除逻辑】
     * 如果要删除的节点是桶的第一个节点，直接将桶指向下一个节点
     * 否则，将前一个节点的next指向要删除节点的下一个节点
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        // 步骤1：检查表是否已初始化且不为空
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            // 步骤2：查找要删除的节点
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;  // 第一个节点就是要删除的节点
            else if ((e = p.next) != null) {
                // 如果是红黑树，使用树查找
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    // 否则遍历链表查找
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            
            // 步骤3：删除节点
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                // 如果是TreeNode，调用树的删除方法
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                // 如果是普通Node，从链表中移除
                else if (node == p)
                    tab[index] = node.next;  // 删除桶的第一个节点
                else
                    p.next = node.next;  // 删除链表中的节点
                
                // 更新modCount和size
                ++modCount;
                --size;
                // 回调：移除后处理
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     * 
     * 【中文说明】
     * 从Map中移除所有映射。
     * 此调用返回后，Map将为空。
     * 
     * 【实现原理】
     * 1. 增加modCount
     * 2. 将size设为0
     * 3. 将表数组的所有元素设为null（帮助GC）
     * 
     * 【性能特点】
     * 时间复杂度O(capacity)，需要遍历整个表数组。
     * 
     * 【与新建Map的比较】
     * clear()会重用现有的数组，而新建Map会分配新数组。
     * 如果Map很大，clear()可能比分配合约新数组更高效。
     * 
     * 【线程安全】
     * 此方法不是同步的。如果多线程并发访问，需要外部同步。
     */
    public void clear() {
        Node<K,V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            // 将所有桶设为null，帮助GC回收
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     * 
     * 【中文说明】
     * 判断此Map是否将一个或多个键映射到指定值。
     * 
     * 【参数说明】
     * @param value 要测试是否存在的值
     * 
     * 【返回值】
     * 如果此Map将一个或多个键映射到指定值返回true，否则返回false。
     * 
     * 【实现原理】
     * 遍历整个表和所有链表/红黑树，检查每个节点的值。
     * 
     * 【时间复杂度】
     * 最坏情况O(n × capacity)，其中n是元素数量，capacity是桶数量。
     * 这是HashMap中少数几个性能较差的操作之一。
     * 
     * 【与containsKey的区别】
     * containsKey根据键查找，时间复杂度平均O(1)。
     * containsValue根据值查找，必须遍历所有元素，时间复杂度O(n)。
     * 
     * 【使用建议】
     * 避免频繁调用containsValue，如果需要按值查找，考虑使用其他数据结构。
     */
    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {
            // 遍历所有桶
            for (int i = 0; i < tab.length; ++i) {
                // 遍历桶中的所有节点
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                        (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     * 
     * 【中文说明】
     * 返回此Map中包含的键的Set视图。
     * 
     * 【视图特性】
     * 1. 集合由Map支持，对Map的修改会反映到集合中，反之亦然
     * 2. 如果在迭代过程中修改Map（除通过迭代器自身的remove操作），迭代结果未定义
     * 3. 支持通过Iterator.remove、Set.remove、removeAll、retainAll和clear操作删除元素
     * 4. 不支持add或addAll操作（因为不能添加没有值的键）
     * 
     * 【性能特点】
     * 返回的Set不是新的对象，而是对Map键的动态视图。
     * size()是O(1)操作，迭代是O(n)操作。
     * 
     * 【使用场景】
     * 1. 获取所有键的集合
     * 2. 批量删除某些键
     * 3. 判断键是否存在
     * 
     * 【与直接使用Map.keySet()的区别】
     * 这是HashMap对keySet方法的重写实现。
     * 默认使用AbstractMap中的keySet实现，但HashMap有自己的KeySet内部类。
     * 
     * @return 此Map中包含的键的集合视图
     */
    public Set<K> keySet() {
        Set<K> ks;
        return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
    }

    /**
     * 键集合视图
     * 
     * 【中文说明】
     * 实现了AbstractSet的内部类，提供键的集合视图。
     * 
     * 【设计原理】
     * KeySet是Map.keySet()方法返回的具体Set实现。
     * 它直接操作底层的HashMap，而不是复制数据。
     * 
     * 【内部操作】
     * - size()：直接返回HashMap的size
     * - clear()：调用HashMap.clear()
     * - iterator()：返回KeyIterator，支持fail-fast
     * - contains()：调用containsKey方法
     * - remove()：调用removeNode方法
     * - forEach：遍历所有键
     */
    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super K> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     * 
     * 【中文说明】
     * 返回此Map中包含的值的Collection视图。
     * 
     * 【视图特性】
     * 1. 集合由Map支持，对Map的修改会反映到集合中，反之亦然
     * 2. 如果在迭代过程中修改Map（除通过迭代器自身的remove操作），迭代结果未定义
     * 3. 支持通过Iterator.remove、Collection.remove、removeAll、retainAll和clear操作删除元素
     * 4. 不支持add或addAll操作（因为不能添加没有键的值）
     * 
     * 【与Set的区别】
     * Values返回的是Collection而不是Set，因为值可以重复。
     * 
     * 【性能特点】
     * 迭代Values的顺序与键的哈希桶顺序相关，但不保证任何特定顺序。
     * 
     * 【使用场景】
     * 1. 获取所有值的集合
     * 2. 批量删除基于值的元素
     * 3. 判断值是否存在（containsValue）
     * 
     * @return 此Map中包含的值的集合视图
     */
    public Collection<V> values() {
        Collection<V> vs;
        return (vs = values) == null ? (values = new Values()) : vs;
    }

    /**
     * 值集合视图
     * 
     * 【中文说明】
     * 实现了AbstractCollection的内部类，提供值的集合视图。
     * 
     * 【设计原理】
     * Values是Map.values()方法返回的具体Collection实现。
     * 因为值可以重复，所以返回Collection而不是Set。
     * 
     * 【内部操作】
     * - size()：直接返回HashMap的size
     * - clear()：调用HashMap.clear()
     * - iterator()：返回ValueIterator
     * - contains()方法
     * - forEach：：调用containsValue遍历所有值
     */
    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     * 
     * 【中文说明】
     * 返回此Map中包含的映射的Set视图。
     * 
     * 【视图特性】
     * 1. 集合由Map支持，对Map的修改会反映到集合中，反之亦然
     * 2. 如果在迭代过程中修改Map（除通过迭代器自身的remove操作，
     *    或通过迭代器返回的Map条目的setValue操作），迭代结果未定义
     * 3. 支持通过Iterator.remove、Set.remove、removeAll、retainAll和clear操作删除元素
     * 4. 不支持add或addAll操作
     * 
     * 【Entry视图】
     * entrySet返回的是Map.Entry对象的集合。
     * 每个Entry是对Map中一个键值对的引用，可以通过setValue修改原Map的值。
     * 
     * 【使用场景】
     * 1. 批量操作键值对
     * 2. 遍历键值对
     * 3. 通过Entry比较和删除
     * 4. 实现Map的浅拷贝
     * 
     * 【与keySet和values的区别】
     * entrySet提供最完整的视图，可以同时访问键和值。
     * 这是实现各种Map操作的核心方法。
     * 
     * @return 此Map中包含的映射的集合视图
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    /**
     * 条目集合视图
     * 
     * 【中文说明】
     * 实现了AbstractSet的内部类，提供键值对（Map.Entry）的集合视图。
     * 
     * 【设计原理】
     * EntrySet是Map.entrySet()方法返回的具体Set实现。
     * 它直接操作底层的HashMap，返回的Entry对象是原Map中条目的引用。
     * 
     * 【特殊操作】
     * 1. contains()：检查Entry是否匹配（比较键和值）
     * 2. remove()：删除指定Entry（需要键和值都匹配）
     * 
     * 【fail-safe vs fail-fast】
     * EntrySet的迭代器是fail-fast的。
     * 但通过Entry.setValue()修改值不会触发ConcurrentModificationException，
     * 因为这不改变Map的结构（只是修改值）。
     */
    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods

    /**
     * 返回指定键映射的值，如果不存在则返回默认值。
     * 
     * 【中文说明】
     * 这是Java 8新增的方法，类似于get()，但可以指定默认值。
     * 
     * 【使用场景】
     * 1. 安全的获取操作，避免NullPointerException
     * 2. 缓存模式：获取不存在的键时返回默认值（可能触发缓存加载）
     * 3. 统计默认值被使用的次数
     * 
     * 【与get的区别】
     * get(key)在键不存在时返回null，getOrDefault可以指定任意默认值。
     * 
     * 【性能特点】
     * 与get相同，平均O(1)，最坏O(n)。
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @return 键映射的值，如果不存在返回默认值
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    /**
     * 如果键不存在或映射到null，则将键与值关联。
     * 
     * 【中文说明】
     * 这是Java 8新增的原子性"put if absent"操作。
     * 
     * 【与put的区别】
     * put总是覆盖旧值，putIfAbsent只在键不存在或值为null时添加。
     * 
     * 【实现原理】
     * 调用putVal方法，传入onlyIfAbsent=true。
     * 
     * 【原子性保证】
     * 此方法是原子的，但在多线程环境下仍需外部同步以确保可见性。
     * 
     * 【使用场景】
     * 1. 缓存初始化：多个线程同时初始化同一键时，只有一个会成功
     * 2. 计数器：安全地初始化计数器
     * 
     * @param key 键
     * @param value 值
     * @return 键之前关联的值，如果之前没有映射返回null
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    /**
     * 仅在键存在且映射到指定值时删除该映射。
     * 
     * 【中文说明】
     * 这是Java 8新增的条件删除方法。
     * 
     * 【使用场景】
     * 1. 乐观锁模式：仅当值匹配时才删除
     * 2. 避免竞态条件：确保删除的是期望的值
     * 
     * 【与remove(key)的区别】
     * remove(key)无条件删除键的映射。
     * remove(key, value)只在值匹配时才删除。
     * 
     * 【返回值】
     * 如果映射被删除返回true，否则返回false。
     * 
     * @param key 键
     * @param value 期望的值
     * @return 如果映射被删除返回true
     */
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    /**
     * 仅在键存在且映射到指定旧值时，用新值替换。
     * 
     * 【中文说明】
     * 这是Java 8新增的条件替换方法。
     * 
     * 【使用场景】
     * 1. 乐观锁模式：仅当值匹配时才更新
     * 2. 避免覆盖其他线程的更新
     * 
     * 【返回值】
     * 如果替换成功返回true，否则返回false。
     * 
     * @param key 键
     * @param oldValue 期望的旧值
     * @param newValue 新值
     * @return 如果替换成功返回true
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
            ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    /**
     * 仅在键存在时，用新值替换。
     * 
     * 【中文说明】
     * 这是Java 8新增的无条件替换方法。
     * 
     * 【与put的区别】
     * replace(key, value)只在键存在时替换，返回旧值或null。
     * put(key, value)总是覆盖，返回旧值或null。
     * 
     * 【使用场景】
     * 1. 安全的更新操作，避免添加新键
     * 2. 与putIfAbsent配合使用：先尝试replace，不行再putIfAbsent
     * 
     * @param key 键
     * @param value 新值
     * @return 键之前关联的值，如果键不存在返回null
     */
    @Override
    public V replace(K key, V value) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    /**
     * 如果键不存在或其值为null，则使用映射函数计算值并关联。
     * 
     * 【中文说明】
     * 这是Java 8新增的条件计算方法。
     * 如果指定的键不存在或其当前映射值为null，
     * 则使用提供的映射函数计算新值，并将键与计算结果关联。
     * 
     * 【使用场景】
     * 1. 缓存延迟加载：首次访问时计算并缓存值
     * 2. 计数器和累加器：安全的增量操作
     * 3. Map的默认值工厂模式
     * 
     * 【函数参数】
     * mappingFunction：接受键，返回计算出的值
     * 如果返回null，则不会插入任何映射
     * 
     * 【实现逻辑】
     * 1. 查找键是否已存在且值不为null
     * 2. 如果存在且值不为null，直接返回旧值
     * 3. 如果不存在或值为null，调用映射函数计算新值
     * 4. 如果计算结果不为null，插入或更新映射
     * 
     * 【注意事项】
     * 1. 映射函数应该没有副作用（幂等性）
     * 2. 映射函数可能调用多次（如果多线程同时调用）
     * 3. 如果函数返回null，键不会被添加
     * 
     * 【与getOrDefault的区别】
     * getOrDefault只是返回默认值，不改变Map内容。
     * computeIfAbsent会在键不存在时添加新值，改变Map内容。
     * 
     * @param key 键
     * @param mappingFunction 计算值的函数
     * @return 键当前或新关联的值
     * @throws NullPointerException 如果mappingFunction为null
     */
    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        // 如果需要扩容，先扩容
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 查找键是否已存在
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            // 如果键存在且值不为null，直接返回旧值
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        // 计算新值
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            // 键存在但值为null，更新值
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            // 插入到红黑树
            t.putTreeVal(this, tab, hash, key, v);
        else {
            // 插入到链表头部
            tab[i] = newNode(hash, key, v, first);
            // 如果链表太长，考虑树化
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    /**
     * 仅在键存在且其值非null时，使用重映射函数计算新值。
     * 
     * 【中文说明】
     * 这是Java 8新增的条件重映射方法。
     * 如果指定的键存在且其当前映射值非null，
     * 则使用提供的重映射函数计算新值。
     * 
     * 【函数行为】
     * 1. 如果重映射函数返回null，则删除该键的映射
     * 2. 如果重映射函数返回非null值，则更新键的映射
     * 3. 如果键不存在或其值为null，不调用函数，返回null
     * 
     * 【使用场景】
     * 1. 有条件的更新或删除
     * 2. Map条目的条件修改
     * 3. 实现CAS-like的更新操作
     * 
     * 【与compute的区别】
     * computeIfPresent只在键存在且值非null时调用函数。
     * compute无论键是否存在都会调用函数（可能需要先插入再更新）。
     * 
     * @param key 键
     * @param remappingFunction 重映射函数：接受键和旧值，返回新值或null
     * @return 新关联的值，如果键被删除或不存在返回null
     * @throws NullPointerException 如果remappingFunction为null
     */
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> e; V oldValue;
        int hash = hash(key);
        // 查找键
        if ((e = getNode(hash, key)) != null &&
            (oldValue = e.value) != null) {
            // 计算新值
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                // 更新值
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                // 返回null，删除该键
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    /**
     * 使用重映射函数计算键的新值，无论键当前状态如何。
     * 
     * 【中文说明】
     * 这是Java 8新增的重映射方法。
     * 对指定的键应用重映射函数，计算其新值。
     * 
     * 【函数行为】
     * 1. 如果键不存在，旧值为null
     * 2. 如果重映射函数返回null，则删除该键的映射
     * 3. 如果重映射函数返回非null值，则更新或添加键的映射
     * 
     * 【使用场景】
     * 1. 更新的Map条目（无论是否存在）
     * 2. 组合操作：删除+添加
     * 3. 实现复杂的状态转换
     * 
     * 【与merge的区别】
     * compute提供更精细的控制，可以根据旧值决定如何处理。
     * merge是简化的合并操作，有默认的"如果不存在则添加"行为。
     * 
     * 【注意事项】
     * 重映射函数应该处理null值的旧值。
     * 
     * @param key 键
     * @param remappingFunction 重映射函数：接受键和旧值，返回新值或null
     * @return 新关联的值，如果键被删除返回null
     * @throws NullPointerException 如果remappingFunction为null
     */
    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        // 如果需要扩容，先扩容
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 查找键是否已存在
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        // 获取旧值（可能为null）
        V oldValue = (old == null) ? null : old.value;
        // 计算新值
        V v = remappingFunction.apply(key, oldValue);
        
        if (old != null) {
            // 键存在
            if (v != null) {
                // 更新值
                old.value = v;
                afterNodeAccess(old);
            }
            else
                // 返回null，删除该键
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            // 键不存在，但计算出新值，添加新映射
            if (t != null)
                // 插入到红黑树
                t.putTreeVal(this, tab, hash, key, v);
            else {
                // 插入到链表头部
                tab[i] = newNode(hash, key, v, first);
                // 如果链表太长，考虑树化
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    /**
     * 如果键不存在则添加，否则使用重映射函数合并旧值和新值。
     * 
     * 【中文说明】
     * 这是Java 8新增的合并方法。
     * 简化了"如果不存在则添加，否则合并"的常见模式。
     * 
     * 【函数行为】
     * 1. 如果键不存在：将值与键关联
     * 2. 如果键存在：将旧值与新值作为参数调用重映射函数
     * 
     * 【使用场景】
     * 1. 合并两个Map
     * 2. 计数器累加
     * 3. 集合合并
     * 4. 默认值更新
     * 
     * 【与putIfAbsent的区别】
     * putIfAbsent只是简单的"如果不存在则添加"。
     * merge会在键存在时合并新旧值，提供更强大的功能。
     * 
     * 【与compute的区别】
     * merge的函数签名更简单：(oldValue, newValue) -> result
     * compute的函数签名需要处理null旧值的情况。
     * 
     * 【注意事项】
     * 1. 如果新值为null，抛出NullPointerException
     * 2. 如果重映射函数返回null，删除该键
     * 
     * @param key 键
     * @param value 值（不能为null）
     * @param remappingFunction 合并函数：接受旧值和新值，返回合并结果或null
     * @return 合并后的值，如果键被删除返回null
     * @throws NullPointerException 如果value或remappingFunction为null
     */
    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        // 如果需要扩容，先扩容
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 查找键是否已存在
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        
        if (old != null) {
            // 键存在，合并旧值和新值
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;  // 如果旧值为null，直接使用新值
            if (v != null) {
                // 更新值
                old.value = v;
                afterNodeAccess(old);
            }
            else
                // 返回null，删除该键
                removeNode(hash, key, null, false, true);
            return v;
        }
        
        // 键不存在，添加新值
        if (value != null) {
            if (t != null)
                // 插入到红黑树
                t.putTreeVal(this, tab, hash, key, value);
            else {
                // 插入到链表头部
                tab[i] = newNode(hash, key, value, first);
                // 如果链表太长，考虑树化
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K,V> result;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() { return loadFactor; }
    final int capacity() {
        return (table != null) ? table.length :
            (threshold > 0) ? threshold :
            DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                             mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                       DEFAULT_INITIAL_CAPACITY :
                       (fc >= MAXIMUM_CAPACITY) ?
                       MAXIMUM_CAPACITY :
                       tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                         (int)ft : Integer.MAX_VALUE);
            @SuppressWarnings({"rawtypes","unchecked"})
                Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                    K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                    V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    /**
     * 哈希迭代器抽象基类
     * 
     * 【中文说明】
     * HashMap所有迭代器的抽象基类。
     * 提供了迭代器的通用功能和fail-fast机制。
     * 
     * 【核心功能】
     * 1. 定位下一个节点：nextNode()方法
     * 2. 删除当前节点：remove()方法
     * 3. 检查是否有下一个节点：hasNext()方法
     * 
     * 【fail-fast机制】
     * 迭代器维护expectedModCount，在每次操作前检查：
     * - 如果modCount != expectedModCount，抛出ConcurrentModificationException
     * - 如果检测到结构性修改（添加/删除），迭代立即失效
     * 
     * 【遍历策略】
     * 1. 从表数组的第一个非空桶开始
     * 2. 遍历当前桶的链表
     * 3. 当前桶遍历完后，移动到下一个非空桶
     * 
     * 【字段说明】
     * - next: 下一个要返回的节点
     * - current: 当前节点（用于remove操作）
     * - expectedModCount: 期望的修改次数
     * - index: 当前遍历到的桶索引
     */
    abstract class HashIterator {
        Node<K,V> next;        // next entry to return - 下一个要返回的节点
        Node<K,V> current;     // current entry - 当前节点
        int expectedModCount;  // for fast-fail - 用于fail-fast检查
        int index;             // current slot - 当前遍历的桶索引

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            // 前进到第一个非空桶
            if (t != null && size > 0) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        /**
         * 检查是否还有下一个节点
         * @return 如果有下一个节点返回true
         */
        public final boolean hasNext() {
            return next != null;
        }

        /**
         * 返回下一个节点
         * 
         * 【中文说明】
         * 返回迭代器的下一个元素，并移动到其后继节点。
         * 
         * 【实现逻辑】
         * 1. 检查fail-fast条件
         * 2. 如果没有更多节点，抛出NoSuchElementException
         * 3. 保存当前节点为current
         * 4. 移动到当前节点的下一个节点
         * 5. 如果next为null，继续查找下一个非空桶
         * 
         * @return 下一个节点
         * @throws ConcurrentModificationException 如果检测到结构性修改
         * @throws NoSuchElementException 如果没有更多元素
         */
        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            // 保存当前节点并移动到下一个节点
            if ((next = (current = e).next) == null && (t = table) != null) {
                // 当前桶已遍历完，继续找下一个非空桶
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        /**
         * 移除当前节点
         * 
         * 【中文说明】
         * 移除迭代器最后返回的节点。
         * 
         * 【使用限制】
         * 1. 必须在调用next()之后调用
         * 2. 每次next()后只能调用一次remove()
         * 3. 如果当前节点为null，抛出IllegalStateException
         * 
         * 【实现原理】
         * 调用removeNode方法删除节点，并更新expectedModCount。
         * 
         * @throws IllegalStateException 如果没有当前节点
         * @throws ConcurrentModificationException 如果检测到结构性修改
         */
        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    /**
     * 键迭代器
     * 
     * 【中文说明】
     * 迭代HashMap的键的迭代器。
     * 每次迭代返回键（K类型）。
     * 
     * 【使用场景】
     * for-each循环遍历所有键：
     * for (K key : map.keySet()) { ... }
     * 
     * 【性能特点】
     * O(n)时间复杂度，n是Map的大小。
     * 遍历过程中不允许结构性修改。
     */
    final class KeyIterator extends HashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    /**
     * 值迭代器
     * 
     * 【中文说明】
     * 迭代HashMap的值的迭代器。
     * 每次迭代返回值（V类型）。
     * 
     * 【使用场景】
     * for-each循环遍历所有值：
     * for (V value : map.values()) { ... }
     * 
     * 【注意事项】
     * 值可以重复，迭代顺序与键的哈希桶相关。
     * 
     * 【性能特点】
     * O(n)时间复杂度。
     */
    final class ValueIterator extends HashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    /**
     * 条目迭代器
     * 
     * 【中文说明】
     * 迭代HashMap的键值对的迭代器。
     * 每次迭代返回Map.Entry对象。
     * 
     * 【使用场景】
     * for-each循环遍历所有键值对：
     * for (Map.Entry<K,V> entry : map.entrySet()) { ... }
     * 
     * 【Entry特性】
     * 返回的Entry是Map中实际条目的引用。
     * 可以通过setValue()修改原Map的值。
     * 
     * 【性能特点】
     * O(n)时间复杂度。
     * 提供最完整的遍历能力。
     */
    final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    /**
     * HashMap Spliterator的抽象基类
     * 
     * 【中文说明】
     * 支持并行遍历的分割迭代器。
     * 
     * 【Java 8并行流】
     * Spliterator用于支持Java 8的并行流操作。
     * 可以将数据集分割成更小的部分，支持多线程并行处理。
     * 
     * 【核心方法】
     * 1. trySplit(): 尝试分割成两部分
     * 2. forEachRemaining(): 对所有剩余元素执行操作
     * 3. estimateSize(): 估算剩余元素数量
     * 4. characteristics(): 返回Spliterator的特性标志
     * 
     * 【特性标志】
     * - SIZED: 有大小估计
     * - SUBSIZED: 分割后的部分也有大小估计
     * - NONNULL: 不包含null元素
     * - DISTINCT: 键唯一（对于KeySpliterator）
     * 
     * 【字段说明】
     * - map: 关联的HashMap
     * - current: 当前节点
     * - index: 当前索引
     * - fence: 结束边界（exclusive）
     * - est: 大小估计
     * - expectedModCount: 用于并发检查
     */
    static class HashMapSpliterator<K,V> {
        final HashMap<K,V> map;
        Node<K,V> current;          // current node - 当前节点
        int index;                  // current index, modified on advance/split - 当前索引
        int fence;                  // one past last index - 结束边界
        int est;                    // size estimate - 大小估计
        int expectedModCount;       // for comodification checks - 并发检查

        HashMapSpliterator(HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        /**
         * 获取边界值，必要时初始化
         * 
         * 【中文说明】
         * 延迟初始化边界和大小估计。
         * 首次调用时计算表的边界和大小。
         * 
         * @return 边界值
         */
        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    /**
     * 键分割迭代器
     * 
     * 【中文说明】
     * 支持并行遍历HashMap键的Spliterator。
     * 用于parallelStream()操作。
     * 
     * 【分割策略】
     * 尝试将当前范围分成两半：
     * - 如果范围足够大且当前节点为null，尝试分割
     * - 分割后的两部分可以并行处理
     * 
     * 【特性】
     * - SIZED: 有大小估计
     * - SUBSIZED: 分割后的子Spliterator也有精确大小
     * - NONNULL: 不包含null键
     * - DISTINCT: 键唯一
     */
    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        /**
         * 遍历所有剩余元素
         * 
         * 【中文说明】
         * 对Spliterator中所有剩余元素执行给定操作。
         * 
         * 【实现逻辑】
         * 1. 初始化边界和modCount
         * 2. 遍历从当前位置到结束边界的所有桶
         * 3. 对每个节点执行action.accept()
         * 4. 检查并发修改
         * 
         * @param action 要执行的操作
         * @throws NullPointerException 如果action为null
         * @throws ConcurrentModificationException 如果检测到结构性修改
         */
        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        /**
         * 尝试处理下一个元素
         * 
         * 【中文说明】
         * 如果还有剩余元素，处理一个元素并返回true。
         * 如果没有更多元素，返回false。
         * 
         * 【与forEachRemaining的区别】
         * tryAdvance每次只处理一个元素，适合提前终止的场景。
         * forEachRemaining处理所有剩余元素。
         * 
         * @param action 要执行的操作
         * @return 如果处理了元素返回true，否则返回false
         * @throws NullPointerException 如果action为null
         * @throws ConcurrentModificationException 如果检测到结构性修改
         */
        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 返回Spliterator的特性标志
         * 
         * 【中文说明】
         * 返回描述此Spliterator特性的标志值。
         * 
         * 【特性说明】
         * - SIZED: 有大小估计（fence < 0 或 est == map.size）
         * - DISTINCT: 键都是唯一的
         * 
         * @return 特性标志的组合值
         */
        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /**
     * 值分割迭代器
     * 
     * 【中文说明】
     * 支持并行遍历HashMap值的Spliterator。
     * 用于map.values().parallelStream()操作。
     * 
     * 【与KeySpliterator的区别】
     * 1. 每次处理的是值而不是键
     * 2. 没有DISTINCT特性（值可以重复）
     * 3. 实现与KeySpliterator类似
     * 
     * 【特性】
     * - SIZED: 有大小估计
     * - SUBSIZED: 分割后的子Spliterator也有精确大小
     * - NONNULL: 不包含null值
     */
    static final class ValueSpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        /**
         * 尝试分割成两部分
         * @return 新的ValueSpliterator，或null（如果无法分割）
         */
        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        /**
         * 遍历所有剩余元素
         * @param action 要执行的操作
         */
        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        /**
         * 尝试处理下一个元素
         * @param action 要执行的操作
         * @return 如果处理了元素返回true，否则返回false
         */
        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 返回Spliterator的特性标志
         * @return 特性标志的组合值
         */
        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    /**
     * 条目分割迭代器
     * 
     * 【中文说明】
     * 支持并行遍历HashMap键值对的Spliterator。
     * 用于map.entrySet().parallelStream()操作。
     * 
     * 【特性】
     * - SIZED: 有大小估计
     * - SUBSIZED: 分割后的子Spliterator也有精确大小
     * - NONNULL: 不包含null条目
     */
    static final class EntrySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    /**
     * 创建普通节点
     * 
     * 【中文说明】
     * 创建并返回一个新的普通链表节点。
     * 
     * 【设计目的】
     * 1. 为HashMap提供节点创建的统一入口
     * 2. 允许LinkedHashMap通过重写此方法创建带有访问顺序链接的节点
     * 
     * 【调用场景】
     * 在putVal方法插入新节点时调用。
     * 
     * @param hash 键的哈希码
     * @param key 键
     * @param value 值
     * @param next 下一个节点的引用
     * @return 新创建的节点
     */
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
        return new Node<>(hash, key, value, next);
    }

    /**
     * 将树节点转换为普通节点
     * 
     * 【中文说明】
     * 在树退化为链表时，将TreeNode转换为普通Node。
     * 
     * 【使用场景】
     * 1. untreeify方法中调用
     * 2. 树节点拆分后，某些节点需要转回普通节点
     * 
     * 【实现逻辑】
     * 保留原节点的hash、key、value，但将next指针设置为新值。
     * 
     * @param p 原始的树节点
     * @param next 下一个节点的引用
     * @return 转换后的普通节点
     */
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    /**
     * 创建树节点
     * 
     * 【中文说明】
     * 创建并返回一个新的树节点（TreeNode）。
     * 
     * 【设计目的】
     * 为HashMap提供树节点创建的统一入口。
     * 允许LinkedHashMap通过重写此方法创建带有额外特性的树节点。
     * 
     * 【调用场景】
     * 在treeifyBin或putTreeVal方法中调用。
     * 
     * @param hash 键的哈希码
     * @param key 键
     * @param value 值
     * @param next 下一个节点的引用
     * @return 新创建的树节点
     */
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    /**
     * 创建树节点用于treeifyBin
     * 
     * 【中文说明】
     * 将普通节点转换为树节点，用于链表转红黑树的场景。
     * 
     * 【与newTreeNode的区别】
     * replacementTreeNode用于将已有节点转换为树节点（保留原数据）。
     * newTreeNode用于创建全新的树节点。
     * 
     * @param p 原始的普通节点
     * @param next 下一个节点的引用
     * @return 新创建的树节点
     */
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * 重置为初始默认状态
     * 
     * 【中文说明】
     * 将HashMap的所有字段重置为初始状态。
     * 
     * 【调用场景】
     * 1. clone方法中：克隆后重置状态
     * 2. readObject方法中：反序列化后重置状态
     * 
     * 【重置的字段】
     * - table: 设为null
     * - entrySet/keySet/values: 设为null（延迟初始化）
     * - modCount: 设为0
     * - threshold: 设为0
     * - size: 设为0
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    /**
     * 节点访问后的回调
     * 
     * 【中文说明】
     * 在节点被访问（读取值）后调用的钩子方法。
     * 
     * 【设计目的】
     * 允许LinkedHashMap实现LRU（最近最少使用）缓存策略。
     * 当启用accessOrder模式时，访问节点后将其移到链表末尾。
     * 
     * 【LinkedHashMap的实现】
     * LinkedHashMap会重写此方法，将被访问的节点移到双向链表的尾部。
     * 
     * 【默认实现】
     * HashMap中为空实现，不执行任何操作。
     * 
     * @param p 被访问的节点
     */
    void afterNodeAccess(Node<K,V> p) { }
    
    /**
     * 节点插入后的回调
     * 
     * 【中文说明】
     * 在新节点插入后调用的钩子方法。
     * 
     * 【设计目的】
     * 允许LinkedHashMap实现LRU策略或维护插入顺序。
     * 
     * 【LinkedHashMap的实现】
     * LinkedHashMap会重写此方法，检查是否需要删除最老的节点（当超出容量时）。
     * 
     * 【参数说明】
     * @param evict true表示正常插入操作，false表示构造函数中的初始化插入
     *              构造函数中为false，避免在初始化时就触发移除逻辑
     * 
     * 【默认实现】
     * HashMap中为空实现，不执行任何操作。
     */
    void afterNodeInsertion(boolean evict) { }
    
    /**
     * 节点移除后的回调
     * 
     * 【中文说明】
     * 在节点被移除后调用的钩子方法。
     * 
     * 【设计目的】
     * 允许LinkedHashMap维护双向链表的完整性。
     * 
     * 【LinkedHashMap的实现】
     * LinkedHashMap会重写此方法，从双向链表中移除该节点。
     * 
     * 【调用时机】
     * 在removeNode方法中，节点被成功移除后调用。
     * 
     * @param p 被移除的节点
     */
    void afterNodeRemoval(Node<K,V> p) { }

    /**
     * 内部写入条目到序列化流
     * 
     * 【中文说明】
     * 将HashMap中的所有键值对写入序列化流。
     * 
     * 【调用场景】
     * 仅从writeObject方法调用，用于自定义序列化。
     * 
     * 【实现逻辑】
     * 遍历表中的每个桶，再遍历桶中的每个节点，
     * 将每个节点的key和value写入流。
     * 
     * 【与直接序列化的区别】
     * 不直接序列化整个table，而是逐个序列化条目。
     * 这样可以支持不同实现的Map之间的兼容性。
     * 
     * @param s 对象输出流
     * @throws IOException 如果写入过程中发生IO错误
     */
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * 红黑树节点
     * 
     * 【中文说明】
     * HashMap中用于红黑树的节点类。
     * 继承自LinkedHashMap.Entry，可以同时作为链表节点和树节点使用。
     * 
     * 【继承关系】
     * TreeNode -> LinkedHashMap.Entry -> Node -> Map.Entry
     * 
     * 【设计特点】
     * 1. 双向链表能力：继承自LinkedHashMap.Entry，具有before和after指针
     * 2. 红黑树能力：具有parent、left、right、prev指针和red布尔值
     * 3. prev指针用于在删除时快速解链（unlink next）
     * 
     * 【与普通Node的区别】
     * 1. 普通Node只有next指针（单向链表）
     * 2. TreeNode有left、right、parent指针（红黑树）
     * 3. TreeNode继承自LinkedHashMap.Entry，还有before、after指针（双向链表）
     * 
     * 【使用场景】
     * 当桶中的节点数量超过TREEIFY_THRESHOLD（8）时，
     * 链表会转换为红黑树以提高查找性能。
     * 
     * 【红黑树特性】
     * - 近似平衡的二叉搜索树
     * - 查找、插入、删除操作的时间复杂度为O(log n)
     * - 通过颜色旋转保持平衡
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links - 父节点指针
        TreeNode<K,V> left;    // 左子节点指针
        TreeNode<K,V> right;   // 右子节点指针
        TreeNode<K,V> prev;    // needed to unlink next upon deletion - 前驱节点指针（删除时解链用）
        boolean red;           // 节点颜色（红色/黑色）
        
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * 返回包含此节点的树的根节点
         * 
         * 【中文说明】
         * 通过父节点指针向上追溯，直到找到根节点。
         * 
         * 【实现逻辑】
         * 从当前节点开始，不断将父节点赋值给当前节点，
         * 直到父节点为null，此时当前节点就是根节点。
         * 
         * @return 根节点
         */
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * 确保给定的根节点是其桶的第一个节点
         * 
         * 【中文说明】
         * 将红黑树的根节点移动到桶数组的开头。
         * 同时维护双向链表（LinkedHashMap）的结构。
         * 
         * 【实现逻辑】
         * 1. 计算根节点在桶数组中的索引
         * 2. 如果根节点不在该位置，将其移到该位置
         * 3. 更新相关节点的next和prev指针
         * 4. 验证红黑树的不变式
         * 
         * 【设计目的】
         * 确保根节点总是桶数组中的第一个元素，
         * 便于快速访问红黑树的根节点。
         * 
         * @param tab 桶数组
         * @param root 要移动到桶开头的根节点
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * 从根节点开始查找具有给定哈希和键的节点。
         * kc参数缓存comparableClassFor(key)的结果，用于首次比较键时。
         * 
         * 【中文说明】
         * 在红黑树中递归查找具有指定哈希值和键的节点。
         * 这是TreeNode实现高效查找的核心方法。
         * 
         * 【查找策略】
         * 1. 比较当前节点的哈希值与目标哈希值，决定向左还是向右搜索
         * 2. 如果哈希值相等，比较键是否相等（先比较引用，再比较equals）
         * 3. 如果键不相等，递归搜索右子树
         * 4. 如果右子树也没有找到，最后搜索左子树
         * 
         * 【性能特点】
         * O(log n)时间复杂度，因为红黑树是平衡的。
         * 
         * 【与链表查找的区别】
         * 链表查找是O(n)，红黑树查找是O(log n)。
         * 当桶中节点过多时，红黑树提供更好的查找性能。
         * 
         * 【参数说明】
         * @param h 目标键的哈希值
         * @param k 目标键
         * @param kc 键的可比较类缓存，避免重复计算
         * @return 找到的节点，如果没找到返回null
         */
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * 从根节点调用find方法查找节点。
         * 
         * 【中文说明】
         * 这是一个便捷方法，首先找到红黑树的根节点，
         * 然后从根节点开始查找指定哈希值和键的节点。
         * 
         * 【实现逻辑】
         * 1. 如果当前节点有父节点，调用root()找到根节点
         * 2. 否则当前节点本身就是根节点
         * 3. 从根节点调用find方法查找
         * 
         * 【使用场景】
         * 当我们知道某个键对应的桶，但需要找到具体节点时调用。
         * 
         * @param h 目标键的哈希值
         * @param k 目标键
         * @return 找到的节点，如果没找到返回null
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * 当哈希值相等且键不可比较时，用于打破平局的工具方法。
         * 
         * 【中文说明】
         * 在插入节点时，如果两个节点的哈希值相等，
         * 并且它们的键类型没有实现Comparable接口，
         * 需要使用此方法来确定节点的插入顺序。
         * 
         * 【打破平局的策略】
         * 1. 首先比较键的类名（通过getClass().getName()）
         * 2. 如果类名也不同，使用System.identityHashCode()比较
         * 3. 返回-1或1确定左右子树
         * 
         * 【设计目的】
         * 确保在哈希冲突且键不可比较的情况下，
         * 红黑树仍然可以保持一致的插入规则，
         * 从而在重新平衡时保持等价性。
         * 
         * 【为什么需要】
         * 红黑树是有序结构，需要能够比较节点的大小。
         * 当哈希值相等且键不可比较时，必须有一种确定性的方式来决定顺序。
         * 
         * @param a 第一个对象
         * @param b 第二个对象
         * @return 如果a应该在b左边返回负数，否则返回正数
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }

        /**
         * 将以当前节点为头的链表转换为红黑树。
         * 
         * 【中文说明】
         * 这是HashMap中链表转红黑树的核心方法。
         * 将链表中的所有节点转换为一个红黑树。
         * 
         * 【实现步骤】
         * 1. 首先找到或创建根节点（第一个节点作为根，设为黑色）
         * 2. 遍历链表中的其余节点
         * 3. 对每个节点，使用二叉搜索树的插入方式找到插入位置
         * 4. 使用tieBreakOrder处理哈希冲突的情况
         * 5. 插入后调用balanceInsertion保持红黑树性质
         * 6. 最后调用moveRootToFront确保根节点在桶数组的开头
         * 
         * 【为什么需要转换】
         * 链表在节点数多时查找性能是O(n)，
         * 红黑树可以提供O(log n)的查找性能。
         * 
         * 【treeify与treeifyBin的区别】
         * treeifyBin是入口方法，检查容量是否足够；
         * treeify是实际执行链表转红黑树的方法。
         * 
         * @param tab 桶数组
         * @return 树的根节点
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * 将红黑树转换回链表。
         * 
         * 【中文说明】
         * 当红黑树中的节点数过少时，将其转换回普通链表。
         * 这是treeify的逆操作。
         * 
         * 【触发场景】
         * 1. 扩容拆分后，某组的节点数 <= UNTREEIFY_THRESHOLD(6)
         * 2. 树节点删除后，节点数过少
         * 
         * 【实现逻辑】
         * 遍历树中的所有节点，创建普通Node节点并链接成链表。
         * 使用replacementNode方法将TreeNode转换为Node。
         * 
         * 【与untreeify的区别】
         * untreeify是实例方法，从当前节点开始遍历；
         * replacementNode是静态方法，负责单个节点的转换。
         * 
         * 【性能特点】
         * O(n)时间复杂度，n是树中的节点数。
         * 
         * @param map HashMap引用，用于创建普通节点
         * @return 转换后的链表头节点
         */
        final Node<K,V> untreeify(HashMap<K,V> map) {
            Node<K,V> hd = null, tl = null;
            for (Node<K,V> q = this; q != null; q = q.next) {
                Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * 在红黑树中插入节点（TreeNode版本的putVal）。
         * 
         * 【中文说明】
         * TreeNode版本的putVal方法，用于在红黑树中插入或更新节点。
         * 
         * 【实现步骤】
         * 1. 从根节点开始搜索
         * 2. 如果找到相同键的节点，返回该节点（更新值）
         * 3. 如果没找到，插入新节点到合适位置
         * 4. 调用balanceInsertion保持红黑树性质
         * 5. 调用moveRootToFront确保根节点在桶开头
         * 
         * 【搜索策略】
         * 1. 比较哈希值决定方向
         * 2. 如果哈希值相等，尝试使用Comparable比较
         * 3. 如果仍无法比较，使用tieBreakOrder
         * 4. 如果子树为空，插入新节点
         * 5. 如果子树不为空，继续递归搜索
         * 
         * 【重复搜索优化】
         * 如果遇到相等的哈希值且不可比较，先搜索左右子树一次。
         * 如果找到了就返回，没找到再使用tieBreakOrder。
         * 避免在tieBreakOrder返回0的情况下重复搜索。
         * 
         * 【参数说明】
         * @param map HashMap引用
         * @param tab 桶数组
         * @param h 键的哈希值
         * @param k 键
         * @param v 值
         * @return 如果键已存在返回原节点，否则返回null
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K,V> root = (parent != null) ? root() : this;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * 移除红黑树中的节点。
         * 
         * 【中文说明】
         * TreeNode版本的节点删除方法。
         * 这是HashMap中最复杂的方法之一，涉及红黑树的删除操作。
         * 
         * 【删除的复杂性】
         * 红黑树删除比插入复杂得多：
         * 1. 如果删除的是红色节点，直接移除即可
         * 2. 如果删除的是黑色节点，需要修复黑色高度
         * 3. 由于有next指针的存在，不能简单地交换节点内容
         * 
         * 【实现步骤】
         * 1. 从链表中移除节点（更新prev和next指针）
         * 2. 如果节点有两个子节点，找到后继节点交换位置
         * 3. 删除节点，如果需要则平衡红黑树
         * 4. 如果树太小，转换回链表
         * 5. 如果需要，移动根节点到桶开头
         * 
         * 【参数说明】
         * @param map HashMap引用
         * @param tab 桶数组
         * @param movable 是否需要移动根节点到桶开头
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null || root.right == null ||
                (rl = root.left) == null || rl.left == null) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode<K,V> sr = s.right;
                TreeNode<K,V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            }
            else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map the map
         * @param tab the table for recording bin heads
         * @param index the index of the table being split
         * @param bit the bit of hash to split on
         * 
         * 【中文说明】
         * 将树桶中的节点拆分为低位和高位两个树桶，
         * 如果节点太少则转换回链表。
         * 此方法仅在resize时调用。
         * 
         * 【拆分原理】
         * 在扩容时，需要将原桶中的节点重新分配到新表中。
         * 对于红黑树中的节点，根据hash的第bit位来决定位置：
         * - (e.hash & bit) == 0：保持原索引（低位桶）
         * - (e.hash & bit) != 0：移动到 index + bit 的位置（高位桶）
         * 
         * 【实现步骤】
         * 1. 遍历原树中的所有节点
         * 2. 根据hash的bit位将节点分成两组（lo和hi）
         * 3. 如果某组的节点数 <= UNTREEIFY_THRESHOLD(6)，转换回链表
         * 4. 否则，重新树化该组节点
         * 
         * 【为什么需要拆分】
         * 扩容后，原来的红黑树可能需要重新组织：
         * - 某些节点可能需要移动到新位置
         * - 如果节点太少，树化开销不值得，转为链表更高效
         * 
         * @param map HashMap引用
         * @param tab 表数组，用于记录桶的头节点
         * @param index 被拆分的桶的索引
         * @param bit 用于拆分的哈希位（通常是旧容量）
         */
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            TreeNode<K,V> b = this;
            // 重新链接到lo和hi列表，保持顺序
            TreeNode<K,V> loHead = null, loTail = null;
            TreeNode<K,V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;  // loCount, hiCount
            
            // 遍历原树中的所有节点
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (TreeNode<K,V>)e.next;
                e.next = null;
                
                // 根据hash的第bit位拆分
                if ((e.hash & bit) == 0) {
                    // 加入低位桶（保持原索引）
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                }
                else {
                    // 加入高位桶（移动到 index + bit）
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            // 处理低位桶
            if (loHead != null) {
                // 节点太少，转为链表
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    // 如果高位桶也有节点，需要重新树化
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            
            // 处理高位桶
            if (hiHead != null) {
                // 节点太少，转为链表
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    // 如果低位桶也有节点，需要重新树化
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        /**
         * 左旋操作
         * 
         * 【中文说明】
         * 对红黑树执行左旋操作。
         * 左旋以p为中心，将其右子节点r提升到p的位置，
         * p成为r的左子节点。
         * 
         * 【旋转变换】
         * 旋转前：
         *       pp
         *       |
         *       p
         *      / \
         *     l   r
         *        / \
         *       rl  rr
         * 
         * 旋转后：
         *       pp
         *       |
         *       r
         *      / \
         *     p   rr
         *    / \
         *   l   rl
         * 
         * 【应用场景】
         * 1. 插入节点后修复红黑树性质
         * 2. 删除节点后修复红黑树性质
         * 
         * @param root 红黑树的根节点
         * @param p 要左旋的节点
         * @return 旋转后的新根节点
         */
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                // 将r的左子节点rl设为p的右子节点
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                // 将r的父节点设为p的父节点
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;  // r成为新根，设为黑色
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                // 将p设为r的左子节点
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        /**
         * 右旋操作
         * 
         * 【中文说明】
         * 对红黑树执行右旋操作。
         * 右旋以p为中心，将其左子节点l提升到p的位置，
         * p成为l的右子节点。
         * 
         * 【旋转变换】
         * 旋转前：
         *       pp
         *       |
         *       p
         *      / \
         *     l   r
         *    / \
         *   ll  lr
         * 
         * 旋转后：
         *       pp
         *       |
         *       l
         *      / \
         *     ll  p
         *        / \
         *       lr  r
         * 
         * 【与左旋的关系】
         * 右旋是左旋的镜像操作。
         * 在修复红黑树性质时，左旋处理右倾，右旋处理左倾。
         * 
         * @param root 红黑树的根节点
         * @param p 要右旋的节点
         * @return 旋转后的新根节点
         */
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                // 将l的右子节点lr设为p的左子节点
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                // 将l的父节点设为p的父节点
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;  // l成为新根，设为黑色
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                // 将p设为l的右子节点
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        /**
         * 平衡插入操作
         * 
         * 【中文说明】
         * 在红黑树中插入节点后，执行平衡操作以维护红黑树性质。
         * 
         * 【红黑树性质】
         * 1. 节点是红色或黑色
         * 2. 根节点是黑色
         * 3. 所有叶子节点（NIL）是黑色
         * 4. 红色节点的子节点必须是黑色
         * 5. 从任一节点到其每个叶子的路径上黑色节点数量相同
         * 
         * 【插入后的修复策略】
         * 1. 新节点初始为红色
         * 2. 如果父节点是黑色，不需要修复
         * 3. 如果父节点是红色（违反了性质4），需要修复：
         *    a. 叔节点是红色：变色 + 递归向上处理
         *    b. 叔节点是黑色，且新节点是内孙：左右旋
         *    c. 叔节点是黑色，且新节点是外孙：变色 + 右旋
         * 
         * 【变色和旋转】
         * 变色：将红色变黑色，黑色变红色
         * 旋转：左旋或右旋调整树结构
         * 
         * @param root 红黑树的根节点
         * @param x 新插入的节点
         * @return 修复后的根节点
         */
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;  // 新节点初始为红色
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                // 情况1：x是根节点，设为黑色并返回
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                // 情况2：父节点是黑色，无需修复
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                
                // x的父节点是红色，且祖父节点存在
                if (xp == (xppl = xpp.left)) {
                    // x的父节点是祖父节点的左子节点
                    if ((xppr = xpp.right) != null && xppr.red) {
                        // 叔节点是红色：变色
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;  // 向上继续处理
                    }
                    else {
                        // 叔节点是黑色或不存在
                        if (x == xp.right) {
                            // 内孙情况：先左旋
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;  // 父节点变黑
                            if (xpp != null) {
                                xpp.red = true;  // 祖父节点变红
                                root = rotateRight(root, xpp);  // 右旋
                            }
                        }
                    }
                }
                else {
                    // x的父节点是祖父节点的右子节点（与上面镜像）
                    if (xppl != null && xppl.red) {
                        // 叔节点是红色：变色
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;  // 向上继续处理
                    }
                    else {
                        // 叔节点是黑色或不存在
                        if (x == xp.left) {
                            // 内孙情况：先右旋
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;  // 父节点变黑
                            if (xpp != null) {
                                xpp.red = true;  // 祖父节点变红
                                root = rotateLeft(root, xpp);  // 左旋
                            }
                        }
                    }
                }
            }
        }

        /**
         * 平衡删除操作
         * 
         * 【中文说明】
         * 在红黑树中删除节点后，执行平衡操作以维护红黑树性质。
         * 
         * 【删除的复杂性】
         * 删除操作比插入更复杂，因为：
         * 1. 如果删除的是红色节点，不会影响黑色高度，无需修复
         * 2. 如果删除的是黑色节点，会导致某条路径的黑色高度减少1，需要修复
         * 
         * 【删除后的修复策略】
         * 设x是替代被删除节点的节点（可能为null）
         * 1. 如果x是红色或根节点，直接设为黑色
         * 2. 如果x是黑色非根节点，需要修复：
         *    - 兄弟节点是红色：变色 + 旋转
         *    - 兄弟节点是黑色，且兄弟的子节点都是黑色：兄弟变红，向上递归
         *    - 兄弟节点的远侄是黑色，近侄是红色：旋转 + 变色
         *    - 兄弟节点的近侄是黑色，远侄是红色：变色 + 旋转
         * 
         * 【参数说明】
         * @param root 红黑树的根节点
         * @param x 替代被删除节点的节点
         * @return 修复后的根节点
         */
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                // 终止条件：x是根节点或x为null
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    // x是根节点，设为黑色
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                    // x是红色，直接变黑（补回黑色高度）
                    x.red = false;
                    return root;
                }
                else if ((xpl = xp.left) == x) {
                    // x是父节点的左子节点
                    if ((xpr = xp.right) != null && xpr.red) {
                        // 兄弟节点是红色：变色 + 左旋
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        // 兄弟节点是黑色且没有子节点，向上递归
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            // 兄弟节点的子节点都是黑色：兄弟变红
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                // 远侄是黑色，近侄是红色：右旋 + 变色
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }
                            if (xpr != null) {
                                // 远侄是红色：变色 + 左旋
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    // x是父节点的右子节点（与上面镜像）
                    if (xpl != null && xpl.red) {
                        // 兄弟节点是红色：变色 + 右旋
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        // 兄弟节点是黑色且没有子节点，向上递归
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {
                            // 兄弟节点的子节点都是黑色：兄弟变红
                            xpl.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                // 远侄是黑色，近侄是红色：左旋 + 变色
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                    null : xp.left;
                            }
                            if (xpl != null) {
                                // 远侄是红色：变色 + 右旋
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * 递归不变式检查
         * 
         * 【中文说明】
         * 递归检查红黑树的不变式，确保树结构正确。
         * 此方法主要用于调试和测试，验证红黑树的性质是否被维护。
         * 
         * 【检查的不变式】
         * 1. 双向链表指针正确（prev和next互相引用）
         * 2. 父指针正确（父节点的左右子节点包含自己）
         * 3. 哈希码有序（左子节点hash < 父节点hash < 右子节点hash）
         * 4. 没有连续的红色节点
         * 5. 递归检查左右子树
         * 
         * 【应用场景】
         * 1. 在moveRootToFront中调用，确保树操作后树仍然有效
         * 2. 调试时检测红黑树是否被意外破坏
         * 
         * 【性能特点】
         * O(n)时间复杂度，遍历所有节点。
         * 由于有assert调用，仅在断言启用时执行。
         * 
         * @param t 要检查的树节点
         * @return 如果所有不变式都满足返回true，否则返回false
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;
            
            // 检查双向链表指针
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            
            // 检查父指针
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            
            // 检查哈希码有序性
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            
            // 检查没有连续的红色节点
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            
            // 递归检查子树
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            
            return true;
        }
    }

}
