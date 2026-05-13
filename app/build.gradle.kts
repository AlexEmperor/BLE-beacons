plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bleapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.bleapp"
        minSdk = 31
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.7.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "AUTH_BASE_URL",
            "\"https://bleapp-auth.bleapp-auth.workers.dev\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

tasks.register("generateVersionJson") {
    val outputFile = layout.buildDirectory.file("outputs/update/version.json")
    outputs.file(outputFile)

    doLast {
        val versionCode = android.defaultConfig.versionCode ?: 1
        val versionName = android.defaultConfig.versionName ?: "1.0.4"
        val apkUrl = providers
            .gradleProperty("updateApkUrl")
            .orElse("https://github.com/AlexEmperor/BLE-beacons/releases/latest/download/app-release.apk")
            .get()

        val json = """
            {
              "versionCode": $versionCode,
              "versionName": "$versionName",
              "message": "Хорошие новости! Вышла новая версия приложения.",
              "apkUrl": "$apkUrl",
              "required": false
            }
        """.trimIndent()

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(json)
    }
}

dependencies {
    dependencies {

        implementation("androidx.core:core-ktx:1.10.1")

        implementation("androidx.activity:activity-compose:1.8.0")

        implementation(platform("androidx.compose:compose-bom:2024.02.00"))

        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.material3:material3")

        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

        implementation("com.caverock:androidsvg-aar:1.4")

        implementation("org.osmdroid:osmdroid-android:6.1.20")

        implementation("androidx.security:security-crypto:1.1.0-alpha06")
    }
}
