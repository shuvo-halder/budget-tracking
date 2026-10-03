# System Architecture & Technical Specifications

This document details the architectural patterns, financial accounting models, data persistence layer, concurrency strategy, and user interface structure of the **Salary-Based Personal Finance & Budget Management System**.

---

## 1. Implementation Status

| Feature / Module | Status | Description |
|---|---|---|
| **Default Dashboard Landing** | ✅ Completed | The app launches directly into the comprehensive financial dashboard |
| **Salary & Income Tracking** | ✅ Completed | Monthly salary and additional income streams (bonus, freelance, business) |
| **Month-Specific Allocations** | ✅ Completed | Category allocations (Rent, Family, Daily, Bills, Savings, etc.) stored per month |
| **Non-Destructive Room Migration** | ✅ Completed | Database upgraded to v2 via `MIGRATION_1_2` preserving all historical records |
| **Pre-Fill Draft Allocations** | ✅ Completed | Unconfigured months automatically pre-fill previous month targets as drafts |
| **Planned Shortfall Alert Banner** | ✅ Completed | Visual alert banner when planned allocations exceed recorded monthly income |
| **Independent Daily Spending Limit** | ✅ Completed | Today's expenses tracked against a configurable limit with optional 1-tap suggestion |
| **Strict Anti-Double-Counting** | ✅ Completed | Allocations do not deduct cash; expenses reduce cash and exactly one category budget |
| **Planned Savings Separation** | ✅ Completed | Savings and emergency reserves are earmarked plans, never counted as expenses |
| **Loan Accounting Isolation** | ✅ Completed | Personal debts (Lent/Borrowed) remain separated from operating cash flows |
| **Interactive Monthly Calendar** | ✅ Completed | 42-day calendar matrix with color dots for Income, Expense, and Loan activity |
| **Daily Activity Bottom Sheet** | ✅ Completed | Daily transaction inspection, aggregation, and inline entry creation |
| **Background Thread Offloading** | ✅ Completed | `Dispatchers.IO` for database operations, `Dispatchers.Default` for computations |
| **Robolectric & Unit Tests** | ✅ Completed | Automated verification of financial formulas, allocations, and loan boundaries |
| **GitHub Actions CI/CD** | ✅ Completed | Automated release compilation, zipalign, and apksigner with keystore secrets |
| **CSV / Excel Data Export** | ⏳ Roadmap / Pending | Export transaction records and loan statements to CSV or Excel files |
| **Biometric App Lock** | ⏳ Roadmap / Pending | Optional fingerprint / face unlock for privacy protection |

---

## 2. Core Financial Accounting Architecture

To prevent double-counting and misrepresentation of funds, the system establishes clear accounting boundaries between:
1. **Actual Income:** Real cash inflow (Salary, Bonus, Freelance, Business).
2. **Actual Expenses:** Real cash outflows (Rent payment, Grocery purchase, Electricity bill).
3. **Budget Allocations:** Financial plans/envelopes. They plan how salary is allocated but **never** deduct cash directly.
4. **Daily Spending Limit:** Independent daily target ceiling. An expense today reduces both the today-limit remaining and the monthly category remaining, and cash—counted once, never duplicated.
5. **Planned Savings / Reserves:** Earmarked goals. Not treated as expenses or cash reductions unless an explicit external transfer occurs.
6. **Loans (Lent / Borrowed):** Balance sheet assets (receivables) and liabilities (payables). Loan repayments and collections do not inflate ordinary living expenses or living income.

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (Default: Dashboard, Calendar, Loans, Dialogs)       │
└──────────────────────────▲─────────────────────────────┘
                           │ StateFlow<FinanceUiState>
                           │ User Events (lambdas)
┌──────────────────────────┴─────────────────────────────┐
│                    FinanceViewModel                    │
│   - Bounded monthly date intervals [start, end]        │
│   - Category remaining & usage percentage math         │
│   - Today's expense aggregation & daily budget math    │
│   - Previous month allocation pre-fill resolution      │
│   - Runs on Dispatchers.Default                        │
└──────────────────────────▲─────────────────────────────┘
                           │ Flow<List<T>> & Suspend calls
┌──────────────────────────┴─────────────────────────────┐
│                   FinanceRepository                    │
│   - Enforces Dispatchers.IO on all database accesses   │
│   - Transactions, Loans, BudgetSettings, Allocations   │
└──────────────────────────▲─────────────────────────────┘
                           │ Room DAOs
┌──────────────────────────┴─────────────────────────────┐
│                 Room Database (v3)                     │
│   (AppDatabase, TransactionDao, LoanDao,               │
│    BudgetSettingDao, BudgetAllocationDao)              │
└────────────────────────────────────────────────────────┘
```

---

## 3. Database Schema (Room Version 3)

The database is defined in `com.engrshuvo.financemanager.data.local.AppDatabase`:

### 1. `budget_allocations` Table *(Added in v2)*
Stores month-specific category allocation plans.
Primary Key: `(monthKey, categoryId)`, Index: `monthKey`.

### 2. `transactions` Table *(Updated in v3)*
Stores all actual financial transaction records.
Columns: `id`, `type`, `amount`, `categoryId`, `categoryName`, `note`, `timestamp`, `loanId`, `archivedAt` (Nullable Long, Indexed).

### 3. `loans` Table *(Updated in v3)*
Stores counterparty debt records.
Columns: `id`, `type`, `personName`, `phoneNumber`, `initialAmount`, `remainingAmount`, `status`, `startDate`, `dueDate`, `note`, `archivedAt` (Nullable Long, Indexed).

### 4. `loan_repayments` Table *(Updated in v3)*
Tracks installment payments against loans.
Columns: `id`, `loanId`, `amount`, `note`, `timestamp`, `archivedAt` (Nullable Long, Indexed).

### 5. `budget_settings` Table
Key-value storage for overall user settings (`monthly_budget_limit`, `daily_budget_limit`).

---

## 4. Non-Destructive Room Migration Strategy

To ensure zero user data loss during schema evolution, the app registers explicit migrations:

1. **`MIGRATION_1_2` (v1 → v2)**: Creates `budget_allocations` table and index.
2. **`MIGRATION_2_3` (v2 → v3)**: Adds nullable `archivedAt INTEGER DEFAULT NULL` columns to `transactions`, `loans`, and `loan_repayments`, and creates indexing on each `archivedAt` column. Active records default to null.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budget_allocations` (
                `monthKey` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `categoryName` TEXT NOT NULL,
                `allocatedAmount` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`monthKey`, `categoryId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_allocations_monthKey` ON `budget_allocations` (`monthKey`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `loans` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `loan_repayments` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_archivedAt` ON `transactions` (`archivedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_archivedAt` ON `loans` (`archivedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_repayments_archivedAt` ON `loan_repayments` (`archivedAt`)")
    }
}
```
            CREATE TABLE IF NOT EXISTS `budget_allocations` (
                `monthKey` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `categoryName` TEXT NOT NULL,
                `allocatedAmount` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`monthKey`, `categoryId`)
            )
        """.trimIndent())
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_budget_allocations_monthKey` ON `budget_allocations` (`monthKey`)"
        )
    }
}
```

---

## 5. Threading & Concurrency Strategy

- **`Dispatchers.IO`**: All Room DAO queries, inserts, updates, and deletes are executed on `Dispatchers.IO` through `flowOn(Dispatchers.IO)` and `withContext(Dispatchers.IO)`.
- **`Dispatchers.Default`**: Complex calculations (month boundary filtering, category aggregations, calendar 42-cell matrix generation, and percentage calculations) run on the background CPU dispatcher.
- **Main (UI) Thread**: Renders immutable `FinanceUiState` via `collectAsStateWithLifecycle()` without performing blocking calculations.

---

## 6. UI Component Hierarchy

```
MainActivity
└── MainFinanceScreen (Default Tab: FinanceTab.DASHBOARD)
    ├── TopAppBar (Title, Category Subtitle, Actions)
    ├── NavigationBar (Transactions, Budget, Dashboard [Center - Default], Loans, More [Rightmost])
    ├── FloatingActionButton (Universal Add Entry)
    │
    ├── AnimatedContent (Tab Transitions)
    │   ├── [DASHBOARD TAB - CENTER DEFAULT]
    │   │   ├── MonthSelectorHeader (< October 2026 >)
    │   │   ├── PlannedShortfallAlertBanner (Conditional when allocated > income)
    │   │   ├── SalaryFinancialHeroCard (Available Cash, Monthly Cash Flow, Allocations, Debts)
    │   │   ├── TodaysSpendingCard (Today's Expense vs Daily Limit, Progress, Overspend Tag)
    │   │   ├── QuickActionsRow (+ Salary/Income, - Expense)
    │   │   ├── MonthlyBudgetCategorySection (Rent, Family, Daily, Bills, Savings, etc.)
    │   │   ├── CategorySpendChart (Donut chart of actual expenses)
    │   │   └── FilterChipsBar & TransactionItemCard List
    │   │
    │   ├── [TRANSACTIONS TAB]
    │   │   ├── Filter & Search Controls (Income, Expense, Category, Date range)
    │   │   └── Transaction Ledger List (Item details, Swipe-to-archive, Edit, Undo)
    │   │
    │   ├── [BUDGET TAB]
    │   │   ├── Month Selector Header (< October 2026 >)
    │   │   ├── Planned Shortfall Alert Banner
    │   │   ├── Budget Planning Overview Card (Income vs Planned Targets)
    │   │   ├── Daily Living Limit Ceiling & Progress Indicator
    │   │   └── Category Allocations List (Planned vs Actual, Usage Bar, Copy Last Month)
    │   │
    │   ├── [LOANS TAB]
    │   │   ├── LoanOverviewHeader (Receivables vs Payables)
    │   │   └── Loans List (Progress, Installment Repayments, Full Settlement, Archive)
    │   │
    │   └── [MORE TAB - RIGHTMOST]
    │       ├── MoreScreen Hub (Interactive Calendar, Archive & Recovery, Reports & Analytics)
    │       ├── InteractiveCalendarView (42-day dot grid & Day activity sheet)
    │       ├── ArchiveScreen (Soft-deleted records, Search, Filter, Restore, Permanent Purge)
    │       ├── ReportsView (Donut chart, Cash flow ratios, Planned savings reserve)
    │       └── Privacy & 100% Offline-first local Room v3 data information
    │
    └── Modal Sheets & Dialogs
        ├── BudgetPlanningDialog (Month category allocation planner & Copy Last Month action)
        ├── DailyLimitDialog (Daily spending limit configuration & Monthly suggestion helper)
        ├── UniversalTransactionSheet (Salary/Income, Expense, and Loan creation)
        └── AddRepaymentDialog (Loan installment payment)
```
