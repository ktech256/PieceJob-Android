plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
}

android {
    namespace = "com.piecejob"
    compileSdk = 35

    configurations.all {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }

    defaultConfig {
        applicationId = "com.piecejob"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val mapsApiKey = project.findProperty("GOOGLE_MAPS_API_KEY") ?: ""
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    flavorDimensions += "app"
    productFlavors {
        create("customer") {
            dimension = "app"
            applicationIdSuffix = ".customer"
            versionNameSuffix = "-customer"
            resValue("string", "app_name", "PieceJob")
        }
        create("provider") {
            dimension = "app"
            applicationIdSuffix = ".provider"
            versionNameSuffix = "-provider"
            resValue("string", "app_name", "PieceJob Provider")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Dagger Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.play.services.places)
    implementation(libs.google.navigation)

    // Socket.io
    implementation(libs.socket.io)

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
