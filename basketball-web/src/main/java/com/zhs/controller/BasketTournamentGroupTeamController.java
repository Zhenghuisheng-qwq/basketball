package com.zhs.controller;



import com.zhs.service.IBasketTournamentGroupTeamService;
import com.zhs.model.BasketTournamentGroupTeam;
import com.zhs.dto.BasketTournamentGroupTeamDto;

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
@RequestMapping("/basket/tournament/group/team")
@Api(value = "")
@Slf4j
public class BasketTournamentGroupTeamController {

    @Resource
    private IBasketTournamentGroupTeamService  iBasketTournamentGroupTeamService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentGroupTeam>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentGroupTeam> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentGroupTeam> lambda = new QueryWrapper<BasketTournamentGroupTeam>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentGroupTeam::getIsDelete,0);
        IPage<BasketTournamentGroupTeam> pages =  iBasketTournamentGroupTeamService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentGroupTeam> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentGroupTeam> wrapper = new QueryWrapper<BasketTournamentGroupTeam>()
                            .lambda().eq(BasketTournamentGroupTeam::getId,id).eq(BasketTournamentGroupTeam::getIsDelete,0);
        return R.ok(iBasketTournamentGroupTeamService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentGroupTeam> query = new QueryWrapper<BasketTournamentGroupTeam>().lambda().eq(BasketTournamentGroupTeam::getId, id).eq(BasketTournamentGroupTeam::getIsDelete, 0);
        BasketTournamentGroupTeam basketTournamentGroupTeam = iBasketTournamentGroupTeamService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentGroupTeam)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentGroupTeam.setIsDelete(1);
        iBasketTournamentGroupTeamService.updateById(basketTournamentGroupTeam);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentGroupTeamDto basketTournamentGroupTeamDto){
        if (CollectionUtils.isEmpty(basketTournamentGroupTeamDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentGroupTeam> list = new ArrayList<>();
        basketTournamentGroupTeamDto.getIdList().stream().forEach(id ->{
        BasketTournamentGroupTeam basketTournamentGroupTeam = iBasketTournamentGroupTeamService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentGroupTeam)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentGroupTeam.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentGroupTeam.setIsDelete(1);
            list.add(basketTournamentGroupTeam);
        });
        iBasketTournamentGroupTeamService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentGroupTeamDto basketTournamentGroupTeamDto){
        return iBasketTournamentGroupTeamService.addOrUpdate(basketTournamentGroupTeamDto);
    }

}
