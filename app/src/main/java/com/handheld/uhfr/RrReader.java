package com.handheld.uhfr;

import android.util.Log;

import com.rfid.trans.MaskClass;
import com.rfid.trans.ReaderHelp;
import com.rfid.trans.ReaderParameter;
import com.uhf.api.cls.Reader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import cn.pda.serialport.Tools;

/**
 * @author LeiHuang
 * 荣睿读写管理类
 */
public class RrReader {
    public static ReaderHelp rrlib;

    private static int savedSession = -1;
    private static int savedQValue = -1;

    /**
     * 荣睿频段参数
     */
    public enum RrRegion_Conf {
        /**
         * 中国2频段 [920.125MHz, 924.875MHz], 920.125 + N*0.25 (MHz) 其中N∈[0, 19]。
         */
        RG_PRC2(1),
        /**
         * 北美频段 [902.75MHz, 927.25MHz], 902.75 + N*0.5 (MHz) 其中N∈[0, 49]。
         */
        RG_NA(2),
        /**
         * 韩国频段 [917.1MHz, 923.3MHz], 917.1 + N*0.2 (MHz) 其中N∈[0, 31]。
         */
        RG_KR(3),
        /**
         * 欧洲频段 [865.1MHz, 867.9MHz], 865.1 + N*0.2 (MHz) 其中N∈[0, 14]。
         */
        RG_EU3(4),
        /**
         * 中国1频段 [840.125MHz, 844.875MHz], 840.125 + N*0.25 (MHz) 其中N∈[0, 19]。
         */
        RG_PRC(8),
        /**
         * 全频段 [840.0MHz, 960.0MHz], 840.0 + N*2 (MHz) 其中N∈[0, 60]。
         */
        RG_OPEN(0),
        /**
         * 未知的频段
         */
        RG_NONE(255);

        private final int value;

        RrRegion_Conf(int i) {
            this.value = i;
        }

        public int value() {
            return value;
        }

        public static RrRegion_Conf valueOf(int value) {
            switch (value) {
                case 6:
                    // 芯联中国1频段对应荣睿中国2频段
                    return RG_PRC2;
                case 1:
                    // 芯联北美频段对应荣睿北美频段
                    return RG_NA;
                case 3:
                    // 芯联韩国频段对应荣睿韩国频段
                    return RG_KR;
                case 8:
                    // 芯联欧洲3频段对应荣睿的欧洲频段
                    return RG_EU3;
                case 10:
                    // 芯联的中国2频段对应荣睿的中国1频段
                    return RG_PRC;
                case 255:
                    // 芯联的全频段对应荣睿的全频段
                    return RG_OPEN;
                default:
                    return RG_NONE;
            }
        }

        public static Reader.Region_Conf convertToClRegion(int value) {
            switch (value) {
                case 1:
                    // 芯联中国1频段对应荣睿中国2频段
                    return Reader.Region_Conf.RG_PRC;
                case 2:
                    // 芯联北美频段对应荣睿北美频段
                    return Reader.Region_Conf.RG_NA;
                case 3:
                    // 芯联韩国频段对应荣睿韩国频段
                    return Reader.Region_Conf.RG_KR;
                case 4:
                    // 芯联欧洲3频段对应荣睿的欧洲频段
                    return Reader.Region_Conf.RG_EU3;
                case 8:
                    // 芯联的中国2频段对应荣睿的中国1频段
                    return Reader.Region_Conf.RG_PRC2;
                case 0:
                    // 芯联的全频段对应荣睿的全频段
                    return Reader.Region_Conf.RG_OPEN;
                default:
                    return Reader.Region_Conf.RG_NONE;
            }
        }
    }

    public static enum RrLockObj {
        /**
         * 销毁密码
         */
        LOCK_OBJECT_KILL_PASSWORD(0),
        /**
         * 访问密码
         */
        LOCK_OBJECT_ACCESS_PASSWD(1),
        /**
         * EPC
         */
        LOCK_OBJECT_BANK1(2),
        /**
         * TID
         */
        LOCK_OBJECT_BANK2(3),
        /**
         * USER
         */
        LOCK_OBJECT_BANK3(4),
        /**
         * 无效
         */
        LOCK_OBJECT_NONE(255);

        private final int pV;

        private RrLockObj(int v) {
            this.pV = v;
        }

        public int value() {
            return this.pV;
        }

        public static RrLockObj valueOf(Reader.Lock_Obj lockObj) {
            switch (lockObj.value()) {
                case 1:
                    return LOCK_OBJECT_KILL_PASSWORD;
                case 2:
                    return LOCK_OBJECT_ACCESS_PASSWD;
                case 4:
                    return LOCK_OBJECT_BANK1;
                case 8:
                    return LOCK_OBJECT_BANK2;
                case 16:
                    return LOCK_OBJECT_BANK3;
                default:
                    return LOCK_OBJECT_NONE;
            }
        }
    }

    public static enum RrLockType {
        /**
         * 可读写，解锁
         */
        UNLOCK(0),
        /**
         * 永久可读写，永久解锁
         */
        PERM_UNLOCK(1),
        /**
         * 带密码可读写，锁定
         */
        LOCK(2),
        /**
         * 永远不可读写，永久锁定
         */
        PERM_LOCK(3),
        /**
         * 无效参数
         */
        NONE(255);

        private final int pV;

        private RrLockType(int v) {
            this.pV = v;
        }

        public int value() {
            return this.pV;
        }

        public static RrLockType valueOf(Reader.Lock_Type lockType) {
            switch (lockType.value()) {
                case 0:
                    return UNLOCK;
                case 512:
                case 128:
                case 32:
                case 8:
                case 2:
                    return LOCK;
                case 768:
                case 192:
                case 48:
                case 12:
                case 3:
                    return PERM_LOCK;
                default:
                    return NONE;
            }
        }
    }

    public static int connect(String comPort, int baudRate, int logswitch) {
        rrlib = new ReaderHelp();
        // 默认Q值为8，默认session为0
        ReaderParameter param = rrlib.GetInventoryPatameter();
        param.Session = 0;
        savedSession = 0;
        savedQValue = param.QValue;
        rrlib.SetInventoryPatameter(param);
        int result = rrlib.Connect(comPort, baudRate, logswitch);
        Log.d("huang,UHFRManager", "Rr connect rrlib result = " + result);
        if (result == 0) {
            // 恢复默认写入功率设置
            byte[] readPower = new byte[1];
            result = rrlib.GetReaderInformation(new byte[2], readPower, new byte[1], new byte[1], new byte[1]);
            if (result == 0) {
                int writePower = (readPower[0] | (1 << 7));
                result = rrlib.SetWritePower((byte) writePower);
                if (result == 0) {
                    result = setJgDwell(6, 2);
                }
            }
        }
        return result;
    }

    public static int getMaxFrmPoint(int region) {
        int maxFrmPoint = 0;
        if (region == RrRegion_Conf.RG_PRC2.value()) {
            maxFrmPoint = 19;
        } else if (region == RrRegion_Conf.RG_NA.value()) {
            maxFrmPoint = 49;
        } else if (region == RrRegion_Conf.RG_KR.value()) {
            maxFrmPoint = 31;
        } else if (region == RrRegion_Conf.RG_EU3.value()) {
            maxFrmPoint = 14;
        } else if (region == RrRegion_Conf.RG_PRC.value()) {
            maxFrmPoint = 19;
        } else if (region == RrRegion_Conf.RG_OPEN.value()) {
            maxFrmPoint = 60;
        }
        return maxFrmPoint;
    }

    public static String getVersion() {
        byte[] version = new byte[2];
        int result = rrlib.GetReaderInformation(version, new byte[1], new byte[1], new byte[1], new byte[1]);
        if (result == 0) {
            String hvn = String.valueOf(version[0] & 255);
            if (hvn.length() == 1) {
                hvn = "0" + hvn;
            }
            String lvn = String.valueOf(version[1] & 255);
            if (lvn.length() == 1) {
                lvn = "0" + lvn;
            }
            String moduleInfo;
            int readerType = rrlib.GetReaderType();
            if (readerType == 0x70 || readerType == 0x71 || readerType == 0x31) {
                byte[] describe = new byte[16];
                rrlib.GetModuleDescribe(describe);
                String dscInfo = "";
                if (describe[0] == 0x00) {
                    dscInfo = "S";
                } else if (describe[0] == 0x01) {
                    dscInfo = "Plus";
                } else if (describe[0] == 0x02) {
                    dscInfo = "Pro";
                }
                moduleInfo = hvn + "." + lvn + " (" + Integer.toHexString(readerType) + "-" + dscInfo + ")";
            } else {
                moduleInfo = hvn + "." + lvn + " (" + Integer.toHexString(readerType) + ")";
            }
            return moduleInfo;
        }
        return "";
    }

    public static int setRegion(Reader.Region_Conf region) {
        RrReader.RrRegion_Conf regionConf = RrReader.RrRegion_Conf.valueOf(region.value());
        int maxFrmPoint = RrReader.getMaxFrmPoint(regionConf.value());
        return rrlib.SetRegion((byte) regionConf.value(), (byte) maxFrmPoint, (byte) 0);
    }

    public static int setReadWritePower(int readPower, int writePower) {
        // Bit7=0掉电保存；Bit7=1掉电不保存
        readPower = (readPower | (1 << 7));
        int result = rrlib.SetRfPower((byte) readPower);
        if (result == 0) {
            // Bit7：是否启用写功率设置。
            // 0 – 不启用写功率设置。执行写操作相关命令时的功率与读功率一样；
            // 1 – 启用写功率设置。执行写操作相关命令时的功率为Bit6～Bit0设定的功率值。
            writePower = (writePower | (1 << 7));
            // 该命令用于单独设置读写器在执行写操作相关命令时的功率，写功率参数默认值为0x00，该参数掉电不丢失
            result = rrlib.SetWritePower((byte) writePower);
        }
        return result;
    }

    public static int[] getReadWritePower() {
        byte[] writePower = new byte[1];
        int result = rrlib.GetWritePower(writePower);
        if (result == 0) {
            byte[] readPower = new byte[1];
            result = rrlib.GetReaderInformation(new byte[2], readPower, new byte[1], new byte[1], new byte[1]);
            if (result == 0) {
                int[] powers = new int[2];
                powers[0] = readPower[0] & 127;
                powers[1] = writePower[0] & 127;
                return powers;
            }
        }
        return null;
    }

    public static void setSession(int session) {
        ReaderParameter param = rrlib.GetInventoryPatameter();
        param.Session = session;
        savedSession = session;
        rrlib.SetInventoryPatameter(param);
    }

    public static int getSession() {
        return savedSession;
    }

    public static void setQ(int qValue) {
        ReaderParameter param = rrlib.GetInventoryPatameter();
        param.QValue = qValue;
        savedQValue = qValue;
        rrlib.SetInventoryPatameter(param);
    }

    public static int getQ() {
        return savedQValue;
    }

    public static void setInvMask(byte[] strData, int fbank, int fstartaddr, boolean matching) {
        MaskClass mask = new MaskClass();
        mask.MaskData = strData;
        int maskAddr = fstartaddr * 16;
        mask.MaskAdr[0] = (byte) (maskAddr >> 8);
        mask.MaskAdr[1] = (byte) (maskAddr);
        int maskLen = strData.length * 8;
        mask.MaskLen = (byte) maskLen;
        mask.MaskMem = (byte) fbank;
        rrlib.AddMaskList(mask);
        rrlib.SetMatchType(matching ? (byte) 0 : (byte) 1);
    }

    public static int setJgDwell(int jgTime, int dwell) {
        if (rrlib.ModuleType == 2) {
            byte[] data = new byte[3];
            data[0] = (byte) jgTime;
            data[1] = (byte) dwell;
            int len = 3;
            return rrlib.SetCfgParameter((byte) 0, (byte) 7, data, len);
        }
        return -1;
    }

    public static int[] getJgDwell() {
        int[] ints = new int[]{-1, -1};
        if (rrlib.ModuleType == 2) {
            byte[] data = new byte[30];
            int[] len = new int[1];
            int fCmdRet = rrlib.GetCfgParameter((byte) 7, data, len);
            if (fCmdRet == 0 && len[0] == 3) {
                ints[0] = (data[0] & 0xFF);
                ints[1] = (data[1] & 0xFF);
            }
        }
        return ints;
    }

    public static int startRead() {
        ReaderParameter parameter = rrlib.GetInventoryPatameter();
        parameter.ScanTime = 50;
        parameter.Session = 253;
        parameter.QValue = 8;
        if (parameter.IvtType != 2) {
            parameter.IvtType = 0;
        }
        rrlib.SetInventoryPatameter(parameter);
        return rrlib.StartRead();
    }

    public static void stopRead() {
        rrlib.StopRead();
        ReaderParameter parameter = rrlib.GetInventoryPatameter();
        parameter.Session = savedSession;
        parameter.QValue = savedQValue;
        rrlib.SetInventoryPatameter(parameter);
    }

    public static int scanRfid(int ivtType, int memory, int wordPtr, int length, String psw, int readTime) {
        try {
            int result;
            synchronized (UHFRManager.waitLock) {
                ReaderParameter parameter = rrlib.GetInventoryPatameter();
                if (parameter.IvtType != 2 || ivtType != 0) {
                    parameter.IvtType = ivtType;
                }
                parameter.Session = savedSession;
                parameter.QValue = savedQValue;
                parameter.Memory = memory;
                parameter.WordPtr = wordPtr;
                parameter.Length = length;
                parameter.Password = psw;
                rrlib.SetInventoryPatameter(parameter);
                result = rrlib.ScanRfid(Math.max(readTime, 100));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -2;
    }

    private static void setParameterMask(byte maskMem, byte maskAdr, byte maskLen, byte[] maskData) {
        int maskAdr2 = maskAdr * 16;
        ReaderParameter patameter = rrlib.GetInventoryPatameter();
        patameter.MaskMem = (byte) maskMem;
        patameter.MaskAdr[0] = (byte) (maskAdr2 >> 8);
        patameter.MaskAdr[1] = (byte) (maskAdr2);
        patameter.MaskLen = (byte) (maskLen * 8);
        patameter.MaskData = maskData;
        rrlib.SetInventoryPatameter(patameter);
    }

    public static void setTarget(int target) {
        ReaderParameter parameter = rrlib.GetInventoryPatameter();
        parameter.Target = target;
        rrlib.SetInventoryPatameter(parameter);
    }

    public static void setFastId(boolean openFlag) {
        ReaderParameter param = rrlib.GetInventoryPatameter();
        param.IvtType = openFlag ? 2 : 0;
        rrlib.SetInventoryPatameter(param);
    }

    public static int readG2Data(int mbank, int startaddr, int len,
                                 byte[] password, short timeout, byte[] fdata, int fbank,
                                 int fstartaddr, boolean matching, byte[] rdata) {
        rrlib.ClearMaskList();
        setParameterMask((byte) fbank, (byte) fstartaddr, (byte) fdata.length, fdata);
        rrlib.SetMatchType(matching ? (byte) 0 : (byte) 1);
        int readDataG2Result = rrlib.ReadData_G2((byte) (fdata.length == 0 ? 0 : 255), new byte[0], (byte) mbank, startaddr, (byte) len, password, rdata, new byte[1]);
        setParameterMask((byte) 1, (byte) 0, (byte) 0, new byte[0]);
        return readDataG2Result;
    }

    public static int writeG2Data(char mbank, int startaddress,
                                  byte[] data, int datalen, byte[] accesspasswd, short timeout,
                                  byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        rrlib.ClearMaskList();
        setParameterMask((byte) fbank, (byte) fstartaddr, (byte) fdata.length, fdata);
        rrlib.SetMatchType(matching ? (byte) 0 : (byte) 1);
        int writeDataG2Result = rrlib.WriteData_G2((byte) (datalen / 2), (byte) (fdata.length == 0 ? 0 : 255), new byte[0], (byte) mbank, startaddress, data, accesspasswd, new byte[1]);
        setParameterMask((byte) 1, (byte) 0, (byte) 0, new byte[0]);
        return writeDataG2Result;
    }

    public static int writeTagEpc(byte[] data, byte[] accesspwd,
                                  short timeout, byte[] fdata, int fbank, int fstartaddr,
                                  boolean matching) {
        rrlib.ClearMaskList();
        setParameterMask((byte) fbank, (byte) fstartaddr, (byte) fdata.length, fdata);
        rrlib.SetMatchType(matching ? (byte) 0 : (byte) 1);
        int pc = (data.length / 2) << 11;
        byte[] pcBytes = new byte[2];
        pcBytes[0] = (byte) ((pc & 0xFF00) >> 8);
        pcBytes[1] = (byte) (pc & 0xFF);
        byte[] finalData = new byte[data.length + 2];
        System.arraycopy(pcBytes, 0, finalData, 0, pcBytes.length);
        System.arraycopy(data, 0, finalData, pcBytes.length, data.length);
        int writeDataG2Result = rrlib.WriteData_G2((byte) (finalData.length / 2), (byte) (fdata.length == 0 ? 0 : 255), new byte[0], (byte) 1, 1, finalData, accesspwd, new byte[1]);
        setParameterMask((byte) 1, (byte) 0, (byte) 0, new byte[0]);
        return writeDataG2Result;
    }

    public static int lockTag(Reader.Lock_Obj lockobject, Reader.Lock_Type locktype,
                              byte[] accesspasswd, short timeout, byte[] fdata, int fbank,
                              int fstartaddr, boolean matching) {
        byte epclen = 0;
        if (fdata != null && fdata.length > 0) {
            // 荣睿锁定标签，只能以EPC作为过滤条件
            if (fbank != 1 || fstartaddr != 2) {
                Log.e("huang,UHFRManager", "Rr lock tag to unsupported fbank or fstartaddr");
                return -2;
            }
            epclen = (byte) (fdata.length / 2);
        }
        RrLockObj lockObj = RrLockObj.valueOf(lockobject);
        RrLockType lockType = RrLockType.valueOf(locktype);
        return rrlib.Lock_G2(epclen, fdata, (byte) lockObj.value(), (byte) lockType.value(), accesspasswd, new byte[1]);
    }

    public static int killTag(byte[] killpasswd, short timeout,
                              byte[] fdata, int fbank, int fstartaddr, boolean matching) {
        byte epclen = 0;
        if (fdata != null && fdata.length > 0) {
            // 荣睿锁定标签，只能以EPC作为过滤条件
            if (fbank != 1 || fstartaddr != 2) {
                Log.e("huang,UHFRManager", "Rr lock tag to unsupported fbank or fstartaddr");
                return -2;
            }
            epclen = (byte) (fdata.length / 2);
        }
        return rrlib.Kill_G2(epclen, fdata, killpasswd, new byte[1]);
    }

    public static List<com.handheld.uhfr.Reader.TEMPTAGINFO> measureYueHeTemp() {
        List<com.handheld.uhfr.Reader.TEMPTAGINFO> taginfos = null;
        List<ReaderHelp.EpcTemp> epcTemps = RrReader.rrlib.MeasureTemp(0, (byte) 0, new byte[0]);
        if (epcTemps != null && epcTemps.size() > 0) {
            taginfos = new ArrayList<>();
            for (ReaderHelp.EpcTemp epcTemp : epcTemps) {
                com.handheld.uhfr.Reader.TEMPTAGINFO temptaginfo = new com.handheld.uhfr.Reader.TEMPTAGINFO();
                if (epcTemp.EPC == null) {
                    epcTemp.EPC = "";
                }
                byte[] epcId = Tools.HexString2Bytes(epcTemp.EPC);
                temptaginfo.EpcId = epcId;
                temptaginfo.Epclen = (short) epcId.length;
                BigDecimal bigDecimal = new BigDecimal(epcTemp.temp);
                temptaginfo.Temperature = bigDecimal.setScale(2, RoundingMode.HALF_UP).doubleValue();
                taginfos.add(temptaginfo);
            }
        }
        return taginfos;
    }

}
