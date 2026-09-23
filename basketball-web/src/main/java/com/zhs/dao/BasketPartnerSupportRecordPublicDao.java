package com.zhs.dao;

import com.zhs.response.publicapi.PublicPartnerSupportRecordResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BasketPartnerSupportRecordPublicDao {

    Long countPublicRecords();

    Long countPublicSupportRows();

    BigDecimal sumPublicCashAmount();

    List<PublicPartnerSupportRecordResp> selectPublicPage(@Param("offset") long offset, @Param("limit") int limit);
}
