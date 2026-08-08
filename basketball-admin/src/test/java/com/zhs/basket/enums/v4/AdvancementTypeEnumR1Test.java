package com.zhs.basket.enums.v4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdvancementTypeEnumR1Test {

    @Test
    void winnerAdvance_fromCode() {
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode("WINNER_ADVANCE");
        assertEquals("WINNER_ADVANCE", adv.getCode());
        assertEquals("胜者晋级", adv.getDescription());
    }

    @Test
    void seriesWinner_stillReadableForLegacy() {
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode("SERIES_WINNER");
        assertNotNull(adv);
        assertEquals("SERIES_WINNER", adv.getCode());
    }
}
