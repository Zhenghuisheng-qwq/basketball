package com.zhs.dao;


import com.zhs.model.BasketPlayerCertApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球员认证申请表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPlayerCertApplicationDao extends BaseMapper<BasketPlayerCertApplication> {

}
