package com.zhs.controller;



import com.zhs.service.IBasketInviteApplyService;
import com.zhs.model.BasketInviteApply;
import com.zhs.dto.BasketInviteApplyDto;

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
@RequestMapping("/basket/invite/apply")
@Api(value = "")
@Slf4j
public class BasketInviteApplyController {

    @Resource
    private IBasketInviteApplyService  iBasketInviteApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketInviteApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketInviteApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketInviteApply> lambda = new QueryWrapper<BasketInviteApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketInviteApply::getIsDelete,0);
        IPage<BasketInviteApply> pages =  iBasketInviteApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketInviteApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketInviteApply> wrapper = new QueryWrapper<BasketInviteApply>()
                            .lambda().eq(BasketInviteApply::getId,id).eq(BasketInviteApply::getIsDelete,0);
        return R.ok(iBasketInviteApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketInviteApply> query = new QueryWrapper<BasketInviteApply>().lambda().eq(BasketInviteApply::getId, id).eq(BasketInviteApply::getIsDelete, 0);
        BasketInviteApply basketInviteApply = iBasketInviteApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketInviteApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketInviteApply.setIsDelete(1);
        iBasketInviteApplyService.updateById(basketInviteApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketInviteApplyDto basketInviteApplyDto){
        if (CollectionUtils.isEmpty(basketInviteApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketInviteApply> list = new ArrayList<>();
        basketInviteApplyDto.getIdList().stream().forEach(id ->{
        BasketInviteApply basketInviteApply = iBasketInviteApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketInviteApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketInviteApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketInviteApply.setIsDelete(1);
            list.add(basketInviteApply);
        });
        iBasketInviteApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketInviteApplyDto basketInviteApplyDto){
        return iBasketInviteApplyService.addOrUpdate(basketInviteApplyDto);
    }

}
