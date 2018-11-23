package com.bugly.database;


import com.bugly.signin.DevicePostion;

import org.litepal.crud.DataSupport;

import java.util.List;

/**
 * Created by wuwentao on 2018/10/31.
 */

public class SqlUtils {


    /**
     * 增加一条数据
     */
    public static void addDevicePostion(AllBthDeviceInfo userPatrol)
    {
    }

    /**
     * 批量增加数据
     */
    public static void addDevicePostionList(List<AllBthDeviceInfo> userPatrol)
    {
        DataSupport.saveAll(userPatrol);
    }


    /**
     * 删除所有数据
     */
    public static void deleteAll() {
        int i = DataSupport.deleteAll(AllBthDeviceInfo.class);
    }


    /**
    * 查询所有数据
    */
    public static List<AllBthDeviceInfo> findAll() {
        List<AllBthDeviceInfo> all = DataSupport.findAll(AllBthDeviceInfo.class);
        return all;
    }




}
