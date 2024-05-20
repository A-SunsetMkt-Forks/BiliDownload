package cc.kafuu.bilidownload.view.activity

import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.core.CoreActivity
import cc.kafuu.bilidownload.common.core.CoreViewModel
import cc.kafuu.bilidownload.databinding.ActivityAboutBinding

class OpenLicenseActivity : CoreActivity<ActivityAboutBinding, CoreViewModel>(
    CoreViewModel::class.java,
    R.layout.activity_about,
    0
) {

    override fun initViews() {

    }
}