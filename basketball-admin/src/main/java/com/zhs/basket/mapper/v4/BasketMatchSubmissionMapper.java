package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchSubmission
 */
public interface BasketMatchSubmissionMapper {
    BasketMatchSubmission selectById(@Param("id") Long id);
    List<BasketMatchSubmission> selectList(BasketMatchSubmission query);
    int insert(BasketMatchSubmission entity);
    int updateById(BasketMatchSubmission entity);
    int softDelete(@Param("id") Long id);
}
