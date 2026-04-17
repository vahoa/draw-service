package cn.vahoa.draw.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 抽奖结果DTO
 *
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Builder
public class DrawResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 中奖记录编号
     */
    private String recordNo;

    /**
     * 奖品ID
     */
    private Long prizeId;

    /**
     * 奖品名称
     */
    private String prizeName;

    /**
     * 奖品类型: 1-积分 2-会员 3-实物 4-虚拟卡券
     */
    private Integer prizeType;

    /**
     * 奖品值
     */
    private String prizeValue;

    /**
     * 是否中奖
     */
    private boolean win;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 剩余抽奖次数
     */
    private Integer remainingQuota;
}
