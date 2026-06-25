package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val morningTrade: Double,
    val nightTrade: Double,
    val cardPayment: Double,
    val notes: String = ""
) {
    val totalTrade: Double
        get() = morningTrade + nightTrade

    val cashPayment: Double
        get() = totalTrade - cardPayment
}
