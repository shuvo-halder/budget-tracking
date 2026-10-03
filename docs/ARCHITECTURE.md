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
│                 Room Database (v2)                     │
│   (AppDatabase, TransactionDao, LoanDao,               │
│    BudgetSettingDao, BudgetAllocationDao)              │
└────────────────────────────────────────────────────────┘
```

---

## 3. Database Schema (Room Version 2)

The database is defined in `com.engrshuvo.financemanager.data.local.AppDatabase`:

### 1. `budget_allocations` Table *(Added in v2)*
Stores month-specific category allocation plans.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `monthKey` | `TEXT` | `String` | Part of Composite Primary Key, Indexed (`"yyyy-MM"`) |
| `categoryId` | `TEXT` | `String` | Part of Composite Primary Key (e.g. `"housing"`, `"daily_expenses"`) |
| `categoryName` | `TEXT` | `String` | Human-readable category label |
| `allocatedAmount` | `REAL` | `Double` | Planned target budget in BDT |
| `updatedAt` | `INTEGER` | `Long` | Timestamp of last modification |

### 2. `transactions` Table
Stores all actual financial transaction records.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate |
| `type` | `TEXT` | `TransactionType` | Enum: `INCOME`, `EXPENSE`, `LOAN` |
| `amount` | `REAL` | `Double` | Transaction monetary value in BDT |
| `categoryId` | `TEXT` | `String` | Category identifier |
| `categoryName` | `TEXT` | `String` | Display name of the category |
| `note` | `TEXT` | `String` | User notes or description |
| `timestamp` | `INTEGER` | `Long` | Epoch timestamp in milliseconds |
| `loanId` | `INTEGER` | `Long?` | Optional reference to linked loan |

### 3. `loans` Table
Stores counterparty debt records.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate |
| `type` | `TEXT` | `LoanType` | Enum: `LENT` (Receivable) or `BORROWED` (Payable) |
| `personName` | `TEXT` | `String` | Counterparty full name |
| `phoneNumber` | `TEXT` | `String` | Contact phone number |
| `initialAmount`| `REAL` | `Double` | Principal loan value |
| `remainingAmount`| `REAL` | `Double` | Outstanding debt balance |
| `status` | `TEXT` | `LoanStatus` | Enum: `ACTIVE` or `SETTLED` |
| `startDate` | `INTEGER` | `Long` | Issuance timestamp |
| `dueDate` | `INTEGER` | `Long?` | Repayment deadline |
| `note` | `TEXT` | `String` | Supplementary terms |

### 4. `loan_repayments` Table
Tracks installment payments against loans.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate |
| `loanId` | `INTEGER` | `Long` | Foreign Key references `loans(id)` on `CASCADE` delete |
| `amount` | `REAL` | `Double` | Repayment amount |
| `note` | `TEXT` | `String` | Payment notes |
| `timestamp` | `INTEGER` | `Long` | Epoch timestamp |

### 5. `budget_settings` Table
Key-value storage for overall user settings (`monthly_budget_limit`, `daily_budget_limit`).

---

## 4. Non-Destructive Room Migration Strategy

To ensure zero user data loss during schema evolution from version 1 to 2, the app registers an explicit migration:

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
    ├── NavigationBar (Dashboard [Default], Calendar, Loans)
    ├── FloatingActionButton (Universal Add Entry)
    │
    ├── AnimatedContent (Tab Transitions)
    │   ├── [DASHBOARD TAB - DEFAULT]
    │   │   ├── MonthSelectorHeader (< October 2026 >)
    │   │   ├── PlannedShortfallAlertBanner (Conditional when allocated > income)
    │   │   ├── SalaryFinancialHeroCard (Available Cash, Monthly Cash Flow, Allocations, Debts)
    │   │   ├── TodaysSpendingCard (Today's Expense vs Daily Limit, Progress, Overspend Tag)
    │   │   ├── QuickActionsRow (+ Salary/Income, - Expense)
    │   │   ├── MonthlyBudgetCategorySection (Rent, Family, Daily, Bills, Savings, etc.)
    │   │   ├── CategorySpendChart (Donut chart of actual expenses)
    │   │   └── FilterChipsBar & TransactionItemCard List
    │   │
    │   ├── [CALENDAR TAB]
    │   │   ├── InteractiveCalendarView (Header, Day-of-Week, 42-day dot grid)
    │   │   └── Selected Day Activity Cards
    │   │
    │   └── [LOANS TAB]
    │       ├── LoanOverviewHeader (Receivables vs Payables)
    │       └── Loans List (Progress, Repayments, Settlement)
    │
    └── Modal Sheets & Dialogs
        ├── BudgetPlanningDialog (Month category allocation planner & Copy Last Month action)
        ├── DailyLimitDialog (Daily spending limit configuration & Monthly suggestion helper)
        ├── UniversalTransactionSheet (Salary/Income, Expense, and Loan creation)
        └── AddRepaymentDialog (Loan installment payment)
```
