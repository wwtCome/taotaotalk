package com.bugly.database;

import org.litepal.crud.DataSupport;

/**
 * Created by wuwentao on 2018/11/12.
 */

public class AllBthDeviceInfo extends DataSupport {

    private int id;
    private String device_id;
    private String position;
    private String customer_id;
    private int createtime;
    private int updatetime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public int getCreatetime() {
        return createtime;
    }

    public void setCreatetime(int createtime) {
        this.createtime = createtime;
    }

    public int getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(int updatetime) {
        this.updatetime = updatetime;
    }

    @Override
    public String toString() {
        return "AllBthDeviceInfo{" +
                "id=" + id +
                ", device_id='" + device_id + '\'' +
                ", position='" + position + '\'' +
                ", customer_id='" + customer_id + '\'' +
                ", createtime=" + createtime +
                ", updatetime=" + updatetime +
                '}';
    }
}
