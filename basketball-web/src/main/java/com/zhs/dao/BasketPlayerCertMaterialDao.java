package com.zhs.dao;


import com.zhs.model.BasketPlayerCertMaterial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球员认证材料表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPlayerCertMaterialDao extends BaseMapper<BasketPlayerCertMaterial> {

}
