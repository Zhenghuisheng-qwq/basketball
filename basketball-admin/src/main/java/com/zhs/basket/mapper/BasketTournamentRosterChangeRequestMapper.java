package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRosterChangeRequest;
import com.zhs.basket.query.RosterChangeAuditQuery;
import com.zhs.basket.vo.RosterChangeAuditDetailVo;
import com.zhs.basket.vo.RosterChangeAuditListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTournamentRosterChangeRequestMapper {
    List<RosterChangeAuditListVo> selectAuditList(RosterChangeAuditQuery query);
    RosterChangeAuditDetailVo selectDetailVo(@Param("id") Long id);
    BasketTournamentRosterChangeRequest selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("reviewerId") Long reviewerId,
            @Param("reviewRemark") String reviewRemark, @Param("rejectReason") String rejectReason,
            @Param("reviewedTime") Date reviewedTime, @Param("resultRosterId") Long resultRosterId);
}
