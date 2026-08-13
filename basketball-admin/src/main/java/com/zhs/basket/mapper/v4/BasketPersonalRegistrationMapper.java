package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalRegistration;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalRegistrationMapper {
    BasketPersonalRegistration selectById(@Param("id") Long id);

    BasketPersonalRegistration selectActiveByDivisionPlayer(@Param("divisionId") Long divisionId, @Param("playerId") Long playerId);

    List<BasketPersonalRegistrationListVo> selectVoList(BasketPersonalRegistrationQuery query);

    int countApprovedByDivisionId(@Param("divisionId") Long divisionId);

    java.util.List<BasketPersonalRegistration> selectApprovedByDivisionId(@Param("divisionId") Long divisionId);

    int updateAssignedDrawTeamId(@Param("id") Long id, @Param("assignedDrawTeamId") Long assignedDrawTeamId);

    int countPendingReviewByDivisionId(@Param("divisionId") Long divisionId);

    int countActiveByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketPersonalRegistration entity);

    int updateById(BasketPersonalRegistration entity);

    int softDelete(@Param("id") Long id);

    int updateActiveUniqueKey(@Param("id") Long id, @Param("activeUniqueKey") String activeUniqueKey);
}
