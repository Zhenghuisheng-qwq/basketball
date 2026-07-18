package com.zhs.dao;


import com.zhs.model.BasketRemoveApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 移除成员申请表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketRemoveApplyDao extends BaseMapper<BasketRemoveApply> {

}
