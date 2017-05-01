package com.xiaojing.distributed.server;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.xiaojing.distributed.annotation.DistributeJob;
import com.xiaojing.distributed.annotation.DoJob;
import com.xiaojing.distributed.annotation.GetConnection;
import com.xiaojing.distributed.annotation.SharedKey;
import com.xiaojing.distributed.annotation.UndoJob;
import com.xiaojing.distributed.dao.DistributeJobDao;
import com.xiaojing.distributed.dao.UndoJobDao;
import com.xiaojing.distributed.dao.UserDao;
import com.xiaojing.distributed.dao.sharding.DBShardingStrategy;
import com.xiaojing.distributed.util.Context;
import com.xiaojing.distributed.util.DataBaseUtil;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

@Singleton
public class SimpleTransferImplV2 implements SimpleTransfer.Iface {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTransferImplV2.class);
  private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
  @Inject
  private DistributeJobDao distributeJobDao;
  @Inject
  private UserDao userDao;
  @Inject
  private UndoJobDao undoJobDao;
  @Inject
  private DBShardingStrategy dbShardingStrategy;

  @Override
  public boolean transfer(String fromId, String toId, long amount) throws TException {
    return transfer(null, fromId, toId, amount);
  }

  @DistributeJob
  public boolean transfer(Context context, String fromId, String toId, long amount)
      throws TException {
    try {
      transferOut(context, fromId, toId, amount);
      transferIn(context, fromId, toId, amount);
    } catch (Exception e) {
      throw new TException(e);
    }
    return true;
  }


  @DoJob
  @GetConnection
  public boolean transferOut(Context context, @SharedKey("userId") String fromId, String toId,
                             long amount) throws Exception {
    userDao.updateBalanceById(context.getConnection(), fromId, -amount, 0L);
    undoTransferOut(context, null, fromId, toId, amount);
    return true;
  }

  @UndoJob
  @GetConnection
  public boolean undoTransferOut(Context context, com.xiaojing.distributed.model.UndoJob undoJob,
                                 @SharedKey("userId") String fromId, String toId, long amount)
      throws Exception {
    userDao.updateBalanceById(context.getConnection(), fromId, amount, Long.MIN_VALUE);
    return true;
  }

  @DoJob
  @GetConnection
  public boolean transferIn(Context context, String fromId, @SharedKey("userId") String toId,
                            long amount) throws Exception {
    userDao.updateBalanceById(context.getConnection(), toId, amount, Long.MIN_VALUE);
    undoTransferIn(context, null, fromId, toId, amount);
    return true;
  }

  @UndoJob
  @GetConnection
  public boolean undoTransferIn(Context context, com.xiaojing.distributed.model.UndoJob undoJob,
                                String fromId, @SharedKey("userId") String toId, long amount)
      throws Exception {
    userDao.updateBalanceById(context.getConnection(), toId, -amount, Long.MIN_VALUE);
    return true;
  }






  public void scheduleRollback(long intervalInMillis) {
    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          List<com.xiaojing.distributed.model.DistributeJob>
              suspendedJobList =
              distributeJobDao.selectSuspended(System.currentTimeMillis() - 60 * 1000);//todo 分库分表？
          for (com.xiaojing.distributed.model.DistributeJob suspendedJob : suspendedJobList) {
            String key = dbShardingStrategy.getDataSourceKey(suspendedJob.getId());
            DataSource dataSource = DataBaseUtil.dataSourceMap.get(key);
            Connection connection = null;

            try {
              connection = dataSource.getConnection();
              distributeJobDao
                  .selectForUpdateById(connection, suspendedJob.getId());//这里通过自己的id，可以拿到connection
              Map<String, com.xiaojing.distributed.model.UndoJob> undoJobMap = new HashMap<>();
              for (com.xiaojing.distributed.model.UndoJob undoJob : undoJobDao
                  .getSuspendedByDistributeJobId(suspendedJob.getId())) {
                undoJobMap.put(undoJob.getId(), undoJob);
              }
              suspendedJob.getUndoJobList().addAll(undoJobMap.values());
              try {
                if (!suspendedJob.rollback(connection)) {
                  LOGGER.error("Distribute job: {} rollback failed", suspendedJob);
                }
              } catch (Throwable throwable) {
                LOGGER.error("Exception in offline rollback:", throwable);
              }
            } finally {
              if (connection != null) {
                connection.close();
              }
            }
          }
        } catch (Throwable throwable) {
          LOGGER.error("exception in offline rollback:", throwable);
        }
      }
    }, intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
  }
}
