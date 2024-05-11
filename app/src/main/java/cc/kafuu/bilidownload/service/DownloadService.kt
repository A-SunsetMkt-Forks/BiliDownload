package cc.kafuu.bilidownload.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import cc.kafuu.bilidownload.common.jniexport.FFMpegJNI
import cc.kafuu.bilidownload.common.manager.DownloadManager
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.room.entity.DownloadTaskEntity
import cc.kafuu.bilidownload.common.room.entity.ResourceEntity
import cc.kafuu.bilidownload.common.room.repository.BiliVideoRepository
import cc.kafuu.bilidownload.common.utils.CommonLibs
import cc.kafuu.bilidownload.model.event.DownloadRequestFailedEvent
import cc.kafuu.bilidownload.model.event.DownloadStatusChangeEvent
import cc.kafuu.bilidownload.notification.DownloadNotification
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadGroupTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import kotlin.properties.Delegates

class DownloadService : Service() {
    companion object {
        private const val KEY_ENTITY_ID = "entityId"

        private val mDownloadTaskDao = CommonLibs.requireAppDatabase().downloadTaskDao()
        private val mResourceDao = CommonLibs.requireAppDatabase().resourceDao()

        private const val TAG = "DownloadService"
        private fun startService(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startDownload(context: Context, taskId: Long) {
            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra(KEY_ENTITY_ID, taskId)
            startService(context, intent)
        }

        suspend fun resumeDownload(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            mDownloadTaskDao.getLatestDownloadTask(
                DownloadTaskEntity.STATUS_DOWNLOADING,
                DownloadTaskEntity.STATUS_PREPARE
            ).forEach {
                if (it.downloadTaskId != null && !DownloadManager.containsTask(it.downloadTaskId!!)) {
                    intent.putExtra(KEY_ENTITY_ID, it.id)
                    startService(context, intent)
                }
            }
        }
    }

    private val mServiceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var mRunningTaskCount by Delegates.observable(0) { _, _, value ->
        if (value > 0) return@observable
        stopSelf()
    }

    private lateinit var mDownloadNotification: DownloadNotification

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        Aria.download(this).register()

        mDownloadNotification = DownloadNotification(this)

        EventBus.getDefault().register(this)
        startForeground(
            mDownloadNotification.getChannelNotificationId(),
            mDownloadNotification.getForegroundNotification()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Aria.download(this).unRegister()
        EventBus.getDefault().unregister(this)
        mServiceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val entityId = intent?.getLongExtra(KEY_ENTITY_ID, -1L)
        if (entityId == null || entityId == -1L) {
            return super.onStartCommand(intent, flags, startId)
        }
        Log.d(TAG, "onStartCommand: $entityId")
        mRunningTaskCount++
        mServiceScope.launch {
            try {
                assigningTask(entityId)
            } catch (e: Exception) {
                mRunningTaskCount--
                Log.e(TAG, e.message ?: "Unknown error")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 分配下载任务
     * @param entityId entity id
     * */
    private suspend fun assigningTask(entityId: Long) {
        val entity = mDownloadTaskDao.getDownloadTaskById(entityId)
            ?: throw IllegalStateException("Task [E$entityId] get download task entity failed")

        // 尝试保存视频信息和请求下载
        doSaveVideoDetails(entity)

        // 开始请求下载
        DownloadManager.requestDownload(entity)
    }

    /**
     * 尝试保存视频信息
     * previous: [assigningTask]
     * 1. 请求获取视频详情；2. 根据获取到的数据更新数据库；
     * */
    private suspend fun doSaveVideoDetails(entity: DownloadTaskEntity) {
        val biliVideoData =
            NetworkManager.biliVideoRepository.syncGetVideoDetail(entity.biliBvid) { responseCode, returnCode, message ->
                mDownloadNotification.notificationGetVideoDetailsFailed(
                    entity,
                    responseCode,
                    returnCode,
                    message
                )
                // 如果无法获取视频详情，且这个任务还在准备阶段则直接删除任务
                // 因为没有对应获取视频详情失败的STATUS（也不需要）
                if (entity.status == DownloadTaskEntity.STATUS_PREPARE) {
                    mServiceScope.launch { mDownloadTaskDao.delete(entity) }
                }
                Log.e(
                    TAG,
                    "Task [E${entity.id}] get video details failed, responseCode: $responseCode, returnCode: $returnCode, message: $message"
                )
            } ?: throw IllegalStateException("Task [E${entity.id}] get video details failed")

        // 更新数据库中的视频信息
        BiliVideoRepository.doInsertOrUpdateVideoDetails(biliVideoData)
    }


    /**
     * 请求下载资源失败事件 */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onRequestFailedEvent(event: DownloadRequestFailedEvent) {
        mRunningTaskCount--
        mDownloadNotification.notificationRequestFailed(
            event.entity,
            event.httpCode,
            event.code,
            event.message
        )
    }

    /**
     * 下载状态改变事件 */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onDownloadStatusChangeEvent(event: DownloadStatusChangeEvent) {
        Log.d(
            TAG,
            "Task [D${event.task.entity.id}, E${event.entity.id}] status change, status: ${event.status}"
        )
        mServiceScope.launch {
            when (event.status) {
                DownloadManager.TaskStatus.FAILURE -> onDownloadFailed(event.entity, event.task)
                DownloadManager.TaskStatus.COMPLETED -> onDownloadCompleted(
                    event.entity,
                    event.task
                )

                DownloadManager.TaskStatus.EXECUTING -> onDownloadExecuting(
                    event.entity,
                    event.task
                )

                DownloadManager.TaskStatus.CANCELLED -> onDownloadCancelled(
                    event.entity,
                    event.task
                )

                else -> Unit
            }
            // 是终止态
            if (event.status.isEndStatus) {
                mDownloadNotification.updateDownloadProgress(event.entity, null)
                mRunningTaskCount--
            }
        }
    }


    /**
     * 下载失败
     * from [onDownloadStatusChangeEvent]
     * */
    private suspend fun onDownloadFailed(entity: DownloadTaskEntity, task: DownloadGroupTask) {
        mDownloadTaskDao.update(entity.apply {
            status = DownloadTaskEntity.STATUS_DOWNLOAD_FAILED
        })
        mDownloadNotification.notificationDownloadFailed(entity)
    }

    /**
     * 完成下载操作
     * from [onDownloadStatusChangeEvent]
     * */
    private suspend fun onDownloadCompleted(entity: DownloadTaskEntity, task: DownloadGroupTask) {
        // 更新状态为正在合成
        mDownloadTaskDao.update(entity.apply {
            status = DownloadTaskEntity.STATUS_SYNTHESIS
        })

        // 尝试合成音视频
        val syntheticStatus = if (doSynthetic(entity, task)) {
            DownloadTaskEntity.STATUS_COMPLETED
        } else {
            DownloadTaskEntity.STATUS_SYNTHESIS_FAILED
        }

        // 更新下载状态为合成音视频状态
        mDownloadTaskDao.update(entity.apply {
            status = syntheticStatus
        })

        // 未完成的
        if (syntheticStatus == DownloadTaskEntity.STATUS_SYNTHESIS_FAILED) {
            mDownloadNotification.notificationSynthesisFailed(entity)
            return
        }

        // 登记资源
        val resource = entity.getDefaultOutputFile()
        mResourceDao.insert(
            ResourceEntity(
                taskEntityId = entity.id,
                name = "Default Resource",
                mimeType = entity.dashVideoMimeType ?: entity.dashAudioMimeType ?: "unknown",
                storageSizeBytes = resource.length(),
                creationTime = System.currentTimeMillis(),
                file = resource.path
            )
        )
    }

    /**
     * 下载任务执行中...
     * from [onDownloadStatusChangeEvent]*/
    private suspend fun onDownloadExecuting(entity: DownloadTaskEntity, task: DownloadGroupTask) {
        Log.d(
            TAG,
            "Task [D${task.entity.id}, E${entity.id}] status change, percent: ${task.percent}%"
        )
        if (entity.status != DownloadTaskEntity.STATUS_DOWNLOADING) {
            mDownloadTaskDao.update(entity.apply {
                status = DownloadTaskEntity.STATUS_DOWNLOADING
            })
        }
        mDownloadNotification.updateDownloadProgress(entity, task.percent)
    }

    /**
     * 下载任务被取消
     * from [onDownloadStatusChangeEvent] */
    private suspend fun onDownloadCancelled(entity: DownloadTaskEntity, task: DownloadGroupTask) {
        mDownloadTaskDao.delete(entity)
        mDownloadNotification.notificationDownloadCancel(entity)
    }

    /**
     * 尝试合成视频
     * */
    private fun doSynthetic(entity: DownloadTaskEntity, task: DownloadGroupTask): Boolean {
        // 取得最终合成输出路径
        val outputFile = entity.getDefaultOutputFile()

        // 取得缓存文件路径数组
        val cachesPathArray = task.entity.subEntities.map { it.filePath }.toTypedArray()

        val outputStatus = try {
            when {
                // 缓存为空
                cachesPathArray.isEmpty() -> false
                // 只有一个文件，则不进行合成，此缓存即最终输出
                cachesPathArray.size == 1 -> {
                    File(cachesPathArray[0]).copyTo(outputFile, true)
                    true
                }
                // 多个文件则对其进行合成
                else -> FFMpegJNI.mergeMedia(outputFile.path, cachesPathArray)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        // 清理缓存
        CommonLibs.requireDownloadCacheDir(entity.id).let {
            it.deleteRecursively()
            it.deleteOnExit()
        }

        return outputStatus
    }
}