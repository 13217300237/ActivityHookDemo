package study.hank.com.activityhookdemo.methodA;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActivityHookHelper {

    public static void hook(Activity activity) {

        //目标:Activity的mInstrumentation成员
        try {
            //1.拿到要hook的对象：Activity的mInstrumentation成员
            Field mInstrumentationField = Activity.class.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation base = (Instrumentation) mInstrumentationField.get(activity);

            //2.构建自己的代理对象，这里Instrumentation是一个class，而不是接口，所以只能用创建内部类的方式来做
            ProxyInstrumentation proxyInstrumentation = new ProxyInstrumentation(base);

            //3.替换
            mInstrumentationField.set(activity, proxyInstrumentation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ProxyInstrumentation extends Instrumentation {
        public ProxyInstrumentation(Instrumentation base) {
            this.base = base;
        }

        Instrumentation base;

        public ActivityResult execStartActivity(
                Context who, IBinder contextThread, IBinder token, Activity target,
                Intent intent, int requestCode, Bundle options) {

            Log.d("ActivityHook", "我们自己的逻辑");

            //这里还要执行系统的原本逻辑，但是突然发现，这个execStartActivity居然是hide的，只能反射咯
            try {
                Class<?> InstrumentationClz = Class.forName("android.app.Instrumentation");
                Method execStartActivity = InstrumentationClz.getDeclaredMethod("execStartActivity",
                        Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
                return (ActivityResult) execStartActivity.invoke(base, who, contextThread, token, target, intent, requestCode, options);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }
}
