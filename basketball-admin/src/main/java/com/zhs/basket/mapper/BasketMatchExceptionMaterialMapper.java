package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchExceptionMaterial;
import com.zhs.basket.vo.MatchDataSubmissionMaterialVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchExceptionMaterialMapper {
    int batchInsert(@Param("list") List<BasketMatchExceptionMaterial> list);

    List<BasketMatchExceptionMaterial> selectByApplicationId(@Param("applicationId") Long applicationId);

    List<MatchDataSubmissionMaterialVo> selectVoByApplicationId(@Param("applicationId") Long applicationId);
}
