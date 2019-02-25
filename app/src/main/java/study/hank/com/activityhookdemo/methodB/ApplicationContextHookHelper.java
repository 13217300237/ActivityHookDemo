package study.hank.com.activityhookdemo.methodB;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ApplicationContextHookHelper {
    public static void hook() {
        // 如何取得ActivityThread好像是单例的吧。那就简单了，
        try {
            //1.主线程ActivityThread内部的mInstrumentation对象，先把他拿出来
            Class<?> ActivityThreadClz = Class.forName("android.app.ActivityThread");
            //再拿到sCurrentActivityThread
            Field sCurrentActivityThreadField = ActivityThreadClz.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object activityThreadObj = sCurrentActivityThreadField.get(null);//静态变量的属性get不需要参数，传null即可.
            //再去拿它的mInstrumentation
            Field mInstrumentationField = ActivityThreadClz.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation base = (Instrumentation) mInstrumentationField.get(activityThreadObj);// OK,拿到

            //2.构建自己的代理对象，这里Instrumentation是一个class，而不是接口，所以只能用创建内部类的方式来做
            ProxyInstrumentation proxyInstrumentation = new ProxyInstrumentation(base);

            //3.偷梁换柱
            mInstrumentationField.set(activityThreadObj, proxyInstrumentation);

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

            Log.d("ApplicationContextHook", "我们自己的逻辑");

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
