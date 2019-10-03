package me.semoro.parsekt

import me.semoro.parsekt.ex.InstrumentedCopy
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class BoxedThrowable(val throwable: Throwable)
object Null
inline class Res<T>(val v: Any?) {

    val isThrowable get() = v is BoxedThrowable
    val isValue get() = !isNone && !isThrowable
    val isNone get() = v === Null

    fun getOrThrow(): T {
        if (v is BoxedThrowable) throw v.throwable
        if (v === Null) error("Empty")
        return v as T
    }
}

inline fun createMultiShotState(continuation: Continuation<Unit>): MultiShotState {
    return if (continuation is InstrumentedCopy) {
        MultiShotState(continuation)
    } else {
        MultiShotState(MultiShotState.Reflective(continuation))
    }
}

inline class MultiShotState(val v: Any?) {



    inline fun resume(instrumented: Boolean) {
        val continuation = if (instrumented)
            (v as InstrumentedCopy).copy() as Continuation<Unit>
        else
            (v as Reflective).recoveredContinuation()
        continuation.resume(Unit)
    }

    class Reflective(val continuation: Continuation<Unit>) {
        val storage = continuation.collectStateStack()
        fun recoveredContinuation(): Continuation<Unit> {
            continuation.restoreStateStack(storage)
            return continuation
        }
    }
}

abstract class GrammarMatcher(val source: DataSource) {

    class ResCont<T> : Continuation<T> {
        var result = Res<T>(Null)

        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            val ex = result.exceptionOrNull()
            this.result = if (ex != null) {
                Res(BoxedThrowable(ex))
            } else {
                Res(result.getOrDefault(Null))
            }
        }
    }

    var dead: Continuation<Unit>? = null

    suspend inline fun suicide() = suspendCoroutineUninterceptedOrReturn<Unit> {
        dead = it
        COROUTINE_SUSPENDED
    }

    suspend inline fun c(char: Char) = requireOnce { it == char }
    suspend inline fun eof() {
        if (source.hasNext()) suicide()
    }

    suspend inline fun requireOnce(predicate: (Char) -> Boolean) {
        if (!source.hasNext() || !predicate(source.peek())) suicide()
        source.advance()
    }

    suspend inline fun requireOnceR(predicate: (Char) -> Boolean) = ranged {
        if (!source.hasNext() || !predicate(source.peek())) suicide()
        source.advance()
    }

    suspend inline fun cset(vararg char: Char) {
        val mask = setOf(*char.toTypedArray())
        return requireOnce { it in mask }
    }

    suspend inline fun ncset(vararg char: Char) {
        val mask = setOf(*char.toTypedArray())
        return requireOnce { it !in mask }
    }
    inline fun <T> requireMultiple(limit: Int = Int.MAX_VALUE, crossinline body: suspend () -> T): List<T> {
        val result = mutableListOf<T>()
        for (i in 0 until limit) {
            val res = requireOrFail { body() }
            if (res.isNone) break
            result.add(res.getOrThrow())
        }
        return result
    }



    suspend inline fun <T> internalRqL(limit: Int, crossinline consume: (T) -> Unit, crossinline body: suspend () -> T) {
        suspendCoroutineUninterceptedOrReturn<Unit> { continuation ->

            val instrumented = continuation as? InstrumentedCopy
            val storage = if (instrumented == null) continuation.collectStateStack() else null

            for (i in 0 until limit) {
                val before = source.position()

                val multishot = if (instrumented != null) {
                    instrumented.copy() as Continuation<Unit>
                } else {
                    continuation.restoreStateStack(storage!!)
                    continuation
                }
                multishot.resume(Unit)
                if (dead == null) {
                    return@suspendCoroutineUninterceptedOrReturn COROUTINE_SUSPENDED
                } else {
                    dead = null
                }


                source.seekAbs(before)

                val res = requireOrFail { body() }
                if (res.isNone) break
                consume(res.getOrThrow())
            }
        }

    }

    suspend inline fun <T> requireMultipleLazy(
        cmin: Int = 0, limit: Int = Int.MAX_VALUE,
        crossinline consume: (T) -> Unit,
        crossinline body: suspend () -> T
    ) {
        for (i in 0 until cmin) {
            val res = requireOrFail { body() }
            if (res.isNone) break
            consume(res.getOrThrow())
        }

        internalRqL(limit - cmin, consume) { body() }
    }

    suspend inline fun <T, R> requireMultipleLazy(
        cmin: Int = 0, limit: Int = Int.MAX_VALUE,
        initial: R, crossinline fold: (R, T) -> R,
        crossinline body: suspend () -> T
    ): R {
        var result = initial
        requireMultipleLazy(cmin, limit, { result = fold(result, it) }) { body() }
        return result
    }

    suspend inline fun <T> requireMultipleLazy(
        cmin: Int = 0, limit: Int = Int.MAX_VALUE,
        crossinline body: suspend () -> T
    ): List<T> {
        return requireMultipleLazy(cmin, limit, mutableListOf(), { acc, t -> acc.apply { add(t) } }) { body() }
    }

    @JvmName("requireMultipleLazyMatchRange")
    suspend inline fun requireMultipleLazy(
        cmin: Int = 0, limit: Int = Int.MAX_VALUE,
        crossinline body: suspend () -> MatchRange
    ): MatchRange {
        val min = source.position()
        var max = source.position()
        requireMultipleLazy(cmin, limit,
            Unit,
            { acc: Unit, t: MatchRange ->
                max = t.end
                Unit
            }
        ) { body() }
        return MatchRange(min, max)
    }

    inline fun <T> requireMatches(crossinline body: suspend () -> T): Res<T> {

        val cont = ResCont<T>()
        val res = suspend {
            body()
        }.startCoroutineUninterceptedOrReturn(cont)
        dead = null
        return when(res) {
            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED -> cont.result
            else -> Res(res)
        }
    }

    inline fun <T> requireOrFail(crossinline body: suspend () -> T): Res<T> {
        val res = requireMatches { body() }
        return res
    }

    suspend inline fun <T> requireEither(crossinline a: suspend () -> T, crossinline b: suspend () -> T): T {
        val pos = source.position()
        val res = requireOrFail(a)
        if (res.isNone) return res.getOrThrow()
        source.seekAbs(pos)
        val res1 = requireOrFail(b)
        if (res1.isNone) return res1.getOrThrow()
        return suspendCoroutine{}
    }

    inline fun <T> requireOptional(crossinline body: suspend () -> T): T? {
        val res = requireOrFail { body() }
        if (!res.isNone) return res.getOrThrow()
        return null
    }

    inline fun ranged(body: () -> Unit): MatchRange {
        val pos = source.position()
        body()
        return MatchRange(pos, source.position())
    }

    inline fun <T> match(crossinline match: suspend () -> T): T {
        val res = requireMatches { match() }
        if (res.isNone) error("Unmatched, ${source.position()}")
        return res.getOrThrow()
    }
}