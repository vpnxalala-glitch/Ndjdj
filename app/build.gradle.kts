plugins {
    id("com.android.application")
}

android {
    namespace = "com.vich.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vich.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "3").toInt()
        versionName = "0.3.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
        buildConfigField("String", "TELEGRAM_API_ID", "\"${System.getenv("TELEGRAM_API_ID") ?: ""}\"")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${System.getenv("TELEGRAM_API_HASH") ?: ""}\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("androidx.core:core:1.15.0")
}
