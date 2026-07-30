package com.zhs.basket.service;

import com.zhs.basket.req.ScheduleGenerateReq;
import com.zhs.basket.req.SchedulePublishReq;
import com.zhs.basket.vo.ScheduleEventContextVo;
import com.zhs.basket.vo.ScheduleGeneratePreviewVo;
import com.zhs.basket.vo.ScheduleGenerateResultVo;

public interface IBasketScheduleGenerateService {
    ScheduleEventContextVo getEventContext(Long eventId);
    ScheduleGeneratePreviewVo preview(ScheduleGenerateReq req);

    /**
     * 统一赛程草稿生成入口：后台手动生成、后续定时任务、MQ 事件均调用此方法。
     */
    ScheduleGenerateResultVo generateDraftSchedule(ScheduleGenerateReq req);

    ScheduleGenerateResultVo generateDraft(ScheduleGenerateReq req);
    void publish(SchedulePublishReq req);
    void unpublish(SchedulePublishReq req);
    void deleteDraft(Long eventId, String reason);

    /**
     * 扫描报名已截止且满足条件的赛事，自动生成草稿赛程（不自动发布）。
     *
     * @return 成功生成的赛季数量
     */
    int autoGenerateEligibleSchedules();
}
