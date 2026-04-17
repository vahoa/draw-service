package cn.vahoa.draw.application.service;

import cn.vahoa.draw.domain.entity.LotteryRecord;
import cn.vahoa.draw.interfaces.mq.dto.NotificationMessage;

public interface NotificationService {

    void sendSms(NotificationMessage message);

    void sendPush(NotificationMessage message);

    void sendWechat(NotificationMessage message);

    void sendEmail(NotificationMessage message);

    void notifyGrantSuccess(LotteryRecord record);

    void sendAlert(String title, String detail);
}
