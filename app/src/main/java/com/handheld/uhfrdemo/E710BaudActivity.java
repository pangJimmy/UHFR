package com.handheld.uhfrdemo;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.handheld.uhfr.R;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;

public class E710BaudActivity extends Activity implements View.OnClickListener{

    private TextView tvTips ;
    private Button btnInit ;
    private Button btnClose ;
    private Button btnModify ;

    public UHFRManager mUhfrManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baud);
        initView() ;
        initUhf() ;

    }

    private void initUhf() {
        mUhfrManager = UHFRManager.getInstance();
        if(mUhfrManager!=null){
            Reader.READER_ERR err = mUhfrManager.setPower(33, 3);//set uhf module power
            Log.e("MainActivity", "[onStart] setPower 33dBm: " + err);
            if(err== Reader.READER_ERR.MT_OK_ERR){
                mUhfrManager.setRegion(Reader.Region_Conf.valueOf(1));
                Toast.makeText(getApplicationContext(),"FreRegion:"+Reader.Region_Conf.valueOf(1)+
                        "\n"+"Read Power:"+33+
                        "\n"+"Write Power:"+33,Toast.LENGTH_LONG).show();
//                showToast(getString(R.string.inituhfsuccess));
            }else {

                Reader.READER_ERR err1 = mUhfrManager.setPower(30, 30);//set uhf module power
                Log.e("MainActivity", "[onStart] setPower 30dBm: " + err1);
                if(err1== Reader.READER_ERR.MT_OK_ERR) {
                    mUhfrManager.setRegion(Reader.Region_Conf.valueOf(1));
                    Toast.makeText(getApplicationContext(), "FreRegion:" + Reader.Region_Conf.valueOf(1) +
                            "\n" + "Read Power:" + 30 +
                            "\n" + "Write Power:" + 30, Toast.LENGTH_LONG).show();
                    tvTips.append("初始化超高频成功\n");
                }else {
//                    showToast(getString(R.string.inituhffail));
                    tvTips.append("初始化超高频失败\n");
                }
            }
        }else {
//            showToast(getString(R.string.inituhffail));
            tvTips.append("初始化超高频失败\n");
        }
    }

    private void closeUhf() {
        if (mUhfrManager != null) {
            mUhfrManager.close() ;
            tvTips.append("关闭超高频成功\n");
        }else {
            tvTips.append("未打开超高频\n");
        }
    }

    private void modifyBaud() {
        if (mUhfrManager != null) {
            boolean flag = mUhfrManager.setBaudrate(961200);
            tvTips.append("设置961200波特率:" + flag +"\n");
        }
    }

    private void initView() {
        tvTips = findViewById(R.id.textView_tips);
        btnInit = findViewById(R.id.button_init);
        btnClose = findViewById(R.id.button_close);
        btnModify = findViewById(R.id.button_modify);

        btnInit.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnModify.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_init:
                initUhf();
                break ;
            case R.id.button_close:
                closeUhf() ;
                break ;
            case R.id.button_modify:
                modifyBaud() ;
                break ;
        }
    }
}
