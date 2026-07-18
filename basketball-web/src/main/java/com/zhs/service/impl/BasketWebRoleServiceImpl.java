package com.zhs.service.impl;


import com.zhs.model.BasketWebRole;
import com.zhs.dao.BasketWebRoleDao;
import com.zhs.service.IBasketWebRoleService;
import com.zhs.dto.BasketWebRoleDto;


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
public class BasketWebRoleServiceImpl extends ServiceImpl< BasketWebRoleDao, BasketWebRole> implements IBasketWebRoleService {

    @Resource
    private BasketWebRoleDao basketWebRoleDao;

    @Override
    public R addOrUpdate(BasketWebRoleDto basketWebRoleDto) {
        if (BeanUtil.isEmpty(basketWebRoleDto)) throw new ServiceException("数据不能为空");
        BasketWebRole basketWebRole = new BasketWebRole();
        BeanUtil.copyProperties(basketWebRoleDto,basketWebRole);
        if (basketWebRoleDto.getId() == null){
            basketWebRoleDao.insert(basketWebRole);
            return R.ok("数据插入成功");
        }else {
            basketWebRoleDao.updateById(basketWebRole);
            return R.ok("数据更新成功");
        }
    }
}