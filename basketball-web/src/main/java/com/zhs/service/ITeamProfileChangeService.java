package com.zhs.service;

import com.zhs.request.team.TeamProfileChangeCancelReq;
import com.zhs.request.team.TeamProfileChangeSubmitReq;
import com.zhs.response.team.TeamProfileChangeDetailResp;
import com.zhs.response.team.TeamProfileChangeItemResp;
import com.zhs.response.team.TeamProfileChangeSubmitResp;
import com.zhs.response.team.TeamProfileEditResp;

import java.util.List;

public interface ITeamProfileChangeService {

    TeamProfileEditResp getProfileEditData(String teamId);

    TeamProfileChangeSubmitResp submitChange(String teamId, TeamProfileChangeSubmitReq req);

    void cancelChange(String teamId, String changeId, TeamProfileChangeCancelReq req);

    List<TeamProfileChangeItemResp> pageHistory(String teamId, Integer pageNum, Integer pageSize,
                                                Integer auditStatus, String keyword);

    long countHistory(String teamId, Integer auditStatus, String keyword);

    TeamProfileChangeDetailResp getChangeDetail(String teamId, String changeId);
}
