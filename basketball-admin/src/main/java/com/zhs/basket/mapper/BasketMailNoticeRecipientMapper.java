package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMailNoticeRecipient;
import com.zhs.basket.req.MailNoticeRecipientQueryReq;
import com.zhs.basket.resp.MailNoticeRecipientResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMailNoticeRecipientMapper {
    List<BasketMailNoticeRecipient> selectSendableMembers();

    int insertBatch(@Param("list") List<BasketMailNoticeRecipient> list);

    List<BasketMailNoticeRecipient> selectPending(@Param("noticeId") Long noticeId);

    int markResult(@Param("id") Long id, @Param("sendStatus") int sendStatus, @Param("failReason") String failReason);

    List<MailNoticeRecipientResp> selectList(MailNoticeRecipientQueryReq query);
}
