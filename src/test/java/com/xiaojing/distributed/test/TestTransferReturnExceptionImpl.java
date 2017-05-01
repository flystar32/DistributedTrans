package com.xiaojing.distributed.test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;

import com.xiaojing.distributed.annotation.DistributeJob;
import com.xiaojing.distributed.annotation.UndoJob;
import com.xiaojing.distributed.BaseTest;
import com.xiaojing.distributed.annotation.GetConnection;
import com.xiaojing.distributed.annotation.TransferInDoJob;
import com.xiaojing.distributed.annotation.TransferOutDoJob;
import com.xiaojing.distributed.dao.UserDao;
import com.xiaojing.distributed.impl.TransferReturnExceptionImpl;
import com.xiaojing.distributed.interceptor.DistributeJobInterceptor;
import com.xiaojing.distributed.interceptor.DoJobInterceptor;
import com.xiaojing.distributed.interceptor.ShardingInterceptor;
import com.xiaojing.distributed.interceptor.UndoJobInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by xiaojing on 15/10/9.
 *
 * 在这个测试中,测试了转账本身已经完成,系统抛出其他异常的情景
 * 此时框架会执行TransferOut和TransferIn对应的undoLog,撤销TransferOut和TransferIn的操作
 * 从结果可以看出,保持了数据的一致性
 */
public class TestTransferReturnExceptionImpl extends BaseTest {


    @Before
    public void before() {
        //create Injector
        System.out.println("---  init Injector  ---");
        List<Module> modules = new LinkedList<Module>();


        modules.add(new Module() {
            public void configure(Binder binder) {
                ShardingInterceptor shardingInterceptor = new ShardingInterceptor();
                binder.requestInjection(shardingInterceptor);
                //SessionKeyInterceptor sessionKeyInterceptor = new SessionKeyInterceptor();
                //binder.requestInjection(sessionKeyInterceptor);
                DoJobInterceptor doJobInterceptor = new DoJobInterceptor();
                binder.requestInjection(doJobInterceptor);
                UndoJobInterceptor undoJobInterceptor = new UndoJobInterceptor();
                binder.requestInjection(undoJobInterceptor);
                DistributeJobInterceptor distributeJobInterceptor = new DistributeJobInterceptor();
                binder.requestInjection(distributeJobInterceptor);

                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(TransferInDoJob.class), doJobInterceptor);
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(TransferOutDoJob.class), doJobInterceptor);
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(UndoJob.class), undoJobInterceptor);
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(DistributeJob.class), distributeJobInterceptor);
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(GetConnection.class),
                                       shardingInterceptor);
            }
        });


        injector = Guice.createInjector(modules);
        System.out.println("---init Injector ok ---");
    }



    @Test
    public void testTransferReturnExceptionImpl() throws Exception {
        TransferReturnExceptionImpl transferReturnException = injector.getInstance(TransferReturnExceptionImpl.class);
        UserDao userDao = injector.getInstance(UserDao.class);

        long beforeA = userDao.query(connection2, "1").getBalance();
        long beforeB = userDao.query(connection1, "2").getBalance();
        long beforeSum = beforeA + beforeB;
        try {
            transferReturnException.transfer("1", "2", 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long afterA = userDao.query(connection2, "1").getBalance();
        long afterB = userDao.query(connection1, "2").getBalance();
        long afterSum = afterA + afterB;
        System.out.println("beforeA     " + beforeA);
        System.out.println("beforeB     " + beforeB);
        System.out.println("beforeSum   " + beforeSum);
        System.out.println("afterA      " + afterA);
        System.out.println("afterB      " + afterB);
        System.out.println("afterSum    " + afterSum);
        Assert.assertEquals(beforeA, afterA);
        Assert.assertEquals(beforeB, afterB);
        Assert.assertEquals(beforeSum, afterSum);


    }
}
