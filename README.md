# Daily Budget & Loan Manager

A modern, privacy-focused, offline-first personal finance and debt tracking application for Android. Built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**, the app empowers users to monitor daily transactions, track income and expenses, maintain personal debts and loans, and visualize financial habits with an interactive calendar and comprehensive dashboards.

---

## 📱 App Overview

Managing personal finances often involves tracking fragmented details: daily discretionary expenses, variable income streams, and informal loans with friends, family, or colleagues. **Daily Budget & Loan Manager** unifies these financial streams into a single, intuitive interface with full offline capability and local data sovereignty.

- **Currency:** Bangladeshi Taka (৳ BDT) with support for decimal formatting.
- **Privacy:** 100% offline local database. No accounts, external analytics, or remote tracking.
- **Visual Clarity:** Daily transaction dot indicators on an interactive monthly calendar with real-time financial health summaries.

---

## ✨ Key Features

### 📅 Interactive Monthly Calendar View
- **42-Day Calendar Grid:** Full month calendar showing current days and adjacent month boundary days.
- **Daily Financial Indicators:** Color-coded dots beneath each day:
  - 🟢 **Green Dot:** Income logged
  - 🔴 **Red Dot:** Expense logged
  - 🔵 **Blue Dot:** Loan activity logged
- **Daily Summary Bottom Sheet:** Tap any day to inspect daily income, expense, and loan aggregates, with instant entry creation and inline transaction editing.
- **One-Tap Today Jump:** Fast navigation to the current date or browsing previous/future months.

### 📊 Comprehensive Financial Dashboard
- **Real-Time Financial Snapshot:** Visual hero card displaying current net balance, total income, total expenses, total money you lent (receivable), and total money you owe (payable).
- **Monthly Spending Target:** Configurable monthly expense limit with a live dynamic progress bar (Green → Amber → Red warning when approaching or exceeding limits).
- **Category Expense Breakdown:** Animated donut chart showing proportional spending across categorized expenses (Food, Groceries, Transport, Bills, Rent, Entertainment, Health, and more).
- **Multi-Filter Transaction Search:** Filter transaction logs by type (Income, Expense, Loan), category, pre-defined date ranges (Today, This Week, This Month, All Time), and full-text keyword search.

### 🤝 Debt & Loan Tracker (Lend & Borrow)
- **Two-Way Debt Management:** Track money you lent to others (assets/receivables) and money borrowed from others (liabilities/debts).
- **Repayment Milestones:** Record partial payments with timestamps and notes, automatically updating remaining balances and progress indicators.
- **Settlement & Overdue Tracking:** Mark debts as fully settled or track overdue dates with prominent visual alerts.
- **Contact Integration:** Store counterparty names, notes, and contact phone numbers for every loan.

### ⚡ Offline Local Persistence
- **Room Database (SQLite):** Fast, resilient, offline persistence with zero network latency.
- **Reactive Streams:** Continuous UI updates via Room `Flow` queries and background coroutine dispatchers.

### 📁 CSV / Excel Export *(Planned Roadmap)*
- Export transaction logs, monthly summaries, and debt registers into standard CSV and Excel formats for external auditing, spreadsheet analysis, and offline backups.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) (100% modern Kotlin) |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF) |
| **Local Storage** | [Room Database](https://developer.android.com/training/data-storage/room) via KSP |
| **Concurrency** | Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Default`) & Kotlin Flow |
| **State Management** | `StateFlow`, `collectAsStateWithLifecycle`, `@Immutable` models |
| **CI/CD** | GitHub Actions (JDK 17, Gradle Build Tools, Native `zipalign` & `apksigner`) |

---

## 🚀 CI/CD Pipeline

The project includes an automated GitHub Actions workflow (`.github/workflows/android.yml`) that triggers on every push and pull request to the `main` branch.

### Automated Workflow Steps
1. **Checkout & Java Setup:** Clones repository and configures **JDK 17 (Eclipse Temurin)**.
2. **Gradle Setup:** Bootstraps Gradle via `gradle/actions/setup-gradle@v3`.
3. **Assemble Release APK:** Runs `gradle assembleRelease` to compile an unsigned release binary (`app-release-unsigned.apk`).
4. **Native Android Signing:**
   - Extracts the release keystore from a Base64 repository secret (`KEYSTORE_BASE64`).
   - Locates the latest Android SDK `build-tools` on the runner.
   - Executes `zipalign -v 4` for 4-byte boundary optimization required by Android runtime.
   - Executes `apksigner sign` using the repository secrets (`KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`).
5. **Artifact Publishing:** Uploads the signed, production-ready `Budget.apk` as a downloadable GitHub Actions artifact.

### Required GitHub Secrets for Signing
To enable APK signing in your own repository fork:
- `KEYSTORE_BASE64`: Base64 encoded string of your `.jks` keystore file.
- `KEY_ALIAS`: Alias name of the signing key.
- `KEY_PASSWORD`: Password for the private key.
- `KEYSTORE_PASSWORD`: Password for the keystore file.

---

## 🏁 Getting Started

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer / **IntelliJ IDEA** / **Firebase Studio**
- **JDK 17** (configured as your Gradle JDK)
- **Android SDK:** Compile SDK 35, Min SDK 26, Target SDK 35

### Local Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/daily-finance-and-loan-tracking.git
   cd daily-finance-and-loan-tracking
   ```

2. **Open in Android Studio:**
   - Open Android Studio, select **File > Open**, and select the project root folder.
   - Wait for Gradle sync to complete automatically.

3. **Build the Debug APK:**
   ```bash
   gradle assembleDebug
   ```
   The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

4. **Build the Release APK (Unsigned):**
   ```bash
   gradle assembleRelease
   ```
   The output unsigned APK will be generated at `app/build/outputs/apk/release/app-release-unsigned.apk`.

5. **Run Unit & Robolectric Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

6. **Deploy to Device / Emulator:**
   - Connect an Android device with USB Debugging enabled or start an Android Virtual Device (AVD).
   - In Android Studio, click the green **Run ▶** button or execute:
     ```bash
     gradle installDebug
     ```

---

## 📄 License

This project is licensed under the Apache License 2.0.
