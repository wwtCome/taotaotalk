package com.bugly.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


import java.util.Observable;

/**
 * Singleton settings class for universal access to the app's preferences.
 * You can listen to LibSettings events by registering your class as an observer and responding according to the observer key passed.
 *
 * @author morlunk
 *
 */
public class AppSettings extends Observable {
	// If you can't find a specifically observable key here, listen for "all".
	//关键字
	public final static String KEY_PERSONAL_HOST = "key_personal_host";
	public final static String KEY_PERSONAL_USERID = "key_personal_userid";
	public final static String KEY_SINGIN_TIME = "key_singin_time";
	public final static String KEY_PERSONAL_PASSWORD = "key_personal_password";
	public final static String KEY_AUTO_LOGIN = "key_auto_login";
	public final static String KEY_AUTO_LAUNCH = "key_auto_launch";	//开机自启动
	//提示音量
	public final static String KEY_VOLUME_ONLINE = "key_volume_online";
	public final static String KEY_VOLUME_OFFLINE = "key_volume_offline";
	public final static String KEY_VOLUME_PRESS = "key_volume_press";
	public final static String KEY_VOLUME_I_BEGIN = "key_volume_i_begin";
	public final static String KEY_VOLUME_I_END = "key_volume_i_end";
	public final static String KEY_VOLUME_OTHER_BEGIN = "key_volume_other_begin";
	public final static String KEY_VOLUME_OTHER_END = "key_volume_other_end";

	public final static String KEY_FIRST_USE = "key_first_use";	//是否首次使用

	private final SharedPreferences preferences;

	private static AppSettings settings;

	public static AppSettings getInstance(Context context) {
		if(settings == null)
			settings = new AppSettings(context);
		return settings;
	}

	private AppSettings(final Context ctx) {
		preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	public String getUserid() {
		return preferences.getString(KEY_PERSONAL_USERID, null);
	}
	public void setUserid(String s) {
		preferences.edit().putString(KEY_PERSONAL_USERID, s).commit();
	}

	public int getSingInTime() {
		//默认签到30分钟不重复
		return preferences.getInt(KEY_SINGIN_TIME, 1800000);
	}
	public void setSingInTime(int s) {
		preferences.edit().putInt(KEY_SINGIN_TIME, s).commit();
	}

	public String getPassword() {
		return preferences.getString(KEY_PERSONAL_PASSWORD, null);
	}
	public void setPassword(String s) {
		preferences.edit().putString(KEY_PERSONAL_PASSWORD, s).commit();
	}

	public boolean getAutoLogin() {
//        boolean isN9I = (Build.MODEL!=null && Build.MODEL.contains("N9I"));
        return preferences.getBoolean(KEY_AUTO_LOGIN, true);
	}
	public void setAutoLogin(boolean b) {
		preferences.edit().putBoolean(KEY_AUTO_LOGIN, b).commit();
	}

	public boolean getAutoLaunch() {
//		boolean isN9I = (Build.MODEL!=null && Build.MODEL.contains("N9I"));
		return preferences.getBoolean(KEY_AUTO_LAUNCH, true);
	}
	public void setAutoLaunch(boolean b) {
		preferences.edit().putBoolean(KEY_AUTO_LAUNCH, b).commit();
	}

	public String getHost() {
		return preferences.getString(KEY_PERSONAL_HOST, AppConstants.DEFAULT_PERSONAL_HOST);
	}
	public void setHost(String s) {
		preferences.edit().putString(KEY_PERSONAL_HOST, s).commit();
	}

	public boolean getFirstUse() {
		return preferences.getBoolean(KEY_FIRST_USE, true);
	}
	public void setFirstUse(boolean b) {
		preferences.edit().putBoolean(KEY_FIRST_USE, b).commit();
	}

	//几种提示音量, 0-100
	public int getVolumeOnline() {
		return preferences.getInt(KEY_VOLUME_ONLINE, 100);
	}
	public void setKeyVolumeOnline(int v) {
		preferences.edit().putInt(KEY_VOLUME_ONLINE, v).commit();
	}
	public int getVolumeOffline() {
		return preferences.getInt(KEY_VOLUME_OFFLINE, 100);
	}
	public void setKeyVolumeOffline(int v) {
		preferences.edit().putInt(KEY_VOLUME_OFFLINE, v).commit();
	}

	public int getVolumePress() {
		return preferences.getInt(KEY_VOLUME_PRESS, 100);
	}
	public void setVolumePress(int v) {
		preferences.edit().putInt(KEY_VOLUME_PRESS, v).commit();
	}

	public int getVolumeIBegin() {
		return preferences.getInt(KEY_VOLUME_I_BEGIN, 100);
	}
	public void setVolumeIBegin(int v) {
		preferences.edit().putInt(KEY_VOLUME_I_BEGIN, v).commit();
	}

	public int getVolumeIEnd() {
		return preferences.getInt(KEY_VOLUME_I_END, 100);
	}
	public void setVolumeIEnd(int v) {
		preferences.edit().putInt(KEY_VOLUME_I_END, v).commit();
	}

	public int getVolumeOtherBegin() {
		return preferences.getInt(KEY_VOLUME_OTHER_BEGIN, 100);
	}
	public void setVolumeOtherBegin(int v) {
		preferences.edit().putInt(KEY_VOLUME_OTHER_BEGIN, v).commit();
	}

	public int getVolumeOtherEnd() {
		return preferences.getInt(KEY_VOLUME_OTHER_END, 100);
	}
	public void setVolumeOtherEnd(int v) {
		preferences.edit().putInt(KEY_VOLUME_OTHER_END, v).commit();
	}

}
