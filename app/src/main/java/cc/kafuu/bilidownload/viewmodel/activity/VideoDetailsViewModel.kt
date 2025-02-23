package cc.kafuu.bilidownload.viewmodel.activity

import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.CommonLibs
import cc.kafuu.bilidownload.common.constant.DashType
import cc.kafuu.bilidownload.common.core.CoreViewModel
import cc.kafuu.bilidownload.common.ext.liveData
import cc.kafuu.bilidownload.common.manager.DownloadManager
import cc.kafuu.bilidownload.common.model.LoadingStatus
import cc.kafuu.bilidownload.common.model.action.popmessage.ToastMessageAction
import cc.kafuu.bilidownload.common.model.bili.BiliDashModel
import cc.kafuu.bilidownload.common.model.bili.BiliMediaModel
import cc.kafuu.bilidownload.common.model.bili.BiliResourceModel
import cc.kafuu.bilidownload.common.model.bili.BiliVideoModel
import cc.kafuu.bilidownload.common.model.bili.BiliVideoPartModel
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliPlayStreamDash
import cc.kafuu.bilidownload.common.network.model.BiliSeasonData
import cc.kafuu.bilidownload.common.network.model.BiliVideoData
import cc.kafuu.bilidownload.common.utils.TimeUtils
import cc.kafuu.bilidownload.view.dialog.BiliPartDialog

class VideoDetailsViewModel : CoreViewModel() {
    private val mLoadingStatusLiveData = MutableLiveData(LoadingStatus.waitStatus())
    val loadingStatusLiveData = mLoadingStatusLiveData.liveData()

    private val mBiliResourceModelLiveData = MutableLiveData<BiliResourceModel>()
    val biliResourceModelLiveData = mBiliResourceModelLiveData.liveData()

    private val mBiliVideoPageListLiveData = MutableLiveData<List<BiliVideoPartModel>>()
    val biliVideoPageListLiveData = mBiliVideoPageListLiveData.liveData()

    // 选中的片段
    private val mSelectedVideoPartLiveData = MutableLiveData<BiliVideoPartModel?>()
    val selectedVideoPartLiveData = mSelectedVideoPartLiveData.liveData()

    // 正在加载视频流数据的片段
    private val mLoadingVideoPartLiveData = MutableLiveData<BiliVideoPartModel?>()
    val loadingVideoPartLiveData = mLoadingVideoPartLiveData.liveData()

    fun initData(media: BiliMediaModel) {
        mLoadingStatusLiveData.value = LoadingStatus.loadingStatus()
        mBiliResourceModelLiveData.value = media

        val callback = object : IServerCallback<BiliSeasonData> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliSeasonData
            ) {
                mBiliVideoPageListLiveData.postValue(data.episodes.map {
                    BiliVideoPartModel(
                        bvid = it.bvid,
                        cid = it.cid,
                        name = "${it.title} ${it.longTitle}",
                        remark = it.badge
                    )
                })
                mLoadingStatusLiveData.value = LoadingStatus.doneStatus()
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                mLoadingStatusLiveData.value = LoadingStatus.errorStatus(
                    message = message
                )
            }
        }
        if (media.seasonId != 0L) {
            NetworkManager.biliVideoRepository.requestSeasonDetailBySeasonId(
                media.seasonId,
                callback
            )
        } else {
            NetworkManager.biliVideoRepository.requestSeasonDetailByEpId(media.mediaId, callback)
        }
    }

    fun initData(video: BiliVideoModel) {
        mLoadingStatusLiveData.value = LoadingStatus.loadingStatus()
        mBiliResourceModelLiveData.value = video
        val callback = object : IServerCallback<BiliVideoData> {
            override fun onSuccess(httpCode: Int, code: Int, message: String, data: BiliVideoData) {
                mBiliVideoPageListLiveData.postValue(data.pages.map {
                    BiliVideoPartModel(
                        bvid = video.bvid,
                        cid = it.cid,
                        name = it.part,
                        remark = TimeUtils.formatSecondTime(it.duration)
                    )
                })
                mLoadingStatusLiveData.value = LoadingStatus.doneStatus()
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                mLoadingStatusLiveData.value = LoadingStatus.errorStatus(
                    message = message
                )
            }
        }
        NetworkManager.biliVideoRepository.requestVideoDetail(video.bvid, callback)
    }

    @Synchronized
    fun onPartSelected(item: BiliVideoPartModel) {
        if (loadingVideoPartLiveData.value != null) return

        mSelectedVideoPartLiveData.value = item
        mLoadingVideoPartLiveData.value = item

        val callback = object : IServerCallback<BiliPlayStreamDash> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliPlayStreamDash
            ) {
                popSelectedVideoPartDialog(item, data)
                mLoadingVideoPartLiveData.postValue(null)
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                popMessage(ToastMessageAction(message, Toast.LENGTH_SHORT))
                mLoadingVideoPartLiveData.postValue(null)
            }
        }
        NetworkManager.biliVideoRepository.requestPlayStreamDash(item.bvid, item.cid, callback)
    }

    private fun popSelectedVideoPartDialog(
        part: BiliVideoPartModel,
        dash: BiliPlayStreamDash
    ) = popDialog(
        dialog = BiliPartDialog.buildDialog(
            part.name, dash.video, dash.getAllAudio()
        ),
        success = {
            val result = it as BiliPartDialog.Companion.Result
            val resources = mutableListOf<BiliDashModel>().apply {
                result.videoStream?.let { res -> add(BiliDashModel.create(DashType.VIDEO, res)) }
                result.audioStream?.let { res -> add(BiliDashModel.create(DashType.AUDIO, res)) }
            }
            DownloadManager.startDownload(
                CommonLibs.requireContext(), part.bvid, part.cid, resources
            )
        }
    )

    fun onDescriptionLongClick(): Boolean {
        val resource = mBiliResourceModelLiveData.value ?: return false
        val isSuccess = CommonLibs.copyToClipboard(
            label = resource.title,
            text = CommonLibs.getString(
                R.string.video_details_format,
                resource.title,
                resource.description
            )
        )
        if (isSuccess) {
            popMessage(ToastMessageAction(CommonLibs.getString(R.string.success_copy_video_info)))
        }
        return isSuccess
    }
}