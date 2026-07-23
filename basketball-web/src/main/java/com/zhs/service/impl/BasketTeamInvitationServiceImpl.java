package com.zhs.service.impl;


import com.zhs.model.BasketTeamInvitation;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.service.IBasketTeamInvitationService;
import com.zhs.dto.BasketTeamInvitationDto;


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
public class BasketTeamInvitationServiceImpl extends ServiceImpl< BasketTeamInvitationDao, BasketTeamInvitation> implements IBasketTeamInvitationService {

    @Resource
    private BasketTeamInvitationDao basketTeamInvitationDao;

    @Override
    public R addOrUpdate(BasketTeamInvitationDto basketTeamInvitationDto) {
        if (BeanUtil.isEmpty(basketTeamInvitationDto)) throw new ServiceException("数据不能为空");
        BasketTeamInvitation basketTeamInvitation = new BasketTeamInvitation();
        BeanUtil.copyProperties(basketTeamInvitationDto,basketTeamInvitation);
        if (basketTeamInvitationDto.getId() == null){
            basketTeamInvitationDao.insert(basketTeamInvitation);
            return R.ok("数据插入成功");
        }else {
            basketTeamInvitationDao.updateById(basketTeamInvitation);
            return R.ok("数据更新成功");
        }
    }
}