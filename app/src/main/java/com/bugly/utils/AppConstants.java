package com.bugly.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class AppConstants {
	public final static String DEFAULT_PERSONAL_HOST = "47.105.39.161";
	public final static String DEFAULT_ENTERPRISE_HOST = "";
	public final static boolean TEST_SMS = false;
	public final static boolean DEBUG = true;

	public final static int EXP_CHANNEL_ID = 1024;	//体验频道
	public final static int MOTOR_CHANNEL_ID = 1302;//体验频道
	public final static int HAM_CHANNEL_ID = 6702;	//体验频道
	public final static int CAR_CHANNEL_ID = 6704;	//体验频道
	public final static Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

	//用户名和密码的正则表达式
	public static final int NAME_MAX_LENGTH = 512;	//频道和用户名称的最大长度
	public static final String EX_CHANNELNAME = "[ \\-=\\w\\#\\[\\]\\{\\}\\(\\)\\@\\|]+";	//从服务器copy过来，与服务器保持一致
	public static final String EX_PASSWORD = "\\w{6,32}+";				//6-32位，不含空格
	public static final String EX_CHANNEL_PASSWORD = "^\\d{4}$";				//4-16位，不含空格
	public static final String EX_NICK = "[-=\\w\\[\\]\\{\\}\\(\\)\\@\\|\\. ]+";		//允许点和空格
	public static final String EX_PHONE = "^[1][3,4,5,7,8][0-9]{9}$";		//国内手机号
	public static final String EX_VERIFY_CODE = "^\\d{4}$";		//4位数字

	public final static int CURRENT_NICK_COLOR = Color.rgb(29, 103, 203);	//本人名字高亮显示颜色
	public final static int OTHER_NICK_COLOR = Color.rgb(0x33, 0x33, 0x33);	//别人名字

	//以下来自原来的Globals.java
	public static final String LOG_TAG = "Totalk";

	public enum NETWORK_STATE {DISCONNECT, WIFI, MOBILE}

	public static final String[] permReason =
			{
					"没有权限", "频道名称格式错误", "用户名格式错误", "频道人数已满", "频道已过期" ,
					"每个用户最多创建3个频道", "频道不存在", "频道口令错误"
			};
}
