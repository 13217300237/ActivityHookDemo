package study.hank.com;

import android.app.Application;

import study.hank.com.activityhookdemo.GlobalActivityHookHelper;

public class AppMain extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GlobalActivityHookHelper.hook(this);
    }
}
