package com.zhs.basket.service;

import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.vo.PlayerCertAuditRecordVo;
import com.zhs.basket.vo.PlayerCertDetailVo;
import com.zhs.basket.vo.PlayerCertListVo;
import java.util.List;

public interface IBasketPlayerCertService {
    List<PlayerCertListVo> selectList(PlayerCertQuery query);
    PlayerCertDetailVo selectDetail(Long applicationId);
    List<PlayerCertAuditRecordVo> selectAuditRecords(Long applicationId);
    void startReview(Long applicationId);
    void approve(Long applicationId, PlayerCertApproveReq req);
    void reject(Long applicationId, PlayerCertRejectReq req);
}
