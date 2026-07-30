package com.zhs.service;

import com.zhs.request.matchdata.PlayerMatchDisputeSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDisputeAvailableResp;
import com.zhs.response.matchdata.PlayerMatchDisputeDetailResp;
import com.zhs.response.matchdata.PlayerMatchDisputeHistoryItem;
import com.zhs.response.matchdata.PlayerMatchDisputeStatusResp;
import com.zhs.response.matchdata.PlayerMatchDisputeSubmitResultResp;

import java.util.List;

public interface IMPlayerMatchDisputeService {

    PlayerMatchDisputeAvailableResp getAvailable(Long matchId);

    PlayerMatchDisputeDetailResp getDetail(Long matchId);

    PlayerMatchDisputeStatusResp getStatus(Long matchId);

    List<PlayerMatchDisputeHistoryItem> getHistory(Long matchId);

    PlayerMatchDisputeSubmitResultResp submit(Long matchId, PlayerMatchDisputeSubmitReq req);
}
