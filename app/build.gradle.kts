plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.androidtv.gameswidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.androidtv.gameswidget"
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
    }

    // Output APK named "WidgetAndroidTV-<buildtype>.apk".
    base.archivesName.set("WidgetAndroidTV")

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            // The three BouncyCastle jars each ship these metadata files.
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.tvprovider:tvprovider:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // HTTP + TLS to the Sunshine/GameStream host
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // X.509 client certificate generation (mutual TLS pairing)
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
}
