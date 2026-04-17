package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.Prize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrefixSumLotteryAlgorithmTest {

    private PrefixSumLotteryAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new PrefixSumLotteryAlgorithm();
    }

    @Test
    void drawRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> algorithm.draw(null));
        assertThrows(IllegalArgumentException.class, () -> algorithm.draw(List.of()));
    }

    @Test
    void drawRejectsZeroTotalProbability() {
        Prize a = new Prize().setId(1L).setProbability(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> algorithm.draw(List.of(a)));
    }

    @Test
    void singlePrizeAlwaysReturned() {
        Prize only = new Prize().setId(99L).setProbability(new BigDecimal("0.25"));
        assertSame(only, algorithm.draw(List.of(only)));
    }
}
