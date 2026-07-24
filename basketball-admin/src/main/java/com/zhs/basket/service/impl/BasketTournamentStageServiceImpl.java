package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.TournamentStageStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.query.StageQuery;
import com.zhs.basket.req.StageCreateReq;
import com.zhs.basket.req.StageUpdateReq;
import com.zhs.basket.service.IBasketTournamentStageService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.StageDetailVo;
import com.zhs.basket.vo.StageListVo;
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
public class BasketTournamentStageServiceImpl implements IBasketTournamentStageService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<StageListVo> selectList(StageQuery query) {
        return stageMapper.selectList(query).stream().map(this::enrichListVo).collect(Collectors.toList());
    }

    @Override
    public StageDetailVo selectById(Long stageId) {
        BasketTournamentStage stage = requireStage(stageId);
        return toDetailVo(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StageCreateReq req) {
        if (req.getSeasonId() == null) {
            throw new ServiceException("赛季不能为空");
        }
        requireSeason(req.getSeasonId());
        validateStageTimes(req.getStartTime(), req.getEndTime(), null);
        validateStageNameUnique(req.getSeasonId(), req.getStageName(), null);
        BasketTournamentStage stage = new BasketTournamentStage();
        BeanUtils.copyBeanProp(stage, req);
        stage.setStageNo(playerNoGenerator.generateBizNo("ST"));
        stage.setStageStatus(TournamentStageStatusEnum.DRAFT.getCode());
        stage.setIsDelete(NOT_DELETED);
        if (stage.getSequenceNo() == null) {
            stage.setSequenceNo(0);
        }
        if (stage.getPublicStatus() == null) {
            stage.setPublicStatus(1);
        }
        stageMapper.insert(stage);
        return stage.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long stageId, StageUpdateReq req) {
        BasketTournamentStage stage = requireStage(stageId);
        if (stage.getStageStatus() == null || stage.getStageStatus() != TournamentStageStatusEnum.DRAFT.getCode()) {
            throw new ServiceException("仅草稿阶段可编辑");
        }
        validateStageTimes(req.getStartTime(), req.getEndTime(), null);
        validateStageNameUnique(stage.getSeasonId(), req.getStageName(), stageId);
        BeanUtils.copyBeanProp(stage, req);
        if (stageMapper.updateById(stage) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long stageId) {
        BasketTournamentStage stage = requireStage(stageId);
        BasketTournamentSeason season = requireSeason(stage.getSeasonId());
        validateStageTimes(stage.getStartTime(), stage.getEndTime(), season);
        changeStatus(stageId, Collections.singletonList(TournamentStageStatusEnum.DRAFT.getCode()),
                TournamentStageStatusEnum.ACTIVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long stageId) {
        changeStatus(stageId, Collections.singletonList(TournamentStageStatusEnum.ACTIVE.getCode()),
                TournamentStageStatusEnum.FINISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long stageId) {
        changeStatus(stageId, Collections.singletonList(TournamentStageStatusEnum.ACTIVE.getCode()),
                TournamentStageStatusEnum.CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long stageId) {
        BasketTournamentStage stage = requireStage(stageId);
        if (stage.getStageStatus() == null || stage.getStageStatus() != TournamentStageStatusEnum.DRAFT.getCode()) {
            throw new ServiceException("仅草稿阶段可删除");
        }
        if (matchMapper.countByStageId(stageId) > 0) {
            throw new ServiceException("阶段下存在比赛，无法删除");
        }
        if (stageMapper.softDelete(stageId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private void changeStatus(Long stageId, List<Integer> from, TournamentStageStatusEnum to) {
        if (stageMapper.updateStatusIfMatch(stageId, from, to.getCode()) != 1) {
            throw new ServiceException("状态变更失败");
        }
    }

    private void validateStageTimes(Date startTime, Date endTime, BasketTournamentSeason season) {
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new ServiceException("开始时间不能晚于结束时间");
        }
        if (season != null && startTime != null && endTime != null
                && season.getSeasonStartTime() != null && season.getSeasonEndTime() != null) {
            if (startTime.before(season.getSeasonStartTime()) || endTime.after(season.getSeasonEndTime())) {
                throw new ServiceException("阶段时间须在赛季时间范围内");
            }
        }
    }

    private void validateStageNameUnique(Long seasonId, String stageName, Long excludeId) {
        if (stageName != null && stageMapper.countBySeasonIdAndStageName(seasonId, stageName, excludeId) > 0) {
            throw new ServiceException("同赛季下阶段名称已存在");
        }
    }

    private BasketTournamentStage requireStage(Long id) {
        BasketTournamentStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private BasketTournamentSeason requireSeason(Long id) {
        BasketTournamentSeason season = seasonMapper.selectById(id);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private StageListVo enrichListVo(StageListVo vo) {
        vo.setStageStatusName(TournamentStageStatusEnum.fromCode(vo.getStageStatus()).getDescription());
        return vo;
    }

    private StageDetailVo toDetailVo(BasketTournamentStage stage) {
        StageDetailVo vo = new StageDetailVo();
        BeanUtils.copyBeanProp(vo, stage);
        vo.setStageStatusName(TournamentStageStatusEnum.fromCode(stage.getStageStatus()).getDescription());
        BasketTournamentSeason season = seasonMapper.selectById(stage.getSeasonId());
        if (season != null) {
            vo.setSeasonName(season.getSeasonName());
            vo.setTournamentId(season.getTournamentId());
            BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
            if (tournament != null) {
                vo.setTournamentName(tournament.getTournamentName());
            }
        }
        int matchCount = matchMapper.countByStageId(stage.getId());
        boolean draft = stage.getStageStatus() != null && stage.getStageStatus() == TournamentStageStatusEnum.DRAFT.getCode();
        boolean active = stage.getStageStatus() != null && stage.getStageStatus() == TournamentStageStatusEnum.ACTIVE.getCode();
        vo.setCanEdit(draft);
        vo.setCanActivate(draft);
        vo.setCanFinish(active);
        vo.setCanCancel(active);
        vo.setCanDelete(draft && matchCount == 0);
        return vo;
    }
}
