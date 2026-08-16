package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineExecutionDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDisciplineExecutionDetailMapper {

    int insert(BasketDisciplineExecutionDetail row);

    int updateStatus(BasketDisciplineExecutionDetail row);

    BasketDisciplineExecutionDetail selectByAction(@Param("disciplineRecordId") Long disciplineRecordId,
                                                   @Param("actionType") String actionType,
                                                   @Param("targetType") String targetType,
                                                   @Param("targetId") Long targetId);

    List<BasketDisciplineExecutionDetail> selectByDisciplineId(@Param("disciplineRecordId") Long disciplineRecordId);
}
