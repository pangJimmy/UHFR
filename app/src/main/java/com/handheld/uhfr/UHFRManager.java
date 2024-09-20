package com.handheld.uhfr;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import com.gg.reader.api.dal.GClient;
import com.gg.reader.api.dal.HandlerTag6bLog;
import com.gg.reader.api.dal.HandlerTag6bOver;
import com.gg.reader.api.dal.HandlerTagEpcLog;
import com.gg.reader.api.dal.HandlerTagEpcOver;
import com.gg.reader.api.dal.HandlerTagGJbLog;
import com.gg.reader.api.dal.HandlerTagGJbOver;
import com.gg.reader.api.dal.HandlerTagGbLog;
import com.gg.reader.api.dal.HandlerTagGbOver;
import com.gg.reader.api.protocol.gx.EnumG;
import com.gg.reader.api.protocol.gx.LogBase6bInfo;
import com.gg.reader.api.protocol.gx.LogBase6bOver;
import com.gg.reader.api.protocol.gx.LogBaseEpcInfo;
import com.gg.reader.api.protocol.gx.LogBaseEpcOver;
import com.gg.reader.api.protocol.gx.LogBaseGJbInfo;
import com.gg.reader.api.protocol.gx.LogBaseGJbOver;
import com.gg.reader.api.protocol.gx.LogBaseGbInfo;
import com.gg.reader.api.protocol.gx.LogBaseGbOver;
import com.gg.reader.api.protocol.gx.MsgAppGetBaseVersion;
import com.gg.reader.api.protocol.gx.MsgBaseDestroyEpc;
import com.gg.reader.api.protocol.gx.MsgBaseGetBaseband;
import com.gg.reader.api.protocol.gx.MsgBaseGetFreqRange;
import com.gg.reader.api.protocol.gx.MsgBaseGetFrequency;
import com.gg.reader.api.protocol.gx.MsgBaseGetPower;
import com.gg.reader.api.protocol.gx.MsgBaseInventory6b;
import com.gg.reader.api.protocol.gx.MsgBaseInventoryEpc;
import com.gg.reader.api.protocol.gx.MsgBaseInventoryGJb;
import com.gg.reader.api.protocol.gx.MsgBaseInventoryGb;
import com.gg.reader.api.protocol.gx.MsgBaseLock6b;
import com.gg.reader.api.protocol.gx.MsgBaseLock6bGet;
import com.gg.reader.api.protocol.gx.MsgBaseLockEpc;
import com.gg.reader.api.protocol.gx.MsgBaseLockGJb;
import com.gg.reader.api.protocol.gx.MsgBaseSetBaseband;
import com.gg.reader.api.protocol.gx.MsgBaseSetFreqRange;
import com.gg.reader.api.protocol.gx.MsgBaseSetFrequency;
import com.gg.reader.api.protocol.gx.MsgBaseSetPower;
import com.gg.reader.api.protocol.gx.MsgBaseStop;
import com.gg.reader.api.protocol.gx.MsgBaseWrite6b;
import com.gg.reader.api.protocol.gx.MsgBaseWriteEpc;
import com.gg.reader.api.protocol.gx.MsgBaseWriteGJb;
import com.gg.reader.api.protocol.gx.Param6bReadUserdata;
import com.gg.reader.api.protocol.gx.ParamEpcFilter;
import com.gg.reader.api.protocol.gx.ParamEpcReadEpc;
import com.gg.reader.api.protocol.gx.ParamEpcReadReserved;
import com.gg.reader.api.protocol.gx.ParamEpcReadTid;
import com.gg.reader.api.protocol.gx.ParamEpcReadUserdata;
import com.gg.reader.api.protocol.gx.ParamFastId;
import com.gg.reader.api.utils.HexUtils;
import com.rfid.trans.ReadTag;
import com.rfid.trans.TagCallback;
import com.uhf.api.cls.InvEmbeddedBankData;
import com.uhf.api.cls.R2000_calibration.TagLED_DATA;
import com.uhf.api.cls.ReadListener;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import cn.pda.serialport.SerialPort;
import cn.pda.serialport.Tools;

/**
 * @author LeiHuang
 */
public class UHFRManager {

    //6108
    private static GClient client;
    private static List<LogBaseEpcInfo> epcList = new ArrayList<>();

    private static List<LogBaseGbInfo> gbepcList = new ArrayList<>();

    private static List<LogBaseGJbInfo> gjbepcList = new ArrayList<>();

    private static List<LogBase6bInfo> tag6bList = new ArrayList<>();

    private static final List<ReadTag> rrTagList = new ArrayList<>();
    public static final Object waitLock = new Object();
    private static final MsgCallback callback = new MsgCallback();
    //6106
    private static final String tag = "UHFRManager";
    private static Reader reader;
    private final int[] ants = new int[]{1};
    private final int ant = 1;
    public deviceVersion dv;
    public static READER_ERR mErr = READER_ERR.MT_CMD_FAILED_ERR;

    //停顿时间比
    String[] spiperst = {"0%", "5%", "10%", "15%", "20%", "25%", "30%", "35%",
            "40%", "45%", "50%"};

//    private static boolean is6108 = true;
    /**
     * 判断型号。是否为6108   type 0 国芯,1芯联,2锐迪,3荣睿
     */
    private static int type = -1;

    private static final int port = 13;

    private ParamFastId fastId = new ParamFastId();

    /**
     * 是否开启了附加数据（作用域只在于本app中，非模块），避免每次调用盘存方法都需要设置该项
     */
//    private boolean MTR_PARAM_TAG_EMBEDEDDATA = true;

    private static boolean DEBUG = false;

    private int rPower = 0;
    private int wPower = 0;

    public static void setDebuggable(boolean debuggable) {
        DEBUG = debuggable;
    }

    private static void logPrint(String content) {
        if (DEBUG) {
            Log.i(tag, content);
        }
    }

    private static void logPrint(String tag, String content) {
        logPrint("[" + tag + "]->" + content);
    }

    private static UHFRManager uhfrManager = null;

    /**
     * init Uhf module
     *
     * @return UHFRManager
     */
    public static UHFRManager getInstance() {
        long enterTime = SystemClock.elapsedRealtime();
        if (uhfrManager == null) {
            if (connect()) {
                uhfrManager = new UHFRManager();
            } else {
                logPrint("First connect failed, try it again");
                boolean reconnect = connect();
                if (reconnect) {
                    uhfrManager = new UHFRManager();
                }
            }
        }
        long outTime = SystemClock.elapsedRealtime();
        logPrint("Init uhf time: " + (outTime - enterTime));
        return uhfrManager;
    }

    public boolean close() {
        if (type == 0) {
            if (client != null) {
                client.close();
                client.hdPowerOff();
            }
            client = null;
        } else if (type == 1) {
            if (reader != null) {
                reader.CloseReader();
            }
            reader = null;
        } else if (type == 3) {
            int disconnectResult = RrReader.rrlib.DisConnect();
            if (disconnectResult == 0) {
                new SerialPort().power_5Voff();
                uhfrManager = null;
                logPrint("Close power of rr reader");
                return true;
            } else {
                logPrint("Rr close error: " + disconnectResult);
            }
        }
        new SerialPort().power_5Voff();
        uhfrManager = null;
        logPrint("Close power of reader");
        return true;

    }

    //读版本号1.1.01为国芯， 1.1.02.xx为芯联系列， 1.1.03为锐迪系列
    public String getHardware() {
        String version = null;
        if (type == 0) {
            Objects.requireNonNull(client);
            MsgAppGetBaseVersion msg = new MsgAppGetBaseVersion();
            client.sendSynMsg(msg);
            logPrint("MsgAppGetBaseVersion", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                String[] arrays = msg.getBaseVersions().split("\\.");
                if (arrays.length > 2) {
                    /**
                     * 获取固件gx
                     * X.X.1.X表示通用版本（1或者3表示通用版本支持6c/b; 5表示GJB，6表示GB，7表示GJB和GB）
                     * baseVersions='1.1.3.15'
                     */
                    version = "1.1.01." + arrays[2];
                }
//                return msg.getBaseVersions();
                return version;
            }
            return version;
        } else if (type == 1) {
            HardwareDetails val = reader.new HardwareDetails();
            READER_ERR er = reader.GetHardwareDetails(val);
            if (er == READER_ERR.MT_OK_ERR) {
                version = "1.1.02." + val.module.value();
//                return val.module.toString();
            }
            return version;

        } else if (type == 3) {
            String str = RrReader.getVersion();
            version = String.format("1.1.04.%s", str);
        }
        return version;
    }

    private static boolean connect() {
        // 重置静态变量标志
        type = -1;
        isE710 = false;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        SerialPort serialPort = null;
        try {
            new SerialPort().power_5Von();
//            new SerialPort().scaner_poweron();
            Thread.sleep(500);
            serialPort = new SerialPort(port, 115200, 0);
            // 国芯获取版本号指令
            String cmd = "5A000101010000EBD5";
            outputStream = serialPort.getOutputStream();
            outputStream.write(Tools.HexString2Bytes(cmd));
            outputStream.flush();
            Thread.sleep(20);
            byte[] bytes = new byte[128];
            inputStream = serialPort.getInputStream();
            int read = inputStream.read(bytes);
            String retStr = Tools.Bytes2HexString(bytes, read);
            logPrint("zeng-", "retStr0:" + retStr);
            if (retStr.length() >= 10 && retStr.contains("5A00010101")) {
                // 获取到国芯模块返回
                type = 0;
            } else {
                // 芯联获取版本号指令
                cmd = "FF00031D0C";
                outputStream.write(Tools.HexString2Bytes(cmd));
                outputStream.flush();
                Thread.sleep(20);
                read = inputStream.read(bytes);
                retStr = Tools.Bytes2HexString(bytes, read);
                logPrint("zeng-", "retStr1:" + retStr);
                if (retStr.length() > 40) {
                    // 获取到芯联R2000模块返回
                    type = 1;
                    isE710 = false;
                } else {
                    // E710
                    serialPort.close(port);
                    serialPort = new SerialPort(port, 921600, 0);
                    outputStream = serialPort.getOutputStream();
                    inputStream = serialPort.getInputStream();
                    cmd = "FF00031D0C";
                    outputStream.write(Tools.HexString2Bytes(cmd));
                    outputStream.flush();
                    Thread.sleep(20);
                    read = inputStream.read(bytes);
                    retStr = Tools.Bytes2HexString(bytes, read);
                    logPrint("zeng-", "retStr2:" + retStr);
                    if (retStr.length() > 40) {
                        // 获取到芯联E710模块返回
                        type = 1;
                        isE710 = true;
                    } else {
                        SystemClock.sleep(80);
                        serialPort = new SerialPort(port, 921600, 0);
                        outputStream = serialPort.getOutputStream();
                        inputStream = serialPort.getInputStream();
                        outputStream.write(Tools.HexString2Bytes("04004C3AD2"));
                        outputStream.flush();
                        SystemClock.sleep(10);
                        read = inputStream.read(bytes);
                        retStr = Tools.Bytes2HexString(bytes, read);
                        logPrint("connect", "retStr3(921600): " + retStr);
                        if (retStr.length() > 10) {
                            // 荣睿模块
                            type = 3;
                            // 切换波特率
                            outputStream.write(Tools.HexString2Bytes("05002806B3E5"));
                            outputStream.flush();
                            SystemClock.sleep(50);
                            read = inputStream.read(bytes);
                            retStr = Tools.Bytes2HexString(bytes, read);
                            logPrint("connect", "rr switch to 115200: " + retStr);
                        } else {
                            serialPort = new SerialPort(port, 115200, 0);
                            outputStream = serialPort.getOutputStream();
                            inputStream = serialPort.getInputStream();
                            outputStream.write(Tools.HexString2Bytes("04004C3AD2"));
                            outputStream.flush();
                            SystemClock.sleep(10);
                            read = inputStream.read(bytes);
                            retStr = Tools.Bytes2HexString(bytes, read);
                            logPrint("connect", "retStr3(115200): " + retStr);
                            if (retStr.length() > 10) {
                                // 荣睿模块
                                type = 3;
                            } else {
                                cmd = "A55A000902000B0D0A";
                                outputStream.write(Tools.HexString2Bytes(cmd));
                                outputStream.flush();
                                Thread.sleep(20);
                                read = inputStream.read(bytes);
                                retStr = Tools.Bytes2HexString(bytes, read);
                                logPrint("connect", "retStr4: " + retStr);
                                if (retStr.length() > 10) {
                                    type = 2;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (serialPort != null) {
                    serialPort.close(port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logPrint("Zeng-", "type:" + type);
        if (type == 0) {
            client = new GClient();
            if (client.openHdSerial("13:115200", 0)) {
                onTagHandler();
                client.hdPowerOn();
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var1) {
                    var1.printStackTrace();
                }
                return true;
            }
        } else if (type == 1) {
            reader = new Reader();
            READER_ERR er;
            long enterTime = SystemClock.elapsedRealtime();
            logPrint("Zeng-", "isE710:" + isE710);
            if (isE710) {
                // E710 波特率921600
                er = reader.InitReader_Notype("/dev/ttyMT1:921600", 1);
            } else {
                // R2000 波特率115200
                er = reader.InitReader_Notype("/dev/ttyMT1", 1);
            }
            long outTime = SystemClock.elapsedRealtime();
            Log.i("zeng-", "InitReader cusTime: " + (outTime - enterTime));
            if (er == READER_ERR.MT_OK_ERR) {
                if (connect2()) {
                    return true;
                }
            }
            reader.CloseReader();
        } else if (type == 3) {
            int result = RrReader.connect("/dev/ttyMT1", 115200, DEBUG ? 1 : 0);
            if (result == 0) {
                RrReader.rrlib.SetCallBack(callback);
                return true;
            } else {
                logPrint("Rr connect error: " + result);
            }
        }
        new SerialPort().power_5Voff();
//        new SerialPort().scaner_poweroff();
        return false;
    }


    private static boolean isE710 = false;

    private static boolean connectE710() {
        try {
            new SerialPort().power_5Von();
//            new SerialPort().scaner_poweron();
            Thread.sleep(500);
            // 芯联获取版本号指令
            String cmd = "FF00031D0C";
            SerialPort serialPort = new SerialPort(port, 921600, 0);
            ;
            OutputStream outputStream = serialPort.getOutputStream();
            InputStream inputStream = serialPort.getInputStream();
            outputStream.write(Tools.HexString2Bytes(cmd));
            outputStream.flush();
            Thread.sleep(20);
            byte[] bytes = new byte[128];
            int read;
            String retStr;
            read = inputStream.read(bytes);
            retStr = Tools.Bytes2HexString(bytes, read);
            if (retStr.length() > 10) {
                logPrint("connect", "connectE710 xinlian retStr: " + retStr);
                type = 1;
            }
            serialPort.close(port);
        } catch (Exception e) {

        }
        if (type == 1) {
            reader = new Reader();
            //波特率921600
            READER_ERR er = reader.InitReader_Notype("/dev/ttyMT1:921600", 1);
            logPrint("connect", "connectE710 xinlian retStr: " + er.name());
            if (er == READER_ERR.MT_OK_ERR) {
                connect2();
                //判断是否为E710模块
//                HardwareDetails val = reader.new HardwareDetails();
//                er = reader.GetHardwareDetails(val);
//                logPrint("pang", "" + val.module)  ;
//                if(val.module == Reader.Module_Type.MODOULE_SLR7100){
//                    boolean baudFlag = setBaudrate(921600);
//                    logPrint("pang", "baudFlag = " + baudFlag)  ;
//                }
                isE710 = false;
                return true;
            }
        }
        return false;
    }

    //芯联专属方法
    private static boolean connect2() {
        long enterTime = SystemClock.elapsedRealtime();
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
        long outTime = SystemClock.elapsedRealtime();
        Log.i("zeng-", "connect2 cusTime: " + (outTime - enterTime));
        return er == READER_ERR.MT_OK_ERR;
    }

    //芯联专用，设置波特率
    public static boolean setBaudrate(int baudtrate) {
        Reader.Default_Param dp = reader.new Default_Param();
        dp.isdefault = false;
        dp.key = Mtr_Param.MTR_PARAM_SAVEINMODULE_BAUD;
        dp.val = baudtrate;
//        int[] val = {baudtrate} ;
        READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_SAVEINMODULE_BAUD, dp);
//        logPrint("pang ", "Error = " + er.name());
        return er == READER_ERR.MT_OK_ERR;
    }


    public READER_ERR asyncStartReading() {
        if (type == 0) {
            MsgBaseGetBaseband getBaseband = new MsgBaseGetBaseband();
            client.sendSynMsg(getBaseband);
            if (getBaseband.getRtCode() == 0) {
                MsgBaseInventoryEpc inventoryEpc = new MsgBaseInventoryEpc();
                inventoryEpc.setAntennaEnable(EnumG.AntennaNo_1);
                inventoryEpc.setInventoryMode(EnumG.InventoryMode_Inventory);
                if (filter != null && filter.isMatching()) {
                    inventoryEpc.setFilter(filter.getFilter());
                }
                if (fastId.getFastId() != 0) {
                    inventoryEpc.setParamFastId(fastId);
                }
                client.sendSynMsg(inventoryEpc);
                logPrint("MsgBaseInventoryEpc", inventoryEpc.getRtMsg());
                return inventoryEpc.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
//            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
//
//            logPrint("asyncStartReading, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
            // 设置gen2 tag编码(PROF)
//        int[] val = new int[] {19};//profile3，默认M4
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, val);
//        logPrint("asyncStartReading, ParamSet MTR_PARAM_POTL_GEN2_TAGENCODING result: " + er.toString());
            // option = 0, 多标签快速模式(不含附加数据)
            //E710智能模式
            if (isE710 && !isEmb) {
                /*新E7快速模式*/
                logPrint("pang", "E710 AsyncStartReading");
                Reader.CustomParam_ST cpst = reader.new CustomParam_ST();
                cpst.ParamName = "Reader/Ex10fastmode";
                byte[] vals = new byte[22];
                vals[0] = 1;
                vals[1] = 20;
                for (int i = 0; i < 20; i++)
                    vals[2 + i] = 0;
                cpst.ParamVal = vals;
                reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpst);
                return reader.AsyncStartReading(ants, 1, 0);


//                Object[] objs3 = new Object[5];
//                objs3[0] = 500;
//                objs3[1] = 50;
//                objs3[2] = 100;
//                objs3[3] = 1;
//                objs3[4] = 101;
//                boolean flag = false;
//                try {
//                    flag = reader.Set_IT_Params(Reader.IT_MODE.IT_MODE_E7, objs3);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                logPrint("Set_IT_Params", "Set_IT_Params = " + flag);
//                reader.addReadListener(readListener);
//
//                er = reader.AsyncStartReading_IT(Reader.IT_MODE.IT_MODE_E7, ants, 1, 0);
//                logPrint("AsyncStartReading_IT, IT_MODE_E7 result: " + er.toString());

//                return er;
                /**/
            } else {
                int session = getGen2session();
                logPrint("pang", "AsyncStartReading");
                int option = 16;

                if (session == 1) {
                    if (isEmb) {
                        option = Emboption;
                    }
                    return reader.AsyncStartReading(ants, 1, option);
                } else {
                    option = 0;
                    if (isEmb) {
                        option = Emboption;
                    }
                    return reader.AsyncStartReading(ants, 1, option);
                }

            }

            // option = 16, 多标签手持机模式(不含附加数据)
//        return reader.AsyncStartReading(ants, 1, 16);
        } else if (type == 3) {
            synchronized (rrTagList) {
                rrTagList.clear();
            }
            int startReadResult = RrReader.startRead();
            if (startReadResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr async start reading error:" + startReadResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    private List<TAGINFO> listTag = new ArrayList<>();
    //E710 E智能模式的异步接收数据
    private ReadListener readListener = new ReadListener() {
        @Override
        public void tagRead(Reader reader, TAGINFO[] taginfos) {
            synchronized (listTag) {
                if (taginfos != null && taginfos.length > 0) {
//                    listTag.clear();
                    Collections.addAll(listTag, taginfos);
                }
            }
        }
    };

    public READER_ERR asyncStartReading(int option) {
        if (type == 0) {
            MsgBaseGetBaseband getBaseband = new MsgBaseGetBaseband();
            client.sendSynMsg(getBaseband);
            if (getBaseband.getRtCode() == 0) {
                MsgBaseInventoryEpc inventoryEpc = new MsgBaseInventoryEpc();
                inventoryEpc.setAntennaEnable(EnumG.AntennaNo_1);
                inventoryEpc.setInventoryMode(EnumG.InventoryMode_Inventory);
                if (filter != null && filter.isMatching()) {
                    inventoryEpc.setFilter(filter.getFilter());
                }
                if (fastId.getFastId() != 0) {
                    inventoryEpc.setParamFastId(fastId);
                }
                client.sendSynMsg(inventoryEpc);
                logPrint("MsgBaseInventoryEpc", inventoryEpc.getRtMsg());
                return inventoryEpc.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {

//            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
//            logPrint("asyncStartReading, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
            // 设置gen2 tag编码(PROF)
//        int[] val = new int[] {19};//profile3，默认M4
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, val);
//        logPrint("asyncStartReading, ParamSet MTR_PARAM_POTL_GEN2_TAGENCODING result: " + er.toString());
            if (isE710) {
                /*新E7快速模式，有待验证                 */
                logPrint("pang", "AsyncStartReading");
                Reader.CustomParam_ST cpst = reader.new CustomParam_ST();
                cpst.ParamName = "Reader/Ex10fastmode";
                byte[] vals = new byte[22];
                vals[0] = 1;
                vals[1] = 20;
                for (int i = 0; i < 20; i++)
                    vals[2 + i] = 0;
                cpst.ParamVal = vals;
                reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpst);

                return reader.AsyncStartReading(ants, 1, option);
            }
            return reader.AsyncStartReading(ants, 1, option);
        } else if (type == 3) {
            return asyncStartReading();
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR asyncStopReading() {

        if (type == 0) {
            MsgBaseStop stop = new MsgBaseStop();
            client.sendSynMsg(stop);
            logPrint("MsgBaseStop", stop.getRtMsg());
            return stop.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {
            if (isE710) {
                READER_ERR er = reader.AsyncStopReading();
                logPrint("pang", "asyncStopReading");
                ///////////////E7智能模式////////
//                READER_ERR er = reader.AsyncStopReading_IT();
//                reader.removeReadListener(readListener);
//                logPrint("pang", "AsyncStopReading_IT er = " + er.name());
                ///////////////////////////////
                return er;
            } else {
                return reader.AsyncStopReading();
            }

        } else if (type == 3) {
            RrReader.stopRead();
            return READER_ERR.MT_OK_ERR;
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR InventoryFilters() {
        if (type == 1) {
            int session = getGen2session();
            logPrint("pang", "AsyncStartReading");
            int option = 16;

            if (session == 1) {
                if (isEmb) {
                    option = Emboption;
                }
                return reader.AsyncStartReading(ants, 1, option);
            } else {
                option = 0;
                if (isEmb) {
                    option = Emboption;
                }
                return reader.AsyncStartReading(ants, 1, option);
            }

        } else {
            return READER_ERR.MT_CMD_FAILED_ERR;
        }
    }

    public boolean setInventoryFilters(String[] mepc) {
        if (type == 0) {
//            ParamEpcFilter paramEpcFilter = new ParamEpcFilter();
//            paramEpcFilter.setArea(fbank);
//            paramEpcFilter.setBitStart(fstartaddr * 16);//word
//            paramEpcFilter.setbData(fdata);
//            paramEpcFilter.setBitLength(fdata.length * 8);
//            filter = new CusParamFilter(paramEpcFilter, matching);
//            return true;
        } else if (type == 1) {
//            for(int i=0;i<myapp.mfiltags.size();i++)
//                mepc[i]=myapp.mfiltags.get(i);
            READER_ERR er = reader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_MULTISELECTORS, mepc);
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("setInventoryFilters, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
                return false;
            }
            return true;
        } else if (type == 3) {
//            RrReader.setInvMask(fdata, fbank, fstartaddr, matching);
            return true;
        }
        return false;
    }

    public boolean setInventoryFilter(byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        if (type == 0) {
            ParamEpcFilter paramEpcFilter = new ParamEpcFilter();
            paramEpcFilter.setArea(fbank);
            paramEpcFilter.setBitStart(fstartaddr * 16);//word
            paramEpcFilter.setbData(fdata);
            paramEpcFilter.setBitLength(fdata.length * 8);
            filter = new CusParamFilter(paramEpcFilter, matching);
            return true;
        } else if (type == 1) {
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
        } else if (type == 2) {
//锐迪未完成//
            return true;


        } else if (type == 3) {
            RrReader.setInvMask(fdata, fbank, fstartaddr, matching);
            return true;
        }
        return false;
    }

    //
    public boolean setCancleInventoryFilter() {
        if (type == 0) {
            filter = null;
            return true;
        } else if (type == 1) {

            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("setCancleInventoryFilter, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
                return false;
            }
            return true;
        } else if (type == 2) {
//锐迪未完成//
            return true;
        } else if (type == 3) {
            RrReader.rrlib.ClearMaskList();
            return true;
        }
        return false;
    }


    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的方法
    public List<LogBaseGbInfo> formatGBData(int bank) {

        synchronized (gbepcList) {
            List<LogBaseGbInfo> mgblist = new ArrayList<>();
            mgblist.clear();
            if (gbepcList != null && !gbepcList.isEmpty()) {
                mgblist.addAll(gbepcList);
            }
            gbepcList.clear();
            return mgblist;
        }
    }

    public List<LogBase6bInfo> format6BData() {

        synchronized (tag6bList) {
            List<LogBase6bInfo> mgblist = new ArrayList<>();
            mgblist.clear();
            if (tag6bList != null && !tag6bList.isEmpty()) {
                mgblist.addAll(tag6bList);
            }
            tag6bList.clear();
            return mgblist;
        }
    }

    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的方法
    public List<LogBaseGJbInfo> formatGJBData(int bank) {

        synchronized (gjbepcList) {
            List<LogBaseGJbInfo> mgblist = new ArrayList<>();
            mgblist.clear();
            if (gjbepcList != null && !gjbepcList.isEmpty()) {
                mgblist.addAll(gjbepcList);
            }
            gjbepcList.clear();
            return mgblist;
        }
    }


    private CusParamFilter filter;

    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的方法
    public List<TAGINFO> formatData(int bank) {
        synchronized (epcList) {
            HashMap<String, TAGINFO> tagMap = new HashMap<>();
            for (LogBaseEpcInfo info : epcList) {
                TAGINFO taginfo = new Reader().new TAGINFO();
                taginfo.AntennaID = (byte) info.getAntId();
                if (info.getFrequencyPoint() != null) {
                    taginfo.Frequency = info.getFrequencyPoint().intValue();
                }
                if (info.getReplySerialNumber() != null) {
                    taginfo.TimeStamp = info.getReplySerialNumber().intValue();
                }
                switch (bank) {
                    case 0:
                        if (info.getReserved() != null) {
                            taginfo.EmbededData = info.getbRes();
                            taginfo.EmbededDatalen = (short) info.getbRes().length;
                        }
                        break;
                    case 1:
                        if (info.getEpcData() != null) {
                            taginfo.EmbededData = info.getbEpcData();
                            taginfo.EmbededDatalen = (short) info.getbEpcData().length;
                        }
                        break;
                    case 2:
                        if (info.getTid() != null) {
                            taginfo.EmbededData = info.getbTid();
                            taginfo.EmbededDatalen = (short) info.getbTid().length;
                        }
                        break;
                    case 3:
                        if (info.getUserdata() != null) {
                            taginfo.EmbededData = info.getbUser();
                            taginfo.EmbededDatalen = (short) info.getbUser().length;
                        }
                        break;
                    default:
                        break;
                }
                taginfo.EpcId = info.getbEpc();
                taginfo.Epclen = (short) info.getbEpc().length;
                taginfo.PC = HexUtils.int2Bytes(info.getPc());
//                taginfo.Temperature = info.getCtesiusLtu31();
                //epcbank读取会影响速度，新增协议0x15获取
                if (info.getCrc() != 0) {
                    taginfo.CRC = HexUtils.int2Bytes(info.getCrc());
                }
                taginfo.protocol = SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
                taginfo.Phase = info.getPhase();
                double v = -39.9 + 6 * log2(info.getRssi());
                taginfo.RSSI = (int) Math.round(v);
                if (info.getTid() != null) {
                    if (!tagMap.containsKey(info.getTid())) {
                        taginfo.ReadCnt = 1;
                    } else {
                        TAGINFO temp = tagMap.get(info.getTid());
                        if (temp != null) {
                            temp.ReadCnt += 1;
                            tagMap.put(info.getTid(), temp);
                        }
                    }
                    tagMap.put(info.getTid(), taginfo);
                } else {
                    if (!tagMap.containsKey(info.getEpc())) {
                        taginfo.ReadCnt = 1;
                        tagMap.put(info.getEpc(), taginfo);
                    } else {
                        TAGINFO temp = tagMap.get(info.getEpc());
                        if (temp != null) {
                            temp.ReadCnt += 1;
                            tagMap.put(info.getEpc(), temp);
                        }
                    }
                }
            }
            epcList.clear();
            return new ArrayList<>(tagMap.values());
        }
    }

    //double v1 = -39.9 + 6*log2()
    //国芯计算RSSI值
    private double log2(double N) {
        return Math.log(N / 190) / Math.log(2);
    }

    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的处理温度标签的方法
    public List<com.handheld.uhfr.Reader.TEMPTAGINFO> formatData() {
        synchronized (epcList) {
            HashMap<String, com.handheld.uhfr.Reader.TEMPTAGINFO> tagMap = new HashMap<>();
            for (LogBaseEpcInfo info : epcList) {
                com.handheld.uhfr.Reader.TEMPTAGINFO taginfo = new com.handheld.uhfr.Reader.TEMPTAGINFO();
                taginfo.AntennaID = (byte) info.getAntId();
                if (info.getFrequencyPoint() != null) {
                    taginfo.Frequency = info.getFrequencyPoint().intValue();
                }
                if (info.getReplySerialNumber() != null) {
                    taginfo.TimeStamp = info.getReplySerialNumber().intValue();
                }
                //从user区读出温度数据
                if (info.getUserdata() != null) {
                    logPrint("pang", "pang, " + info.getUserdata());
//                            taginfo.EmbededData = info.getbUser();
//                            taginfo.EmbededDatalen = (short) info.getbUser().length;
                    //宜链
//                    String userdata = "48E8";
                    String userdata = info.getUserdata();
                    if (userdata != null && !"0000".equals(userdata) && userdata.length() > 2) {
                        int integer = Integer.parseInt(userdata.substring(0, 2), 16);
                        double decimal = (double) (Integer.parseInt(userdata.substring(2, 4), 16)) / 255;
                        decimal = (double) Math.round(decimal * 100) / 100;
                        if (integer > 45) {
                            logPrint("temp ", "temp = " + (integer - 45 + decimal));
                            taginfo.Temperature = (integer - 45 + decimal);
                        } else {
                            logPrint("temp ", "temp = -" + (45 - integer + decimal));
                            taginfo.Temperature = -(45 - integer + decimal);
                        }
                        //
                        taginfo.EpcId = info.getbEpc();
                        taginfo.Epclen = (short) info.getbEpc().length;
                        taginfo.PC = HexUtils.int2Bytes(info.getPc());

                        //epcbank读取会影响速度，新增协议0x15获取
                        if (info.getCrc() != 0) {
                            taginfo.CRC = HexUtils.int2Bytes(info.getCrc());
                        }
                        taginfo.protocol = com.handheld.uhfr.Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
                        taginfo.Phase = info.getPhase();
                        double v = -39.9 + 6 * log2(info.getRssi());
                        taginfo.RSSI = (int) Math.round(v);
                        if (info.getTid() != null) {
                            if (!tagMap.containsKey(info.getTid())) {
                                taginfo.ReadCnt = 1;
                            } else {
                                com.handheld.uhfr.Reader.TEMPTAGINFO temp = tagMap.get(info.getTid());
                                if (temp != null) {
                                    temp.ReadCnt += 1;
                                    tagMap.put(info.getTid(), temp);
                                }
                            }
                            tagMap.put(info.getTid(), taginfo);
                        } else {
                            if (!tagMap.containsKey(info.getEpc())) {
                                taginfo.ReadCnt = 1;
                                tagMap.put(info.getEpc(), taginfo);
                            } else {
                                com.handheld.uhfr.Reader.TEMPTAGINFO temp = tagMap.get(info.getEpc());
                                if (temp != null) {
                                    temp.ReadCnt += 1;
                                    tagMap.put(info.getEpc(), temp);
                                }
                            }
                        }
                    }

                }


            }
            epcList.clear();
            return new ArrayList<>(tagMap.values());
        }
    }

    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的处理温度标签的方法
    public List<com.handheld.uhfr.Reader.TEMPTAGINFO> formatYueheData() {
        synchronized (epcList) {
            HashMap<String, com.handheld.uhfr.Reader.TEMPTAGINFO> tagMap = new HashMap<>();
            for (LogBaseEpcInfo info : epcList) {
                com.handheld.uhfr.Reader.TEMPTAGINFO taginfo = new com.handheld.uhfr.Reader.TEMPTAGINFO();
                taginfo.AntennaID = (byte) info.getAntId();
                if (info.getFrequencyPoint() != null) {
                    taginfo.Frequency = info.getFrequencyPoint().intValue();
                }
                if (info.getReplySerialNumber() != null) {
                    taginfo.TimeStamp = info.getReplySerialNumber().intValue();
                }

                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                //悦和
                taginfo.Temperature = (Double.valueOf(nf.format(info.getCtesiusLtu31() * 0.01)));

                //
                taginfo.EpcId = info.getbEpc();
                taginfo.Epclen = (short) info.getbEpc().length;
                taginfo.PC = HexUtils.int2Bytes(info.getPc());

                //epcbank读取会影响速度，新增协议0x15获取
                if (info.getCrc() != 0) {
                    taginfo.CRC = HexUtils.int2Bytes(info.getCrc());
                }
                taginfo.protocol = com.handheld.uhfr.Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
                taginfo.Phase = info.getPhase();
                double v = -39.9 + 6 * log2(info.getRssi());
                taginfo.RSSI = (int) Math.round(v);
                if (info.getTid() != null) {
                    if (!tagMap.containsKey(info.getTid())) {
                        taginfo.ReadCnt = 1;
                    } else {
                        com.handheld.uhfr.Reader.TEMPTAGINFO temp = tagMap.get(info.getTid());
                        if (temp != null) {
                            temp.ReadCnt += 1;
                            tagMap.put(info.getTid(), temp);
                        }
                    }
                    tagMap.put(info.getTid(), taginfo);
                } else {
                    if (!tagMap.containsKey(info.getEpc())) {
                        taginfo.ReadCnt = 1;
                        tagMap.put(info.getEpc(), taginfo);
                    } else {
                        com.handheld.uhfr.Reader.TEMPTAGINFO temp = tagMap.get(info.getEpc());
                        if (temp != null) {
                            temp.ReadCnt += 1;
                            tagMap.put(info.getEpc(), temp);
                        }
                    }
                }
            }
            epcList.clear();
            return new ArrayList<>(tagMap.values());
        }
    }


    //芯联处理宜链标签
    private List<com.handheld.uhfr.Reader.TEMPTAGINFO> handleYilian(int type, List<TAGINFO> epclist) {
        List<com.handheld.uhfr.Reader.TEMPTAGINFO> list = new ArrayList<>();
        if (epclist != null && !epclist.isEmpty()) {
            for (int i = 0; i < epclist.size(); i++) {
                //温度区不为空返回
                if (epclist.get(i).EmbededData != null) {
                    com.handheld.uhfr.Reader.TEMPTAGINFO taginfo = new com.handheld.uhfr.Reader.TEMPTAGINFO();
                    String userdata = Tools.Bytes2HexString(epclist.get(i).EmbededData, epclist.get(i).EmbededData.length);
                    if (userdata != null && !"0000".equals(userdata) && userdata.length() > 2) {
                        int integer = Integer.parseInt(userdata.substring(0, 2), 16);
                        double decimal = (double) (Integer.parseInt(userdata.substring(2, 4), 16)) / 255;
                        decimal = (double) Math.round(decimal * 100) / 100;
                        if (integer > 45) {
                            logPrint("temp ", "temp = " + (integer - 45 + decimal));
                            taginfo.Temperature = (integer - 45 + decimal);
                        } else {
                            logPrint("temp ", "temp = -" + (45 - integer + decimal));
                            taginfo.Temperature = -(45 - integer + decimal);
                        }
                        byte[] epcId = epclist.get(i).EpcId;
                        if (epcId == null) {
                            epcId = new byte[0];
                        }
                        taginfo.EpcId = epcId;
                        taginfo.Epclen = (short) epcId.length;
                        if (type == 1) {
                            // 芯联
                            taginfo.PC = epclist.get(i).PC;
                            taginfo.AntennaID = epclist.get(i).AntennaID;
                            taginfo.Frequency = epclist.get(i).Frequency;
                            taginfo.RSSI = epclist.get(i).RSSI;
                        }
                        list.add(taginfo);
                    }
                }
            }
        }
        return list;
    }


    //     排除 fData-过滤的数据//国芯的方法
    public List<TAGINFO> formatExcludeData(int bank, byte[] fData) {
        List<TAGINFO> tagInfos = formatData(bank);
        List<TAGINFO> temp = new ArrayList<>();
        for (TAGINFO info : tagInfos) {
            if (!HexUtils.bytes2HexString(info.EmbededData).equals(HexUtils.bytes2HexString(fData))) {
                temp.add(info);
            }
        }
        return temp;
    }

    TAGINFO taginfo = new Reader().new TAGINFO();

    public List<TAGINFO> tagInventoryRealTime() {
        List<TAGINFO> list = new ArrayList<>();
        if (type == 0) {
            int bank = 4;
            if (fastId.getFastId() != 0) {
                bank = 2;
            }
            if (filter != null && !filter.isMatching()) {
                return formatExcludeData(bank, filter.getFilter().getbData());
            }
            return formatData(bank);
        } else if (type == 1) {
            ///////////////////E710///////////////////////////////
//            if (isE710) {
//                synchronized (listTag) {
//                    list.addAll(listTag);
//                    logPrint("pang", "tagInventoryRealTime : listTag size = " + listTag.size());
//                    listTag.clear();
//                    return list;
//                }
            ///////////////////E710///////////////////////////////
//            }else{

            READER_ERR er;
//        if (MTR_PARAM_TAG_EMBEDEDDATA) {
//        er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
//        Log.d(tag, "[tagInventoryRealTime] : 0");
//        if (er == READER_ERR.MT_OK_ERR) {
//                MTR_PARAM_TAG_EMBEDEDDATA = false;
//        }
//        }
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
//            }


        } else if (type == 3) {
            return formatRrTagList();
        }
        return list;
    }


    int count = 0;

    //锐迪处理数据
    public TAGINFO getBuf(String getBuffString) {
        TAGINFO tfs = new Reader().new TAGINFO();
        ;
        int Hb = 0;
        int Lb = 0;
        int rssi = 0;
        String[] tmp = new String[3];
        HashMap<String, String> temp = new HashMap<>();
        String text = getBuffString.substring(4);
        String len = getBuffString.substring(0, 2);
        int epclen = (Integer.parseInt(len, 16) / 8) * 4;
        tmp[0] = text.substring(epclen, text.length() - 6);
        tmp[1] = text.substring(0, text.length() - 6);
        tmp[2] = text.substring(text.length() - 6, text.length() - 2);

        if (4 != tmp[2].length()) {
            tmp[2] = "0000";
        } else {
            Hb = Integer.parseInt(tmp[2].substring(0, 2), 16);
            Lb = Integer.parseInt(tmp[2].substring(2, 4), 16);
            rssi = ((Hb - 256 + 1) * 256 + (Lb - 256)) / 10;
        }

        tfs.EpcId = Tools.HexString2Bytes(tmp[1]);
        tfs.Epclen = (short) (tmp[1].length() / 4);
        tfs.RSSI = Integer.valueOf(rssi);
        count++;
        return tfs;
    }

    public boolean stopTagInventory() {
        if (type == 0) {

            READER_ERR reader_err = asyncStopReading();
            return reader_err.value() == 0;
        } else if (type == 1) {
            READER_ERR er = reader.AsyncStopReading();
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("stopTagInventory, AsyncStopReading result: " + er.toString());
                return false;
            }
            return true;
        } else if (type == 2) {
            //锐迪，未完成
//            logPrint("zeng-","cont:"+count);
            READER_ERR reader_err = asyncStopReading();
            return reader_err.value() == 0;
        } else if (type == 3) {
            RrReader.rrlib.StopRead();
            return true;
        }
        return false;
    }

    //
    public List<TAGINFO> tagInventoryByTimer(short readtime) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            if (filter != null && filter.isMatching()) {
                msg.setFilter(filter.getFilter());
            }
//            if (fastId.getFastId() != 0) {
//                msg.setParamFastId(fastId);
//            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("MsgBaseStop", stop.getRtCode() + "");
                    if (filter != null && !filter.isMatching()) {
                        return formatExcludeData(4, filter.getFilter().getbData());
                    }
                    return formatData(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        } else if (type == 1) {
            READER_ERR er;
//        if (MTR_PARAM_TAG_EMBEDEDDATA) {
//            er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
//            logPrint("tagInventoryByTimer, ParamSet MTR_PARAM_TAG_EMBEDEDDATA result: " + er.toString());
//        if (er == READER_ERR.MT_OK_ERR) {
//                MTR_PARAM_TAG_EMBEDEDDATA = false;
//        }
//        }
            List<TAGINFO> list = new ArrayList<>();

            int[] tagcnt = new int[1];
            er = reader.TagInventory_Raw(ants, 1, readtime, tagcnt);
            logPrint("tagInventoryByTimer, TagInventory_Raw er: " + er.toString() + "; tagcnt[0]=" + tagcnt[0]);

            if (er == READER_ERR.MT_OK_ERR) {
                for (int i = 0; i < tagcnt[0]; i++) {
                    TAGINFO tfs = reader.new TAGINFO();
                    er = reader.GetNextTag(tfs);
                    if (er == READER_ERR.MT_OK_ERR) {
                        list.add(tfs);
                    } else {
                        //GetNextTag出现异常的时候跳出
                        break;
                    }
                }

            } else {
                mErr = er;
                list = null;
//                return null;
            }

            return list;
        } else if (type == 3) {
            synchronized (rrTagList) {
                rrTagList.clear();
            }
            int scanRfidResult = RrReader.scanRfid(0, 1, 0, 0, "00000000", readtime);
            if (scanRfidResult == 0) {
                return formatRrTagList();
            } else {
                logPrint("Rr inventory tag by timer error: " + scanRfidResult);
            }
        }
        return null;
    }

    //6B盘存
    public List<LogBase6bInfo> inventory6BTag(short readtime) {
        List<LogBase6bInfo> list = null;
        if (type == 0) {
            MsgBaseInventory6b msg = new MsgBaseInventory6b();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    list = format6BData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

        return list;
    }

    //读6B user区
    public byte[] read6BUser(boolean isMatch, byte[] tid, int startAddr, int len) {
        byte[] data = null;
        if (type == 0) {
            MsgBaseInventory6b msg = new MsgBaseInventory6b();
            //获取天线
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            msg.setArea(EnumG.ReadMode6b_Userdata);
            //匹配参数
            Param6bReadUserdata userdata = new Param6bReadUserdata();
            userdata.setStart(startAddr);
            userdata.setLen(len);
            msg.setReadUserdata(userdata);
            //是否匹配TID
            if (isMatch && tid != null) {
                msg.setHexMatchTid(Tools.Bytes2HexString(tid, tid.length));
            }
            //发送指令
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(20);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    List<LogBase6bInfo> list6B = format6BData();
                    if (list6B != null && list6B.size() > 0) {
                        data = list6B.get(0).getbUser();
                    }

                } catch (Exception e) {

                }
            }
        }
        return data;
    }

    //写6B区
    public boolean write6BUser(byte[] tid, int startAddr, byte[] data) {
        boolean flag = false;
        if (type == 0) {
            MsgBaseWrite6b msg = new MsgBaseWrite6b();
            //天线
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            //起始地址
            msg.setStart(startAddr);
            //设置匹配的TID
            msg.setbMatchTid(tid);
            //数据内容
            msg.setBwriteData(data);
            //发送指令
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                flag = true;
            }
        }
        return flag;
    }

    //锁定6B，按字节锁定
    public boolean lock6B(byte[] tid, int lockIndex) {
        boolean flag = false;
        if (type == 0) {
            MsgBaseLock6b msg = new MsgBaseLock6b();
            //天线
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            //待锁定标签TID
            msg.setbMatchTid(tid);
            //锁定地址
            msg.setLockIndex(lockIndex);
            //发送指令
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                flag = true;
            }
        }

        return flag;
    }

    //查询锁定
    public boolean lock6BQuery(byte[] tid, int lockIndex) {
        boolean flag = false;
        if (type == 0) {
            MsgBaseLock6bGet msg = new MsgBaseLock6bGet();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setbMatchTid(tid);
            msg.setLockIndex(lockIndex);
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                flag = (msg.getLockState() == 0 ? true : false);
            }
        }


        return flag;
    }

    /****
     * 盘存国标标签
     * @param isInventoryTid
     * @param readtime
     * @return
     */
    //国芯
    public List<LogBaseGbInfo> inventoryGBTag(boolean isInventoryTid, short readtime) {
        List<LogBaseGbInfo> list = null;
        if (type == 0) {
            MsgBaseInventoryGb msg = new MsgBaseInventoryGb();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            //
            if (isInventoryTid) {
                ParamEpcReadTid tid = new ParamEpcReadTid();
                tid.setMode(EnumG.ParamTidMode_Auto);
                tid.setLen(6);
                msg.setReadTid(tid);
            }
            client.sendSynMsg(msg);

            logPrint("inventoryGBTag", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("inventoryGBTag", stop.getRtCode() + "");
                    if (filter != null && !filter.isMatching()) {
//                        return formatExcludeData(2, filter.getFilter().getbData());
                    }
                    return formatGBData(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        }
        return list;
    }

    /****
     * 盘存国标标签
     * @param isInventoryTid
     * @param readtime
     * @return
     */
    //国芯
    public List<LogBaseGJbInfo> inventoryGJBTag(boolean isInventoryTid, short readtime) {
        List<LogBaseGJbInfo> list = null;
        if (type == 0) {
            MsgBaseInventoryGJb msg = new MsgBaseInventoryGJb();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            //
            if (isInventoryTid) {
                ParamEpcReadTid tid = new ParamEpcReadTid();
                tid.setMode(EnumG.ParamTidMode_Auto);
                tid.setLen(6);
                msg.setReadTid(tid);
            }
            client.sendSynMsg(msg);

            logPrint("inventoryGBTag", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("inventoryGBTag", stop.getRtCode() + "");
                    if (filter != null && !filter.isMatching()) {
//                        return formatExcludeData(2, filter.getFilter().getbData());
                    }
                    return formatGJBData(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        }
        return list;
    }

    //国军标读标签内容

    /**
     * 国军标读标签user区内容
     *
     * @param matchType 匹配数据类型，0-不匹配， 1-标签信息区， 2-标签编码区，3-标签安全区，4-用户区
     * @param matchAddr 匹配数据起始位
     * @param matchData 匹配数据内容
     * @param readAddr  读数据起始地址
     * @param readLen   读数据长度
     * @param password  密码
     * @return
     */
    public byte[] readGJBUser(int matchType, int matchAddr, byte[] matchData, int readAddr, int readLen, byte[] password) {
        byte[] readData = null;
        MsgBaseInventoryGJb msg = new MsgBaseInventoryGJb();
        //获取天线
        msg.setAntennaEnable(EnumG.AntennaNo_1);
        msg.setInventoryMode(EnumG.InventoryMode_Inventory);
        //匹配数据类型
        if (matchType > 0) {
            ParamEpcFilter filter = new ParamEpcFilter();
            filter.setArea(matchType - 1);
            filter.setBitStart(matchAddr);
            if (matchData != null) {
                filter.setbData(matchData);
                filter.setBitLength(matchData.length * 2);
            }
            msg.setFilter(filter);
        }

        ParamEpcReadUserdata paramEpcReadUserdata = new ParamEpcReadUserdata();
        paramEpcReadUserdata.setStart(readAddr);
        paramEpcReadUserdata.setLen(readLen);
        msg.setReadUserdata(paramEpcReadUserdata);

        if (password != null) {
            msg.setHexPassword(Tools.Bytes2HexString(password, password.length));
        }
        //发送读的指令

        client.sendSynMsg(msg);
        if (0x00 == msg.getRtCode()) {
            try {
                Thread.sleep(20);
                MsgBaseStop stop = new MsgBaseStop();
                client.sendSynMsg(stop);
                List<LogBaseGJbInfo> listGJB = formatGJBData(2);
                if (listGJB != null && listGJB.size() > 0) {
                    readData = listGJB.get(0).getbUser();
                }

            } catch (Exception e) {

            }
        }
        return readData;

    }

    //写国军标标签

    /***
     *
     *
     * @param matchType  匹配数据类型，0-不匹配， 1-标签信息区， 2-标签编码区，3-标签安全区，4-用户区
     * @param matchStartAddr 匹配数据起始位，按bit计算
     * @param matchData  匹配数据内容
     * @param newEPC  新EPC数据
     * @param password   访问密码
     * @return true写入成功，false写入失败
     */
    public boolean modifyGJBEPC(int matchType, int matchStartAddr, byte[] matchData, byte[] newEPC, byte[] password) {
        boolean writeFlag = false;
        MsgBaseWriteGJb msg = new MsgBaseWriteGJb();
        msg.setAntennaEnable(EnumG.AntennaNo_1);
        msg.setArea(1);//1标签编码区，2标签安全区，3用户区
        msg.setStart(0);
        //设置匹配参数
        if (matchType > 0) {
            ParamEpcFilter filter = new ParamEpcFilter();
            filter.setArea(matchType - 1);
            filter.setbData(matchData);
            filter.setBitLength(matchData.length * 2);
            filter.setBitStart(matchStartAddr);
            msg.setFilter(filter);
        }
        if (password != null) {
            msg.setHexPassword(Tools.Bytes2HexString(password, password.length));
        }
        if (newEPC != null) {
            //写入新epc
            String s = HexUtils.bytes2HexString(newEPC);
            int pcLen = PcUtils.getValueLen(s);
            //Log.e("pang", "GJB EPC: "+PcUtils.getGJBPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
            msg.setHexWriteData(PcUtils.getGJBPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
        }

//        if (writeData != null) {
//            msg.setBwriteData(writeData);
//        }
        client.sendSynMsg(msg);
        if (0x00 == msg.getRtCode()) {
            writeFlag = true;
        }
        return writeFlag;

    }


    /**
     * @param matchType      匹配数据类型，0-不匹配， 1-标签信息区， 2-标签编码区，3-标签安全区，4-用户区
     * @param matchStartAddr 匹配数据起始位，按bit计算
     * @param matchData      匹配数据内容
     * @param areaIndex      0-标签信息区， 1-标签编码区，2-标签安全区，3-用户区
     * @param startAddr      写起始地址
     * @param writeData      写数据内容
     * @param password       访问密码
     * @return true写入成功，false写入失败
     */
    public boolean writeGJB(int matchType, int matchStartAddr, byte[] matchData, int areaIndex, int startAddr, byte[] writeData, byte[] password) {
        boolean writeFlag = false;
        MsgBaseWriteGJb msg = new MsgBaseWriteGJb();
        msg.setAntennaEnable(EnumG.AntennaNo_1);
        msg.setArea(areaIndex);//1标签编码区，2标签安全区，3用户区
        msg.setStart(startAddr);
        //设置匹配参数
        if (matchType > 0) {
            ParamEpcFilter filter = new ParamEpcFilter();
            filter.setArea(matchType - 1);
            filter.setbData(matchData);
            filter.setBitLength(matchData.length * 2);
            filter.setBitStart(matchStartAddr);
            msg.setFilter(filter);
        }
        if (password != null) {
            msg.setHexPassword(Tools.Bytes2HexString(password, password.length));
        }
        if (writeData != null) {
            msg.setBwriteData(writeData);
        }
        client.sendSynMsg(msg);
        if (0x00 == msg.getRtCode()) {
            writeFlag = true;
        }
        return writeFlag;
    }

    //国军标锁定操作

    /**
     * 国军标锁定操作
     *
     * @param matchType      匹配数据类型，0-不匹配， 1-标签信息区， 2-标签编码区，3-标签安全区，4-用户区
     * @param matchStartAddr 匹配数据起始位
     * @param matchData      匹配数据内容
     * @param lockArea       锁定区域  0-标签信息区， 1-标签编码区，2-标签安全区，3-用户区
     * @param lockType       锁定类型  0-可读可写， 1-不可读可写， 2-可读不可写， 3-不可读不可写
     * @param password       锁定密码
     * @return true锁定成功，false锁定失败
     */
    public boolean lockGJB(int matchType, int matchStartAddr, byte[] matchData, int lockArea, int lockType, byte[] password) {
        boolean lockFlag = false;
        MsgBaseLockGJb msg = new MsgBaseLockGJb();
        msg.setAntennaEnable(EnumG.AntennaNo_1);
        //设置匹配参数
        if (matchType > 0) {
            ParamEpcFilter filter = new ParamEpcFilter();
            filter.setArea(matchType - 1);
            filter.setbData(matchData);
            filter.setBitLength(matchData.length * 2);
            filter.setBitStart(matchStartAddr);
            msg.setFilter(filter);
        }
        msg.setArea(lockArea);
        msg.setLockParam(lockType);
        if (password != null) {
            msg.setHexPassword(Tools.Bytes2HexString(password, password.length));
        }
        return lockFlag;
    }

    /**
     * 盘存时附加数据读取tid（非FastTid）
     */
    public List<TAGINFO> tagEpcTidInventoryByTimer(short readtime) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            msg.setReadTid(new ParamEpcReadTid(0, 6));
            if (filter != null && filter.isMatching()) {
                msg.setFilter(filter.getFilter());
            }
//            if (fastId.getFastId() != 0) {
//              msg.setParamFastId(fastId);
//            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("tagInventoryByTimer", stop.getRtCode() + "");
                    if (filter != null && !filter.isMatching()) {
                        return formatExcludeData(2, filter.getFilter().getbData());
                    }
                    return formatData(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        } else if (type == 1) {

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
                } else {
                    break;
                }
            }
            return list;

        } else if (type == 2) {
//锐迪未完成//
            return null;
        } else if (type == 3) {
            synchronized (rrTagList) {
                rrTagList.clear();
            }
            int scanRfidResult = RrReader.scanRfid(1, 2, 0, 6, "00000000", readtime);
            if (scanRfidResult == 0) {
                return formatRrTagList();
            } else {
                logPrint("Rr inventory tag & tid by timer error: " + scanRfidResult);
            }
        }
        return null;
    }

    public List<TAGINFO> tagEpcOtherInventoryByTimer(short readtime, int bank, int startaddr, int bytecnt, @NonNull byte[] accesspwd) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            switch (bank) {
                case 0:
                    msg.setReadReserved(new ParamEpcReadReserved(startaddr, bytecnt));
                    break;
                case 1:
                    msg.setReadEpc(new ParamEpcReadEpc(startaddr + 2, bytecnt));
                    break;
                case 2:
                    msg.setReadTid(new ParamEpcReadTid(EnumG.ParamTidMode_Fixed, bytecnt));
                    break;
                case 3:
                    msg.setReadUserdata(new ParamEpcReadUserdata(startaddr, bytecnt));
                    break;
            }
            msg.setHexPassword(HexUtils.bytes2HexString(accesspwd));
            if (filter != null && filter.isMatching()) {
                msg.setFilter(filter.getFilter());
            }
            if (fastId.getFastId() != 0) {
                msg.setParamFastId(fastId);
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(readtime);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("tagEpcOtherInventory", stop.getRtCode() + "");
                    if (filter != null && !filter.isMatching()) {
                        return formatExcludeData(bank, filter.getFilter().getbData());
                    }
                    return formatData(bank);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        } else if (type == 1) {
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
                } else {
                    break;
                }
            }
            return list;
        } else if (type == 2) {
//锐迪未完成//
            return null;
        } else if (type == 3) {
            synchronized (rrTagList) {
                rrTagList.clear();
            }
            int scanRfidResult = RrReader.scanRfid(1, bank, startaddr, bytecnt / 2, Tools.Bytes2HexString(accesspwd, accesspwd.length), readtime);
            if (scanRfidResult == 0) {
                return formatRrTagList();
            } else {
                logPrint("Rr inventory tag & other by timer error: " + scanRfidResult);
            }
        }
        return null;
    }

    int Emboption = 0;//附加数据操作
    private boolean isEmb = false;//是否附加数据返回

    //设置附加数据返回
    public boolean setEMBEDEDATA(int bank, int startaddr, int bytecnt, byte[] accesspwd) {
        boolean flag = false;
        if (type == 1) {
            READER_ERR er;
//        BackReadOption m_BROption = new BackReadOption();
//        m_BROption.TMFlags.IsEmdData = true;

//        if (isquicklymode) {
//            m_BROption.IsFastRead = true;
//        }else{
//            m_BROption.IsFastRead = false;
//        }
            isEmb = true;
            Emboption = 0x0080;
            Emboption = (Emboption << 8);
            //by lbx 2017-4-27 get other:res=0, bank epc =1 tid=2 user =3
            Reader.EmbededData_ST edst = reader.new EmbededData_ST();
            edst.bank = bank;
            edst.startaddr = startaddr;
            edst.bytecnt = bytecnt;
            edst.accesspwd = accesspwd;
            er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
            if (er == READER_ERR.MT_OK_ERR) {
                flag = true;
            }
        }
        return flag;
    }

    //取消附加数据
    public boolean cancelEMBEDEDATA() {
        boolean flag = false;
        if (type == 1) {
            READER_ERR er;
            er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
            if (er == READER_ERR.MT_OK_ERR) {
                isEmb = false;
                flag = true;
            }
        }
        return flag;
    }

    public READER_ERR getTagData(int mbank, int startaddr, int len,
                                 @NonNull byte[] rdata, byte[] password, short timeout) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            switch (mbank) {
                case 0:
                    msg.setReadReserved(new ParamEpcReadReserved(startaddr, len));
                    break;
                case 1:
                    msg.setReadEpc(new ParamEpcReadEpc(startaddr, len));
                    break;
                case 2:
                    msg.setReadTid(new ParamEpcReadTid(EnumG.ParamTidMode_Fixed, len));
                    break;
                case 3:
                    msg.setReadUserdata(new ParamEpcReadUserdata(startaddr, len));
                    break;
            }
            msg.setHexPassword(HexUtils.bytes2HexString(password));
            if (fastId.getFastId() != 0) {
                msg.setParamFastId(fastId);
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(timeout);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("tagEpcOtherInventory", stop.getRtCode() + "");
                    List<TAGINFO> taginfos = formatData(mbank);
                    if (taginfos.size() > 0) {
                        System.arraycopy(taginfos.get(0).EmbededData, 0, rdata, 0, taginfos.get(0).EmbededData.length);
                        return READER_ERR.MT_OK_ERR;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
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
        } else if (type == 3) {
//            RrReader.rrlib.ClearMaskList();
            int readDataG2Result = RrReader.readG2Data(mbank, startaddr, len, password, timeout, new byte[0], 1, 0, true, rdata);
//            int readDataG2Result = RrReader.rrlib.ReadData_G2((byte) 0, new byte[0], (byte) mbank, startaddr, (byte) len, password, rdata, new byte[1]);
            if (readDataG2Result == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr get Tag data g2 error: " + readDataG2Result);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public byte[] getTagDataByFilter(int mbank, int startaddr, int len,
                                     byte[] password, short timeout, byte[] fdata, int fbank,
                                     int fstartaddr, boolean matching) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            switch (mbank) {
                case 0:
                    msg.setReadReserved(new ParamEpcReadReserved(startaddr, len));
                    break;
                case 1:
                    msg.setReadEpc(new ParamEpcReadEpc(startaddr, len));
                    break;
                case 2:
                    msg.setReadTid(new ParamEpcReadTid(EnumG.ParamTidMode_Fixed, len));
                    break;
                case 3:
                    msg.setReadUserdata(new ParamEpcReadUserdata(startaddr, len));
                    break;
            }
            msg.setHexPassword(HexUtils.bytes2HexString(password));

            if (matching) {
                ParamEpcFilter filter = new ParamEpcFilter();
                filter.setArea(fbank);
                filter.setBitStart(fstartaddr * 16);//word
                filter.setbData(fdata);
                filter.setBitLength(fdata.length * 8);
                msg.setFilter(filter);
            }

            if (fastId.getFastId() != 0) {
                msg.setParamFastId(fastId);
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(timeout);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("tagEpcOtherInventory", stop.getRtCode() + "");
                    List<TAGINFO> taginfos;
                    if (matching) {
                        taginfos = formatData(mbank);
                    } else {
                        taginfos = formatExcludeData(mbank, fdata);
                    }
                    if (taginfos.size() > 0) {
                        return taginfos.get(0).EmbededData;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        } else if (type == 1) {

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
        } else if (type == 3) {
            byte[] rdata = new byte[len * 2];
            int readDataG2Result = RrReader.readG2Data(mbank, startaddr, len, password, timeout, fdata, fbank, fstartaddr, matching, rdata);
            if (readDataG2Result == 0) {
                return rdata;
            } else {
                logPrint("Rr get tag data g2 by filter error: " + readDataG2Result);
            }
        }
        return null;
    }

    public READER_ERR writeTagData(char mbank, int startaddress, byte[] data,
                                   int datalen, byte[] accesspasswd, short timeout) {
        if (type == 0) {
            MsgBaseWriteEpc msg = new MsgBaseWriteEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setArea(mbank);
            msg.setStart(startaddress);
            String s = HexUtils.bytes2HexString(data);
            //这里使用len计算pc长度
            int pcLen = PcUtils.getValueLen(datalen);
//            if (mbank == 1) {
            //写入epc地址从1需要计算pc值
            //              if (startaddress == 1) {
            //                msg.setHexWriteData(PcUtils.getPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
            //          } else {
            msg.setHexWriteData(PcUtils.padRight(s, pcLen * 4, '0'));
            //        }
            //  } else {
            //    msg.setHexWriteData(PcUtils.padRight(s, pcLen * 4, '0'));
            //}
            msg.setHexPassword(HexUtils.bytes2HexString(accesspasswd));
            client.sendSynMsg(msg);
            logPrint("MsgBaseWriteEpc", msg.getRtMsg());
            return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
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
        } else if (type == 3) {
//            RrReader.rrlib.ClearMaskList();
            int writeDataG2Result = RrReader.writeG2Data(mbank, startaddress, data, datalen, accesspasswd, timeout, new byte[0], 1, 0, true);
//            int writeDataG2Result = RrReader.rrlib.WriteData_G2((byte) (datalen * 2), (byte) 0, new byte[0], (byte) mbank, startaddress, data, accesspasswd, new byte[1]);
            if (writeDataG2Result == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Write tag data g2 error: " + writeDataG2Result);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR writeTagDataByFilter(char mbank, int startaddress,
                                           byte[] data, int datalen, byte[] accesspasswd, short timeout,
                                           byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        if (type == 0) {
            MsgBaseWriteEpc msg = new MsgBaseWriteEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setArea(mbank);
            msg.setStart(startaddress);
            String s = HexUtils.bytes2HexString(data);
            //这里使用len计算pc长度
            int pcLen = PcUtils.getValueLen(datalen);
//            if (mbank == 1) {
            //写入epc地址从1需要计算pc值
            //              if (startaddress == 1) {
            //                msg.setHexWriteData(PcUtils.getPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
            //          } else {
            msg.setHexWriteData(PcUtils.padRight(s, pcLen * 4, '0'));
            msg.setHexWriteData(PcUtils.padRight(s, pcLen * 4, '0'));
            msg.setHexPassword(HexUtils.bytes2HexString(accesspasswd));

            if (matching) {
                ParamEpcFilter filter = new ParamEpcFilter();
                filter.setArea(fbank);
                filter.setBitStart(fstartaddr * 16);//word
                filter.setbData(fdata);
                filter.setBitLength(fdata.length * 8);
                msg.setFilter(filter);
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseWriteEpc", msg.getRtMsg());
            return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {

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
        } else if (type == 3) {
            int writeG2DataByFilterResult = RrReader.writeG2Data(mbank, startaddress, data, datalen, accesspasswd, timeout, fdata, fbank, fstartaddr, matching);
            if (writeG2DataByFilterResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Write tag data g2 by filter error: " + writeG2DataByFilterResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR writeTagEPC(byte[] data, byte[] accesspwd, short timeout) {
        if (type == 0) {
            MsgBaseWriteEpc msg = new MsgBaseWriteEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setArea(EnumG.WriteArea_Epc);
            msg.setStart(1);
            String s = HexUtils.bytes2HexString(data);
            int pcLen = PcUtils.getValueLen(s);
            msg.setHexWriteData(PcUtils.getPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
            msg.setHexPassword(HexUtils.bytes2HexString(accesspwd));
            client.sendSynMsg(msg);
            logPrint("MsgBaseWriteEpc", msg.getRtMsg());
            return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
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
        } else if (type == 2) {
            //锐迪未完成//

            return READER_ERR.MT_CMD_FAILED_ERR;


        } else if (type == 3) {
            int writeTagEpcResult = RrReader.writeTagEpc(data, accesspwd, timeout, new byte[0], 1, 0, true);
            if (writeTagEpcResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Write tag EPC error: " + writeTagEpcResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR writeTagEPCByFilter(byte[] data, byte[] accesspwd,
                                          short timeout, byte[] fdata, int fbank, int fstartaddr,
                                          boolean matching) {
        if (type == 0) {
            MsgBaseWriteEpc msg = new MsgBaseWriteEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setArea(EnumG.WriteArea_Epc);
            msg.setStart(1);
            String s = HexUtils.bytes2HexString(data);
            int pcLen = PcUtils.getValueLen(s);
            msg.setHexWriteData(PcUtils.getPc(pcLen) + PcUtils.padRight(s, pcLen * 4, '0'));
            msg.setHexPassword(HexUtils.bytes2HexString(accesspwd));
            if (matching) {
                ParamEpcFilter filter = new ParamEpcFilter();
                filter.setArea(fbank);
                filter.setBitStart(fstartaddr * 16);//word
                filter.setbData(fdata);
                filter.setBitLength(fdata.length * 8);
                msg.setFilter(filter);
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseWriteEpc", msg.getRtMsg());
            return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
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

        } else if (type == 3) {
            int writeTagEpcResult = RrReader.writeTagEpc(data, accesspwd, timeout, fdata, fbank, fstartaddr, matching);
            if (writeTagEpcResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Write tag EPC by filter error: " + writeTagEpcResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR lockTag(Lock_Obj lockobject, Lock_Type locktype,
                              byte[] accesspasswd, short timeout) {
        if (type == 0) {
            MsgBaseWriteEpc writePas = new MsgBaseWriteEpc();
            if (locktype.value() != 0) {//非0为锁定时需要写入密码
                writePas.setAntennaEnable(EnumG.AntennaNo_1);
                writePas.setArea(0);
                writePas.setStart(2);
                writePas.setHexWriteData(HexUtils.bytes2HexString(accesspasswd));
                client.sendSynMsg(writePas);
                logPrint("MsgBaseWritePas", writePas.getRtMsg());
            } else {
                writePas.setRtCode((byte) 0);//为0时表示解锁，不需要再次执行写入密码逻辑
            }
            if (writePas.getRtCode() == 0) {
                MsgBaseLockEpc msg = new MsgBaseLockEpc();
                msg.setAntennaEnable(EnumG.AntennaNo_1);
                msg.setArea((int) (Math.log(lockobject.value()) / Math.log(2)));//传入值需要对数计算转化为协议值0，1，2，3，4
                switch (locktype.value()) {
                    case 0:
                        msg.setMode(0);//解锁
                        break;
                    case 512:
                    case 128:
                    case 32:
                    case 8:
                    case 2:
                        msg.setMode(1);//锁定
                        break;
                    case 768:
                    case 192:
                    case 48:
                    case 12:
                    case 3:
                        msg.setMode(3);//永久锁定
                        break;
                }
                msg.setHexPassword(HexUtils.bytes2HexString(accesspasswd));
                client.sendSynMsg(msg);
                logPrint("MsgBaseLockEpc", msg.getRtMsg());
                return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {
            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
            if (er == READER_ERR.MT_OK_ERR) {
                er = reader.LockTag(ant, (byte) lockobject.value(),
                        (short) locktype.value(), accesspasswd, timeout);
            }
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("lockTag, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
            }
            return er;
        } else if (type == 3) {
            int lockTagResult = RrReader.lockTag(lockobject, locktype, accesspasswd, timeout, new byte[0], 1, 0, true);
            if (lockTagResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr lock tag error: " + lockTagResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR lockTagByFilter(Lock_Obj lockobject, Lock_Type locktype,
                                      byte[] accesspasswd, short timeout, byte[] fdata, int fbank,
                                      int fstartaddr, boolean matching) {
        if (type == 0) {
            MsgBaseWriteEpc writePas = new MsgBaseWriteEpc();
            if (locktype.value() != 0) {//非0为锁定时需要写入密码
                writePas.setAntennaEnable(EnumG.AntennaNo_1);
                writePas.setArea(0);
                writePas.setStart(2);
                writePas.setHexWriteData(HexUtils.bytes2HexString(accesspasswd));
                if (matching) {
                    ParamEpcFilter filter = new ParamEpcFilter();
                    filter.setArea(fbank);
                    filter.setBitStart(fstartaddr * 16);//word
                    filter.setbData(fdata);
                    filter.setBitLength(fdata.length * 8);
                    writePas.setFilter(filter);
                }
                client.sendSynMsg(writePas);
                logPrint("MsgBaseWritePas", writePas.getRtMsg());
            } else {
                writePas.setRtCode((byte) 0);//为0时表示解锁，不需要再次执行写入密码逻辑
            }
            if (writePas.getRtCode() == 0) {
                MsgBaseLockEpc msg = new MsgBaseLockEpc();
                msg.setAntennaEnable(EnumG.AntennaNo_1);
                msg.setArea((int) (Math.log(lockobject.value()) / Math.log(2)));//传入值需要对数计算转化为协议值0，1，2，3，4
                switch (locktype.value()) {
                    case 0:
                        msg.setMode(0);//解锁
                        break;
                    case 512:
                    case 128:
                    case 32:
                    case 8:
                    case 2:
                        msg.setMode(1);//锁定
                        break;
                    case 768:
                    case 192:
                    case 48:
                    case 12:
                    case 3:
                        msg.setMode(3);//永久锁定
                        break;
                }
                msg.setHexPassword(HexUtils.bytes2HexString(accesspasswd));
                if (matching) {
                    ParamEpcFilter filter = new ParamEpcFilter();
                    filter.setArea(fbank);
                    filter.setBitStart(fstartaddr * 16);//word
                    filter.setbData(fdata);
                    filter.setBitLength(fdata.length * 8);
                    msg.setFilter(filter);
                }
                client.sendSynMsg(msg);
                logPrint("MsgBaseLockEpc", msg.getRtMsg());
                return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {
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
        } else if (type == 3) {
            int lockTagResult = RrReader.lockTag(lockobject, locktype, accesspasswd, timeout, fdata, fbank, fstartaddr, true);
            if (lockTagResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr lock tag by filter error: " + lockTagResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR killTag(byte[] killpasswd, short timeout) {
        if (type == 0) {
            MsgBaseWriteEpc writePas = new MsgBaseWriteEpc();
            writePas.setAntennaEnable(EnumG.AntennaNo_1);
            writePas.setArea(0);
            writePas.setStart(0);
            writePas.setHexWriteData(HexUtils.bytes2HexString(killpasswd));
            client.sendSynMsg(writePas);
            logPrint("MsgBaseWritePas", writePas.getRtMsg());
            if (writePas.getRtCode() == 0) {
                MsgBaseDestroyEpc msg = new MsgBaseDestroyEpc();
                msg.setAntennaEnable(EnumG.AntennaNo_1);
                msg.setHexPassword(HexUtils.bytes2HexString(killpasswd));
                client.sendSynMsg(msg);
                logPrint("MsgBaseDestroyEpc", msg.getRtMsg());
                return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {
            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, null);
            if (er == READER_ERR.MT_OK_ERR) {
                er = reader.KillTag(ant, killpasswd, timeout);
            }
            if (er != READER_ERR.MT_OK_ERR) {
                logPrint("killTag, ParamSet MTR_PARAM_TAG_FILTER result: " + er.toString());
            }
            return er;
        } else if (type == 3) {
            int killG2Result = RrReader.killTag(killpasswd, timeout, new byte[0], 1, 0, true);
            if (killG2Result == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr kill g2 error: " + killG2Result);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR killTagByFilter(byte[] killpasswd, short timeout,
                                      byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        if (type == 0) {
            MsgBaseWriteEpc writePas = new MsgBaseWriteEpc();
            writePas.setAntennaEnable(EnumG.AntennaNo_1);
            writePas.setArea(0);
            writePas.setStart(0);
            writePas.setHexWriteData(HexUtils.bytes2HexString(killpasswd));
            if (matching) {
                ParamEpcFilter filter = new ParamEpcFilter();
                filter.setArea(fbank);
                filter.setBitStart(fstartaddr * 16);//word
                filter.setbData(fdata);
                filter.setBitLength(fdata.length * 8);
                writePas.setFilter(filter);
            }
            client.sendSynMsg(writePas);
            logPrint("MsgBaseWritePas", writePas.getRtMsg());
            if (writePas.getRtCode() == 0) {
                MsgBaseDestroyEpc msg = new MsgBaseDestroyEpc();
                msg.setAntennaEnable(EnumG.AntennaNo_1);
                msg.setHexPassword(HexUtils.bytes2HexString(killpasswd));
                if (matching) {
                    ParamEpcFilter filter = new ParamEpcFilter();
                    filter.setArea(fbank);
                    filter.setBitStart(fstartaddr * 16);//word
                    filter.setbData(fdata);
                    filter.setBitLength(fdata.length * 8);
                    msg.setFilter(filter);
                }
                client.sendSynMsg(msg);
                logPrint("MsgBaseDestroyEpc", msg.getRtMsg());
                return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
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
        } else if (type == 3) {
            int killG2Result = RrReader.killTag(killpasswd, timeout, fdata, fbank, fstartaddr, matching);
            if (killG2Result == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr kill g2 by filter error: " + killG2Result);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR setRegion(Region_Conf region) {
        int[] a = getPower();
        if (type == 0) {
            logPrint("zeng-", region.value() + "");
            MsgBaseSetFreqRange msg = new MsgBaseSetFreqRange();
            switch (region.value()) {
                case 6:
                    msg.setFreqRangeIndex(0);//GB_920-925
                    break;
                case 1:
                    msg.setFreqRangeIndex(3);//fcc_902_928
                    break;
                case 8:
                    msg.setFreqRangeIndex(4);//ETSI_866_868
                    break;
                case 255:
                    msg.setFreqRangeIndex(9);//TEST(全频段)
                    break;
                default:
                    msg.setFreqRangeIndex(99);//其它值给个不存在的值，默认失败
                    break;
            }
            client.sendSynMsg(msg);
            logPrint("MsgBaseSetFreqRange", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                return setPower(a[0], a[1]);
            } else {
                return READER_ERR.MT_CMD_FAILED_ERR;
            }
        } else if (type == 1) {
            return reader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION, region);
        } else if (type == 3) {
            int setRegionResult = RrReader.setRegion(region);
            if (setRegionResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr set region error: " + setRegionResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public Region_Conf getRegion() {
        if (type == 0) {
            MsgBaseGetFreqRange msg = new MsgBaseGetFreqRange();
            client.sendSynMsg(msg);
            logPrint("MsgBaseGetFreqRange", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                switch (msg.getFreqRangeIndex()) {
                    case 0://GB_920-925
                        return Region_Conf.valueOf(6);
                    case 3://fcc_902_928
                        return Region_Conf.valueOf(1);
                    case 4://ETSI_866_868
                        return Region_Conf.valueOf(8);
                    case 9://TEST(全频段)
                        return Region_Conf.valueOf(255);
                }
            }
            return null;

        } else if (type == 1) {
            Region_Conf[] rcf2 = new Region_Conf[1];
            READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION,
                    rcf2);
            if (er == READER_ERR.MT_OK_ERR) {
                return rcf2[0];
            }
            logPrint("getRegion, ParamGet MTR_PARAM_FREQUENCY_REGION result: " + er.toString());
            return null;
        } else if (type == 3) {
            byte[] band = new byte[1];
            int result = RrReader.rrlib.GetReaderInformation(new byte[2], new byte[1], band, new byte[1], new byte[1]);
            if (result == 0) {
                return RrReader.RrRegion_Conf.convertToClRegion(band[0]);
            } else {
                logPrint("Rr get region error: " + result);
            }
        }
        return null;
    }


    //获取频点，已完成，待测试
    public int[] getFrequencyPoints() {
        if (type == 0) {
            MsgBaseGetFrequency msg = new MsgBaseGetFrequency();
            client.sendSynMsg(msg);
            logPrint("MsgBaseGetFrequency", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                int[] temp = new int[msg.getListFreqCursor().size()];
                for (int i = 0; i < msg.getListFreqCursor().size(); i++) {
                    temp[i] = msg.getListFreqCursor().get(i);
                }
                return temp;
            }
            return null;
        } else if (type == 1) {
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
        return null;
    }

    //设置频点，已完成，待测试
    public READER_ERR setFrequencyPoints(int[] frequencyPoints) {
        if (type == 0) {
            MsgBaseSetFrequency msg = new MsgBaseSetFrequency();
            msg.setAutomatically(false);
            List<Integer> temp = new ArrayList<>();
            for (int i = 0; i < frequencyPoints.length; i++) {
                temp.add(frequencyPoints[i]);
            }
            msg.setListFreqCursor(temp);
            client.sendSynMsg(msg);
            logPrint("MsgBaseSetFrequency", msg.getRtMsg());
            return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;

        } else if (type == 1) {
            HoptableData_ST hdst = reader.new HoptableData_ST();
            hdst.lenhtb = frequencyPoints.length;
            hdst.htb = frequencyPoints;
            return reader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }


    //设置功率
    public READER_ERR setPower(int readPower, int writePower) {
        rPower = readPower;
        wPower = writePower;
        if (type == 0) {
            MsgBaseGetPower getPower = new MsgBaseGetPower();
            client.sendSynMsg(getPower);
            if (getPower.getRtCode() == 0) {
                if (getPower.getDicPower().get(1) == readPower
//                        || getPower.getDicPower().get(1) == writePower
                ) {
                    return READER_ERR.MT_OK_ERR;
                }
                MsgBaseSetPower msg = new MsgBaseSetPower();
                Hashtable<Integer, Integer> hashtable = new Hashtable<>();
                hashtable.put(1, readPower);
                msg.setDicPower(hashtable);
                client.sendSynMsg(msg);
                logPrint("MsgBaseSetPower", msg.getRtMsg());
                return msg.getRtCode() == 0 ? READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        } else if (type == 1) {
            AntPowerConf antPowerConf = reader.new AntPowerConf();
            antPowerConf.antcnt = ant;
            AntPower antPower = reader.new AntPower();
            antPower.antid = 1;
            antPower.readPower = (short) ((short) readPower * 100);
            antPower.writePower = (short) ((short) writePower * 100);
            antPowerConf.Powers[0] = antPower;
            return reader.ParamSet(Mtr_Param.MTR_PARAM_RF_ANTPOWER, antPowerConf);
        } else if (type == 3) {
            int setReadWritePowerResult = RrReader.setReadWritePower(readPower, writePower);
            if (setReadWritePowerResult == 0) {
                return READER_ERR.MT_OK_ERR;
            } else {
                logPrint("Rr set power error: " + setReadWritePowerResult);
            }
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    //获取功率
    public int[] getPower() {
        if (type == 0) {
            MsgBaseGetPower msg = new MsgBaseGetPower();
            client.sendSynMsg(msg);
            logPrint("MsgBaseGetPower", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                Integer power = msg.getDicPower().get(1);
                return new int[]{power, power};
            }
            return null;
        } else if (type == 1) {
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
        } else if (type == 3) {
            return RrReader.getReadWritePower();
        }
        return null;
    }

    //悦和标签
    public List<com.handheld.uhfr.Reader.TEMPTAGINFO> getYueheTagTemperature(byte[] accesspassword) {
        List<com.handheld.uhfr.Reader.TEMPTAGINFO> taginfos = null;
        if (type == 0) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            msg.setCtesius(2);
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(50);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    taginfos = formatYueheData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return taginfos;
        } else if (type == 1) {
            return null;
        } else if (type == 3) {
            taginfos = RrReader.measureYueHeTemp();
        }
        return taginfos;
    }

    //
    public List<com.handheld.uhfr.Reader.TEMPTAGINFO> getYilianTagTemperature() {
        List<com.handheld.uhfr.Reader.TEMPTAGINFO> taginfos = null;
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            ParamEpcReadUserdata userParam = new ParamEpcReadUserdata();
            userParam.setStart(127);
            userParam.setLen(1);
            msg.setReadUserdata(userParam);
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(50);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    taginfos = formatData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            return taginfos ;
        } else if (type == 1 || type == 3) {
            List<TAGINFO> list = tagEpcOtherInventoryByTimer((short) 50, 3, 127, 2, new byte[4]);
            if (list != null && !list.isEmpty()) {
                taginfos = handleYilian(type, list);
            }
        }
        return taginfos;
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
        if (type == 0) {
            int gen2session = getGen2session();
            if (gen2session != -1) {
                if (isMulti) {
                    if (gen2session != 2) {
                        MsgBaseSetBaseband msg = new MsgBaseSetBaseband();
                        msg.setSession(2);
                        msg.setqValue(4);
                        client.sendSynMsg(msg);

                        logPrint("setGen2session", msg.getRtMsg());
                    }

                    return true;
                } else {
                    MsgBaseSetBaseband msg = new MsgBaseSetBaseband();
                    msg.setSession(0);
                    msg.setqValue(4);
                    client.sendSynMsg(msg);

                    logPrint("setGen2session", msg.getRtMsg());
                    return msg.getRtCode() == 0;
                }
            }
            return false;
        } else if (type == 1) {
            try {
                READER_ERR er;
                int[] val = new int[]{-1};
                if (isMulti) {
                    val[0] = 1;
                    if (isE710) {
                        val[0] = 2;
                        //E710模式无需设置
                        return true;
                    }
                } else {
                    // session0
                    val[0] = 0;
                }
                er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);

                //////////清除快速模式设置////////////
                Reader.CustomParam_ST cpst = reader.new CustomParam_ST();
                cpst.ParamName = "Reader/Ex10fastmode";
                byte[] vals = new byte[22];
                vals[0] = 0;
                vals[1] = 20;
                for (int i = 0; i < 20; i++)
                    vals[2 + i] = 0;
                cpst.ParamVal = vals;
                reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpst);
                ///////////////////////////////////
                return er == READER_ERR.MT_OK_ERR;
            } catch (Exception var4) {
                return false;
            }
        } else if (type == 3) {
            if (isMulti) {
                return true;
            } else {
                return setGen2session(0);
            }
        }
        return false;
    }

    public boolean setGen2session(int session) {
        if (type == 0) {
            int gen2session = getGen2session();
            if (gen2session != -1) {
                if (gen2session == session) {
                    return true;
                }
                MsgBaseSetBaseband msg = new MsgBaseSetBaseband();
                msg.setSession(session);
                msg.setqValue(4);
                client.sendSynMsg(msg);

                logPrint("setGen2session", msg.getRtMsg());
                return msg.getRtCode() == 0;
            }
            return false;
        } else if (type == 1) {
            try {
                int[] val = new int[]{-1};
                val[0] = session;
                READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);
                return er == READER_ERR.MT_OK_ERR;
            } catch (Exception var4) {
                return false;
            }
        } else if (type == 3) {
            RrReader.setSession(session);
            return true;
        }
        return false;
    }

    /**
     * 获取session
     *
     * @return
     */
    public int getGen2session() {
        if (type == 0) {
            MsgBaseGetBaseband msg = new MsgBaseGetBaseband();
            client.sendSynMsg(msg);
            logPrint("getGen2session", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                return msg.getSession();
            }
        } else if (type == 1) {
            int[] val = new int[]{-1};
            READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);
            if (er == READER_ERR.MT_OK_ERR) {
                logPrint("pang", "getGen2session = " + val[0]);
                return val[0];
            }
        } else if (type == 3) {
            return RrReader.getSession();
        }
        return -1;
    }

    private int Q = 0;

    // 设置Q值
    public boolean setQvaule(int qvaule) {
        boolean flag = false;
        if (type == 0) {
            MsgBaseSetBaseband msg = new MsgBaseSetBaseband();
            msg.setqValue(qvaule);
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                logPrint("setQvaule", msg.getRtMsg());
                flag = true;
            }
        } else if (type == 1) {
//            int[] val = new int[]{-1};
//            val[0] = qvaule;
//            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_Q, val);
//            if (er == READER_ERR.MT_OK_ERR) {
//                flag = true;
//            }
            Q = qvaule;
            flag = true;
        } else if (type == 3) {
            RrReader.setQ(qvaule);
            return true;
        }

        return flag;
    }

    // 获取Q值
    public int getQvalue() {
        int value = -1;
        if (type == 0) {
            MsgBaseGetBaseband getBaseband = new MsgBaseGetBaseband();
            client.sendSynMsg(getBaseband);
            if (getBaseband.getRtCode() == 0) {
                value = getBaseband.getqValue();
                logPrint("getQvalue", getBaseband.getRtMsg());
            }
        } else if (type == 1) {
//            int[] val = new int[]{-1};
//            READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_POTL_GEN2_Q, val);
//            if (er == READER_ERR.MT_OK_ERR) {
//                value = val[0];
//            }
            value = Q;
        } else if (type == 3) {
            return RrReader.getQ();
        }

        return value;
    }

    //获取A|B面
    public int getTarget() {
        int target = -1;
        if (type == 0) {
            MsgBaseGetBaseband msg = new MsgBaseGetBaseband();
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                target = msg.getInventoryFlag();
            }
        } else if (type == 1) {
            int[] val = new int[]{-1};
            READER_ERR er = reader.ParamGet(Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, val);
            if (er == READER_ERR.MT_OK_ERR) {
                target = val[0];
            }
        } else if (type == 3) {
            return RrReader.rrlib.GetInventoryPatameter().Target;
        }
        return target;
    }

    //设置A|B面
    public boolean setTarget(int target) {
        boolean flag = false;
        if (type == 0) {
            MsgBaseSetBaseband msg = new MsgBaseSetBaseband();
            msg.setInventoryFlag(target);
            client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                flag = true;
            }
        } else if (type == 1) {
            int[] val = new int[]{-1};
            val[0] = target;
            READER_ERR er = reader.ParamSet(Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, val);
            if (er == READER_ERR.MT_OK_ERR) {
                flag = true;
            }
        } else if (type == 3) {
            RrReader.setTarget(target);
            return true;
        }

        return flag;
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

    //
    public READER_ERR ReadTagLED(int ant, short timeout, short metaflag, TagLED_DATA tagled) {
        if (type == 0) {
            return READER_ERR.MT_CMD_NO_TAG_ERR;
        } else {
            return reader.ReadTagLED(ant, timeout, metaflag, tagled);

        }
    }

    /**
     * 开启/关闭FastTid
     */
    public boolean setFastID(boolean isOpenFastTiD) {
        if (type == 0) {
            fastId.setFastId(isOpenFastTiD ? 1 : 0);
            return true;
        } else if (type == 1) {
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
        } else if (type == 3) {
            RrReader.setFastId(isOpenFastTiD);
            return true;
        }
        return false;
    }

    /**
     * 设置荣睿的盘点间隔以及载波持续时间
     *
     * @return 0 成功，其他 失败
     */
    public int setRrJgDwell(int jgTiem, int dwell) {
        if (type == 3) {
            return RrReader.setJgDwell(jgTiem, dwell);
        } else {
            return -1;
        }
    }

    /**
     * 获取荣睿的盘点间隔以及载波持续时间
     */
    public int[] getRrJgDwell() {
        if (type == 3) {
            return RrReader.getJgDwell();
        } else {
            return new int[]{-1, -1};
        }
    }

    //国芯的方法
    private static void onTagHandler() {
        client.onTagEpcLog = new HandlerTagEpcLog() {
            @Override
            public void log(String readerName, LogBaseEpcInfo info) {
                //logPrint("onTagEpcLog", "info.getResult() = " + info.getResult());
                if (DEBUG && info != null) {
                    //logPrint("onTagEpcLog", "EPC: " + info.getEpc());
                }
                //info.getResult() == 4为LED标签盘点时返回
                if (info.getResult() == 0 || info.getResult() == 4) {
                    synchronized (epcList) {
                        //暂时存储时间戳
                        info.setReplySerialNumber(System.currentTimeMillis());
//                        if(info.getEpc() != null)
                        epcList.add(info);
                    }
                }
                //logPrint("onTagEpcLog", "epcList.size() = " + epcList.size());
            }
        };
        client.onTagEpcOver = new HandlerTagEpcOver() {
            @Override
            public void log(String readerName, LogBaseEpcOver info) {
                if (DEBUG) {
                    logPrint("onTagEpcOver", "HandlerTagEpcOver");
                }
                //温度读取还需要同步
                synchronized (epcList) {
                    epcList.notify();
                }
            }
        };

        client.onTagGbLog = new HandlerTagGbLog() {
            public void log(String readerName, LogBaseGbInfo info) {
//                System.out.println(info);
                if (info.getResult() == 0) {
                    logPrint("pang", "gbepc = " + info.getEpc());
                    synchronized (gbepcList) {
                        //暂时存储时间戳
                        gbepcList.add(info);
                    }
//
                }

            }
        };
        client.onTagGbOver = new HandlerTagGbOver() {
            public void log(String readerName, LogBaseGbOver info) {
//                handlerStop.sendEmptyMessage(new Message().what = 1);
                synchronized (gbepcList) {
                    gbepcList.notify();
                }
            }
        };

        client.onTagGJbLog = new HandlerTagGJbLog() {
            public void log(String readerName, LogBaseGJbInfo info) {
//                System.out.println(info);
                if (info.getResult() == 0) {
                    logPrint("pang", "gbepc = " + info.getEpc());
                    synchronized (gjbepcList) {
                        //暂时存储时间戳
                        gjbepcList.add(info);
                    }
//
                }

            }
        };
        client.onTagGJbOver = new HandlerTagGJbOver() {
            @Override
            public void log(String s, LogBaseGJbOver logBaseGJbOver) {
                synchronized (gjbepcList) {
                    gjbepcList.notify();
                }
            }
        };

        client.onTag6bLog = new HandlerTag6bLog() {
            @Override
            public void log(String s, LogBase6bInfo logBase6bInfo) {
                if (logBase6bInfo.getResult() == 0) {
//                    logPrint("pang", "gbepc = " + info.getEpc());
                    synchronized (tag6bList) {
                        //暂时存储时间戳
                        tag6bList.add(logBase6bInfo);
                    }
//
                }
            }
        };

        client.onTag6bOver = new HandlerTag6bOver() {
            @Override
            public void log(String s, LogBase6bOver logBase6bOver) {
                tag6bList.notify();
            }
        };
    }

    private static long lastEnterTime = SystemClock.elapsedRealtime();
    public static class MsgCallback implements TagCallback {

        @Override
        public void tagCallback(ReadTag arg0) {
            synchronized (rrTagList) {
                rrTagList.add(arg0);
            }
        }

        @Override
        public void StopReadCallBack() {
            logPrint("Rr stop read callback");
        }
    }

    private List<Reader.TAGINFO> formatRrTagList() {
        synchronized (rrTagList) {
            HashMap<String, TAGINFO> tagMap = new HashMap<>();
            for (ReadTag info : rrTagList) {
                TAGINFO taginfo = new Reader().new TAGINFO();
                taginfo.AntennaID = (byte) info.antId;
                byte[] epcIdBytes = Tools.HexString2Bytes(info.epcId);
                taginfo.EpcId = epcIdBytes;
                taginfo.Epclen = (short) epcIdBytes.length;
                if (info.memId != null && info.memId.length() > 0) {
                    int ivtType = RrReader.rrlib.GetInventoryPatameter().IvtType;
                    if (ivtType == 2) {
                        byte[] fastIdBytes = Tools.HexString2Bytes(info.epcId + info.memId);
                        taginfo.EpcId = fastIdBytes;
                        taginfo.Epclen = (short) fastIdBytes.length;
                    } else {
                        byte[] embededDataBytes = Tools.HexString2Bytes(info.memId);
                        taginfo.EmbededData = embededDataBytes;
                        taginfo.EmbededDatalen = (short) embededDataBytes.length;
                    }
                }
                taginfo.protocol = SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
                taginfo.Phase = info.phase;
                double v = -130 + info.rssi;
                taginfo.RSSI = (int) Math.round(v);
                if (!tagMap.containsKey(info.epcId)) {
                    taginfo.ReadCnt = 1;
                    tagMap.put(info.epcId, taginfo);
                } else {
                    TAGINFO temp = tagMap.get(info.epcId);
                    if (temp != null) {
                        temp.ReadCnt += 1;
                        tagMap.put(info.epcId, temp);
                    }
                }
            }
            rrTagList.clear();
            return new ArrayList<>(tagMap.values());
        }
    }

    //坤锐
    public READER_ERR getTemperature(@NonNull byte[] rdata) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            msg.setQuanray(1);
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(1000);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    logPrint("tagEpcOtherInventory", stop.getRtCode() + "");
                    List<TAGINFO> taginfos = formatKRData();
                    if (taginfos.size() > 0) {
                        System.arraycopy(taginfos.get(0).EmbededData, 0, rdata, 0, taginfos.get(0).EmbededData.length);
                        return READER_ERR.MT_OK_ERR;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return READER_ERR.MT_CMD_FAILED_ERR;
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    public READER_ERR getOpen(@NonNull byte[] rdata) {
        if (type == 0) {
            MsgBaseInventoryEpc msg = new MsgBaseInventoryEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setInventoryMode(EnumG.InventoryMode_Inventory);
            msg.setQuanray(2);
            client.sendSynMsg(msg);
            logPrint("MsgBaseInventoryEpc", msg.getRtMsg());
            if (msg.getRtCode() == 0) {
                try {
                    Thread.sleep(100);
                    MsgBaseStop stop = new MsgBaseStop();
                    client.sendSynMsg(stop);
                    List<TAGINFO> taginfos = formatKRData();
                    if (taginfos.size() > 0) {
                        System.arraycopy(taginfos.get(0).EmbededData, 0, rdata, 0, taginfos.get(0).EmbededData.length);
                        return READER_ERR.MT_OK_ERR;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return READER_ERR.MT_CMD_FAILED_ERR;
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    //bank 0:RESERVED区，1:EPC区，2:TID区，3:USER区//国芯的处理温度标签的方法
    public List<TAGINFO> formatKRData() {
        HashMap<String, TAGINFO> tagMap = new HashMap<>();
        synchronized (epcList) {
            for (LogBaseEpcInfo info : epcList) {
                TAGINFO taginfo = new Reader().new TAGINFO();
                taginfo.AntennaID = (byte) info.getAntId();
                if (info.getFrequencyPoint() != null) {
                    taginfo.Frequency = info.getFrequencyPoint().intValue();
                }
                if (info.getReplySerialNumber() != null) {
                    taginfo.TimeStamp = info.getReplySerialNumber().intValue();
                }
                if (info.getUserdata() != null) {
                    taginfo.EmbededData = info.getbUser();
                    taginfo.EmbededDatalen = (short) info.getbUser().length;
                }

                taginfo.EpcId = info.getbEpc();
                taginfo.Epclen = (short) info.getbEpc().length;
                taginfo.PC = HexUtils.int2Bytes(info.getPc());
                double v = -130 + info.getRssi();
                taginfo.RSSI = (int) Math.round(v);

                //epcbank读取会影响速度，新增协议0x15获取
                if (info.getCrc() != 0) {
                    taginfo.CRC = HexUtils.int2Bytes(info.getCrc());
                }
            }
            epcList.clear();
            return new ArrayList<>(tagMap.values());
        }
    }

    public READER_ERR LEDKR(byte[] fdata, int fbank, int fstartaddr) {
        if (type == 0) {

            MsgBaseWriteEpc msg = new MsgBaseWriteEpc();
            msg.setAntennaEnable(EnumG.AntennaNo_1);
            msg.setArea(EnumG.WriteArea_Userdata);
            msg.setStart(128);
            msg.setHexWriteData("0001");
            ParamEpcFilter filter = new ParamEpcFilter();
            filter.setArea(fbank);
            filter.setBitStart(fstartaddr * 16);//word
            filter.setbData(fdata);
            filter.setBitLength(fdata.length * 8);
            msg.setFilter(filter);
            msg.setStayCarrierWave(1);//载波保持client.sendSynMsg(msg);
            if (msg.getRtCode() == 0) {
                return Reader.READER_ERR.MT_OK_ERR;

            }
//            return READER_ERR.MT_CMD_FAILED_ERR;
        }
        return READER_ERR.MT_CMD_FAILED_ERR;

    }


    public READER_ERR StopLEDKR() {
        if (type == 0) {
            MsgBaseStop stop = new MsgBaseStop();
            client.sendSynMsg(stop);
            return READER_ERR.MT_OK_ERR;
        }
        return READER_ERR.MT_CMD_FAILED_ERR;

    }

    public void setAttachedData() {
        if (type == 1) {

            ArrayList<InvEmbeddedBankData> bankdatas = new ArrayList<InvEmbeddedBankData>();
//            bankdatas.add(new InvEmbeddedBankData((byte)0, 0, (byte)2));
//            bankdatas.add(new InvEmbeddedBankData((byte)2, 0, (byte)6));
            bankdatas.add(new InvEmbeddedBankData((byte) 3, 0, (byte) 2));
            byte[] accpwd = new byte[]{0x00, 0x00, 0x00, 0x00};
            READER_ERR err = reader.SetInvMultiEmbeddedData(bankdatas, accpwd);
        }
    }

    public int setCarrier(int value) {

        if (type == 3) {
            return RrReader.setCarrier(value);

        }
        return -1;
    }

    //isOpen true    开载波，false关载波
    //power 功率      33传3300
    //fre   频点      输入载波频点：例如：915250
    public READER_ERR setCarrier(boolean isOpen, int power, int fre) {
        if (type == 1) {
            if (isOpen) {
                try {
                    Reader.CustomParam_ST cpst = reader.new CustomParam_ST();

                    cpst.ParamName = "0";
                    byte[] vals = new byte[9];
                    int p = 0;
                    vals[p++] = 0x01;
                    vals[p++] = 0x01;
                    vals[p++] = (byte) ant;
                    vals[p++] = (byte) ((power & 0xff00) >> 8);
                    vals[p++] = (byte) (power & 0x00ff);
                    vals[p++] = (byte) ((fre & 0xff000000) >> 24);
                    vals[p++] = (byte) ((fre & 0x00ff0000) >> 16);
                    vals[p++] = (byte) ((fre & 0x0000ff00) >> 8);
                    vals[p++] = (byte) (fre & 0x000000ff);
                    cpst.ParamVal = vals;
                    return reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpst);

                } catch (Exception ex) {
                    return null;
                }
            } else {
                try {
                    Reader.CustomParam_ST cpst = reader.new CustomParam_ST();

                    cpst.ParamName = "0";
                    byte[] vals = new byte[9];
                    int p = 0;
                    vals[p++] = 0x01;
                    vals[p++] = 0x00;
                    vals[p++] = (byte) ant;
                    vals[p++] = (byte) ((power & 0xff00) >> 8);
                    vals[p++] = (byte) (power & 0x00ff);
                    vals[p++] = (byte) ((fre & 0xff000000) >> 24);
                    vals[p++] = (byte) ((fre & 0x00ff0000) >> 16);
                    vals[p++] = (byte) ((fre & 0x0000ff00) >> 8);
                    vals[p++] = (byte) (fre & 0x000000ff);
                    cpst.ParamVal = vals;
                    return reader.ParamSet(Mtr_Param.MTR_PARAM_CUSTOM, cpst);

                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

}

