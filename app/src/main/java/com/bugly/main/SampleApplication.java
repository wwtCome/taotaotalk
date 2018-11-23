package com.bugly.main;
import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by wuwentao on 2018/9/17.
 */

public class SampleApplication extends TinkerApplication {

    public SampleApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, SampleApplicationLike.class.getCanonicalName(),
                "com.tencent.tinker.loader.TinkerLoader", false);
    }
}
