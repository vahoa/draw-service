package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.*;
import cn.vahoa.draw.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 抽奖服务核心类
 *
 * @author vahoa
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryService {

    private final PrizeRepository prizeRepository;
    private final LotteryRecordRepository lotteryRecordRepository;
    private final UserLotteryQuotaRepository userLotteryQuotaRepository;
    private final LotteryAlgorithm lotteryAlgorithm;
    private final RedissonClient redissonClient;

    // 默认每日抽奖次数
    private static final int DEFAULT_DAILY_QUOTA = 3;
    private static final int DEFAULT_FREE_QUOTA = 1;

    /**
     * 获取奖池可用奖品列表
     */
    public List<Prize> getAvailablePrizes(Long poolId) {
        return prizeRepository.findAvailableByPoolId(poolId);
    }

    /**
     * 获取用户抽奖次数信息
     */
    public UserLotteryQuota getUserQuota(String userId, Long poolId) {
        UserLotteryQuota quota = userLotteryQuotaRepository.findByUserIdAndPoolId(userId, poolId);
        if (quota == null) {
            // 初始化用户抽奖次数
            quota = userLotteryQuotaRepository.initQuota(userId, poolId, DEFAULT_DAILY_QUOTA, DEFAULT_FREE_QUOTA);
        } else if (quota.needReset()) {
            // 需要重置（跨天）
            quota.doReset();
            userLotteryQuotaRepository.update(quota);
        }
        return quota;
    }

    /**
     * 执行抽奖 - 核心方法
     *
     * 业务流程：
     * 1. 验证用户资格（登录状态、抽奖次数）
     * 2. 获取可用奖品列表
     * 3. 分布式锁防止并发问题
     * 4. 扣减抽奖次数
     * 5. 执行抽奖算法
     * 6. 扣减奖品库存
     * 7. 发放奖品
     * 8. 记录抽奖日志
     */
    @Transactional(rollbackFor = Exception.class)
    public LotteryRecord draw(String userId, Long poolId, String clientIp, String deviceId) {
        // 1. 验证用户资格
        UserLotteryQuota quota = getUserQuota(userId, poolId);
        if (!quota.hasQuota()) {
            throw new IllegalStateException("今日抽奖次数已用完");
        }

        // 2. 获取可用奖品
        List<Prize> availablePrizes = getAvailablePrizes(poolId);
        if (availablePrizes.isEmpty()) {
            throw new IllegalStateException("暂无可用奖品");
        }

        // 3. 分布式锁 - 防止同一用户并发抽奖
        String lockKey = "lottery:draw:" + userId + ":" + poolId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待3秒，锁持有10秒
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("操作过于频繁，请稍后再试");
            }

            try {
                // 双重检查 - 获取锁后再次检查次数
                quota = getUserQuota(userId, poolId);
                if (!quota.hasQuota()) {
                    throw new IllegalStateException("今日抽奖次数已用完");
                }

                // 4. 扣减抽奖次数
                int affected = userLotteryQuotaRepository.useQuota(userId, poolId);
                if (affected == 0) {
                    throw new IllegalStateException("扣减次数失败");
                }

                // 5. 执行抽奖算法
                Prize prize = lotteryAlgorithm.draw(availablePrizes);

                // 6. 扣减库存（实物奖品）
                if (prize.getType() == 3 && prize.getStock() > 0) {
                    int stockAffected = prizeRepository.deductStock(prize.getId());
                    if (stockAffected == 0) {
                        // 库存不足，转为未中奖
                        log.warn("奖品库存不足，prizeId={}", prize.getId());
                        prize = getNonePrize();
                    }
                }

                // 7. 创建抽奖记录
                LotteryRecord record = new LotteryRecord();
                record.generateRecordNo();
                record.setUserId(userId);
                record.setPoolId(poolId);
                record.setPrizeId(prize.getId());
                record.setPrizeName(prize.getName());
                record.setPrizeType(prize.getType());
                record.setPrizeValue(prize.getValue());
                record.setStatus(1); // 待发放
                record.setDrawTime(LocalDateTime.now());
                record.setClientIp(clientIp);
                record.setDeviceId(deviceId);

                lotteryRecordRepository.insert(record);

                // 8. 发放奖品（虚拟奖品直接发放）
                if (prize.getType() == 1 || prize.getType() == 2) {
                    // 积分、会员直接发放
                    grantVirtualPrize(userId, prize);
                    record.markAsGranted();
                    lotteryRecordRepository.update(record);
                }

                // 更新奖品今日发放数量
                prizeRepository.incrementTodaySent(prize.getId());

                log.info("用户抽奖成功，userId={}, prizeId={}, prizeName={}",
                    userId, prize.getId(), prize.getName());

                return record;

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("抽奖被中断");
        }
    }

    /**
     * 发放虚拟奖品
     */
    private void grantVirtualPrize(String userId, Prize prize) {
        switch (prize.getType()) {
            case 1: // 积分
                log.info("发放积分给用户 {}，数量 {}", userId, prize.getValue());
                // TODO: 调用积分服务
                break;
            case 2: // 会员
                log.info("发放会员给用户 {}，天数 {}", userId, prize.getValue());
                // TODO: 调用会员服务
                break;
            default:
                break;
        }
    }

    /**
     * 获取"未中奖"奖品
     */
    private Prize getNonePrize() {
        Prize nonePrize = new Prize();
        nonePrize.setId(0L);
        nonePrize.setName("谢谢参与");
        nonePrize.setType(1); // 积分类型，但值为0
        nonePrize.setValue("0");
        return nonePrize;
    }

    /**
     * 获取最近中奖通知
     */
    public List<LotteryRecord> getRecentWinNotices(int limit) {
        // 查询最近的中奖记录（排除积分奖品）
        return lotteryRecordRepository.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create()
                .from("lottery_record")
                .where("prize_type != 1")
                .orderBy("draw_time DESC")
                .limit(limit)
        );
    }
}
