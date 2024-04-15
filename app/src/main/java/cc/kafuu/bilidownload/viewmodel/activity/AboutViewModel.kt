package cc.kafuu.bilidownload.viewmodel.activity

import android.util.Log
import cc.kafuu.bilidownload.common.core.CoreViewModel

class AboutViewModel: CoreViewModel() {
    companion object {
        private const val TAG = "AboutViewModel"
    }


    fun onClickInvitation() {
        Log.d(TAG, "你点击了: 参与内测")
    }

    fun onClickVersion() {
        Log.d(TAG, "你点击了: 当前版本")
    }

    fun onClickLicense() {
        Log.d(TAG, "你点击了: 开源组件许可")
    }

    fun onClickAboutUS() {
        Log.d(TAG, "你点击了: 关于我们")
    }

    fun onClickGrade() {
        Log.d(TAG, "你点击了: 给我们评分")
    }
}