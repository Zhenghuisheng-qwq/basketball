package com.zhs.basket.service;

import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.vo.TournamentRuleVo;
import java.util.List;

public interface IBasketTournamentRuleService {
    List<TournamentRuleVo> selectBySeasonId(Long seasonId);
    Long create(Long seasonId, TournamentRuleCreateReq req);
    void update(Long ruleId, TournamentRuleUpdateReq req);
    void delete(Long ruleId);
}
