plugins {
    kotlin("jvm") version "1.3.50"
}

group = "org.example"
version = "1.0-SNAPSHOT"
allprojects {
    repositories {
        mavenCentral()
    }
}


subprojects {
    apply(plugin = "kotlin")


    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

}