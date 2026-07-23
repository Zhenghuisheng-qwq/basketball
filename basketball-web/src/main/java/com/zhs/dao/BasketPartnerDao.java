package com.zhs.dao;


import com.zhs.model.BasketPartner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 合作伙伴表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPartnerDao extends BaseMapper<BasketPartner> {

}
