package com.xiaojing.distributed.util;

import com.xiaojing.distributed.model.UndoJob;
import com.xiaojing.distributed.model.DistributeJob;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Context {
    private DistributeJob distributeJob;
    private List<UndoJob> pendingUndoJobs = new ArrayList<>();
    private Connection connection;

    public Context(DistributeJob distributeJob) {
        this.distributeJob = distributeJob;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public List<UndoJob> getPendingUndoJobs() {
        return pendingUndoJobs;
    }

    public String getId() {
        return getDistributeJob().getId();
    }

    public void setId(String id) {
        getDistributeJob().setId(id);
    }

    public DistributeJob getDistributeJob() {
        return distributeJob;
    }
}
