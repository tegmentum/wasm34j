pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // wasm34j / wasm34j-jni are consumed from the local Maven repo until published.
        mavenLocal()
    }
}

rootProject.name = "wasm34j-android"
