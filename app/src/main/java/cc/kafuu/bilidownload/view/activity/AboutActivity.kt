package cc.kafuu.bilidownload.view.activity

import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
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
    override fun initViews() {
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        val textView: String
        binding.apply {
            textView = "rv_about_invitation"
        }
        val invitation = findViewById<RelativeLayout>(R.id.rv_about_invitation)
        invitation.setOnClickListener {
            Toast.makeText(this, textView, Toast.LENGTH_SHORT).show()
        }
    }
}