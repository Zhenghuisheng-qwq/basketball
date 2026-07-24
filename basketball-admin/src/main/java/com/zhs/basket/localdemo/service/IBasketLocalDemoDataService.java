package com.zhs.basket.localdemo.service;

import com.zhs.basket.localdemo.resp.BasketLocalDemoResetResp;
import com.zhs.basket.localdemo.resp.BasketLocalDemoSeedResp;
import com.zhs.basket.localdemo.resp.BasketLocalDemoStatusResp;

public interface IBasketLocalDemoDataService {

    BasketLocalDemoStatusResp getStatus();

    BasketLocalDemoSeedResp seedLocalDemoData();

    BasketLocalDemoResetResp resetLocalDemoData(String confirmMarker);
}
