package com.handheld.uhfrdemo;

import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

/**
 * description : ScanUtils for ScanService, to set ScanService
 * update : 2019/10/24 18:35,LeiHuang,Init commit
 *
 * @author : LeiHuang
 * @version : 1.0
 */
public class ScanUtil {

    private final String ACTION_SCAN_INIT = "com.rfid.SCAN_INIT";

    private final String ACTION_SET_SCAN_MODE = "com.rfid.SET_SCAN_MODE";

    private final String ACTION_SCAN = "com.rfid.SCAN_CMD";

    private final String ACTION_STOP_SCAN = "com.rfid.STOP_SCAN";

    private final String ACTION_CLOSE_SCAN = "com.rfid.CLOSE_SCAN";

    private final String ACTION_SCAN_TIME = "com.rfid.SCAN_TIME";

    private final String ACTION_SCAN_VOICE = "com.rfid.SCAN_VOICE";

    private final String ACTION_SCAN_VIBERATE = "com.rfid.SCAN_VIBERATE";

    private final String ACTION_SCAN_CONTINUOUS = "com.rfid.SCAN_CONTINUOUS";

    private final String ACTION_SCAN_INTERVAL = "com.rfid.SCAN_INTERVAL";

    private final String ACTION_SCAN_FILTER_BLANK = "com.rfid.SCAN_FILTER_BLANK";

    private final String ACTION_SCAN_FILTER_INVISIBLE_CHARS = "com.rfid.SCAN_FILTER_INVISIBLE_CHARS";

    private final String ACTION_SCAN_PREFIX = "com.rfid.SCAN_PREFIX";

    private final String ACTION_SCAN_SUFFIX = "com.rfid.SCAN_SUFFIX";

    private final String ACTION_SCAN_END_CHAR = "com.rfid.SCAN_END_CHAR";

    private final String ACTION_KEY_SET = "com.rfid.KEY_SET";

    private static WeakReference<Context> sWeakReference;

    private ScanUtil() {
    }

    public static ScanUtil getInstance(Context context) {
        sWeakReference = new WeakReference<>(context);
        return ScanUtilHolder.sScanUtils;
    }

    private static class ScanUtilHolder {
        private static ScanUtil sScanUtils = new ScanUtil();
    }

    /**
     * Enable scanner
     */
    public void initReader() {
        Intent intent = new Intent(ACTION_SCAN_INIT);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Set the mode to send barcodes or QR codes
     *
     * @param barcodeSendMode send mode; 0(Broadcast), 1(Focus), 2(EmuKey), 3(Clipboard); The default value is 1
     */
    public void setBarcodeSendMode(int barcodeSendMode) {
        Intent intent = new Intent(ACTION_SET_SCAN_MODE);
        intent.putExtra("mode", barcodeSendMode);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * If it is not scanning, start scanning.
     */
    public void startScan() {
        Intent intent = new Intent(ACTION_SCAN);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Stop scanning if it is scanning
     */
    public void stopScan() {
        Intent intent = new Intent(ACTION_STOP_SCAN);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Disable scanner
     */
    public void uninitReader() {
        Intent intent = new Intent(ACTION_CLOSE_SCAN);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Set scan timeout
     *
     * @param timeout 500 - 10000; The default value is 10000, and the unit is milliseconds
     */
    public void setDecodeTimeout(String timeout) {
        Intent intent = new Intent(ACTION_SCAN_TIME);
        intent.putExtra("time", timeout);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Enable or disable scan sounds
     *
     * @param voiceEnable true -- enable, false -- disable; The default value is true
     */
    public void setScanVoice(boolean voiceEnable) {
        Intent intent = new Intent(ACTION_SCAN_VOICE);
        intent.putExtra("sound_play", voiceEnable);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Enable or disable scanning vibration
     *
     * @param viberatEnable true -- enable, false -disable; The default value is false
     */
    public void setScanViberate(boolean viberatEnable) {
        Intent intent = new Intent(ACTION_SCAN_VIBERATE);
        intent.putExtra("viberate", viberatEnable);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Enable or disable continuous scan mode
     *
     * @param continu true -- enable, false -disable; The default value is false
     */
    public void setScanContinu(boolean continu) {
        Intent intent = new Intent(ACTION_SCAN_CONTINUOUS);
        intent.putExtra("ContinuousMode", continu);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Set the continuous scan interval
     *
     * @param interval 0 - 60000; The default value is 1000, and the unit is milliseconds
     */
    public void setScanContinuInterval(String interval) {
        Intent intent = new Intent(ACTION_SCAN_INTERVAL);
        intent.putExtra("ContinuousInternal", interval);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Set whether to filter the space before and after the scan result
     * @param isFilterBlank true -- filter, false -- do not filter; The default value is false
     */
    public void setScanFilterBlank(boolean isFilterBlank){
        Intent intent = new Intent(ACTION_SCAN_FILTER_BLANK);
        intent.putExtra("filter_prefix_suffix_blank", isFilterBlank);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Set whether to filter invisible characters in scan results
     * @param isFilterInvisible true -- filter, false -- do not filter; The default value is false
     */
    public void setScanFilterInvisibleChars(boolean isFilterInvisible){
        Intent intent = new Intent(ACTION_SCAN_FILTER_INVISIBLE_CHARS);
        intent.putExtra("filter_invisible_chars", isFilterInvisible);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Barcode and QR code prefix setting
     * @param prefix String type, defaults to ""
     */
    public void setScanPrefix(String prefix){
        Intent intent = new Intent(ACTION_SCAN_PREFIX);
        intent.putExtra("prefix", prefix);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Barcode and QR code suffix setting
     * @param suffix String type, defaults to ""
     */
    public void setScanSuffix(String suffix){
        Intent intent = new Intent(ACTION_SCAN_SUFFIX);
        intent.putExtra("suffix", suffix);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Barcode and QR code append end char settings
     * @param endChar String type, defaults to "NONE", the value can be "ENTER","TAB","SPACE","NONE"
     */
    public void setScanEndChar(String endChar){
        Intent intent = new Intent(ACTION_SCAN_END_CHAR);
        intent.putExtra("endchar", endChar);
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Enable scan key
     * @param keyValues key code
     */
    public void enableScanKey(String... keyValues){
        Intent intent = new Intent(ACTION_KEY_SET);
        intent.putExtra("keyValueArray", keyValues);
        for (String value : keyValues){
            intent.putExtra(value, true);
        }
        sWeakReference.get().sendBroadcast(intent);
    }

    /**
     * Disable scan key
     * @param keyValues keycode
     */
    public void disableScanKey(String... keyValues){
        Intent intent = new Intent(ACTION_KEY_SET);
        intent.putExtra("keyValueArray", keyValues);
        for (String value : keyValues){
            intent.putExtra(value, false);
        }
        sWeakReference.get().sendBroadcast(intent);
    }
}
