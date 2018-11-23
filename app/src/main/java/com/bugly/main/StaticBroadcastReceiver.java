package com.bugly.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bugly.utils.AndroidUtils;
import com.bugly.utils.AppSettings;
import com.kylindev.pttlib.service.InterpttService;

import static com.kylindev.pttlib.LibConstants.ACTION_AUTO_LAUNCH;

/**
 * Created by wuwentao on 2018/8/23.
 */

public class StaticBroadcastReceiver extends BroadcastReceiver {



    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if("android.intent.action.BOOT_COMPLETED".equals(action)) {//开机启动服务
            AndroidUtils.Log("开机启动服务");
            boolean auto = AppSettings.getInstance(context).getAutoLaunch();
            if (auto) {
                //延时一段时间再启动
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            Intent serviceIntent = new Intent(context, InterpttService.class);

                            //自动启动service，在Service的实现里判断，如果是自动启动的，则自动登录
                            serviceIntent.setAction(ACTION_AUTO_LAUNCH);

                            context.startService(serviceIntent);

                            Intent userIntent = new Intent(context, UserService.class);
                            context.startService(userIntent);



                        } catch (Exception e) {

                        }
                    }
                }).start();
            }
        }
    }

}
