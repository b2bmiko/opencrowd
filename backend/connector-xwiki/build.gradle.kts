plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    implementation(project(":connectors-sdk"))
    implementation(project(":core"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.spring.boot.starter.test)
}
