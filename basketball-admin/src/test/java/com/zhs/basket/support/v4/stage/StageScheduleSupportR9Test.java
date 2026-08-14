package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StageScheduleSupportR9Test {

    @Test
    void childMatchInheritsPublishedBatchVisibility() {
        BasketScheduleBatch published = new BasketScheduleBatch();
        published.setPublishStatus("PUBLISHED");
        assertEquals("PUBLISHED", StageScheduleSupport.matchPublishStatusForBatch(published));
    }

    @Test
    void draftBatchKeepsDraftMatches() {
        BasketScheduleBatch draft = new BasketScheduleBatch();
        draft.setPublishStatus("DRAFT");
        assertEquals("DRAFT", StageScheduleSupport.matchPublishStatusForBatch(draft));
        assertEquals("DRAFT", StageScheduleSupport.matchPublishStatusForBatch(null));
    }
}
