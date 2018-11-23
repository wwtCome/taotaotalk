package com.bugly.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bugly.utils.AndroidUtils;
import com.kylindev.pttlib.db.ChatMessageBean;
import com.kylindev.pttlib.service.InterpttService;

import java.util.List;

/**
 * Created by wuwentao on 2018/10/27.
 */

public class DynamicReceiver extends BroadcastReceiver{
    private UserService userService;
    private InterpttService interpttService;
    private static long key1_down_time;//按键1按下的时间
    private static long key2_down_time;//按键2按下的时间

    public DynamicReceiver(UserService userService,InterpttService interpttService) {
        this.userService = userService;
        this.interpttService = interpttService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if("android.intent.action.button1Key".equals(action))
        {
            int keyCode=intent.getIntExtra("keycode", 0);
            if(keyCode == 1) {
                key1_down_time = System.currentTimeMillis();
            }else if(keyCode == 2)
            {
            }else{
                long duration = System.currentTimeMillis()-key1_down_time;
                if(duration < 2000)
                {
                    List<ChatMessageBean> tblist = userService.loadMoreRecords(interpttService.getCurrentChannel().id);
                    if(tblist == null || tblist.size() == 0)
                    {
//                        AndroidUtils.Log("tblist.size()="+"null");
                    }else{
//                        AndroidUtils.Log("tblist.size()="+tblist.size());
                        ChatMessageBean tbub = tblist.get(userService.getBackVoiceIndex());
                        byte[] data = tbub.getVoice();
                        if(data != null && data.length > 0)
                        {
                            interpttService.playback(data, interpttService.getCurrentChannel().id,0);
                        }
                    }
                }
            }
        }else if("android.intent.action.button2Key".equals(action))
        {
        }
    }
}
