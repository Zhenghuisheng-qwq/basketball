package com.zhs.basket.service;

import com.zhs.basket.req.AnnouncementAddReq;
import com.zhs.basket.req.AnnouncementEditReq;
import com.zhs.basket.req.AnnouncementQueryReq;
import com.zhs.basket.req.AnnouncementRuleSupersedeReq;
import com.zhs.basket.req.AnnouncementScheduleReq;
import com.zhs.basket.resp.AnnouncementDetailResp;
import com.zhs.basket.resp.AnnouncementListResp;

import java.util.List;

public interface IBasketAnnouncementService {
    List<AnnouncementListResp> selectList(AnnouncementQueryReq query);

    AnnouncementDetailResp selectById(Long id);

    AnnouncementDetailResp getCurrentRule(Long seasonId);

    Long add(AnnouncementAddReq req);

    void edit(Long id, AnnouncementEditReq req);

    void publish(Long id);

    void offline(Long id);

    void supersedeRule(Long newId, AnnouncementRuleSupersedeReq req);

    void schedulePublish(Long id, AnnouncementScheduleReq req);

    void top(Long id);

    void untop(Long id);

    void remove(Long id);
}
