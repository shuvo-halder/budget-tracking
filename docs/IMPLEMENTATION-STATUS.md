# Implementation & Audit Status Log

**Project:** Daily Budget & Loan Manager  
**Application ID:** `com.engshuvo.financemanager`  
**Namespace:** `com.engrshuvo.financemanager`  
**Database Schema:** Room Version 6 (Locked)  
**Last Updated:** Phase 19.3 (October 2026)

---

## Chronological Phase Summary

### Phase 1–16: Core Features & Financial Invariants
- Built salary-based envelope budgeting, live shortfall calculation, and independent daily spending ceiling.
- Established loan accounting: Lent receivables and borrowed payables isolated from operational cash flows.
- Implemented soft-delete archive with 2-month auto-purge and Snackbar undo.
- Added WorkManager morning notifications and quiet-hours expense reminders.

### Phase 17: Performance & Security Hardening
- Resolved `PERF-001`: Converted dialog state mutations into atomic operations to eliminate race conditions.
- Resolved `SEC-001`: Configured `backup_rules.xml` and `data_extraction_rules.xml` to protect local financial databases.

### Phase 18: Google Play Protect Block Investigation
- Diagnosed root cause of Android 15 installation warning: High-risk `android.permission.RECEIVE_SMS` declaration and background broadcast receiver.
- Identified need for zero-permission SMS import and audited signing anomalies.

### Phase 19: Zero-Permission SMS Import Remediation
- Completely removed `RECEIVE_SMS`, `READ_SMS`, and `SEND_SMS` from the Android manifest.
- Removed incoming SMS broadcast receiver.
- Added Android `ACTION_SEND` intent filter (`text/plain`) allowing users to share SMS directly from messaging apps.
- Added first-class Manual Paste SMS parsing interface.
- Preserved pending-import review staging table.
- Verified with 145 unit/Robolectric test cases.

### Phase 19.1: Artifact, Signing & Upgrade Verification
- Extracted and compared binary signatures using `apksigner` and `aapt2`.
- Diagnosed signing drift: Ephemeral build containers generated fresh `~/.android/debug.keystore` instances because no explicit keystore was bound in Gradle.
- Marked device testing as `NOT RUN` due to absence of connected physical hardware.

### Phase 19.2: Signing Strategy Formulation
- Performed read-only audit of project keystores and configurations.
- Identified tracked `./debug.keystore` (`debug.keystore.base64`) as the authentic project development key.
- Designed deterministic debug signing configuration and fail-closed release guard.
- Defined user-safe upgrade procedure using SAF JSON backup to safeguard user data against signature mismatch.

### Phase 19.3: Stable Signing Implementation & Release Guard
- **Debug Signing:** Configured `signingConfigs.debug` in `app/build.gradle.kts` explicitly pointing to `${rootDir}/debug.keystore`. Enforced fail-closed behavior if the keystore file is absent.
- **Release Signing Guard:** Added `verifyReleaseSigning` task that validates presence of release keystore and environment variables, failing closed if unsigned distribution is attempted.
- **Reproducible Build Verified:** Debug APK confirmed signed with fixed SHA-256: `C5:C8:B9:74:34:64:45:DD:B3:F1:62:E7:18:90:F6:3F:26:8E:07:7C:05:0B:5E:2D:3D:93:C1:69:83:5A:9F:62`.

---

## Current Signing & Verification Status Matrix

| Component | Status | Details |
| :--- | :---: | :--- |
| **Debug APK Signature** | **PASS** | Validated via `apksigner`. Explicitly bound to repository `debug.keystore`. SHA-256: `C5:C8:B9:74...`. |
| **Debug Build Determinism** | **PASS** | Multiple clean builds verified producing identical signature fingerprints. |
| **Release Signing Status** | **BLOCKED** | Unconfigured in environment. Production release keystores are kept outside source control. Task `verifyReleaseSigning` fails closed as intended. |
| **Old/New Upgrade Compatibility** | **FAIL / UNKNOWN** | Old APK on user's device remains uninspected. If signed by an earlier ephemeral container key, Android OS will reject in-place upgrade. User must use SAF JSON backup before reinstalling. |
| **Automated Test Suite** | **PASS** | 145 unit and Robolectric tests passing. |
| **Android 15 Physical Upgrade** | **NOT RUN** | Physical device not connected via ADB in container environment. |
| **Play Protect Device Test** | **NOT RUN** | Manifest and binary verified free of high-risk permissions; physical device confirmation pending. |

---

## Verification Commands

### Inspect Debug APK Certificate
```bash
/opt/android/sdk/build-tools/36.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
```

### Inspect Package Identity and Permissions
```bash
/opt/android/sdk/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | grep -E "(package:|uses-permission:)"
```

### Run Release Signing Guard Check
```bash
gradle :app:verifyReleaseSigning
```
*(Fails closed with exit code 1 if credentials are missing)*
