package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.BasketV4AdminOptionVo;
import com.zhs.basket.vo.v4.BasketV4GrantablePlayerOptionVo;
import com.zhs.basket.vo.v4.BasketV4TeamPlayerOptionVo;

import java.util.List;

public interface IBasketV4AdminOptionsService {

    List<BasketV4AdminOptionVo> selectSeasonOptions();

    List<BasketV4AdminOptionVo> selectDivisionOptions(Long seasonId);

    List<BasketV4AdminOptionVo> selectGrantableTeamOptions(Long divisionId);

    List<BasketV4TeamPlayerOptionVo> selectTeamPlayerOptions(Long teamId, Long divisionId);

    List<BasketV4GrantablePlayerOptionVo> selectGrantablePlayerOptions(Long divisionId);
}
