package com.xiaojing.distributed.dao;

import com.sop4j.dbutils.BasicRowProcessor;
import com.sop4j.dbutils.BatchExecutor;
import com.sop4j.dbutils.GenerousBeanProcessor;
import com.sop4j.dbutils.InsertExecutor;
import com.sop4j.dbutils.QueryExecutor;
import com.sop4j.dbutils.QueryRunner;
import com.sop4j.dbutils.UpdateExecutor;
import com.sop4j.dbutils.handlers.BeanHandler;
import com.sop4j.dbutils.handlers.BeanListHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class DbUtil {

  private final static QueryRunner QUERY_RUNNER = new QueryRunner();
  private static Logger LOGGER = LoggerFactory.getLogger(DbUtil.class);

  public DbUtil() {
  }

  public <T> List<T> queryList(Connection connection, String sql, Class clazz,
                               Map<String, Object> params)
      throws SQLException {

    long before = System.currentTimeMillis();
    boolean success = false;
    List<T> result = null;
    Throwable failureException = null;
    LOGGER.info("sql={} , params={}", sql, params);

    try {
      QueryExecutor queryExecutor = QUERY_RUNNER.query(connection, false, sql);
      for (String key : params.keySet()) {
        queryExecutor.bind(key, params.get(key));
      }
      result = queryExecutor.execute(new BeanListHandler<T>(clazz, new BasicRowProcessor(new GenerousBeanProcessor())));
      success = true;
      return result;
    } catch (Exception e) {
      LOGGER.error("database error,sql={},params={}", sql, params, e);
      failureException = e;
      throw e;
    } finally {
      finallyBlock(sql, params, before, success, result, failureException);
    }
  }

  public <T> T queryOne(Connection connection, String sql, Class clazz, Map<String, Object> params)
      throws SQLException {

    long before = System.currentTimeMillis();
    boolean success = false;
    T result = null;
    Throwable failureException = null;
    LOGGER.info("sql={} , params={}", sql, params);

    try {
      QueryExecutor queryExecutor = QUERY_RUNNER.query(connection, false, sql);
      for (String key : params.keySet()) {
        queryExecutor.bind(key, params.get(key));
      }
      result = queryExecutor.execute(new BeanHandler<T>(clazz, new BasicRowProcessor(new GenerousBeanProcessor())));
      success = true;
      return result;
    } catch (Exception e) {
      LOGGER.error("database error,sql={},params={}", sql, params, e);
      failureException = e;
      throw e;
    } finally {
      finallyBlock(sql, params, before, success, result, failureException);
    }
  }

  public int update(Connection connection, String sql, Map<String, Object> params)
      throws SQLException {

    long before = System.currentTimeMillis();
    boolean success = false;
    int result = 0;
    Throwable failureException = null;
    LOGGER.info("sql={} , params={}", sql, params);

    try {
      UpdateExecutor updateExecutor = QUERY_RUNNER.update(connection, false, sql);
      for (String key : params.keySet()) {
        updateExecutor.bind(key, params.get(key));
      }
      result = updateExecutor.execute();
      success = (result > 0);
      return result;
    } catch (Exception e) {
      LOGGER.error("database error,sql={},params={}", sql, params, e);
      failureException = e;
      throw e;
    } finally {
      finallyBlock(sql, params, before, success, result, failureException);
    }
  }


  public int insert(Connection connection, String sql, Map<String, Object> params)
      throws SQLException {

    long before = System.currentTimeMillis();
    boolean success = false;
    int result = 0;
    Throwable failureException = null;
    LOGGER.info("sql={} , params={}", sql, params);

    try {
      InsertExecutor executor = QUERY_RUNNER.insert(connection, false, sql);
      for (String key : params.keySet()) {
        executor.bind(key, params.get(key));
      }
      result = executor.execute();
      success = (result == 1);
      return result;
    } catch (Exception e) {
      LOGGER.error("database error,sql={},params={}", sql, params, e);
      failureException = e;
      throw e;
    } finally {
      finallyBlock(sql, params, before, success, result, failureException);
    }
  }

  public int[] batch(Connection connection, String sql, Map<String, Object> params)
      throws SQLException {

    long before = System.currentTimeMillis();
    boolean success = false;
    int[] result = null;
    Throwable failureException = null;
    LOGGER.info("sql={} , params={}", sql, params);

    try {

      BatchExecutor executor = QUERY_RUNNER.batch(connection, false, sql);
      for (String key : params.keySet()) {
        executor.bind(key, params.get(key));
      }
      result = executor.execute();
      success = true;
      return result;
    } catch (Exception e) {
      LOGGER.error("database error,sql={},params={}", sql, params, e);
      failureException = e;
      throw e;
    } finally {
      finallyBlock(sql, params, before, success, result, failureException);
    }
  }

  public <T> T executeTransaction(Connection connection, Transaction<T> transaction)
      throws Throwable {
    Statement statement = connection.createStatement();
    try {
      statement.execute("START TRANSACTION");
      return transaction.doTransaction();
    } finally {
      if (transaction.shouldRollback) {
        statement.execute("ROLLBACK");
      } else {
        statement.execute("COMMIT");
      }
      statement.close();
      connection.close();
    }
  }

  public static abstract class Transaction<T> {

    protected boolean shouldRollback = true;

    abstract T doTransaction() throws Throwable;
  }

  private static void finallyBlock(String sql, Map paramStr, long before, boolean success,
                                   Object result,
                                   Throwable failureException) {
    LOGGER.info("sql={},params={},result={},e=", sql, paramStr, result, failureException);
  }

}

