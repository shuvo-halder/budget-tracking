# Changelog

All notable changes to the **Budget & Loan Manager** application are documented in this file.

---

## [Unreleased] - 2026-10-30

### Added
* **Expense–Budget Category Consistency & Mobile UI Text Overflow Polish (Phase 20.6):**
  * Synchronized all 16 canonical expense categories (`housing`, `family`, `daily_expenses`, `food`, `grocery`, `bills`, `transport`, `savings`, `emergency`, `shopping`, `health`, `education`, `entertainment`, `fitness`, `personal_refreshments`, `other_expense`) across CategoryCatalog, Budget screen, Monthly Budget Allocation, and Budget Planning Dialog.
  * Resolved category inconsistency where 7 expense categories were omitted from budget planning unless manually allocated.
  * Preserved unallocated category invariant: unallocated categories start with `allocatedAmount = 0.0`, reflecting actual spending without distorting `totalAllocated`, `unallocatedIncome`, or `plannedShortfall`.
  * Preserved non-expense category isolation: Income and Loan categories remain strictly excluded from budget planning envelopes.
  * Eliminated text clipping and overflow across mobile viewports:
    * `MoreScreen.kt`: Utilized `FlowRow` for title and badge, added `lineHeight = 18.sp` and `softWrap = true` to subtitle text.
    * `BudgetScreen.kt`: Added `FlowRow` and responsive padding to Shortfall Alert, Summary cards (Monthly Income, Total Allocated, Shortfall/Unallocated), Daily Living Limit card, and Category Allocation cards.
    * `DashboardOverviewView.kt`: Added `FlowRow` for category title and over-budget badge, responsive weight and wrapping to `MiniStatPill`, and `softWrap = true` on hero balance and shortfall alerts.
    * `BudgetPlanningDialog.kt`: Added `softWrap = true` and vertical alignments across header summary, shortfall banner, copy button, and category rows.
    * `ReportsScreen.kt`: Updated `ReportMetricCard`, cash flow/balance summary cards, and `BudgetItemReportRow` with `softWrap = true`, `maxLines = 2`, and responsive column spacing.
    * `ModernBottomNavigationBar.kt`: Distributed 5 navigation tabs evenly with `Modifier.weight(1f)` and compact padding, preventing tab squeeze and text truncation.
  * Added `CategoryBudgetConsistencyUnitTest.kt` with 5 unit tests validating 16-category coverage, non-expense isolation, and zero-allocation calculation invariants.

### Added
* **Mobile Banking SMS Approval Decoupling & SMS History Screen (Phase 20.5):**
  * Decoupled mobile banking SMS approval from the financial ledger: approving an SMS message updates its status to `APPROVED` and strictly avoids modifying Main Balance, Income, Expense, Budgets, Dashboard, or Reports.
  * Created dedicated `MobileBankingSmsHistoryScreen.kt` featuring provider filter chips (bKash, Nagad, Rocket, DBBL, Banks), search input, summary inflow/outflow metrics, BDT formatted amounts (`৳`), click-to-copy TrxID, and single-record deletion.
  * Added `MoreSubDestination.SMS_HISTORY` accessible via the More hub ("Automated Imports" section) and directly from `PaymentSmsImportScreen` via top-bar action and quick link card.
  * Added comprehensive unit test coverage verifying zero ledger bleed, balance invariance, and backward compatibility with legacy records.
* **Bottom Navigation & Calendar Promotion:**
  * Added `FinanceTab.CALENDAR` as a primary bottom navigation destination.
  * Reordered bottom navigation bar: 1. Transactions, 2. Budget, 3. Dashboard (center default), 4. Calendar, 5. Loans.
  * Created dedicated `CalendarScreen.kt` providing interactive month navigation, "Today" quick-reset, 42-day dot-indicator matrix, and selected-day summary comparing daily spending with the daily budget ceiling.
  * Added top-right overflow action menu in `MainFinanceScreen` to access More destinations (Backup & Restore, Archive, Reports, Notifications, and Daily Limit).
* **Manual Backup & Restore (JSON via Storage Access Framework):**
  * Created `FinanceBackupModels.kt` containing versioned JSON serializer & parser with schema version metadata (`BUDGET_AND_LOAN_MANAGER_BACKUP`, version 1).
  * Added `BackupRestoreScreen.kt` for local document export (`ACTION_CREATE_DOCUMENT`) and validated restore (`ACTION_OPEN_DOCUMENT`).
  * Implemented pre-restore summary preview showing record counts for active/archived transactions, loans, repayments, and budget allocations.
  * Implemented atomic database restore in `FinanceRepository` inside Room's `withTransaction` block, guaranteeing rollback on any error or foreign key violation.
  * Added DAO queries and bulk insert/clear methods across all Room DAOs.
* **Backup/Restore Data-Integrity Test Suite (Phase 6):**
  * Added `BackupRestoreDataIntegrityTest.kt` with 10 comprehensive unit tests.
  * Tested complete full-database JSON export/import round-trip comparing all 5 Room entities, null fields, and archive timestamps field-by-field.
  * Tested atomic restore rollback using a fault-injecting DAO inside real Room `withTransaction`, verifying 100% preservation of pre-restore snapshot.
  * Strengthened relationship and foreign key validation: rejected orphan repayments when loans collection is empty, validated transaction loan references, and enforced duplicate ID rejection.
  * Added concurrency guards in `FinanceViewModel` to prevent concurrent export/restore executions.

### Changed
* Updated `FinanceUiState` to include `MoreSubDestination.BACKUP_RESTORE` and backup preview state.
* Enhanced `FinanceViewModel` with asynchronous backup export, validation, and atomic restoration methods.

### Preserved
* Room Database version 3 preserved with zero destructive migrations.
* All financial accounting rules, anti-double-counting safeguards, loan isolation, and 2-month archive auto-purge rules remain strictly preserved.
