package com.xiaojing.distributed;

import com.xiaojing.distributed.server.SimpleTransferImpl;
import com.xiaojing.distributed.util.GuiceConfig;

/**
 * Created by xiaojing on 15/10/27.
 */
public class SchedulerRollback {
    public static void main(String[] args) {
        SimpleTransferImpl phoenixCore = GuiceConfig.getInstance().getInjector().getInstance(SimpleTransferImpl.class);

        phoenixCore.scheduleRollback(10000);//超过10秒仍处于初始状态认为事务已经失败
    }
}
