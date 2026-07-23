package com.zhs.service.impl;


import com.zhs.model.BasketTournamentAwardConfig;
import com.zhs.dao.BasketTournamentAwardConfigDao;
import com.zhs.service.IBasketTournamentAwardConfigService;
import com.zhs.dto.BasketTournamentAwardConfigDto;


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
public class BasketTournamentAwardConfigServiceImpl extends ServiceImpl< BasketTournamentAwardConfigDao, BasketTournamentAwardConfig> implements IBasketTournamentAwardConfigService {

    @Resource
    private BasketTournamentAwardConfigDao basketTournamentAwardConfigDao;

    @Override
    public R addOrUpdate(BasketTournamentAwardConfigDto basketTournamentAwardConfigDto) {
        if (BeanUtil.isEmpty(basketTournamentAwardConfigDto)) throw new ServiceException("数据不能为空");
        BasketTournamentAwardConfig basketTournamentAwardConfig = new BasketTournamentAwardConfig();
        BeanUtil.copyProperties(basketTournamentAwardConfigDto,basketTournamentAwardConfig);
        if (basketTournamentAwardConfigDto.getId() == null){
            basketTournamentAwardConfigDao.insert(basketTournamentAwardConfig);
            return R.ok("数据插入成功");
        }else {
            basketTournamentAwardConfigDao.updateById(basketTournamentAwardConfig);
            return R.ok("数据更新成功");
        }
    }
}