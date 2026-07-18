package com.zhs.service.impl;


import com.zhs.model.BasketTournamentTeam;
import com.zhs.dao.BasketTournamentTeamDao;
import com.zhs.service.IBasketTournamentTeamService;
import com.zhs.dto.BasketTournamentTeamDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketTournamentTeamServiceImpl extends ServiceImpl< BasketTournamentTeamDao, BasketTournamentTeam> implements IBasketTournamentTeamService {

    @Resource
    private BasketTournamentTeamDao basketTournamentTeamDao;

    @Override
    public R addOrUpdate(BasketTournamentTeamDto basketTournamentTeamDto) {
        if (BeanUtil.isEmpty(basketTournamentTeamDto)) throw new ServiceException("数据不能为空");
        BasketTournamentTeam basketTournamentTeam = new BasketTournamentTeam();
        BeanUtil.copyProperties(basketTournamentTeamDto,basketTournamentTeam);
        if (basketTournamentTeamDto.getId() == null){
            basketTournamentTeamDao.insert(basketTournamentTeam);
            return R.ok("数据插入成功");
        }else {
            basketTournamentTeamDao.updateById(basketTournamentTeam);
            return R.ok("数据更新成功");
        }
    }
}