package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmissionMaterial;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchSubmissionMaterial
 */
public interface BasketMatchSubmissionMaterialMapper {
    BasketMatchSubmissionMaterial selectById(@Param("id") Long id);
    List<BasketMatchSubmissionMaterial> selectList(BasketMatchSubmissionMaterial query);
    int insert(BasketMatchSubmissionMaterial entity);
    int updateById(BasketMatchSubmissionMaterial entity);
    int softDelete(@Param("id") Long id);
}
