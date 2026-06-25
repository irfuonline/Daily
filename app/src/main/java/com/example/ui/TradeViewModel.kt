package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TradeDatabase
import com.example.data.TradeRecord
import com.example.data.TradeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TradeViewModel(private val repository: TradeRepository) : ViewModel() {

    // Helper functions for date formatting
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    // Form states
    val selectedDate = MutableStateFlow(getCurrentDateString())
    val morningInput = MutableStateFlow("")
    val nightInput = MutableStateFlow("")
    val cardInput = MutableStateFlow("")
    val notesInput = MutableStateFlow("")
    val isEditing = MutableStateFlow(false)

    // Statement filtering state
    val statementMonth = MutableStateFlow(getCurrentMonthString())

    // UI Toast/Notification Events
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Load all records
    val allRecords: StateFlow<List<TradeRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load records for the selected statement month (reactive to statementMonth state)
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyRecords: StateFlow<List<TradeRecord>> = statementMonth
        .flatMapLatest { month ->
            repository.getRecordsForMonth(month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load initial record for today's date if it exists
        loadRecordForDate(getCurrentDateString())
    }

    private fun getCurrentDateString(): String {
        return dateFormatter.format(Calendar.getInstance().time)
    }

    private fun getCurrentMonthString(): String {
        return monthFormatter.format(Calendar.getInstance().time)
    }

    fun onDateChanged(dateStr: String) {
        loadRecordForDate(dateStr)
    }

    fun onMorningInputChanged(value: String) {
        // Allow numeric inputs with at most one decimal point
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            morningInput.value = value
        }
    }

    fun onNightInputChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            nightInput.value = value
        }
    }

    fun onCardInputChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            cardInput.value = value
        }
    }

    fun onNotesInputChanged(value: String) {
        notesInput.value = value
    }

    fun onStatementMonthChanged(monthStr: String) {
        statementMonth.value = monthStr
    }

    private fun loadRecordForDate(dateStr: String) {
        selectedDate.value = dateStr
        viewModelScope.launch {
            val record = repository.getRecordByDate(dateStr)
            if (record != null) {
                morningInput.value = if (record.morningTrade == 0.0) "" else record.morningTrade.toString()
                nightInput.value = if (record.nightTrade == 0.0) "" else record.nightTrade.toString()
                cardInput.value = if (record.cardPayment == 0.0) "" else record.cardPayment.toString()
                notesInput.value = record.notes
                isEditing.value = true
            } else {
                morningInput.value = ""
                nightInput.value = ""
                cardInput.value = ""
                notesInput.value = ""
                isEditing.value = false
            }
        }
    }

    fun saveRecord() {
        viewModelScope.launch {
            val dateStr = selectedDate.value
            val morning = morningInput.value.toDoubleOrNull() ?: 0.0
            val night = nightInput.value.toDoubleOrNull() ?: 0.0
            val card = cardInput.value.toDoubleOrNull() ?: 0.0
            val notes = notesInput.value.trim()

            val total = morning + night

            // Validation: Card payment cannot exceed total daily trades
            if (card > total) {
                _toastMessage.emit("Validation Error: Card payments (${formatCurrency(card)}) cannot be greater than total sales (${formatCurrency(total)})")
                return@launch
            }

            val record = TradeRecord(
                date = dateStr,
                morningTrade = morning,
                nightTrade = night,
                cardPayment = card,
                notes = notes
            )

            repository.insertRecord(record)
            _toastMessage.emit("Record for $dateStr successfully saved!")
            
            // Reload record to reflect isEditing status
            loadRecordForDate(dateStr)
        }
    }

    fun deleteRecord(record: TradeRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            _toastMessage.emit("Deleted record for ${record.date}")
            // If deleting currently active date, reload inputs
            if (record.date == selectedDate.value) {
                loadRecordForDate(selectedDate.value)
            }
        }
    }

    fun deleteByDate(dateStr: String) {
        viewModelScope.launch {
            repository.deleteByDate(dateStr)
            _toastMessage.emit("Deleted record for $dateStr")
            if (dateStr == selectedDate.value) {
                loadRecordForDate(selectedDate.value)
            }
        }
    }

    fun clearForm() {
        morningInput.value = ""
        nightInput.value = ""
        cardInput.value = ""
        notesInput.value = ""
        isEditing.value = false
    }

    // Helper formatting function
    fun formatCurrency(value: Double): String {
        return String.format(Locale.getDefault(), "$%.2f", value)
    }

    // Factory Provider
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TradeViewModel::class.java)) {
                val database = TradeDatabase.getDatabase(application)
                val repository = TradeRepository(database.tradeDao())
                return TradeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
