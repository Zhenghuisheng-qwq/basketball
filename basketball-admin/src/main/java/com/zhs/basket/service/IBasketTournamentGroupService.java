package com.zhs.basket.service;

import com.zhs.basket.req.GroupCreateReq;
import com.zhs.basket.req.GroupTeamAssignReq;
import com.zhs.basket.req.GroupUpdateReq;
import com.zhs.basket.vo.AvailableTeamVo;
import com.zhs.basket.vo.GroupDetailVo;
import com.zhs.basket.vo.GroupListVo;
import com.zhs.basket.vo.GroupTeamVo;
import java.util.List;

public interface IBasketTournamentGroupService {
    List<GroupListVo> selectByStageId(Long stageId);
    GroupDetailVo selectById(Long groupId);
    Long create(Long stageId, GroupCreateReq req);
    void update(Long groupId, GroupUpdateReq req);
    void delete(Long groupId);
    List<GroupTeamVo> selectTeamsByGroupId(Long groupId);
    List<AvailableTeamVo> selectAvailableTeams(Long groupId);
    void assignTeams(Long groupId, GroupTeamAssignReq req);
}
