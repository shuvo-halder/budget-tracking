import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.engrshuvo.financemanager"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.engshuvo.financemanager"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      val devKeystore = file("${rootDir}/debug.keystore")
      val devBase64 = file("${rootDir}/debug.keystore.base64")
      if (!devKeystore.exists() && devBase64.exists()) {
        try {
          val decoded = java.util.Base64.getDecoder().decode(devBase64.readText().replace("\r", "").replace("\n", "").trim())
          devKeystore.writeBytes(decoded)
        } catch (_: Exception) {
          // Fall back gracefully if decoding fails
        }
      }
      if (devKeystore.exists()) {
        storeFile = devKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("release") {
      val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
      val releaseStorePass = System.getenv("RELEASE_STORE_PASSWORD")
      val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
      val releaseKeyPass = System.getenv("RELEASE_KEY_PASSWORD")

      if (!releaseKeystorePath.isNullOrBlank() &&
          !releaseStorePass.isNullOrBlank() &&
          !releaseKeyAlias.isNullOrBlank() &&
          !releaseKeyPass.isNullOrBlank()) {
        val releaseFile = file(releaseKeystorePath)
        if (releaseFile.exists()) {
          storeFile = releaseFile
          storePassword = releaseStorePass
          keyAlias = releaseKeyAlias
          keyPassword = releaseKeyPass
        }
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseConfig = signingConfigs.getByName("release")
      signingConfig = if (releaseConfig.storeFile?.exists() == true) releaseConfig else null
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.work.testing)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("verifyReleaseSigning") {
  group = "verification"
  description = "Validates that production release signing credentials are fully configured for distribution."
  val releaseStoreFile = android.signingConfigs.getByName("release").storeFile
  val releaseAlias = android.signingConfigs.getByName("release").keyAlias
  doLast {
    val missing = mutableListOf<String>()
    val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    if (keystorePath.isNullOrBlank()) missing.add("RELEASE_KEYSTORE_PATH")
    if (System.getenv("RELEASE_STORE_PASSWORD").isNullOrBlank()) missing.add("RELEASE_STORE_PASSWORD")
    if (System.getenv("RELEASE_KEY_ALIAS").isNullOrBlank()) missing.add("RELEASE_KEY_ALIAS")
    if (System.getenv("RELEASE_KEY_PASSWORD").isNullOrBlank()) missing.add("RELEASE_KEY_PASSWORD")

    if (releaseStoreFile == null || !releaseStoreFile.exists()) {
      missing.add("Keystore file not found or inaccessible")
    }

    if (missing.isNotEmpty()) {
      throw GradleException(
        "RELEASE SIGNING IS BLOCKED: Cannot create a distributable release.\n" +
        "Missing required configuration: ${missing.joinToString(", ")}.\n" +
        "Unsigned release artifacts must never be labeled or distributed as installable."
      )
    }
    logger.lifecycle("Release signing verification: PASS. Keystore: ${releaseStoreFile?.absolutePath}, Alias: $releaseAlias")
  }
}
