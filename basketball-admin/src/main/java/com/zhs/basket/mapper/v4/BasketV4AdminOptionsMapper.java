package com.zhs.basket.mapper.v4;

import com.zhs.basket.vo.v4.BasketV4AdminOptionVo;
import com.zhs.basket.vo.v4.BasketV4GrantablePlayerOptionVo;
import com.zhs.basket.vo.v4.BasketV4TeamPlayerOptionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketV4AdminOptionsMapper {

    List<BasketV4AdminOptionVo> selectSeasonOptions();

    List<BasketV4AdminOptionVo> selectDivisionOptions(@Param("seasonId") Long seasonId);

    List<BasketV4AdminOptionVo> selectGrantableTeamOptions(@Param("divisionId") Long divisionId);

    List<BasketV4TeamPlayerOptionVo> selectTeamPlayerOptions(@Param("teamId") Long teamId,
                                                             @Param("divisionId") Long divisionId,
                                                             @Param("platformCode") String platformCode);

    List<BasketV4GrantablePlayerOptionVo> selectGrantablePlayerOptions(@Param("divisionId") Long divisionId,
                                                                       @Param("platformCode") String platformCode);
}
