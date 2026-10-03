# System Architecture & Technical Specifications

This document outlines the architecture, data persistence layer, concurrency model, and user interface structure of the **Daily Budget & Loan Manager** Android application.

---

## 1. Implementation Status

| Feature / Module | Status | Description |
|---|---|---|
| **Room Local Persistence** | ✅ Completed | SQLite database with KSP code generation, foreign keys, and reactive streams |
| **Interactive Monthly Calendar** | ✅ Completed | 42-day calendar matrix with color dots for Income, Expense, and Loan activity |
| **Daily Activity Bottom Sheet** | ✅ Completed | Day summary stats (Income/Expense/Loan), transaction list, and quick entry |
| **Financial Dashboard** | ✅ Completed | Balance hero card, debts summary, animated category donut chart, and filter chips |
| **Budget Limit Tracking** | ✅ Completed | Configurable monthly budget ceiling with dynamic color-coded progress indicator |
| **Loan & Debt Tracker** | ✅ Completed | Lent/Borrowed management, partial repayments, full settlement, and overdue alerts |
| **Universal Transaction Entry** | ✅ Completed | Unified Modal BottomSheet for Income, Expense, and Loans with categories & notes |
| **Swipe-to-Delete & Undo** | ✅ Completed | Swipe-to-dismiss support on transaction cards with Snackbar undo action |
| **Background Thread Offloading** | ✅ Completed | `Dispatchers.IO` for database operations, `Dispatchers.Default` for computations |
| **Compose Recomposition Optimization** | ✅ Completed | `@Immutable` models, `derivedStateOf`, memoization, and stable `LazyColumn` keys |
| **GitHub Actions CI/CD** | ✅ Completed | Automated release builds, zipalign, apksigner with keystore secrets |
| **CSV / Excel Data Export** | ⏳ Roadmap / Pending | Export transaction records and loan statements to CSV or Excel files |
| **Biometric App Lock** | ⏳ Roadmap / Pending | Optional fingerprint / face unlock for privacy protection |
| **Recurring Transactions** | ⏳ Roadmap / Pending | Automatic scheduling for periodic subscriptions, rent, and utility bills |

---

## 2. App Architecture & State Management

The application follows the **Unidirectional Data Flow (UDF)** and **MVVM (Model-View-ViewModel)** architectural patterns recommended by modern Android standards.

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (MainFinanceScreen, Dashboard, Calendar, Loans)      │
└──────────────────────────▲─────────────────────────────┘
                           │ StateFlow<FinanceUiState>
                           │ User Events (lambdas)
┌──────────────────────────┴─────────────────────────────┐
│                    FinanceViewModel                    │
│   - Combines UI State Flows                            │
│   - Heavy aggregations on Dispatchers.Default          │
└──────────────────────────▲─────────────────────────────┘
                           │ Kotlin Flow / Suspend calls
┌──────────────────────────┴─────────────────────────────┐
│                   FinanceRepository                    │
│   - Enforces Dispatchers.IO on all database accesses   │
└──────────────────────────▲─────────────────────────────┘
                           │ Room DAOs
┌──────────────────────────┴─────────────────────────────┐
│                 Room Database (SQLite)                 │
│   (AppDatabase, TransactionDao, LoanDao, BudgetDao)    │
└────────────────────────────────────────────────────────┘
```

### Unidirectional Data Flow (UDF)
1. **State:** The ViewModel exposes a single, immutable `StateFlow<FinanceUiState>` representing the entire presentation layer state.
2. **Events:** UI components trigger explicit action functions on the ViewModel (e.g., `saveTransaction`, `submitLoanRepayment`, `changeMonth`, `selectDate`).
3. **Immutability:** UI state classes (`FinanceUiState`, `TransactionEntity`, `LoanEntity`, `CalendarDayCell`, `DaySummaryStats`, `CategorySpending`) are marked with `@androidx.compose.runtime.Immutable`, allowing the Compose compiler to skip recompositions when input parameters are structurally identical.

### Recomposition Optimizations
- **`remember` & `derivedStateOf`:** Derived values such as formatted currency strings (`CurrencyUtils.formatBDT`), progress percentages, overdue flags, and status colors are wrapped in `derivedStateOf` to prevent child composables from recomposing on parent state fluctuations.
- **Stable Item Keys & Content Types:** All `LazyColumn` items in the calendar view, dashboard list, loan overview, and day summary sheet supply unique, stable `key` parameters (e.g., `key = { "tx_${it.id}" }`) and distinct `contentType` tags for layout recycling.

### Navigation Architecture
The application uses a tab-driven architecture anchored by `FinanceTab` (`CALENDAR`, `DASHBOARD`, `LOANS`) rendered with Compose `AnimatedContent` for smooth transitions. Sub-screens and dialogs (e.g. `UniversalTransactionSheet`, `DaySummarySheet`, `AddRepaymentDialog`, `SetBudgetDialog`) are managed as overlay modal states within the ViewModel, ensuring lifecycle safety and back-stack predictability.

---

## 3. Database Schema (Room)

The database is defined in `com.engrshuvo.financemanager.data.local.AppDatabase` and consists of 4 core tables:

### 1. `transactions` Table
Stores all financial transactions (Income, Expense, and initial Loan records).

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate (`@PrimaryKey(autoGenerate = true)`) |
| `type` | `TEXT` | `TransactionType` | Enum: `INCOME`, `EXPENSE`, `LOAN` |
| `amount` | `REAL` | `Double` | Transaction monetary value in BDT |
| `categoryId` | `TEXT` | `String` | Category identifier (e.g., `food`, `salary`, `bills`) |
| `categoryName` | `TEXT` | `String` | Display name of the category |
| `note` | `TEXT` | `String` | User notes or description |
| `timestamp` | `INTEGER` | `Long` | Epoch timestamp in milliseconds |
| `loanId` | `INTEGER` | `Long?` | Optional reference to linked loan in `loans` table |

### 2. `loans` Table
Stores personal debt records (money lent to others or borrowed from others).

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate (`@PrimaryKey(autoGenerate = true)`) |
| `type` | `TEXT` | `LoanType` | Enum: `LENT` (Receivable) or `BORROWED` (Payable) |
| `personName` | `TEXT` | `String` | Counterparty full name |
| `phoneNumber` | `TEXT` | `String` | Contact phone number (optional) |
| `initialAmount`| `REAL` | `Double` | Initial principal loan amount |
| `remainingAmount`| `REAL` | `Double` | Outstanding debt balance |
| `status` | `TEXT` | `LoanStatus` | Enum: `ACTIVE` or `SETTLED` |
| `startDate` | `INTEGER` | `Long` | Date when the loan was issued |
| `dueDate` | `INTEGER` | `Long?` | Optional agreed repayment deadline |
| `note` | `TEXT` | `String` | Supplementary terms or notes |

### 3. `loan_repayments` Table
Tracks installment payments made against a loan.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `id` | `INTEGER` | `Long` | Primary Key, Auto-generate |
| `loanId` | `INTEGER` | `Long` | Foreign Key references `loans(id)` on `CASCADE` delete, Indexed (`Index("loanId")`) |
| `amount` | `REAL` | `Double` | Repayment installment value |
| `note` | `TEXT` | `String` | Payment notes (e.g., "Cash", "bKash", "Bank Transfer") |
| `timestamp` | `INTEGER` | `Long` | Epoch millisecond timestamp |

### 4. `budget_settings` Table
Key-value storage for persistent user preferences and targets.

| Column Name | SQLite Type | Kotlin Type | Constraints & Description |
|---|---|---|---|
| `settingKey` | `TEXT` | `String` | Primary Key (e.g. `monthly_budget_limit`) |
| `amountLimit` | `REAL` | `Double` | Monthly expense ceiling (Default: 30,000.0 BDT) |
| `currencyCode`| `TEXT` | `String` | Currency standard (Default: `BDT`) |

---

## 4. Data Access Objects (DAOs)

The database operations are segmented into three specialized interfaces:

### `TransactionDao`
- `getAllTransactions(): Flow<List<TransactionEntity>>`
- `getTransactionsByType(type): Flow<List<TransactionEntity>>`
- `getTransactionsInDateRange(startTime, endTime): Flow<List<TransactionEntity>>`
- `getTotalIncomeFlow(): Flow<Double>`
- `getTotalExpenseFlow(): Flow<Double>`
- `getMonthlyExpenseFlow(startTime): Flow<Double>`
- `insertTransaction(transaction): Long`
- `updateTransaction(transaction)`
- `deleteTransaction(transaction)`
- `deleteTransactionById(id)`

### `LoanDao`
- `getAllLoansFlow(): Flow<List<LoanEntity>>`
- `getActiveLoansFlow(): Flow<List<LoanEntity>>`
- `getLoansByTypeFlow(type): Flow<List<LoanEntity>>`
- `getLoanByIdDirect(id): LoanEntity?`
- `getTotalActiveLentFlow(): Flow<Double>`
- `getTotalActiveBorrowedFlow(): Flow<Double>`
- `getRepaymentsForLoanFlow(loanId): Flow<List<LoanRepaymentEntity>>`
- `insertLoan(loan): Long`
- `updateLoan(loan)`
- `deleteLoan(loan)`
- `insertRepayment(repayment): Long`

### `BudgetSettingDao`
- `getSettingFlow(key): Flow<BudgetSettingEntity?>`
- `insertOrUpdateSetting(setting)`

---

## 5. Threading & Concurrency Strategy

To prevent Android UI freezes, main-thread jank, and skipped frames, execution is strictly partitioned across specialized coroutine dispatchers:

```
┌────────────────────────────────────────────────────────┐
│                   Main (UI) Thread                     │
│  - Compose layout, measurement, and draw calls         │
│  - collectAsStateWithLifecycle rendering               │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌─────────────────────────┐ ┌─────────────────────────┐
│     Dispatchers.IO      │ │   Dispatchers.Default   │
│ - Room Database queries │ │ - 42-day calendar grid  │
│ - SQLite Insert/Update  │ │ - Group-by aggregations │
│ - Repayment cascade ops │ │ - Multi-filter search   │
│ - Reactive DAO Flows    │ │ - Percentage math       │
└─────────────────────────┘ └─────────────────────────┘
```

1. **`Dispatchers.IO` for Database Operations:**
   - All DAO query flows in `FinanceRepository` are chained with `.flowOn(Dispatchers.IO)`.
   - All insert, update, delete, and repayment transaction methods are enclosed in `withContext(Dispatchers.IO)`.
2. **`Dispatchers.Default` for Heavy Calculations:**
   - In `FinanceViewModel`, heavy computations—such as generating the 42-cell monthly calendar matrix, grouping expenses by category, calculating proportional percentages, and filtering against text queries and timestamps—are bound to `Dispatchers.Default` via `flowOn(Dispatchers.Default)`.
   - The main UI thread only receives the finalized, immutable `FinanceUiState` for instantaneous rendering.

---

## 6. UI Component Hierarchy

The Compose UI hierarchy is organized under `com.engrshuvo.financemanager.ui`:

```
MainActivity
└── MainFinanceScreen (Scaffold)
    ├── TopAppBar (Title, Category Subtitle, Budget Settings Button)
    ├── NavigationBar (Calendar, Dashboard, Loans tabs)
    ├── FloatingActionButton (Universal Add Entry)
    │
    ├── AnimatedContent (Tab switching)
    │   ├── [CALENDAR TAB]
    │   │   ├── InteractiveCalendarView (Header, Day-of-Week row, 42-cell grid)
    │   │   └── Selected Day Activity Cards (Totals & Transactions)
    │   │
    │   ├── [DASHBOARD TAB]
    │   │   ├── ComprehensiveDashboardCard (Net balance, Income/Expense, Debt summary)
    │   │   ├── Monthly Spending Target Card (Progress bar & Target dialog trigger)
    │   │   ├── QuickActionsRow (+ Income, - Expense quick buttons)
    │   │   ├── CategorySpendChart (Animated Canvas Donut & top category breakdown)
    │   │   ├── FilterChipsBar (Search field, Type chips, Category filter, Date chips)
    │   │   └── Filtered Transactions List (TransactionItemCard with Swipe-to-Delete)
    │   │
    │   └── [LOANS TAB]
    │       ├── LoanOverviewHeader (You are Owed vs You Owe stat cards)
    │       ├── Loan Type & Status Filters (All, Lent, Borrowed, Active, Settled)
    │       └── Loans List (LoanCardItem with progress, repayment dialog, settle actions)
    │
    └── Modal Sheets & Dialogs
        ├── DaySummarySheet (Detailed daily breakdown modal)
        ├── UniversalTransactionSheet (Unified Income/Expense/Loan creator)
        ├── AddRepaymentDialog (Partial repayment submission dialog)
        └── SetBudgetDialog (Monthly spending limit configuration dialog)
```
