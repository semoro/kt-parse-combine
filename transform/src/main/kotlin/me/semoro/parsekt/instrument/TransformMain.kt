package me.semoro.parsekt.instrument

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File

object TransformMain {



    fun transform(path: File) {

        val reader = ClassReader(path.readBytes())

        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        val transformer = ClassTransformer(Opcodes.ASM7, writer)
        var skip = true
        val checker = object : ClassVisitor(Opcodes.ASM7, transformer) {
            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String,
                interfaces: Array<out String>?
            ) {
                if ("ContinuationImpl" in superName || "SuspendLambda" in superName) {
                    skip = false
                } else {
                    cv = null
                }
                super.visit(version, access, name, signature, superName, interfaces)
            }
        }
        reader.accept(checker, 0)
        if (!skip) {
            path.writeBytes(writer.toByteArray())
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {

        println(args.contentToString())
        for(arg in args) {
            val f = File(arg).parentFile
            f.listFiles().filter {
                it.toString().startsWith(arg)
            }.forEach {
                transform(it)
            }
        }
    }

}