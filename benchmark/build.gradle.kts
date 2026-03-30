plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":content"))

    testImplementation(libs.junit5)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
