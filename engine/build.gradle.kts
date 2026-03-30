plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    testImplementation(libs.junit5)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
