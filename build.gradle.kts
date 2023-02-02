plugins {
    java
    checkstyle
    distribution
    maven
    id("org.omegat.gradle") version "1.5.9"
}

version = "0.1.0"

omegat {
    version = "5.7.1"
    pluginClass = "org.omegat.connectors.machinetranslators.MicrosoftTranslatorAzure"
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
}

repositories {
    mavenCentral()
}

checkstyle {
    isIgnoreFailures = true
    toolVersion = "7.1"
}

distributions {
    main {
        contents {
            from(tasks["jar"], "README.md", "COPYING", "CHANGELOG.md")
        }
    }
}
