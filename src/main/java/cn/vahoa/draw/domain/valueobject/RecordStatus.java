package cn.vahoa.draw.domain.valueobject;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 中奖记录状态枚举
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Getter
public enum RecordStatus {
    
    PENDING(1, "待发放"),
    GRANTED(2, "已发放"),
    CLAIMED(3, "已领取"),
    EXPIRED(4, "已过期"),
    FAILED(5, "发放失败");

    @EnumValue
    private final int code;
    private final String name;

    RecordStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 是否终态
     */
    public boolean isFinal() {
        return this == CLAIMED || this == EXPIRED || this == FAILED;
    }
}