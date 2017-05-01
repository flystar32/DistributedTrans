package com.xiaojing.distributed.interceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.xiaojing.distributed.util.Context;
import com.xiaojing.distributed.annotation.GetConnection;
import com.xiaojing.distributed.annotation.SharedKey;
import com.xiaojing.distributed.dao.sharding.DBShardingStrategy;
import com.xiaojing.distributed.util.DataBaseUtil;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.sql.DataSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Connection;

@Singleton
public class ShardingInterceptor implements MethodInterceptor {

  @Inject
  private DBShardingStrategy dbShardingStrategy;

  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Context context = (Context) methodInvocation.getArguments()[0];
    Method method = methodInvocation.getMethod();
    Object[] args = methodInvocation.getArguments();
    boolean autoCommit = method.getAnnotation(GetConnection.class).autoCommit();
    Annotation[][] annotationsArray = method.getParameterAnnotations();

    Object shardindKey = null;

    for (int i = 0; i < annotationsArray.length; i++) {
      Annotation[] annotations = annotationsArray[i];
      if (annotations.length > 0) {
        for (Annotation annotation : annotations) {
          if (annotation instanceof SharedKey) {
            shardindKey = args[i];
          }
        }
      }
    }

    DataSource dataSource = DataBaseUtil.dataSourceMap.get(dbShardingStrategy.getDataSourceKey(shardindKey + ""));
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(autoCommit);
    context.setConnection(connection);
    return methodInvocation.proceed();
  }
}
