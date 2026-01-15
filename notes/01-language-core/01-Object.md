# Object 类源码深度解析

## 一、类的概述

### 1.1 基本信息

`Object` 类是 Java 语言中所有类的根类，是整个 Java 对象模型的基础。在 Java 虚拟机启动时，会创建一个特殊的 `root` ClassLoader 来加载 `Object` 类，确保所有对象都能继承自 `Object`。

**源码位置**：`src/main/jdk8/java/lang/Object.java`

**类的声明**：

```java
public class Object
```

**类的定位**：

- `Object` 类是 Java 继承体系中的根节点
- 所有 Java 类（包括数组）都直接或者间接继承自 `Object`
- `Object` 类定义了所有对象都应该具备的基本行为
- 在 Java 中，不存在不属于 `Object` 子类的对象

**类的职责**：

`Object` 类承担了以下核心职责：

- 提供对象的身份标识和行为基础（`getClass()`、`hashCode()`、`equals()`）
- 支持对象的克隆和字符串表示（`clone()`、`toString()`）
- 提供线程间的通信机制（`wait()`、`notify()`、`notifyAll()`）
- 支持对象的垃圾回收回调（`finalize()`）

### 1.2 设计理念

`Object` 类的设计体现了以下核心理念：

**1. 简单性原则**

`Object` 类只包含最基本、最通用的方法，没有引入复杂的依赖。这种简洁的设计使得所有对象都能轻松继承这些基础能力，而不会产生不必要的开销。

**2. 可扩展性原则**

虽然 `Object` 提供了一些基础实现（如 `equals()` 的默认实现是比较引用地址），但它鼓励子类根据需要进行重写。例如，大多数类都会重写 `toString()` 方法以提供更有意义的对象描述。

**3. 多线程支持**

`Object` 类内置了完整的线程通信机制（`wait/notify`），这是 Java 实现线程协作的基础。这种设计将线程通信与对象状态紧密绑定，简化了多线程编程的复杂度。

**4. 安全性考虑**

`Object` 类的一些方法被设计为 `final`（如 `getClass()`、`notify()`、`notifyAll()`、`wait()` 的重载版本），防止子类破坏核心行为。同时，`clone()` 方法被设计为 `protected`，要求子类显式实现 `Cloneable` 接口才能使用。

### 1.3 版本信息

- **JDK 版本**：JDK 1.0
- **作者**：unascribed（未署名）
- **版权声明**：Oracle Proprietary and Confidential

## 二、类的继承结构

### 2.1 UML 类图

```
┌─────────────────────────────────────┐
│           java.lang.Object          │
├─────────────────────────────────────┤
│ + getClass(): Class<?>              │
│ + hashCode(): int                   │
│ + equals(Object): boolean           │
│ + clone(): Object                   │
│ + toString(): String                │
│ + notify(): void                    │
│ + notifyAll(): void                 │
│ + wait(long): void                  │
│ + wait(long, int): void             │
│ + wait(): void                      │
│ + finalize(): void                  │
├─────────────────────────────────────┤
│ ^ 所有类的超类                       │
└─────────────────────────────────────┘
           △
           │
           │ extends
           │
┌─────────────────────────────────────┐
│        java.lang.String             │
│        java.lang.Integer            │
│        java.lang.Thread             │
│        java.util.ArrayList          │
│        java.util.HashMap            │
│        ...                          │
│        自定义的所有类                │
└─────────────────────────────────────┘
```

### 2.2 与 Object 直接相关的类和接口

**直接继承 Object 的类**：

- `java.lang.String`：字符串类，重写了 `equals()`、`hashCode()`、`toString()` 方法
- `java.lang.Thread`：线程类，重写了 `equals()`、`toString()` 方法
- `java.lang.Class`：类对象，是 `getClass()` 方法的返回类型
- `java.lang.Throwable`：异常和错误的基类
- 所有数组类型（`int[]`、`Object[]` 等）

**实现 Object 方法的接口**：

- `java.lang.Cloneable`：标记接口，表示对象可以被克隆
- `java.io.Serializable`：标记接口，表示对象可以被序列化

**与 Object 密切相关的类**：

- `java.lang.System`：提供了 `identityHashCode()` 方法，返回与 `hashCode()` 相同但基于对象内存地址的值
- `java.lang.ref.Reference`：Java 引用类型的基类

### 2.3 继承关系图示

```
java.lang.Object
    │
    ├── java.lang.String
    │       └── java.lang.StringBuffer (synchronized)
    │       └── java.lang.StringBuilder
    │
    ├── java.lang.Number
    │       ├── java.lang.Byte
    │       ├── java.lang.Short
    │       ├── java.lang.Integer
    │       ├── java.lang.Long
    │       ├── java.lang.Float
    │       └── java.lang.Double
    │
    ├── java.lang.Thread
    │
    ├── java.lang.Throwable
    │       ├── java.lang.Error
    │       └── java.lang.Exception
    │
    ├── java.util.Collection
    │       ├── java.util.List
    │       │       ├── java.util.ArrayList
    │       │       └── java.util.LinkedList
    │       └── java.util.Set
    │               ├── java.util.HashSet
    │               └── java.util.TreeSet
    │
    ├── java.util.Map
    │       ├── java.util.HashMap
    │       └── java.util.TreeMap
    │
    ├── 数组类型
    │       ├── int[]
    │       ├── Object[]
    │       └── ...
    │
    └── 自定义类（默认继承 Object）
```

## 三、核心字段解析

### 3.1 私有字段

`Object` 类本身没有任何公开的实例字段，这体现了信息隐藏的设计原则。对象的实际数据由子类定义，`Object` 只提供行为。

**唯一字段**：

```java
private static native void registerNatives();
```

这是一个私有静态方法，不是字段，用于注册本地方法。虽然不是字段，但它是 `Object` 类中唯一与 JVM 底层交互的入口。

### 3.2 静态初始化块

```java
static {
    registerNatives();
}
```

**设计意图**：

- 在类加载阶段自动调用 `registerNatives()` 方法
- 确保 `Object` 类的本地方法在类初始化时就完成注册
- 这些本地方法实现了 `hashCode()`、`clone()`、`notify()`、`notifyAll()`、`wait()` 等核心方法

## 四、构造方法分析

### 4.1 构造方法的缺失

`Object` 类没有显式定义构造方法，这可能导致一个误解。实际原因是：

**1. 默认构造方法的自动生成**

当类没有定义任何构造方法时，Java 编译器会自动生成一个默认的无参构造方法。对于 `Object` 类，这个自动生成的构造方法如下：

```java
public Object() {
    // Object 类没有父类（除了隐式的 java.lang.Object）
    // 没有任何显式的初始化代码
}
```

**2. 为什么 Object 不需要自定义构造方法**

`Object` 类没有任何实例字段需要初始化，因此默认构造方法已经足够。设计者选择不显式定义构造方法，体现了极简主义的设计理念。

### 4.2 初始化过程

当创建一个新的 Java 对象时，`Object` 的构造方法调用过程如下：

```java
// 创建自定义对象时
public class Person {
    private String name;
    
    public Person(String name) {
        this.name = name;
    }
}

// 实例化过程
Person p = new Person("张三");
```

**调用链**：

1. `Person(String)` 构造方法被调用
2. 隐式调用 `super()`，即 `Object()` 构造方法
3. `Object` 构造方法执行（无操作）
4. 执行 `Person` 构造方法的剩余代码
5. 字段初始化

## 五、核心方法详解

### 5.1 getClass() 方法

```java
public final native Class<?> getClass();
```

**方法签名**：

- `public`：所有对象都可以访问
- `final`：不能被子类重写，确保返回的 `Class` 对象是运行时类型
- `native`：由 JVM 实现，是本地方法
- 返回值：`Class<?>` 类型，表示对象的运行时类

**方法功能**：

`getClass()` 方法返回对象的运行时类（Runtime Class），这个 `Class` 对象包含了对象的完整类型信息。

**重要特性**：

1. **返回精确类型**：`getClass()` 返回的是对象的确切类型，而不是声明类型。例如：

```java
Object obj = new String("hello");
Class<?> clazz = obj.getClass();  // 返回 String.class，而不是 Object.class
```

2. **final 修饰的原因**：将 `getClass()` 声明为 `final` 是为了确保多态不会影响类型信息的获取。如果允许重写，可能会返回错误的类型信息。

3. **与类字面量的区别**：

```java
// 声明类型
Number n = 123;

// 使用 getClass() 获取运行时类型
Class<?> runtimeClass = n.getClass();  // Integer.class

// 使用类字面量获取声明类型
Class<?> declaredClass = Number.class; // Number.class

// instanceof 检查
boolean isInteger = n instanceof Integer;  // true
```

**使用场景**：

- 反射编程中获取对象类型信息
- 类型检查和类型转换
- 动态创建对象和调用方法

**与静态类型检查的对比**：

```java
public void process(Object obj) {
    // 动态获取类型
    Class<?> clazz = obj.getClass();
    
    // 静态 instanceof 检查
    if (obj instanceof String) {
        String str = (String) obj;
        // 处理 String
    }
}
```

### 5.2 hashCode() 方法

```java
Code();
```

**public native int hash方法功能**：

`hashCode()` 方法返回对象的哈希码值，用于支持哈希表（如 `HashMap`、`HashSet`、`Hashtable` 等）。

**hashCode 的契约**：

`Object` 类的 `hashCode()` 方法必须遵循以下契约：

**契约一：一致性**

> 在同一个应用程序执行期间，对同一对象多次调用 `hashCode()` 方法，必须返回相同的整数，前提是 `equals()` 比较中所用的信息没有被修改。

```java
String s = "test";
int hash1 = s.hashCode();
int hash2 = s.hashCode();
// hash1 必须等于 hash2
```

**契约二：相等对象的哈希码相同**

> 如果两个对象根据 `equals(Object)` 方法比较是相等的，那么这两个对象的 `hashCode()` 方法必须返回相同的整数。

```java
String s1 = new String("test");
String s2 = new String("test");
boolean isEqual = s1.equals(s2);  // true
int hash1 = s1.hashCode();
int hash2 = s2.hashCode();
// hash1 必须等于 hash2
```

**契约三：不相等对象的哈希码可以不同（但建议不同）**

> 如果两个对象根据 `equals(Object)` 方法比较是不相等的，不要求它们的 `hashCode()` 方法返回不同的整数。但是，程序员应该意识到，为不相等的对象生成不同的哈希码可以提高哈希表的性能。

```java
String s1 = "Aa";
String s2 = "BB";
// s1.hashCode() 可能等于 s2.hashCode()（哈希冲突）
// 但设计良好的哈希函数应该尽量避免这种情况
```

**默认实现原理**：

在 HotSpot JVM 中，`Object` 的默认 `hashCode()` 实现并不是直接将对象地址转换为整数，而是使用一种称为"散列码生成器"的机制：

```c
// JVM 内部的实现思路（非实际代码）
int hash_code = hash_code_generator.next();
```

这种方法的好处是：
- 即使对象被移动（GC 压缩），哈希码也能保持不变
- 提供了更好的随机性，减少哈希冲突

**与 System.identityHashCode() 的区别**：

```java
Object obj = new Object();

// 使用 hashCode() 方法
int hash1 = obj.hashCode();

// 使用 System.identityHashCode()
int hash2 = System.identityHashCode(obj);

// 对于 Object 类，两者通常相同
// 但对于 String 等重写了 hashCode() 的类，两者不同
```

**重写 hashCode() 的原则**：

当重写 `equals()` 方法时，必须同时重写 `hashCode()` 方法，以保持契约：

```java
public class Person {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

### 5.3 equals() 方法

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

**方法功能**：

`equals()` 方法用于判断另一个对象与当前对象是否"相等"。

**Object 默认实现**：

`Object` 类的默认 `equals()` 实现非常简单，只是比较两个对象的引用地址（即判断是否为同一个对象）：

```java
Object obj1 = new Object();
Object obj2 = new Object();
Object obj3 = obj1;

boolean result1 = obj1.equals(obj2);  // false，不同的对象
boolean result2 = obj1.equals(obj3);  // true，同一个对象
boolean result3 = (obj1 == obj3);     // true，引用比较
```

**equals() 方法的五大特性**：

`equals()` 方法必须满足以下五大特性：

**1. 自反性（Reflexive）**

> 对于任何非空引用值 x，`x.equals(x)` 必须返回 true。

```java
String s = "test";
boolean result = s.equals(s);  // 总是 true
```

**2. 对称性（Symmetric）**

> 对于任何非空引用值 x 和 y，当且仅当 `y.equals(x)` 返回 true 时，`x.equals(y)` 必须返回 true。

```java
String s1 = "hello";
String s2 = new String("hello");

// 对称性
boolean result1 = s1.equals(s2);  // true
boolean result2 = s2.equals(s1);  // true
```

**3. 传递性（Transitive）**

> 对于任何非空引用值 x、y 和 z，如果 `x.equals(y)` 返回 true 且 `y.equals(z)` 返回 true，那么 `x.equals(z)` 必须返回 true。

```java
// 传递性示例
Person p1 = new Person("张三", 20);
Person p2 = new Person("张三", 20);
Person p3 = new Person("张三", 20);

boolean result1 = p1.equals(p2);  // true
boolean result2 = p2.equals(p3);  // true
boolean result3 = p1.equals(p3);  // 必须是 true
```

**4. 一致性（Consistent）**

> 对于任何非空引用值 x 和 y，在 `equals()` 比较中使用的信息没有被修改的前提下，多次调用 `x.equals(y)` 必须一致地返回 true 或 false。

```java
String s = "test";
// 在字符串内容不变的情况下，多次比较结果应该一致
boolean result1 = s.equals("test");
boolean result2 = s.equals("test");
boolean result3 = s.equals("test");
// result1、result2、result3 应该相同
```

**5. 非空性（Non-null）**

> 对于任何非空引用值 x，`x.equals(null)` 必须返回 false。

```java
String s = "test";
boolean result = s.equals(null);  // 总是 false
```

**equals() 方法的最佳实践**：

在实际开发中，通常需要重写 `equals()` 方法。以下是最佳实践：

```java
public class Person {
    private String name;
    private int age;
    private String email;
    
    @Override
    public boolean equals(Object o) {
        // 1. 检查对象引用是否相同（性能优化）
        if (this == o) return true;
        
        // 2. 检查对象是否为 null 或类型是否匹配
        if (o == null || getClass() != o.getClass()) return false;
        
        // 3. 转换为具体类型
        Person person = (Person) o;
        
        // 4. 比较所有关键字段
        // 注意：优先比较可以快速判断相等性的字段
        return age == person.age && 
               Objects.equals(name, person.name) &&
               Objects.equals(email, person.email);
    }
    
    @Override
    public int hashCode() {
        // 5. 重写 hashCode() 保持契约
        return Objects.hash(name, age, email);
    }
}
```

**equals() 方法的重写步骤**：

1. 使用 `==` 比较引用是否相同
2. 检查 `null` 和类型匹配
3. 将参数转换为正确的类型
4. 比较所有关键字段
5. 不要比较非关键字段（避免性能问题）
6. 始终重写 `hashCode()` 方法

### 5.4 clone() 方法

```java
protected native Object clone() throws CloneNotSupportedException;
```

**方法功能**：

`clone()` 方法用于创建并返回对象的副本（克隆）。

**Object 默认实现**：

`Object` 类的 `clone()` 是一个本地方法，其默认行为是执行浅拷贝（Shallow Copy）：

```java
// Object.clone() 的行为
public class Object {
    protected native Object clone() throws CloneNotSupportedException;
    // 实际上执行的是：
    // 1. 检查是否实现了 Cloneable 接口
    // 2. 分配新的内存空间
    // 3. 复制所有字段的值（按位复制）
}
```

**浅拷贝 vs 深拷贝**：

**浅拷贝**：

```java
public class shallowCopyExample {
    public static void main(String[] args) {
        Original original = new Original();
        original.setValue(new int[]{1, 2, 3});
        
        // 浅拷贝
        Original cloned = (Original) original.clone();
        
        // 修改原对象的数组
        original.getValue()[0] = 99;
        
        // 克隆对象的数组也被修改了（因为引用相同）
        System.out.println(cloned.getValue()[0]);  // 输出 99
    }
}

class Original implements Cloneable {
    private int[] values;
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();  // 浅拷贝
    }
    
    public int[] getValue() {
        return values;
    }
    
    public void setValue(int[] value) {
        this.values = value;
    }
}
```

**深拷贝**：

```java
public class DeepCopyExample {
    public static void main(String[] args) {
        Original original = new Original();
        original.setValue(new int[]{1, 2, 3});
        
        // 深拷贝
        Original cloned = (Original) original.clone();
        
        // 修改原对象的数组
        original.getValue()[0] = 99;
        
        // 克隆对象的数组不受影响（因为是独立的新数组）
        System.out.println(cloned.getValue()[0]);  // 输出 1
    }
}

class Original implements Cloneable {
    private int[] values;
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
        Original cloned = (Original) super.clone();
        // 深拷贝：复制数组内容
        cloned.values = values.clone();
        return cloned;
    }
    
    public int[] getValue() {
        return values;
    }
    
    public void setValue(int[] value) {
        this.values = value;
    }
}
```

**Cloneable 接口的作用**：

`Cloneable` 是一个标记接口，它的作用是告诉 JVM 这个类的实例可以被克隆：

```java
public interface Cloneable {
    // 没有定义任何方法
    // 只是作为标记使用
}
```

如果类没有实现 `Cloneable` 接口，调用 `clone()` 方法会抛出 `CloneNotSupportedException`：

```java
public class NotCloneable {
    public static void main(String[] args) {
        NotCloneable obj = new NotCloneable();
        try {
            Object cloned = obj.clone();  // 抛出异常
        } catch (CloneNotSupportedException e) {
            System.out.println("Cannot clone: " + e.getMessage());
        }
    }
}
```

**clone() 方法的限制**：

`Object` 类的 `clone()` 方法有以下限制：

1. **访问权限限制**：`clone()` 是 `protected` 方法，必须在子类中重写并改为 `public` 才能被外部调用

2. **Cloneable 要求**：类必须实现 `Cloneable` 接口

3. **默认浅拷贝**：默认实现是浅拷贝，对于包含可变对象的字段需要额外处理

4. **final 字段问题**：如果字段被声明为 `final`，则无法在 `clone()` 方法中重新赋值

**clone() 方法的最佳实践**：

```java
public class Person implements Cloneable {
    private String name;
    private int age;
    private Address address;  // 可变对象
    
    @Override
    public Person clone() {
        try {
            Person cloned = (Person) super.clone();
            // 深拷贝：处理可变对象
            if (address != null) {
                cloned.address = address.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloneable should be implemented", e);
        }
    }
}

class Address implements Cloneable {
    private String city;
    private String street;
    
    @Override
    public Address clone() {
        try {
            return (Address) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloneable should be implemented", e);
        }
    }
}
```

**clone() 方法 vs 拷贝构造方法**：

除了 `clone()` 方法，还有一种常用的对象拷贝方式是拷贝构造方法：

```java
public class Person {
    private String name;
    private int age;
    
    // 拷贝构造方法
    public Person(Person other) {
        this.name = other.name;
        this.age = other.age;
    }
    
    // 静态工厂方法
    public static Person newInstance(Person original) {
        return new Person(original);
    }
}
```

**拷贝构造方法的优点**：

- 不依赖 `Cloneable` 接口
- 可以有选择性地复制字段
- 可以返回更具体的类型
- 不受 `final` 字段的限制

### 5.5 toString() 方法

```java
public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
}
```

**方法功能**：

`toString()` 方法返回对象的字符串表示。

**Object 默认实现**：

`Object` 类的默认 `toString()` 实现返回以下格式的字符串：

```java
类名@哈希码的十六进制表示

// 例如：
// java.lang.String@139a8f4e
// com.example.Person@5f9f3b1c
```

**为什么应该重写 toString()**：

默认实现的字符串表示对用户不友好，通常需要重写以提供更有意义的信息：

```java
public class Person {
    private String name;
    private int age;
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
    
    public static void main(String[] args) {
        Person p = new Person("张三", 25);
        System.out.println(p.toString());
        // 输出：Person{name='张三', age=25}
    }
}
```

**toString() 方法的最佳实践**：

```java
public class Person {
    private String name;
    private int age;
    private String email;
    
    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                '}';
    }
    
    // 使用 Java 8+ 的 StringBuilder
    @Override
    public String toString() {
        return new StringBuilder()
                .append("Person{")
                .append("name='").append(name).append('\'')
                .append(", age=").append(age)
                .append(", email='").append(email).append('\'')
                .append('}')
                .toString();
    }
    
    // 使用 Objects 辅助类
    @Override
    public String toString() {
        return "Person{" +
                "name=" + Objects.toString(name, "null") +
                ", age=" + age +
                ", email=" + Objects.toString(email, "null") +
                '}';
    }
    
    // Lombok 注解方式
    // @ToString
    // @ToString(callSuper = true)
}
```

**toString() 的常见应用场景**：

```java
public class ToStringUsage {
    public static void main(String[] args) {
        Person person = new Person("张三", 25);
        
        // 1. 直接打印
        System.out.println(person);
        // 内部会调用 person.toString()
        
        // 2. 日志记录
        logger.info("User info: " + person);
        
        // 3. 字符串拼接
        String info = "Person: " + person;
        
        // 4. 调试输出
        System.out.println("Debug - " + person);
        
        // 5. 异常信息
        throw new RuntimeException("Invalid person: " + person);
    }
}
```

**使用 IDE 自动生成**：

大多数现代 IDE 都支持自动生成 `toString()` 方法：

- **IntelliJ IDEA**：`Generate` → `toString()`
- **Eclipse**：`Source` → `Generate toString()`

### 5.6 notify() 方法

```java
public final native void notify();
```

**方法功能**：

`notify()` 方法唤醒在此对象监视器上等待的单个线程。

**核心概念**：

在 Java 中，每个对象都有一把锁（Monitor），线程可以通过 `synchronized` 关键字获取这把锁。`notify()` 方法用于唤醒等待在该对象锁上的一个线程。

**使用前提**：

调用 `notify()` 方法的线程必须是对象监视器的所有者，即必须满足以下条件之一：

- 当前线程正在执行对象的 `synchronized` 实例方法
- 当前线程正在执行 `synchronized(object)` 代码块

```java
public class NotifyExample {
    public static void main(String[] args) {
        Object monitor = new Object();
        
        // 线程1：等待通知
        new Thread(() -> {
            synchronized (monitor) {
                try {
                    System.out.println("Thread1 waiting...");
                    monitor.wait();  // 释放锁并等待
                    System.out.println("Thread1 notified!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        // 线程2：发送通知
        new Thread(() -> {
            synchronized (monitor) {
                try {
                    Thread.sleep(1000);  // 等待线程1开始等待
                    System.out.println("Thread2 sending notification...");
                    monitor.notify();  // 唤醒一个等待线程
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
```

**notify() vs notifyAll()**：

- `notify()`：只唤醒一个等待线程（具体哪个由 JVM 决定）
- `notifyAll()`：唤醒所有等待线程

```java
// 使用 notifyAll() 的典型模式
synchronized (resource) {
    // 状态改变
    resource.notifyAll();
}

// 使用 notify() 的场景（只有一个条件）
synchronized (queue) {
    // 只有一个消费者时使用 notify()
    queue.notify();
}
```

**错误使用 notify() 的示例**：

```java
// 错误示例
public class WrongNotify {
    private boolean ready = false;
    
    public void waitForReady() throws InterruptedException {
        synchronized (this) {
            while (!ready) {
                wait();  // 使用 while，不是 if
            }
        }
    }
    
    public void setReady() {
        synchronized (this) {
            ready = true;
            notify();  // 可能唤醒错误的线程
        }
    }
}
```

### 5.7 notifyAll() 方法

```java
public final native void notifyAll();
```

**方法功能**：

`notifyAll()` 方法唤醒在此对象监视器上等待的所有线程。

**使用场景**：

当有多个线程在等待不同条件时，应该使用 `notifyAll()` 而不是 `notify()`：

```java
public class ProducerConsumer {
    private Queue<Integer> queue = new LinkedList<>();
    private int capacity = 10;
    
    // 生产者
    public synchronized void produce(int value) throws InterruptedException {
        while (queue.size() == capacity) {
            System.out.println("Queue is full, producer waiting...");
            wait();  // 队列满，等待消费者
        }
        
        queue.add(value);
        System.out.println("Produced: " + value);
        
        // 通知所有等待的线程（包括消费者）
        notifyAll();
    }
    
    // 消费者
    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            System.out.println("Queue is empty, consumer waiting...");
            wait();  // 队列空，等待生产者
        }
        
        int value = queue.poll();
        System.out.println("Consumed: " + value);
        
        // 通知所有等待的线程（包括生产者）
        notifyAll();
        
        return value;
    }
}
```

**notifyAll() 的优势**：

1. **避免死锁**：确保所有等待线程都被唤醒
2. **条件安全**：适用于多个条件变量的场景
3. **简化逻辑**：不需要追踪哪个线程应该被唤醒

**notifyAll() 的潜在问题**：

如果使用不当，`notifyAll()` 可能会导致"惊群效应"（所有线程都被唤醒，但只有一个能获取锁）：

```java
// 优化方案：使用条件变量
public class OptimizedProducerConsumer {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 10;
    
    public void produce(int value) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.add(value);
            notEmpty.signal();  // 只唤醒消费者
        } finally {
            lock.unlock();
        }
    }
    
    public int consume() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            int value = queue.poll();
            notFull.signal();  // 只唤醒生产者
            return value;
        } finally {
            lock.unlock();
        }
    }
}
```

### 5.8 wait() 方法

```java
public final native void wait(long timeout) throws InterruptedException;
```

**方法功能**：

`wait()` 方法导致当前线程等待，直到其他线程调用此对象的 `notify()` 或 `notifyAll()` 方法，或者超过指定的时间。

**方法重载**：

`Object` 类提供了三个 `wait()` 方法的重载版本：

**1. wait() - 无限等待**

```java
public final void wait() throws InterruptedException {
    wait(0);
}
```

**2. wait(long timeout) - 限时等待（毫秒）**

```java
public final native void wait(long timeout) throws InterruptedException;
```

**3. wait(long timeout, int nanos) - 精确等待**

```java
public final void wait(long timeout, int nanos) throws InterruptedException {
    if (timeout < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }
    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
                            "nanosecond timeout value out of range");
    }
    if (nanos > 0) {
        timeout++;
    }
    wait(timeout);
}
```

**wait() 方法的工作原理**：

```java
// wait() 方法的执行过程
public final native void wait(long timeout) throws InterruptedException;

// 实际上执行以下步骤：
// 1. 释放当前对象监视器的锁
// 2. 将线程加入等待队列
// 3. 线程进入等待状态
// 4. 当被唤醒或超时时，重新尝试获取锁
// 5. 获取锁后继续执行
```

**wait() 的使用模式**：

`wait()` 方法必须始终在循环中使用，以防止虚假唤醒：

```java
// 正确的使用模式
synchronized (object) {
    while (conditionIsFalse) {
        object.wait(timeout);
    }
    // 执行条件满足后的操作
}

// 错误的使用模式
synchronized (object) {
    if (conditionIsFalse) {  // 不应该在 if 中使用 wait()
        object.wait(timeout);
    }
    // 可能会在条件不满足时继续执行
}
```

**wait() 的参数说明**：

```java
// wait(0) - 无限等待，直到被 notify/notifyAll
object.wait(0);

// wait(1000) - 最多等待 1000 毫秒
object.wait(1000);

// wait(1000, 500000) - 最多等待 1000.5 秒（精确到纳秒）
object.wait(1000, 500000);
```

**wait() 的异常处理**：

```java
synchronized (lock) {
    try {
        while (!condition) {
            lock.wait(1000);  // 最多等待 1 秒
        }
        // 条件满足，处理任务
    } catch (InterruptedException e) {
        // 线程被中断时的处理
        Thread.currentThread().interrupt();  // 恢复中断状态
        throw new RuntimeException("Waiting interrupted", e);
    }
}
```

**wait() 的典型应用场景**：

```java
// 场景1：线程间通信
public class SharedResource {
    private String data;
    private boolean ready = false;
    
    public synchronized void produce(String value) {
        while (ready) {
            try {
                wait();  // 等待消费者消费
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        data = value;
        ready = true;
        notify();  // 通知消费者
    }
    
    public synchronized String consume() {
        while (!ready) {
            try {
                wait();  // 等待生产者生产
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        ready = false;
        notify();  // 通知生产者可以继续生产
        return data;
    }
}

// 场景2：超时控制
public class TimeoutExample {
    public synchronized boolean waitForCondition(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        long remainingTime = timeoutMs;
        
        while (!condition) {
            if (remainingTime <= 0) {
                return false;  // 超时
            }
            
            try {
                wait(remainingTime);
            } catch (InterruptedException e) {
                if (condition) {
                    return true;  // 虽然被中断，但条件已满足
                }
                Thread.currentThread().interrupt();
                return false;
            }
            
            remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
        }
        
        return true;
    }
}
```

### 5.9 finalize() 方法

```java
protected void finalize() throws Throwable { }
```

**方法功能**：

`finalize()` 方法在对象被垃圾回收之前由垃圾回收器调用，用于执行清理工作。

**方法特点**：

1. **废弃方法**：`finalize()` 方法在 JDK 9 中已被标记为 `@Deprecated`，在 JDK 18 中被完全移除。在 Java 8 中仍然存在，但不建议使用。

2. **调用时机不确定**：`finalize()` 方法的调用时机由 JVM 决定，不保证一定会被调用。

3. **执行线程不确定**：哪个线程调用 `finalize()` 方法是不确定的。

4. **只调用一次**：`finalize()` 方法在对象的生命周期内最多只会被调用一次。

**finalize() 的问题**：

```java
// finalize() 的典型问题
public class ProblematicClass {
    private Resource resource;
    
    @Override
    protected void finalize() throws Throwable {
        try {
            // 清理资源
            resource.close();
        } finally {
            super.finalize();
        }
    }
}

// 问题1：可能导致对象复活
public class Resurrection {
    public static Resurrection resurrectingObject;
    
    @Override
    protected void finalize() throws Throwable {
        // 让对象重新被引用，实现"复活"
        resurrectingObject = this;
    }
}

// 问题2：影响 GC 性能
// 实现了 finalize() 的对象需要额外的处理，GC 需要更多时间
```

**替代方案**：

```java
// 方案1：使用 try-with-resources（推荐）
public class ResourceHandler implements AutoCloseable {
    @Override
    public void close() {
        // 清理资源
    }
}

// 使用方式
try (ResourceHandler handler = new ResourceHandler()) {
    // 使用资源
}  // 自动调用 close() 方法

// 方案2：使用 Cleaner 类（Java 9+）
import java.lang.ref.Cleaner;

public class CleanableResource implements Runnable {
    private final Cleaner cleaner;
    private final State state;
    
    public CleanableResource(Cleaner cleaner) {
        this.cleaner = cleaner;
        this.state = new State();
    }
    
    @Override
    public void run() {
        // 清理逻辑
    }
}

// 方案3：显式调用清理方法
public class ExplicitCleanup {
    private Resource resource;
    
    public void close() {
        if (resource != null) {
            resource.close();
            resource = null;
        }
    }
}
```

## 六、设计模式应用

### 6.1 模板方法模式

`Object` 类中的 `wait()` 方法家族展示了模板方法模式的应用：

```java
// 模板方法模式
public final void wait(long timeout, int nanos) throws InterruptedException {
    // 模板方法，定义了调用流程
    if (timeout < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }
    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException("nanosecond timeout value out of range");
    }
    
    if (nanos > 0) {
        timeout++;  // 模板中的步骤
    }
    
    wait(timeout);  // 调用基本方法（由子类或本地实现）
}

public final void wait() throws InterruptedException {
    wait(0);  // 简化的模板调用
}
```

### 6.2 观察者模式

`Object` 类的 `wait/notify` 机制是观察者模式的简化实现：

```java
// 观察者模式的简化实现
public class Observable {
    private List<Observer> observers = new ArrayList<>();
    
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }
}

// 等价于 Object 的 wait/notify 机制
// 线程作为观察者，Object 作为被观察者
```

### 6.3 标记接口模式

`Object` 类与标记接口配合使用：

```java
// Cloneable 标记接口
public interface Cloneable {
    // 没有方法，只是标记
}

// Serializable 标记接口
public interface Serializable {
    // 没有方法，只是标记
}
```

## 七、常见面试题

### 面试题 1：Object 类有哪些主要方法？

**答案**：

`Object` 类主要有以下 9 个方法：

1. `getClass()` - 获取对象的运行时类
2. `hashCode()` - 获取对象的哈希码
3. `equals(Object obj)` - 判断对象是否相等
4. `clone()` - 创建对象的副本
5. `toString()` - 返回对象的字符串表示
6. `notify()` - 唤醒一个等待线程
7. `notifyAll()` - 唤醒所有等待线程
8. `wait(long timeout)` - 让线程等待
9. `finalize()` - 垃圾回收前调用（已废弃）

### 面试题 2：为什么重写 equals() 必须重写 hashCode()？

**答案**：

根据 Java 规范，如果两个对象 `equals()` 返回 true，它们必须有相同的 `hashCode()`。如果不重写 `hashCode()`，会导致以下问题：

1. **违反契约**：`HashMap`、`HashSet` 等基于哈希的集合依赖于 `hashCode()` 和 `equals()` 的契约
2. **集合行为异常**：

```java
Map<Person, String> map = new HashMap<>();
Person p1 = new Person("张三", 25);
Person p2 = new Person("张三", 25);

map.put(p1, "test");
String value = map.get(p2);  // 可能返回 null
```

### 面试题 3：== 和 equals() 的区别？

**答案**：

| 特性 | `==` | `equals()` |
|------|------|-----------|
| 作用 | 比较引用地址或基本类型值 | 比较对象内容 |
| 被重写 | 不会被重写 | 可以被重写 |
| 默认行为 | 比较地址 | 默认比较地址 |
| 性能 | 快 | 相对慢 |
| 使用场景 | 基本类型比较、引用地址比较 | 对象内容比较 |

```java
String s1 = new String("hello");
String s2 = new String("hello");

s1 == s2;           // false，比较地址
s1.equals(s2);      // true，比较内容（String 重写了 equals）
```

### 面试题 4：wait() 和 sleep() 的区别？

**答案**：

| 特性 | `wait()` | `sleep()` |
|------|----------|-----------|
| 所属类 | `Object` | `Thread` |
| 锁处理 | 释放对象锁 | 不释放锁 |
| 使用场景 | 线程间通信 | 线程休眠 |
| 精度 | 毫秒/纳秒 | 毫秒 |
| 是否需要同步块 | 需要 | 不需要 |
| 被中断 | 抛出 InterruptedException | 抛出 InterruptedException |

### 面试题 5：为什么 wait() 必须在同步块中调用？

**答案**：

1. **竞态条件**：防止在检查条件和调用 `wait()` 之间条件被其他线程修改
2. **状态可见性**：确保线程能看见其他线程对共享状态的修改
3. **锁语义**：`wait()` 会释放锁，必须先持有锁才能释放

```java
// 错误示例
public class WrongWait {
    private boolean ready = false;
    
    public void wrongMethod() {
        while (!ready) {
            // 可能抛出 IllegalMonitorStateException
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

// 正确示例
public class CorrectWait {
    private boolean ready = false;
    
    public void correctMethod() {
        synchronized (this) {  // 必须在同步块中
            while (!ready) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

### 面试题 6：Object 的 hashCode() 默认实现是什么？

**答案**：

`Object` 的 `hashCode()` 是本地方法（native），具体实现由 JVM 决定。在 HotSpot JVM 中，它使用的是一种伪随机数生成器，而不是简单的对象地址转换，这样即使对象被 GC 移动，哈希码也能保持稳定。

### 面试题 7：如何正确实现对象的拷贝？

**答案**：

有三种常见方式：

```java
// 方式1：实现 Cloneable 接口
public class CloneExample implements Cloneable {
    private int value;
    
    @Override
    protected CloneExample clone() {
        try {
            return (CloneExample) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

// 方式2：拷贝构造方法
public class CopyConstructorExample {
    private int value;
    
    public CopyConstructorExample(CopyConstructorExample other) {
        this.value = other.value;
    }
}

// 方式3：静态工厂方法
public class FactoryExample {
    private int value;
    
    public static FactoryExample newInstance(FactoryExample original) {
        FactoryExample copy = new FactoryExample();
        copy.value = original.value;
        return copy;
    }
}
```

### 面试题 8：notify() 和 notifyAll() 的区别？

**答案**：

| 特性 | `notify()` | `notifyAll()` |
|------|------------|---------------|
| 唤醒数量 | 一个线程 | 所有线程 |
| 适用场景 | 只有一个条件 | 多个条件 |
| 死锁风险 | 较低 | 较高（惊群效应） |
| 竞态条件 | 需要精确控制 | 更安全 |

```java
// 使用 notify() 的场景
synchronized (lock) {
    // 只有一个消费者
    lock.notify();
}

// 使用 notifyAll() 的场景
synchronized (lock) {
    // 有多个不同条件的等待者
    lock.notifyAll();
}
```

## 八、实践应用场景

### 8.1 自定义类的 equals() 和 hashCode()

```java
public class User implements Cloneable {
    private String username;
    private String email;
    private int age;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return age == user.age && 
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username, email, age);
    }
    
    @Override
    protected User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
}
```

### 8.2 线程间通信

```java
public class BlockingQueue<E> {
    private final Queue<E> queue = new LinkedList<>();
    private final int capacity;
    
    public synchronized void put(E element) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(element);
        notifyAll();  // 唤醒等待的消费者
    }
    
    public synchronized E take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        E element = queue.poll();
        notifyAll();  // 唤醒等待的生产者
        return element;
    }
}
```

### 8.3 对象池实现

```java
public class ObjectPool<T> {
    private final Queue<T> pool;
    private final ObjectFactory<T> factory;
    private final int maxSize;
    
    public ObjectPool(ObjectFactory<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
        this.pool = new LinkedList<>();
    }
    
    public synchronized T borrowObject() throws Exception {
        T obj;
        if (pool.isEmpty()) {
            obj = factory.create();
        } else {
            obj = pool.poll();
        }
        return obj;
    }
    
    public synchronized void returnObject(T obj) {
        if (obj != null && pool.size() < maxSize) {
            pool.offer(obj);
            notify();  // 唤醒等待的借用者
        }
    }
}

public interface ObjectFactory<T> {
    T create() throws Exception;
}
```

## 九、注意事项和陷阱

### 9.1 避免在 equals() 中使用可变字段

```java
// 错误示例
public class BadPerson {
    private Date birthDate;  // 可变字段
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BadPerson badPerson = (BadPerson) o;
        return Objects.equals(birthDate, badPerson.birthDate);
    }
    
    // 问题：如果 birthDate 被修改，对象的哈希码可能改变
    // 违反 hashCode() 契约
}
```

### 9.2 避免在 hashCode() 中使用浮点数

```java
// 浮点数的比较问题
public class BadPoint {
    private double x;
    private double y;
    
    @Override
    public int hashCode() {
        // Double.hashCode() 可能有精度问题
        return Objects.hash(x, y);
    }
}
```

### 9.3 正确处理 null 值

```java
public class SafeEquals {
    private String name;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;  // 显式检查 null
        // ...
        return true;
    }
}
```

### 9.4 使用 final 类防止意外继承

```java
// 如果类不应该被继承，应该声明为 final
public final class ImmutablePerson {
    private final String name;
    private final int age;
    
    // 不可变对象的 equals/hashCode 可以更高效
}
```

## 十、与相关类的对比

### 10.1 Object vs System

| 方法 | Object | System |
|------|--------|--------|
| hashCode() | 可重写 | `identityHashCode()` 不可重写 |
| gc() | 无 | 提供 `gc()` 方法 |
| exit() | 无 | 提供 `exit()` 方法 |

### 10.2 Object vs Arrays

```java
// 数组继承自 Object
int[] arr = new int[]{1, 2, 3};
Object obj = arr;  // 合法，数组是 Object 的子类

// 数组重写了 toString()
System.out.println(arr);  // 输出：[I@hashcode
System.out.println(Arrays.toString(arr));  // 输出：[1, 2, 3]
```

### 10.3 Object vs Class

```java
Object obj = new Object();
Class<?> clazz = obj.getClass();  // 获取对象的 Class 对象

// Object 没有直接与 Class 相关的方法
// 但 getClass() 返回 Class 对象
```

## 十一、总结

### 11.1 核心要点

1. `Object` 是所有 Java 类的根类
2. 提供了对象身份标识和行为基础
3. `equals()` 和 `hashCode()` 必须保持契约
4. `wait/notify` 是 Java 线程通信的基础
5. `clone()` 需要实现 `Cloneable` 接口
6. `finalize()` 已被废弃，应使用其他资源清理方式

### 11.2 学习建议

1. **理解原理**：不仅要会用这些方法，还要理解其底层实现
2. **实践验证**：编写测试代码验证方法的行为
3. **对比学习**：对比不同方法的适用场景
4. **源码阅读**：阅读 JDK 源码，了解最佳实践

### 11.3 进阶学习

- 学习 `java.util.concurrent` 包中的并发工具
- 学习 JVM 层面的对象模型
- 学习垃圾回收机制
- 学习反射和动态代理

## 参考资料

- JDK 8 官方文档：https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html
- 《Java 核心技术 卷 I》
- 《Effective Java》- Joshua Bloch
- 《深入理解 Java 虚拟机》- 周志明

---

**笔记创建时间**：2025-12-26

**最后更新时间**：2025-12-26

**版本**：1.0
