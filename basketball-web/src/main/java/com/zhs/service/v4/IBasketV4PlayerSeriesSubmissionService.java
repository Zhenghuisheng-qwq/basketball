package com.zhs.service.v4;

import com.zhs.request.v4.V4SeriesSubmissionDeclareReq;
import com.zhs.request.v4.V4SeriesSubmissionIdReq;
import com.zhs.request.v4.V4SeriesSubmissionResubmitReq;
import com.zhs.request.v4.V4SeriesSubmissionSaveGameReq;
import com.zhs.response.v4.V4SeriesSubmissionWorkspaceResp;

public interface IBasketV4PlayerSeriesSubmissionService {

    V4SeriesSubmissionWorkspaceResp getWorkspace(Long matchSeriesId);

    Long declareScore(V4SeriesSubmissionDeclareReq req);

    Long saveGame(V4SeriesSubmissionSaveGameReq req);

    void submitSeries(V4SeriesSubmissionIdReq req);

    Long resubmitSeries(V4SeriesSubmissionResubmitReq req);
}
