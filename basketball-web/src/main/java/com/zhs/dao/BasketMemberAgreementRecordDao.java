package com.zhs.dao;


import com.zhs.model.BasketMemberAgreementRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 会员协议确认记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMemberAgreementRecordDao extends BaseMapper<BasketMemberAgreementRecord> {

}
