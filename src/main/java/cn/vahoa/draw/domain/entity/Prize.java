package cn.vahoa.draw.domain.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖品实体
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Table("prize")
public class Prize implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 奖品ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 奖品类型: 1-积分 2-会员 3-实物 4-虚拟卡券
     */
    private Integer type;

    /**
     * 奖品值(积分数量/会员天数/卡券码)
     */
    private String value;

    /**
     * 奖品图片URL
     */
    private String imageUrl;

    /**
     * 中奖概率(0-1)
     */
    private BigDecimal probability;

    /**
     * 库存数量,-1表示无限
     */
    private Integer stock;

    /**
     * 每日发放上限,-1表示无限制
     */
    private Integer dailyLimit;

    /**
     * 今日已发放数量
     */
    private Integer todaySent;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查是否有库存
     */
    public boolean hasStock() {
        return stock == -1 || stock > 0;
    }

    /**
     * 检查是否达到每日限额
     */
    public boolean isDailyLimitReached() {
        return dailyLimit != -1 && todaySent >= dailyLimit;
    }

    /**
     * 扣减库存
     */
    public void deductStock() {
        if (stock > 0) {
            stock--;
        }
    }

    /**
     * 增加今日发放数量
     */
    public void incrementTodaySent() {
        todaySent++;
    }
}