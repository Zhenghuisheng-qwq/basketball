package com.zhs.controller;



import com.zhs.service.IBasketMatchTeamService;
import com.zhs.model.BasketMatchTeam;
import com.zhs.dto.BasketMatchTeamDto;

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
@RequestMapping("/basket/match/team")
@Api(value = "")
@Slf4j
public class BasketMatchTeamController {

    @Resource
    private IBasketMatchTeamService  iBasketMatchTeamService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchTeam>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchTeam> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchTeam> lambda = new QueryWrapper<BasketMatchTeam>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchTeam::getIsDelete,0);
        IPage<BasketMatchTeam> pages =  iBasketMatchTeamService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchTeam> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchTeam> wrapper = new QueryWrapper<BasketMatchTeam>()
                            .lambda().eq(BasketMatchTeam::getId,id).eq(BasketMatchTeam::getIsDelete,0);
        return R.ok(iBasketMatchTeamService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchTeam> query = new QueryWrapper<BasketMatchTeam>().lambda().eq(BasketMatchTeam::getId, id).eq(BasketMatchTeam::getIsDelete, 0);
        BasketMatchTeam basketMatchTeam = iBasketMatchTeamService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchTeam)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchTeam.setIsDelete(1);
        iBasketMatchTeamService.updateById(basketMatchTeam);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchTeamDto basketMatchTeamDto){
        if (CollectionUtils.isEmpty(basketMatchTeamDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchTeam> list = new ArrayList<>();
        basketMatchTeamDto.getIdList().stream().forEach(id ->{
        BasketMatchTeam basketMatchTeam = iBasketMatchTeamService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchTeam)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchTeam.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchTeam.setIsDelete(1);
            list.add(basketMatchTeam);
        });
        iBasketMatchTeamService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchTeamDto basketMatchTeamDto){
        return iBasketMatchTeamService.addOrUpdate(basketMatchTeamDto);
    }

}
