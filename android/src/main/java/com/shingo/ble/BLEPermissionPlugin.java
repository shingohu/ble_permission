package com.shingo.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * BLEPermissionPlugin
 */
public class BLEPermissionPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {
    private MethodChannel channel;

    private Context mContext;


    private Activity mActivity;
    ActivityPluginBinding activityBinding;


    Map<Integer, PermissionCallback> permissionCallbackMap = new HashMap<>();

    ///定位服务开启回调
    Map<Integer, PermissionCallback> openLocationServiceCallbackMap = new HashMap<>();
    ///蓝牙适配器开启
    Map<Integer, PermissionCallback> openBluetoothAdapterCallbackMap = new HashMap<>();
    Map<Integer, PermissionCallback> openPermissionCallbackMap = new HashMap<>();


    private static final String BLE_STATE_OFF = "android.bluetooth.BluetoothAdapter.STATE_OFF";
    private static final String BLE_STATE_ON = "android.bluetooth.BluetoothAdapter.STATE_ON";

    private BroadcastReceiver bluetoothStateReceiver;

    /**
     * 注册
     *
     * @param context
     */
    public void registerBluetoothState(Context context) {

        if (bluetoothStateReceiver == null) {
            bluetoothStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (channel == null) {
                        return;
                    }
                    int BLEState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (BLEState) {
                        case BluetoothAdapter.STATE_ON:
                            // 蓝牙已经打开
                            channel.invokeMethod("onBluetoothAdapterStateChanged", true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            // 蓝牙正在关闭
                            channel.invokeMethod("onBluetoothAdapterStateChanged", false);
                            break;
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.setPriority(Integer.MAX_VALUE);
            // 监视蓝牙关闭和打开的状态
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BLE_STATE_OFF);
            filter.addAction(BLE_STATE_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(bluetoothStateReceiver, filter);
            }
        }

    }

    /**
     * 注销
     *
     * @param context
     */
    public void unregisterBluetoothState(Context context) {
        if (bluetoothStateReceiver != null) {
            context.unregisterReceiver(bluetoothStateReceiver);
            bluetoothStateReceiver = null;
        }
    }


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ble_permission");
        channel.setMethodCallHandler(this);
        mContext = flutterPluginBinding.getApplicationContext();
        registerBluetoothState(mContext);
    }


    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        String method = call.method;
        if ("openBluetoothAdapter".equals(method)) {
            openBluetoothAdapter(result);
        } else if ("isBluetoothAdapterEnable".equals(method)) {
            result.success(isBluetoothAdapterOpen());
        } else if ("isLocationServiceEnable".equals(method)) {
            result.success(isLocationServiceEnable(mContext));
        } else if ("openLocationService".equals(method)) {
            openLocationService(result);
        } else if ("openPermission".equals(method)) {
            openPermission(result);
        } else if ("requestPermission".equals(method)) {
            requestPermission(result);
        } else if ("checkPermission".equals(method)) {
            result.success(checkBluetoothPermission());
        } else if ("isReady".equals(method)) {
            boolean hasPermission = checkBluetoothPermission();
            boolean adapterOpen = isBluetoothAdapterOpen();
            if (hasPermission && adapterOpen) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    result.success(isLocationServiceEnable(mContext));
                    return;
                }
                result.success(true);
            } else {
                result.success(false);
            }


        }
    }


    public void requestPermission(Result result) {

        PermissionCallback callback = new PermissionCallback() {
            @Override
            void onPermission(boolean hasPermission) {
                result.success(hasPermission);
                permissionCallbackMap.remove(result.hashCode());
            }
        };
        permissionCallbackMap.put(result.hashCode(), callback);
        if (checkBluetoothPermission()) {
            callback.onPermission(true);
        } else {
            if (mActivity != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    //targetSdkVersion 31 更改了蓝牙相关权限
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, result.hashCode());
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //targetSdkVersion 28以及以下使用模糊定位权限即可,但是如果是29以及以上要使用精准定位权限,否则无法搜索到蓝牙
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, result.hashCode());
                    } else {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, result.hashCode());
                    }

                }
            }
        }
    }


    public boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int p1 = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN);
            int p2 = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT);
            return p1 == PackageManager.PERMISSION_GRANTED && p2 == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int p = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION);
            return p == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int p1 = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION);
            int p2 = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
            return p1 == PackageManager.PERMISSION_GRANTED || p2 == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }


    ///打开蓝牙适配器
    private void openBluetoothAdapter(Result result) {
        PermissionCallback callback = new PermissionCallback() {
            @Override
            void onPermission(boolean hasPermission) {
                result.success(hasPermission);
                openBluetoothAdapterCallbackMap.remove(result.hashCode());
            }
        };
        openBluetoothAdapterCallbackMap.put(result.hashCode(), callback);
        if (isBluetoothAdapterOpen()) {
            callback.onPermission(true);
        } else {
            if (mActivity != null) {
                Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btIntent.putExtra(Intent.EXTRA_PACKAGE_NAME, mActivity.getPackageName());
                mActivity.startActivityForResult(btIntent, result.hashCode());
            } else {
                callback.onPermission(false);
            }
        }

    }

    private void openPermission(Result result) {
        if (mActivity != null) {
            PermissionCallback callback = new PermissionCallback() {
                @Override
                void onPermission(boolean hasPermission) {
                    result.success(hasPermission);
                    openPermissionCallbackMap.remove(result.hashCode());
                }
            };
            openPermissionCallbackMap.put(result.hashCode(), callback);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", this.mActivity.getPackageName(), null));
            this.mActivity.startActivityForResult(intent, result.hashCode());
        } else {
            result.success(false);
        }
    }

    public boolean isBluetoothAdapterOpen() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }


    public boolean isLocationServiceEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return gpsProvider;
    }


    ///打开定位服务,android 12以下蓝牙权限需要
    public void openLocationService(Result result) {
        PermissionCallback callback = new PermissionCallback() {
            @Override
            void onPermission(boolean hasPermission) {
                result.success(hasPermission);
                openLocationServiceCallbackMap.remove(result.hashCode());
            }
        };
        openLocationServiceCallbackMap.put(result.hashCode(), callback);
        if (isLocationServiceEnable(mContext)) {
            callback.onPermission(true);
        } else {
            if (mActivity != null) {
                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mActivity.startActivityForResult(locationIntent, result.hashCode());
            } else {
                callback.onPermission(false);
            }
        }
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
        unregisterBluetoothState(binding.getApplicationContext());
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityBinding = binding;
        mActivity = binding.getActivity();
        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        if (activityBinding != null) {
            activityBinding.removeRequestPermissionsResultListener(this);
            activityBinding.removeActivityResultListener(this);
            activityBinding = null;
            mActivity = null;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (openLocationServiceCallbackMap.containsKey(requestCode)) {
            openLocationServiceCallbackMap.get(requestCode).onPermission(isLocationServiceEnable(mContext));
            return true;
        }
        if (openBluetoothAdapterCallbackMap.containsKey(requestCode)) {
            openBluetoothAdapterCallbackMap.get(requestCode).onPermission(isBluetoothAdapterOpen());
            return true;
        }
        if (openPermissionCallbackMap.containsKey(requestCode)) {
            openPermissionCallbackMap.get(requestCode).onPermission(checkBluetoothPermission());
            return true;
        }

        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionCallbackMap.containsKey(requestCode)) {
            if (grantResults.length > 0)
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionCallbackMap.get(requestCode).onPermission(true);
                    return true;
                } else {
                    permissionCallbackMap.get(requestCode).onPermission(false);
                }
        }
        return false;
    }


    abstract class PermissionCallback {
        abstract void onPermission(boolean hasPermission);
    }
}
