package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRosterOperationLog;
import com.zhs.basket.query.RosterOperationLogQuery;
import com.zhs.basket.vo.RosterOperationLogVo;
import java.util.List;

public interface BasketTournamentRosterOperationLogMapper {
    int insert(BasketTournamentRosterOperationLog log);
    List<RosterOperationLogVo> selectList(RosterOperationLogQuery query);
}
