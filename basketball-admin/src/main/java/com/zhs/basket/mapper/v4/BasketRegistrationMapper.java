package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.vo.v4.BasketRegistrationAuditListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketRegistrationMapper {
    BasketRegistration selectById(@Param("id") Long id);

    List<BasketRegistration> selectList(BasketRegistration query);

    List<BasketRegistration> selectByDivisionId(@Param("divisionId") Long divisionId);

    BasketRegistration selectByDivisionTeam(@Param("divisionId") Long divisionId, @Param("teamId") Long teamId);

    List<BasketRegistrationAuditListVo> selectAuditVoList(BasketRegistrationAuditQuery query);

    int countApprovedByDivisionId(@Param("divisionId") Long divisionId);

    int countApprovedByDivisionAndRole(@Param("divisionId") Long divisionId, @Param("participationRole") String participationRole);

    int selectMaxCandidateOrder(@Param("divisionId") Long divisionId);

    int updateParticipation(BasketRegistration entity);

    int countPendingReviewByDivisionId(@Param("divisionId") Long divisionId);

    int countActiveByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketRegistration entity);

    int updateById(BasketRegistration entity);

    int bumpLineupVersion(@Param("id") Long id, @Param("lineupCount") Integer lineupCount, @Param("expectedVersion") Integer expectedVersion);

    int softDelete(@Param("id") Long id);
}
