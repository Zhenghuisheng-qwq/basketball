package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentEventGameIdTest {

    @Test
    void boundAccountWinsWhenGameIdChanges() {
        BasketPlayerPlatformAccount ps = account(11L, "PS", "old-ps");
        ps.setDisplayGameId("new-ps");
        BasketPlayerPlatformAccount xbox = account(12L, "XBOX", "xbox-id");

        assertEquals("new-ps", CurrentEventGameId.resolve(11L, "PS_XBOX", List.of(xbox, ps)));
    }

    @Test
    void divisionPlatformUsesMatchingAccountWhenIdentityIsMissing() {
        BasketPlayerPlatformAccount pc = account(3L, "PC", "pc-id");
        BasketPlayerPlatformAccount ps = account(4L, "PS", "ps-id");

        assertEquals("ps-id", CurrentEventGameId.resolve(null, "PS_XBOX", List.of(pc, ps)));
        assertEquals("pc-id", CurrentEventGameId.resolve(null, "PC", List.of(ps, pc)));
    }

    @Test
    void otherPlatformDoesNotReplaceSnapshot() {
        BasketPlayerPlatformAccount xbox = account(8L, "XBOX", "xbox-id");

        assertEquals("xbox-id", CurrentEventGameId.resolve(8L, "PC", List.of(xbox)));
        assertNull(CurrentEventGameId.resolve(null, "PS", List.of(xbox)));
    }

    private static BasketPlayerPlatformAccount account(Long id, String platform, String gameId) {
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setId(id);
        account.setPlatform(platform);
        account.setDisplayGameId(gameId);
        account.setAccountStatus(PlatformAccountStatusEnum.ACTIVE.getCode());
        return account;
    }
}
