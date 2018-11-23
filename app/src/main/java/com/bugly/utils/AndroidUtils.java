package com.bugly.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.bugly.signin.DevicePostion;
import com.bugly.signin.MemberInfo;
import com.bugly.signin.SingInResult;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


/**
 * Created by wuwentao on 2018/9/18.
 */

public class AndroidUtils {

    public static final String debug = "tttalk";

    /**
     * 获取当前本地apk的版本
     * @return
     */
    public static int getVerCode(Context ctx) {
        int localVersion = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 获取版本号名称you a
     * @return
     */
    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    /**
     * log输出
     * @param cat
     */
    public static void Log(String cat)
    {
        Log.e(debug,cat);
    }


    public static void showToast(Context c, String str) {
        if (c != null) {
            Toast t = Toast.makeText(c, str, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    /**
     * 通过String的ID显示提示
     * @param c
     * @param strId
     */
    public static void showIdToast(Context c, int strId) {
        Toast t = Toast.makeText(c, strId, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }


    public static MemberInfo PraMemberInfo(String gsondata)
    {
        Gson gson = new Gson();
        MemberInfo memberInfo = gson.fromJson(gsondata,MemberInfo.class);
        if(memberInfo.getMsg().equals(1) && memberInfo.getData().size() > 0)
        {
            AndroidUtils.Log("json数据解析完成"+"msg=="+memberInfo.getMsg()+",code=="+memberInfo.getCode()+
                    ",time=="+memberInfo.getTime()+",id=="+memberInfo.getData().get(0).getId()+",uid=="+memberInfo.getData().get(0).getUid()
                    +",customer_id=="+memberInfo.getData().get(0).getCustomer_id()  +",createtime=="+memberInfo.getData().get(0).getCreatetime());
        }
        return memberInfo;
    }

    public static List<DevicePostion.DataBean> PraDevicePostion(String gsondata){
        Gson gson = new Gson();
        DevicePostion devicePostion = gson.fromJson(gsondata, DevicePostion.class);
        List<DevicePostion.DataBean> data = devicePostion.getData();
        return data;
    }

    public static SingInResult PraSingInResult(String gsondata)
    {
        Gson gson = new Gson();
        SingInResult singInResult = gson.fromJson(gsondata, SingInResult.class);
        return singInResult;
    }

    public static long getNetTime() {
        URL url = null;//取得资源对象
        try {
            url = new URL("http://www.baidu.com");
            //url = new URL("http://www.ntsc.ac.cn");//中国科学院国家授时中心
            //url = new URL("http://www.bjtime.cn");
            URLConnection uc = url.openConnection();//生成连接对象
            uc.connect(); //发出连接
            long ld = uc.getDate(); //取得网站日期时间
            Log("uc.getDate():"+ld);
            return ld;
//            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTimeInMillis(ld);
//            final String format = formatter.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return System.currentTimeMillis();
    }


}
