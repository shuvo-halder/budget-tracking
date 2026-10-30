package com.engrshuvo.financemanager.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileFriendly
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.SmsImportStatus
import com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSmsImportScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onToggleEnabled: (Boolean, Context) -> Unit,
    onToggleAutoPost: (Boolean) -> Unit,
    onOpenReviewRecord: (SmsImportedRecordEntity) -> Unit,
    onDismissReviewRecord: () -> Unit,
    onApproveRecord: (id: Long, categoryId: String, note: String) -> Unit,
    onDismissRecord: (id: Long) -> Unit,
    onDeleteRecord: (id: Long) -> Unit,
    onPasteInputChange: (String) -> Unit,
    onTestParsePaste: (sender: String, body: String) -> Unit,
    onImportPastedSms: (Context, sender: String, body: String) -> Unit,
    onDismissOperationMessage: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showProvidersDialog by remember { mutableStateOf(false) }
    var showBatteryInfoDialog by remember { mutableStateOf(false) }
    var showPasteSection by remember { mutableStateOf(true) }
    var pasteSender by remember { mutableStateOf("bKash") }

    LaunchedEffect(uiState.smsPasteInput) {
        if (uiState.smsPasteInput.isNotBlank()) {
            showPasteSection = true
        }
    }

    LaunchedEffect(uiState.smsOperationMessage) {
        uiState.smsOperationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onDismissOperationMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Payment SMS Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isSmsImportEnabled) "Share & manual import active" else "Import disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sms_import_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (onNavigateToHistory != null) {
                        IconButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier.testTag("sms_history_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "SMS History",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showBatteryInfoDialog = true },
                        modifier = Modifier.testTag("sms_battery_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Zero battery impact info",
                            tint = IncomeGreen
                        )
                    }
                    IconButton(
                        onClick = { showProvidersDialog = true },
                        modifier = Modifier.testTag("sms_providers_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Supported providers",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Toggle Card
            item(key = "toggle_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_toggle_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isSmsImportEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isSmsImportEnabled) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = if (uiState.isSmsImportEnabled) IncomeGreen else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Manual & Shared SMS Import",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (uiState.isSmsImportEnabled) "Active • Share via messaging app or paste" else "Feature is turned off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.isSmsImportEnabled) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = uiState.isSmsImportEnabled,
                                onCheckedChange = { checked ->
                                    onToggleEnabled(checked, context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = IncomeGreen,
                                    checkedTrackColor = IncomeGreen.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("sms_import_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(
                                label = "Zero Permissions Required",
                                color = IncomeGreen
                            )
                            StatusBadge(
                                label = "Android Share Supported",
                                color = Color(0xFF0284C7)
                            )
                            StatusBadge(
                                label = "100% Offline Local",
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }
            }

            // Zero-Permission Policy & How-To Info
            item(key = "activation_info") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Zero-Permission Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "• 100% Zero-Permission: No SMS permissions needed.\n" +
                                    "• Share SMS text directly from your messaging app to 'Daily Budget'.\n" +
                                    "• Or copy and paste payment text into the import box below.\n" +
                                    "• Ignores OTPs, promo alerts, and non-financial texts.\n" +
                                    "• All parsing happens on-device; no data is ever uploaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.smsImportActivatedAt > 0) {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            Text(
                                text = "Activated: ${dateFormat.format(Date(uiState.smsImportActivatedAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Statistics Row
            item(key = "stats_row") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Imported",
                        value = "${uiState.importedSmsRecords.size}",
                        subtitle = "Total records",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pending Review",
                        value = "${uiState.pendingReviewSmsRecords.size}",
                        subtitle = "Awaiting approval",
                        color = if (uiState.pendingReviewSmsRecords.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Auto-Post Option Card
            item(key = "autopost_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Post High-Confidence Items", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Automatically record clear transactions without waiting for review. Credit card bills always require review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.smsAutoPostUnambiguous,
                            onCheckedChange = onToggleAutoPost,
                            modifier = Modifier.testTag("sms_auto_post_switch")
                        )
                    }
                }
            }

            // SMS History Link Card
            if (onNavigateToHistory != null) {
                item(key = "sms_history_shortcut_card") {
                    Card(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_history_shortcut_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Mobile Banking SMS History",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "View all approved bKash, Nagad, Rocket & bank records",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${uiState.approvedSmsRecords.size} Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pending Review Header
            item(key = "pending_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pending Review (${uiState.pendingReviewSmsRecords.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (uiState.pendingReviewSmsRecords.isEmpty()) {
                item(key = "empty_pending") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.PlaylistAddCheck, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All Caught Up!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "No payment SMS messages currently waiting for your review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.pendingReviewSmsRecords, key = { it.id }) { record ->
                    SmsRecordCard(
                        record = record,
                        onReview = { onOpenReviewRecord(record) },
                        onDismiss = { onDismissRecord(record.id) }
                    )
                }
            }

            // Manual SMS Tester / Fallback Section
            item(key = "manual_paste_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Manual SMS Paste / Testing Fallback", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Test message parsing or manually import payment texts without needing system SMS permissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showPasteSection = !showPasteSection }) {
                                Icon(
                                    imageVector = if (showPasteSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPasteSection) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = pasteSender,
                                    onValueChange = { pasteSender = it },
                                    label = { Text("Sender Name / Shortcode") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = uiState.smsPasteInput,
                                    onValueChange = onPasteInputChange,
                                    label = { Text("Paste SMS Text Here") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sms_paste_input"),
                                    placeholder = { Text("e.g. Payment Tk 450.00 to 01712345678 successful. TrxID 9K38AB12CD") },
                                    minLines = 3
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { onTestParsePaste(pasteSender, uiState.smsPasteInput) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sms_test_parse_button")
                                    ) {
                                        Text("Test Parse")
                                    }
                                    Button(
                                        onClick = { onImportPastedSms(context, pasteSender, uiState.smsPasteInput) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sms_import_pasted_button")
                                    ) {
                                        Text("Import")
                                    }
                                }

                                if (!uiState.smsPasteTestResult.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.smsPasteTestResult,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Review & Approval Dialog
    uiState.smsRecordToReview?.let { record ->
        ReviewSmsRecordDialog(
            record = record,
            onDismiss = onDismissReviewRecord,
            onApprove = { catId, note -> onApproveRecord(record.id, catId, note) },
            onReject = { onDismissRecord(record.id) }
        )
    }

    // Supported Providers Dialog
    if (showProvidersDialog) {
        SupportedProvidersDialog(onDismiss = { showProvidersDialog = false })
    }

    // Battery Optimization Dialog
    if (showBatteryInfoDialog) {
        BatteryOptimizationDialog(onDismiss = { showBatteryInfoDialog = false })
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmsRecordCard(
    record: SmsImportedRecordEntity,
    onReview: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_candidate_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (record.type == TransactionType.INCOME) IncomeGreen.copy(alpha = 0.15f)
                                else ExpenseRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (record.provider.contains("Card", ignoreCase = true) || record.provider.contains("Bank", ignoreCase = true)) {
                                Icons.Default.CreditCard
                            } else {
                                Icons.Default.PhoneAndroid
                            },
                            contentDescription = null,
                            tint = if (record.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(record.provider, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "${if (record.type == TransactionType.INCOME) "+" else "-"}৳${String.format(Locale.US, "%,.2f", record.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (record.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = record.note.ifBlank { "Transaction via ${record.provider}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!record.trxId.isNullOrBlank()) {
                Text(
                    text = "TrxID: ${record.trxId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = onReview,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review & Approve")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSmsRecordDialog(
    record: SmsImportedRecordEntity,
    onDismiss: () -> Unit,
    onApprove: (categoryId: String, note: String) -> Unit,
    onReject: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(record.suggestedCategory) }
    var note by remember { mutableStateOf(record.note) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = if (record.type == TransactionType.INCOME) {
        CategoryCatalog.incomeCategories
    } else {
        CategoryCatalog.expenseCategories
    }

    val selectedCategory = categories.find { it.id == selectedCategoryId } ?: categories.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Payment Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Amount: ৳${String.format(Locale.US, "%,.2f", record.amount)} (${record.type.name})",
                    fontWeight = FontWeight.Bold,
                    color = if (record.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                )
                if (record.fee > 0) {
                    Text("Fee: ৳${String.format(Locale.US, "%.2f", record.fee)}", style = MaterialTheme.typography.labelSmall)
                }
                if (!record.trxId.isNullOrBlank()) {
                    Text("TrxID: ${record.trxId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                Text("Select Category:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)

                Box {
                    OutlinedButton(
                        onClick = { categoryDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCategory.name)
                    }
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Transaction Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApprove(selectedCategoryId, note) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Approve & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SupportedProvidersDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supported Payment SMS Providers", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The app parses transaction confirmation messages from:\n\n" +
                            "• bKash (Payment, Send Money, Cash Out, Cash In, Recharge, Pay Bill)\n" +
                            "• Nagad (Payment, Send Money, Cash Out, Cash In, Recharge)\n" +
                            "• Rocket / DBBL (Merchant Pay, Cash Out, Received, Transfers)\n" +
                            "• upay (UCB MFS Payment, Cash Out, Received)\n" +
                            "• tap (Trust Axiata Pay)\n" +
                            "• mCash (Islami Bank Bangladesh)\n" +
                            "• OK Wallet (ONE Bank MFS)\n" +
                            "• MyCash & SureCash\n" +
                            "• Commercial Banks & Debit/Credit Cards (City Bank, BRAC, EBL, SCB, DBBL, etc.)\n\n" +
                            "Note: OTPs, security alerts, and promotional messages are strictly ignored.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun BatteryOptimizationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery Optimization Architecture", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• Event-Driven Only: No continuous background scanning or polling.\n" +
                            "• Zero Wake Locks: No device wake locks or permanent background workers.\n" +
                            "• Fast Return: The receiver evaluates messages in <10ms and releases system resources immediately.\n" +
                            "• Pre-Filtering: Rejects non-financial messages before allocating background tasks.\n" +
                            "• Battery Impact: Practically negligible (~0.01% daily battery impact during normal SMS arrivals).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
