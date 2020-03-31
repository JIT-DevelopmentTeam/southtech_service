package org.jeecg.modules.management.mobile.stage;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.management.progressreport.entity.ProgressReport;
import org.jeecg.modules.management.progressreport.service.IProgressReportService;
import org.jeecg.modules.management.progressreport.vo.MobileReportDTO;
import org.jeecg.modules.management.signin.entity.SignIn;
import org.jeecg.modules.management.signin.service.ISignInService;
import org.jeecg.modules.management.stage.service.IStageService;
import org.jeecg.modules.management.stage.vo.MobileStageDTO;
import org.jeecg.modules.management.workorder.entity.WorkOrderProgress;
import org.jeecg.modules.management.workorder.service.IWorkOrderDetailService;
import org.jeecg.modules.management.workorder.service.IWorkOrderProgressService;
import org.jeecg.modules.management.workorder.vo.MobileWorkOrderDetailDTO;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/mobile/stage")
public class MobileStageController {

    @Autowired
    private IStageService stageService;

    @Autowired
    private ISignInService signInService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private IProgressReportService progressReportService;

    @Autowired
    private IWorkOrderDetailService workOrderDetailService;

    @Autowired
    private IWorkOrderProgressService workOrderProgressService;

    @RequestMapping(value = "/queryStageByWorkOrderId")
    public Result<?> queryStageByWorkOrderId(HttpServletRequest req) {
        List<MobileStageDTO> list = stageService.queryStageByWorkOrderId(req.getParameter("workOrderId"));
        return Result.ok(list);
    }

    @RequestMapping(value = "/getByReportId", method = RequestMethod.GET)
    public Result<?> getByReportId(HttpServletRequest req) {
        SysUser user = sysUserService.getByEnterpriseId(req.getParameter("userId"));
        String progressId = req.getParameter("progressId");
        String reportId = req.getParameter("reportId");
        MobileReportDTO reportDTO = progressReportService.getByReportId(user.getUsername(), progressId, reportId);
        return Result.ok(reportDTO);
    }

    @RequestMapping(value = "/progressReport", method = RequestMethod.POST)
    public Result<?> progressReport(@RequestParam Map<String, Object> params) {
        Result<String> result = new Result<>();
        SysUser user = sysUserService.getByEnterpriseId(params.get("userId").toString());
        // 进度汇报保存
        String progressReportId = reportSave(params);
        // 签到 签出保存
        signInSave("1", params, user, signInService, progressReportId);
        signInSave("2", params, user, signInService, progressReportId);
        // 检查当前工单下面的所以工单明细是否已经完成
        checkAllCompleted(params.get("ticketId").toString(), params.get("progressId").toString());
        result.setSuccess(true);
        result.setResult(progressReportId);
        result.setMessage("保存成功");
        return result;
    }

    /**
     * 签到表（保存操作）
     * @param type
     * @param params
     * @param user
     * @param signInService
     */
    private void signInSave(String type, Map<String, Object> params, SysUser user, ISignInService signInService, String reportId) {
        SignIn signIn = new SignIn();
        signIn.setProgressId(params.get("progressId").toString());
        signIn.setServiceEngineerName(user.getUsername());
        signIn.setType(type);
        signIn.setReportId(reportId);
        try {
            switch (type) {
                case "1":
                    signIn.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(params.get("checkIn").toString()));
                    signIn.setLongitude(Double.parseDouble(params.get("signInLongitude").toString()));
                    signIn.setLatitude(Double.parseDouble(params.get("signInLatitude").toString()));
                    break;
                case "2":
                    signIn.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(params.get("checkOut").toString()));
                    signIn.setLongitude(Double.parseDouble(params.get("signOutLongitude").toString()));
                    signIn.setLatitude(Double.parseDouble(params.get("signOutLatitude").toString()));
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        signInService.save(signIn);
    }

    /**
     * 进度汇报（保存操作）
     * @param params
     */
    private String reportSave(Map<String, Object> params) {
        ProgressReport progressReport = new ProgressReport();
        progressReport.setDescription(params.get("faultJudgement").toString());
        progressReport.setProgressId(params.get("progressId").toString());
        progressReport.setIsCompleted(params.get("completeStatus").toString());
        progressReport.setWorkOrderDetailId(params.get("detailId").toString());
        progressReport.setWorkOrderId(params.get("ticketId").toString());
        progressReport.setFaultLocation(params.get("faultLocation").toString());
        if (!"null".equals(params.get("reportId").toString())) {
            progressReport.setId(params.get("reportId").toString());
            progressReportService.updateById(progressReport);
        } else {
            progressReportService.save(progressReport);
        }
        return progressReport.getId();
    }

    public void checkAllCompleted(String workOrderId, String progressId) {
        List<MobileWorkOrderDetailDTO> list = workOrderDetailService.getByWorkOrderId(workOrderId);
        int allCompleted = 0;// 统计工单明细完成的个数
        for (MobileWorkOrderDetailDTO dto : list) {// 循环判断工单明细是否已完成
            if ("1".equals(dto.getIsCompleted())) {
                allCompleted += 1;
            }
        }
        if (allCompleted == list.size()) {// 当前进度的工单明细已全部完成
            workOrderProgressService.updateFinishTimeById(new Date(), progressId);// 工单进度表写入完成时间
            List<WorkOrderProgress> progressList = workOrderProgressService.selectByMainId(workOrderId);
            String currentProgressId = "";
            for (int i = 0; i < progressList.size(); i++) {
                if (progressList.get(i).getFinishTime() == null) {
                    currentProgressId = progressList.get(i).getId();
                    break;
                }
            }
            if (!"".equals(currentProgressId)) {
                workOrderDetailService.updateCurrentProgressByWorkOrderId(currentProgressId, workOrderId);
            }
        }
    }

}
