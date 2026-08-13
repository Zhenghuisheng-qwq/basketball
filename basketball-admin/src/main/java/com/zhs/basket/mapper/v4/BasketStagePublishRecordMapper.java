package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStagePublishRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketStagePublishRecordMapper {

    BasketStagePublishRecord selectById(@Param("id") Long id);

    BasketStagePublishRecord selectCurrentByStageId(@Param("stageId") Long stageId);

    List<BasketStagePublishRecord> selectByStageId(@Param("stageId") Long stageId);

    Integer selectMaxPublishNo(@Param("stageId") Long stageId);

    int insert(BasketStagePublishRecord entity);

    int clearCurrentByStageId(@Param("stageId") Long stageId);
}
