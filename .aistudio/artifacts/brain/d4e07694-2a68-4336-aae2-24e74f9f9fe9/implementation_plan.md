# Daily Finance & Loan Tracking App (com.engshuvo.financemanager)

A comprehensive personal finance and loan management Android application built with Kotlin, Jetpack Compose, and Room Database. Configured specifically under application ID `com.engshuvo.financemanager` and namespace directory structure `java/com/engrshuvo/financemanager/`, featuring a custom adaptive launcher icon matching the attached Money Sack & Calculator design.

---

## Configuration & Architecture Specifications

### 1. Package Structure & Application ID
- **Application ID (`app/build.gradle.kts`)**: `com.engshuvo.financemanager`
- **Namespace & Source Package**: `com.engrshuvo.financemanager`
- **Directory Structure**: `app/src/main/java/com/engrshuvo/financemanager/`
  - `data/model/`: `TransactionEntity`, `LoanEntity`, `LoanRepaymentEntity`, `BudgetSettingEntity`, `TransactionType`, `LoanType`, `LoanStatus`, `CategoryCatalog`
  - `data/local/`: `AppDatabase`, `TransactionDao`, `LoanDao`, `BudgetSettingDao`, `RoomConverters`
  - `data/repository/`: `FinanceRepository`
  - `ui/theme/`: `Color.kt`, `Theme.kt`, `Type.kt`
  - `ui/state/`: `FinanceUiState.kt`
  - `ui/model/`: `CalendarDayCell.kt`, `DaySummaryStats.kt`
  - `ui/viewmodel/`: `FinanceViewModel.kt`, `FinanceViewModelFactory.kt`
  - `ui/components/`: `InteractiveCalendarView.kt`, `DaySummarySheet.kt`, `DashboardOverviewView.kt`, `LoansScreen.kt`, `UniversalTransactionSheet.kt`, `AddRepaymentDialog.kt`, `SetBudgetDialog.kt`, `FilterChipsBar.kt`, `TransactionItemCard.kt`, `CategorySpendChart.kt`, `QuickActionsRow.kt`
  - `ui/screens/`: `MainFinanceScreen.kt`
  - `MainActivity.kt`

### 2. Custom Adaptive Launcher Icon
- **Foreground & Background**: Recreate the custom gradient icon matching the attached asset (Money Sack with Dollar Sign & Calculator in white over an Indigo `#2E1065` to Purple `#7E22CE` to Pink `#EC4899` gradient).
- **MIPMAP Densities**: Configured across all mipmap density directories with round icon masking.

### 3. Core Capabilities
1. **Interactive Monthly Calendar**:
   - Monthly grid with month/year navigation and Today shortcut.
   - Activity color dots: **Income (Green)**, **Expense (Red)**, **Loan (Blue)**.
   - Date selection bottom sheet showing exact **Total Income**, **Total Expense**, **Total Loan**, and the list of that day's transactions with swipe-to-delete.
2. **Dashboard Summary**:
   - Prominent cards for **Current Balance**, **Total Income**, **Total Expense**, and **Total Active Loans** (Lent vs Borrowed).
   - Category spending donut chart & monthly budget tracking.
3. **Universal Transaction Entry (FAB)**:
   - 3 tabs: **Income**, **Expense**, and **Loan**.
   - Handles Amount, Category / Person Name, Phone, Due Date, and Note.
4. **Loans & Debts Management**:
   - Tracking Lent (Receivable) and Borrowed (Payable) loans.
   - Partial and full repayment recording with instant balance synchronization.
5. **Room Local Database**:
   - Persistent offline storage with foreign-key relations and reactive Kotlin Flows.
