package com.zhs.dao;

import com.zhs.dto.PlayerMyScheduleEventOptionRow;
import com.zhs.dto.PlayerMySchedulePlatformOptionRow;
import com.zhs.dto.PlayerMyScheduleRow;
import com.zhs.request.matchdata.PlayerMyScheduleQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface PlayerMyScheduleDao {

    List<PlayerMyScheduleRow> selectMySchedulePage(@Param("memberId") Long memberId,
                                                     @Param("playerId") Long playerId,
                                                     @Param("query") PlayerMyScheduleQuery query,
                                                     @Param("startTime") Date startTime,
                                                     @Param("endTimeExclusive") Date endTimeExclusive,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);

    long countMySchedule(@Param("memberId") Long memberId,
                         @Param("playerId") Long playerId,
                         @Param("query") PlayerMyScheduleQuery query,
                         @Param("startTime") Date startTime,
                         @Param("endTimeExclusive") Date endTimeExclusive);

    List<PlayerMyScheduleEventOptionRow> selectEventOptions(@Param("memberId") Long memberId,
                                                            @Param("playerId") Long playerId);

    List<PlayerMySchedulePlatformOptionRow> selectPlatformOptions(@Param("memberId") Long memberId,
                                                                  @Param("playerId") Long playerId);
}
