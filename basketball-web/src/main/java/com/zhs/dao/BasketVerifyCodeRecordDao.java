package com.zhs.dao;


import com.zhs.model.BasketVerifyCodeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 验证码记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketVerifyCodeRecordDao extends BaseMapper<BasketVerifyCodeRecord> {

}
