package com.zhs.mapper.v4;

import com.zhs.response.v4.V4MyPersonalRegistrationResp;
import com.zhs.response.v4.V4PersonalAvailableDivisionResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PersonalRegistrationMapper {
    List<V4PersonalAvailableDivisionResp> selectAvailablePersonalDivisions(@Param("memberId") Long memberId,
                                                                           @Param("playerId") Long playerId);

    List<V4MyPersonalRegistrationResp> selectMyPersonalRegistrations(@Param("memberId") Long memberId);
}
