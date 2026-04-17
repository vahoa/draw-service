package cn.vahoa.draw.interfaces.mq.dto;

import lombok.Data;

@Data
public class NotificationMessage {

    private NotificationChannel channel;

    private String target;

    private String title;

    private String content;

    private String templateId;
}
