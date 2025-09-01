import com.android.build.api.dsl.ApkSigningConfig
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
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
    compileSdk = 36
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
    packaging {
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}