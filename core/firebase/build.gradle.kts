plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.unaj.appmoviles.network"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    api(platform(libs.firebase.bom))
    api(libs.firebase.auth)                       // FirebaseAuth aparece en el constructor público
    api(libs.firebase.firestore)                  // FirebaseFirestore aparece en el constructor público
    api(libs.firebase.storage)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(project(":core:domain"))   // solo para comprobar que las fechas coinciden con toFechaCalendario()
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}