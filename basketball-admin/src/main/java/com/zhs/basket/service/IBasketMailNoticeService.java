package com.zhs.basket.service;

import com.zhs.basket.req.MailNoticeQueryReq;
import com.zhs.basket.req.MailNoticeRecipientQueryReq;
import com.zhs.basket.req.MailNoticeSaveReq;
import com.zhs.basket.resp.MailNoticeRecipientResp;
import com.zhs.basket.resp.MailNoticeResp;

import java.util.List;

public interface IBasketMailNoticeService {
    List<MailNoticeResp> selectList(MailNoticeQueryReq query);

    MailNoticeResp selectById(Long id);

    Long add(MailNoticeSaveReq req);

    void edit(Long id, MailNoticeSaveReq req);

    void remove(Long id);

    MailNoticeResp prepareSend(Long id);

    void dispatch(Long id);

    List<MailNoticeRecipientResp> selectRecipients(MailNoticeRecipientQueryReq query);
}
