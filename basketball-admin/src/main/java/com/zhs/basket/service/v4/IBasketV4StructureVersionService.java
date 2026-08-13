package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.support.v4.version.StructureVersionContext;

/**
 * Structure Version 绑定与失效。Header 仍是 ScheduleBatch。
 */
public interface IBasketV4StructureVersionService {
    StructureVersionContext resolveForGenerate(Long divisionId);

    void bindOnCreate(BasketScheduleBatch batch, StructureVersionContext context);

    void markStale(Long batchId);

    int invalidateByRuleVersion(Long ruleVersionId);

    int invalidateByRosterVersion(Long rosterVersionId);

    boolean isStructureUsable(BasketScheduleBatch batch);

    boolean isStructureUsable(Long batchId);

    void assertBoundRuleVersion(BasketScheduleBatch batch, Long expectedRuleVersionId);
}
