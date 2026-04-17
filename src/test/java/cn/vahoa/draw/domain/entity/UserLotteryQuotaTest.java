package cn.vahoa.draw.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLotteryQuotaTest {

    @Test
    void remainingAndUseQuota() {
        UserLotteryQuota q = new UserLotteryQuota()
                .setTotalQuota(3)
                .setUsedQuota(1);
        assertEquals(2, q.getRemainingQuota());
        assertTrue(q.useQuota());
        assertEquals(1, q.getRemainingQuota());
        assertTrue(q.useQuota());
        assertFalse(q.useQuota());
        assertEquals(0, q.getRemainingQuota());
    }

    @Test
    void needResetAndDoReset() {
        UserLotteryQuota q = new UserLotteryQuota()
                .setTotalQuota(2)
                .setUsedQuota(2)
                .setResetTime(LocalDate.now().minusDays(1));
        assertTrue(q.needReset());
        q.doReset();
        assertEquals(0, q.getUsedQuota());
        assertEquals(LocalDate.now(), q.getResetTime());
        assertFalse(q.needReset());
    }

    @Test
    void addQuotaNeverNegativeRemainingDisplay() {
        UserLotteryQuota q = new UserLotteryQuota()
                .setTotalQuota(1)
                .setUsedQuota(5);
        assertEquals(0, q.getRemainingQuota());
        q.addQuota(10);
        assertEquals(6, q.getRemainingQuota());
    }
}
