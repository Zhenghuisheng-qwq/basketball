package com.zhs.mapper.v4;

import com.zhs.response.v4.V4MyMatchResp;
import com.zhs.response.v4.V4MySubmissionListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PlayerMatchSubmissionMapper {
    List<V4MyMatchResp> selectMyMatches(@Param("memberId") Long memberId,
                                         @Param("playerId") Long playerId,
                                         @Param("seasonId") Long seasonId,
                                         @Param("divisionId") Long divisionId,
                                         @Param("matchStatus") String matchStatus,
                                         @Param("publishStatus") String publishStatus,
                                         @Param("submissionStatus") String submissionStatus,
                                         @Param("upcomingOnly") Boolean upcomingOnly);

    List<V4MySubmissionListResp> selectMySubmissions(@Param("memberId") Long memberId);
}
