package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentOpenRegistration;
import com.zhs.basket.query.OpenRegistrationAuditQuery;
import com.zhs.basket.vo.OpenRegistrationAuditDetailVo;
import com.zhs.basket.vo.OpenRegistrationAuditListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTournamentOpenRegistrationMapper {
    List<OpenRegistrationAuditListVo> selectAuditList(OpenRegistrationAuditQuery query);
    OpenRegistrationAuditDetailVo selectDetailVo(@Param("id") Long id);
    BasketTournamentOpenRegistration selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("currentAuditorId") Long currentAuditorId,
            @Param("remark") String remark, @Param("auditTime") Date auditTime, @Param("auditorId") Long auditorId,
            @Param("resultRosterId") Long resultRosterId);
}
