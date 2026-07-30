package com.zhs.basket.job;

import com.zhs.basket.service.IBasketScheduleGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 后续定时任务入口（V1 仅预留，不自动触发）。
 * <p>
 * 扫描条件建议：
 * 1. 当前时间超过报名结束时间
 * 2. 当前赛事没有待审核报名
 * 3. 正式 EventTeam 数量 &gt;= 2
 * 4. 当前赛事没有已发布赛程
 * 5. 当前赛事没有草稿赛程，或管理员确认重新生成
 * 6. 当前赛事状态允许生成赛程
 * <p>
 * 任务只调用 {@link IBasketScheduleGenerateService#generateDraftSchedule} 生成草稿，不自动发布。
 */
@Component
public class ScheduleAutoGenerateJob {

    @Autowired
    private IBasketScheduleGenerateService scheduleGenerateService;

    // V1: 手动生成。后续在此实现 scanEligibleEvents() + generateDraftSchedule() 调用链。
}
