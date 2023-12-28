package com.chenyeju.flutter_uvc_camera

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine


class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        flutterEngine.plugins.add(MainPlugin())
        super.configureFlutterEngine(flutterEngine)

    }

}

