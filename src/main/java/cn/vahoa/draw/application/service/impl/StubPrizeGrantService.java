package cn.vahoa.draw.application.service.impl;

import cn.vahoa.draw.application.service.PrizeGrantService;
import cn.vahoa.draw.domain.entity.LotteryRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubPrizeGrantService implements PrizeGrantService {

    @Override
    public boolean grantPoints(String userId, int points) {
        log.debug("Stub grantPoints userId={} points={}", userId, points);
        return true;
    }

    @Override
    public boolean grantMembership(String userId, int days) {
        log.debug("Stub grantMembership userId={} days={}", userId, days);
        return true;
    }

    @Override
    public boolean createLogisticsOrder(LotteryRecord record) {
        log.debug("Stub createLogisticsOrder recordId={}", record.getId());
        return true;
    }

    @Override
    public boolean grantCoupon(String userId, String couponCode) {
        log.debug("Stub grantCoupon userId={} code={}", userId, couponCode);
        return true;
    }
}
