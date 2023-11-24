import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class UsbService {
  static const platform = MethodChannel('com.chenyeju.flutter_uvc_camera/usb');

  static Future<void> connectToUsbDevice() async {
    try {
      final result = await platform.invokeMethod('connectToUsbDevice');
      print(result);
    } on PlatformException catch (e) {
      print("Failed to connect to USB device: '${e.message}'.");
    }
  }

  static Future<void> writeToDevice(List<int> data) async {
    try {
      final result = await platform.invokeMethod('writeToDevice', Uint8List.fromList(data));
      print(result);
    } on PlatformException catch (e) {
      print("Failed to write to USB device: '${e.message}'.");
    }
  }
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              'You have pushed the button this many times:',
            ),
            const SizedBox(height: 20),
            TextButton(
                onPressed: () {
                  try {
                    UsbService.connectToUsbDevice();
                  } catch (e) {
                    print(e);
                  }
                },
                child: Text('Get USB Devices')),
            const SizedBox(height: 20),
            TextButton(
                onPressed: () {
                  try {
                    UsbService.writeToDevice([54619, 0, 120, 16, 16, 4]);
                  } catch (e) {
                    print(e);
                  }
                },
                child: Text('Write to USB Devices')),
            const SizedBox(height: 20),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
