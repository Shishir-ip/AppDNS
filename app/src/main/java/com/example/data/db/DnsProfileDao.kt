package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DnsProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsProfileDao {
    @Query("SELECT * FROM dns_profiles ORDER BY isPreset DESC, name ASC")
    fun getAllProfiles(): Flow<List<DnsProfile>>

    @Query("SELECT * FROM dns_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): DnsProfile?

    @Query("SELECT * FROM dns_profiles WHERE id = :id LIMIT 1")
    fun getProfileByIdFlow(id: String): Flow<DnsProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<DnsProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DnsProfile)

    @Update
    suspend fun updateProfile(profile: DnsProfile)

    @Delete
    suspend fun deleteProfile(profile: DnsProfile)

    @Query("UPDATE dns_profiles SET latencyMs = :latency WHERE id = :id")
    suspend fun updateLatency(id: String, latency: Long)

    @Query("UPDATE dns_profiles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)
}
