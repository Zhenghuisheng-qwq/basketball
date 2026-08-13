package com.zhs.basket.support.v4.formation;

import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.common.exception.ServiceException;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TemporaryCaptainResolver {

    private TemporaryCaptainResolver() {
    }

    public static FormationPlayer resolve(List<FormationPlayer> members, Map<Long, BasketMemberAccount> accountsById) {
        if (members == null || members.isEmpty()) {
            throw new ServiceException("\u4e34\u65f6\u961f\u4f0d\u6ca1\u6709\u6210\u5458\uff0c\u65e0\u6cd5\u786e\u5b9a\u4e34\u65f6\u961f\u957f");
        }
        return members.stream()
                .min(Comparator
                        .comparing((FormationPlayer p) -> accountCreateTime(p, accountsById), Comparator.nullsLast(Date::compareTo))
                        .thenComparing(p -> accountId(p, accountsById), Comparator.nullsLast(Long::compareTo))
                        .thenComparing(FormationPlayer::getUserId, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow(() -> new ServiceException("\u65e0\u6cd5\u786e\u5b9a\u4e34\u65f6\u961f\u957f"));
    }

    private static Date accountCreateTime(FormationPlayer player, Map<Long, BasketMemberAccount> accountsById) {
        BasketMemberAccount account = accountOf(player, accountsById);
        if (account == null || account.getCreateTime() == null) {
            throw new ServiceException("\u627e\u4e0d\u5230\u7528\u6237\u521b\u5efa\u65f6\u95f4\uff0c\u65e0\u6cd5\u6309\u89c4\u5219\u786e\u5b9a\u4e34\u65f6\u961f\u957f");
        }
        return account.getCreateTime();
    }

    private static Long accountId(FormationPlayer player, Map<Long, BasketMemberAccount> accountsById) {
        BasketMemberAccount account = accountOf(player, accountsById);
        return account == null ? player.getUserId() : account.getId();
    }

    private static BasketMemberAccount accountOf(FormationPlayer player, Map<Long, BasketMemberAccount> accountsById) {
        if (player == null || player.getUserId() == null || accountsById == null) {
            return null;
        }
        return accountsById.get(player.getUserId());
    }
}
