package com.bugly.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bugly.utils.AppConstants;
import com.bugly.R;
import com.bugly.utils.AndroidUtils;
import com.bugly.view.CircleImageView;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;

import java.util.ArrayList;
import java.util.Map;

/*
 * 本界面负责展示12个最常用的
 */

public class ContactViewManager {
	private ChannelActivity mMainActivity;
	//联系人
	private ContactListAdapter contactAdapter;
	private ListView mLVContact;
	private InterpttService mService;
	private AlertDialog mDetailDialog = null;

	public ContactViewManager(ChannelActivity act) {
		mMainActivity = act;
	}

	public void setService(InterpttService service) {
		mService = service;
	}

	public View createView(LayoutInflater inflater, ViewGroup container) {
		View view = inflater.inflate(R.layout.view_contact, container, false);

		mLVContact = (ListView) view.findViewById(R.id.lv_contacts);
		mLVContact.setOnItemClickListener(mContactOnItemClickListener);
		mLVContact.setOnItemLongClickListener(mContactOnItemLongClickListener);
		return view;
	}

	class ContactListAdapter extends BaseAdapter {
		private final InterpttService service;
		private ArrayList<Contact> mContacts;
		private LayoutInflater mInflator;

		public ContactListAdapter(final InterpttService service) {
			super();

			this.service = service;
			mContacts = new ArrayList<Contact>();
			mInflator = mMainActivity.getLayoutInflater();
		}

		public void updateData() {
			mContacts.clear();
			if (service == null) {
				return;
			}

			Contact c;
			//最上面显示自己
			User me = mService.getCurrentUser();
			if (me != null) {
				c = new Contact(false, true, me.iId, me.nick, me.avatar, false, me.audioSource);
				mContacts.add(c);
			}

			//pending contact
			Map<Integer, Contact> pendingContactMap = service.getPendingContacts();
			if (pendingContactMap != null) {
				for(Contact value:pendingContactMap.values()) {
					c = new Contact(true, true, value.iId, value.nick, value.avatar, false, value.audioSource);
					mContacts.add(c);
				}
			}

			Map<Integer, Contact> contactMap = service.getContacts();
			if (contactMap != null) {
				for(Contact value:contactMap.values()) {
					if (value.online) {
						c = new Contact(false, value.online, value.iId, value.nick, value.avatar, value.selected, value.audioSource);
						mContacts.add(c);
					}
				}
				//先显示在线的，再显示离线的
				for(Contact value:contactMap.values()) {
					if (! value.online) {
						c = new Contact(false, value.online, value.iId, value.nick, value.avatar, value.selected, 0);
						mContacts.add(c);
					}
				}
			}
		}

		public Contact getContact(int position) {
			return mContacts.get(position);
		}

		public void clear() {
			mContacts.clear();
		}

		@Override
		public int getCount() {
			return mContacts.size();
		}

		@Override
		public Object getItem(int i) {
			return mContacts.get(i);
		}

		public void setItem(int i, Contact c) {
			mContacts.set(i, c);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ContactViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_contact, null);
				viewHolder = new ContactViewHolder();
				viewHolder.ivAvatar = (CircleImageView) view.findViewById(R.id.civ_contact_avatar);
				viewHolder.tvNick = (TextView) view.findViewById(R.id.tv_contact_nick);
				viewHolder.tvApply = (TextView) view.findViewById(R.id.tv_contact_apply);
				viewHolder.ivAudioSource = (ImageView) view.findViewById(R.id.iv_contact_audio_source);
				viewHolder.tvId = (TextView) view.findViewById(R.id.tv_contact_id);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ContactViewHolder) view.getTag();
			}

			Contact con = mContacts.get(i);

			//是否待同意
			boolean pending = con.pending;
			if (pending) {
				viewHolder.tvApply.setVisibility(View.VISIBLE);
				view.setBackgroundColor(ContextCompat.getColor(mMainActivity, R.color.holo_orange_light));
			}else{
				viewHolder.tvApply.setVisibility(View.GONE);
				view.setBackgroundResource(R.drawable.selector_other_contact);
//				view.setBackground(mMainActivity.getResources().getDrawable(R.drawable.item_stytle));
			}

			//头像
			viewHolder.ivAvatar.setImageResource(con.online ? R.drawable.ic_default_avatar : R.drawable.ic_default_avatar_gray);

			viewHolder.tvNick.setText(con.nick);
			if (! con.online) {
				viewHolder.tvNick.setTextColor(ContextCompat.getColor(mMainActivity, R.color.gray_b0));
			}
			else {
				if (mService.getCurrentUser()!=null && con.iId == mService.getCurrentUser().iId) {
					//自己高亮显示
					viewHolder.tvNick.setTextColor(AppConstants.CURRENT_NICK_COLOR);
				}
				else {
					viewHolder.tvNick.setTextColor(mMainActivity.getResources().getColor(R.color.channel_otheruser_bgc));
				}
			}

			//audio source
			viewHolder.ivAudioSource.setImageLevel(con.audioSource);
			//id
			viewHolder.tvId.setText(String.valueOf(con.iId));

			//选择指示
			if(con.selected)
			{
				view.setBackgroundColor(mMainActivity.getResources().getColor(R.color.dimgray3));
//				view.setBackgroundColor(mMainActivity.getResources().getColor(R.color.haoyoubg_gray));
//				view.setBackground(mMainActivity.getResources().getDrawable(R.drawable.item_stytle_select));
			}

			return view;
		}
	}

	static class ContactViewHolder {
		CircleImageView ivAvatar;
		TextView tvNick;
		TextView tvApply;
		ImageView ivAudioSource;
		TextView tvId;
	}

	private AdapterView.OnItemLongClickListener mContactOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			final Contact con = contactAdapter.getContact(position);
			if (con == null) {
				return true;
			}
			if (mService==null || mService.getCurrentUser()==null) {
				return true;
			}
			if (con.iId == mService.getCurrentUser().iId) {
				AndroidUtils.Log("长按自己，不给反应");
				return true;
			}
//			final AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
//			LayoutInflater inflater = LayoutInflater.from(mMainActivity);
//			View layout = inflater.inflate(R.layout.contact_detail, null);
//
//			builder.setTitle(con.nick);
//			builder.setView(layout);
//
//			final Button btn_contact_delete = (Button) layout.findViewById(R.id.btn_contact_delete);
//
//			btn_contact_delete.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//
//				}
//			});
//			mDetailDialog = builder.show();
			AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
			builder.setTitle(mMainActivity.getString(R.string.notif))
					.setMessage(mMainActivity.getString(R.string.confirm_delete_contact))
					.setPositiveButton(mMainActivity.getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mService.applyContact(false, con.iId);
						}
					});
			builder.setNegativeButton(mMainActivity.getString(R.string.cancel), null);
			builder.setCancelable(false);
			AlertDialog alertDialog = builder.create();
			alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);//系统级别的dialog
			alertDialog.setCanceledOnTouchOutside(false);//失去焦点不会消失
			alertDialog.show();
			return true;
		}
	};

	//联系人相关
	private AdapterView.OnItemClickListener mContactOnItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			final Contact con = contactAdapter.getContact(position);
			if (con == null) {
				return;
			}
			if (mService==null || mService.getCurrentUser()==null) {
				return;
			}
			if (con.pending) {
				AndroidUtils.Log("短按收到添加好友消息on.pending="+con.pending);
				new AlertDialog.Builder(mMainActivity)
						.setTitle(R.string.notif)
						.setMessage(con.nick + mMainActivity.getString(R.string.add_you_as_contact))
						.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mService.applyContact(true, con.iId);
								mService.deletePendingContact(con.iId);
							}
						})
						.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mService.applyContact(false, con.iId);
								mService.deletePendingContact(con.iId);
							}
						})
						.show();
			}else{
				if (mService!=null && mService.getCurrentUser()!=null && !con.pending && con.iId != mService.getCurrentUser().iId && con.online) {
					AndroidUtils.Log("短按选择好友on.pending="+con.pending);
					mService.selectContact(con, !con.selected);
				}
			}

		}
	};

	public void setupList() {
		contactAdapter = new ContactListAdapter(mService);
		mLVContact.setAdapter(contactAdapter);
		updateContactList();
	}

	public void setListAdapter(ListAdapter adapter) {
		if (mLVContact != null) {
			mLVContact.setAdapter(adapter);
		}
	}

	public void updateContactList() {
		if (contactAdapter == null) {
			contactAdapter = new ContactListAdapter(mService);
			mLVContact.setAdapter(contactAdapter);
		}

		contactAdapter.updateData();

		contactAdapter.notifyDataSetChanged();
	}
}
