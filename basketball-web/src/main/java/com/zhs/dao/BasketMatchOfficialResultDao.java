package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketMatchOfficialResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketMatchOfficialResultDao extends BaseMapper<BasketMatchOfficialResult> {

    @Select("""
            SELECT id, match_id, data_version, home_score, away_score, winner_team_id, loser_team_id,
                   is_draw, result_type, effective_time, confirmed_by, confirmed_time, is_current,
                   is_public, revision_no, create_time, update_time, is_delete
            FROM basket_match_official_result
            WHERE match_id = #{matchId} AND is_current = 1 AND is_delete = 0
            LIMIT 1
            """)
    BasketMatchOfficialResult selectCurrentByMatchId(@Param("matchId") Long matchId);
}
