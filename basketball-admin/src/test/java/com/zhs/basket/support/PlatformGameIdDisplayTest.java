package com.zhs.basket.support;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlatformGameIdDisplayTest {

    @Test
    void usesMatchingPlatformGameId() {
        BasketPlayerPlatformAccount ps = account("PS", "Gzy-DL");
        BasketPlayerPlatformAccount pc = account("PC", "should-hide");
        assertEquals("Gzy-DL", PlatformGameIdDisplay.format("PS_XBOX", Arrays.asList(pc, ps)));
    }

    @Test
    void labelsBothPlatformsWhenDivisionAcceptsBoth() {
        BasketPlayerPlatformAccount ps = account("PS", "Gzy-DL");
        BasketPlayerPlatformAccount xbox = account("XBOX", "XxNoCtIs");
        assertEquals("PS Gzy-DL / Xbox XxNoCtIs", PlatformGameIdDisplay.format("PS_XBOX", Arrays.asList(ps, xbox)));
    }

    @Test
    void emptyWhenNoMatchingGameId() {
        assertNull(PlatformGameIdDisplay.format("PS_XBOX", Collections.singletonList(account("PC", "pc-id"))));
        assertNull(PlatformGameIdDisplay.format("PS", Collections.emptyList()));
    }

    private static BasketPlayerPlatformAccount account(String platform, String gameId) {
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setPlatform(platform);
        account.setDisplayGameId(gameId);
        account.setAccountStatus(1);
        return account;
    }
}
