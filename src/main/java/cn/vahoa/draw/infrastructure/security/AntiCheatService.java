package cn.vahoa.draw.infrastructure.security;

import cn.vahoa.draw.domain.entity.AntiCheatLog;
import cn.vahoa.draw.domain.entity.UserLotteryStats;
import cn.vahoa.draw.infrastructure.cache.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 防刷服务
 * <p>
 * 多层防护体系：
 * 1. 频率限制检测
 * 2. 设备指纹检测
 * 3. IP风险检测
 * 4. 行为模式检测
 * 5. 关联分析
 *
 * @author vahoa
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiCheatService {

    private final RedisCacheService redisCacheService;
    private final MongoTemplate mongoTemplate;

    // 配置参数
    private static final int USER_RATE_LIMIT = 5;      // 用户每分钟限制
    private static final int IP_RATE_LIMIT = 20;       // IP每分钟限制
    private static final int WINDOW_SIZE = 60;         // 滑动窗口（秒）
    private static final int DEVICE_USER_LIMIT = 5;    // 设备关联用户数限制
    private static final int IP_SEGMENT_LIMIT = 50;    // IP段用户数限制

    /**
     * 执行防刷检测
     *
     * @param userId  用户ID
     * @param request HTTP请求
     * @return 检测结果
     */
    public AntiCheatResult check(String userId, HttpServletRequest request) {
        String ip = getClientIp(request);
        String deviceId = request.getHeader("X-Device-ID");
        String fingerprint = generateFingerprint(request);
        
        RiskScore score = new RiskScore();
        List<String> reasons = new ArrayList<>();
        
        // 1. 频率限制检测
        if (!checkFrequencyLimit(userId, ip)) {
            score.add(30);
            reasons.add("频率超限");
        }
        
        // 2. 设备指纹检测
        if (checkDeviceRisk(deviceId)) {
            score.add(25);
            reasons.add("设备风险");
        }
        
        // 3. IP风险检测
        if (checkIpRisk(ip)) {
            score.add(20);
            reasons.add("IP风险");
        }
        
        // 4. 行为模式检测
        if (checkBehaviorPattern(userId)) {
            score.add(15);
            reasons.add("行为异常");
        }
        
        // 5. 关联分析
        if (checkAssociation(userId, ip, deviceId)) {
            score.add(10);
            reasons.add("关联风险");
        }
        
        // 记录检测日志
        recordCheckLog(userId, ip, deviceId, score.getTotal(), reasons);
        
        // 根据风险评分决策
        if (score.getTotal() >= 60) {
            // 加入黑名单
            addToBlacklist(userId, ip);
            return AntiCheatResult.block("检测到异常行为，请稍后再试");
        } else if (score.getTotal() >= 30) {
            return AntiCheatResult.captcha("请完成验证后继续");
        }
        
        return AntiCheatResult.pass();
    }
    
    /**
     * 频率限制检测
     */
    private boolean checkFrequencyLimit(String userId, String ip) {
        // 用户维度限流
        if (!redisCacheService.checkUserRateLimit(userId, USER_RATE_LIMIT, WINDOW_SIZE)) {
            log.warn("用户[{}]频率超限", userId);
            return false;
        }
        
        // IP维度限流
        if (!redisCacheService.checkIpRateLimit(ip, IP_RATE_LIMIT, WINDOW_SIZE)) {
            log.warn("IP[{}]频率超限", ip);
            return false;
        }
        
        // 滑动窗口限流（更严格）
        if (!redisCacheService.checkSlidingWindow(userId, 2, 10)) {
            log.warn("用户[{}]滑动窗口超限", userId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 设备指纹风险检测
     */
    private boolean checkDeviceRisk(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return true; // 无设备ID视为风险
        }
        
        // 检查该设备关联的用户数
        long userCount = redisCacheService.getDeviceUserCount(deviceId);
        if (userCount > DEVICE_USER_LIMIT) {
            log.warn("设备[{}]关联用户数[{}]超过限制", deviceId, userCount);
            return true;
        }
        
        // 检查设备是否在黑名单
        if (redisCacheService.isDeviceBlacklisted(deviceId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * IP风险检测
     */
    private boolean checkIpRisk(String ip) {
        // 检查IP黑名单
        if (redisCacheService.isIpBlacklisted(ip)) {
            return true;
        }
        
        // 检查IP段风险
        String ipSegment = getIpSegment(ip);
        long segmentCount = redisCacheService.getIpSegmentUserCount(ipSegment);
        if (segmentCount > IP_SEGMENT_LIMIT) {
            log.warn("IP段[{}]用户数[{}]超过限制", ipSegment, segmentCount);
            return true;
        }
        
        // 检查是否为代理IP/VPN
        if (isProxyOrVpn(ip)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 行为模式检测
     */
    private boolean checkBehaviorPattern(String userId) {
        // 获取用户近期行为记录
        List<DrawBehavior> behaviors = getRecentBehaviors(userId, 24);
        
        // 1. 固定间隔检测（脚本特征）
        if (isFixedInterval(behaviors)) {
            log.warn("用户[{}]存在固定间隔抽奖行为", userId);
            return true;
        }
        
        // 2. 时间分布异常（集中在凌晨）
        if (isAbnormalTimeDistribution(behaviors)) {
            log.warn("用户[{}]抽奖时间分布异常", userId);
            return true;
        }
        
        // 3. 操作路径异常（直接调用API）
        if (isDirectApiCall(behaviors)) {
            log.warn("用户[{}]存在直接API调用行为", userId);
            return true;
        }
        
        // 4. 中奖率异常
        if (isAbnormalWinRate(userId)) {
            log.warn("用户[{}]中奖率异常", userId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 关联分析 - 检测羊毛党团伙
     */
    private boolean checkAssociation(String userId, String ip, String deviceId) {
        // 检查用户-设备-IP关联网络
        // 如果该用户使用了多个高风险设备/IP
        
        // 获取用户关联的设备列表
        List<String> userDevices = redisCacheService.getUserDevices(userId);
        int riskDeviceCount = 0;
        for (String device : userDevices) {
            if (redisCacheService.isDeviceHighRisk(device)) {
                riskDeviceCount++;
            }
        }
        
        if (riskDeviceCount >= 3) {
            log.warn("用户[{}]使用了[{}]个高风险设备", userId, riskDeviceCount);
            return true;
        }
        
        // 检查IP关联
        List<String> userIps = redisCacheService.getUserIps(userId);
        int riskIpCount = 0;
        for (String userIp : userIps) {
            if (redisCacheService.isIpHighRisk(userIp)) {
                riskIpCount++;
            }
        }
        
        if (riskIpCount >= 5) {
            log.warn("用户[{}]使用了[{}]个高风险IP", userId, riskIpCount);
            return true;
        }
        
        return false;
    }
    
    /**
     * 检测固定间隔
     */
    private boolean isFixedInterval(List<DrawBehavior> behaviors) {
        if (behaviors.size() < 5) return false;
        
        // 计算相邻行为的时间差
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < behaviors.size(); i++) {
            long diff = behaviors.get(i).getTimestamp() - behaviors.get(i-1).getTimestamp();
            intervals.add(diff);
        }
        
        // 检查时间差是否过于一致（方差很小）
        double variance = calculateVariance(intervals);
        return variance < 100; // 方差小于100ms视为固定间隔
    }
    
    /**
     * 检测异常时间分布
     */
    private boolean isAbnormalTimeDistribution(List<DrawBehavior> behaviors) {
        if (behaviors.size() < 10) return false;
        
        // 统计凌晨时段（0-6点）的行为占比
        long midnightCount = behaviors.stream()
            .filter(b -> {
                int hour = b.getHour();
                return hour >= 0 && hour < 6;
            })
            .count();
        
        double ratio = (double) midnightCount / behaviors.size();
        return ratio > 0.8; // 80%以上在凌晨视为异常
    }
    
    /**
     * 检测直接API调用
     */
    private boolean isDirectApiCall(List<DrawBehavior> behaviors) {
        // 检查是否有Referer或页面访问记录
        long directCallCount = behaviors.stream()
            .filter(b -> !b.hasReferer() && !b.hasPageView())
            .count();
        
        return directCallCount > behaviors.size() * 0.9;
    }
    
    /**
     * 检测异常中奖率
     */
    private boolean isAbnormalWinRate(String userId) {
        // 从MongoDB查询用户统计数据
        UserLotteryStats stats = mongoTemplate.findById(userId, UserLotteryStats.class);
        if (stats == null || stats.getTotalDraws() < 50) return false;
        
        double winRate = (double) stats.getWins() / stats.getTotalDraws();
        
        // 中奖率过高或过低都视为异常
        return winRate > 0.5 || winRate < 0.01;
    }
    
    /**
     * 加入黑名单
     */
    private void addToBlacklist(String userId, String ip) {
        redisCacheService.addToBlacklist(userId, ip, 30); // 30分钟
        log.warn("用户[{}] IP[{}]已加入黑名单", userId, ip);
    }
    
    /**
     * 记录检测日志
     */
    private void recordCheckLog(String userId, String ip, String deviceId, 
                                 int riskScore, List<String> reasons) {
        AntiCheatLog cheatLog = new AntiCheatLog()
            .setUserId(userId)
            .setClientIp(ip)
            .setDeviceId(deviceId)
            .setRiskLevel(riskScore >= 60 ? 3 : riskScore >= 30 ? 2 : 1)
            .setRiskScore(riskScore)
            .setReasons(reasons)
            .setCreatedAt(LocalDateTime.now());
        
        mongoTemplate.save(cheatLog);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // 取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 生成设备指纹
     */
    private String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String accept = request.getHeader("Accept");
        String acceptLang = request.getHeader("Accept-Language");
        String acceptEnc = request.getHeader("Accept-Encoding");
        
        String raw = userAgent + accept + acceptLang + acceptEnc;
        return org.springframework.util.DigestUtils.md5DigestAsHex(raw.getBytes());
    }
    
    /**
     * 获取IP段
     */
    private String getIpSegment(String ip) {
        if (ip == null || !ip.contains(".")) return ip;
        int lastDot = ip.lastIndexOf('.');
        return ip.substring(0, lastDot);
    }
    
    /**
     * 检测代理/VPN
     */
    private boolean isProxyOrVpn(String ip) {
        // 可接入第三方IP检测服务
        // 这里简化处理
        return false;
    }
    
    /**
     * 计算方差
     */
    private double calculateVariance(List<Long> values) {
        if (values.isEmpty()) return 0;
        
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        
        return variance;
    }
    
    /**
     * 获取近期行为
     */
    private List<DrawBehavior> getRecentBehaviors(String userId, int hours) {
        // 从MongoDB查询
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        // 实现查询逻辑
        return new ArrayList<>();
    }

    private static final class RiskScore {
        private int total;

        void add(int value) {
            total += value;
        }

        int getTotal() {
            return total;
        }
    }
}