import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.calisthenics.ai"
    compileSdk = 35

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                load(stream)
            }
        }
    }

    val defaultProdApiBaseUrl = "https://api.sky-enterprise.id.vn/"
    val prodApiBaseUrl = (project.findProperty("API_BASE_URL") as? String)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: System.getenv("API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: defaultProdApiBaseUrl
    val prodApiUri = URI(prodApiBaseUrl)
    require(prodApiUri.scheme?.equals("https", ignoreCase = true) == true) {
        "Production API_BASE_URL must use HTTPS."
    }
    require(prodApiUri.host?.equals("api.sky-enterprise.id.vn", ignoreCase = true) == true) {
        "Production API_BASE_URL must use the configured VPS API host."
    }

    val devApiBaseUrl = (project.findProperty("DEV_API_BASE_URL") as? String)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("DEV_API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: System.getenv("DEV_API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: "http://10.0.2.2:8080/"

    val normalizedProdApiBaseUrl = if (prodApiBaseUrl.endsWith("/")) prodApiBaseUrl else "$prodApiBaseUrl/"
    val normalizedDevApiBaseUrl = if (devApiBaseUrl.endsWith("/")) devApiBaseUrl else "$devApiBaseUrl/"

    defaultConfig {
        applicationId = "com.calisthenics.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["appName"] = "SKY"

        val googleClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")?.takeIf { it.isNotBlank() }
            ?: System.getenv("GOOGLE_WEB_CLIENT_ID")?.takeIf { it.isNotBlank() }
            ?: "401410597082-lqt3t94tlan0lbvc44kttifma34pjpaq.apps.googleusercontent.com"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appName"] = "SKY Dev"
            buildConfigField("String", "API_BASE_URL", "\"$normalizedDevApiBaseUrl\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appName"] = "SKY"
            buildConfigField("String", "API_BASE_URL", "\"$normalizedProdApiBaseUrl\"")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = (project.findProperty("ANDROID_KEYSTORE_PATH") as? String)
                ?: System.getenv("ANDROID_KEYSTORE_PATH")
                ?: (project.findProperty("RELEASE_STORE_FILE") as? String)
                ?: System.getenv("RELEASE_STORE_FILE")
            val keystorePassword = (project.findProperty("ANDROID_KEYSTORE_PASSWORD") as? String)
                ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
                ?: (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keyAlias = (project.findProperty("ANDROID_KEY_ALIAS") as? String)
                ?: System.getenv("ANDROID_KEY_ALIAS")
                ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPassword = (project.findProperty("ANDROID_KEY_PASSWORD") as? String)
                ?: System.getenv("ANDROID_KEY_PASSWORD")
                ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else if ((project.findProperty("USE_DEBUG_SIGNING_FOR_RELEASE") as? String)?.toBoolean() == true
                || System.getenv("USE_DEBUG_SIGNING_FOR_RELEASE")?.toBoolean() == true) {
                signingConfig = signingConfigs.getByName("debug")
            }
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
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INDEX/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.configureEach {
    if (name.startsWith("processDev") && name.endsWith("GoogleServices") &&
        !file("src/dev/google-services.json").exists()
    ) {
        enabled = false
    }
}
