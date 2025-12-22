pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "wyplayRepositoryReleases"
            url = uri(extra.properties["wycdnMavenRepoUrl"] as String? ?: "https://maven.wyplay.com/releases")
            credentials(PasswordCredentials::class)
        }
    }

    // Allow to override wycdnService version from gradle.properties or from command-line
    // (eg: `./gradlew build -P wycdnService=x.x.x`)
    val wycdnService = extra.properties["wycdnService"] as String?
    if (wycdnService != null) {
        versionCatalogs {
            create("libs") {
                version("wycdnService", wycdnService)
            }
        }
    }
}

rootProject.name = "wycdn-sampleapp-android"
include(":wycdn-sampleapp")
