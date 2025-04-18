package cc.kafuu.bilidownload.feature.viewbinding.view.activity

import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.core.viewbinding.CoreActivity
import cc.kafuu.bilidownload.databinding.ActivitySearchBinding
import cc.kafuu.bilidownload.common.constant.SearchType
import cc.kafuu.bilidownload.common.utils.bindOnEditorAction
import cc.kafuu.bilidownload.feature.viewbinding.view.fragment.SearchListFragment
import cc.kafuu.bilidownload.feature.viewbinding.viewmodel.activity.SearchViewModel


class SearchActivity : CoreActivity<ActivitySearchBinding, SearchViewModel>(
    SearchViewModel::class.java,
    R.layout.activity_search,
    BR.viewModel
), AdapterView.OnItemSelectedListener {

    override fun initViews() {
        setImmersionStatusBar()
        initListener()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mViewDataBinding.etSearchContent.apply {
            requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                this,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    private fun initListener() {
        mViewModel.searchRequestLiveData.observe(this) {
            if (TextUtils.isEmpty(it)) return@observe
            mViewDataBinding.fvFragment.getFragment<SearchListFragment>().doSearch(it)
            hideSoftInput()
        }
        mViewDataBinding.spSearchType.onItemSelectedListener = this
        mViewDataBinding.tvSearch.setOnClickListener {
            mViewModel.onSearch(mViewDataBinding.etSearchContent.text.toString())
        }
        bindOnEditorAction(mViewDataBinding.etSearchContent) {
            mViewModel.onSearch(mViewDataBinding.etSearchContent.text.toString())
        }
    }

    private fun hideSoftInput() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            mViewDataBinding.etSearchContent.windowToken, 0
        )
    }

    /**
     * 搜索类型被选择事件 */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        mViewDataBinding.fvFragment.getFragment<SearchListFragment>()
            .onSearchTypeChange(getSearchType())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    private fun getSearchType() = when (mViewDataBinding.spSearchType.selectedItemPosition) {
        0 -> SearchType.VIDEO
        1 -> SearchType.MEDIA_BANGUMI
        2 -> SearchType.MEDIA_FT
        else -> throw IllegalArgumentException("Unknown search type: $mViewDataBinding")
    }
}