package cn.vahoa.draw.domain.valueobject;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 奖品状态枚举
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Getter
public enum PrizeStatus {
    
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final int code;
    private final String name;

    PrizeStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}