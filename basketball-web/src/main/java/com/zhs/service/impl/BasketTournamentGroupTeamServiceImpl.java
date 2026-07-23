package com.zhs.service.impl;


import com.zhs.model.BasketTournamentGroupTeam;
import com.zhs.dao.BasketTournamentGroupTeamDao;
import com.zhs.service.IBasketTournamentGroupTeamService;
import com.zhs.dto.BasketTournamentGroupTeamDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketTournamentGroupTeamServiceImpl extends ServiceImpl< BasketTournamentGroupTeamDao, BasketTournamentGroupTeam> implements IBasketTournamentGroupTeamService {

    @Resource
    private BasketTournamentGroupTeamDao basketTournamentGroupTeamDao;

    @Override
    public R addOrUpdate(BasketTournamentGroupTeamDto basketTournamentGroupTeamDto) {
        if (BeanUtil.isEmpty(basketTournamentGroupTeamDto)) throw new ServiceException("数据不能为空");
        BasketTournamentGroupTeam basketTournamentGroupTeam = new BasketTournamentGroupTeam();
        BeanUtil.copyProperties(basketTournamentGroupTeamDto,basketTournamentGroupTeam);
        if (basketTournamentGroupTeamDto.getId() == null){
            basketTournamentGroupTeamDao.insert(basketTournamentGroupTeam);
            return R.ok("数据插入成功");
        }else {
            basketTournamentGroupTeamDao.updateById(basketTournamentGroupTeam);
            return R.ok("数据更新成功");
        }
    }
}