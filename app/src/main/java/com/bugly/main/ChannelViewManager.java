package com.bugly.main;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bugly.R;
import com.bugly.utils.AndroidUtils;
import com.bugly.utils.AppCommonUtil;
import com.bugly.view.CircleImageView;
import com.bugly.view.InterpttNestedAdapter;
import com.bugly.view.InterpttNestedListView;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_DISCONNECTED;

/*
 * 本界面负责展示12个最常用的
 */

public class ChannelViewManager implements OnClickListener, InterpttNestedListView.OnNestedChildClickListener, InterpttNestedListView.OnNestedChildLongClickListener, InterpttNestedListView.OnNestedGroupClickListener, InterpttNestedListView.OnNestedGroupLongClickListener {
	private static Context mContext;
	private ChannelActivity mMainActivity;
	private InterpttService mService;

	private InterpttNestedListView mLVChannel;
	private ChannelListAdapter channelAdapter;

	private LinearLayout mLLTips;

	public ChannelViewManager(ChannelActivity act) {
		mMainActivity = act;
		mContext = act.getBaseContext();
	}

	public void setService(InterpttService service) {
		mService = service;
	}

	public View createView(LayoutInflater inflater, ViewGroup container) {
		View view = inflater.inflate(R.layout.view_channel, container, false);

		// Get the UI views
		mLVChannel = (InterpttNestedListView) view.findViewById(R.id.channelUsers);
		mLVChannel.setOnChildClickListener(this);
		mLVChannel.setOnChildLongClickListener(this);
		mLVChannel.setOnGroupClickListener(this);
		mLVChannel.setOnGroupLongClickListener(this);

		mLLTips = (LinearLayout) view.findViewById(R.id.ll_tips);
		TextView tvCreateChannel = (TextView) view.findViewById(R.id.tv_create_channel);
		tvCreateChannel.setOnClickListener(this);
		TextView tvJoinChannel = (TextView) view.findViewById(R.id.tv_join_channel);
		tvJoinChannel.setOnClickListener(this);

		return view;
	}

	public void resume() {

	}

	public void pause() {

	}

	public void destroy() {

	}

	public void setupList() {
		channelAdapter = new ChannelListAdapter(mMainActivity, mService);
		mLVChannel.setAdapter(channelAdapter);
		updateChannelList();
	}

	public void setListAdapter(ChannelListAdapter adapter) {
		if (mLVChannel != null) {
			mLVChannel.setAdapter(adapter);
		}
	}

	public void updateChannelList() {
		if (channelAdapter == null) {
			channelAdapter = new ChannelListAdapter(mMainActivity, mService);
			mLVChannel.setAdapter(channelAdapter);
		}

		channelAdapter.updateChannelList();
		/**
		 * Registers the passed observer and calls the most recent connection state callback immediately.
		 * @param observer
		 */
		channelAdapter.notifyDataSetChanged();
		//如果没有频道，则显示提示
		if (mService!=null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
			if (channelAdapter.isEmpty()) {
				mLLTips.setVisibility(View.VISIBLE);
				mLVChannel.setVisibility(View.INVISIBLE);
			}
			else {
				mLLTips.setVisibility(View.INVISIBLE);
				mLVChannel.setVisibility(View.VISIBLE);
			}
		}
	}


	private void channelOptions(int position) {
		if (mService.getConnectionState()==CONNECTION_STATE_DISCONNECTED) {
			return;
		}

		final Channel channel = (Channel) channelAdapter.getGroup(position);
		if (channel.isTemporary) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
		builder.setTitle(channel.name);

		User me = mService.getCurrentUser();
		if (channel.creatorId == me.iId) {
			//说明是我创建的
			DialogInterface.OnClickListener dlg = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
						case 0:
							infoChannel(channel);
							break;
						case 1:
							memberChannel(channel);
							break;
						case 2:
							shareChannel(channel);
							break;
						case 3:
							changeChannelName(channel);
							break;
						case 4:
							changeChannelPwd(channel);
							break;
						case 5:
							switchChannelPub(channel);
							break;
						case 6:
							manageMember(channel, true, 0);
							break;
						case 7:
							deleteChannel(channel.id, channel.name);
						default:
							break;
					}
				}
			};

			CharSequence[] cs;
			if (channel.searchable) {
				builder.setItems(R.array.channel_options_admin_public, dlg);
			}
			else {
				builder.setItems(R.array.channel_options_admin_private, dlg);
			}
		}
		else if (mService.isMonitor(me.iId, channel)) {
			builder.setItems(R.array.channel_options_monitor, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
						case 0:
							infoChannel(channel);
							break;
						case 1:
							memberChannel(channel);
							break;
						case 2:
							shareChannel(channel);
							break;
						case 3:
							manageMember(channel, false, 0);
							break;
						case 4:
							quitChannel(channel);
						default:
							break;
					}
				}
			});
		}
		else {
			builder.setItems(R.array.channel_options_guest, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
						case 0:
							infoChannel(channel);
							break;
						case 1:
							memberChannel(channel);
							break;
						case 2:
							shareChannel(channel);
							break;
						case 3:
							quitChannel(channel);
							break;
						default:
							break;
					}
				}
			});

		}
		builder.show();
	}

	private void infoChannel(Channel c) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(c.name);
		String message = c.searchable ? mContext.getString(R.string.public_channel) : mContext.getString(R.string.private_channel);
		message += "\n";
		message += mContext.getString(R.string.channelId) + ":" + c.id + "\n";
		String pwd = (c.pwd != null && c.pwd.length() > 0) ? c.pwd : mContext.getString(R.string.no_pwd);
		message += mContext.getString(R.string.channel_kouling) + ":" + pwd;

		builder.setMessage(message);

		builder.setPositiveButton(R.string.ok, null);
		builder.show();
	}

	private void memberChannel(Channel c) {
		Intent i = new Intent(mMainActivity, MemberChannel.class);
		i.putExtra("ChanId", c.id);
		i.putExtra("ChanName", c.name);
		i.putExtra("ChanMemberCount", c.memberCount);
		mMainActivity.startActivity(i);
	}

	private void shareChannel(final Channel c) {
		User me = mService.getCurrentUser();
		boolean isAdmin = (me.iId == c.creatorId);

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.share));
		String s = null;
		if (isAdmin) {
			s = mContext.getString(R.string.i_create) + c.name + "]";
		} else {
			s = mContext.getString(R.string.i_in_totalk) + c.name + "]" + mContext.getString(R.string.channel);
		}

		s += "，" + mContext.getString(R.string.channelId) + c.id + "，";
		if (c.pwd == null || c.pwd.length() == 0) {
			s += mContext.getString(R.string.no_pwd);
		} else {
			s += mContext.getString(R.string.kouling) + c.pwd;
		}
		s += "，来聊聊吧！在App Store或应用市场搜索\"滔滔对讲\"下载安装即可";

		intent.putExtra(Intent.EXTRA_TEXT, s);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mMainActivity.startActivity(Intent.createChooser(intent, "分享"));
	}

	private void changeChannelName(final Channel c) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(mContext.getString(R.string.change_chan_name));
		LayoutInflater inflater = LayoutInflater.from(mMainActivity);
		View layout = inflater.inflate(R.layout.change_channel_name, null);
		builder.setView(layout);
		final EditText etChannel = (EditText) layout.findViewById(R.id.et_change_channel_name);
		etChannel.setText(c.name);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final String str = etChannel.getText().toString();
				if (!AppCommonUtil.validChannelName(str)) {
					AndroidUtils.showIdToast(mMainActivity, R.string.channel_name_bad_format);
				} else {
					mService.changeChannelName(c.id, str);
				}
			}
		});

		builder.setNegativeButton(R.string.cancel, null);

		builder.show();
	}

	private void changeChannelPwd(final Channel c) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(mContext.getString(R.string.change_chan_pwd));
		LayoutInflater inflater = LayoutInflater.from(mMainActivity);
		View layout = inflater.inflate(R.layout.change_channel_pwd, null);
		builder.setView(layout);
		final EditText etPwd = (EditText) layout.findViewById(R.id.et_change_channel_pwd);
		etPwd.setText(c.pwd);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final String pwd = etPwd.getText().toString();

				if (pwd.length() != 0 && !AppCommonUtil.validChannelPwd(pwd)) {
					AndroidUtils.showIdToast(mMainActivity, R.string.channel_pwd_bad_format);
				} else {
					//此时有两种情况，要么是合法的口令，要么为空，表示频道无需口令
					if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
						mService.changeChannelPwd(c.id, pwd);
					}
				}
			}
		});

		builder.setNegativeButton(R.string.cancel, null);

		builder.show();
	}

	private void switchChannelPub(final Channel c) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
		final boolean oldPub = c.searchable;

		builder.setTitle(mContext.getString(R.string.notif));
		String s = oldPub ? mContext.getString(R.string.confirm_set_private) : mContext.getString(R.string.confirm_set_public);
		builder.setMessage(s);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
					mService.setChannelSearchable(c.id, !oldPub);
				}
			}
		});

		builder.setNegativeButton(R.string.cancel, null);

		builder.show();
	}

	//uid是目标用户id。uid为0时，表示未指定用户
	private void manageMember(final Channel c, final boolean isCreator, final int uid) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(mContext.getString(R.string.manage_member_auth));
		LayoutInflater inflater = LayoutInflater.from(mMainActivity);
		View layout = inflater.inflate(R.layout.manage_member, null);
		builder.setView(layout);

		final EditText etUid = (EditText) layout.findViewById(R.id.et_ban_member_uid);
		if (uid>1000000 && uid<9999999) {
			etUid.setText(uid + "");
		}
		final RadioGroup rgOpt = (RadioGroup) layout.findViewById(R.id.rg_manage_member);
		final RadioButton rbBan = (RadioButton) layout.findViewById(R.id.rb_ban);
		final RadioButton rbCancelBan = (RadioButton) layout.findViewById(R.id.rb_cancel_ban);
		final RadioButton rbMute = (RadioButton) layout.findViewById(R.id.rb_mute);
		final RadioButton rbCancelMute = (RadioButton) layout.findViewById(R.id.rb_cancel_mute);
		final RadioButton rbMonitor = (RadioButton) layout.findViewById(R.id.rb_monitor);
		final RadioButton rbCancelMonitor = (RadioButton) layout.findViewById(R.id.rb_cancel_monitor);
		final RadioButton rbPrior = (RadioButton) layout.findViewById(R.id.rb_prior);
		final RadioButton rbCancelPrior = (RadioButton) layout.findViewById(R.id.rb_cancel_prior);
		//副管不能设置增删其他副管
		if (! isCreator) {
			rbMonitor.setEnabled(false);
			rbCancelMonitor.setEnabled(false);
			rbPrior.setEnabled(false);
			rbCancelPrior.setEnabled(false);
		}

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final String strUid = etUid.getText().toString();
				int uid = 0;
				try {
					uid = Integer.parseInt(strUid);
				} catch (Exception e) {

				}

				final int check = rgOpt.getCheckedRadioButtonId();

				if (!AppCommonUtil.validTotalkId(strUid)) {
					AndroidUtils.showIdToast(mMainActivity, R.string.userid_bad_format);
				}
				else if (check == rbBan.getId()) {
					mService.manageMember(c.id, uid, 0);
				}
				else if (check == rbCancelBan.getId()) {
					mService.manageMember(c.id, uid, 1);
				}
				else if (check == rbMute.getId()) {
					mService.manageMember(c.id, uid, 2);
				}
				else if (check == rbCancelMute.getId()) {
					mService.manageMember(c.id, uid, 3);
				}
				else if (check == rbMonitor.getId()) {
					mService.manageMember(c.id, uid, 4);
				}
				else if (check == rbCancelMonitor.getId()) {
					mService.manageMember(c.id, uid, 5);
				}
				else if (check == rbPrior.getId()) {
					mService.manageMember(c.id, uid, 6);
				}
				else if (check == rbCancelPrior.getId()) {
					mService.manageMember(c.id, uid, 7);
				}
			}
		});

		builder.setNegativeButton(R.string.cancel, null);

		builder.show();
	}

	private void deleteChannel(final int chanId, final String chanName) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(mContext.getString(R.string.channel) + chanName + mContext.getString(R.string.confirm_delete_chan));
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mService.deleteChannel(chanId);
			}
		});

		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}


	private void quitChannel(final Channel c) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

		builder.setTitle(mContext.getString(R.string.confirm_quit_chan));
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				mService.quitChannel(c.id);
			}
		});

		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}


	/**
	 * Updates the user specified in the users adapter.
	 *
	 * @param user
	 */
	public void updateUser(User user) {
		if (user == null) {
			return;
		}

		if (channelAdapter == null) {
			return;
		}

		channelAdapter.refreshUser(user);
	}

	/**
	 * Removes the user from the channel list.
	 *
	 * @param user
	 */
	private void removeUser(User user) {
		channelAdapter.notifyDataSetChanged();
	}

	private void updateChannel(Channel channel) {
		updateChannelList();
	}

	/**
	 * Scrolls to the passed channel.
	 */
	@TargetApi(Build.VERSION_CODES.FROYO)
	private void scrollToChannel(Channel channel) {
		if (channelAdapter==null || channelAdapter.channels==null) {
			return;
		}

		int channelPosition = channelAdapter.channels.indexOf(channel);
		int flatPosition = channelAdapter.getVisibleFlatGroupPosition(channelPosition);
		mLVChannel.smoothScrollToPosition(flatPosition);
	}

	@Override
	public void onNestedChildClick(AdapterView<?> parent, View view, int groupPosition, int childPosition, long id) {
		final AlertDialog builder = new AlertDialog.Builder(mMainActivity).create();
		builder.setCanceledOnTouchOutside(true);
		LayoutInflater inflater = LayoutInflater.from(mMainActivity);
		View layout = inflater.inflate(R.layout.member_detail, null);

		final User user = (User) channelAdapter.getChild(groupPosition, childPosition);
		if (user == null) {
			return;
		}
		builder.setTitle(user.nick);
		builder.setView(layout);

		final ImageView ivAvatar = (ImageView) layout.findViewById(R.id.iv_user_detail_avatar);
		final TextView tvId = (TextView) layout.findViewById(R.id.tv_user_detail_id);
		final ImageView ivManage = (ImageView) layout.findViewById(R.id.iv_user_detail_manage);
		final ImageView ivAdd = (ImageView) layout.findViewById(R.id.iv_user_detail_add_contact);
		ivAvatar.setImageResource(R.drawable.ic_default_avatar);

		tvId.setText(String.valueOf(user.iId));

		ivManage.setVisibility(View.INVISIBLE);
		if (mService!=null && mService.getCurrentUser()!=null && user.getChannel()!=null) {
			final User me = mService.getCurrentUser();
			final Channel chan = user.getChannel();
			if (me.iId==chan.creatorId || mService.isMonitor(me.iId, chan)) {
				ivManage.setVisibility(View.VISIBLE);
				ivManage.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						manageMember(chan, (me.iId==chan.creatorId), user.iId);
					}
				});
			}
		}

		ivAdd.setVisibility(View.INVISIBLE);
		if (mService!=null && mService.getContacts()!=null && mService.getCurrentUser()!=null) {
			if (! mService.getContacts().containsKey(user.iId) && user.iId != mService.getCurrentUser().iId) {
				//不是好友，且非本人
				ivAdd.setVisibility(View.VISIBLE);
				ivAdd.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						new AlertDialog.Builder(mMainActivity)
								.setTitle(mContext.getString(R.string.notif))
								.setMessage(mContext.getString(R.string.add) + user.nick + mContext.getString(R.string.as_contact))
								.setPositiveButton(mContext.getString(R.string.ok), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mService.applyContact(true, user.iId);
										AndroidUtils.showToast(mMainActivity, mContext.getString(R.string.apply_sent));
									}
								})
								.setNegativeButton(mContext.getString(R.string.cancel), null)
								.show();
					}
				});
			}
		}

		builder.show();
	}

	@Override
	public void onNestedChildLongClick(AdapterView<?> parent, View view, int groupPosition, int childPosition, long id) {
	}

	@Override
	public void onNestedGroupClick(AdapterView<?> parent, View view, int groupPosition, long id) {
		if (mService.getConnectionState() != CONNECTION_STATE_CONNECTED) {
			return;
		}

		final Channel channel = (Channel) channelAdapter.getGroup(groupPosition);
		final Channel curr = mService.getCurrentChannel();
		if (curr.id == channel.id) {
			channelAdapter.groupClicked(groupPosition);
		} else {
			mService.enterChannel(channel.id);
			mMainActivity.voiceBroast(mMainActivity.getString(R.string.join_channel)+channel.name,true);
			AndroidUtils.Log("进入群组，"+channel.name);
		}
	}

	@Override
	public void onNestedGroupLongClick(AdapterView<?> parent, View view, int groupPosition, long id) {
		channelOptions(groupPosition);
	}

	class ChannelListAdapter extends InterpttNestedAdapter {
		private final InterpttService service;
		private ArrayList<Channel> channels = new ArrayList<Channel>();
		@SuppressLint("UseSparseArrays") // Don't like 'em
		private Map<Integer, List<User>> channelMap = new ConcurrentHashMap<Integer, List<User>>();

		public ChannelListAdapter(final Context context, final InterpttService service) {
			super(context);
			this.service = service;
		}

		/**
		 * Fetches a new list of channels from the service.
		 */
		public void updateChannelList() {
			if (service != null) {
				this.channels = service.getChannelList();
				this.channelMap = service.getSortedChannelMap();
			}
		}

		public void groupClicked(int pos) {
			Channel c = channels.get(pos);

			if (channelAdapter.isGroupExpanded(pos)) {
				channelAdapter.collapseGroup(pos);
				c.expanded = 0;
			}
			else {
				expandGroup(pos);
				c.expanded = 1;
			}

			channels.add(pos, c);
			notifyVisibleSetChanged();
			updateChannelList();
		}

		public void refreshUser(User user) {
			if (!service.getUserList().contains(user))
				return;

			if (channels == null) {
				return;
			}

			int channelPosition = channels.indexOf(user.getChannel());
			if (!channelAdapter.isGroupExpanded(channelPosition))
				return; // Don't refresh

			if (channelMap==null || channelMap.get(user.getChannel().id)==null) {
				return;
			}

			int userPosition = channelMap.get(user.getChannel().id).indexOf(user);
			int position = channelAdapter.getVisibleFlatChildPosition(channelPosition, userPosition);

			View userView = mLVChannel.getChildAt(position - mLVChannel.getFirstVisiblePosition());

			//经测，这些判断不能省略，否则崩溃
			if (userView != null && userView.getTag() != null && userView.getTag().equals(user)) {
				refreshElements(userView, user);
			}
		}

		private void refreshElements(final View view, final User user) {
			final ImageView ivPerm = (ImageView) view.findViewById(R.id.iv_perm);
			final CircleImageView avatar = (CircleImageView) view.findViewById(R.id.userRowAvatar);
			final TextView tvNick = (TextView) view.findViewById(R.id.userRowNick);
			final ImageView ivPrior = (ImageView) view.findViewById(R.id.iv_prior_mic);	//插话权限
			final ImageView ivMute = (ImageView) view.findViewById(R.id.iv_mute);
			final ImageView ivAudioSource = (ImageView) view.findViewById(R.id.iv_audio_source);
			final TextView tvId = (TextView) view.findViewById(R.id.userRowId);

			if (user == null) {
				return;
			}

			if (user.iId == user.getChannel().creatorId) {
				avatar.setImageResource(R.drawable.owner);
			}else if (mService!=null && mService.isMonitor(user.iId, user.getChannel())) {
				avatar.setImageResource(R.drawable.monitor);
			}else{
				//头像
//				if (true) {//(user.avatar == null) {
//					avatar.setImageResource(R.drawable.ic_default_avatar);
//				} else {
//					Bitmap bm = BitmapFactory.decodeByteArray(user.avatar, 0, user.avatar.length);
//					avatar.setImageBitmap(bm);
//				}
			}

			//talking状态
			//如果跟自己在同一频道，根据localTalking状态判断；如果在其他频道，根据服务器消息判断
			if (user.getChannel() != null && mService.getCurrentChannel() != null) {
				final ImageView circle = (ImageView) view.findViewById(R.id.iv_avatar_circle);
				if (user.getChannel().id == mService.getCurrentChannel().id) {
					//同一频道
					if (user.isLocalTalking) {
						circle.setVisibility(View.VISIBLE);
						circle.startAnimation(AppCommonUtil.createTalkingAnimation());
					} else {
						//先停止动画再set gone，尝试解决停止讲话后红圈仍闪烁的问题。测试无效
						circle.clearAnimation();
						circle.setVisibility(View.GONE);
					}
				} else {
					if (user.isTalking) {
						circle.setVisibility(View.VISIBLE);
						circle.startAnimation(AppCommonUtil.createTalkingAnimation());
					} else {
						//先停止动画再set gone，尝试解决停止讲话后红圈仍闪烁的问题。测试无效
						circle.clearAnimation();
						circle.setVisibility(View.GONE);
					}
				}
			}


			//zcx change
			tvNick.setText(user.nick);
			if (mService.getCurrentUser() != null && user.session == mService.getCurrentUser().session) {
//				tvNick.setTextColor(AppConstants.CURRENT_NICK_COLOR);
				tvNick.setTextColor(mMainActivity.getResources().getColor(R.color.channel_otheruser_bgc));
				avatar.setImageResource(R.drawable.ic_default_avatar);

			} else {
//				tvNick.setTextColor(AppConstants.OTHER_NICK_COLOR);
				tvNick.setTextColor(mMainActivity.getResources().getColor(R.color.black));
				avatar.setImageResource(R.drawable.ic_default_avatar_gray);
			}

			//audio source
			ivAudioSource.setImageLevel(user.audioSource);

			//是否有插话权限
			if (mService.isPrior(user.iId, user.getChannel())) {
				ivPrior.setVisibility(View.VISIBLE);
			}
			else {
				ivPrior.setVisibility(View.GONE);
			}
			//是否禁言
			if (mService.isMute(user.iId, user.getChannel())) {
				ivMute.setVisibility(View.VISIBLE);
			}
			else {
				ivMute.setVisibility(View.INVISIBLE);
			}

			tvId.setText(String.valueOf(user.iId));
		}

		public int getNestedLevel(Channel channel) {
			return 0;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			Channel channel = null;
			try {
				channel = channels.get(groupPosition);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				return null;
			}

			List<User> channelUsers = channelMap.get(channel.id);
			if (channelUsers == null) {
				return null;
			}

			Object obj = null;
			try {
				obj = channelUsers.get(childPosition);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}

			return obj;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, int depth, View v, ViewGroup arg4) {
			if (v == null) {
				final LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.listitem_user, null);
			}

			User user = (User) getChild(groupPosition, childPosition);

			refreshElements(v, user);
			v.setTag(user);

			return v;
		}

		@Override
		public int getChildCount(int arg0) {			if (channels == null || channelMap==null) {
			return 0;
		}
			int i = channels.get(arg0).id;
			List<User> l = channelMap.get(i);
			//#0015实现时，删除频道事，如果频道里有人在线，则会出现l为null的情况
			if (l == null)
				return 0;
			return l.size();
		}

		@Override
		public Object getGroup(int arg0) {
			return channels.get(arg0);
		}

		@Override
		public int getGroupCount() {
			return channels.size();
		}

		@Override
		public View getGroupView(final int groupPosition, int depth, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				final LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.listitem_channel, null);
			}

			final Channel channel = channels.get(groupPosition);
			if (mService.getCurrentChannel() == null) {
				return v;
			}

			ImageView expandView = (ImageView) v.findViewById(R.id.channel_row_expand);
			expandView.setImageResource(isGroupExpanded(groupPosition) ? R.drawable.ic_action_minus : R.drawable.ic_action_add);
			expandView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isGroupExpanded(groupPosition)) {
						collapseGroup(groupPosition);
						channel.expanded = 0;
					}
					else {
						expandGroup(groupPosition);
						channel.expanded = 1;
					}
					channels.add(groupPosition, channel);
					notifyVisibleSetChanged();
					updateChannelList();
				}
			});

			TextView nameView = (TextView) v.findViewById(R.id.channel_row_name);
			TextView countView = (TextView) v.findViewById(R.id.channel_row_count);
			TextView tvTalker = (TextView) v.findViewById(R.id.tv_chan_talker);
//			TextView tvChanNumber = (TextView) v.findViewById(R.id.tv_channel_number);
			ImageView ivListen = (ImageView) v.findViewById(R.id.iv_channel_listen);
			ivListen.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean oldListen = mService.isListen(mService.getCurrentUser(), channel.id);
					mService.setListen(channel.id, ! oldListen);
				}
			});


			nameView.setText(channel.name);

			//是否监听
			if (mService.isListen(mService.getCurrentUser(), channel.id)) {
				ivListen.setImageResource(R.drawable.im_monitored);
			}
			else {
				ivListen.setImageResource(R.drawable.im_monitor);
			}


			countView.setText("(" + channel.userCount + "/" + channel.memberCount + ")");

			User talker = mService.whoIsTalking(channel);
//			if (talker != null) {
//				tvTalker.setVisibility(View.VISIBLE);
//				tvTalker.setText(talker.nick);
//			} else {
//				tvTalker.setText("");
//				tvTalker.setVisibility(View.GONE);
//			}

			if (channel.id == mService.getCurrentChannel().id) {
//				v.setBackgroundResource(R.drawable.selector_current_channel);
				v.setBackgroundColor(mMainActivity.getResources().getColor(R.color.dimgray));
				nameView.setTextColor(mMainActivity.getResources().getColor(R.color.white));
//				tvChanNumber.setTextColor(mMainActivity.getResources().getColor(R.color.white));
				countView.setTextColor(mMainActivity.getResources().getColor(R.color.white));
			} else {
				v.setBackgroundResource(R.drawable.selector_other_channel);
				nameView.setTextColor(mMainActivity.getResources().getColor(R.color.black));
//				tvChanNumber.setTextColor(mMainActivity.getResources().getColor(R.color.black));
				countView.setTextColor(mMainActivity.getResources().getColor(R.color.black));
			}
//			tvChanNumber.setText(mMainActivity.getString(R.string.chanid, channel.id));
			return v;
		}

		@Override
		public int getGroupDepth(int groupPosition) {
			Channel channel = (Channel) getGroup(groupPosition);
			return getNestedLevel(channel);
		}

		@Override
		public int getParentId(int groupPosition) {
			return -1;
		}

		@Override
		public int getGroupId(int groupPosition) {
			Channel channel = channels.get(groupPosition);
			return channel.id;
		}

		@Override
		public int getChildId(int groupPosition, int childPosition) {
			return channelMap.get(channels.get(groupPosition)).get(childPosition).iId;
		}

		@Override
		public boolean isGroupExpandedByDefault(int groupPosition) {
			if (mService.getCurrentChannel() == null) {
				return false;
			}
			Channel c = channels.get(groupPosition);
			if (c.expanded == -1) {
				return (c.id == mService.getCurrentChannel().id);
			}
			else {
				return (c.expanded == 1);
			}
		}
	}

	public void handleCurrentChannelChanged() {
		updateChannelList();
		scrollToChannel(mService.getCurrentChannel());
	}

	public void handleChannelAdded(Channel channel) {
		if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
			updateChannelList();
		}
	}

	public void handleChannelRemoved(Channel channel) {
		if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
			updateChannelList();
		}
	}

	public void handleChannelUpdated(Channel channel) {
		if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
			updateChannelList();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();

		switch (id) {
			case R.id.tv_create_channel:
//				mMainActivity.createChannel();
				break;
			case R.id.tv_join_channel:
//				mMainActivity.joinChannel();
				break;
		}
	}

}
