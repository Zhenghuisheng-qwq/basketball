package com.zhs.basket.job;

import com.zhs.basket.service.IBasketMatchAutoFinishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时任务入口：计划开始时间已过的已发布比赛自动流转为已结束，并初始化数据状态为未提交。
 * <p>
 * 处理范围：
 * 1. 已发布（publish_status = 1）
 * 2. 比赛状态为已编排（match_status = 1）
 * 3. scheduled_start_time &lt;= 当前时间
 * 4. 排除草稿、未发布、已取消、已延期、进行中、已结束
 * 5. 排除存在待处理/处理中异常申请的比赛
 * <p>
 * 可在 Quartz 中配置 invokeTarget = matchStatusAutoFinishJob.runAutoFinish()，
 * cron 建议：0 0/5 * * * ?（每 5 分钟）或 0 0/1 * * * ?（每 1 分钟）。
 * <p>
 * CNPA V4 第 1.5 轮：本 Job 会更新旧 basket_match。建议立即在 sys_job 停用；
 * 同名表切换/清库前必须停用，避免污染新旧结构。第 8 轮删除。
 * 详见 docs/cnpa-v4-r1_5-legacy-event-domain-retirement-plan.md
 *
 * @deprecated 旧赛事域比赛自动结束入口，勿接入 V4 业务。
 */
@Deprecated
@Component("matchStatusAutoFinishJob")
public class MatchStatusAutoFinishJob {

    private static final Logger log = LoggerFactory.getLogger(MatchStatusAutoFinishJob.class);

    @Autowired
    private IBasketMatchAutoFinishService matchAutoFinishService;

    public int runAutoFinish() {
        int count = matchAutoFinishService.autoFinishEligibleMatches();
        log.info("比赛计划时间自动结束任务完成，成功 {} 场", count);
        return count;
    }
}
