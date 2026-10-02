# Performance Optimization Plan: Compose Recompositions & Background Dispatching

Comprehensive performance optimization to eliminate UI frame drops, optimize Compose recompositions, and ensure all Room I/O is executed strictly off the main thread.

---

## 1. Strict Dispatchers.IO for Room Database & Dispatchers.Default for Heavy Calculations
- **`FinanceRepository.kt`**: Wrap all Room operations (inserts, updates, deletes, query Flow transformations) inside `withContext(Dispatchers.IO)`.
- **`FinanceViewModel.kt`**:
  - Run the 42-day calendar grid generation, transaction date-range mapping, and category spend percentage calculations using `flowOn(Dispatchers.Default)` inside Kotlin Flow combinations so the Main (UI) thread remains completely unblocked for smooth 60/120 fps rendering.

---

## 2. Stability & Skippability for Compose
- Add `@androidx.compose.runtime.Immutable` and `@androidx.compose.runtime.Stable` annotations to:
  - `FinanceUiState`
  - `CalendarDayCell`
  - `DaySummaryStats`
  - `CategorySpending`
  - `TransactionEntity`
  - `LoanEntity`
  - `TransactionCategory`
- Replace mutable collections or ensure all list parameters are immutable and stable for the Compose compiler.

---

## 3. `LazyColumn` Optimization & Unique Stable Keys
- Ensure every `LazyColumn` item across:
  - `MainFinanceScreen.kt` (Calendar day logs: `"day_${item.id}"`)
  - `DashboardOverviewView.kt` (Filtered transaction list: `key = { "tx_${it.id}" }`)
  - `LoansScreen.kt` (Loan cards: `key = { "loan_${it.id}" }`)
  - `DaySummarySheet.kt` (Day transactions: `key = { "sheet_tx_${it.id}" }`)
  has a strictly unique, stable `key` parameter.
- Add `contentType` parameter to `LazyColumn` items to enable efficient item recycling.

---

## 4. `derivedStateOf` & `remember` Fine-Grained Scoping
- In Composables (e.g. `ComprehensiveDashboardCard`, `CategorySpendChart`, `FilterChipsBar`, `InteractiveCalendarView`), wrap state computations in `remember` and `derivedStateOf` so child components only recompose when their specific sub-properties change.
- Pass lambda callbacks (e.g. `onEditClick`, `onDeleteClick`, `onDateClick`) as stable function references or `remember`ed closures.
