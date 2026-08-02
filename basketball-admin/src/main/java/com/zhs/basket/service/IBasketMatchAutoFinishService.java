package com.zhs.basket.service;

/**
 * 比赛计划时间已过后，自动将已发布且已编排的比赛流转为已结束。
 */
public interface IBasketMatchAutoFinishService {

    /**
     * 扫描并自动结束符合条件的比赛。
     *
     * @return 成功更新的比赛数量
     */
    int autoFinishEligibleMatches();
}
