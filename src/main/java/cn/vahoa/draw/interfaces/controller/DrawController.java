package cn.vahoa.draw.interfaces.controller;

import cn.vahoa.draw.application.dto.DrawPageInfo;
import cn.vahoa.draw.application.dto.DrawRequest;
import cn.vahoa.draw.application.dto.DrawResult;
import cn.vahoa.draw.application.service.DrawAppService;
import cn.vahoa.draw.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 抽奖控制器
 *
 * @author vahoa
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/draw")
@RequiredArgsConstructor
public class DrawController {

    private final DrawAppService drawAppService;

    /**
     * 获取抽奖页面信息
     */
    @GetMapping("/info")
    public Result<DrawPageInfo> getDrawPageInfo(
            @RequestParam String userId,
            @RequestParam Long poolId) {
        log.info("获取抽奖页面信息，userId={}, poolId={}", userId, poolId);
        DrawPageInfo info = drawAppService.getDrawPageInfo(userId, poolId);
        return Result.success(info);
    }

    /**
     * 执行抽奖
     */
    @PostMapping("/execute")
    public Result<DrawResult> draw(
            @Valid @RequestBody DrawRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String deviceId = httpRequest.getHeader("X-Device-Id");

        log.info("执行抽奖，userId={}, poolId={}, ip={}",
                request.getUserId(), request.getPoolId(), clientIp);

        DrawResult result = drawAppService.draw(request, clientIp, deviceId);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.fail(result.getMessage());
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个IP取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
