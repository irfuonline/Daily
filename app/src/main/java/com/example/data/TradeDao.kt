package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE date LIKE :monthPattern ORDER BY date ASC")
    fun getRecordsForMonth(monthPattern: String): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): TradeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TradeRecord)

    @Delete
    suspend fun deleteRecord(record: TradeRecord)
    
    @Query("DELETE FROM trade_records WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
