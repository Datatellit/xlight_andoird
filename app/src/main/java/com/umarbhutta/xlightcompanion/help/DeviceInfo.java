package com.umarbhutta.xlightcompanion.help;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by 75932 on 2017/11/3.
 */

public class DeviceInfo {
    /*
    获取手机IMEI
     */
    public static String getIMEI(Context context) {
        TelephonyManager TelephonyMgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        return TelephonyMgr.getDeviceId();
    }

    /*
    获取网卡地址
     */
    public static String getWlanMAC(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wm.getConnectionInfo().getMacAddress();
    }

    /*
    获取手机蓝牙地址
     */
    public static String getBluetoothMAC() {
        BluetoothAdapter mBlueth = BluetoothAdapter.getDefaultAdapter();
        return mBlueth.getAddress();
    }

    public static String getSign(Context context) {
        String m_szLongID = "";
        String imei = getIMEI(context);
        String wlan = getWlanMAC(context);
        String bluetooth = getBluetoothMAC();
        if (imei == null) imei = "XLight";
        if (wlan == null) imei = "XLight";
        if (bluetooth == null) imei = "XLight";
        m_szLongID = imei + wlan + bluetooth;
        // compute md5
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        m.update(m_szLongID.getBytes(), 0, m_szLongID.length());
        // get md5 bytes
        byte p_md5Data[] = m.digest();
        // create a hex string
        String m_szUniqueID = new String();
        for (int i = 0; i < p_md5Data.length; i++) {
            int b = (0xFF & p_md5Data[i]);
            // if it is a single digit, make sure it have 0 in front (proper padding)
            if (b <= 0xF)
                m_szUniqueID += "0";
            // add number to string
            m_szUniqueID += Integer.toHexString(b);
        }   // hex string to uppercase
        m_szUniqueID = m_szUniqueID.toUpperCase();
        return m_szUniqueID;
    }
}