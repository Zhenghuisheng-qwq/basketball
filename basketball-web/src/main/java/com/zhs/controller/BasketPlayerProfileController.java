package com.zhs.controller;



import com.zhs.service.IBasketPlayerProfileService;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.dto.BasketPlayerProfileDto;

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
@RequestMapping("/basket/player/profile")
@Api(value = "")
@Slf4j
public class BasketPlayerProfileController {

    @Resource
    private IBasketPlayerProfileService  iBasketPlayerProfileService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerProfile>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerProfile> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerProfile> lambda = new QueryWrapper<BasketPlayerProfile>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerProfile::getIsDelete,0);
        IPage<BasketPlayerProfile> pages =  iBasketPlayerProfileService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerProfile> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerProfile> wrapper = new QueryWrapper<BasketPlayerProfile>()
                            .lambda().eq(BasketPlayerProfile::getId,id).eq(BasketPlayerProfile::getIsDelete,0);
        return R.ok(iBasketPlayerProfileService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerProfile> query = new QueryWrapper<BasketPlayerProfile>().lambda().eq(BasketPlayerProfile::getId, id).eq(BasketPlayerProfile::getIsDelete, 0);
        BasketPlayerProfile basketPlayerProfile = iBasketPlayerProfileService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerProfile)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerProfile.setIsDelete(1);
        iBasketPlayerProfileService.updateById(basketPlayerProfile);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerProfileDto basketPlayerProfileDto){
        if (CollectionUtils.isEmpty(basketPlayerProfileDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerProfile> list = new ArrayList<>();
        basketPlayerProfileDto.getIdList().stream().forEach(id ->{
        BasketPlayerProfile basketPlayerProfile = iBasketPlayerProfileService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerProfile)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerProfile.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerProfile.setIsDelete(1);
            list.add(basketPlayerProfile);
        });
        iBasketPlayerProfileService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerProfileDto basketPlayerProfileDto){
        return iBasketPlayerProfileService.addOrUpdate(basketPlayerProfileDto);
    }

}
