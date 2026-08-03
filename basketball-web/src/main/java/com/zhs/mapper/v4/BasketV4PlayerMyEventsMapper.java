package com.zhs.mapper.v4;

import com.zhs.response.v4.V4MyEventResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PlayerMyEventsMapper {
    List<V4MyEventResp> selectMyEvents(@Param("memberId") Long memberId,
                                        @Param("playerId") Long playerId);
}
