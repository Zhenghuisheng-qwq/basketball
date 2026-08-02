package com.zhs.dao;


import com.zhs.model.BasketMatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;


/**
 * 比赛主表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMatchDao extends BaseMapper<BasketMatch> {

    @Select("""
            SELECT id, match_no, season_id, stage_id, round_id, group_id, venue_id,
                   home_team_id, away_team_id, scheduled_start_time, scheduled_end_time,
                   actual_start_time, actual_end_time, match_status, home_score, away_score,
                   winner_team_id, overtime_count, walkover_team_id, cancel_reason, data_status,
                   result_status, score_confirmed, data_version, origin_match_id, settlement_locked,
                   public_status, sort_no, create_time, update_time, is_delete
            FROM basket_match
            WHERE id = #{matchId} AND is_delete = 0
            FOR UPDATE
            """)
    BasketMatch selectByIdForUpdate(@Param("matchId") Long matchId);

    @Update("""
            UPDATE basket_match
            SET data_status = #{newStatus}, update_time = NOW()
            WHERE id = #{matchId}
              AND is_delete = #{notDeleted}
              AND data_status != 2
            """)
    int updateDataStatusIfAllowed(@Param("matchId") Long matchId,
                                  @Param("newStatus") Integer newStatus,
                                  @Param("notDeleted") Integer notDeleted);

    @Update("""
            UPDATE basket_match
            SET match_status = #{finishedStatus}, update_time = NOW()
            WHERE id = #{matchId}
              AND is_delete = #{notDeleted}
              AND publish_status = #{publishedStatus}
              AND match_status = #{scheduledStatus}
              AND scheduled_start_time IS NOT NULL
              AND scheduled_start_time <= NOW()
            """)
    int finishScheduledMatchIfTimePassed(@Param("matchId") Long matchId,
                                         @Param("scheduledStatus") Integer scheduledStatus,
                                         @Param("finishedStatus") Integer finishedStatus,
                                         @Param("publishedStatus") Integer publishedStatus,
                                         @Param("notDeleted") Integer notDeleted);
}
