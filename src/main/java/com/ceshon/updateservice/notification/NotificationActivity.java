package com.ceshon.updateservice.notification;


import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.ceshon.updateservice.R;
import com.ceshon.updateservice.AppInfo;
import static android.content.Context.NOTIFICATION_SERVICE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.O)
public class NotificationActivity extends Notification {
    private Application application;
    private String AppClass ="";
    private  Context context;
    private static Notification notification;
    private static NotificationManager manager;
    private String service = NOTIFICATION_SERVICE;


    /**
     * 设置通知图标
     * 默认为应用图标
     */
    private int Icon = R.drawable.update;
//    private Bitmap IconBitmap = BitmapFactory.decodeResource(context.getResources(),R.raw.favicon);
    public void setIcon(int icon) {
        this.Icon = icon;
        return;
    }

    /**
     * 设置通知大图
     * */
    private Bitmap Img;
    public void imgBitmap(String url){
        Img = BitmapFactory.decodeFile(url);
        Img = BitmapFactory.decodeResource(context.getResources(),R.raw.update);
    }



    public NotificationActivity(Application application) {
        this.application = application;
        this.AppClass =application.getPackageName();
        this.context =application.getApplicationContext();
        manager = (NotificationManager) context.getSystemService(service);
//        initChannel();
    }


    /**
     * 设置通知标题
     * 默认为 ”Notification“
     * 设置通知内容
     */
    private String title = "Notification";
    private String contentText =  " ";
    public void setContent(String title,String contentText){
        this.title = title;
        this.contentText = contentText;
    }

    /**
     * 设置通知类型描述及id
     * */
    private String description = "";
    private int id = -1;
    private String channelId = "channel_"+id;
    public void  Option(String description,int id){
        this.description = description;
        this.id = id;
    }

    /**
     * 设置点击跳转事件
     * FLAG_CANCEL_CURRENT：如果该PendingIntent已经存在，则在生成新的之前取消当前的。
     * FLAG_NO_CREATE：如果该PendingIntent不存在，直接返回null而不是创建一个PendingIntent.
     * FLAG_ONE_SHOT：该PendingIntent只能用一次，在send()方法执行后，自动取消。
     * FLAG_UPDATE_CURRENT：如果该PendingIntent已经存在，则用新传入的Intent更新当前的数据。
     */
    private PendingIntent pendingIntent = null;
    public PendingIntent getPendingIntent(Context context, Intent intent){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            return pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_ONE_SHOT);
        }
    }

    public void Options(String description,int id,Context context, Intent intent, int flags){
        Option(description,id);
        getPendingIntent(context,  intent);
    }


    /**
     * 设置震动与呼吸灯LED
     */
    private boolean light,Vibration = false;
    public void setLightVibration(boolean l,boolean v) {
        this.light = l;
        this.Vibration = v;
    }
    private boolean setSound = true;
    public boolean setSound(boolean is){
        return setSound = is;
    }
    private void setSound (NotificationChannel channel){
        if(setSound){
            channel.setSound(Uri.parse("android.resource://"+AppClass+"/" + R.raw.notice),Notification.AUDIO_ATTRIBUTES_DEFAULT);
        }else {
            channel.setSound(null,null);
        }

    }

    /**
     * 删除通知
     * 1.all全部
     * 2.按照id删除
     * 3.按照id和tag删除
     */
    public void delete(){
        manager.cancelAll();
    }
    public void delete(int id){
        manager.cancel(id);
    }
    public void delete(String tag,int id){
        manager.cancel(tag,id);
    }


    @SuppressLint("WrongConstant")
    protected void initChannel(){
        if (pendingIntent==null){
//            getPendingIntent(context,new Intent(context, application.getClass()),FLAG_CANCEL_CURRENT);
        }
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(channelId, description, importance);
        channel.setVibrationPattern(new long[]{0,1000,1000,1000});
        channel.setLightColor(5);
        channel.enableLights(light);
        channel.enableVibration(Vibration);
        channel.setImportance(NotificationManager.IMPORTANCE_MAX);
        channel.shouldShowLights();
        channel.shouldVibrate();
        setSound(channel);
//        channel.setSound(null,null);
//        channel.setSound(Uri.parse("android.resource://"+AppClass+"/" + R.raw.notice),Notification.AUDIO_ATTRIBUTES_DEFAULT);
        manager.createNotificationChannel(channel);
    }
    private void setFlag(Notification notification){
        notification.flags  = Notification.FLAG_AUTO_CANCEL|NotificationCompat.FLAG_BUBBLE;//将重复音频，直到取消通知或打开通知窗口，且设置点击通知后，通知自动消失
    }

    @SuppressLint("WrongConstant")
    public void show(){
        new Thread(new Runnable(){
            public void run(){
                Log.e("Thread-noti",Thread.currentThread().getName());
                initChannel();
                notification = new NotificationCompat.Builder(context, channelId)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setSmallIcon(R.drawable.update)
//                        .setActions()
                        .setSmallIcon(Icon)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .setContentTitle(title)
                        .setContentText(contentText)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.BADGE_ICON_LARGE)
//                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .build();
                setFlag(notification);
                manager.notify(id, notification);
            }}).start();

    }
    public void showRich(){
        new Thread(new Runnable(){
            public void run(){
                Log.e("Thread-noti-Rich",Thread.currentThread().getName());
                initChannel();
                notification = new NotificationCompat.Builder(context, channelId)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setSmallIcon(R.drawable.update)
//                        .setActions()
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .setContentTitle(title)
                        .setContentText(contentText)
//                        .setStyle(new BigTextStyle().bigText(contentText))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.BADGE_ICON_LARGE)
//                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .build();
                setFlag(notification);
                manager.notify(id, notification);
            }}).start();

    }


    public void showImg(String urls){
        imgBitmap(urls);
        new Thread(new Runnable(){
            public void run(){
                Log.e("Thread-noti-Img",Thread.currentThread().getName());
                initChannel();
                notification = new NotificationCompat.Builder(context, channelId)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setSmallIcon(R.drawable.update)
//                        .setActions()
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .setContentTitle(title)
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(Img))
                        .setStyle(new NotificationCompat.BigPictureStyle().setBigContentTitle(title))
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.BADGE_ICON_LARGE)
//                        .setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .build();
                setFlag(notification);
                manager.notify(id, notification);
            }}).start();

    }


    public void showRemoteView(String url) {
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent","TVFix App Loading Image")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("PUSH_TAG", "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) {

                InputStream inputStream = response.body().byteStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Log.i("PUSH_TAG",bitmap.getByteCount()+"");
                Message message = Message.obtain();
                message.obj = bitmap;
                handler.sendMessage(message);
            }
        });
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            AppInfo appInfo = new AppInfo();
            Date date = new Date();
            Log.i("PUSH_TAG","handleMessage_ok");
            @SuppressLint("RemoteViewLayout")
            RemoteViews remoteViews = new RemoteViews(appInfo.getPackageName(),
                    R.layout.layout_notifycation);
            remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.update);
            remoteViews.setTextViewText(R.id.tv_time,date.getHours()+":"+ date.getMinutes());
            remoteViews.setTextViewText(R.id.tv_title, title);
            remoteViews.setTextViewText(R.id.tv_description,
                    "    "+contentText.toString());
//            remoteViews.setBitmap(R.id.tv_image,"get", (Bitmap) msg.obj);
            remoteViews.setImageViewBitmap(R.id.tv_image, (Bitmap) msg.obj);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context,channelId);
            builder.setSmallIcon(R.drawable.update);
            builder.setContent(remoteViews);
            builder.setContentIntent(pendingIntent);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                builder.setCustomBigContentView(remoteViews);
            }
            setFlag(builder.build());
            manager.notify(id, builder.build());
        }
    };

}
