buildscript {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.gradle.agp)
        classpath(libs.gradle.kotlin)
        classpath(libs.gradle.kotlin.serialization)
        classpath(libs.gradle.kotlinter)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("com.github.inorichi.injekt:injekt-core:65b0440")).using(module("uy.kohesive.injekt:injekt-core:1.16.1"))
                substitute(module("com.github.inorichi.injekt:injekt-core")).using(module("uy.kohesive.injekt:injekt-core:1.16.1"))
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }
}
