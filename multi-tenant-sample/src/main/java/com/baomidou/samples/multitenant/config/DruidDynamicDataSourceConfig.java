package com.baomidou.samples.multitenant.config;

import com.baomidou.dynamic.datasource.processor.DsHeaderProcessor;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.processor.DsSessionProcessor;
import com.baomidou.dynamic.datasource.processor.DsSpelExpressionProcessor;
import com.baomidou.dynamic.datasource.provider.AbstractJdbcDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidConfig;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidWallConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis-plus获取主数据源的配置
 * @author Vic
 * @date 2020-11-02 17:27
 */
@Configuration
public class DruidDynamicDataSourceConfig {

  /**
   * 主库url
   */
  @Value("${master.url}")
  private String url;

  /**
   * 主库username
   */
  @Value("${master.username}")
  private String username;

  /**
   * 主库password
   */
  @Value("${master.password}")
  private String password;


  //主库key值
  private final String MASTER = "master";


  /**
   * 自定义数据源从主库获取
   * @return
   */
  @Bean
  public DynamicDataSourceProvider dynamicDataSourceProvider() {
    return new AbstractJdbcDataSourceProvider(null, url, username, password) {
      @Override
      protected Map<String, DataSourceProperty> executeStmt(Statement statement)
          throws SQLException {
        //主库租户表core_tenant
        ResultSet rs = statement.executeQuery("select * from core_tenant");
        Map<String, DataSourceProperty> map = new HashMap<>();
        DruidConfig config = new DruidConfig();
        config.setWall(druidWallConfig());
        while (rs.next()) {
          //防止数据库中出现空数据
          if(null==rs.getString("tenant_name"))
          {
            continue;
          }
          String name = rs.getString("tenant_name");
          String username = rs.getString("user_name");
          String password = rs.getString("password");
          String url = rs.getString("url");
          String driver = rs.getString("driver_name");
          DataSourceProperty property = new DataSourceProperty();
          property.setUsername(username);
          property.setPassword(password);
          property.setUrl(url);
          property.setDriverClassName(driver);
          property.setDruid(config);
          map.put(name, property);
        }
        //另注入master主库配置
        DataSourceProperty masterProperty = new DataSourceProperty();
        masterProperty.setUsername(username);
        masterProperty.setPassword(password);
        masterProperty.setUrl(url);
        masterProperty.setDruid(config);
        map.put(MASTER,masterProperty);
        return map;
      }
    };
  }

  public DruidWallConfig  druidWallConfig()
  {
    DruidWallConfig wallConfig = new DruidWallConfig();
    wallConfig.setAlterTableAllow(true);
    wallConfig.setMultiStatementAllow(true);

    return wallConfig;
  }

  /**
   * 需要自定义数据源处理器的时候可以注入
   * @return
   */
/*  @Bean
  @ConditionalOnMissingBean*/
  public DsProcessor dsProcessor() {
    DsHeaderProcessor headerProcessor = new DsHeaderProcessor();
    DsSessionProcessor sessionProcessor = new DsSessionProcessor();
    DsSpelExpressionProcessor spelExpressionProcessor = new DsSpelExpressionProcessor();
    DomainDataSourceProcessor domainDataSourceProcessor = new DomainDataSourceProcessor();
    headerProcessor.setNextProcessor(sessionProcessor);
    sessionProcessor.setNextProcessor(spelExpressionProcessor);
    spelExpressionProcessor.setNextProcessor(domainDataSourceProcessor);
    return headerProcessor;
  }

}
