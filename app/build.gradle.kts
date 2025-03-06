plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.sensorrecorderapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sensorrecorderapp"
        minSdk = 26
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
}

configurations.all {
    exclude(group = "com.android.support", module = "support-compat")
    exclude(group = "com.android.support", module = "support-v4")
}

dependencies {

    implementation(libs.appcompat) {
        exclude(group = "com.android.support")
    }
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.core) {
        exclude(group = "com.android.support", module = "support-compat")
    }
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.wear.tiles:tiles-material:1.2.0")
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.opencsv:opencsv:5.7.1")
    implementation("com.google.firebase:firebase-firestore:23.0.3")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.jjoe64:graphview:4.2.2")
}