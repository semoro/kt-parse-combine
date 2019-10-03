import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
apply(plugin = "kotlin")

val transformRuntime by configurations.creating

dependencies {
    transformRuntime(project(":transform"))
}

val runTransform by tasks.creating(JavaExec::class) {
    classpath = transformRuntime
    main = "me.semoro.parsekt.instrument.TransformMain"

    afterEvaluate {

        val kotlinSourceSetOut =
            sourceSets["main"].output.classesDirs.filter { it.endsWith("kotlin/main") }.singleFile
        args("${kotlinSourceSetOut}/me/semoro/parsekt/GrammarMatcher")
        args("${kotlinSourceSetOut}/me/semoro/parsekt/TraceGrammar")
        args("${kotlinSourceSetOut}/me/semoro/parsekt/TestKt")


        outputs.dir(kotlinSourceSetOut)
        dependsOn(transformRuntime)
        dependsOn(tasks.compileKotlin)
        tasks.getByName("classes").dependsOn(this@creating)
    }
}
