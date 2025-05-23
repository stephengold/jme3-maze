// Gradle script to build the jme3-maze project

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
    checkstyle  // to analyze Java sourcecode for style violations
}

val isMacOS = DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX()
val javaVersion = JavaVersion.current()

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass = "jme3maze.MazeGame"
}
tasks.register<JavaExec>("runAssetProcessor") {
    mainClass = "jme3maze.AssetProcessor"
    description = "Converts Blender 3-D models to runtime (.j3o) format."
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

tasks.withType<JavaCompile>().all { // Java compile-time options:
    options.compilerArgs.add("-Xdiags:verbose")
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_20)) {
        // Suppress warnings that source value 8 is obsolete.
        options.compilerArgs.add("-Xlint:-options")
    }
    options.compilerArgs.add("-Xlint:unchecked")
    options.setDeprecation(true) // to provide detailed deprecation warnings
    options.encoding = "UTF-8"
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_1_10)) {
        options.release = 8
    }
}

val enableNativeAccess = JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)

tasks.withType<JavaExec>().all { // Java runtime options:
    if (isMacOS) {
        jvmArgs("-XstartOnFirstThread")
    } else {
        args("--showSettingsDialog")
    }
    //args("--verbose") // to enable additional log output
    classpath = sourceSets.main.get().getRuntimeClasspath()
    enableAssertions = true
    if (enableNativeAccess) {
        jvmArgs("--enable-native-access=ALL-UNNAMED") // suppress System::load() warning
    }
    //jvmArgs("-verbose:gc")
    //jvmArgs("-Xms512m", "-Xmx512m") // to enlarge the Java heap
    //jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=10")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

dependencies {
    implementation(libs.heart)
    implementation(libs.acorus)
    implementation(libs.jme3.utilities.x)
    implementation(libs.tonegodgui)

    implementation(libs.jme3.blender)
    runtimeOnly(libs.jme3.lwjgl3)
    runtimeOnly(libs.jme3.testdata)

    // jme3-maze doesn't use jme3-jogg nor jme3-plugins
    //  -- they are included solely to avoid warnings from AssetConfig.
    runtimeOnly(libs.jme3.jogg)
    runtimeOnly(libs.jme3.plugins)
}

// Register cleanup tasks:

tasks.named("clean") {
    dependsOn("cleanLogs", "cleanSandbox")
}

tasks.register<Delete>("cleanLogs") { // JVM crash logs
    delete(fileTree(".").matching{ include("hs_err_pid*.log") })
}
tasks.register<Delete>("cleanSandbox") { // Acorus sandbox
    delete("Written Assets")
}
