import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';
import 'package:flutter_uvc_camera/uvc_camera_view.dart';

void main() {
  runApp(const MyApp());
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
      home: const MyHomePage(title: 'Flutter UVC Camera Demo'),
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
  final cameraController = UVCCameraController();

  @override
  void initState() {
    super.initState();
    _incrementCounter();
  }

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
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const SizedBox(height: 40),
              TextButton(
                  onPressed: () {
                    cameraController.initializeCamera(width: 640, height: 480);
                    setState(() {});
                  },
                  child: Text('刷新摄像')),
              const SizedBox(height: 20),
              SizedBox(child: UVCCameraView(width: 300, height: 300)),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () {
                    cameraController.takePicture();
                    setState(() {});
                  },
                  child: Text('takePicture')),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () {
                    cameraController.writeToDevice(_counter);
                    _counter++;
                    setState(() {});
                  },
                  child: Text('writeToDevice')),
              TextButton(
                  onPressed: () {
                    cameraController.readFromDevice(_counter);
                    _counter++;
                    setState(() {});
                  },
                  child: Text('readFromDevice')),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () {
                    cameraController.getAllPreviewSize();
                    setState(() {});
                  },
                  child: Text('getAllPreviewSize')),
              const SizedBox(height: 20),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () {
                    cameraController.getDevicesList();
                    setState(() {});
                  },
                  child: Text('getDevicesList')),
              const SizedBox(height: 20),
              ...cameraController.getCallStrings.map((e) => Text(e)),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
