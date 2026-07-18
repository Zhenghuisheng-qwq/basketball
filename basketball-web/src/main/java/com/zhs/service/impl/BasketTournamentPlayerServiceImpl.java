package com.zhs.service.impl;


import com.zhs.model.BasketTournamentPlayer;
import com.zhs.dao.BasketTournamentPlayerDao;
import com.zhs.service.IBasketTournamentPlayerService;
import com.zhs.dto.BasketTournamentPlayerDto;


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
public class BasketTournamentPlayerServiceImpl extends ServiceImpl< BasketTournamentPlayerDao, BasketTournamentPlayer> implements IBasketTournamentPlayerService {

    @Resource
    private BasketTournamentPlayerDao basketTournamentPlayerDao;

    @Override
    public R addOrUpdate(BasketTournamentPlayerDto basketTournamentPlayerDto) {
        if (BeanUtil.isEmpty(basketTournamentPlayerDto)) throw new ServiceException("数据不能为空");
        BasketTournamentPlayer basketTournamentPlayer = new BasketTournamentPlayer();
        BeanUtil.copyProperties(basketTournamentPlayerDto,basketTournamentPlayer);
        if (basketTournamentPlayerDto.getId() == null){
            basketTournamentPlayerDao.insert(basketTournamentPlayer);
            return R.ok("数据插入成功");
        }else {
            basketTournamentPlayerDao.updateById(basketTournamentPlayer);
            return R.ok("数据更新成功");
        }
    }
}