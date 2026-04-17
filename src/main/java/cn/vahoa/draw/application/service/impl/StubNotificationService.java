package cn.vahoa.draw.application.service.impl;

import cn.vahoa.draw.application.service.NotificationService;
import cn.vahoa.draw.domain.entity.LotteryRecord;
import cn.vahoa.draw.interfaces.mq.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubNotificationService implements NotificationService {

    @Override
    public void sendSms(NotificationMessage message) {
        log.debug("Stub sendSms {}", message);
    }

    @Override
    public void sendPush(NotificationMessage message) {
        log.debug("Stub sendPush {}", message);
    }

    @Override
    public void sendWechat(NotificationMessage message) {
        log.debug("Stub sendWechat {}", message);
    }

    @Override
    public void sendEmail(NotificationMessage message) {
        log.debug("Stub sendEmail {}", message);
    }

    @Override
    public void notifyGrantSuccess(LotteryRecord record) {
        log.info("Stub notifyGrantSuccess recordId={} userId={}", record.getId(), record.getUserId());
    }

    @Override
    public void sendAlert(String title, String detail) {
        log.warn("Stub sendAlert {} {}", title, detail);
    }
}
