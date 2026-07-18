package com.zhs.controller;



import com.zhs.service.IBasketTournamentTeamService;
import com.zhs.model.BasketTournamentTeam;
import com.zhs.dto.BasketTournamentTeamDto;

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
@RequestMapping("/basket/tournament/team")
@Api(value = "")
@Slf4j
public class BasketTournamentTeamController {

    @Resource
    private IBasketTournamentTeamService  iBasketTournamentTeamService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentTeam>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentTeam> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentTeam> lambda = new QueryWrapper<BasketTournamentTeam>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentTeam::getIsDelete,0);
        IPage<BasketTournamentTeam> pages =  iBasketTournamentTeamService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentTeam> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentTeam> wrapper = new QueryWrapper<BasketTournamentTeam>()
                            .lambda().eq(BasketTournamentTeam::getId,id).eq(BasketTournamentTeam::getIsDelete,0);
        return R.ok(iBasketTournamentTeamService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentTeam> query = new QueryWrapper<BasketTournamentTeam>().lambda().eq(BasketTournamentTeam::getId, id).eq(BasketTournamentTeam::getIsDelete, 0);
        BasketTournamentTeam basketTournamentTeam = iBasketTournamentTeamService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentTeam)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentTeam.setIsDelete(1);
        iBasketTournamentTeamService.updateById(basketTournamentTeam);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentTeamDto basketTournamentTeamDto){
        if (CollectionUtils.isEmpty(basketTournamentTeamDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentTeam> list = new ArrayList<>();
        basketTournamentTeamDto.getIdList().stream().forEach(id ->{
        BasketTournamentTeam basketTournamentTeam = iBasketTournamentTeamService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentTeam)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentTeam.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentTeam.setIsDelete(1);
            list.add(basketTournamentTeam);
        });
        iBasketTournamentTeamService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentTeamDto basketTournamentTeamDto){
        return iBasketTournamentTeamService.addOrUpdate(basketTournamentTeamDto);
    }

}
