package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMailNotice;
import com.zhs.basket.req.MailNoticeQueryReq;
import com.zhs.basket.resp.MailNoticeResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMailNoticeMapper {
    BasketMailNotice selectById(@Param("id") Long id);

    BasketMailNotice selectByIdForUpdate(@Param("id") Long id);

    MailNoticeResp selectDetail(@Param("id") Long id);

    List<MailNoticeResp> selectList(MailNoticeQueryReq query);

    List<Long> selectSendingIds();

    int insert(BasketMailNotice notice);

    int updateDraft(BasketMailNotice notice);

    int markDeleted(@Param("id") Long id);

    int markSending(@Param("id") Long id, @Param("recipientCount") int recipientCount);

    int refreshCounts(@Param("id") Long id);

    int markCompletedIfNoPending(@Param("id") Long id);
}
