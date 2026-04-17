package cn.vahoa.draw.domain.service;

import cn.vahoa.draw.domain.entity.Prize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliasMethodLotteryAlgorithmTest {

    private AliasMethodLotteryAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new AliasMethodLotteryAlgorithm();
    }

    @Test
    void drawRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> algorithm.draw(null));
        assertThrows(IllegalArgumentException.class, () -> algorithm.draw(List.of()));
    }

    @Test
    void singlePrizeAlwaysReturned() {
        Prize only = new Prize().setId(42L).setProbability(new BigDecimal("1"));
        assertSame(only, algorithm.draw(List.of(only)));
    }
}
