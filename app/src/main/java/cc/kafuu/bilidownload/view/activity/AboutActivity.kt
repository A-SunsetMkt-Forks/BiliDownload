package cc.kafuu.bilidownload.view.activity

import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.core.CoreActivity
import cc.kafuu.bilidownload.databinding.ActivityAboutBinding
import cc.kafuu.bilidownload.viewmodel.activity.AboutViewModel


class AboutActivity : CoreActivity<ActivityAboutBinding, AboutViewModel>(
    AboutViewModel::class.java,
    R.layout.activity_about,
    BR.viewModel
) {
    companion object {
        private const val PREFIX = "tv_about"
    }

    override fun initViews() {
        findViewById<ViewGroup>(android.R.id.content).iterateChildrenWithPrefix(PREFIX) { textView ->
            textView.setOnClickListener {
                val componentName = resources.getResourceEntryName(textView.id).substringAfterLast('/')
                Toast.makeText(this, "Clicked on view: $componentName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ViewGroup.iterateChildrenWithPrefix(prefix: String, action: (AppCompatTextView) -> Unit) {
        for (i in 0 until childCount) {
            when (val child = getChildAt(i)) {
                is ViewGroup -> child.iterateChildrenWithPrefix(prefix, action)
                is AppCompatTextView -> child.takeIf { it.id != View.NO_ID && resources.getResourceEntryName(it.id).startsWith(prefix) }?.let(action)
            }
        }
    }

}