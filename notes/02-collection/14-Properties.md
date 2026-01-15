# Properties 源码解读

## 一、类概述

Properties是Java集合框架中的一个古老类,它表示一个持久的属性集合。Properties继承自Hashtable<Object,Object>,键和值都是String类型。Properties支持从流中加载和保存属性。

### 1.1 核心特性

- **持久化**: 支持从流中加载和保存属性
- **继承Hashtable**: 继承Hashtable的所有功能
- **String类型**: 键和值都是String类型
- **默认属性**: 支持defaults属性列表
- **多种格式**: 支持文本格式和XML格式
- **线程安全**: 继承Hashtable的线程安全特性
- **古老实现**: JDK 1.0就存在,设计较为陈旧

### 1.2 适用场景

- 配置文件管理
- 国际化资源文件
- 应用程序参数
- 环境变量管理

### 1.3 与Hashtable的区别

| 特性 | Properties | Hashtable |
|------|------------|-----------|
| 键值类型 | 仅String | 任意Object |
| 持久化 | 支持 | 不支持 |
| 默认属性 | 支持 | 不支持 |
| 文件格式 | 支持文本和XML | 不支持 |
| 继承 | 继承Hashtable | - |

## 二、设计原理

### 2.1 属性列表结构

Properties继承自Hashtable,使用哈希表存储键值对:
```
Hashtable存储:
[key1] -> value1
[key2] -> value2
...
```

### 2.2 默认属性链

Properties支持defaults属性列表,形成链式查找:
```
查找顺序:
1. 当前Properties
2. defaults Properties
3. defaults的defaults
...
```

### 2.3 文件格式

Properties支持两种文件格式:

**文本格式**:
```
# 注释
key1=value1
key2:value2
key3 value3
```

**XML格式**:
```xml
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <entry key="key1">value1</entry>
  <entry key="key2">value2</entry>
</properties>
```

## 三、继承结构

```
Hashtable<Object,Object>
    ↑
Properties
```

### 3.1 继承的类

- Hashtable: 哈希表,线程安全的Map实现

### 3.2 继承的特性

- 线程安全: 继承Hashtable的synchronized方法
- 哈希表: 继承Hashtable的哈希表结构
- 动态扩容: 继承Hashtable的扩容机制

## 四、核心字段分析

### 4.1 实例字段

```java
// 包含默认值的属性列表
protected Properties defaults;
```

### 4.2 字段设计要点

1. **defaults字段**: 存储默认属性列表
2. **protected访问**: 便于子类访问
3. **链式查找**: 支持defaults的链式查找

## 五、核心方法解析

### 5.1 构造方法

```java
/**
 * 创建一个没有默认值的空属性列表。
 */
public Properties() {
    this(null);
}

/**
 * 创建具有指定默认值的空属性列表。
 *
 * @param   defaults   默认值。
 */
public Properties(Properties defaults) {
    this.defaults = defaults;
}
```

### 5.2 setProperty - 设置属性

```java
/**
 * 调用<tt>Hashtable</tt>方法<tt>put</tt>。提供与
 * <tt>getProperty</tt>方法并行。强制使用
 * 字符串作为属性键和值。返回的值是
 * 对<tt>Hashtable</tt>调用<tt>put</tt>的结果。
 *
 * @param key 要放入此属性列表的键。
 * @param value 与<tt>key</tt>对应的值。
 * @return     此属性列表中指定键的先前值,
 *             如果它没有一个,则为<tt>null</tt>。
 * @see #getProperty
 * @since    1.2
 */
public synchronized Object setProperty(String key, String value) {
    return put(key, value);
}
```

**方法要点**:
- 调用Hashtable的put()方法
- 强制键和值为String类型
- 返回旧值

### 5.3 getProperty - 获取属性

```java
/**
 * 在此属性列表中搜索具有指定键的属性。
 * 如果在此属性列表中找不到键,则默认属性列表,
 * 及其默认值,递归地,然后被检查。如果未找到属性,
 * 则方法返回<tt>null</tt>。
 *
 * @param   key   属性键。
 * @return  此属性列表中具有指定键值的值。
 * @see     #setProperty
 * @see     #defaults
 */
public String getProperty(String key) {
    Object oval = super.get(key);
    String sval = (oval instanceof String) ? (String)oval : null;
    return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
}

/**
 * 在此属性列表中搜索具有指定键的属性。
 * 如果在此属性列表中找不到键,则默认属性列表,
 * 及其默认值,递归地,然后被检查。方法返回
 * 默认值参数如果未找到属性。
 *
 * @param   key            哈希表键。
 * @param   defaultValue   默认值。
 *
 * @return  此属性列表中具有指定键值的值。
 * @see     #setProperty
 * @see     #defaults
 */
public String getProperty(String key, String defaultValue) {
    String val = getProperty(key);
    return (val == null) ? defaultValue : val;
}
```

**方法要点**:
- 先在当前Properties中查找
- 如果找不到,在defaults中查找
- 支持递归查找defaults的defaults
- getProperty(key, defaultValue)提供默认值

### 5.4 load - 从Reader加载

```java
/**
 * 从输入字符流以简单的面向行的格式读取属性列表(键和元素对)。
 * <p>
 * 属性按行处理。有两种
 * 行,<i>自然行</i>和<i>逻辑行</i>。
 * 自然行定义为被一组行终止符字符
 * (<tt>\n</tt>或<tt>\r</tt>或<tt>\r\n</tt>)
 * 或流结束终止的字符行。自然行可以是空行,
 * 注释行,或包含全部或部分键-元素对。逻辑
 * 行包含键-元素对的所有数据,该数据可能通过转义
 * 行终止符序列和反斜杠字符
 * <tt>\</tt>分布在几个相邻的自然行上。注意
 * 注释行不能以这种方式扩展;每个自然行是注释的
 * 必须有自己的注释指示符,如下所述。从输入读取行,
 * 直到到达流结束。
 * ...
 */
public synchronized void load(Reader reader) throws IOException {
    load0(new LineReader(reader)));
}
```

**方法要点**:
- 支持注释行(以#或!开头)
- 支持行续(以\结尾)
- 支持Unicode转义(\uxxxx)
- 忽略空白行和前导空白字符

### 5.5 load - 从InputStream加载

```java
/**
 * 从输入字节流读取属性列表(键和元素对)。输入流采用
 * <tt>load(java.io.Reader) load(Reader)</tt>中指定的简单面向行
 * 格式,并假定使用ISO 8859-1字符编码;即每个字节是一个Latin1字符。
 * 不在Latin1中的字符以及某些特殊字符,使用
 * <cite>Java&trade; Language Specification</cite>第3.3节中定义的Unicode转义
 * 表示为键和元素。
 * <p>
 * 指定的流在此方法返回后保持打开。
 * ...
 */
public synchronized void load(InputStream inStream) throws IOException {
    load0(new LineReader(inStream));
}
```

**方法要点**:
- 使用ISO 8859-1编码
- 支持Unicode转义
- 与load(Reader)功能相同

### 5.6 store - 保存到Writer

```java
/**
 * 将此属性列表(键和元素对)在此<tt>Properties</tt>表中写入
 * 输出字符流,格式适合使用
 * <tt>load(java.io.Reader) load(Reader)</tt>方法。
 * <p>
 * 此<tt>Properties</tt>表的默认表(如果有)的属性
 * <i>不</i>由此方法写出。
 * <p>
 * 如果comments参数不为null,则ASCII<tt>#</tt>字符,
 * 注释字符串和行分隔符首先写入输出流。因此,
 * <tt>comments</tt>可以作为标识注释。行馈符(<tt>\n</tt>),
 * 回车符(<tt>\r</tt>)或回车符后紧跟行馈符在注释中
 * 被行分隔符替换,如果注释中的下一个字符不是字符<tt>#</tt>或
 * 字符<tt>!</tt>,则在行分隔符后写出ASCII<tt>#</tt>。
 * <p>
 * 接下来,总是写出注释行,由ASCII<tt>#</tt>字符组成,
 * 当前日期和时间(由当前时间的<tt>Date</tt>对象的
 * <tt>toString</tt>方法生成),以及由<tt>Writer</tt>生成的行分隔符。
 * <p>
 * 然后,此<tt>Properties</tt>表中的每个条目被写出,
 * 每行一个。对于每个条目,写出键字符串,
 * 然后是ASCII<tt>=</tt>,然后是关联的元素字符串。
 * 对于键,所有空白字符用前导<tt>\</tt>字符写出。
 * 对于元素,前导空白字符,但不包括嵌入或尾随
 * 空白字符,用前导<tt>\</tt>字符写出。键和元素字符
 * <tt>#</tt>,<tt>!</tt>,<tt>=</tt>和<tt>:</tt>用前导反斜杠写出
 * 以确保它们被正确加载。
 * <p>
 * 写出条目后,输出流被刷新。
 * 输出流在此方法返回后保持打开。
 * ...
 */
public void store(Writer writer, String comments)
    throws IOException
{
    store0((writer instanceof BufferedWriter)?(BufferedWriter)writer
                                                 : new BufferedWriter(writer),
               comments,
               false);
}
```

**方法要点**:
- 写出注释(如果提供)
- 写出日期和时间
- 转义特殊字符(#, !, =, :, 空格, 换行符)
- 键和值都是String类型

### 5.7 store - 保存到OutputStream

```java
/**
 * 将此属性列表(键和元素对)在此<tt>Properties</tt>表中写入
 * 输出流,格式适合使用
 * <tt>load(InputStream) load(InputStream)</tt>方法加载到
 * <tt>Properties</tt>表中。
 * <p>
 * 此方法以与
 * <tt>store(java.io.Writer, java.lang.String) store(Writer)</tt>相同的方式输出注释、属性键和值,
 * 但有以下区别:
 * <ul>
 * <li>流使用ISO 8859-1字符编码写出。
 *
 * <li>注释中不在Latin1中的字符作为其适当的unicode十六进制值
 * <tt>\u005Cuxxxx</tt>写出。
 *
 * <li>属性键或值中小于<tt>\u0020</tt>且大于
 * <tt>\u005Cu007E</tt>的字符作为<tt>\u005Cuxxxx</tt>写出
 * 其适当的十六进制值<tt>xxxx</tt>。
 * </ul>
 * <p>
 * 写出条目后,输出流被刷新。
 * 输出流在此方法返回后保持打开。
 * ...
 */
public void store(OutputStream out, String comments)
    throws IOException
{
    store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
               comments,
               true);
}
```

**方法要点**:
- 使用ISO 8859-1编码
- 转义非Latin1字符
- 与store(Writer)功能类似

### 5.8 loadFromXML - 从XML加载

```java
/**
 * 从指定输入流上表示的所有属性加载到
 * 此属性表中。
 *
 * <p>XML文档必须具有以下DOCTYPE声明:
 * <pre>
 * <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
 * </pre>
 * 此外,文档必须满足上面描述的属性DTD。
 *
 * <p>实现需要读取使用"{@code UTF-8}"或"{@code UTF-16}"编码的XML文档。
 * 实现可能支持其他编码。
 *
 * <p>指定的流在此方法返回后关闭。
 * ...
 */
public synchronized void loadFromXML(InputStream in)
    throws IOException, InvalidPropertiesFormatException
{
    XmlSupport.load(this, Objects.requireNonNull(in));
    in.close();
}
```

**方法要点**:
- 使用XmlSupport类解析XML
- 支持UTF-8和UTF-16编码
- 关闭输入流

### 5.9 storeToXML - 保存为XML

```java
/**
 * 发出表示此表中包含的所有属性的XML文档。
 *
 * <p><tt>props.storeToXML(os, comment)</tt>形式的调用
 * 与<tt>props.storeToXML(os, comment, "UTF-8");</tt>行为完全相同。
 *
 * @param os 要在其上发出XML文档的输出流。
 * @param comment 属性列表的描述,或<tt>null</tt>
 *        如果不需要注释。
 * ...
 */
public void storeToXML(OutputStream os, String comment)
    throws IOException
{
    storeToXML(os, comment, "UTF-8");
}

/**
 * 发出表示此表中包含的所有属性的XML文档,使用指定的编码。
 *
 * <p>XML文档将具有以下DOCTYPE声明:
 * <pre>
 * <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
 * </pre>
 *
 * <p>如果指定的注释为<tt>null</tt>,则文档中不存储注释。
 *
 * <p>实现需要写出使用"{@code UTF-8}"或"{@code UTF-16}"编码的XML文档。
 * 实现可能支持其他编码。
 *
 * <p>指定的流在此方法返回后保持打开。
 * ...
 */
public void storeToXML(OutputStream os, String comment, String encoding)
    throws IOException
{
    XmlSupport.save(this, Objects.requireNonNull(os), comment,
                        Objects.requireNonNull(encoding));
}
```

**方法要点**:
- 使用XmlSupport类生成XML
- 默认使用UTF-8编码
- 支持指定编码

### 5.10 propertyNames - 获取所有键名

```java
/**
 * 返回此属性列表中所有键的枚举,包括默认属性列表中
 * 不同键,如果尚未从主属性列表中找到相同名称的键。
 *
 * @return 此属性列表中所有键的枚举,包括默认属性列表中的键。
 */
public Enumeration<?> propertyNames() {
    Hashtable<Object,Object> h;
    if (defaults != null) {
        h = new Hashtable<>();
        // 将defaults的键添加到h
        for (Enumeration<?> e = defaults.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (!h.containsKey(key)) {
                h.put(key, defaults.get(key));
            }
        }
        // 将当前Properties的键添加到h
        for (Enumeration<?> e = keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (!h.containsKey(key)) {
                h.put(key, get(key));
            }
        }
    } else {
        h = this;
    }
    return h.keys();
}
```

**方法要点**:
- 返回所有键的枚举,包括defaults中的键
- 不重复相同名称的键

### 5.11 stringPropertyNames - 获取所有键名(String)

```java
/**
 * 返回此属性列表中所有键的枚举,如果键名和值都是字符串,
 * 包括默认属性列表中不同键,如果尚未从主属性列表中找到相同名称的键。
 *
 * @return 此属性列表中所有键的枚举,包括默认属性列表中的键。
 * @since   1.6
 */
public Enumeration<String> stringPropertyNames() {
    Hashtable<String,String> h = new Hashtable<>();
    // 将defaults的键添加到h
    if (defaults != null) {
        for (Enumeration<?> e = defaults.keys(); e.hasMoreElements();) {
            Object k = e.nextElement();
            if (k instanceof String && ((String)k).length() > 0) {
                String key = (String)k;
                if (!h.containsKey(key)) {
                    h.put(key, defaults.getProperty(key));
                }
            }
        }
    }
    // 将当前Properties的键添加到h
    for (Enumeration<?> e = keys(); e.hasMoreElements();) {
        Object k = e.nextElement();
        if (k instanceof String && ((String)k).length() > 0) {
            String key = (String)k;
            if (!h.containsKey(key)) {
                h.put(key, (String)get(key));
            }
        }
    }
    return h.keys();
}
```

**方法要点**:
- 只返回String类型的键
- 过滤空字符串键
- 包括defaults中的键

### 5.12 list - 列出属性

```java
/**
 * 将此属性列表输出到指定的输出流。
 * ...
 */
public void list(PrintStream out) {
    out.println("-- listing properties --");
    Hashtable<Object,Object> h = new Hashtable<>();
    // 将defaults的键添加到h
    if (defaults != null) {
        for (Enumeration<?> e = defaults.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (!h.containsKey(key)) {
                h.put(key, defaults.get(key));
            }
        }
    }
    // 将当前Properties的键添加到h
    for (Enumeration<?> e = keys(); e.hasMoreElements();) {
        Object key = e.nextElement();
        if (!h.containsKey(key)) {
            h.put(key, get(key));
        }
    }
    // 列出所有属性
    for (Enumeration<?> e = h.keys(); e.hasMoreElements();) {
        Object key = e.nextElement();
        Object value = h.get(key);
        out.println(key + "=" + value);
    }
}
```

## 六、设计模式

### 6.1 继承模式

Properties继承Hashtable,复用Hashtable的功能:
- Hashtable: 提供哈希表和线程安全
- Properties: 添加属性管理功能

### 6.2 责任链模式

Properties支持defaults属性列表,形成链式查找:
```
查找顺序:
1. 当前Properties
2. defaults Properties
3. defaults的defaults
...
```

### 6.3 适配器模式

Properties将Hashtable适配为属性管理:
- Hashtable: 哈希表
- Properties: 属性管理接口

## 七、面试常见问题

### 7.1 Properties与Hashtable的区别?

| 特性 | Properties | Hashtable |
|------|------------|-----------|
| 键值类型 | 仅String | 任意Object |
| 持久化 | 支持 | 不支持 |
| 默认属性 | 支持 | 不支持 |
| 文件格式 | 支持文本和XML | 不支持 |
| 继承 | 继承Hashtable | - |

### 7.2 Properties是线程安全的吗?

是的,继承自Hashtable,Hashtable的所有公共方法都使用synchronized修饰。

### 7.3 Properties支持哪些文件格式?

1. **文本格式**: 简单的键值对格式
2. **XML格式**: XML文档格式

### 7.4 Properties的defaults有什么作用?

提供默认属性列表,当在当前Properties中找不到属性时,会在defaults中查找:
```
查找顺序:
1. 当前Properties
2. defaults Properties
3. defaults的defaults
...
```

### 7.5 Properties支持null键和null值吗?

继承自Hashtable,不允许null键和null值。

### 7.6 Properties的load方法支持什么编码?

- load(InputStream): 使用ISO 8859-1编码
- load(Reader): 使用Reader指定的编码
- loadFromXML(InputStream): 支持UTF-8和UTF-16

### 7.7 Properties的store方法支持什么编码?

- store(OutputStream): 使用ISO 8859-1编码
- store(Writer): 使用Writer指定的编码
- storeToXML(OutputStream): 默认UTF-8,可指定编码

### 7.8 Properties支持Unicode转义吗?

支持,使用\uxxxx格式:
```
\u0041  # 'A'
\u0042  # 'B'
\u4e2d  # '中'
```

### 7.9 Properties支持行续吗?

支持,使用反斜杠(\)续行:
```
key1=value1,value2,value3,\
value4,value5

等价于:
key1=value1,value2,value3,value4,value5
```

### 7.10 为什么Properties继承Hashtable而不是实现Map接口?

因为Properties是JDK 1.0就存在的古老类,当时还没有Map接口。为了向后兼容,保持继承Hashtable。

## 八、使用场景

### 8.1 配置文件管理

```java
// 创建Properties对象
Properties props = new Properties();

// 从文件加载
try (FileInputStream fis = new FileInputStream("config.properties")) {
    props.load(fis);
}

// 获取配置值
String value = props.getProperty("key", "default");
```

### 8.2 国际化资源文件

```java
// 加载国际化资源文件
ResourceBundle bundle = ResourceBundle.getBundle("messages");

// 或者使用Properties
Properties props = new Properties();
try (FileInputStream fis = new FileInputStream("messages_zh_CN.properties")) {
    props.load(fis);
}
```

### 8.3 应用程序参数

```java
// 读取命令行参数
Properties props = new Properties();
props.setProperty("port", "8080");
props.setProperty("host", "localhost");

// 保存到文件
try (FileOutputStream fos = new FileOutputStream("config.properties")) {
    props.store(fos, "Application Configuration");
}
```

### 8.4 环境变量管理

```java
// 获取系统属性
Properties props = System.getProperties();

// 获取环境变量
String path = props.getProperty("java.home");
```

## 九、注意事项

### 9.1 String类型限制

```java
Properties props = new Properties();

// 推荐: 使用setProperty
props.setProperty("key", "value");

// 不推荐: 使用put
// props.put("key", "value");  // 可能导致ClassCastException
```

### 9.2 默认属性链

```java
Properties defaults = new Properties();
defaults.setProperty("default.key", "default.value");

Properties props = new Properties(defaults);
props.setProperty("key", "value");

// 查找时会先在props中查找,找不到再到defaults中查找
String value = props.getProperty("key");  // 返回"value"
String defaultValue = props.getProperty("default.key");  // 返回"default.value"
```

### 9.3 文件编码

```java
Properties props = new Properties();

// 加载文本文件(ISO 8859-1编码)
try (FileInputStream fis = new FileInputStream("config.properties")) {
    props.load(fis);
}

// 加载UTF-8编码的文件
try (FileInputStream fis = new FileInputStream("config.properties")) {
    props.load(new InputStreamReader(fis, "UTF-8"));
}

// 保存为XML(UTF-8编码)
try (FileOutputStream fos = new FileOutputStream("config.xml")) {
    props.storeToXML(fos, "Configuration");
}
```

### 9.4 线程安全

```java
// Properties是线程安全的,继承自Hashtable
Properties props = new Properties();

// 多线程环境下可以安全使用
```

## 十、最佳实践

### 10.1 使用setProperty而不是put

```java
Properties props = new Properties();

// 推荐: 使用setProperty
props.setProperty("key", "value");

// 不推荐: 使用put
// props.put("key", "value");  // 可能导致ClassCastException
```

### 10.2 使用getProperty而不是get

```java
Properties props = new Properties();
props.setProperty("key", "value");

// 推荐: 使用getProperty
String value = props.getProperty("key");

// 不推荐: 使用get
// Object value = props.get("key");  // 需要类型转换
```

### 10.3 使用默认值

```java
Properties props = new Properties();

// 推荐: 提供默认值
String value = props.getProperty("key", "default");

// 不推荐: 手动检查
String value = props.getProperty("key");
if (value == null) {
    value = "default";
}
```

### 10.4 使用try-with-resources

```java
// 推荐: 使用try-with-resources
try (FileInputStream fis = new FileInputStream("config.properties")) {
    props.load(fis);
}

// 不推荐: 手动关闭
// FileInputStream fis = new FileInputStream("config.properties");
// try {
//     props.load(fis);
// } finally {
//     fis.close();
// }
```

### 10.5 使用XML格式

```java
Properties props = new Properties();
props.setProperty("key", "value");

// 推荐: 使用XML格式
try (FileOutputStream fos = new FileOutputStream("config.xml")) {
    props.storeToXML(fos, "Configuration");
}

// 加载XML格式
try (FileInputStream fis = new FileInputStream("config.xml")) {
    props.loadFromXML(fis);
}
```

## 十一、总结

Properties是一个古老的属性管理类,继承自Hashtable,用于管理持久化的属性集合。Properties支持从流中加载和保存属性,支持文本格式和XML格式。

### 核心要点

1. **继承Hashtable**: 继承Hashtable的所有功能和线程安全特性
2. **String类型**: 键和值都是String类型
3. **持久化**: 支持从流中加载和保存属性
4. **默认属性**: 支持defaults属性列表,形成链式查找
5. **多种格式**: 支持文本格式和XML格式
6. **线程安全**: 继承Hashtable的synchronized方法
7. **古老实现**: JDK 1.0就存在,设计较为陈旧

### 适用场景

- 配置文件管理
- 国际化资源文件
- 应用程序参数
- 环境变量管理

### 不适用场景

- 需要非String类型的键值
- 高性能要求
- 需要复杂的配置管理

### 替代方案

- **简单配置**: Properties
- **复杂配置**: YAML、JSON等
- **国际化**: ResourceBundle
- **系统属性**: System.getProperties()
