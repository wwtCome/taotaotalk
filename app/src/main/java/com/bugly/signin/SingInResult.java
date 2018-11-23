package com.bugly.signin;

/**
 * Created by wuwentao on 2018/11/13.
 */

public class SingInResult {


    /**
     * code : 1
     * msg : 操作数据成功！
     * time : 1542072097
     * data : 12
     */

    private int code;
    private String msg;
    private String time;
    private String data;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
