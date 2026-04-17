package cn.vahoa.draw.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("user_lottery_stats")
public class UserLotteryStats {

    @Id
    private String id;

    private int totalDraws;

    private int wins;
}
