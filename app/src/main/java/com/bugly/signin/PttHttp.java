package com.bugly.signin;

import android.util.Log;

import com.bugly.utils.AndroidUtils;
import com.zhouyou.http.EasyHttp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by guojianyong on 2018/11/9.
 *  所有请求为耗时操作，如有异常 请开辟线程
 *
 *  操作方式
 *    (必须操作)
 *      1：，登陆后 ，获取所有蓝牙设备的位置信息
 *           getAllDevicePostionJson
 *      2：通过账号UID获取当前的客户群体ID
 *          getUidBoundJson
 *    （非必须）
 *      3:上传蓝牙签到信息
 *        pushBtDevicesPosition
 *      4上传GPS签到信息
 *        pushGpsPosition
 */

public class PttHttp {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    public  static  final  String  IP ="http://47.99.207.177";
    public  static  final  String  ADD_BT_SIGIN_URL=IP+"/api/patrol/addUserInfo";
    public  static  final  String  ADD_GPS_SIGIN_URL=IP+"/api/patrol/addSiginInfo";
    public  static  final  String  GET_BT_POSTION_URL=IP+"/api/patrol/getAllDeviceInfo";
    public  static  final  String  GET_UID_BOUND_ID_URL = IP+"/api/patrol/getMemberInfo";
    /***
     * 说明: 登陆后获取全部蓝牙设备对应的位置
     * httpParams  -> 需传入 customer_id  进行查询
     * return : 成功JSON  失败的JSON
     * **/
    public static  String  getAllDevicePostionJson( final Map<String ,String > httpParams)
    {
         return  request(GET_BT_POSTION_URL,httpParams);
    }
    /***
     * 说明: 登陆后获取当前用户ID或者用户名  对应的客户群体  以便后续其他接口操作
     * httpParams  -> 需传入 uid   进行查询
     * return : 成功JSON  失败的JSON
     * **/
    public static  String  getUidBoundJson( final Map<String ,String > httpParams)
    {
        return  request(GET_UID_BOUND_ID_URL,httpParams);
    }
    /***
     * 说明:  上传蓝牙签到信息
     * httpParams  ->
     *
     *           'device_id'  蓝牙设备ID
     *            'uid'      用户ID
     *            'position'   对应的位置
     *            'customer_id'   客户群体ID
     *            'signtime'   签到时间
     *
     *   return : 成功JSON  失败的JSON
     * **/
    public static  String pushBtDevicesPosition( final Map<String ,String > httpParams)
    {
         return   request(ADD_BT_SIGIN_URL,httpParams);
    }
    /***
     * 说明:  上传GPS信息
     * httpParams  ->
     *
     *             'uid'          用户ID
     *             'longitude'    经度
     *             'dimension'     纬度
     *            'customer_id'   客户群体ID
     *            'signtime'   签到时间
     *
     *   return : 成功JSON  失败的JSON
     * **/
    public static  String pushGpsPosition( final Map<String ,String > httpParams)
    {
         return   request(ADD_GPS_SIGIN_URL,httpParams);
    }


    /************************************************************http请求****************************************************************/
    public static String request(String urlpath,Map<String ,String> httpParams)
    {
        OkHttpClient okHttpClient = EasyHttp.getOkHttpClient ( ).newBuilder ()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).build ();
        try {
            //创建一个请求实体对象 RequestBody
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, getBody(httpParams));
            //创建一个请求
            final Request request =  new Request.Builder().url(urlpath).post(body).build();
            //创建一个Call
            final Call call = okHttpClient.newCall(request);
            //执行请求
            Response response = call.execute();
            //请求执行成功
            if (response.isSuccessful()) {
                //获取返回数据 可以是String，bytes ,byteStream
                String data = response.body().string();
                AndroidUtils.Log("response_data ----->" +data);
                return data;
            }else {
                return "失败"+response.body().string();
            }
        } catch (Exception e) {
            AndroidUtils.Log("response_fail ----->"+e.toString());
            return  "";
        }
    }

    public static  String  getBody(Map<String,String> map)
    {
        StringBuilder tempParams = new StringBuilder();
        int pos = 0;
        for (String key : map.keySet()) {
            if (pos > 0) {
                tempParams.append("&");
            }
            try {
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(map.get(key), "utf-8")));
            } catch ( UnsupportedEncodingException e ) {
                e.printStackTrace ( );
            }
            pos++;
        }
        return  tempParams.toString();
    }



    /**
     * okHttp post同步请求
     * @param actionUrl  接口地址
     * @param paramsMap   请求参数
     */
    public  static  void requestPostBySyn(String actionUrl, Map<String, String> paramsMap) {
        OkHttpClient okHttpClient  = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        try {
            //处理参数
            StringBuilder tempParams = new StringBuilder();
            AndroidUtils.Log("tempParams=="+tempParams);
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            //生成参数
            String params = tempParams.toString();
            Log.e ( "TAG","tempParams=>"+tempParams.toString () );

            //创建一个请求实体对象 RequestBody
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            //创建一个请求
            final Request request =  new Request.Builder().url(actionUrl).post(body).build();
            //创建一个Call
            final Call call = okHttpClient.newCall(request);
            //执行请求
            Response response = call.execute();
            //请求执行成功
            if (response.isSuccessful()) {
                //获取返回数据 可以是String，bytes ,byteStream
                Log.e("tttalk", "tttalk+response ----->" + response.body().string());
            }else {
                Log.e("tttalk", "tttalk+response_fail ----->" + response.body().string());
            }
        } catch (Exception e) {
            Log.e("TAG", e.toString());
        }
    }


}
