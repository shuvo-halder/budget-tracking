# Daily Finance & Loan Tracker

A comprehensive personal finance and debt management application built with Kotlin, Jetpack Compose, and Room Database. Features an interactive monthly calendar with daily financial breakdowns, multi-type transaction logging (Income, Expense, Loans), and dedicated loan tracking with partial repayments and due dates.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product specifications were confirmed and incorporated into the design:

- **Loan Categorization**: Both **Lent (Receivable / Money Given)** and **Borrowed (Payable / Money Taken)** with complete settlement lifecycle (Active, Partially Paid, Settled, Overdue).
- **Calendar Visualization**: Dynamic dot indicators on date cells showing daily presence of **Income (Green)**, **Expense (Red)**, and **Loans (Amber/Blue)**.
- **Loan Management**: Dedicated **Loans tab** with one-tap settlement, partial repayment records, due date tracking, and debtor/creditor summaries.
- **Daily Financial Drilldown**: Tapping any calendar day triggers a detailed view/sheet showing exact daily **Total Income**, **Total Expense**, **Total Loan**, and the individual transaction entries with swipe-to-delete.

---

## 1. Overview & Core Concept

- **What It Does**: Unifies daily personal expense tracking and interpersonal loan management into a seamless calendar-driven interface.
- **Target Audience**: Individuals needing full visibility over daily spending, earnings, and who owes what (or who they owe) with offline persistence.
- **Key Value**: Real-time calendar financial heatmaps, zero-friction loan repayment tracking, and instant local Room storage.

---

## 2. User Experience & Visual Design

### Key Views & Navigation (M3 Navigation Bar)

1. **Calendar View (Main Screen)**:
   - **Monthly Header**: Month and Year navigation (prev/next month + jump to Today).
   - **Interactive Calendar Grid**: 7-column day grid with day headers (Sun–Sat). Days display multi-colored status dots for recorded Income, Expense, or Loans.
   - **Selected Day Card / Bottom Sheet**: Highlights selected date, displays day aggregates (Income, Expense, Loan), and lists that specific day's records with swipe-to-delete and edit triggers.
2. **Dashboard Overview**:
   - Top Summary Banner: **Current Balance**, **Total Income** (Mint `#10B981`), **Total Expense** (Coral `#EF4444`), and **Total Active Loans** (Amber `#F59E0B` & Blue `#3B82F6`).
   - Category spending breakdown donut chart.
   - Searchable, filterable transaction history with date range and category chips.
3. **Loans & Debts Hub**:
   - Summary cards: **You are Owed (Lent)** vs **You Owe (Borrowed)**.
   - Loan list filterable by Status (*Active*, *Settled*) and Type (*Lent*, *Borrowed*).
   - Card displays person's name, total amount, remaining balance, due date status (Overdue badge if applicable), and progress bar.
   - **Repayment Action**: Modal to record partial or full repayments or mark settled.
4. **Universal Multi-Type FAB Entry**:
   - Floating Action Button opening bottom sheet with 3 tabs: **Income**, **Expense**, **Loan**.
   - For **Income & Expense**: Numeric amount input, quick chips (+৳100, +৳500, +৳1,000, +৳5,000), visual category picker, note, and date picker.
   - For **Loan**: Amount, Loan direction (Lent to / Borrowed from), Person's name, Phone number (optional), Due date selector, and note.

---

## 3. Key Product Decisions & Trade-Offs

- **Relational Room Schema**:
  - `transactions`: Stores general income, expense, and loan initiation records.
  - `loans`: Stores loan specifics (person name, direction, total amount, remaining amount, due date, status).
  - `loan_repayments`: Relational table linked by `loanId` tracking individual repayments with timestamps and notes.
- **Efficient Monthly Aggregations in SQLite**:
  - DAO queries compute daily calendar dots and monthly totals directly using timestamps, avoiding heavy in-memory processing.

---

## 4. Technical Architecture & Component Structure

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose Navigation                      │
│                                                                        │
│   ┌───────────────────┐  ┌────────────────────┐  ┌─────────────────┐   │
│   │ 📅 Calendar Screen│  │ 📊 Dashboard Screen│  │ 🤝 Loans Screen │   │
│   │   • Monthly Grid  │  │   • Balance Hero   │  │   • Lent Cards  │   │
│   │   • Activity Dots │  │   • Category Chart │  │   • Debt Cards  │   │
│   │   • Day Sheet     │  │   • Filter History │  │   • Repay Modal │   │
│   └─────────┬─────────┘  └─────────┬──────────┘  └────────┬────────┘   │
│             │                      │                      │            │
│   ┌─────────┴──────────────────────┴──────────────────────┴────────┐   │
│   │           Universal Entry Modal Bottom Sheet (FAB)             │   │
│   │               (Income • Expense • Loan tabs)                   │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │ StateFlow & UI Events
┌────────────────────────────────────▼───────────────────────────────────┐
│                        FinanceViewModel                                │
│   • Combines transactions, loan registries, calendar day summaries     │
│   • Manages selected calendar date, filter states, and repayment flow  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Reactive Repositories
┌────────────────────────────────────▼───────────────────────────────────┐
│              FinanceRepository  &  LoanRepository                      │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                     Room Database (SQLite)                             │
│   • transactions (id, type, amount, categoryId, note, timestamp)       │
│   • loans (id, type, personName, phone, amount, remaining, dueDate)    │
│   • loan_repayments (id, loanId, amount, note, timestamp)              │
│   • budget_settings (monthly limit, currency preferences)              │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Implementation Steps & Verification

1. **Database Schema & DAOs**:
   - Define `LoanEntity`, `LoanType` (`LENT`, `BORROWED`), `LoanStatus` (`ACTIVE`, `SETTLED`), and `LoanRepaymentEntity`.
   - Update `TransactionEntity` to support `TransactionType.LOAN`.
   - Implement `LoanDao` and updated `TransactionDao`.
   - Update `AppDatabase` version with migration strategy.
2. **Repositories & ViewModel**:
   - `FinanceRepository` and `LoanRepository`.
   - `FinanceViewModel` providing unified `FinanceUiState` (Calendar monthly matrix with activity dots, daily selected stats, dashboard totals, active loan aggregates).
3. **UI Components**:
   - `InteractiveCalendarView`: Custom monthly calendar matrix with week header, day cells, activity dots (Green/Red/Amber), and month navigation.
   - `DaySummarySheet`: Bottom sheet / detailed card showing specific day's totals and transaction list.
   - `LoansScreen`: Lent & Borrowed overview, loan item cards, settlement buttons, and `AddRepaymentDialog`.
   - `AddEditTransactionSheet`: 3-tab selector (Income, Expense, Loan) with person name & due date fields for loans.
   - `MainNavigationScaffold`: Modern bottom navigation bar switching between Calendar, Dashboard, and Loans.
4. **Verification**:
   - Run `compile_applet` and test all UI flows.
