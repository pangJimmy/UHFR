package com.handheld.uhfrdemo;

import java.io.IOException;
import java.util.Set;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.handheld.uhfr.R;
import com.handheld.uhfr.RrReader;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;


public class MainActivity extends FragmentActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private FragmentManager mFm; //fragment manager
    private FragmentTransaction mFt;//fragment transaction
    private Fragment1_Inventory fragment1;//
    private Fragment2_ReadAndWrite fragment2;
    private Fragment3_Lock fragment3;
    private Fragment4_Kill fragment4;
    private Fragment5_Settings fragment5;
    public static UHFRManager mUhfrManager;//uhf
    public static Set<String> mSetEpcs; //epc set ,epc list
    private TextView textView_title;
    private TextView textView_f1;
    private TextView textView_f2;
    private TextView textView_f3;
    private TextView textView_f4;
    private TextView textView_f5;
    private ScanUtil instance;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"SourceLockedOrientationActivity", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        initView(); // Init UI

        Util.initSoundPool(this);//Init sound pool
        mSharedPreferences = this.getSharedPreferences("UHF", MODE_PRIVATE);


    }
    private SharedPreferences mSharedPreferences;
    @Override
    protected void onStart() {
        super.onStart();

        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion > Build.VERSION_CODES.N) {
            // For Android10.0 module
            instance = ScanUtil.getInstance(this);
            instance.disableScanKey("133");
            instance.disableScanKey("134");
            instance.disableScanKey("137");
        }

        UHFRManager.setDebuggable(true);
        mUhfrManager = UHFRManager.getInstance();// Init Uhf module
        if(mUhfrManager!=null){
            int session = mUhfrManager.getGen2session();
            System.out.println("session: " + session);
//            Reader.READER_ERR err = mUhfrManager.setPower(mSharedPreferences.getInt("readPower",33), mSharedPreferences.getInt("writePower",33));//set uhf module power
//            Log.e("MainActivity", "[onStart] setPower 33dBm: " + err);
//            if(err== Reader.READER_ERR.MT_OK_ERR){
//                mUhfrManager.setRegion(Reader.Region_Conf.valueOf(mSharedPreferences.getInt("freRegion", 1)));
//                Toast.makeText(getApplicationContext(),"FreRegion:"+Reader.Region_Conf.valueOf(mSharedPreferences.getInt("freRegion",1))+
//                        "\n"+"Read Power:"+mSharedPreferences.getInt("readPower",33)+
//                        "\n"+"Write Power:"+mSharedPreferences.getInt("writePower",33),Toast.LENGTH_LONG).show();
//                showToast(getString(R.string.inituhfsuccess));
//            }else {
//
//                Reader.READER_ERR err1 = mUhfrManager.setPower(30, 30);//set uhf module power
//                Log.e("MainActivity", "[onStart] setPower 30dBm: " + err1);
//                if(err1== Reader.READER_ERR.MT_OK_ERR) {
//                    mUhfrManager.setRegion(Reader.Region_Conf.valueOf(mSharedPreferences.getInt("freRegion", 1)));
//                    Toast.makeText(getApplicationContext(), "FreRegion:" + Reader.Region_Conf.valueOf(mSharedPreferences.getInt("freRegion", 1)) +
//                            "\n" + "Read Power:" + 30 +
//                            "\n" + "Write Power:" + 30, Toast.LENGTH_LONG).show();
//                }else {
//                    showToast(getString(R.string.inituhffail));
//                }
//            }
        }else {
            showToast(getString(R.string.inituhffail));
        }
    }


    @Override
    protected void onStop() {
        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion > Build.VERSION_CODES.N) {
            // For Android10.0 module
            instance.enableScanKey("133");
            instance.enableScanKey("134");
            instance.enableScanKey("137");
        }
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (mUhfrManager != null) {//close uhf module
            mUhfrManager.close();
            mUhfrManager = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub

        destroy = true;
        if (mUhfrManager != null) {//close uhf module
            mUhfrManager.close();
            mUhfrManager = null;
        }
        super.onDestroy();

    }

    private void initView() {
        fragment1 = new Fragment1_Inventory();
        mFragmentCurrent = fragment1;
        fragment2 = new Fragment2_ReadAndWrite();
        fragment3 = new Fragment3_Lock();
        fragment4 = new Fragment4_Kill();
        fragment5 = new Fragment5_Settings();

        mFm = getSupportFragmentManager();
        mFt = mFm.beginTransaction();
        mFt.add(R.id.framelayout_main, fragment1);
        mFt.commit();

        textView_title = (TextView) findViewById(R.id.title);
        textView_f1 = (TextView) findViewById(R.id.textView_f1);
        textView_f2 = (TextView) findViewById(R.id.textView_f2);
        textView_f3 = (TextView) findViewById(R.id.textView_f3);
        textView_f4 = (TextView) findViewById(R.id.textView_f4);
        textView_f5 = (TextView) findViewById(R.id.textView_f5);
        textView_f1.setOnClickListener(this);
        textView_f2.setOnClickListener(this);
        textView_f3.setOnClickListener(this);
        textView_f4.setOnClickListener(this);
        textView_f5.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_about) {
            PackageManager packageManager = getPackageManager();
            PackageInfo packInfo = null;
            try {
                packInfo = packageManager.getPackageInfo(getPackageName(), 0);
                String version = packInfo.versionName;//get this version
                showToast("Version:" + version
                    +"\nDate:"+"2021-10-29" +"\nType:"+mUhfrManager.getHardware());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private Fragment mFragmentCurrent;
    //switch fragments
    public void switchContent(Fragment to) {
//        Log.e("switch",""+to.getId());
        textView_f1.setTextColor(getResources().getColor(R.color.gre));
        textView_f2.setTextColor(getResources().getColor(R.color.gre));
        textView_f3.setTextColor(getResources().getColor(R.color.gre));
        textView_f4.setTextColor(getResources().getColor(R.color.gre));
        textView_f5.setTextColor(getResources().getColor(R.color.gre));
        if (mFragmentCurrent != to) {
            mFt = mFm.beginTransaction();
            if (!to.isAdded()) {    //
                mFt.hide(mFragmentCurrent).add(R.id.framelayout_main, to).commit(); //
            } else {
                mFt.hide(mFragmentCurrent).show(to).commit(); //
            }
            mFragmentCurrent = to;
        }
    }

    private boolean destroy = false;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textView_f1:
                switchContent(fragment1);
                textView_f1.setTextColor(getResources().getColor(R.color.blu));
                textView_title.setText(R.string.inventory_epc);
                break;
            case R.id.textView_f2:
                switchContent(fragment2);
                textView_f2.setTextColor(getResources().getColor(R.color.blu));
                textView_title.setText(R.string.read_write_tag);
                break;
            case R.id.textView_f3:
                switchContent(fragment3);
                textView_f3.setTextColor(getResources().getColor(R.color.blu));
                textView_title.setText(R.string.lock);
                break;
            case R.id.textView_f4:
                switchContent(fragment4);
                textView_f4.setTextColor(getResources().getColor(R.color.blu));
                textView_title.setText(R.string.kill);
                break;
            case R.id.textView_f5:
                switchContent(fragment5);
                textView_f5.setTextColor(getResources().getColor(R.color.blu));
                textView_title.setText(R.string.setting_);
                break;
        }
    }

//    private long exitTime = 0;//key down time
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_BACK:
//                if (System.currentTimeMillis() - exitTime >= 2000) {
//                    exitTime = System.currentTimeMillis();
//                    showToast(getString(R.string.quit_on_double_click_));
//                    return true;
//                } else {
//                    showToast(getString(R.string.exiting));
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    finish();
//                }
//                break;
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    private Toast mToast;
    //show toast
    private void showToast(String info) {
        if (mToast == null)
            mToast = Toast.makeText(this, info, Toast.LENGTH_SHORT);
        else
            mToast.setText(info);
        mToast.show();
    }

}
