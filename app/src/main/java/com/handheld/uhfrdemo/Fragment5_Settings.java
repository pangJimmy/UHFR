package com.handheld.uhfrdemo;

import com.handheld.uhfr.R;
import com.uhf.api.cls.Reader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Fragment5_Settings extends Fragment implements OnClickListener{
	final String TAG = "Fragment5";
	private View view;

	private Spinner spinnerReadPower ;//read power select spinner
	private Spinner spinnerWritePower ;//write power select spinner
	private Spinner spinnerFreq ;//frequent
	private EditText edtTimeout;
	private Button btnGetPower ;
	private Button btnSetPower ;
	private Button btnGetFreq ;
	private Button btnSetTimeout;
//	private Button btnGetTime ;
//	private Button btnSetTime ;
//	private EditText editTextTime;

	private Button btnSetFreq ;
	private int timeout;

	private String[] powers;
	private String[] freqs;
	int[] mPowers = new int[2];
	private Reader.Region_Conf currentFreRegion ;

	private SharedPreferences mSharedPreferences;
	private SharedPreferences.Editor mEditor;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		view= inflater.inflate(R.layout.fragment_settings, null);
		mSharedPreferences = getActivity().getSharedPreferences("UHF", Context.MODE_PRIVATE);
		mEditor = mSharedPreferences.edit();
		timeout = mSharedPreferences.getInt("timeOut",10000);
		initView();
		//init share preferences
		return view;
	}
	private void initView(){
		powers = getResources().getStringArray(R.array.read_power);
		freqs = getResources().getStringArray(R.array.freregions);
		spinnerReadPower = (Spinner) view.findViewById(R.id.spinner_read_power) ;
		spinnerWritePower = (Spinner) view.findViewById(R.id.spinner_write_power) ;
		spinnerFreq = (Spinner) view.findViewById(R.id.spinner_freq) ;
		edtTimeout = view.findViewById(R.id.edit_timeout);
		edtTimeout.setText(String.valueOf(timeout));
		spinnerReadPower.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, powers));
		spinnerWritePower.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, powers));
		spinnerFreq.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, freqs));
		if(MainActivity.mUhfrManager != null){
			int[] ps = MainActivity.mUhfrManager.getPower() ;
			if (ps!=null) mPowers = ps;
			currentFreRegion = MainActivity.mUhfrManager.getRegion() ;
			if (currentFreRegion==Reader.Region_Conf.RG_PRC) spinnerFreq.setSelection(0);
			if (currentFreRegion==Reader.Region_Conf.RG_NA) spinnerFreq.setSelection(1);
			if (currentFreRegion==Reader.Region_Conf.RG_OPEN) spinnerFreq.setSelection(2);
			if (currentFreRegion==Reader.Region_Conf.RG_KR) spinnerFreq.setSelection(3);
			if (currentFreRegion==Reader.Region_Conf.RG_EU) spinnerFreq.setSelection(4);
			if (currentFreRegion==Reader.Region_Conf.RG_EU2) spinnerFreq.setSelection(5);
			if (currentFreRegion==Reader.Region_Conf.RG_EU3) spinnerFreq.setSelection(6);
		}
		btnGetPower = (Button) view.findViewById(R.id.button_get_power) ;
		btnSetPower = (Button) view.findViewById(R.id.button_set_power) ;
		btnGetFreq = (Button) view.findViewById(R.id.button_get_freq) ;
		btnSetFreq = (Button) view.findViewById(R.id.button_set_freq) ;
		btnSetTimeout = view.findViewById(R.id.btn_set_timeout);
		spinnerReadPower.setSelection(mPowers[0]);
		spinnerWritePower.setSelection(mPowers[1]);
		listenSpinner() ;
		btnGetPower.setOnClickListener(this);
		btnSetPower.setOnClickListener(this);
		btnGetFreq.setOnClickListener(this);
		btnSetFreq.setOnClickListener(this);
		btnSetTimeout.setOnClickListener(this);
	}
	private void listenSpinner() {
		spinnerReadPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mPowers[0] = position  ;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {


			}
		});
		spinnerWritePower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mPowers[1] = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		spinnerFreq.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch (position){
					case 0:
						currentFreRegion = Reader.Region_Conf.RG_PRC;
						break;
					case 1:
						currentFreRegion = Reader.Region_Conf.RG_NA;
						break;
					case 2:
						currentFreRegion = Reader.Region_Conf.RG_OPEN;
						break;
					case 3:
						currentFreRegion = Reader.Region_Conf.RG_KR;
						break;
					case 4:
						currentFreRegion = Reader.Region_Conf.RG_EU;
						break;
					case 5:
						currentFreRegion = Reader.Region_Conf.RG_EU2;
						break;
					case 6:
						currentFreRegion = Reader.Region_Conf.RG_EU3;
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();


	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.button_get_power:
				int[] ps = MainActivity.mUhfrManager.getPower();
				if (ps!=null){
					showToast(getString(R.string.success)+"ps0:"+ps[0]+",ps1:"+ps[1]);
					spinnerReadPower.setSelection(ps[0]);
					
					spinnerWritePower.setSelection(ps[1]);
				}else showToast(getString(R.string.fail));
				break;
			case R.id.button_set_power:
				Reader.READER_ERR err = MainActivity.mUhfrManager.setPower(mPowers[0],mPowers[1]);
				if (err== Reader.READER_ERR.MT_OK_ERR) {
					showToast(getString(R.string.success));
					mEditor.putInt("readPower", mPowers[0]);
					mEditor.putInt("writePower", mPowers[1]);
					mEditor.commit();
				}else
					showToast(getString(R.string.fail));
				break;
			case R.id.button_get_freq:
				Reader.Region_Conf region = MainActivity.mUhfrManager.getRegion();
				if (region!=null)
				{   currentFreRegion = region;
					if (currentFreRegion==Reader.Region_Conf.RG_PRC) spinnerFreq.setSelection(0);
					if (currentFreRegion==Reader.Region_Conf.RG_NA) spinnerFreq.setSelection(1);
					if (currentFreRegion==Reader.Region_Conf.RG_OPEN) spinnerFreq.setSelection(2);
					if (currentFreRegion==Reader.Region_Conf.RG_KR) spinnerFreq.setSelection(3);
					if (currentFreRegion==Reader.Region_Conf.RG_EU) spinnerFreq.setSelection(4);
					if (currentFreRegion==Reader.Region_Conf.RG_EU2) spinnerFreq.setSelection(5);
					if (currentFreRegion==Reader.Region_Conf.RG_EU3) spinnerFreq.setSelection(6);
					showToast(getString(R.string.success));
				}else showToast(getString(R.string.fail));
				break;
			case R.id.button_set_freq:
				Reader.READER_ERR er  = MainActivity.mUhfrManager.setRegion(currentFreRegion);
				if (er== Reader.READER_ERR.MT_OK_ERR) {
					showToast(getString(R.string.success));
					mEditor.putInt("freRegion",currentFreRegion.value());
					mEditor.commit();
				}
				else showToast(getString(R.string.fail));
				break;
//			case R.id.button_set_time:
//				if(!editTextTime.getText().equals("")&&Integer.valueOf(editTextTime.getText().toString())!=0){
//					timeout = Integer.valueOf( editTextTime.getText().toString().trim());
//					Log.e(TAG,timeout+"");
//					mEditor.putInt("timeOut", timeout);
//					mEditor.commit();
//				}else {
//					showToast("请输入超时时间");
//				}
//				break;
//			case R.id.button_get_time:
//				editTextTime.setText(String.valueOf(timeout));
//				break;
			case R.id.btn_set_timeout:
				int timeout = Integer.parseInt(edtTimeout.getText().toString().trim());
				mEditor.putInt("timeOut", timeout).apply();
				break;
			default:
				break;
		}
	}
	private Toast mToast;
	private void showToast(String info){
		if (mToast==null)
			mToast = Toast.makeText(getActivity(),info,Toast.LENGTH_SHORT);
		else
			mToast.setText(info);
		mToast.show();
	}


}
