package cc.kafuu.bilidownload.common.core.viewbinding

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class CoreRVHolder<V: ViewDataBinding>(var binding: V) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun getDataVariableId(): Int
    abstract fun getVMVariableId(): Int
    open fun onBinding(data: Any, position: Int) = Unit
    open fun onViewAttachedToWindow() = Unit
    open fun onViewDetachedFromWindow() = Unit
}