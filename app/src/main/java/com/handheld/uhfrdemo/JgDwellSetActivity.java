package com.handheld.uhfrdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.handheld.uhfr.R;

public class JgDwellSetActivity extends Activity {

    private Spinner spDwell;
    private Spinner spJgTime;
    private SharedPreferences sharedPreferences;
    private int checkedRadio = 3;
    private RadioButton rb1;
    private RadioButton rb2;
    private RadioButton rb3;
    private RadioButton rb4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_jg_dwell);
        sharedPreferences = getSharedPreferences("UHF", MODE_PRIVATE);
        initView();
        initRadio();
        initListener();
    }

    private void initView() {
        String[] strjtTime = new String[7];
        for (int index = 0; index < 7; index++) {
            strjtTime[index] = (index * 10) + "ms";
        }
        spJgTime = findViewById(R.id.jgTime_spinner);
        ArrayAdapter<String> spada_jgTime = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, strjtTime);
        spada_jgTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJgTime.setAdapter(spada_jgTime);

        String[] dwelltime = new String[254];
        for (int index = 2; index < 256; index++) {
            dwelltime[index - 2] = (index * 100) + "ms";
        }
        spDwell = findViewById(R.id.dwell_spinner);
        ArrayAdapter<String> spada_dwell = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dwelltime);
        spada_dwell.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDwell.setAdapter(spada_dwell);

        rb1 = findViewById(R.id.rb_perf);
        rb2 = findViewById(R.id.rb_bal);
        rb3 = findViewById(R.id.rb_ene);
        rb4 = findViewById(R.id.rb_cus);
    }

    private void initListener() {
        rb1.setOnClickListener(v -> {
            spJgTime.setEnabled(false);
            spDwell.setEnabled(false);
            spJgTime.setSelection(0, false);
            spDwell.setSelection(8, false);
            checkedRadio = 1;
        });
        rb2.setOnClickListener(v -> {
            spJgTime.setEnabled(false);
            spDwell.setEnabled(false);
            spJgTime.setSelection(3, false);
            spDwell.setSelection(0, false);
            checkedRadio = 2;
        });
        rb3.setOnClickListener(v -> {
            spJgTime.setEnabled(false);
            spDwell.setEnabled(false);
            spJgTime.setSelection(6, false);
            spDwell.setSelection(0, false);
            checkedRadio = 3;
        });
        rb4.setOnClickListener(v -> {
            spJgTime.setEnabled(true);
            spDwell.setEnabled(true);
            checkedRadio = 4;
        });
        Button read = findViewById(R.id.ivt_read);
        Button set = findViewById(R.id.ivt_setting);
        read.setOnClickListener(v -> {
            int[] rrJgDwell = MainActivity.mUhfrManager.getRrJgDwell();
            if (rrJgDwell[0] != -1 && rrJgDwell[1] != -1) {
                spJgTime.setSelection(rrJgDwell[0], true);
                spDwell.setSelection(rrJgDwell[1] - 2, true);
                checkedRadio = sharedPreferences.getInt("checked_radio", 3);
                switch (checkedRadio) {
                    case 1:
                        rb1.setChecked(true);
                        spJgTime.setEnabled(false);
                        spDwell.setEnabled(false);
                        break;
                    case 2:
                        rb2.setChecked(true);
                        spJgTime.setEnabled(false);
                        spDwell.setEnabled(false);
                        break;
                    case 3:
                        rb3.setChecked(true);
                        spJgTime.setEnabled(false);
                        spDwell.setEnabled(false);
                        break;
                    case 4:
                    default:
                        spJgTime.setEnabled(true);
                        spDwell.setEnabled(true);
                        rb4.setChecked(true);
                        break;
                }
                showToast(getString(R.string.get_success_));
            } else {
                showToast(getString(R.string.get_fail_));
            }
        });
        set.setOnClickListener(v -> {
            int jgTimes = spJgTime.getSelectedItemPosition();
            int dwell = spDwell.getSelectedItemPosition();
            int i = MainActivity.mUhfrManager.setRrJgDwell(jgTimes, dwell + 2);
            if (i == 0) {
                sharedPreferences.edit().putInt("checked_radio", checkedRadio).apply();
                sharedPreferences.edit().putInt("jg_time", jgTimes).apply();
                sharedPreferences.edit().putInt("dwell", dwell + 2).apply();
                showToast(getString(R.string.set_success_));
            } else {
                showToast(getString(R.string.set_fail_));
            }
        });
    }

    private void initRadio() {
        checkedRadio = sharedPreferences.getInt("checked_radio", 3);
        int jgTime = sharedPreferences.getInt("jg_time", 6);
        int dwell = sharedPreferences.getInt("dwell", 2);
        switch (checkedRadio) {
            case 1:
                rb1.setChecked(true);
                spJgTime.setEnabled(false);
                spDwell.setEnabled(false);
                break;
            case 2:
                rb2.setChecked(true);
                spJgTime.setEnabled(false);
                spDwell.setEnabled(false);
                break;
            case 3:
                rb3.setChecked(true);
                spJgTime.setEnabled(false);
                spDwell.setEnabled(false);
                break;
            case 4:
            default:
                spJgTime.setEnabled(true);
                spDwell.setEnabled(true);
                rb4.setChecked(true);
                break;
        }
        spJgTime.setSelection(jgTime, true);
        spDwell.setSelection(dwell - 2, true);
        int i = MainActivity.mUhfrManager.setRrJgDwell(jgTime, dwell);
        if (i == 0) {
            showToast(getString(R.string.set_success_));
        } else {
            showToast(getString(R.string.set_fail_));
        }
    }

    private Toast toast;
    private void showToast(String str) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, str, Toast.LENGTH_SHORT);
        toast.show();
    }
}
