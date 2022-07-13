package com.handheld.uhfr;

import com.gg.reader.api.utils.BitBuffer;
import com.gg.reader.api.utils.HexUtils;

public class PcUtils {
    //写入EPC长度
    public static int getValueLen(String value) {
        int iLength = 0;
        if (value.length() % 4 == 0) {
            iLength = value.length() / 4;
        } else {
            iLength = value.length() / 4 + 1;
        }
        return iLength;
    }

    public static int getValueLen(byte[] value) {
        int iLength = 0;
        if (value.length % 2 == 0) {
            iLength = value.length / 2;
        } else {
            iLength = value.length / 2 + 1;
        }
        return iLength;
    }

    public static int getValueLen(int byteLen) {
        int iLength = 0;
        if (byteLen % 2 == 0) {
            iLength = byteLen / 2;
        } else {
            iLength = byteLen / 2 + 1;
        }
        return iLength;
    }

    //PC值
    public static String getPc(int pcLen) {
        int iPc = pcLen << 11;
        BitBuffer buffer = BitBuffer.allocateDynamic();
        buffer.put(iPc);
        buffer.position(16);
        byte[] bTmp = new byte[2];
        buffer.get(bTmp);
        return HexUtils.bytes2HexString(bTmp);
    }

    public static String padRight(String src, int len, char ch) {
        int diff = len - src.length();
        if (diff <= 0) {
            return src;
        }

        char[] chars = new char[len];
        System.arraycopy(src.toCharArray(), 0, chars, 0, src.length());
        for (int i = src.length(); i < len; i++) {
            chars[i] = ch;
        }
        return new String(chars);
    }

}
