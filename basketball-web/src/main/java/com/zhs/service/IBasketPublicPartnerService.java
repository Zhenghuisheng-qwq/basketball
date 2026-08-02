package com.zhs.service;

import com.zhs.response.publicapi.PublicPartnerResp;
import com.zhs.response.publicapi.PublicPartnersResp;

import java.util.List;

public interface IBasketPublicPartnerService {

    List<PublicPartnerResp> listPartners();

    PublicPartnersResp pagePartners(Integer currentPage, Integer pageSize);
}
