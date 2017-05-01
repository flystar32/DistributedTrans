package com.xiaojing.distributed.interceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mysql.jdbc.CommunicationsException;
import com.xiaojing.distributed.util.Context;
import com.xiaojing.distributed.dao.DistributeJobDao;
import com.xiaojing.distributed.dao.sharding.DBShardingStrategy;
import com.xiaojing.distributed.model.DistributeJob;
import com.xiaojing.distributed.util.DataBaseUtil;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.UUID;

@Singleton
public class DistributeJobInterceptor implements MethodInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DistributeJobInterceptor.class);
  @Inject
  private DistributeJobDao distributeJobDao;
  @Inject
  private DBShardingStrategy dbShardingStrategy;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    DistributeJob distributeJob = new DistributeJob(UUID.randomUUID().toString(), "insert", System.currentTimeMillis());
    LOGGER.debug("====distributeJobDao======");

    String key = dbShardingStrategy.getDataSourceKey(distributeJob.getId());
    DataSource dataSource = DataBaseUtil.dataSourceMap.get(key);
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      distributeJobDao.insertDistributeJob(connection, distributeJob);
      Context context = new Context(distributeJob);
      methodInvocation.getArguments()[0] = context;
      distributeJobDao.selectForUpdateById(connection, distributeJob.getId());
      try {
        Object result = methodInvocation.proceed();
        context.getDistributeJob().commit(connection);
        return result;
      } catch (Throwable throwable) {
            /*TODO do not rollback if mysql network exception thrown as transaction may committed/rollbacked after sending commit command, let offline rollback handle this situation*/
        if (!(throwable instanceof CommunicationsException)) {
          try {
            LOGGER.info("exception is not sqlIOE,invoke undoLog to roll back now");
            if (!context.getDistributeJob().rollback(connection)) {
              LOGGER.error("Distribute job: {} rollback failed", context.getDistributeJob());
            }
          } catch (Throwable innerThrowable) {
            LOGGER.error("Exception in rollback:", innerThrowable);
          }
        }
        LOGGER.warn("Exception during execute distribute job:", throwable);
        throw throwable;
      }
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}