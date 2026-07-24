package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentRound;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.TournamentRoundStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketTournamentRoundMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.req.RoundCreateReq;
import com.zhs.basket.req.RoundUpdateReq;
import com.zhs.basket.service.IBasketTournamentRoundService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.RoundDetailVo;
import com.zhs.basket.vo.RoundListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketTournamentRoundServiceImpl implements IBasketTournamentRoundService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentRoundMapper roundMapper;
    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<RoundListVo> selectByStageId(Long stageId) {
        requireStage(stageId);
        return roundMapper.selectByStageId(stageId).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public RoundDetailVo selectById(Long roundId) {
        BasketTournamentRound round = requireRound(roundId);
        return toDetailVo(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long stageId, RoundCreateReq req) {
        BasketTournamentStage stage = requireStage(stageId);
        validateRoundTimes(req.getStartTime(), req.getEndTime(), stage);
        validateRoundNameUnique(stageId, req.getRoundName(), null);
        BasketTournamentRound round = new BasketTournamentRound();
        BeanUtils.copyBeanProp(round, req);
        round.setSeasonId(stage.getSeasonId());
        round.setStageId(stageId);
        round.setRoundNo(playerNoGenerator.generateBizNo("RD"));
        round.setRoundStatus(TournamentRoundStatusEnum.DRAFT.getCode());
        round.setIsDelete(NOT_DELETED);
        if (round.getSequenceNo() == null) {
            round.setSequenceNo(0);
        }
        if (round.getBestOf() == null) {
            round.setBestOf(1);
        }
        roundMapper.insert(round);
        return round.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long roundId, RoundUpdateReq req) {
        BasketTournamentRound round = requireRound(roundId);
        if (round.getRoundStatus() == null || round.getRoundStatus() != TournamentRoundStatusEnum.DRAFT.getCode()) {
            throw new ServiceException("仅草稿轮次可编辑");
        }
        BasketTournamentStage stage = requireStage(round.getStageId());
        validateRoundTimes(req.getStartTime(), req.getEndTime(), stage);
        validateRoundNameUnique(round.getStageId(), req.getRoundName(), roundId);
        BeanUtils.copyBeanProp(round, req);
        if (roundMapper.updateById(round) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long roundId) {
        BasketTournamentRound round = requireRound(roundId);
        BasketTournamentStage stage = requireStage(round.getStageId());
        validateRoundTimes(round.getStartTime(), round.getEndTime(), stage);
        changeStatus(roundId, Collections.singletonList(TournamentRoundStatusEnum.DRAFT.getCode()),
                TournamentRoundStatusEnum.ACTIVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long roundId) {
        changeStatus(roundId, Collections.singletonList(TournamentRoundStatusEnum.ACTIVE.getCode()),
                TournamentRoundStatusEnum.FINISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long roundId) {
        changeStatus(roundId, Collections.singletonList(TournamentRoundStatusEnum.ACTIVE.getCode()),
                TournamentRoundStatusEnum.CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long roundId) {
        BasketTournamentRound round = requireRound(roundId);
        if (round.getRoundStatus() == null || round.getRoundStatus() != TournamentRoundStatusEnum.DRAFT.getCode()) {
            throw new ServiceException("仅草稿轮次可删除");
        }
        if (matchMapper.countByRoundId(roundId) > 0) {
            throw new ServiceException("轮次下存在比赛，无法删除");
        }
        if (roundMapper.softDelete(roundId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private void changeStatus(Long roundId, List<Integer> from, TournamentRoundStatusEnum to) {
        if (roundMapper.updateStatusIfMatch(roundId, from, to.getCode()) != 1) {
            throw new ServiceException("状态变更失败");
        }
    }

    private void validateRoundTimes(Date startTime, Date endTime, BasketTournamentStage stage) {
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new ServiceException("开始时间不能晚于结束时间");
        }
        if (stage != null && startTime != null && endTime != null
                && stage.getStartTime() != null && stage.getEndTime() != null) {
            if (startTime.before(stage.getStartTime()) || endTime.after(stage.getEndTime())) {
                throw new ServiceException("轮次时间须在阶段时间范围内");
            }
        }
    }

    private void validateRoundNameUnique(Long stageId, String roundName, Long excludeId) {
        if (roundName != null && roundMapper.countByStageIdAndRoundName(stageId, roundName, excludeId) > 0) {
            throw new ServiceException("同阶段下轮次名称已存在");
        }
    }

    private BasketTournamentStage requireStage(Long id) {
        BasketTournamentStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private BasketTournamentRound requireRound(Long id) {
        BasketTournamentRound round = roundMapper.selectById(id);
        if (round == null) {
            throw new ServiceException("轮次不存在");
        }
        return round;
    }

    private RoundListVo toListVo(BasketTournamentRound round) {
        RoundListVo vo = new RoundListVo();
        BeanUtils.copyBeanProp(vo, round);
        vo.setRoundStatusName(TournamentRoundStatusEnum.fromCode(round.getRoundStatus()).getDescription());
        return vo;
    }

    private RoundDetailVo toDetailVo(BasketTournamentRound round) {
        RoundDetailVo vo = new RoundDetailVo();
        BeanUtils.copyBeanProp(vo, round);
        vo.setRoundStatusName(TournamentRoundStatusEnum.fromCode(round.getRoundStatus()).getDescription());
        BasketTournamentStage stage = stageMapper.selectById(round.getStageId());
        if (stage != null) {
            vo.setStageName(stage.getStageName());
        }
        int matchCount = matchMapper.countByRoundId(round.getId());
        boolean draft = round.getRoundStatus() != null && round.getRoundStatus() == TournamentRoundStatusEnum.DRAFT.getCode();
        boolean active = round.getRoundStatus() != null && round.getRoundStatus() == TournamentRoundStatusEnum.ACTIVE.getCode();
        vo.setCanEdit(draft);
        vo.setCanActivate(draft);
        vo.setCanFinish(active);
        vo.setCanCancel(active);
        vo.setCanDelete(draft && matchCount == 0);
        return vo;
    }
}
