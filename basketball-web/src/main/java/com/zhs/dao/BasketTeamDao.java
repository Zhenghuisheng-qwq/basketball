package com.zhs.dao;


import com.zhs.model.BasketTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;


/**
 * 球队表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamDao extends BaseMapper<BasketTeam> {

    @Select("select id from basket_team where id = #{id} and is_delete = 0 for update")
    Long lockById(@Param("id") Long id);
}
