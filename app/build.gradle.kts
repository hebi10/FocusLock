plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val releaseSigningNames = listOf(
    "ANDROID_RELEASE_KEYSTORE_PATH", "ANDROID_RELEASE_STORE_PASSWORD",
    "ANDROID_RELEASE_KEY_ALIAS", "ANDROID_RELEASE_KEY_PASSWORD", "ANDROID_RELEASE_CERT_SHA256"
)
val releaseSigningEnvironment = releaseSigningNames.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigningEnvironment.values.all { !it.isNullOrBlank() }
val requestedVersionCode = providers.gradleProperty("versionCode")
    .orElse(providers.environmentVariable("ANDROID_VERSION_CODE")).orElse("2").get()
val releaseVersionCode = requestedVersionCode.toIntOrNull()
    ?.takeIf { it in 1..2100000000 }
    ?: throw GradleException("versionCode / ANDROID_VERSION_CODE must be an integer between 1 and 2100000000")

android {
    namespace = "com.haebi.focuslock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.haebi.focuslock"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("existingRelease") {
                storeFile = file(releaseSigningEnvironment.getValue("ANDROID_RELEASE_KEYSTORE_PATH")!!)
                storePassword = releaseSigningEnvironment.getValue("ANDROID_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningEnvironment.getValue("ANDROID_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningEnvironment.getValue("ANDROID_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("existingRelease")
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val verifyReleaseSigning by tasks.registering(Exec::class) {
    doFirst {
        val missing = releaseSigningNames.filter { releaseSigningEnvironment[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw GradleException("Release signing requires the provisioned app signer: ${missing.joinToString()}")
        }
        commandLine(
            "python3", rootProject.file("scripts/release_signing.py").absolutePath,
            "verify-keystore", "--keystore", releaseSigningEnvironment.getValue("ANDROID_RELEASE_KEYSTORE_PATH")!!
        )
    }
}
tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(verifyReleaseSigning)
}

dependencies {
    implementation(libs.activity.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
