package com.zhs.basket.service.v4;

/**
 * Division 业务结束与球队生命周期收尾。
 */
public interface IBasketV4DivisionClosingService {

    /**
     * 结束 Division：FINISHED + Grant/Team/Member/Invitation/Application 收尾。
     * 幂等：已 FINISHED / ARCHIVED 直接成功。
     */
    void finishDivision(Long divisionId, String reason);
}
