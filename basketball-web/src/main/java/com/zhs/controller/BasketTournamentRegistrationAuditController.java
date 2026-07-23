package com.zhs.controller;



import com.zhs.service.IBasketTournamentRegistrationAuditService;
import com.zhs.model.BasketTournamentRegistrationAudit;
import com.zhs.dto.BasketTournamentRegistrationAuditDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/tournament/registration/audit")
@Api(value = "")
@Slf4j
public class BasketTournamentRegistrationAuditController {

    @Resource
    private IBasketTournamentRegistrationAuditService  iBasketTournamentRegistrationAuditService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentRegistrationAudit>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentRegistrationAudit> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentRegistrationAudit> lambda = new QueryWrapper<BasketTournamentRegistrationAudit>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentRegistrationAudit::getIsDelete,0);
        IPage<BasketTournamentRegistrationAudit> pages =  iBasketTournamentRegistrationAuditService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentRegistrationAudit> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentRegistrationAudit> wrapper = new QueryWrapper<BasketTournamentRegistrationAudit>()
                            .lambda().eq(BasketTournamentRegistrationAudit::getId,id).eq(BasketTournamentRegistrationAudit::getIsDelete,0);
        return R.ok(iBasketTournamentRegistrationAuditService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentRegistrationAudit> query = new QueryWrapper<BasketTournamentRegistrationAudit>().lambda().eq(BasketTournamentRegistrationAudit::getId, id).eq(BasketTournamentRegistrationAudit::getIsDelete, 0);
        BasketTournamentRegistrationAudit basketTournamentRegistrationAudit = iBasketTournamentRegistrationAuditService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentRegistrationAudit)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentRegistrationAudit.setIsDelete(1);
        iBasketTournamentRegistrationAuditService.updateById(basketTournamentRegistrationAudit);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentRegistrationAuditDto basketTournamentRegistrationAuditDto){
        if (CollectionUtils.isEmpty(basketTournamentRegistrationAuditDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentRegistrationAudit> list = new ArrayList<>();
        basketTournamentRegistrationAuditDto.getIdList().stream().forEach(id ->{
        BasketTournamentRegistrationAudit basketTournamentRegistrationAudit = iBasketTournamentRegistrationAuditService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentRegistrationAudit)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentRegistrationAudit.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentRegistrationAudit.setIsDelete(1);
            list.add(basketTournamentRegistrationAudit);
        });
        iBasketTournamentRegistrationAuditService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentRegistrationAuditDto basketTournamentRegistrationAuditDto){
        return iBasketTournamentRegistrationAuditService.addOrUpdate(basketTournamentRegistrationAuditDto);
    }

}
