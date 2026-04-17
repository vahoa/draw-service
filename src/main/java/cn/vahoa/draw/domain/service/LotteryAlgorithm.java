package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.Prize;

import java.util.List;

/**
 * 抽奖算法接口
 * 
 * @author vahoa
 * @since 1.0.0
 */
public interface LotteryAlgorithm {
    
    /**
     * 执行抽奖
     * 
     * @param prizes 可用奖品列表
     * @return 中奖奖品
     */
    Prize draw(List<Prize> prizes);
    
    /**
     * 批量初始化（别名方法等需要预处理的算法）
     * 
     * @param prizes 奖品列表
     */
    default void initialize(List<Prize> prizes) {
        // 默认空实现
    }
}