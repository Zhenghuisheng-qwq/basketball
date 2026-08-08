package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketV4AdminOptionsMapper;
import com.zhs.basket.service.v4.IBasketV4AdminOptionsService;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.vo.v4.BasketV4AdminOptionVo;
import com.zhs.basket.vo.v4.BasketV4GrantablePlayerOptionVo;
import com.zhs.basket.vo.v4.BasketV4TeamPlayerOptionVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4AdminOptionsServiceImpl implements IBasketV4AdminOptionsService {

    @Autowired
    private BasketV4AdminOptionsMapper optionsMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;

    @Override
    public List<BasketV4AdminOptionVo> selectSeasonOptions() {
        return optionsMapper.selectSeasonOptions();
    }

    @Override
    public List<BasketV4AdminOptionVo> selectDivisionOptions(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("seasonId 不能为空");
        }
        return optionsMapper.selectDivisionOptions(seasonId);
    }

    @Override
    public List<BasketV4AdminOptionVo> selectGrantableTeamOptions(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        return optionsMapper.selectGrantableTeamOptions(divisionId);
    }

    @Override
    public List<BasketV4TeamPlayerOptionVo> selectTeamPlayerOptions(Long teamId, Long divisionId) {
        if (teamId == null) {
            throw new ServiceException("teamId 不能为空");
        }
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        String platformCode = participationSupport.normalizePlatformCode(division.getPlatformCode());
        return optionsMapper.selectTeamPlayerOptions(teamId, divisionId, platformCode);
    }

    @Override
    public List<BasketV4GrantablePlayerOptionVo> selectGrantablePlayerOptions(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        String platformCode = participationSupport.normalizePlatformCode(division.getPlatformCode());
        return optionsMapper.selectGrantablePlayerOptions(divisionId, platformCode);
    }
}
