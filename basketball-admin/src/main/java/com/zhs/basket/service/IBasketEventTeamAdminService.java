package com.zhs.basket.service;

import com.zhs.basket.query.EventTeamAdminQuery;
import com.zhs.basket.req.EventTeamParticipationStatusReq;
import com.zhs.basket.vo.EventTeamDetailVo;
import com.zhs.basket.vo.EventTeamListVo;
import java.util.List;

public interface IBasketEventTeamAdminService {
    List<EventTeamListVo> selectList(EventTeamAdminQuery query);
    EventTeamDetailVo selectDetail(Long id);
    void lockRoster(Long id);
    void unlockRoster(Long id);
    void repairRosterLock(Long id);
    void updateParticipationStatus(Long id, EventTeamParticipationStatusReq req);
}
