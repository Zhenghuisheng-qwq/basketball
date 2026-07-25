package com.zhs.basket.service;

import com.zhs.basket.vo.PermanentPlayerArchiveVo;
import com.zhs.basket.vo.PermanentTeamArchiveVo;

public interface IBasketPermanentArchiveService {
    PermanentPlayerArchiveVo getPlayerArchive(Long playerId);

    PermanentTeamArchiveVo getTeamArchive(Long teamId);
}
