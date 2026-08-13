package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.query.v4.BasketEventPlayerQuery;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketEventPlayerMapper {
    BasketEventPlayer selectById(@Param("id") Long id);

    List<BasketEventPlayer> selectList(BasketEventPlayer query);

    List<BasketEventPlayerListVo> selectVoList(BasketEventPlayerQuery query);

    BasketEventPlayer selectByDivisionSourcePlayer(@Param("divisionId") Long divisionId, @Param("sourcePlayerId") Long sourcePlayerId);

    BasketEventPlayer selectByDivisionRosterSourcePlayer(@Param("divisionId") Long divisionId,
                                                         @Param("rosterVersionId") Long rosterVersionId,
                                                         @Param("sourcePlayerId") Long sourcePlayerId);

    List<BasketEventPlayer> selectByEventTeamId(@Param("eventTeamId") Long eventTeamId);

    int insert(BasketEventPlayer entity);

    int batchInsert(@Param("list") List<BasketEventPlayer> list);

    int updateById(BasketEventPlayer entity);

    int softDelete(@Param("id") Long id);
}
