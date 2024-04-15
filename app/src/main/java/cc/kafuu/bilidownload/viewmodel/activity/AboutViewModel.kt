package cc.kafuu.bilidownload.viewmodel.activity

import android.util.Log
import cc.kafuu.bilidownload.common.core.CoreViewModel
import cc.kafuu.bilidownload.model.AboutItemType

class AboutViewModel: CoreViewModel() {
    companion object {
        private const val TAG = "AboutViewModel"
    }
    fun itemOnClickListener(@AboutItemType position: Int) = when (position){

        AboutItemType.ITEM_GRADE -> {
            Log.d(TAG, "你点击了: 为我们评分")
        }

        AboutItemType.ITEM_INVITATION -> {
            Log.d(TAG, "你点击了: 参与内测")
        }

        AboutItemType.ITEM_LICENSE -> {
            Log.d(TAG, "你点击了: 开源组件许可")
        }

        AboutItemType.ITEM_US -> {
            Log.d(TAG, "你点击了: 关于我们")
        }

        AboutItemType.ITEM_VERSION -> {
            Log.d(TAG, "你点击了: 当前版本")
        }

        else -> {Log.d(TAG, "你点击了: 未知错误")}
    }
}