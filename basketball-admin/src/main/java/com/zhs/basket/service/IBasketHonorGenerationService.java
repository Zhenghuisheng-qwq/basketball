package com.zhs.basket.service;

import com.zhs.basket.req.HonorGenerationConfirmReq;
import com.zhs.basket.resp.HonorGenerationBatchResp;
import com.zhs.basket.resp.HonorGenerationPreviewResp;

import java.util.List;

public interface IBasketHonorGenerationService {

    HonorGenerationPreviewResp preview(Long seasonId);

    HonorGenerationBatchResp confirm(HonorGenerationConfirmReq req);

    void revokeBatch(Long batchId);

    List<HonorGenerationBatchResp> listBatches(Long seasonId);
}
