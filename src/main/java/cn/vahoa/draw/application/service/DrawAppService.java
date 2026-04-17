package cn.vahoa.draw.application.service;

import cn.vahoa.draw.application.dto.DrawRequest;
import cn.vahoa.draw.application.dto.DrawResult;
import cn.vahoa.draw.application.dto.DrawPageInfo;
import cn.vahoa.draw.domain.entity.LotteryRecord;
import cn.vahoa.draw.domain.entity.Prize;
import cn.vahoa.draw.domain.entity.UserLotteryQuota;
import cn.vahoa.draw.domain.service.LotteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 抽奖应用服务
 * 负责协调领域服务，处理应用层逻辑
 *
 * @author vahoa
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrawAppService {

    private final LotteryService lotteryService;
    private final PrizeGrantService prizeGrantService;
    private final NotificationService notificationService;

    /**
     * 获取抽奖页面信息
     */
    public DrawPageInfo getDrawPageInfo(String userId, Long poolId) {
        // 获取可用奖品列表
        List<Prize> prizes = lotteryService.getAvailablePrizes(poolId);

        // 获取用户抽奖次数
        UserLotteryQuota quota = lotteryService.getUserQuota(userId, poolId);

        // 获取最近中奖通知
        List<LotteryRecord> recentRecords = lotteryService.getRecentWinNotices(10);

        // 构建页面信息
        return DrawPageInfo.builder()
                .prizes(prizes.stream().map(this::convertToPrizeDTO).collect(Collectors.toList()))
                .remainingQuota(quota.getRemainingQuota())
                .totalQuota(quota.getTotalQuota())
                .usedQuota(quota.getUsedQuota())
                .recentWinNotices(recentRecords.stream()
                        .map(r -> "恭喜用户" + maskUserId(r.getUserId()) + "抽中" + r.getPrizeName())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 执行抽奖
     */
    public DrawResult draw(DrawRequest request, String clientIp, String deviceId) {
        log.info("用户发起抽奖请求，userId={}, poolId={}", request.getUserId(), request.getPoolId());

        try {
            // 调用领域服务执行抽奖
            LotteryRecord record = lotteryService.draw(
                    request.getUserId(),
                    request.getPoolId(),
                    clientIp,
                    deviceId
            );

            // 构建返回结果
            DrawResult result = DrawResult.builder()
                    .success(true)
                    .recordNo(record.getRecordNo())
                    .prizeId(record.getPrizeId())
                    .prizeName(record.getPrizeName())
                    .prizeType(record.getPrizeType())
                    .prizeValue(record.getPrizeValue())
                    .win(record.getPrizeType() != null && record.getPrizeType() != 1)
                    .message(buildWinMessage(record))
                    .build();

            // 发送中奖通知
            if (result.isWin()) {
                notificationService.notifyGrantSuccess(record);
            }

            return result;

        } catch (IllegalStateException e) {
            log.warn("抽奖失败，userId={}, reason={}", request.getUserId(), e.getMessage());
            return DrawResult.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    /**
     * 转换奖品为DTO
     */
    private DrawPageInfo.PrizeDTO convertToPrizeDTO(Prize prize) {
        return DrawPageInfo.PrizeDTO.builder()
                .id(prize.getId())
                .name(prize.getName())
                .type(prize.getType())
                .imageUrl(prize.getImageUrl())
                .value(prize.getValue())
                .build();
    }

    /**
     * 脱敏用户ID
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "***";
        }
        return userId.substring(0, 2) + "**" + userId.substring(userId.length() - 2);
    }

    /**
     * 构建中奖消息
     */
    private String buildWinMessage(LotteryRecord record) {
        if (record.getPrizeType() == null || record.getPrizeType() == 1) {
            return "很遗憾，这次没有中奖，再试一次吧！";
        }
        return "恭喜您获得 " + record.getPrizeName() + "！";
    }
}
