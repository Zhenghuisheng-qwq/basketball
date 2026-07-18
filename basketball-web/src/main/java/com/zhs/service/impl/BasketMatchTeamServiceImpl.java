package com.zhs.service.impl;


import com.zhs.model.BasketMatchTeam;
import com.zhs.dao.BasketMatchTeamDao;
import com.zhs.service.IBasketMatchTeamService;
import com.zhs.dto.BasketMatchTeamDto;


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
public class BasketMatchTeamServiceImpl extends ServiceImpl< BasketMatchTeamDao, BasketMatchTeam> implements IBasketMatchTeamService {

    @Resource
    private BasketMatchTeamDao basketMatchTeamDao;

    @Override
    public R addOrUpdate(BasketMatchTeamDto basketMatchTeamDto) {
        if (BeanUtil.isEmpty(basketMatchTeamDto)) throw new ServiceException("数据不能为空");
        BasketMatchTeam basketMatchTeam = new BasketMatchTeam();
        BeanUtil.copyProperties(basketMatchTeamDto,basketMatchTeam);
        if (basketMatchTeamDto.getId() == null){
            basketMatchTeamDao.insert(basketMatchTeam);
            return R.ok("数据插入成功");
        }else {
            basketMatchTeamDao.updateById(basketMatchTeam);
            return R.ok("数据更新成功");
        }
    }
}