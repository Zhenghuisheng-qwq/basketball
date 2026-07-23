package com.zhs.service.impl;


import com.zhs.model.BasketTournament;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.service.IBasketTournamentService;
import com.zhs.dto.BasketTournamentDto;


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
public class BasketTournamentServiceImpl extends ServiceImpl< BasketTournamentDao, BasketTournament> implements IBasketTournamentService {

    @Resource
    private BasketTournamentDao basketTournamentDao;

    @Override
    public R addOrUpdate(BasketTournamentDto basketTournamentDto) {
        if (BeanUtil.isEmpty(basketTournamentDto)) throw new ServiceException("数据不能为空");
        BasketTournament basketTournament = new BasketTournament();
        BeanUtil.copyProperties(basketTournamentDto,basketTournament);
        if (basketTournamentDto.getId() == null){
            basketTournamentDao.insert(basketTournament);
            return R.ok("数据插入成功");
        }else {
            basketTournamentDao.updateById(basketTournament);
            return R.ok("数据更新成功");
        }
    }
}