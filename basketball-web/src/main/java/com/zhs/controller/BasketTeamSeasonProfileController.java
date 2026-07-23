package com.zhs.controller;



import com.zhs.service.IBasketTeamSeasonProfileService;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.dto.BasketTeamSeasonProfileDto;

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
@RequestMapping("/basket/team/season/profile")
@Api(value = "")
@Slf4j
public class BasketTeamSeasonProfileController {

    @Resource
    private IBasketTeamSeasonProfileService  iBasketTeamSeasonProfileService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeamSeasonProfile>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeamSeasonProfile> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamSeasonProfile> lambda = new QueryWrapper<BasketTeamSeasonProfile>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeamSeasonProfile::getIsDelete,0);
        IPage<BasketTeamSeasonProfile> pages =  iBasketTeamSeasonProfileService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeamSeasonProfile> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeamSeasonProfile> wrapper = new QueryWrapper<BasketTeamSeasonProfile>()
                            .lambda().eq(BasketTeamSeasonProfile::getId,id).eq(BasketTeamSeasonProfile::getIsDelete,0);
        return R.ok(iBasketTeamSeasonProfileService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeamSeasonProfile> query = new QueryWrapper<BasketTeamSeasonProfile>().lambda().eq(BasketTeamSeasonProfile::getId, id).eq(BasketTeamSeasonProfile::getIsDelete, 0);
        BasketTeamSeasonProfile basketTeamSeasonProfile = iBasketTeamSeasonProfileService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeamSeasonProfile)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeamSeasonProfile.setIsDelete(1);
        iBasketTeamSeasonProfileService.updateById(basketTeamSeasonProfile);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamSeasonProfileDto basketTeamSeasonProfileDto){
        if (CollectionUtils.isEmpty(basketTeamSeasonProfileDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeamSeasonProfile> list = new ArrayList<>();
        basketTeamSeasonProfileDto.getIdList().stream().forEach(id ->{
        BasketTeamSeasonProfile basketTeamSeasonProfile = iBasketTeamSeasonProfileService.getById(id);
            if (ObjectUtils.isEmpty(basketTeamSeasonProfile)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeamSeasonProfile.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeamSeasonProfile.setIsDelete(1);
            list.add(basketTeamSeasonProfile);
        });
        iBasketTeamSeasonProfileService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamSeasonProfileDto basketTeamSeasonProfileDto){
        return iBasketTeamSeasonProfileService.addOrUpdate(basketTeamSeasonProfileDto);
    }

}
