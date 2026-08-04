package com.zhs.service.v4;

import com.zhs.request.v4.V4PersonalFormationBindTeamReq;
import com.zhs.response.v4.V4PersonalFormationTaskResp;

import java.util.List;

public interface IBasketV4PersonalFormationService {
    List<V4PersonalFormationTaskResp> listFormationTasks();

    V4PersonalFormationTaskResp getFormationDetail(Long drawTeamId);

    void bindLongTermTeam(V4PersonalFormationBindTeamReq req);

    Long confirmFormation(Long drawTeamId);
}
