package com.umarbhutta.xlightcompanion.settings.utils;

//import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.umarbhutta.xlightcompanion.Tools.NetworkUtils;

public class BaseFragment extends  Fragment {
    private boolean isRegistered = false;
    private NetBroadcastReceiver netWorkChangReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册网络状态监听广播
        netWorkChangReceiver = new NetBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(netWorkChangReceiver, filter);
        isRegistered = true;
    }

    // 自定义接口
    public interface NetEvevt {
        public void onNetChange(int netMobile);
    }

    private NetEvevt mNetEvent = null;

    public void setNetEvent(NetEvevt event) {
        this.mNetEvent = event;
    }

    /**
     * 自定义检查手机网络状态是否切换的广播接受器
     *
     * @author cj
     */
    public class NetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            // 如果相等的话就说明网络状态发生了变化
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                // 接口回调传过去状态的类型
                if (mNetEvent != null) {
                    int netWorkState = NetworkUtils.getNetworkType(context);
                    mNetEvent.onNetChange(netWorkState);
                }
            }
        }
    }
}
