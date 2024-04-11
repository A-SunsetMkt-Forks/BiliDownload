package cc.kafuu.bilidownload.common.room.dto

import androidx.room.Embedded
import cc.kafuu.bilidownload.common.room.entity.DownloadTaskEntity

data class DownloadTaskWithVideoDetails(
    // DownloadTaskEntity
    @Embedded
    val downloadTask: DownloadTaskEntity,
    // BiliVideoMainEntity
    val title: String,
    val description: String,
    val cover: String,
    // BiliVideoPartEntity
    val partTitle: String
)