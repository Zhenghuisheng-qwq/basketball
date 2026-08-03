package com.zhs.service.v4;

import com.zhs.request.v4.V4RegistrationIdReq;
import com.zhs.request.v4.V4RegistrationSaveReq;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyRegistrationResp;

import java.util.List;

public interface IBasketV4PlayerRegistrationService {
    List<V4AvailableDivisionResp> listAvailableDivisions();

    Long saveRegistration(V4RegistrationSaveReq req);

    void submitRegistration(V4RegistrationIdReq req);

    void withdrawRegistration(V4RegistrationIdReq req);

    List<V4MyRegistrationResp> listMyRegistrations();
}
