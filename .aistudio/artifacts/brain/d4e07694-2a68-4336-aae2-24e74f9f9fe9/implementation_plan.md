# Remove signingConfigs Block for Unsigned Release APK

Update `app/build.gradle.kts` to remove the hardcoded keystore signing configuration, enabling CI to build an unsigned release APK (`app-release-unsigned.apk`) for signing via GitHub Actions.

---

## 1. Edit `app/build.gradle.kts`
- Remove the `signingConfigs` block:
  ```kotlin
  signingConfigs {
    create("release") { ... }
    create("debugConfig") { ... }
  }
  ```
- Update `buildTypes`:
  - In `release`, remove `signingConfig = signingConfigs.getByName("release")`.
  - In `debug`, remove `signingConfig = signingConfigs.getByName("debugConfig")`.
  - Retain `isCrunchPngs = false`, `isMinifyEnabled = false`, and `proguardFiles(...)`.

---

## 2. Verification
- Run `compile_applet` to ensure the project compiles and builds successfully without signing errors.
