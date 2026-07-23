package com.zhs.controller;



import com.zhs.service.IBasketPlayerCertAuditService;
import com.zhs.model.BasketPlayerCertAudit;
import com.zhs.dto.BasketPlayerCertAuditDto;

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
@RequestMapping("/basket/player/cert/audit")
@Api(value = "")
@Slf4j
public class BasketPlayerCertAuditController {

    @Resource
    private IBasketPlayerCertAuditService  iBasketPlayerCertAuditService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerCertAudit>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerCertAudit> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerCertAudit> lambda = new QueryWrapper<BasketPlayerCertAudit>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerCertAudit::getIsDelete,0);
        IPage<BasketPlayerCertAudit> pages =  iBasketPlayerCertAuditService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerCertAudit> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerCertAudit> wrapper = new QueryWrapper<BasketPlayerCertAudit>()
                            .lambda().eq(BasketPlayerCertAudit::getId,id).eq(BasketPlayerCertAudit::getIsDelete,0);
        return R.ok(iBasketPlayerCertAuditService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerCertAudit> query = new QueryWrapper<BasketPlayerCertAudit>().lambda().eq(BasketPlayerCertAudit::getId, id).eq(BasketPlayerCertAudit::getIsDelete, 0);
        BasketPlayerCertAudit basketPlayerCertAudit = iBasketPlayerCertAuditService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerCertAudit)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerCertAudit.setIsDelete(1);
        iBasketPlayerCertAuditService.updateById(basketPlayerCertAudit);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerCertAuditDto basketPlayerCertAuditDto){
        if (CollectionUtils.isEmpty(basketPlayerCertAuditDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerCertAudit> list = new ArrayList<>();
        basketPlayerCertAuditDto.getIdList().stream().forEach(id ->{
        BasketPlayerCertAudit basketPlayerCertAudit = iBasketPlayerCertAuditService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerCertAudit)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerCertAudit.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerCertAudit.setIsDelete(1);
            list.add(basketPlayerCertAudit);
        });
        iBasketPlayerCertAuditService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerCertAuditDto basketPlayerCertAuditDto){
        return iBasketPlayerCertAuditService.addOrUpdate(basketPlayerCertAuditDto);
    }

}
