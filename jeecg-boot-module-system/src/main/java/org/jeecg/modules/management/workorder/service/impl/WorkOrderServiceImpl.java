package org.jeecg.modules.management.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.management.stage.entity.Stage;
import org.jeecg.modules.management.stage.mapper.StageMapper;
import org.jeecg.modules.management.workorder.entity.WorkOrder;
import org.jeecg.modules.management.workorder.entity.WorkOrderDetail;
import org.jeecg.modules.management.workorder.entity.WorkOrderProgress;
import org.jeecg.modules.management.workorder.mapper.WorkOrderDetailMapper;
import org.jeecg.modules.management.workorder.mapper.WorkOrderMapper;
import org.jeecg.modules.management.workorder.mapper.WorkOrderProgressMapper;
import org.jeecg.modules.management.workorder.service.IWorkOrderService;
import org.jeecg.modules.management.workorder.vo.MobileWorkOrderDTO;
import org.jeecg.modules.management.workorder.vo.WorkOrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 工单信息
 * @Author: jeecg-boot
 * @Date:   2020-03-13
 * @Version: V1.0
 */
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements IWorkOrderService {

	@Autowired
	private WorkOrderMapper workOrderMapper;
	@Autowired
	private WorkOrderDetailMapper workOrderDetailMapper;
    @Autowired
    private WorkOrderProgressMapper workOrderProgressMapper;
    @Autowired
    private StageMapper stageMapper;

	@Override
	@Transactional
	public void delMain(String id) {
		workOrderDetailMapper.deleteByMainId(id);
		workOrderProgressMapper.deleteByMainId(id);
		workOrderMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			workOrderDetailMapper.deleteByMainId(id.toString());
            workOrderProgressMapper.deleteByMainId(id.toString());
			workOrderMapper.deleteById(id);
		}
	}

    @Override
    @Transactional
    public void saveMain(WorkOrder workOrder, List<WorkOrderDetail> workOrderDetailList) {
	    workOrder.setStatus("1");
        workOrderMapper.insert(workOrder);
        QueryWrapper<Stage> stageQueryWrapper = new QueryWrapper<>();
        stageQueryWrapper.eq("work_order_type",workOrder.getType());
        stageQueryWrapper.orderByAsc("order_index");
        List<Stage> stageList = stageMapper.selectList(stageQueryWrapper);
        WorkOrderProgress currentProgress = null;
        for (int i = 0; i < stageList.size(); i++) {
            WorkOrderProgress workOrderProgress = new WorkOrderProgress();
            if (i == 0) {
                workOrderProgress.setFinishTime(DateUtils.getDate());
            }
            workOrderProgress.setName(stageList.get(i).getStageName());
            workOrderProgress.setStageId(stageList.get(i).getId());
            workOrderProgress.setWorkOrderId(workOrder.getId());
            workOrderProgressMapper.insert(workOrderProgress);
            if (i == 1) {
                currentProgress = workOrderProgress;
            }
        }
        if (workOrderDetailList != null) {
            for (WorkOrderDetail entity : workOrderDetailList) {
                if (currentProgress != null) {
                    entity.setCurrentProgress(currentProgress.getId());
                }
                entity.setWorkOrderId(workOrder.getId());
                workOrderDetailMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateMain(WorkOrder workOrder, List<WorkOrderDetail> workOrderDetailList) {
        workOrderMapper.updateById(workOrder);
        if (workOrderDetailList != null) {
            QueryWrapper<WorkOrderDetail> workOrderDetailQueryWrapper = new QueryWrapper<>();
            workOrderDetailQueryWrapper.eq("work_order_id",workOrder.getId());
            WorkOrderDetail workOrderDetail = workOrderDetailMapper.selectOne(workOrderDetailQueryWrapper);
            if (oConvertUtils.isNotEmpty(workOrderDetail)) {
                workOrderDetail.setDeviceNumber(workOrderDetailList.get(0).getDeviceNumber());
                workOrderDetail.setFaultLocation(workOrderDetailList.get(0).getFaultLocation());
                workOrderDetail.setDescription(workOrderDetailList.get(0).getDescription());
                workOrderDetailMapper.updateById(workOrderDetail);
            }
        }
    }

    /**
     * 查询出所有待分派的工单（过滤条件：type）
     * @param type
     * @return
     */
    @Override
    public Page<WorkOrderDTO> queryListByType(Page<WorkOrderDTO> page, String type) {
        return page.setRecords(workOrderMapper.queryListByType(page, type));
    }

    /**
     * 钉钉-查询所有工单
     * @param page
     * @return
     */
    @Override
    public Page<MobileWorkOrderDTO> queryMobileList(Page<MobileWorkOrderDTO> page, String userName, String status) {
        return page.setRecords(workOrderMapper.queryMobileList(page, userName, status));
    }

    /**
     * 钉钉-根据工单id查询附件
     * @param workOrderId
     * @return
     */
    @Override
    public WorkOrder getWorkOrderAnnex(String workOrderId) {
        return workOrderMapper.getWorkOrderAnnex(workOrderId);
    }

    /**
     * 服务号-查询所有工单
     * @param page
     * @param status
     * @return
     */
    @Override
    public Page<MobileWorkOrderDTO> queryWXworkOrderList(Page<MobileWorkOrderDTO> page, String openId, String status) {
        return page.setRecords(workOrderMapper.queryWXworkOrderList(page, openId, status));
    }

    @Override
    public List<WorkOrder> queryWorkOrderByComment(String clientId, String status) {
        return workOrderMapper.queryWorkOrderByComment(clientId,status);
    }

    @Override
    public List<WorkOrder> queryTimeoutWorkOrder() {
        return workOrderMapper.queryTimeoutWorkOrder();
    }
}
