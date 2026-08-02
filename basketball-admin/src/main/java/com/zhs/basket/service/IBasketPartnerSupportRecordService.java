package com.zhs.basket.service;

import com.zhs.basket.req.PartnerSupportAddReq;
import com.zhs.basket.req.PartnerSupportEditReq;
import com.zhs.basket.req.PartnerSupportQueryReq;
import com.zhs.basket.resp.PartnerSupportListResp;

import java.util.List;

public interface IBasketPartnerSupportRecordService {
    List<PartnerSupportListResp> selectList(PartnerSupportQueryReq query);

    PartnerSupportListResp selectById(Long id);

    Long add(PartnerSupportAddReq req);

    void edit(Long id, PartnerSupportEditReq req);

    void enable(Long id);

    void disable(Long id);

    void remove(Long id);
}
