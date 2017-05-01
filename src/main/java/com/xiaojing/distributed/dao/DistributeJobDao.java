package com.xiaojing.distributed.dao;

import com.google.inject.Inject;

import com.xiaojing.distributed.model.DistributeJob;
import com.xiaojing.distributed.util.DataBaseUtil;
import com.xiaojing.distributed.dao.sharding.DBShardingStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;


/**
 * Created by xiaojing on 15/10/14.
 */
public class DistributeJobDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(DistributeJobDao.class);

  @Inject
  private DbUtil dbUtil;

  @Inject
  private DBShardingStrategy dbShardingStrategy;

  public int insertDistributeJob(Connection connection, DistributeJob distributeJob)
      throws SQLException {
    String sql = "insert into " + getTableName(distributeJob.getId()) +
                 " (id, state, last_modify_timestamp) values(:id,:state,:lastModifyTimestamp)";
    Map<String, Object> params = new HashMap<>();
    params.put("id", distributeJob.getId());
    params.put("state", distributeJob.getState());
    params.put("lastModifyTimestamp", System.currentTimeMillis());
    return dbUtil.insert(connection, sql, params);
  }

  public DistributeJob selectForUpdateById(Connection connection, String distributeJobId)
      throws SQLException {
    String sql = "select * from " + getTableName(distributeJobId) + " where id=:id for update";
    Map<String, Object> params = new HashMap<>();
    params.put("id", distributeJobId);
    return dbUtil.queryOne(connection, sql, DistributeJob.class, params);
  }

  // TODO 这个没法分表分库了,没有idx，这里使用的方法为扫库和扫表
  public List<DistributeJob> selectSuspended(long lastModifyTimestamp) throws SQLException {
    Map<String, DataSource> map = DataBaseUtil.dataSourceMap;
    Set<String> keySet = map.keySet();
    Set<DataSource> dataSourceSet = new HashSet();
    for (String key : keySet) {
      dataSourceSet.add(map.get(key));
    }
    Connection connection = null;
    String sql;
    Map<String, Object> params = new HashMap<>();
    List<DistributeJob> list = new LinkedList<>();
    for (DataSource dataSource : dataSourceSet) {
      try {
        connection = dataSource.getConnection();
        sql = "select * from distribute_job where state!='commited' and"
              + " state!='rollbacked' and last_modify_timestamp < :lastModifyTimestamp";
        params.put("lastModifyTimestamp", lastModifyTimestamp);
        List<DistributeJob>
            distributeJobs =
            dbUtil.queryList(connection, sql, DistributeJob.class, params);
        list.addAll(distributeJobs);
      } finally {
        if (connection != null) {
          connection.close();
        }
      }
    }
    return list;
  }

  public int updateDistributeJobStateById(Connection connection, String distributeJobId,
                                          String state) throws SQLException {
    String sql = "update " + getTableName(distributeJobId) +
                 " set state=:state , last_modify_timestamp = :lastModifyTimestamp where id=:id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", distributeJobId);
    params.put("state", state);
    params.put("lastModifyTimestamp", System.currentTimeMillis());
    return dbUtil.update(connection, sql, params);
  }

  public Connection getConnection(String id) throws SQLException {
    String key = dbShardingStrategy.getDataSourceKey(id);
    DataSource dataSource = DataBaseUtil.dataSourceMap.get(key);
    return dataSource.getConnection();
  }

  private String getTableName(String distributeJobId) {
    return "distribute_job";
  }
}
