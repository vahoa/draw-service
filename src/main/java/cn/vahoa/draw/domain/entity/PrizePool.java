package cn.vahoa.draw.domain.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 奖池实体
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Table("prize_pool")
public class PrizePool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 奖池ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 奖池编码
     */
    private String poolCode;

    /**
     * 奖池名称
     */
    private String poolName;

    /**
     * 关联活动ID
     */
    private Long activityId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

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
     * 检查奖池是否有效
     */
    public boolean isValid() {
        if (status == null || status != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (startTime == null || !now.isBefore(startTime)) &&
               (endTime == null || !now.isAfter(endTime));
    }

    /**
     * 检查是否已开始
     */
    public boolean isStarted() {
        if (startTime == null) {
            return true;
        }
        return !LocalDateTime.now().isBefore(startTime);
    }

    /**
     * 检查是否已结束
     */
    public boolean isEnded() {
        if (endTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(endTime);
    }
}