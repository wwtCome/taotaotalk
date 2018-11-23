package com.bugly.main;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.bugly.R;
import com.bugly.database.AllBthDeviceInfo;
import com.bugly.database.SqlUtils;
import com.bugly.signin.AlreadySingInBth;
import com.bugly.signin.DevicePostion;
import com.bugly.signin.MemberInfo;
import com.bugly.signin.PttHttp;
import com.bugly.signin.SingInResult;
import com.bugly.utils.AndroidUtils;
import com.bugly.utils.AppCommonUtil;
import com.bugly.utils.AppSettings;
import com.kylindev.pttlib.db.ChatMessageBean;
import com.kylindev.pttlib.service.InterpttService;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;

/**
 * Created by wuwentao on 2018/10/27.
 */

public class UserService extends Service{

    List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
    List<AlreadySingInBth> AlrSInBthList = new ArrayList<>();
    private Handler mHandler = new Handler();
    private List<ChatMessageBean> tblist = new ArrayList<ChatMessageBean>();
    public List<ChatMessageBean> getTblist() {
        return tblist;
    }
    ArrayList<AllBthDeviceInfo> deviceInfolist = new ArrayList<>();

    public int backVoiceIndex = -1;//语音回放到哪一条的记录

    public void setBackVoiceIndex(int backVoiceIndex) {
        this.backVoiceIndex = backVoiceIndex;
    }

    public int getBackVoiceIndex() {
        backVoiceIndex ++;
        if(backVoiceIndex >= 10 || backVoiceIndex >= tblist.size())
        {
            backVoiceIndex = 0;
        }
        AndroidUtils.Log("backVoiceIndex == "+ backVoiceIndex);
        return backVoiceIndex;
    }

    private int currentChanId = 0;
    private boolean needReload = false;
    private boolean isBound = false;
    DynamicReceiver dynamicReceiver;
    private boolean hasRegisteDynamicReceiver = false;
    private InterpttService interpttService;
    private ServiceConnection mServiceConnection = null;
    private Intent mServiceIntent = null;
    public  TextToSpeech tts;
    private ChannelActivity mainView;
    private IBinder mBinder=new MyBinder();
    private boolean isSearchReceiverRegeist;
    private SearchReceiver searchReceiver;
    private boolean isFirstStartService = true;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, new MyOnInitialListener());
        mServiceIntent = new Intent(this, InterpttService.class);
        initServiceConnection();
        if(AppSettings.getInstance(this).getAutoLaunch())
        {
            doBindService();
            AndroidUtils.Log("自动启动doBindService()");
        }
        AndroidUtils.Log("UserOnCreate");
    }

    public BluetoothAdapter mBluetoothAdapter;
    private void checkBth() {
        //获取BluetoothAdapter对象
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            AndroidUtils.showToast(this,getString(R.string.error_bluetooth_not_supported));
            return;
        }
        if(!mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.enable();
            AndroidUtils.Log("蓝牙未打开-------->去打开蓝牙");
        }
        searchReceiver = new SearchReceiver(UserService.this);
        //创建一个查找蓝牙设备的广播意图
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(ACTION_DISCOVERY_FINISHED);
        registerReceiver(searchReceiver, filter);
        isSearchReceiverRegeist = true;
        startSearch();
    }


    public void startSearch()
    {
        if(!mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.startDiscovery();
        }else{
            AndroidUtils.Log("正在扫描周围蓝牙");
        }
    }



    public void cleanBthList()
    {
        bluetoothDeviceList.clear();
    }

    public void addDevice(BluetoothDevice device)
    {
        String address = device.getAddress();
//        for(BluetoothDevice d : bluetoothDeviceList)
//        {
//            if(d.getAddress().equals(address))
//            {
//                AndroidUtils.Log("list中已经存在这个蓝牙设备了："+device.getName()+":"+device.getAddress());
//                AndroidUtils.Log("蓝牙设备："+device.getName()+":"+device.getAddress());
//                return;
//            }
//        }
//        bluetoothDeviceList.add(device);
        final List<AllBthDeviceInfo> allBthDeviceInfos = DataSupport.where("device_id = ?", address).find(AllBthDeviceInfo.class);
        if(allBthDeviceInfos.size() > 0)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AllBthDeviceInfo allBthDeviceInfo = allBthDeviceInfos.get(0);
                    AndroidUtils.Log("查询到数据库有对应设备，开始签到"+allBthDeviceInfo.getDevice_id());

                    long lonetime = AndroidUtils.getNetTime() / 1000;
                    String time = String.valueOf(lonetime);
                    String device_id = allBthDeviceInfo.getDevice_id();

                    for (int i = 0; i < AlrSInBthList.size();i++)
                    {
                        AlreadySingInBth alreadySingInBth = AlrSInBthList.get(i);
                        if(alreadySingInBth.getAddress().equals(device_id))
                        {
                            long time_alr = alreadySingInBth.getTime();
                            if((lonetime - time_alr) > 1800)
                            {
                                AlrSInBthList.remove(alreadySingInBth);
                                AndroidUtils.Log("本次签到的时间距离上次签到的时间大于30分钟可以签到"+(lonetime - time_alr)+"秒");
                                break;
                            }else{
                                AndroidUtils.Log("本次签到的时间距离上次签到的时间小于30分钟不能签到"+(lonetime - time_alr)+"秒");
                                return;
                            }
                        }
                    }

                    Map<String ,String > BtDevicesPositionParams = new HashMap<>();
//                    AndroidUtils.Log("device_id="+allBthDeviceInfo.getDevice_id()+",uid=="+AppSettings.getInstance(getApplicationContext()).getUserid()+
//                            ",position=="+ allBthDeviceInfo.getPosition()+",customer_id=="+allBthDeviceInfo.getCustomer_id()+
//                            ",sign_time=="+time);
                    BtDevicesPositionParams.put("device_id",device_id);
                    BtDevicesPositionParams.put("uid", AppSettings.getInstance(getApplicationContext()).getUserid());
                    BtDevicesPositionParams.put("position", allBthDeviceInfo.getPosition());
                    BtDevicesPositionParams.put("customer_id",allBthDeviceInfo.getCustomer_id());
                    BtDevicesPositionParams.put("sign_time",time);
                    String s = PttHttp.pushBtDevicesPosition(BtDevicesPositionParams);
//                    AndroidUtils.Log("签到结果"+s);
                    SingInResult singInResult = AndroidUtils.PraSingInResult(s);
                    if(singInResult.getCode() == 1)
                    {
                        voiceBroast(getString(R.string.sign_in_success),true);
                        //记录签到时间，然后下次搜索到这个蓝牙的时候，先查看，已经签到过的蓝牙list里是否有这
                        // 个蓝牙，签到的时间间隔是否满足了X分钟以上，如果满足了时间间隔，就再次签到
                        AlrSInBthList.add(new AlreadySingInBth(allBthDeviceInfo.getDevice_id(),lonetime));
                    }else {
                        voiceBroast(getString(R.string.sign_in_fail),true);
                    }
                }
            }).start();
        }else{
//            AndroidUtils.Log("无"+address+"对应设备");
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AndroidUtils.Log("UserOnStartCommand");
        return START_STICKY;
    }
    /**
     * user服务绑定成功之后
     * 再来开启SDK服务和绑定SDK服务
     */
    public void userBindOk()
    {
        if (!AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
            startService(mServiceIntent);
        }
        doBindService();
    }

    public InterpttService getInterpttService() {
        if(interpttService != null)return interpttService;else return null;
    }

    /**
     * 绑定service
     */
    void doBindService() {
        if (!isBound) {
            bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            isBound = true;
        }else {
            if(mainView != null)
            {
                AndroidUtils.Log("doBindService222222222222222222");
                mainView.setmServiceIntent(interpttService);
            }
        }
    }
    /**
     * 解绑service
     */
    void doUnbindService() {
        if (isBound) {
            unbindService(mServiceConnection);
            isBound = false;
        }
    }


    private void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InterpttService.LocalBinder localBinder = (InterpttService.LocalBinder) service;
                interpttService = localBinder.getService();
                if(mainView != null) {
                    AndroidUtils.Log("initServiceConnection111111111111111111111");
                    mainView.setmServiceIntent(interpttService);
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.button1Key");
                filter.addAction("android.intent.action.button2Key");
                dynamicReceiver = new DynamicReceiver(UserService.this,interpttService);
                registerReceiver(dynamicReceiver,filter);
                hasRegisteDynamicReceiver = true;
                taotaoqingqiu();
            }
            //此方法调用时机：This is called when the connection with the service has been unexpectedly disconnected
            //-- that is, its process crashed. Because it is running in our same process, we should never see this happen.
            @Override
            public void onServiceDisconnected(ComponentName name) {
                interpttService = null;
                if(mainView != null) {
                    AndroidUtils.Log("onServiceDisconnected00000000000000000000000");
                    mainView.setmServiceIntent(null);
                }
                mServiceConnection = null;
                stopService(mServiceIntent);
                //此函数只有在service被异常停止时才会调用，如被系统或其他软件强行停止
//                finish();
            }
        };
    }




    public void taotaoqingqiu()
    {
        new Thread()
        {
            @Override
            public void run ( ) {
                super.run ( );
                AndroidUtils.Log("请求");

                final Map<String ,String > uidParams =  new HashMap<>(  );
                String userid = AppSettings.getInstance(getApplicationContext()).getUserid();
                AndroidUtils.Log("请求userid=="+userid);
                uidParams.put("uid",userid);

                String uidBoundJson = PttHttp.getUidBoundJson(uidParams);
                AndroidUtils.Log("uidBoundJson数据" + uidBoundJson);
                MemberInfo memberInfo = AndroidUtils.PraMemberInfo(uidBoundJson);

                if(memberInfo.getCode() == 1 && memberInfo.getData().get(0).getCustomer_id() != null)
                {
                    Map<String ,String > customeridParams = new HashMap<>();
                    customeridParams.put("customer_id", memberInfo.getData().get(0).getCustomer_id());
                    String allDevicePostionJson = PttHttp.getAllDevicePostionJson(customeridParams);
                    AndroidUtils.Log("allDevicePostionJson数据" + allDevicePostionJson);
                    List<DevicePostion.DataBean> dataBeen = AndroidUtils.PraDevicePostion(allDevicePostionJson);
                    if(dataBeen != null)
                    {
                        deviceInfolist.clear();
                        for (DevicePostion.DataBean d : dataBeen)
                        {
                            AllBthDeviceInfo allBthDeviceInfo = new AllBthDeviceInfo();
                            allBthDeviceInfo.setId(d.getId());
                            allBthDeviceInfo.setDevice_id(d.getDevice_id());
                            allBthDeviceInfo.setPosition(d.getPosition());
                            allBthDeviceInfo.setCustomer_id(d.getCustomer_id());
                            allBthDeviceInfo.setCreatetime(d.getCreatetime());
                            allBthDeviceInfo.setUpdatetime(d.getUpdatetime());
                            deviceInfolist.add(allBthDeviceInfo);
                            AndroidUtils.Log(allBthDeviceInfo.toString()+'\'');
                        }
                        SqlUtils.deleteAll();
                        SqlUtils.addDevicePostionList(deviceInfolist);
                        checkBth();
                    }
                }
            }
        }.start ();
    }


    public void voiceBroast(String inf, boolean input){
        if(input)
        {
            tts.stop();
        }
        tts.speak(inf, TextToSpeech.QUEUE_FLUSH, null);
    }

    //tts
    class MyOnInitialListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                //String able = getResources().getConfiguration().locale.getCountry();
                tts.setEngineByPackageName("com.iflytek.vflynote");
                //  int result = tts.setLanguage(Locale.CHINESE);
                int result = tts.setLanguage(Locale.ENGLISH);
                if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE){
                    Toast.makeText(UserService.this, getString(R.string.tts_notsupport),Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Binder
     * @author corget
     */
    public class MyBinder extends Binder {
        UserService getService() {
            return UserService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 设置MainView
     * @param mv
     */
    public void setMainView(ChannelActivity mv) {
        mainView = mv;
    }

    /**
     * 加载当前频道的语音回放数据
     */
    public List<ChatMessageBean> loadMoreRecords(int cid) {
        if(cid == 0)
        {
            return null;
        }
        if(backVoiceIndex == tblist.size()-1 || currentChanId != cid)//避免在数据多的时候重复获取
        {
            AndroidUtils.Log("backVoiceIndex =="+backVoiceIndex + "==" + (tblist.size()-1) + "的时候，重新获取录音数据");
            tblist.clear();
            currentChanId = cid;
            backVoiceIndex = -1;
            List<ChatMessageBean> pagelist = interpttService.loadDBRecords(cid, 0, 10);
            if (pagelist != null && pagelist.size() != 0) {
                AndroidUtils.Log("pagelist.size()="+pagelist.size());
                tblist.addAll(0, pagelist);
                Collections.reverse(tblist);
                return tblist;
            } else {
                AndroidUtils.Log(getString(R.string.noRecording_data));
                voiceBroast(getString(R.string.noRecording_data),true);
                return null;
            }
        }else{
            return tblist;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //注销广播
        if(hasRegisteDynamicReceiver){
            unregisterReceiver(dynamicReceiver);
        }
        if(isSearchReceiverRegeist)
        {
            unregisterReceiver(searchReceiver);
        }
        if (tts!=null) {
            tts.stop();
            tts.shutdown();
        }
        doUnbindService();
        stopService(mServiceIntent);
    }
}
