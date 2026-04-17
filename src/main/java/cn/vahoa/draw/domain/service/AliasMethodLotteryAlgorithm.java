package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.Prize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 别名方法（Alias Method）抽奖算法
 * 
 * 时间复杂度：O(1) - 预处理 O(n)，查询 O(1)
 * 空间复杂度：O(n)
 * 
 * 适用于高频抽奖场景，预处理一次后可重复使用
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Component
public class AliasMethodLotteryAlgorithm implements LotteryAlgorithm {

    /**
     * 别名表缓存
     */
    private final ConcurrentHashMap<Long, AliasTable> tableCache = new ConcurrentHashMap<>();

    @Override
    public Prize draw(List<Prize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            throw new IllegalArgumentException("奖品列表不能为空");
        }
        
        // 生成缓存key
        Long cacheKey = generateCacheKey(prizes);
        
        // 获取或构建别名表
        AliasTable table = tableCache.computeIfAbsent(cacheKey, k -> buildAliasTable(prizes));
        
        return table.draw(prizes);
    }
    
    /**
     * 构建别名表
     */
    private AliasTable buildAliasTable(List<Prize> prizes) {
        int n = prizes.size();
        
        int[] alias = new int[n];
        double[] prob = new double[n];
        
        // 归一化概率
        double[] normalizedProbs = new double[n];
        double sum = 0.0;
        
        for (int i = 0; i < n; i++) {
            BigDecimal p = prizes.get(i).getProbability();
            sum += p != null ? p.doubleValue() : 0.0;
        }
        
        for (int i = 0; i < n; i++) {
            BigDecimal p = prizes.get(i).getProbability();
            normalizedProbs[i] = (p != null ? p.doubleValue() : 0.0) * n / sum;
        }
        
        // 使用队列分离大于1和小于1的概率
        Deque<Integer> small = new ArrayDeque<>();
        Deque<Integer> large = new ArrayDeque<>();
        
        for (int i = 0; i < n; i++) {
            if (normalizedProbs[i] < 1.0) {
                small.add(i);
            } else {
                large.add(i);
            }
        }
        
        // 构建别名表
        while (!small.isEmpty() && !large.isEmpty()) {
            int l = small.poll();
            int g = large.poll();
            
            prob[l] = normalizedProbs[l];
            alias[l] = g;
            
            normalizedProbs[g] = normalizedProbs[g] + normalizedProbs[l] - 1.0;
            
            if (normalizedProbs[g] < 1.0) {
                small.add(g);
            } else {
                large.add(g);
            }
        }
        
        // 处理剩余
        while (!large.isEmpty()) {
            prob[large.poll()] = 1.0;
        }
        
        while (!small.isEmpty()) {
            prob[small.poll()] = 1.0;
        }
        
        return new AliasTable(alias, prob);
    }
    
    /**
     * 生成缓存key
     */
    private Long generateCacheKey(List<Prize> prizes) {
        // 基于奖品ID和概率生成hash
        long hash = 17;
        for (Prize prize : prizes) {
            hash = 31 * hash + prize.getId();
            hash = 31 * hash + (prize.getProbability() != null ? 
                prize.getProbability().hashCode() : 0);
        }
        return hash;
    }
    
    /**
     * 别名表内部类
     */
    private static class AliasTable {
        private final int[] alias;
        private final double[] prob;
        
        AliasTable(int[] alias, double[] prob) {
            this.alias = alias;
            this.prob = prob;
        }
        
        Prize draw(List<Prize> prizes) {
            int column = ThreadLocalRandom.current().nextInt(prob.length);
            double coinToss = ThreadLocalRandom.current().nextDouble();
            
            int index = coinToss < prob[column] ? column : alias[column];
            return prizes.get(index);
        }
    }
}