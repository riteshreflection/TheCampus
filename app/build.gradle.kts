plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.reflection.thecampus"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.reflection.thecampus"
        minSdk = 24
        targetSdk = 36
        versionCode = 222
        versionName = "2.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "PAYMENT_API_URL", "\"https://create-payment.essyritesh.workers.dev/\"")
            buildConfigField("String", "WITHDRAWAL_API_URL", "\"https://withdrawal.essyritesh.workers.dev/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "PAYMENT_API_URL", "\"https://create-payment.essyritesh.workers.dev/\"")
            buildConfigField("String", "WITHDRAWAL_API_URL", "\"https://withdrawal.essyritesh.workers.dev/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.crashlytics)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.messaging)
    implementation("androidx.annotation:annotation:1.7.1")
    implementation(libs.firebase.analytics) // For @Keep annotation
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.cashfree.pg:api:2.2.2")
}