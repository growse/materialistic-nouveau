plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.ktfmt)
}

android {
  compileSdk = 35

  defaultConfig {
    applicationId = "com.growse.android.io.github.hidroh.materialistic"
    minSdk = 23
    targetSdk = 35
    versionCode = 79
    versionName = "3.3"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("int", "LATEST_RELEASE", "77")
    buildConfigField("String", "GITHUB_TOKEN", "\"\"")
    buildConfigField("String", "MERCURY_TOKEN", "\"\"")
    resourceConfigurations += setOf("en", "zh-rCN", "es")
  }

  buildFeatures { buildConfig = true }

  buildTypes {
    debug { isMinifyEnabled = false }
    release {
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
  implementation(libs.dagger)
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

  kapt(libs.androidx.room.compiler)
  kapt(libs.dagger.compiler)
  kaptTest(libs.androidx.room.compiler)
  kaptTest(libs.dagger.compiler)

  androidTestImplementation(libs.kaspresso)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
}
