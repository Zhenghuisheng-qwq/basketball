package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketMatchDataSubmissionMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketMatchDataSubmissionMaterialDao extends BaseMapper<BasketMatchDataSubmissionMaterial> {
}
