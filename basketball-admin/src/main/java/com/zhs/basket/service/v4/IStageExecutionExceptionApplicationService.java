package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketAdminOfficialResultReq;
import com.zhs.basket.dto.v4.BasketAdminResultCorrectionReq;
import com.zhs.basket.dto.v4.BasketManualAdvanceReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitRevokeReq;
import com.zhs.basket.dto.v4.BasketMatchReopenSubmissionReq;
import com.zhs.basket.dto.v4.BasketPairingAdjustReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesManualResolutionReq;
import com.zhs.basket.dto.v4.BasketSeriesRevokeResolutionReq;

public interface IStageExecutionExceptionApplicationService {

    void forfeitMatch(BasketMatchForfeitReq req);

    void overrideMatchOfficialResultForDisqualification(BasketMatchForfeitReq req);

    void revokeMatchForfeit(BasketMatchForfeitRevokeReq req);

    void forfeitSeries(BasketSeriesForfeitReq req);

    void applySeriesOfficialSweepForDisqualification(BasketSeriesForfeitReq req);

    void manualAdvance(BasketManualAdvanceReq req);

    void adjustPairing(BasketPairingAdjustReq req);

    void revokeSeriesResolution(BasketSeriesRevokeResolutionReq req);

    void enterOfficialResult(BasketAdminOfficialResultReq req);

    void correctOfficialResult(BasketAdminResultCorrectionReq req);

    void reopenApprovedSubmission(BasketMatchReopenSubmissionReq req);

    void resolveSeriesManually(BasketSeriesManualResolutionReq req);
}
