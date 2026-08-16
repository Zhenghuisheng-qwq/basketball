package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineRestriction;
import org.apache.ibatis.annotations.Param;

public interface BasketDisciplineRestrictionMapper {
    BasketDisciplineRestriction selectActiveBySubject(@Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId, @Param("seasonId") Long seasonId);

    BasketDisciplineRestriction selectBySubjectForUpdate(@Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId, @Param("seasonId") Long seasonId);

    int deactivateBySubject(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId,
            @Param("seasonId") Long seasonId);

    int insert(BasketDisciplineRestriction row);

    int updateRemainingSuspensions(@Param("id") Long id, @Param("remaining") Integer remaining);

    int updateProjection(@Param("id") Long id,
            @Param("allowRegistration") Integer allowRegistration,
            @Param("allowTransfer") Integer allowTransfer,
            @Param("allowCaptain") Integer allowCaptain,
            @Param("allowLineup") Integer allowLineup,
            @Param("allowParticipation") Integer allowParticipation,
            @Param("remainingMatchSuspensions") Integer remainingMatchSuspensions,
            @Param("effectiveDisciplineId") Long effectiveDisciplineId,
            @Param("activeFlag") Integer activeFlag);
}
