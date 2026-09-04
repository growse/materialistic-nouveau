plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ktfmt)
}

val appVersionCode = 4007
val appVersionName = "v4.0.7"

// Feeds the in-app "What's new" screen from the same file the release workflow requires,
// so there's a single place to update per release instead of two.
val releaseNotesFile = file("../fastlane/metadata/android/en-US/changelogs/$appVersionCode.txt")
val releaseNotesEscaped =
    releaseNotesFile
        .takeIf { it.exists() }
        ?.readText()
        ?.trim()
        ?.replace("\\", "\\\\")
        ?.replace("\"", "\\\"")
        ?.replace("\n", "\\n")
        .orEmpty()

android {
  compileSdk = 37

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  defaultConfig {
    applicationId = "com.growse.android.io.github.hidroh.materialistic"
    minSdk = 23
    targetSdk = 37
    versionCode = appVersionCode
    versionName = appVersionName
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("int", "LATEST_RELEASE", "77")
    buildConfigField("String", "GITHUB_TOKEN", "\"\"")
    buildConfigField("String", "MERCURY_TOKEN", "\"\"")
    buildConfigField("String", "RELEASE_NOTES_HTML", "\"$releaseNotesEscaped\"")
  }

  androidResources { localeFilters += setOf("en", "zh-rCN", "es") }

  buildFeatures { buildConfig = true }

  signingConfigs {
    create("release") {
      storeFile = System.getenv("KEYSTORE_PATH")?.let { file(it) }
      storePassword = System.getenv("KEYSTORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS")
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro",
          "proguard-square.pro",
          "proguard-support.pro",
          "proguard-rx.pro",
      )
    }
  }

  testOptions {
    unitTests {
      isReturnDefaultValues = true
      all { it.useJUnitPlatform() } // Kotest runs on the JUnit 5 platform.
    }

    managedDevices {
      localDevices {
        // ATD (Automated Test Device) images are stripped of the UI apps an
        // instrumentation run never touches, so they boot faster and use less memory
        // than a stock emulator. Only available from API 30 up.
        create("atdApi33") {
          device = "Pixel 2"
          sdkVersion = 33
          systemImageSource = "aosp-atd"
          require64Bit = true
        }
        // Guards minSdk. No ATD image exists this far back, so this is a stock
        // AOSP image and is correspondingly slower to boot.
        create("aospApi23") {
          device = "Nexus 5"
          sdkVersion = 23
          systemImageSource = "aosp"
          require64Bit = true
        }
      }
    }
  }

  lint {
    htmlReport = false
    xmlReport = false
    textReport = true
    lintConfig = file("${rootProject.rootDir}/lint.xml")
    abortOnError = true
    explainIssues = false
    absolutePaths = false
  }

  namespace = "com.growse.android.io.github.hidroh.materialistic"
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.cardview)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.material)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.localbroadcastmanager)
  implementation(libs.hilt.android)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.retrofit.adapter.rxjava)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.rxandroid)
  implementation(libs.rxjava)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.compiler)

  ksp(libs.androidx.room.compiler)
  ksp(libs.hilt.android.compiler)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.rxjava)

  androidTestImplementation(libs.kaspresso)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
}
