package com.xiaojing.distributed.dao.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xiaojing on 17/4/29.
 * 分表策略,简单处理,目前采用不分表的策略
 */
public class TableShardingStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(TableShardingStrategy.class);

  public String getTableName(String tableName, String id) {
    return tableName;
  }
}
