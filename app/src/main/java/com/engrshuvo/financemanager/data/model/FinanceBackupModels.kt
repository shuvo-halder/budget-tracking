package com.engrshuvo.financemanager.data.model

import androidx.compose.runtime.Immutable
import com.engrshuvo.financemanager.ui.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class BackupSummaryPreview(
    val totalTransactions: Int,
    val activeTransactions: Int,
    val archivedTransactions: Int,
    val totalLoans: Int,
    val activeLoans: Int,
    val settledLoans: Int,
    val totalRepayments: Int,
    val totalAllocations: Int,
    val totalSettings: Int,
    val exportTimestamp: Long,
    val formattedDate: String
)

@Immutable
data class FinanceBackupData(
    val backupFormat: String = BACKUP_FORMAT_IDENTIFIER,
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val appVersion: String = "1.0",
    val databaseVersion: Int = 3,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val transactions: List<TransactionEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanRepayments: List<LoanRepaymentEntity> = emptyList(),
    val budgetSettings: List<BudgetSettingEntity> = emptyList(),
    val budgetAllocations: List<BudgetAllocationEntity> = emptyList()
) {
    companion object {
        const val BACKUP_FORMAT_IDENTIFIER = "BUDGET_AND_LOAN_MANAGER_BACKUP"
        const val CURRENT_BACKUP_VERSION = 1

        fun fromJsonString(jsonStr: String): FinanceBackupData {
            val root = try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JSON format: ${e.message}")
            }

            val format = root.optString("backupFormat")
            if (format != BACKUP_FORMAT_IDENTIFIER) {
                throw IllegalArgumentException("Unrecognized backup file format. Expected '$BACKUP_FORMAT_IDENTIFIER' but found '$format'.")
            }

            val version = root.optInt("backupVersion", -1)
            if (version < 1) {
                throw IllegalArgumentException("Invalid or missing backup version in backup file.")
            }
            if (version > CURRENT_BACKUP_VERSION) {
                throw IllegalArgumentException("Unsupported backup version ($version). This app version supports up to version $CURRENT_BACKUP_VERSION. Please update your app.")
            }

            val appVersion = root.optString("appVersion", "1.0")
            val databaseVersion = root.optInt("databaseVersion", 3)
            val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            // 1. Parse Transactions
            val txList = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val id = obj.optLong("id", 0L)
                    val typeStr = obj.optString("type")
                    val type = try {
                        TransactionType.valueOf(typeStr)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid transaction type '$typeStr' at record index $i.")
                    }
                    val amount = obj.optDouble("amount", 0.0)
                    if (amount <= 0.0) {
                        throw IllegalArgumentException("Transaction amount must be greater than 0 (found $amount at index $i).")
                    }
                    val categoryId = obj.optString("categoryId", "other")
                    val categoryName = obj.optString("categoryName", "Other")
                    val note = obj.optString("note", "")
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    val loanId = if (obj.has("loanId") && !obj.isNull("loanId")) obj.getLong("loanId") else null
                    val archivedAt = if (obj.has("archivedAt") && !obj.isNull("archivedAt")) obj.getLong("archivedAt") else null

                    txList.add(
                        TransactionEntity(
                            id = id,
                            type = type,
                            amount = amount,
                            categoryId = categoryId,
                            categoryName = categoryName,
                            note = note,
                            timestamp = timestamp,
                            loanId = loanId,
                            archivedAt = archivedAt
                        )
                    )
                }
            }

            // 2. Parse Loans
            val loanList = mutableListOf<LoanEntity>()
            val loanIdSet = mutableSetOf<Long>()
            if (root.has("loans")) {
                val loanArray = root.getJSONArray("loans")
                for (i in 0 until loanArray.length()) {
                    val obj = loanArray.getJSONObject(i)
                    val id = obj.optLong("id", 0L)
                    val typeStr = obj.optString("type")
                    val type = try {
                        LoanType.valueOf(typeStr)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid loan type '$typeStr' at record index $i.")
                    }
                    val personName = obj.optString("personName", "").trim()
                    if (personName.isBlank()) {
                        throw IllegalArgumentException("Person name in loan cannot be blank (record index $i).")
                    }
                    val phoneNumber = obj.optString("phoneNumber", "")
                    val initialAmount = obj.optDouble("initialAmount", 0.0)
                    if (initialAmount <= 0.0) {
                        throw IllegalArgumentException("Loan initial amount must be greater than 0 (record index $i).")
                    }
                    val remainingAmount = obj.optDouble("remainingAmount", initialAmount)
                    val statusStr = obj.optString("status", "ACTIVE")
                    val status = try {
                        LoanStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        LoanStatus.ACTIVE
                    }
                    val startDate = obj.optLong("startDate", System.currentTimeMillis())
                    val dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null
                    val note = obj.optString("note", "")
                    val archivedAt = if (obj.has("archivedAt") && !obj.isNull("archivedAt")) obj.getLong("archivedAt") else null

                    val loanEntity = LoanEntity(
                        id = id,
                        type = type,
                        personName = personName,
                        phoneNumber = phoneNumber,
                        initialAmount = initialAmount,
                        remainingAmount = remainingAmount.coerceIn(0.0, initialAmount),
                        status = status,
                        startDate = startDate,
                        dueDate = dueDate,
                        note = note,
                        archivedAt = archivedAt
                    )
                    loanList.add(loanEntity)
                    if (id > 0) loanIdSet.add(id)
                }
            }

            // 3. Parse Loan Repayments
            val repaymentList = mutableListOf<LoanRepaymentEntity>()
            if (root.has("loanRepayments")) {
                val repArray = root.getJSONArray("loanRepayments")
                for (i in 0 until repArray.length()) {
                    val obj = repArray.getJSONObject(i)
                    val id = obj.optLong("id", 0L)
                    val loanId = obj.optLong("loanId", -1L)
                    if (loanIdSet.isNotEmpty() && !loanIdSet.contains(loanId)) {
                        throw IllegalArgumentException("Repayment refers to non-existent loan ID $loanId at repayment index $i.")
                    }
                    val amount = obj.optDouble("amount", 0.0)
                    if (amount <= 0.0) {
                        throw IllegalArgumentException("Repayment amount must be positive at repayment index $i.")
                    }
                    val note = obj.optString("note", "")
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    val archivedAt = if (obj.has("archivedAt") && !obj.isNull("archivedAt")) obj.getLong("archivedAt") else null

                    repaymentList.add(
                        LoanRepaymentEntity(
                            id = id,
                            loanId = loanId,
                            amount = amount,
                            note = note,
                            timestamp = timestamp,
                            archivedAt = archivedAt
                        )
                    )
                }
            }

            // 4. Parse Budget Settings
            val settingsList = mutableListOf<BudgetSettingEntity>()
            if (root.has("budgetSettings")) {
                val setArray = root.getJSONArray("budgetSettings")
                for (i in 0 until setArray.length()) {
                    val obj = setArray.getJSONObject(i)
                    val key = obj.optString("settingKey", "")
                    if (key.isNotBlank()) {
                        val amountLimit = obj.optDouble("amountLimit", 0.0)
                        val currencyCode = obj.optString("currencyCode", "BDT")
                        settingsList.add(
                            BudgetSettingEntity(
                                settingKey = key,
                                amountLimit = amountLimit.coerceAtLeast(0.0),
                                currencyCode = currencyCode
                            )
                        )
                    }
                }
            }

            // 5. Parse Budget Allocations
            val allocationsList = mutableListOf<BudgetAllocationEntity>()
            if (root.has("budgetAllocations")) {
                val allocArray = root.getJSONArray("budgetAllocations")
                for (i in 0 until allocArray.length()) {
                    val obj = allocArray.getJSONObject(i)
                    val monthKey = obj.optString("monthKey", "")
                    val categoryId = obj.optString("categoryId", "")
                    val categoryName = obj.optString("categoryName", "")
                    val allocatedAmount = obj.optDouble("allocatedAmount", 0.0)
                    val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

                    if (monthKey.isNotBlank() && categoryId.isNotBlank()) {
                        allocationsList.add(
                            BudgetAllocationEntity(
                                monthKey = monthKey,
                                categoryId = categoryId,
                                categoryName = categoryName.ifBlank { categoryId },
                                allocatedAmount = allocatedAmount.coerceAtLeast(0.0),
                                updatedAt = updatedAt
                            )
                        )
                    }
                }
            }

            return FinanceBackupData(
                backupFormat = format,
                backupVersion = version,
                appVersion = appVersion,
                databaseVersion = databaseVersion,
                exportTimestamp = exportTimestamp,
                transactions = txList,
                loans = loanList,
                loanRepayments = repaymentList,
                budgetSettings = settingsList,
                budgetAllocations = allocationsList
            )
        }
    }

    fun toJsonString(): String {
        val root = JSONObject()
        root.put("backupFormat", backupFormat)
        root.put("backupVersion", backupVersion)
        root.put("appVersion", appVersion)
        root.put("databaseVersion", databaseVersion)
        root.put("exportTimestamp", exportTimestamp)

        // Transactions
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("categoryId", tx.categoryId)
            obj.put("categoryName", tx.categoryName)
            obj.put("note", tx.note)
            obj.put("timestamp", tx.timestamp)
            if (tx.loanId != null) obj.put("loanId", tx.loanId)
            if (tx.archivedAt != null) obj.put("archivedAt", tx.archivedAt)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Loans
        val loanArray = JSONArray()
        loans.forEach { loan ->
            val obj = JSONObject()
            obj.put("id", loan.id)
            obj.put("type", loan.type.name)
            obj.put("personName", loan.personName)
            obj.put("phoneNumber", loan.phoneNumber)
            obj.put("initialAmount", loan.initialAmount)
            obj.put("remainingAmount", loan.remainingAmount)
            obj.put("status", loan.status.name)
            obj.put("startDate", loan.startDate)
            if (loan.dueDate != null) obj.put("dueDate", loan.dueDate)
            obj.put("note", loan.note)
            if (loan.archivedAt != null) obj.put("archivedAt", loan.archivedAt)
            loanArray.put(obj)
        }
        root.put("loans", loanArray)

        // Loan Repayments
        val repArray = JSONArray()
        loanRepayments.forEach { rep ->
            val obj = JSONObject()
            obj.put("id", rep.id)
            obj.put("loanId", rep.loanId)
            obj.put("amount", rep.amount)
            obj.put("note", rep.note)
            obj.put("timestamp", rep.timestamp)
            if (rep.archivedAt != null) obj.put("archivedAt", rep.archivedAt)
            repArray.put(obj)
        }
        root.put("loanRepayments", repArray)

        // Budget Settings
        val setArray = JSONArray()
        budgetSettings.forEach { set ->
            val obj = JSONObject()
            obj.put("settingKey", set.settingKey)
            obj.put("amountLimit", set.amountLimit)
            obj.put("currencyCode", set.currencyCode)
            setArray.put(obj)
        }
        root.put("budgetSettings", setArray)

        // Budget Allocations
        val allocArray = JSONArray()
        budgetAllocations.forEach { alloc ->
            val obj = JSONObject()
            obj.put("monthKey", alloc.monthKey)
            obj.put("categoryId", alloc.categoryId)
            obj.put("categoryName", alloc.categoryName)
            obj.put("allocatedAmount", alloc.allocatedAmount)
            obj.put("updatedAt", alloc.updatedAt)
            allocArray.put(obj)
        }
        root.put("budgetAllocations", allocArray)

        return root.toString(2)
    }

    fun getSummaryPreview(): BackupSummaryPreview {
        val activeTxs = transactions.count { it.archivedAt == null }
        val archivedTxs = transactions.count { it.archivedAt != null }
        val activeLoans = loans.count { it.status == LoanStatus.ACTIVE && it.archivedAt == null }
        val settledLoans = loans.count { it.status == LoanStatus.SETTLED || it.archivedAt != null }

        return BackupSummaryPreview(
            totalTransactions = transactions.size,
            activeTransactions = activeTxs,
            archivedTransactions = archivedTxs,
            totalLoans = loans.size,
            activeLoans = activeLoans,
            settledLoans = settledLoans,
            totalRepayments = loanRepayments.size,
            totalAllocations = budgetAllocations.size,
            totalSettings = budgetSettings.size,
            exportTimestamp = exportTimestamp,
            formattedDate = DateUtils.formatHeaderDate(exportTimestamp)
        )
    }
}
