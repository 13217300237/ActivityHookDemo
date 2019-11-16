package study.hank.com;

import android.app.Application;
import android.content.Context;

import me.weishu.reflection.Reflection;
import study.hank.com.activityhookdemo.GlobalActivityHookHelper;

public class AppMain extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalActivityHookHelper.hook(this);
    }
}
