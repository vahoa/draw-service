package cn.vahoa.draw.domain.repository;

import cn.vahoa.draw.domain.entity.Prize;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static cn.vahoa.draw.domain.entity.table.PrizeTableDef.PRIZE;

/**
 * 奖品仓储接口
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Mapper
public interface PrizeRepository extends BaseMapper<Prize> {

    /**
     * 根据奖池ID查询可用奖品列表
     */
    default List<Prize> findAvailableByPoolId(Long poolId) {
        QueryWrapper query = QueryWrapper.create()
            .select(PRIZE.ALL_COLUMNS)
            .from(PRIZE)
            .where(PRIZE.STATUS.eq(1))
            .and(PRIZE.STOCK.gt(0).or(PRIZE.STOCK.eq(-1)))
            .orderBy(PRIZE.SORT_ORDER.asc());
        return selectListByQuery(query);
    }

    /**
     * 乐观锁扣减库存
     */
    @Update("UPDATE prize SET stock = stock - 1, updated_at = NOW() " +
            "WHERE id = #{prizeId} AND (stock > 0 OR stock = -1)")
    int deductStock(@Param("prizeId") Long prizeId);

    /**
     * 批量扣减库存（带版本号）
     */
    @Update("UPDATE prize SET stock = stock - #{quantity}, updated_at = NOW() " +
            "WHERE id = #{prizeId} AND stock >= #{quantity}")
    int deductStockWithQuantity(@Param("prizeId") Long prizeId, @Param("quantity") int quantity);

    /**
     * 增加今日发放数量
     */
    @Update("UPDATE prize SET today_sent = today_sent + 1, updated_at = NOW() " +
            "WHERE id = #{prizeId}")
    int incrementTodaySent(@Param("prizeId") Long prizeId);

    /**
     * 重置每日发放数量
     */
    @Update("UPDATE prize SET today_sent = 0, updated_at = NOW()")
    int resetDailySent();
}