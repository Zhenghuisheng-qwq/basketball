package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class HonorGenerationPreviewResp {
    private Long seasonId;
    private String seasonName;
    private Long tournamentId;
    private String tournamentName;
    private HonorGenerationBatchResp currentBatch;
    private List<HonorGenerationPreviewItemResp> items;
}
