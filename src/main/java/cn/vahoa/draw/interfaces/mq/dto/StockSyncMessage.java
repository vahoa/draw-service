package cn.vahoa.draw.interfaces.mq.dto;

import lombok.Data;

@Data
public class StockSyncMessage {

    private Long prizeId;
    private Integer stock;
}
