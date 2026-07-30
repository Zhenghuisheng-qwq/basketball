package com.zhs.basket.service;

import com.zhs.basket.vo.ScheduleRuleDetailVo;
import com.zhs.basket.vo.ScheduleRuleListVo;

import java.util.List;

public interface IBasketScheduleRuleService {
    List<ScheduleRuleListVo> listEnabled();
    ScheduleRuleDetailVo getDetail(Long ruleId);
}
