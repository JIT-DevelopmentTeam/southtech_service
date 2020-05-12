package org.jeecg.modules.management.workorder.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiWorkrecordAddRequest;
import com.dingtalk.api.response.OapiWorkrecordAddResponse;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.dingtalk.constant.DingTalkConstant;
import org.jeecg.modules.management.client.entity.Client;
import org.jeecg.modules.management.client.entity.DeviceNumber;
import org.jeecg.modules.management.client.service.IClientService;
import org.jeecg.modules.management.client.service.IDeviceNumberService;
import org.jeecg.modules.management.stage.entity.Stage;
import org.jeecg.modules.management.stage.service.IStageService;
import org.jeecg.modules.management.workorder.entity.*;
import org.jeecg.modules.management.workorder.service.*;
import org.jeecg.modules.management.workorder.vo.WorkOrderDTO;
import org.jeecg.modules.management.workorder.vo.WorkOrderPage;
import org.jeecg.modules.message.entity.SysMessageTemplate;
import org.jeecg.modules.message.service.ISysMessageTemplateService;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.service.*;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * @Description: 工单信息
 * @Author: jeecg-boot
 * @Date:   2020-03-13
 * @Version: V1.0
 */
@RestController
@RequestMapping("/workorder/workOrder")
@Slf4j
public class WorkOrderController extends JeecgController<WorkOrder, IWorkOrderService> {

	@Autowired
	private IWorkOrderService workOrderService;

	@Autowired
    private IWorkOrderPageService workOrderPageService;

	@Autowired
	private IWorkOrderDetailService workOrderDetailService;

     @Autowired
     private IWorkOrderProgressService workOrderProgressService;

    @Autowired
    private IServiceCommenteryService serviceCommenteryService;

	@Autowired
    private IClientService clientService;

     @Autowired
     private ISysUserService sysUserService;

     @Autowired
     private ISysMessageTemplateService sysMessageTemplateService;

    @Autowired
    private ISysAnnouncementService sysAnnouncementService;

    @Autowired
    private ISysAnnouncementSendService sysAnnouncementSendService;

    @Autowired
    private ISysDictService sysDictService;

    @Autowired
    private IDeviceNumberService deviceNumberService;

    @Autowired
    private IStageService stageService;

     @Autowired
     private RedisUtil redisUtil;


	/*---------------------------------主表处理-begin-------------------------------------*/

	/**
	 * 分页列表查询
	 * @param workOrder
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(WorkOrder workOrder,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<WorkOrder> queryWrapper = QueryGenerator.initQueryWrapper(workOrder, req.getParameterMap());
        queryWrapper.orderByAsc("status");
        queryWrapper.orderByDesc("create_time");
        if (StringUtils.isNotBlank(req.getParameter("clientName"))) {
            QueryWrapper<Client> clientQueryWrapper = new QueryWrapper<Client>();
            clientQueryWrapper.like("name",req.getParameter("clientName").trim());
            List<Client> clientList = clientService.list(clientQueryWrapper);
            List<String> clientIdsList = new ArrayList<>();
            for (Client client : clientList) {
                clientIdsList.add(client.getId());
            }
            if (!clientIdsList.isEmpty()) {
                queryWrapper.in("client_id",clientIdsList);
            } else {
                queryWrapper.eq("client_id","0");
            }
        }
        if (StringUtils.isNotBlank(req.getParameter("customerServiceRealName"))) {
            QueryWrapper<SysUser> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.like("realname", req.getParameter("customerServiceRealName").trim());
            List<SysUser> userList = sysUserService.list(userQueryWrapper);
            List<String> userNamesList = new ArrayList<>();
            for (SysUser user : userList) {
                userNamesList.add(user.getUsername());
            }
            if (!userNamesList.isEmpty()) {
                queryWrapper.in("customer_service_name", userNamesList);
            } else {
                queryWrapper.eq("customer_service_name", "0");
            }
        }
        Page<WorkOrder> page = new Page<WorkOrder>(pageNo, pageSize);
		IPage<WorkOrder> pageList = workOrderService.page(page, queryWrapper);
		return Result.ok(pageList);
	}

    /**
     * 分页列表查询
     * @param workOrderPageDTO
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping(value = "/listPage")
	public Result<?> listPage(WorkOrderPageDTO workOrderPageDTO,
                              @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                              @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                              HttpServletRequest req) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        List<SysUser> serviceEngineerUserList = sysUserService.listByRoleCode("service_engineer");
        boolean isServiceEngineer = false;
        for (SysUser serviceEngineer : serviceEngineerUserList) {
            if (loginUser.getId().equals(serviceEngineer.getId())) {
                isServiceEngineer = true;
                break;
            }
        }
        QueryWrapper<WorkOrderPageDTO> queryWrapper = QueryGenerator.initQueryWrapper(workOrderPageDTO, req.getParameterMap());
        queryWrapper.orderByAsc("status");
        queryWrapper.orderByDesc("create_time");
        if (isServiceEngineer) {
            queryWrapper.eq("need_dispatch","1");
        }
        if (StringUtils.isNotBlank(req.getParameter("clientName"))) {
            QueryWrapper<Client> clientQueryWrapper = new QueryWrapper<Client>();
            clientQueryWrapper.like("name",req.getParameter("clientName").trim());
            List<Client> clientList = clientService.list(clientQueryWrapper);
            List<String> clientIdsList = new ArrayList<>();
            for (Client client : clientList) {
                clientIdsList.add(client.getId());
            }
            if (!clientIdsList.isEmpty()) {
                queryWrapper.in("client_id",clientIdsList);
            } else {
                queryWrapper.eq("client_id","0");
            }
        }
        if (StringUtils.isNotBlank(req.getParameter("customerServiceRealName"))) {
            QueryWrapper<SysUser> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.like("realname", req.getParameter("customerServiceRealName").trim());
            List<SysUser> userList = sysUserService.list(userQueryWrapper);
            List<String> userNamesList = new ArrayList<>();
            for (SysUser user : userList) {
                userNamesList.add(user.getUsername());
            }
            if (!userNamesList.isEmpty()) {
                queryWrapper.in("customer_service_name", userNamesList);
            } else {
                queryWrapper.eq("customer_service_name", "0");
            }
        }
        Page<WorkOrderPageDTO> page = new Page<>(pageNo, pageSize);
        IPage<WorkOrderPageDTO> pageList = workOrderPageService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

	/**
     *   添加
     * @param workOrderPage
     * @return
     */
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody WorkOrderPage workOrderPage) {
        WorkOrder workOrder = new WorkOrder();
        BeanUtils.copyProperties(workOrderPage, workOrder);
        workOrderService.saveMain(workOrder, workOrderPage.getWorkOrderDetailList());
        List<SysMessageTemplate> messageTemplateList = sysMessageTemplateService.selectByCode("sys_new_ticket_reminder");
        if (!messageTemplateList.isEmpty()) {
            List<SysUser> userList = sysUserService.listByRoleCode("customer_service");
            if (!userList.isEmpty()) {
                StringBuffer userIdsStr = new StringBuffer();
                for (SysUser user : userList) {
                    userIdsStr.append(user.getId()+",");
                }
                userIdsStr = userIdsStr.deleteCharAt(userIdsStr.length()-1);
                SysAnnouncement announcement = new SysAnnouncement();
                announcement.setTitile(messageTemplateList.get(0).getTemplateName());
                announcement.setMsgContent(messageTemplateList.get(0).getTemplateContent());
                announcement.setStartTime(new Date());
                announcement.setSender("admin");
                announcement.setPriority("H");
                announcement.setMsgCategory("2");
                announcement.setMsgType("USER");
                announcement.setSendStatus("1");
                announcement.setSendTime(new Date());
                announcement.setUserIds(userIdsStr.toString());
                announcement.setDelFlag("0");
                sysAnnouncementService.saveAnnouncement(announcement);
                for (SysUser user : userList) {
                    SysAnnouncementSend announcementSend = new SysAnnouncementSend();
                    announcementSend.setAnntId(announcement.getId());
                    announcementSend.setUserId(user.getId());
                    announcementSend.setReadFlag("0");
                    sysAnnouncementSendService.save(announcementSend);
                }
            }
        }
        return Result.ok("添加成功！");
    }

    /**
     *  编辑
     * @param workOrderPage
     * @return
     */
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody WorkOrderPage workOrderPage) {
        WorkOrder workOrder = new WorkOrder();
        BeanUtils.copyProperties(workOrderPage, workOrder);
        workOrderService.updateMain(workOrder, workOrderPage.getWorkOrderDetailList());
        return Result.ok("编辑成功!");
    }

    /**
     * 通过id删除
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name="id",required=true) String id) {
        workOrderService.delMain(id);
        return Result.ok("删除成功!");
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.workOrderService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功!");
    }

    /**
     * 导出
     * @return
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WorkOrder workOrder) {
        return super.exportXls(request, workOrder, WorkOrder.class, "工单信息");
    }

    /**
     * 导入
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WorkOrder.class);
    }

    /**
     * 批量设置状态
     * @param ids
     * @param status
     * @return
     */
    @PostMapping(value = "/setStatusBatch")
    public Result<?> setStatusBatch(@RequestParam("ids") String ids,@RequestParam("status") String status) {
        String[] idsArray = ids.split(",");
        StringBuffer messge = new StringBuffer();
        messge.append("操作成功!");
        StringBuffer failWorkOrder = new StringBuffer();
        for (String id : idsArray) {
            WorkOrder workOrder = workOrderService.getById(id);
            if (StringUtils.equals("3",workOrder.getStatus()) || StringUtils.equals("4",workOrder.getStatus()) || StringUtils.equals("8",workOrder.getStatus()) || StringUtils.equals("9",workOrder.getStatus())) {
                failWorkOrder.append("'"+workOrder.getNumber()+"',");
                continue;
            }
            workOrder.setStatus(status);
            workOrderService.updateById(workOrder);
        }
        if (failWorkOrder.length() > 0) {
            failWorkOrder = failWorkOrder.deleteCharAt(failWorkOrder.length()-1);
            messge.append("("+failWorkOrder+"工单已完成或已关闭无法设置!)");
        }
        return Result.ok(messge.toString());
    }

	/*---------------------------------主表处理-end-------------------------------------*/
	

    /*--------------------------------子表处理-工单明细-begin----------------------------------------------*/
	/**
	 * 查询子表信息 会传入主表ID
	 * @return
	 */
	@GetMapping(value = "/listWorkOrderDetailByMainId")
    public Result<?> listWorkOrderDetailByMainId(WorkOrderDetail workOrderDetail,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
        QueryWrapper<WorkOrderDetail> queryWrapper = QueryGenerator.initQueryWrapper(workOrderDetail, req.getParameterMap());
        Page<WorkOrderDetail> page = new Page<WorkOrderDetail>(pageNo, pageSize);
        IPage<WorkOrderDetail> pageList = workOrderDetailService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

     /**
      * 通过id查询
      *
      * @param id
      * @return
      */
     @GetMapping(value = "/queryWorkOrderDetailListByMainId")
     public Result<?> queryOrderCustomerListByMainId(@RequestParam(name = "id", required = true) String id) {
         List<WorkOrderDetail> workOrderDetailList = workOrderDetailService.selectByMainId(id);
         return Result.ok(workOrderDetailList);
     }

	/**
	 * 添加
	 * @param workOrderDetail
	 * @return
	 */
	@PostMapping(value = "/addWorkOrderDetail")
	public Result<?> addWorkOrderDetail(@RequestBody WorkOrderDetail workOrderDetail) {
		workOrderDetailService.save(workOrderDetail);
		return Result.ok("添加成功！");
	}

    /**
	 * 编辑
	 * @param workOrderDetail
	 * @return
	 */
	@PutMapping(value = "/editWorkOrderDetail")
	public Result<?> editWorkOrderDetail(@RequestBody WorkOrderDetail workOrderDetail) {
		workOrderDetailService.updateById(workOrderDetail);
		return Result.ok("编辑成功!");
	}

	/**
	 * 通过id删除
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/deleteWorkOrderDetail")
	public Result<?> deleteWorkOrderDetail(@RequestParam(name="id",required=true) String id) {
		workOrderDetailService.removeById(id);
		return Result.ok("删除成功!");
	}

	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@DeleteMapping(value = "/deleteBatchWorkOrderDetail")
	public Result<?> deleteBatchWorkOrderDetail(@RequestParam(name="ids",required=true) String ids) {
		this.workOrderDetailService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.ok("批量删除成功!");
	}

    /**
     * 批量派工
     * @param workOrderDetailIds 工单ids
     * @param serviceEngineerName 服务工程师
     *  @param appointment 预约服务时间
     * @param plannedCompletionTime 计划完成时间
     * @param peers 同行人
     * @return
     */
	@PostMapping(value = "/dispatchWorkOrderDetailByIds")
    public Result<?> dispatchWorkOrderDetailByIds(@RequestParam("workOrderDetailIds") String workOrderDetailIds,
                                                  @RequestParam("serviceEngineerName") String serviceEngineerName,
                                                  @RequestParam("appointment") String appointment,
                                                  @RequestParam("plannedCompletionTime") String plannedCompletionTime,
                                                  String peers) {
        String[] idsArray = workOrderDetailIds.split(",");
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        WorkOrderDetail findParent = workOrderDetailService.getById(idsArray[0]);
        WorkOrder workOrder = workOrderService.getById(findParent.getWorkOrderId());
        SysUser serviceEngineer = sysUserService.getUserByName(serviceEngineerName);
        String domainName = null;
        try {
            domainName = PropertiesLoaderUtils.loadAllProperties(("domainname/domain_name_config.properties")).getProperty("domain_name");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String id : idsArray) {
            WorkOrderDetail workOrderDetail = workOrderDetailService.getById(id);
            workOrderDetail.setServiceEngineerName(serviceEngineerName);
            try {
                workOrderDetail.setAppointment(DateUtils.parseDate(appointment,"yyyy-MM-dd"));
                workOrderDetail.setPlannedCompletionTime(DateUtils.parseDate(plannedCompletionTime,"yyyy-MM-dd"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (StringUtils.isNotBlank(peers)) {
                workOrderDetail.setPeers(peers);
            }
            workOrderDetail.setAssigneeName(loginUser.getUsername());
            workOrderDetail.setAssignedTime(DateUtils.getDate());
            workOrderDetailService.updateById(workOrderDetail);
        }
        List<WorkOrderDetail> workOrderDetailList = workOrderDetailService.selectByMainId(workOrder.getId());
        boolean isFinish = true;
        for (WorkOrderDetail workOrderDetail : workOrderDetailList) {
            if (workOrderDetail.getAssignedTime() == null) {
                isFinish = false;
            }
        }
        if (isFinish) {
            workOrder.setStatus("2");
            QueryWrapper<WorkOrderProgress> workOrderProgressQueryWrapper = new QueryWrapper<>();
            workOrderProgressQueryWrapper.eq("work_order_id",workOrder.getId());
            List<WorkOrderProgress> workOrderProgressesList = workOrderProgressService.list(workOrderProgressQueryWrapper);
            for (int i = 0; i < workOrderProgressesList.size(); i++) {
                if (i == 1) {
                    workOrderProgressesList.get(i).setFinishTime(new Date());
                    workOrderProgressService.updateById(workOrderProgressesList.get(i));
                }
            }
            for (WorkOrderDetail workOrderDetail : workOrderDetailList) {
                workOrderDetail.setCurrentProgress(workOrderProgressesList.get(2).getId());
                workOrderDetailService.updateById(workOrderDetail);
            }
            workOrderService.updateById(workOrder);
        }
        // 钉钉发起待办
        Client client = clientService.getById(workOrder.getClientId());
        List<SysMessageTemplate> messageTemplateList = sysMessageTemplateService.selectByCode("dingTalk_dispatch_remind");
        if (!messageTemplateList.isEmpty()) {
            DingTalkClient dingTalkClient = new DefaultDingTalkClient(DingTalkConstant.ADD_WORK_RECORD_URL);
            OapiWorkrecordAddRequest req = new OapiWorkrecordAddRequest();
            req.setUserid(serviceEngineer.getEnterpriseId());
            req.setCreateTime(System.currentTimeMillis());
            req.setTitle("待办");
            req.setUrl(domainName);
            List<OapiWorkrecordAddRequest.FormItemVo> list2 = new ArrayList<>();
            OapiWorkrecordAddRequest.FormItemVo obj3 = new OapiWorkrecordAddRequest.FormItemVo();
            list2.add(obj3);
            obj3.setTitle(messageTemplateList.get(0).getTemplateName());
            obj3.setContent(messageTemplateList.get(0).getTemplateContent().replace("${clientServiceName}",loginUser.getRealname()).replace("${clientName}",client.getName()).replace("${sendingTime}",DateUtils.formatDate(new Date(),"yyyy-MM-dd HH:mm:ss")));
            req.setFormItemList(list2);
            try {
                OapiWorkrecordAddResponse rsp = dingTalkClient.execute(req, redisUtil.get(DingTalkConstant.ACCESS_TOKEN_KEY).toString());
                if (rsp.isSuccess()) {
                    log.info("钉钉派单待办提醒发送成功!");
                } else {
                    log.info("钉钉派单待办提醒发送失败,错误码:"+rsp.getCode()+",错误信息:"+rsp.getErrmsg());
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
	    return Result.ok("派工成功!");
    }

    /**
     * 导出报表
     * @return
     */
    @RequestMapping(value = "/exportReport")
    public ModelAndView exportReport() {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String title = "维修记录报表";
        List<ServiceReport> exportList = new ArrayList<>();
        List<WorkOrderDetail> workOrderDetailList = workOrderDetailService.list();
        // 故障部位
        List<DictModel> faultLocationDictItemList = sysDictService.queryDictItemsByCode("work_order_detail_fault_location");
        // 工单状态
        List<DictModel> stageDictItemList = sysDictService.queryDictItemsByCode("work_order_status");
        for (int i = 0; i < workOrderDetailList.size(); i++) {
            WorkOrderDetail workOrderDetail = workOrderDetailList.get(i);
            WorkOrder workOrder = workOrderService.getById(workOrderDetail.getWorkOrderId());
            SysUser serviceEngineer = sysUserService.getUserByName(workOrderDetail.getServiceEngineerName());
            Client client = clientService.getById(workOrder.getClientId());
            DeviceNumber deviceNumber = deviceNumberService.getById(workOrderDetail.getDeviceNumber());
            String[] faultLocationValues = workOrderDetail.getFaultLocation().split(",");
            StringBuffer faultLocationText = new StringBuffer();
            for (String faultLocationValue : faultLocationValues) {
                for (DictModel faultLocationDictItem : faultLocationDictItemList) {
                    if (faultLocationValue.equals(faultLocationDictItem.getValue())) {
                        faultLocationText.append(faultLocationDictItem.getText()+",");
                    }
                }
            }
            ServiceReport serviceReport = new ServiceReport();
            serviceReport.setNumber(i+1);
            serviceReport.setProvince(client.getProvince());
            serviceReport.setCity(client.getCity());
            serviceReport.setClientName(client.getName());
            // 炉型 ?
            serviceReport.setFurnace("");
            serviceReport.setFeedbackQuestion("");
            // 问题类型 ?
            serviceReport.setQuestionType("");
            if (faultLocationText.length() > 0) {
                faultLocationText = faultLocationText.deleteCharAt(faultLocationText.length()-1);
                serviceReport.setFaultLocation(faultLocationText.toString());
            }
            if (oConvertUtils.isNotEmpty(deviceNumber.getQgp())) {
                serviceReport.setIsQgp(System.currentTimeMillis() <= deviceNumber.getQgp().getTime() ? "是" : "否");
            }
            // 是否收费 ?
            serviceReport.setIsCharge("");
            serviceReport.setServiceEngineerName(serviceEngineer.getRealname());
            serviceReport.setAppointment(workOrderDetail.getAppointment());
            if ("3".equals(workOrder.getStatus())) {
                QueryWrapper<Stage> stageQueryWrapper = new QueryWrapper<>();
                stageQueryWrapper.eq("work_order_type",workOrder.getType());
                stageQueryWrapper.orderByDesc("order_index");
                stageQueryWrapper.last("LIMIT 0,1");
                Stage stage = stageService.getOne(stageQueryWrapper);
                QueryWrapper<WorkOrderProgress> workOrderProgressQueryWrapper = new QueryWrapper<>();
                workOrderProgressQueryWrapper.eq("work_order_id",workOrder.getId());
                workOrderProgressQueryWrapper.eq("stage_id",stage.getId());
                WorkOrderProgress finishProgress = workOrderProgressService.getOne(workOrderProgressQueryWrapper);
                if (oConvertUtils.isNotEmpty(finishProgress.getFinishTime())) {
                    serviceReport.setCompleteTime(finishProgress.getFinishTime());
                }
            }
            serviceReport.setProcessResult("");
            for (DictModel stageDictItem : stageDictItemList) {
                if (workOrder.getStatus().equals(stageDictItem.getValue())) {
                    serviceReport.setStatus(stageDictItem.getText());
                }
            }
            serviceReport.setReasonForIncomplete("");
            serviceReport.setResultsEvaluation("");
            serviceReport.setStaffEvaluation("");
            exportList.add(serviceReport);
        }

        mv.addObject(NormalExcelConstants.FILE_NAME, title); //此处设置的filename无效 ,前端会重更新设置一下
        mv.addObject(NormalExcelConstants.CLASS, ServiceReport.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams(title, "", title));
        mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        return mv;
    }

    /*--------------------------------子表处理-工单明细-end----------------------------------------------*/

     /*--------------------------------子表处理-工单进度-begin----------------------------------------------*/
     /**
      * 通过id查询
      *
      * @param id
      * @return
      */
     @GetMapping(value = "/queryWorkOrderProgressByMainId")
     public Result<?> queryWorkOrderProgressListByMainId(@RequestParam(name="id",required=true) String id) {
         List<WorkOrderProgress> workOrderProgressList = workOrderProgressService.selectByMainId(id);
         return Result.ok(workOrderProgressList);
     }

     /*--------------------------------子表处理-工单进度-end----------------------------------------------*/


    @GetMapping(value = "/queryWorkOrderList")
    public Result<Page<WorkOrderDTO>> queryWorkOrderList(WorkOrderDTO workOrderDTO, @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                        @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                                        HttpServletRequest req) {
        Result<Page<WorkOrderDTO>> result = new Result<>();
        Page<WorkOrderDTO> pageList = new Page<>(pageNo, pageSize);
        pageList = workOrderService.queryListByType(pageList, req.getParameter("type"));
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    /*--------------------------------子表处理-服务评价-begin----------------------------------------------*/
    /**
     * 查询子表信息 会传入主表ID
     * @return
     */
    @GetMapping(value = "/listServiceCommenteryByMainId")
    public Result<?> listServiceCommenteryByMainId(ServiceCommentery serviceCommentery,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest req) {
        QueryWrapper<ServiceCommentery> queryWrapper = QueryGenerator.initQueryWrapper(serviceCommentery, req.getParameterMap());
        Page<ServiceCommentery> page = new Page<ServiceCommentery>(pageNo, pageSize);
        IPage<ServiceCommentery> pageList = serviceCommenteryService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    /**
     * 添加
     * @param serviceCommentery
     * @return
     */
    @PostMapping(value = "/addServiceCommentery")
    public Result<?> addServiceCommentery(@RequestBody ServiceCommentery serviceCommentery) {
        serviceCommenteryService.save(serviceCommentery);
        return Result.ok("添加成功！");
    }

    /**
     * 编辑
     * @param serviceCommentery
     * @return
     */
    @PutMapping(value = "/editServiceCommentery")
    public Result<?> editServiceCommentery(@RequestBody ServiceCommentery serviceCommentery) {
        serviceCommenteryService.updateById(serviceCommentery);
        return Result.ok("编辑成功!");
    }

    /**
     * 通过id删除
     * @param id
     * @return
     */
    @DeleteMapping(value = "/deleteServiceCommentery")
    public Result<?> deleteServiceCommentery(@RequestParam(name="id",required=true) String id) {
        serviceCommenteryService.removeById(id);
        return Result.ok("删除成功!");
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping(value = "/deleteBatchServiceCommentery")
    public Result<?> deleteBatchServiceCommentery(@RequestParam(name="ids",required=true) String ids) {
        this.serviceCommenteryService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功!");
    }

    /*--------------------------------子表处理-服务评价-end----------------------------------------------*/

}
