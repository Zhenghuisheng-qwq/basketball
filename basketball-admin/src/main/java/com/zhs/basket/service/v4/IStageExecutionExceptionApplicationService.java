package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketManualAdvanceReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketPairingAdjustReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesRevokeResolutionReq;

public interface IStageExecutionExceptionApplicationService {

    void forfeitMatch(BasketMatchForfeitReq req);

    void forfeitSeries(BasketSeriesForfeitReq req);

    void manualAdvance(BasketManualAdvanceReq req);

    void adjustPairing(BasketPairingAdjustReq req);

    void revokeSeriesResolution(BasketSeriesRevokeResolutionReq req);
}
