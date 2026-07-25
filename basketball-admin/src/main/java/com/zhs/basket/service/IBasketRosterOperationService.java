package com.zhs.basket.service;

import com.zhs.basket.query.RosterOperationLogQuery;
import com.zhs.basket.req.RosterOperationForceRemoveReq;
import com.zhs.basket.req.RosterOperationUpdateRoleReq;
import com.zhs.basket.vo.RosterOperationLogVo;
import java.util.List;

public interface IBasketRosterOperationService {
    List<RosterOperationLogVo> selectLogs(RosterOperationLogQuery query);
    void forceRemove(RosterOperationForceRemoveReq req);
    void updateRole(RosterOperationUpdateRoleReq req);
}
