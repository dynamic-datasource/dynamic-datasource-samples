package com.baomidou.samples.multitenant.controller.tenantController;

import com.baomidou.samples.multitenant.dto.DataSourceDTO;
import com.baomidou.samples.multitenant.entity.MultipleTenant;
import com.baomidou.samples.multitenant.service.tenant.MultipleTenantService;
import com.baomidou.samples.multitenant.utils.createDB.ScriptExecute;
import com.baomidou.samples.multitenant.utils.webUtils.JsonResult;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceCreator;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 无数据源注解会自动选择master
 * @author Vic
 * @date 2020-11-24 17:00
 */
@RestController
@Api("租户信息管理")
@RequestMapping("/tenant")
@AllArgsConstructor
public class TenantController {

  @Autowired
  private final DataSource dataSource;

  @Autowired
  private final DataSourceCreator dataSourceCreator;

  @Autowired
  private MultipleTenantService service;

  @GetMapping("/list")
  public JsonResult<List<MultipleTenant>> getAll()
  {
    return JsonResult.Success(null,service.list());
  }

  /**
   * 新增租户信息并执行数据库脚本新建schema,动态添加数据源
   * @return
   */
  @PostMapping("/add")
  @Api("新增数据源并创建独立表空间")
  public JsonResult<DataSourceDTO> add(@RequestBody MultipleTenant tenant)
  {

    service.save(tenant);
    //添加或者修改tenantName
    tenant.setTenantName("TENANT_"+tenant.getId());
    //添加成功,新建schema
    ScriptExecute execute = new ScriptExecute();
    try {
      //用tenantName创建schema
      execute.create(tenant.getTenantName());
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //添加数据源
    return  JsonResult.Success(null,this.addDataSource(new DataSourceDTO(tenant)));
  }


  @GetMapping("/getAllDataSources")
  @ApiOperation("获取当前所有数据源")
  public Set<String> now() {
    DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
    return ds.getCurrentDataSources().keySet();
  }


  /**
   * 通用添加数据源（推荐）
   * @param dto
   * @return
   */
  private DataSource addDataSource(DataSourceDTO dto) {
    DataSourceProperty dataSourceProperty = new DataSourceProperty();
    BeanUtils.copyProperties(dto, dataSourceProperty);
    DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
    DataSource dataSource = dataSourceCreator.createDataSource(dataSourceProperty);
    ds.addDataSource(dto.getPollName(), dataSource);
    return ds.getDataSource(dto.getPollName());
  }


}
