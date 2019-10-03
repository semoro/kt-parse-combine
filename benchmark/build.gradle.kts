plugins {
    id("kotlinx.benchmark") version "0.2.0-dev-5"
}

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime-jvm:0.2.0-dev-5")
    compile(project(":runtime"))
}
benchmark {
    val main by configurations
    main.apply {
        include("SimpleBenchmark")
    }

    targets {
        register("main") {
            //jmhVersion = "1.21" // available only for JVM compilations & Java source sets
        }
    }
}
