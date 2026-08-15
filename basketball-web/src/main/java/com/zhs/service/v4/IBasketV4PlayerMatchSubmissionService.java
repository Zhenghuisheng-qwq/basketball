package com.zhs.service.v4;

import com.zhs.request.v4.V4MatchSubmissionIdReq;
import com.zhs.request.v4.V4MatchSubmissionResubmitReq;
import com.zhs.request.v4.V4MatchSubmissionSaveReq;
import com.zhs.response.v4.V4MatchSubmissionDetailResp;
import com.zhs.response.v4.V4MatchSubmissionEligiblePlayerResp;
import com.zhs.response.v4.V4MyMatchResp;
import com.zhs.response.v4.V4MySubmissionListResp;

import java.util.List;

public interface IBasketV4PlayerMatchSubmissionService {
    List<V4MyMatchResp> listMyMatches(Long seasonId, Long divisionId, String matchStatus,
                                      String publishStatus, String submissionStatus, Boolean upcomingOnly);

    V4MatchSubmissionDetailResp getDetail(Long matchId, Long submitEventTeamId);

    List<V4MatchSubmissionEligiblePlayerResp> listEligiblePlayers(Long matchId, Long submitEventTeamId);

    Long saveDraft(V4MatchSubmissionSaveReq req);

    void submit(V4MatchSubmissionIdReq req);

    Long resubmit(V4MatchSubmissionResubmitReq req);

    List<V4MySubmissionListResp> listMySubmissions();
}
