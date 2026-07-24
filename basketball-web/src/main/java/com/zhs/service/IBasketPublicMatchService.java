package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicMatchDetailVo;
import com.zhs.response.publicapi.PublicMatchItemVo;

public interface IBasketPublicMatchService {

    IPage<PublicMatchItemVo> pageSchedule(Long seasonId, Integer currentPage, Integer pageSize,
                                          Long stageId, Long groupId, Long teamId);

    IPage<PublicMatchItemVo> pageResult(Long seasonId, Integer currentPage, Integer pageSize,
                                        Long stageId, Long groupId, Long teamId);

    PublicMatchDetailVo getMatch(Long matchId);
}
