# System Architecture & Technical Specifications

**Project:** Daily Budget & Loan Manager  
**Application ID:** `com.engshuvo.financemanager`  
**Namespace:** `com.engrshuvo.financemanager`  
**Platform:** Android (Min SDK 24, Target SDK 36, Compile SDK 36)  
**Stack:** Kotlin 2.2.10, Jetpack Compose (Material 3), Room Database v6, Kotlin Coroutines & Flow, WorkManager, DataStore Preferences.

---

## 1. Architectural Pattern: MVVM / Unidirectional Data Flow (UDF)

The application adheres strictly to standard Android architecture principles with Unidirectional Data Flow:

```
[ Room SQLite Database v6 ] 
             ▲
             │ (Data Layer: DAO & Flow<T>)
             ▼
[ FinanceRepository ]
             ▲
             │ (State Layer: StateFlow<FinanceUiState>)
             ▼
[ FinanceViewModel ]
             ▲
             │ (UI Events / User Actions)
             ▼
[ Jetpack Compose UI (Material 3) ]
```

- **Single Source of Truth:** The Room database serves as the canonical persistent store.
- **State Emission:** `FinanceViewModel` consolidates Room flows, dialog states, and active month selections into an immutable `StateFlow<FinanceUiState>` collected with `collectAsStateWithLifecycle()`.
- **Atomic Dialog States:** Dialog states are transitioned atomically inside the ViewModel to prevent UI synchronization races or recomposition flicker.

---

## 2. Core Financial Accounting Model

The accounting engine guarantees mathematical accuracy and prevents double-counting:

1. **Operating Income:**
   - Logged as actual received funds (Salary, Bonus, Freelance, Business).
   - Only records with `type == INCOME` and `categoryId != "loan_collected"` count toward operating cash.
2. **Operating Expenses:**
   - Actual out-of-pocket spending on living expenses (House Rent, Groceries, Food, Utilities, Transport, etc.).
   - Reduces remaining category allocation and available cash once.
   - Records with `type == EXPENSE` and `categoryId != "loan_repaid"` count toward operating expenses.
3. **Budget Allocations (Plans vs Cash):**
   - Monthly category allocations are financial targets/plans, **never cash deductions**. Cash is deducted solely upon actual expense entry.
   - Over-allocation triggers a non-blocking **Planned Shortfall Alert Banner** without modifying user values.
4. **Loan & Debt Isolation:**
   - Lending decreases cash once as an investment/receivable (`categoryId = "loan_lent"`), not an expense.
   - Borrowing increases cash once as a payable liability (`categoryId = "loan_borrowed"`), not income.
   - Principal repayments adjust receivables/payables and cash exactly once without polluting monthly budget envelopes.

---

## 3. Data Persistence & Room Schema (Version 6)

The local SQLite schema version remains locked at **Version 6**:

- `transactions`: ID, type (INCOME/EXPENSE), amount, categoryId, note, timestamp, archivedAt.
- `loans`: ID, borrowerName, amount, type (LENT/BORROWED), interestRate, date, dueDate, status, archivedAt.
- `loan_repayments`: ID, loanId, amount, repaymentDate, notes, archivedAt.
- `category_monthly_budgets`: ID, yearMonth, categoryId, allocatedAmount.
- `user_daily_limits`: ID, yearMonth, dailySpendingLimit.
- `user_financial_goals`: ID, title, targetAmount, currentAmount, deadline, priority, isCompleted.
- `purchase_planner_items`: ID, title, estimatedCost, priority, urgency, link, isPurchased.
- `sms_imported_records`: ID, provider, senderAddress, rawMessageText, parsedAmount, parsedType, transactionId, receivedTimestamp, status (PENDING/IMPORTED/REJECTED).

---

## 4. Zero-Permission Payment SMS Import

To eliminate Google Play Protect warnings regarding high-risk SMS permissions (`RECEIVE_SMS`, `READ_SMS`, `SEND_SMS`), Phase 19 transitioned the SMS import system to an entirely zero-permission architecture:

1. **Android Share Target (`ACTION_SEND`):**
   - `MainActivity` declares an `ACTION_SEND` intent filter with `mimeType="text/plain"`.
   - Users can share any SMS directly from their device messaging app into Daily Budget.
2. **First-Class Manual Paste:**
   - Dedicated review sheet allows users to paste raw SMS text directly for regex-driven extraction.
3. **Safe Staging Workflow:**
   - Extracted transactions enter a review queue in `sms_imported_records`.
   - The user inspects and approves or rejects entries before ledger balances are touched.

---

## 5. Storage Access Framework (SAF) Backup & Restore

- **100% Offline Portability:** Uses `ActivityResultContracts.CreateDocument` and `OpenDocument` to export/import JSON documents.
- **Relational Integrity:** Restores entities inside an atomic Room database transaction with full rollback on validation failure.
- **Signature Independence:** SAF backups are stored in user-accessible storage (Downloads, Drive) and are completely decoupled from APK signing certificates.

---

## 6. Deterministic Signing Architecture (Phase 19.3)

### A. Development / Debug Signing
- **Keystore:** Tracked repository keystore (`rootDir/debug.keystore` mirrored via `debug.keystore.base64`).
- **Fail-Closed Guard:** Gradle halts immediately if `debug.keystore` is absent, preventing accidental fallback to randomized ephemeral container keys.
- **Consistent Certificate Fingerprint:**
  - **SHA-256:** `C5:C8:B9:74:34:64:45:DD:B3:F1:62:E7:18:90:F6:3F:26:8E:07:7C:05:0B:5E:2D:3D:93:C1:69:83:5A:9F:62`
  - **SHA-1:** `89:49:27:27:A2:2F:B8:B8:E5:EF:82:F4:9B:FA:45:F6:01:2C:4B:2E`

### B. Production Release Signing
- **Isolation:** Release keystores are kept strictly outside version control.
- **Credential Injection:** Credentials must be supplied via environment variables (`RELEASE_KEYSTORE_PATH`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
- **Distribution Guard:** A dedicated `verifyReleaseSigning` task fails closed if release credentials or keystores are missing, preventing unsigned APKs from being labeled or distributed as installable releases.

---

## 7. Modern Finance UI Redesign & Bottom Navigation Architecture (Phase 20)

### A. 5-Item Modern Bottom Navigation Bar
- **Tab Structure & Exact Order:**
  1. `REPORTS` (`FinanceTab.REPORTS`): Monthly financial trends, expense charts, and analytics.
  2. `BUDGET` (`FinanceTab.BUDGET`): Monthly envelope planning, allocation categories, and daily limits.
  3. `DASHBOARD` (`FinanceTab.DASHBOARD`): Exact center default landing screen with prominent circular badge.
  4. `LOANS` (`FinanceTab.LOANS`): Debt tracker (lent/borrowed, settlements, and repayment history).
  5. `MORE` (`FinanceTab.MORE`): Unified hub for Transactions Ledger, Calendar, SMS Import, Goals, Purchase Planner, PDF Export, and Settings.
- **Visual Styling:**
  - Floating, wide rounded dark-charcoal container (`0xFF18181B`) with 24.dp corner radii and subtle hairline border (`0x1FFFFFFF`).
  - Active selection highlighted in vibrant green accent (`#10B981` / `IncomeGreen`).
  - Prominent elevated center Dashboard badge (`#10B981` container with on-primary `#FFFFFF` icon).
  - Explicit test tags on all navigation items (`nav_tab_reports`, `nav_tab_budget`, `nav_tab_dashboard`, `nav_tab_loans`, `nav_tab_more`).

### B. Dashboard Section Hierarchy
1. **Section A (Header):** Current date formatted, financial dashboard title, and full calendar shortcut icon.
2. **Section B (Hero & Summary Cards):** Available cash balance, monthly income/expense flow, net operating cash, planned allocations, and loan summaries.
3. **Section C (Today's Spending):** Daily limit progress bar, remaining living budget, and configure limit button.
4. **Section D (Compact Monthly Calendar Preview):** Inline `InteractiveCalendarView` allowing quick date inspection directly from the dashboard.
5. **Section E (Selected-Day Activity):** Selected date summary, day income/expense/loan breakdown, and up to 5 day transactions with direct "Ledger →" navigation.
6. **Section F (Quick Actions):** 4 responsive action buttons (`+ Income`, `- Expense`, `+ Loan`, `SMS Import`).

### C. Ledger & Calendar Access Continuity
- **Transactions Ledger:** Relocated from bottom navigation to `MoreSubDestination.TRANSACTIONS`, fully accessible directly from the Reports header banner, Dashboard Ledger shortcuts, and More Hub.
- **Calendar:** Embedded as an interactive compact preview on Dashboard and full-screen view in `MoreSubDestination.CALENDAR`.
- **BackHandler:** Pressing system back from any secondary bottom tab seamlessly returns to Dashboard before exiting.

