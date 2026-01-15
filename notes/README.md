# Java 8 源码学习笔记索引

## 目录结构

```
notes/
├── README.md                          # 本索引文件
├── 01-language-core/                  # 第一阶段：语言核心基础
│   ├── 01-Object.md
│   ├── 02-String.md
│   ├── 03-Integer.md
│   ├── 04-Long.md
│   ├── 05-Enum.md
│   ├── 06-Thread.md
│   ├── 07-ThreadLocal.md
│   ├── 08-ClassLoader.md
│   └── 09-Reflection.md
├── 02-collection/                     # 第二阶段：集合框架
│   ├── 01-ArrayList.md
│   ├── 02-LinkedList.md
│   ├── 03-Vector.md
│   ├── 04-HashMap.md
│   ├── 05-LinkedHashMap.md
│   ├── 06-TreeMap.md
│   ├── 07-HashSet.md
│   ├── 08-TreeSet.md
│   ├── 09-Hashtable.md
│   └── 10-Collections.md
├── 03-io/                             # 第三阶段：输入输出
│   ├── 01-InputStream.md
│   ├── 02-OutputStream.md
│   ├── 03-Reader.md
│   ├── 04-Writer.md
│   ├── 05-File.md
│   ├── 06-Buffer.md
│   └── 07-FileChannel.md
├── 04-net/                            # 第四阶段：网络编程
│   ├── 01-Socket.md
│   ├── 02-ServerSocket.md
│   └── 03-InetAddress.md
├── 05-time/                           # 第五阶段：时间日期 API
│   ├── 01-LocalDate.md
│   ├── 02-LocalTime.md
│   ├── 03-LocalDateTime.md
│   ├── 04-Instant.md
│   ├── 05-ZoneId.md
│   ├── 06-ZonedDateTime.md
│   ├── 07-Duration.md
│   └── 08-Period.md
└── 06-util/                           # 第六阶段：其他工具类
    ├── 01-Optional.md
    ├── 02-Stream.md
    └── 03-Lambda.md
```

## 学习顺序建议

### 必学核心类（按优先级排序）

1. **String** - 字符串处理
2. **HashMap** - 哈希表实现
3. **ArrayList** - 动态数组
4. **Object** - 所有类的根基
5. **Thread** - 线程基础
6. **ThreadLocal** - 线程本地存储
7. **Integer** - 包装类和缓存机制
8. **ClassLoader** - 类加载机制
9. **LinkedList** - 链表实现
10. **LinkedHashMap** - 有序哈希表

### 进阶学习类

11. **TreeMap** - 红黑树实现
12. **Vector** - 同步数组
13. **Hashtable** - 同步哈希表
14. **File** - 文件操作
15. **Buffer** - NIO 缓冲区

### Java 8 新特性

16. **LocalDate/LocalTime/LocalDateTime**
17. **Instant**
18. **Optional**
19. **Stream API**

## 笔记格式规范

每个笔记文件包含以下章节：

1. **类的概述** - 基本信息和设计目的
2. **类的继承结构** - UML 类图
3. **核心字段解析** - 常量和实例字段
4. **构造方法分析** - 初始化逻辑
5. **核心方法详解** - 重要方法的实现
6. **设计模式应用** - 使用的设计模式
7. **常见面试题** - 面试相关问题
8. **实践应用场景** - 实际使用建议
9. **注意事项和陷阱** - 使用中的坑
10. **与相关类的对比** - 横向对比分析

## 更新日志

- 2025-12-26：创建笔记索引和目录结构
- 持续更新中...

## 贡献指南

欢迎贡献笔记！请遵循以下规范：

1. 使用中文标题和中文标点
2. 代码示例使用英文注释
3. 保持格式一致性
4. 添加实际运行示例
5. 注明参考资料来源

## 许可证

本笔记仅供学习交流使用。
