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
│                 Room Database (v6)                     │
│   (AppDatabase, TransactionDao, LoanDao,               │
│    BudgetSettingDao, BudgetAllocationDao,              │
│    FinancialGoalDao, PurchaseItemDao, SmsDao)          │
└────────────────────────────────────────────────────────┘
```

---

## 3. Database Schema (Room Version 6)

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

### 6. `financial_goals` & `goal_contributions` Tables *(Added in v4)*
Tracks financial goal savings targets, monthly contributions, and archive status.

### 7. `purchase_items` Table *(Added in v5)*
Shopping notes and task planning items with strict financial isolation.

### 8. `sms_imported_records` Table *(Added in v6)*
Stores parsed payment SMS audit records and candidates for review.
Columns: `id`, `sender`, `provider`, `trxId` (Nullable Text, UNIQUE Indexed), `type`, `amount`, `fee`, `counterparty`, `suggestedCategory`, `timestamp`, `status` (PENDING_REVIEW, POSTED, DISMISSED), `linkedTransactionId` (Nullable Long), `fallbackFingerprint` (Indexed), `note`.

---

## 4. Non-Destructive Room Migration Strategy

To ensure zero user data loss during schema evolution, the app registers explicit migrations:

1. **`MIGRATION_1_2` (v1 → v2)**: Creates `budget_allocations` table and index.
2. **`MIGRATION_2_3` (v2 → v3)**: Adds nullable `archivedAt INTEGER DEFAULT NULL` columns to `transactions`, `loans`, and `loan_repayments`, and creates indexing on each `archivedAt` column. Active records default to null.
3. **`MIGRATION_3_4` (v3 → v4)**: Creates `financial_goals` and `goal_contributions` tables with foreign key cascades.
4. **`MIGRATION_4_5` (v4 → v5)**: Creates `purchase_items` table for shopping notes without touching financial ledger.
5. **`MIGRATION_5_6` (v5 → v6)**: Creates `sms_imported_records` table with unique index on `trxId` and indexes on `timestamp`, `status`, and `fallbackFingerprint`.

---

## 4.1 Automatic Payment SMS Import Architecture

The Automatic Payment SMS Import feature operates under strict security, privacy, and data integrity guarantees:

1. **Pure Event-Driven Receiver:**
   - Listens only to `android.provider.Telephony.SMS_RECEIVED`.
   - Protected in `AndroidManifest.xml` by `android.permission.BROADCAST_SMS` ensuring only the Android telephony framework can deliver broadcasts.
   - Synchronous pre-filter (`PaymentSmsParser.isPotentialFinancialSms`) skips non-financial, spam, OTP, and failed messages within microseconds.
   - Asynchronous processing executes inside `goAsync()` on `Dispatchers.IO` and terminates within bounded execution limits.

2. **Strict New-Message-Only Enforcement:**
   - Evaluates: `messageTimestamp >= activatedAt` and `isEnabled == true` and `RECEIVE_SMS` granted.
   - When the user turns on the feature, `activatedAt` is stamped to the current epoch time.
   - When the feature is turned off, `activatedAt` resets to 0. Re-enabling creates a fresh timestamp.
   - Any SMS with a timestamp preceding `activatedAt` is immediately dropped.
   - **Zero Historical Scanning:** The app does NOT request `android.permission.READ_SMS` and never accesses `content://sms/inbox`.

3. **Deduplication & Financial Safety:**
   - Two-tier deduplication:
     - Primary: SQLite unique constraint and query check on `trxId`.
     - Secondary: Composite `fallbackFingerprint` (`provider:type:amount:minute:bodyHash`) preventing duplicate entries on provider retries while preserving legitimate distinct transactions with different content or timestamps.
   - Ambiguous messages (missing `trxId` or credit card bill payments) are flagged `isConfident = false` and routed exclusively to `PENDING_REVIEW`.
   - Pending-review candidates do NOT affect cash balance or create financial transactions until explicitly approved by the user.
   - Approval idempotency: `FinanceRepository.approveSmsRecord` verifies `record.status == PENDING_REVIEW && record.linkedTransactionId == null` and executes inside `database.withTransaction` to prevent duplicate transaction creation.

4. **Battery & Privacy Compliance:**
   - Zero background polling, zero wake locks, zero persistent foreground services.
   - Zero network transmission: all parsing is strictly local regex matching.
   - Privacy-safe: complete SMS bodies, OTPs, and account numbers are never persisted or logged.


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
    │       └── Privacy & 100% Offline-first local Room v6 data information
    │
    └── Modal Sheets & Dialogs
        ├── BudgetPlanningDialog (Month category allocation planner & Copy Last Month action)
        ├── DailyLimitDialog (Daily spending limit configuration & Monthly suggestion helper)
        ├── UniversalTransactionSheet (Salary/Income, Expense, and Loan creation)
        └── AddRepaymentDialog (Loan installment payment)
```

---

## 7. Android Security, Privacy & OS Auto-Backup Architecture

- **Zero Unintended Cloud Extraction (SEC-001):**
  - `data_extraction_rules.xml` (Android 12+, API 31+) and `backup_rules.xml` (API 23–30) explicitly exclude Room SQLite database files (`domain="database"`), DataStore preference protobufs (`domain="file" path="datastore"`), and SharedPreferences (`domain="sharedpref"`) from OS Google Cloud Auto-Backup.
  - Direct cable/Wi-Fi phone-to-phone migration is permitted via `<device-transfer>` rules to support seamless hardware upgrades without routing financial data through external cloud servers.
- **Atomic Dialog State (PERF-001):**
  - Transaction entry sheet transitions are mediated via immutable `TransactionSheetState(isOpen, editingTransaction, defaultEntryType, defaultLoanType)` emitted through a single `_transactionSheetState` StateFlow, eliminating intermediate asynchronous desynchronization during rapid user interactions.
- **Manual SAF Backup Preservation:**
  - User-initiated export and import via Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) operate via direct user-selected ContentResolver URIs, fully decoupled from OS-managed backup policies.
