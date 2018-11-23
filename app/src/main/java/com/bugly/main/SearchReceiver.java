package com.bugly.main;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bugly.utils.AndroidUtils;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

/**
 * Created by wuwentao on 2018/11/8.
 */

public class SearchReceiver extends BroadcastReceiver {

    private UserService userService;

    public SearchReceiver(UserService userService) {
        this.userService = userService;
    }

    public Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context =  context;
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            AndroidUtils.Log( "扫描到了 --- > name : " + device.getName() + "-----> address:" + device.getAddress());
            //设置监听回调给service处理
            userService.addDevice(device);

        }else if(ACTION_DISCOVERY_FINISHED.equals(action))
        {
            AndroidUtils.Log("蓝牙设备扫描结束"+"-->重新开始扫描");
            userService.startSearch();
        }
    }




}
