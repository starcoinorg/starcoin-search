package org.starcoin.utils;

import org.apache.commons.text.StringEscapeUtils;

public class ByteUtils {
    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    public static byte[] hexToByteArray(String inHex) {
        String tmp = inHex.substring(0, 2);
        if (tmp.equals("0x")) {
            inHex = inHex.substring(2);
        }
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    public static String unescapeEvent(String source) {
        String result = StringEscapeUtils.unescapeJson(source);
        result = result.replace("\"{\"struct", "{\"struct");
        result = result.replace("}}\"", "}}");
        return result;
    }
}
