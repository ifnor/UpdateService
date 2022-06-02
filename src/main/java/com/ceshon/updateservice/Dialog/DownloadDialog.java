package com.ceshon.updateservice.Dialog;


import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;


import com.ceshon.updateservice.R;

public class DownloadDialog extends Dialog {

    private ProgressBar bar;
    private TextView num;

    public DownloadDialog(@NonNull Context context) {
        super(context, R.style.DownloadDialog);
        setContentView(R.layout.downloaddialog);

        //初始化界面控件
        initView();
//        //初始化界面数据
        refreshView();
//        //初始化界面控件的事件
    }

    @Override
    public void show() {
        super.show();
        refreshView();
    }


    private void initView() {
        bar = findViewById(R.id.UpdateDownloadBar);
        num = findViewById(R.id.UpdateDownloadNum);
    }


    private void refreshView() {
        setBar(0);
    }

    public void setBar(int pro){
        bar.setProgress(pro);
        num.setText(pro+"%");
    }




}
