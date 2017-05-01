package com.xiaojing.distributed.util;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class DataBaseUtil {

  public static final Map<String, DataSource> dataSourceMap = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(DataBaseUtil.class);

  //读配置获取datasource
  public static void init() {
    dataSourceMap.put("0", initDataSource("1.properties"));
    dataSourceMap.put("1", initDataSource("2.properties"));
  }

  private static DataSource initDataSource(String fileName) {

    Properties properties = new Properties();
    DataSource dataSource = null;
    try {
      properties.load(DataBaseUtil.class.getClassLoader().getResourceAsStream(fileName));
      dataSource = BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {

      e.printStackTrace();
      LOGGER.warn("init datasource fail,filename={}", fileName);
    }
    return dataSource;

  }

}
