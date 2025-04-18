package cc.kafuu.bilidownload.common.adapter

import android.content.Context
import android.view.ViewGroup
import cc.kafuu.bilidownload.common.adapter.holder.ItemLocalResourceHolder
import cc.kafuu.bilidownload.common.core.viewbinding.CoreRVAdapter
import cc.kafuu.bilidownload.common.core.viewbinding.CoreRVHolder
import cc.kafuu.bilidownload.feature.viewbinding.viewmodel.activity.HistoryDetailsViewModel

class LocalResourceRVAdapter(viewModel: HistoryDetailsViewModel, context: Context) :
    CoreRVAdapter<HistoryDetailsViewModel>(viewModel, context) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoreRVHolder<*> {
        return ItemLocalResourceHolder(parent)
    }
}