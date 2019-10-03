package me.semoro.parsekt.instrument

import org.objectweb.asm.ClassVisitor

data class GrammarDesc(val grammarClass: String, val inners: List<String>)

abstract class GrammarFinder(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {

    abstract fun onGrammarFound(desc: GrammarDesc)
    abstract fun isKnownGrammarSubClass(name: String): Boolean

    fun isGrammarSubClass(name: String): Boolean = isGrammarClass(name) || isKnownGrammarSubClass(name)
    fun isGrammarClass(name: String): Boolean = name == "me/semoro/parsekt/GrammarMatcher"

    var shouldProcess: Boolean = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        inners.clear()
        shouldProcess = isGrammarSubClass(name)
    }

    val inners = mutableListOf<String>()

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)
    }

    override fun visitEnd() {
        inners
    }
}