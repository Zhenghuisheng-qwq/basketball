package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfirmedSlotStrategy implements BracketSlotAllocationStrategy {

    @Override
    public BracketSlotPlan allocate(List<BracketParticipant> participants, SlotAllocationRequest request) {
        if (request == null || request.getConfirmedSlots() == null || request.getConfirmedSlots().isEmpty()) {
            throw new ServiceException("当前抽签尚未确认，无法生成结构");
        }
        return SeededBracketPlacement.fromSlots(request.getConfirmedSlots(), participants.size());
    }
}
