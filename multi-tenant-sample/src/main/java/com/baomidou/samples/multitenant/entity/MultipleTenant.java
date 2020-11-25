package com.baomidou.samples.multitenant.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 多租户模式租户对象
 * @author Vic
 * @date 2020-11-24 16:57
 */
@Data
@ApiModel("租户信息")
public class MultipleTenant implements Serializable {



    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("公司名称")
    private String groupName;

    @ApiModelProperty(value = "系统生成的租户ID",notes = "值唯一")
    private String tenantName;

    @ApiModelProperty("公司识别代码")
    private String identityInformation;

    @ApiModelProperty("数据库用户名")
    @NotNull
    private String userName;

    @ApiModelProperty("数据库地址")
    private String url;

    @ApiModelProperty("数据库密码")
    private String password;

    @ApiModelProperty("数据库驱动名称")
    private String driverName;

    /**
     * 新增时间
     */
    @ApiModelProperty("新增时间")
    private Date gmtCreate;

    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    private Date gmtModified;
}
