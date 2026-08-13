package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionRosterEntry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionRosterEntryMapper {
    BasketDivisionRosterEntry selectById(@Param("id") Long id);

    List<BasketDivisionRosterEntry> selectByRosterVersionId(@Param("rosterVersionId") Long rosterVersionId);

    int insert(BasketDivisionRosterEntry entity);

    int batchInsert(@Param("list") List<BasketDivisionRosterEntry> list);
}
