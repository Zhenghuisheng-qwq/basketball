package com.zhs.service.impl;


import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.service.IBasketTeamJoinApplicationService;
import com.zhs.dto.BasketTeamJoinApplicationDto;


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
public class BasketTeamJoinApplicationServiceImpl extends ServiceImpl< BasketTeamJoinApplicationDao, BasketTeamJoinApplication> implements IBasketTeamJoinApplicationService {

    @Resource
    private BasketTeamJoinApplicationDao basketTeamJoinApplicationDao;

    @Override
    public R addOrUpdate(BasketTeamJoinApplicationDto basketTeamJoinApplicationDto) {
        if (BeanUtil.isEmpty(basketTeamJoinApplicationDto)) throw new ServiceException("数据不能为空");
        BasketTeamJoinApplication basketTeamJoinApplication = new BasketTeamJoinApplication();
        BeanUtil.copyProperties(basketTeamJoinApplicationDto,basketTeamJoinApplication);
        if (basketTeamJoinApplicationDto.getId() == null){
            basketTeamJoinApplicationDao.insert(basketTeamJoinApplication);
            return R.ok("数据插入成功");
        }else {
            basketTeamJoinApplicationDao.updateById(basketTeamJoinApplication);
            return R.ok("数据更新成功");
        }
    }
}