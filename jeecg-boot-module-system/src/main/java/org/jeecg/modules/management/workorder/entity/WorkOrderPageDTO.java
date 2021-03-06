package org.jeecg.modules.management.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

@Data
@TableName("view_work_order")
public class WorkOrderPageDTO implements Serializable {

    private java.lang.String id;
    private java.lang.String createrName;
    private java.lang.String createBy;
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private java.util.Date createTime;
    private java.lang.String updateBy;
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private java.util.Date updateTime;
    private java.lang.String sysOrgCode;
    private java.lang.String number;
    private java.lang.String status;
    private java.lang.String type;
    private java.lang.String accessMethod;
    private java.lang.String needDispatch;
    private java.lang.String clientId;
    private java.lang.String contactId;
    private java.lang.String mobilePhone;
    private java.lang.String emergencyLevel;
    private java.lang.String customerServiceName;
    private java.util.Date declarationTime;
    private java.lang.String annex;
    private java.lang.String deviceNumber;
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private java.util.Date qgp;
    private java.lang.String serviceEngineerName;
    private java.lang.String faultLocation;
    private java.util.Date assignedTime;
    private java.lang.String peers;
    private java.util.Date appointment;
    private java.util.Date plannedCompletionTime;
    private java.util.Date actualCompletionTime;
    private java.lang.String description;
    private java.lang.String workOrderDetailId;

}
