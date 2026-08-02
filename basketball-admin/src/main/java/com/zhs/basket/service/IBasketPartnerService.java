package com.zhs.basket.service;

import com.zhs.basket.req.PartnerAddReq;
import com.zhs.basket.req.PartnerEditReq;
import com.zhs.basket.req.PartnerQueryReq;
import com.zhs.basket.resp.PartnerListResp;

import java.util.List;

public interface IBasketPartnerService {
    List<PartnerListResp> selectList(PartnerQueryReq query);

    PartnerListResp selectById(Long id);

    Long add(PartnerAddReq req);

    void edit(Long id, PartnerEditReq req);

    void publish(Long id);

    void offline(Long id);

    void remove(Long id);

    /** 按名称查找合作伙伴；不存在则创建并直接发布，供赞助记录录入使用 */
    Long findOrCreatePublishedByName(String partnerName);
}
