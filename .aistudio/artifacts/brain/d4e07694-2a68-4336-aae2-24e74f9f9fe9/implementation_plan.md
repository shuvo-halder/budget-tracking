# Accurate Salary-Based Personal Finance & Budget Management System (Revised Plan)

Evolve the existing application into an accurate, salary-based personal finance management system with month-specific budget allocations, independent daily spending limits, category-based accounting, and strict accounting boundaries to prevent double-counting.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX policies are confirmed:
> 1. **Daily Spending Limit**: Managed independently with an optional 1-tap calculation from the monthly Daily Expenses allocation (`dailyLimit = monthlyAllocation / daysInMonth`). Calculating the suggestion never mutates either saved value silently.
> 2. **New Month Setup**: When opening a month without saved budget allocations, pre-fill with the previous month's allocations as an editable draft. Copying last month's budget allocations **never** copies income, expenses, savings transfers, or loans.
> 3. **Planned Shortfall Alert**: When planned budget allocations exceed actual monthly income, display a prominent shortfall alert banner and highlight over-allocated categories without mutating or rejecting user allocations.
> 4. **Default Landing Screen**: The app opens directly to the **Dashboard** (`FinanceTab.DASHBOARD`).
> 5. **Non-Destructive Room Migration**: Upgrade Room schema from version 1 to 2 using an explicit `Migration(1, 2)` to preserve all existing transactions, loans, repayments, and settings.

---

## 1. Audit of Existing Codebase & Double-Counting Analysis

Our code audit of `FinanceViewModel.kt`, `FinanceRepository.kt`, and `FinanceDaos.kt` revealed key accounting and boundary gaps:

1. **Global vs. Category-Based Budgeting**:
   - *Current*: A single global `monthlyLimit` (default 30,000 ৳) in `budget_settings` compared against all expenses since `getStartOfMonth()`. No category allocations exist.
   - *Fix*: Introduce a dedicated `budget_allocations` table keyed by `(monthKey, categoryId)`. Users allocate money per category (Rent, Family Maintenance, Daily Expenses, Bills, Savings, etc.) for each month.
2. **Month Leakage & Fixed Date Boundaries**:
   - *Current*: `monthlySpent` uses `timestamp >= DateUtils.getStartOfMonth()` (current system month only), ignoring the user's selected/displayed month and lacking an `endOfMonth` upper bound.
   - *Fix*: All monthly calculations use the displayed month with a strict half-open interval: `startOfMonth <= timestamp <= endOfMonth`.
3. **Loan Repayments vs. Ordinary Living Expenses**:
   - *Current*: `FinanceRepository.recordLoanRepayment` creates a `TransactionEntity` with type `INCOME` (for collected debt) or `EXPENSE` (for repaid debt). Consequently, debt repayments currently inflate ordinary living expenses and distort category budgets.
   - *Fix*: Distinguish operating transactions from financing transactions. Ordinary living expenses exclude loan repayment categories (`loan_repaid`, `loan_collected`), preventing debt settlements from exhausting monthly living expense allocations or distorting the daily expense budget.
4. **Planned Allocations vs. Actual Outflows (Double-Counting Prevention)**:
   - *Principle*: Budget allocation is a plan, not an expense. Actual expenses reduce available cash and the remaining budget of exactly one assigned category. Budget allocations are never subtracted from cash.
5. **Planned Savings vs. Actual Expenses**:
   - *Principle*: Earmarked savings allocations (e.g. 10,000 ৳ to Savings) are planned targets and are not counted as expenses or cash drains.
6. **Independent Daily Limit vs. Monthly Daily Expenses Category**:
   - *Principle*: The daily spending limit (e.g. 500 ৳/day) and the monthly Daily Expenses allocation (e.g. 10,000 ৳/month) are tracked independently. Logging a daily expense reduces today's daily limit remaining, the monthly Daily Expenses category remaining, and total cash once—never twice.

---

## 2. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  - MainFinanceScreen (Default: Dashboard Tab)                          │
│  - DashboardOverviewView:                                              │
│    * Section A: Month Selector & Cash Overview + Shortfall Banner      │
│    * Section B: Today's Spending Card (Limit vs. Actual, Overspend)    │
│    * Section C: Monthly Category Allocations (Rent, Family, Daily, etc)│
│    * Section D: Recent Transactions (Filtered & Grouped)               │
│    * Section E: Category Spend Donut Chart                             │
│  - BudgetPlanningDialog (Editable allocations per month)              │
│  - DailyLimitDialog (Manual limit or suggest from monthly allocation) │
│  - UniversalTransactionSheet (Salary/Income & Categorized Expenses)    │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ StateFlow<FinanceUiState>
                                    │ User Actions
┌───────────────────────────────────┴────────────────────────────────────┐
│                           FinanceViewModel                             │
│  - Reactive Flow combination on Dispatchers.Default                    │
│  - Strict half-open date interval filtering per displayed month        │
│  - Core Accounting Calculations:                                       │
│    * totalIncome, totalExpenses, netOperatingCashChange                │
│    * totalAllocated, unallocatedIncome, plannedShortfall               │
│    * categoryRemaining = categoryAllocation - categoryActualSpent      │
│    * todayExpenses = sum(EXPENSE on startOfToday..endOfToday)          │
│    * dailyBudgetRemaining = dailyLimit - todayExpenses                 │
│  - Pre-fill draft allocations from previous month if unconfigured      │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Flow / Suspend methods
┌───────────────────────────────────┴────────────────────────────────────┐
│                          FinanceRepository                             │
│  - Dispatchers.IO isolation                                            │
│  - Streams: Transactions, Loans, BudgetSettings, BudgetAllocations     │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ SQLite / Room DAOs
┌───────────────────────────────────┴────────────────────────────────────┐
│                    Room Database (AppDatabase v2)                      │
│  - transactions                                                        │
│  - loans & loan_repayments                                             │
│  - budget_settings (daily_budget_limit, monthly_budget_limit)          │
│  - budget_allocations [NEW TABLE]                                      │
│    * Primary Key: (monthKey, categoryId)                               │
│    * Columns: monthKey, categoryId, categoryName, allocatedAmount      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Database Schema Changes & Non-Destructive Migration

### New Entity: `BudgetAllocationEntity`
```kotlin
@Immutable
@Entity(
    tableName = "budget_allocations",
    primaryKeys = ["monthKey", "categoryId"],
    indices = [Index("monthKey")]
)
data class BudgetAllocationEntity(
    val monthKey: String, // Normalized format "yyyy-MM", e.g. "2026-10"
    val categoryId: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Room Migration 1 -> 2
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
```
All existing records in `transactions`, `loans`, `loan_repayments`, and `budget_settings` remain completely untouched.

---

## 4. Financial Calculation Specifications

For a selected month `monthKey` with interval `[startOfMonth, endOfMonth]`:

1. **Monthly Actual Income**:
   $$\text{totalIncome} = \sum \text{amount for } (\text{type} == \text{INCOME} \land \text{timestamp} \in [\text{startOfMonth}, \text{endOfMonth}])$$
2. **Monthly Actual Expenses**:
   $$\text{totalExpenses} = \sum \text{amount for } (\text{type} == \text{EXPENSE} \land \text{categoryId} \neq \text{"loan\_repaid"} \land \text{timestamp} \in [\text{startOfMonth}, \text{endOfMonth}])$$
3. **Net Operating Cash Change**:
   $$\text{netOperatingCashChange} = \text{totalIncome} - \text{totalExpenses}$$
4. **Monthly Category Budgeting**:
   $$\text{categoryRemaining} = \text{allocatedAmount} - \text{categoryActualSpent}$$
   $$\text{usagePercentage} = \begin{cases} \frac{\text{categoryActualSpent}}{\text{allocatedAmount}} \times 100 & \text{if } \text{allocatedAmount} > 0 \\ 0 & \text{otherwise} \end{cases}$$
5. **Planned Allocations vs. Income**:
   $$\text{totalAllocated} = \sum \text{allocatedAmount for all active categories in } monthKey$$
   $$\text{unallocatedIncome} = \text{totalIncome} - \text{totalAllocated}$$
   $$\text{plannedShortfall} = \max(0.0, \text{totalAllocated} - \text{totalIncome})$$
6. **Daily Spending Limit**:
   $$\text{todayExpenses} = \sum \text{amount for } (\text{type} == \text{EXPENSE} \land \text{categoryId} \neq \text{"loan\_repaid"} \land \text{timestamp} \in [\text{startOfToday}, \text{endOfToday}])$$
   $$\text{dailyBudgetRemaining} = \text{dailyBudgetLimit} - \text{todayExpenses}$$
   $$\text{dailyUsagePercentage} = \begin{cases} \frac{\text{todayExpenses}}{\text{dailyBudgetLimit}} \times 100 & \text{if } \text{dailyBudgetLimit} > 0 \\ 0 & \text{otherwise} \end{cases}$$
7. **Savings Integrity**:
   - `savings` and `emergency` allocations are earmarked plans. They never count as expenses or deductions from cash.

---

## 5. UI Structure & Screens

1. **Default Tab**: `FinanceTab.DASHBOARD` on initial load.
2. **Dashboard Overview View**:
   - **Section A: Financial Overview**: Month switcher, Received Salary/Income, Total Expenses, Net Cash Change, Total Planned Allocations, Unallocated/Shortfall banner.
   - **Section B: Today's Spending Card**: Prominent display of Today's Spending vs Daily Limit with overspending alert and config dialog.
   - **Section C: Monthly Category Allocations**: Category cards (House Rent, Family Maintenance, Daily Expenses, Food, Transport, Bills, Savings, Emergency Reserve, Other) with allocated, spent, remaining, progress bar, and "Plan / Edit Budget" action.
   - **Section D: Recent Transactions**: Filterable transaction list with category badges, amounts, and swipe-to-delete.
   - **Section E: Category Spending Distribution**: Animated Donut chart of actual expenses.
3. **Budget Planning Dialog**:
   - Edit category allocations for the selected month.
   - Action to "Copy from Previous Month" (copies budget allocation targets only, never transactions).
4. **Universal Transaction Sheet**:
   - Updated categories with House Rent, Family Maintenance, Daily Expenses, Savings, Emergency Reserve, and Income sources (Salary, Bonus, Freelance, Business, Other).

---

## 6. Verification Plan

1. **Automated Unit Tests (`SalaryFinanceCalculationTest.kt`)**:
   - Verify salary entries increase monthly income.
   - Verify category allocations persist independently per month.
   - Verify copying previous month allocations does not copy transactions.
   - Verify daily limit and monthly daily expenses allocation remain independent.
   - Verify expenses reduce exactly one category allocation and available cash without double counting.
   - Verify planned savings do not count as actual expenses.
   - Verify loans remain separated from operating income and expenses.
   - Verify planned shortfall is triggered when allocations exceed income.
2. **Build & Migration Verification**:
   - Run `gradle :app:testDebugUnitTest`.
   - Run `gradle assembleDebug` / `compile_applet`.
3. **Documentation Updates**:
   - Update `README.md` and `docs/ARCHITECTURE.md` with the new schema, formulas, and accounting rules.
