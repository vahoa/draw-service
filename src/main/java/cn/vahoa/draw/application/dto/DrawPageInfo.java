package cn.vahoa.draw.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 抽奖页面信息DTO
 *
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Builder
public class DrawPageInfo {

    /**
     * 奖品列表
     */
    private List<PrizeDTO> prizes;

    /**
     * 剩余抽奖次数
     */
    private Integer remainingQuota;

    /**
     * 总抽奖次数
     */
    private Integer totalQuota;

    /**
     * 已使用次数
     */
    private Integer usedQuota;

    /**
     * 最近中奖通知列表
     */
    private List<String> recentWinNotices;

    /**
     * 奖品DTO
     */
    @Data
    @Builder
    public static class PrizeDTO {
        private Long id;
        private String name;
        private Integer type;
        private String imageUrl;
        private String value;
    }
}
