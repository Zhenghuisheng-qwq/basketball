package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.matchdata.PlayerMatchDataResubmitReq;
import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDataStatusResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmissionDetailResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmitResultResp;
import com.zhs.response.matchdata.PlayerMatchTaskResp;

public interface IMPlayerMatchDataService {

    IPage<PlayerMatchTaskResp> pageMyMatches(Long seasonId, Long stageId, Integer dataStatus,
                                             Integer matchStatus, Integer pageNum, Integer pageSize);

    PlayerMatchDataSubmissionDetailResp getSubmitDetail(Long matchId);

    PlayerMatchDataSubmitResultResp submit(Long matchId, PlayerMatchDataSubmitReq req);

    PlayerMatchDataStatusResp getStatus(Long matchId);

    PlayerMatchDataSubmissionDetailResp getResubmitDetail(Long matchId);

    PlayerMatchDataSubmitResultResp resubmit(Long matchId, PlayerMatchDataResubmitReq req);
}
