package me.semoro.parsekt

import java.lang.reflect.Field
import kotlin.coroutines.Continuation

private val coroutineImplClass by lazy { Class.forName("kotlin.coroutines.jvm.internal.BaseContinuationImpl") }
private val coroutine2ImplClass by lazy { Class.forName("kotlin.coroutines.jvm.internal.ContinuationImpl") }

private val interceptedField by lazy { coroutine2ImplClass.getDeclaredField("intercepted").apply { isAccessible = true } }
private val completionField by lazy { coroutineImplClass.getDeclaredField("completion").apply { isAccessible = true } }

private var <T> Continuation<T>.completion: Continuation<*>?
    get() = completionField.get(this) as Continuation<*>
    set(value) = completionField.set(this@completion, value)

var perClassCache = mutableMapOf<Class<*>, List<Field>>()

private val Continuation<*>.handles: List<Field> get() {
    val javaClass = this.javaClass
    return perClassCache.getOrPut(javaClass) {
        (javaClass.declaredFields + interceptedField).mapNotNull {
            it.isAccessible = true
            it
        }
    }
}


fun Continuation<*>.restoreStateStack(value: List<List<Pair<Field, Any?>>>) {

    var subj = this
    for (listForThis in value) {
        for ((handle, res) in listForThis) {
            handle.set(subj, res)
        }
        subj = subj.completion!!
    }
}


fun Continuation<*>.collectStateStack(): List<List<Pair<Field, Any?>>> {
    val result = mutableListOf<List<Pair<Field, Any?>>>()
    var subj: Continuation<*>? = this
    while(subj != null) {
        if (!coroutineImplClass.isInstance(subj)) break
        val handles = subj.handles
        result += handles.map { it to it.get(subj) }
        subj = subj.completion
    }
    return result
}
