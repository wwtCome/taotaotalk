package com.bugly.main;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.multidex.MultiDex;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.zhouyou.http.EasyHttp;

import org.litepal.LitePal;

/**
 * Created by wuwentao on 2018/9/17.
 */

public class SampleApplicationLike extends DefaultApplicationLike {

    public static final String TAG = "Tinker.SampleApplicationLike";
    public static final String APPID = "4a54eeb07f";

    public SampleApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 这里实现SDK初始化，appId替换成你的在Bugly平台申请的appId
        // 这第二个参数是Bugly平台申请的appId
        // 调试时，将第三个参数改为true
        Bugly.init(getApplication(),APPID,true);
        CrashReport.initCrashReport(getApplication(),APPID, false);
        // 初始化LitePal数据库
        LitePal.initialize(getApplication());
        //rxeasyhttp网络请求库初始化
        EasyHttp.init(getApplication());
//        EasyHttp.getInstance().setBaseUrl("http://47.99.207.177");
//        EasyHttp.getInstance().setBaseUrl("http://47.96.235.12");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        // you must install multiDex whatever tinker is installed!
        MultiDex.install(base);
        // 安装tinker
        // TinkerManager.installTinker(this); 替换成下面Bugly提供的方法
        Beta.installTinker(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallback(Application.ActivityLifecycleCallbacks callbacks) {
        getApplication().registerActivityLifecycleCallbacks(callbacks);
    }
}
