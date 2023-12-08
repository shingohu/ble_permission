import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

_BLEPermission BLEPermission = _BLEPermission._();

class _BLEPermission {
  static const MethodChannel _channel = MethodChannel('ble_permission');

  ValueNotifier<bool?> bluetoothAdapterState = ValueNotifier(null);

  _BLEPermission._() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == "onBluetoothAdapterStateChanged") {
        bluetoothAdapterState.value = call.arguments;
      }
    });
  }

  ///打开蓝牙适配器
  ///android open by api
  ///iOS will  go to settings
  Future<bool> openBluetoothAdapter() async {
    return await _channel.invokeMethod("openBluetoothAdapter");
  }

  ///蓝牙适配器是否已开启
  Future<bool> get isBluetoothAdapterEnable async {
    bool open = await _channel.invokeMethod("isBluetoothAdapterEnable");
    return open;
  }

  ///判断权限以及相应的开关是否都有了
  Future<bool> isReady() async {
    return await _channel.invokeMethod("isReady");
  }

  ///GPS是否开启,android23-30版本蓝牙搜索的时候需要
  ///only Android
  Future<bool> get isLocationServiceEnable async {
    if (Platform.isAndroid) {
      return await _channel.invokeMethod("isLocationServiceEnable");
    }
    return true;
  }

  ///开启定位服务,android23-30版本蓝牙搜索的时候需要
  ///only Android
  Future<bool> openLocationService() async {
    if (Platform.isAndroid) {
      return await _channel.invokeMethod("openLocationService");
    }
    return true;
  }

  ///请求蓝牙相关权限(有则直接返回,没有则先请求)
  Future<bool> requestPermission() async {
    bool hasPermission =
        await _channel.invokeMethod<bool>("requestPermission") ?? false;
    return hasPermission;
  }

  ///检查蓝牙相关的权限
  Future<bool> checkPermission() async {
    return await _channel.invokeMethod<bool>("checkPermission") ?? false;
  }

  ///跳转到app设置页面,去打开权限
  ///iOS开关蓝牙权限会重启app
  Future<bool> openPermission() async {
    return await _channel.invokeMethod("openPermission");
  }
}
