package com.zhs.dao;


import com.zhs.model.BasketPrivacySetting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 会员隐私设置表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPrivacySettingDao extends BaseMapper<BasketPrivacySetting> {

}
