pluginManagement {
    repositories {
        // Canonical Maven Central, listed first on purpose. ~/.gradle/init.gradle
        // rewrites repo.maven.apache.org -> aliyun, and aliyun lags on KSP releases
        // (2.0.21-1.0.28 404s there), so KSP must resolve from the real thing.
        // repo1.maven.org is not in that mirror map, so it is left untouched.
        maven { url = uri("https://repo1.maven.org/maven2") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tahdig"
include(":app")
