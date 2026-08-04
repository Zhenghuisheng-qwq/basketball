package com.zhs.mapper.v4;

import com.zhs.response.v4.V4PersonalFormationAvailableTeamResp;
import com.zhs.response.v4.V4PersonalFormationDetailResp;
import com.zhs.response.v4.V4PersonalFormationMemberResp;
import com.zhs.response.v4.V4PersonalFormationTaskResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PersonalFormationMapper {
    List<V4PersonalFormationTaskResp> selectFormationTasksByCaptainUserId(@Param("captainUserId") Long captainUserId);

    V4PersonalFormationDetailResp selectFormationDetail(@Param("drawTeamId") Long drawTeamId);

    List<V4PersonalFormationMemberResp> selectFormationMembers(@Param("drawTeamId") Long drawTeamId);

    List<V4PersonalFormationAvailableTeamResp> selectCandidateTeamsByCaptainMemberId(@Param("memberId") Long memberId);
}
