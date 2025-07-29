import 'package:ble_permission/ble_permission.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: PopScope(
        canPop: false,
        onPopInvokedWithResult: (_, __) async {},
        child: Scaffold(
          appBar: AppBar(
            title: const Text('Bluetooth Permission Demo'),
          ),
          body: Center(
            child: Column(
              children: [
                CupertinoButton(
                    child: Text("蓝牙适配器状态"),
                    onPressed: () async {
                      print(await BLEPermission.isBluetoothAdapterEnable);
                    }),
                CupertinoButton(
                    child: Text("打开蓝牙适配器"),
                    onPressed: () async {
                      print(await BLEPermission.openBluetoothAdapter());
                    }),
                CupertinoButton(
                    child: Text("检查蓝牙权限"),
                    onPressed: () async {
                      print(await BLEPermission.checkPermission());
                    }),
                CupertinoButton(
                    child: Text("请求蓝牙权限"),
                    onPressed: () async {
                      print(await BLEPermission.requestPermission());
                    }),
                CupertinoButton(
                    child: Text("跳转蓝牙设置"),
                    onPressed: () async {
                      print(await BLEPermission.openPermission());
                    })
              ],
            ),
          ),
        ),
      ),
    );
  }
}
