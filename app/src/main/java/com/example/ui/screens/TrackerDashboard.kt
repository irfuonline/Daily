package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.TradeRecord
import com.example.ui.TradeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDashboard(
    viewModel: TradeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }

    // Observe ViewModel State
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val morningInput by viewModel.morningInput.collectAsStateWithLifecycle()
    val nightInput by viewModel.nightInput.collectAsStateWithLifecycle()
    val cardInput by viewModel.cardInput.collectAsStateWithLifecycle()
    val notesInput by viewModel.notesInput.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()

    val statementMonth by viewModel.statementMonth.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val monthlyRecords by viewModel.monthlyRecords.collectAsStateWithLifecycle()

    // Handle ViewModel Toast Messages
    LaunchedEffect(key1 = Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Daily Trade Tracker",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Daily Log") },
                    label = { Text("Daily Log") },
                    modifier = Modifier.testTag("tab_daily_log")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Ledger") },
                    label = { Text("Daily Ledger") },
                    modifier = Modifier.testTag("tab_ledger")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Statements") },
                    label = { Text("Statements") },
                    modifier = Modifier.testTag("tab_statements")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                0 -> LogShiftScreen(
                    viewModel = viewModel,
                    selectedDate = selectedDate,
                    morningInput = morningInput,
                    nightInput = nightInput,
                    cardInput = cardInput,
                    notesInput = notesInput,
                    isEditing = isEditing
                )
                1 -> LedgerScreen(
                    viewModel = viewModel,
                    records = allRecords,
                    onEditRecord = { record ->
                        viewModel.onDateChanged(record.date)
                        activeTab = 0
                    }
                )
                2 -> StatementsScreen(
                    viewModel = viewModel,
                    statementMonth = statementMonth,
                    records = monthlyRecords
                )
            }
        }
    }
}

@Composable
fun LogShiftScreen(
    viewModel: TradeViewModel,
    selectedDate: String,
    morningInput: String,
    nightInput: String,
    cardInput: String,
    notesInput: String,
    isEditing: Boolean
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Live calculations
    val morningDouble = morningInput.toDoubleOrNull() ?: 0.0
    val nightDouble = nightInput.toDoubleOrNull() ?: 0.0
    val cardDouble = cardInput.toDoubleOrNull() ?: 0.0
    val totalTrade = morningDouble + nightDouble
    val calculatedCash = totalTrade - cardDouble

    // Setup date picker dialog
    val calendar = Calendar.getInstance()
    val parts = selectedDate.split("-")
    if (parts.size == 3) {
        try {
            calendar.set(Calendar.YEAR, parts[0].toInt())
            calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
            calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        } catch (e: Exception) {
            // fallback
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.onDateChanged(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Graphic Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dashboard_hero_1782419922087),
                contentDescription = "Dashboard banner decoration",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.55f
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (isEditing) "Edit Record" else "Record Today's Trade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Track shifting income and card payments smoothly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Date Picker Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Trading Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateLong(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("date_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Date")
                }
            }
        }

        if (isEditing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Edit mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Modifying existing entry for this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Input Fields Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Shift Cashflows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Morning trade input
                OutlinedTextField(
                    value = morningInput,
                    onValueChange = { viewModel.onMorningInputChanged(it) },
                    label = { Text("Morning Sales ($)") },
                    prefix = { Text("$ ") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_morning_trade")
                )

                // Night trade input
                OutlinedTextField(
                    value = nightInput,
                    onValueChange = { viewModel.onNightInputChanged(it) },
                    label = { Text("Night Sales ($)") },
                    prefix = { Text("$ ") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_night_trade")
                )

                // Card payments input
                OutlinedTextField(
                    value = cardInput,
                    onValueChange = { viewModel.onCardInputChanged(it) },
                    label = { Text("Card Payments ($)") },
                    prefix = { Text("$ ") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_card_payment")
                )

                // Notes input
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { viewModel.onNotesInputChanged(it) },
                    label = { Text("Transaction Notes (Optional)") },
                    placeholder = { Text("e.g. Card machine issue, busy weekend") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes")
                )
            }
        }

        // Real-time Calculation Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Live Calculations Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Shift Trade:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = viewModel.formatCurrency(totalTrade),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Card Payment Total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = viewModel.formatCurrency(cardDouble),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Calculated Cash Total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = viewModel.formatCurrency(calculatedCash),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (calculatedCash >= 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.clearForm()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("clear_form_button")
            ) {
                Text("Clear Form")
            }

            Button(
                onClick = {
                    viewModel.saveRecord()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_record_button")
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Update Record" else "Save Record")
            }
        }
    }
}

@Composable
fun LedgerScreen(
    viewModel: TradeViewModel,
    records: List<TradeRecord>,
    onEditRecord: (TradeRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) {
            records
        } else {
            records.filter {
                it.date.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Daily Ledger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${records.size} total days tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by date or notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ledger_search_bar")
        )

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No records found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isEmpty()) "Go to 'Daily Log' tab to add your first sales record." else "Try adjusting your search filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecords, key = { it.date }) { record ->
                    LedgerItemCard(
                        record = record,
                        formatCurrency = { viewModel.formatCurrency(it) },
                        onEdit = { onEditRecord(record) },
                        onDelete = { viewModel.deleteRecord(record) }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerItemCard(
    record: TradeRecord,
    formatCurrency: (Double) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete the trade record for ${formatDateLong(record.date)}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${record.date}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formatDateLong(record.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit record",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete record",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Split breakdown grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MORNING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(record.morningTrade), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("NIGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(record.nightTrade), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("CARD TOTAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(record.cardPayment), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Total highlighting and Cash balance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Cash Left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(record.cashPayment),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Daily Total Trade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(record.totalTrade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (record.notes.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatementsScreen(
    viewModel: TradeViewModel,
    statementMonth: String,
    records: List<TradeRecord>
) {
    val context = LocalContext.current

    // Aggregate monthly statistics
    val totalMorning = records.sumOf { it.morningTrade }
    val totalNight = records.sumOf { it.nightTrade }
    val totalCard = records.sumOf { it.cardPayment }
    val totalSales = records.sumOf { it.totalTrade }
    val totalCash = totalSales - totalCard
    val recordedDaysCount = records.size
    val dailyAverage = if (recordedDaysCount > 0) totalSales / recordedDaysCount else 0.0

    // Adjust months
    val onPreviousMonth = {
        viewModel.onStatementMonthChanged(adjustMonth(statementMonth, -1))
    }
    val onNextMonth = {
        viewModel.onStatementMonthChanged(adjustMonth(statementMonth, 1))
    }

    // Share action
    val shareStatement = {
        val shareText = generateShareText(formatMonthHeader(statementMonth), records)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Trade Statement - ${formatMonthHeader(statementMonth)}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Monthly Statement")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Monthly Statement",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatMonthHeader(statementMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }
        }

        if (records.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "No data",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No records for this month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Try logging shift records in the 'Daily Log' tab using dates in this month to generate statements.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // General Statement Summary Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TOTAL SALES IN ${formatMonthHeader(statementMonth).uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.formatCurrency(totalSales),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Recorded Days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "$recordedDaysCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Daily Average",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                viewModel.formatCurrency(dailyAverage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Detailed breakdowns card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aggregated Cashflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Morning Shifts Total:", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.formatCurrency(totalMorning), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Night Shifts Total:", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.formatCurrency(totalNight), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Card Payments Total:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(viewModel.formatCurrency(totalCard), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cash Collected Total:", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.formatCurrency(totalCash), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Visual Charts Section
            Text(
                text = "Analytics & Visualization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Cash vs Card Ring Chart
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Payment Split",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PaymentDistributionChart(
                                cardAmount = totalCard,
                                cashAmount = totalCash,
                                cardColor = MaterialTheme.colorScheme.secondary,
                                cashColor = MaterialTheme.colorScheme.primary
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val cardPct = if (totalSales > 0) (totalCard / totalSales * 100).toInt() else 0
                                Text(
                                    text = "$cardPct%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Legend
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LegendRow(color = MaterialTheme.colorScheme.secondary, label = "Cards: ${viewModel.formatCurrency(totalCard)}")
                            LegendRow(color = MaterialTheme.colorScheme.primary, label = "Cash: ${viewModel.formatCurrency(totalCash)}")
                        }
                    }
                }

                // Card 2: Shift Sales Bar Chart
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Shift Splits",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            ShiftSplitChart(
                                morningAmount = totalMorning,
                                nightAmount = totalNight,
                                morningColor = MaterialTheme.colorScheme.primary,
                                nightColor = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        // Legend
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LegendRow(color = MaterialTheme.colorScheme.primary, label = "Morning: ${viewModel.formatCurrency(totalMorning)}")
                            LegendRow(color = MaterialTheme.colorScheme.tertiary, label = "Night: ${viewModel.formatCurrency(totalNight)}")
                        }
                    }
                }
            }

            // Share & Export button
            Button(
                onClick = shareStatement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_statement_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share Statement")
            }

            // Monthly breakdown listing
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Statement Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider()

                    records.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatDateShort(record.date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Morn: ${viewModel.formatCurrency(record.morningTrade)} | Night: ${viewModel.formatCurrency(record.nightTrade)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = viewModel.formatCurrency(record.totalTrade),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Card: ${viewModel.formatCurrency(record.cardPayment)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDistributionChart(
    cardAmount: Double,
    cashAmount: Double,
    cardColor: Color,
    cashColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val total = cardAmount + cashAmount
        if (total == 0.0) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
        } else {
            val cardSweep = ((cardAmount / total) * 360).toFloat()
            val cashSweep = 360f - cardSweep

            // Card arc
            drawArc(
                color = cardColor,
                startAngle = -90f,
                sweepAngle = cardSweep,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )

            // Cash arc
            drawArc(
                color = cashColor,
                startAngle = -90f + cardSweep,
                sweepAngle = cashSweep,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun ShiftSplitChart(
    morningAmount: Double,
    nightAmount: Double,
    morningColor: Color,
    nightColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxAmount = maxOf(morningAmount, nightAmount)
        if (maxAmount == 0.0) {
            // Draw empty lines
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.2f),
                strokeWidth = 24f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.2f),
                strokeWidth = 24f,
                cap = StrokeCap.Round
            )
        } else {
            val mornHeight = ((morningAmount / maxAmount) * size.height * 0.8f).toFloat()
            val nightHeight = ((nightAmount / maxAmount) * size.height * 0.8f).toFloat()

            // Morning Bar
            drawLine(
                color = morningColor,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height - maxOf(mornHeight, 4f)),
                strokeWidth = 32f,
                cap = StrokeCap.Round
            )

            // Night Bar
            drawLine(
                color = nightColor,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height - maxOf(nightHeight, 4f)),
                strokeWidth = 32f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Global Formatter Utilities (safe for older APIs)
fun formatDateLong(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

fun formatDateShort(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

fun formatMonthHeader(monthStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val date = parser.parse(monthStr)
        if (date != null) formatter.format(date) else monthStr
    } catch (e: Exception) {
        monthStr
    }
}

fun adjustMonth(currentMonthStr: String, offset: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(currentMonthStr) ?: return currentMonthStr
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, offset)
        sdf.format(cal.time)
    } catch (e: Exception) {
        currentMonthStr
    }
}

fun generateShareText(month: String, records: List<TradeRecord>): String {
    val totalMorning = records.sumOf { it.morningTrade }
    val totalNight = records.sumOf { it.nightTrade }
    val totalCard = records.sumOf { it.cardPayment }
    val totalSales = records.sumOf { it.totalTrade }
    val totalCash = totalSales - totalCard
    val dailyAverage = if (records.isNotEmpty()) totalSales / records.size else 0.0

    val sb = StringBuilder()
    sb.append("📊 DAILY TRADE TRACKER - MONTHLY STATEMENT\n")
    sb.append("Month: $month\n")
    sb.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
    sb.append("=====================================\n\n")
    sb.append("📈 FINANCIAL SUMMARY:\n")
    sb.append(String.format("• Total Trade Volume: $%.2f\n", totalSales))
    sb.append(String.format("• Morning Shift Total: $%.2f\n", totalMorning))
    sb.append(String.format("• Night Shift Total: $%.2f\n", totalNight))
    sb.append(String.format("• Card Payments Total: $%.2f\n", totalCard))
    sb.append(String.format("• Cash Collected: $%.2f\n", totalCash))
    sb.append(String.format("• Active Trading Days: %d days\n", records.size))
    sb.append(String.format("• Daily Average Trade: $%.2f\n", dailyAverage))
    sb.append("=====================================\n\n")
    sb.append("📅 DAILY BREAKDOWN LEDGER:\n")

    records.forEach { r ->
        sb.append(String.format("Date: %s\n", r.date))
        sb.append(String.format("  - Morning Shift: $%.2f\n", r.morningTrade))
        sb.append(String.format("  - Night Shift: $%.2f\n", r.nightTrade))
        sb.append(String.format("  - Card Payments: $%.2f\n", r.cardPayment))
        sb.append(String.format("  - Cash Payments: $%.2f\n", r.cashPayment))
        sb.append(String.format("  - Daily Total: $%.2f\n", r.totalTrade))
        if (r.notes.isNotEmpty()) {
            sb.append("  - Notes: ${r.notes}\n")
        }
        sb.append("  --------------------\n")
    }
    sb.append("\nReport prepared via Daily Trade Tracker.")
    return sb.toString()
}
