package com.ceshon.updateservice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.ceshon.updateservice.notification.NotificationActivity;
import com.ceshon.updateservice.Dialog.DownloadDialog;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateService {
    /**
     * static
     */
    private Application application;
    private AppInfo appInfo = new AppInfo();
    private AlertDialog.Builder dialog;
    private Context context;
    private Activity activity;
    private String url = "";
    private String downloadUrl = "";
    private String fileFullName = ".apk";
    private static String dir;
    private DownloadDialog dDialog;
    private NotificationActivity notificationActivity;
    private JSONObject jsonData;
    private Intent installIntent;

    private HHandle hHandle = new HHandle();

    final static int UpdateService_ID = 25;

    public UpdateService(Context context, Application application,Activity activity,String url) {
        appInfo.init(application);
        this.url = url;
        this.application = application;
        this.context = context;
        this.activity = activity;
        this.dialog = new AlertDialog.Builder(context);
        this.fileFullName = appInfo.getAppName()+fileFullName;
        verifyStoragePermissions(activity);
        dialog.setCancelable(false);
        dDialog = new DownloadDialog(context);
        notificationActivity = new NotificationActivity(application);
    }

    /**
     * 动态获取权限
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static void verifyStoragePermissions(Activity activity) {
        //检查权限
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //if not ,apply
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    /**
     * 检查更新
     */
    public void checkUpdate() {
        hHandle.sendEmptyMessage(302);
    }

    /** **************
     *  *** 消息处理 ***
     *  **************
     * UpdateService msg.what about 301~350
     * Download & showView info: DOWN_SHOW = 302
     * Downloader : 305~308
     *              305:downloading
     *              306:download susses
     *              307:download failed
     */
    class HHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            showDialog(msg.obj);
            Log.i("handleMessage-ups",msg.what+"");
            if (msg.what<100){
                dDialog.setBar(msg.what+1);
                notificationActivity.setContent("正在下载...","已下载..."+(msg.what+1)+"%");
                notificationActivity.Option("UpdateService",UpdateService_ID);
                notificationActivity.setSound(false);
                notificationActivity.show();

            }
            switch (msg.what) {
                case 302:
                    Log.e("301", Thread.currentThread().getName());
                    new Thread(new Runnable(){
                        public void run(){
                            getInfo();
                        }}).start();
                    break;
                case 303:
                    Log.e("302", Thread.currentThread().getName());
                    showDialog();
                    break;
                case 305:
                    notificationActivity.setContent("正在下载...","");
                    hHandle.sendEmptyMessage(0);
                    dDialog.show();
                    break;
                case 306:
                    Install();
                    notificationActivity.delete(UpdateService_ID);
                    notificationActivity.setContent("下载完成","下载完成请及时安装最新版本。");
                    notificationActivity.Option("UpdateService",UpdateService_ID);
                    notificationActivity.getPendingIntent(context,installIntent);
                    notificationActivity.show();

                    dDialog.dismiss();
                    dialog.setMessage("下载完成");
                    dialog.setNegativeButton("",null);
                    dialog.setPositiveButton("安装",new install());
//                    dialog.create();
                    dialog.show();
                    System.out.println(dir);
                    break;
                case 307:
                    dialog.setMessage("下载失败");
                    dialog.setNegativeButton("退出",new exitApp());
                    dialog.setPositiveButton("重新下载",new downloadApk());
//                    dialog.create();
                    dialog.show();
                    break;
                case 500:

                    break;

            }

        }
    }


    /**
     * updateService download service
     * download update infomation
     */
    private void getInfo() {
        new Thread(new Runnable(){
            public void run(){
                Log.e("getinfo",Thread.currentThread().getName());
                OkHttpClient okHttpClient = new OkHttpClient();
                final Request request = new Request.Builder()
                        .url(url)
                        .get()//默认就是GET请求，可以不写
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d("err", "onFailure: ");
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
//                Log.d("ok", "onResponse: " + response.body().string());
                        Log.e("Thread-ups", Thread.currentThread().getName());
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            jsonData = jsonObject;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        hHandle.sendEmptyMessage(303);
                    }
                });
            }}).start();
    }


    public void showDialog() {
        JSONObject data ;
        Message msg = new Message();
        try {
            data = (JSONObject)jsonData.get("data");
            if(appInfo.getVersionName()<Integer.parseInt(data.getString("version").replace(".",""))
                    ||appInfo.getVersionCode()!=Integer.parseInt(data.getString("versionCode"))) {
                Log.i("dddd", data.toString());
                downloadUrl = data.getString("downloadUrl");
                Log.e("Thread-ups-showDialog()", downloadUrl);
                dialog.setTitle("升级提示\t" + appInfo.VersionName() + "-->" + data.getString("version"));
                dialog.setIcon(R.drawable.update);
                dialog.setMessage(data.getString("versionDesc"));
                notificationActivity.setContent("有更新...", data.getString("versionDesc"));
                notificationActivity.Option("UpdateService", UpdateService_ID);
                notificationActivity.getPendingIntent(context, new Intent(context,activity.getClass()));
                notificationActivity.showRich();
                notificationActivity.setLightVibration(false, true);
                dialog.setPositiveButton("更新", new downloadApk());
                dialog.setNegativeButton("退出", new exitApp());
                dialog.create();
                dialog.show();
                msg.obj="update";
                msg.what = 500;
                hHandle.handleMessage(msg);
            }
            //Toast.makeText(context,"已是最新版本",Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            e.printStackTrace();

            msg.obj="已是最新版本";
            msg.what = 500;
            hHandle.handleMessage(msg);
        }

    }

    private class exitApp implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialogs, int i) {
            dialogs.cancel();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    private class downloadApk  implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialogs, int i) {
            File file = new File(dir+"/"+fileFullName);
            if(file.exists()){
                hHandle.sendEmptyMessage(306);
            }else {
                createThread();
                dialogs.dismiss();
                hHandle.sendEmptyMessage(305);
            }

        }
    }
    /**
     * 下载线程
     */
    private  void createThread() {
        new DownLoadThread().start();
    }
    private class DownLoadThread extends Thread{
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e("Thread-ups-downloader",Thread.currentThread().getName());
            String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_MOUNTED)){
                File SDpath = new File(context.getExternalCacheDir().getPath());
//                File SDpath = new File(context.getApplicationContext().getFilesDir().getAbsolutePath());
                dir = SDpath.toString();
            }
            try {
                downloadFile(downloadUrl,dir,fileFullName);
            } catch (Exception e) {
                e.printStackTrace();
//                message.what = DOWN_ERROR;
//                mHandler.sendEmptyMessage(404);
            }
        }
    }




    /**
     * 安装apk
     * */
    private class install extends Thread implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialogs, int which) {
            try {
                installApk();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void installApk() {
        File file = new File(dir+"/"+fileFullName);// /storage/emulated/0/Android/data/com.ceshon.myinfo/cache/ceshon/d/App.apk
        System.out.println(file.toString());
        Uri uri=null;
        try {
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//为intent 设置特殊的标志，会覆盖 intent 已经设置的所有标志。
            if(Build.VERSION.SDK_INT>=24){//7.0 以上版本利用FileProvider进行访问私有文件
                uri= FileProvider.getUriForFile(context,context.getPackageName() + ".fileprovider",file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//为intent 添加特殊的标志，不会覆盖，只会追加。
            }
            else {
                //直接访问文件
                uri=Uri.fromFile(file);
                intent.setAction(Intent.ACTION_VIEW);
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过通知栏安装
     * */
    protected void Install(){
        File file = new File(dir+"/"+fileFullName);// /data/user/0/net.coding.android_demo/files/ceshon/d/App.apk
        System.out.println(file.toString());
        Uri uri=null;
        installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//为intent 设置特殊的标志，会覆盖 intent 已经设置的所有标志。
        if(Build.VERSION.SDK_INT>=24){//7.0 以上版本利用FileProvider进行访问私有文件
            uri= FileProvider.getUriForFile(context,context.getPackageName() + ".fileprovider",file);
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//为intent 添加特殊的标志，不会覆盖，只会追加。
        }
        else {
            //直接访问文件
            uri=Uri.fromFile(file);
            installIntent.setAction(Intent.ACTION_VIEW);
        }
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
//        context.startActivity(intent);
    }




    /**
     * 下载器
     */
    protected final File downloadFile(String urlPath, String downloadDir,String fileFullName) throws Exception{

        File file = null;
        try {
            // 统一资源
            URL url = new URL(urlPath);
            // 连接类的父类，抽象类
            URLConnection urlConnection = url.openConnection();
            // http的连接类
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            //设置超时
            httpURLConnection.setConnectTimeout(1000*5);
            //设置请求方式，默认是GET
            httpURLConnection.setRequestMethod("GET");
            // 设置字符编码
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            // 打开到此 URL引用的资源的通信链接（如果尚未建立这样的连接）。
            httpURLConnection.connect();
            // 文件大小
            int fileLength = httpURLConnection.getContentLength();

            // 控制台打印文件大小
            System.out.println("您要下载的文件大小为:" + fileLength / (1024 * 1024) + "MB");

            // 建立链接从请求中获取数据
            URLConnection con = url.openConnection();
            BufferedInputStream bin = new BufferedInputStream(httpURLConnection.getInputStream());
            // 指定文件名称(有需求可以自定义)
//            String fileFullName = "aaa.apk";
            // 指定存放位置(有需求可以自定义)
            String path = downloadDir + File.separatorChar + fileFullName;
            file = new File(path);
            // 校验文件夹目录是否存在，不存在就创建一个目录
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            OutputStream out = new FileOutputStream(file);
            int size = 0;
            int len = 0;
            int per = 0;
            byte[] buf = new byte[2048];
            while ((size = bin.read(buf)) != -1) {
                len += size;
                out.write(buf, 0, size);
                if (per<(len * 100 / fileLength)){
                    // 控制台打印文件下载的百分比情况
                    System.out.println("下载了-------> " + (len * 100 / fileLength) + "%\n");
                    hHandle.sendEmptyMessage((len * 100 / fileLength));
                    per=(len * 100 / fileLength);
                }
            }
            // 关闭资源
            bin.close();
            out.close();
            hHandle.sendEmptyMessage(306);
            System.out.println("文件下载成功！");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("文件下载失败！");
        } finally {
            return file;
        }

    }
}
