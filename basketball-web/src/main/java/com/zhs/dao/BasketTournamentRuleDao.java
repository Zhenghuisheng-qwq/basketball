package com.zhs.dao;


import com.zhs.model.BasketTournamentRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事规则表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTournamentRuleDao extends BaseMapper<BasketTournamentRule> {

}
