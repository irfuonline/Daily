package com.example.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(private val tradeDao: TradeDao) {
    val allRecords: Flow<List<TradeRecord>> = tradeDao.getAllRecords()

    fun getRecordsForMonth(month: String): Flow<List<TradeRecord>> {
        // month should be in format "yyyy-MM"
        return tradeDao.getRecordsForMonth("$month%")
    }

    suspend fun getRecordByDate(date: String): TradeRecord? {
        return tradeDao.getRecordByDate(date)
    }

    suspend fun insertRecord(record: TradeRecord) {
        tradeDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: TradeRecord) {
        tradeDao.deleteRecord(record)
    }

    suspend fun deleteByDate(date: String) {
        tradeDao.deleteByDate(date)
    }
}
