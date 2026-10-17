package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.support.v4.version.RuleVersionSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasketV4RuleVersionServiceImpl implements IBasketV4RuleVersionService {

    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;

    @Override
    public RuleVersionView getCurrent(Long divisionId) {
        if (divisionId == null) {
            return null;
        }
        return fromSnapshot(snapshotMapper.selectActiveByDivisionId(divisionId));
    }

    @Override
    public RuleVersionView getById(Long ruleVersionId) {
        if (ruleVersionId == null) {
            return null;
        }
        return fromSnapshot(snapshotMapper.selectById(ruleVersionId));
    }

    @Override
    public RuleVersionView fromSnapshot(BasketDivisionFormatSnapshot snapshot) {
        RuleVersionView view = RuleVersionSupport.fromSnapshot(snapshot);
        if (view != null && snapshot != null && view.isLocked()) {
            view.setRuleVersionNo(snapshotMapper.countLockedThroughId(snapshot.getDivisionId(), snapshot.getId()));
        }
        return view;
    }
}
