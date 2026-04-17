package cn.vahoa.draw.domain.repository;

import cn.vahoa.draw.domain.entity.UserLotteryQuota;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

import static cn.vahoa.draw.domain.entity.table.UserLotteryQuotaTableDef.USER_LOTTERY_QUOTA;

/**
 * 用户抽奖次数仓储接口
 *
 * @author vahoa
 * @since 1.0.0
 */
@Mapper
public interface UserLotteryQuotaRepository extends BaseMapper<UserLotteryQuota> {

    /**
     * 根据用户ID和奖池ID查询
     */
    default UserLotteryQuota findByUserIdAndPoolId(String userId, Long poolId) {
        QueryWrapper query = QueryWrapper.create()
            .select(USER_LOTTERY_QUOTA.ALL_COLUMNS)
            .from(USER_LOTTERY_QUOTA)
            .where(USER_LOTTERY_QUOTA.USER_ID.eq(userId))
            .and(USER_LOTTERY_QUOTA.POOL_ID.eq(poolId));
        return selectOneByQuery(query);
    }

    /**
     * 使用一次抽奖次数
     */
    @Update("UPDATE user_lottery_quota SET used_quota = used_quota + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND pool_id = #{poolId} AND used_quota < total_quota")
    int useQuota(@Param("userId") String userId, @Param("poolId") Long poolId);

    /**
     * 初始化用户抽奖次数
     */
    default UserLotteryQuota initQuota(String userId, Long poolId, int totalQuota, int freeQuota) {
        UserLotteryQuota quota = new UserLotteryQuota();
        quota.setUserId(userId);
        quota.setPoolId(poolId);
        quota.setTotalQuota(totalQuota);
        quota.setUsedQuota(0);
        quota.setFreeQuota(freeQuota);
        quota.setResetTime(LocalDate.now());
        insert(quota);
        return quota;
    }
}
