package com.zhs.dao;


import com.zhs.model.BasketCaptainApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 队长转让申请表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketCaptainApplyDao extends BaseMapper<BasketCaptainApply> {

}
