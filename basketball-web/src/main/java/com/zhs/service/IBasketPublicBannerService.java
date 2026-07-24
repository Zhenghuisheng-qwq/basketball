package com.zhs.service;

import com.zhs.response.publicapi.PublicHomeBannerResp;

import java.util.List;

public interface IBasketPublicBannerService {

    List<PublicHomeBannerResp> listBanners(String positionCode);
}
