package cn.vahoa.draw.domain.repository;

import cn.vahoa.draw.domain.entity.LotteryRecord;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

import static cn.vahoa.draw.domain.entity.table.LotteryRecordTableDef.LOTTERY_RECORD;

/**
 * 抽奖记录仓储接口
 * 
 * @author vahoa
 * @since 1.0.0
 */
@Mapper
public interface LotteryRecordRepository extends BaseMapper<LotteryRecord> {

    /**
     * 根据用户ID查询记录
     */
    default List<LotteryRecord> findByUserId(String userId, int limit) {
        QueryWrapper query = QueryWrapper.create()
            .select(LOTTERY_RECORD.ALL_COLUMNS)
            .from(LOTTERY_RECORD)
            .where(LOTTERY_RECORD.USER_ID.eq(userId))
            .orderBy(LOTTERY_RECORD.DRAW_TIME.desc())
            .limit(limit);
        return selectListByQuery(query);
    }

    /**
     * 查询用户最近的抽奖记录
     */
    @Select("SELECT * FROM lottery_record WHERE user_id = #{userId} " +
            "ORDER BY draw_time DESC LIMIT #{limit}")
    List<LotteryRecord> findRecentByUserId(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 统计用户抽奖次数
     */
    @Select("SELECT COUNT(*) FROM lottery_record WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") String userId);

    /**
     * 统计用户中奖次数
     */
    @Select("SELECT COUNT(*) FROM lottery_record WHERE user_id = #{userId} " +
            "AND prize_type != 1")
    long countWinsByUserId(@Param("userId") String userId);

    /**
     * 查询用户连续未中奖次数
     */
    default int countConsecutiveLoses(String userId) {
        QueryWrapper query = QueryWrapper.create()
            .select(LOTTERY_RECORD.PRIZE_TYPE)
            .from(LOTTERY_RECORD)
            .where(LOTTERY_RECORD.USER_ID.eq(userId))
            .orderBy(LOTTERY_RECORD.DRAW_TIME.desc());
        
        List<LotteryRecord> records = selectListByQuery(query);
        int count = 0;
        for (LotteryRecord record : records) {
            if (record.getPrizeType() != null && record.getPrizeType() == 1) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 查询时间范围内的记录
     */
    default List<LotteryRecord> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper query = QueryWrapper.create()
            .select(LOTTERY_RECORD.ALL_COLUMNS)
            .from(LOTTERY_RECORD)
            .where(LOTTERY_RECORD.DRAW_TIME.ge(start))
            .and(LOTTERY_RECORD.DRAW_TIME.le(end));
        return selectListByQuery(query);
    }

    /**
     * 根据记录编号查询
     */
    default LotteryRecord findByRecordNo(String recordNo) {
        QueryWrapper query = QueryWrapper.create()
            .select(LOTTERY_RECORD.ALL_COLUMNS)
            .from(LOTTERY_RECORD)
            .where(LOTTERY_RECORD.RECORD_NO.eq(recordNo));
        return selectOneByQuery(query);
    }
}