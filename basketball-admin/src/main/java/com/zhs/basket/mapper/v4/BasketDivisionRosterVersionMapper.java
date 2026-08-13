package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionRosterVersionMapper {
    BasketDivisionRosterVersion selectById(@Param("id") Long id);

    BasketDivisionRosterVersion selectCurrentLockedByDivisionId(@Param("divisionId") Long divisionId);

    List<BasketDivisionRosterVersion> selectByDivisionId(@Param("divisionId") Long divisionId);

    int selectMaxVersionByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketDivisionRosterVersion entity);

    int updateById(BasketDivisionRosterVersion entity);

    int markSuperseded(@Param("id") Long id);

    int clearActiveUniqueKey(@Param("id") Long id);
}
