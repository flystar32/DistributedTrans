package com.xiaojing.distributed.interceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.xiaojing.distributed.util.Context;
import com.xiaojing.distributed.util.DistributedTransInvocation;
import com.xiaojing.distributed.dao.UndoJobDao;
import com.xiaojing.distributed.model.DistributeJob;
import com.xiaojing.distributed.model.UndoJob;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Singleton
public class UndoJobInterceptor implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndoJobInterceptor.class);

    @Inject
    private UndoJobDao undoJobDao;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {

        Context context = (Context) methodInvocation.getArguments()[0];
        UndoJob undoJob = (UndoJob) methodInvocation.getArguments()[1];

        if (undoJob != null) {
            return invokeUndoJobMethod(methodInvocation,context,undoJob);
        } else {
            return serializeUndoJob(methodInvocation,context,undoJob);
        }
    }

    /**
     * 当传入的UndoJob为null时，执行此方法
     * 换言之，正常的DoJob时，执行此方法
     *
     * 此时并不执行真实的Undo操作，而是将调用此UndoJob的DoJob方法所对应的Undo操作序列化成byte数组并保存在UndoJob中
     * 此UndoJob保存在context中，DoJob的二阶段使用
     *
     * @param methodInvocation
     * @param context
     * @param undoJob
     * @return
     * @throws Throwable
     */
    private Object serializeUndoJob(MethodInvocation methodInvocation,Context context,UndoJob undoJob)throws Throwable{
        LOGGER.debug("====serialize UndoJob======");
        Object[] arguments = methodInvocation.getArguments();
        //set context to be null as we don't need it in rollback action, content to null as we don't need it
        arguments[0] = null;
        undoJob = new UndoJob(context.getDistributeJob().getId(), UUID.randomUUID().toString(), "insert", null);
        undoJob.setContent("".getBytes());
        arguments[1] = undoJob;
        DistributedTransInvocation
            distributedTransInvocation = new DistributedTransInvocation(methodInvocation.getMethod().getDeclaringClass().getName(),
                methodInvocation.getMethod().getName(), methodInvocation.getMethod().getParameterTypes(), arguments);
        byte[] content = DistributedTransInvocation.serialize(distributedTransInvocation);
        undoJob.setContent(content);
        context.getPendingUndoJobs().add(undoJob);
        return null;
    }


    /**
     *
     * 当传入的UndoJob不为bull时，执行此方法
     * 换言之，离线程序进行rollback补偿时，执行此方法
     *
     *
     * 执行此方法前，从数据库中拿到的UndoJob的content中拿到此Undo的函数、参数等保存的信息，并调用
     * 由于之前将context设置为null，应该先new一个以保存connection
     * 同时将content置为nul，节省空间
     *
     * 第一步，执行补偿操作，即Undo，得到boolean类型的result
     * 副作用为根据userId得到connection，存储在context，此connect与undoJobDao共用，两者处于同一个事务
     * connection的autoCommit属性根据分库的@Shared中的autoCommit属性来设置，事务的开始在@Shared对应的SharedKeyInterceptor
     *
     * 第二步，检查数据库防止重入，如果不存在且result为false，此时return false，上层接收到后标记为distribute_job_rollback_fail
     * 如果UndoJob为rollbacked状态，connection.rollback()，回滚执行的补偿操作，return true以保持一致性
     *
     * 第三步，Undo执行成功且UndoJob之前不为rollbacked状态时，将其保存为rollbacked并提交事务
     *
     *
     * @param methodInvocation
     * @param context
     * @param undoJob
     * @return
     * @throws Throwable
     */
    private Object invokeUndoJobMethod(MethodInvocation methodInvocation,Context context,UndoJob undoJob)throws Throwable{
        LOGGER.debug("====invoke Undo======");
        Object result;
        methodInvocation.getArguments()[0] = new Context(new DistributeJob());
        Connection connection = null;
        try {
            result = methodInvocation.proceed();
            context = (Context) methodInvocation.getArguments()[0];
            undoJob.setContent(new byte[]{});
            connection = context.getConnection();
            try {
                undoJobDao.insertUndo(connection, undoJob);
            } catch (SQLException e) {
                //Duplicate entry exception is expected, catch and log it
                //Error: 1062 SQLSTATE: 23000 (ER_DUP_ENTRY) Message: Duplicate entry '%s' for key %d
                LOGGER.error("error code for h2 is {}",e.getErrorCode());
                LOGGER.error("sql state is {}",e.getSQLState());
                if (e.getErrorCode() == 1062 || e.getErrorCode() ==23505) {
                    LOGGER.info("Exception when try to insert a duplicate undo log:", e);
                } else {
                    throw e;
                }
            }
            undoJob = undoJobDao.getAndLockUndoById(connection, undoJob.getId());
            if (undoJob.getState().equals("rollbacked")) {
                connection.rollback();
                return true;
            }

            if ((boolean) (result)) {
                undoJobDao.updateUndoStateById(connection, undoJob.getId(), "rollbacked");
                connection.commit();
            } else {
                connection.rollback();
                return false;
            }
        } catch (Throwable throwable) {
            connection.rollback();
            LOGGER.error("Exception in execute undo job:", throwable);
            throw throwable;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        return result;
    }
}
