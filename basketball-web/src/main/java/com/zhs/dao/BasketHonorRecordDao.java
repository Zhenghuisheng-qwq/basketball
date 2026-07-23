package com.zhs.dao;


import com.zhs.model.BasketHonorRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 荣誉记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketHonorRecordDao extends BaseMapper<BasketHonorRecord> {

}
