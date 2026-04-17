package cn.vahoa.draw.domain.valueobject;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 奖品类型枚举
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Getter
public enum PrizeType {
    
    POINTS(1, "积分", "虚拟积分，可直接到账"),
    MEMBERSHIP(2, "会员", "会员天数"),
    PHYSICAL(3, "实物", "实物奖品，需填写地址"),
    VOUCHER(4, "卡券", "虚拟卡券，发放兑换码");

    @EnumValue
    private final int code;
    private final String name;
    private final String description;

    PrizeType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static PrizeType of(int code) {
        for (PrizeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid prize type code: " + code);
    }

    /**
     * 是否需要物流
     */
    public boolean needLogistics() {
        return this == PHYSICAL;
    }

    /**
     * 是否虚拟奖品
     */
    public boolean isVirtual() {
        return this == POINTS || this == MEMBERSHIP || this == VOUCHER;
    }
}