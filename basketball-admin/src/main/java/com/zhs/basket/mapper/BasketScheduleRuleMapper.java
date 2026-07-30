package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketScheduleRule;
import com.zhs.basket.vo.ScheduleRuleDetailVo;
import com.zhs.basket.vo.ScheduleRuleListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketScheduleRuleMapper {
    List<ScheduleRuleListVo> selectEnabledList();
    BasketScheduleRule selectById(@Param("id") Long id);
    ScheduleRuleDetailVo selectDetailById(@Param("id") Long id);
    BasketScheduleRule selectByCodeAndVersion(@Param("ruleCode") String ruleCode, @Param("ruleVersion") String ruleVersion);
}
