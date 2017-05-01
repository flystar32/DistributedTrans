package com.xiaojing.distributed.util;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;

import com.xiaojing.distributed.annotation.DistributeJob;
import com.xiaojing.distributed.annotation.DoJob;
import com.xiaojing.distributed.annotation.GetConnection;
import com.xiaojing.distributed.annotation.UndoJob;
import com.xiaojing.distributed.interceptor.DistributeJobInterceptor;
import com.xiaojing.distributed.interceptor.DoJobInterceptor;
import com.xiaojing.distributed.interceptor.ShardingInterceptor;
import com.xiaojing.distributed.interceptor.UndoJobInterceptor;

public class GuiceConfig {
    private static final GuiceConfig INSTANCE = new GuiceConfig();
    private final Injector injector;

    private GuiceConfig() {

        // init configure
        DataBaseUtil.init();

        // bindings
        injector = Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                ShardingInterceptor shardingInterceptor = new ShardingInterceptor();
                binder.requestInjection(shardingInterceptor);
                DoJobInterceptor doJobInterceptor = new DoJobInterceptor();
                binder.requestInjection(doJobInterceptor);
                UndoJobInterceptor undoJobInterceptor = new UndoJobInterceptor();
                binder.requestInjection(undoJobInterceptor);
                DistributeJobInterceptor distributeJobInterceptor = new DistributeJobInterceptor();
                binder.requestInjection(distributeJobInterceptor);
                binder.bindInterceptor(Matchers.any(),
                        Matchers.annotatedWith(DoJob.class), doJobInterceptor);
                binder.bindInterceptor(Matchers.any(),
                        Matchers.annotatedWith(UndoJob.class), undoJobInterceptor);
                binder.bindInterceptor(Matchers.any(),
                        Matchers.annotatedWith(DistributeJob.class),
                        distributeJobInterceptor);
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(GetConnection.class),
                                       shardingInterceptor);
            }
        });
    }

    public static GuiceConfig getInstance() {
        return INSTANCE;
    }

    public Injector getInjector() {
        return injector;
    }
}
