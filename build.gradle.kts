plugins {
    kotlin("jvm") version "2.1.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
