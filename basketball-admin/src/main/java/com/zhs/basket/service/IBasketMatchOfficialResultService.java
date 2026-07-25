package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchOfficialResult;

public interface IBasketMatchOfficialResultService {
    BasketMatchOfficialResult getCurrent(Long matchId);

    BasketMatchOfficialResult publishFromMatch(BasketMatch match, Long auditorId);

    void supersedeCurrent(Long matchId);
}
