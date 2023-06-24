package ctw.base.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis缓存应用工具类（SpringBoot）
 *
 * @author:liva
 * @date:2020-7-15 desc:基于StringRedisTemplate实现以下四个功能函数
 * 方法1：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
 * 方法2：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
 * <p>
 * 方法3：根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
 * 方法4：根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
 * <p>
 * 注意：本工具类配套使用RedisData类，封装redis数据和逻辑过期字段，便于通过逻辑过期字段解决缓存穿透策略。
 */

@Component
@Slf4j
public class RedisCacheClient {

    /* 逻辑过期key前缀 */
    private static final String LOCK_PREFIX = "lock:logical:";

    private StringRedisTemplate redisTemplate;

    /**
     * 构造方法获取stringRedisTemplate
     *
     * @param stringRedisTemplate
     */
    public RedisCacheClient(StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = stringRedisTemplate;
    }

    public void setHashWithMapAndTtl(String key, Map value, int time, TimeUnit unit) {
        redisTemplate.opsForHash().putAll(key, value);
        redisTemplate.opsForHash().getOperations().expire(key, time, unit);
    }

    public void set(String key, Object value) {
        redisTemplate.boundValueOps(key).set(value.toString());
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void remove(String... key) {
        ArrayList<String> keys = CollectionUtil.toList(key);
        List<String> withPrefixKeys = keys.stream().map(i -> i).collect(Collectors.toList());
        redisTemplate.delete(withPrefixKeys);
    }

    public Collection<String> getAllKeys() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null) {
            return new HashSet<>(keys);
        } else {
            return CollectionUtil.newHashSet();
        }
    }

    public Collection<String> getAllValues() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null) {
            return redisTemplate.opsForValue().multiGet(keys);
        } else {
            return CollectionUtil.newArrayList();
        }
    }

    public Map<String, Object> getAllKeyValues() {
        Collection<String> allKeys = this.getAllKeys();
        HashMap<String, Object> results = MapUtil.newHashMap();
        for (String key : allKeys) {
            results.put(key, this.get(key));
        }
        return results;
    }

    /**
     * 添加string类型时设置ttl
     *
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithTtl(Object key, Object value, int time, TimeUnit unit) {
        redisTemplate.opsForValue().set(key.toString(), JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 添加数据时封装逻辑过期时间
     *
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, int time, TimeUnit unit) {
        //通过RedisData封装原始数据和逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 解决缓存击穿策略
     *
     * @param keyPrefix  key的前缀
     * @param id
     * @param type
     * @param dbFullBack 查询数据库回调函数
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix,
                                          ID id, Class<R> type,
                                          Function<ID, R> dbFullBack, //Function<ID,R> 代表有参有返回值的函数，ID为参，R为返回值
                                          int time, TimeUnit unit) {
        //1.从redis中查询商铺信息
        String key = keyPrefix + id;
        String json = redisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {    //isNotBlank当数据为null和""的时候会返回false
            //3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        //判断是否为空值,缓存穿透存储null值的情况,  此时只有2种情况  1.为null，2为""空值
        //为空的话还需要判断是否触发了缓存穿透策略（查到了我们缓存给redis“”空值）
        if (json != null) {
            return null;
        }
        //4.不存在，根据id查询数据库  函数式编程，使用时可以直接lambda   this::queryById 传入逻辑
        R r = dbFullBack.apply(id);
        //5.不存在，返回错误信息
        if (r == null) {
            // 缓存击穿策略，数据库不存在则写入空值到redis，注意ttl的合理性
            redisTemplate.opsForValue().set(key, "", time, TimeUnit.MINUTES);
            return null;
        }
        //6.存在 写入redis
        this.setWithTtl(key, r, time, unit);
        //7.返回
        return r;
    }

    //创建线程池，解决缓存穿透策略时需要用
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 解决缓存穿透策略
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFullBack
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix,
                                            ID id, Class<R> type,
                                            Function<ID, R> dbFullBack,
                                            int time, TimeUnit unit) {
        //1.从redis中查询商铺信息
        String key = keyPrefix + id;
        String json = redisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isBlank(json)) {
            //3.存在，直接返回
            return null;
        }

        //4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        //5.判断是否过期
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1未过期，直接返回店铺信息
            return r;
        }
        //5.2已过期，需要缓存重建
        //6缓存重建
        //6.1获取互斥锁
        String lockKey = LOCK_PREFIX + id;
        boolean isLock = tryLock(lockKey);
        //6.2判断是否获取锁成功
        if (isLock) {
            //6.3成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //重建缓存
                try {
                    //查数据库
                    R r1 = dbFullBack.apply(id);
                    //写入redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //6.4返回过期的商铺信息
        return r;
    }

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key
     */
    private void unLock(String key) {
        redisTemplate.delete(key);
    }
}
