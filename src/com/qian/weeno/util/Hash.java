package com.qian.weeno.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

	public static String md5sum(String toEncode) {
        return md5sum(toEncode.getBytes());
    }
	
	public static String md5sum(byte[] toEncode) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(toEncode);
            return hexEncode(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
 
    public static String hexEncode(byte[] toEncode) {
        StringBuilder sb = new StringBuilder(toEncode.length * 2);
        for(byte b: toEncode){
            sb.append(Integer.toHexString((b & 0xf0) >>> 4));
            sb.append(Integer.toHexString(b & 0x0f));
        }
        return sb.toString();
    }
}
