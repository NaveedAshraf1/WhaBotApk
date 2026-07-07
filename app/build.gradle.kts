import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.whabotpro"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.example.whabotpro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

        // API keys — loaded from secrets.properties (gitignored) or env vars.
        val secretsFile = rootProject.file("secrets.properties")
        val secrets = Properties()
        if (secretsFile.exists()) secrets.load(secretsFile.inputStream())
        fun secret(key: String): String =
            (project.findProperty(key) as String?) ?: System.getenv(key) ?: secrets.getProperty(key) ?: ""
        val groqKey = secret("GROQ_API_KEY")
        val geminiKey = secret("GEMINI_API_KEY")
        val mistralKey = secret("MISTRAL_API_KEY")
        val hfKey = secret("HUGGINGFACE_API_KEY")
        val openrouterKey = secret("OPENROUTER_API_KEY")
        val cohereKey = secret("COHERE_API_KEY")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "MISTRAL_API_KEY", "\"$mistralKey\"")
        buildConfigField("String", "HUGGINGFACE_API_KEY", "\"$hfKey\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openrouterKey\"")
        buildConfigField("String", "COHERE_API_KEY", "\"$cohereKey\"")

        externalNativeBuild {
            cmake {
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    packaging {
        jniLibs {
            pickFirsts += listOf("libnode.so")
            useLegacyPackaging = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    debugImplementation(libs.compose.ui.tooling)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // OkHttp
    implementation(libs.okhttp)

    // ZXing
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // Gson
    implementation(libs.gson)

    // DataStore
    implementation(libs.datastore.preferences)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
