plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ktfmt)
}

android {
  compileSdk = 36

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  defaultConfig {
    applicationId = "com.growse.android.io.github.hidroh.materialistic"
    minSdk = 23
    targetSdk = 36
    versionCode = 4005
    versionName = "v4.0.5"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("int", "LATEST_RELEASE", "77")
    buildConfigField("String", "GITHUB_TOKEN", "\"\"")
    buildConfigField("String", "MERCURY_TOKEN", "\"\"")
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
    debug { isMinifyEnabled = false }
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

  androidTestImplementation(libs.kaspresso)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
}
