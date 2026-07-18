package com.zhs.controller;



import com.zhs.service.IBasketScoreAuditService;
import com.zhs.model.BasketScoreAudit;
import com.zhs.dto.BasketScoreAuditDto;

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
@RequestMapping("/basket/score/audit")
@Api(value = "")
@Slf4j
public class BasketScoreAuditController {

    @Resource
    private IBasketScoreAuditService  iBasketScoreAuditService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketScoreAudit>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketScoreAudit> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketScoreAudit> lambda = new QueryWrapper<BasketScoreAudit>().lambda();
        //此处可以拼条件
        lambda.eq(BasketScoreAudit::getIsDelete,0);
        IPage<BasketScoreAudit> pages =  iBasketScoreAuditService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketScoreAudit> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketScoreAudit> wrapper = new QueryWrapper<BasketScoreAudit>()
                            .lambda().eq(BasketScoreAudit::getId,id).eq(BasketScoreAudit::getIsDelete,0);
        return R.ok(iBasketScoreAuditService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketScoreAudit> query = new QueryWrapper<BasketScoreAudit>().lambda().eq(BasketScoreAudit::getId, id).eq(BasketScoreAudit::getIsDelete, 0);
        BasketScoreAudit basketScoreAudit = iBasketScoreAuditService.getOne(query);
        if(ObjectUtils.isEmpty(basketScoreAudit)) throw new ServiceException("该数据不存在或者已经被删除");
        basketScoreAudit.setIsDelete(1);
        iBasketScoreAuditService.updateById(basketScoreAudit);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketScoreAuditDto basketScoreAuditDto){
        if (CollectionUtils.isEmpty(basketScoreAuditDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketScoreAudit> list = new ArrayList<>();
        basketScoreAuditDto.getIdList().stream().forEach(id ->{
        BasketScoreAudit basketScoreAudit = iBasketScoreAuditService.getById(id);
            if (ObjectUtils.isEmpty(basketScoreAudit)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketScoreAudit.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketScoreAudit.setIsDelete(1);
            list.add(basketScoreAudit);
        });
        iBasketScoreAuditService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketScoreAuditDto basketScoreAuditDto){
        return iBasketScoreAuditService.addOrUpdate(basketScoreAuditDto);
    }

}
