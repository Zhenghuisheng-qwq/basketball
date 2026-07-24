package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineRecord;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketDisciplineRecordMapper {
    List<BasketDisciplineRecord> selectActiveBySeasonAndMembers(@Param("seasonId") Long seasonId,
            @Param("memberIds") List<Long> memberIds, @Param("now") Date now);
}
