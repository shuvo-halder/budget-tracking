# AI Handoff & Technical Context

**Project:** Budget & Loan Manager  
**Package:** `com.engrshuvo.financemanager`  
**Current State:** v3 Room Database, Jetpack Compose Material 3, MVVM + UDF  
**Date:** October 4, 2026  

---

## 1. High-Level Summary

The Daily Budget & Loan Manager application is a 100% offline-first financial tracker built with Kotlin, Jetpack Compose Material 3, Room SQLite Database (Version 3), and Android WorkManager.

Key system capabilities:
1. **Financial Ledger & Operating Cash:** Tracks salary, additional income, ordinary living expenses, and personal debt agreements (Lent/Borrowed).
2. **Monthly Envelopes:** Month-specific category allocations with automatic draft pre-filling and planned shortfall warning banners.
3. **Daily Budget Ceiling:** Independent daily spending limit comparing today's ordinary expenses with visual over-budget indicators.
4. **Interactive Calendar Matrix:** 42-day calendar with Income (Green), Expense (Red), and Loan (Blue) dot indicators.
5. **Archive & Recovery:** 2-month soft-delete buffer with undo capability and automatic expired purge.
6. **Local Backup & Restore:** Versioned JSON export and validated atomic restore via Android Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).

---

## 2. Navigation Architecture

* **Bottom Navigation Destinations (5 Tabs):**
  1. `Transactions`: Searchable ledger with category and date filters.
  2. `Budget`: Monthly category allocations and envelope planning.
  3. `Dashboard` (Center - Default): Financial overview, balance, cash flows, daily spending summary, and recent activities.
  4. `Calendar`: Interactive monthly calendar, day expense inspection, and selected day ledger.
  5. `Loans`: Counterparty debt tracker with installment repayments and full settlement.
* **Top-Right Overflow Menu (`More`):**
  * Backup & Restore (`MoreSubDestination.BACKUP_RESTORE`)
  * Archive & Recovery (`MoreSubDestination.ARCHIVE`)
  * Reports & Analytics (`MoreSubDestination.REPORTS`)
  * Notifications & Reminders (`MoreSubDestination.NOTIFICATION_SETTINGS`)
  * Daily Spending Target (`DailyLimitDialog`)
  * Tools & Services Hub (`MoreSubDestination.NONE`)

---

## 3. Database & Concurrency Rules

* **Room Version:** 3 (No destructive migrations allowed).
* **Entities:** `TransactionEntity`, `LoanEntity`, `LoanRepaymentEntity`, `BudgetSettingEntity`, `BudgetAllocationEntity`.
* **Dispatchers:**
  * All database operations MUST use `Dispatchers.IO`.
  * All list transformations, aggregation math, and calendar matrices MUST run on `Dispatchers.Default`.
  * Main thread renders immutable `FinanceUiState` via `collectAsStateWithLifecycle()`.
* **Atomic Restore & Rollback:** `FinanceRepository.restoreBackupData` runs inside `database.withTransaction { ... }` ensuring all-or-nothing data replacement with zero partial corruption.
* **Backup Validation:** Strict schema and relational integrity validation in `FinanceBackupData.fromJsonString` parses loans first, enforces positive IDs, detects duplicate keys, rejects missing foreign key targets, and prevents orphan repayments even if the loans list is empty.
* **Concurrency Guards:** `FinanceViewModel` prevents overlapping concurrent backup export, file reading, and restoration executions.
* **Automated Test Coverage:** Verified with 68 automated unit and Robolectric tests in `app/src/test/`, including `BackupRestoreDataIntegrityTest.kt` verifying full database round-trip and fault-injected transaction rollback.

