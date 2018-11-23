package com.bugly.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bugly.database.SqlTestActivity;
import com.bugly.utils.AppConstants;
import com.bugly.R;
import com.bugly.utils.AndroidUtils;
import com.bugly.utils.AppCommonUtil;
import com.bugly.utils.AppSettings;
import com.bugly.view.ActionItem;
import com.bugly.view.CircleImageView;
import com.bugly.view.MyScrollScreen;
import com.bugly.view.TitlePopup;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttProtocolHandler.DisconnectReason;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.HandmicState;
import com.kylindev.pttlib.service.InterpttService.HeadsetState;
import com.kylindev.pttlib.service.InterpttService.MicState;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;
import java.util.Map;

import static com.kylindev.pttlib.service.InterpttService.ConnState;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTING;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_DISCONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_SYNCHRONIZING;

public class ChannelActivity extends Activity implements OnClickListener, MyScrollScreen.OnScreenChangeListener {

	private Intent userServiceIntent = null;
	private boolean isBound = false;

	private InterpttService mService;
	private UserService userService;
	private Handler mHandler = new Handler();	//用于其他线程更新UI
	private boolean appWantQuit = false;

	private CircleImageView mCIVAvatarOfSearchedUser;
	private TextView mTVNickOfSearchedUser;
	private TextView mTVIdOfSearchedUser;
	private Button mBtnAddContact;
	private User searchedUser;

	//scroll screen
	private MyScrollScreen mScreen;
	private ContactViewManager mContactViewManager;
	private ChannelViewManager mChannelViewManager;
	private LinearLayout mLLTabBg;

	private int currIndex;
	private int position_left;
	private int position_right;
	private LinearLayout llTabContact, llTabChannel;

	private AlertDialog mJoinChannelDialog = null;

	private Button bt_initTmp;
	private LinearLayout Lin_creat_channel;
	private TextView title;
	private ImageView iv_con_option,iv_app_option,iv_set;

	private ImageView mIVPttCirc, mIVPtt;

	void voiceBroast(String info,boolean stop)
	{
		if(userService != null)
		{
			userService.voiceBroast(info,stop);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		doBindService();
	}

	@Override
	protected void onStop() {
		super.onStop();
		doUnbindService();
	}

	/**
	 * 绑定service
	 */
	void doBindService() {
		if (!isBound) {
			bindService(userServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
			isBound = true;
		}
	}
	/**
	 * 解绑service
	 */
	void doUnbindService() {
		if (isBound) {
			unbindService(mConnection);
			isBound = false;
		}
	}


	/**
	 * 绑定完成回调*
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			userService = ((UserService.MyBinder) binder).getService();
			userService.setMainView(ChannelActivity.this);
			userService.userBindOk();
			AndroidUtils.Log("userService绑定成功");
		}
		public void onServiceDisconnected(ComponentName className) {
		}
	};

	/**
	 * 在服务里面绑定SDK服务，绑定成功后传输
	 * @param interpttService
	 */
	public void setmServiceIntent(InterpttService interpttService)
	{
		AndroidUtils.Log("setmServiceIntent在服务里面绑定SDK服务，绑定成功后传输");
		mService = interpttService;
		if(mService == null)
		{
			return;
		}
		mContactViewManager.setService(mService);
		mChannelViewManager.setService(mService);
		mService.registerObserver(serviceObserver);

		//设置提示音量
		int volIBegin = AppSettings.getInstance(ChannelActivity.this).getVolumeIBegin();
		int volIEnd = AppSettings.getInstance(ChannelActivity.this).getVolumeIEnd();
		int volOtherBegin = AppSettings.getInstance(ChannelActivity.this).getVolumeOtherBegin();
		int volOtherEnd = AppSettings.getInstance(ChannelActivity.this).getVolumeOtherEnd();
		mService.setVolumeTalkroomBegin(volIBegin);
		mService.setVolumeTalkroomEnd(volIEnd);
		mService.setVolumeOtherBegin(volOtherBegin);
		mService.setVolumeOtherEnd(volOtherEnd);

		mService.activityShowing(true);

		//channel界面出现过一次后，才设置notification对应的activity
		Intent notifIntent = new Intent(getApplicationContext(), ChannelActivity.class);
		mService.setNotifIntent(notifIntent);

		//此时有可能是服务器断开状态，且用户点击app图标执行到这里的。因此，应先检查service的状态是否是CONNECTION_STATE_CONNECTED
		//是的话，才能执行setupChannelList等操作。否则，无需setup，只需等待自动重连后，自动setup
		if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
			mContactViewManager.setupList();
			mChannelViewManager.setupList();
		}

		//显示哪个tab
		currIndex = mService.getScreenIndex();	//刚启动时onScreenChanged不响应，原因不明。这里手动赋值
		mScreen.setToScreen(currIndex);
		if (currIndex == 1) {

			title.setText(getString(R.string.channel));
			iv_app_option.setVisibility(View.VISIBLE);
			iv_con_option.setVisibility(View.INVISIBLE);

			TranslateAnimation animation = new TranslateAnimation(position_left, position_right, 0, 0);
			animation.setFillAfter(true);
			animation.setDuration(1);
			mLLTabBg.startAnimation(animation);
		}else {
			title.setText(getString(R.string.contact));
			iv_app_option.setVisibility(View.INVISIBLE);
			iv_con_option.setVisibility(View.VISIBLE);
		}
		refreshMicState(mService.getMicState());
		showConnection();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channel);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		InitWidth();
		InitTab();
		InitViewPager();
		iv_con_option = (ImageView) findViewById(R.id.iv_con_option);
		iv_app_option = (ImageView) findViewById(R.id.iv_app_option);
		iv_set = (ImageView) findViewById(R.id.iv_set);
		iv_con_option.setOnClickListener(this);
		iv_app_option.setOnClickListener(this);
		iv_set.setOnClickListener(this);

		userServiceIntent = new Intent(this, UserService.class);
		if (!AppCommonUtil.isServiceRunning(this, "com.bugly.main.UserService")) {
			AndroidUtils.Log("userService未启动");
			startService(userServiceIntent);
		}
		doBindService();

		mIVPtt = (ImageView) findViewById(R.id.iv_ptt);
		mIVPttCirc = (ImageView) findViewById(R.id.iv_ptt_circle);
		FrameLayout flPtt = (FrameLayout) findViewById(R.id.fl_ptt);
		flPtt.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (mService == null) {
					return false;
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mService.userPressDown(false);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mService.userPressUp("31");
				} else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
					mService.userPressUp("32");
				}
				return true; // We return true so that the selector that changes the background does not fire.
			}
		});


		//首次运行，显示帮助信息
		boolean firstUse = AppSettings.getInstance(this).getFirstUse();
		if (firstUse) {
			//help();
			AppSettings.getInstance(this).setFirstUse(false);
		}
	}

	public void totest(View V)
	{
//		startActivity(new Intent(this, SqlTestActivity.class));
	}

	private void refreshMicState(MicState state) {
		boolean ready = true;
		if (mService == null) {
			ready = false;
			AndroidUtils.Log("mService == null");
		} else if (mService.getConnectionState() != CONNECTION_STATE_CONNECTED) {
			AndroidUtils.Log("mService.getConnectionState() != CONNECTION_STATE_CONNECTED");
			ready = false;
		} else  {
			Channel c = mService.getCurrentChannel();
			if (c == null || c.id == 0) {
				ready = false;
				AndroidUtils.Log("c == null || c.id == 0");
			}
		}

		if (!ready) {
			mIVPtt.setImageResource(R.drawable.ic_ptt_up);
			mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_noready);
		} else {
			switch (state) {
				case MIC_READY:
					mIVPtt.setImageResource(R.drawable.ic_ptt_up);
					mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_ready);
					break;
				case MIC_GIVINGBACK:
					break;
				case MIC_APPLYING:
					//注意：申请期间，按键是按下状态，模仿真实按钮
					mIVPtt.setImageResource(R.drawable.ic_ptt_down);
					mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_applying);
					break;
				case MIC_OPENING_SCO:
					mIVPtt.setImageResource(R.drawable.ic_ptt_down);
					mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_opening_sco);
					break;
				case MIC_TALKING:
					mIVPtt.setImageResource(R.drawable.ic_ptt_down);
					mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_talking);
					break;
			}
		}
	}

	ImageView hhh;
	private void InitWidth() {
		hhh = (ImageView) findViewById(R.id.hhh);

		mLLTabBg = (LinearLayout) findViewById(R.id.ll_tab_bg);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		//物理像素
		int screenW = dm.widthPixels;
		int screenH = dm.heightPixels;
		AndroidUtils.Log("screenW="+screenW+",screenH="+screenH);
		position_left = 0;
		position_right = screenW*2/3;

		//设置ivTabBackGround的宽度为屏幕宽度�?/3
		LayoutParams para;
		para = mLLTabBg.getLayoutParams();
		para.width = screenW / 3;	//设置比tab略小
		mLLTabBg.setLayoutParams(para);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		int width = hhh.getWidth();
		int height = hhh.getHeight();
		AndroidUtils.Log("width="+width+",height="+height);
	}

	private void InitTab() {
		llTabContact = (LinearLayout) findViewById(R.id.ll_tab_contact);
		llTabChannel = (LinearLayout) findViewById(R.id.ll_tab_channel);

		llTabContact.setOnClickListener(this);
		llTabChannel.setOnClickListener(this);

		title = (TextView) findViewById(R.id.title);
	}

	private void help(String connectstateinfo) {
		if(appWantQuit)
		{
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.app_help, null);
		TextView tv_info = (TextView) layout.findViewById(R.id.tv_connect_state);
		tv_info.setText(connectstateinfo);
		tv_info.setTextColor(getResources().getColor(R.color.holo_red_dark));
		builder.setTitle(R.string.notif);

		builder.setView(layout);
		builder.setPositiveButton(R.string.ok, null);
		builder.show();
	}

	private void quit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.confirm_quit_app, null);
		builder.setTitle(R.string.quit_app);
		builder.setView(layout);

		final CheckBox cb = (CheckBox) layout.findViewById(R.id.cb_auto_login);
		boolean autoLogin = AppSettings.getInstance(this).getAutoLogin();
		cb.setChecked(autoLogin);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				cb.setChecked(arg1);
				AppSettings.getInstance(ChannelActivity.this).setAutoLogin(arg1);
			}
		});

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				doQuit();
			}
		});

		builder.setNegativeButton(R.string.cancel, null);

		builder.show();
	}
	private void doQuit() {
		if(userService != null) {
			stopService(userServiceIntent);
		}
		if (mService != null) {
			voiceBroast(getString(R.string.quit),true);
			//记录用户意图停止
			mService.appWantQuit();
			appWantQuit = true;    //表示app要退出了，在disconnected回调中进行判断
			//首先停止音频，因为当前可能正在讲话
			mService.userPressUp("37");
			//先断开连接。此时可能已经处于断开状态了，若已断开，则无需再次调用断开，但需要停止重连
			ConnState connState = mService.getConnectionState();
			//加上connecting判断，否则在3g连接、然后连上一个实际不能上网的wifi时，无法退出，因为mService.disconnect超时
			if (connState == CONNECTION_STATE_DISCONNECTED ||
					connState == CONNECTION_STATE_CONNECTING) {
				//若已断开，无需先调用disconnect，再等待disconnected回调，直接退出即可
				bye();
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						mService.disconnect();
					}
				}).start();
			}
		} else {
			bye();
		}
	}

	/**
	 * 创建临时群组
	 */
	public void initTmp()
	{
		if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
			String members =  mService.getSelects();
			if (members.length() > 0) {
				//lenth>0说明选中了至少一个人。先加上自己
				members += ",";
				members += mService.getCurrentUser().iId;

				mService.createChannel("", "", members, false, false,true);
				//自动切换到频道界面
				mScreen.setToScreen(1);
			}
		}
		mService.cancelSelect();
	}


	/**
	 * 创建常用群组
	 */
	public void initNormal()
	{
		if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
			AndroidUtils.Log("创建常用群组");
			voiceBroast(getString(R.string.create_channel),true);
			mService.createChannel(getString(R.string.normal_call), "", null, true, false,false);
			//自动切换到频道界面
			mScreen.setToScreen(1);
		}
	}

	/**
	 * 加入群组
	 */
	public void joinChannel() {
		if (mService == null) {
			mService = userService.getInterpttService();
			return;
		}
		if (mService.getConnectionState()==CONNECTION_STATE_DISCONNECTED) {
			return;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.join_channel);
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.join_channel, null);
		builder.setView(layout);
		final EditText etChannel = (EditText) layout.findViewById(R.id.et_join_channel_id);
		final EditText etPwd = (EditText) layout.findViewById(R.id.et_join_channel_pwd);
		final Button btnJoinCustomChan = (Button) layout.findViewById(R.id.btn_join_custom_chan);
		final Button btnJoinExpChan = (Button) layout.findViewById(R.id.btn_join_exp_chan);
		final Button btnJoinMotorChan = (Button) layout.findViewById(R.id.btn_join_motor_chan);
		final Button btnJoinHamChan = (Button) layout.findViewById(R.id.btn_join_ham_chan);
		final Button btnJoinCarChan = (Button) layout.findViewById(R.id.btn_join_car_chan);

		btnJoinCustomChan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String str = etChannel.getText().toString();
				final String pwd = etPwd.getText().toString();
				if (!AppCommonUtil.validChannelId(str)) {
					AndroidUtils.showIdToast(ChannelActivity.this, R.string.channel_id_bad_format);
				} else if (etPwd.length() != 0 && !AppCommonUtil.validChannelPwd(pwd)) {
					AndroidUtils.showIdToast(ChannelActivity.this, R.string.channel_pwd_bad_format);
				} else {
					if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
						int id = Integer.parseInt(str);
						mService.joinChannel(id, pwd, "");
					}
				}

				if (mJoinChannelDialog != null) {
					mJoinChannelDialog.dismiss();
				}
			}
		});

		btnJoinExpChan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
					mService.joinChannel(AppConstants.EXP_CHANNEL_ID, "","");
				}
			}
		});
		btnJoinMotorChan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
					mService.joinChannel(AppConstants.MOTOR_CHANNEL_ID, "","");
				}
			}
		});

		btnJoinHamChan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
					mService.joinChannel(AppConstants.HAM_CHANNEL_ID, "","");
				}
			}
		});

		btnJoinCarChan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
					mService.joinChannel(AppConstants.CAR_CHANNEL_ID, "","");
				}

			}
		});

		mJoinChannelDialog = builder.show();
	}

	private void InitViewPager() {
		mScreen = (MyScrollScreen) findViewById(R.id.scroll_screen);
		mContactViewManager = new ContactViewManager(this);
		mChannelViewManager = new ChannelViewManager(this);

		LayoutInflater inflater = LayoutInflater.from(this);
		mScreen.addScreen(mContactViewManager.createView(inflater, null));
		mScreen.addScreen(mChannelViewManager.createView(inflater, null));

		mScreen.setOnScreenChangedListener(this);

		bt_initTmp = (Button) findViewById(R.id.bt_initTmp);
		bt_initTmp.setOnClickListener(this);
		Lin_creat_channel = (LinearLayout) findViewById(R.id.Lin_creat_channel);
	}

	private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
		//与服务器之间的连接状态改变了，包括掉线、上线、正在同步等
		public void onConnectionStateChanged(ConnState state) {
			switch (state) {
				case CONNECTION_STATE_CONNECTING:
					break;
				case CONNECTION_STATE_SYNCHRONIZING:
					break;
				case CONNECTION_STATE_CONNECTED:
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mContactViewManager.setupList();
							mChannelViewManager.setupList();
						}
					});
					break;
				case CONNECTION_STATE_DISCONNECTED:
					AndroidUtils.Log("CONNECTION_STATE_DISCONNECTED--appWantQuit = "+appWantQuit);
					if (appWantQuit) {
						bye();
					} else {
						//setnull不能省，否则断开后继续显示adapter的getview，其中getcurrentuser等得到null，崩溃
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								mContactViewManager.setListAdapter(null);
								mChannelViewManager.setListAdapter(null);
							}
						});
					}
					break;
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mService != null) {
						refreshMicState(mService.getMicState());
						showConnection();
					}
				}
			});

			if (mService != null) {
				mService.userPressUp("35");
			}
		}

		//服务器拒绝了某操作，例如试图进入无权限的频道、试图创建超过3个频道等
		@Override
		public void onPermissionDenied(String reason, int denyType) {
			AndroidUtils.showToast(ChannelActivity.this, AppConstants.permReason[denyType]);
		}

		//自己所在的频道改变了。例如之前在频道1，现在进入了频道2
		@Override
		public void onCurrentChannelChanged() {
			if (mService == null) {
				return;
			}

			if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
				mChannelViewManager.handleCurrentChannelChanged();
				//如果按着ptt时切换频道，则需先放弃讲话
				refreshMicState(mService.getMicState());
				mService.userPressUp("36");
			}
		}

		//服务器下发了新的、本地用户可进入的频道：
		@Override
		public void onChannelAdded(Channel channel) {
			mChannelViewManager.handleChannelAdded(channel);
		}


		//自己的资料改变了，如昵称、头像
		@Override
		public void onCurrentUserUpdated() throws RemoteException {
		}

		//频道移除了。移除可能是创建者删除了频道，或者自己退出了该频道，不再是该频道的成员，所以本地无需再显示了
		@Override
		public void onChannelRemoved(Channel channel) {
			mChannelViewManager.handleChannelRemoved(channel);
		}

		//某个频道信息有变化
		@Override
		public void onChannelUpdated(Channel channel) {
			mChannelViewManager.handleChannelUpdated(channel);
		}

		//某个本地可见的用户资料改变了
		@Override
		public void onUserUpdated(User user) {
			updateUser(user);
		}

		//某个本地可见的用户的讲话状态改变了。状态有两种，在讲话，不在讲话
		@Override
		public void onUserTalkingChanged(User user, boolean talk) {
			//刷新频道红点
			//updateUser(user);
			mChannelViewManager.updateChannelList();	//如果只刷新user，则频道上的talker指示不能刷新
		}

		//某个本地用户的讲话状态改变了。与上一接口的区别在于，onUserTalkingChanged根据服务器同步下来的信息判断，
		// onLocalUserTalkingChanged根据本地是否真正有该用户的声音正在播放判断。
		// 如果跟该user在同一频道，则应根据onLocalUserTalkingChanged判断，更加准确、实时。
		@Override
		public void onLocalUserTalkingChanged(User user, boolean talk) {
			//刷新频道红点
			mChannelViewManager.updateChannelList();	//如果只刷新user，则频道上的talker指示不能刷新
			if(user != null)
			{
				AndroidUtils.Log("开始讲话");
				AndroidUtils.Log("name="+user.name+":nick="+user.nick+":"+user.iId+":isLocalTalking="+user.isLocalTalking+":isTalking="+user.isTalking);
			}else {
				AndroidUtils.Log("user != null停止讲话");
			}
		}

		//本地播放的实时音量数据。如果不显示，可忽略此接口
		@Override
		public void onNewVolumeData(short volume) {
		}

		/**
		 *话筒状态变化。注意，线控、蓝牙触发对讲时，由service直接处理。
		 *并通过此回调，告知界面此时的话筒状态，包括空闲、申请中、讲话中、归还中
		 */
		@Override
		public void onMicStateChanged(MicState s) {
			AndroidUtils.Log("申请讲话回调"+s.name());
			refreshMicState(s);
		}

		//蓝牙音频连接状态
		@Override
		public void onHeadsetStateChanged(HeadsetState s) {
		}

		//蓝牙sco连接状态
		@Override
		public void onScoStateChanged(int s) {
		}

		//滔滔手咪连接状态。这个是BLE协议的连接
		@Override
		public void onTargetHandmicStateChanged(BluetoothDevice device, HandmicState s) {
		}

		//讲话计时
		@Override
		public void onTalkingTimerTick(int seconds) {
		}

		@Override
		public void onTalkingTimerCanceled() {
		}

		@Override
		public void onUserAdded(final User u) {
			mChannelViewManager.updateChannelList();
		}

		@Override
		public void onUserRemoved(final User user) {
			mChannelViewManager.updateChannelList();
		}

		//正在搜索手咪
		@Override
		public void onLeDeviceScanStarted(boolean start) {
		}

		@Override
		public void onShowToast(final String str) {
			AndroidUtils.showToast(ChannelActivity.this, str);
		}

		//搜索到手咪
		@Override
		public void onLeDeviceFound(BluetoothDevice bluetoothDevice) throws RemoteException {
		}

		@Override
		public void onInvited(final Channel chan) {
			//mayNotifyInvitation();
		}

		@Override
		public void onUserSearched(User user) {
			searchedUser = user;

			if (user.avatar != null) {
				Bitmap bm = BitmapFactory.decodeByteArray(user.avatar, 0, user.avatar.length);
				mCIVAvatarOfSearchedUser.setImageBitmap(bm);
			}
			mCIVAvatarOfSearchedUser.setVisibility(View.VISIBLE);
			mTVNickOfSearchedUser.setText(user.nick);
			mTVNickOfSearchedUser.setVisibility(View.VISIBLE);
			mTVIdOfSearchedUser.setText(String.valueOf(user.iId));
			mTVIdOfSearchedUser.setVisibility(View.VISIBLE);

			mBtnAddContact.setVisibility(View.VISIBLE);
		}

		@Override
		public void onApplyContactReceived(boolean add, Contact contact) {
			mContactViewManager.updateContactList();
		}

		@Override
		public void onPendingContactChanged() {
			mContactViewManager.updateContactList();
		}

		@Override
		public void onContactChanged() {
			mContactViewManager.updateContactList();
			refreshTmpTalk();
		}

		@Override
		public void onSynced() {
			mContactViewManager.updateContactList();
			mChannelViewManager.updateChannelList();
		}

		@Override
		public void onVoiceToggleChanged(boolean on) {
		}

		@Override
		public void onListenChanged(boolean listen) {
			mChannelViewManager.updateChannelList();
			if (listen) {
				AndroidUtils.showIdToast(ChannelActivity.this, R.string.listen_ok);
			}
		}
	};




	//遍历contact，确定是否需要显示临时呼叫按钮
	private void  refreshTmpTalk() {
		boolean hasSelected = false;
		if (mService == null) {
			return;
		}
		Map<Integer, Contact> m = mService.getContacts();
		if (m == null) {
			return;
		}
		for (Contact c:m.values()) {
			if (c.selected) {
				hasSelected = true;
			}

			Lin_creat_channel.setVisibility(hasSelected ? View.VISIBLE : View.GONE);
		}
	}

	private void showConnection() {
		ConnState connState = mService.getConnectionState();
		if (connState == CONNECTION_STATE_CONNECTED) {
//			mTVConnect.setVisibility(View.GONE);
		}
		else if (connState == CONNECTION_STATE_CONNECTING || connState == CONNECTION_STATE_SYNCHRONIZING) {
//			help(getString(R.string.syncing));
			if(!appWantQuit) {
				AndroidUtils.showIdToast(this, R.string.syncing);
			}
		}
		else {
			//此时可能是暂时断线，也可能是其他设备登录而被踢掉
			if (mService.getDisconnectReason() == DisconnectReason.Kick) {
//				help(getString(R.string.be_kicked));
				AndroidUtils.showIdToast(this,R.string.be_kicked);
				AndroidUtils.Log(getString(R.string.be_kicked));
				voiceBroast(getString(R.string.be_kicked),true);
			} else {
				if(!appWantQuit)
				{
					AndroidUtils.showIdToast(this,R.string.net_fail_retry);
				}
			}
		}
	}

	private void bye() {
		finish();
	}

	private void updateUser(User user) {
		if (user == null) {
			return;
		}

		mChannelViewManager.updateUser(user);
		//可能是自己修改里头像或昵称引起的
		if (mService != null && mService.getCurrentUser() != null && user.iId == mService.getCurrentUser().iId) {
			mContactViewManager.updateContactList();	//用于刷新自己的显示。其他人修改时，会收到contactChanged。自己修改资料时，不会收到
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode == KeyEvent.KEYCODE_DPAD_UP)
		{
			//上键
		}else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
			//下键
		}else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
			//左键
			if(mScreen.getCurrentScreen() == 1)
			{
				mScreen.setToScreen(0);
			}
		}else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
			//右键
			if(mScreen.getCurrentScreen() == 0)
			{
				mScreen.setToScreen(1);
			}
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			boolean selecting = mService!=null && mService.isSelectingContact();
			if (selecting) {
				mService.cancelSelect();
				return true;
			}
		}
		else {
			int savedCode = mService.getPttKeycode();
			if (savedCode!=0 && savedCode==keyCode) {
				//至此，说明需要响应ptt事件
				if (mService != null) {
					mService.userPressDown(false);
				}
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	protected void onDestroy() {
		doUnbindService();
		if (mService != null) {
			mService.unregisterObserver(serviceObserver);
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id)
		{
			case R.id.bt_initTmp:
				initTmp();
				break;
			case R.id.iv_set:
				final TitlePopup optionSetPopup = new TitlePopup(this, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				optionSetPopup.addAction(new ActionItem(this, getString(R.string.settings), R.drawable.setting));
				optionSetPopup.addAction(new ActionItem(this, getString(R.string.quit), R.drawable.ic_quit));

				optionSetPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
					@Override
					public void onItemClick(ActionItem item, int position) {
						switch (position) {
							case 0:
								settings();
								break;
							case 1:
								quit();
								break;
							default:
								break;
						}
					}
				});
				optionSetPopup.show(v);
				break;
			case R.id.iv_con_option:
				final TitlePopup optionConPopup = new TitlePopup(this, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				optionConPopup.addAction(new ActionItem(this, getString(R.string.add_contact), R.drawable.ic_add_contact));

				optionConPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
					@Override
					public void onItemClick(ActionItem item, int position) {
						switch (position) {
							case 0:
								addContact();
								break;
							default:
								break;
						}
					}
				});
				optionConPopup.show(v);
				break;
			case R.id.iv_app_option:
				final TitlePopup optionPopup = new TitlePopup(this, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				optionPopup.addAction(new ActionItem(this, getString(R.string.join_channel), R.drawable.join_channel));
				optionPopup.addAction(new ActionItem(this, getString(R.string.create_channel), R.drawable.create_channel));

				optionPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
					@Override
					public void onItemClick(ActionItem item, int position) {
						switch (position) {
							case 0:
								joinChannel();
								break;
							case 1:
								initNormal();
								break;
							default:
								break;
						}
					}
				});
				optionPopup.show(v);
				break;
			case R.id.ll_tab_contact:
				mScreen.setToScreen(0);
				break;
			case R.id.ll_tab_channel:
				mScreen.setToScreen(1);
				break;
		}
	}

	/**
	 * 添加好友
	 */
	private void addContact() {
		if (mService == null) {
			mService = userService.getInterpttService();
			return;
		}
		if (mService.getConnectionState()==CONNECTION_STATE_DISCONNECTED) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.add_contact);
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.add_contact, null);
		builder.setView(layout);

		mCIVAvatarOfSearchedUser = (CircleImageView) layout.findViewById(R.id.civ_searched_user_avatar);
		mTVNickOfSearchedUser = (TextView) layout.findViewById(R.id.tv_searched_user_nick);
		mTVIdOfSearchedUser = (TextView) layout.findViewById(R.id.tv_searched_user_id);
		mBtnAddContact = (Button) layout.findViewById(R.id.btn_add_contact);
		mBtnAddContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchedUser != null) {
					if (searchedUser.iId == mService.getCurrentUser().iId) {
						AndroidUtils.showToast(ChannelActivity.this, getString(R.string.cant_add_yourself));
					}
					else {
						mService.applyContact(true, searchedUser.iId);
						AndroidUtils.showToast(ChannelActivity.this, "已发送申请");
					}
				}
			}
		});

		final EditText etUser = (EditText) layout.findViewById(R.id.et_search_contact);
		final ImageButton ibSearch = (ImageButton) layout.findViewById(R.id.ib_search_contact);
		ibSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String str = etUser.getText().toString();
				if (AppCommonUtil.validUserId(str)) {
					mCIVAvatarOfSearchedUser.setVisibility(View.INVISIBLE);
					mTVNickOfSearchedUser.setVisibility(View.INVISIBLE);
					mTVIdOfSearchedUser.setVisibility(View.INVISIBLE);
					mBtnAddContact.setVisibility(View.INVISIBLE);
					searchedUser = null;

					mService.searchUser(str);
					AndroidUtils.Log("搜索的id正常，显示对应的用户");
				} else {
					AndroidUtils.showIdToast(ChannelActivity.this, R.string.userid_bad_format);
				}
			}
		});

		builder.setNegativeButton(R.string.close, null);

		builder.show();
	}

	private void getFocus(View v)
	{
		v.setFocusableInTouchMode(true);
		v.setFocusable(true);
		v.requestFocus();
	}

	private void settings() {
		AlertDialog.Builder buildertol = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.settings, null);

//		builder.setTitle(R.string.settings);
		buildertol.setView(view);

		final ImageView ivFloat = (ImageView) view.findViewById(R.id.iv_float_btn);
		boolean check = mService.getFloatWindow();
		ivFloat.setImageResource(check ? R.drawable.checkbox_on : R.drawable.checkbox_off);

		final LinearLayout llFloatBtn = (LinearLayout) view.findViewById(R.id.ll_float_btn);
		getFocus(llFloatBtn);
		llFloatBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean oldChecked = mService.getFloatWindow();
				boolean now = !oldChecked;
				mService.setFloatWindow(now);
				ivFloat.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
				if (now) {
					AndroidUtils.showIdToast(ChannelActivity.this, R.string.confirm_float_permission);
				}
			}
		});



		final ImageView ivAutoLaunch = (ImageView) view.findViewById(R.id.iv_auto_launch);
		boolean c = AppSettings.getInstance(this).getAutoLaunch();
		ivAutoLaunch.setImageResource(c ? R.drawable.checkbox_on : R.drawable.checkbox_off);

		final LinearLayout llAutoLaunch = (LinearLayout) view.findViewById(R.id.ll_auto_launch);
		llAutoLaunch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean old = AppSettings.getInstance(ChannelActivity.this).getAutoLaunch();
				boolean n = !old;
				AppSettings.getInstance(ChannelActivity.this).setAutoLaunch(n);
				ivAutoLaunch.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
			}
		});

		//提示音
		final LinearLayout llNotif = (LinearLayout) view.findViewById(R.id.ll_notification);
		final LinearLayout ll_language = (LinearLayout) view.findViewById(R.id.ll_language);
		llNotif.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				notifSetting();	//关于音量的设置
			}
		});
		ll_language.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				languageSetting();	//关于语言的设置
			}
		});

		//耳机按键支持
		final ImageView ivHeadset = (ImageView) view.findViewById(R.id.iv_headset_key);
		boolean bt = mService.getSupportHeadsetKey();
		ivHeadset.setImageResource(bt ? R.drawable.checkbox_on : R.drawable.checkbox_off);

		final LinearLayout llHeadset = (LinearLayout) view.findViewById(R.id.ll_headset_key);
		llHeadset.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean oldChecked = mService.getSupportHeadsetKey();
				boolean now = !oldChecked;
				mService.setSupportHeadsetKey(now);
				ivHeadset.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
			}
		});
		final ImageView ivHeadsetHelp = (ImageView) view.findViewById(R.id.iv_headset_help);
		ivHeadsetHelp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(ChannelActivity.this)
						.setMessage(R.string.headset_help)
						.setPositiveButton(R.string.ok, null)
						.show();
			}
		});

		//PTT按键定义
		final LinearLayout llPttKey = (LinearLayout) view.findViewById(R.id.ll_ptt_key);
		final TextView tvCode = (TextView) view.findViewById(R.id.tv_ptt_keycode);
		int code = mService.getPttKeycode();
		tvCode.setText(String.valueOf(code));

		llPttKey.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builderptt = new AlertDialog.Builder(ChannelActivity.this);
				LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
				View layout = inflater.inflate(R.layout.set_ptt_key, null);
				final EditText etKeycode = (EditText) layout.findViewById(R.id.et_keycode);
				Button btnDelete = (Button) layout.findViewById(R.id.btn_delete_keycode);
				LinearLayout llBroadcast = (LinearLayout) layout.findViewById(R.id.ll_set_ptt_broadcast);
				btnDelete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						etKeycode.setText(String.valueOf(0));
					}
				});

				llBroadcast.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
						LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
						View layout = inflater.inflate(R.layout.set_ptt_broadcast, null);
						final EditText etDown = (EditText) layout.findViewById(R.id.et_broadcast_down);
						final EditText etUp = (EditText) layout.findViewById(R.id.et_broadcast_up);
						String savedDown = mService.getBroadcastDown();
						String savedUp = mService.getBroadcastUp();
						etDown.setText(savedDown);
						etUp.setText(savedUp);

						builder.setView(layout);
						builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String strDown = etDown.getText().toString();
								String strUp = etUp.getText().toString();
								if (strDown==null || strUp== null || strDown.length()==0 || strUp.length()==0) {
									AndroidUtils.showToast(ChannelActivity.this, "广播格式错误，请重试");
								}
								else {
									mService.setBroadcastDown(strDown);
									mService.setBroadcastUp(strUp);
									//通知service，准备reciever
									if (mService != null) {
										mService.updateCustomPttKeyReceiver();
									}
								}
							}
						});
						builder.setNegativeButton(R.string.cancel, null);

						builder.show();
					}
				});

				int code = mService.getPttKeycode();
				etKeycode.setText(String.valueOf(code));
				builderptt.setView(layout);

				final AlertDialog dlg = builderptt.create();
				dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode!=KeyEvent.KEYCODE_BACK && keyCode!=KeyEvent.KEYCODE_HOME && keyCode!=KeyEvent.KEYCODE_MENU && keyCode!=KeyEvent.KEYCODE_MEDIA_PLAY && keyCode!=KeyEvent.KEYCODE_MEDIA_PAUSE && keyCode!=KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && keyCode!=KeyEvent.KEYCODE_HEADSETHOOK) {
							//不允许设置某些特殊按键为ptt
							etKeycode.setText(String.valueOf(keyCode));
							return true;
						}
						return false;
					}
				});
				layout.findViewById(R.id.tv_cancle).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						dlg.dismiss();
					}
				});
				layout.findViewById(R.id.tv_ok).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						String txt = etKeycode.getText().toString();
						mService.setPttKeycode(Integer.valueOf(txt).intValue());
						tvCode.setText(txt);
						dlg.dismiss();
					}
				});
				dlg.show();
			}
		});

		//录音模式
		final LinearLayout llRecord = (LinearLayout) view.findViewById(R.id.ll_record_mode);
		final TextView tvMode = (TextView) view.findViewById(R.id.tv_record_mode);
		int mode = mService.getRecordMode();
		tvMode.setText((mode==0) ? getString(R.string.normal_mode) : getString(R.string.process_mode));

		llRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
				LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
				View layout = inflater.inflate(R.layout.set_record_mode, null);
				LinearLayout llNormal = (LinearLayout) layout.findViewById(R.id.ll_record_mode_normal);
				LinearLayout llProcess = (LinearLayout) layout.findViewById(R.id.ll_record_mode_process);
				final ImageView ivNormal = (ImageView) layout.findViewById(R.id.iv_record_mode_normal);
				final ImageView ivProcess = (ImageView) layout.findViewById(R.id.iv_record_mode_process);

				int mode = mService.getRecordMode();
				if (mode == 0) {
					ivNormal.setVisibility(View.VISIBLE);
					ivProcess.setVisibility(View.INVISIBLE);
				}
				else {
					ivNormal.setVisibility(View.INVISIBLE);
					ivProcess.setVisibility(View.VISIBLE);
				}

				llNormal.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.setRecordMode(0);
						ivNormal.setVisibility(View.VISIBLE);
						ivProcess.setVisibility(View.INVISIBLE);
						tvMode.setText(R.string.normal_mode);
					}
				});
				llProcess.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.setRecordMode(1);
						ivNormal.setVisibility(View.INVISIBLE);
						ivProcess.setVisibility(View.VISIBLE);
						tvMode.setText(R.string.process_mode);
					}
				});

				//解决蓝牙录音只能3秒的问题
				final ImageView ivFix3s = (ImageView) layout.findViewById(R.id.iv_fix_3s);
				boolean f = (mService!=null) && (mService.getFix3s());
				ivFix3s.setImageResource(f ? R.drawable.checkbox_on : R.drawable.checkbox_off);
				final LinearLayout llFix3s = (LinearLayout) layout.findViewById(R.id.ll_fix_3s);
				llFix3s.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean f = (mService!=null) && (mService.getFix3s());
						boolean n = !f;
						ivFix3s.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
						if (mService != null) {
							mService.setFix3s(n);
						}
					}
				});

				builder.setView(layout);

				builder.setPositiveButton(R.string.ok, null);
				builder.show();
			}
		});
		//显示
		final AlertDialog dialog = buildertol.show();
		view.findViewById(R.id.setting_tv_ok).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});


	}

	/**
	 * 提示音
	 */
	private void notifSetting() {
		AlertDialog.Builder buildernotif = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.settings_alert, null);
		buildernotif.setView(view);

		//提示音风格
		final LinearLayout llAlertType = (LinearLayout) view.findViewById(R.id.ll_alert_type);
		final TextView tvAlert = (TextView) view.findViewById(R.id.tv_alert_type);
		int alert = mService.getAlertType();
		tvAlert.setText((alert==0) ? getString(R.string.normal) : "HAM");

		llAlertType.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
				LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
				View layout = inflater.inflate(R.layout.set_alert_type, null);
				LinearLayout llNormal = (LinearLayout) layout.findViewById(R.id.ll_alert_type_normal);
				LinearLayout llHam = (LinearLayout) layout.findViewById(R.id.ll_alert_type_ham);
				final ImageView ivNormal = (ImageView) layout.findViewById(R.id.iv_alert_type_normal);
				final ImageView ivHam = (ImageView) layout.findViewById(R.id.iv_alert_type_ham);

				int type = mService.getAlertType();
				if (type == 0) {
					ivNormal.setVisibility(View.VISIBLE);
					ivHam.setVisibility(View.INVISIBLE);
				}
				else {
					ivNormal.setVisibility(View.INVISIBLE);
					ivHam.setVisibility(View.VISIBLE);
				}

				llNormal.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.setAlertType(0);
						ivNormal.setVisibility(View.VISIBLE);
						ivHam.setVisibility(View.INVISIBLE);
						tvAlert.setText(R.string.normal);
					}
				});
				llHam.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.setAlertType(1);
						ivNormal.setVisibility(View.INVISIBLE);
						ivHam.setVisibility(View.VISIBLE);
						tvAlert.setText("HAM");
					}
				});

				builder.setView(layout);
				final AlertDialog show = builder.show();
				layout.findViewById(R.id.music_tv_okc).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						show.dismiss();
					}
				});
			}
		});

		//提示音量
		final LinearLayout llVolume = (LinearLayout) view.findViewById(R.id.ll_alert_volume);
		llVolume.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
				LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
				View layout = inflater.inflate(R.layout.set_alert_volume, null);
				final SeekBar sbOnline = (SeekBar) layout.findViewById(R.id.sb_volume_online);
				final SeekBar sbOffline = (SeekBar) layout.findViewById(R.id.sb_volume_offline);
				final SeekBar sbPress = (SeekBar) layout.findViewById(R.id.sb_volume_ptt_press);
				final SeekBar sbIBegin = (SeekBar) layout.findViewById(R.id.sb_volume_i_begin);
				final SeekBar sbIEnd = (SeekBar) layout.findViewById(R.id.sb_volume_i_end);
				final SeekBar sbOtherBegin = (SeekBar) layout.findViewById(R.id.sb_volume_other_begin);
				final SeekBar sbOtherEnd = (SeekBar) layout.findViewById(R.id.sb_volume_other_end);

				int volOnline = AppSettings.getInstance(ChannelActivity.this).getVolumeOnline();
				int volOffline = AppSettings.getInstance(ChannelActivity.this).getVolumeOffline();
				int volPress = AppSettings.getInstance(ChannelActivity.this).getVolumePress();
				int volIBegin = AppSettings.getInstance(ChannelActivity.this).getVolumeIBegin();
				int volIEnd = AppSettings.getInstance(ChannelActivity.this).getVolumeIEnd();
				int volOtherBegin = AppSettings.getInstance(ChannelActivity.this).getVolumeOtherBegin();
				int volOtherEnd = AppSettings.getInstance(ChannelActivity.this).getVolumeOtherEnd();

				sbOnline.setProgress(volOnline);
				sbOffline.setProgress(volOffline);
				sbPress.setProgress(volPress);
				sbIBegin.setProgress(volIBegin);
				sbIEnd.setProgress(volIEnd);
				sbOtherBegin.setProgress(volOtherBegin);
				sbOtherEnd.setProgress(volOtherEnd);

				builder.setView(layout);

				final AlertDialog show = builder.show();
				layout.findViewById(R.id.tv_alert_cancle).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						show.dismiss();
					}
				});
				layout.findViewById(R.id.tv_alert_ok).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						int iOnline = sbOnline.getProgress();
						int iOffline = sbOffline.getProgress();
						int iPress = sbPress.getProgress();
						int iIBegin = sbIBegin.getProgress();
						int iIEnd = sbIEnd.getProgress();
						int iOtherBegin = sbOtherBegin.getProgress();
						int iOtherEnd = sbOtherEnd.getProgress();

						AppSettings.getInstance(ChannelActivity.this).setKeyVolumeOnline(iOnline);
						AppSettings.getInstance(ChannelActivity.this).setKeyVolumeOffline(iOffline);
						AppSettings.getInstance(ChannelActivity.this).setVolumePress(iPress);
						AppSettings.getInstance(ChannelActivity.this).setVolumeIBegin(iIBegin);
						AppSettings.getInstance(ChannelActivity.this).setVolumeIEnd(iIEnd);
						AppSettings.getInstance(ChannelActivity.this).setVolumeOtherBegin(iOtherBegin);
						AppSettings.getInstance(ChannelActivity.this).setVolumeOtherEnd(iOtherEnd);

						if (mService != null) {
							mService.setVolumeOnline(iOnline);
							mService.setVolumeOffline(iOffline);
							mService.setVolumeTalkroomPress(iPress);
							mService.setVolumeTalkroomBegin(iIBegin);
							mService.setVolumeTalkroomEnd(iIEnd);
							mService.setVolumeOtherBegin(iOtherBegin);
							mService.setVolumeOtherEnd(iOtherEnd);
						}
					}
				});
			}
		});

		//手咪追加提示音
		final ImageView ivHandmicAlarm = (ImageView) view.findViewById(R.id.iv_handmic_alarm);
		boolean alarm = mService.getHandmicAlarm();
		ivHandmicAlarm.setImageResource(alarm ? R.drawable.checkbox_on : R.drawable.checkbox_off);

		final LinearLayout llHandmicAlarm = (LinearLayout) view.findViewById(R.id.ll_handmic_alarm);
		llHandmicAlarm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean oldChecked = mService.getHandmicAlarm();
				boolean now = !oldChecked;
				mService.setHandmicAlarm(now);
				ivHandmicAlarm.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
			}
		});
		final ImageView ivHandmicAlarmHelp = (ImageView) view.findViewById(R.id.iv_handmic_alarm_help);
		ivHandmicAlarmHelp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(ChannelActivity.this)
						.setMessage(R.string.handmic_alarm_help)
						.setPositiveButton(R.string.ok, null)
						.show();
			}
		});

		//显示
		final AlertDialog show = buildernotif.show();
		view.findViewById(R.id.notify_tv_ok).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				show.dismiss();
			}
		});
	}

	/**
	 * 语言选择
	 */
	private void languageSetting() {
		AlertDialog.Builder buildernotif = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.language_alert, null);
		buildernotif.setView(view);
		ImageView iv_alert_chain = (ImageView) view.findViewById(R.id.iv_alert_chain);
		ImageView iv_alert_english = (ImageView) view.findViewById(R.id.iv_alert_english);
		ImageView iv_alert_minawen = (ImageView) view.findViewById(R.id.iv_alert_minawen);
		LinearLayout ll_alert_chain = (LinearLayout) view.findViewById(R.id.ll_alert_chain);
		LinearLayout ll_alert_english = (LinearLayout) view.findViewById(R.id.ll_alert_english);
		LinearLayout ll_alert_minawen = (LinearLayout) view.findViewById(R.id.ll_alert_minawen);
		ll_alert_chain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AndroidUtils.showToast(getApplicationContext(),"中文");
			}
		});
		ll_alert_english.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AndroidUtils.showToast(getApplicationContext(),"英文");
			}
		});
		ll_alert_minawen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AndroidUtils.showToast(getApplicationContext(),"缅文");
			}
		});

		//显示
		final AlertDialog show = buildernotif.show();
	}
	@Override
	public void onScreenChanged(int index) {
		Animation animation = null;

		switch (index) {
			case 0:
				if (currIndex == 1) {
					animation = new TranslateAnimation(position_right, position_left, 0, 0);
					title.setText(getString(R.string.contact));
					iv_app_option.setVisibility(View.INVISIBLE);
					iv_con_option.setVisibility(View.VISIBLE);
				}
				break;
			case 1:
				if (currIndex == 0) {
					animation = new TranslateAnimation(position_left, position_right, 0, 0);
					title.setText(getString(R.string.channel));
					iv_app_option.setVisibility(View.VISIBLE);
					iv_con_option.setVisibility(View.INVISIBLE);
				}
				break;
			default:
				break;
		}
		currIndex = index;
		if (null != animation) {
			animation.setFillAfter(true);
			animation.setDuration(100);
			mLLTabBg.startAnimation(animation);
		}

		//service 纪录
		if (mService != null) {
			mService.recordScreen(index);
		}
	}

}
