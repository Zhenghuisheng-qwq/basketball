package com.zhs.dao;


import com.zhs.model.BasketRenameApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队更名申请表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketRenameApplyDao extends BaseMapper<BasketRenameApply> {

}
