package com.baomidou.samples.multitenant.config;

import com.baomidou.dynamic.datasource.processor.DsProcessor;
import javax.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 继承dynamic-datasource的Processor,采用域名区分租户
 * 获取子域名决定租户
 * @author Vic
 * @date 2020-11-24 16:38
 */
public class DomainDataSourceProcessor extends DsProcessor{

  private static final String DOMAIN_PREFIX = "#domain.tenantName";

  //主库key值
  private final String MASTER = "master";

  @Override
  public boolean matches(String s) {
    //以域名开头的使用该解析器
    return s.startsWith(DOMAIN_PREFIX);
  }


  //旧有域名方式废弃,改用header以及参数的ds注解
  @Override
  public String doDetermineDatasource(MethodInvocation methodInvocation, String s) {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    //获取子域名
    String url = request.getServerName();
    //处理一级域名指向master数据源的问题
    //只有一个 "." 或者www开头说明是父域名访问,设置数据源为master
    String tenantName = url.substring(0,url.indexOf("."));
    if(url.split(".").length==2||"www".equals(tenantName))
    {
        //返回主数据源,其实不设置时找不到匹配值也会返回default dataSource -->master
        return MASTER;
    }
    return tenantName;
  }



}
