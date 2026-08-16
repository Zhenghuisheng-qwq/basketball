package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmission;
import com.zhs.basket.query.v4.BasketMatchSubmissionAuditQuery;
import com.zhs.basket.vo.v4.BasketMatchSubmissionAuditListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchSubmission
 */
public interface BasketMatchSubmissionMapper {
    BasketMatchSubmission selectById(@Param("id") Long id);
    List<BasketMatchSubmission> selectList(BasketMatchSubmission query);
    BasketMatchSubmission selectLatestByMatchAndTeam(@Param("matchId") Long matchId, @Param("submitEventTeamId") Long submitEventTeamId);
    Integer selectMaxVersionNo(@Param("matchId") Long matchId, @Param("submitEventTeamId") Long submitEventTeamId);
    List<BasketMatchSubmission> selectVersionsByMatchAndTeam(@Param("matchId") Long matchId, @Param("submitEventTeamId") Long submitEventTeamId);
    List<BasketMatchSubmissionAuditListVo> selectAuditVoList(BasketMatchSubmissionAuditQuery query);
    int insert(BasketMatchSubmission entity);
    int updateById(BasketMatchSubmission entity);
    int softDelete(@Param("id") Long id);

    int countByScheduleBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    List<BasketMatchSubmission> selectBySeriesSubmissionId(@Param("seriesSubmissionId") Long seriesSubmissionId);
}
