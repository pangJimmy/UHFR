package com.handheld.uhfr;

import android.util.Log;

import com.uhf.api.cls.R2000_calibration.TagLED_DATA;
import com.uhf.api.cls.Reader;
import com.uhf.api.cls.Reader.AntPower;
import com.uhf.api.cls.Reader.AntPowerConf;
import com.uhf.api.cls.Reader.HardwareDetails;
import com.uhf.api.cls.Reader.HoptableData_ST;
import com.uhf.api.cls.Reader.Inv_Potl;
import com.uhf.api.cls.Reader.Inv_Potls_ST;
import com.uhf.api.cls.Reader.Lock_Obj;
import com.uhf.api.cls.Reader.Lock_Type;
import com.uhf.api.cls.Reader.Mtr_Param;
import com.uhf.api.cls.Reader.READER_ERR;
import com.uhf.api.cls.Reader.Region_Conf;
import com.uhf.api.cls.Reader.SL_TagProtocol;
import com.uhf.api.cls.Reader.TAGINFO;
import com.uhf.api.cls.Reader.TagFilter_ST;
import com.uhf.api.cls.Reader.deviceVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.pda.serialport.SerialPort;

/**
 * @author LeiHuang
 */
public class UHFRManager {

    private static SerialPort sSerialPort;
    private final String tag = "UHFRManager";
    private static Reader reader;
    private final int[] ants = new int[]{1};
    private final int ant = 1;
    public deviceVersion dv;
    public static READER_ERR mErr;

    /**
     * 是否开启了附加数据（作用域只在于本app中，非模块），避免每次调用盘存方法都需要设置该项
     */
//    private boolean MTR_PARAM_TAG_EMBEDEDDATA = true;

    private static boolean DEBUG = false;

    public static void setDebuggable(boolean debuggable) {
        DEBUG = debuggable;
    }

    private void logPrint(String content) {
        if (DEBUG) {
            Log.e("huang,UHFRManager", content);
        }
    }

    /**
     * init Uhf module
     *
     * @return UHFRManager
     */
    public static UHFRManager getInstance() {
        if (connect()) {
            return new UHFRManager();
        }
        return null;
    }

    public boolean close() {
        if (reader != null) {
            reader.CloseReader();
        }
        sSerialPort.power_5Voff();
        reader = null;
        return true;
    }

    public String getHardware() {
        HardwareDetails val = reader.new HardwareDetails();
        READER_ERR er = reader.GetHardwareDetails(val);
        if (er == READER_ERR.MT_OK_ERR) {
            return val.module.toString();
        }
        return null;
    }

    private static boolean connect() {
        reader = new Reader();
        try {
            sSerialPort = new SerialPort(13, 115200, 0);
            sSerialPort.close(13);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        sSerialPort.power_5Von();
        try {
            Thread.sleep(100L);
        } catch (InterruptedException var1) {
            var1.printStackTrace();
        }

        READER_ERR er = reader.InitReader_Notype("/dev/ttyMT1", 1);
        if (er == READER_ERR.MT_OK_ERR) {
            connect2();
            return true;
        } else {
            return false;
        }
    }

    private static boolean connect2() {
        Inv_Potls_ST ipst = reader.new Inv_Potls_ST();
        List<SL_TagProtocol> ltp = new ArrayList<SL_TagProtocol>();
        ltp.add(SL_TagProtocol.SL_TAG_PROTOCOL_GEN2);
        ipst.potlcnt = ltp.size();
        ipst.potls = new Inv_Potl[ipst.potlcnt];
        SL_TagProtocol[] stp = ltp.toArray(new SL_TagProtocol[ipst.potlcnt]);
        for (int i = 0; i < ipst.potlcnt; i++) {
            Inv_Potl ipl = reader.new Inv_Potl();
            ipl.weight = 30;
            ipl.potl = stp[i];
            ipst.potls[0] = ipl;
        }

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst);
        return er == READER_ERR.MT_OK_ERR;
    }

    public READER_ERR asyncStartReading() {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
        logPrint("asyncStartReading, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
        // 设置gen2 tag编码(PROF)
//        int[] val = new int[] {19};//profile3，默认M4
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, val);
//        logPrint("asyncStartReading, ParamSet MTR_PARAM_POTL_GEN2_TAGENCODING result: " + er.toString());
        // option = 0, 多标签快速模式(不含附加数据)
        return reader.AsyncStartReading(ants, 1, 0);
        // option = 16, 多标签手持机模式(不含附加数据)
//        return reader.AsyncStartReading(ants, 1, 16);
    }

    public READER_ERR asyncStartReading(int option) {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
        logPrint("asyncStartReading, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
        // 设置gen2 tag编码(PROF)
//        int[] val = new int[] {19};//profile3，默认M4
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, val);
//        logPrint("asyncStartReading, ParamSet MTR_PARAM_POTL_GEN2_TAGENCODING result: " + er.toString());
        return reader.AsyncStartReading(ants, 1, option);
    }

    public READER_ERR asyncStopReading() {
        return reader.AsyncStopReading();
    }

    public boolean setInventoryFilter(byte[] fdata, int fbank, int fstartaddr,
                                      boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }
        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("setInventoryFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
            return false;
        }
        return true;
    }

    public boolean setCancleInventoryFilter() {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("setCancleInventoryFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
            return false;
        }
        return true;
    }

    public List<TAGINFO> tagInventoryRealTime() {
        READER_ERR er;
//        if (MTR_PARAM_TAG_EMBEDEDDATA) {
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
//        Log.d(tag, "[tagInventoryRealTime] : 0");
//        if (er == READER_ERR.MT_OK_ERR) {
//                MTR_PARAM_TAG_EMBEDEDDATA = false;
//        }
//        }
        List<TAGINFO> list = new ArrayList<>();
        int[] tagcnt = new int[1];
        er = reader.AsyncGetTagCount(tagcnt);
        if (er != READER_ERR.MT_OK_ERR) {
            mErr = er;
            return null;
        }
        for (int i = 0; i < tagcnt[0]; i++) {
            TAGINFO tfs = reader.new TAGINFO();
            er = reader.AsyncGetNextTag(tfs);
            if (er == READER_ERR.MT_OK_ERR) {
                list.add(tfs);
            }
        }
        return list;
    }

    public boolean stopTagInventory() {
        READER_ERR er = reader.AsyncStopReading();
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("stopTagInventory, AsyncStopReading result: " + er.toString());
            return false;
        }
        return true;
    }

    public List<TAGINFO> tagInventoryByTimer(short readtime) {
        READER_ERR er;
//        if (MTR_PARAM_TAG_EMBEDEDDATA) {
        er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
        logPrint("tagInventoryByTimer, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
//        if (er == READER_ERR.MT_OK_ERR) {
//                MTR_PARAM_TAG_EMBEDEDDATA = false;
//        }
//        }
        List<TAGINFO> list = new ArrayList<>();

        int[] tagcnt = new int[1];
        er = reader.TagInventory_Raw(ants, 1, readtime, tagcnt);
        if (er != READER_ERR.MT_OK_ERR) {
            mErr = er;
            return null;
        }
        for (int i = 0; i < tagcnt[0]; i++) {
            TAGINFO tfs = reader.new TAGINFO();
            er = reader.GetNextTag(tfs);
            if (er == READER_ERR.MT_OK_ERR) {
                list.add(tfs);
            }
        }
        return list;
    }

    /**
     * 盘存时附加数据读取tid（非FastTid）
     */
    public List<TAGINFO> tagEpcTidInventoryByTimer(short readtime) {
        List<TAGINFO> list = new ArrayList<>();
        READER_ERR er;

        Reader.EmbededData_ST edst = reader.new EmbededData_ST();
        edst.accesspwd = null;
        edst.bank = 2;
        edst.startaddr = 0;
        edst.bytecnt = 12;
        reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);

        int[] tagcnt = new int[1];
        er = reader.TagInventory_Raw(ants, 1, readtime, tagcnt);
        if (er != READER_ERR.MT_OK_ERR) {
            mErr = er;
            return null;
        }

        for (int i = 0; i < tagcnt[0]; i++) {
            TAGINFO tfs = reader.new TAGINFO();
            er = reader.GetNextTag(tfs);
            if (er == READER_ERR.MT_OK_ERR) {
                list.add(tfs);
            }
        }
        return list;
    }

    public List<TAGINFO> tagEpcOtherInventoryByTimer(short readtime, int bank, int startaddr, int bytecnt, byte[] accesspwd) {
        List<TAGINFO> list = new ArrayList<>();
        READER_ERR er;

        //by lbx 2017-4-27 get other:res=0, bank epc =1 tid=2 user =3
        Reader.EmbededData_ST edst = reader.new EmbededData_ST();
        edst.bank = bank;
        edst.startaddr = startaddr;
        edst.bytecnt = bytecnt;
        edst.accesspwd = accesspwd;
        reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);

        int[] tagcnt = new int[1];
        er = reader.TagInventory_Raw(ants, 1, readtime, tagcnt);
        if (er != READER_ERR.MT_OK_ERR) {
            mErr = er;
            return null;
        }

        for (int i = 0; i < tagcnt[0]; i++) {
            TAGINFO tfs = reader.new TAGINFO();
            er = reader.GetNextTag(tfs);
            if (er == READER_ERR.MT_OK_ERR) {
                list.add(tfs);
            }
        }
        return list;
    }

    public READER_ERR getTagData(int mbank, int startaddr, int len,
                                 byte[] rdata, byte[] password, short timeout) {

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er == READER_ERR.MT_OK_ERR) {
            int trycount = 3;
            do {
                er = reader.GetTagData(ant, (char) mbank, startaddr, len,
                        rdata, password, timeout);

                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != READER_ERR.MT_OK_ERR);

            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("getTagData, GetTagData result: " + er.toString());
            }
        } else {
            logPrint("getTagData, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public byte[] getTagDataByFilter(int mbank, int startaddr, int len,
                                     byte[] password, short timeout, byte[] fdata, int fbank,
                                     int fstartaddr, boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }
        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;

        byte[] rdata = new byte[len * 2];
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.GetTagData(ant, (char) mbank, startaddr, len,
                    rdata, password, (short) timeout);
            if (er == READER_ERR.MT_OK_ERR) {
                return rdata;
            } else {
                logPrint("getTagDataByFilter, GetTagData result: " + er.toString());
                return null;
            }
        } else {
            logPrint("getTagDataByFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
            return null;
        }
    }

    public READER_ERR writeTagData(char mbank, int startaddress, byte[] data,
                                   int datalen, byte[] accesspasswd, short timeout) {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er == READER_ERR.MT_OK_ERR) {
            int trycount = 3;
            do {
                er = reader.WriteTagData(1, mbank, startaddress, data, datalen,
                        accesspasswd, timeout);
                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != READER_ERR.MT_OK_ERR);

            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("writeTagData, WriteTagData result: " + er.toString());
            }
        } else {
            logPrint("writeTagData, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR writeTagDataByFilter(char mbank, int startaddress,
                                           byte[] data, int datalen, byte[] accesspasswd, short timeout,
                                           byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }
        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er == READER_ERR.MT_OK_ERR) {
            int trycount = 3;
            do {
                er = reader.WriteTagData(1, mbank, startaddress, data, datalen,
                        accesspasswd, timeout);
                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != READER_ERR.MT_OK_ERR);

            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("writeTagDataByFilter, WriteTagData result: " + er.toString());
            }
        } else {
            logPrint("writeTagDataByFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR writeTagEPC(byte[] data, byte[] accesspwd, short timeout) {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        int trycount = 3;
        do {
            er = reader.WriteTagEpcEx(ant, data, data.length, accesspwd,
                    timeout);
            if (trycount < 1) {
                break;
            }
            trycount--;
        } while (er != READER_ERR.MT_OK_ERR);

        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("writeTagEPC, WriteTagEpcEx result: " + er.toString());
        }
        return er;
    }

    public READER_ERR writeTagEPCByFilter(byte[] data, byte[] accesspwd,
                                          short timeout, byte[] fdata, int fbank, int fstartaddr,
                                          boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }

        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er == READER_ERR.MT_OK_ERR) {
            int trycount = 3;
            do {
                er = reader.WriteTagEpcEx(ant, data, data.length, accesspwd,
                        timeout);
                if (trycount < 1) {
                    break;
                }
                trycount = trycount - 1;
            } while (er != READER_ERR.MT_OK_ERR);
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("writeTagEPCByFilter, WriteTagEpcEx result: " + er.toString());
            }
        } else {
            logPrint("writeTagEPCByFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;

    }

    public READER_ERR lockTag(Lock_Obj lockobject, Lock_Type locktype,
                              byte[] accesspasswd, short timeout) {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.LockTag(ant, (byte) lockobject.value(),
                    (short) locktype.value(), accesspasswd, timeout);
        }
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("lockTag, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR lockTagByFilter(Lock_Obj lockobject, Lock_Type locktype,
                                      byte[] accesspasswd, short timeout, byte[] fdata, int fbank,
                                      int fstartaddr, boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }
        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.LockTag(ant, (byte) lockobject.value(),
                    (short) locktype.value(), accesspasswd, timeout);
        }
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("lockTagByFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR killTag(byte[] killpasswd, short timeout) {
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.KillTag(ant, killpasswd, timeout);
        }
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("killTag, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR killTagByFilter(byte[] killpasswd, short timeout,
                                      byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        TagFilter_ST g2tf;
        g2tf = reader.new TagFilter_ST();
        g2tf.fdata = fdata;
        g2tf.flen = fdata.length * 8;
        if (matching) {
            g2tf.isInvert = 0;
        } else {
            g2tf.isInvert = 1;
        }
        g2tf.bank = fbank;
        g2tf.startaddr = fstartaddr * 16;

        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.KillTag(ant, killpasswd, timeout);
        }
        if (er != READER_ERR.MT_OK_ERR) {
            logPrint("killTagByFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
        }
        return er;
    }

    public READER_ERR setRegion(Region_Conf region) {
        return reader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION, region);
    }

    public Region_Conf getRegion() {
        Region_Conf[] rcf2 = new Region_Conf[1];
        READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION,
                rcf2);
        if (er == READER_ERR.MT_OK_ERR) {
            return rcf2[0];
        }
        logPrint("getRegion, ParamGet MTR_PARAM_FREQUENCY_REGION result: " + er.toString());
        return null;

    }

    public int[] getFrequencyPoints() {
        HoptableData_ST hdst2 = reader.new HoptableData_ST();
        READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE,
                hdst2);
        int[] tablefre;
        if (er == READER_ERR.MT_OK_ERR) {
            tablefre = sort(hdst2.htb, hdst2.lenhtb);
            return tablefre;
        }
        logPrint("getFrequencyPoints, ParamGet MTR_PARAM_FREQUENCY_HOPTABLE result: " + er.toString());
        return null;
    }

    public READER_ERR setFrequencyPoints(int[] frequencyPoints) {
        HoptableData_ST hdst = reader.new HoptableData_ST();
        hdst.lenhtb = frequencyPoints.length;
        hdst.htb = frequencyPoints;
        return reader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
    }

    public READER_ERR setPower(int readPower, int writePower) {
        AntPowerConf antPowerConf = reader.new AntPowerConf();
        antPowerConf.antcnt = ant;
        AntPower antPower = reader.new AntPower();
        antPower.antid = 1;
        antPower.readPower = (short) ((short) readPower * 100);
        antPower.writePower = (short) ((short) writePower * 100);
        antPowerConf.Powers[0] = antPower;
        return reader.ParamSet(Mtr_Param.MTR_PARAM_RF_ANTPOWER, antPowerConf);
    }

    public int[] getPower() {
        int[] powers = new int[2];
        AntPowerConf apcf2 = reader.new AntPowerConf();
        READER_ERR er = reader.ParamGet(
                Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf2);
        if (er == READER_ERR.MT_OK_ERR) {
            powers[0] = apcf2.Powers[0].readPower / 100;
            powers[1] = apcf2.Powers[0].writePower / 100;
            return powers;
        } else {
            logPrint("getPower, ParamGet MTR_PARAM_RF_ANTPOWER result: " + er.toString());
            return null;
        }
    }

    /**
     * get temperature
     */
    public int getTemperature() {
        int[] val = new int[1];
        READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_RF_TEMPERATURE, val);
        if (er == READER_ERR.MT_OK_ERR) {
            return val[0];
        } else {
            logPrint("getTemperature, ParamGet MTR_PARAM_RF_TEMPERATURE result: " + er.toString());
            return -1;
        }
    }

    @Deprecated
    public READER_ERR setFastMode() {
        READER_ERR er = setPower((short) 30, (short) 30);
        if (er == READER_ERR.MT_OK_ERR) {
            er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION,
                    new int[]{1});
        }
        return er;
    }

    @Deprecated
    public READER_ERR setCancelFastMode() {
        return reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION,
                new int[]{0});
    }

    private int[] sort(int[] array, int len) {
        int tmpIntValue;
        for (int xIndex = 0; xIndex < len; xIndex++) {
            for (int yIndex = 0; yIndex < len; yIndex++) {
                if (array[xIndex] < array[yIndex]) {
                    tmpIntValue = array[xIndex];
                    array[xIndex] = array[yIndex];
                    array[yIndex] = tmpIntValue;
                }
            }
        }
        return array;
    }

    public boolean setGen2session(boolean isMulti) {
        try {
            int[] val = new int[]{-1};
            if (isMulti) {
                // session 2
                val[0] = 2;
                // session 1
//                val[0] = 1;
            } else {
                // session0
                val[0] = 0;
            }
            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);
            return er == READER_ERR.MT_OK_ERR;
        } catch (Exception var4) {
            return false;
        }
    }

    public boolean setGen2session(int session) {
        try {
            int[] val = new int[]{-1};
            val[0] = session;
            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);
            return er == READER_ERR.MT_OK_ERR;
        } catch (Exception var4) {
            return false;
        }
    }

    public String getInfo() {
        HardwareDetails val = reader.new HardwareDetails();
        dv = new deviceVersion();
        Reader.GetDeviceVersion("/dev/ttyMT1", dv);
        if (reader.GetHardwareDetails(val) == READER_ERR.MT_OK_ERR) {
            return "module:" + val.module.toString() + "\r\nhard:" +
                    dv.hardwareVer + "\r\nsoft:" + dv.softwareVer;
        }
        return "";
    }

    public READER_ERR ReadTagLED(int ant, short timeout, short metaflag, TagLED_DATA tagled) {
        return reader.ReadTagLED(ant, timeout, metaflag, tagled);
    }

    /**
     * 开启/关闭FastTid
     */
    public boolean setFastID(boolean isOpenFastTiD) {
        if (isOpenFastTiD) {
            Reader.CustomParam_ST cpara = reader.new CustomParam_ST();
            cpara.ParamName = "tagcustomcmd/fastid";
            cpara.ParamVal = new byte[1];
            cpara.ParamVal[0] = 1;
            READER_ERR ret = reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpara);
            return ret == READER_ERR.MT_OK_ERR;
        } else {
            Reader.CustomParam_ST cpara = reader.new CustomParam_ST();
            cpara.ParamName = "tagcustomcmd/fastid";
            cpara.ParamVal = new byte[1];
            READER_ERR ret = reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpara);
            return ret == READER_ERR.MT_OK_ERR;
        }
    }

}
