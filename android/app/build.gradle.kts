plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "app.piru.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.piru.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/catalog"))
}

val syncCatalog by tasks.registering(Copy::class) {
    from(rootProject.file("../data/snapshots/substances.json"))
    into(layout.buildDirectory.dir("generated/catalog/catalog"))
}

tasks.named("preBuild") { dependsOn(syncCatalog) }

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}
