package com.xiaojing.distributed.dao.sharding;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xiaojing on 17/4/29.
 *
 * 分库策略,目前都是根据主键取MD5,然后后三位,然后对2取模的方式
 * 也就是说,在这里,只有两个数据库
 */
public class DBShardingStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBShardingStrategy.class);

  public String getDataSourceKey(String id) {

    if (StringUtils.isBlank(id) || StringUtils.equals("null", id)) {
      throw new RuntimeException("no database source from null id");
    }

    String key = DigestUtils.md5Hex(id);
    int subId = Integer.valueOf(StringUtils.substring(key, key.length() - 3), 16);
    key = String.valueOf(subId % 2);

    LOGGER.info("id={},datasource key={}", id, key);
    return key;
  }
}
