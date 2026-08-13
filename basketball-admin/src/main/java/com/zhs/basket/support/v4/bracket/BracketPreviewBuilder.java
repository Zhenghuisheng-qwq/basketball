package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.vo.v4.BracketPreviewVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BracketPreviewBuilder {

    private BracketPreviewBuilder() {
    }

    public static BracketPreviewVo build(BasketDivisionStage stage,
                                         BracketSlotPlan plan,
                                         Map<Long, String> teamNames) {
        BracketPreviewVo vo = new BracketPreviewVo();
        EliminationCapacity capacity = plan.getCapacity();
        vo.setParticipantCount(capacity.getParticipantCount());
        vo.setBracketCapacity(capacity.getCapacity());
        vo.setByeCount(capacity.getByeCount());
        vo.setFirstRoundActual(capacity.getFirstRoundActual());
        vo.setTotalActualSeries(capacity.getTotalActualSeries());
        vo.setRoundCount(capacity.getRoundCount());

        List<BracketSlot> slots = plan.getSlots();
        int roundCount = capacity.getRoundCount();
        List<BracketPreviewVo.SlotView> current = toViews(slots, teamNames);
        for (int roundNo = 1; roundNo <= roundCount; roundNo++) {
            BracketPreviewVo.Round round = new BracketPreviewVo.Round();
            round.setRoundNo(roundNo);
            round.setBoRole(EliminationBoMapping.roleOf(roundNo, roundCount).name());
            round.setBestOf(EliminationBoMapping.bestOf(stage, roundNo, roundCount));
            round.setRoundName(roundName(capacity.getCapacity() >> (roundNo - 1)));
            List<BracketPreviewVo.SlotView> next = new ArrayList<>();
            int pairs = current.size() / 2;
            for (int slot = 0; slot < pairs; slot++) {
                BracketPreviewVo.SlotView home = current.get(slot * 2);
                BracketPreviewVo.SlotView away = current.get(slot * 2 + 1);
                BracketPreviewVo.Pairing pairing = new BracketPreviewVo.Pairing();
                pairing.setSlot(slot);
                pairing.setHome(home);
                pairing.setAway(away);
                boolean byeAdvance = Boolean.TRUE.equals(home.getBye()) || Boolean.TRUE.equals(away.getBye());
                pairing.setByeAdvance(byeAdvance);
                pairing.setNextRoundLabel(roundNo == roundCount ? "CHAMPION" : roundName(capacity.getCapacity() >> roundNo));
                round.getPairings().add(pairing);
                BracketPreviewVo.SlotView winner = new BracketPreviewVo.SlotView();
                if (Boolean.TRUE.equals(home.getBye())) {
                    copyTeam(away, winner);
                } else if (Boolean.TRUE.equals(away.getBye())) {
                    copyTeam(home, winner);
                } else {
                    winner.setTeamName("Winner");
                    winner.setSourceLabel("R" + roundNo + " S" + (slot + 1));
                    winner.setBye(false);
                }
                next.add(winner);
            }
            vo.getRounds().add(round);
            current = next;
        }
        return vo;
    }

    private static List<BracketPreviewVo.SlotView> toViews(List<BracketSlot> slots, Map<Long, String> teamNames) {
        List<BracketPreviewVo.SlotView> views = new ArrayList<>();
        for (BracketSlot slot : slots) {
            BracketPreviewVo.SlotView view = new BracketPreviewVo.SlotView();
            view.setSlotNo(slot.getSlotNo());
            view.setBye(slot.isBye());
            view.setEventTeamId(slot.effectiveEventTeamId());
            view.setSeedNo(slot.getSeedNo());
            view.setRankingNo(slot.getRankingNo());
            view.setSourceType(slot.getSourceType());
            view.setSourceGroupId(slot.getSourceGroupId());
            view.setSourceGroupCode(slot.getSourceGroupCode());
            view.setSourceRank(slot.getSourceRank());
            view.setDisplayCode(slot.getDisplayCode());
            view.setDisplayName(slot.getDisplayName());
            view.setResolutionStatus(slot.getResolutionStatus());
            view.setResolvedEventTeamId(slot.getResolvedEventTeamId());
            if (slot.getDisplayName() != null) {
                view.setSourceDisplay(slot.getDisplayName());
                view.setSourceLabel(slot.getDisplayName());
            }
            if (slot.isBye()) {
                view.setTeamName("BYE");
            } else if (slot.effectiveEventTeamId() != null && teamNames != null) {
                String name = teamNames.getOrDefault(slot.effectiveEventTeamId(), "Team " + slot.effectiveEventTeamId());
                view.setResolvedEventTeamName(name);
                view.setTeamName(slot.getDisplayName() != null ? slot.getDisplayName() + " · " + name : name);
            } else if (slot.getDisplayName() != null) {
                view.setTeamName(slot.getDisplayName());
            }
            if (slot.getSourceRank() != null && slot.getDisplayName() == null) {
                view.setSourceLabel("REGULAR_STAGE_RANK_" + slot.getSourceRank());
            }
            views.add(view);
        }
        return views;
    }

    private static void copyTeam(BracketPreviewVo.SlotView from, BracketPreviewVo.SlotView to) {
        to.setEventTeamId(from.getEventTeamId());
        to.setTeamName(from.getTeamName());
        to.setSeedNo(from.getSeedNo());
        to.setRankingNo(from.getRankingNo());
        to.setBye(false);
        to.setSourceLabel(from.getSourceLabel());
        to.setSourceDisplay(from.getSourceDisplay());
        to.setDisplayName(from.getDisplayName());
        to.setDisplayCode(from.getDisplayCode());
        to.setResolutionStatus(from.getResolutionStatus());
        to.setResolvedEventTeamId(from.getResolvedEventTeamId());
        to.setResolvedEventTeamName(from.getResolvedEventTeamName());
    }

    private static String roundName(int remainingSlots) {
        if (remainingSlots <= 2) {
            return "FINAL";
        }
        if (remainingSlots == 4) {
            return "SEMIFINAL";
        }
        return "ROUND OF " + remainingSlots;
    }
}
