package com.zhs.basket.job;

import com.zhs.basket.service.IBasketScheduleGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时任务入口：扫描报名已截止的赛事并自动生成草稿赛程（不自动发布）。
 * <p>
 * 扫描条件：
 * 1. 当前时间超过报名结束时间
 * 2. 无待审核报名（SUBMITTED / REVIEWING）
 * 3. 正式 EventTeam 数量 &gt;= 2
 * 4. 当前赛事没有已发布赛程
 * 5. 当前赛事没有草稿赛程/批次
 * 6. 使用 REGULAR_ROUND_ROBIN V2 规则
 * <p>
 * 可在 Quartz 中配置 invokeTarget = scheduleAutoGenerateJob.runAutoGenerate()。
 */
@Component("scheduleAutoGenerateJob")
public class ScheduleAutoGenerateJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleAutoGenerateJob.class);

    @Autowired
    private IBasketScheduleGenerateService scheduleGenerateService;

    public int runAutoGenerate() {
        int count = scheduleGenerateService.autoGenerateEligibleSchedules();
        log.info("自动草稿赛程生成完成，成功 {} 个赛季", count);
        return count;
    }
}
