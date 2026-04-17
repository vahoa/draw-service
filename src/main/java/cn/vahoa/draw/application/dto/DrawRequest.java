package cn.vahoa.draw.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抽奖请求DTO
 *
 * @author vahoa
 * @since 1.0.0
 */
@Data
public class DrawRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private String userId;

    /**
     * 奖池ID
     */
    @NotNull(message = "奖池ID不能为空")
    private Long poolId;
}
