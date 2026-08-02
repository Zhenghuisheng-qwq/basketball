package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.vo.MatchDataSubmissionListVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataSubmissionMapper {
    int insert(BasketMatchDataSubmission row);

    BasketMatchDataSubmission selectById(Long id);

    BasketMatchDataSubmission selectByIdForUpdate(Long id);

    BasketMatchDataSubmission selectLatestByMatchId(Long matchId);

    BasketMatchDataSubmission selectLatestByMatchIdAndSubmitterTeamId(@Param("matchId") Long matchId,
            @Param("submitterTeamId") Long submitterTeamId);

    BasketMatchDataSubmission selectActiveByMatchId(Long matchId);

    BasketMatchDataSubmission selectActiveByMatchIdAndSubmitterTeamId(@Param("matchId") Long matchId,
            @Param("submitterTeamId") Long submitterTeamId);

    List<MatchDataSubmissionListVo> selectListForAudit(AdminMatchDataSubmissionPageReq query);

    int countByMatchId(Long matchId);

    int updateStatusIfMatch(@Param("id") Long id,
            @Param("fromSubmissionStatus") Integer fromSubmissionStatus,
            @Param("fromAuditStatus") String fromAuditStatus,
            @Param("toSubmissionStatus") Integer toSubmissionStatus,
            @Param("toAuditStatus") String toAuditStatus,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewedByName") String reviewedByName,
            @Param("rejectReason") String rejectReason,
            @Param("auditRemark") String auditRemark);

    List<MatchDataSubmissionVersionVo> selectVersionsByMatchId(Long matchId);
}
