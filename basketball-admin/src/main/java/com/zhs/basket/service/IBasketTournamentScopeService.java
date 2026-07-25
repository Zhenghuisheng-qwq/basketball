package com.zhs.basket.service;

import com.zhs.basket.req.TournamentScopeGrantReq;
import com.zhs.basket.vo.TournamentScopeListVo;

import java.util.List;

public interface IBasketTournamentScopeService {
    List<TournamentScopeListVo> list(Long tournamentId, Long adminUserId);

    Long grant(TournamentScopeGrantReq req);

    void revoke(Long id);
}
