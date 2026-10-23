pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("androidx.*")
                excludeGroupByRegex("com\\.android.*")
            }
        }
        gradlePluginPortal {
            content {
                excludeGroupByRegex("androidx.*")
                excludeGroupByRegex("com\\.android.*")
            }
        }
    }
}

// Jedes Repository mit Content-Filter (SEC-SUP-01): Google liefert nur seine eigenen Gruppen,
// Maven Central nie AndroidX oder Android-Werkzeuge, JitPack nur sherpa-onnx und sherpa-onnx nur
// von dort. Die Prüfsummen der JitPack-Artefakte pinnt gradle/verification-metadata.xml.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("androidx.*")
                excludeGroupByRegex("com\\.android.*")
            }
        }
        // sherpa-onnx gibt es nicht auf Maven Central. JitPack liefert das unveränderte AAR des
        // Upstream-Releases (byte-gleich, SECURITY.md Herkunftsnachweis); F-Droid lässt JitPack zu.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") { name = "JitPack" } }
            filter { includeGroup("com.github.k2-fsa.sherpa-onnx") }
        }
    }
}

rootProject.name = "Atemkraft"
include(":app")
