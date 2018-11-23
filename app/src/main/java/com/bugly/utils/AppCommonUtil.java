package com.bugly.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import com.bugly.utils.AppConstants.NETWORK_STATE;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppCommonUtil {
	public static boolean isServiceRunning(Context mContext, String className) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		//200 安全起见，此值选大点，以免不够
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(200);

		if (!(serviceList.size()>0)) {
			return false;
		}

		for (int i=0; i<serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}

	private static boolean validEnterpriseUsername(String str) {
		return (matchPattern(str, "^[a-zA-Z0-9_]*$"));	//字母，数字，下划线
	}

	public static boolean validUserId(String str) {
		return validPhone(str) || validTotalkId(str);
	}

	//7位滔滔号，100万到1000万-1
	public static boolean validTotalkId(String str) {
		if (str == null) return false;

		if (! str.matches("\\d+")) {
			return false;
		}

		int id = 0;
		try {
			id = Integer.parseInt(str);
		} catch (Exception e) {
			return false;
		}

		return (id>1000000 && id<9999999);
	}

	public static boolean validChannelName(String str) {
		return (matchPattern(str, AppConstants.EX_CHANNELNAME) && str.length()<=AppConstants.NAME_MAX_LENGTH);
	}

	public static boolean validPwd(String str) {
		return (matchPattern(str, AppConstants.EX_PASSWORD));
	}

	public static boolean validChannelPwd(String str) {
		return (matchPattern(str, AppConstants.EX_CHANNEL_PASSWORD) && !hasChinese(str));
	}

	public static boolean validNick(String str) {
		return (matchPattern(str, AppConstants.EX_NICK) && str.length()<=AppConstants.NAME_MAX_LENGTH);
	}

	public static boolean validPhone(String str) {
		return (matchPattern(str, AppConstants.EX_PHONE));
	}

	public static boolean validCode(String str) {
		return (matchPattern(str, AppConstants.EX_VERIFY_CODE));
	}

	public static boolean validChannelId(String str) {
		if (str == null) return false;

		if (! str.matches("\\d+")) {
			return false;
		}

		int id = 0;
		try {
			id = Integer.parseInt(str);
		} catch (Exception e) {
			return false;
		}

		return (id>0 && id<999999);
	}

	private static boolean matchPattern(String source, String ex) {
		if (source == null)
			return false;

		Pattern pattern = Pattern.compile(ex);
		Matcher matcher = pattern.matcher(source);

		return matcher.matches();
	}

	/**
	 * 中文识别
	 */
	public static boolean hasChinese(String source) {
		String reg_charset = "([\\u4E00-\\u9FA5]*+)";
		Pattern p = Pattern.compile(reg_charset);
		Matcher m = p.matcher(source);
		boolean hasChinese = false;
		while (m.find()) {
			if(!"".equals(m.group(1))){
				hasChinese=true;
			}
		}

		return hasChinese;
	}

	public static String transferString(String in) {
		if (in==null || in.length()==0) {
			return null;
		}

		if (in.equals("wrong password")) {
			return "密码错误";
		}
		if (in.equals("unknown user")) {
			return "用户名未注册";
		}
		if (in.equals("cannot login now")) {
			return "暂时无法登录";
		}
		if (in.equals("Username already in use")) {
			return "用户名已占用";
		}
		if (in.contains("Server is full")) {
			return "服务器人数已达上限";
		}
		if (in.equals("cannot login now")) {
			return "暂时无法登录";
		}

		return in;
	}

	/*
	 * 0:无网络连接；1：wifi；2：移动网络
	 */
	public static NETWORK_STATE getNetworkState(Context context) {
		try {
			// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivityManager != null) {
				NetworkInfo info = connectivityManager.getActiveNetworkInfo();

				// 获取网络连接管理的对象
				if (info != null && info.isAvailable() && info.isConnected()) {
					// 判断当前网络是否已经连接
					if (info.getState() == NetworkInfo.State.CONNECTED) {
						if(info.getType() == ConnectivityManager.TYPE_WIFI) {
							return NETWORK_STATE.WIFI;
						}
						else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
							return NETWORK_STATE.MOBILE;
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return NETWORK_STATE.DISCONNECT;
	}

	public static int wifiIp(Context context) {
		WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiMan.getConnectionInfo();

		return info.getIpAddress();
	}

	public static boolean existSDCard() {
		boolean flag = false;
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			flag = true;
		}
		return flag;
	}

	public static String getSdCardDirectory() {
		File path = Environment.getExternalStorageDirectory();
		return path.getPath();
	}

	public static Animation createTalkingAnimation() {
		Animation a = new AlphaAnimation((float)1.0,(float)0.2); // Change alpha from fully visible to invisible
		a.setDuration(300);
		a.setInterpolator(new LinearInterpolator()); // do not alter animation rate
		a.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
		a.setRepeatMode(Animation.REVERSE); //

		return a;
	}

	public static Animation createRotateAnimation() {
		//参数1：从哪个旋转角度开始
		//参数2：转到什么角度
		//后4个参数用于设置围绕着旋转的圆的圆心在哪里
		//参数3：确定x轴坐标的类型，有ABSOLUT绝对坐标、RELATIVE_TO_SELF相对于自身坐标、RELATIVE_TO_PARENT相对于父控件的坐标
		//参数4：x轴的值，0.5f表明是以自身这个控件的一半长度为x轴
		//参数5：确定y轴坐标的类型
		//参数6：y轴的值，0.5f表明是以自身这个控件的一半长度为x轴
		RotateAnimation a = new RotateAnimation(0, 360,
				Animation.RELATIVE_TO_SELF,0.5f,
				Animation.RELATIVE_TO_SELF,0.5f);
		a.setDuration(5000);
		a.setRepeatCount(-1);
		a.setInterpolator(new LinearInterpolator());	//匀速

		return a;
	}

	/**
	 * 得到设备屏幕的宽度
	 */
	public static int getScreenWidth(Context context) {
		return context.getResources().getDisplayMetrics().widthPixels;
	}

	/**
	 * 得到设备屏幕的高度
	 */
	public static int getScreenHeight(Context context) {
		return context.getResources().getDisplayMetrics().heightPixels;
	}

	public static String generateRandomStr(int len) {
		//字符源，可以根据需要删减
		String generateSource = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String rtnStr = "";
		for (int i = 0; i < len; i++) {
			//循环随机获得当次字符，并移走选出的字符
			String nowStr = String.valueOf(generateSource.charAt((int) Math.floor(Math.random() * generateSource.length())));
			rtnStr += nowStr;
			generateSource = generateSource.replaceAll(nowStr, "");
		}
		return rtnStr;
	}

	/**
	 * 十六进制转换字符串
	 * @return String 对应的字符串
	 */
	public static String hexStr2Str(String hexStr)
	{
		String str = "0123456789abcdef";
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;

		for (int i = 0; i < bytes.length; i++)
		{
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}


	/**
	 * 将彩色图转换为灰度图
	 * @param img 位图
	 * @return  返回转换好的位图
	 */
	public static Bitmap convertGreyImg(Bitmap img) {
		int width = img.getWidth();         //获取位图的宽
		int height = img.getHeight();       //获取位图的高

		int []pixels = new int[width * height]; //通过位图的大小创建像素点数组

		img.getPixels(pixels, 0, width, 0, 0, width, height);
		int alpha = 0xFF << 24;
		for(int i = 0; i < height; i++)  {
			for(int j = 0; j < width; j++) {
				int grey = pixels[width * i + j];

				int red = ((grey  & 0x00FF0000 ) >> 16);
				int green = ((grey & 0x0000FF00) >> 8);
				int blue = (grey & 0x000000FF);

				grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
				grey = alpha | (grey << 16) | (grey << 8) | grey;
				pixels[width * i + j] = grey;
			}
		}
		Bitmap result = Bitmap.createBitmap(width, height, AppConstants.BITMAP_CONFIG);
		result.setPixels(pixels, 0, width, 0, 0, width, height);
		return result;
	}

	public static boolean isPackageInstalled(String packagename, PackageManager packageManager) {
		try {
			packageManager.getPackageInfo(packagename, 0);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}
}
