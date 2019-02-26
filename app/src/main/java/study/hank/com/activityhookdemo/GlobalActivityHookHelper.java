package study.hank.com.activityhookdemo;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * hook Activity的实现方式3:
 * hook AMS（ActivityManagerService）兼容 26以上，以及26以下的版本(SDK 26对AMS实例的获取进行了代码更改)
 *
 * 今天，在已经能够实现全局hook MS的方案下，进一步改造，实现 无清单启动Activity
 */
public class GlobalActivityHookHelper {

    //设备系统版本是不是大于等于26
    private static boolean ifSdkOverIncluding26() {
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 26 || SDK_INT == 26) {
            return true;
        } else {
            return false;
        }
    }

    public static void hook() {

        try {
            Class<?> ActivityManagerClz;
            final Object IActivityManagerObj;
            if (ifSdkOverIncluding26()) {
                ActivityManagerClz = Class.forName("android.app.ActivityManager");
                Method getServiceMethod = ActivityManagerClz.getDeclaredMethod("getService");
                IActivityManagerObj = getServiceMethod.invoke(null);//OK，已经取得这个系统自己的AMS实例
            } else {
                ActivityManagerClz = Class.forName("android.app.ActivityManagerNative");
                Method getServiceMethod = ActivityManagerClz.getDeclaredMethod("getDefault");
                IActivityManagerObj = getServiceMethod.invoke(null);//OK，已经取得这个系统自己的AMS实例
            }

            //2.现在创建我们的AMS实例
            //由于IActivityManager是一个接口，那么其实我们可以使用Proxy类来进行代理对象的创建
            // 结果被摆了一道，IActivityManager这玩意居然还是个AIDL，动态生成的类，编译器还不认识这个类，怎么办？反射咯
            Class<?> IActivityManagerClz = Class.forName("android.app.IActivityManager");
            Object proxyIActivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{IActivityManagerClz}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    //proxy是创建出来的代理类，method是接口中的方法，args是接口执行时的实参
                    if (method.getName().equals("startActivity")) {
                        Log.d("GlobalActivityHook", "全局hook 到了 startActivity");
                    }
                    return method.invoke(IActivityManagerObj, args);
                }
            });

            //3.偷梁换柱,这里有点纠结，这个实例居然被藏在了一个单例辅助类里面
            Field IActivityManagerSingletonField;
            if (ifSdkOverIncluding26()) {
                IActivityManagerSingletonField = ActivityManagerClz.getDeclaredField("IActivityManagerSingleton");
            } else {
                IActivityManagerSingletonField = ActivityManagerClz.getDeclaredField("gDefault");
            }

            IActivityManagerSingletonField.setAccessible(true);
            Object IActivityManagerSingletonObj = IActivityManagerSingletonField.get(null);
            Class<?> SingletonClz = Class.forName("android.util.Singleton");//反射创建一个Singleton的class
            Field mInstanceField = SingletonClz.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            mInstanceField.set(IActivityManagerSingletonObj, proxyIActivityManager);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
