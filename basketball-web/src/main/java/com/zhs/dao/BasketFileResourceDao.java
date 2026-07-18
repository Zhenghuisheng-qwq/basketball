package com.zhs.dao;


import com.zhs.model.BasketFileResource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 文件资源表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketFileResourceDao extends BaseMapper<BasketFileResource> {

}
