package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketScheduleBatchGenerateReq;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.FormatTypeEnum;
import com.zhs.basket.enums.v4.GenerateStatusEnum;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketScheduleBatchQuery;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketV4ScheduleBatchService;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Pairing;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Round;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.TimeSlotAllocator;
import com.zhs.basket.vo.v4.BasketScheduleBatchDetailVo;
import com.zhs.basket.vo.v4.BasketScheduleBatchListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketV4ScheduleBatchServiceImpl implements IBasketV4ScheduleBatchService {

    private static final Integer NOT_DELETED = 0;
    private static final String GENERATE_TYPE_MANUAL = "MANUAL";
    private static final String STAGE_REGULAR = "REGULAR";

    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;

    @Override
    public List<BasketScheduleBatchListVo> selectList(BasketScheduleBatchQuery query) {
        return scheduleBatchMapper.selectVoList(query);
    }

    @Override
    public BasketScheduleBatchDetailVo selectById(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        BasketScheduleBatchQuery query = new BasketScheduleBatchQuery();
        query.setId(id);
        List<BasketScheduleBatchListVo> rows = scheduleBatchMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("赛程批次不存在");
        }
        BasketScheduleBatchDetailVo detail = new BasketScheduleBatchDetailVo();
        BeanUtils.copyProperties(rows.get(0), detail);
        detail.setBatchNo(batch.getBatchNo());
        detail.setGenerateType(batch.getGenerateType());

        BasketV4MatchQuery matchQuery = new BasketV4MatchQuery();
        matchQuery.setScheduleBatchId(id);
        detail.setMatches(matchMapper.selectVoList(matchQuery));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generate(BasketScheduleBatchGenerateReq req) {
        if (req == null || req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = requireDivision(req.getDivisionId());
        validateDivisionForGenerate(division);

        if (!FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode().equals(division.getFormatType())) {
            throw new ServiceException("仅支持 REGULAR_ROUND_ROBIN 赛制");
        }
        if (scheduleBatchMapper.selectPublishedByDivisionId(division.getId()) != null) {
            throw new ServiceException("该 Division 已有已发布赛程，请先作废或不可重复生成");
        }
        BasketScheduleBatch existingDraft = scheduleBatchMapper.selectDraftByDivisionId(division.getId());
        if (existingDraft != null) {
            throw new ServiceException("该 Division 已有草稿赛程批次，请先作废后再生成");
        }

        List<BasketEventTeam> teams = eventTeamMapper.selectActiveByDivisionId(division.getId());
        if (teams.size() < 2) {
            throw new ServiceException("该 Division 有效 EventTeam 不足 2 支，无法生成赛程");
        }

        List<Long> teamIds = teams.stream().map(BasketEventTeam::getId).collect(Collectors.toList());
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(
                BasketV4RoundRobinScheduler.sortedTeamIds(teamIds));
        validatePairings(rounds, division.getId());

        Date now = new Date();
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setSeasonId(division.getSeasonId());
        batch.setDivisionId(division.getId());
        batch.setBatchNo("V4-BATCH-" + division.getId() + "-" + System.currentTimeMillis());
        batch.setFormatType(FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode());
        batch.setRuleCode(BasketV4RoundRobinScheduler.ruleCode());
        batch.setTeamCount(teams.size());
        batch.setMatchCount(BasketV4RoundRobinScheduler.countMatches(rounds));
        batch.setGenerateType(GENERATE_TYPE_MANUAL);
        batch.setGenerateStatus(GenerateStatusEnum.SUCCESS.getCode());
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        batch.setGenerateTime(now);
        batch.setGeneratedBy(SecurityUtils.getUserId());
        batch.setRemark(req.getRemark());
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        if (scheduleBatchMapper.insert(batch) != 1) {
            throw new ServiceException("创建赛程批次失败");
        }

        TimeSlotAllocator allocator = new TimeSlotAllocator(division.getMatchStartTime() != null
                ? division.getMatchStartTime() : now);
        String platformCode = division.getPlatformCode();
        for (Round round : rounds) {
            int seq = 1;
            for (Pairing pairing : round.getPairings()) {
                BasketV4Match match = new BasketV4Match();
                match.setSeasonId(division.getSeasonId());
                match.setDivisionId(division.getId());
                match.setScheduleBatchId(batch.getId());
                match.setRoundNo(round.getRoundNo());
                match.setMatchNo(BasketV4RoundRobinScheduler.buildMatchNo(division.getId(), round.getRoundNo(), seq));
                match.setStageCode(STAGE_REGULAR);
                match.setMatchTime(allocator.allocate());
                match.setHomeEventTeamId(pairing.getHomeEventTeamId());
                match.setAwayEventTeamId(pairing.getAwayEventTeamId());
                match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
                match.setPublishStatus(MatchPublishStatusEnum.DRAFT.getCode());
                match.setPlatformCode(platformCode);
                match.setCreateTime(now);
                match.setUpdateTime(now);
                match.setIsDelete(NOT_DELETED);
                if (matchMapper.insert(match) != 1) {
                    throw new ServiceException("写入比赛失败: " + match.getMatchNo());
                }
                seq++;
            }
        }
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿批次可发布");
        }
        if (matchMapper.countByBatchId(id) <= 0) {
            throw new ServiceException("批次下无比赛，无法发布");
        }

        Date now = new Date();
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        batch.setPublishTime(now);
        batch.setPublishedBy(SecurityUtils.getUserId());
        batch.setUpdateTime(now);
        if (scheduleBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("发布赛程批次失败");
        }
        matchMapper.publishByBatchId(id);

        BasketDivision division = requireDivision(batch.getDivisionId());
        division.setDivisionStatus(DivisionStatusEnum.SCHEDULED.getCode());
        division.setUpdateTime(now);
        divisionMapper.updateById(division);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿批次可作废");
        }
        matchMapper.softDeleteDraftByBatchId(id);
        if (scheduleBatchMapper.softDelete(id) != 1) {
            throw new ServiceException("作废赛程批次失败");
        }
    }

    private void validateDivisionForGenerate(BasketDivision division) {
        if (division.getFormatSnapshotId() != null) {
            throw new ServiceException("该 Division 已启用动态赛制，请使用「阶段执行」入口生成赛程，Legacy 入口仅保留历史只读");
        }
        String status = division.getDivisionStatus();
        if (DivisionStatusEnum.PLAYING.getCode().equals(status)
                || DivisionStatusEnum.FINISHED.getCode().equals(status)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(status)) {
            throw new ServiceException("Division 当前状态不允许生成赛程");
        }
        if (DivisionStatusEnum.DRAFT.getCode().equals(status)
                || DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(status)) {
            throw new ServiceException("Division 需处于报名截止( REGISTRATION_CLOSED )或已排赛(SCHEDULED)状态");
        }
    }

    private void validatePairings(List<Round> rounds, Long divisionId) {
        Set<String> pairKeys = new HashSet<>();
        for (Round round : rounds) {
            Set<Long> teamsInRound = new HashSet<>();
            for (Pairing p : round.getPairings()) {
                if (p.getHomeEventTeamId().equals(p.getAwayEventTeamId())) {
                    throw new ServiceException("主客队不能相同");
                }
                long a = Math.min(p.getHomeEventTeamId(), p.getAwayEventTeamId());
                long b = Math.max(p.getHomeEventTeamId(), p.getAwayEventTeamId());
                String key = a + "-" + b;
                if (!pairKeys.add(key)) {
                    throw new ServiceException("存在重复对阵: " + key);
                }
                if (!teamsInRound.add(p.getHomeEventTeamId())) {
                    throw new ServiceException("同一轮次球队重复出场");
                }
                if (!teamsInRound.add(p.getAwayEventTeamId())) {
                    throw new ServiceException("同一轮次球队重复出场");
                }
            }
        }
    }

    private BasketScheduleBatch requireBatch(Long id) {
        if (id == null) {
            throw new ServiceException("ID 不能为空");
        }
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(id);
        if (batch == null) {
            throw new ServiceException("赛程批次不存在");
        }
        return batch;
    }

    private BasketDivision requireDivision(Long id) {
        BasketDivision division = divisionMapper.selectById(id);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }
}
