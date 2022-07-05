## 1、项目简介

[Cache](https://github.com/houbb/cache) 用于实现一个可拓展的本地缓存。

有人的地方，就有江湖。

有高性能的地方，就有 cache。

🎨🎨🎨🎨💻💻💻💻✏️✏️✏️✏️

## 2、创作目的

- 为日常开发提供一套简单易用的缓存框架

- 便于后期多级缓存开发

- 学以致用，开发一个类似于 redis 的渐进式缓存框架

## 3、特性

- MVP 开发策略

- fluent 流式编程体验，纵享丝滑

- 支持 cache 固定大小

- 支持自定义 map 实现策略

- 支持 expire 过期特性

- 支持自定义 evict 驱除策略

内置 FIFO 和 LRU 驱除策略

- 支持自定义删除监听器

- 日志整合框架，自适应常见日志

- 支持 load 初始化和 persist 持久化

RDB 和 AOF 两种模式


## 4、快速开始

### 4.1、准备

JDK1.7 及其以上版本

Maven 3.X 及其以上版本

### 4.2、maven 项目依赖

```xml
<dependency>
    <groupId>com.github.houbb</groupId>
    <artifactId>cache-core</artifactId>
    <version>0.0.15</version>
</dependency>
```

### 4.3、入门测试

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
                .size(2)
                .build();

cache.put("1", "1");
cache.put("2", "2");
cache.put("3", "3");
cache.put("4", "4");

Assert.assertEquals(2, cache.size());
```

默认为先进先出的策略，此时输出 keys，内容如下：

```
[3, 4]
```

### 4.4、引导类配置属性

`CacheBs` 作为缓存的引导类，支持 fluent 写法，编程更加优雅便捷。

上述配置等价于：

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
                .map(Maps.<String,String>hashMap())
                .evict(CacheEvicts.<String, String>fifo())
                .size(2)
                .build();
```

### 4.5、淘汰策略

目前内置了几种淘汰策略，可以直接通过 `CacheEvicts` 工具类创建。

| 策略 | 说明 |
|:---|:---|
| none | 没有任何淘汰策略 |
| fifo | 先进先出（默认策略） |
| lru | 最基本的朴素 LRU 策略，性能一般 |
| lruDoubleListMap | 基于双向链表+MAP 实现的朴素 LRU，性能优于 lru |
| lruLinkedHashMap | 基于 LinkedHashMap 实现的朴素 LRU，与 lruDoubleListMap 差不多 |
| lru2Q | 基于 LRU 2Q 的改进版 LRU 实现，命中率优于朴素LRU |
| lru2 | 基于 LRU-2 的改进版 LRU 实现，命中率优于 lru2Q |

### 4.6、过期支持

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
        .size(3)
        .build();

cache.put("1", "1");
cache.put("2", "2");

cache.expire("1", 10);
Assert.assertEquals(2, cache.size());

TimeUnit.MILLISECONDS.sleep(50);
Assert.assertEquals(1, cache.size());
System.out.println(cache.keySet());
```

`cache.expire("1", 10);` 指定对应的 key 在 10ms 后过期。

## 5、删除监听器

### 5.1、说明

淘汰和过期，这些都是缓存的内部行为。

如果用户也关心的话，可以自定义删除监听器。

### 5.2、自定义监听器

直接实现 `ICacheRemoveListener` 接口即可。

```java
public class MyRemoveListener<K,V> implements ICacheRemoveListener<K,V> {

    @Override
    public void listen(ICacheRemoveListenerContext<K, V> context) {
        System.out.println("【删除提示】可恶，我竟然被删除了！" + context.key());
    }

}
```

### 5.3、使用

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
        .size(1)
        .addRemoveListener(new MyRemoveListener<String, String>())
        .build();

cache.put("1", "1");
cache.put("2", "2");
```

- 测试日志

```
【删除提示】可恶，我竟然被删除了！2
```

## 6、添加慢操作监听器

### 6.1、说明

redis 中会存储慢操作的相关日志信息，主要是由两个参数构成：

（1）slowlog-log-slower-than 预设阈值,它的单位是毫秒(1秒=1000000微秒)默认值是10000

（2）slowlog-max-len 最多存储多少条的慢日志记录

不过 redis 是直接存储到内存中，而且有长度限制。

根据实际工作体验，如果我们可以添加慢日志的监听，然后有对应的存储或者报警，这样更加方便问题的分析和快速反馈。

所以我们引入类似于删除的监听器。

### 6.2、自定义监听器

实现接口 `ICacheSlowListener`

这里每一个监听器都可以指定自己的慢日志阈值，便于分级处理。

```java
public class MySlowListener implements ICacheSlowListener {

    @Override
    public void listen(ICacheSlowListenerContext context) {
        System.out.println("【慢日志】name: " + context.methodName());
    }

    @Override
    public long slowerThanMills() {
        return 0;
    }

}
```

### 6.3、使用

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
        .addSlowListener(new MySlowListener())
        .build();

cache.put("1", "2");
cache.get("1");
```

- 测试效果

```
[DEBUG] [2020-09-30 17:40:11.547] [main] [c.g.h.c.c.s.i.c.CacheInterceptorCost.before] - Cost start, method: put
[DEBUG] [2020-09-30 17:40:11.551] [main] [c.g.h.c.c.s.i.c.CacheInterceptorCost.after] - Cost end, method: put, cost: 10ms
【慢日志】name: put
[DEBUG] [2020-09-30 17:40:11.554] [main] [c.g.h.c.c.s.i.c.CacheInterceptorCost.before] - Cost start, method: get
[DEBUG] [2020-09-30 17:40:11.554] [main] [c.g.h.c.c.s.i.c.CacheInterceptorCost.after] - Cost end, method: get, cost: 1ms
【慢日志】name: get
```

实际工作中，我们可以针对慢日志数据存储，便于后期分析。

也可以直接接入报警系统，及时反馈问题。

## 7、添加 load 加载器

### 7.1、说明

有时候我们需要在 cache 初始化的时候，添加对应的数据初始化。

后期可以从文件等地方加载数据。

### 7.2、实现

实现 `ICacheLoad` 接口即可。

```java
public class MyCacheLoad implements ICacheLoad<String,String> {

    @Override
    public void load(ICache<String, String> cache) {
        cache.put("1", "1");
        cache.put("2", "2");
    }

}
```

我们在缓存初始化的时候，放入 2 个元素。

### 7.3、测试效果

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
        .load(new MyCacheLoad())
        .build();

Assert.assertEquals(2, cache.size());
```

## 8、添加 persist 持久化类

### 8.1、说明

如果我们只是把文件放在内存中，应用重启信息就丢失了。

有时候我们希望这些 key/value 信息可以持久化，存储到文件或者 database 中。

### 8.2、持久化

`CachePersists.<String, String>dbJson("1.rdb")` 指定将数据文件持久化到文件中。

定期执行，暂时全量持久化的间隔为 10min，后期考虑支持更多配置。

```java
public void persistTest() throws InterruptedException {
    ICache<String, String> cache = CacheBs.<String,String>newInstance()
            .load(new MyCacheLoad())
            .persist(CachePersists.<String, String>dbJson("1.rdb"))
            .build();

    Assert.assertEquals(2, cache.size());
    TimeUnit.SECONDS.sleep(5);
}
```

- 1.rdb

文件内容如下：

```
{"key":"2","value":"2"}
{"key":"1","value":"1"}
```

### 8.3、加载器

存储之后，可以使用对应的加载器读取文件内容：

```java
ICache<String, String> cache = CacheBs.<String,String>newInstance()
        .load(CacheLoads.<String, String>dbJson("1.rdb"))
        .build();

Assert.assertEquals(2, cache.size());
```

## 开发文档

文档是对项目开发过程中遇到的一些问题的详细记录，主要是为了帮助没有基础的小伙伴快速理解这个项目。

1.[实现固定缓存大小](https://github.com/zxlrise/cache/wiki/%E5%AE%9E%E7%8E%B0%E5%9B%BA%E5%AE%9A%E7%BC%93%E5%AD%98%E5%A4%A7%E5%B0%8F)

2.[redis expire 过期原理](https://github.com/zxlrise/cache/wiki/redis-expire-%E8%BF%87%E6%9C%9F%E5%8E%9F%E7%90%86)

3.[内存数据如何重启不丢失](https://github.com/zxlrise/cache/wiki/%E5%86%85%E5%AD%98%E6%95%B0%E6%8D%AE%E5%A6%82%E4%BD%95%E9%87%8D%E5%90%AF%E4%B8%8D%E4%B8%A2%E5%A4%B1%EF%BC%9F)

4.[添加监听器 ](https://github.com/zxlrise/cache/wiki/%E6%B7%BB%E5%8A%A0%E7%9B%91%E5%90%AC%E5%99%A8)

5.[过期策略的另一种实现思路](https://github.com/zxlrise/cache/wiki/%E8%BF%87%E6%9C%9F%E7%AD%96%E7%95%A5%E7%9A%84%E5%8F%A6%E4%B8%80%E7%A7%8D%E5%AE%9E%E7%8E%B0%E6%80%9D%E8%B7%AF)

6.[redis AOF 持久化原理详解及实现](https://github.com/zxlrise/cache/wiki/redis-AOF-%E6%8C%81%E4%B9%85%E5%8C%96%E5%8E%9F%E7%90%86%E8%AF%A6%E8%A7%A3%E5%8F%8A%E5%AE%9E%E7%8E%B0)

7.[朴素 LRU 淘汰算法性能优化](https://github.com/zxlrise/cache/wiki/%E6%9C%B4%E7%B4%A0-LRU-%E6%B7%98%E6%B1%B0%E7%AE%97%E6%B3%95%E6%80%A7%E8%83%BD%E4%BC%98%E5%8C%96)

8.[LRU 缓存淘汰算法如何避免缓存污染](https://github.com/zxlrise/cache/wiki/LRU-%E7%BC%93%E5%AD%98%E6%B7%98%E6%B1%B0%E7%AE%97%E6%B3%95%E5%A6%82%E4%BD%95%E9%81%BF%E5%85%8D%E7%BC%93%E5%AD%98%E6%B1%A1%E6%9F%93)

9.[缓存淘汰算法 LFU 最少使用频次](https://github.com/zxlrise/cache/wiki/%E7%BC%93%E5%AD%98%E6%B7%98%E6%B1%B0%E7%AE%97%E6%B3%95-LFU-%E6%9C%80%E5%B0%91%E4%BD%BF%E7%94%A8%E9%A2%91%E6%AC%A1)

10.[clock时钟淘汰算法详解及实现](https://github.com/zxlrise/cache/wiki/clock%E6%97%B6%E9%92%9F%E6%B7%98%E6%B1%B0%E7%AE%97%E6%B3%95%E8%AF%A6%E8%A7%A3%E5%8F%8A%E5%AE%9E%E7%8E%B0)

11.[redis expire 过期实现随机获取keys](https://github.com/zxlrise/cache/wiki/redis-expire-%E8%BF%87%E6%9C%9F%E5%AE%9E%E7%8E%B0%E9%9A%8F%E6%9C%BA%E8%8E%B7%E5%8F%96keys)

12.[redis渐进式rehash详解](https://github.com/zxlrise/cache/wiki/%E5%AE%9E%E7%8E%B0%E6%B8%90%E8%BF%9B%E5%BC%8F-rehash-map)

13.[实现自己的 HashMap](https://github.com/zxlrise/cache/wiki/%E5%AE%9E%E7%8E%B0%E8%87%AA%E5%B7%B1%E7%9A%84-HashMap)

14.[实现渐进式 rehash map](https://github.com/zxlrise/cache/wiki/%E5%AE%9E%E7%8E%B0%E6%B8%90%E8%BF%9B%E5%BC%8F-rehash-map)
