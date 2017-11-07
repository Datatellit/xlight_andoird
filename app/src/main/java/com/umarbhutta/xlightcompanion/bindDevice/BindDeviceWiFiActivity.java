package com.umarbhutta.xlightcompanion.bindDevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.umarbhutta.xlightcompanion.R;
import com.umarbhutta.xlightcompanion.SDK.CloudAccount;
import com.umarbhutta.xlightcompanion.SDK.xltDevice;
import com.umarbhutta.xlightcompanion.Tools.AndroidBug54971Workaround;
import com.umarbhutta.xlightcompanion.Tools.ToastUtil;
import com.umarbhutta.xlightcompanion.help.ClsUtils;
import com.umarbhutta.xlightcompanion.help.WifiAdmin;
import com.umarbhutta.xlightcompanion.main.SlidingMenuMainActivity;
import com.umarbhutta.xlightcompanion.okHttp.HttpUtils;
import com.umarbhutta.xlightcompanion.okHttp.model.CheckDeviceResult;
import com.umarbhutta.xlightcompanion.okHttp.model.ScanAP;
import com.umarbhutta.xlightcompanion.okHttp.model.ScanAPs;
import com.umarbhutta.xlightcompanion.okHttp.requests.imp.RequestCheckDevice;
import com.umarbhutta.xlightcompanion.settings.BaseActivity;
import com.umarbhutta.xlightcompanion.settings.utils.DisplayUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import io.particle.android.sdk.utils.Crypto;

/**
 * Created by 75932 on 2017/11/1.
 */

public class BindDeviceWiFiActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout llBack;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice bluetoothXlight;
    private Spinner mySpinner;
    private ArrayAdapter<String> adapter;
    private List<String> list = new ArrayList<String>();
    private List<ScanResult> wifiScanList;
    private List<ScanResult> wifiList24 = new ArrayList<ScanResult>();
    private ScanResult connectResult;
    private WifiAdmin wifiAdmin;
    private BluetoothInfo bluetoothInfo;
    private xltDevice _xltDevice = null;
    private TextView etPassword = null;
    private TextView etSSID = null;
    private String coreID = null;
    final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    private int step = 0;
    private WifiReceiver mWifiReceiver;
    private final int WIFI_PERMISSION_REQ_CODE = 100;
    private int type = 0;
    private ScanAPs aps;
    private ScanAP selectAp;
    final java.util.Timer timer = new java.util.Timer(true);


    public class BluetoothInfo {
        String name;
        String address;

        public BluetoothInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    public class WifiInfo {
        String ssid;
        String password;
        String capabilities = "WPA2";

        public WifiInfo(String ssid, String password, String capabilities) {
            this.ssid = ssid;
            this.password = password;
            this.capabilities = capabilities;
        }

        public WifiInfo(String ssid, String password) {
            this.ssid = ssid;
            this.password = password;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_bind_device_wifi);

        AndroidBug54971Workaround.assistActivity(findViewById(android.R.id.content));
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= 20) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bar_color));
        }

        RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        ViewGroup.LayoutParams params = rootLayout.getLayoutParams();
        params.height = DisplayUtils.getScreenHeight(this) - 100;
        rootLayout.setLayoutParams(params);

        llBack = (LinearLayout) findViewById(R.id.ll_back);
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView btnSure = (TextView) findViewById(R.id.tvEditSure);
        btnSure.setVisibility(View.INVISIBLE);

        TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvTitle.setText(R.string.find_devices);

        etPassword = (TextView) findViewById(R.id.etPassword);
        etSSID = (TextView) findViewById(R.id.etWifi);

        type = getIntent().getIntExtra("type", 0);
        mySpinner = (Spinner) findViewById(R.id.spinner);
        list.add(getResources().getString(R.string.add_device_wifi_scan));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);
        mySpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                String selectWifi = adapter.getItem(arg2);
                if (type == 1) {
                    if (aps != null && aps.scans.size() > 0) {
                        //找到这个Wifi
                        for (ScanAP ap : aps.scans) {
                            if (ap.ssid.equals(selectWifi)) {
                                selectAp = ap;
                                break;
                            }
                        }
                    }
                } else {
                    if (wifiScanList != null && wifiScanList.size() > 0) {
                        //找到这个Wifi
                        for (ScanResult scanResult : wifiScanList) {
                            if (scanResult.SSID.equals(selectWifi)) {
                                connectResult = scanResult;
                                break;
                            }
                        }
                    }
                }
                /* 将mySpinner 显示*/
                arg0.setVisibility(View.VISIBLE);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                ((EditText) findViewById(R.id.etPassword)).setText("NONE");
                arg0.setVisibility(View.VISIBLE);
            }
        });
        wifiAdmin = new WifiAdmin(this);
        if (type == 0) {
            // 获取xlight蓝牙信息
            Bundle bundle = getIntent().getBundleExtra("bundle");
            bluetoothInfo = new BluetoothInfo(bundle.getString("name"), bundle.getString("mac"));
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // 配对蓝牙设备
            bluetoothXlight = mBluetoothAdapter.getRemoteDevice(bluetoothInfo.address);
            Log.d("XLight", "open wifi");
            wifiAdmin.openWifi();
            Log.d("XLight", "start scan wifi");
            //wifiAdmin.startScan(mReceiver);
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

            mWifiReceiver = new WifiReceiver();
            registerReceiver(mWifiReceiver, filter);
            getWifiList();
            updateWifiHandler.sendEmptyMessageDelayed(1, 5000);
            ReceiverBluetooth();
        } else {
            HttpUtils.getInstance().getRequestInfo("http://192.168.0.1/device-id", null, new HttpUtils.OnHttpRequestCallBack() {
                @Override
                public void onHttpRequestSuccess(Object result) {
                    try {
                        JSONObject object = new JSONObject(result.toString());
                        coreID = object.getString("id");
                        JSONObject jb = new JSONObject();
                        jb.put("k", "cc");
                        jb.put("v", "AYKZwDUEe8ZoFL+CoujbjRa/6h1h8kmbN3roGvnFpTW/5EThZThcQ4z7o7sVZKk");
                        HttpUtils.getInstance().postRequestInfoByForm("http://192.168.0.1/set", jb.toString(), null, new HttpUtils.OnHttpRequestCallBack() {
                            @Override
                            public void onHttpRequestSuccess(Object result) {
                                Log.d("XLight", "set:" + result.toString());
                            }

                            @Override
                            public void onHttpRequestFail(int code, String errMsg) {

                            }
                        });
                    } catch (Exception e) {
                        Log.e("XLight", e.getMessage(), e);
                    }
                }

                @Override
                public void onHttpRequestFail(int code, String errMsg) {

                }
            });
            //ap模式，进行获取
            HttpUtils.getInstance().getRequestInfo("http://192.168.0.1/scan-ap", ScanAPs.class, new HttpUtils.OnHttpRequestCallBack() {
                @Override
                public void onHttpRequestSuccess(Object result) {
                    try {
                        aps = (ScanAPs) result;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (aps != null & aps.scans.size() > 0)
                                    list.clear();
                                // 显示Wifi信息
                                for (ScanAP ap : aps.scans)
                                    list.add(ap.ssid);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    } catch (Exception e) {
                        Log.e("XLight", e.getMessage(), e);
                    }
                }

                @Override
                public void onHttpRequestFail(int code, String errMsg) {
                }
            });
        }
    }

    final Handler timeoutHandler = new Handler() {          // handle
        public void handleMessage(Message msg) {
            Intent intent;
            switch (msg.what) {
                case 1:
                    // 跳转到下一步
                    myThread = false;
                    Log.e("XLight", "Connect BLE timeout,Redirect to ErrorActivity");
                    intent = new Intent(getApplicationContext(), BindDeviceErrorActivity.class);
                    intent.putExtra("type", type);
                    startActivity(intent);
                    finish();
                    break;
                case 2:
                    myThread = false;
                    ToastUtil.showToast(BindDeviceWiFiActivity.this, R.string.add_device_wifi_step7);
                    intent = new Intent(getApplication(), BindDeviceSuccessActivity.class);
                    intent.putExtra("coreID", coreID);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private int connectTimeout = 60;
    private boolean myThread = true;

    public class MyThread implements Runnable {      // thread
        @Override
        public void run() {
            while (connectTimeout > 0) {
                try {
                    if (!myThread) {
                        break;
                    }
                    connectTimeout--;
                    if (connectTimeout > 1)
                        Thread.sleep(1000);     // sleep 1000ms
                } catch (Exception e) {
                }
            }
            // 超时
            if (connectTimeout == 0) {
                Message message = new Message();
                message.what = 1;
                timeoutHandler.sendMessage(message);
            }
        }
    }

    public class DelayThread implements Runnable {      // thread
        @Override
        public void run() {
            try {
                Thread.sleep(32000);
                new Thread(new CheckStateThread()).start();
            } catch (Exception e) {

            }
        }
    }

    public class CheckStateThread implements Runnable {      // thread
        @Override
        public void run() {
            while (connectTimeout > 0) {
                try {
                    if (!myThread) {
                        break;
                    }
                    RequestCheckDevice.getInstance().checkDevice(getApplicationContext(), coreID, new RequestCheckDevice.OnAddDeviceCallBack() {
                        @Override
                        public void mOnAddDeviceCallBackFail(int code, String errMsg) {
                        }

                        @Override
                        public void mOnAddDeviceCallBackSuccess(CheckDeviceResult device) {
                            //判断状态，并传递
                            if (device.id != null && device.connected) {
                                if (connectTimeout > 0) {
                                    Message message = new Message();
                                    message.what = 2;
                                    timeoutHandler.sendMessage(message);
                                }
                            }
                        }
                    });
                    if (connectTimeout > 1)
                        Thread.sleep(2000);     // sleep 1000ms
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            connectTimeout = 0;
            myThread = false;
            ToastUtil.dismissLoading();
            if (type == 0) {
                Log.d("XLight", "Destroy WiFiActivity");
                stopScanWifi = true;
                updateWifiHandler.removeCallbacks(runnable);
                unBindReceiver();
                //wifiAdmin.unRegisterReceiver(mReceiver);
                unregisterReceiver(mWifiReceiver);
                //取消蓝牙配对
                ClsUtils.removeBond(bluetoothXlight.getClass(), bluetoothXlight);
                if (_xltDevice != null)
                    _xltDevice.Disconnect();
            } else if (type == 1) {
                timer.cancel();
            }
        } catch (Exception ex) {
            Log.e("XLight", ex.getMessage(), ex);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void ChangeClick(View v) {
        if (((CheckBox) findViewById(R.id.chkHide)).isChecked()) {
            // 显示
            ((TextView) findViewById(R.id.etWifi)).setVisibility(View.VISIBLE);
            // 隐藏
            ((Spinner) findViewById(R.id.spinner)).setVisibility(View.GONE);
        } else {
            // 显示
            ((TextView) findViewById(R.id.etWifi)).setVisibility(View.GONE);
            // 隐藏
            ((Spinner) findViewById(R.id.spinner)).setVisibility(View.VISIBLE);
        }
    }

    public void ConnectClick(View v) {
        try {
            //验证输入的密码
            if (!etPassword.getText().toString().equals("") && etPassword.getText().toString().length() < 8) {
                ToastUtil.showToast(this, R.string.add_device_wifi_check);
                return;
            }
            if (type == 1) {
                //开始计时
                new Thread(new MyThread()).start();
                ConnectWLAN();
                return;
            }
            if (bluetoothXlight.getBondState() == BluetoothDevice.BOND_NONE) {
                //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                Log.d("XLight", "开始配对");
                new Thread(new MyThread()).start();
                ToastUtil.showLoading(this, null, getResources().getString(R.string.add_device_wifi_step1));
                ClsUtils.pair(bluetoothXlight.getAddress(), "1234");
            } else if (bluetoothXlight.getBondState() == BluetoothDevice.BOND_BONDED) {
                ClsUtils.removeBond(bluetoothXlight.getClass(), bluetoothXlight);
                ConnectClick(null);
            }
        } catch (Exception e) {

        }
    }

    public void ConnectWLAN() {
        ToastUtil.showLoading(this, null, getString(R.string.add_device_wifi_step2));
        //获取public_key
        HttpUtils.getInstance().getRequestInfo("http://192.168.0.1/public-key", null, new HttpUtils.OnHttpRequestCallBack() {
            @Override
            public void onHttpRequestSuccess(Object result) {
                try {
                    Log.d("XLight", "Public-key:" + result.toString());
                    JSONObject jsonObject = new JSONObject(result.toString());
                    Log.d("XLight", "Public-key:" + jsonObject.getString("b"));
                    PublicKey publicKey = Crypto.readPublicKeyFromHexEncodedDerString(jsonObject.getString("b"));
                    //获取wifi信息
                    WifiInfo wifiInfo = getWifiInfo();
                    final ScanAP configAp;
                    if (wifiInfo == null) {
                        configAp = selectAp == null ? aps.scans.get(0) : selectAp;
                        configAp.pwd = ((EditText) findViewById(R.id.etPassword)).toString().trim();
                    } else {
                        //隐藏的
                        configAp = new ScanAP();
                        configAp.ssid = wifiInfo.ssid;
                        configAp.pwd = wifiInfo.password;
                    }
                    JSONObject submitData = new JSONObject();
                    submitData.put("idx", configAp.idx);
                    submitData.put("ssid", configAp.ssid);
                    submitData.put("ch", configAp.ch);
                    submitData.put("sec", configAp.sec);
                    if (!configAp.pwd.equals("")) {
                        configAp.pwd = Crypto.encryptAndEncodeToHex(configAp.pwd, publicKey);
                        submitData.put("pwd", configAp.pwd);
                    }
                    Log.d("XLight", submitData.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getString(R.string.add_device_wifi_step4));
                        }
                    });
                    //提交请求
                    HttpUtils.getInstance().postRequestInfoByForm("http://192.168.0.1/configure-ap", submitData.toString(), null, new HttpUtils.OnHttpRequestCallBack() {
                        @Override
                        public void onHttpRequestSuccess(Object result) {
                            Log.d("XLight", "configure-ap:" + result.toString());
                            //获取请求connect-ap
                            try {
                                JSONObject jb = new JSONObject();
                                jb.put("idx", 0);
                                HttpUtils.getInstance().postRequestInfoByForm("http://192.168.0.1/connect-ap", jb.toString(), null, new HttpUtils.OnHttpRequestCallBack() {
                                    @Override
                                    public void onHttpRequestSuccess(Object result) {
                                        //result
                                        Log.d("XLight", "connect-ap:" + result.toString());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getString(R.string.add_device_two_title2));
                                            }
                                        });
                                        TimerTask task = new TimerTask() {
                                            @Override
                                            public void run() {
                                                //每次需要执行的代码放到这里面。
                                                if (ping()) {
                                                    timer.cancel();
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getString(R.string.add_device_wifi_step6));
                                                        }
                                                    });
                                                    StartCheckDevice();
                                                }
                                            }
                                        };
                                        timer.schedule(task, 2000, 2000);
                                    }

                                    @Override
                                    public void onHttpRequestFail(int code, String errMsg) {

                                    }
                                });
                            } catch (Exception e) {
                                Log.e("XLight", e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onHttpRequestFail(int code, String errMsg) {

                        }
                    });
                } catch (Exception e) {

                }
            }

            @Override
            public void onHttpRequestFail(int code, String errMsg) {

            }
        });
    }

    boolean once = true;

    public void initXltDevice() {
        Log.d("XLight", "Init Bluetooth xltDevice");
        _xltDevice = new xltDevice();
        _xltDevice.setBridgePriority(xltDevice.BridgeType.BLE, 99);
        _xltDevice.useBridge(xltDevice.BridgeType.BLE);
        _xltDevice.Init(this);
        if (_xltDevice.isBLEOK()) {
            Log.d("XLight", "ble not need connect");
            clearWiFi();
        } else {
            Log.d("XLight", "ble connect start");
            _xltDevice.ConnectBLE(m_bcsHandler, new xltDevice.callbackConnect() {
                @Override
                public void onConnected(xltDevice.BridgeType bridge, boolean connected) {
                    Log.d("XLight", "connected callback");
                    if (once) {
                        once = false;
                        clearWiFi();
                    }
                }
            }, bluetoothXlight);
        }
    }

    public void TestWiFi() {
        WifiInfo wifiInfo = getWifiInfo();
        wifiAdmin.openWifi();
        WifiConfiguration tempConfig = wifiAdmin.IsExsits(wifiInfo.ssid);
        if (tempConfig != null && tempConfig.networkId != -1) {
            Log.d("XLight", "remove ssid at:" + tempConfig.networkId);
            wifiAdmin.RemoveSSID(tempConfig.networkId);
        }
        int type = 1;
        if (wifiInfo.capabilities.toUpperCase().contains("WPA")) {
            type = 3;
        } else if (wifiInfo.capabilities.toUpperCase().contains("WEP")) {
            type = 2;
        }
        wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(wifiInfo.ssid, wifiInfo.password, type));
    }

    Handler m_bcsHandler = new Handler() {
        public void handleMessage(Message msg) {
            String bridgeName = (String) msg.obj;
            Log.d("XLight", "bridgeName->" + bridgeName);
            switch (msg.what) {
                case xltDevice.BCS_CONNECTED:
                    Log.d("XLight", "Bluetooth device connected");
                    break;
                case xltDevice.BCS_NOT_CONNECTED:
                case xltDevice.BCS_CONNECTION_FAILED:
                case xltDevice.BCS_CONNECTION_LOST:
                    break;
                case xltDevice.BCS_FUNCTION_ACK:
                    Log.d("XLight", msg.arg1 == 1 ? "OK" : "Failed");
                    if (step == 1) {
                        WifiInfo wifiInfo = getWifiInfo();
                        Log.d("XLight", String.format("设置当前新的Wi-Fi信息,%s,%s", wifiInfo.ssid, wifiInfo.password));
                        if (wifiInfo.capabilities.toUpperCase().contains("WPA2")) {
                            //做一点事
                            _xltDevice.sysWiFiSetup(wifiInfo.ssid, wifiInfo.password, xltDevice.WLAN_SEC_WPA2);
                        } else if (wifiInfo.capabilities.toUpperCase().contains("WPA")) {
                            //做一点事
                            _xltDevice.sysWiFiSetup(wifiInfo.ssid, wifiInfo.password, xltDevice.WLAN_SEC_WPA);
                        } else if (wifiInfo.capabilities.toUpperCase().contains("WEP")) {
                            //做一点事
                            _xltDevice.sysWiFiSetup(wifiInfo.ssid, wifiInfo.password, xltDevice.WLAN_SEC_WEP);
                        } else {
                            _xltDevice.sysWiFiSetup(wifiInfo.ssid, wifiInfo.password, xltDevice.WLAN_SEC_UNSEC);
                        }
                        ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getResources().getString(R.string.add_device_wifi_step4));
                        step++;
                    } else if (step == 2) {
                        //Log.d("XLight", "ControllerID:" + _xltDevice.sysQueryCoreID());
                        //保存设备CoreID
                        if (bridgeName.split(":").length == 3)
                            coreID = bridgeName.split(":")[2];
                        ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getResources().getString(R.string.add_device_wifi_step5));
                        Log.d("XLight", "重启控制器");
                        _xltDevice.sysControl("reset");
                        step++;
                    } else if (step == 3) {
                        Log.d("XLight", "响应->重启控制器");
                        //开始检测状态
                        ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getResources().getString(R.string.add_device_wifi_step6));
                        //先检测一次当前状态
                        StartCheckDevice();
                    }
                    break;
                case xltDevice.BCS_FUNCTION_COREID:
                    Log.d("XLight", "CoreID: " + bridgeName);
                    break;
            }
        }
    };

    public void StartCheckDevice() {
        RequestCheckDevice.getInstance().checkDevice(getApplicationContext(), coreID, new RequestCheckDevice.OnAddDeviceCallBack() {
            @Override
            public void mOnAddDeviceCallBackFail(int code, String errMsg) {

            }

            @Override
            public void mOnAddDeviceCallBackSuccess(CheckDeviceResult device) {
                // 判断是否启动延时
                if (device.id != null && device.connected) {
                    //重置超时时间，并启动延时状态检查
                    connectTimeout = 60;
                    new Thread(new DelayThread()).start();
                } else {
                    new Thread(new CheckStateThread()).start();
                }
            }
        });
    }

    /*********WiFi相关*********/
    public void clearWiFi() {
        Log.d("XLight", "蓝牙可用:" + _xltDevice.isBLEOK());
        ToastUtil.showLoading(this, null, getResources().getString(R.string.add_device_wifi_step3));
        _xltDevice.sysControl("clear credentials");
        Log.d("XLight", "清除当前Wi-Fi信息:" + _xltDevice.isBLEOK());
        step++;
    }

    public WifiInfo getWifiInfo() {
        if (((CheckBox) findViewById(R.id.chkHide)).isChecked()) {
            return new WifiInfo(etSSID.getText().toString(), etPassword.getText().toString());
        } else {
            if (type == 1)
                return null;
            if (connectResult == null) {
                connectResult = wifiList24.get(0);
            }
            return new WifiInfo(connectResult.SSID, etPassword.getText().toString(), connectResult.capabilities);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WIFI_PERMISSION_REQ_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {// 允许
                    getWifiList();
                    updateWifiHandler.sendEmptyMessageDelayed(1, 5000);
                } else { // 不允许
                    ToastUtil.showToast(this, R.string.please_open_wifi);
                }
                break;
        }
    }

    /**
     * wifi列表
     */
    private void getWifiList() {
        if (isWifiContect()) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Log.d("XLight", "get scan wifi result");
            //获取结果
            wifiScanList = wifiManager.getScanResults();
            if (wifiScanList.size() > 0) {
                list.clear();
                wifiList24.clear();
            }
            //处理结果
            for (ScanResult scanResult : wifiScanList) {
                try {
                    // 把每个数据当作一对象添加到数组里
                    //Log.d("XLight", "SSID:" + scanResult.SSID + ",frequency:" + scanResult.frequency);
                    // 去除5g和名称为空的SSID
                    if (scanResult.SSID.equals("") || (scanResult.frequency > 4900 && scanResult.frequency < 5900))
                        continue;
                    wifiList24.add(scanResult);
                    list.add(scanResult.SSID);
                } catch (Exception ex) {
                    Log.e("XLight", ex.getMessage());
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                //获取当前的wifi状态int类型数据
                int mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (mWifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        //已打开
                        getWifiList();
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        //打开中
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        //已关闭
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        //关闭中
                        break;
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        //未知
                        break;
                }
            } else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                Log.d("XLight", "Get WiFi Result:" + linkWifiResult);
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                    ToastUtil.showToast(getApplicationContext(), "密码错误");
                }
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {//wifi连接上与否
                Log.d("XLight", "网络状态改变");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    Log.d("XLight", "wifi网络连接断开");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //获取当前wifi名称
                    Log.d("XLight", "连接到网络 " + wifiInfo.getSSID());
                }
            }
        }
    }

    /**
     * wifi是否打开
     *
     * @return
     */
    private boolean isWifiContect() {
        WifiManager wifimanager;
        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifimanager.isWifiEnabled()) {
            return true;
        }
        return false;
    }

    private boolean stopScanWifi = false;

    Handler updateWifiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!stopScanWifi) {
                updateWifiHandler.postDelayed(runnable, 100);
                updateWifiHandler.sendEmptyMessageDelayed(1, 5000);
            }
        }
    };

    public boolean ping() {
        String result = null;
        try {
            String ip = "www.baidu.com";// ping 的地址，可以换成任何一种可靠的外网
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);// ping网址3次
            // 读取ping的内容，可以不加
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }
            Log.d("------ping-----", "result content : " + stringBuffer.toString());
            // ping的状态
            int status = p.waitFor();
            if (status == 0) {
                result = "success";
                return true;
            } else {
                result = "failed";
            }
        } catch (IOException e) {
            result = "IOException";
        } catch (InterruptedException e) {
            result = "InterruptedException";
        } finally {
            Log.d("----result---", "result = " + result);
        }
        return false;
    }

    /**
     * 定时更新wifi列表
     */
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            getWifiList();
        }
    };

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // 该扫描已成功完成。
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d("XLight", "get scan wifi result");
                //获取结果
                wifiScanList = wifiAdmin.getWifiList();
                Log.d("XLight", wifiAdmin.lookUpScan().toString());
                if (wifiScanList.size() > 0) {
                    list.clear();
                    wifiList24.clear();
                }
                //处理结果
                for (ScanResult scanResult : wifiScanList) {
                    try {
                        // 把每个数据当作一对象添加到数组里
                        Log.d("XLight", "SSID:" + scanResult.SSID + ",frequency:" + scanResult.frequency);
                        // 去除5g和名称为空的SSID
                        if (scanResult.SSID.equals("") || (scanResult.frequency > 4900 && scanResult.frequency < 5900))
                            continue;
                        wifiList24.add(scanResult);
                        list.add(scanResult.SSID);
                    } catch (Exception ex) {
                        Log.e("XLight", ex.getMessage());
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }
    };

    /*******蓝牙相关*********/

    public void ReceiverBluetooth() {
        IntentFilter filter = new IntentFilter();
        //发现设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //设备连接状态改变
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(ACTION_PAIRING_REQUEST);
        filter.setPriority(1000);
        //蓝牙设备状态改变
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mBluetoothReceiver, filter);
    }

    public void unBindReceiver() {
        try {
            this.unregisterReceiver(mBluetoothReceiver);
        } catch (Exception e) {
            Log.e("XLight", e.getMessage());
        }
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("XLight", "mBluetoothReceiver action =" + action);
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.d("XLight", "正在配对......");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d("XLight", "完成配对");
                        //进行下一步动作
                        initXltDevice();
                        ToastUtil.showLoading(BindDeviceWiFiActivity.this, null, getResources().getString(R.string.add_device_wifi_step2));
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.d("XLight", "取消配对，仅是取消对话框");
                    default:
                        break;
                }
            } else if (intent.getAction().equals(ACTION_PAIRING_REQUEST)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    abortBroadcast();
                    ClsUtils.setPin(device.getClass(), device, "1234"); // 手机和蓝牙采集器配对
                    //ClsUtils.createBond(device.getClass(), device);
                    //ClsUtils.cancelPairingUserInput(device.getClass(), device);
                    Log.d("XLight", "setPin is success ");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                }
            }
        }
    };

    @Override
    public void onClick(View v) {

    }
}