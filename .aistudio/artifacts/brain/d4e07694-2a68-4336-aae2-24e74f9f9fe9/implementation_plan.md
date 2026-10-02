# Daily Budget Tracker (SpendWise)

A clean, modern, and intuitive personal finance tracker built with Kotlin, Jetpack Compose, and Room Database to seamlessly manage daily income, expenses, monthly budget limits, and spending breakdowns.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed and incorporated into the architecture:

- **Confirmed Currency**: Bangladeshi Taka (BDT / ৳) formatted with clear numeric localization.
- **Confirmed Key Features**:
  - Top Dashboard showing Current Balance, Total Income (green), and Total Expense (red/coral).
  - Prominent Add Income and Add Expense actions opening a sleek entry sheet/dialog.
  - Category and Date range filtering for transaction history.
  - Delete and Edit transaction support with swipe-to-delete and edit dialogs.
  - Monthly spending limit with dynamic progress bar and budget health warnings.
  - Visual category spending breakdown donut & bar chart.
- **Initial Data State**: Clean slate on first launch with rich default category catalog (Food, Transport, Groceries, Salary, Freelance, Bills, Entertainment, Health, Shopping, Investment, Education, Utilities, Other).

---

## 1. Overview & Core Concept

- **What It Does**: Provides real-time financial clarity by logging daily earnings and expenditures, computing net balance, tracking monthly budget utilization, and displaying visual spending distribution.
- **Target Audience**: Anyone wanting effortless, offline-first personal budget tracking without complicated spreadsheets or mandatory cloud accounts.
- **Key Value**: Instant offline persistence, zero latency, Material 3 visual polish, and informative budget insights.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Summary View**:
   - High-contrast Hero Card with Total Balance, Total Income badge (soft emerald), and Total Expense badge (soft crimson).
   - Monthly Budget Limit indicator (e.g. `৳14,500 / ৳30,000 spent • 48% used`) with an animated progress bar changing to warning colors as limit approaches.
2. **Adding & Editing Transactions**:
   - Tap **+ Add Income** or **- Add Expense** (or the floating action button).
   - Dynamic modal bottom sheet opens with pre-selected transaction type.
   - Quick amount suggestions (`+৳100`, `+৳500`, `+৳1,000`, `+৳5,000`) and numeric input field.
   - Visual category picker with colored icons.
   - Note/Title field and customizable date picker.
3. **Transaction History & Analytics**:
   - Search & Filter bar by category (All, Food, Bills, etc.) and type (All, Income, Expense).
   - Grouped transaction list by date (e.g., *Today*, *Yesterday*, *October 1, 2026*).
   - Tap any item to edit; swipe or use item menu to delete with instant confirmation and undo snackbar.
4. **Category Breakdown & Charts**:
   - Visual interactive donut/arc chart and category distribution list showing exact spending percentages.

### Visual Identity & Theme

- **Palette**: Modern FinTech Emerald & Slate
  - Primary: Deep Emerald Slate (`#006C4C` / `#47DDA0`)
  - Income Accent: Vibrant Mint (`#10B981`)
  - Expense Accent: Coral Rose (`#F43F5E`)
  - Surface Background: Warm Alabaster (`#F8FAF9`) in Light mode / Deep Slate Obsidian (`#0F172A`) in Dark mode.
- **Typography**: Clean Material 3 typography with bold tabular figures for financial amounts.
- **Micro-interactions**: Spring animations on progress updates, smooth category selection pills, and fluid bottom sheet transitions.

---

## 3. Key Product Decisions & Trade-Offs

- **Local Storage via Room**:
  - *Chosen Approach*: Android Room ORM with SQLite, reactive Kotlin Coroutines `Flow`, and DAO repository layer.
  - *Why*: Reliable, offline-first data guarantee, instant search/filtering, and seamless schema migrations.
- **Custom Compose Canvas Charts vs External Heavy Library**:
  - *Chosen Approach*: Pure Jetpack Compose custom canvas-rendered Donut and Bar charts with animated transitions.
  - *Why*: Eliminates bloated third-party chart library dependencies, allows full dark/light theme alignment, and guarantees 60fps animations.
- **State Architecture**:
  - *Chosen Approach*: Single unidirectional data flow `BudgetViewModel` with `BudgetUiState` combining balance aggregates, filtered transaction flows, and category summaries.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                     │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────────┐  │
│  │ DashboardCard │ │ BudgetBarView │ │ CategoryChartCard │  │
│  └───────┬───────┘ └───────┬───────┘ └─────────┬─────────┘  │
│          │                 │                   │            │
│  ┌───────┴─────────────────┴───────────────────┴─────────┐  │
│  │               TransactionHistoryList                  │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │                                │
│  ┌─────────────────────────┴─────────────────────────────┐  │
│  │               AddEditTransactionSheet                 │  │
│  └─────────────────────────┬─────────────────────────────┘  │
└────────────────────────────┼────────────────────────────────┘
                             │ Events & StateFlow
┌────────────────────────────▼────────────────────────────────┐
│                      BudgetViewModel                        │
│   • MutableStateFlow<BudgetUiState>                         │
│   • Transaction filtering & aggregation                     │
│   • Budget limits & Monthly calculations                    │
└────────────────────────────┬────────────────────────────────┘
                             │ Suspend / Flow
┌────────────────────────────▼────────────────────────────────┐
│                    TransactionRepository                    │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                     Room Database (SQLite)                  │
│   • TransactionEntity (id, type, amount, category, note, ts)│
│   • BudgetPreferenceDao / Setting Store                     │
└─────────────────────────────────────────────────────────────┘
```

### Data Entities & Models

```kotlin
enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_settings")
data class BudgetSettingEntity(
    @PrimaryKey val id: String = "default_monthly_budget",
    val monthlyLimit: Double = 25000.0
)
```

---

## 5. Implementation Roadmap & Verification

1. **Database Layer**:
   - Room Entities (`TransactionEntity`, `BudgetSettingEntity`), TypeConverters, and `TransactionDao`.
   - `AppDatabase` singleton builder.
   - `BudgetRepository` handling data aggregation and CRUD operations.
2. **ViewModel & State Management**:
   - `BudgetViewModel` computing balance, income/expense totals, filtered lists, category distribution, and monthly progress.
3. **UI Components & Screens**:
   - Modern Theme palette (`Color.kt`, `Theme.kt`, `Type.kt`).
   - `DashboardHeader`: Balance card, quick stats, monthly budget progress bar.
   - `ActionButtons`: Quick Add Income / Add Expense triggers.
   - `CategoryBreakdownCard`: Interactive visual spend chart.
   - `TransactionHistory`: Grouped by date, search filter, category filter chips, swipe-to-delete, edit action.
   - `AddEditTransactionSheet`: Form validation, category selector with icons, quick amount pills.
   - `SetBudgetDialog`: Allows setting custom monthly spending limits.
4. **App Metadata & Resources**:
   - Sync `metadata.json` and `res/values/strings.xml` with app name "Daily Budget Tracker".
   - Vector drawables for categories (Food, Transport, Bills, Shopping, Salary, etc.).
5. **Compilation & Verification**:
   - Compile and verify build with `compile_applet`.
