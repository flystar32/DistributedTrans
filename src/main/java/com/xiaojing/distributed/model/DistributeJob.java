package com.xiaojing.distributed.model;

import com.google.common.collect.Lists;

import com.xiaojing.distributed.util.DistributedTransInvocation;
import com.xiaojing.distributed.dao.DistributeJobDao;
import com.xiaojing.distributed.util.GuiceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DistributeJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributeJob.class);
    private String id;
    private String state;
    private long lastModifyTimestamp;
    private List<UndoJob> undoJobList = new ArrayList<>();

    public DistributeJob() {
    }

    public DistributeJob(String id, String state, long lastModifyTimestamp) {
        this.id = id;
        this.state = state;
        this.lastModifyTimestamp = lastModifyTimestamp;
    }

    public List<UndoJob> getUndoJobList() {
        return undoJobList;
    }

    public long getLastModifyTimestamp() {
        return lastModifyTimestamp;
    }

    public void setLastModifyTimestamp(long lastModifyTimestamp) {
        this.lastModifyTimestamp = lastModifyTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    public void commit(Connection connection) throws Exception {
        DistributeJobDao
            distributeJobDao = GuiceConfig.getInstance().getInjector().getInstance(DistributeJobDao.class);
        distributeJobDao.updateDistributeJobStateById(connection, id, "commited");
    }

    public boolean rollback(Connection connection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IOException, IllegalAccessException, SQLException {
        DistributeJobDao distributeJobDao = GuiceConfig.getInstance().getInjector().getInstance(DistributeJobDao.class);
        for (UndoJob undoJob : Lists.reverse(undoJobList)) {
            if (!(boolean) DistributedTransInvocation.deserialize(undoJob.getContent()).invoke()) {
                LOGGER.error("rollback failed:", undoJob);

                distributeJobDao.updateDistributeJobStateById(connection, id, "rollback_fail");
                return false;
            }
        }
        distributeJobDao.updateDistributeJobStateById(connection, id, "rollbacked");
        return true;
    }

    @Override
    public String toString() {
        return "DistributeJob{" +
                "id='" + id + '\'' +
                ", state='" + state + '\'' +
                ", lastModifyTimestamp=" + lastModifyTimestamp +
                '}';
    }
}