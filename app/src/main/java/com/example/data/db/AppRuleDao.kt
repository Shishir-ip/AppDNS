package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllRulesList(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): AppRule?

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    fun getRuleForPackageFlow(packageName: String): Flow<AppRule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("UPDATE app_rules SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun setRuleEnabled(packageName: String, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM app_rules WHERE isEnabled = 1")
    fun getActiveRulesCount(): Flow<Int>
}
