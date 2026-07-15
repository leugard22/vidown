package app.vidown.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'MERGING')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextQueuedDownload(): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeDownloadById(id: Long): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long): Int

    @Query("DELETE FROM downloads WHERE status = 'DONE'")
    suspend fun deleteCompletedDownloads()

    @Query("UPDATE downloads SET status = 'CANCELLED' WHERE status IN ('QUEUED', 'DOWNLOADING', 'MERGING')")
    suspend fun cancelAllActiveDownloads()
}
