package com.zhs.basket.service;

import com.zhs.basket.req.SeasonResultConfirmReq;
import com.zhs.basket.req.SeasonResultRevokeReq;
import com.zhs.basket.resp.SeasonCandidateTeamResp;
import com.zhs.basket.resp.SeasonEndCheckResp;
import com.zhs.basket.resp.SeasonResultDetailResp;
import com.zhs.basket.resp.SeasonResultListResp;

import java.util.List;

public interface IBasketSeasonResultService {
    List<SeasonResultListResp> list(Long tournamentId);

    SeasonResultDetailResp getDetail(Long seasonId);

    List<SeasonCandidateTeamResp> listCandidateTeams(Long seasonId);

    SeasonEndCheckResp endCheck(Long seasonId);

    void confirm(Long seasonId, SeasonResultConfirmReq req);

    void publish(Long seasonId);

    void revoke(Long seasonId, SeasonResultRevokeReq req);
}
