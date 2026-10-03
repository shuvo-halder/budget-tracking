# Salary-Based Personal Finance & Budget Management System

A modern, privacy-focused, offline-first personal finance, salary budgeting, and debt tracking application for Android. Built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**, the app empowers users to manage monthly salary and additional income, allocate funds to essential categories (House Rent, Family Maintenance, Daily Expenses, Bills, Savings, Emergency Reserves), record actual living expenses, track daily spending against independent limits, and maintain personal loans without double counting.

---

## 📱 App Overview

**Daily Budget & Loan Manager** implements an accurate salary-based envelope budgeting and financial accounting model:

- **Primary Scenario:**
  1. The user logs their monthly salary and any additional income (Bonus, Freelance, Business, etc.).
  2. The user allocates funds across categories such as House Rent, Family Maintenance, Daily Expenses, Food & Dining, Groceries, Bills & Utilities, Savings, and Emergency Reserve.
  3. The user records actual expenses throughout the month. Each expense reduces the remaining allocation of exactly one category and available cash once.
  4. Daily spending is tracked against an independent daily limit, with an optional 1-tap suggestion derived from the monthly Daily Expenses allocation.
  5. The application shows how much was allocated, how much was spent, how much remains, and the available operating cash balance.
  6. Personal debt and loan tracking (Lent/Borrowed) remains cleanly isolated from operating income and living expenses.

- **Currency:** Bangladeshi Taka (৳ BDT) with clear decimal and sign formatting.
- **Privacy:** 100% offline local Room database. Zero remote tracking, external cloud sync, or third-party telemetry.
- **Default Screen:** The app launches directly into the **Dashboard** overview.

---

## ✨ Key Features

### 💵 Salary & Income Management
- **Actual Received Funds:** Logs monthly salary, festive bonuses, freelance income, and business receipts.
- **Distinction Between Plans & Cash:** Budget allocations are financial plans, not cash deductions. Cash is reduced only by actual expenses.

### 📋 Month-Specific Budget Allocations
- **Envelope Budgeting:** Set planned budgets for essential categories:
  - 🏠 House Rent
  - 👨‍👩‍👧‍👦 Family Maintenance
  - 💳 Daily Expenses
  - 🍔 Food & Dining
  - 🛒 Groceries
  - 💡 Bills & Utilities
  - 🚗 Transportation
  - 💰 Savings (Planned Reserve)
  - 🛡️ Emergency Reserve
  - 🛍️ Shopping, Health, Education, Fitness, and Other
- **Pre-Fill Drafts:** Opening a new month automatically pre-fills the previous month's allocation targets as editable drafts without copying historical transactions.
- **Planned Shortfall Alert Banner:** Prominently alerts the user when planned allocations exceed actual monthly income (`totalAllocated > totalIncome`) and highlights over-allocated categories without rejecting or silently modifying user plans.

### ⏱️ Independent Daily Spending Limit
- **Today's Living Expenses:** Tracks today's actual expenses against a configurable daily spending ceiling.
- **Suggested Daily Calculation:** 1-tap helper suggests `monthlyDailyExpensesAllocation ÷ daysInMonth` without silently altering either saved value until confirmed.
- **Visual Feedback:** Color-coded progress bar (Green → Amber → Red) with real-time overspending alerts.

### 🤝 Debt & Loan Tracker (Lend & Borrow)
- **Clear Accounting Boundaries:** Lent receivables and borrowed payables remain separate from ordinary operating income and living expenses.
- **Partial Repayments:** Record installment payments with timestamped receipts and notes.
- **Full Settlement:** One-tap loan settlement.

### 📅 Interactive Monthly Calendar
- **42-Day Matrix:** Visual month calendar with colored activity dots:
  - 🟢 **Green Dot:** Income logged
  - 🔴 **Red Dot:** Expense logged
  - 🔵 **Blue Dot:** Loan activity logged
- **Daily Activity Sheet:** Inspect daily breakdown and log entries directly from any day.

### 🗑️ Archive, Recovery & 2-Month Retention
- **Soft-Deletion with Undo:** Deleting any transaction or loan moves it to the archive, preserving relational links and original identifiers.
- **2 Calendar Months Expiration:** Expired records are automatically and permanently purged after 2 calendar months using calendar-month arithmetic.
- **Zero Accidental Data Loss:** Instant Snackbar Undo and independent Archive screen Restore capability.

### 🔔 Daily Budget Notifications & Expense Reminders
- **Morning Daily Budget Alerts:** Default 8:00 AM local time notification delivering current day's planned spending limit and remaining allowance (`dailyBudgetLimit - todayExpenses`). Tapping opens the Dashboard.
- **Periodic Expense Recording Reminders:** Customizable background reminders (every 2, 3, 4, 6, 8 hours) prompting users to record daily expenses. Tapping opens the Add Expense sheet.
- **Smart Quiet Hours:** Mutes reminders during sleep hours (default 10:00 PM to 8:00 AM) with support for overnight wrapping.
- **Offline & Battery-Friendly:** Powered by AndroidX WorkManager and DataStore Preferences. Zero cloud reliance, survives device reboots and timezone adjustments without duplicate alarms.

### 🧭 Center-Dashboard Bottom Navigation
- **Balanced 5-Destination Layout:** Transactions, Budget, **Dashboard (Exact Center)**, Loans, More.
- **Edge-to-Edge & Gesture Navigation:** Fully compliant with Material 3 insets and predictive back handling.

---

## 🧮 Core Financial Accounting Formulas

$$\text{totalIncome} = \sum \text{amount for } (\text{type} == \text{INCOME} \land \text{categoryId} \neq \text{"loan\_collected"})$$

$$\text{totalExpenses} = \sum \text{amount for } (\text{type} == \text{EXPENSE} \land \text{categoryId} \neq \text{"loan\_repaid"})$$

$$\text{netOperatingCashChange} = \text{totalIncome} - \text{totalExpenses}$$

$$\text{availableCashBalance} = \text{allTimeIncome} - \text{allTimeExpense} + \text{allTimeBorrowed} - \text{allTimeLent}$$

$$\text{totalAllocated} = \sum \text{allocatedAmount for all active categories in } M$$

$$\text{unallocatedIncome} = \text{totalIncome} - \text{totalAllocated}$$

$$\text{plannedShortfall} = \max(0.0, \text{totalAllocated} - \text{totalIncome})$$

$$\text{categoryRemaining} = \text{allocatedAmount} - \text{categoryActualSpent}$$

$$\text{todayExpenses} = \sum \text{amount for } (\text{type} == \text{EXPENSE} \land \text{categoryId} \neq \text{"loan\_repaid"} \land \text{timestamp} \in [\text{startOfToday}, \text{endOfToday}])$$

$$\text{dailyBudgetRemaining} = \text{dailyBudgetLimit} - \text{todayExpenses}$$

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) (100% modern Kotlin) |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF) |
| **Local Storage** | [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) via KSP |
| **Concurrency** | Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Default`) & Kotlin Flow |
| **State Management** | `StateFlow`, `collectAsStateWithLifecycle`, `@Immutable` models |
| **CI/CD** | GitHub Actions (JDK 17, Native `zipalign` & `apksigner` with Keystore secrets) |

---

## 💾 Database Schema & Room Migrations

The database (`AppDatabase`, Version 2) includes:
- `transactions`: Log of all Income, Expense, and Loan transactions.
- `loans`: Personal debt records (Lent/Borrowed), counterparties, and balances.
- `loan_repayments`: Installment repayments referencing `loans(id)` on `CASCADE` delete.
- `budget_settings`: Key-value configuration for `monthly_budget_limit` and `daily_budget_limit`.
- `budget_allocations` *(Added in v2)*: Month-specific category allocations keyed by `(monthKey, categoryId)`.

### Non-Destructive Migration (1 -> 2)
The schema upgrade is executed through an explicit `MIGRATION_1_2` SQLite script that preserves all historical transactions, loans, and settings without data loss.

---

## 🚀 CI/CD Pipeline

Automated GitHub Actions workflow (`.github/workflows/android.yml`):
1. **Checkout & Java Setup:** Sets up **JDK 17 (Eclipse Temurin)**.
2. **Gradle Setup:** Uses `gradle/actions/setup-gradle@v3`.
3. **Assemble Release APK:** Runs `gradle assembleRelease` to compile `app-release-unsigned.apk`.
4. **Native Android Signing:**
   - Decodes `KEYSTORE_BASE64` to `my-upload-key.jks`.
   - Locates latest Android SDK build-tools.
   - Runs `zipalign -v 4` for 4-byte boundary alignment.
   - Signs APK with `apksigner sign` using repository secrets (`KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`).
5. **Artifact Upload:** Publishes the signed `Budget.apk`.

---

## 🏁 Getting Started

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17**
- **Android SDK:** Compile SDK 35, Min SDK 26, Target SDK 35

### Local Build & Execution
```bash
# Clone the repository
git clone https://github.com/your-username/daily-finance-and-loan-tracking.git
cd daily-finance-and-loan-tracking

# Run unit tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle assembleDebug

# Build release APK
gradle assembleRelease
```
