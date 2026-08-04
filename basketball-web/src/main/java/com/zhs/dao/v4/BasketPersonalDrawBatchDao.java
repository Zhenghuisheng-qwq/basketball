package com.zhs.dao.v4;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.v4.BasketPersonalDrawBatch;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketPersonalDrawBatchDao extends BaseMapper<BasketPersonalDrawBatch> {
}
