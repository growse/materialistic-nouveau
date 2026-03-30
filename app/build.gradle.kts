plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

val okHttpVersion = "3.9.1"
val daggerVersion = "1.2.5"
val retrofitVersion = "2.9.0"
val roomVersion = "2.8.4"
val lifecycleVersion = "2.6.1"
val kotlinVersion = "2.1.0"

android {
    compileSdk = 35

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
            getDefaultProguardFile("proguard-android.txt"),
            "proguard-rules.pro",
            "proguard-square.pro",
            "proguard-support.pro",
            "proguard-rx.pro"
        )
        resConfigs("en", "zh-rCN", "es")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    lint {
        isHtmlReport = false
        isXmlReport = false
        isTextReport = true
        lintConfig = rootProject.file("lint.xml")
        isAbortOnError = true
        isExplainIssues = false
        isAbsolutePaths = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    namespace = "io.github.hidroh.materialistic"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.browser:browser:1.5.0")
    implementation("com.squareup.dagger:dagger:$daggerVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.retrofit2:adapter-rxjava:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("io.reactivex:rxandroid:1.2.1")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-compiler:$lifecycleVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    kapt("androidx.room:room-compiler:$roomVersion")
    kapt("com.squareup.dagger:dagger-compiler:$daggerVersion")
    kaptTest("androidx.room:room-compiler:$roomVersion")
    kaptTest("com.squareup.dagger:dagger-compiler:$daggerVersion")
}
