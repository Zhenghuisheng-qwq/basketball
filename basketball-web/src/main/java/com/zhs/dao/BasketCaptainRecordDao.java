package com.zhs.dao;


import com.zhs.model.BasketCaptainRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 队长授权记录表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketCaptainRecordDao extends BaseMapper<BasketCaptainRecord> {

}
