package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketMatchDataSubmissionPeriod;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketMatchDataSubmissionPeriodDao extends BaseMapper<BasketMatchDataSubmissionPeriod> {
}
