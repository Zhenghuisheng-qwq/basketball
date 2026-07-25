package com.zhs.basket.service;

import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.vo.MatchDataAuditListVo;

import java.util.List;

public interface IBasketMatchDataAuditService {
    List<MatchDataAuditListVo> listPending(MatchDataAuditQuery query);
}
