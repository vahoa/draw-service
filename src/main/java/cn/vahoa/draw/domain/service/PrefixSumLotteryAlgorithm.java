package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.Prize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 前缀和 + 二分查找抽奖算法
 * 
 * 时间复杂度：O(log n)
 * 空间复杂度：O(n)
 * 
 * 适用于奖品数量较多的场景
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Component
public class PrefixSumLotteryAlgorithm implements LotteryAlgorithm {

    @Override
    public Prize draw(List<Prize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            throw new IllegalArgumentException("奖品列表不能为空");
        }
        
        int n = prizes.size();
        
        // 构建前缀和数组
        double[] prefixSum = new double[n];
        double sum = 0.0;
        
        for (int i = 0; i < n; i++) {
            BigDecimal probability = prizes.get(i).getProbability();
            sum += probability != null ? probability.doubleValue() : 0.0;
            prefixSum[i] = sum;
        }
        
        if (sum <= 0) {
            throw new IllegalStateException("奖品概率总和必须大于0");
        }
        
        // 生成随机数 [0, sum)
        double random = ThreadLocalRandom.current().nextDouble() * sum;
        
        // 二分查找
        int index = binarySearch(prefixSum, random);
        
        return prizes.get(index);
    }
    
    /**
     * 二分查找：找到第一个大于等于target的位置
     */
    private int binarySearch(double[] prefixSum, double target) {
        int left = 0;
        int right = prefixSum.length - 1;
        
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (prefixSum[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        
        return left;
    }
}