# Implementation Plan: Calendar Bottom Navigation, Daily Spending & Manual Backup/Restore

**Application:** Budget & Loan Manager  
**Target Architecture:** Kotlin, Jetpack Compose Material 3, Room v3, MVVM/UDF, Coroutines/Flow, Android SAF (Storage Access Framework)  
**Date:** October 4, 2026  

---

## 1. Executive Summary & Objective

This plan details the implementation of three key functional enhancements to the Daily Budget & Loan Manager Android application without breaking existing accounting models, database boundaries, or offline-first integrity:

1. **Bottom Navigation & Calendar Promotion:**
   * Restructure Bottom Navigation into 5 destinations: **Transactions**, **Budget**, **Dashboard** (center default), **Calendar**, and **Loans**.
   * Relocate the **More** destination to a top-right overflow menu in the TopAppBar, maintaining full access to Archive, Reports, Notifications, Settings, and Backup & Restore.
   * Provide a dedicated, responsive **CalendarScreen** featuring interactive month navigation, "Today" reset, 42-cell dot-indicator matrix, selected-day transaction list, and selected-day expense & daily limit comparison.

2. **Daily Spending Limit & Category Tracking:**
   * Reuse the existing `daily_budget_limit` setting stored in `budget_settings`.
   * Unify daily spending calculation (`remaining = dailyLimit - eligibleExpensesForThatDay`) with negative remaining amounts permitted when over-budget.
   * Maintain strict anti-double-counting invariants: ordinary expenses count once in daily and monthly totals; loan principal, collections, and repayments are isolated.

3. **Manual Backup & Restore (JSON via Storage Access Framework):**
   * Versioned logical JSON backup format (`BUDGET_AND_LOAN_MANAGER_BACKUP`, version 1).
   * Safe document creation via `ActivityResultContracts.CreateDocument("application/json")` outside app-private storage.
   * Safe document opening via `ActivityResultContracts.OpenDocument()`.
   * Comprehensive validation (schema, version, foreign keys, timestamps, and ranges) with a pre-restore summary preview.
   * Atomic database restoration inside a single Room `withTransaction` block with complete rollback on any error.

---

## 2. Existing Architecture & Invariants

* **Database Schema (Room Version 3):**
  * `transactions`: `id`, `type`, `amount`, `categoryId`, `categoryName`, `note`, `timestamp`, `loanId`, `archivedAt`.
  * `loans`: `id`, `type`, `personName`, `phoneNumber`, `initialAmount`, `remainingAmount`, `status`, `startDate`, `dueDate`, `note`, `archivedAt`.
  * `loan_repayments`: `id`, `loanId` (FK -> `loans.id` CASCADE), `amount`, `note`, `timestamp`, `archivedAt`.
  * `budget_settings`: `settingKey` (PK), `amountLimit`, `currencyCode`.
  * `budget_allocations`: `monthKey` + `categoryId` (Composite PK), `categoryName`, `allocatedAmount`, `updatedAt`.
* **Zero Schema Change Required:** All fields needed for daily limits and backup/restore already exist in v3.
* **Financial Invariants:**
  * Cash Balance = All Time Income - All Time Expense + All Time Borrowed - All Time Lent (excluding loan repayment ledger entries).
  * Ordinary Expenses exclude category `loan_repaid`.
  * Ordinary Incomes exclude category `loan_collected`.
  * Budget allocations are planning envelopes and do not deduct cash directly.
  * Archived records (`archivedAt != null`) are excluded from active totals, active calendar cells, and active ledger views.

---

## 3. Files Expected to Change / Be Created

| Component | Path | Action | Description |
|---|---|---|---|
| **Enums & State** | `com/engrshuvo/financemanager/ui/state/FinanceUiState.kt` | Modify | Update `FinanceTab` (5 tabs) and add `MoreSubDestination.BACKUP_RESTORE` |
| **Backup Data Model** | `com/engrshuvo/financemanager/data/model/FinanceBackupModels.kt` | Create | Data classes for JSON serialization & parsing with version metadata |
| **DAOs** | `com/engrshuvo/financemanager/data/local/FinanceDaos.kt` | Modify | Add direct export queries and bulk insert/clear methods for atomic restore |
| **Repository** | `com/engrshuvo/financemanager/data/repository/FinanceRepository.kt` | Modify | Add `exportBackupData()`, `validateBackupData()`, and `restoreBackupData()` inside `withTransaction` |
| **ViewModel** | `com/engrshuvo/financemanager/ui/viewmodel/FinanceViewModel.kt` | Modify | Add backup export/restore state, validation preview, and dispatch handlers |
| **Calendar Screen** | `com/engrshuvo/financemanager/ui/screens/CalendarScreen.kt` | Create | Dedicated first-class screen with month nav, calendar matrix, and selected-day summary |
| **Backup Screen** | `com/engrshuvo/financemanager/ui/screens/BackupRestoreScreen.kt` | Create | Dedicated UI for SAF file pickers, export, validation preview dialog, and restore execution |
| **Main Screen** | `com/engrshuvo/financemanager/ui/screens/MainFinanceScreen.kt` | Modify | Update BottomNavigation bar (5 items) and TopAppBar overflow menu for More destinations |
| **More Screen** | `com/engrshuvo/financemanager/ui/screens/MoreScreen.kt` | Modify | Add Backup & Restore destination card and integrate sub-screen routing |
| **Unit & Integration Tests** | `com/engrshuvo/financemanager/UiReliabilityAndPerformanceUnitTest.kt` | Modify | Add tests for navigation order, calendar integration, daily spending, JSON export, atomic restore, and rollback |

---

## 4. Detailed Stage-by-Stage Plan

### Stage 1: Bottom Navigation & Calendar Screen
* Update `FinanceTab` enum: `TRANSACTIONS`, `BUDGET`, `DASHBOARD`, `CALENDAR`, `LOANS`.
* In `MainFinanceScreen.kt`, update `NavigationBar` to place Dashboard in the center (position index 2) with Calendar at index 3.
* Add overflow action menu to `TopAppBar` with DropdownMenu directing to Archive, Reports, Notifications, Settings, and Backup & Restore.
* Create `CalendarScreen.kt` providing month header, "Today" button, `InteractiveCalendarView`, selected-day expense vs daily limit summary, and selected-day transactions list.

### Stage 2: Daily Spending Limit Integration
* Ensure `dailyBudgetLimit` is reactively loaded from `budget_settings` (`daily_budget_limit`).
* Ensure `dailyBudgetRemaining` (`dailyLimit - todayExpenses`) and `dailyBudgetProgress` are computed cleanly on `Dispatchers.Default`.
* Selected day in Calendar computes `dayExpense` and displays comparison against `dailyBudgetLimit`.

### Stage 3: Versioned Backup Export (SAF)
* Implement `FinanceBackupData` JSON serializer in `FinanceBackupModels.kt`.
* In `FinanceRepository`, read all tables and export structured JSON via `OutputStream` on `Dispatchers.IO`.
* In `BackupRestoreScreen.kt`, launch `ActivityResultContracts.CreateDocument("application/json")` suggesting `budget-manager-backup-YYYY-MM-DD.json`.

### Stage 4: Validated Atomic Restore (SAF)
* In `FinanceRepository`, read input stream from `ActivityResultContracts.OpenDocument()`.
* Validate version identifier, version <= 1, non-empty fields, valid enum values, and foreign key relations (`loanId` existence).
* Display preview dialog with record counts and replacement warning.
* On user confirmation, execute atomic wipe-and-reinsert in Room `database.withTransaction`.
* On failure, rollback automatically and report specific error.

### Stage 5: Verification & Automated Tests
* Execute `gradle :app:testDebugUnitTest`.
* Verify 100% pass rate on all accounting, navigation, backup, and restore tests.
* Execute `gradle :app:assembleDebug` and `gradle :app:assembleRelease`.

---

## 5. Rollback Considerations & Risk Mitigation

* **Restore Safety:** Restore uses a single atomic Room SQLite transaction. If an error occurs midway, SQLite automatically reverts all changes, leaving existing data untouched.
* **Foreign Key Integrity:** Re-insert order strictly respects foreign keys (`loans` before `loan_repayments`).
* **Offline-First:** All operations use local storage access without network or cloud dependencies.
