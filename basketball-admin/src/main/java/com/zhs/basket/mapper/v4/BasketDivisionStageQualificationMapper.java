package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionStageQualificationMapper {
    List<BasketDivisionStageQualification> selectPreviewByStageId(@Param("stageId") Long stageId);

    List<BasketDivisionStageQualification> selectConfirmedByStageId(@Param("stageId") Long stageId);

    int insert(BasketDivisionStageQualification entity);

    int batchInsert(@Param("list") List<BasketDivisionStageQualification> list);

    int updateStatus(BasketDivisionStageQualification entity);

    int softDeletePreviewByStageId(@Param("stageId") Long stageId);

    int countByStageIdAndStatus(@Param("stageId") Long stageId, @Param("qualificationStatus") String qualificationStatus);

    int revertConfirmedToPreviewByStageId(@Param("stageId") Long stageId);
}
