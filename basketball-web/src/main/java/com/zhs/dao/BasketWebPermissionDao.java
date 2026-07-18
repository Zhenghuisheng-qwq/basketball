package com.zhs.dao;


import com.zhs.model.BasketWebPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 网页权限表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketWebPermissionDao extends BaseMapper<BasketWebPermission> {

}
