package com.zhs.service;

import com.zhs.response.publicapi.PublicStandingItemVo;

import java.util.List;

public interface IBasketPublicStandingService {

    List<PublicStandingItemVo> listBySeason(Long seasonId, Long stageId);

    List<PublicStandingItemVo> listByStage(Long stageId);

    List<PublicStandingItemVo> listByGroup(Long groupId);
}
