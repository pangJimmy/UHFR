package com.handheld.uhfrdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handheld.uhfr.R;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;
import com.uhf.api.cls.Reader.TAGINFO;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cn.pda.serialport.Tools;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import static com.handheld.uhfrdemo.Util.context;


public class Fragment1_Inventory extends Fragment implements OnCheckedChangeListener, OnClickListener {
    final String TAG = "Fragment1";
    private View view;// this fragment UI
    private TextView tvTagCount;//tag count text view
    private TextView tvTagSum;//tag sum text view
    private TextView tvRunCount;//tag sum text view
    private TextView tvTitle;//tag sum text view
    private TextView tvErr;

    private ListView lvEpc;// epc list view
    private Button btnStart;//inventory button
    private Button btnClear;// clear button
    private Button btnExport;// clear button
        private Button btnTime;// clear button
    private CheckBox checkMulti;//multi model check box
    private CheckBox checkTid;//multi model check box
    private CheckBox checkPlay;//multi model check box

    private Set<String> epcSet = null; //store different EPC
    private List<EpcDataModel> listEpc = null;//EPC list
    private Map<String, Integer> mapEpc = null; //store EPC position
    private EPCadapter adapter;//epc list adapter

    private boolean isMulti = false;// multi mode flag
    private boolean isPlay = true;// multi mode flag
    private boolean isTid = false;// multi mode flag
    private int allCount = 0;// inventory count

    private long lastTime = 0L;// record play sound time
    private long timeout;
    private SharedPreferences mSharedPreferences;

    long statenvtick;
    //handler
    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String epc = msg.getData().getString("data");
                    String rssi = msg.getData().getString("rssi");
                    if (epc == null || epc.length() == 0) {
                        return;
                    }
                    int position;
                    allCount++;
                    if (epcSet == null) {//first
                        epcSet = new HashSet<String>();
                        listEpc = new ArrayList<EpcDataModel>();
                        mapEpc = new HashMap<String, Integer>();
                        epcSet.add(epc);
                        mapEpc.put(epc, 0);
                        EpcDataModel epcTag = new EpcDataModel();
                        epcTag.setepc(epc);
                        epcTag.setrssi(rssi);
                        epcTag.setCount(1);
                        listEpc.add(epcTag);
                        adapter = new EPCadapter(getActivity(), listEpc);
                        lvEpc.setAdapter(adapter);

                        MainActivity.mSetEpcs = epcSet;
                    } else {
                        if (epcSet.contains(epc)) {//set already exit
                            position = mapEpc.get(epc);
                            EpcDataModel epcOld = listEpc.get(position);
                            epcOld.setrssi(rssi);
                            epcOld.setCount(epcOld.getCount() + 1);
                            listEpc.set(position, epcOld);
                        } else {
                            epcSet.add(epc);
                            mapEpc.put(epc, listEpc.size());
                            EpcDataModel epcTag = new EpcDataModel();
                            epcTag.setepc(epc);
                            epcTag.setrssi(rssi);
                            epcTag.setCount(1);
                            listEpc.add(epcTag);
                            MainActivity.mSetEpcs = epcSet;
                        }
                        tvTagCount.setText("" + allCount);
                        tvTagSum.setText("" + listEpc.size());
                        adapter.notifyDataSetChanged();
                    }
                    break;
                case 1980:
                    String countString = tvRunCount.getText().toString();
                    if (countString.equals("") || countString == null) {
                        tvRunCount.setText(String.valueOf(1));
                    } else {
                        int previousCount = Integer.valueOf(countString);
                        int nowCount = previousCount + 1;
                        tvRunCount.setText(String.valueOf(nowCount));
                    }
                    break;
                case 1000:
                    btnStart.setEnabled(true);
                    btnExport.setEnabled(true);
                    btnTime.setEnabled(true);
                    showToast("Schedule inventory finished!");
                    break;
                case 404:// error info
                    tvErr.setText(UHFRManager.mErr.name());
                default:
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        Log.e("f1", "create view");
        view = inflater.inflate(R.layout.fragment_inventory, null);
        mSharedPreferences = getActivity().getSharedPreferences("UHF", Context.MODE_PRIVATE);
        timeout = mSharedPreferences.getInt("timeOut", 10000);
        Log.e(TAG, String.valueOf(timeout));
        initView();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.rfid.FUN_KEY");
        getActivity().getApplicationContext().registerReceiver(keyReceiver, filter);

        return view/*super.onCreateView(inflater, container, savedInstanceState)*/;
    }


    private void initView() {
        tvTagCount = (TextView) view.findViewById(R.id.textView_tag_count);
        lvEpc = (ListView) view.findViewById(R.id.listView_epc);
        btnStart = (Button) view.findViewById(R.id.button_start);
        btnTime = (Button) view.findViewById(R.id.button_time_start);
        tvTagSum = (TextView) view.findViewById(R.id.textView_tag);
        tvRunCount = (TextView) view.findViewById(R.id.textView_run_count);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvErr = (TextView) view.findViewById(R.id.textView_err);
        checkMulti = (CheckBox) view.findViewById(R.id.checkBox_multi);
        checkTid = (CheckBox) view.findViewById(R.id.checkBox_tid);
        checkPlay = (CheckBox) view.findViewById(R.id.checkBox_sound);
        checkMulti.setOnCheckedChangeListener(this);
        checkTid.setOnCheckedChangeListener(this);
        checkPlay.setOnCheckedChangeListener(this);
        btnClear = (Button) view.findViewById(R.id.button_clear_epc);
        btnExport = view.findViewById(R.id.button_export);
        lvEpc.setFocusable(false);
        lvEpc.setClickable(false);
        lvEpc.setItemsCanFocus(false);
        lvEpc.setScrollingCacheEnabled(false);
        lvEpc.setOnItemClickListener(null);
        btnStart.setOnClickListener(this);
        btnExport.setOnClickListener(this);
        btnTime.setOnClickListener(this);
        btnClear.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub

        if (isStart) {
            isStart = false;
//            isRunning = false;
            MainActivity.mUhfrManager.stopTagInventory();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {

            IntentFilter filter = new IntentFilter();
            filter.addAction("android.rfid.FUN_KEY");
            getActivity().getApplicationContext().registerReceiver(keyReceiver, filter);

        } else {
//			Log.e(TAG, "onStop() getActivity returns null");
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
//		Log.e("f1","pause");
        if (isStart) {
            isStart = false;
            handler1.removeCallbacks(runnable_MainActivity);
            btnStart.setText(this.getString(R.string.start_inventory_epc));
        }
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.getApplicationContext().unregisterReceiver(keyReceiver);
        } else {
//			Log.e(TAG, "onStop() getActivity returns null");
        }

    }

    private boolean f1hidden = false;
    private String TGA = "Fragment1";

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        f1hidden = hidden;
//		Log.e("hidden", "hide"+hidden) ;
        if (isStart) {
            isStart = false;
            handler1.removeCallbacks(runnable_MainActivity);
            btnStart.setText(this.getString(R.string.start_inventory_epc));
        }
        if (MainActivity.mUhfrManager != null) MainActivity.mUhfrManager.setCancleInventoryFilter();
    }

    //    private boolean isRunning = false;
    private boolean isStart = false;

    //inventory epc
    /**
     *
     */
    private Runnable runnable_MainActivity = new Runnable() {

        @Override
        public void run() {
            List<TAGINFO> list1;
            if (isMulti) {
                Log.e(TGA, "runnable-isMulti-true");
                list1 = MainActivity.mUhfrManager.tagInventoryRealTime();
            } else {
                if (isTid) {
                    list1 = MainActivity.mUhfrManager.tagEpcTidInventoryByTimer((short) 50);
                } else {
                    list1 = MainActivity.mUhfrManager.tagInventoryByTimer((short) 50);
                }
            }
            String data;
            handler1.sendEmptyMessage(1980);
            if (list1 == null) {
                // error info, Stop Inventory
                handler1.sendEmptyMessage(404);
                onClick(btnStart);
                return;
            }
            if (list1 != null && list1.size() > 0) {//
                Log.e(TGA, list1.size() + "");
                if (isPlay) {
                    Util.play(1, 0);
                }
                for (TAGINFO tfs : list1) {
                    byte[] epcdata = tfs.EpcId;
                    if (isTid) {
                        data = Tools.Bytes2HexString(tfs.EmbededData, tfs.EmbededDatalen);
                    } else {
                        data = Tools.Bytes2HexString(epcdata, epcdata.length);
                    }
                    int rssi = tfs.RSSI;
                    Message msg = new Message();
                    msg.what = 1;
                    Bundle b = new Bundle();
                    b.putString("data", data);
                    b.putString("rssi", rssi + "");
                    msg.setData(b);
                    handler1.sendMessage(msg);
                }
            }
            handler1.postDelayed(runnable_MainActivity, 0);
        }
    };

    private Runnable runnable_MainActivity1 = new Runnable() {
        @Override
        public void run() {
            List<TAGINFO> list1;
            if (isMulti) {
                Log.e(TGA, "[ScheduleInventoryTask] multi read");
                list1 = MainActivity.mUhfrManager.tagInventoryRealTime();
            } else {
                if (isTid) {
                    list1 = MainActivity.mUhfrManager.tagEpcTidInventoryByTimer((short) 50);
                } else {
                    list1 = MainActivity.mUhfrManager.tagInventoryByTimer((short) 50);
                }
            }
            String data;
            handler1.sendEmptyMessage(1980);
            if (list1 == null) {
                // error info, Stop schedule inventory
                handler1.sendEmptyMessage(404);
                onClick(btnTime);
            }
            if (list1 != null && list1.size() > 0) {
                Log.e(TGA, list1.size() + "");
                if (isPlay) {
                    Util.play(1, 0);
                }
                for (TAGINFO tfs : list1) {
                    byte[] epcdata = tfs.EpcId;
                    if (isTid) {
                        data = Tools.Bytes2HexString(tfs.EmbededData, tfs.EmbededDatalen);
                    } else {
                        data = Tools.Bytes2HexString(epcdata, epcdata.length);
                    }
                    int rssi = tfs.RSSI;
                    Message msg = new Message();
                    msg.what = 1;
                    Bundle b = new Bundle();
                    b.putString("data", data);
                    b.putString("rssi", rssi + "");
                    msg.setData(b);
                    handler1.sendMessage(msg);
                }
            }
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - statenvtick < timeout) {
                handler1.postDelayed(runnable_MainActivity1, 0);
            } else {
                isRead();
                handler1.sendEmptyMessage(1000);
            }
        }
    };

    private boolean keyControl = true;

    public void isRead() {
        if (MainActivity.mUhfrManager == null) {
            showToast(getActivity().getString(R.string.connection_failed));
            return;
        }
        if (!isStart) {
            // Clear err info
            tvErr.setText("");
            checkMulti.setEnabled(false);
            btnStart.setText(this.getString(R.string.stop_inventory_epc));
            MainActivity.mUhfrManager.setGen2session(isMulti);
            if (isMulti) {
                Log.e(TGA, "isMulti-true");
                MainActivity.mUhfrManager.asyncStartReading();
            }
            handler1.postDelayed(runnable_MainActivity, 0);
            isStart = true;
        } else {
            checkMulti.setEnabled(true);
            if (isMulti) {
                Log.e(TGA, "isMulti-true");
                MainActivity.mUhfrManager.asyncStopReading();
            }
            handler1.removeCallbacks(runnable_MainActivity);
            btnStart.setText(this.getString(R.string.start_inventory_epc));
            isStart = false;
        }
    }

    public void scheduleRead() {
        if (MainActivity.mUhfrManager == null) {
            showToast(getActivity().getString(R.string.connection_failed));
            return;
        }
        if (!isStart) {
            // Clear err info
            tvErr.setText("");
            checkMulti.setEnabled(false);
            btnStart.setText(this.getString(R.string.stop_inventory_epc));
            MainActivity.mUhfrManager.setGen2session(isMulti);
            if (isMulti) {
                MainActivity.mUhfrManager.asyncStartReading();
                Log.e(TGA, "[scheduleRead] multi read");
            }
            handler1.postDelayed(runnable_MainActivity1, 0);
            isStart = true;
        } else {
            checkMulti.setEnabled(true);
            if (isMulti) {
                Log.e(TGA, "isMulti-true");
                MainActivity.mUhfrManager.asyncStopReading();
            }
            handler1.removeCallbacks(runnable_MainActivity1);
            btnStart.setText(this.getString(R.string.start_inventory_epc));
            isStart = false;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.checkBox_multi:
                isMulti = isChecked;
                break;
            case R.id.checkBox_sound:
                isPlay = isChecked;
                break;
            case R.id.checkBox_tid:
                if (isChecked) {
                    isTid = true;
                    tvTitle.setText("TID");
                    isMulti = false;
                    checkMulti.setChecked(false);
                    checkMulti.setEnabled(false);
                } else {
                    isTid = false;
                    tvTitle.setText("EPC");
                    checkMulti.setEnabled(true);
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start:
                isRead();
                break;
            case R.id.button_clear_epc:
                clearEpc();
                break;
            case R.id.button_export:
                if (listEpc != null && listEpc.size() != 0) {
                    save(FileName());
                    Toast.makeText(getContext(), "Success" + listEpc.size(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Fila", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button_time_start:
                btnStart.setEnabled(false);
                btnExport.setEnabled(false);
                btnTime.setEnabled(false);
                statenvtick = System.currentTimeMillis();
                Log.e(TAG, "statetime:" + statenvtick);
                Log.e(TGA, "isMulti-true");
                scheduleRead();
                break;
            default:
                break;

        }
    }

    private void clearEpc() {
        if (epcSet != null) {
            epcSet.clear();
        }
        if (listEpc != null)
            listEpc.clear();
        if (mapEpc != null)
            mapEpc.clear(); //store EPC position
        if (adapter != null)
            adapter.notifyDataSetChanged();
        allCount = 0;
        tvTagSum.setText("0");
        tvTagCount.setText("0");
        tvRunCount.setText("0");
        if (MainActivity.mSetEpcs != null) {
            MainActivity.mSetEpcs.clear();
        }
//        lvEpc.removeAllViews();
    }

    //show tips
    private Toast toast;

    private void showToast(String info) {
        if (toast == null) toast = Toast.makeText(getActivity(), info, Toast.LENGTH_SHORT);
        else toast.setText(info);
        toast.show();
    }

    //key receiver
    private long startTime = 0;
    private boolean keyUpFalg = true;
    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (f1hidden) return;
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {//H941
                keyCode = intent.getIntExtra("keycode", 0);
            }
//            Log.e("key ","keyCode = " + keyCode) ;
            boolean keyDown = intent.getBooleanExtra("keydown", false);
//			Log.e("key ", "down = " + keyDown);
            if (keyUpFalg && keyDown && System.currentTimeMillis() - startTime > 500) {
                keyUpFalg = false;
                startTime = System.currentTimeMillis();
                if ((//keyCode == KeyEvent.KEYCODE_F1 || keyCode == KeyEvent.KEYCODE_F2
                        keyCode == KeyEvent.KEYCODE_F3 ||
//                                 keyCode == KeyEvent.KEYCODE_F4 ||
                                keyCode == KeyEvent.KEYCODE_F4
                || keyCode == KeyEvent.KEYCODE_F7)) {
//                Log.e("key ","inventory.... " ) ;
                    onClick(btnStart);
                }
                return;
            } else if (keyDown) {
                startTime = System.currentTimeMillis();
            } else {
                keyUpFalg = true;
            }

        }
    };

    public void save(/*List<String> listepc,*/ String fileName) {
        WritableWorkbook wwb = null;
        File file = new File(Environment.getExternalStorageDirectory() + "/EPC");
        if (!file.exists()) {
            file.mkdir();
            Log.e("hai-1", "1");
        }
        try {
            //Create a statistical file based on the current file path and instantiate an object that operates excel
            wwb = Workbook.createWorkbook(new File(Environment.getExternalStorageDirectory() + "/EPC/" + fileName));
            Log.e("hai-1", "2");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("hai-1", e.toString());
        }

        if (wwb != null) {
            // Create a tab at the bottom The parameters are the name of the tab and the index of the selection card
            WritableSheet writableSheet = wwb.createSheet("1", 0);
            //Create excel header information
            String[] topic = {"ID", "EPC", "Count"};
            for (int i = 0; i < topic.length; i++) {
                //Fill data in cells horizontally
                Label labelC = new Label(i, 0, topic[i]);
                try {
                    writableSheet.addCell(labelC);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }


            if (listEpc != null) {
                for (int i = 0; i < listEpc.size(); i++) {
                    Label labelC1 = new Label(0, i + 1, i + 1 + "");
                    Label labelC2 = new Label(1, i + 1, listEpc.get(i).getepc() + "");
                    Label labelC3 = new Label(2, i + 1, listEpc.get(i).getCount() + "");
                    try {
                        writableSheet.addCell(labelC1);
                        writableSheet.addCell(labelC2);
                        writableSheet.addCell(labelC3);
                    } catch (WriteException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                wwb.write();
                wwb.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        }
        sysToScan(Environment.getExternalStorageDirectory() + "/EPC/" + fileName);
    }

    public void sysToScan(String filePath) {
        //Scan files in the specified folder
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        //Broadcast to the system
        context.sendBroadcast(intent);
    }

    public String FileName() {
        String res;
        long time = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(time);
        res = simpleDateFormat.format(date).trim() + ".xls";
        Log.e("hai-1", res);
        return res;
    }


}
