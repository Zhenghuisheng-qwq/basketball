package com.zhs.basket.service;

import com.zhs.basket.query.PlayerPlatformChangeQuery;
import com.zhs.basket.req.PlayerPlatformChangeApproveReq;
import com.zhs.basket.req.PlayerPlatformChangeRejectReq;
import com.zhs.basket.vo.PlayerPlatformChangeDetailVo;
import com.zhs.basket.vo.PlayerPlatformChangeListVo;
import java.util.List;

public interface IBasketPlayerPlatformChangeService {
    List<PlayerPlatformChangeListVo> selectList(PlayerPlatformChangeQuery query);
    PlayerPlatformChangeDetailVo selectDetail(Long id);
    void approve(Long id, PlayerPlatformChangeApproveReq req);
    void reject(Long id, PlayerPlatformChangeRejectReq req);
}
