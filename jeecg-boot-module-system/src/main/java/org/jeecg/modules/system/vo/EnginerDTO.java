package org.jeecg.modules.system.vo;

import lombok.Data;

@Data
public class EnginerDTO {

    /**工程师id*/
    private String id;
    /**工程师工号*/
    private String userName;
    /**名称*/
    private String realname;
    /**经度*/
    private Double longitude;
    /**纬度*/
    private Double latitude;
}
