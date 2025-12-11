import java.util.Properties

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.androidApplication)
}

fun File.normalizeUserHome(): File {
    return if (absolutePath.contains("~/")) {
        File(System.getProperty("user.home")).resolve(absolutePath.substringAfter("~/"))
    } else {
        this
    }
}

android {
    namespace = "packages"
    compileSdk = 36
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    signingConfigs {
        create("release") {
            val signingConfigFile =
                rootProject.file("local.properties")
                    .takeIf { it.exists() }
                    ?.let {
                        it.resolveSibling(
                            File(Properties().apply {
                                load(it.inputStream())
                            }.getProperty("signing.config")).normalizeUserHome()
                        )
                    }
            when {
                signingConfigFile?.exists() == true -> {
                    val signingProps = Properties()
                    signingProps.load(signingConfigFile.inputStream())
                    storeFile =
                        signingConfigFile.resolveSibling(signingProps.getProperty("signing.store.file"))
                    storePassword = signingProps.getProperty("signing.store.password")
                    keyAlias = signingProps.getProperty("signing.key.alias")
                    keyPassword = signingProps.getProperty("signing.key.password")
                }
                else -> {
                    // Use debug keystore for development
                    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }
    defaultConfig {
        applicationId = "io.github.taosha.packages"
        minSdk = 23
        targetSdk = 36
        versionCode = 19
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFiles.add(file("proguard-rules.pro"))
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile != null) {
                signingConfig = releaseConfig
            }
        }
        debug {
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile != null) {
                signingConfig = releaseConfig
            }
        }
    }
    namespace = "packages"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
