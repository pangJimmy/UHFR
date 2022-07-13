package com.handheld.uhfr;

public class Reader {

    public enum Lock_Obj {
        LOCK_OBJECT_KILL_PASSWORD(1),
        LOCK_OBJECT_ACCESS_PASSWD(2),
        LOCK_OBJECT_BANK1(4),
        LOCK_OBJECT_BANK2(8),
        LOCK_OBJECT_BANK3(16);

        int p_v;

        private Lock_Obj(int v) {
            this.p_v = v;
        }

        public int value() {
            return this.p_v;
        }
    }

    public enum Lock_Type {
        KILL_PASSWORD_UNLOCK(0),
        KILL_PASSWORD_LOCK(512),
        KILL_PASSWORD_PERM_LOCK(768),
        ACCESS_PASSWD_UNLOCK(0),
        ACCESS_PASSWD_LOCK(128),
        ACCESS_PASSWD_PERM_LOCK(192),
        BANK1_UNLOCK(0),
        BANK1_LOCK(32),
        BANK1_PERM_LOCK(48),
        BANK2_UNLOCK(0),
        BANK2_LOCK(8),
        BANK2_PERM_LOCK(12),
        BANK3_UNLOCK(0),
        BANK3_LOCK(2),
        BANK3_PERM_LOCK(3);

        int p_v;

        private Lock_Type(int v) {
            this.p_v = v;
        }

        public int value() {
            return this.p_v;
        }
    }

    public static enum READER_ERR {

        MT_OK_ERR(0),
        MT_IO_ERR(1),
        MT_INTERNAL_DEV_ERR(2),
        MT_CMD_FAILED_ERR(3),
        MT_CMD_NO_TAG_ERR(4),
        MT_M5E_FATAL_ERR(5),
        MT_OP_NOT_SUPPORTED(6),
        MT_INVALID_PARA(7),
        MT_INVALID_READER_HANDLE(8),
        MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS(9),
        MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET(10),
        MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS(11),
        MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE(12),
        MT_HARDWARE_ALERT_ERR_BY_READER_DOWN(13),
        MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR(14),
        M6E_INIT_FAILED(15),
        MT_OP_EXECING(16),
        MT_UNKNOWN_READER_TYPE(17),
        MT_OP_INVALID(18),
        MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE(19),
        MT_MAX_ERR_NUM(20),
        MT_MAX_INT_NUM(21),
        MT_TEST_DEV_FAULT_1(51),
        MT_TEST_DEV_FAULT_2(52),
        MT_TEST_DEV_FAULT_3(53),
        MT_TEST_DEV_FAULT_4(54),
        MT_TEST_DEV_FAULT_5(55),
        MT_UPDFWFROMSP_OPENFILE_FAILED(80),
        MT_UPDFWFROMSP_FILE_FORMAT_ERR(81),
        MT_JNI_INVALID_PARA(101),
        MT_OTHER_ERR(-268435457);

        private int value = 0;

        private READER_ERR(int value) {
            this.value = value;
        }

        public static READER_ERR valueOf(int value) {
            switch(value) {
                case 0:
                    return MT_OK_ERR;
                case 1:
                    return MT_IO_ERR;
                case 2:
                    return MT_INTERNAL_DEV_ERR;
                case 3:
                    return MT_CMD_FAILED_ERR;
                case 4:
                    return MT_CMD_NO_TAG_ERR;
                case 5:
                    return MT_M5E_FATAL_ERR;
                case 6:
                    return MT_OP_NOT_SUPPORTED;
                case 7:
                    return MT_INVALID_PARA;
                case 8:
                    return MT_INVALID_READER_HANDLE;
                case 9:
                    return MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS;
                case 10:
                    return MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET;
                case 11:
                    return MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS;
                case 12:
                    return MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE;
                case 13:
                    return MT_HARDWARE_ALERT_ERR_BY_READER_DOWN;
                case 14:
                    return MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR;
                case 15:
                    return M6E_INIT_FAILED;
                case 16:
                    return MT_OP_EXECING;
                case 17:
                    return MT_UNKNOWN_READER_TYPE;
                case 18:
                    return MT_OP_INVALID;
                case 19:
                    return MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE;
                case 20:
                    return MT_OTHER_ERR;
                case 21:
                    return MT_OTHER_ERR;
                case 51:
                    return MT_TEST_DEV_FAULT_1;
                case 52:
                    return MT_TEST_DEV_FAULT_2;
                case 53:
                    return MT_TEST_DEV_FAULT_3;
                case 54:
                    return MT_TEST_DEV_FAULT_4;
                case 55:
                    return MT_TEST_DEV_FAULT_5;
                case 80:
                    return MT_UPDFWFROMSP_OPENFILE_FAILED;
                case 81:
                    return MT_UPDFWFROMSP_FILE_FORMAT_ERR;
                case 101:
                    return MT_JNI_INVALID_PARA;
                default:
                    return MT_OTHER_ERR;
            }
        }

        public int value() {
            return this.value;
        }
    }

    public static enum Region_Conf {
        RG_PRC(0),
        RG_NA(1),
        RG_NONE(2),
        RG_KR(3),
        RG_EU(4),
        RG_EU2(5),
        RG_EU3(6);

//        RG_NONE(0),
//        RG_NA(1),
//        RG_EU(2),
//        RG_EU2(7),
//        RG_EU3(8),
//        RG_KR(3),
//        RG_PRC(6),
//        RG_PRC2(10),
//        RG_OPEN(255);
        //----
//        GB_920_925(0),
//        FCC_902_928(1),
//        ETSI_866_868(6);
        //----
//        GB_920_925(0),
//        GB_840_845(1),
//        GB_840_845_920_925(2),
//        FCC_902_928(3),
//        ETSI_866_868(4),
//        JP_916_8_920_4(5),
//        TW_922_25_927_75(6),
//        ID_923_125_925_125(7),
//        RUS_866_6_867_4(8),
//        TEST_802_75_998_75(9),
//        JP_LBT_916_8_920_8(10);
        int p_v;

        private Region_Conf(int v) {
            this.p_v = v;
        }

        public int value() {
            return this.p_v;
        }

//        public static Region_Conf valueOf(int value) {
//            switch (value) {
//                case 0:
//                    return GB_920_925;
//                case 1:
//                    return GB_840_845;
//                case 2:
//                    return GB_840_845_920_925;
//                case 3:
//                    return FCC_902_928;
//                case 4:
//                    return ETSI_866_868;
//                case 5:
//                    return JP_916_8_920_4;
//                case 6:
//                    return TW_922_25_927_75;
//                case 7:
//                    return ID_923_125_925_125;
//                case 8:
//                    return RUS_866_6_867_4;
//                case 9:
//                    return TEST_802_75_998_75;
//                case 10:
//                    return JP_LBT_916_8_920_8;
//                default:
//                    return null;
//            }
//        }

        public static Region_Conf valueOf(int value) {
            switch (value) {
                case 0:
                    return RG_PRC;
                case 1:
                    return RG_NA;
                case 2:
                    return RG_NONE;
                case 3:
                    return RG_KR;
                case 4:
                    return RG_EU;
                case 5:
                    return RG_EU2;
                case 6:
                    return RG_EU3;
                default:
                    return null;
            }
        }

    }

    public static enum SL_TagProtocol {
        SL_TAG_PROTOCOL_NONE(0),
        SL_TAG_PROTOCOL_ISO180006B(3),
        SL_TAG_PROTOCOL_GEN2(5),
        SL_TAG_PROTOCOL_ISO180006B_UCODE(6),
        SL_TAG_PROTOCOL_IPX64(7),
        SL_TAG_PROTOCOL_IPX256(8);

        int p_v;

        private SL_TagProtocol(int v) {
            this.p_v = v;
        }

        public int value() {
            return this.p_v;
        }

        public static SL_TagProtocol valueOf(int value) {
            switch(value) {
                case 0:
                    return SL_TAG_PROTOCOL_NONE;
                case 1:
                case 2:
                case 4:
                default:
                    return null;
                case 3:
                    return SL_TAG_PROTOCOL_ISO180006B;
                case 5:
                    return SL_TAG_PROTOCOL_GEN2;
                case 6:
                    return SL_TAG_PROTOCOL_ISO180006B_UCODE;
                case 7:
                    return SL_TAG_PROTOCOL_IPX64;
                case 8:
                    return SL_TAG_PROTOCOL_IPX256;
            }
        }
    }



    public static class TEMPTAGINFO implements Cloneable {

        public byte AntennaID; //标签被哪个天线读到
        public int Frequency; //标签是从哪个频点读到的
        public int TimeStamp; //标签读到的时间戳，单位毫秒（相对于命令发//出的时刻）
        public short EmbededDatalen; //嵌入数据的字节数--可作其它区存储
        public byte[] EmbededData; //嵌入数据
        public byte[] Res=new byte[2]; //保留,不是保留区
        public byte[] PC=new byte[2]; //PC段
        public byte[] CRC=new byte[2]; //CRC段
        public short Epclen; //EPC码长度，单位为字节
        public byte[] EpcId; //EPC码
        public int Phase;
        public Reader.SL_TagProtocol protocol; //标签协议
        public int ReadCnt; //标签被读到的次数
        public int RSSI; //接收到的标签的信号强度
        public double Temperature; //悦和/宜链温度
        public int index ;
        public int count ;

        public TEMPTAGINFO() {

        }

        @Override
        public Object clone() {
            TEMPTAGINFO o = null;
            try {
                o = (TEMPTAGINFO) super.clone();
                return o;
            } catch (CloneNotSupportedException var3) {
                return null;
            }
        }
    }

}
