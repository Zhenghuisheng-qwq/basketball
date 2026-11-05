package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.DisciplineQuery;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.resp.HonorRecordListResp;
import com.zhs.basket.service.IBasketPermanentArchiveService;
import com.zhs.basket.vo.DisciplineSelectorOptionVo;
import com.zhs.basket.vo.PermanentPlayerArchiveVo;
import com.zhs.basket.vo.PermanentTeamArchiveVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketPermanentArchiveServiceImpl implements IBasketPermanentArchiveService {

    @Autowired private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketHonorRecordMapper honorRecordMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTeamAuditRecordMapper teamAuditRecordMapper;
    @Autowired private BasketDisciplineSelectorMapper disciplineSelectorMapper;

    @Override
    public PermanentPlayerArchiveVo getPlayerArchive(Long playerId) {
        BasketPlayerProfile profile = playerProfileMapper.selectById(playerId);
        if (profile == null) {
            throw new ServiceException("球员不存在");
        }
        PermanentPlayerArchiveVo vo = new PermanentPlayerArchiveVo();
        vo.setProfile(profile);
        if (profile.getMemberId() != null) {
            vo.setAccount(memberAccountMapper.selectById(profile.getMemberId()));
            vo.setTeamHistory(teamMemberMapper.selectTeamSummaryByMemberId(profile.getMemberId()));
        }
        DisciplineQuery disciplineQuery = new DisciplineQuery();
        disciplineQuery.setPlayerId(playerId);
        List<BasketDisciplineRecord> records = disciplineRecordMapper.selectList(disciplineQuery).stream()
                .map(item -> disciplineRecordMapper.selectById(item.getId()))
                .collect(Collectors.toList());
        vo.setDisciplineRecords(records);
        HonorRecordQueryReq honorQuery = new HonorRecordQueryReq();
        honorQuery.setPlayerId(playerId);
        List<HonorRecordListResp> honors = honorRecordMapper.selectList(honorQuery);
        vo.setHonors(honors != null ? honors : Collections.emptyList());
        return vo;
    }

    @Override
    public PermanentTeamArchiveVo getTeamArchive(Long teamId) {
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        PermanentTeamArchiveVo vo = new PermanentTeamArchiveVo();
        vo.setTeam(team);
        vo.setAuditHistory(teamAuditRecordMapper.selectByTeamId(teamId));
        DisciplineQuery disciplineQuery = new DisciplineQuery();
        disciplineQuery.setTeamId(teamId);
        List<BasketDisciplineRecord> records = disciplineRecordMapper.selectList(disciplineQuery).stream()
                .map(item -> disciplineRecordMapper.selectById(item.getId()))
                .collect(Collectors.toList());
        vo.setDisciplineRecords(records);
        HonorRecordQueryReq honorQuery = new HonorRecordQueryReq();
        honorQuery.setTeamId(teamId);
        List<HonorRecordListResp> honors = honorRecordMapper.selectList(honorQuery);
        vo.setHonors(honors != null ? honors : Collections.emptyList());
        return vo;
    }

    @Override
    public List<DisciplineSelectorOptionVo> selectPlayerOptions(String keyword) {
        return disciplineSelectorMapper.selectArchivePlayerOptions(StringUtils.trim(keyword));
    }

    @Override
    public List<DisciplineSelectorOptionVo> selectTeamOptions(String keyword) {
        return disciplineSelectorMapper.selectArchiveTeamOptions(StringUtils.trim(keyword));
    }
}
