package com.zhs.basket.service;

import com.zhs.basket.vo.DisciplineSelectorOptionVo;
import com.zhs.basket.vo.PermanentPlayerArchiveVo;
import com.zhs.basket.vo.PermanentTeamArchiveVo;

import java.util.List;

public interface IBasketPermanentArchiveService {
    PermanentPlayerArchiveVo getPlayerArchive(Long playerId);

    PermanentTeamArchiveVo getTeamArchive(Long teamId);

    List<DisciplineSelectorOptionVo> selectPlayerOptions(String keyword);

    List<DisciplineSelectorOptionVo> selectTeamOptions(String keyword);
}
