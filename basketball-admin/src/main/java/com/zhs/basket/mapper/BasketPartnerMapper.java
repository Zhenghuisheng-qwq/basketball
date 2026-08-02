package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPartner;
import com.zhs.basket.req.PartnerQueryReq;
import com.zhs.basket.resp.PartnerListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPartnerMapper {
    BasketPartner selectById(@Param("id") Long id);

    PartnerListResp selectDetailById(@Param("id") Long id);

    List<PartnerListResp> selectList(PartnerQueryReq query);

    int countByName(@Param("partnerName") String partnerName, @Param("excludeId") Long excludeId);

    BasketPartner selectByName(@Param("partnerName") String partnerName);

    Integer selectMaxPartnerCodeSequence();

    int insert(BasketPartner partner);

    int updateById(BasketPartner partner);

    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatus") Integer fromStatus,
                            @Param("toStatus") Integer toStatus);

    int softDelete(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses);
}
