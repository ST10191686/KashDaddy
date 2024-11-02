
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
        // Add the Google services Gradle plugin
        id("com.google.gms.google-services")

    }

android {
    namespace = "com.example.kashdaddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kashdaddy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries

    //parsing json API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //superbase
    //implementation("io.supabase:supabase-android:1.0.0") // Use the latest version if available

    implementation ("com.google.firebase:firebase-auth:22.1.1") // Firebase Authentication
    implementation("com.google.android.gms:play-services-auth:20.7.0") // Google Sign-In
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")//Pie chart
    implementation ("com.google.android.material:material:1.9.0") // tabs
    implementation("androidx.biometric:biometric:1.2.0-alpha05")//biometrics

    // Add the Firebase Messaging dependency for FCM
    // DopeBase
    // Android Push Notifications with Kotlin: A Step-by-Step Guide
    // https://dopebase.com/android-push-notifications-kotlin-step-step-guide

    implementation("com.google.firebase:firebase-messaging")

}
