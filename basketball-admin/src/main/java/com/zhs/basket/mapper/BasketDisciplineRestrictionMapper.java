package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineRestriction;
import org.apache.ibatis.annotations.Param;

public interface BasketDisciplineRestrictionMapper {
    BasketDisciplineRestriction selectActiveBySubject(@Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId, @Param("seasonId") Long seasonId);

    int deactivateBySubject(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId,
            @Param("seasonId") Long seasonId);

    int insert(BasketDisciplineRestriction row);

    int updateRemainingSuspensions(@Param("id") Long id, @Param("remaining") Integer remaining);
}
