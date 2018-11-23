package com.bugly.signin;

/**
 * Created by wuwentao on 2018/11/14.
 */

public class AlreadySingInBth {

    String address;
    long time;

    public AlreadySingInBth(String address, long time) {
        this.address = address;
        this.time = time;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "AlreadySingInBth{" +
                "address='" + address + '\'' +
                ", time=" + time +
                '}';
    }
}
