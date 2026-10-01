import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "network.chaintech"
version = "1.0.53"

base {
    archivesName.set("compose-multiplatform-media-player")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvm(){
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                    freeCompilerArgs.add("-Xjdk-release=8")
                }
            }
        }
    }

    val xcfName = "compose-multiplatform-media-player"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
            binaryOption("bundleId", "chaintech.videoplayer")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        commonMain.dependencies {
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui.tooling.preview)

            implementation(libs.ktor.client.core)
            implementation(libs.image.loader)
            implementation("io.github.kevinnzou:compose-webview-multiplatform:2.0.3")
            implementation("net.engawapg.lib:zoomable:2.8.1")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
//            implementation(libs.vlcj)

            // 連結至 JavaCvPlayer 專案中的核心庫 (使用 Skia 渲染路徑)
//                implementation(project(":common-lite"))
            implementation("com.github.cybernhl.media:lib-common-lite:727538c430")
//                implementation(project(":core"))
            implementation("com.github.CMingTseng.JavaCvPlayer:core:1.0.0")
//            implementation(project(":core_ui_compose"))
            implementation("com.github.CMingTseng.JavaCvPlayer:core_ui_compose:1.0.0")
//            implementation(project(":core-video-skia"))
            implementation("com.github.CMingTseng.JavaCvPlayer:core-video-skia:1.0.0")


            // JavaCV & FFmpeg support
            api(libs.org.bytedeco.javacv.platform)
            api(libs.org.bytedeco.ffmpeg.platform.gpl)


            val javafxVersion = "17.0.10"
            val os = when {
                org.gradle.internal.os.OperatingSystem.current().isMacOsX -> if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
                org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
                else -> "linux"
            }
            implementation("org.openjfx:javafx-controls:$javafxVersion:$os")
            implementation("org.openjfx:javafx-graphics:$javafxVersion:$os")
            implementation("org.openjfx:javafx-swing:$javafxVersion:$os")
            implementation("org.openjfx:javafx-web:$javafxVersion:$os")
            implementation("org.openjfx:javafx-base:$javafxVersion:$os")
            implementation("org.openjfx:javafx-media:$javafxVersion:$os")
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.common)
            implementation(libs.media3.datasource)
            implementation(libs.media3.datasource.okhttp)
            implementation("androidx.media3:media3-ui:${libs.versions.media3.get()}")
            implementation("androidx.media3:media3-exoplayer-hls:${libs.versions.media3.get()}")

            implementation(libs.jetbrains.compose.ui.tooling)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
        }

        iosMain.dependencies {

        }

        wasmJsMain.dependencies {

        }
    }
}

android {
    namespace = "chaintech.videoplayer"
    compileSdk = 36
    androidResources {
        enable = true
    }
    buildFeatures {
        compose = true
    }
    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

}

compose.resources {
    publicResClass = true
    packageOfResClass = "reelsdemo.composemultiplatformmediaplayer.generated.resources"
    generateResClass = always
}

afterEvaluate {
    configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            if (artifactId.contains("media_player")) {
                artifactId = artifactId.replace("media_player", "compose-multiplatform-media-player")
            }
        }
    }
}