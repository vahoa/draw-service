package cn.vahoa.draw.infrastructure.cache;

import cn.vahoa.draw.domain.entity.UserLotteryQuota;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 缓存与限流（统一通过 Redisson 访问 Redis，不使用 Lettuce/Jedis / Spring Data Redis Template）。
 */
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedissonClient redisson;

    private static final String KEY_PREFIX = "lottery:";

    public Long getPrizeStock(Long prizeId) {
        String key = KEY_PREFIX + "prize:stock:" + prizeId;
        RAtomicLong atomic = redisson.getAtomicLong(key);
        if (!atomic.isExists()) {
            return null;
        }
        return atomic.get();
    }

    public void setPrizeStock(Long prizeId, long stock) {
        String key = KEY_PREFIX + "prize:stock:" + prizeId;
        RAtomicLong atomic = redisson.getAtomicLong(key);
        atomic.set(stock);
        atomic.expire(Duration.ofHours(1));
    }

    public Long decrementStock(Long prizeId) {
        String key = KEY_PREFIX + "prize:stock:" + prizeId;
        return redisson.getAtomicLong(key).decrementAndGet();
    }

    public UserLotteryQuota getUserQuota(String userId, Long poolId) {
        String key = KEY_PREFIX + "user:quota:" + userId + ":" + poolId;
        RBucket<String> bucket = redisson.getBucket(key);
        String value = bucket.get();
        return value != null ? JSON.parseObject(value, UserLotteryQuota.class) : null;
    }

    public void setUserQuota(String userId, Long poolId, UserLotteryQuota quota) {
        String key = KEY_PREFIX + "user:quota:" + userId + ":" + poolId;
        RBucket<String> bucket = redisson.getBucket(key);
        bucket.set(JSON.toJSONString(quota), Duration.ofDays(1));
    }

    public void decrementUserQuota(String userId, Long poolId) {
        String key = KEY_PREFIX + "user:quota:" + userId + ":" + poolId;
        RBucket<String> bucket = redisson.getBucket(key);
        String value = bucket.get();
        if (value != null) {
            UserLotteryQuota quota = JSON.parseObject(value, UserLotteryQuota.class);
            quota.setUsedQuota(quota.getUsedQuota() + 1);
            setUserQuota(userId, poolId, quota);
        }
    }

    public boolean checkUserRateLimit(String userId, int maxAttempts, int windowSeconds) {
        long window = System.currentTimeMillis() / (windowSeconds * 1000L);
        String key = KEY_PREFIX + "rl:user:" + userId + ":" + window;
        return incrementWithinLimit(key, maxAttempts, windowSeconds);
    }

    public boolean checkIpRateLimit(String ip, int maxAttempts, int windowSeconds) {
        long window = System.currentTimeMillis() / (windowSeconds * 1000L);
        String key = KEY_PREFIX + "rl:ip:" + ip + ":" + window;
        return incrementWithinLimit(key, maxAttempts, windowSeconds);
    }

    public boolean checkSlidingWindow(String userId, int maxAttempts, int windowSeconds) {
        long window = System.currentTimeMillis() / (windowSeconds * 1000L);
        String key = KEY_PREFIX + "sw:user:" + userId + ":" + window;
        return incrementWithinLimit(key, maxAttempts, windowSeconds);
    }

    public long getDeviceUserCount(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return 0;
        }
        RBucket<String> bucket = redisson.getBucket(KEY_PREFIX + "device:users:" + deviceId);
        String v = bucket.get();
        return v == null ? 0 : Long.parseLong(v);
    }

    public boolean isDeviceBlacklisted(String deviceId) {
        return deviceId != null && redisson.getBucket(KEY_PREFIX + "bl:device:" + deviceId).isExists();
    }

    public boolean isIpBlacklisted(String ip) {
        return ip != null && redisson.getBucket(KEY_PREFIX + "bl:ip:" + ip).isExists();
    }

    public long getIpSegmentUserCount(String segment) {
        if (segment == null) {
            return 0;
        }
        RBucket<String> bucket = redisson.getBucket(KEY_PREFIX + "ipseg:users:" + segment);
        String v = bucket.get();
        return v == null ? 0 : Long.parseLong(v);
    }

    public List<String> getUserDevices(String userId) {
        return Collections.emptyList();
    }

    public boolean isDeviceHighRisk(String deviceId) {
        return isDeviceBlacklisted(deviceId);
    }

    public List<String> getUserIps(String userId) {
        return Collections.emptyList();
    }

    public boolean isIpHighRisk(String ip) {
        return isIpBlacklisted(ip);
    }

    public void addToBlacklist(String userId, String ip, int minutes) {
        Duration ttl = Duration.ofMinutes(minutes);
        if (userId != null) {
            redisson.getBucket(KEY_PREFIX + "bl:user:" + userId).set("1", ttl);
        }
        if (ip != null) {
            redisson.getBucket(KEY_PREFIX + "bl:ip:" + ip).set("1", ttl);
        }
    }

    private boolean incrementWithinLimit(String key, int maxAttempts, int ttlSeconds) {
        RAtomicLong atomic = redisson.getAtomicLong(key);
        long c = atomic.incrementAndGet();
        if (c == 1L) {
            atomic.expire(Duration.ofSeconds(ttlSeconds));
        }
        return c <= maxAttempts;
    }
}
