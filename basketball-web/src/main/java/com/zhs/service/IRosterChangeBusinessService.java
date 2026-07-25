package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.participation.RosterChangeCaptainTransferReq;
import com.zhs.request.participation.RosterChangeFreeAgentAddReq;
import com.zhs.request.participation.RosterChangeTransferReq;
import com.zhs.response.participation.RosterChangeRequestItemResp;
import com.zhs.response.participation.RosterChangeSubmitResp;

public interface IRosterChangeBusinessService {

    RosterChangeSubmitResp submitTransfer(RosterChangeTransferReq req);

    RosterChangeSubmitResp submitFreeAgentAdd(RosterChangeFreeAgentAddReq req);

    RosterChangeSubmitResp submitCaptainTransfer(RosterChangeCaptainTransferReq req);

    void withdrawRequest(String requestId);

    IPage<RosterChangeRequestItemResp> pageMyRequests(Integer currentPage, Integer pageSize, Integer status);
}
