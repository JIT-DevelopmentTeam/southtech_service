package org.jeecg.modules.management.mobile.workOrder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.management.progressreport.service.IProgressReportService;
import org.jeecg.modules.management.workorder.entity.WorkOrder;
import org.jeecg.modules.management.workorder.service.IWorkOrderDetailService;
import org.jeecg.modules.management.workorder.service.IWorkOrderService;
import org.jeecg.modules.management.workorder.vo.MobileWorkOrderDTO;
import org.jeecg.modules.management.workorder.vo.MobileWorkOrderDetailDTO;
import org.jeecg.modules.management.workorder.vo.WorkOrderPage;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/mobile/workOrder")
public class MobileWorkOrderController {

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private IWorkOrderService workOrderService;

    @Autowired
    private IWorkOrderDetailService workOrderDetailService;

    @Autowired
    protected IProgressReportService progressReportService;

    @Autowired
    private ISysBaseAPI sysBaseAPI;


    //-----------------------------------钉钉----------------------------------
    @RequestMapping(value = "/workOrderList", method = RequestMethod.GET)
    public Result<?> workOrderList(@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                                   HttpServletRequest req) {
        Result<Page<MobileWorkOrderDTO>> result = new Result<Page<MobileWorkOrderDTO>>();
        Page<MobileWorkOrderDTO> pageList = new Page<>(pageNo, pageSize);
        String enterpriseId = req.getParameter("userId");
        SysUser user = sysUserService.getByEnterpriseId(enterpriseId);
        if (user == null) {
            result.setSuccess(false);
            result.setMessage("用户不存在！");
            return result;
        }
        pageList = workOrderService.queryMobileList(pageList, user.getUsername(), req.getParameter("status"));
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    @RequestMapping(value = "/getWorkOrderAnnex", method = RequestMethod.GET)
    public Result<?> getWorkOrderAnnex(@RequestParam(name = "ticketId") String ticketId, HttpServletRequest request) {
        Result<List<String>> result = new Result<>();
        List<String> annexList = new ArrayList<>();
        WorkOrder workOrderAnnex = workOrderService.getWorkOrderAnnex(ticketId);
        String annex = workOrderAnnex.getAnnex();
        if (annex != null) {
            String[] annexSplit = annex.split(",");
            for (String annex_ : annexSplit) {
                annexList.add(annex_);
            }
        }
        result.setSuccess(true);
        result.setResult(annexList);
        return result;
    }

    //-----------------------------------服务号----------------------------------
    @GetMapping(value = "/worderOrderListByComment")
    public Result<?> worderOrderListByComment(@RequestParam(value = "clientId") String clientId) {
        return Result.ok(workOrderService.queryWorkOrderByComment(clientId,"3"));
    }

    @RequestMapping(value = "/getAllDetail", method = RequestMethod.GET)
    public Result<?> getAllDetail(HttpServletRequest req) {
        List<MobileWorkOrderDetailDTO> detailList = workOrderDetailService.getByWorkOrderId(req.getParameter("workOrderId"));
        return Result.ok(detailList);
    }

    @RequestMapping(value = "/getAllReport", method = RequestMethod.GET)
    public Result<?> getAllReport(HttpServletRequest req) {
        List<MobileWorkOrderDetailDTO> reportList = progressReportService.getByWorkOrderId(req.getParameter("workOrderId"));
        return Result.ok(reportList);
    }

    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody WorkOrderPage workOrderPage) {
        WorkOrder workOrder = new WorkOrder();
        BeanUtils.copyProperties(workOrderPage, workOrder);
        SysUser sysUser = sysUserService.getByEnterpriseId(workOrderPage.getCreateBy());
        workOrder.setCreateBy(sysUser.getUsername());
        workOrderService.saveMain(workOrder, workOrderPage.getWorkOrderDetailList());
        List<SysUser> userList = sysUserService.listByRoleCode("customer_service");
        Map<String, String> map = new HashMap<>();
        for (SysUser user : userList) {
            sysBaseAPI.sendSysAnnouncement("admin", user.getUsername(), "新工单提醒", map, "sys_new_ticket_reminder");
        }
        return Result.ok("添加成功！");
    }

    //--------------------------------------服务号----------------------------------------
    @RequestMapping(value = "/WXworkOrderList", method = RequestMethod.GET)
    public Result<?> WXworkOrderList(@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                     @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                                     HttpServletRequest req) {
        Result<Page<MobileWorkOrderDTO>> result = new Result<Page<MobileWorkOrderDTO>>();
        Page<MobileWorkOrderDTO> pageList = new Page<>(pageNo, pageSize);
        String status = req.getParameter("status");
        switch (status) {
            case "0":
                status = "('1','2','7')";
                break;
            case "1":
                status = "('3','4')";
                break;
            case "2":
                status = "";
                break;
        }
        pageList = workOrderService.queryWXworkOrderList(pageList, req.getParameter("openId"), status);
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

}
