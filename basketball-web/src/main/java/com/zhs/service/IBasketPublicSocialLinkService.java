package com.zhs.service;

import com.zhs.response.publicapi.PublicSocialLinkResp;

import java.util.List;

public interface IBasketPublicSocialLinkService {
    List<PublicSocialLinkResp> listEnabled();
}
