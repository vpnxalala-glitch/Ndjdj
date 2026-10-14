plugins {
    id("com.android.application")
}

val telegramApiId = providers.environmentVariable("VICHGRAM_TG_API_ID")
    .orElse(providers.gradleProperty("vichgramTelegramApiId"))
    .getOrElse("0")
    .toIntOrNull() ?: 0
val telegramApiHash = providers.environmentVariable("VICHGRAM_TG_API_HASH")
    .orElse(providers.gradleProperty("vichgramTelegramApiHash"))
    .getOrElse("")

android {
    namespace = "com.vich.assistant"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.vich.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "3").toInt()
        versionName = "0.3.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
        buildConfigField("int", "TELEGRAM_API_ID", telegramApiId.toString())
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$telegramApiHash\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("androidx.core:core:1.15.0")
}
