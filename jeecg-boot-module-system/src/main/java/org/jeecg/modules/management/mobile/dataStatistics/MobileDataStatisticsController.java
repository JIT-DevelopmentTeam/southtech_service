package org.jeecg.modules.management.mobile.dataStatistics;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.management.workorder.service.IWorkOrderService;
import org.jeecg.modules.management.workorder.vo.RankingDTO;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/mobile/dataStatistics")
public class MobileDataStatisticsController {

    @Autowired
    private IWorkOrderService workOrderService;

    @Autowired
    private ISysUserService sysUserService;

    @GetMapping(value = "/dataStatistics")
    public Result<?> dataStatistics(String userId, String status) {
        Map<String, Object> result = new HashMap<>();
        Integer all = workOrderService.dataStatistics("", "");// 接单量
        Integer completed = workOrderService.dataStatistics("", status);// 完成量
        SysUser sysUser = sysUserService.getByEnterpriseId(userId);
        Integer myAll = 0;
        Integer myCompleted = 0;
        if (sysUser != null) {
            myAll = workOrderService.dataStatistics(sysUser.getUsername(), "");// 我的接单量
            myCompleted = workOrderService.dataStatistics(sysUser.getUsername(), status);// 我的完成量
        }
        result.put("all", all);
        result.put("completed", completed);
        result.put("myAll", myAll);
        result.put("myCompleted", myCompleted);
        return Result.ok(result);
    }

    @GetMapping(value = "/ranking")
    public Result<?> ranking(String status) {
        ArrayList<List<RankingDTO>> allArr = new ArrayList<>();
        List<RankingDTO> list = workOrderService.ranking(status);
        allArr.add(list);
        return Result.ok(list);
    }
}
