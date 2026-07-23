package com.zhs.service.impl;


import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.zhs.dao.BasketTournamentRegistrationPlayerDao;
import com.zhs.service.IBasketTournamentRegistrationPlayerService;
import com.zhs.dto.BasketTournamentRegistrationPlayerDto;


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
public class BasketTournamentRegistrationPlayerServiceImpl extends ServiceImpl< BasketTournamentRegistrationPlayerDao, BasketTournamentRegistrationPlayer> implements IBasketTournamentRegistrationPlayerService {

    @Resource
    private BasketTournamentRegistrationPlayerDao basketTournamentRegistrationPlayerDao;

    @Override
    public R addOrUpdate(BasketTournamentRegistrationPlayerDto basketTournamentRegistrationPlayerDto) {
        if (BeanUtil.isEmpty(basketTournamentRegistrationPlayerDto)) throw new ServiceException("数据不能为空");
        BasketTournamentRegistrationPlayer basketTournamentRegistrationPlayer = new BasketTournamentRegistrationPlayer();
        BeanUtil.copyProperties(basketTournamentRegistrationPlayerDto,basketTournamentRegistrationPlayer);
        if (basketTournamentRegistrationPlayerDto.getId() == null){
            basketTournamentRegistrationPlayerDao.insert(basketTournamentRegistrationPlayer);
            return R.ok("数据插入成功");
        }else {
            basketTournamentRegistrationPlayerDao.updateById(basketTournamentRegistrationPlayer);
            return R.ok("数据更新成功");
        }
    }
}