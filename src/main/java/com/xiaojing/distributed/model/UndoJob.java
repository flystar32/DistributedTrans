package com.xiaojing.distributed.model;

import java.io.Serializable;

public class UndoJob implements Serializable {
    private String id;
    private String distributeJobId;
    private String state;
    private byte[] content;

    public UndoJob() {
    }

    public UndoJob(String distributeJobId, String id, String state, byte[] content) {
        this.id = id;
        this.distributeJobId = distributeJobId;
        this.state = state;
        this.content = content;
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

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getDistributeJobId() {
        return distributeJobId;
    }

    public void setDistributeJobId(String distributeJobId) {
        this.distributeJobId = distributeJobId;
    }

    @Override
    public String toString() {
        return "UndoJob{" +
                "id='" + id + '\'' +
                ", distributeJobId='" + distributeJobId + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
