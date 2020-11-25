package com.baomidou.samples.multitenant.util;

import com.baomidou.samples.multitenant.utils.cmdUtils.SpringContextUtils;
import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import org.apache.ibatis.io.Resources;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Vic
 * @date 2020-11-24 17:49
 */
public class ScriptExecute {

  public void create (String schemaName) throws SQLException,IOException {

    DataSource source = SpringContextUtils.getBean(DataSource.class);
    Connection pool = source.getConnection();
    if (null!=pool)
    {
      //执行脚本新建schema
      Statement newSmt = pool.createStatement();
      newSmt.executeUpdate("CREATE  DATABASE IF NOT EXISTS "+schemaName);
      //设置字符集,不然中文乱码插入错误
      Resources.setCharset(Charset.forName("UTF-8"));
      // DDL语句返回值为0;创建数据库
      //打开 转向数据库
      newSmt.execute("USE "+schemaName+";");
      //执行脚本
      newSmt.execute(getSQLSript());
      newSmt.close();
    }
  }

  private String getSQLSript()throws IOException{
    ClassPathResource classPathResource = new ClassPathResource("db/schema.sql");
    InputStream inputStream =classPathResource.getInputStream();
    StringBuilder buffer = new StringBuilder();
    String line;
    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
    while ((line = br.readLine()) != null) {
      buffer.append(line);
    }
    return buffer.toString();
  }

}
