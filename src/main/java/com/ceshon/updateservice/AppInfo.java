package com.ceshon.updateservice;


import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class AppInfo {


    /**
     * App Information
     * app versionCode
     * app versionName
     * app uuid
     */
    public AppInfo(){
    }

    private String appName;
    private int versionCode;
    private String versionName;
    private String PackageName;
    private Application application;

    public void  init(Application application){
        this.application = application;
        versionName = setVersionName();
        versionCode = setVersionCode();
        appName = setAppName();
        PackageName = setPackageName();
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return PackageName;
    }

    private String setAppName(){
        String name="";
        PackageManager packageManager = application.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(application.getApplicationInfo().packageName, 0);
            name = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : name);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }
    private String setPackageName(){
        return application.getPackageName();
    }

    private String setVersionName(){
        String  version ="null";
        try {
            PackageManager pm = application.getPackageManager();
            PackageInfo p1 = pm.getPackageInfo(application.getPackageName(), 0);
            version= p1.versionName;
            if (TextUtils.isEmpty(version) || version.length()<=0){
                return "";
            }
        }catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
        }
        return version;
    }
    private int setVersionCode() {
        int code = 0;
        try {
            PackageManager pm = application.getPackageManager();
            PackageInfo p1 = pm.getPackageInfo(application.getPackageName(), 0);
            code = p1.versionCode;
            if (code<=0){
                return 0;
            }
        }catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        }
        return code;
    }

    public int getVersionCode(){
        return this.versionCode;
    }
    public String VersionName(){
        return  this.versionName;
    }
    public int getVersionName(){
//        Log.e("###",VersionName().replace(".",""));
        return Integer.parseInt(VersionName().replace(".",""));
    }



}
