pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

include(":core")

// Strictly load ONLY the 5 requested extensions
val requestedExtensions = listOf("tnaflix", "eporner", "xhamster", "spankbang", "bigfuck")
requestedExtensions.forEach { include("src:en:$it") }
