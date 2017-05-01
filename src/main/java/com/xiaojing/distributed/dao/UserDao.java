package com.xiaojing.distributed.dao;

import com.google.inject.Inject;

import com.xiaojing.distributed.dao.sharding.DBShardingStrategy;
import com.xiaojing.distributed.model.User;
import com.xiaojing.distributed.util.DataBaseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by xiaojing on 15/10/14.
 */
public class UserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDao.class);
    @Inject
    private DbUtil dbUtil;

  @Inject
  private DBShardingStrategy dbShardingStrategy;

    public int insert(Connection connection, String userId) throws Exception {
        String sql = "insert into " + getTableName(userId) + " values(:id,0)";
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);
        return dbUtil.insert(connection, sql, params);
    }

    public User query(Connection connection, String userId) throws Exception {
        String sql = "select id,balance from " + getTableName(userId) + " where id=:id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);
        return dbUtil.queryOne(connection, sql, User.class, params);
    }

    public int updateBalanceById(Connection connection, String userId, long delta, long minBalance) throws SQLException {
        String sql = "update " + getTableName(userId) + " set balance=balance+:delta where id=:id and balance+:delta >= :minBalance";
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);
        params.put("delta", delta);
        params.put("minBalance", minBalance);
        return dbUtil.update(connection, sql, params);
    }

    public Connection getConnection(String id) throws SQLException {
        String key = dbShardingStrategy.getDataSourceKey(id);
        DataSource dataSource = DataBaseUtil.dataSourceMap.get(key);
        return dataSource.getConnection();
    }

    private String getTableName(String userId) {
        return "user";
    }
}
