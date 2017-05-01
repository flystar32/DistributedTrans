package com.xiaojing.distributed.model;

public class User {
    private String id;
    private long balance;

    public User() {
    }

    public User(String id, long balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", balance=" + balance +
                '}';
    }
}
