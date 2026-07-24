package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentRule;
import com.zhs.basket.mapper.BasketTournamentRuleMapper;
import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.service.IBasketTournamentRuleService;
import com.zhs.basket.vo.TournamentRuleVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketTournamentRuleServiceImpl implements IBasketTournamentRuleService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentRuleMapper ruleMapper;

    @Override
    public List<TournamentRuleVo> selectBySeasonId(Long seasonId) {
        return ruleMapper.selectBySeasonId(seasonId).stream().map(r -> {
            TournamentRuleVo vo = new TournamentRuleVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long seasonId, TournamentRuleCreateReq req) {
        BasketTournamentRule rule = new BasketTournamentRule();
        BeanUtils.copyBeanProp(rule, req);
        rule.setSeasonId(seasonId);
        rule.setIsDelete(NOT_DELETED);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long ruleId, TournamentRuleUpdateReq req) {
        BasketTournamentRule rule = requireRule(ruleId);
        BeanUtils.copyBeanProp(rule, req);
        if (ruleMapper.updateById(rule) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ruleId) {
        if (ruleMapper.softDelete(ruleId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private BasketTournamentRule requireRule(Long id) {
        BasketTournamentRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }
        return rule;
    }
}
