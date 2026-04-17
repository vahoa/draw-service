package cn.vahoa.draw.domain.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@Document("anti_cheat_log")
public class AntiCheatLog {

    @Id
    private String id;

    private String userId;

    private String clientIp;

    private String deviceId;

    private Integer riskLevel;

    private Integer riskScore;

    private List<String> reasons;

    private LocalDateTime createdAt;
}
