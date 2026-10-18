package com.zhs.dao.v4;

import com.zhs.model.v4.BasketDivision;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * CNPA 赛事域 V4 Dao：BasketDivision
 */
@Mapper
@Repository
public interface BasketDivisionDao extends BaseMapper<BasketDivision> {

    @Select("""
            select count(1) from basket_division_roster_version
            where division_id = #{divisionId} and is_delete = 0 and is_current = 1 and status = 'LOCKED'
            lock in share mode
            """)
    int countCurrentLockedRoster(@Param("divisionId") Long divisionId);
}
