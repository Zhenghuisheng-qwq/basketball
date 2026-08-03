package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketRegistration;

public interface IBasketV4EventSnapshotService {
    void generateFromApprovedRegistration(BasketRegistration registration);
}
