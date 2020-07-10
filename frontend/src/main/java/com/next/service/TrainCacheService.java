package com.next.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import javax.annotation.Resource;
import java.util.Map;

@Service
@Slf4j
public class TrainCacheService {

    @Resource(name = "shardedJedisPool")
    private ShardedJedisPool shardedJedisPool;

    private ShardedJedis instance() {
        return shardedJedisPool.getResource();
    }

    private void safeClose(ShardedJedis shardedJedis) {
        try {
            if (shardedJedis != null) {
                shardedJedis.close();
            }
        } catch (Exception e) {
            log.error("jedis close exception", e);
        }
    }

    public void set(String cacheKey, String value) {
        if (value == null) {
            return;
        }
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            shardedJedis.set(cacheKey, value);
        } catch (Exception e) {
            log.error("jedis.set exception, cacheKey:{},value:{}", cacheKey, value, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }

    public String get(String cacheKey) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            return shardedJedis.get(cacheKey);
        } catch (Exception e) {
            log.error("jedis.get exception, cacheKey:{}", cacheKey, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }

    public String hget(String cacheKey, String field) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            return shardedJedis.hget(cacheKey, field);
        } catch (Exception e) {
            log.error("jedis.hget exception, cacheKey:{},field:{}", cacheKey, field, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }

    public Map<String, String> hgetAll(String cacheKey) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            return shardedJedis.hgetAll(cacheKey);
        } catch (Exception e) {
            log.error("jedis.hgetAll exception, cacheKey:{}", cacheKey, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }

    public void hset(String cacheKey, String field, String value) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            shardedJedis.hset(cacheKey, field, value);
        } catch (Exception e) {
            log.error("jedis.hset exception, cacheKey:{},field:{},value:{}", cacheKey, field, value, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }

    public void hincrBy(String cacheKey, String field, Long value) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = instance();
            shardedJedis.hincrBy(cacheKey, field, value);
        } catch (Exception e) {
            log.error("jedis.hincrBy exception, cacheKey:{},field:{},value:{}", cacheKey, field, value, e);
            throw e;
        } finally {
            safeClose(shardedJedis);
        }
    }
}
