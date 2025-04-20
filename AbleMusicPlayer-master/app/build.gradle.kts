plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "io.github.uditkarode.able"
    ndkVersion = "24.0.8215888"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.uditkarode.able"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "InterdimensionalBoop"

        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
        buildFeatures {
            viewBinding = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("STORE_PASS")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASS")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildTypes {
        getByName("release") {
            postprocessing {
                isRemoveUnusedCode = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFiles("proguard-rules.pro")
            }
            signingConfig = signingConfigs["release"]
        }
    }

    externalNativeBuild {
        cmake {
            path("CMakeLists.txt")
        }
    }
    packagingOptions {
        jniLibs {
            excludes += listOf("lib/x86_64/**", "lib/armeabi-v7a/**", "lib/x86/**")
        }
        resources {
            excludes += listOf("lib/x86_64/**", "lib/armeabi-v7a/**", "lib/x86/**")
        }
    }
}

dependencies {
    implementation(projects.core.model)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.calligraphy3)
    implementation(libs.viewpump)
    implementation(libs.recyclerview)
    implementation(libs.core)
    implementation(libs.material.dialogs.input)
    implementation(libs.okhttp)

 implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.5")
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    // Add this if you don't have it for coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
implementation("com.google.android.material:material:1.6.0")
implementation("androidx.cardview:cardview:1.0.0")
    //noinspection GradleDependency
    implementation(libs.material.intro.screen)
    implementation(libs.lottie)
//    implementation(libs.gradient)
    implementation(libs.gson)
    implementation(libs.material.dialogs.bottomsheets)
    implementation(libs.glide)
    implementation(libs.analytics)
    implementation(libs.roundedimageview)
    implementation(libs.preference.ktx)
    implementation(libs.preferencex)
    implementation(libs.coordinatorlayout)
    implementation(libs.constraintlayout)
//    implementation(libs.xfetch2)
    implementation(libs.work.runtime)
    implementation(libs.palette.ktx)
    implementation(libs.jaudiotagger.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    annotationProcessor(libs.glide.compiler)
    implementation(files("../app/src/main/libs/mobile-ffmpeg.aar"))
}

//sourceSets {
//    getByName("main").java.srcDirs("src/main/kotlin")
//}