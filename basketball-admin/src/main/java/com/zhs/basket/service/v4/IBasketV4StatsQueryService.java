package com.zhs.basket.service.v4;

import com.zhs.basket.query.v4.BasketV4StatsQuery;
import com.zhs.basket.vo.v4.*;

import java.util.List;

public interface IBasketV4StatsQueryService {
    List<BasketStandingListVo> listStanding(BasketV4StatsQuery query);

    List<BasketTeamStatListVo> listTeamStat(BasketV4StatsQuery query);

    List<BasketPlayerStatListVo> listPlayerStat(BasketV4StatsQuery query);

    List<BasketRankListVo> listRank(BasketV4StatsQuery query);
}
