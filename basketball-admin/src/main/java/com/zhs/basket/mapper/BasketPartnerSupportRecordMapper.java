package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPartnerSupportRecord;
import com.zhs.basket.req.PartnerSupportQueryReq;
import com.zhs.basket.resp.PartnerSupportListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPartnerSupportRecordMapper {
    BasketPartnerSupportRecord selectById(@Param("id") Long id);

    PartnerSupportListResp selectDetailById(@Param("id") Long id);

    List<PartnerSupportListResp> selectList(PartnerSupportQueryReq query);

    int insert(BasketPartnerSupportRecord record);

    int updateById(BasketPartnerSupportRecord record);

    int updatePublishStatus(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    int softDelete(@Param("id") Long id);

    int countActiveByPartnerId(@Param("partnerId") Long partnerId);
}
