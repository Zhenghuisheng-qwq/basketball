package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.matchdata.PlayerMatchDataResubmitReq;
import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import com.zhs.request.matchdata.PlayerMatchTeamPlayerStatsResubmitReq;
import com.zhs.request.matchdata.PlayerMatchTeamPlayerStatsSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDataStatusResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmissionDetailResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmitResultResp;
import com.zhs.response.matchdata.PlayerMatchTaskResp;

public interface IMPlayerMatchDataService {

    IPage<PlayerMatchTaskResp> pageMyMatches(Long seasonId, Long stageId, Integer dataStatus,
                                             Integer matchStatus, Integer pageNum, Integer pageSize);

    IPage<PlayerMatchTaskResp> pageEventTeamMatches(String eventTeamId, Long stageId, Integer dataStatus,
                                                    Integer matchStatus, String beginTime, String endTime,
                                                    Integer pageNum, Integer pageSize);

    PlayerMatchTaskResp getEventTeamMatchDetail(String eventTeamId, Long matchId);

    PlayerMatchDataSubmissionDetailResp getSubmitDetail(Long matchId);

    PlayerMatchDataSubmitResultResp submit(Long matchId, PlayerMatchDataSubmitReq req);

    PlayerMatchDataStatusResp getStatus(Long matchId);

    PlayerMatchDataSubmissionDetailResp getResubmitDetail(Long matchId);

    PlayerMatchDataSubmitResultResp resubmit(Long matchId, PlayerMatchDataResubmitReq req);

    PlayerMatchDataSubmitResultResp submitTeamPlayerStats(Long matchId, PlayerMatchTeamPlayerStatsSubmitReq req);

    PlayerMatchDataSubmitResultResp resubmitTeamPlayerStats(Long matchId, PlayerMatchTeamPlayerStatsResubmitReq req);
}
