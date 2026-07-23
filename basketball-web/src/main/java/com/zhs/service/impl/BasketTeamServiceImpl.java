package com.zhs.service.impl;


import com.zhs.model.BasketTeam;
import com.zhs.dao.BasketTeamDao;
import com.zhs.service.IBasketTeamService;
import com.zhs.dto.BasketTeamDto;


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
public class BasketTeamServiceImpl extends ServiceImpl< BasketTeamDao, BasketTeam> implements IBasketTeamService {

    @Resource
    private BasketTeamDao basketTeamDao;

    @Override
    public R addOrUpdate(BasketTeamDto basketTeamDto) {
        if (BeanUtil.isEmpty(basketTeamDto)) throw new ServiceException("数据不能为空");
        BasketTeam basketTeam = new BasketTeam();
        BeanUtil.copyProperties(basketTeamDto,basketTeam);
        if (basketTeamDto.getId() == null){
            basketTeamDao.insert(basketTeam);
            return R.ok("数据插入成功");
        }else {
            basketTeamDao.updateById(basketTeam);
            return R.ok("数据更新成功");
        }
    }
}