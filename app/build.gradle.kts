import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(true)
}

// Release signing reads from a gitignored keystore.properties (copy
// keystore.properties.example and fill it in). When it's absent the release
// build falls back to the debug key, so it stays installable for local testing
// but is NOT suitable for the Play Store.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            FileInputStream(keystorePropertiesFile).use { load(it) }
        }
    }

android {
    namespace = "com.floppyzedolfin.auloup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.floppyzedolfin.auloup"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // Only define the release config when the keystore is present; otherwise
        // the release build below falls back to the debug key.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (keystorePropertiesFile.exists()) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// AGP 9 ships built-in Kotlin support, so the standalone kotlin-android plugin is
// gone and compiler options move from android.kotlinOptions to this top-level block.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// Drop a copy of every built APK named "auloup.apk" next to the Gradle default
// ("app-<variant>.apk"). The legacy applicationVariants output-renaming API was
// removed in AGP 9, so instead we finalize each variant's `assemble` task with a
// Copy that renames the packaged APK. This leaves the real APK artifact and its
// metadata untouched (install/bundle/release keep working) and just adds the
// fixed-name copy that CI uploads and the Makefile/docs reference. Each variant
// writes to its own directory (debug/, release/), so the names never collide.
androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val apkDir = variant.artifacts.get(SingleArtifact.APK)
        // A plain action (not a Copy task) that declares the APK dir only as an
        // input: it depends on packaging but does NOT claim the shared output
        // directory, which Gradle 9 would otherwise flag as overlapping with
        // AGP's own listing tasks. It runs as a finalizer of `assemble`.
        val copyApk =
            tasks.register("copy${variantName}AuloupApk") {
                inputs.dir(apkDir)
                doLast {
                    val dir = apkDir.get().asFile
                    val built =
                        dir.listFiles { f -> f.extension == "apk" }?.firstOrNull()
                            ?: throw GradleException("No APK produced in $dir")
                    built.copyTo(dir.resolve("auloup.apk"), overwrite = true)
                }
            }
        afterEvaluate {
            tasks.named("assemble$variantName").configure { finalizedBy(copyApk) }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.libphonenumber)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
