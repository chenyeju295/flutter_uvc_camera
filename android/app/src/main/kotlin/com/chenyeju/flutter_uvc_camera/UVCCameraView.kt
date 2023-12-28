import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.chenyeju.flutter_uvc_camera.UVCPictureCallback
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.chenyeju.flutter_uvc_camera.usbvideo.IButtonCallback
import com.chenyeju.flutter_uvc_camera.usbvideo.USBCameraSDK
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

internal class UVCCameraView(
    private val mContext: Context,
    private val mChannel: MethodChannel, params: Any?) : PlatformView {
    private var mViewBinding = ActivityMainBinding.inflate(LayoutInflater.from(mContext))

    override fun getView(): View {
        return mViewBinding.root
    }

    override fun dispose() {

    }

    fun initCamera(arguments: Any?) {

    }

    fun takePicture(callback: UVCPictureCallback) {

    }

    fun getAllPreviewSize() {
    }

    fun getDevicesList() {
    }

    fun writeToDevice(i: Int) {

    }

    fun closeCamera() {
    }
}