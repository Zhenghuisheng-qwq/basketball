package com.zhs.dao;


import com.zhs.model.BasketMatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;


/**
 * 比赛主表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMatchDao extends BaseMapper<BasketMatch> {

    BasketMatch selectByIdForUpdate(@Param("matchId") Long matchId);

    int updateDataStatusIfAllowed(@Param("matchId") Long matchId,
                                  @Param("newStatus") Integer newStatus,
                                  @Param("notDeleted") Integer notDeleted);
}
