plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.ktfmt)
}

android {
  compileSdk = 36

  defaultConfig {
    applicationId = "com.growse.android.io.github.hidroh.materialistic"
    minSdk = 21
    targetSdk = 35
    versionCode = 79
    versionName = "3.3"
    buildConfigField("int", "LATEST_RELEASE", "77")
    buildConfigField("String", "GITHUB_TOKEN", "\"\"")
    buildConfigField("String", "MERCURY_TOKEN", "\"\"")
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
        "proguard-square.pro",
        "proguard-support.pro",
        "proguard-rx.pro",
    )
  }

  buildFeatures { buildConfig = true }

  buildTypes {
    debug { isMinifyEnabled = false }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
    }
  }
  lint { lintConfig = rootProject.file("lint.xml") }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlin { jvmToolchain(21) }
  namespace = "io.github.hidroh.materialistic"
  buildToolsVersion = "36.1.0"
}

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
  implementation(libs.kotlin.stdlib)

  kapt(libs.androidx.room.compiler)
  kapt(libs.dagger.compiler)
  kaptTest(libs.androidx.room.compiler)
  kaptTest(libs.dagger.compiler)
}
