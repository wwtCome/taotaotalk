package com.bugly.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bugly.R;
import com.bugly.utils.AndroidUtils;
import com.bugly.utils.AppCommonUtil;
import com.bugly.utils.AppSettings;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.utils.ServerProto;

import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{


    private EditText mETUserId, mETPassword;//账号密码框
    private String mUser, mPwd;//服务器地址，账号，密码
    private TextView  mTVVersion;//显示版本号
    private Button mBtnLogin;//登录按钮
    private InterpttService mService = null;//服务连接成功返回的对象
    private Intent mServiceIntent = null;
    private boolean mServiceBind = false;//服务绑定是否成功
    private boolean autoFinish = false;//是否登录成功自动结束
    //注册返回后，因为注册的connection会断开，从而使本界面显示“连接失败”，会困扰用户。
    // 加此标识以区分，只显示“登录”引起的disconnection
    private boolean showDisconnect = true;
    private TextView login_ip;//登录IP地址按钮
    /**
     * 绑定完成回调
     */
    private ServiceConnection mConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //之前在验证手机号时，按home，查看短信，再点击icon启动时，会重新显示本界面
        //加这个判断后，会直接显示按home时所在的界面
        if (!isTaskRoot()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_login);
        BuildInfo();
        login_ip = (TextView) findViewById(R.id.login_ip);
        login_ip.setOnClickListener(this);
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
        mETUserId = (EditText) findViewById(R.id.et_serverUsername);
        mETPassword = (EditText) findViewById(R.id.et_serverPassword);
        mTVVersion = (TextView) findViewById(R.id.tv_version);
        mTVVersion.setText("v"+AndroidUtils.getVerName(this));
        refreshLayout();

        mServiceIntent = new Intent(this, InterpttService.class);
        initServiceConn();

        //本activity有可能是已经开始工作，用户又点击app图标而启动的。此时，应先判断service是否已经运行。
        if(AppCommonUtil.isServiceRunning(this,INTERPTT_SERVICE)){
            //service正在运行。此时还不能直接跳转到channel里，因为此service有可能是之前登录失败了，而service还在。
            //因此，这里不jump，而是bindService。bind成功后，在serviceConnected里进行处理
            mServiceBind = bindService(mServiceIntent, mConnection, 0);
            AndroidUtils.Log("服务正在运行>>>>>>绑定服务");
        }else{
            //如果service未运行，表明是首次进入，则检查是否要自动登录
            AndroidUtils.Log("服务未运行");
            if(AppSettings.getInstance(this).getAutoLogin())
            {
                mUser = AppSettings.getInstance(this).getUserid();
                mPwd = AppSettings.getInstance(this).getPassword();
                AndroidUtils.Log("自动登录,mUser="+mUser+"mPwd="+mPwd);
//                if (AppCommonUtil.validUserId(mUser) && AppCommonUtil.validPwd(mPwd)) {
                startService(mServiceIntent);
                mServiceBind = bindService(mServiceIntent, mConnection, 0);
                //此后，在onServiceConnected里会开始mService.connect()。然后connected之后，会自动jumpToChannel
//                }
            }
        }

    }

    private void BuildInfo() {
        AndroidUtils.Log("Build.MODEL:"+Build.MODEL);
        //是否是Android6.0以上的系统
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {
            AndroidUtils.Log("6.0以上的系统");
            verifyStoragePermissions(this);
        }else{
            AndroidUtils.Log("6.0以下的系统");
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION" };

    public static void verifyStoragePermissions(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            AndroidUtils.Log("还没权限");
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }else {
            AndroidUtils.Log("有权限了");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                AndroidUtils.Log("LoginActivity," + "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }

    private void initServiceConn() {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                InterpttService.LocalBinder localBinder = (InterpttService.LocalBinder) service;
                mService = localBinder.getService();
                AndroidUtils.Log("LoginActivity绑定服务成功，mService==null?"+(mService==null));
                mService.registerObserver(serviceObserver);

                //有可能是已经连接成功过至少一次后，用户再次点击app图标进入的。应先检查是否已经连接成功过
                if (mService.isLoginOnceOK()) {
                    //如果已经连接成功过，即使现在是断开状态，也应立即进入对讲界面
                    AndroidUtils.Log("LoginActivity-jumpToChannel();");
                    jumpToChannel();
                } else if (mUser != null && mPwd != null) {
                    //说明进入此界面后，至少点击过一次"登陆"按钮
                    AndroidUtils.Log("LoginActivity- login();");
                    login();
                }
            }
            public void onServiceDisconnected(ComponentName className) {
                AndroidUtils.Log("LoginActivity绑定服务失败");
                mConnection = null;
                mService = null;
            }
        };
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            AndroidUtils.Log("message.what="+message.what);
            switch (message.what) {
                case 0:
                    AndroidUtils.showIdToast(LoginActivity.this, R.string.user_or_password_wrong);
                    mBtnLogin.setText(getString(R.string.login));
                    break;
                case 1:
                    AndroidUtils.showIdToast(LoginActivity.this, R.string.server_full);
                    mBtnLogin.setText(getString(R.string.login));
                    break;
                case 2:
                    AndroidUtils.showIdToast(LoginActivity.this, R.string.wrong_version);
                    mBtnLogin.setText(getString(R.string.login));
                    break;
                case 3:
                    AndroidUtils.showIdToast(LoginActivity.this, R.string.wrong_client_type);
                    mBtnLogin.setText(getString(R.string.login));
                    break;
                case 4:
//					AndroidUtils.showToast(LoginActivity.this, R.string.connect_fail_please_retry);
                    Toast t = Toast.makeText(LoginActivity.this, R.string.connect_fail_please_retry, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.BOTTOM, 0, 0);
                    t.show();
                    mBtnLogin.setText(getString(R.string.login));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     *接收service的广播
     */
    private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
        public void onConnectionStateChanged(InterpttService.ConnState state) {
            switch (state) {
                case CONNECTION_STATE_CONNECTING:
                    break;
                case CONNECTION_STATE_SYNCHRONIZING:
                    break;
                case CONNECTION_STATE_CONNECTED:
                    break;
                case CONNECTION_STATE_DISCONNECTED:
                    if (showDisconnect) {
                        Message msg = new Message();
                        msg.what = 4;
                        mHandler.sendMessage(msg);
                    }
                    break;
            }
        }

        @Override
        public void onRejected(ServerProto.Reject.RejectType type) {
            Message msg = new Message();

            switch (type) {
                case None:
                    //登录成功
                    jumpToChannel();
                    break;
                case InvalidUsername:
                case WrongUserPW:
                case AuthenticatorFail:
                    //杀掉service，无需再重试
                    if (mService != null) {
                        mService.stopSelf();
                    }
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    break;
                case ServerFull:
                    msg.what = 1;
                    mHandler.sendMessage(msg);
                    break;
                case WrongVersion:
                    msg.what = 2;
                    mHandler.sendMessage(msg);
                    break;
                case WrongClientType:
                    msg.what = 3;
                    mHandler.sendMessage(msg);
                    break;
                default:
                    break;
            }
        }
    };


    private void refreshLayout() {
        String id = AppSettings.getInstance(this).getUserid();
        String pwd = AppSettings.getInstance(this).getPassword();
        AndroidUtils.Log("refreshLayout,"+AppSettings.getInstance(this).getUserid()+":"+ AppSettings.getInstance(this).getPassword());
//        if (AppCommonUtil.validUserId(id) && AppCommonUtil.validPwd(pwd)) {
        mETUserId.setText(id);
        mETPassword.setText(pwd);
//        } else {
//            mETUserId.setText("");
//            mETPassword.setText("");
//        }

        mETUserId.setHint(R.string.personal_username_hint);
    }

    @Override
    protected void onDestroy() {
        //执行到onDestroy，有两种情况，一种是登录成功，自动finish()，一种是未登录成功，用户退出。
        //所以应判断登录是否成功，如果未成功，则应同时销毁service
        if (mService != null) {
            mService.unregisterObserver(serviceObserver);
            if (! autoFinish) {
                //如果不在线程里disconnect，ui会卡住
                //mService.disconnect();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mService.disconnect();
                    }
                }).start();

                mService.stopSelf();
                System.exit(0);	//增加，彻底退出
            }

            if (mConnection != null) {
                // Unbind to service
                if (mServiceBind) {
                    unbindService(mConnection);
                }
                mConnection = null;
            }

            mService = null;
        }

        super.onDestroy();
    }


    private void login() {
        //注：以下登录方式，应根据服务器类型选择一种，第三方一般使用企业版登录。
        //主要区别：个人版可以客户端创建和删除频道、管理成员、加入和退出频道等，企业版所有频道、用户、成员均由管理员在后台管理
        //企业版和个人版均可在客户端搜索和添加好友，并勾选若干个好友发起临时呼叫
        //使用时，请把第一个参数改为实际服务器地址，第二个参数：0-个人版；1-企业版

        //使用企业版
        String mHost = AppSettings.getInstance(this).getHost();
        if(mHost.isEmpty())
        {
            AndroidUtils.showIdToast(this,R.string.server_address_empty);
            return;
        }
        if (mUser.isEmpty() || mPwd.isEmpty())
        {
            return;
        }
        AndroidUtils.Log("mHost="+mHost);
        mBtnLogin.setText(getString(R.string.logging_in));
        mService.login(mHost, 1, mUser, mPwd);

        //使用个人版：
//		mService.login("192.168.0.7", 0, mUser, mPwd);
    }

    private void jumpToChannel() {
        Intent i = new Intent(LoginActivity.this, ChannelActivity.class);
        startActivity(i);
        autoFinish = true;
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id)
        {
            case R.id.login_ip:
                loginIP();
                break;
            case R.id.btn_login:
                showDisconnect = true;
                mUser = mETUserId.getText().toString();
                mPwd = mETPassword.getText().toString();

                AppSettings.getInstance(this).setUserid(mUser);
                AppSettings.getInstance(this).setPassword(mPwd);
                AndroidUtils.Log("mUser="+mUser+",mPwd="+mPwd);
                AndroidUtils.Log(AppSettings.getInstance(this).getUserid()+":"+  AppSettings.getInstance(this).getPassword());
                //用户可在此界面多次尝试登录
                if (! AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
                    startService(mServiceIntent);
                }

                if (mService == null) {
                    //如果之前没有启动并bind mService，则需先bind，在onServiceConnected里开始connect
                    if (mConnection == null) {
                        initServiceConn();
                    }

                    mServiceBind = bindService(mServiceIntent, mConnection, 0);
                }
                else {
                    //如果之前已经有mService，则可以直接开始connect
                    login();
                }
                break;
        }
    }
    private EditText et_ipaddress;
    /**
     * 添加IP地址
     */
    private void loginIP() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.address_ip);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.ip_address, null);
        builder.setView(layout);
        et_ipaddress = (EditText) layout.findViewById(R.id.et_ipaddress);
        et_ipaddress.setText(AppSettings.getInstance(getApplicationContext()).getHost());
        et_ipaddress.setSelection(et_ipaddress.getText().length());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String s = et_ipaddress.getText().toString();
                if(!s.isEmpty())
                {
                    AppSettings.getInstance(getApplicationContext()).setHost(s);
                    AndroidUtils.Log("确定，地址不为空，保存"+et_ipaddress.getText().toString());
                }

            }
        });
        //builder.setNegativeButton(R.string.close, null);

        builder.show();
    }

}
