package com.baomidou.samples.multitenant.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author Vic
 * @date 2020-11-24 15:26
 */
@Configuration
public class CrossConfig {

  @Bean
  public FilterRegistrationBean crosFilter()
  {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration cros = new CorsConfiguration();
    cros.setAllowCredentials(false);
    cros.addAllowedOrigin("*");
    cros.addAllowedHeader("*");
    cros.addAllowedMethod("*");
    source.registerCorsConfiguration("/**", cros);
    FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
    bean.setOrder(0);
    return bean;
  }

}
