package com.bugly.database;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bugly.R;
import com.bugly.signin.DevicePostion;
import com.bugly.signin.MemberInfo;
import com.bugly.signin.PttHttp;
import com.bugly.utils.AndroidUtils;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlTestActivity extends Activity {

    private TextView tv_show;
    private StringBuffer sb;
    private EditText et_update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sql_test);
        tv_show = (TextView) findViewById(R.id.show);
        et_update = (EditText) findViewById(R.id.et_update);
        sb = new StringBuffer();
    }

    public void taotaoqingqiu(View v)
    {
        AndroidUtils.Log("请求");
        final Map<String ,String > httpParams =  new HashMap<> (  );
        httpParams.put("uid","meinenguser7");
        new Thread()
        {
            @Override
            public void run ( ) {
                super.run ( );
                String uidBoundJson = PttHttp.getUidBoundJson(httpParams);
                AndroidUtils.Log("uidBoundJson数据" + uidBoundJson);
                MemberInfo memberInfo = AndroidUtils.PraMemberInfo(uidBoundJson);
            }
        }.start ();
    }

    /**
     * 增加一条
     * @param v
     */
    public void addone(View v)
    {
    }

    /**
     * 删除全部
     * @param v
     */
    public void delall(View v)
    {
        SqlUtils.deleteAll();
    }

    /**
     * 修改
     * @param view
     */
    public void update(View view)
    {
    }

    /**
     * 查询全部
     * @param v
     */
    public void findall(View v)
    {
        sb.delete(0,sb.length());
        List<AllBthDeviceInfo> all = SqlUtils.findAll();
        if(all != null && all.size() > 0)
        {
            for(AllBthDeviceInfo u: all)
            {
                sb.append(u.toString());
                sb.append("\r\n");
                sb.append("-----------------------------------------");
                AndroidUtils.Log(u.toString());
            }
            tv_show.setText(sb.toString());
        }else {
            tv_show.setText("null");
        }
    }
}
