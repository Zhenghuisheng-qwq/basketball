package com.zhs.controller;



import com.zhs.service.IBasketTournamentAuditService;
import com.zhs.model.BasketTournamentAudit;
import com.zhs.dto.BasketTournamentAuditDto;

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
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/tournament/audit")
@Api(value = "")
@Slf4j
public class BasketTournamentAuditController {

    @Resource
    private IBasketTournamentAuditService  iBasketTournamentAuditService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentAudit>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentAudit> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentAudit> lambda = new QueryWrapper<BasketTournamentAudit>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentAudit::getIsDelete,0);
        IPage<BasketTournamentAudit> pages =  iBasketTournamentAuditService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentAudit> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentAudit> wrapper = new QueryWrapper<BasketTournamentAudit>()
                            .lambda().eq(BasketTournamentAudit::getId,id).eq(BasketTournamentAudit::getIsDelete,0);
        return R.ok(iBasketTournamentAuditService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentAudit> query = new QueryWrapper<BasketTournamentAudit>().lambda().eq(BasketTournamentAudit::getId, id).eq(BasketTournamentAudit::getIsDelete, 0);
        BasketTournamentAudit basketTournamentAudit = iBasketTournamentAuditService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentAudit)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentAudit.setIsDelete(1);
        iBasketTournamentAuditService.updateById(basketTournamentAudit);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentAuditDto basketTournamentAuditDto){
        if (CollectionUtils.isEmpty(basketTournamentAuditDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentAudit> list = new ArrayList<>();
        basketTournamentAuditDto.getIdList().stream().forEach(id ->{
        BasketTournamentAudit basketTournamentAudit = iBasketTournamentAuditService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentAudit)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentAudit.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentAudit.setIsDelete(1);
            list.add(basketTournamentAudit);
        });
        iBasketTournamentAuditService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentAuditDto basketTournamentAuditDto){
        return iBasketTournamentAuditService.addOrUpdate(basketTournamentAuditDto);
    }

}
