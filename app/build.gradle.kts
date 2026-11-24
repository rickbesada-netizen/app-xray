plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Each GitHub build gets a higher number, so a new APK installs over the old one.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.appxray"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.appxray"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // A fixed key kept in the repo, so every build can update the installed app.
    signingConfigs {
        create("xray") {
            storeFile = file("xray.keystore")
            storePassword = "android"
            keyAlias = "xray"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("xray")
        }
        debug {
            signingConfig = signingConfigs.getByName("xray")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
}
