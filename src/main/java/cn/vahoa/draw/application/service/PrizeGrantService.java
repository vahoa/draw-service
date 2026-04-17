package cn.vahoa.draw.application.service;

import cn.vahoa.draw.domain.entity.LotteryRecord;

public interface PrizeGrantService {

    boolean grantPoints(String userId, int points);

    boolean grantMembership(String userId, int days);

    boolean createLogisticsOrder(LotteryRecord record);

    boolean grantCoupon(String userId, String couponCode);
}
