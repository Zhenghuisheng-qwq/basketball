package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataAudit;
import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.vo.MatchDataAuditListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataAuditMapper {
    int insert(BasketMatchDataAudit row);

    int updateAuditResult(@Param("id") Long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("auditorId") Long auditorId,
            @Param("auditRemark") String auditRemark, @Param("rejectReason") String rejectReason);

    List<MatchDataAuditListVo> selectList(MatchDataAuditQuery query);

    BasketMatchDataAudit selectLatestPendingByMatch(@Param("matchId") Long matchId, @Param("auditType") String auditType);
}
