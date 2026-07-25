package com.zhs.basket.service;

import com.zhs.basket.req.HonorRecordAddReq;
import com.zhs.basket.req.HonorRecordEditReq;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.req.HonorRecordRevokeReq;
import com.zhs.basket.req.PlayerHonorCandidateQueryReq;
import com.zhs.basket.resp.HonorRecordDetailResp;
import com.zhs.basket.resp.HonorRecordListResp;
import com.zhs.basket.resp.PlayerHonorCandidateResp;

import java.util.List;

public interface IBasketHonorRecordService {
    List<HonorRecordListResp> selectList(HonorRecordQueryReq query, String recipientType);

    HonorRecordDetailResp selectById(Long id, String recipientType);

    Long add(HonorRecordAddReq req, String recipientType, String sourceType);

    void edit(Long id, HonorRecordEditReq req, String recipientType);

    void publish(Long id, String recipientType);

    void offline(Long id);

    void revoke(Long id, HonorRecordRevokeReq req, String recipientType);

    void remove(Long id, String recipientType);

    List<PlayerHonorCandidateResp> listPlayerCandidates(PlayerHonorCandidateQueryReq query);

    List<HonorRecordListResp> listImportCandidates(Long tournamentId, Long seasonId);

    Long importFromHonor(Long honorId, String hallSourceType);
}
