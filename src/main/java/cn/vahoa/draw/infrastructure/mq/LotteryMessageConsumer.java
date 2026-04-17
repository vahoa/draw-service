package cn.vahoa.draw.infrastructure.mq;

import cn.vahoa.draw.application.service.NotificationService;
import cn.vahoa.draw.application.service.PrizeGrantService;
import cn.vahoa.draw.domain.entity.LotteryRecord;
import cn.vahoa.draw.domain.repository.LotteryRecordRepository;
import cn.vahoa.draw.interfaces.mq.dto.BehaviorLogMessage;
import cn.vahoa.draw.interfaces.mq.dto.GrantPrizeMessage;
import cn.vahoa.draw.interfaces.mq.dto.NotificationMessage;
import cn.vahoa.draw.interfaces.mq.dto.StockSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 抽奖消息消费者
 * <p>
 * 处理异步任务：
 * 1. 奖品发放
 * 2. 库存同步
 * 3. 日志记录
 * 4. 通知推送
 *
 * @author vahoa
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotteryMessageConsumer {

    private final LotteryRecordRepository recordRepository;
    private final MongoTemplate mongoTemplate;
    private final PrizeGrantService prizeGrantService;
    private final NotificationService notificationService;

    /**
     * 消费奖品发放消息
     */
    @KafkaListener(
        topics = "${lottery.kafka.topic.grant:lottery-grant}",
        groupId = "${lottery.kafka.consumer.group:draw-service-grant-group}"
    )
    public void consumeGrantPrize(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String messageId = record.key();
        String messageBody = record.value();
        
        log.info("收到奖品发放消息, messageId={}", messageId);
        
        try {
            GrantPrizeMessage message = parseMessage(messageBody);
            
            // 幂等性检查
            if (isMessageProcessed(messageId)) {
                log.warn("消息[{}]已处理，跳过", messageId);
                acknowledgment.acknowledge();
                return;
            }
            
            // 执行奖品发放
            processGrantPrize(message);
            
            // 记录消息处理状态
            recordMessageProcessed(messageId);
            
            // 确认消息
            acknowledgment.acknowledge();
            
            log.info("奖品发放消息处理完成, messageId={}", messageId);
            
        } catch (Exception e) {
            log.error("处理奖品发放消息失败, messageId={}", messageId, e);
            // 不确认消息，触发重试
            throw e;
        }
    }
    
    /**
     * 消费库存同步消息
     */
    @KafkaListener(
        topics = "${lottery.kafka.topic.stock-sync:lottery-stock-sync}",
        groupId = "${lottery.kafka.consumer.group:draw-service-stock-group}"
    )
    public void consumeStockSync(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String messageId = record.key();
        String messageBody = record.value();
        
        log.info("收到库存同步消息, messageId={}", messageId);
        
        try {
            StockSyncMessage message = parseStockMessage(messageBody);
            
            // 同步库存到数据库
            syncStockToDatabase(message);
            
            acknowledgment.acknowledge();
            
            log.info("库存同步完成, prizeId={}, stock={}", message.getPrizeId(), message.getStock());
            
        } catch (Exception e) {
            log.error("库存同步失败, messageId={}", messageId, e);
            throw e;
        }
    }
    
    /**
     * 消费行为日志消息
     */
    @KafkaListener(
        topics = "${lottery.kafka.topic.behavior:lottery-behavior}",
        groupId = "${lottery.kafka.consumer.group:draw-service-behavior-group}"
    )
    public void consumeBehaviorLog(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            BehaviorLogMessage message = parseBehaviorMessage(record.value());
            
            // 保存到MongoDB
            mongoTemplate.save(message);
            
            // 更新用户画像
            updateUserProfile(message);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("行为日志处理失败", e);
            throw e;
        }
    }
    
    /**
     * 消费通知消息
     */
    @KafkaListener(
        topics = "${lottery.kafka.topic.notification:lottery-notification}",
        groupId = "${lottery.kafka.consumer.group:draw-service-notify-group}"
    )
    public void consumeNotification(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            NotificationMessage message = parseNotificationMessage(record.value());
            
            // 发送通知
            switch (message.getChannel()) {
                case SMS -> notificationService.sendSms(message);
                case PUSH -> notificationService.sendPush(message);
                case WECHAT -> notificationService.sendWechat(message);
                case EMAIL -> notificationService.sendEmail(message);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("通知发送失败", e);
            throw e;
        }
    }
    
    /**
     * 处理奖品发放
     */
    private void processGrantPrize(GrantPrizeMessage message) {
        Long recordId = message.getRecordId();
        
        // 查询中奖记录
        LotteryRecord record = recordRepository.selectOneById(recordId);
        if (record == null) {
            log.error("中奖记录不存在, recordId={}", recordId);
            return;
        }
        
        // 根据奖品类型执行发放
        boolean granted = switch (record.getPrizeType()) {
            case 1 -> grantPoints(record);      // 积分
            case 2 -> grantMembership(record);  // 会员
            case 3 -> grantPhysical(record);    // 实物
            case 4 -> grantCoupon(record);      // 虚拟卡券
            default -> false;
        };
        
        if (granted) {
            // 更新记录状态
            record.setStatus(2); // 已发放
            record.setGrantTime(LocalDateTime.now());
            recordRepository.update(record);
            
            // 发送发放成功通知
            notificationService.notifyGrantSuccess(record);
            
            log.info("奖品发放成功, recordId={}, prizeName={}", recordId, record.getPrizeName());
        } else {
            // 发放失败，记录异常
            record.setStatus(4); // 发放失败
            recordRepository.update(record);
            
            // 发送告警
            notificationService.sendAlert("奖品发放失败", "recordId=" + recordId);
            
            log.error("奖品发放失败, recordId={}", recordId);
        }
    }
    
    /**
     * 发放积分
     */
    private boolean grantPoints(LotteryRecord record) {
        try {
            // 调用积分服务
            return prizeGrantService.grantPoints(record.getUserId(), 
                Integer.parseInt(record.getPrizeValue()));
        } catch (Exception e) {
            log.error("发放积分失败", e);
            return false;
        }
    }
    
    /**
     * 发放会员
     */
    private boolean grantMembership(LotteryRecord record) {
        try {
            // 调用会员服务
            return prizeGrantService.grantMembership(record.getUserId(), 
                Integer.parseInt(record.getPrizeValue()));
        } catch (Exception e) {
            log.error("发放会员失败", e);
            return false;
        }
    }
    
    /**
     * 发放实物奖品
     */
    private boolean grantPhysical(LotteryRecord record) {
        try {
            // 创建物流订单
            return prizeGrantService.createLogisticsOrder(record);
        } catch (Exception e) {
            log.error("发放实物奖品失败", e);
            return false;
        }
    }
    
    /**
     * 发放虚拟卡券
     */
    private boolean grantCoupon(LotteryRecord record) {
        try {
            // 调用卡券服务
            return prizeGrantService.grantCoupon(record.getUserId(), record.getPrizeValue());
        } catch (Exception e) {
            log.error("发放卡券失败", e);
            return false;
        }
    }
    
    /**
     * 同步库存到数据库
     */
    private void syncStockToDatabase(StockSyncMessage message) {
        // 实现库存同步逻辑
    }
    
    /**
     * 更新用户画像
     */
    private void updateUserProfile(BehaviorLogMessage message) {
        // 实现用户画像更新逻辑
    }
    
    /**
     * 检查消息是否已处理（幂等）
     */
    private boolean isMessageProcessed(String messageId) {
        // 从Redis或数据库检查
        return false;
    }
    
    /**
     * 记录消息已处理
     */
    private void recordMessageProcessed(String messageId) {
        // 保存到Redis，设置过期时间
    }
    
    // 解析消息方法
    private GrantPrizeMessage parseMessage(String body) {
        return com.alibaba.fastjson2.JSON.parseObject(body, GrantPrizeMessage.class);
    }
    
    private StockSyncMessage parseStockMessage(String body) {
        return com.alibaba.fastjson2.JSON.parseObject(body, StockSyncMessage.class);
    }
    
    private BehaviorLogMessage parseBehaviorMessage(String body) {
        return com.alibaba.fastjson2.JSON.parseObject(body, BehaviorLogMessage.class);
    }
    
    private NotificationMessage parseNotificationMessage(String body) {
        return com.alibaba.fastjson2.JSON.parseObject(body, NotificationMessage.class);
    }
}