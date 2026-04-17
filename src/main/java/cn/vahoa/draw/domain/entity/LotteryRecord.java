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
 * 抽奖记录实体
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Table("lottery_record")
public class LotteryRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 中奖记录编号
     */
    private String recordNo;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 奖池ID
     */
    private Long poolId;

    /**
     * 奖品ID
     */
    private Long prizeId;

    /**
     * 奖品名称
     */
    private String prizeName;

    /**
     * 奖品类型
     */
    private Integer prizeType;

    /**
     * 奖品值
     */
    private String prizeValue;

    /**
     * 状态: 1-待发放 2-已发放 3-已领取
     */
    private Integer status;

    /**
     * 抽奖时间
     */
    private LocalDateTime drawTime;

    /**
     * 发放时间
     */
    private LocalDateTime grantTime;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 生成记录编号
     */
    public void generateRecordNo() {
        this.recordNo = "DR" + System.currentTimeMillis() + 
                       String.format("%04d", (int)(Math.random() * 10000));
    }

    /**
     * 标记为已发放
     */
    public void markAsGranted() {
        this.status = 2;
        this.grantTime = LocalDateTime.now();
    }

    /**
     * 标记为已领取
     */
    public void markAsClaimed() {
        this.status = 3;
    }

    /**
     * 是否为实物奖品
     */
    public boolean isPhysicalPrize() {
        return prizeType != null && prizeType == 3;
    }
}