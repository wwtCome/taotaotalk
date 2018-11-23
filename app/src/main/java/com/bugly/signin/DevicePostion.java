package com.bugly.signin;


import java.util.List;

/**
 * Created by wuwentao on 2018/11/10.
 */

public class DevicePostion {


    /**
     * code : 1
     * msg : 获取设备绑定信息成功！
     * time : 1541821946
     * data : [{"id":1,"device_id":"21213111","position":"China","customer_id":"00000004","createtime":1541812975,"updatetime":1541812975},{"id":2,"device_id":"21141112","position":"Japan","customer_id":"00000004","createtime":1541812975,"updatetime":1541812975}]
     */

    private int code;
    private String msg;
    private String time;
    private List<DataBean> data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean{
        /**
         * id : 1
         * device_id : 21213111
         * position : China
         * customer_id : 00000004
         * createtime : 1541812975
         * updatetime : 1541812975
         */

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
    }
}
