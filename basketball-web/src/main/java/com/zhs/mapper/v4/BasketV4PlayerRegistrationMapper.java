package com.zhs.mapper.v4;

import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyRegistrationResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PlayerRegistrationMapper {
    List<V4AvailableDivisionResp> selectAvailableDivisions(@Param("memberId") Long memberId, @Param("playerId") Long playerId);

    List<V4MyRegistrationResp> selectMyRegistrations(@Param("memberId") Long memberId);

    int countApprovedByDivisionId(@Param("divisionId") Long divisionId);
}
