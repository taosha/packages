import com.android.build.api.dsl.ApkSigningConfig
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
}

fun File.normalizeUserHome(): File {
    return if (absolutePath.contains("~/")) {
        File(System.getProperty("user.home")).resolve(absolutePath.substringAfter("~/"))
    } else {
        this
    }
}

infix fun ApkSigningConfig.load(properties: File) {
    val conf = properties.normalizeUserHome()
    val props = conf.reader().use { Properties().apply { load(it) } }
    storeFile =
        conf.resolveSibling(File(props.getProperty("signing.store.file")).normalizeUserHome())
    storePassword = props.getProperty("signing.store.password")
    keyAlias = props.getProperty("signing.key.alias")
    keyPassword = props.getProperty("signing.key.password")
}

android {
    namespace = "packages"
    compileSdk = 33
    viewBinding {
        enable = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    signingConfigs {
        create("release") {
            load(file(project.properties["signing.config"] ?: "signing.properties"))
        }
    }
    defaultConfig {
        applicationId = "io.github.taosha.packages"
        minSdk = 21
        targetSdk = 33
        versionCode = 15
        versionName = "1.3.0"

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
            proguardFiles.add(File("proguard-rules.pro"))
            signingConfig = signingConfigs["release"]
        }
        debug {
            signingConfig = signingConfigs["release"]
        }
    }
    namespace = "packages"
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}