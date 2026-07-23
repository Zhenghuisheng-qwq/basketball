package com.zhs.dao;


import com.zhs.model.BasketPlayerCertAudit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球员认证审核记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPlayerCertAuditDao extends BaseMapper<BasketPlayerCertAudit> {

}
