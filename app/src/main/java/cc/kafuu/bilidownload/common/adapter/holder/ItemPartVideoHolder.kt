package cc.kafuu.bilidownload.common.adapter.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.common.core.viewbinding.CoreRVHolder
import cc.kafuu.bilidownload.databinding.ItemPartVideoBinding

class ItemPartVideoHolder(parent: ViewGroup) : CoreRVHolder<ItemPartVideoBinding>(
    ItemPartVideoBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) {
    override fun getDataVariableId(): Int = BR.data

    override fun getVMVariableId(): Int = BR.viewModel
}