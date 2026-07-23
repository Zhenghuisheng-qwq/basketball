package com.zhs.service.impl;


import com.zhs.model.BasketTournamentStage;
import com.zhs.dao.BasketTournamentStageDao;
import com.zhs.service.IBasketTournamentStageService;
import com.zhs.dto.BasketTournamentStageDto;


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
public class BasketTournamentStageServiceImpl extends ServiceImpl< BasketTournamentStageDao, BasketTournamentStage> implements IBasketTournamentStageService {

    @Resource
    private BasketTournamentStageDao basketTournamentStageDao;

    @Override
    public R addOrUpdate(BasketTournamentStageDto basketTournamentStageDto) {
        if (BeanUtil.isEmpty(basketTournamentStageDto)) throw new ServiceException("数据不能为空");
        BasketTournamentStage basketTournamentStage = new BasketTournamentStage();
        BeanUtil.copyProperties(basketTournamentStageDto,basketTournamentStage);
        if (basketTournamentStageDto.getId() == null){
            basketTournamentStageDao.insert(basketTournamentStage);
            return R.ok("数据插入成功");
        }else {
            basketTournamentStageDao.updateById(basketTournamentStage);
            return R.ok("数据更新成功");
        }
    }
}