package cc.kafuu.bilidownload.common.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cc.kafuu.bilidownload.common.room.entity.DownloadResourceEntity

@Dao
interface DownloadResourceDao {

    // 插入一个新的ResourceEntity
    @Insert
    suspend fun insert(resourceEntity: DownloadResourceEntity): Long

    // 更新已存在的ResourceEntity
    @Update
    suspend fun update(resourceEntity: DownloadResourceEntity)

    @Query("DELETE FROM DownloadResource WHERE taskId = :taskId")
    suspend fun deleteTaskByTaskId(taskId: Long)

    // 根据ID删除ResourceEntity
    @Query("DELETE FROM DownloadResource WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 查询所有ResourceEntity
    @Query("SELECT * FROM DownloadResource")
    suspend fun queryAllResources(): List<DownloadResourceEntity>

    // 根据taskId查询ResourceEntity
    @Query("SELECT * FROM DownloadResource WHERE taskId = :taskEntityId")
    suspend fun queryResourcesByTaskId(taskEntityId: Long): List<DownloadResourceEntity>

    @Query("SELECT * FROM DownloadResource WHERE taskId = :taskEntityId")
    fun queryResourcesLiveDataByTaskId(taskEntityId: Long): LiveData<List<DownloadResourceEntity>>

    // 根据ID查询单个ResourceEntity
    @Query("SELECT * FROM DownloadResource WHERE id = :id")
    suspend fun queryResourceById(id: Long): DownloadResourceEntity?

    // 根据ID查询单个ResourceEntity
    @Query("SELECT * FROM DownloadResource WHERE id = :id")
    fun queryResourceLiveDataById(id: Long): LiveData<DownloadResourceEntity>
}