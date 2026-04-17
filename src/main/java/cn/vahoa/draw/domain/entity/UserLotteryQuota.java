package cn.vahoa.draw.domain.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户抽奖次数实体
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Table("user_lottery_quota")
public class UserLotteryQuota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 奖池ID
     */
    private Long poolId;

    /**
     * 总次数
     */
    private Integer totalQuota;

    /**
     * 已使用次数
     */
    private Integer usedQuota;

    /**
     * 免费次数
     */
    private Integer freeQuota;

    /**
     * 重置日期
     */
    private LocalDate resetTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 获取剩余次数
     */
    public int getRemainingQuota() {
        return Math.max(0, totalQuota - usedQuota);
    }

    /**
     * 检查是否还有次数
     */
    public boolean hasQuota() {
        return getRemainingQuota() > 0;
    }

    /**
     * 使用一次抽奖机会
     */
    public boolean useQuota() {
        if (!hasQuota()) {
            return false;
        }
        usedQuota++;
        return true;
    }

    /**
     * 增加次数
     */
    public void addQuota(int count) {
        totalQuota += count;
    }

    /**
     * 检查是否需要重置（按日）
     */
    public boolean needReset() {
        return resetTime == null || !resetTime.equals(LocalDate.now());
    }

    /**
     * 执行重置
     */
    public void doReset() {
        this.usedQuota = 0;
        this.resetTime = LocalDate.now();
    }
}