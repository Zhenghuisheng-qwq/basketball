package com.zhs.controller;



import com.zhs.service.IBasketTeamService;
import com.zhs.model.BasketTeam;
import com.zhs.dto.BasketTeamDto;

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
@RequestMapping("/basket/team")
@Api(value = "")
@Slf4j
public class BasketTeamController {

    @Resource
    private IBasketTeamService  iBasketTeamService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeam>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeam> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeam> lambda = new QueryWrapper<BasketTeam>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeam::getIsDelete,0);
        IPage<BasketTeam> pages =  iBasketTeamService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeam> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeam> wrapper = new QueryWrapper<BasketTeam>()
                            .lambda().eq(BasketTeam::getId,id).eq(BasketTeam::getIsDelete,0);
        return R.ok(iBasketTeamService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeam> query = new QueryWrapper<BasketTeam>().lambda().eq(BasketTeam::getId, id).eq(BasketTeam::getIsDelete, 0);
        BasketTeam basketTeam = iBasketTeamService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeam)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeam.setIsDelete(1);
        iBasketTeamService.updateById(basketTeam);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamDto basketTeamDto){
        if (CollectionUtils.isEmpty(basketTeamDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeam> list = new ArrayList<>();
        basketTeamDto.getIdList().stream().forEach(id ->{
        BasketTeam basketTeam = iBasketTeamService.getById(id);
            if (ObjectUtils.isEmpty(basketTeam)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeam.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeam.setIsDelete(1);
            list.add(basketTeam);
        });
        iBasketTeamService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamDto basketTeamDto){
        return iBasketTeamService.addOrUpdate(basketTeamDto);
    }

}
