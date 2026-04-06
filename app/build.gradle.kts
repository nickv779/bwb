plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ex.bwb"
    compileSdk = 36

    defaultConfig {
        applicationId = "edu.ufl.gameenginedev"
        minSdk = 19
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

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
        sourceCompatibility = JavaVersion.VERSION_15
        targetCompatibility = JavaVersion.VERSION_15
    }
}

dependencies {
    implementation(libs.multidex)
    implementation(libs.annotation)
    implementation(libs.core)
    implementation(libs.constraintlayout.v214)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.ui.text)

    implementation(libs.jbullet)
    testImplementation(libs.junit)

}