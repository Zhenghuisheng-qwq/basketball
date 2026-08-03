package com.zhs.dao.v4;

import com.zhs.model.v4.BasketMatchSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * CNPA 赛事域 V4 Dao：BasketMatchSubmission
 */
@Mapper
@Repository
public interface BasketMatchSubmissionDao extends BaseMapper<BasketMatchSubmission> {
}
