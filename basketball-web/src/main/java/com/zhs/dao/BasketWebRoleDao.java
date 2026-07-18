package com.zhs.dao;


import com.zhs.model.BasketWebRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 网页角色表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketWebRoleDao extends BaseMapper<BasketWebRole> {

}
