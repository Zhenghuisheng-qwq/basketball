package com.zhs.service.impl;


import com.zhs.model.BasketTeamMember;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.service.IBasketTeamMemberService;
import com.zhs.dto.BasketTeamMemberDto;


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
public class BasketTeamMemberServiceImpl extends ServiceImpl< BasketTeamMemberDao, BasketTeamMember> implements IBasketTeamMemberService {

    @Resource
    private BasketTeamMemberDao basketTeamMemberDao;

    @Override
    public R addOrUpdate(BasketTeamMemberDto basketTeamMemberDto) {
        if (BeanUtil.isEmpty(basketTeamMemberDto)) throw new ServiceException("数据不能为空");
        BasketTeamMember basketTeamMember = new BasketTeamMember();
        BeanUtil.copyProperties(basketTeamMemberDto,basketTeamMember);
        if (basketTeamMemberDto.getId() == null){
            basketTeamMemberDao.insert(basketTeamMember);
            return R.ok("数据插入成功");
        }else {
            basketTeamMemberDao.updateById(basketTeamMember);
            return R.ok("数据更新成功");
        }
    }
}