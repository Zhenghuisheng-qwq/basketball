package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataDisputeMaterial;
import com.zhs.basket.vo.MatchDataSubmissionMaterialVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataDisputeMaterialMapper {
    int batchInsert(@Param("list") List<BasketMatchDataDisputeMaterial> list);

    List<BasketMatchDataDisputeMaterial> selectByDisputeId(@Param("disputeId") Long disputeId);

    List<MatchDataSubmissionMaterialVo> selectVoByDisputeId(@Param("disputeId") Long disputeId);
}
