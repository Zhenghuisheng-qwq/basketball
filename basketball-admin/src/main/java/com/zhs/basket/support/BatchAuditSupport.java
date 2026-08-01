package com.zhs.basket.support;

import com.zhs.basket.vo.BatchAuditFailureVo;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

import java.util.List;
import java.util.function.Consumer;

public final class BatchAuditSupport {

    private BatchAuditSupport() {
    }

    public static BatchAuditResultVo run(List<Long> ids, Consumer<Long> action) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择至少一条记录");
        }
        BatchAuditResultVo result = new BatchAuditResultVo();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            try {
                action.accept(id);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                BatchAuditFailureVo failure = new BatchAuditFailureVo();
                failure.setId(id);
                failure.setMessage(StringUtils.isNotEmpty(ex.getMessage()) ? ex.getMessage() : "操作失败");
                result.getFailures().add(failure);
                result.setFailCount(result.getFailCount() + 1);
            }
        }
        if (result.getSuccessCount() == 0 && result.getFailCount() == 0) {
            throw new ServiceException("请选择至少一条有效记录");
        }
        return result;
    }
}
