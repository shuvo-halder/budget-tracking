# Professional Finance App UX + Archive & Recovery System

Implement a professional, multi-screen Android personal finance application with a 5-destination navigation bar, dedicated feature screens, and an offline-first **Archive, Restore, Undo, and 2-Month Auto-Purge** system.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed in Phase 1:
> 1. **Secondary Navigation ("More" Tab)**: Dedicated "More" tab screen in the bottom navigation bar listing **Calendar**, **Archive & Recovery**, and **Reports & Analytics**.
> 2. **Loan Archive Strategy**: Cascade archive and restore — archiving a loan also archives its linked transaction and repayment entries; restoring the loan restores all linked records together cleanly.
> 3. **Archive Deletion Policy**: Item-by-item permanent deletion with an explicit confirmation dialog (no bulk wipe button).

---

## 1. Architecture & Soft-Delete Data Model

### Non-Destructive Room Migration (Version 2 -> Version 3)
Add an indexed nullable `archivedAt: Long?` timestamp to `transactions`, `loans`, and `loan_repayments`:
- **Active Record:** `archivedAt == null`
- **Archived Record:** `archivedAt != null` (stores epoch millis of when it was archived)
- **Restored Record:** Set `archivedAt = null`

```sql
-- Migration 2 -> 3
ALTER TABLE `transactions` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL;
ALTER TABLE `loans` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL;
ALTER TABLE `loan_repayments` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `index_transactions_archivedAt` ON `transactions` (`archivedAt`);
CREATE INDEX IF NOT EXISTS `index_loans_archivedAt` ON `loans` (`archivedAt`);
```
*Existing records remain 100% active with `archivedAt = NULL`.*

---

## 2. Archive, Restore, Undo & Auto-Purge Engine

### A. Soft-Delete & Undo Flow
1. User swipes-to-delete or taps Delete on a transaction or loan.
2. The repository updates `archivedAt = System.currentTimeMillis()`. For loans, its linked transaction and repayments are soft-deleted atomically.
3. The record disappears immediately from active flows, dashboards, category allocations, and calendar grids.
4. A Material 3 Snackbar appears with an **"Undo"** action.
5. Tapping **Undo** sets `archivedAt = null`, restoring the item and its active calculations exactly once.

### B. Two-Calendar-Month Auto-Purge Rule
- Retention limit is calculated using **calendar-month arithmetic**, not a naive fixed 60 days:
  ```kotlin
  fun calculateExpirationTimestamp(archivedAt: Long): Long {
      val cal = Calendar.getInstance().apply {
          timeInMillis = archivedAt
          add(Calendar.MONTH, 2) // Safe calendar month arithmetic (handles 28/29/30/31 days)
      }
      return cal.timeInMillis
  }
  ```
- **Purge Execution**:
  - Run automatically on app startup, on app resume, and whenever opening the Archive screen.
  - Optional `PeriodicWorkRequest` using `WorkManager` for periodic background execution.
  - Idempotent Room transaction: permanently deletes records where `archivedAt <= threshold`.

### C. Dedicated Archive Screen
- Filter chips: **All**, **Income**, **Expenses**, **Loans**.
- Real-time search query filtering.
- Visual retention badge on each card: e.g. *"Permanently deleted in 42 days"*.
- **Restore Action**: Restores record to active state with instant Snackbar confirmation.
- **Delete Permanently Action**: Shows a non-cancellable confirmation dialog warning that this action is irreversible.

---

## 3. Professional Navigation & Dedicated Screens

Update `FinanceTab` to support 5 primary destinations in the bottom `NavigationBar`:
1. 🏠 **Dashboard**: High-level financial health: Available cash, monthly cash flow, today's spending vs. daily limit, planned shortfall alerts, and recent transactions.
2. 💳 **Transactions**: Dedicated full-screen transaction ledger with search, multi-criteria filters (Type, Category, Date), "+ Income", "- Expense", click-to-edit, and swipe-to-archive.
3. 📊 **Budget**: Dedicated salary envelope budgeting screen: Monthly category allocations (Rent, Family, Daily Expenses, Food, Groceries, Bills, Transport, Savings, Emergency), live shortfall tracking, and independent daily limit configuration with 1-tap suggestions.
4. 🤝 **Loans**: Counterparty debt tracker: Lent receivables, borrowed payables, partial repayments, full settlement, and loan archiving.
5. 📂 **More**: Professional hub navigating to:
   - 📅 **Interactive Calendar** (full 42-day dot grid and day activity sheet).
   - 🗄️ **Archive & Recovery** (dedicated restore/purge screen).
   - 📈 **Reports & Breakdown** (category donut chart and monthly savings reserve summaries).

---

## 4. Financial Accounting Invariants (Anti-Double-Counting)

- **Archived records** are strictly filtered out of:
  - Active balance & net operating cash change (`archivedAt IS NULL`).
  - Monthly income and monthly expenses.
  - Category actual spent and remaining allocation math.
  - Today's spending and daily limit calculations.
  - Calendar daily indicators and loan totals.
- **Restored records** re-enter active calculations exactly once.
- **Planned allocations** remain financial plans, never cash deductions.

---

## 5. Verification Plan

1. **Automated Unit Tests (`ArchiveAndAccountingTest.kt`)**:
   - Soft-delete marks `archivedAt` without destroying row.
   - Archived transaction excluded from active streams and included in archive stream.
   - Undo restores record to active state.
   - Archived loan cascades `archivedAt` to repayments and linked transaction.
   - Restoring a loan cascades restore to repayments and linked transaction.
   - Purge removes expired records (`archivedAt <= 2 months ago`) and preserves non-expired records.
   - Calendar-month math handles end-of-month and February leap/non-leap boundaries.
   - Existing salary accounting, shortfall alerts, and independent daily limit tests pass.
2. **Build Verification**:
   - Run `gradle :app:testDebugUnitTest`.
   - Run `compile_applet` to verify compilation and live streaming preview.
