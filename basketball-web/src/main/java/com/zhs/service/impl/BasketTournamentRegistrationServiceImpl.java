package com.zhs.service.impl;


import com.zhs.model.BasketTournamentRegistration;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.service.IBasketTournamentRegistrationService;
import com.zhs.dto.BasketTournamentRegistrationDto;


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
public class BasketTournamentRegistrationServiceImpl extends ServiceImpl< BasketTournamentRegistrationDao, BasketTournamentRegistration> implements IBasketTournamentRegistrationService {

    @Resource
    private BasketTournamentRegistrationDao basketTournamentRegistrationDao;

    @Override
    public R addOrUpdate(BasketTournamentRegistrationDto basketTournamentRegistrationDto) {
        if (BeanUtil.isEmpty(basketTournamentRegistrationDto)) throw new ServiceException("数据不能为空");
        BasketTournamentRegistration basketTournamentRegistration = new BasketTournamentRegistration();
        BeanUtil.copyProperties(basketTournamentRegistrationDto,basketTournamentRegistration);
        if (basketTournamentRegistrationDto.getId() == null){
            basketTournamentRegistrationDao.insert(basketTournamentRegistration);
            return R.ok("数据插入成功");
        }else {
            basketTournamentRegistrationDao.updateById(basketTournamentRegistration);
            return R.ok("数据更新成功");
        }
    }
}