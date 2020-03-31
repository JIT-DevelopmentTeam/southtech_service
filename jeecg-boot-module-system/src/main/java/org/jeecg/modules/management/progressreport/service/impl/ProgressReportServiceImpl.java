package org.jeecg.modules.management.progressreport.service.impl;

import org.jeecg.modules.management.progressreport.entity.ProgressReport;
import org.jeecg.modules.management.progressreport.mapper.ProgressReportMapper;
import org.jeecg.modules.management.progressreport.service.IProgressReportService;
import org.jeecg.modules.management.progressreport.vo.MobileReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 进度汇报
 * @Author: jeecg-boot
 * @Date:   2020-03-24
 * @Version: V1.0
 */
@Service
public class ProgressReportServiceImpl extends ServiceImpl<ProgressReportMapper, ProgressReport> implements IProgressReportService {

    @Autowired
    private ProgressReportMapper progressReportMapper;

    /**
     * 通过汇报id查出历史记录
     * @param userName 用户名
     * @param progressId 进度id
     * @param reportId 汇报id
     * @return
     */
    @Override
    public MobileReportDTO getByReportId(String userName, String progressId, String reportId) {
        return progressReportMapper.getByReportId(userName, progressId, reportId);
    }

}
