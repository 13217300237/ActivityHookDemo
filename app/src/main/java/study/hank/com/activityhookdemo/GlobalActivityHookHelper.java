package study.hank.com.activityhookdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * hookAMS Activity的实现方式3:
 * hookAMS AMS（ActivityManagerService）兼容 26以上，以及26以下的版本(SDK 26对AMS实例的获取进行了代码更改)
 * 今天，在已经能够实现全局hook MS的方案下，进一步改造，实现 无清单启动Activity
 */
public class GlobalActivityHookHelper {


    public static void hook(Context context) {

        hookAMS(context);//使用假的Activity，骗过AMS的检测

        hookActivityThread_mH(context);

        hookPMAfter28(context);//由于AppCompatActivity存在PMS检测，如果这里不hook的话，就会包PackageNameNotFoundException
    }

    //设备系统版本是不是大于等于29(Android 10)
    private static boolean ifSdkOverIncluding29() {
        int SDK_INT = Build.VERSION.SDK_INT;
        return SDK_INT >= 29;
    }

    //设备系统版本是不是大于等于26(Android 8.0 Oreo)
    private static boolean ifSdkOverIncluding26() {
        int SDK_INT = Build.VERSION.SDK_INT;
        return SDK_INT >= 26;
    }

    //设备系统版本是不是大于等于28(Android 9.0 Pie)
    private static boolean ifSdkOverIncluding28() {
        int SDK_INT = Build.VERSION.SDK_INT;
        return SDK_INT >= 28;
    }

    /**
     * 这里对AMS进行hook
     * ActivityManager(ActivityManagerNative)里的IActivityManager是一个单例，用我们的代理对象替换它!
     *
     * @param context
     */
    private static void hookAMS(Context context) {
        try {
            final Class<?> ActivityManagerClz;
            final String getServiceMethodStr;
            final String IActivityManagerSingletonFieldStr;
            if (ifSdkOverIncluding29()) {//29的ams获取方式是通过ActivityTaskManager.getService()
                ActivityManagerClz = Class.forName("android.app.ActivityTaskManager");
                getServiceMethodStr = "getService";
                IActivityManagerSingletonFieldStr = "IActivityTaskManagerSingleton";
            } else if (ifSdkOverIncluding26()) {//26，27，28的ams获取方式是通过ActivityManager.getService()
                ActivityManagerClz = Class.forName("android.app.ActivityManager");
                getServiceMethodStr = "getService";
                IActivityManagerSingletonFieldStr = "IActivityManagerSingleton";
            } else {//25往下，是ActivityManagerNative.getDefault()
                ActivityManagerClz = Class.forName("android.app.ActivityManagerNative");
                getServiceMethodStr = "getDefault";
                IActivityManagerSingletonFieldStr = "gDefault";
            }

            //这个就是ActivityManager实例
            Object ActivityManagerObj = ReflectUtil.invokeStaticMethod(ActivityManagerClz, getServiceMethodStr);
            //这个就是这个就是ActivityManager实例中的IActivityManager单例对象
            Object IActivityManagerSingleton = ReflectUtil.staticFieldValue(ActivityManagerClz,
                    IActivityManagerSingletonFieldStr);

            // 2.现在创建我们的IActivityManager实例
            // 由于IActivityManager是一个接口，那么其实我们可以使用Proxy类来进行代理对象的创建
            // 结果被摆了一道，IActivityManager这玩意居然还是个AIDL，动态生成的类，编译器还不认识这个类，怎么办？反射咯
            Class<?> IActivityManagerClz;
            if (ifSdkOverIncluding29()) {
                IActivityManagerClz = Class.forName("android.app.IActivityTaskManager");
            } else {
                IActivityManagerClz = Class.forName("android.app.IActivityManager");
            }


            // 构建代理类需要两个东西用于创建伪装的Intent
            String packageName = Util.getPMName(context);
            String clz = Util.getHostClzName(context, packageName);
            Object proxyIActivityManager =
                    Proxy.newProxyInstance(
                            Thread.currentThread().getContextClassLoader(),
                            new Class[]{IActivityManagerClz},
                            new AMSProxyInvocation(ActivityManagerObj, packageName, clz));

            //3.拿到AMS实例，然后用代理的AMS换掉真正的AMS，代理的AMS则是用 假的Intent骗过了 activity manifest检测.
            //偷梁换柱
            Field mInstanceField = ReflectUtil.findSingletonField("mInstance");
            mInstanceField.set(IActivityManagerSingleton, proxyIActivityManager);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String ORI_INTENT_TAG = "origin_intent";

    /**
     * 把InvocationHandler的实现类提取出来，因为这里包含了核心技术逻辑，最好独立，方便维护
     */
    private static class AMSProxyInvocation implements InvocationHandler {

        Object amObj;
        String packageName;//这两个String是用来构建Intent的ComponentName的
        String clz;

        public AMSProxyInvocation(Object amObj, String packageName, String clz) {
            this.amObj = amObj;
            this.packageName = packageName;
            this.clz = clz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.e("GlobalActivityHook", "method.getName() = " + method.getName());
            //proxy是创建出来的代理类，method是接口中的方法，args是接口执行时的实参
            if (method.getName().equals("startActivity")) {
                Log.d("GlobalActivityHook", "全局hook 到了 startActivity");

                Intent currentRealIntent = null;//侦测到startActivity动作之后，把intent存到这里
                int intentIndex = -1;
                //遍历参数，找到Intent
                for (int i = 0; i < args.length; i++) {
                    Object temp = args[i];
                    if (temp instanceof Intent) {
                        currentRealIntent = (Intent) temp;//这是原始的Intent,存起来,后面用得着
                        intentIndex = i;
                        break;
                    }
                }

                //构造自己的Intent，这是为了绕过manifest检测
                Intent proxyIntent = new Intent();
                ComponentName componentName = new ComponentName(packageName, clz);//用ComponentName重新创建一个intent
                proxyIntent.setComponent(componentName);
                proxyIntent.putExtra(ORI_INTENT_TAG, currentRealIntent);//将真正的proxy作为参数，存放到extras中，后面会拿出来还原

                args[intentIndex] = proxyIntent;//替换掉intent
                //哟，已经成功绕过了manifest清单检测. 那么，我不能老让它跳到 伪装的Activity啊，我要给他还原回去，那么，去哪里还原呢？
                //继续看源码。

            }
            return method.invoke(amObj, args);
        }
    }


    //下面进行ActivityThread的mH的hook,这是针对SDK28做的hook
    //将真实的Intent还原回去，让系统可以跳到原本该跳的地方.
    private static class ProxyHandlerCallback implements Handler.Callback {

        private int EXECUTE_TRANSACTION = 159;//这个值，是android.app.ActivityThread的内部类H 中定义的常量EXECUTE_TRANSACTION

        @Override
        public boolean handleMessage(Message msg) {
            boolean result = false;//返回值，请看Handler的源码，dispatchMessage就会懂了
            //Handler的dispatchMessage有3个callback优先级，首先是msg自带的callback，其次是Handler的成员mCallback,最后才是Handler类自身的handlerMessage方法,
            //它成员mCallback.handleMessage的返回值为true，则不会继续往下执行 Handler.handlerMessage
            //我们这里只是要hook，插入逻辑，所以必须返回false，让Handler原本的handlerMessage能够执行.
            if (msg.what == EXECUTE_TRANSACTION) {//这是跳转的时候,要对intent进行还原
                try {
                    //先把相关@hide的类都建好
                    Class<?> ClientTransactionClz = Class.forName("android.app.servertransaction.ClientTransaction");
                    Class<?> LaunchActivityItemClz = Class.forName("android.app.servertransaction.LaunchActivityItem");

                    Field mActivityCallbacksField = ClientTransactionClz.getDeclaredField("mActivityCallbacks");//ClientTransaction的成员
                    mActivityCallbacksField.setAccessible(true);
                    //类型判定，好习惯
                    if (!ClientTransactionClz.isInstance(msg.obj)) return true;
                    Object mActivityCallbacksObj = mActivityCallbacksField.get(msg.obj);//根据源码，在这个分支里面,msg.obj就是 ClientTransaction类型,所以，直接用
                    //拿到了ClientTransaction的List<ClientTransactionItem> mActivityCallbacks;
                    List list = (List) mActivityCallbacksObj;

                    if (list.size() == 0) return true;
                    Object LaunchActivityItemObj = list.get(0);//所以这里直接就拿到第一个就好了

                    if (!LaunchActivityItemClz.isInstance(LaunchActivityItemObj)) return true;
                    //这里必须判定 LaunchActivityItemClz，
                    // 因为 最初的ActivityResultItem传进去之后都被转化成了这LaunchActivityItemClz的实例

                    Field mIntentField = LaunchActivityItemClz.getDeclaredField("mIntent");
                    mIntentField.setAccessible(true);
                    Intent mIntent = (Intent) mIntentField.get(LaunchActivityItemObj);

                    Bundle extras = mIntent.getExtras();
                    if (extras != null) {
                        Intent oriIntent = (Intent) extras.get(ORI_INTENT_TAG);
                        //那么现在有了最原始的intent，应该怎么处理呢？
                        Log.d("1", "2");
                        mIntentField.set(LaunchActivityItemObj, oriIntent);
                    }

                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
    }

    private static void hookActivityThread_mH(Context context) {

        try {
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");

            Object sCurrentActivityThread = ReflectUtil.staticFieldValue(activityThreadClazz, "sCurrentActivityThread");

            Handler mH = (Handler) ReflectUtil.fieldValue(sCurrentActivityThread, "mH");

            Field mCallBackField = ReflectUtil.findField(Handler.class, "mCallback");

            Handler.Callback callback;
            if (ifSdkOverIncluding28()) {
                //2.现在，造一个代理
                // 他就是一个简单的Handler子类
                callback = new ProxyHandlerCallback();//不需要重写全部mH，只需要对mH的callback进行重新定义
            } else {
                callback = new ActivityThreadHandlerCallBack(context);
            }

            //3.替换
            //将Handler的mCallback成员，替换成创建出来的代理HandlerCallback
            mCallBackField.set(mH, callback);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static class ActivityThreadHandlerCallBack implements Handler.Callback {

        private final Context mContext;

        public ActivityThreadHandlerCallBack(Context context) {
            mContext = context;
        }

        @Override
        public boolean handleMessage(Message msg) {
            int LAUNCH_ACTIVITY = 0;
            try {
                Class<?> clazz = Class.forName("android.app.ActivityThread$H");
                LAUNCH_ACTIVITY = (int) ReflectUtil.staticFieldValue(clazz, "LAUNCH_ACTIVITY");
            } catch (Exception e) {
            }
            if (msg.what == LAUNCH_ACTIVITY) {
                handleLaunchActivity(mContext, msg);
            }
            return false;
        }
    }

    private static void handleLaunchActivity(Context context, Message msg) {
        try {
            Object obj = msg.obj;

            Intent proxyIntent = (Intent) ReflectUtil.fieldValue(obj, "intent");
            //拿到之前真实要被启动的Intent 然后把Intent换掉
            Intent originallyIntent = proxyIntent.getParcelableExtra(ORI_INTENT_TAG);
            if (originallyIntent == null) {
                return;
            }
            proxyIntent.setComponent(originallyIntent.getComponent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 由于我只在SDK 28 对应的9.0设备上做过成功的试验，所以此方法命名为hookPMAfter28
     *
     * @param context
     */
    private static void hookPMAfter28(Context context) {
        try {
            String pmName = Util.getPMName(context);
            String hostClzName = Util.getHostClzName(context, pmName);


            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Object sCurrentActivityThread = ReflectUtil.staticFieldValue(activityThreadClazz, "sCurrentActivityThread");//PM居然是来自ActivityThread


            Object iPackageManager = ReflectUtil.invokeMethod(sCurrentActivityThread, "getPackageManager");

            String packageName = Util.getPMName(context);
            PMSInvocationHandler handler = new PMSInvocationHandler(iPackageManager, packageName, hostClzName);
            Class<?> iPackageManagerIntercept = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new
                    Class<?>[]{iPackageManagerIntercept}, handler);
            // 获取 sPackageManager 属性
            Field sPackageManagerField = ReflectUtil.findField(sCurrentActivityThread, "sPackageManager");
            sPackageManagerField.set(sCurrentActivityThread, proxy);
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

    static class PMSInvocationHandler implements InvocationHandler {

        private Object base;
        private String packageName;
        private String hostClzName;

        public PMSInvocationHandler(Object base, String packageName, String hostClzName) {
            this.packageName = packageName;
            this.base = base;
            this.hostClzName = hostClzName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getName().equals("getActivityInfo")) {
                ComponentName componentName = new ComponentName(packageName, hostClzName);
                return method.invoke(base, componentName, PackageManager.GET_META_DATA, 0);//破费，一定是这样
            }

            return method.invoke(base, args);
        }
    }

}
