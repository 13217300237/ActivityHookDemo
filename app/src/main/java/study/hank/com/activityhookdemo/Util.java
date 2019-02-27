package study.hank.com.activityhookdemo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Util {
    /**
     * 获取当前应用的第一个Activity的name
     *
     * @param context
     * @param pmName
     * @return
     */
    public static String getHostClzName(Context context, String pmName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pmName, PackageManager
                    .GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        ActivityInfo[] activities = packageInfo.activities;
        if (activities == null || activities.length == 0) {
            return "";
        }
        ActivityInfo activityInfo = activities[0];
        return activityInfo.name;

    }

    /**
     * 获取包名
     *
     * @param context
     * @return
     */
    public static String getPMName(Context context) {
        // 获取当前进程已经注册的 activity
        Context applicationContext = context.getApplicationContext();
        return applicationContext.getPackageName();
    }
}
