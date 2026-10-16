import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vinyl.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vinyl.launcher"
        minSdk = 29
        targetSdk = 36
        // Nomor build CI dipakai sebagai versionCode agar setiap rilis bisa dipasang sebagai pembaruan
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = 100 + run
        versionName = "1.0.$run"
    }

    // Kunci tanda tangan TETAP (disimpan di repo). Tanpa ini, setiap build di GitHub Actions
    // memakai kunci debug acak → Android menolak pembaruan ("Aplikasi tidak terinstal").
    signingConfigs {
        create("vinyl") {
            storeFile = file("vinyl.keystore")
            storePassword = "vinyl-launcher"
            keyAlias = "vinyl"
            keyPassword = "vinyl-launcher"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("vinyl")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("vinyl")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

/*
 * Font (Inter, Instrument Serif, JetBrains Mono — semuanya lisensi OFL) diunduh otomatis
 * dari repositori google/fonts saat build pertama, lalu disimpan di res/font.
 */
val fetchFonts by tasks.registering {
    val fontDir = file("src/main/res/font")
    outputs.dir(fontDir)
    doLast {
        val base = "https://github.com/google/fonts/raw/main/ofl"
        val fonts = mapOf(
            "inter.ttf" to "$base/inter/Inter%5Bopsz,wght%5D.ttf",
            "instrument_serif.ttf" to "$base/instrumentserif/InstrumentSerif-Regular.ttf",
            "jetbrains_mono.ttf" to "$base/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf",
        )
        fontDir.mkdirs()
        fonts.forEach { (name, url) ->
            val target = File(fontDir, name)
            if (!target.exists() || target.length() < 10_000) {
                println("Mengunduh font $name")
                URI(url).toURL().openStream().use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn(fetchFonts) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}
