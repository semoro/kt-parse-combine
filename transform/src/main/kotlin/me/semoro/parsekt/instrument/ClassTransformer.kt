package me.semoro.parsekt.instrument

import org.objectweb.asm.*
import kotlin.coroutines.Continuation

private val itfType = Type.getObjectType("me/semoro/parsekt/ex/InstrumentedCopy")

private class FieldInfo(val owner: String, val name: String, val descriptor: String)

private inline class MVCtx(val visitor: MethodVisitor) {
    fun FieldInfo.loadFromThis() = with(visitor) {
        visitVarInsn(Opcodes.ALOAD, 0)
        load()
    }

    fun FieldInfo.copyToLocal() = with(visitor) {
        visitInsn(Opcodes.DUP) // dup copy
        loadFromThis() // -> value
        store() // kill value, copy
    }

    fun FieldInfo.loadOrStore(opcode: Int) = with(visitor) {
        visitFieldInsn(opcode, owner, name, descriptor)
    }

    fun FieldInfo.store() = loadOrStore(Opcodes.PUTFIELD)
    fun FieldInfo.load() = loadOrStore(Opcodes.GETFIELD)
}

private const val continuationImplFqName = "kotlin/coroutines/jvm/internal/ContinuationImpl"
private val copyName = "copy"
private val copyDesc = Type.getMethodDescriptor(itfType)
private val continuationType = Type.getType(Continuation::class.java)

private val baseContinuationImpl = "kotlin/coroutines/jvm/internal/BaseContinuationImpl"

//private val completionField = FieldInfo("kotlin/coroutines/jvm/internal/BaseContinuationImpl", "completion", continuationType.descriptor)
private val interceptedField = FieldInfo(continuationImplFqName, "intercepted", continuationType.descriptor)



class ClassTransformer(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {

    var className = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        val newInterfaces = if (interfaces == null) arrayOf(itfType.internalName) else arrayOf(*interfaces, itfType.internalName)
        super.visit(version, access, name, signature, superName, newInterfaces)
        className = name
        otherFields.clear()
    }

    private val otherFields = mutableListOf<FieldInfo>()

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        otherFields.add(FieldInfo(className, name, descriptor))
        return super.visitField(access, name, descriptor, signature, value)
    }




    fun generateCopyMethod() {
        val visitor = visitMethod(Opcodes.ACC_PUBLIC, copyName, copyDesc, null, null)
        visitor.visitCode()
        visitor.apply {
            MVCtx(visitor).apply {

                visitVarInsn(Opcodes.ALOAD, 0)
                visitInsn(Opcodes.DUP)

                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    baseContinuationImpl,
                    "getCompletion",
                    Type.getMethodDescriptor(continuationType),
                    false
                ) // ret value -> value

                visitInsn(Opcodes.DUP) // dup value -> value, value
                val jumpOver = Label()
                // if (completion is InstrumentedCopy)
                visitTypeInsn(Opcodes.INSTANCEOF, itfType.internalName) // kill value, add res -> value, res
                visitJumpInsn(Opcodes.IFEQ, jumpOver) // kill res -> value
                // {
                // val newCompletion = completion.copy()
                visitMethodInsn(Opcodes.INVOKEINTERFACE, itfType.internalName, copyName, copyDesc, true) // kill value, add value_copy -> value_copy
                visitTypeInsn(Opcodes.CHECKCAST, continuationType.internalName)
                // }
                visitLabel(jumpOver)



                visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "create", Type.getMethodDescriptor(continuationType, continuationType), false)
                visitTypeInsn(Opcodes.CHECKCAST, className)


                for (field in otherFields) {
                    field.copyToLocal()
                }

                visitInsn(Opcodes.ARETURN)
            }
        }
        visitor.visitMaxs(0, 0)
        visitor.visitEnd()
    }

    override fun visitEnd() {
        generateCopyMethod()
        super.visitEnd()
    }
}

