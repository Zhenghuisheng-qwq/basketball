package com.zhs.service;

import com.zhs.request.matchdata.PlayerMatchExceptionSubmitReq;
import com.zhs.response.matchdata.PlayerMatchExceptionAvailableResp;
import com.zhs.response.matchdata.PlayerMatchExceptionHistoryItem;
import com.zhs.response.matchdata.PlayerMatchExceptionStatusResp;
import com.zhs.response.matchdata.PlayerMatchExceptionSubmitResultResp;

import java.util.List;

public interface IMPlayerMatchExceptionService {

    PlayerMatchExceptionAvailableResp getAvailable(Long matchId);

    PlayerMatchExceptionStatusResp getStatus(Long matchId);

    List<PlayerMatchExceptionHistoryItem> getHistory(Long matchId);

    PlayerMatchExceptionSubmitResultResp submit(Long matchId, PlayerMatchExceptionSubmitReq req);
}
